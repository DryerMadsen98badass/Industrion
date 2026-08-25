package net.mads.industron.mixin;

import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import net.mads.industron.transport.FluidTransportRates;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(PumpBlockEntity.class)
public abstract class PumpBlockEntityMixin {
    @ModifyArg(
            method = "distributePressureTo",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/Math;abs(F)F"
            ),
            index = 0
    )
    private float createExpansion$scaleDistributedPressure(float vanillaPressure) {
        return FluidTransportRates.scalePumpPressure(
                (PumpBlockEntity) (Object) this,
                vanillaPressure
        );
    }
}
