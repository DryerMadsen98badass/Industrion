package net.mads.industron.data;

import net.mads.industron.Industron;
import net.mads.industron.block.DirectionalSimpleBlock;
import net.mads.industron.block.SimpleBlockDefinition;
import net.mads.industron.block.SimpleBlockVariant;
import net.mads.industron.block.SimpleBlocks;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.defenitions.IndustrialMaterials;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.MaterialOreHost;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.loaders.CompositeModelBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MaterialBlockStateProvider extends BlockStateProvider {

    private final ExistingFileHelper existingFileHelper;

    private final Map<String, ResourceLocation> tintedShapeTemplates = new LinkedHashMap<>();
    private ResourceLocation tintedCubeTemplate;
    private ResourceLocation tintedCustomCubeTemplate;
    private ResourceLocation tintedMaterialCubeTemplate;
    private ResourceLocation tintedCutoutCubeTemplate;
    private ResourceLocation tintedTwoLayerCutoutCubeTemplate;

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
        // Keep the final model self-contained. Generated models that inherit another
        // generated template can load as Minecraft's missing-model cube when that
        // template is not present in the active client resource output.
        BlockModelBuilder model = models()
                .getBuilder(id)
                .parent(new ModelFile.UncheckedModelFile(
                        ResourceLocation.withDefaultNamespace("block/block")
                ))
                .texture("all", texture)
                .texture("particle", texture);

        model.element()
                .from(0, 0, 0)
                .to(16, 16, 16)
                .allFaces((direction, face) -> face.texture("#all")
                        .cullface(direction)
                        .tintindex(0));
        return model;
    }

    private BlockModelBuilder createTintedCustomCubeModel(
            String id,
            ResolvedFaceTextures textures
    ) {
        return models()
                .getBuilder(id)
                .parent(tintedCustomCubeTemplate())
                .texture("front", textures.front())
                .texture("right", textures.right())
                .texture("back", textures.back())
                .texture("left", textures.left())
                .texture("up", textures.top())
                .texture("down", textures.bottom())
                .texture("particle", textures.front());
    }

    private BlockModelBuilder createTintedShapeModel(
            String id,
            ResourceLocation texture,
            ModelBox... boxes
    ) {
        // Slabs, stairs and walls must not depend on a second generated template
        // model. Write their geometry, texture and tint index directly into the
        // concrete model so the model remains valid on its own at runtime.
        BlockModelBuilder model = models()
                .getBuilder(id)
                .parent(new ModelFile.UncheckedModelFile(
                        ResourceLocation.withDefaultNamespace("block/block")
                ))
                .texture("all", texture)
                .texture("particle", texture);

        for (ModelBox box : boxes) {
            model.element()
                    .from(box.fromX(), box.fromY(), box.fromZ())
                    .to(box.toX(), box.toY(), box.toZ())
                    .allFaces((direction, face) -> face.texture("#all")
                            .tintindex(0));
        }
        return model;
    }

    private ModelFile tintedCubeTemplate() {
        if (tintedCubeTemplate != null) {
            return new ModelFile.UncheckedModelFile(tintedCubeTemplate);
        }

        String name = "templates/tinted_full_cube";
        BlockModelBuilder template = models()
                .getBuilder(name)
                .parent(new ModelFile.UncheckedModelFile(
                        ResourceLocation.withDefaultNamespace("block/block")
                ));
        template.element()
                .from(0, 0, 0)
                .to(16, 16, 16)
                .allFaces((direction, face) -> face.texture("#all")
                        .cullface(direction)
                        .tintindex(0));

        tintedCubeTemplate = modLoc("block/" + name);
        return new ModelFile.UncheckedModelFile(tintedCubeTemplate);
    }

    private ModelFile tintedCustomCubeTemplate() {
        if (tintedCustomCubeTemplate != null) {
            return new ModelFile.UncheckedModelFile(tintedCustomCubeTemplate);
        }

        String name = "templates/tinted_custom_cube";
        BlockModelBuilder template = models()
                .getBuilder(name)
                .parent(new ModelFile.UncheckedModelFile(
                        ResourceLocation.withDefaultNamespace("block/block")
                ));
        template.element().from(0, 0, 0).to(16, 16, 16)
                .face(Direction.NORTH).texture("#front").cullface(Direction.NORTH).tintindex(0).end()
                .face(Direction.EAST).texture("#right").cullface(Direction.EAST).tintindex(0).end()
                .face(Direction.SOUTH).texture("#back").cullface(Direction.SOUTH).tintindex(0).end()
                .face(Direction.WEST).texture("#left").cullface(Direction.WEST).tintindex(0).end()
                .face(Direction.UP).texture("#up").cullface(Direction.UP).tintindex(0).end()
                .face(Direction.DOWN).texture("#down").cullface(Direction.DOWN).tintindex(0).end();

        tintedCustomCubeTemplate = modLoc("block/" + name);
        return new ModelFile.UncheckedModelFile(tintedCustomCubeTemplate);
    }

    private ModelFile tintedMaterialCubeTemplate() {
        if (tintedMaterialCubeTemplate != null) {
            return new ModelFile.UncheckedModelFile(tintedMaterialCubeTemplate);
        }

        String name = "templates/tinted_material_full_cube";
        BlockModelBuilder template = models()
                .getBuilder(name)
                .parent(new ModelFile.UncheckedModelFile(
                        ResourceLocation.withDefaultNamespace("block/block")
                ));
        fullCube(template, "#base", 0);

        tintedMaterialCubeTemplate = modLoc("block/" + name);
        return new ModelFile.UncheckedModelFile(tintedMaterialCubeTemplate);
    }

    private ModelFile tintedCutoutCubeTemplate() {
        if (tintedCutoutCubeTemplate != null) {
            return new ModelFile.UncheckedModelFile(tintedCutoutCubeTemplate);
        }

        String name = "templates/tinted_cutout_full_cube";
        BlockModelBuilder template = models()
                .getBuilder(name)
                .parent(new ModelFile.UncheckedModelFile(
                        ResourceLocation.withDefaultNamespace("block/block")
                ))
                .renderType("minecraft:cutout");
        fullCube(template, "#base", 0);

        tintedCutoutCubeTemplate = modLoc("block/" + name);
        return new ModelFile.UncheckedModelFile(tintedCutoutCubeTemplate);
    }

    private ModelFile tintedTwoLayerCutoutCubeTemplate() {
        if (tintedTwoLayerCutoutCubeTemplate != null) {
            return new ModelFile.UncheckedModelFile(tintedTwoLayerCutoutCubeTemplate);
        }

        String name = "templates/tinted_two_layer_cutout_full_cube";
        BlockModelBuilder template = models()
                .getBuilder(name)
                .parent(new ModelFile.UncheckedModelFile(
                        ResourceLocation.withDefaultNamespace("block/block")
                ))
                .renderType("minecraft:cutout");
        fullCube(template, "#base", 0);
        fullCube(template, "#secondary", 1);

        tintedTwoLayerCutoutCubeTemplate = modLoc("block/" + name);
        return new ModelFile.UncheckedModelFile(tintedTwoLayerCutoutCubeTemplate);
    }

    private ModelFile tintedShapeTemplate(ModelBox... boxes) {
        String key = shapeKey(boxes);
        ResourceLocation existing = tintedShapeTemplates.get(key);
        if (existing != null) {
            return new ModelFile.UncheckedModelFile(existing);
        }

        String baseName = "templates/tinted_shape_" + Integer.toUnsignedString(key.hashCode(), 36);
        ResourceLocation location = modLoc("block/" + baseName);
        int suffix = 1;
        while (tintedShapeTemplates.containsValue(location)) {
            location = modLoc("block/" + baseName + "_" + suffix++);
        }

        BlockModelBuilder template = models()
                .getBuilder(location.getPath().substring("block/".length()))
                .parent(new ModelFile.UncheckedModelFile(
                        ResourceLocation.withDefaultNamespace("block/block")
                ));
        for (ModelBox box : boxes) {
            template.element()
                    .from(box.fromX(), box.fromY(), box.fromZ())
                    .to(box.toX(), box.toY(), box.toZ())
                    .allFaces((direction, face) -> face.texture("#all")
                            .tintindex(0));
        }

        tintedShapeTemplates.put(key, location);
        return new ModelFile.UncheckedModelFile(location);
    }

    private static String shapeKey(ModelBox... boxes) {
        StringBuilder key = new StringBuilder();
        for (ModelBox box : boxes) {
            key.append(box.fromX()).append(',')
                    .append(box.fromY()).append(',')
                    .append(box.fromZ()).append(':')
                    .append(box.toX()).append(',')
                    .append(box.toY()).append(',')
                    .append(box.toZ()).append(';');
        }
        return key.toString();
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
            registerMaterialOreHostBlocks(material);
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

    private void registerMaterialOreHostBlocks(IndustrialMaterial material) {
        if (!MaterialOreHost.hasNaturalOre(material)) {
            return;
        }
        for (MaterialOreHost host : MaterialOreHost.compatibleHosts(material)) {
            for (boolean small : new boolean[]{false, true}) {
                Block block = BlockRegistry.getMaterialOreHostBlock(material, host, small) == null
                        ? null
                        : BlockRegistry.getMaterialOreHostBlock(material, host, small).get();
                if (block == null) {
                    continue;
                }
                simpleBlockWithItem(block, oreHostBlockModel(material, host, small));
            }
        }
    }

    private BlockModelBuilder oreHostBlockModel(IndustrialMaterial material, MaterialOreHost host, boolean small) {
        MaterialPart texturePart = small ? MaterialPart.SMALL_ORE : MaterialPart.ORE;
        ResourceLocation oreTexture = MaterialTextures.blockTexture(material, texturePart).orElseThrow(() ->
                new IllegalStateException("Missing ore texture for " + material.id() + " " + texturePart));
        ResourceLocation overlay = MaterialTextures.blockOverlayTexture(material, texturePart).orElse(oreTexture);

        BlockModelBuilder baseStone = models().nested()
                .parent(new ModelFile.UncheckedModelFile(host.blockModel(material)))
                .renderType("minecraft:solid");

        BlockModelBuilder oreLayer = models().nested()
                .parent(new ModelFile.UncheckedModelFile(ResourceLocation.withDefaultNamespace("block/block")))
                .texture("layer0", oreTexture)
                .texture("layer1", overlay)
                .texture("particle", oreTexture)
                .renderType("minecraft:cutout");
        fullCube(oreLayer, "#layer0", 0);
        fullCube(oreLayer, "#layer1", 1);

        BlockModelBuilder model = models().getBuilder(host.registryName(material, small))
                .parent(new ModelFile.UncheckedModelFile(ResourceLocation.withDefaultNamespace("block/block")))
                .texture("particle", oreTexture);
        model.customLoader(CompositeModelBuilder::begin)
                .child("base_stone", baseStone)
                .child("ore_texture", oreLayer)
                .itemRenderOrder("base_stone", "ore_texture");
        return model;
    }

    private void registerMaterialPartBlocks(
            IndustrialMaterial material
    ) {
        for (MaterialPart part : material.parts()) {
            if (material.hasExistingPart(part)) {
                continue;
            }
            if (part.isOre() && MaterialOreHost.hasNaturalOre(material)) {
                continue;
            }

            if (!part.isBlock() || part == MaterialPart.FIREBOX) {
                continue;
            }

            Block block = BlockRegistry
                    .getMaterialBlock(
                            material,
                            part
                    )
                    .get();

            if (part == MaterialPart.SHAFT) {
                ResourceLocation side = modLoc("block/material_sets/shaft/axis");
                ResourceLocation end = modLoc("block/material_sets/shaft/axis_top");
                registerShaftBlock(part.registryName(material), block, side, end, true);
                continue;
            }

            Optional<ResourceLocation> baseTexture = material.hasCustomPartTexture(part)
                    ? Optional.of(material.customPartTexture(part))
                    : MaterialTextures.blockTexture(material, part);
            if (baseTexture.isEmpty()) {
                // The block remains registered even when no visual variant exists yet.
                continue;
            }

            ResourceLocation texture = baseTexture.get();
            if (part == MaterialPart.BRICK_SLAB) {
                registerSlab(part.registryName(material), (SlabBlock) block, texture);
                continue;
            }
            if (part == MaterialPart.BRICK_STAIRS) {
                registerStairs(part.registryName(material), (StairBlock) block, texture);
                continue;
            }
            if (part == MaterialPart.BRICK_WALL) {
                registerWall(part.registryName(material), (WallBlock) block, texture);
                continue;
            }

            if (material.hasCustomPartTexture(part)) {
                simpleBlockWithItem(block, models().cubeAll(part.registryName(material), texture));
                continue;
            }

            ModelFile materialBlockModel = materialBlockModel(material, part, texture);
            simpleBlockWithItem(block, materialBlockModel);
        }
    }


    private void registerShaftBlock(
            String name,
            Block block,
            ResourceLocation side,
            ResourceLocation end,
            boolean tinted
    ) {
        // Match Create's kinetic-model split: the blockstate model itself contributes no
        // visible shaft geometry. The visible shaft is a dedicated partial model rendered
        // by Flywheel / MaterialShaftRenderer. Keeping the particle texture here preserves
        // normal break particles while avoiding a second static shaft in the world.
        BlockModelBuilder staticModel = models()
                .getBuilder(name)
                .parent(new ModelFile.UncheckedModelFile(
                        ResourceLocation.withDefaultNamespace("block/block")
                ))
                .texture("particle", side);

        BlockModelBuilder rotatingModel = models()
                .getBuilder(name + "_rotating")
                .parent(new ModelFile.UncheckedModelFile(
                        ResourceLocation.withDefaultNamespace("block/block")
                ))
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
                    if (tinted) {
                        face.tintindex(0);
                    }
                });

        getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(staticModel)
                .build());

        // Inventory/item rendering must remain visible.
        simpleBlockItem(block, rotatingModel);
    }

    private BlockModelBuilder materialBlockModel(
            IndustrialMaterial material,
            MaterialPart part,
            ResourceLocation baseTexture
    ) {
        String name = part.registryName(material);
        Optional<ResourceLocation> secondaryTexture = MaterialTextures.blockSecondaryTexture(material, part);

        // Generate frame/raw-block geometry directly in each material model. These two parts used
        // to inherit a generated template model; when that parent was absent from the active data
        // output Minecraft replaced the complete model with the purple/black missing-texture cube.
        // Direct elements also make both the block model and its generated item model self-contained.
        if (part == MaterialPart.BLOCK || part == MaterialPart.FRAME || part == MaterialPart.RAW_BLOCK
                || part == MaterialPart.CLAY_BLOCK || part == MaterialPart.BRICKS) {
            BlockModelBuilder model = models()
                    .getBuilder(name)
                    .parent(new ModelFile.UncheckedModelFile(
                            ResourceLocation.withDefaultNamespace("block/block")
                    ))
                    .texture("base", baseTexture)
                    .texture("particle", baseTexture);

            // BLOCK textures commonly have a transparent secondary/highlight layer. Keep the
            // concrete generated model self-contained and render that second cube as cutout,
            // rather than inheriting the generated two-layer template that may not be present
            // in the active runtime resource output.
            if (part == MaterialPart.BLOCK || part == MaterialPart.FRAME || part == MaterialPart.RAW_BLOCK) {
                model.renderType("minecraft:cutout");
            }

            fullCube(model, "#base", 0);
            secondaryTexture.ifPresent(texture -> {
                model.texture("secondary", texture);
                fullCube(model, "#secondary", 1);
            });
            return model;
        }

        BlockModelBuilder model = models()
                .getBuilder(name)
                .parent(materialBlockTemplate(part, secondaryTexture.isPresent()))
                .texture("base", baseTexture)
                .texture("particle", baseTexture);

        secondaryTexture.ifPresent(texture -> {
            model.texture("secondary", texture);
        });

        return model;
    }

    private ModelFile materialBlockTemplate(
            MaterialPart part,
            boolean hasSecondaryTexture
    ) {
        if (hasSecondaryTexture) {
            return tintedTwoLayerCutoutCubeTemplate();
        }
        if (part == MaterialPart.FRAME) {
            return tintedCutoutCubeTemplate();
        }
        return tintedMaterialCubeTemplate();
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

    private static final List<String>
            SIMPLE_BLOCK_TEXTURE_FOLDERS = List.of(
            "block/casings/casing",
            "block"
    );
}
