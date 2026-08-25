package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

public final class CoolingRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("cooling"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Cooling"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(1, 1, 1, 1))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private CoolingRecipeType() {
    }
}
