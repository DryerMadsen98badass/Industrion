package net.mads.industron.recipe.recipetypes;

import net.mads.industron.material.MaterialUnits;
import net.mads.industron.recipe.CERecipe;
import net.mads.industron.recipe.CERecipeInput;
import net.mads.industron.recipe.CERecipeLookup;
import net.mads.industron.recipe.CERecipeTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

/**
 * Shared runtime lookup for the machine-independent {@code industron:fuel} recipe table.
 *
 * <p>A fuel recipe describes only the fuel value of one registered input. Consumers decide how
 * many ticks, heat, steam, or other work one Fuel Unit provides. This keeps Kiln, future boilers,
 * fireboxes and other burners on the same generated fuel truth.</p>
 */
public final class FuelRecipeLookup {
    private FuelRecipeLookup() {
    }

    /** Fuel Units represented by the supplied item stack, or 0 when it is not a registered fuel. */
    public static double itemFuelUnits(Level level, ItemStack stack) {
        if (level == null || stack == null || stack.isEmpty()) return 0.0D;
        CERecipeInput input = CERecipeInput.of(List.of(stack), List.of());
        return findUnits(level, input, true);
    }

    /**
     * Fuel Units represented by the supplied fluid stack, or 0 when it is not a registered fuel.
     * Generated liquid fuel recipes consume 144 mB per material unit and gas fuel recipes consume
     * 576 mB per material unit; callers must provide at least the recipe amount for a match.
     */
    public static double fluidFuelUnits(Level level, FluidStack stack) {
        if (level == null || stack == null || stack.isEmpty()) return 0.0D;
        CERecipeInput input = CERecipeInput.of(List.of(), List.of(stack));
        return findUnits(level, input, false);
    }

    private static double findUnits(Level level, CERecipeInput input, boolean item) {
        return CERecipeLookup.byType(level.getRecipeManager(), CERecipeTypes.FUEL).stream()
                .map(RecipeHolder::value)
                .filter(recipe -> validFuelShape(recipe, item))
                .filter(recipe -> recipe.matches(input, level))
                .mapToDouble(recipe -> recipe.fuelUnits().orElse(0.0D))
                .filter(units -> units > 0.0D)
                .findFirst()
                .orElse(0.0D);
    }

    private static boolean validFuelShape(CERecipe recipe, boolean item) {
        if (recipe.fuelUnits().orElse(0.0D) <= 0.0D) return false;
        return item
                ? recipe.itemInputs().size() == 1
                        && recipe.itemInputs().getFirst().count() == 1
                        && recipe.fluidInputs().isEmpty()
                : recipe.itemInputs().isEmpty()
                        && recipe.fluidInputs().size() == 1
                        && (recipe.fluidInputs().getFirst().amount() == MaterialUnits.LIQUID_MILLIBUCKETS_PER_UNIT
                        || recipe.fluidInputs().getFirst().amount() == MaterialUnits.GAS_MILLIBUCKETS_PER_UNIT);
    }
}
