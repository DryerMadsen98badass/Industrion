package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

public final class CompressingRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("compressing"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Compressing"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(9, 2, 2, 2))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private CompressingRecipeType() {
    }
}
