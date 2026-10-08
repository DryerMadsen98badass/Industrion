package net.mads.industron.mixin;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import net.mads.industron.climate.EntityClimate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Cow.class)
public abstract class CowThermalMixin {
    @Inject(method="mobInteract",at=@At("HEAD"),cancellable=true)
    private void industron$milk(Player player,InteractionHand hand,CallbackInfoReturnable<InteractionResult> cir) {
        Cow cow=(Cow)(Object)this;
        if(!cow.level().isClientSide && player.getItemInHand(hand).is(Items.BUCKET)&&EntityClimate.strained(cow)) {
            player.displayClientMessage(Component.literal("This animal needs better temperature conditions before it can give milk."),true);
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
