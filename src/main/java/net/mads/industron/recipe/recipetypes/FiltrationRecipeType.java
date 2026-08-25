package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

public final class FiltrationRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("filtration"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Filtration"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(4, 6, 3, 3))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private FiltrationRecipeType() {
    }
}
