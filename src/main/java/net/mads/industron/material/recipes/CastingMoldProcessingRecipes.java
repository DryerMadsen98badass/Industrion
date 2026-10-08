package net.mads.industron.material.recipes;

import net.mads.industron.Industron;
import net.mads.industron.machine.foundry.CastingRegistry;
import net.mads.industron.machine.foundry.casting.TerracottaMoldDefinitions;
import net.mads.industron.material.ClayMaterialRules;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.defenitions.IndustrialMaterials;
import net.mads.industron.recipe.CERecipeTypes;
import net.mads.industron.recipe.RecipeDefinition;
import net.minecraft.data.recipes.RecipeOutput;

/** Drying/firing chain for ceramic casting molds formed manually in a Caster. */
public final class CastingMoldProcessingRecipes {
    private CastingMoldProcessingRecipes() {}

    public static void build(RecipeOutput output) {
        for (IndustrialMaterial ceramic : IndustrialMaterials.ALL) {
            if (!ceramic.supportsCeramicMolds()) continue;

            for (TerracottaMoldDefinitions.Definition definition : TerracottaMoldDefinitions.ALL) {
                String formId = definition.moldPart().id();
                String unfired = item(CastingRegistry.unfiredMoldId(ceramic, definition.moldPart()));
                String dried = item(CastingRegistry.driedUnfiredMoldId(ceramic, definition.moldPart()));
                String finished = item(CastingRegistry.moldId(ceramic, definition.moldPart()));

                // Automated path exists for every ceramic tier.
                RecipeDefinition.recipe()
                        .recipeDefinition(RecipeDefinition.Option.id(
                                ceramic.id() + "/casting_molds/dry_" + formId))
                        .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.DRYING))
                        .recipeDefinition(RecipeDefinition.Option.tier(MaterialProcessingRules.processingTier(ceramic)))
                        .recipeDefinition(RecipeDefinition.Option.duration(ClayMaterialRules.SUN_DRYING_DURATION_TICKS))
                        .recipeDefinition(RecipeDefinition.Option.inputItem(unfired, 1))
                        .recipeDefinition(RecipeDefinition.Option.outputItem(dried, 1))
                        .save(output);

                RecipeDefinition.recipe()
                        .recipeDefinition(RecipeDefinition.Option.id(
                                ceramic.id() + "/casting_molds/sinter_" + formId))
                        .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.SINTERING))
                        .recipeDefinition(RecipeDefinition.Option.tier(MaterialProcessingRules.processingTier(ceramic)))
                        .recipeDefinition(RecipeDefinition.Option.duration(ClayMaterialRules.KILN_FIRING_DURATION_TICKS))
                        .recipeDefinition(RecipeDefinition.Option.temperature(ClayMaterialRules.firingTemperature(ceramic)))
                        .recipeDefinition(RecipeDefinition.Option.inputItem(dried, 1))
                        .recipeDefinition(RecipeDefinition.Option.outputItem(finished, 1))
                        .save(output);

                // Primitive executors stop at ULV/LV. They are alternate routes, not the only route.
                if (MaterialProcessingRules.allowsPrimitive(ceramic.tier())) {
                    RecipeDefinition.recipe()
                            .recipeDefinition(RecipeDefinition.Option.id(
                                    "primitive/" + ceramic.id() + "/casting_molds/dry_" + formId))
                            .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.RACK_DRYING))
                            .recipeDefinition(RecipeDefinition.Option.tier(MaterialProcessingRules.processingTier(ceramic)))
                            .recipeDefinition(RecipeDefinition.Option.duration(ClayMaterialRules.SUN_DRYING_DURATION_TICKS))
                            .recipeDefinition(RecipeDefinition.Option.inputItem(unfired, 1))
                            .recipeDefinition(RecipeDefinition.Option.outputItem(dried, 1))
                            .save(output);

                    RecipeDefinition.recipe()
                            .recipeDefinition(RecipeDefinition.Option.id(
                                    "primitive/" + ceramic.id() + "/casting_molds/fire_" + formId))
                            .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.KILN_FIRING))
                            .recipeDefinition(RecipeDefinition.Option.tier(MaterialProcessingRules.processingTier(ceramic)))
                            .recipeDefinition(RecipeDefinition.Option.duration(ClayMaterialRules.KILN_FIRING_DURATION_TICKS))
                            .recipeDefinition(RecipeDefinition.Option.inputItem(dried, 1))
                            .recipeDefinition(RecipeDefinition.Option.outputItem(finished, 1))
                            .save(output);
                }
            }
        }
    }

    private static String item(String path) {
        return Industron.MOD_ID + ":" + path;
    }
}
