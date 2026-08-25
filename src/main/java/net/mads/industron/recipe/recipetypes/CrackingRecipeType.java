package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

public final class CrackingRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("cracking"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Cracking"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(2, 4, 3, 4))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private CrackingRecipeType() {
    }
}
