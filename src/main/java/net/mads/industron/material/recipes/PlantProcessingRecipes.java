package net.mads.industron.material.recipes;

import net.mads.industron.Industron;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.material.defenitions.PlantMaterials;
import net.mads.industron.material.plant.PlantMaterial;
import net.mads.industron.material.plant.PlantMaterialGenerator;
import net.mads.industron.material.plant.PlantPart;
import net.mads.industron.material.plant.PlantProcessingPlanner;
import net.mads.industron.recipe.CERecipeTypes;
import net.mads.industron.recipe.RecipeDefinition;
import net.mads.industron.recipe.RecipeTypeDefinition;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;

/** Plant recipes planned from physical PlantPart roles + canonical .contains(...) composition. */
public final class PlantProcessingRecipes {
    private static final int WASHING_WATER_MB = 144;

    private PlantProcessingRecipes() {
    }

    public static void build(RecipeOutput output) {
        for (PlantMaterial material : PlantMaterials.ALL) {
            if (!PlantMaterialGenerator.hasProcessSource(material)) continue;
            buildMechanicalPreparation(output, material);
            buildComposting(output, material);
            if (PlantMaterialGenerator.hasFiberFraction(material)) {
                buildFiber(output, material);
                buildString(output, material);
            }
        }
    }

    /**
     * Converts one raw plant item into one route-biomass item. One solid item is always one
     * material unit; fuel/value form factors never alter recipe mass.
     */
    private static void buildMechanicalPreparation(RecipeOutput output, PlantMaterial material) {
        String biomass = intermediate(material, PlantProcessingPlanner.BIOMASS);
        for (var entry : material.existingParts().entrySet()) {
            PlantPart sourcePart = entry.getKey();
            if (!sourcePart.biomassSource()) continue;
            RecipeTypeDefinition type = PlantProcessingPlanner.usesGrindingPreparation(sourcePart)
                    ? CERecipeTypes.GRINDING
                    : CERecipeTypes.CUTTING;
            String processName = type.id().getPath();
            RecipeDefinition.recipe()
                    .recipeDefinition(RecipeDefinition.Option.id(
                            "plants/" + material.id() + "/" + sourcePart.name().toLowerCase()
                                    + "_to_biomass_" + processName
                    ))
                    .recipeDefinition(RecipeDefinition.Option.recipeType(type))
                    .recipeDefinition(RecipeDefinition.Option.inputItem(
                            entry.getValue().toString(), 1
                    ))
                    .recipeDefinition(RecipeDefinition.Option.outputItem(biomass, 1))
                    .recipeDefinition(RecipeDefinition.Option.duration(
                            PlantProcessingPlanner.mechanicalPreparationTicks(material, sourcePart)
                    ))
                    .recipeDefinition(RecipeDefinition.Option.tier(MachineTier.ULV))
                    .save(output);
        }
    }

    private static void buildComposting(RecipeOutput output, PlantMaterial material) {
        String fertilizer = intermediate(material, PlantProcessingPlanner.FERTILIZER);

        // Every raw/existing processable plant form can go directly into the vanilla Composter.
        // One solid input item is one material unit and produces one fertilizer unit.
        for (var entry : material.existingParts().entrySet()) {
            PlantPart sourcePart = entry.getKey();
            if (!sourcePart.biomassSource()) continue;
            RecipeDefinition.recipe()
                    .recipeDefinition(RecipeDefinition.Option.id(
                            "plants/" + material.id() + "/compost_"
                                    + sourcePart.name().toLowerCase() + "_to_fertilizer"
                    ))
                    .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.COMPOSTING))
                    .recipeDefinition(RecipeDefinition.Option.inputItem(entry.getValue().toString(), 1))
                    .recipeDefinition(RecipeDefinition.Option.outputItem(fertilizer, 1))
                    .recipeDefinition(RecipeDefinition.Option.duration(PlantProcessingPlanner.compostTicks(material)))
                    .save(output);
        }

        // Prepared biomass remains compostable as the same material identity.
        RecipeDefinition.recipe()
                .recipeDefinition(RecipeDefinition.Option.id("plants/" + material.id() + "/compost_biomass_to_fertilizer"))
                .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.COMPOSTING))
                .recipeDefinition(RecipeDefinition.Option.inputItem(
                        intermediate(material, PlantProcessingPlanner.BIOMASS), 1
                ))
                .recipeDefinition(RecipeDefinition.Option.outputItem(fertilizer, 1))
                .recipeDefinition(RecipeDefinition.Option.duration(PlantProcessingPlanner.compostTicks(material)))
                .save(output);
    }

    private static void buildFiber(RecipeOutput output, PlantMaterial material) {
        PlantProcessingPlanner.FiberBatch biomassBatch = PlantProcessingPlanner.fiberBatch(material);
        String fiber = generated(material, PlantPart.FIBER);
        String residue = biomassBatch.residueCount() > 0
                ? intermediate(material, PlantProcessingPlanner.RESIDUE)
                : null;

        // Earliest route: deliberate hand work. Ctrl is required only to start the active recipe.
        for (var entry : material.existingParts().entrySet()) {
            PlantPart sourcePart = entry.getKey();
            if (!sourcePart.fiberSource()) continue;

            PlantProcessingPlanner.FiberBatch batch = PlantProcessingPlanner.fiberBatch(material, sourcePart);
            // One held stack is used by Hand Processing, so impossible oversized exact batches are
            // left to the normalized biomass + Washing route rather than silently changing ratios.
            if (batch.inputCount() > 64 || batch.fiberCount() > 64 || batch.residueCount() > 64) continue;

            RecipeDefinition recipe = RecipeDefinition.recipe()
                    .recipeDefinition(RecipeDefinition.Option.id(
                            "plants/" + material.id() + "/hand_" + sourcePart.name().toLowerCase() + "_to_fiber"
                    ))
                    .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.HAND_PROCESSING))
                    .recipeDefinition(RecipeDefinition.Option.inputItem(
                            entry.getValue().toString(), batch.inputCount()
                    ))
                    .recipeDefinition(RecipeDefinition.Option.outputItem(fiber, batch.fiberCount()))
                    .recipeDefinition(RecipeDefinition.Option.uses(
                            PlantProcessingPlanner.handFiberUses(material, sourcePart)
                    ));
            if (batch.residueCount() > 0) {
                recipe.recipeDefinition(RecipeDefinition.Option.outputItem(residue, batch.residueCount()));
            }
            recipe.save(output);
        }

        // Water is a process medium. It is required but not consumed by the material separation.
        RecipeDefinition washing = RecipeDefinition.recipe()
                .recipeDefinition(RecipeDefinition.Option.id("plants/" + material.id() + "/wash_biomass_to_fiber"))
                .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.WASHING))
                .recipeDefinition(RecipeDefinition.Option.inputItem(
                        intermediate(material, PlantProcessingPlanner.BIOMASS), biomassBatch.inputCount()
                ))
                .recipeDefinition(RecipeDefinition.Option.notConsumableFluid(
                        "minecraft:water", WASHING_WATER_MB
                ))
                .recipeDefinition(RecipeDefinition.Option.outputItem(fiber, biomassBatch.fiberCount()))
                .recipeDefinition(RecipeDefinition.Option.duration(PlantProcessingPlanner.washingTicks(material)))
                .recipeDefinition(RecipeDefinition.Option.tier(MachineTier.ULV));
        if (biomassBatch.residueCount() > 0) {
            washing.recipeDefinition(RecipeDefinition.Option.outputItem(residue, biomassBatch.residueCount()));
        }
        washing.save(output);
    }

    private static void buildString(RecipeOutput output, PlantMaterial material) {
        RecipeDefinition.recipe()
                .recipeDefinition(RecipeDefinition.Option.id("plants/" + material.id() + "/fiber_to_string"))
                .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.HAND_PROCESSING))
                .recipeDefinition(RecipeDefinition.Option.inputItem(generated(material, PlantPart.FIBER), 1))
                .recipeDefinition(RecipeDefinition.Option.outputItem(generated(material, PlantPart.STRING), 1))
                .recipeDefinition(RecipeDefinition.Option.uses(PlantProcessingPlanner.stringUses()))
                .save(output);
    }

    public static ResourceLocation generatedId(PlantMaterial material, PlantPart part) {
        return ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, part.registryName(material));
    }

    private static String intermediate(PlantMaterial material, String suffix) {
        return PlantProcessingPlanner.intermediateId(material, suffix).toString();
    }

    private static String generated(PlantMaterial material, PlantPart part) {
        if (!PlantMaterialGenerator.generates(material, part)) {
            throw new IllegalStateException(
                    "Recipe requested missing generated plant form: " + material.id() + " " + part
            );
        }
        return generatedId(material, part).toString();
    }
}
