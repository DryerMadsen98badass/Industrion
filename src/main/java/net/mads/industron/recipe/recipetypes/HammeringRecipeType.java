package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

public final class HammeringRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("hammering"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Hammering"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(2, 2, 0, 0))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private HammeringRecipeType() {
    }
}
