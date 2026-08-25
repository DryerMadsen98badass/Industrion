package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

public final class PrecipitationRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("precipitation"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Precipitation"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(2, 4, 3, 2))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private PrecipitationRecipeType() {
    }
}
