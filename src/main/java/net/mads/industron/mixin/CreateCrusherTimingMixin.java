package net.mads.industron.mixin;

import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingInventory;
import net.mads.industron.integration.create.kinetic.KineticProcessingRules;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(value=CrushingWheelControllerBlockEntity.class,remap=false)
public abstract class CreateCrusherTimingMixin {
    @Shadow public float crushingspeed;
    @Shadow public ProcessingInventory inventory;
    // Native controller speed is wheel RPM / 50. Only item processing changes, never entity damage/motion.
    @ModifyVariable(method="tick",at=@At("STORE"),ordinal=1)
    private float industron$itemSpeed(float nativeSpeed) {
        if(inventory.appliedRecipe)return nativeSpeed;
        float rpm=Math.abs(crushingspeed*50F);
        return rpm>KineticProcessingRules.MAX_RPM?0:(float)KineticProcessingRules.speed(rpm);
    }
    @ModifyConstant(method="tick",constant=@Constant(floatValue=20F,ordinal=1))
    private float industron$finishBeforeApplying(float original) {return 1F;}
}
