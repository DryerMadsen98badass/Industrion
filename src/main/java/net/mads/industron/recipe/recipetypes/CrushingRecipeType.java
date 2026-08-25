package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

public final class CrushingRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("crushing"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Crushing"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(1, 4, 0, 0))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private CrushingRecipeType() {
    }
}
