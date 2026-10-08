package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

/** Hand-worked stone mold recipes. The runtime requires the configured tool separately. */
public final class BrickMoldingRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("brick_molding"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Brick Molding"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(1, 1, 0, 0))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.dedicatedToolSlot())
            .recipeTypeDefinition(RecipeTypeDefinition.Option.ignoreTier())
            .build();

    private BrickMoldingRecipeType() {}
}
