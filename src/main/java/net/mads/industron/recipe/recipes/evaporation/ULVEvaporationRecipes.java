package net.mads.industron.recipe.recipes.evaporation;

import net.mads.industron.machine.MachineTier;
import net.mads.industron.recipe.CERecipeTypes;
import net.mads.industron.recipe.RecipeDefinition;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeOutput;

public final class ULVEvaporationRecipes {

    private ULVEvaporationRecipes() {
    }

    public static void build(
            RecipeOutput output,
            HolderLookup.Provider holderLookup
    ) {
        recipe("water_to_steam")
                .recipeDefinition(RecipeDefinition.Option.inputFluid("minecraft:water", 144))
                .recipeDefinition(RecipeDefinition.Option.outputFluid("industron:steam", 576))
                .recipeDefinition(RecipeDefinition.Option.temperature(100))
                .recipeDefinition(RecipeDefinition.Option.duration(144))
                .save(output);
    }

    private static RecipeDefinition recipe(String id) {
        return RecipeDefinition.recipe()
                .recipeDefinition(RecipeDefinition.Option.id(id))
                .recipeDefinition(RecipeDefinition.Option.tier(MachineTier.ULV))
                .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.EVAPORATION));
    }
}
