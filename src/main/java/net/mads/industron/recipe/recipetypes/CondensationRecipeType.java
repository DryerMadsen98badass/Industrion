package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

public final class CondensationRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("condensation"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Condensation"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(0, 0, 1, 1))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private CondensationRecipeType() {
    }
}
