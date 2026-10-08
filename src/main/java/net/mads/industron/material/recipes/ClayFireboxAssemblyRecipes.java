package net.mads.industron.material.recipes;

import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.defenitions.ClayMaterials;
import net.mads.industron.recipe.recipes.assembly.MaterialType;
import net.mads.industron.recipe.recipes.assembly.Stats;
import net.mads.industron.recipe.recipes.assembly.WorkbenchLevels;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyRecipeDefinition;

import java.util.ArrayList;
import java.util.List;

/** Assembly acquisition for every generated ClayMaterial Firebox. */
public final class ClayFireboxAssemblyRecipes {
    public static final List<AssemblyRecipeDefinition> ALL = buildAll();

    private ClayFireboxAssemblyRecipes() { }

    private static List<AssemblyRecipeDefinition> buildAll() {
        List<AssemblyRecipeDefinition> result = new ArrayList<>();
        for (IndustrialMaterial clay : ClayMaterials.ALL) {
            if (!clay.has(MaterialPart.BRICKS) || !clay.has(MaterialPart.FIREBOX)) continue;
            int requiredTemperature = clay.properties().maxOperatingTemperature();

            result.add(AssemblyRecipeDefinition.recipe(
                            "material/recipes/" + clay.id() + "_firebox"
                    )
                    .level(WorkbenchLevels.forTier(clay.tier()))
                    .baseBlockInput(MaterialPart.BRICKS, clay)
                    .input(MaterialPart.BARS, MaterialType.METAL, 4)
                        .stat(Stats.MELTING_POINT).atLeast(requiredTemperature)
                    .baseBlockOutput(MaterialPart.FIREBOX, clay)
                    .build());
        }
        return List.copyOf(result);
    }
}
