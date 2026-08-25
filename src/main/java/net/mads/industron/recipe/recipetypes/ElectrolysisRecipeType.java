package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

public final class ElectrolysisRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("electrolysis"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Electrolysis"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(9, 9, 3, 3))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private ElectrolysisRecipeType() {
    }
}
