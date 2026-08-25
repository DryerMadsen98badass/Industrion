package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

public final class TestProcessingRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("test_processing"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Test Processing"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(1, 1, 1, 1))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private TestProcessingRecipeType() {
    }
}
