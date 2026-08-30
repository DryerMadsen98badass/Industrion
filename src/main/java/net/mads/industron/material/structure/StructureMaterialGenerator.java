package net.mads.industron.material.structure;

import net.mads.industron.material.MaterialPart;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class StructureMaterialGenerator {
    private static final Map<String, List<StructureBlockDefinition>> BLOCK_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, List<StructureBlockDefinition>> GENERATED_BLOCK_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, List<MaterialPart>> GENERATED_ITEM_CACHE = new ConcurrentHashMap<>();

    private StructureMaterialGenerator() {
    }

    public static List<StructureBlockDefinition> blockDefinitions(StructureMaterial material) {
        return BLOCK_CACHE.computeIfAbsent(cacheKey(material), ignored -> {
            if (material instanceof WoodMaterial wood) {
                return List.copyOf(woodDefinitions(wood));
            }
            if (material instanceof StoneMaterial stone) {
                return List.copyOf(stoneDefinitions(stone));
            }
            if (material instanceof MetalMaterial metal) {
                return MetalStructureCatalog.blocks(metal);
            }
            if (material instanceof GemMaterial gem) {
                return List.copyOf(gemDefinitions(gem));
            }
            throw new IllegalArgumentException("Unsupported structure material type: " + material.getClass().getName());
        });
    }

    public static List<StructureBlockDefinition> generatedBlockDefinitions(StructureMaterial material) {
        return GENERATED_BLOCK_CACHE.computeIfAbsent(cacheKey(material), ignored ->
                blockDefinitions(material).stream()
                        .filter(definition -> definition.part().isEmpty()
                                || !material.hasExistingPart(definition.part().get()))
                        .toList()
        );
    }

    public static List<MaterialPart> generatedItemForms(StructureMaterial material) {
        return GENERATED_ITEM_CACHE.computeIfAbsent(cacheKey(material), ignored ->
                material.generatedForms().stream()
                        .filter(MaterialPart::isItem)
                        .filter(part -> !material.hasExistingPart(part))
                        .sorted((a, b) -> Integer.compare(a.ordinal(), b.ordinal()))
                        .toList()
        );
    }

    private static String cacheKey(StructureMaterial material) {
        return material.getClass().getName() + ":" + material.id();
    }

    private static List<StructureBlockDefinition> woodDefinitions(WoodMaterial material) {
        WoodModel model = material.model();
        List<String> files = StructureSetResolver.textureFiles(model);
        if (files.isEmpty()) {
            throw new IllegalStateException("No wood structure textures found for " + model.id());
        }

        List<StructureBlockDefinition> result = new ArrayList<>();
        String family = model.id();

        String planks = family + "_planks.png";
        if (files.contains(planks)) {
            String planksId = MaterialPart.PLANKS.registryName(material);
            add(result, material, planksId, material.displayName() + " Planks", StructureBlockDefinition.Shape.CUBE,
                    planks, null, null, null, null, MaterialPart.PLANKS, null);
            add(result, material, MaterialPart.SLAB.registryName(material), material.displayName() + " Slab",
                    StructureBlockDefinition.Shape.SLAB, planks, null, null, null, planksId,
                    MaterialPart.SLAB, MaterialPart.PLANKS);
            add(result, material, MaterialPart.STAIRS.registryName(material), material.displayName() + " Stairs",
                    StructureBlockDefinition.Shape.STAIRS, planks, null, null, null, planksId,
                    MaterialPart.STAIRS, MaterialPart.PLANKS);
            add(result, material, MaterialPart.FENCE.registryName(material), material.displayName() + " Fence",
                    StructureBlockDefinition.Shape.FENCE, planks, null, null, null, planksId,
                    MaterialPart.FENCE, MaterialPart.PLANKS);
            add(result, material, MaterialPart.FENCE_GATE.registryName(material), material.displayName() + " Fence Gate",
                    StructureBlockDefinition.Shape.FENCE_GATE, planks, null, null, null, planksId,
                    MaterialPart.FENCE_GATE, MaterialPart.PLANKS);
            add(result, material, MaterialPart.BUTTON.registryName(material), material.displayName() + " Button",
                    StructureBlockDefinition.Shape.BUTTON, planks, null, null, null, planksId,
                    MaterialPart.BUTTON, MaterialPart.PLANKS);
            add(result, material, MaterialPart.PRESSURE_PLATE.registryName(material), material.displayName() + " Pressure Plate",
                    StructureBlockDefinition.Shape.PRESSURE_PLATE, planks, null, null, null, planksId,
                    MaterialPart.PRESSURE_PLATE, MaterialPart.PLANKS);
        }

        addWoodPillar(result, material, files, family + "_log.png", family + "_log_top.png",
                MaterialPart.LOG, material.displayName() + " Log");
        addWoodPillar(result, material, files, "stripped_" + family + "_log.png", "stripped_" + family + "_log_top.png",
                MaterialPart.STRIPPED_LOG, "Stripped " + material.displayName() + " Log");

        if (files.contains(family + "_log.png")) {
            add(result, material, MaterialPart.WOOD.registryName(material), material.displayName() + " Wood",
                    StructureBlockDefinition.Shape.CUBE, family + "_log.png", null, null, null, null,
                    MaterialPart.WOOD, null);
        }
        if (files.contains("stripped_" + family + "_log.png")) {
            add(result, material, MaterialPart.STRIPPED_WOOD.registryName(material), "Stripped " + material.displayName() + " Wood",
                    StructureBlockDefinition.Shape.CUBE, "stripped_" + family + "_log.png", null, null, null, null,
                    MaterialPart.STRIPPED_WOOD, null);
        }

        String doorBottom = family + "_door_bottom.png";
        String doorTop = family + "_door_top.png";
        if (files.contains(doorBottom) && files.contains(doorTop)) {
            String item = files.contains(family + "_door.png") ? family + "_door.png" : null;
            add(result, material, MaterialPart.DOOR.registryName(material), material.displayName() + " Door",
                    StructureBlockDefinition.Shape.DOOR, doorBottom, doorTop, null, item, null,
                    MaterialPart.DOOR, null);
        }

        String trapdoor = family + "_trapdoor.png";
        if (files.contains(trapdoor)) {
            add(result, material, MaterialPart.TRAPDOOR.registryName(material), material.displayName() + " Trapdoor",
                    StructureBlockDefinition.Shape.TRAPDOOR, trapdoor, null, null, null, null,
                    MaterialPart.TRAPDOOR, null);
        }

        String leaves = family + "_leaves.png";
        if (files.contains(leaves)) {
            add(result, material, MaterialPart.LEAVES.registryName(material), material.displayName() + " Leaves",
                    StructureBlockDefinition.Shape.LEAVES, leaves, null, null, null, null,
                    MaterialPart.LEAVES, null);
        }

        String sapling = family + "_sapling.png";
        if (files.contains(sapling)) {
            add(result, material, MaterialPart.SAPLING.registryName(material), material.displayName() + " Sapling",
                    StructureBlockDefinition.Shape.SAPLING, sapling, null, null, null, null,
                    MaterialPart.SAPLING, null);
        }

        String window = family + "_window.png";
        if (files.contains(window)) {
            add(result, material, MaterialPart.WINDOW.registryName(material), material.displayName() + " Window",
                    StructureBlockDefinition.Shape.WINDOW, window, null, null, null, null,
                    MaterialPart.WINDOW, null);
        }

        return result;
    }

    private static void addWoodPillar(
            List<StructureBlockDefinition> result,
            WoodMaterial material,
            List<String> files,
            String side,
            String top,
            MaterialPart part,
            String displayName
    ) {
        if (!files.contains(side)) {
            return;
        }
        add(result, material, part.registryName(material), displayName,
                StructureBlockDefinition.Shape.PILLAR, side,
                files.contains(top) ? top : side,
                files.contains(top) ? top : side,
                null, null, part, null);
    }

    private static List<StructureBlockDefinition> stoneDefinitions(StoneMaterial material) {
        StoneModel model = material.model();
        List<StructureBlockDefinition> result = new ArrayList<>();

        StoneTextures base = stoneTextures(material);
        addStoneFamily(
                result,
                material,
                MaterialPart.STONE,
                MaterialPart.SLAB,
                MaterialPart.STAIRS,
                MaterialPart.WALL,
                base,
                model == StoneModel.BASALT ? StructureBlockDefinition.Shape.PILLAR : StructureBlockDefinition.Shape.CUBE
        );

        addStoneFamily(
                result,
                material,
                MaterialPart.COBBLED_STONE,
                MaterialPart.COBBLED_SLAB,
                MaterialPart.COBBLED_STAIRS,
                MaterialPart.COBBLED_WALL,
                cobbledStoneTextures(),
                StructureBlockDefinition.Shape.CUBE
        );

        addStoneFamily(
                result,
                material,
                MaterialPart.POLISHED_STONE,
                MaterialPart.POLISHED_SLAB,
                MaterialPart.POLISHED_STAIRS,
                MaterialPart.POLISHED_WALL,
                polishedStoneTextures(model),
                model == StoneModel.BASALT ? StructureBlockDefinition.Shape.PILLAR : StructureBlockDefinition.Shape.CUBE
        );

        addVanillaStoneDecorations(result, material);
        if (hasCreatePaletteFamily(model)) {
            addCreatePaletteDecorations(result, material);
        }

        return result.stream()
                .filter(definition -> definition.part().map(part -> !material.isWithout(part)).orElse(true))
                .filter(definition -> definition.basePart().map(part -> !material.isWithout(part)).orElse(true))
                .toList();
    }

    private static void addStoneFamily(
            List<StructureBlockDefinition> result,
            StoneMaterial material,
            MaterialPart blockPart,
            MaterialPart slabPart,
            MaterialPart stairsPart,
            MaterialPart wallPart,
            StoneTextures textures,
            StructureBlockDefinition.Shape blockShape
    ) {
        boolean primaryCobbledFamily = blockPart == MaterialPart.COBBLED_STONE
                && usesCobbledAsPrimaryFamily(material);

        String blockId = blockPart.registryName(material);
        String slabId = primaryCobbledFamily
                ? material.id() + "_slab"
                : slabPart.registryName(material);
        String stairsId = primaryCobbledFamily
                ? material.id() + "_stairs"
                : stairsPart.registryName(material);
        String wallId = primaryCobbledFamily
                ? material.id() + "_wall"
                : wallPart.registryName(material);

        String blockName = blockPart.readableName(material);
        String slabName = primaryCobbledFamily
                ? material.displayName() + " Slab"
                : slabPart.readableName(material);
        String stairsName = primaryCobbledFamily
                ? material.displayName() + " Stairs"
                : stairsPart.readableName(material);
        String wallName = primaryCobbledFamily
                ? material.displayName() + " Wall"
                : wallPart.readableName(material);

        add(result, material, blockId, blockName, blockShape,
                textures.side(), textures.top(), textures.bottom(), null, null, blockPart, null);
        add(result, material, slabId, slabName,
                StructureBlockDefinition.Shape.SLAB, textures.side(), textures.top(), textures.bottom(), null,
                blockId, slabPart, blockPart);
        add(result, material, stairsId, stairsName,
                StructureBlockDefinition.Shape.STAIRS, textures.side(), textures.top(), textures.bottom(), null,
                blockId, stairsPart, blockPart);
        add(result, material, wallId, wallName,
                StructureBlockDefinition.Shape.WALL, textures.side(), textures.top(), textures.bottom(), null,
                blockId, wallPart, blockPart);
    }

    private static boolean usesCobbledAsPrimaryFamily(StoneMaterial material) {
        if (!material.hasExistingPart(MaterialPart.COBBLED_STONE)) {
            return false;
        }
        if (material.isWithout(MaterialPart.STONE)) {
            return true;
        }
        if (!material.hasExistingPart(MaterialPart.STONE)) {
            return false;
        }

        String stonePath = material.existingPart(MaterialPart.STONE).getPath();
        return stonePath.equals("cut_" + material.model().id())
                || stonePath.equals("smooth_basalt");
    }

    private static StoneTextures stoneTextures(StoneMaterial material) {
        StoneModel model = material.model();
        if (material.hasExistingPart(MaterialPart.STONE)) {
            String path = material.existingPart(MaterialPart.STONE).getPath();
            if (path.equals("smooth_basalt")) {
                return StoneTextures.all("smooth_basalt.png");
            }
            if (path.equals("cut_" + model.id())) {
                return StoneTextures.all(model.id() + "_cut.png");
            }
        }
        return new StoneTextures(
                model.baseSideTexture(),
                model.baseTopTexture(),
                model.baseBottomTexture()
        );
    }

    private static StoneTextures cobbledStoneTextures() {
        return StoneTextures.all("cobblestone.png");
    }

    private static StoneTextures polishedStoneTextures(StoneModel model) {
        return switch (model) {
            case ANDESITE -> StoneTextures.all("polished_andesite.png");
            case BASALT -> new StoneTextures("polished_basalt_side.png", "polished_basalt_top.png", "polished_basalt_top.png");
            case BLACKSTONE -> StoneTextures.all("polished_blackstone.png");
            case DEEPSLATE -> StoneTextures.all("polished_deepslate.png");
            case DIORITE -> StoneTextures.all("polished_diorite.png");
            case GRANITE -> StoneTextures.all("polished_granite.png");
            case TUFF -> StoneTextures.all("polished_tuff.png");
            default -> StoneTextures.all("polished_" + model.id() + ".png");
        };
    }

    private static void addVanillaStoneDecorations(
            List<StructureBlockDefinition> result,
            StoneMaterial material
    ) {
        switch (material.model()) {
            case STONE -> {
                // The canonical vanilla-only forms are represented by .existing(...) in StoneMaterials.
                // No source sprites are copied into the structure-set tree for these forms.
            }
            case BASALT -> addSmoothFamily(result, material, StoneTextures.all("smooth_basalt.png"));
            case BLACKSTONE -> {
                addStoneFamily(
                        result,
                        material,
                        MaterialPart.POLISHED_STONE_BRICKS,
                        MaterialPart.POLISHED_STONE_BRICK_SLAB,
                        MaterialPart.POLISHED_STONE_BRICK_STAIRS,
                        MaterialPart.POLISHED_STONE_BRICK_WALL,
                        StoneTextures.all("polished_blackstone_bricks.png"),
                        StructureBlockDefinition.Shape.CUBE
                );
                addSingleStoneBlock(result, material, MaterialPart.CRACKED_POLISHED_STONE_BRICKS,
                        StoneTextures.all("cracked_polished_blackstone_bricks.png"), StructureBlockDefinition.Shape.CUBE);
                addSingleStoneBlock(result, material, MaterialPart.CHISELED_POLISHED_STONE,
                        StoneTextures.all("chiseled_polished_blackstone.png"), StructureBlockDefinition.Shape.CUBE);
                addSingleStoneBlock(result, material, MaterialPart.GILDED_STONE,
                        StoneTextures.all("gilded_blackstone.png"), StructureBlockDefinition.Shape.CUBE);
            }
            case DEEPSLATE -> {
                addStoneFamily(result, material,
                        MaterialPart.STONE_BRICKS, MaterialPart.STONE_BRICK_SLAB,
                        MaterialPart.STONE_BRICK_STAIRS, MaterialPart.STONE_BRICK_WALL,
                        StoneTextures.all("deepslate_bricks.png"), StructureBlockDefinition.Shape.CUBE);
                addStoneFamily(result, material,
                        MaterialPart.STONE_TILES, MaterialPart.STONE_TILE_SLAB,
                        MaterialPart.STONE_TILE_STAIRS, MaterialPart.STONE_TILE_WALL,
                        StoneTextures.all("deepslate_tiles.png"), StructureBlockDefinition.Shape.CUBE);
                addSingleStoneBlock(result, material, MaterialPart.CRACKED_STONE_BRICKS,
                        StoneTextures.all("cracked_deepslate_bricks.png"), StructureBlockDefinition.Shape.CUBE);
                addSingleStoneBlock(result, material, MaterialPart.CRACKED_STONE_TILES,
                        StoneTextures.all("cracked_deepslate_tiles.png"), StructureBlockDefinition.Shape.CUBE);
                addSingleStoneBlock(result, material, MaterialPart.CHISELED_STONE,
                        StoneTextures.all("chiseled_deepslate.png"), StructureBlockDefinition.Shape.CUBE);
            }
            case END_STONE -> addStoneFamily(result, material,
                    MaterialPart.STONE_BRICKS, MaterialPart.STONE_BRICK_SLAB,
                    MaterialPart.STONE_BRICK_STAIRS, MaterialPart.STONE_BRICK_WALL,
                    StoneTextures.all("end_stone_bricks.png"), StructureBlockDefinition.Shape.CUBE);
            case SANDSTONE -> addSandstoneDecorations(result, material, false);
            case RED_SANDSTONE -> addSandstoneDecorations(result, material, true);
            case TUFF -> {
                addStoneFamily(result, material,
                        MaterialPart.STONE_BRICKS, MaterialPart.STONE_BRICK_SLAB,
                        MaterialPart.STONE_BRICK_STAIRS, MaterialPart.STONE_BRICK_WALL,
                        StoneTextures.all("tuff_bricks.png"), StructureBlockDefinition.Shape.CUBE);
                addSingleStoneBlock(result, material, MaterialPart.CHISELED_STONE,
                        new StoneTextures("chiseled_tuff.png", "chiseled_tuff_top.png", "chiseled_tuff_top.png"),
                        StructureBlockDefinition.Shape.CUBE);
                addSingleStoneBlock(result, material, MaterialPart.CHISELED_STONE_BRICKS,
                        new StoneTextures("chiseled_tuff_bricks.png", "chiseled_tuff_bricks_top.png", "chiseled_tuff_bricks_top.png"),
                        StructureBlockDefinition.Shape.CUBE);
            }
            default -> {
            }
        }
    }

    private static void addSandstoneDecorations(
            List<StructureBlockDefinition> result,
            StoneMaterial material,
            boolean red
    ) {
        String prefix = red ? "red_sandstone" : "sandstone";
        addSingleStoneBlock(result, material, MaterialPart.CUT_STONE,
                StoneTextures.all("cut_" + prefix + ".png"), StructureBlockDefinition.Shape.CUBE);
        add(result, material, MaterialPart.CUT_STONE_SLAB.registryName(material),
                MaterialPart.CUT_STONE_SLAB.readableName(material), StructureBlockDefinition.Shape.SLAB,
                "cut_" + prefix + ".png", null, null, null,
                MaterialPart.CUT_STONE.registryName(material), MaterialPart.CUT_STONE_SLAB, MaterialPart.CUT_STONE);
        addSingleStoneBlock(result, material, MaterialPart.CHISELED_STONE,
                StoneTextures.all("chiseled_" + prefix + ".png"), StructureBlockDefinition.Shape.CUBE);
    }

    private static void addSmoothFamily(
            List<StructureBlockDefinition> result,
            StoneMaterial material,
            StoneTextures textures
    ) {
        String blockId = MaterialPart.SMOOTH_STONE.registryName(material);
        add(result, material, blockId, MaterialPart.SMOOTH_STONE.readableName(material),
                StructureBlockDefinition.Shape.CUBE, textures.side(), textures.top(), textures.bottom(), null,
                null, MaterialPart.SMOOTH_STONE, null);
        add(result, material, MaterialPart.SMOOTH_STONE_SLAB.registryName(material),
                MaterialPart.SMOOTH_STONE_SLAB.readableName(material), StructureBlockDefinition.Shape.SLAB,
                textures.side(), textures.top(), textures.bottom(), null, blockId,
                MaterialPart.SMOOTH_STONE_SLAB, MaterialPart.SMOOTH_STONE);
        add(result, material, MaterialPart.SMOOTH_STONE_STAIRS.registryName(material),
                MaterialPart.SMOOTH_STONE_STAIRS.readableName(material), StructureBlockDefinition.Shape.STAIRS,
                textures.side(), textures.top(), textures.bottom(), null, blockId,
                MaterialPart.SMOOTH_STONE_STAIRS, MaterialPart.SMOOTH_STONE);
    }

    private static void addSingleStoneBlock(
            List<StructureBlockDefinition> result,
            StoneMaterial material,
            MaterialPart part,
            StoneTextures textures,
            StructureBlockDefinition.Shape shape
    ) {
        add(result, material, part.registryName(material), part.readableName(material), shape,
                textures.side(), textures.top(), textures.bottom(), null, null, part, null);
    }

    private static boolean hasCreatePaletteFamily(StoneModel model) {
        return switch (model) {
            case ANDESITE, ASURINE, CALCITE, CRIMSITE, DEEPSLATE, DIORITE, DRIPSTONE,
                    GRANITE, LIMESTONE, OCHRUM, SCORCHIA, SCORIA, TUFF, VERIDIUM -> true;
            default -> false;
        };
    }

    private static void addCreatePaletteDecorations(
            List<StructureBlockDefinition> result,
            StoneMaterial material
    ) {
        String family = material.model().id();
        String cut = family + "_cut.png";
        String cutSlab = family + "_cut_slab.png";
        String polished = family + "_cut_polished.png";
        String brick = family + "_cut_brick.png";
        String smallBrick = family + "_cut_small_brick.png";
        String layered = family + "_cut_layered.png";
        String pillar = family + "_cut_pillar.png";
        String cap = family + "_cut_cap.png";

        addStoneFamily(result, material,
                MaterialPart.CUT_STONE, MaterialPart.CUT_STONE_SLAB,
                MaterialPart.CUT_STONE_STAIRS, MaterialPart.CUT_STONE_WALL,
                StoneTextures.all(cut), StructureBlockDefinition.Shape.CUBE);

        String polishedId = MaterialPart.POLISHED_CUT_STONE.registryName(material);
        add(result, material, polishedId, MaterialPart.POLISHED_CUT_STONE.readableName(material),
                StructureBlockDefinition.Shape.CUBE, polished, null, null, null,
                null, MaterialPart.POLISHED_CUT_STONE, null);
        add(result, material, MaterialPart.POLISHED_CUT_STONE_SLAB.registryName(material),
                MaterialPart.POLISHED_CUT_STONE_SLAB.readableName(material), StructureBlockDefinition.Shape.SLAB,
                cutSlab, polished, polished, null, polishedId,
                MaterialPart.POLISHED_CUT_STONE_SLAB, MaterialPart.POLISHED_CUT_STONE);
        add(result, material, MaterialPart.POLISHED_CUT_STONE_STAIRS.registryName(material),
                MaterialPart.POLISHED_CUT_STONE_STAIRS.readableName(material), StructureBlockDefinition.Shape.STAIRS,
                polished, null, null, null, polishedId,
                MaterialPart.POLISHED_CUT_STONE_STAIRS, MaterialPart.POLISHED_CUT_STONE);
        add(result, material, MaterialPart.POLISHED_CUT_STONE_WALL.registryName(material),
                MaterialPart.POLISHED_CUT_STONE_WALL.readableName(material), StructureBlockDefinition.Shape.WALL,
                polished, null, null, null, polishedId,
                MaterialPart.POLISHED_CUT_STONE_WALL, MaterialPart.POLISHED_CUT_STONE);

        addStoneFamily(result, material,
                MaterialPart.CUT_STONE_BRICKS, MaterialPart.CUT_STONE_BRICK_SLAB,
                MaterialPart.CUT_STONE_BRICK_STAIRS, MaterialPart.CUT_STONE_BRICK_WALL,
                StoneTextures.all(brick), StructureBlockDefinition.Shape.CUBE);
        addStoneFamily(result, material,
                MaterialPart.SMALL_STONE_BRICKS, MaterialPart.SMALL_STONE_BRICK_SLAB,
                MaterialPart.SMALL_STONE_BRICK_STAIRS, MaterialPart.SMALL_STONE_BRICK_WALL,
                StoneTextures.all(smallBrick), StructureBlockDefinition.Shape.CUBE);

        addSingleStoneBlock(result, material, MaterialPart.LAYERED_STONE,
                new StoneTextures(layered, cap, cap), StructureBlockDefinition.Shape.CUBE);
        addSingleStoneBlock(result, material, MaterialPart.PILLAR,
                new StoneTextures(pillar, cap, cap), StructureBlockDefinition.Shape.PILLAR);
    }

    private record StoneTextures(String side, String top, String bottom) {
        private static StoneTextures all(String file) {
            return new StoneTextures(file, file, file);
        }
    }

    private static List<StructureBlockDefinition> gemDefinitions(GemMaterial material) {
        List<String> files = StructureSetResolver.textureFiles(material.model());
        List<String> required = List.of(
                "quartz_block_side.png",
                "quartz_block_top.png",
                "quartz_block_bottom.png",
                "quartz_pillar.png",
                "quartz_pillar_top.png",
                "chiseled_quartz_block.png",
                "chiseled_quartz_block_top.png",
                "quartz_bricks.png"
        );
        for (String file : required) {
            if (!files.contains(file)) {
                throw new IllegalStateException(
                        "Gem structure set " + material.model().id() + " is missing " + file
                );
            }
        }

        List<StructureBlockDefinition> result = new ArrayList<>();
        String baseId = material.hasExistingPart(MaterialPart.BLOCK)
                ? material.existingPart(MaterialPart.BLOCK).getPath()
                : material.id() + "_block";

        add(result, material, MaterialPart.SLAB.registryName(material),
                MaterialPart.SLAB.readableName(material),
                StructureBlockDefinition.Shape.SLAB,
                "quartz_block_side.png", "quartz_block_top.png", "quartz_block_bottom.png", null,
                baseId, MaterialPart.SLAB, MaterialPart.BLOCK);
        add(result, material, MaterialPart.STAIRS.registryName(material),
                MaterialPart.STAIRS.readableName(material),
                StructureBlockDefinition.Shape.STAIRS,
                "quartz_block_side.png", "quartz_block_top.png", "quartz_block_bottom.png", null,
                baseId, MaterialPart.STAIRS, MaterialPart.BLOCK);
        add(result, material, MaterialPart.WALL.registryName(material),
                MaterialPart.WALL.readableName(material),
                StructureBlockDefinition.Shape.WALL,
                "quartz_block_side.png", "quartz_block_top.png", "quartz_block_bottom.png", null,
                baseId, MaterialPart.WALL, MaterialPart.BLOCK);

        add(result, material, MaterialPart.PILLAR.registryName(material),
                MaterialPart.PILLAR.readableName(material),
                StructureBlockDefinition.Shape.PILLAR,
                "quartz_pillar.png", "quartz_pillar_top.png", "quartz_pillar_top.png", null,
                null, MaterialPart.PILLAR, null);
        add(result, material, MaterialPart.CHISELED_BLOCK.registryName(material),
                MaterialPart.CHISELED_BLOCK.readableName(material),
                StructureBlockDefinition.Shape.CUBE,
                "chiseled_quartz_block.png", "chiseled_quartz_block_top.png", "chiseled_quartz_block_top.png", null,
                null, MaterialPart.CHISELED_BLOCK, null);
        add(result, material, MaterialPart.BRICKS.registryName(material),
                MaterialPart.BRICKS.readableName(material),
                StructureBlockDefinition.Shape.CUBE,
                "quartz_bricks.png", null, null, null,
                null, MaterialPart.BRICKS, null);

        String smoothId = MaterialPart.SMOOTH_BLOCK.registryName(material);
        add(result, material, smoothId,
                MaterialPart.SMOOTH_BLOCK.readableName(material),
                StructureBlockDefinition.Shape.CUBE,
                "quartz_block_bottom.png", null, null, null,
                null, MaterialPart.SMOOTH_BLOCK, null);
        add(result, material, MaterialPart.SMOOTH_SLAB.registryName(material),
                MaterialPart.SMOOTH_SLAB.readableName(material),
                StructureBlockDefinition.Shape.SLAB,
                "quartz_block_bottom.png", null, null, null,
                smoothId, MaterialPart.SMOOTH_SLAB, MaterialPart.SMOOTH_BLOCK);
        add(result, material, MaterialPart.SMOOTH_STAIRS.registryName(material),
                MaterialPart.SMOOTH_STAIRS.readableName(material),
                StructureBlockDefinition.Shape.STAIRS,
                "quartz_block_bottom.png", null, null, null,
                smoothId, MaterialPart.SMOOTH_STAIRS, MaterialPart.SMOOTH_BLOCK);

        return result;
    }

    private static void add(
            List<StructureBlockDefinition> result,
            StructureMaterial material,
            String registryName,
            String displayName,
            StructureBlockDefinition.Shape shape,
            String textureFile,
            String topTextureFile,
            String bottomTextureFile,
            String itemTextureFile,
            String baseRegistryName,
            MaterialPart part,
            MaterialPart basePart
    ) {
        result.add(new StructureBlockDefinition(
                material,
                registryName,
                displayName,
                shape,
                textureFile,
                Optional.ofNullable(topTextureFile),
                Optional.ofNullable(bottomTextureFile),
                Optional.ofNullable(itemTextureFile),
                Optional.ofNullable(baseRegistryName),
                Optional.ofNullable(part),
                Optional.ofNullable(basePart)
        ));
    }
}
