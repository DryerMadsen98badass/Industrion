package net.mads.industron.client;

import net.mads.industron.Industron;
import net.mads.industron.material.MaterialLookup;
import net.mads.industron.machine.foundry.casting.CastingDefinitions;
import net.mads.industron.machine.foundry.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = Industron.MOD_ID, value = Dist.CLIENT)
public final class CastingTooltip {
    private CastingTooltip() {}
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {
        var stack = event.getItemStack();
        if (stack.getItem() instanceof CastingMoldItem moldItem) {
            var form = moldItem.form();
            if (form != null) {
                event.getToolTip().add(Component.literal("Casts: " + form.cold().displayName() + " (" + form.millibuckets() + " mB)"));
            }
        } else {
            var target = MaterialLookup.find(stack);
            if (target != null) {
                var mold = CastingDefinitions.mold(target.part());
                if (mold != null) {
                    event.getToolTip().add(Component.literal("Casts: " + mold.cold().displayName() + " (" + mold.millibuckets() + " mB)"));
                }
            }
        }
        var ratio = stack.get(FoundryComponents.COMPOSITION.get());
        if (ratio != null) {
            event.getToolTip().add(Component.literal("Unidentified mixture - cannot be cast"));
            ratio.weights().forEach((id, weight) -> event.getToolTip().add(Component.literal(id
                    + String.format(java.util.Locale.ROOT, ": %.2f%%", ratio.share(id) * 100))));
        }
    }
}
