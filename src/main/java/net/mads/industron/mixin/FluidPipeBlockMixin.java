package net.mads.industron.mixin;

import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.mads.industron.transport.color.ColoredPipeBlock;
import net.mads.industron.transport.color.PipeColorManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FluidPipeBlock.class)
public abstract class FluidPipeBlockMixin {
    @Inject(method = "updateBlockState", at = @At("HEAD"), cancellable = true)
    private void createExpansion$updateColoredPipeState(
            BlockState state,
            Direction preferredDirection,
            @Nullable Direction ignore,
            BlockAndTintGetter world,
            BlockPos pos,
            CallbackInfoReturnable<BlockState> cir
    ) {
        if (!(state.getBlock() instanceof ColoredPipeBlock)) {
            return;
        }

        BracketedBlockEntityBehaviour bracket = BlockEntityBehaviour.get(
                world,
                pos,
                BracketedBlockEntityBehaviour.TYPE
        );
        if (bracket != null && bracket.isBracketPresent()) {
            cir.setReturnValue(state);
            return;
        }

        BlockState previousState = state;
        int previousSides = 0;
        for (Direction direction : Direction.values()) {
            if (previousState.getValue(FluidPipeBlock.PROPERTY_BY_DIRECTION.get(direction))) {
                previousSides++;
            }
        }

        for (Direction direction : Direction.values()) {
            if (direction == ignore) {
                continue;
            }

            BlockPos neighbourPos = pos.relative(direction);
            BlockState neighbourState = world.getBlockState(neighbourPos);
            boolean shouldConnect = PipeColorManager.canConnect(state, neighbourState)
                    && FluidPipeBlock.canConnectTo(world, neighbourPos, neighbourState, direction);
            state = state.setValue(FluidPipeBlock.PROPERTY_BY_DIRECTION.get(direction), shouldConnect);
        }

        Direction connectedDirection = null;
        for (Direction direction : Direction.values()) {
            if (!state.getValue(FluidPipeBlock.PROPERTY_BY_DIRECTION.get(direction))) {
                continue;
            }
            if (connectedDirection != null) {
                cir.setReturnValue(state);
                return;
            }
            connectedDirection = direction;
        }

        if (connectedDirection != null) {
            cir.setReturnValue(state.setValue(
                    FluidPipeBlock.PROPERTY_BY_DIRECTION.get(connectedDirection.getOpposite()),
                    true
            ));
            return;
        }

        if (previousSides == 2) {
            cir.setReturnValue(previousState);
            return;
        }

        cir.setReturnValue(state
                .setValue(FluidPipeBlock.PROPERTY_BY_DIRECTION.get(preferredDirection), true)
                .setValue(FluidPipeBlock.PROPERTY_BY_DIRECTION.get(preferredDirection.getOpposite()), true));
    }

    @Inject(method = "shouldDrawRim", at = @At("HEAD"), cancellable = true)
    private static void createExpansion$forceRimForIncompatiblePipeColors(
            BlockAndTintGetter world,
            BlockPos pos,
            BlockState state,
            Direction direction,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!PipeColorManager.isOpenToward(state, direction)) {
            return;
        }

        BlockPos neighbourPos = pos.relative(direction);
        BlockState neighbour = world.getBlockState(neighbourPos);
        if (!PipeColorManager.isColorablePipe(state) || !PipeColorManager.isColorablePipe(neighbour)) {
            return;
        }

        if (!PipeColorManager.canConnect(state, neighbour)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "canConnectTo", at = @At("RETURN"), cancellable = true)
    private static void createExpansion$filterPipeColorConnection(
            BlockAndTintGetter world,
            BlockPos neighbourPos,
            BlockState neighbour,
            Direction direction,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!cir.getReturnValue()) {
            return;
        }

        BlockPos sourcePos = neighbourPos.relative(direction.getOpposite());
        BlockState sourceState = world.getBlockState(sourcePos);
        if (!PipeColorManager.isColorablePipe(sourceState) || !PipeColorManager.isColorablePipe(neighbour)) {
            return;
        }

        if (!PipeColorManager.canConnect(sourceState, neighbour)) {
            cir.setReturnValue(false);
        }
    }
}
