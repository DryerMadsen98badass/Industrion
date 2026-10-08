package net.mads.industron.machine.foundry;

import net.mads.industron.block.loot.AssemblySalvageBlock;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.extensions.IPlayerExtension;

import com.mojang.serialization.MapCodec;
import net.mads.industron.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Bufferless Foundry controller/ability block. All future storage belongs to the controller. */
public class FoundryPartBlock extends DirectionalBlock implements EntityBlock, AssemblySalvageBlock {
    public static final MapCodec<FoundryPartBlock> CODEC = simpleCodec(properties ->
            new FoundryPartBlock(FoundryPartType.ITEM_INPUT_BUS, properties));
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    private final FoundryPartType partType;

    public FoundryPartBlock(FoundryPartType partType) {
        this(partType, BlockBehaviour.Properties.of()
                .requiresCorrectToolForDrops()
                .strength(3.0F, 8.0F)
                .sound(SoundType.DEEPSLATE_BRICKS));
    }

    public FoundryPartBlock(FoundryPartType partType, BlockBehaviour.Properties properties) {
        super(properties);
        this.partType = partType;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public FoundryPartType partType() {
        return partType;
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing;
        if (partType.isController()) {
            facing = context.getHorizontalDirection().getOpposite();
        } else {
            Direction nearest = context.getNearestLookingDirection();
            boolean sneak = context.getPlayer() != null && context.getPlayer().isShiftKeyDown();
            facing = sneak ? nearest : nearest.getOpposite();
        }
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FoundryBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> blockEntityType
    ) {
        if (level.isClientSide() || !partType.isController() || blockEntityType != BlockEntityRegistry.FOUNDRY_PART.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) ->
                FoundryBlockEntity.tick(tickerLevel, pos, tickerState, (FoundryBlockEntity) blockEntity);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (!partType.isController()) return InteractionResult.PASS;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FoundryBlockEntity foundry) {
            ((IPlayerExtension) player).openMenu(foundry, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            BlockPos fromPos,
            boolean isMoving
    ) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FoundryBlockEntity foundry) {
            foundry.markControllerDirty();
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock()
                && !level.isClientSide()
                && level.getBlockEntity(pos) instanceof FoundryBlockEntity foundry) {
            if (partType.isController()) {
                foundry.dropStoredItemsAndDiscardFluids();
                foundry.disassemble();
            } else {
                foundry.markControllerDirty();
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
