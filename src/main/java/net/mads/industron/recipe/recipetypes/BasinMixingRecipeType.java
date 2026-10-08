package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

/** Manual basin mixing; every recipe is also emitted for the main automated mixer. */
public final class BasinMixingRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("basin_mixing"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Basin Mixing"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(9, 4, 6, 4))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.dedicatedToolSlot())
            .recipeTypeDefinition(RecipeTypeDefinition.Option.generateFor(MixingRecipeType.DEFINITION))
            .build();

    private BasinMixingRecipeType() {
    }
}
