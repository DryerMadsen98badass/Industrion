package net.mads.industron.material.structure;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class StructureMaterialGenerator {
    private static final Map<String, List<StructureBlockDefinition>> BLOCK_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, List<StructureBlockDefinition>> GENERATED_BLOCK_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, List<StructureMaterialPart>> GENERATED_ITEM_CACHE = new ConcurrentHashMap<>();

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

    public static List<StructureMaterialPart> generatedItemForms(StructureMaterial material) {
        return GENERATED_ITEM_CACHE.computeIfAbsent(cacheKey(material), ignored ->
                material.generatedForms().stream()
                        .filter(StructureMaterialPart::isItem)
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
            String planksId = StructureMaterialPart.PLANKS.registryName(material);
            add(result, material, planksId, material.displayName() + " Planks", StructureBlockDefinition.Shape.CUBE,
                    planks, null, null, null, null, StructureMaterialPart.PLANKS, null);
            add(result, material, StructureMaterialPart.SLAB.registryName(material), material.displayName() + " Slab",
                    StructureBlockDefinition.Shape.SLAB, planks, null, null, null, planksId,
                    StructureMaterialPart.SLAB, StructureMaterialPart.PLANKS);
            add(result, material, StructureMaterialPart.STAIRS.registryName(material), material.displayName() + " Stairs",
                    StructureBlockDefinition.Shape.STAIRS, planks, null, null, null, planksId,
                    StructureMaterialPart.STAIRS, StructureMaterialPart.PLANKS);
            add(result, material, StructureMaterialPart.FENCE.registryName(material), material.displayName() + " Fence",
                    StructureBlockDefinition.Shape.FENCE, planks, null, null, null, planksId,
                    StructureMaterialPart.FENCE, StructureMaterialPart.PLANKS);
            add(result, material, StructureMaterialPart.FENCE_GATE.registryName(material), material.displayName() + " Fence Gate",
                    StructureBlockDefinition.Shape.FENCE_GATE, planks, null, null, null, planksId,
                    StructureMaterialPart.FENCE_GATE, StructureMaterialPart.PLANKS);
            add(result, material, StructureMaterialPart.BUTTON.registryName(material), material.displayName() + " Button",
                    StructureBlockDefinition.Shape.BUTTON, planks, null, null, null, planksId,
                    StructureMaterialPart.BUTTON, StructureMaterialPart.PLANKS);
            add(result, material, StructureMaterialPart.PRESSURE_PLATE.registryName(material), material.displayName() + " Pressure Plate",
                    StructureBlockDefinition.Shape.PRESSURE_PLATE, planks, null, null, null, planksId,
                    StructureMaterialPart.PRESSURE_PLATE, StructureMaterialPart.PLANKS);
        }

        addWoodPillar(result, material, files, family + "_log.png", family + "_log_top.png",
                StructureMaterialPart.LOG, material.displayName() + " Log");
        addWoodPillar(result, material, files, "stripped_" + family + "_log.png", "stripped_" + family + "_log_top.png",
                StructureMaterialPart.STRIPPED_LOG, "Stripped " + material.displayName() + " Log");

        if (files.contains(family + "_log.png")) {
            add(result, material, StructureMaterialPart.WOOD.registryName(material), material.displayName() + " Wood",
                    StructureBlockDefinition.Shape.CUBE, family + "_log.png", null, null, null, null,
                    StructureMaterialPart.WOOD, null);
        }
        if (files.contains("stripped_" + family + "_log.png")) {
            add(result, material, StructureMaterialPart.STRIPPED_WOOD.registryName(material), "Stripped " + material.displayName() + " Wood",
                    StructureBlockDefinition.Shape.CUBE, "stripped_" + family + "_log.png", null, null, null, null,
                    StructureMaterialPart.STRIPPED_WOOD, null);
        }

        String doorBottom = family + "_door_bottom.png";
        String doorTop = family + "_door_top.png";
        if (files.contains(doorBottom) && files.contains(doorTop)) {
            String item = files.contains(family + "_door.png") ? family + "_door.png" : null;
            add(result, material, StructureMaterialPart.DOOR.registryName(material), material.displayName() + " Door",
                    StructureBlockDefinition.Shape.DOOR, doorBottom, doorTop, null, item, null,
                    StructureMaterialPart.DOOR, null);
        }

        String trapdoor = family + "_trapdoor.png";
        if (files.contains(trapdoor)) {
            add(result, material, StructureMaterialPart.TRAPDOOR.registryName(material), material.displayName() + " Trapdoor",
                    StructureBlockDefinition.Shape.TRAPDOOR, trapdoor, null, null, null, null,
                    StructureMaterialPart.TRAPDOOR, null);
        }

        String leaves = family + "_leaves.png";
        if (files.contains(leaves)) {
            add(result, material, StructureMaterialPart.LEAVES.registryName(material), material.displayName() + " Leaves",
                    StructureBlockDefinition.Shape.LEAVES, leaves, null, null, null, null,
                    StructureMaterialPart.LEAVES, null);
        }

        String sapling = family + "_sapling.png";
        if (files.contains(sapling)) {
            add(result, material, StructureMaterialPart.SAPLING.registryName(material), material.displayName() + " Sapling",
                    StructureBlockDefinition.Shape.SAPLING, sapling, null, null, null, null,
                    StructureMaterialPart.SAPLING, null);
        }

        String window = family + "_window.png";
        if (files.contains(window)) {
            add(result, material, StructureMaterialPart.WINDOW.registryName(material), material.displayName() + " Window",
                    StructureBlockDefinition.Shape.WINDOW, window, null, null, null, null,
                    StructureMaterialPart.WINDOW, null);
        }

        return result;
    }

    private static void addWoodPillar(
            List<StructureBlockDefinition> result,
            WoodMaterial material,
            List<String> files,
            String side,
            String top,
            StructureMaterialPart part,
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
        List<String> files = StructureSetResolver.textureFiles(model);
        if (files.isEmpty()) {
            throw new IllegalStateException("No stone structure textures found for " + model.id());
        }

        Map<String, String> fileByStem = new LinkedHashMap<>();
        for (String file : files) {
            fileByStem.put(StructureSetResolver.stripPng(file), file);
        }

        List<StructureBlockDefinition> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : fileByStem.entrySet()) {
            String sourceStem = entry.getKey();
            String sourceFile = entry.getValue();

            if (isStoneAuxiliary(sourceStem)) {
                continue;
            }

            if (sourceStem.endsWith("_slab")) {
                continue;
            }

            String registryName = replaceFamily(sourceStem, model.id(), material.id());
            boolean root = sourceStem.equals(model.id());
            StructureMaterialPart fixedPart = root ? StructureMaterialPart.BLOCK : null;

            String topFile = fileByStem.get(sourceStem + "_top");
            String bottomFile = fileByStem.get(sourceStem + "_bottom");
            StructureBlockDefinition.Shape shape = sourceStem.endsWith("_pillar")
                    ? StructureBlockDefinition.Shape.PILLAR
                    : StructureBlockDefinition.Shape.CUBE;

            add(result, material, registryName, titleCase(registryName), shape,
                    sourceFile, topFile, bottomFile, null, null, fixedPart, null);

            if (shape == StructureBlockDefinition.Shape.CUBE) {
                String dedicatedSlab = fileByStem.get(sourceStem + "_slab");
                String slabTexture = dedicatedSlab == null ? sourceFile : dedicatedSlab;

                add(result, material, registryName + "_slab", titleCase(registryName) + " Slab",
                        StructureBlockDefinition.Shape.SLAB, slabTexture, topFile, bottomFile, null,
                        registryName, null, root ? StructureMaterialPart.BLOCK : null);
                add(result, material, registryName + "_stairs", titleCase(registryName) + " Stairs",
                        StructureBlockDefinition.Shape.STAIRS, sourceFile, topFile, bottomFile, null,
                        registryName, null, root ? StructureMaterialPart.BLOCK : null);
                add(result, material, registryName + "_wall", titleCase(registryName) + " Wall",
                        StructureBlockDefinition.Shape.WALL, sourceFile, topFile, bottomFile, null,
                        registryName, null, root ? StructureMaterialPart.BLOCK : null);
            }
        }
        return result;
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
        String baseId = material.hasExistingPart(StructureMaterialPart.BLOCK)
                ? material.existingPart(StructureMaterialPart.BLOCK).getPath()
                : material.id() + "_block";

        add(result, material, StructureMaterialPart.SLAB.registryName(material),
                StructureMaterialPart.SLAB.readableName(material),
                StructureBlockDefinition.Shape.SLAB,
                "quartz_block_side.png", "quartz_block_top.png", "quartz_block_bottom.png", null,
                baseId, StructureMaterialPart.SLAB, StructureMaterialPart.BLOCK);
        add(result, material, StructureMaterialPart.STAIRS.registryName(material),
                StructureMaterialPart.STAIRS.readableName(material),
                StructureBlockDefinition.Shape.STAIRS,
                "quartz_block_side.png", "quartz_block_top.png", "quartz_block_bottom.png", null,
                baseId, StructureMaterialPart.STAIRS, StructureMaterialPart.BLOCK);
        add(result, material, StructureMaterialPart.WALL.registryName(material),
                StructureMaterialPart.WALL.readableName(material),
                StructureBlockDefinition.Shape.WALL,
                "quartz_block_side.png", "quartz_block_top.png", "quartz_block_bottom.png", null,
                baseId, StructureMaterialPart.WALL, StructureMaterialPart.BLOCK);

        add(result, material, StructureMaterialPart.PILLAR.registryName(material),
                StructureMaterialPart.PILLAR.readableName(material),
                StructureBlockDefinition.Shape.PILLAR,
                "quartz_pillar.png", "quartz_pillar_top.png", "quartz_pillar_top.png", null,
                null, StructureMaterialPart.PILLAR, null);
        add(result, material, StructureMaterialPart.CHISELED_BLOCK.registryName(material),
                StructureMaterialPart.CHISELED_BLOCK.readableName(material),
                StructureBlockDefinition.Shape.CUBE,
                "chiseled_quartz_block.png", "chiseled_quartz_block_top.png", "chiseled_quartz_block_top.png", null,
                null, StructureMaterialPart.CHISELED_BLOCK, null);
        add(result, material, StructureMaterialPart.BRICKS.registryName(material),
                StructureMaterialPart.BRICKS.readableName(material),
                StructureBlockDefinition.Shape.CUBE,
                "quartz_bricks.png", null, null, null,
                null, StructureMaterialPart.BRICKS, null);

        String smoothId = StructureMaterialPart.SMOOTH_BLOCK.registryName(material);
        add(result, material, smoothId,
                StructureMaterialPart.SMOOTH_BLOCK.readableName(material),
                StructureBlockDefinition.Shape.CUBE,
                "quartz_block_bottom.png", null, null, null,
                null, StructureMaterialPart.SMOOTH_BLOCK, null);
        add(result, material, StructureMaterialPart.SMOOTH_SLAB.registryName(material),
                StructureMaterialPart.SMOOTH_SLAB.readableName(material),
                StructureBlockDefinition.Shape.SLAB,
                "quartz_block_bottom.png", null, null, null,
                smoothId, StructureMaterialPart.SMOOTH_SLAB, StructureMaterialPart.SMOOTH_BLOCK);
        add(result, material, StructureMaterialPart.SMOOTH_STAIRS.registryName(material),
                StructureMaterialPart.SMOOTH_STAIRS.readableName(material),
                StructureBlockDefinition.Shape.STAIRS,
                "quartz_block_bottom.png", null, null, null,
                smoothId, StructureMaterialPart.SMOOTH_STAIRS, StructureMaterialPart.SMOOTH_BLOCK);

        return result;
    }

    private static boolean isStoneAuxiliary(String stem) {
        return stem.endsWith("_connected")
                || stem.endsWith("_top")
                || stem.endsWith("_bottom");
    }

    private static String replaceFamily(String sourceStem, String family, String materialId) {
        if (!sourceStem.contains(family)) {
            return materialId + "_" + sourceStem;
        }
        return sourceStem.replace(family, materialId);
    }

    private static String titleCase(String id) {
        StringBuilder result = new StringBuilder();
        for (String word : id.split("_")) {
            if (word.isEmpty()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                result.append(word.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return result.toString();
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
            StructureMaterialPart part,
            StructureMaterialPart basePart
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
