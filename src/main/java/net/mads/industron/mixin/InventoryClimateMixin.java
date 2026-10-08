package net.mads.industron.mixin;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AbstractContainerScreen.class)
public abstract class InventoryClimateMixin {
    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Inject(method="render",at=@At("RETURN"))
    private void industron$icons(GuiGraphics graphics,int mouseX,int mouseY,float partial,CallbackInfo ci) {
        if((Object)this instanceof InventoryScreen || (Object)this instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen)net.mads.industron.client.ClimateClient.inventory(graphics,mouseX,mouseY,leftPos,topPos);
    }
}
