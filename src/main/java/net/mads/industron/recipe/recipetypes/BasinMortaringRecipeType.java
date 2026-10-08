package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

/** Manual mortar-style processing performed in a Basin with a Pestle. */
public final class BasinMortaringRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("basin_mortaring"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Basin Mortaring"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(1, 3, 0, 0))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.dedicatedToolSlot())
            .recipeTypeDefinition(RecipeTypeDefinition.Option.generateFor(GrindingRecipeType.DEFINITION))
            .build();

    private BasinMortaringRecipeType() {
    }
}
