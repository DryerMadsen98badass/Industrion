package net.mads.industron.menu;

import net.mads.industron.machine.SingleBlockMachineBlockEntity;
import net.mads.industron.gui.MachineGuiLayout;
import net.mads.industron.registry.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.SlotItemHandler;

public class SingleBlockMachineMenu extends AbstractContainerMenu {
    public static final int BUTTON_INPUT_FLUID_SLOT = 1000;
    public static final int BUTTON_OUTPUT_FLUID_SLOT = 1100;
    public static final int BUTTON_CIRCUIT_DECREMENT = 1200;
    public static final int BUTTON_CIRCUIT_INCREMENT = 1201;
    public static final int BUTTON_CIRCUIT_RESET = 1202;
    public static final int BUTTON_TOGGLE_MACHINE = 1300;
    private static final int SLOT_STEP = 18;
    private final SingleBlockMachineBlockEntity blockEntity;
    private final MachineGuiLayout layout;
    private final ContainerData data;
    private final int itemInputSlots;
    private final int itemOutputSlots;
    private final int fluidInputSlots;
    private final int fluidOutputSlots;
    private final int imageWidth;
    private final int playerInventoryY;

    public SingleBlockMachineMenu(
            int containerId,
            Inventory playerInventory,
            RegistryFriendlyByteBuf data
    ) {
        this(containerId, playerInventory, clientBlockEntity(playerInventory, data));
    }

    public SingleBlockMachineMenu(
            int containerId,
            Inventory playerInventory,
            SingleBlockMachineBlockEntity blockEntity
    ) {
        super(MenuRegistry.SINGLE_BLOCK_MACHINE.get(), containerId);
        this.blockEntity = blockEntity;
        this.itemInputSlots = blockEntity == null ? 0 : blockEntity.itemInputSlotCount();
        this.itemOutputSlots = blockEntity == null ? 0 : blockEntity.itemOutputSlotCount();
        this.fluidInputSlots = blockEntity == null ? 0 : blockEntity.inputFluidSlotCount();
        this.fluidOutputSlots = blockEntity == null ? 0 : blockEntity.outputFluidSlotCount();
        this.layout = MachineGuiLayout.automatic(
                itemInputSlots,
                itemOutputSlots,
                fluidInputSlots,
                fluidOutputSlots,
                blockEntity == null ? null : blockEntity.progressBar()
        );
        this.imageWidth = layout.width();
        this.playerInventoryY = layout.playerInventoryY();
        this.data = blockEntity == null || playerInventory.player.level().isClientSide()
                ? new SimpleContainerData(CERecipeMenuData.COUNT)
                : new CERecipeMenuData(blockEntity, () -> true);
        addDataSlots(this.data);

        if (blockEntity != null) {
            addMachineItemSlots(blockEntity);
        }

        addPlayerInventory(playerInventory);
    }

    private static SingleBlockMachineBlockEntity clientBlockEntity(
            Inventory playerInventory,
            RegistryFriendlyByteBuf data
    ) {
        if (data == null) {
            return null;
        }

        BlockPos pos = data.readBlockPos();
        BlockEntity blockEntity =
                playerInventory.player.level().getBlockEntity(pos);
        return blockEntity instanceof SingleBlockMachineBlockEntity machine
                ? machine
                : null;
    }

    private void addPlayerInventory(
            Inventory playerInventory
    ) {
        int playerInventoryX = (imageWidth - 9 * SLOT_STEP) / 2;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(
                        playerInventory,
                        column + row * 9 + 9,
                        playerInventoryX + column * 18,
                        playerInventoryY + row * 18
                ));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(
                    playerInventory,
                    column,
                    playerInventoryX + column * 18,
                    playerInventoryY + 58
            ));
        }
    }

    @Override
    public ItemStack quickMoveStack(
            Player player,
            int index
    ) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        int machineSlots = itemInputSlots + itemOutputSlots;

        if (index < machineSlots) {
            if (!moveItemStackTo(original, machineSlots, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (itemInputSlots <= 0 || !moveItemStackTo(original, 0, itemInputSlots, false)) {
            return ItemStack.EMPTY;
        }

        if (original.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return copy;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (blockEntity == null) {
            return false;
        }

        if (id >= BUTTON_INPUT_FLUID_SLOT && id < BUTTON_INPUT_FLUID_SLOT + fluidInputSlots) {
            int slot = id - BUTTON_INPUT_FLUID_SLOT;
            return interactWithFluidSlot(player, blockEntity.inputFluidTanks(), slot, true, true);
        }
        if (id >= BUTTON_OUTPUT_FLUID_SLOT && id < BUTTON_OUTPUT_FLUID_SLOT + fluidOutputSlots) {
            int slot = id - BUTTON_OUTPUT_FLUID_SLOT;
            return interactWithFluidSlot(player, blockEntity.outputFluidTanks(), slot, false, true);
        }

        switch (id) {
            case BUTTON_CIRCUIT_DECREMENT -> blockEntity.adjustCircuit(-1);
            case BUTTON_CIRCUIT_INCREMENT -> blockEntity.adjustCircuit(1);
            case BUTTON_CIRCUIT_RESET -> blockEntity.resetCircuit();
            case BUTTON_TOGGLE_MACHINE -> blockEntity.toggleMachineEnabled();
            default -> {
                return false;
            }
        }
        return true;
    }

    private boolean interactWithFluidSlot(Player player, java.util.List<FluidTank> tanks, int slot, boolean allowFill, boolean allowDrain) {
        if (slot < 0 || slot >= tanks.size()) {
            return false;
        }
        return FluidSlotClickHandler.interact(this, player, tanks.get(slot), allowFill, allowDrain);
    }

    @Override
    public boolean stillValid(
            Player player
    ) {
        if (blockEntity == null) {
            return true;
        }

        BlockPos pos = blockEntity.getBlockPos();
        return player.level() == blockEntity.getLevel()
                && player.distanceToSqr(
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D
        ) <= 64.0D;
    }

    public SingleBlockMachineBlockEntity blockEntity() {
        return blockEntity;
    }

    public int progress() {
        return data.get(CERecipeMenuData.PROGRESS);
    }

    public int progressTotal() {
        return data.get(CERecipeMenuData.DURATION);
    }

    public boolean machineEnabled() {
        return data.get(CERecipeMenuData.ENABLED) == 1;
    }

    public int itemInputSlots() {
        return itemInputSlots;
    }

    public int itemOutputSlots() {
        return itemOutputSlots;
    }

    public int fluidInputSlots() {
        return fluidInputSlots;
    }

    public int fluidOutputSlots() {
        return fluidOutputSlots;
    }

    public int playerInventoryY() {
        return playerInventoryY;
    }

    public int menuImageHeight() {
        return layout.menuHeight();
    }

    public int menuImageWidth() {
        return imageWidth;
    }

    public int machineContentHeightForLayout() {
        return layout.contentHeight();
    }

    public int inputItemSlotX(int index) {
        return layout.inputItemX(index);
    }

    public int outputItemSlotX(int index) {
        return layout.outputItemX(index);
    }

    public int inputFluidSlotX(int index) {
        return layout.inputFluidX(index);
    }

    public int outputFluidSlotX(int index) {
        return layout.outputFluidX(index);
    }

    public int inputItemSlotY(int index) {
        return layout.inputItemY(index);
    }

    public int outputItemSlotY(int index) {
        return layout.outputItemY(index);
    }

    public int inputFluidSlotY(int index) {
        return layout.inputFluidY(index);
    }

    public int outputFluidSlotY(int index) {
        return layout.outputFluidY(index);
    }

    private void addMachineItemSlots(SingleBlockMachineBlockEntity blockEntity) {
        for (int i = 0; i < itemInputSlots; i++) {
            addSlot(new SlotItemHandler(blockEntity.inputItems(), i, inputItemSlotX(i), inputItemSlotY(i)));
        }
        for (int i = 0; i < itemOutputSlots; i++) {
            addSlot(new OutputSlot(blockEntity, i, outputItemSlotX(i), outputItemSlotY(i)));
        }
    }

    public MachineGuiLayout layout() {
        return layout;
    }

    private static final class OutputSlot extends SlotItemHandler {
        private OutputSlot(SingleBlockMachineBlockEntity blockEntity, int slot, int x, int y) {
            super(blockEntity.outputItems(), slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
