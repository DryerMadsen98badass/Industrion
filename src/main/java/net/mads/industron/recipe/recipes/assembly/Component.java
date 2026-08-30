package net.mads.industron.recipe.recipes.assembly;

import net.mads.industron.recipe.recipetypes.assembly.AssemblyComponent;

/** Public recursive component-tree names used by assembly recipes. */
public final class Component {
    public static final AssemblyComponent SCREW = component("screw", "Screw");
    public static final AssemblyComponent PLATE = component("plate", "Plate");
    public static final AssemblyComponent VERY_LONG_ROD = component("very_long_rod", "Very Long Rod");

    private Component() {
    }

    public static AssemblyComponent component(String id, String displayName) {
        return new AssemblyComponent(id, displayName);
    }
}
