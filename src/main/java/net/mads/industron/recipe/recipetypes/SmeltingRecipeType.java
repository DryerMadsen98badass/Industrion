package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

public final class SmeltingRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("smelting"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Smelting"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(4, 4, 2, 2))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private SmeltingRecipeType() {
    }
}
