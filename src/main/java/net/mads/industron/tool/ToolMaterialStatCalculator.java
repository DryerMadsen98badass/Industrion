package net.mads.industron.tool;

import net.mads.industron.machine.MachineTier;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.material.CompoundMaterialPropertyCalculator;
import net.mads.industron.material.MaterialComponent;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.MaterialProperties;
import net.mads.industron.material.structure.StructureMaterial;

/**
 * Deterministic tool physics derived from material definitions.
 * No individual material receives a hand-written tool stat table.
 */
public final class ToolMaterialStatCalculator {
    private ToolMaterialStatCalculator() {
    }

    public static ToolPartStats calculate(IndustrialSubstance material, MaterialPart part) {
        if (!ToolMaterialRules.allows(material, part)) {
            throw new IllegalArgumentException("Material " + material.id() + " cannot form tool part " + part);
        }

        MechanicalProfile profile = profile(material);
        PartRole role = role(part);

        // Durability is deliberately controlled by repeated-load properties rather than hardness alone.
        double lifeScore = weighted(
                profile.wear(), 0.31D,
                profile.fatigue(), 0.29D,
                profile.toughness(), 0.20D,
                profile.tensile(), 0.12D,
                profile.structural(), 0.08D
        );
        int durability = Math.max(1, (int) Math.round((120.0D + lifeScore * 8.0D) * role.durabilityWeight));

        // Efficiency is seconds per completed use. Better materials therefore contribute fewer seconds.
        double workQuality = weighted(
                profile.hardness(), 0.25D,
                profile.wear(), 0.20D,
                profile.toughness(), 0.15D,
                profile.structural(), 0.20D,
                profile.machinability(), 0.20D
        );
        double normalizedQuality = Math.max(0.0D, workQuality);
        double efficiency = role.baseEfficiencySeconds * (105.0D / (105.0D + normalizedQuality));
        efficiency = clamp(efficiency, role.minimumEfficiencySeconds, role.maximumEfficiencySeconds);

        double impactScore = weighted(
                profile.hardness(), 0.42D,
                profile.structural(), 0.26D,
                profile.tensile(), 0.18D,
                profile.toughness(), 0.14D
        );
        double damage = Math.max(0.0D, role.damageWeight * (0.35D + impactScore / 70.0D));

        return new ToolPartStats(
                ToolMaterialRules.tier(material),
                durability,
                round2(efficiency),
                round2(damage)
        );
    }

    /**
     * Minimum load a rivet must individually survive for a working tool part made from this material.
     * Uses the same intrinsic failure envelope as PracticalCapabilityResolver#fastenerLoad so the
     * candidate rivet and the captured head are compared on the same physical scale.
     */
    public static double fastenerRequirement(IndustrialSubstance material) {
        MechanicalProfile profile = profile(material);
        return Math.min(
                profile.structural(),
                Math.min(profile.tensile(), profile.yield())
        );
    }

    public static MechanicalProfile profile(IndustrialSubstance material) {
        if (material instanceof IndustrialMaterial industrial) {
            MaterialProperties p = industrial.properties();
            return new MechanicalProfile(
                    p.hardness(),
                    p.tensileStrength(),
                    p.yieldStrength(),
                    p.fractureToughness(),
                    p.wearResistance(),
                    p.fatigueResistance(),
                    p.structuralStrength(),
                    p.machinability()
            );
        }
        if (material instanceof StructureMaterial structure) {
            return structureProfile(structure);
        }
        throw new IllegalArgumentException("Unsupported tool material: " + material.id());
    }

    /**
     * Wood/stone have no MaterialProperties object of their own. Their mechanical identity is therefore
     * derived from the same .contains(...) materials already declared in material/defenitions, then scaled
     * by their structure family. Wood remains the weakest ULV family; stone is a harder but more brittle LV family.
     */
    private static MechanicalProfile structureProfile(StructureMaterial material) {
        if (material.components().isEmpty()) {
            return ToolMaterialRules.kind(material) == ToolMaterialRules.Kind.WOOD
                    ? new MechanicalProfile(18, 22, 18, 30, 16, 24, 20, 75)
                    : new MechanicalProfile(55, 24, 20, 18, 42, 18, 48, 35);
        }

        double total = material.components().stream().mapToDouble(MaterialComponent::amount).sum();
        if (!(total > 0.0D)) total = material.components().size();

        double hardness = 0, tensile = 0, yield = 0, toughness = 0, wear = 0, fatigue = 0, structural = 0, machinability = 0;
        for (MaterialComponent component : material.components()) {
            double weight = component.amount() / total;
            // .contains(...) is universal in Industron. Resolve the concrete definition through
            // the same property path used by compound materials instead of assuming every
            // component is already an IndustrialMaterial.
            MaterialProperties p = CompoundMaterialPropertyCalculator.propertiesFor(component.substance());
            hardness += p.hardness() * weight;
            tensile += p.tensileStrength() * weight;
            yield += p.yieldStrength() * weight;
            toughness += p.fractureToughness() * weight;
            wear += p.wearResistance() * weight;
            fatigue += p.fatigueResistance() * weight;
            structural += p.structuralStrength() * weight;
            machinability += p.machinability() * weight;
        }

        return switch (ToolMaterialRules.kind(material)) {
            case WOOD -> new MechanicalProfile(
                    cap(hardness * 0.18D, 8, 30),
                    cap(tensile * 0.20D, 10, 35),
                    cap(yield * 0.20D, 8, 32),
                    cap(toughness * 0.28D, 14, 42),
                    cap(wear * 0.14D, 8, 28),
                    cap(fatigue * 0.24D, 12, 38),
                    cap(structural * 0.20D, 10, 34),
                    cap(55.0D + machinability * 0.20D, 55, 90)
            );
            case STONE -> new MechanicalProfile(
                    cap(hardness * 0.52D, 35, 85),
                    cap(tensile * 0.24D, 12, 42),
                    cap(yield * 0.22D, 10, 38),
                    cap(toughness * 0.20D, 10, 34),
                    cap(wear * 0.46D, 30, 75),
                    cap(fatigue * 0.20D, 10, 34),
                    cap(structural * 0.42D, 28, 70),
                    cap(machinability * 0.35D, 15, 50)
            );
            default -> throw new IllegalArgumentException("Structure material is not a tool material: " + material.id());
        };
    }

    private static PartRole role(MaterialPart part) {
        return switch (part) {
            case SWORD_BLADE -> new PartRole(1.50D, 2.00D, 0.30D, 3.75D, 3.20D);
            case TOOL_HANDLE, SAW_HANDLE -> new PartRole(0.80D, 3.00D, 0.45D, 5.0D, 0.12D);
            case SIFTER_FRAME -> new PartRole(0.90D, 2.80D, 0.50D, 5.5D, 0.0D);
            case WIRE_CUTTER_BODY -> new PartRole(0.90D, 2.50D, 0.40D, 4.5D, 0.18D);
            case TOOL_HEAD_MALLET -> new PartRole(1.05D, 2.15D, 0.35D, 4.0D, 0.55D);
            case TOOL_HEAD_PESTLE -> new PartRole(1.10D, 1.75D, 0.25D, 3.25D, 0.30D);
            case TOOL_HEAD_PICKAXE, TOOL_HEAD_AXE, TOOL_HEAD_SHOVEL, TOOL_HEAD_HOE,
                    TOOL_HEAD_HAMMER, TOOL_HEAD_FILE, TOOL_HEAD_SCREWDRIVER,
                    TOOL_HEAD_CROWBAR, TOOL_HEAD_WIRE_CUTTER, TOOL_HEAD_DRILL,
                    SAW_BLADE, WRENCH -> new PartRole(1.20D, 2.00D, 0.30D, 3.75D, 1.0D);
            default -> new PartRole(1.0D, 2.25D, 0.35D, 4.0D, 0.65D);
        };
    }

    private static double weighted(double... pairs) {
        double sum = 0.0D;
        double weights = 0.0D;
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            sum += pairs[i] * pairs[i + 1];
            weights += pairs[i + 1];
        }
        return weights <= 0.0D ? 0.0D : sum / weights;
    }

    private static double cap(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double round2(double value) {
        return Math.round(value * 100.0D) / 100.0D;
    }

    public record MechanicalProfile(
            double hardness,
            double tensile,
            double yield,
            double toughness,
            double wear,
            double fatigue,
            double structural,
            double machinability
    ) {
    }

    private record PartRole(
            double durabilityWeight,
            double baseEfficiencySeconds,
            double minimumEfficiencySeconds,
            double maximumEfficiencySeconds,
            double damageWeight
    ) {
    }
}
