package net.mads.industron.data;

import net.mads.industron.Industron;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.structure.StructureBlockDefinition;
import net.mads.industron.material.structure.StructureMaterial;
import net.mads.industron.material.structure.StoneMaterial;
import net.mads.industron.material.structure.MetalMaterial;
import net.mads.industron.material.structure.StructureMaterialGenerator;
import net.mads.industron.material.structure.StructureMaterials;
import net.mads.industron.material.structure.StructureSetResolver;
import net.mads.industron.material.structure.WoodMaterial;
import net.mads.industron.registry.BlockRegistry;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.ModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.Optional;

/** Generates standard blockstates/models; specialized metal families use MetalStructureModelProvider. */
public final class StructureMaterialBlockStateProvider extends BlockStateProvider {
    private final ExistingFileHelper existingFileHelper;
    private final StoneTextureResolver stoneTextureResolver;

    public StructureMaterialBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Industron.MOD_ID, existingFileHelper);
        this.existingFileHelper = existingFileHelper;
        this.stoneTextureResolver = new StoneTextureResolver(existingFileHelper);
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
            case CUBE, FALLING -> {
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
            case BOOKSHELF -> registerBookshelf(definition, block, name, side);
            case BARREL -> registerBarrel(definition, block, name, side, top, bottom);
            case CHISELED_BOOKSHELF -> registerChiseledBookshelf(definition, block, name);
            case CHEST -> registerChest(definition, block, name);
            case LADDER -> registerWoodLadder(block, name, side);
            case SHAFT -> registerShaftBlock(name, block, side, top);
            case BARS, BRACKET, BULB, SCAFFOLD, WINDOW_PANE ->
                    throw new IllegalStateException("Metal model routed to the wood/stone provider: " + name);
        }
    }

    private void registerShaftBlock(
            String name,
            Block block,
            ResourceLocation side,
            ResourceLocation end
    ) {
        // The placed block is intentionally geometry-free. The visible shaft is the
        // <name>_rotating partial model used by the kinetic renderer/Flywheel visual.
        var staticModel = models()
                .getBuilder(name)
                .parent(new ModelFile.UncheckedModelFile(mcLoc("block/block")))
                .texture("particle", side);

        var rotatingModel = models()
                .getBuilder(name + "_rotating")
                .parent(new ModelFile.UncheckedModelFile(mcLoc("block/block")))
                .texture("side", side)
                .texture("end", end)
                .texture("particle", side);

        rotatingModel.element()
                .from(6, 0, 6)
                .to(10, 16, 10)
                .allFaces((direction, face) -> {
                    boolean cap = direction == Direction.UP || direction == Direction.DOWN;
                    face.texture(cap ? "#end" : "#side");
                    if (cap) {
                        face.uvs(6, 6, 10, 10);
                    } else {
                        face.uvs(6, 0, 10, 16);
                    }
                });

        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(staticModel)
                .build());

        itemModels().getBuilder(name).parent(rotatingModel);
    }

    private void registerBookshelf(
            StructureBlockDefinition definition,
            Block block,
            String name,
            ResourceLocation bookshelfSide
    ) {
        WoodMaterial wood = requireWood(definition);
        ResourceLocation planks = woodPartTexture(wood, MaterialPart.PLANKS);
        ModelFile model = models().cubeColumn(name, bookshelfSide, planks);
        simpleBlockWithItem(block, model);
    }

    private void registerBarrel(
            StructureBlockDefinition definition,
            Block block,
            String name,
            ResourceLocation side,
            ResourceLocation top,
            ResourceLocation bottom
    ) {
        ResourceLocation openTop = texture(definition, definition.requiredTexture("open_top"));
        ModelFile closed = models().cubeBottomTop(name, side, bottom, top);
        ModelFile open = models().cubeBottomTop(name + "_open", side, bottom, openTop);

        getVariantBuilder(block).forAllStates(state -> {
            Direction facing = state.getValue(BarrelBlock.FACING);
            boolean isOpen = state.getValue(BarrelBlock.OPEN);
            int rotationX;
            int rotationY;
            switch (facing) {
                case DOWN -> { rotationX = 180; rotationY = 0; }
                case UP -> { rotationX = 0; rotationY = 0; }
                case NORTH -> { rotationX = 90; rotationY = 0; }
                case SOUTH -> { rotationX = 90; rotationY = 180; }
                case WEST -> { rotationX = 90; rotationY = 270; }
                case EAST -> { rotationX = 90; rotationY = 90; }
                default -> throw new IllegalStateException("Unexpected barrel facing " + facing);
            }
            return ConfiguredModel.builder()
                    .modelFile(isOpen ? open : closed)
                    .rotationX(rotationX)
                    .rotationY(rotationY)
                    .build();
        });
        simpleBlockItem(block, closed);
    }

    private void registerChiseledBookshelf(
            StructureBlockDefinition definition,
            Block block,
            String name
    ) {
        ResourceLocation empty = texture(definition, definition.textureFile());
        ResourceLocation occupied = texture(definition, definition.requiredTexture("occupied"));
        ResourceLocation side = texture(definition, definition.requiredTexture("side"));
        ResourceLocation top = definition.topTextureFile()
                .map(file -> texture(definition, file))
                .orElseThrow(() -> new IllegalStateException("Missing chiseled bookshelf top texture for " + name));

        ModelFile body = models().withExistingParent(name, mcLoc("block/chiseled_bookshelf"))
                .texture("top", top)
                .texture("side", side)
                .texture("particle", top);

        String[] slotNames = {
                "top_left", "top_mid", "top_right",
                "bottom_left", "bottom_mid", "bottom_right"
        };
        ModelFile[] emptySlots = new ModelFile[6];
        ModelFile[] occupiedSlots = new ModelFile[6];
        for (int i = 0; i < slotNames.length; i++) {
            String slot = slotNames[i];
            emptySlots[i] = models()
                    .withExistingParent(name + "_empty_slot_" + slot, mcLoc("block/template_chiseled_bookshelf_slot_" + slot))
                    .texture("texture", empty);
            occupiedSlots[i] = models()
                    .withExistingParent(name + "_occupied_slot_" + slot, mcLoc("block/template_chiseled_bookshelf_slot_" + slot))
                    .texture("texture", occupied);
        }

        for (Direction facing : Direction.Plane.HORIZONTAL) {
            int rotationY = switch (facing) {
                case NORTH -> 0;
                case EAST -> 90;
                case SOUTH -> 180;
                case WEST -> 270;
                default -> throw new IllegalStateException("Unexpected bookshelf facing " + facing);
            };

            getMultipartBuilder(block)
                    .part().modelFile(body).rotationY(rotationY).uvLock(true).addModel()
                    .condition(HorizontalDirectionalBlock.FACING, facing).end();

            for (int i = 0; i < ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.size(); i++) {
                var property = ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.get(i);
                getMultipartBuilder(block)
                        .part().modelFile(emptySlots[i]).rotationY(rotationY).addModel()
                        .condition(HorizontalDirectionalBlock.FACING, facing)
                        .condition(property, false).end();
                getMultipartBuilder(block)
                        .part().modelFile(occupiedSlots[i]).rotationY(rotationY).addModel()
                        .condition(HorizontalDirectionalBlock.FACING, facing)
                        .condition(property, true).end();
            }
        }

        ModelFile inventory = models().withExistingParent(name + "_inventory", mcLoc("block/chiseled_bookshelf_inventory"))
                .texture("top", top)
                .texture("side", side)
                .texture("front", empty)
                .texture("particle", top);
        simpleBlockItem(block, inventory);
    }

    private void registerChest(
            StructureBlockDefinition definition,
            Block block,
            String name
    ) {
        WoodMaterial wood = requireWood(definition);
        ResourceLocation planks = woodPartTexture(wood, MaterialPart.PLANKS);
        ModelFile model = models().withExistingParent(name, mcLoc("block/chest"))
                .texture("particle", planks);
        simpleBlock(block, model);
        // Inherit vanilla Chest's item-display transforms while the custom BEWLR supplies this
        // wood material's generated chest texture. Using builtin/entity directly renders at a
        // different inventory scale/pose than the normal minecraft:chest item.
        itemModels().getBuilder(name)
                .parent(new ModelFile.UncheckedModelFile(mcLoc("item/chest")));
    }

    private void registerWoodLadder(Block block, String name, ResourceLocation texture) {
        ModelFile model = models().withExistingParent(name, mcLoc("block/ladder"))
                .texture("particle", texture)
                .texture("texture", texture)
                .renderType("cutout");
        getVariantBuilder(block).forAllStates(state -> {
            Direction facing = state.getValue(LadderBlock.FACING);
            int rotationY = switch (facing) {
                case NORTH -> 0;
                case EAST -> 90;
                case SOUTH -> 180;
                case WEST -> 270;
                default -> throw new IllegalStateException("Unexpected ladder facing " + facing);
            };
            return ConfiguredModel.builder().modelFile(model).rotationY(rotationY).build();
        });
        itemModels().singleTexture(name, mcLoc("item/generated"), "layer0", texture);
    }

    private ResourceLocation woodPartTexture(WoodMaterial wood, MaterialPart part) {
        if (wood.hasExistingPart(part)) {
            return stoneTextureResolver.existingTextures(wood.existingPart(part))
                    .map(textures -> textures.side())
                    .orElseGet(() -> ResourceLocation.fromNamespaceAndPath(
                            wood.existingPart(part).getNamespace(),
                            "block/" + wood.existingPart(part).getPath()
                    ));
        }
        return StructureMaterialGenerator.blockDefinitions(wood).stream()
                .filter(definition -> definition.part().orElse(null) == part)
                .findFirst()
                .map(definition -> StructureSetResolver.generatedTexture(wood, definition.textureFile()))
                .orElseThrow(() -> new IllegalStateException(
                        "Wood " + wood.id() + " has no texture-resolvable form " + part
                ));
    }

    private static WoodMaterial requireWood(StructureBlockDefinition definition) {
        if (definition.material() instanceof WoodMaterial wood) return wood;
        throw new IllegalStateException(definition.registryName() + " is not a wood definition");
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
        if (!(definition.material() instanceof StoneMaterial stone) || definition.basePart().isEmpty()) {
            return Optional.empty();
        }
        return stoneTextureResolver.existingTextures(stone, definition.basePart().get())
                .map(textures -> new ExistingTextures(textures.side(), textures.top(), textures.bottom()));
    }

    public record ExistingTextures(ResourceLocation side, ResourceLocation top, ResourceLocation bottom) {
    }

    public ExistingTextures existingBrickTextures(ResourceLocation blockId) {
        StoneTextureResolver.ExistingTextures textures = stoneTextureResolver.existingTextures(blockId)
                .orElseThrow(() -> new IllegalStateException("Cannot resolve existing brick textures for " + blockId));
        return new ExistingTextures(textures.side(), textures.top(), textures.bottom());
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
