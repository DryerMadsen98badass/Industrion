package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

public final class RoastingRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("roasting"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Roasting"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(4, 4, 2, 2))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private RoastingRecipeType() {
    }
}
