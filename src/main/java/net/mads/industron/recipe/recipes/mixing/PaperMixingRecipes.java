package net.mads.industron.recipe.recipes.mixing;

import net.mads.industron.machine.MachineTier;
import net.mads.industron.recipe.CERecipeTypes;
import net.mads.industron.recipe.RecipeDefinition;
import net.mads.industron.recipe.recipes.assembly.Tool;
import net.minecraft.data.recipes.RecipeOutput;

/** Private paper-making mixing route. */
public final class PaperMixingRecipes {
    private static final int WATER_MB = 100;
    private static final int MIXING_TICKS = 20 * 10;
    private static final int SHOVEL_USES = 3;

    private PaperMixingRecipes() {
    }

    public static void build(RecipeOutput output) {
        RecipeDefinition.recipe()
                .recipeDefinition(RecipeDefinition.Option.id("vanilla/paper/fibers_to_wet_paper"))
                .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.BASIN_MIXING))
                .recipeDefinition(RecipeDefinition.Option.tier(MachineTier.ULV))
                .recipeDefinition(RecipeDefinition.Option.inputTag("industron:plant_fibers", 3))
                .recipeDefinition(RecipeDefinition.Option.inputFluid("minecraft:water", WATER_MB))
                .recipeDefinition(RecipeDefinition.Option.outputItem("industron:wet_paper", 1))
                .recipeDefinition(RecipeDefinition.Option.duration(MIXING_TICKS))
                .tool(Tool.SHOVEL, SHOVEL_USES)
                .save(output);
    }
}
