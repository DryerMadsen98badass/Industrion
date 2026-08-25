package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

public final class CuttingRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("cutting"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Cutting"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(2, 4, 1, 1))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private CuttingRecipeType() {
    }
}
