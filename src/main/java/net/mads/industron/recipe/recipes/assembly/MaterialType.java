package net.mads.industron.recipe.recipes.assembly;

import net.mads.industron.recipe.recipetypes.assembly.AssemblyMaterialSelector;

/**
 * Readable Assembly selectors backed by IndustrialMaterials.
 *
 * <p>METAL includes both elemental metals and true alloys. These are category selectors,
 * not hand-maintained material identity lists.</p>
 */
public final class MaterialType {
    public static final AssemblyMaterialSelector ANY = AssemblyMaterialSelector.ANY;
    public static final AssemblyMaterialSelector METAL = AssemblyMaterialSelector.METAL;
    public static final AssemblyMaterialSelector GEM = AssemblyMaterialSelector.GEM;
    public static final AssemblyMaterialSelector FLUID = AssemblyMaterialSelector.FLUID;
    public static final AssemblyMaterialSelector GAS = AssemblyMaterialSelector.GAS;
    /** Normal material-family selectors used by ordinary Assembly parts. */
    public static final AssemblyMaterialSelector WOOD = AssemblyMaterialSelector.WOOD;
    public static final AssemblyMaterialSelector STONE = AssemblyMaterialSelector.STONE;

    /** Tool-only selectors retain the stricter ToolMaterialRules semantics. */
    public static final AssemblyMaterialSelector TOOL_MATERIAL = AssemblyMaterialSelector.TOOL_MATERIAL;
    public static final AssemblyMaterialSelector TOOL_WOOD = AssemblyMaterialSelector.TOOL_WOOD;
    public static final AssemblyMaterialSelector TOOL_STONE = AssemblyMaterialSelector.TOOL_STONE;

    private MaterialType() {
    }
}
