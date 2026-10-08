package net.mads.industron.item;

import com.simibubi.create.content.equipment.goggles.GogglesItem;
import net.mads.industron.registry.ItemRegistry;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Creative-only goggles: normal Create goggles plus Industron forging debug information. */
public final class CreativeGogglesItem extends GogglesItem {
    public CreativeGogglesItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static boolean isWearing(Player player) {
        if (player == null) return false;
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        return head.is(ItemRegistry.CREATIVE_GOGGLES.get());
    }
}
