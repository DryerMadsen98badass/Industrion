package net.mads.industron.registry;

import net.mads.industron.Industron;
import net.mads.industron.worldgen.GeologyDepositFeature;
import net.mads.industron.worldgen.GeologyClayReplacementFeature;
import net.mads.industron.worldgen.PebbleSurfaceFeature;
import net.mads.industron.worldgen.FallenStickSurfaceFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Runtime worldgen features owned by Industron. */
public final class WorldgenRegistry {
    private static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, Industron.MOD_ID);

    public static final DeferredHolder<Feature<?>, GeologyDepositFeature> GEOLOGY_DEPOSIT =
            FEATURES.register("geology_deposit", () -> new GeologyDepositFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, GeologyClayReplacementFeature> GEOLOGY_CLAY_REPLACEMENT =
            FEATURES.register("geology_clay_replacement", () -> new GeologyClayReplacementFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, PebbleSurfaceFeature> PEBBLE_SURFACE =
            FEATURES.register("pebble_surface", () -> new PebbleSurfaceFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, FallenStickSurfaceFeature> FALLEN_STICKS =
            FEATURES.register("fallen_sticks", () -> new FallenStickSurfaceFeature(NoneFeatureConfiguration.CODEC));

    private WorldgenRegistry() {
    }

    public static void register(IEventBus modEventBus) {
        FEATURES.register(modEventBus);
    }
}
