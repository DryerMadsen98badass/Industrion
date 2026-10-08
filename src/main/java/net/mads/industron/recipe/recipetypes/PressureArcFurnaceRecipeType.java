package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

public final class PressureArcFurnaceRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("pressure_arc_furnace"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Pressure Arc Furnace"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(4, 4, 2, 2))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private PressureArcFurnaceRecipeType() {
    }
}
