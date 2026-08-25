package net.mads.industron.data;

import net.mads.industron.Industron;
import net.mads.industron.block.DirectionalSimpleBlock;
import net.mads.industron.block.SimpleBlockDefinition;
import net.mads.industron.block.SimpleBlockVariant;
import net.mads.industron.block.SimpleBlocks;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.IndustrialMaterials;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.MaterialTextures;
import net.mads.industron.registry.BlockRegistry;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.loaders.CompositeModelBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.List;
import java.util.Optional;

public class MaterialBlockStateProvider extends BlockStateProvider {

    private final ExistingFileHelper existingFileHelper;

    public MaterialBlockStateProvider(
            PackOutput output,
            ExistingFileHelper existingFileHelper
    ) {
        super(
                output,
                Industron.MOD_ID,
                existingFileHelper
        );

        this.existingFileHelper = existingFileHelper;
    }

    @Override
    protected void registerStatesAndModels() {
        registerSimpleBlocks();
        registerMaterialBlocks();
    }

    private void registerSimpleBlocks() {
        for (SimpleBlockDefinition definition : SimpleBlocks.ALL) {
            Block baseBlock = BlockRegistry
                    .getSimpleBlock(definition.id())
                    .get();

            ResourceLocation texture =
                    resolveSimpleBlockTexture(definition);

            ModelFile baseModel = definition.hasFaceTextures()
                    ? createTintedCustomCubeModel(
                            definition.id(),
                            resolveSimpleBlockFaceTextures(definition)
                    )
                    : createTintedCubeModel(
                            definition.id(),
                            texture
                    );

            if (definition.hasFaceTextures()) {
                registerDirectionalSimpleBlock(
                        baseBlock,
                        baseModel
                );
            } else {
                simpleBlockWithItem(
                        baseBlock,
                        baseModel
                );
            }

            for (SimpleBlockVariant variant
                    : definition.variants()) {

                registerSimpleBlockVariant(
                        definition,
                        variant,
                        texture
                );
            }
        }
    }

    private void registerDirectionalSimpleBlock(
            Block block,
            ModelFile model
    ) {
        getVariantBuilder(block)
                .forAllStates(state -> {
                    Direction facing = state.getValue(
                            DirectionalSimpleBlock.FACING
                    );

                    int rotationY = switch (facing) {
                        case NORTH -> 0;
                        case EAST -> 90;
                        case SOUTH -> 180;
                        case WEST -> 270;
                        default -> throw new IllegalStateException(
                                "Directional simple block has non-horizontal facing: "
                                        + facing
                        );
                    };

                    return ConfiguredModel.builder()
                            .modelFile(model)
                            .rotationY(rotationY)
                            .build();
                });

        simpleBlockItem(block, model);
    }

    private void registerSimpleBlockVariant(
            SimpleBlockDefinition definition,
            SimpleBlockVariant variant,
            ResourceLocation texture
    ) {
        Block block = BlockRegistry
                .getSimpleBlockVariant(
                        definition.id(),
                        variant
                )
                .get();

        String id = definition.variantId(variant);

        switch (variant) {
            case SLAB -> registerSlab(
                    id,
                    (SlabBlock) block,
                    texture
            );

            case STAIR -> registerStairs(
                    id,
                    (StairBlock) block,
                    texture
            );

            case WALL -> registerWall(
                    id,
                    (WallBlock) block,
                    texture
            );

            case FENCE -> registerFence(
                    id,
                    (FenceBlock) block,
                    texture
            );

            case FENCE_GATE -> registerFenceGate(
                    id,
                    (FenceGateBlock) block,
                    texture
            );

            case BUTTON -> registerButton(
                    id,
                    (ButtonBlock) block,
                    texture
            );

            case PRESSURE_PLATE -> registerPressurePlate(
                    id,
                    (PressurePlateBlock) block,
                    texture
            );
        }
    }


    private void registerSlab(
            String id,
            SlabBlock block,
            ResourceLocation texture
    ) {
        ModelFile bottomModel = createTintedShapeModel(
                id,
                texture,
                box(0, 0, 0, 16, 8, 16)
        );

        ModelFile topModel = createTintedShapeModel(
                id + "_top",
                texture,
                box(0, 8, 0, 16, 16, 16)
        );

        ModelFile doubleModel = createTintedCubeModel(
                id + "_double",
                texture
        );

        slabBlock(
                block,
                bottomModel,
                topModel,
                doubleModel
        );

        simpleBlockItem(
                block,
                bottomModel
        );
    }


    private void registerStairs(
            String id,
            StairBlock block,
            ResourceLocation texture
    ) {
        ModelFile stairs = createTintedShapeModel(
                id,
                texture,
                box(0, 0, 0, 16, 8, 16),
                box(0, 8, 8, 16, 16, 16)
        );

        ModelFile stairsInner = createTintedShapeModel(
                id + "_inner",
                texture,
                box(0, 0, 0, 16, 8, 16),
                box(0, 8, 8, 16, 16, 16),
                box(8, 8, 0, 16, 16, 8)
        );

        ModelFile stairsOuter = createTintedShapeModel(
                id + "_outer",
                texture,
                box(0, 0, 0, 16, 8, 16),
                box(8, 8, 8, 16, 16, 16)
        );

        stairsBlock(
                block,
                stairs,
                stairsInner,
                stairsOuter
        );

        simpleBlockItem(
                block,
                stairs
        );
    }



    private void registerWall(
            String id,
            WallBlock block,
            ResourceLocation texture
    ) {
        ModelFile post = createTintedShapeModel(
                id + "_post",
                texture,
                box(4, 0, 4, 12, 16, 12)
        );

        ModelFile side = createTintedShapeModel(
                id + "_side",
                texture,
                box(5, 0, 0, 11, 14, 8)
        );

        ModelFile sideTall = createTintedShapeModel(
                id + "_side_tall",
                texture,
                box(5, 0, 0, 11, 16, 8)
        );

        getMultipartBuilder(block)
                .part()
                .modelFile(post)
                .addModel()
                .condition(WallBlock.UP, true)
                .end()

                .part()
                .modelFile(side)
                .uvLock(true)
                .addModel()
                .condition(
                        WallBlock.NORTH_WALL,
                        net.minecraft.world.level.block.state.properties.WallSide.LOW
                )
                .end()

                .part()
                .modelFile(side)
                .rotationY(90)
                .uvLock(true)
                .addModel()
                .condition(
                        WallBlock.EAST_WALL,
                        net.minecraft.world.level.block.state.properties.WallSide.LOW
                )
                .end()

                .part()
                .modelFile(side)
                .rotationY(180)
                .uvLock(true)
                .addModel()
                .condition(
                        WallBlock.SOUTH_WALL,
                        net.minecraft.world.level.block.state.properties.WallSide.LOW
                )
                .end()

                .part()
                .modelFile(side)
                .rotationY(270)
                .uvLock(true)
                .addModel()
                .condition(
                        WallBlock.WEST_WALL,
                        net.minecraft.world.level.block.state.properties.WallSide.LOW
                )
                .end()

                .part()
                .modelFile(sideTall)
                .uvLock(true)
                .addModel()
                .condition(
                        WallBlock.NORTH_WALL,
                        net.minecraft.world.level.block.state.properties.WallSide.TALL
                )
                .end()

                .part()
                .modelFile(sideTall)
                .rotationY(90)
                .uvLock(true)
                .addModel()
                .condition(
                        WallBlock.EAST_WALL,
                        net.minecraft.world.level.block.state.properties.WallSide.TALL
                )
                .end()

                .part()
                .modelFile(sideTall)
                .rotationY(180)
                .uvLock(true)
                .addModel()
                .condition(
                        WallBlock.SOUTH_WALL,
                        net.minecraft.world.level.block.state.properties.WallSide.TALL
                )
                .end()

                .part()
                .modelFile(sideTall)
                .rotationY(270)
                .uvLock(true)
                .addModel()
                .condition(
                        WallBlock.WEST_WALL,
                        net.minecraft.world.level.block.state.properties.WallSide.TALL
                )
                .end();

        ModelFile inventoryModel = createTintedShapeModel(
                id + "_inventory",
                texture,
                box(4, 0, 4, 12, 16, 12),
                box(5, 0, 0, 11, 14, 4),
                box(5, 0, 12, 11, 14, 16)
        );

        simpleBlockItem(
                block,
                inventoryModel
        );
    }



    private void registerFence(
            String id,
            FenceBlock block,
            ResourceLocation texture
    ) {
        ModelFile post = createTintedShapeModel(
                id + "_post",
                texture,
                box(6, 0, 6, 10, 16, 10)
        );

        ModelFile side = createTintedShapeModel(
                id + "_side",
                texture,
                box(7, 6, 0, 9, 9, 8),
                box(7, 12, 0, 9, 15, 8)
        );

        getMultipartBuilder(block)
                .part()
                .modelFile(post)
                .addModel()
                .end()

                .part()
                .modelFile(side)
                .uvLock(true)
                .addModel()
                .condition(FenceBlock.NORTH, true)
                .end()

                .part()
                .modelFile(side)
                .rotationY(90)
                .uvLock(true)
                .addModel()
                .condition(FenceBlock.EAST, true)
                .end()

                .part()
                .modelFile(side)
                .rotationY(180)
                .uvLock(true)
                .addModel()
                .condition(FenceBlock.SOUTH, true)
                .end()

                .part()
                .modelFile(side)
                .rotationY(270)
                .uvLock(true)
                .addModel()
                .condition(FenceBlock.WEST, true)
                .end();

        ModelFile inventoryModel = createTintedShapeModel(
                id + "_inventory",
                texture,
                box(6, 0, 6, 10, 16, 10),
                box(7, 6, 0, 9, 9, 6),
                box(7, 12, 0, 9, 15, 6),
                box(7, 6, 10, 9, 9, 16),
                box(7, 12, 10, 9, 15, 16)
        );

        simpleBlockItem(
                block,
                inventoryModel
        );
    }


    private void registerFenceGate(
            String id,
            FenceGateBlock block,
            ResourceLocation texture
    ) {
        ModelFile gate = createTintedShapeModel(
                id,
                texture,
                box(0, 5, 7, 2, 16, 9),
                box(14, 5, 7, 16, 16, 9),
                box(2, 6, 7, 14, 9, 9),
                box(2, 12, 7, 14, 15, 9),
                box(3, 7, 7, 5, 13, 9),
                box(11, 7, 7, 13, 13, 9)
        );

        ModelFile gateOpen = createTintedShapeModel(
                id + "_open",
                texture,
                box(0, 5, 7, 2, 16, 9),
                box(14, 5, 7, 16, 16, 9),
                box(0, 6, 9, 2, 9, 14),
                box(0, 12, 9, 2, 15, 14),
                box(14, 6, 9, 16, 9, 14),
                box(14, 12, 9, 16, 15, 14)
        );

        ModelFile gateWall = createTintedShapeModel(
                id + "_wall",
                texture,
                box(0, 2, 7, 2, 13, 9),
                box(14, 2, 7, 16, 13, 9),
                box(2, 3, 7, 14, 6, 9),
                box(2, 9, 7, 14, 12, 9),
                box(3, 4, 7, 5, 10, 9),
                box(11, 4, 7, 13, 10, 9)
        );

        ModelFile gateWallOpen = createTintedShapeModel(
                id + "_wall_open",
                texture,
                box(0, 2, 7, 2, 13, 9),
                box(14, 2, 7, 16, 13, 9),
                box(0, 3, 9, 2, 6, 14),
                box(0, 9, 9, 2, 12, 14),
                box(14, 3, 9, 16, 6, 14),
                box(14, 9, 9, 16, 12, 14)
        );

        fenceGateBlock(
                block,
                gate,
                gateOpen,
                gateWall,
                gateWallOpen
        );

        simpleBlockItem(
                block,
                gate
        );
    }


    private void registerButton(
            String id,
            ButtonBlock block,
            ResourceLocation texture
    ) {
        ModelFile buttonModel = createTintedShapeModel(
                id,
                texture,
                box(5, 0, 6, 11, 2, 10)
        );

        ModelFile pressedModel = createTintedShapeModel(
                id + "_pressed",
                texture,
                box(5, 0, 6, 11, 1, 10)
        );

        buttonBlock(
                block,
                buttonModel,
                pressedModel
        );

        ModelFile inventoryModel = createTintedShapeModel(
                id + "_inventory",
                texture,
                box(5, 6, 6, 11, 10, 10)
        );

        simpleBlockItem(
                block,
                inventoryModel
        );
    }


    private void registerPressurePlate(
            String id,
            PressurePlateBlock block,
            ResourceLocation texture
    ) {
        ModelFile upModel = createTintedShapeModel(
                id,
                texture,
                box(1, 0, 1, 15, 1, 15)
        );

        ModelFile downModel = createTintedShapeModel(
                id + "_down",
                texture,
                box(1, 0, 1, 15, 0.5F, 15)
        );

        pressurePlateBlock(
                block,
                upModel,
                downModel
        );

        simpleBlockItem(
                block,
                upModel
        );
    }

    /**
     * Lager en vanlig cube_all-modell med tintindex 0.
     *
     * Uten tintindex blir ikke ClientColorHandlers brukt.
     */
    private BlockModelBuilder createTintedCubeModel(
            String id,
            ResourceLocation texture
    ) {
        BlockModelBuilder model = models()
                .getBuilder(id)
                .parent(
                        new ModelFile.UncheckedModelFile(
                                ResourceLocation.withDefaultNamespace(
                                        "block/block"
                                )
                        )
                )
                .texture(
                        "all",
                        texture
                )
                .texture(
                        "particle",
                        texture
                );

        model.element()
                .from(
                        0,
                        0,
                        0
                )
                .to(
                        16,
                        16,
                        16
                )
                .allFaces(
                        (direction, face) ->
                                face.texture("#all")
                                        .cullface(direction)
                                        .tintindex(0)
                );

        return model;
    }

    private BlockModelBuilder createTintedCustomCubeModel(
            String id,
            ResolvedFaceTextures textures
    ) {
        BlockModelBuilder model = models()
                .getBuilder(id)
                .parent(
                        new ModelFile.UncheckedModelFile(
                                ResourceLocation.withDefaultNamespace(
                                        "block/block"
                                )
                        )
                )
                .texture("front", textures.front())
                .texture("right", textures.right())
                .texture("back", textures.back())
                .texture("left", textures.left())
                .texture("up", textures.top())
                .texture("down", textures.bottom())
                .texture("particle", textures.front());

        model.element()
                .from(0, 0, 0)
                .to(16, 16, 16)
                .face(net.minecraft.core.Direction.NORTH)
                .texture("#front")
                .cullface(net.minecraft.core.Direction.NORTH)
                .tintindex(0)
                .end()
                .face(net.minecraft.core.Direction.EAST)
                .texture("#right")
                .cullface(net.minecraft.core.Direction.EAST)
                .tintindex(0)
                .end()
                .face(net.minecraft.core.Direction.SOUTH)
                .texture("#back")
                .cullface(net.minecraft.core.Direction.SOUTH)
                .tintindex(0)
                .end()
                .face(net.minecraft.core.Direction.WEST)
                .texture("#left")
                .cullface(net.minecraft.core.Direction.WEST)
                .tintindex(0)
                .end()
                .face(net.minecraft.core.Direction.UP)
                .texture("#up")
                .cullface(net.minecraft.core.Direction.UP)
                .tintindex(0)
                .end()
                .face(net.minecraft.core.Direction.DOWN)
                .texture("#down")
                .cullface(net.minecraft.core.Direction.DOWN)
                .tintindex(0)
                .end();

        return model;
    }


    private BlockModelBuilder createTintedShapeModel(
            String id,
            ResourceLocation texture,
            ModelBox... boxes
    ) {
        BlockModelBuilder model = models()
                .getBuilder(id)
                .parent(
                        new ModelFile.UncheckedModelFile(
                                ResourceLocation.withDefaultNamespace(
                                        "block/block"
                                )
                        )
                )
                .texture(
                        "all",
                        texture
                )
                .texture(
                        "particle",
                        texture
                );

        for (ModelBox box : boxes) {
            model.element()
                    .from(
                            box.fromX(),
                            box.fromY(),
                            box.fromZ()
                    )
                    .to(
                            box.toX(),
                            box.toY(),
                            box.toZ()
                    )
                    .allFaces(
                            (direction, face) ->
                                    face.texture("#all")
                                            .tintindex(0)
                    );
        }

        return model;
    }

    private static ModelBox box(
            float fromX,
            float fromY,
            float fromZ,
            float toX,
            float toY,
            float toZ
    ) {
        return new ModelBox(
                fromX,
                fromY,
                fromZ,
                toX,
                toY,
                toZ
        );
    }

    private record ModelBox(
            float fromX,
            float fromY,
            float fromZ,
            float toX,
            float toY,
            float toZ
    ) {
    }

    private record ResolvedFaceTextures(
            ResourceLocation front,
            ResourceLocation right,
            ResourceLocation back,
            ResourceLocation left,
            ResourceLocation top,
            ResourceLocation bottom
    ) {
    }

    private ResourceLocation resolveSimpleBlockTexture(
            SimpleBlockDefinition definition
    ) {
        return resolveSimpleBlockTexture(
                definition.id(),
                definition.texture()
        );
    }

    private ResolvedFaceTextures resolveSimpleBlockFaceTextures(
            SimpleBlockDefinition definition
    ) {
        SimpleBlockDefinition.FaceTextures textures =
                definition.faceTextures();

        return new ResolvedFaceTextures(
                resolveSimpleBlockTexture(definition.id(), textures.front()),
                resolveSimpleBlockTexture(definition.id(), textures.right()),
                resolveSimpleBlockTexture(definition.id(), textures.back()),
                resolveSimpleBlockTexture(definition.id(), textures.left()),
                resolveSimpleBlockTexture(definition.id(), textures.top()),
                resolveSimpleBlockTexture(definition.id(), textures.bottom())
        );
    }

    private ResourceLocation resolveSimpleBlockTexture(
            String blockId,
            String texturePath
    ) {
        if (texturePath.equals(blockId)) {
            return findSimpleBlockTexture(
                    blockId
            );
        }

        if (texturePath.contains(":")) {
            ResourceLocation texture =
                    ResourceLocation.tryParse(texturePath);

            if (texture == null) {
                throw new IllegalStateException(
                        "Invalid texture for simple block '"
                                + blockId
                                + "': "
                                + texturePath
                );
            }

            return texture;
        }

        return ResourceLocation.fromNamespaceAndPath(
                Industron.MOD_ID,
                texturePath
        );
    }

    private ResourceLocation findSimpleBlockTexture(
            String blockId
    ) {
        for (String folder : SIMPLE_BLOCK_TEXTURE_FOLDERS) {
            ResourceLocation texture =
                    ResourceLocation.fromNamespaceAndPath(
                            Industron.MOD_ID,
                            folder + "/" + blockId
                    );

            boolean exists = existingFileHelper.exists(
                    texture,
                    PackType.CLIENT_RESOURCES,
                    ".png",
                    "textures"
            );

            if (exists) {
                return texture;
            }
        }

        throw new IllegalStateException(
                "Could not find texture for simple block '"
                        + blockId
                        + "'. Expected a file named '"
                        + blockId
                        + ".png' in one of these folders: "
                        + SIMPLE_BLOCK_TEXTURE_FOLDERS
        );
    }

    private void registerMaterialBlocks() {
        for (IndustrialMaterial material : IndustrialMaterials.ALL) {
            registerMaterialStoneBlocks(material);
            registerMaterialPartBlocks(material);
        }
    }

    private void registerMaterialStoneBlocks(
            IndustrialMaterial material
    ) {
        for (var stoneSource : material.stoneSources()) {
            if (stoneSource.isExisting()) {
                continue;
            }

            Block block = BlockRegistry
                    .getMaterialStoneBlock(
                            material,
                            stoneSource.id()
                    )
                    .get();

            simpleBlockWithItem(
                    block,
                    models().cubeAll(
                            stoneSource.registryName(material),
                            stoneSource.texture().orElseThrow()
                    )
            );
        }
    }

    private void registerMaterialPartBlocks(
            IndustrialMaterial material
    ) {
        for (MaterialPart part : material.parts()) {
            if (material.hasExistingPart(part)) {
                continue;
            }

            if (!part.isBlock()) {
                continue;
            }

            Block block = BlockRegistry
                    .getMaterialBlock(
                            material,
                            part
                    )
                    .get();

            if (material.hasCustomPartTexture(part)) {
                simpleBlockWithItem(
                        block,
                        models().cubeAll(
                                part.registryName(material),
                                material.customPartTexture(part)
                        )
                );

                continue;
            }

            boolean orePart = switch (part) {
                case ORE, SMALL_ORE,
                        DEEPSLATE_ORE, SMALL_DEEPSLATE_ORE,
                        DIORITE_ORE, SMALL_DIORITE_ORE,
                        ANDESITE_ORE, SMALL_ANDESITE_ORE,
                        GRANITE_ORE, SMALL_GRANITE_ORE,
                        TUFF_ORE, SMALL_TUFF_ORE,
                        NETHERRACK_ORE, SMALL_NETHERRACK_ORE,
                        BLACKSTONE_ORE, SMALL_BLACKSTONE_ORE,
                        END_STONE_ORE, SMALL_END_STONE_ORE -> true;
                default -> false;
            };

            ModelFile materialBlockModel;
            if (orePart) {
                if (MaterialTextures.blockTexture(material, part).isEmpty()) {
                    continue;
                }
                materialBlockModel = oreBlockModel(material, part);
            } else {
                Optional<ResourceLocation> baseTexture = MaterialTextures.blockTexture(material, part);
                if (baseTexture.isEmpty()) {
                    // The block remains registered even when no visual variant exists yet.
                    continue;
                }
                materialBlockModel = materialBlockModel(material, part, baseTexture.get());
            }

            simpleBlockWithItem(
                    block,
                    materialBlockModel
            );
        }
    }


    private BlockModelBuilder materialBlockModel(
            IndustrialMaterial material,
            MaterialPart part,
            ResourceLocation baseTexture
    ) {
        String name = part.registryName(material);
        Optional<ResourceLocation> secondaryTexture = MaterialTextures.blockSecondaryTexture(material, part);

        BlockModelBuilder model = models()
                .getBuilder(name)
                .parent(new ModelFile.UncheckedModelFile(
                        ResourceLocation.withDefaultNamespace("block/block")
                ))
                .texture("base", baseTexture)
                .texture("particle", baseTexture)
                .renderType(secondaryTexture.isPresent() || part == MaterialPart.FRAME
                        ? "minecraft:cutout"
                        : "minecraft:solid");

        fullCube(model, "#base", 0);

        secondaryTexture.ifPresent(texture -> {
            model.texture("secondary", texture);
            fullCube(model, "#secondary", 1);
        });

        return model;
    }

    private BlockModelBuilder oreBlockModel(
            IndustrialMaterial material,
            MaterialPart part
    ) {
        String name = part.registryName(material);

        ResourceLocation oreTexture = MaterialTextures
                .blockTexture(material, part)
                .orElseThrow();

        ResourceLocation oreOverlayTexture = MaterialTextures
                .blockOverlayTexture(material, part)
                .orElse(oreTexture);

        ResourceLocation baseTexture =
                baseStoneTexture(part);

        BlockModelBuilder baseStone = models()
                .nested()
                .parent(
                        new ModelFile.UncheckedModelFile(
                                ResourceLocation.withDefaultNamespace(
                                        "block/cube_all"
                                )
                        )
                )
                .texture(
                        "all",
                        baseTexture
                )
                .renderType(
                        "minecraft:solid"
                );

        BlockModelBuilder oreTextureModel = models()
                .nested()
                .parent(
                        new ModelFile.UncheckedModelFile(
                                ResourceLocation.withDefaultNamespace(
                                        "block/block"
                                )
                        )
                )
                .texture(
                        "layer0",
                        oreTexture
                )
                .texture(
                        "layer1",
                        oreOverlayTexture
                )
                .texture(
                        "particle",
                        oreTexture
                )
                .renderType(
                        "minecraft:cutout"
                );

        fullCube(
                oreTextureModel,
                "#layer0",
                0
        );

        fullCube(
                oreTextureModel,
                "#layer1",
                1
        );

        BlockModelBuilder model = models()
                .getBuilder(name)
                .parent(
                        new ModelFile.UncheckedModelFile(
                                ResourceLocation.withDefaultNamespace(
                                        "block/block"
                                )
                        )
                )
                .texture(
                        "particle",
                        baseTexture
                );

        model.customLoader(CompositeModelBuilder::begin)
                .child(
                        "base_stone",
                        baseStone
                )
                .child(
                        "ore_texture",
                        oreTextureModel
                )
                .itemRenderOrder(
                        "base_stone",
                        "ore_texture"
                );

        return model;
    }

    private static void fullCube(
            BlockModelBuilder model,
            String texture,
            int tintIndex
    ) {
        model.element()
                .from(
                        0,
                        0,
                        0
                )
                .to(
                        16,
                        16,
                        16
                )
                .allFaces(
                        (direction, face) ->
                                face.texture(texture)
                                        .cullface(direction)
                                        .tintindex(tintIndex)
                );
    }

    private static ResourceLocation baseStoneTexture(
            MaterialPart part
    ) {
        return switch (part) {
            case ORE, SMALL_ORE ->
                    ResourceLocation.withDefaultNamespace(
                            "block/stone"
                    );

            case DEEPSLATE_ORE, SMALL_DEEPSLATE_ORE ->
                    ResourceLocation.withDefaultNamespace(
                            "block/deepslate"
                    );

            case DIORITE_ORE, SMALL_DIORITE_ORE ->
                    ResourceLocation.withDefaultNamespace(
                            "block/diorite"
                    );

            case ANDESITE_ORE, SMALL_ANDESITE_ORE ->
                    ResourceLocation.withDefaultNamespace(
                            "block/andesite"
                    );

            case GRANITE_ORE, SMALL_GRANITE_ORE ->
                    ResourceLocation.withDefaultNamespace(
                            "block/granite"
                    );

            case TUFF_ORE, SMALL_TUFF_ORE ->
                    ResourceLocation.withDefaultNamespace(
                            "block/tuff"
                    );

            case NETHERRACK_ORE, SMALL_NETHERRACK_ORE ->
                    ResourceLocation.withDefaultNamespace(
                            "block/netherrack"
                    );

            case BLACKSTONE_ORE, SMALL_BLACKSTONE_ORE ->
                    ResourceLocation.withDefaultNamespace(
                            "block/blackstone"
                    );

            case END_STONE_ORE, SMALL_END_STONE_ORE ->
                    ResourceLocation.withDefaultNamespace(
                            "block/end_stone"
                    );

            default ->
                    throw new IllegalArgumentException(
                            "Expected an ore block part, got "
                                    + part
                    );
        };
    }

    private static final List<String>
            SIMPLE_BLOCK_TEXTURE_FOLDERS = List.of(
            "block/casings/casing",
            "block"
    );
}
