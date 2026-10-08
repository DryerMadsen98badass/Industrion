package net.mads.industron.material;

import net.mads.industron.material.structure.StoneMaterial;
import net.mads.industron.material.structure.StructureMaterial;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/** Deterministic clay rules shared by recipes, tooltips and later thermal casings. */
public final class ClayMaterialRules {
    public static final int WATER_PER_CLAY_MB = 250;
    public static final int CLAY_PER_BLOCK = 4;
    public static final int BRICKS_PER_BLOCK = 32;
    public static final int BRICKS_PER_SLAB = BRICKS_PER_BLOCK / 2;
    public static final int BRICKS_PER_STAIRS = BRICKS_PER_BLOCK * 3 / 4;
    public static final int BRICKS_PER_WALL = BRICKS_PER_BLOCK;

    public static final Set<MaterialPart> FORMS = Set.copyOf(EnumSet.of(
            MaterialPart.TINY_DUST,
            MaterialPart.SMALL_DUST,
            MaterialPart.DUST,
            MaterialPart.CLAY,
            MaterialPart.CLAY_BLOCK,
            MaterialPart.UNFIRED_BRICK,
            MaterialPart.DRIED_UNFIRED_BRICK,
            MaterialPart.BRICK,
            MaterialPart.CRACKED_BRICK,
            MaterialPart.BRICKS,
            MaterialPart.FIREBOX,
            MaterialPart.BRICK_SLAB,
            MaterialPart.BRICK_STAIRS,
            MaterialPart.BRICK_WALL
    ));

    public static final int MOLD_CLAY_COUNT = 4;
    public static final int MOLDING_TOOL_USES = 1;
    public static final int BASIN_MIXING_TOOL_USES = 12;
    public static final int MOLDING_DURATION_TICKS = 20 * 12;
    public static final int SUN_DRYING_DURATION_TICKS = 20 * 120;
    public static final int KILN_FIRING_DURATION_TICKS = 20 * 10;
    public static final int KILN_OVERFIRE_DURATION_TICKS = 20 * 20;

    private ClayMaterialRules() {
    }

    /** Firing begins below the ceramic's operating limit; no melting property is required. */
    public static int firingTemperature(IndustrialMaterial material) {
        requireFiringCeramic(material);
        int maximum = material.properties().maxOperatingTemperature();
        double operatingKelvin = Math.max(274.15D, maximum + 273.15D);
        int calculated = (int) Math.round(operatingKelvin * 0.72D - 273.15D);
        return Math.max(100, Math.min(maximum - 1, calculated));
    }

    /** Gameplay block-breaking strength for fired ceramic; separate from the non-existent Hardness property. */
    public static float brickDestroyTime(IndustrialMaterial material) {
        requireClay(material);
        return Math.max(1.5F, Math.min(12.0F, 1.5F + (material.properties().tierMultiplier() - 1) * 0.75F));
    }

    /** Later Foundry code can accept a brick casing only while its calculated limit is high enough. */
    public static boolean canContainTemperature(IndustrialMaterial material, int temperature) {
        return material.isClayMaterial()
                && material.has(MaterialPart.BRICKS)
                && temperature >= 0
                && material.properties().maxOperatingTemperature() >= temperature;
    }

    /** Clay tier is a hard natural-spawn dimension gate. Composition only chooses geology inside it. */
    public static MaterialOrePolicy.DimensionBand worldgenDimension(IndustrialMaterial material) {
        requireClay(material);
        return MaterialOrePolicy.dimensionForTier(material.tier());
    }

    /**
     * 0..10000 affinity between a clay composition and one local StoneMaterial host.
     * Direct StoneMaterial components are strongest; IndustrialMaterial trace components reuse
     * the exact ore/stone chemistry-affinity system already used by geology.
     */
    public static int stoneAffinity(IndustrialMaterial clay, MaterialOreHost host) {
        requireClay(clay);
        if (host == null || worldgenDimension(clay) != host.dimension()) return 0;

        long weighted = 0L;
        int total = 0;
        for (MaterialComponent component : clay.components()) {
            int amount = Math.max(1, component.amount());
            int score = 0;
            if (component.substance() instanceof StoneMaterial stone) {
                if (stone.id().equals(host.id())) {
                    score = 10_000;
                } else {
                    score = leafAffinity(stone, host.stone());
                }
            } else if (component.substance() instanceof IndustrialMaterial material) {
                score = host.affinity(material);
            }
            weighted += (long) amount * score;
            total += amount;
        }
        return total <= 0 ? 0 : (int) Math.min(10_000L, weighted / total);
    }

    /**
     * 0..10000 overlap between the clay composition and one ore/mineral material in the region.
     * Clay .contains(...) amounts remain real selection weights here, just like they are for the
     * stone-host score.
     */
    public static int materialAffinity(IndustrialMaterial clay, IndustrialMaterial material) {
        requireClay(clay);
        if (material == null) return 0;

        long weighted = 0L;
        int total = 0;
        for (MaterialComponent component : clay.components()) {
            int amount = Math.max(1, component.amount());
            weighted += (long) amount * leafAffinity(component.substance(), material);
            total += amount;
        }
        return total <= 0 ? 0 : (int) Math.min(10_000L, weighted / total);
    }

    private static int leafAffinity(IndustrialSubstance left, IndustrialSubstance right) {
        Set<String> a = leafIds(left);
        Set<String> b = leafIds(right);
        if (a.isEmpty() || b.isEmpty()) return 0;
        int shared = 0;
        for (String id : a) if (b.contains(id)) shared++;
        if (shared == 0) return 0;
        int union = a.size() + b.size() - shared;
        return Math.max(1, Math.min(10_000, (int) Math.round((double) shared / union * 10_000.0D)));
    }

    private static Set<String> leafIds(IndustrialSubstance substance) {
        Set<String> result = new HashSet<>();
        collectLeaves(substance, result, new HashSet<>());
        return Set.copyOf(result);
    }

    private static void collectLeaves(IndustrialSubstance substance, Set<String> output, Set<String> stack) {
        if (substance == null || !stack.add(substance.id())) return;
        try {
            if (substance instanceof IndustrialMaterial material && !material.components().isEmpty()) {
                for (MaterialComponent component : material.components()) {
                    collectLeaves(component.substance(), output, stack);
                }
            } else if (substance instanceof StructureMaterial structure && !structure.components().isEmpty()) {
                for (MaterialComponent component : structure.components()) {
                    collectLeaves(component.substance(), output, stack);
                }
            } else {
                output.add(substance.id());
            }
        } finally {
            stack.remove(substance.id());
        }
    }

    private static void requireFiringCeramic(IndustrialMaterial material) {
        if (material == null || (!material.isClayMaterial() && !material.isCeramicBrickMaterial())) {
            throw new IllegalArgumentException("Firing rule requires a clay or ceramic-brick material");
        }
    }

    private static void requireClay(IndustrialMaterial material) {
        if (material == null || !material.isClayMaterial()) {
            throw new IllegalArgumentException("Clay rule requires a clay material");
        }
    }
}
