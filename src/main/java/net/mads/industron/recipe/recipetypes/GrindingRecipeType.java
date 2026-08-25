package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

public final class GrindingRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("grinding"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Grinding"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(1, 3, 0, 0))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private GrindingRecipeType() {
    }
}
