package net.mads.industron.mixin;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.millstone.MillstoneBlockEntity;
import net.mads.industron.integration.create.kinetic.KineticProcessingRules;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value=MillstoneBlockEntity.class,remap=false)
public abstract class CreateMillstoneTimingMixin extends KineticBlockEntity {
    @Shadow public int timer;
    @Shadow public net.neoforged.neoforge.items.ItemStackHandler inputInv;
    @Shadow private com.simibubi.create.content.kinetics.millstone.MillingRecipe lastRecipe;
    @Unique private String industron$pendingRecipe="";
    @Unique private final KineticProcessingRules.WorkCredit industron$work=new KineticProcessingRules.WorkCredit();
    protected CreateMillstoneTimingMixin(BlockEntityType<?> type,BlockPos pos,BlockState state) {super(type,pos,state);}
    @Inject(method="tick",at=@At("HEAD"))
    private void industron$validateInput(CallbackInfo ci) {
        if(level==null || level.isClientSide)return;
        if(!industron$pendingRecipe.isEmpty()) {
            var recipe=net.mads.industron.integration.create.kinetic.NativeCreateRecipeIdentity.restore(level,industron$pendingRecipe);
            lastRecipe=recipe instanceof com.simibubi.create.content.kinetics.millstone.MillingRecipe milling?milling:null;
            industron$pendingRecipe="";
            if(lastRecipe==null) {timer=0;industron$work.reset();}
        }
        if(lastRecipe!=null && !lastRecipe.matches(new net.neoforged.neoforge.items.wrapper.RecipeWrapper(inputInv),level)) {
            lastRecipe=null;timer=0;industron$work.reset();
        }
    }
    @Inject(method="getProcessingSpeed",at=@At("HEAD"),cancellable=true)
    private void industron$speed(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(isOverStressed() || Math.abs(getSpeed())>KineticProcessingRules.MAX_RPM?0:industron$work.advance(getSpeed()));
    }
    @Inject(method="write",at=@At("TAIL"))
    private void industron$save(CompoundTag tag,HolderLookup.Provider registries,boolean packet,CallbackInfo ci) {
        tag.putString("IndustronMillingRecipe",lastRecipe==null?industron$pendingRecipe:
                net.mads.industron.integration.create.kinetic.NativeCreateRecipeIdentity.id(level,lastRecipe));
        tag.putDouble("IndustronWorkCredit",industron$work.value());
    }
    @Inject(method="read",at=@At("TAIL"))
    private void industron$load(CompoundTag tag,HolderLookup.Provider registries,boolean packet,CallbackInfo ci) {industron$pendingRecipe=tag.getString("IndustronMillingRecipe");industron$work.restore(tag.getDouble("IndustronWorkCredit"));}
}
