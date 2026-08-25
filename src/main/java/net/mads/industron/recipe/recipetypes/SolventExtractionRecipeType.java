package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

public final class SolventExtractionRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("solvent_extraction"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Solvent Extraction"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(2, 2, 4, 4))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private SolventExtractionRecipeType() {
    }
}
