package net.mads.industron.material.recipes;

import net.mads.industron.Industron;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.material.ClayMaterialRules;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.material.MaterialCatalog;
import net.mads.industron.material.MaterialComponent;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.MaterialProperties;
import net.mads.industron.material.MaterialUnits;
import net.mads.industron.material.defenitions.IndustrialMaterials;
import net.mads.industron.recipe.CERecipeTypes;
import net.mads.industron.recipe.RecipeDefinition;
import net.mads.industron.recipe.recipes.assembly.Tool;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.level.ItemLike;

import java.util.LinkedHashMap;
import java.util.Map;

/** Generates the complete forming, firing and recycling chain for every clay definition. */
public final class ClayProcessingRecipes {
    private ClayProcessingRecipes() {
    }

    public static void build(RecipeOutput output) {
        for (IndustrialMaterial material : IndustrialMaterials.ALL) {
            if (material.isCeramicBrickMaterial()) {
                buildDirectCeramicBrickFiring(output, material);
                continue;
            }
            if (!material.isClayMaterial()) continue;

            buildDustFromComponents(output, material);
            if (MaterialProcessingRules.allowsPrimitive(material.tier())) {
                buildPrimitiveClayFromComponents(output, material);
            }

            recipe(material, CERecipeTypes.COMPACTING, "clay_to_clay_block", 20 * 30)
                    .recipeDefinition(RecipeDefinition.Option.inputItem(itemId(material, MaterialPart.CLAY), ClayMaterialRules.CLAY_PER_BLOCK))
                    .recipeDefinition(RecipeDefinition.Option.outputItem(itemId(material, MaterialPart.CLAY_BLOCK), 1))
                    .save(output);

            recipe(material, CERecipeTypes.DECOMPACTING, "clay_block_to_clay", 20 * 30)
                    .recipeDefinition(RecipeDefinition.Option.inputItem(itemId(material, MaterialPart.CLAY_BLOCK), 1))
                    .recipeDefinition(RecipeDefinition.Option.outputItem(itemId(material, MaterialPart.CLAY), ClayMaterialRules.CLAY_PER_BLOCK))
                    .save(output);

            recipe(material, CERecipeTypes.EXTRUDING, "clay_to_unfired_brick", 20 * 20)
                    .recipeDefinition(RecipeDefinition.Option.inputItem(itemId(material, MaterialPart.CLAY), 1))
                    .recipeDefinition(RecipeDefinition.Option.outputItem(itemId(material, MaterialPart.UNFIRED_BRICK), 1))
                    .save(output);

            recipe(material, CERecipeTypes.SINTERING, "unfired_brick_to_brick", 20 * 90)
                    .recipeDefinition(RecipeDefinition.Option.inputItem(itemId(material, MaterialPart.UNFIRED_BRICK), 1))
                    .recipeDefinition(RecipeDefinition.Option.outputItem(itemId(material, MaterialPart.BRICK), 1))
                    .recipeDefinition(RecipeDefinition.Option.temperature(ClayMaterialRules.firingTemperature(material)))
                    .save(output);

            // ULV/LV primitive Mortaring emits these exact automated Grinding IDs.
            // Higher-tier clay keeps the machine-only Grinding path.
            if (!BasinMortaringRecipes.supportsTier(material.tier())) {
                recipe(material, CERecipeTypes.GRINDING, "brick_to_dust", 20 * 45)
                        .recipeDefinition(RecipeDefinition.Option.inputItem(itemId(material, MaterialPart.BRICK), 1))
                        .recipeDefinition(RecipeDefinition.Option.outputItem(itemId(material, MaterialPart.DUST), 1))
                        .save(output);

                recipe(material, CERecipeTypes.GRINDING, "cracked_brick_to_small_dust", 20 * 35)
                        .recipeDefinition(RecipeDefinition.Option.inputItem(itemId(material, MaterialPart.CRACKED_BRICK), 1))
                        .recipeDefinition(RecipeDefinition.Option.outputItem(itemId(material, MaterialPart.SMALL_DUST), 2))
                        .save(output);
            }

            if (MaterialProcessingRules.allowsPrimitive(material.tier())) {
                primitiveRecipe(material, CERecipeTypes.BRICK_MOLDING, "mold_brick")
                        .recipeDefinition(RecipeDefinition.Option.inputItem(itemId(material, MaterialPart.CLAY), ClayMaterialRules.MOLD_CLAY_COUNT))
                        .recipeDefinition(RecipeDefinition.Option.outputItem(itemId(material, MaterialPart.UNFIRED_BRICK), 1))
                        .tool(Tool.SHOVEL, ClayMaterialRules.MOLDING_TOOL_USES)
                        .save(output);

                // Wet and rack-dried bricks are separate generated items, so the primitive chain cannot skip drying.
                primitiveRecipe(material, CERecipeTypes.RACK_DRYING, "sun_dry_unfired_brick", ClayMaterialRules.SUN_DRYING_DURATION_TICKS)
                        .recipeDefinition(RecipeDefinition.Option.inputItem(itemId(material, MaterialPart.UNFIRED_BRICK), 1))
                        .recipeDefinition(RecipeDefinition.Option.outputItem(itemId(material, MaterialPart.DRIED_UNFIRED_BRICK), 1))
                        .save(output);

                primitiveRecipe(material, CERecipeTypes.KILN_FIRING, "kiln_fire_brick", ClayMaterialRules.KILN_FIRING_DURATION_TICKS)
                        .recipeDefinition(RecipeDefinition.Option.inputItem(itemId(material, MaterialPart.DRIED_UNFIRED_BRICK), 1))
                        .recipeDefinition(RecipeDefinition.Option.outputItem(itemId(material, MaterialPart.BRICK), 1))
                        .save(output);

                // This second deterministic stage starts only if a fired brick remains while fuel burns.
                primitiveRecipe(material, CERecipeTypes.KILN_FIRING, "kiln_overfire_brick", ClayMaterialRules.KILN_OVERFIRE_DURATION_TICKS)
                        .recipeDefinition(RecipeDefinition.Option.inputItem(itemId(material, MaterialPart.BRICK), 1))
                        .recipeDefinition(RecipeDefinition.Option.outputItem(itemId(material, MaterialPart.CRACKED_BRICK), 1))
                        .save(output);
            }
        }
    }


    /**
     * Direct ceramic-brick chain used by materials such as Nether Brick. The material definition
     * owns BRICK/CRACKED_BRICK only; its raw source is intentionally recipe-specific.
     */
    private static void buildDirectCeramicBrickFiring(RecipeOutput output, IndustrialMaterial material) {
        if (!"nether".equals(material.id())) {
            throw new IllegalStateException("Unhandled direct ceramic brick material: " + material.id());
        }

        if (MaterialProcessingRules.allowsPrimitive(material.tier())) {
            primitiveRecipe(material, CERecipeTypes.KILN_FIRING, "kiln_fire_brick", ClayMaterialRules.KILN_FIRING_DURATION_TICKS)
                    .recipeDefinition(RecipeDefinition.Option.inputItem("minecraft:netherrack", 1))
                    .recipeDefinition(RecipeDefinition.Option.outputItem(itemId(material, MaterialPart.BRICK), 1))
                    .save(output);

            primitiveRecipe(material, CERecipeTypes.KILN_FIRING, "kiln_overfire_brick", ClayMaterialRules.KILN_OVERFIRE_DURATION_TICKS)
                    .recipeDefinition(RecipeDefinition.Option.inputItem(itemId(material, MaterialPart.BRICK), 1))
                    .recipeDefinition(RecipeDefinition.Option.outputItem(itemId(material, MaterialPart.CRACKED_BRICK), 1))
                    .save(output);
        } else {
            recipe(material, CERecipeTypes.SINTERING, "netherrack_to_brick", ClayMaterialRules.KILN_FIRING_DURATION_TICKS)
                    .recipeDefinition(RecipeDefinition.Option.inputItem("minecraft:netherrack", 1))
                    .recipeDefinition(RecipeDefinition.Option.outputItem(itemId(material, MaterialPart.BRICK), 1))
                    .recipeDefinition(RecipeDefinition.Option.temperature(ClayMaterialRules.firingTemperature(material)))
                    .save(output);
        }
    }

    private static void buildDustFromComponents(RecipeOutput output, IndustrialMaterial material) {
        int divisor = 0;
        for (MaterialComponent component : material.components()) {
            divisor = greatestCommonDivisor(divisor, component.amount());
        }
        divisor = Math.max(1, divisor);

        Map<ItemLike, Integer> itemInputs = new LinkedHashMap<>();
        Map<String, Integer> fluidInputs = new LinkedHashMap<>();
        int outputCount = 0;

        for (MaterialComponent component : material.components()) {
            int units = component.amount() / divisor;
            outputCount = Math.addExact(outputCount, units);

            MaterialRecipeHelper.ComponentIngredient ingredient =
                    MaterialRecipeHelper.componentIngredient(component.substance());
            if (ingredient instanceof MaterialRecipeHelper.ItemIngredient item) {
                itemInputs.merge(item.item(), units, Math::addExact);
            } else if (ingredient instanceof MaterialRecipeHelper.FluidIngredient fluid) {
                fluidInputs.merge(fluid.fluidId(), MaterialUnits.toMillibuckets(units, fluid.phase()), Math::addExact);
            }
        }

        if (itemInputs.isEmpty() && fluidInputs.isEmpty()) {
            throw new IllegalStateException("Clay " + material.id() + " has no resolvable composition inputs");
        }
        if (itemInputs.size() > CERecipeTypes.MIXING.maxItemInputs()) {
            throw new IllegalStateException("Clay " + material.id() + " needs " + itemInputs.size()
                    + " distinct item ingredients, but Mixing supports only "
                    + CERecipeTypes.MIXING.maxItemInputs());
        }
        if (fluidInputs.size() > CERecipeTypes.MIXING.maxFluidInputs()) {
            throw new IllegalStateException("Clay " + material.id() + " needs " + fluidInputs.size()
                    + " distinct fluid/gas ingredients, but Mixing supports only "
                    + CERecipeTypes.MIXING.maxFluidInputs());
        }
        if (outputCount > 64 || itemInputs.values().stream().anyMatch(amount -> amount > 64)) {
            throw new IllegalStateException("Clay " + material.id() + " composition exceeds item stack limits");
        }

        RecipeDefinition definition = recipe(material, CERecipeTypes.MIXING, "components_to_dust", 20 * 40);
        for (Map.Entry<ItemLike, Integer> input : itemInputs.entrySet()) {
            definition.recipeDefinition(RecipeDefinition.Option.inputItem(input.getKey(), input.getValue()));
        }
        for (Map.Entry<String, Integer> input : fluidInputs.entrySet()) {
            definition.recipeDefinition(RecipeDefinition.Option.inputFluid(input.getKey(), input.getValue()));
        }
        definition.recipeDefinition(RecipeDefinition.Option.outputItem(itemId(material, MaterialPart.DUST), outputCount))
                .recipeDefinition(RecipeDefinition.Option.circuit(3))
                .save(output);
    }

    /**
     * Primitive basin route: the declared solid components enter as dust, ambient liquids enter
     * as fluids, and water turns the smallest whole composition batch directly into clay.
     * Basin recipes are automatically copied to the main mixer by BasinMixingRecipeType.
     */
    private static void buildPrimitiveClayFromComponents(RecipeOutput output, IndustrialMaterial material) {
        int divisor = compositionDivisor(material);
        Map<ItemLike, Integer> itemInputs = new LinkedHashMap<>();
        Map<String, Integer> fluidInputs = new LinkedHashMap<>();
        int clayOutput = 0;

        for (MaterialComponent component : material.components()) {
            int units = component.amount() / divisor;
            clayOutput = Math.addExact(clayOutput, units);
            if (isGas(component.substance())) {
                throw new IllegalStateException("Clay " + material.id() + " contains gas "
                        + component.substance().id() + "; a primitive basin accepts liquids, not gases");
            }

            MaterialRecipeHelper.ComponentIngredient ingredient =
                    MaterialRecipeHelper.componentIngredient(component.substance());
            if (ingredient instanceof MaterialRecipeHelper.ItemIngredient dust) {
                itemInputs.merge(dust.item(), units, Math::addExact);
            } else if (ingredient instanceof MaterialRecipeHelper.FluidIngredient liquid) {
                fluidInputs.merge(liquid.fluidId(), MaterialUnits.toMillibuckets(units, liquid.phase()), Math::addExact);
            }
        }

        fluidInputs.merge(
                "minecraft:water",
                ClayMaterialRules.WATER_PER_CLAY_MB,
                Math::addExact
        );
        validatePrimitiveBasinComposition(material, itemInputs, fluidInputs, clayOutput);

        RecipeDefinition definition = primitiveRecipe(
                material,
                CERecipeTypes.BASIN_MIXING,
                "component_dusts_to_clay"
        );
        for (Map.Entry<ItemLike, Integer> input : itemInputs.entrySet()) {
            definition.recipeDefinition(RecipeDefinition.Option.inputItem(input.getKey(), input.getValue()));
        }
        for (Map.Entry<String, Integer> input : fluidInputs.entrySet()) {
            definition.recipeDefinition(RecipeDefinition.Option.inputFluid(input.getKey(), input.getValue()));
        }
        definition.recipeDefinition(RecipeDefinition.Option.outputItem(
                        itemId(material, MaterialPart.CLAY),
                        clayOutput
                ))
                .recipeDefinition(RecipeDefinition.Option.duration(
                        MaterialProcessingRules.mixingDuration(material, clayOutput)
                ))
                .tool(Tool.SHOVEL, ClayMaterialRules.BASIN_MIXING_TOOL_USES)
                .save(output);
    }

    private static int compositionDivisor(IndustrialMaterial material) {
        int divisor = 0;
        for (MaterialComponent component : material.components()) {
            divisor = greatestCommonDivisor(divisor, component.amount());
        }
        return Math.max(1, divisor);
    }

    private static boolean isGas(IndustrialSubstance substance) {
        IndustrialMaterial material = substance instanceof IndustrialMaterial direct
                ? direct
                : MaterialCatalog.find(substance.id());
        if (material == null) return false;
        // Match MaterialRecipeHelper.componentIngredient(...): DUST wins, then LIQUID, then GAS.
        // Basin rejects a component only when GAS is the representation that would actually be used.
        return !material.has(MaterialPart.DUST)
                && !material.has(MaterialPart.LIQUID)
                && material.has(MaterialPart.GAS);
    }

    private static void validatePrimitiveBasinComposition(
            IndustrialMaterial material,
            Map<ItemLike, Integer> itemInputs,
            Map<String, Integer> fluidInputs,
            int outputCount
    ) {
        if (itemInputs.isEmpty()) {
            throw new IllegalStateException("Clay " + material.id() + " has no solid dust input for primitive mixing");
        }
        if (itemInputs.size() > CERecipeTypes.BASIN_MIXING.maxItemInputs()) {
            throw new IllegalStateException("Clay " + material.id() + " needs " + itemInputs.size()
                    + " distinct dust inputs, but Basin Mixing supports only "
                    + CERecipeTypes.BASIN_MIXING.maxItemInputs());
        }
        if (fluidInputs.size() > CERecipeTypes.BASIN_MIXING.maxFluidInputs()) {
            throw new IllegalStateException("Clay " + material.id() + " needs " + fluidInputs.size()
                    + " distinct liquid inputs including water, but Basin Mixing supports only "
                    + CERecipeTypes.BASIN_MIXING.maxFluidInputs());
        }
        if (outputCount > 64 || itemInputs.values().stream().anyMatch(amount -> amount > 64)) {
            throw new IllegalStateException("Clay " + material.id() + " primitive batch exceeds item stack limits");
        }
    }

    private static int greatestCommonDivisor(int left, int right) {
        left = Math.abs(left);
        right = Math.abs(right);
        while (right != 0) {
            int remainder = left % right;
            left = right;
            right = remainder;
        }
        return left;
    }

    private static RecipeDefinition recipe(
            IndustrialMaterial material,
            net.mads.industron.recipe.RecipeTypeDefinition type,
            String suffix,
            int duration
    ) {
        return RecipeDefinition.recipe()
                .recipeDefinition(RecipeDefinition.Option.id(material.id() + "/" + suffix))
                .recipeDefinition(RecipeDefinition.Option.recipeType(type))
                .recipeDefinition(RecipeDefinition.Option.duration(duration))
                .recipeDefinition(RecipeDefinition.Option.tier(productionTier(material)));
    }

    private static RecipeDefinition primitiveRecipe(
            IndustrialMaterial material,
            net.mads.industron.recipe.RecipeTypeDefinition type,
            String suffix
    ) {
        return RecipeDefinition.recipe()
                .recipeDefinition(RecipeDefinition.Option.id("primitive/" + material.id() + "/" + suffix))
                .recipeDefinition(RecipeDefinition.Option.recipeType(type))
                .recipeDefinition(RecipeDefinition.Option.tier(productionTier(material)));
    }

    private static RecipeDefinition primitiveRecipe(
            IndustrialMaterial material,
            net.mads.industron.recipe.RecipeTypeDefinition type,
            String suffix,
            int duration
    ) {
        return primitiveRecipe(material, type, suffix)
                .recipeDefinition(RecipeDefinition.Option.duration(duration));
    }

    private static String itemId(IndustrialMaterial material, MaterialPart part) {
        return material.hasExistingPart(part)
                ? material.existingPart(part).toString()
                : Industron.MOD_ID + ":" + part.registryName(material);
    }

    private static MachineTier productionTier(IndustrialMaterial material) {
        return MaterialProcessingRules.processingTier(material);
    }
}
