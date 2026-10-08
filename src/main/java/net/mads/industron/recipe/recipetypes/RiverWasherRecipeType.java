package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

/** Primitive river-powered washing. Water is supplied by the waterlogged machine/world, not a fluid slot. */
public final class RiverWasherRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("riverwasher"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("River Washer"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(1, 16, 0, 0))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.ignoreTier())
            .build();

    private RiverWasherRecipeType() {
    }
}
