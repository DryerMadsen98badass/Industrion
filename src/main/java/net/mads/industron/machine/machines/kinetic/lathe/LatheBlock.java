package net.mads.industron.machine.machines.kinetic.lathe;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.mads.industron.machine.machines.kinetic.KineticMachines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidUtil;

/** Native Create kinetic block; CE processing lives in the block entity. */
public final class LatheBlock extends HorizontalKineticBlock implements IBE<LatheBlockEntity> {
    public static final MapCodec<LatheBlock> CODEC = simpleCodec(LatheBlock::new);
    private static final VoxelShape NORTH_SHAPE = Shapes.or(
            box(0, 1, 0, 1.05, 15, 16),
            box(0, 0, 0, 16, 3, 16),
            box(0, 3, 3, 5, 14, 13),
            box(12, 3, 5, 16, 11, 11),
            box(4, 3, 3, 13, 5, 13));
    private static final VoxelShape EAST_SHAPE = Shapes.or(
            box(0, 1, 0, 16, 15, 1.05),
            box(0, 0, 0, 16, 3, 16),
            box(3, 3, 0, 13, 14, 5),
            box(5, 3, 12, 11, 11, 16),
            box(3, 3, 4, 13, 5, 13));
    private static final VoxelShape SOUTH_SHAPE = Shapes.or(
            box(14.95, 1, 0, 16, 15, 16),
            box(0, 0, 0, 16, 3, 16),
            box(11, 3, 3, 16, 14, 13),
            box(0, 3, 5, 4, 11, 11),
            box(3, 3, 3, 12, 5, 13));
    private static final VoxelShape WEST_SHAPE = Shapes.or(
            box(0, 1, 14.95, 16, 15, 16),
            box(0, 0, 0, 16, 3, 16),
            box(3, 3, 11, 13, 14, 16),
            box(5, 3, 0, 11, 11, 4),
            box(3, 3, 3, 13, 5, 12));

    public LatheBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends LatheBlock> codec() { return CODEC; }
    @Override public Direction.Axis getRotationAxis(BlockState state) { return state.getValue(HORIZONTAL_FACING).getClockWise().getAxis(); }
    @Override public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(HORIZONTAL_FACING).getCounterClockWise();
    }
    @Override public Class<LatheBlockEntity> getBlockEntityClass() { return LatheBlockEntity.class; }
    @Override public BlockEntityType<? extends LatheBlockEntity> getBlockEntityType() { return KineticMachines.LATHE_ENTITY.get(); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(HORIZONTAL_FACING)) {
            case EAST -> EAST_SHAPE; case SOUTH -> SOUTH_SHAPE; case WEST -> WEST_SHAPE; default -> NORTH_SHAPE;
        };
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (AllItems.WRENCH.isIn(stack) || !player.mayBuild()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!(level.getBlockEntity(pos) instanceof LatheBlockEntity be)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;
        if (FluidUtil.interactWithFluidHandler(player, hand, be.ceProcessing().manualFluidCapability(player.isShiftKeyDown()))) return ItemInteractionResult.SUCCESS;
        return be.ceProcessing().insertHeld(player, stack) ? ItemInteractionResult.SUCCESS : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.mayBuild() || !(level.getBlockEntity(pos) instanceof LatheBlockEntity be)) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown() && hit.getDirection() == state.getValue(HORIZONTAL_FACING).getOpposite()) {
            be.ceProcessing().cycleCircuit(player);
            return InteractionResult.SUCCESS;
        }
        return be.ceProcessing().extractToPlayer(player, player.isShiftKeyDown()) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }
}
