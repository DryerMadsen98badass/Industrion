package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

public final class QuenchingRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("quenching"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Quenching"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(2, 2, 2, 2))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private QuenchingRecipeType() {
    }
}
