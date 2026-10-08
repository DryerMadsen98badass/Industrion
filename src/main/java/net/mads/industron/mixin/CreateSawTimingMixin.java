package net.mads.industron.mixin;

import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingInventory;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.mads.industron.integration.create.kinetic.KineticProcessingRules;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(value=SawBlockEntity.class,remap=false)
public abstract class CreateSawTimingMixin {
    @Shadow public ProcessingInventory inventory;
    @ModifyVariable(method="tick",at=@At("STORE"),ordinal=0)
    private float industron$itemSpeed(float nativeSpeed) {
        if(inventory.appliedRecipe)return nativeSpeed;
        KineticBlockEntity machine=(KineticBlockEntity)(Object)this;
        float rpm=Math.abs(machine.getSpeed());
        return machine.isOverStressed() || rpm>KineticProcessingRules.MAX_RPM?0:(float)KineticProcessingRules.speed(rpm);
    }
    @ModifyConstant(method="tick",constant=@Constant(floatValue=5F))
    private float industron$finishBeforeApplying(float original) {return 1F;}
}
