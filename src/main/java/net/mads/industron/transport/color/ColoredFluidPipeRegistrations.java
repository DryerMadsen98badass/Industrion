package net.mads.industron.transport.color;

import com.simibubi.create.content.decoration.encasing.EncasingRegistry;
import com.simibubi.create.content.fluids.pipes.EncasedPipeBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import net.mads.industron.transport.FluidTransportTier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ColoredFluidPipeRegistrations {
    private static final Map<String, EnumMap<DyeColor, RegisteredBlocks>> BLOCKS = new LinkedHashMap<>();
    private static final Map<String, EnumMap<DyeColor, RegisteredItems>> ITEMS = new LinkedHashMap<>();
    private static List<RegisteredBlocks> blockSnapshot = List.of();
    private static List<RegisteredItems> itemSnapshot = List.of();

    private static DeferredHolder<BlockEntityType<?>, BlockEntityType<ColoredCreateFluidPipeBlockEntity>> createPipeBlockEntity;
    private static DeferredHolder<BlockEntityType<?>, BlockEntityType<ColoredCreateGlassFluidPipeBlockEntity>> createGlassPipeBlockEntity;
    private static boolean encasingVariantsRegistered;

    private ColoredFluidPipeRegistrations() {
    }

    public static void registerBlocks(DeferredRegister<Block> registry) {
        if (!BLOCKS.isEmpty()) {
            throw new IllegalStateException("Colored fluid pipe blocks were registered more than once");
        }

        for (PipeColorDefinitions.PipeFamily family : PipeColorDefinitions.allFamilies()) {
            EnumMap<DyeColor, RegisteredBlocks> colors = new EnumMap<>(DyeColor.class);
            for (DyeColor color : DyeColor.values()) {
                DeferredHolder<Block, ? extends Block> pipe;
                DeferredHolder<Block, ? extends Block> glassPipe;
                DeferredHolder<Block, ? extends Block> encasedPipe;

                if (family.isCreatePipe()) {
                    pipe = registry.register(
                            family.coloredPipeId(color),
                            () -> new ColoredCreateFluidPipeBlock(family, color, pipeProperties())
                    );
                    glassPipe = registry.register(
                            family.coloredGlassPipeId(color),
                            () -> new ColoredCreateGlassFluidPipeBlock(family, color, glassPipeProperties())
                    );
                    encasedPipe = registry.register(
                            family.coloredEncasedPipeId(color),
                            () -> new ColoredCreateEncasedFluidPipeBlock(family, color, encasedPipeProperties())
                    );
                } else {
                    FluidTransportTier tier = family.tier();
                    pipe = registry.register(
                            family.coloredPipeId(color),
                            () -> new ColoredFluidTransportPipeBlock(family, tier, color, pipeProperties())
                    );
                    glassPipe = registry.register(
                            family.coloredGlassPipeId(color),
                            () -> new ColoredFluidTransportGlassPipeBlock(family, tier, color, glassPipeProperties())
                    );
                    encasedPipe = registry.register(
                            family.coloredEncasedPipeId(color),
                            () -> new ColoredFluidTransportEncasedPipeBlock(
                                    family,
                                    tier,
                                    color,
                                    encasedPipeProperties()
                            )
                    );
                }

                colors.put(color, new RegisteredBlocks(family, color, pipe, glassPipe, encasedPipe));
            }
            BLOCKS.put(family.id(), colors);
        }
        blockSnapshot = BLOCKS.values().stream().flatMap(colors -> colors.values().stream()).toList();
    }

    public static void registerItems(DeferredRegister<Item> registry) {
        requireBlocks();
        if (!ITEMS.isEmpty()) {
            throw new IllegalStateException("Colored fluid pipe items were registered more than once");
        }

        for (PipeColorDefinitions.PipeFamily family : PipeColorDefinitions.allFamilies()) {
            EnumMap<DyeColor, RegisteredItems> colors = new EnumMap<>(DyeColor.class);
            for (DyeColor color : DyeColor.values()) {
                RegisteredBlocks blocks = blocks(family, color);
                DeferredHolder<Item, BlockItem> pipe = registry.register(
                        family.coloredPipeId(color),
                        () -> new BlockItem(blocks.pipe().get(), new Item.Properties())
                );
                colors.put(color, new RegisteredItems(family, color, pipe));
            }
            ITEMS.put(family.id(), colors);
        }
        itemSnapshot = ITEMS.values().stream().flatMap(colors -> colors.values().stream()).toList();
    }

    public static void registerCreateBlockEntities(DeferredRegister<BlockEntityType<?>> registry) {
        requireBlocks();
        createPipeBlockEntity = registry.register(
                "colored_fluid_pipe",
                () -> BlockEntityType.Builder.of(
                        ColoredCreateFluidPipeBlockEntity::new,
                        createPipeBlockEntityBlocks().toArray(Block[]::new)
                ).build(null)
        );
        createGlassPipeBlockEntity = registry.register(
                "colored_glass_fluid_pipe",
                () -> BlockEntityType.Builder.of(
                        ColoredCreateGlassFluidPipeBlockEntity::new,
                        createGlassPipeBlocks().toArray(Block[]::new)
                ).build(null)
        );
    }

    public static void registerEncasingVariants() {
        requireBlocks();
        if (encasingVariantsRegistered) {
            return;
        }

        for (RegisteredBlocks registration : allBlocks()) {
            Block pipeBlock = registration.pipe().get();
            Block encasedBlock = registration.encasedPipe().get();
            if (!(pipeBlock instanceof FluidPipeBlock pipe)) {
                throw new IllegalStateException("Colored pipe is not encasable: " + pipeBlock);
            }
            if (!(encasedBlock instanceof EncasedPipeBlock encasedPipe)) {
                throw new IllegalStateException("Colored encased pipe is invalid: " + encasedBlock);
            }
            EncasingRegistry.addVariant(pipe, encasedPipe);
        }
        encasingVariantsRegistered = true;
    }

    public static RegisteredBlocks blocks(PipeColorDefinitions.PipeFamily family, DyeColor color) {
        requireBlocks();
        EnumMap<DyeColor, RegisteredBlocks> colors = BLOCKS.get(family.id());
        if (colors == null || colors.get(color) == null) {
            throw new IllegalArgumentException("No colored fluid pipe registration exists for " + family.id() + " / " + color);
        }
        return colors.get(color);
    }

    public static RegisteredItems items(PipeColorDefinitions.PipeFamily family, DyeColor color) {
        if (ITEMS.isEmpty()) {
            throw new IllegalStateException("Colored fluid pipe items have not been registered yet");
        }
        EnumMap<DyeColor, RegisteredItems> colors = ITEMS.get(family.id());
        if (colors == null || colors.get(color) == null) {
            throw new IllegalArgumentException("No colored fluid pipe item registration exists for " + family.id() + " / " + color);
        }
        return colors.get(color);
    }

    public static Collection<RegisteredBlocks> allBlocks() {
        requireBlocks();
        return blockSnapshot;
    }

    public static Collection<RegisteredItems> allItems() {
        if (ITEMS.isEmpty()) {
            throw new IllegalStateException("Colored fluid pipe items have not been registered yet");
        }
        return itemSnapshot;
    }

    public static List<Block> pipeBlocksForTier(FluidTransportTier tier) {
        PipeColorDefinitions.PipeFamily family = PipeColorDefinitions.forTier(tier);
        return blocksForFamily(family, BlockVariant.PIPE);
    }

    public static List<Block> glassPipeBlocksForTier(FluidTransportTier tier) {
        PipeColorDefinitions.PipeFamily family = PipeColorDefinitions.forTier(tier);
        return blocksForFamily(family, BlockVariant.GLASS_PIPE);
    }

    public static List<Block> encasedPipeBlocksForTier(FluidTransportTier tier) {
        PipeColorDefinitions.PipeFamily family = PipeColorDefinitions.forTier(tier);
        return blocksForFamily(family, BlockVariant.ENCASED_PIPE);
    }

    public static DeferredHolder<BlockEntityType<?>, BlockEntityType<ColoredCreateFluidPipeBlockEntity>> createPipeBlockEntity() {
        if (createPipeBlockEntity == null) {
            throw new IllegalStateException("Colored Create fluid pipe block entity type has not been registered yet");
        }
        return createPipeBlockEntity;
    }

    public static DeferredHolder<BlockEntityType<?>, BlockEntityType<ColoredCreateGlassFluidPipeBlockEntity>> createGlassPipeBlockEntity() {
        if (createGlassPipeBlockEntity == null) {
            throw new IllegalStateException("Colored Create glass fluid pipe block entity type has not been registered yet");
        }
        return createGlassPipeBlockEntity;
    }

    private static List<Block> createPipeBlockEntityBlocks() {
        List<Block> blocks = new ArrayList<>(blocksForFamily(PipeColorDefinitions.CREATE_PIPE, BlockVariant.PIPE));
        blocks.addAll(blocksForFamily(PipeColorDefinitions.CREATE_PIPE, BlockVariant.ENCASED_PIPE));
        return List.copyOf(blocks);
    }

    private static List<Block> createGlassPipeBlocks() {
        return blocksForFamily(PipeColorDefinitions.CREATE_PIPE, BlockVariant.GLASS_PIPE);
    }

    private static List<Block> blocksForFamily(
            PipeColorDefinitions.PipeFamily family,
            BlockVariant variant
    ) {
        requireBlocks();
        EnumMap<DyeColor, RegisteredBlocks> colors = BLOCKS.get(family.id());
        if (colors == null) {
            throw new IllegalArgumentException("No colored fluid pipe family is registered for " + family.id());
        }
        return colors.values().stream()
                .map(registration -> switch (variant) {
                    case PIPE -> registration.pipe().get();
                    case GLASS_PIPE -> registration.glassPipe().get();
                    case ENCASED_PIPE -> registration.encasedPipe().get();
                })
                .toList();
    }

    private static void requireBlocks() {
        if (BLOCKS.isEmpty()) {
            throw new IllegalStateException("Colored fluid pipe blocks have not been registered yet");
        }
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

    private static BlockBehaviour.Properties encasedPipeProperties() {
        return BlockBehaviour.Properties.of()
                .strength(1.5F, 6.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.COPPER);
    }

    private enum BlockVariant {
        PIPE,
        GLASS_PIPE,
        ENCASED_PIPE
    }

    public record RegisteredBlocks(
            PipeColorDefinitions.PipeFamily family,
            DyeColor color,
            DeferredHolder<Block, ? extends Block> pipe,
            DeferredHolder<Block, ? extends Block> glassPipe,
            DeferredHolder<Block, ? extends Block> encasedPipe
    ) {
        public String modelId() {
            return PipeColorDefinitions.sharedColoredModelId(color);
        }
    }

    public record RegisteredItems(
            PipeColorDefinitions.PipeFamily family,
            DyeColor color,
            DeferredHolder<Item, BlockItem> pipe
    ) {
    }
}
