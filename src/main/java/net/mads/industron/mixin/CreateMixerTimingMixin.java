package net.mads.industron.mixin;

import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.processing.basin.BasinOperatingBlockEntity;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import net.mads.industron.integration.create.kinetic.KineticProcessingRules;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value=MechanicalMixerBlockEntity.class,remap=false)
public abstract class CreateMixerTimingMixin extends BasinOperatingBlockEntity {
    @Shadow public int runningTicks;
    @Shadow public int processingTicks;
    @Shadow public boolean running;
    @Unique private boolean industron$timingActive;
    @Unique private String industron$pendingRecipe="";
    @Unique private final KineticProcessingRules.WorkCredit industron$work=new KineticProcessingRules.WorkCredit();
    protected CreateMixerTimingMixin(BlockEntityType<?> type,BlockPos pos,BlockState state) {super(type,pos,state);}
    @Inject(method="tick",at=@At("HEAD"),cancellable=true)
    private void industron$process(CallbackInfo ci) {
        if(level!=null && !level.isClientSide && !industron$pendingRecipe.isEmpty()) {
            currentRecipe=net.mads.industron.integration.create.kinetic.NativeCreateRecipeIdentity.restore(level,industron$pendingRecipe);
            industron$pendingRecipe="";
            if(currentRecipe==null || !matchBasinRecipe(currentRecipe)) {
                running=false;processingTicks=-1;basinChecker.scheduleUpdate();sendData();
            }
        }
        if(!running || runningTicks!=20) {industron$timingActive=false;return;}
        if(level==null || level.isClientSide || !(currentRecipe instanceof StandardProcessingRecipe<?> recipe))return;
        if(!industron$timingActive || processingTicks<0) {
            processingTicks=Math.max(1,recipe.getProcessingDuration());industron$timingActive=true;
        }
        int steps=isOverStressed() || Math.abs(getSpeed())>KineticProcessingRules.MAX_RPM || !isSpeedRequirementFulfilled()
                ?0:industron$work.advance(getSpeed());
        if(steps==0) {super.tick();ci.cancel();return;}
        // Let Create run particles, sounds, basin validation, output checks and recipe application.
        processingTicks=Math.max(1,processingTicks-steps+1);
    }
    @Inject(method="write",at=@At("TAIL"))
    private void industron$save(CompoundTag tag,HolderLookup.Provider registries,boolean packet,CallbackInfo ci) {
        tag.putString("IndustronBasinRecipe",currentRecipe==null?industron$pendingRecipe:
                net.mads.industron.integration.create.kinetic.NativeCreateRecipeIdentity.id(level,currentRecipe));
        tag.putBoolean("IndustronTimingActive",industron$timingActive);tag.putInt("IndustronProcessingTicks",processingTicks);tag.putDouble("IndustronWorkCredit",industron$work.value());
    }
    @Inject(method="read",at=@At("TAIL"))
    private void industron$load(CompoundTag tag,HolderLookup.Provider registries,boolean packet,CallbackInfo ci) {
        industron$pendingRecipe=tag.getString("IndustronBasinRecipe");
        industron$timingActive=tag.getBoolean("IndustronTimingActive");
        if(tag.contains("IndustronProcessingTicks"))processingTicks=tag.getInt("IndustronProcessingTicks");
        industron$work.restore(tag.getDouble("IndustronWorkCredit"));
    }
    @Inject(method="matchStaticFilters",at=@At("RETURN"),cancellable=true)
    private void industron$processRecipesOnly(net.minecraft.world.item.crafting.RecipeHolder<? extends net.minecraft.world.item.crafting.Recipe<?>> recipe,
            CallbackInfoReturnable<Boolean> cir) {
        if(recipe.value() instanceof net.minecraft.world.item.crafting.CraftingRecipe)cir.setReturnValue(false);
    }

}
