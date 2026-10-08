package net.mads.industron.recipe.recipes.pyrolysis;

import net.mads.industron.machine.interaction.BlockInteraction;
import net.mads.industron.machine.interaction.BlockRequirement;
import net.mads.industron.machine.interaction.InteractionPhase;
import net.mads.industron.machine.machines.without_energy.multiblock.machines.CharcoalPit;
import net.mads.industron.recipe.CERecipeTypes;
import net.mads.industron.recipe.RecipeDefinition;
import net.minecraft.data.recipes.RecipeOutput;

/** Private primitive-pyrolysis recipes with fixed outputs. */
public final class PrimitivePyrolysisRecipes {
    public static final int CHARCOAL_PIT_DURATION_TICKS = 20 * 60 * 5;

    private PrimitivePyrolysisRecipes() {
    }

    public static void build(RecipeOutput output) {
        RecipeDefinition.recipe()
                .recipeDefinition(RecipeDefinition.Option.id("primitive_pyrolysis/log_to_charcoal_block"))
                .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.PRIMITIVE_PYROLYSIS))
                .recipeDefinition(RecipeDefinition.Option.duration(CHARCOAL_PIT_DURATION_TICKS))
                .recipeDefinition(RecipeDefinition.Option.blockInteraction(
                        BlockInteraction.convert()
                                .inArea(CharcoalPit.INSIDE_AREA)
                                .requires(BlockRequirement.tag("minecraft:logs"))
                                .to("industron:charcoal_block")
                                .when(InteractionPhase.ON_COMPLETE)
                ))
                .save(output);
    }
}
