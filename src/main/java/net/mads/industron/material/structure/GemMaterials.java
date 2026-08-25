package net.mads.industron.material.structure;

import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.IndustrialMaterials;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Calculated and reused vanilla gems in the independent Quartz-based decorative structure family. */
public final class GemMaterials {
    public static final List<GemMaterial> ALL = buildAll();

    private GemMaterials() {
    }

    private static List<GemMaterial> buildAll() {
        Map<String, GemMaterial> materials = new LinkedHashMap<>();

        for (IndustrialMaterial material : IndustrialMaterials.ALL) {
            if (!material.properties().gemCandidate() || !generatesStructures(material.id())) {
                continue;
            }
            materials.put(material.id(), create(material));
        }

        return List.copyOf(materials.values());
    }

    /**
     * Amethyst and actual Netherite may use gem-looking base blocks, but intentionally
     * do not expand into the automatic Quartz decorative structure family.
     */
    private static boolean generatesStructures(String materialId) {
        return !materialId.equals("amethyst") && !materialId.equals("netherite");
    }

    private static GemMaterial create(IndustrialMaterial material) {
        GemMaterial gem = new GemMaterial(material);

        // Quartz already owns these exact vanilla shapes. Reuse them rather than
        // creating Industron duplicates. Quartz Wall does not exist in vanilla,
        // so that one remains generated like every other gem wall.
        if (material.id().equals("quartz")) {
            gem = gem
                    .existing(StructureMaterialPart.SLAB, "minecraft:quartz_slab")
                    .existing(StructureMaterialPart.STAIRS, "minecraft:quartz_stairs")
                    .existing(StructureMaterialPart.PILLAR, "minecraft:quartz_pillar")
                    .existing(StructureMaterialPart.CHISELED_BLOCK, "minecraft:chiseled_quartz_block")
                    .existing(StructureMaterialPart.BRICKS, "minecraft:quartz_bricks")
                    .existing(StructureMaterialPart.SMOOTH_BLOCK, "minecraft:smooth_quartz")
                    .existing(StructureMaterialPart.SMOOTH_SLAB, "minecraft:smooth_quartz_slab")
                    .existing(StructureMaterialPart.SMOOTH_STAIRS, "minecraft:smooth_quartz_stairs");
        }

        return gem;
    }
}
