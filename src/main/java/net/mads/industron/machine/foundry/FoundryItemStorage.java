package net.mads.industron.machine.foundry;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import java.util.TreeMap;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Sparse, controller-owned storage. Shrinking capacity never deletes occupied overflow slots. */
public final class FoundryItemStorage implements IItemHandlerModifiable {
    private final TreeMap<Integer, ItemStack> stacks = new TreeMap<>();
    private final Runnable changed;
    private final BooleanSupplier canInsert;
    private int capacity;

    public FoundryItemStorage(Runnable changed, BooleanSupplier canInsert) {
        this.changed = changed;
        this.canInsert = canInsert;
    }

    public void capacity(int slots) { capacity = Math.max(0, slots); }
    public int capacity() { return capacity; }
    public int occupiedSlots() { return stacks.size(); }
    public java.util.List<Integer> activeSlots() { return java.util.List.copyOf(stacks.headMap(capacity).keySet()); }
    @Override public int getSlots() {
        return Math.max(capacity, stacks.isEmpty() ? 0 : stacks.lastKey() + 1);
    }
    @Override public ItemStack getStackInSlot(int slot) {
        return stacks.getOrDefault(slot, ItemStack.EMPTY);
    }
    @Override public int getSlotLimit(int slot) { return 1; }
    @Override public boolean isItemValid(int slot, ItemStack stack) {
        return slot >= 0 && slot < capacity && canInsert.getAsBoolean();
    }
    @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || !isItemValid(slot, stack)) return stack;
        ItemStack present = getStackInSlot(slot);
        if (!present.isEmpty() && !ItemStack.isSameItemSameComponents(present, stack)) return stack;
        int accepted = Math.min(stack.getCount(), Math.min(getSlotLimit(slot), stack.getMaxStackSize()) - present.getCount());
        if (accepted <= 0) return stack;
        if (!simulate) setStackInSlot(slot, stack.copyWithCount(present.getCount() + accepted));
        return accepted == stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - accepted);
    }
    @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0) return ItemStack.EMPTY;
        ItemStack present = getStackInSlot(slot);
        int taken = Math.min(amount, present.getCount());
        if (taken == 0) return ItemStack.EMPTY;
        ItemStack result = present.copyWithCount(taken);
        if (!simulate) setStackInSlot(slot, present.copyWithCount(present.getCount() - taken));
        return result;
    }
    @Override public void setStackInSlot(int slot, ItemStack stack) {
        if (slot < 0 || slot == Integer.MAX_VALUE) throw new IllegalArgumentException("Invalid Foundry slot " + slot);
        if (stack.isEmpty()) stacks.remove(slot);
        else stacks.put(slot, stack.copyWithCount(Math.min(stack.getCount(), getSlotLimit(slot))));
        changed.run();
    }
    public void dropAll(Consumer<ItemStack> drop) {
        stacks.values().forEach(drop);
        stacks.clear();
        changed.run();
    }
    public ListTag save(HolderLookup.Provider registries) {
        ListTag result = new ListTag();
        stacks.forEach((slot, stack) -> {
            if (!stack.isEmpty()) {
                CompoundTag entry = new CompoundTag();
                entry.putInt("Slot", slot);
                entry.put("Stack", stack.save(registries));
                result.add(entry);
            }
        });
        return result;
    }
    public void load(ListTag entries, HolderLookup.Provider registries) {
        stacks.clear();
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            int slot = entry.getInt("Slot");
            ItemStack stack = ItemStack.parseOptional(registries, entry.getCompound("Stack"));
            if (slot >= 0 && slot < Integer.MAX_VALUE - 27 && !stack.isEmpty()) stacks.put(slot, stack);
        }
    }
}
