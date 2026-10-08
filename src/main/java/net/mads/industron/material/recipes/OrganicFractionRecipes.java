package net.mads.industron.material.recipes;

import net.mads.industron.material.organism.*;
import net.mads.industron.recipe.CERecipeTypes;
import net.mads.industron.recipe.RecipeDefinition;
import net.minecraft.data.recipes.RecipeOutput;

/** Registered intermediates and recipes consume the same immutable, balanced plan. */
public final class OrganicFractionRecipes {
    private OrganicFractionRecipes() {}
    public static void build(RecipeOutput output) {
        for(var step:OrganicFractionPlanner.PLAN.steps()) {
            var type=switch(step.process()) {
                case CUTTING -> CERecipeTypes.CUTTING;
                case CENTRIFUGING -> CERecipeTypes.CENTRIFUGING;
                case DRYING -> CERecipeTypes.DRYING;
            };
            if(step.inputUnits()>64 || step.outputs().stream().anyMatch(p->!p.fluid() && p.units()>64))
                throw new IllegalArgumentException("Organic batch exceeds stack limit: "+step.id());
            var recipe=RecipeDefinition.recipe()
                    .recipeDefinition(RecipeDefinition.Option.id("organics/fractionation/"+step.id()))
                    .recipeDefinition(RecipeDefinition.Option.recipeType(type))
                    .recipeDefinition(RecipeDefinition.Option.inputItem(step.input(),step.inputUnits()))
                    .recipeDefinition(RecipeDefinition.Option.duration(Math.multiplyExact(step.inputUnits(),80)))
                    .recipeDefinition(RecipeDefinition.Option.tier(BiologicalProcessingTier.of(step.material())));
            for(var product:step.outputs()) {
                recipe.recipeDefinition(product.fluid()
                        ? RecipeDefinition.Option.outputFluid(product.itemOrFluid(),Math.multiplyExact(product.units(),144))
                        : RecipeDefinition.Option.outputItem(product.itemOrFluid(),product.units()));
            }
            recipe.save(output);
        }
    }
}
