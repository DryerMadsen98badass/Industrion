package net.mads.industron.mixin;

import com.simibubi.create.content.kinetics.press.BeltPressingCallbacks;
import com.simibubi.create.content.kinetics.press.PressingBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import net.mads.industron.integration.create.kinetic.NativePressWork;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value=BeltPressingCallbacks.class,remap=false)
public abstract class CreateBeltPressTimingMixin {
    @Inject(method="whenItemHeld",at=@At("HEAD"),cancellable=true)
    private static void industron$wait(TransportedItemStack item,TransportedItemStackHandlerBehaviour handler,
            PressingBehaviour pressing,CallbackInfoReturnable<BeltProcessingBehaviour.ProcessingResult> cir) {
        if(pressing.running && pressing instanceof NativePressWork work && !work.industron$pressReady())
            cir.setReturnValue(BeltProcessingBehaviour.ProcessingResult.HOLD);
    }
    @Inject(method="whenItemHeld",at=@At("RETURN"))
    private static void industron$acknowledge(TransportedItemStack item,TransportedItemStackHandlerBehaviour handler,
            PressingBehaviour pressing,CallbackInfoReturnable<BeltProcessingBehaviour.ProcessingResult> cir) {
        if(pressing.running && pressing.runningTicks==PressingBehaviour.CYCLE/2
                && pressing instanceof NativePressWork work && work.industron$pressReady())work.industron$pressApplied();
    }

}
