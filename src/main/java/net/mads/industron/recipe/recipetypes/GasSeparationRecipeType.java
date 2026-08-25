package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

public final class GasSeparationRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("gas_separation"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Gas Separation"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(0, 0, 3, 6))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private GasSeparationRecipeType() {
    }
}
