package net.mads.industron.material.recipes;

import net.mads.industron.Industron;
import net.mads.industron.energy.WireThickness;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.machine.MachineTierStats;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.IndustrialMaterials;
import net.mads.industron.material.MaterialFormGenerator;
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
    private static final List<MaterialPart> GEARS = List.of(
            MaterialPart.TINY_GEAR,
            MaterialPart.SMALL_GEAR,
            MaterialPart.GEAR,
            MaterialPart.LARGE_GEAR,
            MaterialPart.HUGE_GEAR
    );
    private static final List<MaterialPart> ROTORS = List.of(
            MaterialPart.TINY_ROTOR,
            MaterialPart.SMALL_ROTOR,
            MaterialPart.ROTOR,
            MaterialPart.LARGE_ROTOR,
            MaterialPart.HUGE_ROTOR
    );
    private static final List<MaterialPart> BEARINGS = List.of(
            MaterialPart.TINY_BEARING,
            MaterialPart.SMALL_BEARING,
            MaterialPart.BEARING,
            MaterialPart.LARGE_BEARING,
            MaterialPart.HUGE_BEARING
    );

    private MaterialProcessingRecipes() {
    }

    public static void build(RecipeOutput output) {
        for (IndustrialMaterial material : IndustrialMaterials.ALL) {
            buildTurning(output, material);
            buildBending(output, material);
            buildWireDrawing(output, material);
            buildWinding(output, material);
            buildPrecisionMachining(output, material);
            buildAssembling(output, material);
            buildMagnetizing(output, material);
            buildPolishing(output, material);
            buildBuzzSawCutting(output, material);
        }
    }

    private static void buildTurning(RecipeOutput output, IndustrialMaterial material) {
        for (int index = 0; index < RODS.size(); index++) {
            save(output, material, CERecipeTypes.TURNING, 1, ONE_MINUTE,
                    BALLS.get(index), 2, part(RODS.get(index), 1));
            save(output, material, CERecipeTypes.TURNING, 2, ONE_MINUTE,
                    BOLTS.get(index), 8, part(RODS.get(index), 1));
            save(output, material, CERecipeTypes.TURNING, 3, ONE_MINUTE,
                    RIVETS.get(index), 16, part(RODS.get(index), 1));
            save(output, material, CERecipeTypes.TURNING, 4, ONE_MINUTE,
                    SCREWS.get(index), 8, part(BOLTS.get(index), 8));
        }
    }

    private static void buildBending(RecipeOutput output, IndustrialMaterial material) {
        for (int index = 0; index < RODS.size(); index++) {
            save(output, material, CERecipeTypes.BENDING, 1, ONE_MINUTE,
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
        save(output, material, CERecipeTypes.WINDING, 1, ONE_MINUTE,
                MaterialPart.TINY_SPRING, 1, part(MaterialPart.FINE_WIRE, 2));
        save(output, material, CERecipeTypes.WINDING, 1, ONE_MINUTE,
                MaterialPart.SMALL_SPRING, 1, part(MaterialPart.WIRE_1X, 2));
        save(output, material, CERecipeTypes.WINDING, 1, ONE_MINUTE,
                MaterialPart.SPRING, 1, part(MaterialPart.WIRE_2X, 2));
        save(output, material, CERecipeTypes.WINDING, 1, ONE_MINUTE,
                MaterialPart.LARGE_SPRING, 1, part(MaterialPart.WIRE_4X, 2));
        save(output, material, CERecipeTypes.WINDING, 1, ONE_MINUTE,
                MaterialPart.HUGE_SPRING, 1, part(MaterialPart.WIRE_8X, 2));

        save(output, material, CERecipeTypes.WINDING, 2, ONE_MINUTE,
                MaterialPart.COIL, 1, part(MaterialPart.WIRE_1X, 8));
        save(output, material, CERecipeTypes.WINDING, 2, ONE_MINUTE,
                MaterialPart.COIL, 1, part(MaterialPart.WIRE_2X, 4));
        save(output, material, CERecipeTypes.WINDING, 2, ONE_MINUTE,
                MaterialPart.COIL, 1, part(MaterialPart.WIRE_4X, 2));
        save(output, material, CERecipeTypes.WINDING, 2, ONE_MINUTE,
                MaterialPart.COIL, 1, part(MaterialPart.WIRE_8X, 1));
    }

    private static void buildPrecisionMachining(RecipeOutput output, IndustrialMaterial material) {
        save(output, material, CERecipeTypes.PRECISION_MACHINING, 1, ONE_MINUTE,
                MaterialPart.TINY_GEAR, 1,
                part(MaterialPart.VERY_SHORT_ROD, 4), part(MaterialPart.PLATE, 1));
        save(output, material, CERecipeTypes.PRECISION_MACHINING, 1, ONE_MINUTE,
                MaterialPart.SMALL_GEAR, 1,
                part(MaterialPart.SHORT_ROD, 4), part(MaterialPart.PLATE, 2));
        save(output, material, CERecipeTypes.PRECISION_MACHINING, 1, NINETY_SECONDS,
                MaterialPart.GEAR, 1,
                part(MaterialPart.ROD, 4), part(MaterialPart.PLATE, 2));
        save(output, material, CERecipeTypes.PRECISION_MACHINING, 1, NINETY_SECONDS,
                MaterialPart.LARGE_GEAR, 1,
                part(MaterialPart.LONG_ROD, 4), part(MaterialPart.PLATE, 4));
        save(output, material, CERecipeTypes.PRECISION_MACHINING, 1, TWO_MINUTES,
                MaterialPart.HUGE_GEAR, 1,
                part(MaterialPart.VERY_LONG_ROD, 4), part(MaterialPart.PLATE, 8));

        int[] rotorDurations = {ONE_MINUTE, ONE_MINUTE, NINETY_SECONDS, NINETY_SECONDS, TWO_MINUTES};
        for (int index = 0; index < ROTORS.size(); index++) {
            save(output, material, CERecipeTypes.PRECISION_MACHINING, 2, rotorDurations[index],
                    ROTORS.get(index), 1,
                    part(GEARS.get(index), 1), part(RODS.get(index), 1), part(RINGS.get(index), 1));
        }

        save(output, material, CERecipeTypes.PRECISION_MACHINING, 3, NINETY_SECONDS,
                MaterialPart.TURBINE_BLADE, 1,
                part(MaterialPart.LONG_ROD, 2),
                part(MaterialPart.ROD, 1),
                part(MaterialPart.DENSE_PLATE, 2));
    }

    private static void buildAssembling(RecipeOutput output, IndustrialMaterial material) {
        int[] bearingDurations = {ONE_MINUTE, ONE_MINUTE, ONE_MINUTE, NINETY_SECONDS, TWO_MINUTES};
        for (int index = 0; index < BEARINGS.size(); index++) {
            save(output, material, CERecipeTypes.ASSEMBLING, 1, bearingDurations[index],
                    BEARINGS.get(index), 1,
                    part(BALLS.get(index), 8), part(RINGS.get(index), 2));
        }

        save(output, material, CERecipeTypes.ASSEMBLING, 2, NINETY_SECONDS,
                MaterialPart.REINFORCED_PLATE, 1,
                part(MaterialPart.PLATE, 2), part(MaterialPart.SCREW, 4));
        save(output, material, CERecipeTypes.ASSEMBLING, 3, NINETY_SECONDS,
                MaterialPart.HEAT_EXCHANGER_PLATE, 1,
                part(MaterialPart.PLATE, 2),
                part(MaterialPart.FINE_WIRE, 32),
                part(MaterialPart.SCREW, 16));
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

    private static void buildBuzzSawCutting(RecipeOutput output, IndustrialMaterial material) {
        save(output, material, CERecipeTypes.CUTTING, 1, NINETY_SECONDS,
                MaterialPart.TOOL_HEAD_BUZZ_SAW, 1, part(MaterialPart.GEAR, 1));
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
        return MachineTierStats.offset(material.tier(), -1);
    }

    private static boolean isFunctionalWire(MaterialPart part) {
        return WireThickness.ALL.stream().anyMatch(thickness -> thickness.materialPart() == part);
    }

    private record PartStack(MaterialPart part, int count) {
    }
}
