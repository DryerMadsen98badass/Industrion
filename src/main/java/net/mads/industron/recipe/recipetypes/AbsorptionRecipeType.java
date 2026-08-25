package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

public final class AbsorptionRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("absorption"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Absorption"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(2, 2, 4, 3))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private AbsorptionRecipeType() {
    }
}
