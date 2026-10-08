package net.mads.industron.material.recipes;

import net.mads.industron.Industron;
import net.mads.industron.block.SimpleBlockDefinition;
import net.mads.industron.block.SimpleBlocks;
import net.mads.industron.fluid.IndustrialFluidLookup;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.MaterialUnits;
import net.mads.industron.material.chemistry.ChemistryPhase;
import net.mads.industron.material.defenitions.IndustrialMaterials;
import net.mads.industron.material.defenitions.PlantMaterials;
import net.mads.industron.material.defenitions.OrganicMaterials;
import net.mads.industron.material.defenitions.WoodMaterials;
import net.mads.industron.material.fuel.FuelFormFactor;
import net.mads.industron.material.fuel.FuelFormRules;
import net.mads.industron.material.fuel.FuelValueCalculator;
import net.mads.industron.material.plant.PlantMaterial;
import net.mads.industron.material.organic.OrganicMaterial;
import net.mads.industron.material.plant.PlantMaterialGenerator;
import net.mads.industron.material.plant.PlantPart;
import net.mads.industron.material.plant.PlantProcessIntermediate;
import net.mads.industron.material.plant.PlantProcessingPlanner;
import net.mads.industron.material.structure.StructureMaterialGenerator;
import net.mads.industron.material.structure.WoodMaterial;
import net.mads.industron.recipe.CERecipeTypes;
import net.mads.industron.recipe.RecipeDefinition;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

/** Generates the shared Fuel recipe table from .contains(...) + phase + physical form. */
public final class FuelRecipes {
    private record FuelInput(ResourceLocation id, boolean fluid, int amount) {
        private FuelInput {
            if (id == null || amount <= 0) throw new IllegalArgumentException("Invalid fuel input");
        }

        static FuelInput item(ResourceLocation id) {
            return new FuelInput(id, false, 1);
        }

        static FuelInput fluid(ResourceLocation id, ChemistryPhase phase) {
            return new FuelInput(id, true, MaterialUnits.millibucketsPerUnit(phase));
        }
    }

    private FuelRecipes() {
    }

    public static void build(RecipeOutput output) {
        Map<FuelInput, Double> fuels = new LinkedHashMap<>();

        for (WoodMaterial wood : WoodMaterials.ALL) collectWood(fuels, wood);
        for (PlantMaterial plant : PlantMaterials.ALL) collectPlant(fuels, plant);
        for (OrganicMaterial organic : OrganicMaterials.ALL) collectOrganic(fuels, organic);
        for (SimpleBlockDefinition block : SimpleBlocks.ALL) collectSimpleBlock(fuels, block);
        for (PlantProcessIntermediate intermediate : PlantProcessingPlanner.allRequiredIntermediates()) {
            collectPlantIntermediate(fuels, intermediate);
        }
        for (IndustrialMaterial material : IndustrialMaterials.ALL) collectIndustrialMaterial(fuels, material);

        fuels.forEach((input, units) -> {
            String kind = input.fluid() ? "fluid" : "item";
            String path = input.id().getPath().replace('/', '_');
            RecipeDefinition definition = RecipeDefinition.recipe()
                    .recipeDefinition(RecipeDefinition.Option.id(
                            "generated/" + kind + "/" + input.id().getNamespace() + "/" + path
                    ))
                    .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.FUEL))
                    .recipeDefinition(RecipeDefinition.Option.fuelUnits(units));
            if (input.fluid()) {
                definition.recipeDefinition(RecipeDefinition.Option.inputFluid(input.id().toString(), input.amount()));
            } else {
                definition.recipeDefinition(RecipeDefinition.Option.inputItem(input.id().toString(), 1));
            }
            definition.save(output);
        });
    }


    private static void collectOrganic(Map<FuelInput, Double> fuels, OrganicMaterial organic) {
        double units = FuelValueCalculator.fuelUnits(organic, FuelFormFactor.ONE);
        if (units <= 0.0D) return;
        FuelInput input = organic.phase() == net.mads.industron.material.chemistry.ChemistryPhase.SOLID
                ? FuelInput.item(organic.existingForm())
                : FuelInput.fluid(organic.existingForm(), organic.phase());
        putFuel(fuels, input, units);
    }

    private static void collectSimpleBlock(Map<FuelInput, Double> fuels, SimpleBlockDefinition block) {
        if (block.components().isEmpty()) return;

        double units = 0.0D;
        for (var component : block.components()) {
            units += FuelValueCalculator.baseFuelUnits(component.substance()) * component.amount();
        }
        if (units <= 0.0D) return;

        putFuel(
                fuels,
                FuelInput.item(ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, block.id())),
                units
        );
    }

    private static void collectWood(Map<FuelInput, Double> fuels, WoodMaterial wood) {
        for (MaterialPart part : MaterialPart.values()) {
            var factor = FuelFormRules.wood(part);
            if (factor.isEmpty() || !hasWoodPart(wood, part)) continue;
            ResourceLocation id = woodPartId(wood, part);
            if (id == null) continue;
            double units = FuelValueCalculator.fuelUnits(wood, factor.get());
            if (units <= 0.0D) continue;
            putFuel(fuels, FuelInput.item(id), units);
        }
    }

    private static void collectPlant(Map<FuelInput, Double> fuels, PlantMaterial plant) {
        plant.existingParts().forEach((part, id) -> FuelFormRules.plant(part).ifPresent(factor -> {
            double units = FuelValueCalculator.fuelUnits(plant, factor);
            if (units > 0.0D) putFuel(fuels, FuelInput.item(id), units);
        }));

        for (PlantPart part : PlantMaterialGenerator.generatedItemForms(plant)) {
            var factor = FuelFormRules.plant(part);
            if (factor.isEmpty()) continue;
            double units = FuelValueCalculator.fuelUnits(
                    PlantMaterialGenerator.substanceFor(plant, part), factor.get()
            );
            if (units <= 0.0D) continue;
            putFuel(
                    fuels,
                    FuelInput.item(ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, part.registryName(plant))),
                    units
            );
        }
    }

    private static void collectPlantIntermediate(
            Map<FuelInput, Double> fuels,
            PlantProcessIntermediate intermediate
    ) {
        double units = FuelValueCalculator.fuelUnits(
                intermediate,
                FuelFormRules.plantIntermediate(intermediate)
        );
        if (units <= 0.0D) return;
        FuelInput input = intermediate.isSolid()
                ? FuelInput.item(intermediate.registryId())
                : FuelInput.fluid(intermediate.registryId(), intermediate.phase());
        putFuel(fuels, input, units);
    }

    private static void collectIndustrialMaterial(Map<FuelInput, Double> fuels, IndustrialMaterial material) {
        for (MaterialPart part : material.parts()) {
            var factor = FuelFormRules.material(part);
            if (factor.isEmpty()) continue;
            double units = FuelValueCalculator.fuelUnits(material, factor.get());
            if (units <= 0.0D) continue;

            if (part == MaterialPart.LIQUID || part == MaterialPart.GAS) {
                putFuel(
                        fuels,
                        FuelInput.fluid(IndustrialFluidLookup.fluidId(material, part),
                                part == MaterialPart.GAS ? ChemistryPhase.GAS : ChemistryPhase.LIQUID),
                        units
                );
                continue;
            }
            if (!part.isItem() && !part.isBlock()) continue;
            ResourceLocation id = material.hasExistingPart(part)
                    ? material.existingPart(part)
                    : ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, part.registryName(material));
            putFuel(fuels, FuelInput.item(id), units);
        }
    }

    private static boolean hasWoodPart(WoodMaterial wood, MaterialPart part) {
        if (wood.hasExistingPart(part) || wood.generatedForms().contains(part)) return true;
        return StructureMaterialGenerator.blockDefinitions(wood).stream()
                .anyMatch(definition -> definition.part().filter(value -> value == part).isPresent());
    }

    private static ResourceLocation woodPartId(WoodMaterial wood, MaterialPart part) {
        if (wood.hasExistingPart(part)) {
            ResourceLocation id = wood.existingPart(part);
            // Fuel recipes consume ItemStacks. Some valid wood world-parts (notably bamboo_sapling)
            // intentionally have no inventory item and must therefore not get an item-fuel recipe.
            if ("minecraft".equals(id.getNamespace()) && BuiltInRegistries.ITEM.getOptional(id).isEmpty()) {
                return null;
            }
            return id;
        }
        return ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, part.registryName(wood));
    }

    private static void putFuel(Map<FuelInput, Double> fuels, FuelInput input, double units) {
        Double previous = fuels.putIfAbsent(input, units);
        if (previous != null && Double.compare(previous, units) != 0) {
            throw new IllegalStateException(
                    "Conflicting generated fuel value for " + input.id() + ": " + previous + " vs " + units
            );
        }
    }
}
