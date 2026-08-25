package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

public final class PrecisionMachiningRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("precision_machining"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Precision Machining"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(3, 1, 0, 0))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private PrecisionMachiningRecipeType() {
    }
}
