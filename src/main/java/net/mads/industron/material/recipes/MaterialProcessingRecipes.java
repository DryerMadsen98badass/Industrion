package net.mads.industron.material.recipes;

import net.mads.industron.Industron;
import net.mads.industron.energy.WireThickness;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.machine.foundry.FoundryMetallurgy;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.defenitions.IndustrialMaterials;
import net.mads.industron.material.MaterialFormGenerator;
import net.mads.industron.material.MaterialPropertyCalculator;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.recipe.CERecipeTypes;
import net.mads.industron.recipe.RecipeDefinition;
import net.mads.industron.recipe.RecipeTypeDefinition;
import net.minecraft.data.recipes.RecipeOutput;

import java.util.List;

/** Autogenerates material-bound item recipes from the forms owned by each material. */
public final class MaterialProcessingRecipes {
    private static final int ONE_MINUTE = 20 * 60;
    private static final int NINETY_SECONDS = 20 * 90;
    private static final int TWO_MINUTES = 20 * 120;

    private static final List<MaterialPart> RODS = List.of(
            MaterialPart.VERY_SHORT_ROD,
            MaterialPart.SHORT_ROD,
            MaterialPart.ROD,
            MaterialPart.LONG_ROD,
            MaterialPart.VERY_LONG_ROD
    );
    private static final List<MaterialPart> BALLS = List.of(
            MaterialPart.TINY_BALL,
            MaterialPart.SMALL_BALL,
            MaterialPart.BALL,
            MaterialPart.LARGE_BALL,
            MaterialPart.HUGE_BALL
    );
    private static final List<MaterialPart> BOLTS = List.of(
            MaterialPart.TINY_BOLT,
            MaterialPart.SMALL_BOLT,
            MaterialPart.BOLT,
            MaterialPart.LARGE_BOLT,
            MaterialPart.HUGE_BOLT
    );
    private static final List<MaterialPart> SCREWS = List.of(
            MaterialPart.TINY_SCREW,
            MaterialPart.SMALL_SCREW,
            MaterialPart.SCREW,
            MaterialPart.LARGE_SCREW,
            MaterialPart.HUGE_SCREW
    );
    private static final List<MaterialPart> RIVETS = List.of(
            MaterialPart.TINY_RIVET,
            MaterialPart.SMALL_RIVET,
            MaterialPart.RIVET,
            MaterialPart.LARGE_RIVET,
            MaterialPart.HUGE_RIVET
    );
    private static final List<MaterialPart> RINGS = List.of(
            MaterialPart.TINY_RING,
            MaterialPart.SMALL_RING,
            MaterialPart.RING,
            MaterialPart.LARGE_RING,
            MaterialPart.HUGE_RING
    );

    private MaterialProcessingRecipes() {
    }

    public static void build(RecipeOutput output) {
        OreProcessingRecipes.build(output);
        UniversalPhysicalProcessingRecipes.build(output);

        for (IndustrialMaterial material : IndustrialMaterials.ALL) {
            buildTurning(output, material);
            buildBending(output, material);
            buildWireDrawing(output, material);
            buildWinding(output, material);
            buildMagnetizing(output, material);
            buildGemCrystallization(output, material);
            buildPolishing(output, material);
            buildHotFormHeating(output, material);
            buildHotMetalCutting(output, material);
            buildBuzzSawCutting(output, material);
        }
    }

    private static void buildTurning(RecipeOutput output, IndustrialMaterial material) {
        for (int index = 0; index < RODS.size(); index++) {
            int duration = sizeDuration(ONE_MINUTE, index);
            save(output, material, CERecipeTypes.TURNING, 1, duration,
                    BALLS.get(index), 2, part(RODS.get(index), 1));
            save(output, material, CERecipeTypes.TURNING, 2, duration,
                    BOLTS.get(index), 8, part(RODS.get(index), 1));
            save(output, material, CERecipeTypes.TURNING, 3, duration,
                    RIVETS.get(index), 16, part(RODS.get(index), 1));
            save(output, material, CERecipeTypes.TURNING, 4, duration,
                    SCREWS.get(index), 8, part(BOLTS.get(index), 8));
        }

        // A finished shaft is a precision-turned one-ingot form. SHAFT and INGOT are both
        // one material unit, so this route is exactly mass-conserving.
        save(output, material, CERecipeTypes.TURNING, 5, ONE_MINUTE,
                MaterialPart.SHAFT, 1, part(MaterialPart.INGOT, 1));
    }

    private static void buildBending(RecipeOutput output, IndustrialMaterial material) {
        for (int index = 0; index < RODS.size(); index++) {
            save(output, material, CERecipeTypes.BENDING, 1, sizeDuration(ONE_MINUTE, index),
                    RINGS.get(index), 1, part(RODS.get(index), 1));
        }

        save(output, material, CERecipeTypes.BENDING, 1, ONE_MINUTE,
                MaterialPart.FOIL, 4, part(MaterialPart.PLATE, 1));
    }

    private static void buildWireDrawing(RecipeOutput output, IndustrialMaterial material) {
        save(output, material, CERecipeTypes.WIRE_DRAWING, 1, ONE_MINUTE,
                MaterialPart.FINE_WIRE, 8, part(MaterialPart.ROD, 1));
        save(output, material, CERecipeTypes.WIRE_DRAWING, 2, ONE_MINUTE,
                MaterialPart.WIRE_1X, 8, part(MaterialPart.ROD, 1));
        save(output, material, CERecipeTypes.WIRE_DRAWING, 3, ONE_MINUTE,
                MaterialPart.WIRE_2X, 4, part(MaterialPart.ROD, 1));
        save(output, material, CERecipeTypes.WIRE_DRAWING, 4, ONE_MINUTE,
                MaterialPart.WIRE_4X, 2, part(MaterialPart.ROD, 1));
        save(output, material, CERecipeTypes.WIRE_DRAWING, 5, ONE_MINUTE,
                MaterialPart.WIRE_8X, 1, part(MaterialPart.ROD, 1));
        save(output, material, CERecipeTypes.WIRE_DRAWING, 6, TWO_MINUTES,
                MaterialPart.WIRE_16X, 1, part(MaterialPart.ROD, 2));
    }

    private static void buildWinding(RecipeOutput output, IndustrialMaterial material) {
        List<MaterialPart> springs = List.of(MaterialPart.TINY_SPRING, MaterialPart.SMALL_SPRING,
                MaterialPart.SPRING, MaterialPart.LARGE_SPRING, MaterialPart.HUGE_SPRING);
        List<MaterialPart> springWires = List.of(MaterialPart.FINE_WIRE, MaterialPart.WIRE_1X,
                MaterialPart.WIRE_2X, MaterialPart.WIRE_4X, MaterialPart.WIRE_8X);
        for (int index = 0; index < springs.size(); index++) {
            save(output, material, CERecipeTypes.WINDING, 1, sizeDuration(ONE_MINUTE, index),
                    springs.get(index), 1, part(springWires.get(index), 2));
        }

        save(output, material, CERecipeTypes.WINDING, 2, ONE_MINUTE,
                MaterialPart.COIL, 1, part(MaterialPart.WIRE_1X, 8));
        save(output, material, CERecipeTypes.WINDING, 2, ONE_MINUTE,
                MaterialPart.COIL, 1, part(MaterialPart.WIRE_2X, 4));
        save(output, material, CERecipeTypes.WINDING, 2, ONE_MINUTE,
                MaterialPart.COIL, 1, part(MaterialPart.WIRE_4X, 2));
        save(output, material, CERecipeTypes.WINDING, 2, ONE_MINUTE,
                MaterialPart.COIL, 1, part(MaterialPart.WIRE_8X, 1));
    }


    private static void buildMagnetizing(RecipeOutput output, IndustrialMaterial material) {
        for (MaterialPart part : material.parts()) {
            if (!part.isItem()
                    || material.hasExistingPart(part)
                    || isFunctionalWire(part)
                    || !MaterialFormGenerator.hasMagneticVariant(material, part)) {
                continue;
            }

            RecipeDefinition.recipe()
                    .recipeDefinition(RecipeDefinition.Option.id(material.id() + "/" + part.id() + "_to_magnetic"))
                    .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.MAGNETIZING))
                    .recipeDefinition(RecipeDefinition.Option.inputItem(itemId(material, part), 1))
                    .recipeDefinition(RecipeDefinition.Option.outputItem(magneticItemId(material, part), 1))
                    .recipeDefinition(RecipeDefinition.Option.duration(ONE_MINUTE))
                    .recipeDefinition(RecipeDefinition.Option.circuit(1))
                    .recipeDefinition(RecipeDefinition.Option.tier(productionTier(material)))
                    .save(output);
        }
    }

    /** Every calculated gem gets the same dust -> rough gem finishing step. */
    private static void buildGemCrystallization(RecipeOutput output, IndustrialMaterial material) {
        if (!material.properties().gemCandidate()) {
            return;
        }
        save(output, material, CERecipeTypes.CRYSTALLIZATION, 1, NINETY_SECONDS,
                MaterialPart.ROUGH_GEM, 1, part(MaterialPart.DUST, 1));
    }

    private static void buildPolishing(RecipeOutput output, IndustrialMaterial material) {
        save(output, material, CERecipeTypes.POLISHING, 1, ONE_MINUTE,
                MaterialPart.TINY_GEM, 1, part(MaterialPart.ROUGH_TINY_GEM, 1));
        save(output, material, CERecipeTypes.POLISHING, 1, ONE_MINUTE,
                MaterialPart.SMALL_GEM, 1, part(MaterialPart.ROUGH_SMALL_GEM, 1));
        save(output, material, CERecipeTypes.POLISHING, 1, ONE_MINUTE,
                MaterialPart.GEM, 1, part(MaterialPart.ROUGH_GEM, 1));
        save(output, material, CERecipeTypes.POLISHING, 1, NINETY_SECONDS,
                MaterialPart.FLAWLESS_GEM, 1, part(MaterialPart.ROUGH_FLAWLESS_GEM, 1));
        save(output, material, CERecipeTypes.POLISHING, 1, TWO_MINUTES,
                MaterialPart.EXQUISITE_GEM, 1, part(MaterialPart.ROUGH_EXQUISITE_GEM, 1));
        save(output, material, CERecipeTypes.POLISHING, 1, NINETY_SECONDS,
                MaterialPart.LENS, 1, part(MaterialPart.EXQUISITE_GEM, 1));
    }

    /** Every forgeable cold/hot pair owned by a material gets one canonical HEATING route. */
    private static void buildHotFormHeating(RecipeOutput output, IndustrialMaterial material) {
        for (MaterialPart cold : MaterialPart.values()) {
            if (cold.isHotForgePart() || !cold.isForgeableForm()) continue;
            MaterialPart hot = cold.hotForgePart();
            if (hot == null || !material.has(cold) || !material.has(hot)) continue;
            if (material.hasExistingRecipe(hot)) continue;

            int requiredTemperature = MaterialPropertyCalculator.temperatureFor(material.properties(), hot);
            if (requiredTemperature <= 0) continue;

            RecipeDefinition.recipe()
                    .recipeDefinition(RecipeDefinition.Option.id(
                            material.id() + "/heating/" + cold.id() + "_to_" + hot.id()))
                    .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.HEATING))
                    .recipeDefinition(RecipeDefinition.Option.inputItem(itemId(material, cold), 1))
                    .recipeDefinition(RecipeDefinition.Option.outputItem(itemId(material, hot), 1))
                    .recipeDefinition(RecipeDefinition.Option.duration(FoundryMetallurgy.duration(
                            material, cold.materialAmountMb(), requiredTemperature)))
                    .recipeDefinition(RecipeDefinition.Option.temperature(requiredTemperature))
                    .recipeDefinition(RecipeDefinition.Option.tier(productionTier(material)))
                    .save(output);
        }
    }

    /**
     * Manual Assembly no longer performs raw metal shape conversion. Cutting a plate into rods is
     * a real cutting process and therefore uses the hot forms. One 144 mB plate is conserved as
     * four 32 mB short rods plus one 16 mB very-short rod.
     */
    private static void buildHotMetalCutting(RecipeOutput output, IndustrialMaterial material) {
        if (!material.has(MaterialPart.HOT_PLATE)
                || !material.has(MaterialPart.HOT_SHORT_ROD)
                || !material.has(MaterialPart.HOT_VERY_SHORT_ROD)
                || material.hasExistingRecipe(MaterialPart.SHORT_ROD)
                || material.hasExistingRecipe(MaterialPart.VERY_SHORT_ROD)) {
            return;
        }

        RecipeDefinition.recipe()
                .recipeDefinition(RecipeDefinition.Option.id(material.id() + "/hot_plate_to_hot_rods"))
                .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.CUTTING))
                .recipeDefinition(RecipeDefinition.Option.inputItem(itemId(material, MaterialPart.HOT_PLATE), 1))
                .recipeDefinition(RecipeDefinition.Option.outputItem(itemId(material, MaterialPart.HOT_SHORT_ROD), 4))
                .recipeDefinition(RecipeDefinition.Option.outputItem(itemId(material, MaterialPart.HOT_VERY_SHORT_ROD), 1))
                .recipeDefinition(RecipeDefinition.Option.duration(NINETY_SECONDS))
                .recipeDefinition(RecipeDefinition.Option.circuit(1))
                .recipeDefinition(RecipeDefinition.Option.tier(productionTier(material)))
                .save(output);
    }

    private static void buildBuzzSawCutting(RecipeOutput output, IndustrialMaterial material) {
        // Metal cutting uses hot input and keeps the result hot until a cooling process runs.
        save(output, material, CERecipeTypes.CUTTING, 1, NINETY_SECONDS,
                MaterialPart.HOT_BUZZ_SAW, 1, part(MaterialPart.HOT_GEAR, 1));
    }

    private static void save(
            RecipeOutput output,
            IndustrialMaterial material,
            RecipeTypeDefinition type,
            int circuit,
            int duration,
            MaterialPart outputPart,
            int outputCount,
            PartStack... inputs
    ) {
        if (outputCount < 1) throw new IllegalArgumentException("Recipe output count must be >= 1");
        if (!material.has(outputPart) || material.hasExistingRecipe(outputPart)) {
            return;
        }
        for (PartStack input : inputs) {
            if (!material.has(input.part())) {
                return;
            }
        }
        if (inputs.length > type.maxItemInputs() || type.maxItemOutputs() < 1) {
            throw new IllegalStateException("Generated " + type.id() + " recipe exceeds its declared item IO limits");
        }

        int divisor = outputCount;
        for (PartStack input : inputs) divisor = greatestCommonDivisor(divisor, input.count());
        int reducedOutputCount = outputCount / divisor;

        StringBuilder recipeId = new StringBuilder(material.id()).append('/');
        for (int index = 0; index < inputs.length; index++) {
            if (index > 0) recipeId.append("_and_");
            recipeId.append(inputs[index].part().id());
        }
        recipeId.append("_to_").append(outputPart.id());

        RecipeDefinition recipe = RecipeDefinition.recipe()
                .recipeDefinition(RecipeDefinition.Option.id(recipeId.toString()))
                .recipeDefinition(RecipeDefinition.Option.recipeType(type))
                .recipeDefinition(RecipeDefinition.Option.outputItem(itemId(material, outputPart), reducedOutputCount))
                .recipeDefinition(RecipeDefinition.Option.duration(duration))
                .recipeDefinition(RecipeDefinition.Option.circuit(circuit))
                .recipeDefinition(RecipeDefinition.Option.tier(productionTier(material)));

        for (PartStack input : inputs) {
            recipe.recipeDefinition(RecipeDefinition.Option.inputItem(
                    itemId(material, input.part()),
                    input.count() / divisor
            ));
        }
        recipe.save(output);
    }

    private static int greatestCommonDivisor(int first, int second) {
        int left = Math.abs(first);
        int right = Math.abs(second);
        while (right != 0) {
            int remainder = left % right;
            left = right;
            right = remainder;
        }
        return Math.max(1, left);
    }

    /** Tiny, small, normal, large and huge forms should not all take exactly the same time. */
    private static int sizeDuration(int normalDuration, int sizeIndex) {
        int[] percent = {50, 75, 100, 150, 200};
        if (sizeIndex < 0 || sizeIndex >= percent.length) {
            throw new IllegalArgumentException("Unknown material form size index: " + sizeIndex);
        }
        return Math.max(1, (normalDuration * percent[sizeIndex] + 99) / 100);
    }

    private static PartStack part(MaterialPart part, int count) {
        return new PartStack(part, count);
    }

    private static String itemId(IndustrialMaterial material, MaterialPart part) {
        if (material.hasExistingPart(part)) {
            return material.existingPart(part).toString();
        }
        return Industron.MOD_ID + ":" + part.registryName(material);
    }

    private static String magneticItemId(IndustrialMaterial material, MaterialPart part) {
        return Industron.MOD_ID + ":" + part.magneticRegistryName(material);
    }

    private static MachineTier productionTier(IndustrialMaterial material) {
        return MaterialProcessingRules.processingTier(material);
    }

    private static boolean isFunctionalWire(MaterialPart part) {
        return WireThickness.ALL.stream().anyMatch(thickness -> thickness.materialPart() == part);
    }

    private record PartStack(MaterialPart part, int count) {
    }

}
