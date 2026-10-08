package net.mads.industron.machine.foundry;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/** Shared capacity and atomic, composition-preserving molten bath updates. */
public final class FoundryFluidStorage implements IFluidHandler {
    public static final int MB_PER_INSIDE_AREA_BLOCK = 144 * 9;
    public static final int MAX_FLUID_TYPES = 64;
    private final List<FluidStack> fluids = new ArrayList<>();
    private final Runnable changed;
    private final BooleanSupplier canFill;
    private final BooleanSupplier canDrain;
    private int capacity;
    private int amount;
    private java.util.function.DoubleSupplier temperature = () -> 20;

    public void temperatureSource(java.util.function.DoubleSupplier source) { temperature = source; }

    public FoundryFluidStorage(Runnable changed, BooleanSupplier canFill, BooleanSupplier canDrain) {
        this.changed = changed;
        this.canFill = canFill;
        this.canDrain = canDrain;
    }

    public void resize(int insideArea) {
        int next = (int) Math.min(Integer.MAX_VALUE, Math.max(0L, (long) insideArea) * MB_PER_INSIDE_AREA_BLOCK);
        if (capacity != next) {
            capacity = next;
            changed.run();
        }
    }

    public int capacity() { return capacity; }
    public int amount() { return amount; }

    /** Discards the complete molten bath without producing containers or world drops. */
    public void clear() {
        if (fluids.isEmpty() && amount == 0) return;
        fluids.clear();
        amount = 0;
        changed.run();
    }
    public List<FluidStack> snapshot() { return fluids.stream().map(FluidStack::copy).toList(); }
    @Override public int getTanks() { return Math.min(MAX_FLUID_TYPES, fluids.size() + 1); }
    @Override public FluidStack getFluidInTank(int tank) {
        if (tank < 0 || tank >= fluids.size()) return FluidStack.EMPTY;
        FluidStack result = fluids.get(tank).copy();
        if (FoundryMetallurgy.material(result) != null || FoundryMetallurgy.unidentified(result))
            result.set(FoundryComponents.TEMPERATURE.get(), (int) temperature.getAsDouble());
        return result;
    }
    @Override public int getTankCapacity(int tank) { return capacity; }
    @Override public boolean isFluidValid(int tank, FluidStack stack) {
        return tank >= 0 && tank < getTanks() && canFill.getAsBoolean() && !stack.isEmpty();
    }

    @Override public int fill(FluidStack resource, FluidAction action) {
        if (!canFill.getAsBoolean() || resource.isEmpty() || amount >= capacity) return 0;
        int accepted = Math.min(resource.getAmount(), capacity - amount);
        return addBatch(List.of(resource.copyWithAmount(accepted)), action) ? accepted : 0;
    }

    /** Simulate the entire output transaction before consuming any input. */
    public boolean addBatch(List<FluidStack> outputs, FluidAction action) {
        if (!canFill.getAsBoolean()) return false;
        long extra = 0;
        List<FluidStack> next = new ArrayList<>(snapshot());
        for (FluidStack output : outputs) {
            if (output.isEmpty()) return false;
            if (FoundryMetallurgy.unidentified(output) && FoundryMetallurgy.ratio(output) == null) return false;
            extra += output.getAmount();
            next.add(output.copy());
        }
        if (extra + amount > capacity) return false;
        try { next = FoundryMetallurgy.equilibrate(next, (int) temperature.getAsDouble()); }
        catch (IllegalArgumentException | ArithmeticException exception) { return false; }
        if (next.size() > MAX_FLUID_TYPES) return false;
        if (action.execute()) {
            fluids.clear();
            fluids.addAll(next);
            amount += (int) extra;
            changed.run();
        }
        return true;
    }

    public void equilibrate() {
        List<FluidStack> next;
        try { next = FoundryMetallurgy.equilibrate(snapshot(), (int) temperature.getAsDouble()); }
        catch (IllegalArgumentException | ArithmeticException exception) { return; }
        boolean same = next.size() == fluids.size();
        for (int i = 0; same && i < next.size(); i++) {
            same = FluidStack.isSameFluidSameComponents(next.get(i), fluids.get(i))
                    && next.get(i).getAmount() == fluids.get(i).getAmount();
        }
        if (!same) { fluids.clear(); fluids.addAll(next); changed.run(); }
    }

    @Override public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return FluidStack.EMPTY;
        return drainAt(indexOf(resource), resource.getAmount(), action);
    }

    @Override public FluidStack drain(int maxDrain, FluidAction action) {
        return drainAt(0, maxDrain, action);
    }

    private FluidStack drainAt(int index, int maxDrain, FluidAction action) {
        if (!canDrain.getAsBoolean() || index < 0 || index >= fluids.size() || maxDrain <= 0) return FluidStack.EMPTY;
        FluidStack present = fluids.get(index);
        if (temperature.getAsDouble() < FoundryMetallurgy.liquidus(present)) return FluidStack.EMPTY;
        int taken = Math.min(maxDrain, present.getAmount());
        FluidStack result = present.copyWithAmount(taken);
        if (FoundryMetallurgy.material(result) != null || FoundryMetallurgy.unidentified(result))
            result.set(FoundryComponents.TEMPERATURE.get(), (int) temperature.getAsDouble());
        if (action.execute()) {
            present.shrink(taken);
            if (present.isEmpty()) fluids.remove(index);
            amount -= taken;
            changed.run();
        }
        return result;
    }

    private int indexOf(FluidStack resource) {
        resource = resource.copy();
        resource.remove(FoundryComponents.TEMPERATURE.get());
        for (int i = 0; i < fluids.size(); i++) {
            FluidStack stored = fluids.get(i).copy();
            stored.remove(FoundryComponents.TEMPERATURE.get());
            if (FluidStack.isSameFluidSameComponents(stored, resource)) return i;
        }
        return -1;
    }

    /** Adapts a clicked layer to the project's existing bucket/container GUI interaction. */
    public FluidTank interactionView(int layer) {
        FluidStack selected = getFluidInTank(layer);
        return new FluidTank(capacity) {
            @Override public FluidStack getFluid() { return getFluidInTank(0); }
            @Override public int getFluidAmount() { return getFluid().getAmount(); }
            @Override public FluidStack getFluidInTank(int tank) {
                int index = indexOf(selected);
                return index < 0 ? FluidStack.EMPTY : FoundryFluidStorage.this.getFluidInTank(index);
            }
            @Override public int fill(FluidStack stack, FluidAction action) {
                return FoundryFluidStorage.this.fill(stack, action);
            }
            @Override public FluidStack drain(int maxDrain, FluidAction action) {
                return drainAt(indexOf(selected), maxDrain, action);
            }
            @Override public FluidStack drain(FluidStack stack, FluidAction action) {
                return indexOf(selected) >= 0 && indexOf(selected) == indexOf(stack)
                        ? FoundryFluidStorage.this.drain(stack, action) : FluidStack.EMPTY;
            }
        };
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Capacity", capacity);
        ListTag entries = new ListTag();
        for (FluidStack fluid : fluids) entries.add(fluid.saveOptional(registries));
        tag.put("Fluids", entries);
        return tag;
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        capacity = Math.max(0, tag.getInt("Capacity"));
        fluids.clear();
        amount = 0;
        ListTag entries = tag.getList("Fluids", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            FluidStack fluid = FluidStack.parseOptional(registries, entries.getCompound(i));
            if (!fluid.isEmpty()) {
                fluids.add(fluid);
                amount = Math.addExact(amount, fluid.getAmount());
            }
        }
    }
}
