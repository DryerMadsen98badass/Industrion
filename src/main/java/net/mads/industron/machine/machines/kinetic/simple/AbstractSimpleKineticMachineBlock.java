package net.mads.industron.machine.machines.kinetic.simple;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class AbstractSimpleKineticMachineBlock<T extends AbstractSimpleKineticMachineBlockEntity> extends KineticBlock implements IBE<T> {
    private static final VoxelShape SHAPE = box(0, 0, 1, 16, 14, 15);

    protected AbstractSimpleKineticMachineBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(3.0F, 6.0F)
                .sound(SoundType.WOOD)
                .noOcclusion());
    }

    @Override
    protected abstract MapCodec<? extends KineticBlock> codec();

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == getRotationAxis(state);
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return Axis.X;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof AbstractSimpleKineticMachineBlockEntity machine)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        return machine.insertHeldItem(player, stack)
                ? ItemInteractionResult.SUCCESS
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof AbstractSimpleKineticMachineBlockEntity machine)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        return machine.extractToPlayer(player) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public abstract Class<T> getBlockEntityClass();

    @Override
    public abstract BlockEntityType<? extends T> getBlockEntityType();
}
