package net.mads.industron.data;

import net.mads.industron.Industron;
import net.mads.industron.material.structure.StructureBlockDefinition;
import net.mads.industron.material.structure.StructureMaterial;
import net.mads.industron.material.structure.GemMaterial;
import net.mads.industron.material.structure.MetalMaterial;
import net.mads.industron.material.structure.StructureMaterialGenerator;
import net.mads.industron.material.structure.StructureMaterialPart;
import net.mads.industron.material.structure.StructureMaterials;
import net.mads.industron.material.structure.StructureSetResolver;
import net.mads.industron.registry.BlockRegistry;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
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
        ResourceLocation side = texture(definition, definition.textureFile());
        ResourceLocation top = definition.topTextureFile().map(file -> texture(definition, file)).orElse(side);
        ResourceLocation bottom = definition.bottomTextureFile().map(file -> texture(definition, file)).orElse(side);
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
                ResourceLocation doubleModel = definition.material() instanceof GemMaterial
                        ? gemDoubleSlabModel(name, side, bottom, top)
                        : baseModel(definition);
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

    private ResourceLocation gemDoubleSlabModel(
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

    private ResourceLocation texture(StructureBlockDefinition definition, String fileName) {
        ResourceLocation texture = StructureSetResolver.generatedTexture(definition.material(), fileName);
        existingFileHelper.trackGenerated(texture, ModelProvider.TEXTURE);
        return texture;
    }

    private ResourceLocation baseModel(StructureBlockDefinition definition) {
        if (definition.basePart().isPresent()) {
            StructureMaterialPart part = definition.basePart().get();
            if (definition.material().hasExistingPart(part)) {
                ResourceLocation id = definition.material().existingPart(part);
                return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "block/" + id.getPath());
            }
        }

        String baseId = definition.baseRegistryName()
                .orElseThrow(() -> new IllegalStateException("No base model for " + definition.registryName()));
        return modLoc("block/" + baseId);
    }
}
