package net.mads.industron.material.plant;

import net.mads.industron.material.MaterialComponent;
import net.mads.industron.material.chemistry.ChemistryPhase;
import net.mads.industron.material.defenitions.CompoundMaterials;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Deterministic route arithmetic and on-demand plant-intermediate planning. */
public final class PlantProcessingPlanner {
    public static final String BIOMASS = "biomass";
    public static final String RESIDUE = "residue";
    public static final String FERTILIZER = "fertilizer";

    /**
     * Exact whole-item separation batch.
     *
     * <p>For factor-1 route biomass, one solid material unit is one item. If a composition contains
     * eight declared component units, eight biomass items split into eight whole-item component
     * fractions. Raw forms with a different physical-content relation are scaled explicitly by
     * {@link #fiberBatch(PlantMaterial, PlantPart)}.</p>
     */
    public record FiberBatch(int inputCount, int fiberCount, int residueCount) {
        public FiberBatch {
            if (inputCount <= 0 || fiberCount <= 0 || residueCount < 0) {
                throw new IllegalArgumentException("Invalid plant fibre batch");
            }
        }
    }

    private PlantProcessingPlanner() {
    }

    /**
     * The complete declared .contains(...) ratio is the exact separation batch.
     *
     * <p>Example: 2 Sylvara + 6 other component units means
     * 8 source items -> 2 fibre items + 6 residue items.</p>
     */
    public static FiberBatch fiberBatch(PlantMaterial material) {
        int fiber = PlantMaterialGenerator.sylvaraAmount(material);
        int residue = PlantMaterialGenerator.residueAmount(material);
        if (fiber <= 0) {
            throw new IllegalArgumentException("Plant material has no Sylvara fibre fraction: " + material.id());
        }
        return new FiberBatch(fiber + residue, fiber, residue);
    }

    /**
     * Exact hand-separation batch for a concrete raw form. Plant parts do not change the meaning
     * of one material unit: one solid item is one material unit for every part. Physical shape
     * relationships (slab/stair/bale/etc.) are used by fuel/value rules only, not by chemistry.
     */
    public static FiberBatch fiberBatch(PlantMaterial material, PlantPart sourcePart) {
        if (sourcePart == null || !sourcePart.fiberSource()) {
            throw new IllegalArgumentException(
                    "Plant part " + sourcePart + " is not a fibre source for " + material.id()
            );
        }
        return fiberBatch(material);
    }

    /**
     * Returns only intermediates used by selected generated routes.
     *
     * <p>This is deliberately route-driven: no slurry, extract, gas, biomass or other state is
     * registered merely because it could theoretically exist. Add a state here only when an
     * actual generated route consumes or produces it.</p>
     */
    public static List<PlantProcessIntermediate> requiredIntermediates(PlantMaterial material) {
        if (!PlantMaterialGenerator.hasProcessSource(material)) return List.of();

        Map<String, PlantProcessIntermediate> result = new LinkedHashMap<>();

        // Generic mechanical preparation route. It is currently solid because the selected
        // cutting/grinding routes produce a solid worked biomass state.
        put(result, createIntermediate(
                material,
                BIOMASS,
                "Biomass",
                ChemistryPhase.SOLID,
                material.components(),
                "item/plants/intermediates/biomass"
        ));

        // Vanilla Composter is the primitive decomposition route. Fertilizer is useful directly
        // and keeps the complete declared composition 1:1; it is not a dead-end "compost" item.
        put(result, createIntermediate(
                material,
                FERTILIZER,
                "Fertilizer",
                ChemistryPhase.SOLID,
                material.components(),
                "item/plants/intermediates/fertilizer"
        ));

        if (PlantMaterialGenerator.hasFiberFraction(material) && PlantMaterialGenerator.residueAmount(material) > 0) {
            List<MaterialComponent> residue = material.components().stream()
                    .filter(component -> !component.substance().id().equals(CompoundMaterials.SYLVARA.id()))
                    .toList();
            put(result, createIntermediate(
                    material,
                    RESIDUE,
                    "Plant Residue",
                    ChemistryPhase.SOLID,
                    residue,
                    "item/plants/intermediates/residue"
            ));
        }

        return List.copyOf(result.values());
    }

    /**
     * General route-state factory used by current and future planners.
     *
     * <p>A route may request SOLID, LIQUID or GAS and any deterministic suffix/display name.
     * The returned state always carries an explicit .contains(...) composition.</p>
     */
    public static PlantProcessIntermediate createIntermediate(
            PlantMaterial material,
            String suffix,
            String displaySuffix,
            ChemistryPhase phase,
            List<MaterialComponent> components,
            String solidTexture
    ) {
        if (phase == ChemistryPhase.SOLID) {
            return PlantProcessIntermediate.solid(material, suffix, displaySuffix, components, solidTexture);
        }
        if (phase == ChemistryPhase.LIQUID || phase == ChemistryPhase.GAS) {
            return PlantProcessIntermediate.fluid(material, suffix, displaySuffix, phase, components);
        }
        throw new IllegalArgumentException("Unsupported plant intermediate phase: " + phase);
    }

    public static PlantProcessIntermediate requireIntermediate(PlantMaterial material, String suffix) {
        return requiredIntermediates(material).stream()
                .filter(intermediate -> intermediate.suffix().equals(suffix))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Plant route requested unused intermediate " + material.id() + "_" + suffix
                ));
    }

    public static ResourceLocation intermediateId(PlantMaterial material, String suffix) {
        return requireIntermediate(material, suffix).registryId();
    }

    public static List<PlantProcessIntermediate> allRequiredIntermediates() {
        List<PlantProcessIntermediate> result = new ArrayList<>();
        for (var material : net.mads.industron.material.defenitions.PlantMaterials.ALL) {
            result.addAll(requiredIntermediates(material));
        }
        return List.copyOf(result);
    }

    public static int handFiberUses(PlantMaterial material, PlantPart sourcePart) {
        return Math.max(2, fiberBatch(material, sourcePart).inputCount() * 2);
    }

    public static int stringUses() {
        return 4;
    }

    public static int mechanicalPreparationTicks(PlantMaterial material, PlantPart sourcePart) {
        int base = Math.max(2, PlantMaterialGenerator.totalCompositionAmount(material) / 2);
        int complexity = switch (sourcePart) {
            case SEEDS, ROOT, FRUIT, DRIED -> base + 1;
            case BALE, COMPRESSED_BLOCK, BLOCK, FRUIT_BLOCK -> base + 2;
            default -> base;
        };
        return 20 * complexity;
    }

    public static int createPreparationTicks(PlantMaterial material, PlantPart sourcePart) {
        return mechanicalPreparationTicks(material, sourcePart);
    }

    public static int washingTicks(PlantMaterial material) {
        return 20 * Math.max(3, fiberBatch(material).inputCount());
    }

    public static int compostTicks(PlantMaterial material) {
        int resyra = material.components().stream()
                .filter(component -> component.substance().id().equals(CompoundMaterials.RESYRA.id()))
                .mapToInt(MaterialComponent::amount)
                .sum();
        return 20 * (30 + resyra * 10);
    }

    /** Plant parts that are physically better represented by milling/grinding than chopping. */
    public static boolean usesGrindingPreparation(PlantPart part) {
        return switch (part) {
            case CROP, SEEDS, ROOT, FRUIT, DRIED -> true;
            default -> false;
        };
    }


    private static void put(Map<String, PlantProcessIntermediate> map, PlantProcessIntermediate intermediate) {
        PlantProcessIntermediate previous = map.putIfAbsent(intermediate.suffix(), intermediate);
        if (previous != null && !previous.equals(intermediate)) {
            throw new IllegalStateException("Conflicting plant intermediate: " + intermediate.id());
        }
    }
}
