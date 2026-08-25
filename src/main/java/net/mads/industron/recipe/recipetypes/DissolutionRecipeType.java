package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

public final class DissolutionRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("dissolution"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Dissolution"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(4, 2, 3, 2))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private DissolutionRecipeType() {
    }
}
