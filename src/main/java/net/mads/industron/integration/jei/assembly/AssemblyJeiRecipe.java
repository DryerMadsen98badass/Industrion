package net.mads.industron.integration.jei.assembly;

import net.mads.industron.recipe.recipetypes.AssemblyRecipeDefinition;

import java.util.Objects;

/** One final assembly product recipe. Component definitions use their own JEI category. */
public record AssemblyJeiRecipe(AssemblyRecipeDefinition recipe) {
    public AssemblyJeiRecipe {
        Objects.requireNonNull(recipe, "recipe");
    }

    public static AssemblyJeiRecipe recipe(AssemblyRecipeDefinition recipe) {
        return new AssemblyJeiRecipe(recipe);
    }
}
