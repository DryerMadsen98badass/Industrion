package net.mads.industron.progression;

import net.mads.industron.Industron;
import net.mads.industron.tool.EquipmentReplacement;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/** Convert actual mob equipment as well as the eventual loot, preserving native weapon classes. */
@EventBusSubscriber(modid=Industron.MOD_ID)
public final class ProgressionEvents {
    private ProgressionEvents() {}

    @SubscribeEvent
    public static void mobEquipment(EntityJoinLevelEvent event) {
        if(event.getLevel().isClientSide)return;
        boolean nether=ProgressionMaterials.isNether(event.getLevel());
        if(event.getEntity() instanceof ItemEntity item) {
            var original = item.getItem();
            var replacement = EquipmentReplacement.replace(ProgressionMaterials.replace(original, nether), nether);
            if (replacement != original) {
                item.setItem(replacement);
                // ItemEntity joins also cover direct drops that never passed through a loot table.
                // Split converted salvage to legal stacks before it can reach a player's inventory.
                int remaining = replacement.getCount() - replacement.getMaxStackSize();
                if (remaining > 0) {
                    item.setItem(replacement.copyWithCount(replacement.getMaxStackSize()));
                    while (remaining > 0) {
                        int count = Math.min(remaining, replacement.getMaxStackSize());
                        ItemEntity extra = new ItemEntity(event.getLevel(), item.getX(), item.getY(), item.getZ(), replacement.copyWithCount(count));
                        extra.setDeltaMovement(item.getDeltaMovement());
                        extra.setDefaultPickUpDelay();
                        if (item.getOwner() != null) extra.setThrower(item.getOwner());
                        event.getLevel().addFreshEntity(extra);
                        remaining -= count;
                    }
                }
            }
            return;
        }
        if(event.getEntity() instanceof ItemFrame frame) {
            var original = frame.getItem();
            var replacement = EquipmentReplacement.replace(ProgressionMaterials.replace(original, nether), nether);
            if (replacement != original) frame.setItem(replacement);
            return;
        }
        if(!(event.getEntity() instanceof LivingEntity mob) || mob instanceof Player)return;
        for(EquipmentSlot slot:EquipmentSlot.values()) {
            var stack=mob.getItemBySlot(slot);
            var replacement=EquipmentReplacement.replace(ProgressionMaterials.replace(stack,nether),nether);
            if(replacement!=stack)mob.setItemSlot(slot,replacement);
        }
    }
}
