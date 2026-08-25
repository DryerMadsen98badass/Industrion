package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

public final class MagnetizingRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("magnetizing"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Magnetizing"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(1, 1, 0, 0))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private MagnetizingRecipeType() {
    }
}
