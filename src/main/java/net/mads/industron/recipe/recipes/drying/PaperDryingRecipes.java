package net.mads.industron.recipe.recipes.drying;

import net.mads.industron.machine.MachineTier;
import net.mads.industron.recipe.CERecipeTypes;
import net.mads.industron.recipe.RecipeDefinition;
import net.minecraft.data.recipes.RecipeOutput;

/** Private wet-paper drying routes for manual and automated drying. */
public final class PaperDryingRecipes {
    private static final int DRYING_TICKS = 20 * 30;

    private PaperDryingRecipes() {
    }

    public static void build(RecipeOutput output) {
        RecipeDefinition.recipe()
                .recipeDefinition(RecipeDefinition.Option.id("vanilla/paper/rack_dry_wet_paper"))
                .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.RACK_DRYING))
                .recipeDefinition(RecipeDefinition.Option.inputItem("industron:wet_paper", 1))
                .recipeDefinition(RecipeDefinition.Option.outputItem("minecraft:paper", 1))
                .recipeDefinition(RecipeDefinition.Option.duration(DRYING_TICKS))
                .save(output);

        RecipeDefinition.recipe()
                .recipeDefinition(RecipeDefinition.Option.id("vanilla/paper/dry_wet_paper"))
                .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.DRYING))
                .recipeDefinition(RecipeDefinition.Option.tier(MachineTier.ULV))
                .recipeDefinition(RecipeDefinition.Option.inputItem("industron:wet_paper", 1))
                .recipeDefinition(RecipeDefinition.Option.outputItem("minecraft:paper", 1))
                .recipeDefinition(RecipeDefinition.Option.duration(DRYING_TICKS))
                .save(output);
    }
}
