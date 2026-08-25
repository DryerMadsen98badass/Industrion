package net.mads.industron.data;

import net.mads.industron.machine.machines.electric.multiblock.MultiblockControllerDefinition;
import net.mads.industron.registry.BlockRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public final class MultiblockControlerLootTableProvider
        extends LootTableProvider {

    public MultiblockControlerLootTableProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider
    ) {
        super(
                output,
                Set.of(),
                List.of(
                        new SubProviderEntry(
                                MultiblockControllerBlockLoot::new,
                                LootContextParamSets.BLOCK
                        ),
                        new SubProviderEntry(
                                MachinePortBlockLoot::new,
                                LootContextParamSets.BLOCK
                        )
                ),
                lookupProvider
        );
    }

    private static final class MultiblockControllerBlockLoot
            extends BlockLootSubProvider {

        private MultiblockControllerBlockLoot(
                HolderLookup.Provider lookupProvider
        ) {
            super(
                    Set.of(),
                    FeatureFlags.REGISTRY.allFlags(),
                    lookupProvider
            );
        }

        @Override
        protected void generate() {
            for (MultiblockControllerDefinition controller :
                    MultiblockControllerDefinition.all()) {

                Block block = BuiltInRegistries.BLOCK.get(
                        controller.id()
                );

                if (block == Blocks.AIR) {
                    throw new IllegalStateException(
                            "No registered controller block was found for "
                                    + controller.id()
                    );
                }

                dropSelf(block);
            }
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return MultiblockControllerDefinition.all()
                    .stream()
                    .map(controller ->
                            BuiltInRegistries.BLOCK.get(
                                    controller.id()
                            )
                    )
                    .filter(block -> block != Blocks.AIR)
                    .toList();
        }
    }
    private static final class MachinePortBlockLoot
            extends BlockLootSubProvider {

        private MachinePortBlockLoot(
                HolderLookup.Provider lookupProvider
        ) {
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