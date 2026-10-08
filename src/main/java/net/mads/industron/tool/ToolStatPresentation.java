package net.mads.industron.tool;

import net.mads.industron.material.MaterialPart;
import net.mads.industron.recipe.recipetypes.assembly.ToolDefinition;
import net.mads.industron.recipe.recipes.assembly.ToolDefinitions;

/** Show a value only for families whose gameplay uses it. Shared handles show their possible contributions. */
public final class ToolStatPresentation {
    public static boolean showsEfficiency(ToolDefinition definition) {
        return !EquipmentStats.isEquipment(definition.id()) && !definition.id().equals("sword");
    }
    public static boolean showsDamage(ToolDefinition definition) {
        if (EquipmentStats.isEquipment(definition.id())) return false;
        return switch (definition.id()) {
            case "shears", "sifter", "pestle", "snap_ring_pliers", "bearing_press", "clamp", "crimping_tool", "gear_cutter" -> false;
            default -> true;
        };
    }
    public static boolean showsEfficiency(MaterialPart part) {
        return ToolDefinitions.ALL.stream().anyMatch(d -> showsEfficiency(d) && d.parts().stream().anyMatch(p -> p.part() == part));
    }
    public static boolean showsDamage(MaterialPart part) {
        return ToolDefinitions.ALL.stream().anyMatch(d -> showsDamage(d) && d.parts().stream().anyMatch(p -> p.part() == part));
    }
    private ToolStatPresentation() { }
}
