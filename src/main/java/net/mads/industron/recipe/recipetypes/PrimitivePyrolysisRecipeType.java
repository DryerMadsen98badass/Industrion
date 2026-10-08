package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

/** Early-game world-block pyrolysis executed by primitive multiblocks such as the Charcoal Pit. */
public final class PrimitivePyrolysisRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("primitive_pyrolysis"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Primitive Pyrolysis"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(0, 0, 0, 0))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.ignoreTier())
            .build();

    private PrimitivePyrolysisRecipeType() {
    }
}
