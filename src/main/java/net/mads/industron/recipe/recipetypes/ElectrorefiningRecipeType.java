package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

public final class ElectrorefiningRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("electrorefining"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Electrorefining"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(4, 4, 3, 3))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private ElectrorefiningRecipeType() {
    }
}
