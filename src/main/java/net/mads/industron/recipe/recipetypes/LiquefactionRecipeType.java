package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

public final class LiquefactionRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("liquefaction"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Liquefaction"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(0, 0, 1, 1))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private LiquefactionRecipeType() {
    }
}
