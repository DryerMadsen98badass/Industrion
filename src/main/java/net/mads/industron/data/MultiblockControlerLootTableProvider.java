package net.mads.industron.data;

import net.mads.industron.machine.MachinePortBlock;
import net.mads.industron.machine.interaction.BlockLoot;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockControllerBlock;
import net.mads.industron.registry.BlockRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Multiblock controllers and machine ports never drop their assembled block item.
 * Their exact salvage is deferred until their real component recipes are available.
 */
public final class MultiblockControlerLootTableProvider implements DataProvider {
    private final PackOutput.PathProvider lootTables;

    public MultiblockControlerLootTableProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider
    ) {
        this.lootTables = output.createPathProvider(PackOutput.Target.DATA_PACK, "loot_table/blocks");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> futures = new ArrayList<>();

        for (var holder : BlockRegistry.getAllMultiblockControllers()) {
            MultiblockControllerBlock block = holder.get();
            saveController(futures, output, block);
        }
        for (var holder : BlockRegistry.getAllMachinePorts()) {
            MachinePortBlock block = holder.get();
            save(futures, output, block);
        }
        for (var holder : BlockRegistry.getAllStaticMachinePorts()) {
            MachinePortBlock block = holder.get();
            save(futures, output, block);
        }

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }


    private void saveController(
            List<CompletableFuture<?>> futures,
            CachedOutput output,
            MultiblockControllerBlock block
    ) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
        futures.add(DataProvider.saveStable(
                output,
                block.definition().hasLootOverride()
                        ? SalvageLootTable.from(block.definition().loot(), blockId)
                        : SalvageLootTable.empty(),
                lootTables.json(blockId)
        ));
    }

    private void save(
            List<CompletableFuture<?>> futures,
            CachedOutput output,
            Block block
    ) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
        futures.add(DataProvider.saveStable(output, SalvageLootTable.empty(), lootTables.json(blockId)));
    }

    @Override
    public String getName() {
        return "Industron Multiblock Controller And Port Salvage Loot Tables";
    }
}
