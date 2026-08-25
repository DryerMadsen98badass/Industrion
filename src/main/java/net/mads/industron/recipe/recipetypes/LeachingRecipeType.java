package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

public final class LeachingRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("leaching"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Leaching"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(4, 4, 3, 3))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private LeachingRecipeType() {
    }
}
