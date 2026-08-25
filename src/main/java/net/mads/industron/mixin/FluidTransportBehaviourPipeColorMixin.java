package net.mads.industron.mixin;

import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.mads.industron.transport.color.PipeColorManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FluidTransportBehaviour.class)
public abstract class FluidTransportBehaviourPipeColorMixin {
    @Inject(method = "getRenderedRimAttachment", at = @At("HEAD"), cancellable = true)
    private void createExpansion$showRimForIncompatiblePipeColors(
            BlockAndTintGetter world,
            BlockPos pos,
            BlockState state,
            Direction direction,
            CallbackInfoReturnable<FluidTransportBehaviour.AttachmentTypes> cir
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
            cir.setReturnValue(FluidTransportBehaviour.AttachmentTypes.RIM);
        }
    }

    @Inject(method = {"createConnectionData", "wipePressure"}, at = @At("TAIL"))
    private void createExpansion$removeIncompatiblePipeConnections(CallbackInfo ci) {
        FluidTransportBehaviour self = (FluidTransportBehaviour) (Object) this;
        SmartBlockEntity blockEntity = self.blockEntity;
        Level level = blockEntity.getLevel();
        if (level == null || self.interfaces == null || !PipeColorManager.isColorablePipe(blockEntity.getBlockState())) {
            return;
        }

        BlockState state = blockEntity.getBlockState();
        BlockPos pos = blockEntity.getBlockPos();
        self.interfaces.keySet().removeIf(direction -> {
            BlockPos neighbourPos = pos.relative(direction);
            return level.isLoaded(neighbourPos)
                    && PipeColorManager.isColorablePipe(level.getBlockState(neighbourPos))
                    && !PipeColorManager.canConnect(state, level.getBlockState(neighbourPos));
        });
    }
}
