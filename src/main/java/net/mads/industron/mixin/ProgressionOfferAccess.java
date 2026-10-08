package net.mads.industron.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Optional;

@Mixin(MerchantOffer.class)
public interface ProgressionOfferAccess {
    @Mutable @Accessor("baseCostA") void industron$costA(ItemCost cost);
    @Mutable @Accessor("costB") void industron$costB(Optional<ItemCost> cost);
    @Mutable @Accessor("result") void industron$result(ItemStack result);
}
