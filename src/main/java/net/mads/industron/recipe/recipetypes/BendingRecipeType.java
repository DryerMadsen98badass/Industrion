package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

public final class BendingRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("bending"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Bending"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(1, 1, 0, 0))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private BendingRecipeType() {
    }
}
