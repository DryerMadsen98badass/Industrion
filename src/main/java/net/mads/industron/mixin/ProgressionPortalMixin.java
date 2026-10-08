package net.mads.industron.mixin;

import net.mads.industron.progression.PortalActivation;
import net.minecraft.world.level.portal.PortalShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PortalShape.class)
public abstract class ProgressionPortalMixin {
    @Inject(method="createPortalBlocks",at=@At("HEAD"),cancellable=true)
    private void industron$requireActivator(CallbackInfo callback) {
        if(!PortalActivation.allowed())callback.cancel();
    }
}
