package net.mads.industron.recipe.recipes.compressing;

import net.mads.industron.machine.MachineTier;
import net.mads.industron.recipe.CERecipeTypes;
import net.mads.industron.recipe.RecipeDefinition;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeOutput;

public final class LVCompressingRecipes {

    private LVCompressingRecipes() {
    }

    public static void build(
            RecipeOutput output,
            HolderLookup.Provider holderLookup
    ) {
        recipe("portal_core")
                .recipeDefinition(RecipeDefinition.Option.inputItem("minecraft:obsidian", 2))
                .recipeDefinition(RecipeDefinition.Option.inputItem("minecraft:redstone", 8))
                .recipeDefinition(RecipeDefinition.Option.outputItem("industron:portal_core", 1))
                .recipeDefinition(RecipeDefinition.Option.duration(400))
                .save(output);
    }

    private static RecipeDefinition recipe(String id) {
        return RecipeDefinition.recipe()
                .recipeDefinition(RecipeDefinition.Option.id(id))
                .recipeDefinition(RecipeDefinition.Option.tier(MachineTier.LV))
                .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.COMPRESSING));
    }
}
