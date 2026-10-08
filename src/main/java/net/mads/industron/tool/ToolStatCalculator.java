package net.mads.industron.tool;

import net.mads.industron.machine.MachineTier;
import net.mads.industron.machine.MachineTierStats;
import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.recipe.recipetypes.assembly.ToolDefinition;

import java.util.ArrayList;
import java.util.List;

/** Combines permanent part contributions into the final stats of a composed tool. */
public final class ToolStatCalculator {
    private ToolStatCalculator() {
    }

    public static ToolStats calculate(ToolDefinition definition, ToolStackData data) {
        if (definition == null || data == null) return null;

        List<ToolPartStats> parts = new ArrayList<>();
        for (ToolDefinition.PartSlot slot : definition.parts()) {
            String materialKey = data.materialKey(slot.role());
            IndustrialSubstance material = ToolMaterialResolver.resolve(materialKey);
            if (material == null || !ToolMaterialRules.allows(material, slot.part())) return null;
            parts.add(ToolMaterialStatCalculator.calculate(material, slot.part()));
        }
        if (parts.size() != definition.parts().size() || parts.isEmpty()) return null;
        if (EquipmentStats.isEquipment(definition.id())) {
            EquipmentStats equipment = EquipmentStats.calculate(definition, data);
            if (equipment == null) return null;
            var reference = ToolMaterialResolver.resolve(data.materialKey(definition.strengthReferenceRole()));
            return new ToolStats(ToolMaterialRules.tier(reference), equipment.durability(), equipment.drawTicks()/20.0, 1);
        }
        if (switch (definition.id()) {
            case "shears", "snap_ring_pliers", "bearing_press", "clamp", "crimping_tool", "gear_cutter" -> true;
            default -> false;
        }) {
            ToolStats combined = combine(parts);
            int weakestLife = parts.stream().mapToInt(ToolPartStats::durability).min().orElse(1);
            var reference = ToolMaterialResolver.resolve(data.materialKey(definition.strengthReferenceRole()));
            return new ToolStats(ToolMaterialRules.tier(reference), weakestLife,
                    combined.efficiencySeconds(), combined.damage());
        }
        return combine(parts);
    }

    public static ToolStats calculateSingle(IndustrialSubstance material, net.mads.industron.material.MaterialPart part) {
        if (material == null || part == null || !ToolMaterialRules.allows(material, part)) return null;
        return combine(List.of(ToolMaterialStatCalculator.calculate(material, part)));
    }

    public static ToolStats combine(List<ToolPartStats> parts) {
        if (parts == null || parts.isEmpty()) throw new IllegalArgumentException("A tool requires at least one stat-bearing part");

        long durability = 0L;
        double efficiency = 0.0D;
        double damage = 0.0D;
        double tierSum = 0.0D;

        for (ToolPartStats part : parts) {
            durability = Math.min(Integer.MAX_VALUE, durability + part.durability());
            efficiency += part.efficiencySeconds();
            damage += part.damage();
            tierSum += MachineTierStats.tierIndex(part.tier().recipeTier());
        }

        int tierIndex = (int) Math.round(tierSum / parts.size());
        tierIndex = Math.max(0, Math.min(MachineTier.ALL.size() - 1, tierIndex));
        return new ToolStats(
                MachineTier.ALL.get(tierIndex),
                Math.max(1, (int) durability),
                round2(efficiency),
                round2(damage)
        );
    }

    private static double round2(double value) {
        return Math.round(value * 100.0D) / 100.0D;
    }
}
