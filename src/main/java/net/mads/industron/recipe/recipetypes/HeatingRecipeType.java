package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

public final class HeatingRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("heating"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Heating"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(1, 1, 1, 1))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private HeatingRecipeType() {
    }
}
