package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

public final class NeutralizationRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("neutralization"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Neutralization"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(4, 4, 4, 4))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private NeutralizationRecipeType() {
    }
}
