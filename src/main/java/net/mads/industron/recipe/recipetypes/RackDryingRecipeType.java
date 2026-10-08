package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

/** Passive sunlight recipes used only by material drying racks. */
public final class RackDryingRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("rack_drying"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Rack Drying"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(1, 1, 0, 0))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.ignoreTier())
            .build();

    private RackDryingRecipeType() {}
}
