package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

public final class DryingRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("drying"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Drying"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(1, 2, 1, 1))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private DryingRecipeType() {
    }
}
