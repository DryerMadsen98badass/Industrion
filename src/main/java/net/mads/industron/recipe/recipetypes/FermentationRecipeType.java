package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

public final class FermentationRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("fermentation"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Fermentation"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(4, 4, 3, 3))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private FermentationRecipeType() {
    }
}
