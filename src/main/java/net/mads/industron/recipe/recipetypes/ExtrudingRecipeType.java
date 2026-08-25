package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

public final class ExtrudingRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("extruding"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Extruding"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(2, 2, 1, 1))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private ExtrudingRecipeType() {
    }
}
