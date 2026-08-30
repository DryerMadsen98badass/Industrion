package net.mads.industron.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.mads.industron.Industron;
import net.mads.industron.material.structure.StructureBlockDefinition;
import net.mads.industron.material.structure.StructureMaterial;
import net.mads.industron.material.structure.StoneMaterial;
import net.mads.industron.material.structure.MetalMaterial;
import net.mads.industron.material.structure.StructureMaterialGenerator;
import net.mads.industron.material.structure.StructureMaterials;
import net.mads.industron.material.structure.StructureSetResolver;
import net.mads.industron.registry.BlockRegistry;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.ModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Generates standard blockstates/models; specialized metal families use MetalStructureModelProvider. */
public final class StructureMaterialBlockStateProvider extends BlockStateProvider {
    private final ExistingFileHelper existingFileHelper;

    public StructureMaterialBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Industron.MOD_ID, existingFileHelper);
        this.existingFileHelper = existingFileHelper;
    }

    @Override
    public String getName() {
        return "Structure Material Block States: " + Industron.MOD_ID;
    }

    @Override
    protected void registerStatesAndModels() {
        for (StructureMaterial material : StructureMaterials.ALL) {
            for (StructureBlockDefinition definition : StructureMaterialGenerator.generatedBlockDefinitions(material)) {
                if (material instanceof MetalMaterial && requiresCustomMetalProvider(definition)) {
                    continue;
                }
                registerDefinition(definition);
            }
        }
    }

    private static boolean requiresCustomMetalProvider(StructureBlockDefinition definition) {
        return switch (definition.modelKind()) {
            case DEFAULT, CUTOUT_CUBE, VANILLA_DOOR, VANILLA_TRAPDOOR -> false;
            default -> true;
        };
    }

    private void registerDefinition(StructureBlockDefinition definition) {
        Block block = BlockRegistry.getStructureMaterialBlock(definition.registryName()).get();
        Optional<ExistingTextures> inherited = inheritedExistingTextures(definition);
        inherited.ifPresent(this::trackInheritedTextures);
        if (requiresExistingBaseTexture(definition) && inherited.isEmpty()) {
            var stone = (StoneMaterial) definition.material();
            var basePart = definition.basePart().orElseThrow();
            throw new IllegalStateException(
                    "Could not resolve textures from existing base " + stone.existingPart(basePart)
                            + " for generated " + definition.registryName() + " (base part " + basePart + ")"
            );
        }
        ResourceLocation side = inherited.map(ExistingTextures::side)
                .orElseGet(() -> texture(definition, definition.textureFile()));
        ResourceLocation top = inherited.map(ExistingTextures::top)
                .orElseGet(() -> definition.topTextureFile().map(file -> texture(definition, file)).orElse(side));
        ResourceLocation bottom = inherited.map(ExistingTextures::bottom)
                .orElseGet(() -> definition.bottomTextureFile().map(file -> texture(definition, file)).orElse(side));
        String name = definition.registryName();

        switch (definition.shape()) {
            case CUBE -> {
                ModelFile model;
                if (top.equals(side) && bottom.equals(side)) {
                    var builder = models().cubeAll(name, side);
                    if (definition.modelKind() == StructureBlockDefinition.ModelKind.CUTOUT_CUBE) {
                        builder.renderType("cutout");
                    }
                    model = builder;
                } else if (top.equals(bottom)) {
                    model = models().cubeColumn(name, side, top);
                } else {
                    model = models().cubeBottomTop(name, side, bottom, top);
                }
                simpleBlockWithItem(block, model);
            }
            case PILLAR -> {
                axisBlock((RotatedPillarBlock) block, side, top);
                itemModels().getBuilder(name).parent(models().getBuilder(name));
            }
            case SLAB -> {
                // Never depend on an external stone model for the double-slab state.
                // Every material uses the same generated cube template, while the
                // actual side/top/bottom textures still come from this definition.
                ResourceLocation doubleModel = sharedDoubleSlabModel(name, side, bottom, top);
                slabBlock((SlabBlock) block, doubleModel, side, bottom, top);
                itemModels().getBuilder(name).parent(models().getBuilder(name));
            }
            case STAIRS -> {
                stairsBlock((StairBlock) block, name, side, bottom, top);
                itemModels().getBuilder(name).parent(generatedBlockModel(name + "_stairs"));
            }
            case WALL -> {
                wallBlock((WallBlock) block, name, side);
                ModelFile inventory = models()
                        .withExistingParent(name + "_inventory", mcLoc("block/wall_inventory"))
                        .texture("wall", side);
                itemModels().getBuilder(name).parent(inventory);
            }
            case FENCE -> {
                fenceBlock((FenceBlock) block, name, side);
                ModelFile inventory = models()
                        .withExistingParent(name + "_inventory", mcLoc("block/fence_inventory"))
                        .texture("texture", side);
                itemModels().getBuilder(name).parent(inventory);
            }
            case FENCE_GATE -> {
                fenceGateBlock((FenceGateBlock) block, name, side);
                itemModels().getBuilder(name).parent(generatedBlockModel(name + "_fence_gate"));
            }
            case BUTTON -> {
                buttonBlock((ButtonBlock) block, side);
                ModelFile inventory = models()
                        .withExistingParent(name + "_inventory", mcLoc("block/button_inventory"))
                        .texture("texture", side);
                itemModels().getBuilder(name).parent(inventory);
            }
            case PRESSURE_PLATE -> {
                pressurePlateBlock((PressurePlateBlock) block, side);
                itemModels().getBuilder(name).parent(models().getBuilder(name));
            }
            case DOOR -> {
                doorBlockWithRenderType((DoorBlock) block, name, side, top, "cutout");
                ResourceLocation itemTexture = definition.itemTextureFile()
                        .map(file -> texture(definition, file))
                        .orElse(side);
                itemModels().singleTexture(name, mcLoc("item/generated"), "layer0", itemTexture);
            }
            case TRAPDOOR -> {
                trapdoorBlockWithRenderType((TrapDoorBlock) block, name, side, true, "cutout");
                itemModels().getBuilder(name).parent(generatedBlockModel(name + "_trapdoor_bottom"));
            }
            case LEAVES -> {
                ModelFile model = models().cubeAll(name, side).renderType("cutout_mipped");
                simpleBlockWithItem(block, model);
            }
            case SAPLING -> {
                ModelFile model = models().cross(name, side).renderType("cutout");
                simpleBlock(block, model);
                itemModels().singleTexture(name, mcLoc("item/generated"), "layer0", side);
            }
            case WINDOW -> {
                ModelFile model = models().cubeAll(name, side).renderType("cutout");
                simpleBlockWithItem(block, model);
            }
            case BARS, BRACKET, BULB, LADDER, SCAFFOLD, WINDOW_PANE ->
                    throw new IllegalStateException("Metal model routed to the wood/stone provider: " + name);
        }
    }

    /**
     * Builds the full-block model used by every generated slab.
     * The geometry is shared; only the material-specific texture references differ.
     */
    private ResourceLocation sharedDoubleSlabModel(
            String name,
            ResourceLocation side,
            ResourceLocation bottom,
            ResourceLocation top
    ) {
        String modelName = name + "_double";
        if (top.equals(side) && bottom.equals(side)) {
            models().cubeAll(modelName, side);
        } else if (top.equals(bottom)) {
            models().cubeColumn(modelName, side, top);
        } else {
            models().cubeBottomTop(modelName, side, bottom, top);
        }
        return modLoc("block/" + modelName);
    }

    private ModelFile generatedBlockModel(String modelName) {
        return new ModelFile.UncheckedModelFile(modLoc("block/" + modelName));
    }


    private static boolean requiresExistingBaseTexture(StructureBlockDefinition definition) {
        if (!(definition.material() instanceof StoneMaterial stone)) {
            return false;
        }
        if (definition.basePart().isEmpty()) {
            return false;
        }
        return stone.hasExistingPart(definition.basePart().get());
    }

    private Optional<ExistingTextures> inheritedExistingTextures(StructureBlockDefinition definition) {
        if (!(definition.material() instanceof StoneMaterial stone)) {
            return Optional.empty();
        }
        if (definition.basePart().isEmpty()) {
            return Optional.empty();
        }

        var basePart = definition.basePart().get();
        if (!stone.hasExistingPart(basePart)) {
            return Optional.empty();
        }

        ResourceLocation blockId = stone.existingPart(basePart);
        try {
            ResourceLocation modelId = blockModel(blockId);
            if (modelId == null) {
                return Optional.empty();
            }

            Map<String, String> textures = new HashMap<>();
            collectModelTextures(modelId, textures, new HashSet<>());

            ResourceLocation all = resolvedTexture(textures, "all").orElse(null);
            ResourceLocation side = resolvedTexture(textures, "side")
                    .or(() -> resolvedTexture(textures, "wall"))
                    .or(() -> resolvedTexture(textures, "texture"))
                    .or(() -> Optional.ofNullable(all))
                    .or(() -> resolvedTexture(textures, "particle"))
                    .orElse(null);
            if (side == null) {
                return Optional.empty();
            }

            ResourceLocation top = resolvedTexture(textures, "top")
                    .or(() -> resolvedTexture(textures, "end"))
                    .orElse(side);
            ResourceLocation bottom = resolvedTexture(textures, "bottom").orElse(top);
            return Optional.of(new ExistingTextures(side, top, bottom));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private ResourceLocation blockModel(ResourceLocation blockId) throws Exception {
        try (InputStream input = openClientResource(blockId, "blockstates")) {
            if (input == null) {
                return null;
            }
            var reader = new InputStreamReader(input, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonElement model = null;

            JsonObject variants = root.has("variants") && root.get("variants").isJsonObject()
                    ? root.getAsJsonObject("variants")
                    : null;
            if (variants != null) {
                if (variants.has("")) {
                    model = variants.get("");
                } else if (!variants.entrySet().isEmpty()) {
                    model = variants.entrySet().iterator().next().getValue();
                }
            }

            if (model == null && root.has("multipart") && root.get("multipart").isJsonArray()) {
                for (JsonElement part : root.getAsJsonArray("multipart")) {
                    if (part.isJsonObject() && part.getAsJsonObject().has("apply")) {
                        model = part.getAsJsonObject().get("apply");
                        break;
                    }
                }
            }

            if (model == null) {
                return null;
            }
            if (model.isJsonArray()) {
                if (model.getAsJsonArray().isEmpty()) {
                    return null;
                }
                model = model.getAsJsonArray().get(0);
            }
            if (model.isJsonObject() && model.getAsJsonObject().has("model")) {
                return ResourceLocation.tryParse(model.getAsJsonObject().get("model").getAsString());
            }
            return null;
        }
    }

    private void collectModelTextures(
            ResourceLocation modelId,
            Map<String, String> textures,
            Set<ResourceLocation> visited
    ) throws Exception {
        if (!visited.add(modelId)) {
            return;
        }

        try (InputStream input = openClientResource(modelId, "models")) {
            if (input == null) {
                throw new IllegalStateException("Missing existing model resource " + modelId);
            }
            var reader = new InputStreamReader(input, StandardCharsets.UTF_8);
            JsonObject model = JsonParser.parseReader(reader).getAsJsonObject();
            if (model.has("parent")) {
                ResourceLocation parent = ResourceLocation.tryParse(model.get("parent").getAsString());
                if (parent != null) {
                    try {
                        collectModelTextures(parent, textures, visited);
                    } catch (Exception ignored) {
                        // Vanilla geometry-only parents may not contribute texture values.
                    }
                }
            }
            if (model.has("textures") && model.get("textures").isJsonObject()) {
                for (var entry : model.getAsJsonObject("textures").entrySet()) {
                    if (entry.getValue().isJsonPrimitive()) {
                        textures.put(entry.getKey(), entry.getValue().getAsString());
                    }
                }
            }
        }
    }


    /**
     * ExistingFileHelper is not guaranteed to expose dependency assets as readable Resource objects
     * in every datagen setup. Dependency jars are on the runtime classpath, however, so resolve the
     * real asset there first and keep ExistingFileHelper as a compatibility fallback.
     */
    private InputStream openClientResource(ResourceLocation id, String folder) {
        String path = "assets/" + id.getNamespace() + "/" + folder + "/" + id.getPath() + ".json";
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        InputStream stream = contextLoader == null ? null : contextLoader.getResourceAsStream(path);
        if (stream == null) {
            stream = StructureMaterialBlockStateProvider.class.getClassLoader().getResourceAsStream(path);
        }
        if (stream != null) {
            return stream;
        }

        try {
            Resource resource = existingFileHelper.getResource(id, PackType.CLIENT_RESOURCES, ".json", folder);
            return resource.open();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Optional<ResourceLocation> resolvedTexture(Map<String, String> textures, String key) {
        String value = textures.get(key);
        Set<String> visited = new HashSet<>();
        while (value != null && value.startsWith("#")) {
            String next = value.substring(1);
            if (!visited.add(next)) {
                return Optional.empty();
            }
            value = textures.get(next);
        }
        return Optional.ofNullable(value).map(ResourceLocation::tryParse);
    }

    private record ExistingTextures(ResourceLocation side, ResourceLocation top, ResourceLocation bottom) {
    }

    /**
     * The dependency asset was already resolved from the existing block/model JSON above.
     * Some NeoForge datagen setups do not index dependency textures in ExistingFileHelper,
     * even though the texture is present on the runtime classpath. Mark only these verified
     * inherited texture locations as known before ModelBuilder validates them.
     */
    private void trackInheritedTextures(ExistingTextures textures) {
        existingFileHelper.trackGenerated(textures.side(), ModelProvider.TEXTURE);
        existingFileHelper.trackGenerated(textures.top(), ModelProvider.TEXTURE);
        existingFileHelper.trackGenerated(textures.bottom(), ModelProvider.TEXTURE);
    }

    private ResourceLocation texture(StructureBlockDefinition definition, String fileName) {
        ResourceLocation texture = StructureSetResolver.generatedTexture(definition.material(), fileName);
        existingFileHelper.trackGenerated(texture, ModelProvider.TEXTURE);
        return texture;
    }

}