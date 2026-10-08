package net.mads.industron.machine.machines.steam.singleblock;

import net.mads.industron.gui.ProgressBar;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.machine.SingleBlockDefinition;
import net.mads.industron.recipe.CERecipeTypes;
import net.mads.industron.recipe.RecipeTypeDefinition;
import net.mads.industron.machine.interaction.MachineCondition;
import net.mads.industron.machine.interaction.Weather;

import java.util.List;

/** Steam processors use the existing resource, inventory, GUI and CE execution runtime. */
public final class SteamSingleBlockMachines {
    /** Fuel Units supply heat; the temperature operation converts water to steam atomically. */
    public static final SingleBlockDefinition BOILER = SingleBlockDefinition.machine()
            .machineDefinition(SingleBlockDefinition.Option.id("boiler"))
            .machineDefinition(SingleBlockDefinition.Option.displayName("Solid Fuel Boiler"))
            .machineDefinition(SingleBlockDefinition.Option.tier(MachineTier.STEAM))
            .machineDefinition(SingleBlockDefinition.Option.producesSteam())
            .machineDefinition(SingleBlockDefinition.Option.steamConversionRecipe("industron:evaporation/water_to_steam"))
            .machineDefinition(SingleBlockDefinition.Option.recipeType(CERecipeTypes.FUEL))
            .machineDefinition(SingleBlockDefinition.Option.slots(1, 1, 1, 0))
            .machineDefinition(SingleBlockDefinition.Option.steamCapacity(16000))
            .machineDefinition(SingleBlockDefinition.Option.steamOutput(8))
            .machineDefinition(SingleBlockDefinition.Option.temperature(
                    100, 200, 20, 5, 2))
            .machineDefinition(SingleBlockDefinition.Option.progressBar(ProgressBar.ARROW))
            .machineDefinition(SingleBlockDefinition.Option.frontOverlay(
                    "block/machines/overlay/boiler/overlay_front",
                    "block/machines/overlay/boiler/overlay_front_active_1",
                    "block/machines/overlay/boiler/overlay_front_active_2",
                    "block/machines/overlay/boiler/overlay_front_active_3",
                    "block/machines/overlay/boiler/overlay_front_active_4"))
            .machineDefinition(SingleBlockDefinition.Option.tooltip("Solid fuel and water produce steam at 100 C."))
            .machineDefinition(SingleBlockDefinition.Option.tooltip("A sealed boiler produces steam; pipe it out to steam consumers."))
            .build();

    /** Slow solar heating needs clear daylight and an unobstructed collector. */
    public static final SingleBlockDefinition SOLAR_BOILER = SingleBlockDefinition.machine()
            .machineDefinition(SingleBlockDefinition.Option.id("solar_boiler"))
            .machineDefinition(SingleBlockDefinition.Option.displayName("Solar Boiler"))
            .machineDefinition(SingleBlockDefinition.Option.tier(MachineTier.STEAM))
            .machineDefinition(SingleBlockDefinition.Option.producesSteam())
            .machineDefinition(SingleBlockDefinition.Option.steamConversionRecipe("industron:evaporation/water_to_steam"))
            .machineDefinition(SingleBlockDefinition.Option.slots(0, 0, 1, 0))
            .machineDefinition(SingleBlockDefinition.Option.steamCapacity(8000))
            .machineDefinition(SingleBlockDefinition.Option.steamOutput(4))
            .machineDefinition(SingleBlockDefinition.Option.temperature(
                    100, 105, 20, 1, 1,
                    SingleBlockDefinition.Option.condition(MachineCondition.daylight().top(1)),
                    SingleBlockDefinition.Option.condition(MachineCondition.weather(Weather.CLEAR))))
            .machineDefinition(SingleBlockDefinition.Option.topTexture(
                    "block/machines/boilers/solar_collector"))
            .machineDefinition(SingleBlockDefinition.Option.frontOverlay(
                    "block/machines/overlay/solar_boiler/overlay_front",
                    "block/machines/overlay/solar_boiler/overlay_front_active"))
            .machineDefinition(SingleBlockDefinition.Option.tooltip("Clear daylight heats the solar collector; no fuel is used."))
            .machineDefinition(SingleBlockDefinition.Option.tooltip("Keep the collector exposed to the sky."))
            .build();

    public static final SingleBlockDefinition LIQUID_FUEL_BOILER = SingleBlockDefinition.machine()
            .machineDefinition(SingleBlockDefinition.Option.id("liquid_fuel_boiler"))
            .machineDefinition(SingleBlockDefinition.Option.displayName("Liquid Fuel Boiler"))
            .machineDefinition(SingleBlockDefinition.Option.tier(MachineTier.STEAM))
            .machineDefinition(SingleBlockDefinition.Option.producesSteam())
            .machineDefinition(SingleBlockDefinition.Option.steamConversionRecipe("industron:evaporation/water_to_steam"))
            .machineDefinition(SingleBlockDefinition.Option.recipeType(CERecipeTypes.FUEL))
            .machineDefinition(SingleBlockDefinition.Option.slots(0, 0, 2, 0))
            .machineDefinition(SingleBlockDefinition.Option.steamCapacity(32000))
            .machineDefinition(SingleBlockDefinition.Option.steamOutput(16))
            .machineDefinition(SingleBlockDefinition.Option.temperature(
                    100, 250, 20, 10, 3))
            .machineDefinition(SingleBlockDefinition.Option.progressBar(ProgressBar.ARROW))
            .machineDefinition(SingleBlockDefinition.Option.frontOverlay(
                    "block/machines/overlay/liquid_fuel_boiler/overlay_front",
                    "block/machines/overlay/liquid_fuel_boiler/overlay_front_active_1",
                    "block/machines/overlay/liquid_fuel_boiler/overlay_front_active_2",
                    "block/machines/overlay/liquid_fuel_boiler/overlay_front_active_3",
                    "block/machines/overlay/liquid_fuel_boiler/overlay_front_active_4"))
            .machineDefinition(SingleBlockDefinition.Option.tooltip("Liquid fuel and water produce steam at 100 C."))
            .machineDefinition(SingleBlockDefinition.Option.tooltip("Separate input tanks hold water and liquid fuel."))
            .build();

    public static final SingleBlockDefinition HAMMER = processor(
            "hammer", "Hammer", CERecipeTypes.HAMMERING,
            2, 6, 0, 0,
            "block/machines/overlay/forge_hammer/overlay_front_active_1",
            "block/machines/overlay/forge_hammer/overlay_front_active_2",
            "block/machines/overlay/forge_hammer/overlay_front_active_3",
            "block/machines/overlay/forge_hammer/overlay_front_active_4",
            "block/machines/overlay/forge_hammer/overlay_front_active_5");

    public static final SingleBlockDefinition COMPRESSOR = processor(
            "compressor", "Compressor", CERecipeTypes.COMPRESSING,
            2, 2, 0, 0,
            "block/machines/overlay/compressor/overlay_front",
            "block/machines/overlay/compressor/overlay_front_active_1",
            "block/machines/overlay/compressor/overlay_front_active_2",
            "block/machines/overlay/compressor/overlay_front_active_3",
            "block/machines/overlay/compressor/overlay_front_active_4",
            "block/machines/overlay/compressor/overlay_front_active_5",
            "block/machines/overlay/compressor/overlay_front_active_6",
            "block/machines/overlay/compressor/overlay_front_active_7",
            "block/machines/overlay/compressor/overlay_front_active_8",
            "block/machines/overlay/compressor/overlay_front_active_9");

    public static final SingleBlockDefinition EXTRUDER = processor(
            "extruder", "Extruder", CERecipeTypes.EXTRUDING,
            3, 3, 0, 0,
            "block/machines/overlay/extruder/overlay_front",
            "block/machines/overlay/extruder/overlay_front_active_1",
            "block/machines/overlay/extruder/overlay_front_active_2",
            "block/machines/overlay/extruder/overlay_front_active_3",
            "block/machines/overlay/extruder/overlay_front_active_4",
            "block/machines/overlay/extruder/overlay_front_active_5",
            "block/machines/overlay/extruder/overlay_front_active_6");

    public static final SingleBlockDefinition AUTOCLAVE = processor(
            "autoclave", "Autoclave", CERecipeTypes.CRYSTALLIZATION,
            2, 4, 3, 2,
            "block/machines/overlay/autoclave/overlay_front",
            "block/machines/overlay/autoclave/overlay_front_active");

    public static final SingleBlockDefinition EXTRACTOR = processor(
            "extractor", "Extractor", CERecipeTypes.SOLVENT_EXTRACTION,
            3, 6, 3, 3,
            "block/machines/overlay/extractor/overlay_front",
            "block/machines/overlay/extractor/overlay_front_active_1",
            "block/machines/overlay/extractor/overlay_front_active_2",
            "block/machines/overlay/extractor/overlay_front_active_3",
            "block/machines/overlay/extractor/overlay_front_active_4",
            "block/machines/overlay/extractor/overlay_front_active_5");

    public static final List<SingleBlockDefinition> ALL = List.of(
            BOILER, SOLAR_BOILER, LIQUID_FUEL_BOILER,
            HAMMER, COMPRESSOR, EXTRUDER, AUTOCLAVE, EXTRACTOR);

    private static SingleBlockDefinition processor(String id, String name, RecipeTypeDefinition type,
            int itemIn, int itemOut, int fluidIn, int fluidOut, String... overlays) {
        String[] active = java.util.Arrays.copyOfRange(overlays, 1, overlays.length);
        return SingleBlockDefinition.machine()
                .machineDefinition(SingleBlockDefinition.Option.id(id))
                .machineDefinition(SingleBlockDefinition.Option.displayName(name))
                .machineDefinition(SingleBlockDefinition.Option.tier(MachineTier.STEAM))
                .machineDefinition(SingleBlockDefinition.Option.consumesSteam())
                .machineDefinition(SingleBlockDefinition.Option.recipeType(type))
                .machineDefinition(SingleBlockDefinition.Option.slots(itemIn, itemOut, fluidIn, fluidOut))
                .machineDefinition(SingleBlockDefinition.Option.steamCapacity(16000))
                .machineDefinition(SingleBlockDefinition.Option.steamUsage(8))
                .machineDefinition(SingleBlockDefinition.Option.noDurationReset())
                .machineDefinition(SingleBlockDefinition.Option.progressBar(ProgressBar.ARROW))
                .machineDefinition(SingleBlockDefinition.Option.frontOverlay(overlays[0], active))
                .machineDefinition(SingleBlockDefinition.Option.tooltip("Processing duration: 2x the recipe duration at this pressure level."))
                .build();
    }

    private SteamSingleBlockMachines() {}
}
