package net.mads.industron.menu;

import net.mads.industron.machine.foundry.FoundryBlockEntity;
import net.mads.industron.machine.foundry.FoundryItemStorage;
import net.mads.industron.machine.foundry.FoundryFluidStorage;
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
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.List;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/** Fixed-size window over controller storage; scrolling moves one horizontal row at a time. */
public final class FoundryMenu extends AbstractContainerMenu {
    public static final int COLUMNS = 9;
    public static final int VISIBLE_ROWS = 4;
    public static final int PAGE_SIZE = COLUMNS * VISIBLE_ROWS;
    public static final int GRID_X = 11;
    public static final int GRID_Y = 34;
    public static final int SCROLL_UP = 0;
    public static final int SCROLL_DOWN = 1;
    public static final int SCROLL_TRACK_BASE = 2;
    public static final int SCROLL_TRACK_STEPS = 50;
    public static final int FLUID_CLICK_BASE = 60;
    private static final int BASE_VALUES = 12;
    private static final int VALUE_COUNT = BASE_VALUES + PAGE_SIZE * 4;
    private final FoundryBlockEntity controller;
    private final BlockPos pos;
    private final Level level;
    private final ContainerData data;
    private int serverRow;

    public FoundryMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, null, buffer.readBlockPos());
    }

    public FoundryMenu(int id, Inventory inventory, FoundryBlockEntity controller) {
        this(id, inventory, controller, controller.getBlockPos());
    }

    private FoundryMenu(int id, Inventory inventory, FoundryBlockEntity controller, BlockPos pos) {
        super(MenuRegistry.FOUNDRY.get(), id);
        this.controller = controller;
        this.pos = pos;
        this.level = inventory.player.level();
        data = controller == null ? new SimpleContainerData(VALUE_COUNT * 2) : new ContainerData() {
            // Explicit 16-bit halves also work with vanilla short-valued container data packets.
            @Override public int get(int index) {
                int value = serverValue(index / 2);
                return (value >>> ((index & 1) * 16)) & 0xffff;
            }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return VALUE_COUNT * 2; }
        };
        addDataSlots(data);
        IItemHandlerModifiable page = controller == null ? new ItemStackHandler(PAGE_SIZE) : new PageHandler();
        for (int i = 0; i < PAGE_SIZE; i++) {
            final int visibleSlot = i;
            addSlot(new SlotItemHandler(page, i, GRID_X + (i % COLUMNS) * 18, GRID_Y + (i / COLUMNS) * 18) {
                @Override public boolean isActive() {
                    return firstVisibleSlot() + visibleSlot < capacity() || hasItem();
                }
                @Override public boolean mayPlace(ItemStack stack) {
                    return formed() && firstVisibleSlot() + visibleSlot < capacity()
                            && super.mayPlace(stack);
                }
                @Override public int getMaxStackSize() { return 1; }
                @Override public int getMaxStackSize(ItemStack stack) { return 1; }
                @Override public void setChanged() {
                    if (FoundryMenu.this.controller != null) {
                        // Vanilla shift-click mutates the reference returned by getItem().
                        int slot = serverRow * COLUMNS + visibleSlot;
                        FoundryItemStorage storage = FoundryMenu.this.controller.itemStorage();
                        storage.setStackInSlot(slot, storage.getStackInSlot(slot));
                    }
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, GRID_X + column * 18, 138 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, GRID_X + column * 18, 196));
    }

    private int serverValue(int index) {
        if (index >= BASE_VALUES) {
            int offset = index - BASE_VALUES;
            return controller.meltingDisplayValue(serverRow * COLUMNS + offset / 4, offset % 4);
        }
        return switch (index) {
            case 0 -> controller.isFormed() ? 1 : 0;
            case 1 -> (int) Math.round(controller.temperature() * 10.0);
            case 2 -> controller.insideWidth();
            case 3 -> controller.insideHeight();
            case 4 -> controller.itemStorage().capacity();
            case 5 -> controller.safeTemperature();
            case 6 -> controller.damagedBrickCount();
            case 7 -> controller.mostDamagedPercent();
            case 8 -> serverRow;
            case 9 -> Math.max(0, (int) (((long) controller.itemStorage().getSlots() + COLUMNS - 1) / COLUMNS) - VISIBLE_ROWS);
            case 10 -> controller.itemStorage().occupiedSlots();
            case 11 -> controller.receivingHeat() ? 1 : 0;
            default -> 0;
        };
    }

    private int value(int index) { return (data.get(index * 2) & 0xffff) | ((data.get(index * 2 + 1) & 0xffff) << 16); }
    public boolean formed() { return value(0) == 1; }
    public double temperature() { return value(1) / 10.0; }
    public int insideWidth() { return value(2); }
    public int insideHeight() { return value(3); }
    public int capacity() { return value(4); }
    public int safeTemperature() { return value(5); }
    public int damagedBricks() { return value(6); }
    public int mostDamagedPercent() { return value(7); }
    public int scrollRow() { return value(8); }
    public int maximumScrollRow() { return Math.max(0, value(9)); }
    public int firstVisibleSlot() { return scrollRow() * COLUMNS; }
    public int occupiedSlots() { return value(10); }
    public boolean receivingHeat() { return value(11) == 1; }
    public boolean overheating() { return formed() && temperature() > safeTemperature(); }
    public int meltTicks(int visibleSlot) { return value(BASE_VALUES + visibleSlot * 4); }
    public int meltDuration(int visibleSlot) { return value(BASE_VALUES + visibleSlot * 4 + 1); }
    public int meltTemperature(int visibleSlot) { return value(BASE_VALUES + visibleSlot * 4 + 2); }
    public int meltStatus(int visibleSlot) { return value(BASE_VALUES + visibleSlot * 4 + 3); }

    private FoundryFluidStorage tank() {
        FoundryBlockEntity owner = controller != null ? controller
                : level.getBlockEntity(pos) instanceof FoundryBlockEntity foundry && foundry.isController() ? foundry : null;
        return owner == null ? null : owner.fluidStorage();
    }
    public List<FluidStack> fluids() {
        FoundryFluidStorage tank = tank();
        return tank == null ? List.of() : tank.snapshot();
    }
    public int fluidCapacity() {
        FoundryFluidStorage tank = tank();
        return tank == null ? 0 : tank.capacity();
    }
    public int fluidAmount() {
        FoundryFluidStorage tank = tank();
        return tank == null ? 0 : tank.amount();
    }

    @Override public void broadcastChanges() {
        if (controller != null && serverRow > serverValue(9)) {
            serverRow = serverValue(9);
            broadcastFullState();
        }
        super.broadcastChanges();
    }

    @Override public boolean clickMenuButton(Player player, int id) {
        if (controller == null || !stillValid(player)) return false;
        if (id >= FLUID_CLICK_BASE && id <= FLUID_CLICK_BASE + FoundryFluidStorage.MAX_FLUID_TYPES) {
            boolean changed = FluidSlotClickHandler.interact(this, player,
                    controller.fluidStorage().interactionView(id - FLUID_CLICK_BASE), controller.isFormed(), true);
            if (changed) broadcastChanges();
            return changed;
        }
        if (!getCarried().isEmpty()) return false;
        int next;
        if (id == SCROLL_UP || id == SCROLL_DOWN) next = serverRow + (id == SCROLL_DOWN ? 1 : -1);
        else if (id >= SCROLL_TRACK_BASE && id <= SCROLL_TRACK_BASE + SCROLL_TRACK_STEPS) {
            next = (int) Math.round((double) (id - SCROLL_TRACK_BASE) * serverValue(9) / SCROLL_TRACK_STEPS);
        } else return false;
        next = Math.max(0, Math.min(serverValue(9), next));
        if (next == serverRow) return false;
        serverRow = next;
        broadcastChanges();
        broadcastFullState();
        return true;
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();
        // Vanilla's merge pass can bypass mayPlace for occupied slots. Bound the destination
        // explicitly, so shift-click cannot insert into unformed or retained overflow storage.
        int insertableSlots = Math.min(PAGE_SIZE, Math.max(0, capacity() - firstVisibleSlot()));
        if (index >= PAGE_SIZE && (!formed() || insertableSlots == 0)) return ItemStack.EMPTY;
        boolean moved = index < PAGE_SIZE
                ? moveItemStackTo(stack, PAGE_SIZE, slots.size(), true)
                : moveItemStackTo(stack, 0, insertableSlots, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return before;
    }

    @Override public boolean stillValid(Player player) {
        if (controller == null) return true;
        return !controller.isRemoved() && controller.isController()
                && player.level() == controller.getLevel()
                && player.level().getBlockEntity(pos) == controller
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    private final class PageHandler implements IItemHandlerModifiable {
        private int actual(int slot) { return serverRow * COLUMNS + slot; }
        private FoundryItemStorage storage() { return controller.itemStorage(); }
        @Override public int getSlots() { return PAGE_SIZE; }
        @Override public ItemStack getStackInSlot(int slot) { return storage().getStackInSlot(actual(slot)); }
        @Override public void setStackInSlot(int slot, ItemStack stack) { storage().setStackInSlot(actual(slot), stack); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return storage().insertItem(actual(slot), stack, simulate);
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return storage().extractItem(actual(slot), amount, simulate);
        }
        @Override public int getSlotLimit(int slot) { return storage().getSlotLimit(actual(slot)); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return storage().isItemValid(actual(slot), stack); }
    }
}
