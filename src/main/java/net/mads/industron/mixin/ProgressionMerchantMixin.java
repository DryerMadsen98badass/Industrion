package net.mads.industron.mixin;

import net.mads.industron.progression.ProgressionTrades;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Migrates stored offers too, so existing villagers and wandering traders do not keep legacy costs. */
@Mixin(AbstractVillager.class)
public abstract class ProgressionMerchantMixin {
    @Inject(method="getOffers",at=@At("RETURN"))
    private void industron$offers(CallbackInfoReturnable<MerchantOffers> callback) {
        MerchantOffers offers=callback.getReturnValue();
        for(int i=offers.size()-1;i>=0;i--) {
            var replacement=ProgressionTrades.replace(offers.get(i));
            if(replacement==null)offers.remove(i);
            else offers.set(i,replacement);
        }
    }
}
