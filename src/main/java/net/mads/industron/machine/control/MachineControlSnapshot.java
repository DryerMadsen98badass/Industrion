package net.mads.industron.machine.control;

import net.mads.industron.recipe.ChemicalBalanceRange;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * One immutable, lazily populated view of a machine for a single server tick.
 * Every schedule on the same machine shares the expensive inventory, fluid,
 * diagnostics and energy lookups through this snapshot.
 */
public final class MachineControlSnapshot implements MachineControlContext {
    private final Shared shared;
    private final int redstoneInput;

    private MachineControlSnapshot(Shared shared, int redstoneInput) {
        this.shared = shared;
        this.redstoneInput = Math.max(0, Math.min(15, redstoneInput));
    }

    public static Builder builder(long evaluationTick) {
        return new Builder(evaluationTick);
    }

    public MachineControlSnapshot withRedstoneInput(int redstoneInput) {
        int clamped = Math.max(0, Math.min(15, redstoneInput));
        return clamped == this.redstoneInput ? this : new MachineControlSnapshot(shared, clamped);
    }

    @Override
    public long evaluationTick() {
        return shared.evaluationTick;
    }

    @Override
    public long inputRevision() {
        return shared.inputRevision;
    }

    @Override
    public long worldRevision() {
        return shared.world.get().revision();
    }

    @Override
    public int redstoneInput() {
        return redstoneInput;
    }

    @Override public boolean machineRunning() { return shared.machineRunning; }
    @Override public boolean hasActiveRecipe() { return shared.hasActiveRecipe; }
    @Override public int recipeProgress() { return shared.recipeProgress; }
    @Override public int recipeDuration() { return shared.recipeDuration; }
    @Override public int recipeMinimumChemicalBalance() { return shared.recipeMinimumChemicalBalance; }
    @Override public int recipeMaximumChemicalBalance() { return shared.recipeMaximumChemicalBalance; }
    @Override public int machineChemicalBalance() { return shared.machineChemicalBalance; }
    @Override public int recipeMinimumRpm() { return shared.recipeMinimumRpm; }
    @Override public int recipeMaximumRpm() { return shared.recipeMaximumRpm; }
    @Override public int machineRpm() { return shared.machineRpm; }
    @Override public long energyStored() { return shared.energy.get().stored(); }
    @Override public long energyCapacity() { return shared.energy.get().capacity(); }
    @Override public int steamStored() { return shared.steam.get().stored(); }
    @Override public int steamCapacity() { return shared.steam.get().capacity(); }
    @Override public boolean missingEnergy() { return shared.diagnostics.get().missingEnergy(); }
    @Override public boolean outputBlocked() { return shared.diagnostics.get().outputBlocked(); }
    @Override public boolean missingInput() { return shared.diagnostics.get().missingInput(); }
    @Override public boolean multiblockFormed() { return shared.multiblockFormed; }
    @Override public boolean isRaining() { return shared.world.get().raining(); }
    @Override public boolean isThundering() { return shared.world.get().thundering(); }
    @Override public boolean hasDirectSky() { return shared.world.get().directSky(); }
    @Override public boolean isDay() { return shared.world.get().day(); }
    @Override public int temperature() { return shared.temperature; }

    @Override
    public int itemInputCount() {
        return shared.itemCount("");
    }

    @Override
    public int fluidInputAmount() {
        return shared.fluidAmount("");
    }

    @Override
    public int itemInputCount(String filter) {
        return shared.itemCount(filter);
    }

    @Override
    public int fluidInputAmount(String filter) {
        return shared.fluidAmount(filter);
    }

    @Override
    public boolean itemInputMatches(String itemId) {
        return shared.itemMatches(itemId, false);
    }

    @Override
    public boolean itemInputMatchesTag(String tagId) {
        return shared.itemMatches(tagId, true);
    }

    @Override
    public boolean fluidInputMatches(String fluidId) {
        return shared.fluidMatches(fluidId, false);
    }

    @Override
    public boolean fluidInputMatchesTag(String tagId) {
        return shared.fluidMatches(tagId, true);
    }

    public record Diagnostics(boolean missingInput, boolean missingEnergy, boolean outputBlocked) {
        public static final Diagnostics NONE = new Diagnostics(false, false, false);
    }

    public record Energy(long stored, long capacity) {
        public static final Energy NONE = new Energy(0L, 0L);
        public Energy { stored = Math.max(0L, stored); capacity = Math.max(0L, capacity); }
    }

    public record Steam(int stored, int capacity) {
        public static final Steam NONE = new Steam(0, 0);
        public Steam { stored = Math.max(0, stored); capacity = Math.max(0, capacity); }
    }

    public record World(boolean raining, boolean thundering, boolean directSky, boolean day) {
        public static final World NONE = new World(false, false, false, false);

        public long revision() {
            long revision = raining ? 1L : 0L;
            if (thundering) revision |= 1L << 1;
            if (directSky) revision |= 1L << 2;
            if (day) revision |= 1L << 3;
            return revision;
        }
    }

    public static final class Builder {
        private final long evaluationTick;
        private long inputRevision;
        private boolean machineRunning;
        private boolean hasActiveRecipe;
        private int recipeProgress;
        private int recipeDuration;
        private int recipeMinimumChemicalBalance = ChemicalBalanceRange.NEUTRAL_HUNDREDTHS;
        private int recipeMaximumChemicalBalance = ChemicalBalanceRange.NEUTRAL_HUNDREDTHS;
        private int machineChemicalBalance = ChemicalBalanceRange.NEUTRAL_HUNDREDTHS;
        private int recipeMinimumRpm;
        private int recipeMaximumRpm;
        private int machineRpm;
        private boolean multiblockFormed = true;
        private Supplier<World> world = () -> World.NONE;
        private int temperature;
        private Supplier<Energy> energy = () -> Energy.NONE;
        private Supplier<Steam> steam = () -> Steam.NONE;
        private Supplier<Diagnostics> diagnostics = () -> Diagnostics.NONE;
        private Supplier<List<ItemStack>> itemInputs = List::of;
        private Supplier<List<FluidStack>> fluidInputs = List::of;

        private Builder(long evaluationTick) {
            this.evaluationTick = evaluationTick;
        }

        public Builder inputRevision(long value) { inputRevision = value; return this; }
        public Builder machineRunning(boolean value) { machineRunning = value; return this; }
        public Builder hasActiveRecipe(boolean value) { hasActiveRecipe = value; return this; }
        public Builder recipeProgress(int value) { recipeProgress = Math.max(0, value); return this; }
        public Builder recipeDuration(int value) { recipeDuration = Math.max(0, value); return this; }
        public Builder chemicalBalance(int minimum, int maximum, int current) {
            recipeMinimumChemicalBalance = clampChemicalBalance(minimum);
            recipeMaximumChemicalBalance = clampChemicalBalance(maximum);
            machineChemicalBalance = clampChemicalBalance(current);
            return this;
        }
        public Builder rpm(int minimum, int maximum, int current) {
            recipeMinimumRpm = Math.max(0, minimum);
            recipeMaximumRpm = Math.max(0, maximum);
            machineRpm = Math.max(0, current);
            return this;
        }
        public Builder multiblockFormed(boolean value) { multiblockFormed = value; return this; }
        public Builder world(boolean raining, boolean thundering, boolean directSky, boolean day) {
            world = () -> new World(raining, thundering, directSky, day);
            return this;
        }
        public Builder world(Supplier<World> value) {
            if (value == null) {
                world = () -> World.NONE;
            } else {
                world = () -> {
                    World resolved = value.get();
                    return resolved == null ? World.NONE : resolved;
                };
            }
            return this;
        }
        public Builder temperature(int value) { temperature = Math.max(0, value); return this; }
        public Builder energy(LongSupplier stored, LongSupplier capacity) {
            LongSupplier safeStored = stored == null ? () -> 0L : stored;
            LongSupplier safeCapacity = capacity == null ? () -> 0L : capacity;
            energy = () -> new Energy(safeStored.getAsLong(), safeCapacity.getAsLong());
            return this;
        }
        public Builder energy(Supplier<Energy> value) {
            energy = value == null ? () -> Energy.NONE : value;
            return this;
        }
        public Builder steam(IntSupplier stored, IntSupplier capacity) {
            IntSupplier safeStored = stored == null ? () -> 0 : stored;
            IntSupplier safeCapacity = capacity == null ? () -> 0 : capacity;
            steam = () -> new Steam(safeStored.getAsInt(), safeCapacity.getAsInt());
            return this;
        }
        public Builder steam(Supplier<Steam> value) {
            steam = value == null ? () -> Steam.NONE : value;
            return this;
        }
        public Builder diagnostics(Supplier<Diagnostics> value) {
            diagnostics = value == null ? () -> Diagnostics.NONE : value;
            return this;
        }
        public Builder itemInputs(Supplier<List<ItemStack>> value) {
            itemInputs = value == null ? List::of : value;
            return this;
        }
        public Builder fluidInputs(Supplier<List<FluidStack>> value) {
            fluidInputs = value == null ? List::of : value;
            return this;
        }

        private static int clampChemicalBalance(int value) {
            return Math.max(ChemicalBalanceRange.MIN_HUNDREDTHS, Math.min(ChemicalBalanceRange.MAX_HUNDREDTHS, value));
        }

        public MachineControlSnapshot build() {
            return new MachineControlSnapshot(new Shared(this), 0);
        }
    }

    private static final class Shared {
        private final long evaluationTick;
        private final long inputRevision;
        private final boolean machineRunning;
        private final boolean hasActiveRecipe;
        private final int recipeProgress;
        private final int recipeDuration;
        private final int recipeMinimumChemicalBalance;
        private final int recipeMaximumChemicalBalance;
        private final int machineChemicalBalance;
        private final int recipeMinimumRpm;
        private final int recipeMaximumRpm;
        private final int machineRpm;
        private final boolean multiblockFormed;
        private final LazyValue<World> world;
        private final int temperature;
        private final LazyValue<Energy> energy;
        private final LazyValue<Steam> steam;
        private final LazyValue<Diagnostics> diagnostics;
        private final LazyValue<List<ItemStack>> itemInputs;
        private final LazyValue<List<FluidStack>> fluidInputs;
        private Map<String, Integer> itemCountCache;
        private Map<String, Integer> fluidAmountCache;
        private Map<String, Boolean> itemMatchCache;
        private Map<String, Boolean> fluidMatchCache;
        private Map<String, List<FilterToken>> filterCache;

        private Shared(Builder builder) {
            evaluationTick = builder.evaluationTick;
            inputRevision = builder.inputRevision;
            machineRunning = builder.machineRunning;
            hasActiveRecipe = builder.hasActiveRecipe;
            recipeProgress = builder.recipeProgress;
            recipeDuration = builder.recipeDuration;
            recipeMinimumChemicalBalance = builder.recipeMinimumChemicalBalance;
            recipeMaximumChemicalBalance = builder.recipeMaximumChemicalBalance;
            machineChemicalBalance = builder.machineChemicalBalance;
            recipeMinimumRpm = builder.recipeMinimumRpm;
            recipeMaximumRpm = builder.recipeMaximumRpm;
            machineRpm = builder.machineRpm;
            multiblockFormed = builder.multiblockFormed;
            world = new LazyValue<>(builder.world);
            temperature = builder.temperature;
            energy = new LazyValue<>(builder.energy);
            steam = new LazyValue<>(builder.steam);
            diagnostics = new LazyValue<>(builder.diagnostics);
            itemInputs = new LazyValue<>(() -> safeItems(builder.itemInputs.get()));
            fluidInputs = new LazyValue<>(() -> safeFluids(builder.fluidInputs.get()));
        }

        private int itemCount(String filter) {
            String normalized = normalize(filter);
            if (itemCountCache == null) itemCountCache = new HashMap<>();
            return itemCountCache.computeIfAbsent(normalized, key -> {
                long total = 0L;
                List<FilterToken> tokens = parsedFilters(key);
                for (ItemStack stack : itemInputs.get()) {
                    if (stack.isEmpty() || (!tokens.isEmpty() && !matchesItem(stack, tokens))) continue;
                    total += stack.getCount();
                    if (total >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
                }
                return (int) total;
            });
        }

        private int fluidAmount(String filter) {
            String normalized = normalize(filter);
            if (fluidAmountCache == null) fluidAmountCache = new HashMap<>();
            return fluidAmountCache.computeIfAbsent(normalized, key -> {
                long total = 0L;
                List<FilterToken> tokens = parsedFilters(key);
                for (FluidStack stack : fluidInputs.get()) {
                    if (stack.isEmpty() || (!tokens.isEmpty() && !matchesFluid(stack, tokens))) continue;
                    total += stack.getAmount();
                    if (total >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
                }
                return (int) total;
            });
        }

        private boolean itemMatches(String value, boolean tag) {
            String normalized = normalize(value);
            String key = (tag ? "#" : "") + normalized;
            if (itemMatchCache == null) itemMatchCache = new HashMap<>();
            return itemMatchCache.computeIfAbsent(key, ignored -> {
                ResourceLocation id = ResourceLocation.tryParse(normalized);
                if (id == null) return false;
                FilterToken token = new FilterToken(id, tag);
                for (ItemStack stack : itemInputs.get()) if (!stack.isEmpty() && matchesItem(stack, token)) return true;
                return false;
            });
        }

        private boolean fluidMatches(String value, boolean tag) {
            String normalized = normalize(value);
            String key = (tag ? "#" : "") + normalized;
            if (fluidMatchCache == null) fluidMatchCache = new HashMap<>();
            return fluidMatchCache.computeIfAbsent(key, ignored -> {
                ResourceLocation id = ResourceLocation.tryParse(normalized);
                if (id == null) return false;
                FilterToken token = new FilterToken(id, tag);
                for (FluidStack stack : fluidInputs.get()) if (!stack.isEmpty() && matchesFluid(stack, token)) return true;
                return false;
            });
        }

        private List<FilterToken> parsedFilters(String filter) {
            if (filterCache == null) filterCache = new HashMap<>();
            return filterCache.computeIfAbsent(filter, Shared::parseFilters);
        }

        private static List<FilterToken> parseFilters(String filter) {
            if (filter.isBlank()) return List.of();
            List<FilterToken> result = new ArrayList<>();
            for (String raw : filter.split("[,;\\n]+")) {
                String token = raw.trim();
                if (token.isEmpty()) continue;
                boolean tag = token.startsWith("#");
                ResourceLocation id = ResourceLocation.tryParse(tag ? token.substring(1) : token);
                if (id != null) result.add(new FilterToken(id, tag));
            }
            return List.copyOf(result);
        }

        private static boolean matchesItem(ItemStack stack, FilterToken token) {
            return token.tag
                    ? stack.is(TagKey.create(Registries.ITEM, token.id))
                    : BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(token.id);
        }

        private static boolean matchesItem(ItemStack stack, List<FilterToken> tokens) {
            for (FilterToken token : tokens) {
                if (token.tag && stack.is(TagKey.create(Registries.ITEM, token.id))) return true;
                if (!token.tag && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(token.id)) return true;
            }
            return false;
        }

        private static boolean matchesFluid(FluidStack stack, FilterToken token) {
            return token.tag
                    ? stack.getFluid().builtInRegistryHolder().is(TagKey.create(Registries.FLUID, token.id))
                    : BuiltInRegistries.FLUID.getKey(stack.getFluid()).equals(token.id);
        }

        private static boolean matchesFluid(FluidStack stack, List<FilterToken> tokens) {
            for (FilterToken token : tokens) {
                if (token.tag && stack.getFluid().builtInRegistryHolder().is(TagKey.create(Registries.FLUID, token.id))) return true;
                if (!token.tag && BuiltInRegistries.FLUID.getKey(stack.getFluid()).equals(token.id)) return true;
            }
            return false;
        }

        private static String normalize(String value) {
            return value == null ? "" : value.trim();
        }

        private static List<ItemStack> safeItems(List<ItemStack> values) {
            return values == null || values.isEmpty() ? List.of() : List.copyOf(values);
        }

        private static List<FluidStack> safeFluids(List<FluidStack> values) {
            return values == null || values.isEmpty() ? List.of() : List.copyOf(values);
        }
    }

    private record FilterToken(ResourceLocation id, boolean tag) { }

    private static final class LazyValue<T> {
        private Supplier<T> supplier;
        private T value;
        private boolean resolved;

        private LazyValue(Supplier<T> supplier) {
            this.supplier = supplier;
        }

        private T get() {
            if (!resolved) {
                value = supplier.get();
                supplier = null;
                resolved = true;
            }
            return value;
        }
    }

}
