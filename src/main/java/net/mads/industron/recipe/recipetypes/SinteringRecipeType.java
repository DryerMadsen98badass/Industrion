package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

public final class SinteringRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("sintering"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Sintering"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(6, 2, 1, 1))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private SinteringRecipeType() {
    }
}
