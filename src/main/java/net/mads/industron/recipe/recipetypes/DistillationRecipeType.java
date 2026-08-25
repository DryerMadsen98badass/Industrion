package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

public final class DistillationRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("distillation"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Distillation"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(2, 2, 3, 6))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private DistillationRecipeType() {
    }
}
