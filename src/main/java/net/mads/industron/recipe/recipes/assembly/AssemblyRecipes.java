package net.mads.industron.recipe.recipes.assembly;

import net.mads.industron.material.recipes.MaterialCasingAssemblyRecipes;
import net.mads.industron.material.recipes.MaterialFrameAssemblyRecipes;
import net.mads.industron.recipe.recipetypes.AssemblyRecipeDefinition;

import java.util.ArrayList;
import java.util.List;

/** Assembly product recipes generated from material-backed definitions. */
public final class AssemblyRecipes {
    public static final List<AssemblyRecipeDefinition> ALL = buildAll();

    private AssemblyRecipes() {
    }

    private static List<AssemblyRecipeDefinition> buildAll() {
        List<AssemblyRecipeDefinition> result = new ArrayList<>();
        result.addAll(MaterialFrameAssemblyRecipes.ALL);
        result.addAll(MaterialCasingAssemblyRecipes.ALL);
        return List.copyOf(result);
    }

    public static AssemblyRecipeDefinition find(String id) {
        return ALL.stream()
                .filter(recipe -> recipe.id().equals(id))
                .findFirst()
                .orElse(null);
    }
}
