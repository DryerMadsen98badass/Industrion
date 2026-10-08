package net.mads.industron.mixin;

import com.simibubi.create.content.kinetics.press.PressingBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.mads.industron.integration.create.kinetic.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value=PressingBehaviour.class,remap=false)
public abstract class CreatePressTimingMixin extends BeltProcessingBehaviour implements NativePressWork {
    @Shadow public PressingBehaviour.PressingBehaviourSpecifics specifics;
    @Shadow public int runningTicks;
    @Shadow public int prevRunningTicks;
    @Shadow public boolean running;
    @Shadow public PressingBehaviour.Mode mode;
    @Unique private int industron$remaining=-1;
    @Unique private boolean industron$applied;
    @Unique private int industron$beltWait;
    @Unique private final KineticProcessingRules.WorkCredit industron$work=new KineticProcessingRules.WorkCredit();
    protected CreatePressTimingMixin(SmartBlockEntity entity) {super(entity);}
    @Override public boolean industron$pressReady() {return !(specifics instanceof NativePressRecipeHost) || industron$remaining==0;}
    @Override public void industron$pressApplied() {industron$applied=true;}
    @Inject(method="start",at=@At("HEAD"))
    private void industron$start(PressingBehaviour.Mode mode,CallbackInfo ci) {industron$remaining=-1;industron$applied=false;industron$beltWait=0;industron$work.reset();}
    @Inject(method="tick",at=@At("HEAD"),cancellable=true)
    private void industron$waitForWork(CallbackInfo ci) {
        if(!running || runningTicks!=PressingBehaviour.CYCLE/2 || !(specifics instanceof NativePressRecipeHost host))return;
        if(industron$remaining<0)industron$remaining=host.industron$pressDuration();
        if(blockEntity.getLevel()==null)return;
        if(blockEntity.getLevel().isClientSide) {
            if(industron$remaining!=0 || (mode==PressingBehaviour.Mode.BELT && !industron$applied)) {
                prevRunningTicks=runningTicks;super.tick();ci.cancel();
            }
            return;
        }
        double rpm=Math.abs(specifics.getKineticSpeed());
        int steps=rpm>KineticProcessingRules.MAX_RPM?0:industron$work.advance(rpm);
        industron$remaining=Math.max(0,industron$remaining-steps);
        boolean awaitingBelt=industron$remaining==0 && mode==PressingBehaviour.Mode.BELT && !industron$applied && industron$beltWait++<20;
        if(industron$remaining>0 || awaitingBelt) {prevRunningTicks=runningTicks;super.tick();ci.cancel();}
    }
    @Inject(method="write",at=@At("TAIL"))
    private void industron$save(CompoundTag tag,HolderLookup.Provider registries,boolean packet,CallbackInfo ci) {
        tag.putBoolean("IndustronPressApplied",industron$applied);tag.putInt("IndustronBeltWait",industron$beltWait);tag.putInt("IndustronPressWork",industron$remaining);tag.putDouble("IndustronPressCredit",industron$work.value());
    }
    @Inject(method="read",at=@At("TAIL"))
    private void industron$load(CompoundTag tag,HolderLookup.Provider registries,boolean packet,CallbackInfo ci) {
        industron$applied=tag.getBoolean("IndustronPressApplied");industron$beltWait=tag.getInt("IndustronBeltWait");
        industron$remaining=tag.contains("IndustronPressWork")?tag.getInt("IndustronPressWork"):-1;
        industron$work.restore(tag.getDouble("IndustronPressCredit"));
    }
}
