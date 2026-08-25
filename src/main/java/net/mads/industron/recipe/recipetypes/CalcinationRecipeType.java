package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

public final class CalcinationRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("calcination"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Calcination"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(3, 3, 1, 2))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private CalcinationRecipeType() {
    }
}
