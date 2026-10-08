package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

/** Tierless recipes performed directly by the player with no machine or tool. */
public final class HandProcessingRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("hand_processing"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Hand Processing"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(1, 2, 0, 0))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.ignoreTier())
            .build();

    private HandProcessingRecipeType() {
    }
}
