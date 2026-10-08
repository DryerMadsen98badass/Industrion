package net.mads.industron.data;

import com.simibubi.create.content.equipment.sandPaper.SandPaperPolishingRecipe;
import com.simibubi.create.content.kinetics.crusher.CrushingRecipe;
import com.simibubi.create.content.kinetics.fan.processing.SplashingRecipe;
import com.simibubi.create.content.kinetics.millstone.MillingRecipe;
import com.simibubi.create.content.kinetics.mixer.CompactingRecipe;
import com.simibubi.create.content.kinetics.mixer.MixingRecipe;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.kinetics.saw.CuttingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import net.mads.industron.Industron;
import net.mads.industron.material.CompoundMaterialPropertyCalculator;
import net.mads.industron.material.MaterialLookup;
import net.mads.industron.material.MaterialProperties;
import net.mads.industron.material.MaterialPropertyCalculator;
import net.mads.industron.material.structure.StructureMaterial;
import net.mads.industron.material.structure.StructureMaterialItem;
import net.mads.industron.material.structure.StructureMaterials;
import net.mads.industron.recipe.CEChancedItemOutput;
import net.mads.industron.recipe.CERecipe;
import net.mads.industron.recipe.CERecipeTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.SizedIngredient;


/**
 * Generates Create executor recipes from canonical Industron CE recipes.
 *
 * <p>Industron remains the source of truth. Create recipes are emitted only for ULV/LV recipes
 * whose requirements can be represented without dropping Industron constraints.</p>
 */
public final class CreateRecipeBridge {
    private static final int MAX_BASIN_ITEM_INPUTS = 64;
    private static final int MAX_BASIN_ITEM_OUTPUTS = 4;
    private static final int MAX_BASIN_FLUID_INPUTS = 2;
    private static final int MAX_BASIN_FLUID_OUTPUTS = 2;

    // Pressing is intentionally limited to genuinely soft/formable materials.
    private static final double MAX_PRESS_HARDNESS_SCORE = 40.0D;
    private static final int MIN_PRESS_FORMABILITY = 60;

    private CreateRecipeBridge() {
    }

    public static void save(RecipeOutput output, ResourceLocation sourceId, CERecipe recipe) {
        CreateKind kind = kind(recipe);
        if (kind == null || !isLowTier(recipe) || !canRepresentExactly(recipe, kind)) {
            return;
        }
        // Create's Mechanical Saw is a wood/organic executor in Industron. Metal CUTTING
        // remains a real machine process and must never be exported to Create's saw.
        if (kind == CreateKind.CUTTING && hasIndustrialMaterialInput(recipe)) {
            return;
        }
        if (kind == CreateKind.PRESSING && !isPressable(recipe)) {
            return;
        }

        ResourceLocation bridgeId = ResourceLocation.fromNamespaceAndPath(
                Industron.MOD_ID,
                "create_bridge/" + sourceId.getPath()
        );

        StandardProcessingRecipe.Builder<?> builder = builder(kind, bridgeId);
        builder.withItemIngredients(expandInputs(recipe));
        if (!recipe.fluidInputs().isEmpty()) {
            builder.withFluidIngredients(nonNullCopy(recipe.fluidInputs()));
        }

        NonNullList<ProcessingOutput> outputs = NonNullList.create();
        for (CEChancedItemOutput outputItem : recipe.itemOutputs()) {
            if (outputItem.chance() <= 0) continue;
            outputs.add(new ProcessingOutput(
                    outputItem.stack().copy(),
                    outputItem.effectiveChance(java.util.Optional.of(net.mads.industron.integration.create.kinetic.KineticProcessingRules.TIER), recipe.requiredTier()) / (float) CEChancedItemOutput.MAX_CHANCE
            ));
        }
        builder.withItemOutputs(outputs);
        if (!recipe.fluidOutputs().isEmpty()) {
            builder.withFluidOutputs(nonNullCopy(recipe.fluidOutputs()));
        }

        // Export reference work ticks, not Create's native machine-specific speed scaling.
        int lvDuration = recipe.runtimeDuration(net.mads.industron.integration.create.kinetic.KineticProcessingRules.TIER,
                net.mads.industron.machine.MachineDrive.NONE, 0);
        if (kind.supportsDuration || kind == CreateKind.PRESSING)
            builder.duration(net.mads.industron.integration.create.kinetic.KineticProcessingRules.referenceDuration(lvDuration));
        builder.build(output);
    }

    private static CreateKind kind(CERecipe recipe) {
        ResourceLocation type = recipe.recipeType();
        if (type.equals(CERecipeTypes.GRINDING.id())) return CreateKind.MILLING;
        if (type.equals(CERecipeTypes.CRUSHING.id())) return CreateKind.CRUSHING;
        if (type.equals(CERecipeTypes.MIXING.id())) return CreateKind.MIXING;
        if (type.equals(CERecipeTypes.WASHING.id())) return CreateKind.SPLASHING;
        if (type.equals(CERecipeTypes.CUTTING.id())) return CreateKind.CUTTING;
        if (type.equals(CERecipeTypes.POLISHING.id())) return CreateKind.POLISHING;
        if (type.equals(CERecipeTypes.COMPACTING.id())) return CreateKind.COMPACTING;
        if (type.equals(CERecipeTypes.ROLLING.id())) return CreateKind.PRESSING;
        return null;
    }

    private static boolean isLowTier(CERecipe recipe) {
        return recipe.requiredTier().map(net.mads.industron.integration.create.kinetic.KineticProcessingRules::supportsTier).orElse(false);
    }

    /** Reject anything Create cannot encode without weakening the canonical recipe. */
    private static boolean canRepresentExactly(CERecipe recipe, CreateKind kind) {
        boolean splashingWaterMedium = kind == CreateKind.SPLASHING
                && hasOnlySplashingWaterMedium(recipe);
        if (!recipe.chancedItemInputs().isEmpty()
                || !recipe.chancedFluidInputs().isEmpty()
                || !recipe.notConsumableItems().isEmpty()
                || (!recipe.notConsumableFluids().isEmpty() && !splashingWaterMedium)
                || !recipe.tools().isEmpty()
                || !recipe.chancedFluidOutputs().isEmpty()
                || recipe.treeSource().isPresent()
                || recipe.manualUses().isPresent()
                || recipe.fuelUnits().isPresent()
                || recipe.circuit().isPresent()
                || recipe.minRpm().isPresent()
                || recipe.maxRpm().isPresent()
                || recipe.outputRpm().isPresent()
                || recipe.requiredTemp().isPresent()
                || recipe.chemicalBalanceRange().isPresent()
                || !recipe.requiredLogic().isEmpty()
                || !recipe.optionalLogic().isEmpty()
                || !recipe.blockInteractions().isEmpty()
                || !recipe.conditions().isEmpty()
                || !recipe.modifiers().isEmpty()
                || recipe.furnaceFuel()) {
            return false;
        }
        if (recipe.itemOutputs().stream().anyMatch(out -> out.tierBonus() != 0 || out.stack().getCount() > 99)) {
            return false;
        }

        int itemInputs = expandedInputCount(recipe);
        int itemOutputs = (int) recipe.itemOutputs().stream().filter(out -> out.chance() > 0).count();
        int fluidInputs = recipe.fluidInputs().size();
        int fluidOutputs = recipe.fluidOutputs().size();

        if (kind.basin) {
            return itemOutputs + fluidOutputs > 0
                    && itemInputs <= MAX_BASIN_ITEM_INPUTS
                    && itemOutputs <= MAX_BASIN_ITEM_OUTPUTS
                    && fluidInputs <= MAX_BASIN_FLUID_INPUTS
                    && fluidOutputs <= MAX_BASIN_FLUID_OUTPUTS;
        }
        return itemInputs == 1
                && itemOutputs > 0
                && fluidInputs == 0
                && fluidOutputs == 0
                && itemOutputs <= kind.maxItemOutputs;
    }

    private static boolean hasOnlySplashingWaterMedium(CERecipe recipe) {
        if (recipe.notConsumableFluids().size() != 1) return false;
        var medium = recipe.notConsumableFluids().getFirst();
        return medium.amount() > 0
                && medium.test(new net.neoforged.neoforge.fluids.FluidStack(
                net.minecraft.world.level.material.Fluids.WATER, medium.amount()
        ));
    }

    private static <T> NonNullList<T> nonNullCopy(java.util.Collection<? extends T> values) {
        NonNullList<T> copy = NonNullList.create();
        copy.addAll(values);
        return copy;
    }

    private static int expandedInputCount(CERecipe recipe) {
        int total = 0;
        for (SizedIngredient input : recipe.itemInputs()) {
            if (input.count() <= 0) return Integer.MAX_VALUE;
            total += input.count();
            if (total > MAX_BASIN_ITEM_INPUTS) return total;
        }
        return total;
    }

    private static NonNullList<Ingredient> expandInputs(CERecipe recipe) {
        NonNullList<Ingredient> result = NonNullList.create();
        for (SizedIngredient input : recipe.itemInputs()) {
            for (int i = 0; i < input.count(); i++) {
                result.add(input.ingredient());
            }
        }
        return result;
    }

    private static boolean hasIndustrialMaterialInput(CERecipe recipe) {
        for (SizedIngredient input : recipe.itemInputs()) {
            ItemStack[] candidates = input.getItems();
            if (candidates.length == 0) continue;
            for (ItemStack candidate : candidates) {
                if (MaterialLookup.find(candidate) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isPressable(CERecipe recipe) {
        if (recipe.itemInputs().size() != 1 || recipe.itemInputs().getFirst().count() != 1) {
            return false;
        }
        ItemStack[] candidates = recipe.itemInputs().getFirst().getItems();
        if (candidates.length == 0) return false;

        for (ItemStack candidate : candidates) {
            MaterialProperties properties = propertiesFor(candidate);
            if (properties == null || !properties.hasProperty("hardness") || !properties.hasProperty("formability")) {
                return false;
            }
            double tierOffset = Math.max(0, properties.tierMultiplier() - 1)
                    * (double) MaterialPropertyCalculator.DEFAULT_TIER_BAND_SIZE;
            double intrinsicHardness = (properties.hardness() - tierOffset - 5.0D) / 2.5D;
            if (intrinsicHardness > MAX_PRESS_HARDNESS_SCORE
                    || properties.formability() < MIN_PRESS_FORMABILITY) {
                return false;
            }
        }
        return true;
    }

    private static MaterialProperties propertiesFor(ItemStack stack) {
        MaterialLookup.MaterialTarget industrial = MaterialLookup.find(stack);
        if (industrial != null) {
            return CompoundMaterialPropertyCalculator.propertiesFor(industrial.material());
        }
        if (stack.getItem() instanceof StructureMaterialItem structureItem) {
            return CompoundMaterialPropertyCalculator.propertiesFor(structureItem.material());
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        for (StructureMaterial material : StructureMaterials.ALL) {
            if (material.existingParts().containsValue(itemId)) {
                return CompoundMaterialPropertyCalculator.propertiesFor(material);
            }
        }
        return null;
    }

    private static StandardProcessingRecipe.Builder<?> builder(CreateKind kind, ResourceLocation id) {
        return switch (kind) {
            case MILLING -> new StandardProcessingRecipe.Builder<>(MillingRecipe::new, id);
            case CRUSHING -> new StandardProcessingRecipe.Builder<>(CrushingRecipe::new, id);
            case MIXING -> new StandardProcessingRecipe.Builder<>(MixingRecipe::new, id);
            case SPLASHING -> new StandardProcessingRecipe.Builder<>(SplashingRecipe::new, id);
            case CUTTING -> new StandardProcessingRecipe.Builder<>(CuttingRecipe::new, id);
            case POLISHING -> new StandardProcessingRecipe.Builder<>(SandPaperPolishingRecipe::new, id);
            case COMPACTING -> new StandardProcessingRecipe.Builder<>(CompactingRecipe::new, id);
            case PRESSING -> new StandardProcessingRecipe.Builder<>(PressingRecipe::new, id);
        };
    }

    private enum CreateKind {
        MILLING("milling", 4, false, true),
        CRUSHING("crushing", 7, false, true),
        MIXING("mixing", 4, true, true),
        SPLASHING("splashing", 12, false, false),
        CUTTING("cutting", 4, false, true),
        POLISHING("sandpaper_polishing", 1, false, false),
        COMPACTING("compacting", 4, true, true),
        PRESSING("pressing", 2, false, false);

        private final String path;
        private final int maxItemOutputs;
        private final boolean basin;
        private final boolean supportsDuration;

        CreateKind(String path, int maxItemOutputs, boolean basin, boolean supportsDuration) {
            this.path = path;
            this.maxItemOutputs = maxItemOutputs;
            this.basin = basin;
            this.supportsDuration = supportsDuration;
        }
    }
}
