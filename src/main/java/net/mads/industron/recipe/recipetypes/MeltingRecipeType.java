package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.CERecipeLogics;
import net.mads.industron.recipe.RecipeTypeDefinition;

public final class MeltingRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("melting"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Melting"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(2, 1, 1, 1))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.logic(CERecipeLogics.COIL_TEMP))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.requiresCoilTemperature())
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private MeltingRecipeType() {
    }
}
