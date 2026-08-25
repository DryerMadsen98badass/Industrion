package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

public final class PyrolysisRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("pyrolysis"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Pyrolysis"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(4, 6, 3, 4))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private PyrolysisRecipeType() {
    }
}
