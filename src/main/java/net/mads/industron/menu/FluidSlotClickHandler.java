package net.mads.industron.menu;

import net.mads.industron.fluid.FiredFluidBucketWrapper;
import net.mads.industron.registry.FluidRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper;

final class FluidSlotClickHandler {
    private FluidSlotClickHandler() {
    }

    static boolean interact(AbstractContainerMenu menu, Player player, FluidTank tank, boolean allowFill, boolean allowDrain) {
        ItemStack carried = menu.getCarried();
        if (!carried.isEmpty()) {
            return interactWithStack(
                    stack -> menu.setCarried(stack),
                    menu,
                    player,
                    carried,
                    tank,
                    allowFill,
                    allowDrain
            );
        }

        if (interactWithHand(menu, player, InteractionHand.MAIN_HAND, tank, allowFill, allowDrain)) {
            return true;
        }
        return interactWithHand(menu, player, InteractionHand.OFF_HAND, tank, allowFill, allowDrain);
    }

    private static boolean interactWithHand(
            AbstractContainerMenu menu,
            Player player,
            InteractionHand hand,
            FluidTank tank,
            boolean allowFill,
            boolean allowDrain
    ) {
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            return false;
        }

        return interactWithStack(
                stack -> player.setItemInHand(hand, stack),
                menu,
                player,
                held,
                tank,
                allowFill,
                allowDrain
        );
    }

    private static boolean interactWithStack(
            java.util.function.Consumer<ItemStack> replacementSetter,
            AbstractContainerMenu menu,
            Player player,
            ItemStack containerStack,
            FluidTank tank,
            boolean allowFill,
            boolean allowDrain
    ) {
        ItemStack singleContainer = containerStack.copyWithCount(1);
        IFluidHandlerItem handler = fluidHandler(singleContainer);
        if (handler == null) {
            return false;
        }

        if (allowFill && fillTankFromContainer(replacementSetter, player, containerStack, handler, tank)) {
            return true;
        }
        return allowDrain && fillContainerFromTank(replacementSetter, player, containerStack, handler, tank);
    }

    private static IFluidHandlerItem fluidHandler(ItemStack singleContainer) {
        IFluidHandlerItem handler = singleContainer.getCapability(Capabilities.FluidHandler.ITEM);
        if (handler != null) {
            return handler;
        }

        if (!(singleContainer.getItem() instanceof BucketItem)) {
            return null;
        }

        FluidRegistry.buildFiredBucketMaps();
        if (FluidRegistry.NORMAL_BUCKET_BY_FIRED_BUCKET.containsKey(singleContainer.getItem())) {
            return new FiredFluidBucketWrapper(singleContainer);
        }
        return new FluidBucketWrapper(singleContainer);
    }

    private static boolean fillTankFromContainer(
            java.util.function.Consumer<ItemStack> replacementSetter,
            Player player,
            ItemStack containerStack,
            IFluidHandlerItem handler,
            FluidTank tank
    ) {
        FluidStack available = handler.drain(Integer.MAX_VALUE, FluidAction.SIMULATE);
        if (available.isEmpty()) {
            return false;
        }

        int accepted = tank.fill(available, FluidAction.SIMULATE);
        if (accepted <= 0) {
            return false;
        }

        FluidStack drained = handler.drain(accepted, FluidAction.EXECUTE);
        if (drained.isEmpty()) {
            return false;
        }

        int filled = tank.fill(drained, FluidAction.EXECUTE);
        if (filled <= 0) {
            return false;
        }

        replaceOneContainer(replacementSetter, player, containerStack, handler.getContainer());
        return true;
    }

    private static boolean fillContainerFromTank(
            java.util.function.Consumer<ItemStack> replacementSetter,
            Player player,
            ItemStack containerStack,
            IFluidHandlerItem handler,
            FluidTank tank
    ) {
        FluidStack available = tank.drain(tank.getCapacity(), FluidAction.SIMULATE);
        if (available.isEmpty()) {
            return false;
        }

        int accepted = handler.fill(available, FluidAction.SIMULATE);
        if (accepted <= 0) {
            return false;
        }

        FluidStack drained = tank.drain(accepted, FluidAction.EXECUTE);
        if (drained.isEmpty()) {
            return false;
        }

        int filled = handler.fill(drained, FluidAction.EXECUTE);
        if (filled <= 0) {
            tank.fill(drained, FluidAction.EXECUTE);
            return false;
        }

        replaceOneContainer(replacementSetter, player, containerStack, handler.getContainer());
        return true;
    }

    private static void replaceOneContainer(
            java.util.function.Consumer<ItemStack> replacementSetter,
            Player player,
            ItemStack containerStack,
            ItemStack replacement
    ) {
        if (containerStack.getCount() == 1) {
            replacementSetter.accept(replacement);
            return;
        }

        containerStack.shrink(1);
        replacementSetter.accept(containerStack);
        if (!replacement.isEmpty() && !player.getInventory().add(replacement)) {
            player.drop(replacement, false);
        }
    }
}
