package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

/** Physical separation of already distinct magnetic/non-magnetic fractions. */
public final class MagneticSeparationRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("magnetic_separation"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Magnetic Separation"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(9, 9, 3, 3))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private MagneticSeparationRecipeType() {
    }
}
