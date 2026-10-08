package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;

/**
 * Unpacks an identity-preserving storage/packing form back into its smaller units.
 * This is the inverse process family of {@link CompactingRecipeType}; machines may
 * support both recipe types without treating either direction as grinding/cutting.
 */
public final class DecompactingRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("decompacting"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName("Decompacting"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(1, 9, 0, 0))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .build();

    private DecompactingRecipeType() {
    }
}
