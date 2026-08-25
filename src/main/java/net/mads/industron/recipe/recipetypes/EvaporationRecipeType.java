package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

public final class EvaporationRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("evaporation"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Evaporation"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(1, 2, 2, 2))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private EvaporationRecipeType() {
    }
}
