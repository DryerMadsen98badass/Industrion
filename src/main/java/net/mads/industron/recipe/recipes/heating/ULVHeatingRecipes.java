package net.mads.industron.recipe.recipes.heating;

import net.mads.industron.machine.MachineTier;
import net.mads.industron.recipe.CERecipeTypes;
import net.mads.industron.recipe.RecipeDefinition;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeOutput;

public final class ULVHeatingRecipes {

    private ULVHeatingRecipes() {
    }

    public static void build(
            RecipeOutput output,
            HolderLookup.Provider holderLookup
    ) {
    }

    private static RecipeDefinition recipe(String id) {
        return RecipeDefinition.recipe()
                .recipeDefinition(RecipeDefinition.Option.id(id))
                .recipeDefinition(RecipeDefinition.Option.tier(MachineTier.ULV))
                .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.HEATING));
    }
}
