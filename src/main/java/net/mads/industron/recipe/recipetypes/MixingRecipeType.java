package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

public final class MixingRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("mixing"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Mixing"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(6, 4, 4, 4))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private MixingRecipeType() {
    }
}
