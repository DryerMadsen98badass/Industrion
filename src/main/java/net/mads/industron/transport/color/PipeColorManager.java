package net.mads.industron.transport.color;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.pipes.EncasedPipeBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.GlassFluidPipeBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.mads.industron.Industron;
import net.mads.industron.transport.FluidTransportGlassPipeBlock;
import net.mads.industron.transport.FluidTransportPipeBlock;
import net.mads.industron.transport.FluidTransportRegistrations;
import net.mads.industron.transport.TieredFluidPipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class PipeColorManager {
    /** Kept for source compatibility with old worlds/patch files; colors now live in block IDs, not block entity data. */
    public static final String COLOR_DATA_KEY = "IndustronPipeColor";

    public static final TagKey<Block> COLORABLE_FLUID_PIPES = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, "colorable_fluid_pipes")
    );

    private PipeColorManager() {
    }

    public static boolean isColorablePipe(BlockState state) {
        Block block = state.getBlock();
        return block instanceof ColoredPipeBlock
                || block instanceof TieredFluidPipe
                || AllBlocks.FLUID_PIPE.has(state)
                || AllBlocks.GLASS_FLUID_PIPE.has(state)
                || state.is(COLORABLE_FLUID_PIPES);
    }

    @Nullable
    public static DyeColor getColor(BlockGetter level, BlockPos pos) {
        if (level == null || pos == null) {
            return null;
        }
        return getColor(level.getBlockState(pos));
    }

    @Nullable
    public static DyeColor getColor(BlockEntity blockEntity) {
        return blockEntity == null ? null : getColor(blockEntity.getBlockState());
    }

    @Nullable
    public static DyeColor getColor(BlockState state) {
        return state.getBlock() instanceof ColoredPipeBlock colored ? colored.pipeColor() : null;
    }

    @Nullable
    public static PipeColorDefinitions.PipeFamily getFamily(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof ColoredPipeBlock colored) {
            return colored.pipeFamily();
        }
        if (AllBlocks.FLUID_PIPE.has(state) || AllBlocks.GLASS_FLUID_PIPE.has(state)) {
            return PipeColorDefinitions.CREATE_PIPE;
        }
        if (block instanceof TieredFluidPipe tieredPipe) {
            return PipeColorDefinitions.forTier(tieredPipe.transportTier());
        }
        return null;
    }

    public static boolean colorsCompatible(@Nullable DyeColor first, @Nullable DyeColor second) {
        return first == null || second == null || first == second;
    }

    public static boolean canConnect(BlockState firstState, BlockState secondState) {
        if (firstState == null || secondState == null) {
            return true;
        }
        if (!isColorablePipe(firstState) || !isColorablePipe(secondState)) {
            return true;
        }
        return colorsCompatible(getColor(firstState), getColor(secondState));
    }

    public static boolean canConnect(BlockGetter level, BlockPos firstPos, BlockPos secondPos) {
        if (level == null || firstPos == null || secondPos == null) {
            return true;
        }
        return canConnect(level.getBlockState(firstPos), level.getBlockState(secondPos));
    }

    public static boolean isOpenToward(BlockState state, Direction direction) {
        if (state == null || direction == null) {
            return false;
        }

        Property<Boolean> connection = FluidPipeBlock.PROPERTY_BY_DIRECTION.get(direction);
        if (state.hasProperty(connection)) {
            return state.getValue(connection);
        }

        if (state.getBlock() instanceof GlassFluidPipeBlock && state.hasProperty(GlassFluidPipeBlock.AXIS)) {
            return state.getValue(GlassFluidPipeBlock.AXIS) == direction.getAxis();
        }

        return false;
    }

    public static boolean setColor(Level level, BlockPos pos, @Nullable DyeColor color) {
        if (level == null || pos == null || !level.isLoaded(pos)) {
            return false;
        }

        BlockState oldState = level.getBlockState(pos);
        PipeColorDefinitions.PipeFamily family = getFamily(oldState);
        if (family == null || !isColorablePipe(oldState) || Objects.equals(getColor(oldState), color)) {
            return false;
        }

        boolean glass = oldState.getBlock() instanceof GlassFluidPipeBlock;
        boolean encased = oldState.getBlock() instanceof EncasedPipeBlock;
        Block targetBlock = targetBlock(family, color, glass, encased);
        if (targetBlock == null || targetBlock == oldState.getBlock()) {
            return false;
        }

        BracketedBlockEntityBehaviour oldBracket = BlockEntityBehaviour.get(
                level,
                pos,
                BracketedBlockEntityBehaviour.TYPE
        );
        BlockState bracketState = oldBracket == null ? null : oldBracket.removeBracket(true);

        FluidTransportBehaviour.cacheFlows(level, pos);
        BlockState newState = copyCompatibleProperties(oldState, targetBlock.defaultBlockState());
        level.setBlock(pos, newState, Block.UPDATE_ALL);
        FluidTransportBehaviour.loadFlows(level, pos);

        if (bracketState != null) {
            BracketedBlockEntityBehaviour newBracket = BlockEntityBehaviour.get(
                    level,
                    pos,
                    BracketedBlockEntityBehaviour.TYPE
            );
            if (newBracket != null && newBracket.canHaveBracket()) {
                newBracket.applyBracket(bracketState);
            }
        }

        refreshPipe(level, pos);
        for (Direction direction : Direction.values()) {
            BlockPos neighbourPos = pos.relative(direction);
            if (level.isLoaded(neighbourPos) && isColorablePipe(level.getBlockState(neighbourPos))) {
                refreshPipe(level, neighbourPos);
            }
        }
        return true;
    }

    private static Block targetBlock(
            PipeColorDefinitions.PipeFamily family,
            @Nullable DyeColor color,
            boolean glass,
            boolean encased
    ) {
        if (color != null) {
            ColoredFluidPipeRegistrations.RegisteredBlocks colored = ColoredFluidPipeRegistrations.blocks(family, color);
            if (encased) {
                return colored.encasedPipe().get();
            }
            return glass ? colored.glassPipe().get() : colored.pipe().get();
        }

        if (family.isCreatePipe()) {
            if (encased) {
                return AllBlocks.ENCASED_FLUID_PIPE.get();
            }
            return glass ? AllBlocks.GLASS_FLUID_PIPE.get() : AllBlocks.FLUID_PIPE.get();
        }

        if (encased) {
            return null;
        }

        FluidTransportRegistrations.RegisteredBlocks registration = FluidTransportRegistrations.blocks(family.tier());
        return glass ? registration.glassPipe().get() : registration.pipe().get();
    }

    private static BlockState copyCompatibleProperties(BlockState source, BlockState target) {
        BlockState result = target;
        for (Property<?> property : source.getProperties()) {
            if (result.hasProperty(property)) {
                result = copyProperty(source, result, property);
            }
        }
        return result;
    }

    private static <T extends Comparable<T>> BlockState copyProperty(
            BlockState source,
            BlockState target,
            Property<T> property
    ) {
        return target.setValue(property, source.getValue(property));
    }

    private static void refreshPipe(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!isColorablePipe(state)) {
            return;
        }

        if (state.getBlock() instanceof FluidPipeBlock pipe) {
            BlockState updated = pipe.updateBlockState(state, Direction.UP, null, level, pos);
            if (updated != state) {
                level.setBlock(pos, updated, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                state = updated;
            }
        }

        FluidTransportBehaviour behaviour = BlockEntityBehaviour.get(level, pos, FluidTransportBehaviour.TYPE);
        if (behaviour != null) {
            behaviour.wipePressure();
        }

        if (!level.isClientSide) {
            FluidPropagator.propagateChangedPipe(level, pos, state);
        }
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
    }
}
