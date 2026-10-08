package net.mads.industron.machine.machines.electric.singleblock;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.machine.SingleBlockDefinition;
import net.mads.industron.recipe.CERecipeTypes;

import java.util.List;

public final class ElectricSingleBlockMachines {
    private static final int BASE_ENERGY_USAGE = 4;

    public static final SingleBlockDefinition ALLOY_SMELTER =
            SingleBlockDefinition.machine()
                    .machineDefinition(SingleBlockDefinition.Option.id("alloy_smelter"))
                    .machineDefinition(SingleBlockDefinition.Option.displayName("Alloy Smelter"))
                    .machineDefinition(SingleBlockDefinition.Option.consumesEnergy())
                    .machineDefinition(SingleBlockDefinition.Option.tier(MachineTier.ULV))
                    .machineDefinition(SingleBlockDefinition.Option.recipeType(CERecipeTypes.ALLOYING))
                    .machineDefinition(SingleBlockDefinition.Option.slots(9, 2, 3, 2))
                    .machineDefinition(SingleBlockDefinition.Option.energyUsage(BASE_ENERGY_USAGE))
                    .machineDefinition(SingleBlockDefinition.Option.progressBar(ProgressBar.ARROW))
                    .machineDefinition(SingleBlockDefinition.Option.frontOverlay(
                            "block/machines/overlay/alloy_smelter/overlay_front",
                            "block/machines/overlay/alloy_smelter/overlay_front_active"
                    ))
                    .machineDefinition(SingleBlockDefinition.Option.tooltip("Processes alloying recipes."))
                    .build();

    public static final SingleBlockDefinition AUTOCLAVE =
            SingleBlockDefinition.machine()
                    .machineDefinition(SingleBlockDefinition.Option.id("autoclave"))
                    .machineDefinition(SingleBlockDefinition.Option.displayName("Autoclave"))
                    .machineDefinition(SingleBlockDefinition.Option.consumesEnergy())
                    .machineDefinition(SingleBlockDefinition.Option.tier(MachineTier.ULV))
                    .machineDefinition(SingleBlockDefinition.Option.recipeType(CERecipeTypes.CRYSTALLIZATION))
                    .machineDefinition(SingleBlockDefinition.Option.slots(2, 4, 3, 2))
                    .machineDefinition(SingleBlockDefinition.Option.energyUsage(BASE_ENERGY_USAGE))
                    .machineDefinition(SingleBlockDefinition.Option.progressBar(ProgressBar.ARROW))
                    .machineDefinition(SingleBlockDefinition.Option.frontOverlay(
                            "block/machines/overlay/autoclave/overlay_front",
                            "block/machines/overlay/autoclave/overlay_front_active"
                    ))
                    .machineDefinition(SingleBlockDefinition.Option.tooltip("Processes crystallization recipes."))
                    .build();

    public static final SingleBlockDefinition CENTRIFUGE =
            SingleBlockDefinition.machine()
                    .machineDefinition(SingleBlockDefinition.Option.id("centrifuge"))
                    .machineDefinition(SingleBlockDefinition.Option.displayName("Centrifuge"))
                    .machineDefinition(SingleBlockDefinition.Option.consumesEnergy())
                    .machineDefinition(SingleBlockDefinition.Option.tier(MachineTier.ULV))
                    .machineDefinition(SingleBlockDefinition.Option.recipeType(CERecipeTypes.CENTRIFUGING))
                    .machineDefinition(SingleBlockDefinition.Option.slots(9, 9, 3, 3))
                    .machineDefinition(SingleBlockDefinition.Option.energyUsage(BASE_ENERGY_USAGE))
                    .machineDefinition(SingleBlockDefinition.Option.progressBar(ProgressBar.ARROW))
                    .machineDefinition(SingleBlockDefinition.Option.frontOverlay(
                            "block/machines/overlay/centrifuge/overlay_front",
                            "block/machines/overlay/centrifuge/overlay_front_active_1",
                            "block/machines/overlay/centrifuge/overlay_front_active_2",
                            "block/machines/overlay/centrifuge/overlay_front_active_3",
                            "block/machines/overlay/centrifuge/overlay_front_active_4"
                    ))
                    .machineDefinition(SingleBlockDefinition.Option.tooltip("Processes centrifuging recipes."))
                    .build();

    public static final SingleBlockDefinition CHEMICAL_REACTOR =
            SingleBlockDefinition.machine()
                    .machineDefinition(SingleBlockDefinition.Option.id("chemical_reactor"))
                    .machineDefinition(SingleBlockDefinition.Option.displayName("Chemical Reactor"))
                    .machineDefinition(SingleBlockDefinition.Option.consumesEnergy())
                    .machineDefinition(SingleBlockDefinition.Option.tier(MachineTier.ULV))
                    .machineDefinition(SingleBlockDefinition.Option.recipeType(CERecipeTypes.CHEMICAL_REACTION))
                    .machineDefinition(SingleBlockDefinition.Option.slots(9, 9, 3, 3))
                    .machineDefinition(SingleBlockDefinition.Option.energyUsage(BASE_ENERGY_USAGE))
                    .machineDefinition(SingleBlockDefinition.Option.progressBar(ProgressBar.ARROW))
                    .machineDefinition(SingleBlockDefinition.Option.frontOverlay(
                            "block/machines/overlay/chemical_reactor/overlay_front",
                            "block/machines/overlay/chemical_reactor/overlay_front_1",
                            "block/machines/overlay/chemical_reactor/overlay_front_2",
                            "block/machines/overlay/chemical_reactor/overlay_front_3",
                            "block/machines/overlay/chemical_reactor/overlay_front_4",
                            "block/machines/overlay/chemical_reactor/overlay_front_5",
                            "block/machines/overlay/chemical_reactor/overlay_front_6"
                    ))
                    .machineDefinition(SingleBlockDefinition.Option.tooltip("Processes chemical reaction recipes."))
                    .build();

    public static final SingleBlockDefinition COMPRESSOR =
            SingleBlockDefinition.machine()
                    .machineDefinition(SingleBlockDefinition.Option.id("compressor"))
                    .machineDefinition(SingleBlockDefinition.Option.displayName("Compressor"))
                    .machineDefinition(SingleBlockDefinition.Option.consumesEnergy())
                    .machineDefinition(SingleBlockDefinition.Option.tier(MachineTier.ULV))
                    .machineDefinition(SingleBlockDefinition.Option.recipeType(CERecipeTypes.COMPRESSING))
                    .machineDefinition(SingleBlockDefinition.Option.slots(9, 2, 2, 2))
                    .machineDefinition(SingleBlockDefinition.Option.energyUsage(BASE_ENERGY_USAGE))
                    .machineDefinition(SingleBlockDefinition.Option.progressBar(ProgressBar.ARROW))
                    .machineDefinition(SingleBlockDefinition.Option.frontOverlay(
                            "block/machines/overlay/compressor/overlay_front",
                            "block/machines/overlay/compressor/overlay_front_active_1",
                            "block/machines/overlay/compressor/overlay_front_active_2",
                            "block/machines/overlay/compressor/overlay_front_active_3",
                            "block/machines/overlay/compressor/overlay_front_active_4",
                            "block/machines/overlay/compressor/overlay_front_active_5",
                            "block/machines/overlay/compressor/overlay_front_active_6",
                            "block/machines/overlay/compressor/overlay_front_active_7",
                            "block/machines/overlay/compressor/overlay_front_active_8",
                            "block/machines/overlay/compressor/overlay_front_active_9"
                    ))
                    .machineDefinition(SingleBlockDefinition.Option.tooltip("Processes compressing recipes."))
                    .build();

    public static final SingleBlockDefinition EXTRACTOR =
            SingleBlockDefinition.machine()
                    .machineDefinition(SingleBlockDefinition.Option.id("extractor"))
                    .machineDefinition(SingleBlockDefinition.Option.displayName("Extractor"))
                    .machineDefinition(SingleBlockDefinition.Option.consumesEnergy())
                    .machineDefinition(SingleBlockDefinition.Option.tier(MachineTier.ULV))
                    .machineDefinition(SingleBlockDefinition.Option.recipeType(CERecipeTypes.SOLVENT_EXTRACTION))
                    .machineDefinition(SingleBlockDefinition.Option.slots(2, 2, 4, 4))
                    .machineDefinition(SingleBlockDefinition.Option.energyUsage(BASE_ENERGY_USAGE))
                    .machineDefinition(SingleBlockDefinition.Option.progressBar(ProgressBar.ARROW))
                    .machineDefinition(SingleBlockDefinition.Option.frontOverlay(
                            "block/machines/overlay/extractor/overlay_front",
                            "block/machines/overlay/extractor/overlay_front_active_1",
                            "block/machines/overlay/extractor/overlay_front_active_2",
                            "block/machines/overlay/extractor/overlay_front_active_3",
                            "block/machines/overlay/extractor/overlay_front_active_4",
                            "block/machines/overlay/extractor/overlay_front_active_5"
                    ))
                    .machineDefinition(SingleBlockDefinition.Option.tooltip("Processes solvent extraction recipes."))
                    .build();

    public static final SingleBlockDefinition FORGE_HAMMER =
            SingleBlockDefinition.machine()
                    .machineDefinition(SingleBlockDefinition.Option.id("forge_hammer"))
                    .machineDefinition(SingleBlockDefinition.Option.displayName("Forge Hammer"))
                    .machineDefinition(SingleBlockDefinition.Option.consumesEnergy())
                    .machineDefinition(SingleBlockDefinition.Option.tier(MachineTier.ULV))
                    .machineDefinition(SingleBlockDefinition.Option.recipeType(CERecipeTypes.HAMMERING))
                    .machineDefinition(SingleBlockDefinition.Option.slots(2, 2, 0, 0))
                    .machineDefinition(SingleBlockDefinition.Option.energyUsage(BASE_ENERGY_USAGE))
                    .machineDefinition(SingleBlockDefinition.Option.progressBar(ProgressBar.ARROW))
                    // The first frame is the idle front for this texture set.
                    .machineDefinition(SingleBlockDefinition.Option.frontOverlay(
                            "block/machines/overlay/forge_hammer/overlay_front_active_1",
                            "block/machines/overlay/forge_hammer/overlay_front_active_2",
                            "block/machines/overlay/forge_hammer/overlay_front_active_3",
                            "block/machines/overlay/forge_hammer/overlay_front_active_4",
                            "block/machines/overlay/forge_hammer/overlay_front_active_5"
                    ))
                    .machineDefinition(SingleBlockDefinition.Option.tooltip("Processes hammering recipes."))
                    .build();

    public static final SingleBlockDefinition INDUCTION_CHAMBER =
            SingleBlockDefinition.machine()
                    .machineDefinition(SingleBlockDefinition.Option.id("induction_chamber"))
                    .machineDefinition(SingleBlockDefinition.Option.displayName("Induction Chamber"))
                    .machineDefinition(SingleBlockDefinition.Option.consumesEnergy())
                    .machineDefinition(SingleBlockDefinition.Option.tier(MachineTier.ULV))
                    .machineDefinition(SingleBlockDefinition.Option.recipeType(CERecipeTypes.HEATING))
                    .machineDefinition(SingleBlockDefinition.Option.slots(1, 1, 1, 1))
                    .machineDefinition(SingleBlockDefinition.Option.energyUsage(BASE_ENERGY_USAGE))
                    .machineDefinition(SingleBlockDefinition.Option.progressBar(ProgressBar.ARROW))
                    .machineDefinition(SingleBlockDefinition.Option.frontOverlay(
                            "block/machines/overlay/induction_chamber/overlay_front",
                            "block/machines/overlay/induction_chamber/overlay_front_active_1",
                            "block/machines/overlay/induction_chamber/overlay_front_active_2",
                            "block/machines/overlay/induction_chamber/overlay_front_active_3",
                            "block/machines/overlay/induction_chamber/overlay_front_active_4"
                    ))
                    .machineDefinition(SingleBlockDefinition.Option.tooltip("Processes heating recipes."))
                    .build();

    public static final SingleBlockDefinition MIXER =
            SingleBlockDefinition.machine()
                    .machineDefinition(SingleBlockDefinition.Option.id("mixer"))
                    .machineDefinition(SingleBlockDefinition.Option.displayName("Mixer"))
                    .machineDefinition(SingleBlockDefinition.Option.consumesEnergy())
                    .machineDefinition(SingleBlockDefinition.Option.tier(MachineTier.ULV))
                    .machineDefinition(SingleBlockDefinition.Option.recipeType(CERecipeTypes.MIXING))
                    .machineDefinition(SingleBlockDefinition.Option.slots(9, 4, 6, 4))
                    .machineDefinition(SingleBlockDefinition.Option.energyUsage(BASE_ENERGY_USAGE))
                    .machineDefinition(SingleBlockDefinition.Option.progressBar(ProgressBar.ARROW))
                    .machineDefinition(SingleBlockDefinition.Option.frontOverlay(
                            "block/machines/overlay/mixer/overlay_front",
                            "block/machines/overlay/mixer/overlay_front_active_1",
                            "block/machines/overlay/mixer/overlay_front_active_2",
                            "block/machines/overlay/mixer/overlay_front_active_3",
                            "block/machines/overlay/mixer/overlay_front_active_4",
                            "block/machines/overlay/mixer/overlay_front_active_5",
                            "block/machines/overlay/mixer/overlay_front_active_6"
                    ))
                    .machineDefinition(SingleBlockDefinition.Option.tooltip("Processes mixing recipes."))
                    .build();

    public static final SingleBlockDefinition PRESSURE_ARC_FURNACE =
            SingleBlockDefinition.machine()
                    .machineDefinition(SingleBlockDefinition.Option.id("pressure_arc_furnace"))
                    .machineDefinition(SingleBlockDefinition.Option.displayName("Pressure Arc Furnace"))
                    .machineDefinition(SingleBlockDefinition.Option.consumesEnergy())
                    .machineDefinition(SingleBlockDefinition.Option.tier(MachineTier.ULV))
                    .machineDefinition(SingleBlockDefinition.Option.recipeType(CERecipeTypes.PRESSURE_ARC_FURNACE))
                    .machineDefinition(SingleBlockDefinition.Option.slots(4, 4, 2, 2))
                    .machineDefinition(SingleBlockDefinition.Option.energyUsage(BASE_ENERGY_USAGE))
                    .machineDefinition(SingleBlockDefinition.Option.progressBar(ProgressBar.ARROW))
                    .machineDefinition(SingleBlockDefinition.Option.frontOverlay(
                            "block/machines/overlay/pressure_arc_furnace/overlay_front",
                            "block/machines/overlay/pressure_arc_furnace/overlay_front_active_1",
                            "block/machines/overlay/pressure_arc_furnace/overlay_front_active_2",
                            "block/machines/overlay/pressure_arc_furnace/overlay_front_active_3",
                            "block/machines/overlay/pressure_arc_furnace/overlay_front_active_4"
                    ))
                    .machineDefinition(SingleBlockDefinition.Option.tooltip("Processes pressure arc furnace recipes."))
                    .build();

    public static final SingleBlockDefinition SIFTER =
            SingleBlockDefinition.machine()
                    .machineDefinition(SingleBlockDefinition.Option.id("sifter"))
                    .machineDefinition(SingleBlockDefinition.Option.displayName("Sifter"))
                    .machineDefinition(SingleBlockDefinition.Option.consumesEnergy())
                    .machineDefinition(SingleBlockDefinition.Option.tier(MachineTier.ULV))
                    .machineDefinition(SingleBlockDefinition.Option.recipeType(CERecipeTypes.SIFTING))
                    .machineDefinition(SingleBlockDefinition.Option.slots(1, 6, 0, 0))
                    .machineDefinition(SingleBlockDefinition.Option.energyUsage(BASE_ENERGY_USAGE))
                    .machineDefinition(SingleBlockDefinition.Option.progressBar(ProgressBar.ARROW))
                    .machineDefinition(SingleBlockDefinition.Option.frontOverlay(
                            "block/machines/overlay/sifter/overlay_front",
                            "block/machines/overlay/sifter/overlay_front_active_1",
                            "block/machines/overlay/sifter/overlay_front_active_2",
                            "block/machines/overlay/sifter/overlay_front_active_3",
                            "block/machines/overlay/sifter/overlay_front_active_4",
                            "block/machines/overlay/sifter/overlay_front_active_5",
                            "block/machines/overlay/sifter/overlay_front_active_6",
                            "block/machines/overlay/sifter/overlay_front_active_7"
                    ))
                    .machineDefinition(SingleBlockDefinition.Option.tooltip("Processes sifting recipes."))
                    .build();

    public static final List<SingleBlockDefinition> ALL = List.of(
            ALLOY_SMELTER,
            AUTOCLAVE,
            CENTRIFUGE,
            CHEMICAL_REACTOR,
            COMPRESSOR,
            EXTRACTOR,
            FORGE_HAMMER,
            INDUCTION_CHAMBER,
            MIXER,
            PRESSURE_ARC_FURNACE,
            SIFTER
    );

    private ElectricSingleBlockMachines() {
    }
}
