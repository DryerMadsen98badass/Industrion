package net.mads.industron.mixin;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=ProcessingRecipe.class,remap=false)
public abstract class CreatePressDurationMixin {
    @Inject(method="canSpecifyDuration",at=@At("HEAD"),cancellable=true)
    private void industron$duration(CallbackInfoReturnable<Boolean> cir) {if((Object)this instanceof PressingRecipe)cir.setReturnValue(true);}
}
