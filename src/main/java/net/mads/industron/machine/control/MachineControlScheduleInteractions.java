package net.mads.industron.machine.control;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.mads.industron.Industron;
import net.mads.industron.registry.ItemRegistry;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = Industron.MOD_ID)
public final class MachineControlScheduleInteractions {
    private MachineControlScheduleInteractions() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void removeScheduleWithWrench(PlayerInteractEvent.LeftClickBlock event) {
        ItemStack heldItem = event.getItemStack();
        if (event.isCanceled()
                || event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START
                || heldItem.isEmpty()
                || !AllItems.WRENCH.isIn(heldItem)
                || !event.getEntity().mayBuild()) {
            return;
        }

        Direction side = event.getFace();
        BlockEntity blockEntity = event.getLevel().getBlockEntity(event.getPos());
        if (!(blockEntity instanceof MachineControlScheduleHost host)
                || side == null
                || !host.hasMachineControlSchedule(side)) {
            return;
        }

        event.setCanceled(true);
        if (event.getLevel().isClientSide()) {
            return;
        }

        MachineControlSchedule removed = host.removeMachineControlScheduleAndGet(side);
        if (removed == null) {
            return;
        }

        if (!event.getEntity().getAbilities().instabuild) {
            event.getEntity().getInventory().placeItemBackInInventory(
                    MachineControlScheduleItem.stackForSchedule(
                            ItemRegistry.MACHINE_CONTROL_SCHEDULE.get(),
                            removed
                    )
            );
        }
        IWrenchable.playRemoveSound(event.getLevel(), event.getPos());
    }
}
