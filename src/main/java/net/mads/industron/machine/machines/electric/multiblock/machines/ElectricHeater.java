package net.mads.industron.machine.machines.electric.multiblock.machines;

import net.mads.industron.recipe.recipes.assembly.Tool;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockAbility;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockControllerBlockEntity;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockControllerDefinition;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockDefinition;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockDefinition.Option;

import static net.mads.industron.machine.machines.electric.multiblock.MultiblockPattern.row;
import static net.mads.industron.material.recipes.MaterialCasingRecipes.MACHINE_CASING;
import static net.mads.industron.machine.machines.electric.multiblock.MultiblockPredicates.ability;
import static net.mads.industron.machine.machines.electric.multiblock.MultiblockPredicates.casing;
import static net.mads.industron.machine.machines.electric.multiblock.MultiblockPredicates.coils;
import static net.mads.industron.machine.machines.electric.multiblock.MultiblockPredicates.controller;
import static net.mads.industron.machine.machines.electric.multiblock.MultiblockPredicates.machineCasing;

/**
 * Electric heater multiblock and Foundry heat provider.
 *
 * <p>The heater is always-on while formed and powered: coil temperature and coil count define
 * its continuous CE/t demand. Heat is supplied to a matching Foundry directly above through FoundryHeatSource.</p>
 */
public final class ElectricHeater {
    private static final long CE_TEMPERATURE_DIVISOR = 100L;

    public static final MultiblockControllerDefinition CONTROLLER = MultiblockControllerDefinition.machine()
            .machineDefinition(MultiblockControllerDefinition.Option.id("electric_heater"))
            .machineDefinition(MultiblockControllerDefinition.Option.displayName("Electric Heater"))
            .machineDefinition(MultiblockControllerDefinition.Option.mineableWith(Tool.PICKAXE))
            .machineDefinition(MultiblockControllerDefinition.Option.breakingTier(MachineTier.MV))
            .machineDefinition(MultiblockControllerDefinition.Option.model(casing()))
            .machineDefinition(MultiblockControllerDefinition.Option.frontOverlay(
                    "block/machines/overlay/foundry/foundry_off",
                    "block/machines/overlay/foundry/foundry_on"
            ))
            .build();

    public static final MultiblockDefinition DEFINITION = MultiblockDefinition.machine()
            .machineDefinition(Option.id("electric_heater"))
            .machineDefinition(Option.controller(CONTROLLER))
            .machineDefinition(Option.displayName("Electric Heater"))
            .machineDefinition(Option.tooltip(
                    "Supplies heat to a formed Foundry directly above.",
                    "Requires the same footprint; coil temperature sets the heat limit.",
                    "CE/t = ceil(coil temperature × coil count / 100).",
                    "Automatically turns on while the full CE/t demand is available."
            ))
            .machineDefinition(Option.continuousEnergyUsage(ElectricHeater::maxEnergyUsagePerTick))
            .machineDefinition(Option.variant("4", pattern -> pattern
                    .layer(row('d', 'd', 'd', 'd', 'd', 'd', 'd', 'd', 'd'), row('a', 'a', 'a', 'a', 'a', 'a', 'a', 'a', 'a'))
                    .layer(row('d', 'd', 'd', 'd', 'd', 'd', 'd', 'd', 'd'), row('a', 'c', 'c', 'c', 'c', 'c', 'c', 'c', 'a'))
                    .layer(row('d', 'd', 'd', 'd', 'd', 'd', 'd', 'd', 'd'), row('a', 'c', 'c', 'c', 'c', 'c', 'c', 'c', 'a'))
                    .layer(row('d', 'd', 'd', 'd', 'd', 'd', 'd', 'd', 'd'), row('a', 'c', 'c', 'c', 'c', 'c', 'c', 'c', 'a'))
                    .layer(row('x', 'd', 'd', 'd', 'd', 'd', 'd', 'd', 'd'), row('a', 'c', 'c', 'c', 'c', 'c', 'c', 'c', 'a'))
                    .layer(row('d', 'd', 'd', 'd', 'd', 'd', 'd', 'd', 'd'), row('a', 'c', 'c', 'c', 'c', 'c', 'c', 'c', 'a'))
                    .layer(row('d', 'd', 'd', 'd', 'd', 'd', 'd', 'd', 'd'), row('a', 'c', 'c', 'c', 'c', 'c', 'c', 'c', 'a'))
                    .layer(row('d', 'd', 'd', 'd', 'd', 'd', 'd', 'd', 'd'), row('a', 'c', 'c', 'c', 'c', 'c', 'c', 'c', 'a'))
                    .layer(row('d', 'd', 'd', 'd', 'd', 'd', 'd', 'd', 'd'), row('a', 'a', 'a', 'a', 'a', 'a', 'a', 'a', 'a'))
            ))
            .machineDefinition(Option.variant("3", pattern -> pattern
                    .layer(row('d', 'd', 'd', 'd', 'd', 'd', 'd'), row('a', 'a', 'a', 'a', 'a', 'a', 'a'))
                    .layer(row('d', 'd', 'd', 'd', 'd', 'd', 'd'), row('a', 'c', 'c', 'c', 'c', 'c', 'a'))
                    .layer(row('d', 'd', 'd', 'd', 'd', 'd', 'd'), row('a', 'c', 'c', 'c', 'c', 'c', 'a'))
                    .layer(row('x', 'd', 'd', 'd', 'd', 'd', 'd'), row('a', 'c', 'c', 'c', 'c', 'c', 'a'))
                    .layer(row('d', 'd', 'd', 'd', 'd', 'd', 'd'), row('a', 'c', 'c', 'c', 'c', 'c', 'a'))
                    .layer(row('d', 'd', 'd', 'd', 'd', 'd', 'd'), row('a', 'c', 'c', 'c', 'c', 'c', 'a'))
                    .layer(row('d', 'd', 'd', 'd', 'd', 'd', 'd'), row('a', 'a', 'a', 'a', 'a', 'a', 'a'))
            ))
            .machineDefinition(Option.variant("2", pattern -> pattern
                    .layer(row('d', 'd', 'd', 'd', 'd'), row('a', 'a', 'a', 'a', 'a'))
                    .layer(row('d', 'd', 'd', 'd', 'd'), row('a', 'c', 'c', 'c', 'a'))
                    .layer(row('x', 'd', 'd', 'd', 'd'), row('a', 'c', 'c', 'c', 'a'))
                    .layer(row('d', 'd', 'd', 'd', 'd'), row('a', 'c', 'c', 'c', 'a'))
                    .layer(row('d', 'd', 'd', 'd', 'd'), row('a', 'a', 'a', 'a', 'a'))
            ))
            .machineDefinition(Option.variant("1", pattern -> pattern
                    .layer(row('d', 'd', 'd'), row('a', 'a', 'a'))
                    .layer(row('x', 'd', 'd'), row('a', 'c', 'a'))
                    .layer(row('d', 'd', 'd'), row('a', 'a', 'a'))
            ))
            .machineDefinition(Option.where('a', machineCasing(MACHINE_CASING)))
            .machineDefinition(Option.where('d',
                    machineCasing(MACHINE_CASING)
                            .or(ability(MultiblockAbility.ENERGY_INPUT).max(8).model(casing()))
                            .or(ability(MultiblockAbility.REDSTONE).model(casing()))
            ))
            .machineDefinition(Option.where('c', coils()))
            .machineDefinition(Option.where('x', controller()))
            .build();

    private ElectricHeater() {
    }

    /** Continuous CE/t consumed by a coil field, based only on its defined temperature. */
    public static long maxEnergyUsagePerTick(int coilTemperature, int coilCount) {
        if (coilTemperature <= 0 || coilCount <= 0) {
            return 0L;
        }
        long numerator = (long) coilTemperature * coilCount;
        if (numerator < 0L) {
            return Long.MAX_VALUE;
        }
        return Math.max(1L, (numerator + CE_TEMPERATURE_DIVISOR - 1L) / CE_TEMPERATURE_DIVISOR);
    }

    /** Continuous CE/t demand of the current formed heater. */
    public static long maxEnergyUsagePerTick(MultiblockControllerBlockEntity controller) {
        return controller == null
                ? 0L
                : maxEnergyUsagePerTick(controller.formedCoilHeat(), controller.formedCoilCount());
    }
}
