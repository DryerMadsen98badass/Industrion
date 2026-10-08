package net.mads.industron.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Singleblock machine variant that can occupy a water source. */
public final class WaterloggableSingleBlockMachineBlock extends SingleBlockMachineBlock implements SimpleWaterloggedBlock {
    private static final VoxelShape RIVER_WASHER_SHAPE_NORTH_SOUTH = Shapes.or(
            Block.box(0, 0, 0, 16, 3, 16),
            Block.box(0, 3, 0, 2, 10, 16),
            Block.box(14, 3, 0, 16, 10, 16),
            Block.box(2, 3, 0, 14, 7, 2),
            Block.box(2, 3, 14, 14, 7, 16)
    );

    private static final VoxelShape RIVER_WASHER_SHAPE_EAST_WEST = Shapes.or(
            Block.box(0, 0, 0, 16, 3, 16),
            Block.box(0, 3, 0, 16, 10, 2),
            Block.box(0, 3, 14, 16, 10, 16),
            Block.box(0, 3, 2, 2, 7, 14),
            Block.box(14, 3, 2, 16, 7, 14)
    );

    public WaterloggableSingleBlockMachineBlock(SingleBlockMachineInstance instance) {
        super(instance);
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState base = super.getStateForPlacement(context);
        if (base == null) return null;
        return base.setValue(
                BlockStateProperties.WATERLOGGED,
                context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER
        );
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(BlockStateProperties.WATERLOGGED)
                ? Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos
    ) {
        if (state.getValue(BlockStateProperties.WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.WATERLOGGED);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        SingleBlockMachineInstance current = instance();
        if (current != null && current.definition().id().equals("river_washer")) {
            return state.getValue(FACING).getAxis() == Direction.Axis.X
                    ? RIVER_WASHER_SHAPE_EAST_WEST
                    : RIVER_WASHER_SHAPE_NORTH_SOUTH;
        }
        return super.getShape(state, level, pos, context);
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        SingleBlockMachineInstance current = instance();
        if (current != null && current.definition().id().equals("river_washer")) {
            return Shapes.empty();
        }
        return super.getOcclusionShape(state, level, pos);
    }
}
