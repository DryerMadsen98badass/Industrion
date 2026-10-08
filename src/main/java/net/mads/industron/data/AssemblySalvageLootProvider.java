package net.mads.industron.data;

import net.mads.industron.block.loot.AssemblySalvageBlock;
import net.mads.industron.machine.SingleBlockMachineBlock;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockControllerBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Generates loot for registered technical blocks. Machines default to dropping themselves;
 * explicit controller loot overrides take precedence. Other technical blocks retain Assembly salvage.
 */
public final class AssemblySalvageLootProvider implements DataProvider {
    private final PackOutput.PathProvider lootTables;

    public AssemblySalvageLootProvider(PackOutput output) {
        this.lootTables = output.createPathProvider(PackOutput.Target.DATA_PACK, "loot_table/blocks");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> futures = new ArrayList<>();

        for (Block block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof AssemblySalvageBlock)) {
                continue;
            }

            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
            if (block instanceof MultiblockControllerBlock controller && controller.definition().hasLootOverride()) {
                futures.add(DataProvider.saveStable(
                        output,
                        SalvageLootTable.from(controller.definition().loot(), blockId),
                        lootTables.json(blockId)
                ));
                continue;
            }

            if (block instanceof SingleBlockMachineBlock || block instanceof MultiblockControllerBlock) {
                futures.add(DataProvider.saveStable(
                        output,
                        SalvageLootTable.singleItem(blockId),
                        lootTables.json(blockId)
                ));
                continue;
            }

            Map<ResourceLocation, Integer> salvage = AssemblySalvageResolver.salvageForBlock(blockId);
            futures.add(DataProvider.saveStable(
                    output,
                    SalvageLootTable.items(salvage),
                    lootTables.json(blockId)
            ));
        }

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Industron Assembly Salvage Loot Tables";
    }
}
