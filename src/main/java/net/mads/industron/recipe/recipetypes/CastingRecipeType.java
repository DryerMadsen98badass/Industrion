package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

public final class CastingRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("casting"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Casting"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(1, 2, 2, 1))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private CastingRecipeType() {
    }
}
