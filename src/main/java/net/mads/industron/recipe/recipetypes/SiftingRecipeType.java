package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

public final class SiftingRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("sifting"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Sifting"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(1, 6, 0, 0))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private SiftingRecipeType() {
    }
}
