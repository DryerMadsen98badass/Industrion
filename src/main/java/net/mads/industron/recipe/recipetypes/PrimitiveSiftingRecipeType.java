package net.mads.industron.recipe.recipetypes;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.recipe.RecipeTypeDefinition;
import net.mads.industron.recipe.recipes.assembly.Tool;
import net.mads.industron.recipe.recipetypes.primitive.PrimitiveSiftingRules;

/** Hand-held primitive sieve recipes. The runtime selects exactly one weighted dust output. */
public final class PrimitiveSiftingRecipeType {
    public static final RecipeTypeDefinition DEFINITION = RecipeTypeDefinition.recipeType()
            .recipeTypeDefinition(RecipeTypeDefinition.Option.id("primitive_sifting"))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.displayName(PrimitiveSiftingRules.DISPLAY_NAME))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.maxIO(1, 16, 0, 0))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.progressBar(ProgressBar.ARROW))
            .recipeTypeDefinition(RecipeTypeDefinition.Option.dedicatedToolSlot())
            .recipeTypeDefinition(RecipeTypeDefinition.Option.jeiToolIcon(Tool.SIFTER))
            .build();

    private PrimitiveSiftingRecipeType() {
    }
}
