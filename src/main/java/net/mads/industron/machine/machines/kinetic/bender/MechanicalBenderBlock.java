package net.mads.industron.machine.machines.kinetic.bender;

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
public final class MechanicalBenderBlock extends HorizontalKineticBlock implements IBE<MechanicalBenderBlockEntity> {
    public static final MapCodec<MechanicalBenderBlock> CODEC = simpleCodec(MechanicalBenderBlock::new);
    private static final VoxelShape NORTH_SHAPE = Shapes.or(
            box(0, 0, 0, 16, 4, 16),
            box(2, 4, 2, 14, 14, 14));
    private static final VoxelShape EAST_SHAPE = Shapes.or(
            box(0, 0, 0, 16, 4, 16),
            box(2, 4, 2, 14, 14, 14));
    private static final VoxelShape SOUTH_SHAPE = Shapes.or(
            box(0, 0, 0, 16, 4, 16),
            box(2, 4, 2, 14, 14, 14));
    private static final VoxelShape WEST_SHAPE = Shapes.or(
            box(0, 0, 0, 16, 4, 16),
            box(2, 4, 2, 14, 14, 14));

    public MechanicalBenderBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends MechanicalBenderBlock> codec() { return CODEC; }
    @Override public Direction.Axis getRotationAxis(BlockState state) { return Direction.Axis.Y; }
    @Override public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face == Direction.DOWN;
    }
    @Override public Class<MechanicalBenderBlockEntity> getBlockEntityClass() { return MechanicalBenderBlockEntity.class; }
    @Override public BlockEntityType<? extends MechanicalBenderBlockEntity> getBlockEntityType() { return KineticMachines.MECHANICALBENDER_ENTITY.get(); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(HORIZONTAL_FACING)) {
            case EAST -> EAST_SHAPE; case SOUTH -> SOUTH_SHAPE; case WEST -> WEST_SHAPE; default -> NORTH_SHAPE;
        };
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (AllItems.WRENCH.isIn(stack) || !player.mayBuild()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!(level.getBlockEntity(pos) instanceof MechanicalBenderBlockEntity be)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;
        if (FluidUtil.interactWithFluidHandler(player, hand, be.ceProcessing().manualFluidCapability(player.isShiftKeyDown()))) return ItemInteractionResult.SUCCESS;
        return be.ceProcessing().insertHeld(player, stack) ? ItemInteractionResult.SUCCESS : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.mayBuild() || !(level.getBlockEntity(pos) instanceof MechanicalBenderBlockEntity be)) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown() && hit.getDirection() == state.getValue(HORIZONTAL_FACING).getOpposite()) {
            be.ceProcessing().cycleCircuit(player);
            return InteractionResult.SUCCESS;
        }
        return be.ceProcessing().extractToPlayer(player, player.isShiftKeyDown()) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }
}
