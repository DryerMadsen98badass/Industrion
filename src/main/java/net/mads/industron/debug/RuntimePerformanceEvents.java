package net.mads.industron.debug;

import net.mads.industron.Industron;
import net.mads.industron.runtime.*;
import net.mads.industron.energy.CEEnergyNetwork;
import net.mads.industron.recipe.CERecipeLookup;
import net.mads.industron.machine.runtime.AsyncRecipeSearch;
import net.mads.industron.machine.foundry.FoundryMelting;
import net.mads.industron.machine.foundry.FoundryBrickResolver;
import net.mads.industron.material.MaterialLookup;
import net.mads.industron.fluid.IndustrialFluidLookup;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

@EventBusSubscriber(modid = Industron.MOD_ID)
public final class RuntimePerformanceEvents {
    private RuntimePerformanceEvents() {}
    @SubscribeEvent public static void started(ServerStartedEvent event) { clear(); IndustronWorkers.start(); }
    @SubscribeEvent public static void stopping(ServerStoppingEvent event) { IndustronWorkers.stop(); clear(); CEPerformanceProfiler.disable(); }
    private static void clear() {
        CEEnergyNetwork.clear(); CERecipeLookup.clear(); StructureWatch.clear();
        AsyncRecipeSearch.clearCaches(); FoundryMelting.clearCache(); FoundryBrickResolver.clearCache();
        MaterialLookup.clearCaches(); IndustrialFluidLookup.clearCache();
    }
    @SubscribeEvent public static void recipes(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) {
            CERecipeLookup.clear(); AsyncRecipeSearch.clearCaches(); FoundryMelting.clearCache();
            MaterialLookup.clearCaches(); IndustrialFluidLookup.clearCache();
        }
    }
    @SubscribeEvent public static void changed(BlockEvent.NeighborNotifyEvent event) {
        if (event.getLevel() instanceof ServerLevel level)
            StructureWatch.changed(level, event.getPos(), event.getState());
    }
    @SubscribeEvent public static void chunkLoad(ChunkEvent.Load event) {
        // Load may occur before FULL; only invalidate cache, never access chunk/world contents here.
        if (event.getLevel() instanceof ServerLevel level) {
            long chunk = event.getChunk().getPos().toLong();
            if (level.getServer().isSameThread()) CEEnergyNetwork.invalidateChunk(level, chunk);
            else level.getServer().execute(() -> CEEnergyNetwork.invalidateChunk(level, chunk));
        }
    }
    @SubscribeEvent public static void levelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            CEEnergyNetwork.unload(level); StructureWatch.unload(level);
        }
    }
    @SubscribeEvent public static void chunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            long chunk = event.getChunk().getPos().toLong();
            if (level.getServer().isSameThread()) CEEnergyNetwork.invalidateChunk(level, chunk);
            else level.getServer().execute(() -> CEEnergyNetwork.invalidateChunk(level, chunk));
        }
    }
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("industron").requires(source -> source.hasPermission(2))
                .then(Commands.literal("profiler")
                        .then(Commands.literal("enable").executes(ctx -> {
                            CEPerformanceProfiler.enable(); ctx.getSource().sendSuccess(() -> Component.literal("Industron profiler enabled; measurements reset."), false); return 1;
                        }))
                        .then(Commands.literal("disable").executes(ctx -> {
                            CEPerformanceProfiler.disable(); ctx.getSource().sendSuccess(() -> Component.literal("Industron profiler disabled."), false); return 1;
                        }))
                        .then(Commands.literal("reset").executes(ctx -> {
                            CEPerformanceProfiler.reset(); ctx.getSource().sendSuccess(() -> Component.literal("Industron profiler measurements reset."), false); return 1;
                        }))
                        .then(Commands.literal("stats").executes(ctx -> {
                            for (Component line : CEPerformanceProfiler.stats()) ctx.getSource().sendSuccess(() -> line, false);
                            ctx.getSource().sendSuccess(() -> Component.literal(IndustronWorkers.stats()), false); return 1;
                        }))));
    }
}
