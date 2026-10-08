package net.mads.industron.mixin;

import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import net.mads.industron.integration.create.kinetic.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.crafting.RecipeHolder;
import java.util.Optional;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value=MechanicalPressBlockEntity.class,remap=false)
public abstract class CreatePressRecipeMixin implements NativePressRecipeHost {
    @Unique private int industron$selectedDuration=400;
    @Unique private String industron$pendingRecipe="";
    @Inject(method="getRecipe",at=@At("RETURN"))
    private void industron$remember(ItemStack item,CallbackInfoReturnable<Optional<RecipeHolder<PressingRecipe>>> cir) {
        cir.getReturnValue().ifPresent(r->industron$selectedDuration=Math.max(1,r.value().getProcessingDuration()));
    }
    @Inject(method="write",at=@At("TAIL"))
    private void industron$save(CompoundTag tag,HolderLookup.Provider registries,boolean packet,CallbackInfo ci) {
        var access=(CreateBasinRecipeAccess)(Object)this;
        var press=(MechanicalPressBlockEntity)(Object)this;
        tag.putString("IndustronBasinRecipe",access.industron$currentRecipe()==null?industron$pendingRecipe:
                NativeCreateRecipeIdentity.id(press.getLevel(),access.industron$currentRecipe()));
        tag.putInt("IndustronSelectedPressDuration",industron$selectedDuration);
    }
    @Inject(method="read",at=@At("TAIL"))
    private void industron$load(CompoundTag tag,HolderLookup.Provider registries,boolean packet,CallbackInfo ci) {industron$pendingRecipe=tag.getString("IndustronBasinRecipe");industron$selectedDuration=tag.contains("IndustronSelectedPressDuration")?Math.max(1,tag.getInt("IndustronSelectedPressDuration")):400;}
    @Override public int industron$pressDuration() {
        MechanicalPressBlockEntity press=(MechanicalPressBlockEntity)(Object)this;
        if(!industron$pendingRecipe.isEmpty() && press.getLevel()!=null && !press.getLevel().isClientSide) {
            ((CreateBasinRecipeAccess)(Object)this).industron$currentRecipe(NativeCreateRecipeIdentity.restore(press.getLevel(),industron$pendingRecipe));
            industron$pendingRecipe="";
        }
        if(press.getPressingBehaviour().onBasin()) {
            var recipe=((CreateBasinRecipeAccess)(Object)this).industron$currentRecipe();
            if(recipe instanceof StandardProcessingRecipe<?> processing)return Math.max(1,processing.getProcessingDuration());
        }
        return industron$selectedDuration;
    }
    @Inject(method="matchStaticFilters",at=@At("RETURN"),cancellable=true)
    private void industron$processRecipesOnly(net.minecraft.world.item.crafting.RecipeHolder<? extends net.minecraft.world.item.crafting.Recipe<?>> recipe,
            CallbackInfoReturnable<Boolean> cir) {
        if(recipe.value() instanceof net.minecraft.world.item.crafting.CraftingRecipe)cir.setReturnValue(false);
    }

}
