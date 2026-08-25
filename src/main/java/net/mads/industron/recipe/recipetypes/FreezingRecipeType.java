package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

public final class FreezingRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("freezing"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Freezing"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(1, 1, 1, 1))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private FreezingRecipeType() {
    }
}
