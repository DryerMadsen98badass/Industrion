package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

/**
 * Machine-independent fuel-energy declaration.
 *
 * <p>A recipe only declares input + fuel_units. Consumers such as the kiln choose their own
 * Fuel-Unit-to-time/heat conversion and accepted phase.</p>
 */
public final class FuelRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("fuel"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Fuel"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(1, 0, 1, 0))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.ignoreTier())
            .build();

    private FuelRecipeType() {
    }
}
