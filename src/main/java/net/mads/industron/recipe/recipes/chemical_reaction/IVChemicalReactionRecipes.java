package net.mads.industron.recipe.recipes.chemical_reaction;

import net.mads.industron.machine.MachineTier;
import net.mads.industron.recipe.CERecipeTypes;
import net.mads.industron.recipe.RecipeDefinition;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeOutput;

public final class IVChemicalReactionRecipes {

    private IVChemicalReactionRecipes() {
    }

    public static void build(
            RecipeOutput output,
            HolderLookup.Provider holderLookup
    ) {
    }

    private static RecipeDefinition recipe(String id) {
        return RecipeDefinition.recipe()
                .recipeDefinition(RecipeDefinition.Option.id(id))
                .recipeDefinition(RecipeDefinition.Option.tier(MachineTier.IV))
                .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.CHEMICAL_REACTION));
    }
}
