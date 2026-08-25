package net.mads.industron.mixin;

import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import net.mads.industron.transport.FluidTransportRates;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(targets = "com.simibubi.create.content.fluids.pump.PumpBlockEntity$PumpFluidTransferBehaviour")
public abstract class PumpFluidTransferBehaviourMixin {
    @Shadow(remap = false) @Final PumpBlockEntity this$0;

    @ModifyArg(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/Math;abs(F)F"
            ),
            index = 0
    )
    private float createExpansion$scaleInterfacePressure(float vanillaPressure) {
        return FluidTransportRates.scalePumpPressure(this$0, vanillaPressure);
    }
}
