package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

public final class ReformingRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("reforming"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Reforming"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(2, 4, 4, 4))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private ReformingRecipeType() {
    }
}
