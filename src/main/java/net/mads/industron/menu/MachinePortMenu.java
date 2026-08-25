package net.mads.industron.menu;

import net.mads.industron.machine.MachinePortBlockEntity;
import net.mads.industron.registry.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.List;

public class MachinePortMenu extends AbstractContainerMenu {
    public static final int BUTTON_COLOR_DECREMENT = 0;
    public static final int BUTTON_COLOR_INCREMENT = 1;
    public static final int BUTTON_COLOR_RESET = 2;
    public static final int BUTTON_CIRCUIT_DECREMENT = 3;
    public static final int BUTTON_CIRCUIT_INCREMENT = 4;
    public static final int BUTTON_CIRCUIT_RESET = 5;
    public static final int BUTTON_AUTO_TOGGLE = 6;
    public static final int BUTTON_AUTO_RESET = 7;
    public static final int BUTTON_SYNC = 8;
    public static final int BUTTON_FLUID_SLOT = 1000;
    private static final int SLOT_SIZE = 18;
    private static final int CONTROL_COLUMN_WIDTH = 66;
    private static final int CONTENT_PADDING = 10;
    private static final int PLAYER_INVENTORY_WIDTH = 162;

    private final MachinePortBlockEntity blockEntity;
    private final int portSlotCount;
    private final int fluidSlotCount;
    private final int itemGridColumns;
    private final int itemGridRows;
    private final int imageWidth;
    private final int imageHeight;
    private final int itemGridX;
    private final int itemGridY;
    private final int fluidGridX;
    private final int fluidGridY;
    private final int playerInventoryX;
    private final int playerInventoryY;
    private final int playerInventoryStart;
    private final int contentX;
    private final int contentWidth;

    public MachinePortMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf data) {
        this(containerId, playerInventory, clientBlockEntity(playerInventory, data));
    }

    public MachinePortMenu(int containerId, Inventory playerInventory, MachinePortBlockEntity blockEntity) {
        super(MenuRegistry.MACHINE_PORT.get(), containerId);
        this.blockEntity = blockEntity;
        this.portSlotCount = blockEntity == null ? 0 : blockEntity.items().getSlots();
        this.fluidSlotCount = blockEntity == null ? 0 : blockEntity.fluidTanks().size();
        this.itemGridColumns = gridColumns(portSlotCount);
        this.itemGridRows = itemGridColumns == 0 ? 0 : (int) Math.ceil(portSlotCount / (double) itemGridColumns);
        this.contentWidth = computeContentWidth(itemGridColumns, fluidSlotCount);
        this.contentX = hasControls(blockEntity) ? CONTROL_COLUMN_WIDTH + CONTENT_PADDING : 8;
        this.imageWidth = contentX + contentWidth + 8;
        this.itemGridX = itemGridColumns == 0 ? 0 : contentX + (contentWidth - itemGridColumns * SLOT_SIZE) / 2;
        this.itemGridY = 38;
        this.fluidGridX = fluidSlotCount == 0 ? 0 : contentX + (contentWidth - fluidSlotCount * SLOT_SIZE) / 2;
        this.fluidGridY = itemGridY + itemGridRows * SLOT_SIZE + (portSlotCount > 0 ? 16 : 0);
        int contentBottom = contentBottom();
        this.playerInventoryX = contentX + (contentWidth - PLAYER_INVENTORY_WIDTH) / 2;
        this.playerInventoryY = contentBottom + 18;
        this.imageHeight = playerInventoryY + 82 + 8;

        addPortSlots();
        this.playerInventoryStart = slots.size();
        addPlayerInventory(playerInventory);
    }

    private static MachinePortBlockEntity clientBlockEntity(Inventory playerInventory, RegistryFriendlyByteBuf data) {
        if (data == null) {
            return null;
        }

        BlockPos pos = data.readBlockPos();
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        return blockEntity instanceof MachinePortBlockEntity port ? port : null;
    }

    private static int gridColumns(int slots) {
        if (slots <= 0) {
            return 0;
        }
        return Math.min(9, Math.max(1, (int) Math.ceil(Math.sqrt(slots))));
    }

    private static int computeContentWidth(int itemColumns, int fluidSlots) {
        int widestGrid = Math.max(itemColumns, fluidSlots) * SLOT_SIZE;
        return Math.max(176, Math.max(widestGrid + 16, PLAYER_INVENTORY_WIDTH + 14));
    }

    private static boolean hasControls(MachinePortBlockEntity blockEntity) {
        return blockEntity != null
                && (blockEntity.supportsIoColor() || blockEntity.supportsCircuit() || blockEntity.supportsAutoOutput());
    }

    private int contentBottom() {
        int bottom = 42;
        if (portSlotCount > 0) {
            bottom = Math.max(bottom, itemGridY + itemGridRows * SLOT_SIZE);
        }
        if (fluidSlotCount > 0) {
            bottom = Math.max(bottom, fluidGridY + SLOT_SIZE);
        }
        return bottom;
    }

    private void addPortSlots() {
        if (blockEntity == null || itemGridColumns == 0) {
            return;
        }

        for (int slot = 0; slot < portSlotCount; slot++) {
            int x = itemGridX + (slot % itemGridColumns) * SLOT_SIZE;
            int y = itemGridY + (slot / itemGridColumns) * SLOT_SIZE;
            addSlot(new SlotItemHandler(blockEntity.items(), slot, x, y));
        }
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9,
                        playerInventoryX + column * SLOT_SIZE,
                        playerInventoryY + row * SLOT_SIZE));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column,
                    playerInventoryX + column * SLOT_SIZE,
                    playerInventoryY + 58));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        if (index < playerInventoryStart) {
            if (!moveItemStackTo(original, playerInventoryStart, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(original, 0, playerInventoryStart, false)) {
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

        if (id >= BUTTON_FLUID_SLOT && id < BUTTON_FLUID_SLOT + fluidSlotCount) {
            int slot = id - BUTTON_FLUID_SLOT;
            List<FluidTank> tanks = blockEntity.fluidTanks();
            if (slot < 0 || slot >= tanks.size()) {
                return false;
            }
            return FluidSlotClickHandler.interact(
                    this,
                    player,
                    tanks.get(slot),
                    blockEntity.allowsGuiFluidFill(),
                    blockEntity.allowsGuiFluidDrain()
            );
        }

        switch (id) {
            case BUTTON_COLOR_DECREMENT -> blockEntity.adjustIoColor(-1);
            case BUTTON_COLOR_INCREMENT -> blockEntity.adjustIoColor(1);
            case BUTTON_COLOR_RESET -> blockEntity.resetIoColor();
            case BUTTON_CIRCUIT_DECREMENT -> blockEntity.adjustCircuit(-1);
            case BUTTON_CIRCUIT_INCREMENT -> blockEntity.adjustCircuit(1);
            case BUTTON_CIRCUIT_RESET -> blockEntity.resetCircuit();
            case BUTTON_AUTO_TOGGLE -> blockEntity.toggleAutoOutput();
            case BUTTON_AUTO_RESET -> blockEntity.setAutoOutput(false);
            case BUTTON_SYNC -> blockEntity.syncToClient();
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (blockEntity != null) {
            blockEntity.syncToClient();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null) {
            return true;
        }

        BlockPos pos = blockEntity.getBlockPos();
        return player.level() == blockEntity.getLevel()
                && player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }

    public MachinePortBlockEntity blockEntity() {
        return blockEntity;
    }

    public int menuImageWidth() {
        return imageWidth;
    }

    public int menuImageHeight() {
        return imageHeight;
    }

    public int portSlotCount() {
        return portSlotCount;
    }

    public int fluidSlotCount() {
        return fluidSlotCount;
    }

    public int fluidSlotX(int slot) {
        return fluidGridX + slot * SLOT_SIZE;
    }

    public int itemSlotY() {
        return itemGridY;
    }

    public int fluidSlotY() {
        return fluidGridY;
    }

    public int playerInventoryX() {
        return playerInventoryX;
    }

    public int playerInventoryY() {
        return playerInventoryY;
    }

    public int contentX() {
        return contentX;
    }

    public List<FluidTank> fluidTanks() {
        return blockEntity == null ? List.of() : blockEntity.fluidTanks();
    }

    public boolean supportsIoColor() {
        return blockEntity != null && blockEntity.supportsIoColor();
    }

    public boolean supportsCircuit() {
        return blockEntity != null && blockEntity.supportsCircuit();
    }

    public boolean supportsAutoOutput() {
        return blockEntity != null && blockEntity.supportsAutoOutput();
    }
}
