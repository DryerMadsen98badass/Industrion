package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

public final class AssemblingRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("assembling"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Assembling"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(3, 1, 0, 0))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private AssemblingRecipeType() {
    }
}
