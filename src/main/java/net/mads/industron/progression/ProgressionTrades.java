package net.mads.industron.progression;

import net.mads.industron.material.ExternalMaterialSuppression;
import net.mads.industron.tool.EquipmentReplacement;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.Optional;

/** Rewrites both costs and results, retaining uses, demand, discounts and component predicates. */
public final class ProgressionTrades {
    private ProgressionTrades() {}

    public static MerchantOffer replace(MerchantOffer offer) {
        if (offer == null) return null;
        String sold=BuiltInRegistries.ITEM.getKey(offer.getResult().getItem()).toString();
        if(sold.equals("minecraft:cooked_cod") || sold.equals("minecraft:cooked_salmon"))return null;
        if (hiddenTool(offer.getBaseCostA()) || hiddenTool(offer.getCostB())) return null;
        ItemCost first = replaceCost(offer.getItemCostA());
        Optional<ItemCost> second = offer.getItemCostB().map(ProgressionTrades::replaceCost);
        ItemStack result = ProgressionMaterials.replace(offer.getResult(), false);
        result = EquipmentReplacement.replace(result, false);
        String foodId=BuiltInRegistries.ITEM.getKey(result.getItem()).toString();
        if(foodId.equals("minecraft:bread"))result=new ItemStack(net.mads.industron.registry.ItemRegistry.SIMPLE_ITEMS.get("wheat_grain").get(),result.getCount()*3);
        if (first.count() > first.itemStack().getMaxStackSize()
                || second.filter(cost -> cost.count() > cost.itemStack().getMaxStackSize()).isPresent()
                || result.getCount() > result.getMaxStackSize()) return null;
        if (first == offer.getItemCostA() && second.equals(offer.getItemCostB()) && result == offer.getResult()) return offer;
        MerchantOffer replacement = offer.copy();
        var access = (net.mads.industron.mixin.ProgressionOfferAccess)(Object)replacement;
        access.industron$costA(first);
        access.industron$costB(second);
        access.industron$result(result);
        return replacement;
    }

    private static ItemCost replaceCost(ItemCost cost) {
        // Gold-buying villagers demand Nether currency; they cannot produce it before Nether.
        ItemStack source = cost.itemStack();
        ItemStack converted = ProgressionMaterials.replace(source, true);
        if (converted == source) return cost;
        return new ItemCost(converted.getItem().builtInRegistryHolder(), converted.getCount(), cost.components());
    }

    private static boolean hiddenTool(ItemStack stack) {
        return !stack.isEmpty() && ExternalMaterialSuppression.suppressedExternalTestToolIds()
                .contains(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }
}
