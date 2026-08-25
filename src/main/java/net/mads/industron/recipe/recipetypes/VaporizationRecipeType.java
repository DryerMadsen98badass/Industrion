package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

public final class VaporizationRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("vaporization"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Vaporization"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(0, 0, 1, 1))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private VaporizationRecipeType() {
    }
}
