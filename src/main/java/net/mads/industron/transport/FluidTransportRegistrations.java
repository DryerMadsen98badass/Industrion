package net.mads.industron.transport;

import net.mads.industron.transport.color.ColoredFluidPipeRegistrations;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FluidTransportRegistrations {
    private static final Map<String, RegisteredBlocks> BLOCKS = new LinkedHashMap<>();
    private static final Map<String, RegisteredItems> ITEMS = new LinkedHashMap<>();
    private static final Map<String, RegisteredBlockEntities> BLOCK_ENTITIES = new LinkedHashMap<>();
    private static List<RegisteredBlocks> blockSnapshot = List.of();
    private static List<RegisteredItems> itemSnapshot = List.of();
    private static List<RegisteredBlockEntities> entitySnapshot = List.of();

    private FluidTransportRegistrations() {
    }

    public static void registerBlocks(DeferredRegister<Block> registry) {
        if (!BLOCKS.isEmpty()) {
            throw new IllegalStateException("Fluid transport blocks were registered more than once");
        }

        for (FluidTransportTier tier : FluidTransportTier.all()) {
            DeferredHolder<Block, FluidTransportPipeBlock> pipe = registry.register(
                    tier.pipeId(),
                    () -> new FluidTransportPipeBlock(tier, pipeProperties())
            );
            DeferredHolder<Block, FluidTransportGlassPipeBlock> glassPipe = registry.register(
                    tier.glassPipeId(),
                    () -> new FluidTransportGlassPipeBlock(tier, glassPipeProperties())
            );
            DeferredHolder<Block, FluidTransportPumpBlock> pump = registry.register(
                    tier.pumpId(),
                    () -> new FluidTransportPumpBlock(tier, pumpProperties())
            );
            DeferredHolder<Block, FluidTransportTankBlock> tank = registry.register(
                    tier.tankId(),
                    () -> new FluidTransportTankBlock(tier, tankProperties())
            );
            BLOCKS.put(tier.id(), new RegisteredBlocks(tier, pipe, glassPipe, pump, tank));
        }

        blockSnapshot = List.copyOf(BLOCKS.values());
        ColoredFluidPipeRegistrations.registerBlocks(registry);
    }

    public static void registerItems(DeferredRegister<Item> registry) {
        requireBlocks();
        if (!ITEMS.isEmpty()) {
            throw new IllegalStateException("Fluid transport items were registered more than once");
        }

        for (RegisteredBlocks blocks : BLOCKS.values()) {
            FluidTransportTier tier = blocks.tier();
            DeferredHolder<Item, BlockItem> pipe = registry.register(
                    tier.pipeId(),
                    () -> new BlockItem(blocks.pipe().get(), new Item.Properties())
            );
            DeferredHolder<Item, BlockItem> pump = registry.register(
                    tier.pumpId(),
                    () -> new BlockItem(blocks.pump().get(), new Item.Properties())
            );
            DeferredHolder<Item, FluidTransportTankItem> tank = registry.register(
                    tier.tankId(),
                    () -> new FluidTransportTankItem(tier, blocks.tank().get(), new Item.Properties())
            );
            ITEMS.put(tier.id(), new RegisteredItems(tier, pipe, pump, tank));
        }

        itemSnapshot = List.copyOf(ITEMS.values());
        ColoredFluidPipeRegistrations.registerItems(registry);
    }

    public static void registerBlockEntities(DeferredRegister<BlockEntityType<?>> registry) {
        requireBlocks();
        if (!BLOCK_ENTITIES.isEmpty()) {
            throw new IllegalStateException("Fluid transport block entities were registered more than once");
        }

        for (RegisteredBlocks blocks : BLOCKS.values()) {
            FluidTransportTier tier = blocks.tier();
            DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidTransportPipeBlockEntity>> pipe = registry.register(
                    tier.pipeId(),
                    () -> {
                        java.util.List<Block> validBlocks = new java.util.ArrayList<>();
                        validBlocks.add(blocks.pipe().get());
                        validBlocks.addAll(ColoredFluidPipeRegistrations.pipeBlocksForTier(tier));
                        validBlocks.addAll(ColoredFluidPipeRegistrations.encasedPipeBlocksForTier(tier));
                        return BlockEntityType.Builder.of(
                                FluidTransportPipeBlockEntity::new,
                                validBlocks.toArray(Block[]::new)
                        ).build(null);
                    }
            );
            DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidTransportGlassPipeBlockEntity>> glassPipe = registry.register(
                    tier.glassPipeId(),
                    () -> {
                        java.util.List<Block> validBlocks = new java.util.ArrayList<>();
                        validBlocks.add(blocks.glassPipe().get());
                        validBlocks.addAll(ColoredFluidPipeRegistrations.glassPipeBlocksForTier(tier));
                        return BlockEntityType.Builder.of(
                                FluidTransportGlassPipeBlockEntity::new,
                                validBlocks.toArray(Block[]::new)
                        ).build(null);
                    }
            );
            DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidTransportPumpBlockEntity>> pump = registry.register(
                    tier.pumpId(),
                    () -> BlockEntityType.Builder.of(
                            FluidTransportPumpBlockEntity::new,
                            blocks.pump().get()
                    ).build(null)
            );
            DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidTransportTankBlockEntity>> tank = registry.register(
                    tier.tankId(),
                    () -> BlockEntityType.Builder.of(
                            FluidTransportTankBlockEntity::new,
                            blocks.tank().get()
                    ).build(null)
            );
            BLOCK_ENTITIES.put(tier.id(), new RegisteredBlockEntities(tier, pipe, glassPipe, pump, tank));
        }

        entitySnapshot = List.copyOf(BLOCK_ENTITIES.values());
        ColoredFluidPipeRegistrations.registerCreateBlockEntities(registry);
    }

    public static RegisteredBlocks blocks(FluidTransportTier tier) {
        requireBlocks();
        return require(BLOCKS, tier);
    }

    public static RegisteredItems items(FluidTransportTier tier) {
        if (ITEMS.isEmpty()) {
            throw new IllegalStateException("Fluid transport items have not been registered yet");
        }
        return require(ITEMS, tier);
    }

    public static RegisteredBlockEntities blockEntities(FluidTransportTier tier) {
        if (BLOCK_ENTITIES.isEmpty()) {
            throw new IllegalStateException("Fluid transport block entities have not been registered yet");
        }
        return require(BLOCK_ENTITIES, tier);
    }

    public static Collection<RegisteredBlocks> allBlocks() {
        requireBlocks();
        return blockSnapshot;
    }

    public static Collection<RegisteredItems> allItems() {
        if (ITEMS.isEmpty()) {
            throw new IllegalStateException("Fluid transport items have not been registered yet");
        }
        return itemSnapshot;
    }

    public static Collection<RegisteredBlockEntities> allBlockEntities() {
        if (BLOCK_ENTITIES.isEmpty()) {
            throw new IllegalStateException("Fluid transport block entities have not been registered yet");
        }
        return entitySnapshot;
    }

    private static BlockBehaviour.Properties pipeProperties() {
        return BlockBehaviour.Properties.of()
                .strength(1.5F, 6.0F)
                .requiresCorrectToolForDrops()
                .forceSolidOff()
                .sound(SoundType.COPPER);
    }

    private static BlockBehaviour.Properties glassPipeProperties() {
        return BlockBehaviour.Properties.of()
                .strength(1.5F, 6.0F)
                .requiresCorrectToolForDrops()
                .noOcclusion()
                .forceSolidOff()
                .sound(SoundType.COPPER);
    }

    private static BlockBehaviour.Properties pumpProperties() {
        return BlockBehaviour.Properties.of()
                .strength(1.5F, 6.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.COPPER);
    }

    private static BlockBehaviour.Properties tankProperties() {
        return BlockBehaviour.Properties.of()
                .strength(1.5F, 6.0F)
                .requiresCorrectToolForDrops()
                .noOcclusion()
                .isRedstoneConductor((state, level, pos) -> true)
                .sound(SoundType.COPPER);
    }

    private static void requireBlocks() {
        if (BLOCKS.isEmpty()) {
            throw new IllegalStateException("Fluid transport blocks have not been registered yet");
        }
    }

    private static <T> T require(Map<String, T> values, FluidTransportTier tier) {
        T value = values.get(tier.id());
        if (value == null) {
            throw new IllegalArgumentException("No fluid transport registration exists for tier " + tier.id());
        }
        return value;
    }

    public record RegisteredBlocks(
            FluidTransportTier tier,
            DeferredHolder<Block, FluidTransportPipeBlock> pipe,
            DeferredHolder<Block, FluidTransportGlassPipeBlock> glassPipe,
            DeferredHolder<Block, FluidTransportPumpBlock> pump,
            DeferredHolder<Block, FluidTransportTankBlock> tank
    ) {
    }

    public record RegisteredItems(
            FluidTransportTier tier,
            DeferredHolder<Item, BlockItem> pipe,
            DeferredHolder<Item, BlockItem> pump,
            DeferredHolder<Item, FluidTransportTankItem> tank
    ) {
        public List<DeferredHolder<Item, ? extends BlockItem>> visibleItems() {
            return List.of(pipe, pump, tank);
        }
    }

    public record RegisteredBlockEntities(
            FluidTransportTier tier,
            DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidTransportPipeBlockEntity>> pipe,
            DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidTransportGlassPipeBlockEntity>> glassPipe,
            DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidTransportPumpBlockEntity>> pump,
            DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidTransportTankBlockEntity>> tank
    ) {
    }
}
