package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

public final class WashingRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("washing"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Washing"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(1, 4, 1, 1))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private WashingRecipeType() {
    }
}
