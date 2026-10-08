package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

/** Manual mortar-style grinding performed in a Basin with a Pestle. */
public final class BasinGrindingRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("basin_grinding"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Basin Grinding"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(1, 3, 0, 0))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.ignoreTier())
            .recipeTypeDefinition(RecipeTypeDefinition.Option.generateFor(GrindingRecipeType.DEFINITION))
            .build();

    private BasinGrindingRecipeType() {
    }
}
