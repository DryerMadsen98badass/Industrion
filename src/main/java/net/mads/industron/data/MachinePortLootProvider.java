package net.mads.industron.data;

import net.mads.industron.registry.BlockRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public final class MachinePortLootProvider extends LootTableProvider {

    public MachinePortLootProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider
    ) {
        super(
                output,
                Set.of(),
                List.of(
                        new SubProviderEntry(
                                MachinePortBlockLoot::new,
                                LootContextParamSets.BLOCK
                        )
                ),
                lookupProvider
        );
    }

    private static final class MachinePortBlockLoot extends BlockLootSubProvider {

        private MachinePortBlockLoot(HolderLookup.Provider lookupProvider) {
            super(
                    Set.of(),
                    FeatureFlags.REGISTRY.allFlags(),
                    lookupProvider
            );
        }

        @Override
        protected void generate() {
            getKnownBlocks().forEach(this::dropSelf);
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return Stream.concat(
                            BlockRegistry.getAllMachinePorts().stream(),
                            BlockRegistry.getAllStaticMachinePorts().stream()
                    )
                    .map(holder -> (Block) holder.get())
                    .toList();
        }
    }
}
