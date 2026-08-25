package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

public final class PolymerizationRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("polymerization"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Polymerization"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(6, 3, 4, 2))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private PolymerizationRecipeType() {
    }
}
