package net.mads.industron.integration.create.kinetic;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.item.ItemHelper;
import net.mads.industron.machine.MachineDrive;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.machine.interaction.*;
import net.mads.industron.machine.runtime.*;
import net.mads.industron.recipe.*;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import java.util.*;

/**
 * CE inventory/execution adapter only. Rotation, networks, stress, placement and rendering
 * belong to the machine's native Create block/entity. No Create ProcessingRecipe is used.
 */
public final class CEKineticProcessing implements CERecipeLogicHost {
    private long inputRevision;
    private final net.mads.industron.machine.runtime.AsyncRecipeSearch asyncSearch = new net.mads.industron.machine.runtime.AsyncRecipeSearch();

    private static final int TANK_CAPACITY = 16000;
    private final KineticBlockEntity owner;
    private final RecipeTypeDefinition type;
    private final int minimumRpm;
    private final ItemStackHandler inputs = inventory(9);
    private final ItemStackHandler outputs = inventory(16);
    private final FluidTank[] fluidInputs = tanks(4);
    private final FluidTank[] fluidOutputs = tanks(4);
    private final CERecipeLogic logic = new CERecipeLogic(this);
    private final InteractionWearStore wear = new InteractionWearStore();
    private final IItemHandler items = new Items();
    private final IFluidHandler fluids = new Fluids(false);
    private double workCredit;
    private float clientProgress;
    private int circuit;
    private boolean batching;
    private CERecipe activeRecipe;

    public CEKineticProcessing(KineticBlockEntity owner, RecipeTypeDefinition type, int minimumRpm) {
        this.owner = owner;
        this.type = type;
        this.minimumRpm = minimumRpm;
    }

    public CERecipeLogic logic() { return logic; }
    public RecipeTypeDefinition recipeType() { return type; }
    public int minimumRpm() { return minimumRpm; }
    public IItemHandler itemCapability(Direction side) { return items; }
    public IFluidHandler fluidCapability(Direction side) { return fluids; }
    public IFluidHandler manualFluidCapability(boolean recoverInputs) { return new Fluids(recoverInputs); }
    private Level level() { return owner.getLevel(); }
    private int rpm() { return Math.round(Math.abs(owner.getSpeed())); }

    public void tick() {
        long performanceStart = net.mads.industron.debug.CEPerformanceProfiler.begin(level());
        try {
        if (level() == null) return;
        if (level().isClientSide()) {
            if (logic.isActive() && rpmReady()) clientProgress = Math.min(logic.duration(), clientProgress + speedFactor());
            return;
        }
        if (!logic.isProcessing()) {
            logic.serverTick();
        } else if (!rpmReady()) {
            logic.serverTick(); // WAIT_FOR_RPM preserves the execution, input and progress.
            workCredit = 0;
        } else {
            workCredit += speedFactor();
            int steps = Math.min(16, (int) workCredit);
            workCredit -= steps;
            for (int i = 0; i < steps && logic.isProcessing(); i++) logic.serverTick();
        }
        if (logic.isProcessing() && level().getGameTime() % 10 == 0) owner.sendData();
        if (logic.isActive() && level().getGameTime() % 40 == 0) {
            level().playSound(null, owner.getBlockPos(), SoundEvents.GRINDSTONE_USE,
                    SoundSource.BLOCKS, 0.18F, 0.65F + Math.min(1.0F, rpm() / KineticProcessingRules.MAX_RPM));
        }
            } finally {
            net.mads.industron.debug.CEPerformanceProfiler.record(net.mads.industron.debug.CEPerformanceProfiler.Metric.SINGLEBLOCK_TICK, performanceStart);
        }
    }

    private float speedFactor() {
        return (float) KineticProcessingRules.speed(rpm());
    }

    private boolean rpmReady() {
        CERecipeExecution e = logic.execution();
        int min = e == null ? minimumRpm : Math.max(minimumRpm, e.machineData().getInt("MinRpm"));
        int max = e == null ? KineticProcessingRules.MAX_RPM : e.machineData().getInt("MaxRpm");
        return !owner.isOverStressed() && rpm() >= min && rpm() <= Math.min(KineticProcessingRules.MAX_RPM, max);
    }

    public float progress(float partialTick) {
        if (!logic.isProcessing() || logic.duration() == 0) return 0;
        float progress = level() != null && level().isClientSide() ? clientProgress : logic.progress();
        if (logic.isActive() && rpmReady()) progress += partialTick * speedFactor();
        return Math.min(1F, progress / logic.duration());
    }

    public ItemStack workpiece() {
        CERecipeExecution e = logic.execution();
        if (e != null && !e.itemInputs().isEmpty()) return e.itemInputs().getFirst();
        for (int i = 0; i < inputs.getSlots(); i++) if (!inputs.getStackInSlot(i).isEmpty()) return inputs.getStackInSlot(i);
        return ItemStack.EMPTY;
    }

    public boolean insertHeld(Player player, ItemStack held) {
        if (held.isEmpty()) return false;
        ItemStack remaining = held.copy();
        if (player.getAbilities().instabuild) remaining.setCount(1);
        int before = remaining.getCount();
        for (int i = 0; i < inputs.getSlots() && !remaining.isEmpty(); i++) remaining = inputs.insertItem(i, remaining, false);
        if (remaining.getCount() == before) return false;
        if (!player.getAbilities().instabuild) held.shrink(before - remaining.getCount());
        return true;
    }

    public boolean extractToPlayer(Player player, boolean recoverInputs) {
        if (recoverInputs && logic.isProcessing()) return false;
        ItemStackHandler inventory = recoverInputs ? inputs : outputs;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack extracted = inventory.extractItem(slot, 64, false);
            if (extracted.isEmpty()) continue;
            if (!player.addItem(extracted)) player.drop(extracted, false);
            return true;
        }
        return false;
    }

    public void cycleCircuit(Player player) {
        if (logic.isProcessing()) return;
        circuit = (circuit + 1) % 33;
        changed();
        player.displayClientMessage(Component.translatable("message.industron.kinetic_circuit", circuit), true);
    }

    private CERecipeInput recipeInput() {
        return new CERecipeInput(itemStacks(inputs), fluidStacks(fluidInputs),
                circuit == 0 ? Optional.empty() : Optional.of(circuit), Set.of(),
                Optional.of(KineticProcessingRules.TIER), Optional.of(KineticProcessingRules.TIER), Optional.empty(),
                MachineDrive.KINETIC, rpm(), 0);
    }

    private InteractionContext context() {
        return new InteractionContext() {
            public Level level() { return CEKineticProcessing.this.level(); }
            public BlockPos origin() { return owner.getBlockPos(); }
            public Direction facing() { return owner.getBlockState().getValue(HorizontalKineticBlock.HORIZONTAL_FACING); }
            public List<ItemStack> itemInputs() { return itemStacks(inputs); }
            public List<FluidStack> fluidInputs() { return fluidStacks(CEKineticProcessing.this.fluidInputs); }
            public InteractionWearStore wearStore() { return wear; }
        };
    }

    @Override public boolean recipeSearchPending() { return asyncSearch.pending(); }

    @Override public boolean recipeMachineReady() { return level() != null && !owner.isRemoved(); }

    @Override
    public Optional<CERecipeExecution> findAndConsumeRecipeInputs() {
        if (!rpmReady()) return Optional.empty();
        CERecipeInput input = recipeInput();
        for (var holder : asyncSearch.candidates(level().getRecipeManager(),
                CERecipeLookup.candidatesByTypes(level().getRecipeManager(), List.of(type.id()), input), input, inputRevision)) {
            CERecipe recipe = holder.value();
            if (recipe.requiredTier().isEmpty() || !KineticProcessingRules.supportsTier(recipe.requiredTier().get())) continue;
            if (!recipe.tools().isEmpty() || recipe.furnaceFuel() || recipe.treeSource().isPresent()
                    || recipe.chemicalBalanceRange().isPresent() || !recipe.matches(input, level())) continue;
            if (!InteractionRuntime.conditionsMatch(recipe.conditions(), context(), InteractionPhase.ON_START)
                    || !InteractionRuntime.interactionsMatch(recipe.blockInteractions(), context(), InteractionPhase.ON_START)) continue;
            List<ItemStack> possibleItems = recipe.itemOutputs().stream().map(o -> o.stack().copy()).toList();
            List<FluidStack> possibleFluids = new ArrayList<>(recipe.fluidOutputs());
            recipe.chancedFluidOutputs().forEach(o -> possibleFluids.add(o.stack().copy()));
            // Reserve space for every possible output before rolling; blocked outputs cannot reroll chances.
            if (!fits(possibleItems, possibleFluids)) continue;
            List<ItemStack> resultItems = new ArrayList<>();
            recipe.itemOutputs().forEach(o -> { if (roll(o.effectiveChance(Optional.of(KineticProcessingRules.TIER), recipe.requiredTier()))) resultItems.add(o.stack().copy()); });
            List<FluidStack> resultFluids = new ArrayList<>();
            recipe.fluidOutputs().forEach(o -> resultFluids.add(o.copy()));
            recipe.chancedFluidOutputs().forEach(o -> { if (roll(o.effectiveChance(Optional.of(KineticProcessingRules.TIER), recipe.requiredTier()))) resultFluids.add(o.stack().copy()); });
            ItemStack displayed = ItemStack.EMPTY;
            for (ItemStack stack : input.items()) {
                if (recipe.itemInputs().stream().anyMatch(i -> i.ingredient().test(stack))) { displayed = stack.copyWithCount(1); break; }
            }
            if (displayed.isEmpty()) displayed = workpiece().copy();
            if (!InteractionRuntime.applyInteractions(recipe.blockInteractions(), context(), InteractionPhase.ON_START)) continue;
            int duration = recipe.runtimeDuration(KineticProcessingRules.TIER, MachineDrive.NONE, 0, input);
            duration = KineticProcessingRules.referenceDuration(duration);
            duration = InteractionRuntime.adjustedDuration(duration, Optional.empty(),
                    InteractionRuntime.firstMatchingModifier(recipe.modifiers(), context()));
            batching = true;
            try {
                recipe.itemInputs().forEach(i -> consumeItems(i.ingredient(), i.count()));
                recipe.fluidInputs().forEach(i -> consumeFluids(i.ingredient(), i.amount()));
                recipe.chancedItemInputs().forEach(i -> {
                    int n = 0;
                    for (int k = 0; k < i.ingredient().count(); k++) if (roll(i.effectiveChance(Optional.of(KineticProcessingRules.TIER), recipe.requiredTier()))) n++;
                    consumeItems(i.ingredient().ingredient(), n);
                });
                recipe.chancedFluidInputs().forEach(i -> {
                    if (roll(i.effectiveChance(Optional.of(KineticProcessingRules.TIER), recipe.requiredTier()))) consumeFluids(i.ingredient().ingredient(), i.ingredient().amount());
                });
            } finally { batching = false; }
            // Non-consumable dies/catalysts stay in the input inventory and cannot be retrieved mid-process.
            activeRecipe = recipe;
            workCredit = 0;
            CompoundTag data = new CompoundTag();
            data.putInt("BaseRpm", recipe.baseRpm());
            data.putInt("MinRpm", recipe.minRpm().orElse(minimumRpm));
            data.putInt("MaxRpm", recipe.maxRpm().orElse(KineticProcessingRules.MAX_RPM));
            return Optional.of(new CERecipeExecution(holder.id(), recipe.recipeType(), duration, 0, 1,
                    displayed.isEmpty() ? List.of() : List.of(displayed), List.of(), resultItems, resultFluids, data));
        }
        return Optional.empty();
    }

    private CERecipe currentRecipe(CERecipeExecution execution) {
        if (activeRecipe == null) activeRecipe = CERecipeLookup.byId(level().getRecipeManager(), execution.recipeId()).map(h -> h.value()).orElse(null);
        return activeRecipe;
    }

    @Override public CERecipeTickResult consumeRecipeTick(CERecipeExecution execution) {
        if (!rpmReady()) return CERecipeTickResult.WAIT_FOR_RPM;
        CERecipe recipe = currentRecipe(execution);
        if (recipe == null) return CERecipeTickResult.PAUSE;
        ConditionFailure failure = InteractionRuntime.failedConditionBehavior(recipe.conditions(), context(), InteractionPhase.WHILE_PROCESSING);
        if (failure != null) return failure == ConditionFailure.CANCEL ? CERecipeTickResult.CANCEL
                : failure == ConditionFailure.RESET ? CERecipeTickResult.WAIT_FOR_RESOURCE : CERecipeTickResult.PAUSE;
        return InteractionRuntime.applyInteractions(recipe.blockInteractions(), context(), InteractionPhase.WHILE_PROCESSING)
                ? CERecipeTickResult.CONTINUE : CERecipeTickResult.PAUSE;
    }

    @Override public boolean canCompleteRecipe(CERecipeExecution execution) {
        CERecipe recipe = currentRecipe(execution);
        return recipe != null && fits(execution.itemOutputs(), execution.fluidOutputs())
                && InteractionRuntime.conditionsMatch(recipe.conditions(), context(), InteractionPhase.ON_COMPLETE)
                && InteractionRuntime.interactionsMatch(recipe.blockInteractions(), context(), InteractionPhase.ON_COMPLETE);
    }

    @Override public boolean completeRecipe(CERecipeExecution execution) {
        CERecipe recipe = currentRecipe(execution);
        if (recipe == null || !InteractionRuntime.applyInteractions(recipe.blockInteractions(), context(), InteractionPhase.ON_COMPLETE)) return false;
        batching = true;
        try {
            for (ItemStack stack : execution.itemOutputs()) insertAll(outputs, stack);
            for (FluidStack stack : execution.fluidOutputs()) fillAll(fluidOutputs, stack);
        } finally { batching = false; }
        activeRecipe = null;
        workCredit = 0;
        changed();
        return true;
    }

    @Override public boolean resetDurationWhenResourceMissing() { return true; }
    @Override public void onRecipeLogicChanged(boolean activeChanged) {
        owner.setChanged();
        if (!logic.isProcessing()) activeRecipe = null;
        if (activeChanged) owner.sendData();
    }

    private boolean roll(int chance) { return chance >= 10000 || level().random.nextInt(10000) < chance; }
    private void consumeItems(Ingredient ingredient, int count) {
        for (int i = 0; i < inputs.getSlots() && count > 0; i++) {
            if (!ingredient.test(inputs.getStackInSlot(i))) continue;
            count -= inputs.extractItem(i, count, false).getCount();
        }
    }
    private void consumeFluids(FluidIngredient ingredient, int amount) {
        for (FluidTank tank : fluidInputs) {
            if (amount <= 0) break;
            if (ingredient.test(tank.getFluid())) amount -= tank.drain(amount, IFluidHandler.FluidAction.EXECUTE).getAmount();
        }
    }
    private static ItemStack insertAll(ItemStackHandler inventory, ItemStack original) {
        ItemStack stack = original.copy();
        for (int i = 0; i < inventory.getSlots() && !stack.isEmpty(); i++) stack = inventory.insertItem(i, stack, false);
        return stack;
    }
    private static int fillAll(FluidTank[] tanks, FluidStack original) {
        FluidStack stack = original.copy();
        // Fill compatible tanks before claiming an empty tank.
        for (int pass = 0; pass < 2; pass++) for (FluidTank tank : tanks) {
            if (tank.isEmpty() != (pass == 1)) continue;
            stack.shrink(tank.fill(stack, IFluidHandler.FluidAction.EXECUTE));
            if (stack.isEmpty()) return 0;
        }
        return stack.getAmount();
    }
    private boolean fits(List<ItemStack> items, List<FluidStack> fluids) {
        ItemStackHandler simulated = new ItemStackHandler(outputs.getSlots());
        for (int i = 0; i < outputs.getSlots(); i++) simulated.setStackInSlot(i, outputs.getStackInSlot(i).copy());
        for (ItemStack stack : items) if (!insertAll(simulated, stack).isEmpty()) return false;
        FluidTank[] tanks = new FluidTank[fluidOutputs.length];
        for (int i = 0; i < tanks.length; i++) { tanks[i] = new FluidTank(TANK_CAPACITY); tanks[i].setFluid(fluidOutputs[i].getFluid().copy()); }
        for (FluidStack fluid : fluids) if (fillAll(tanks, fluid) > 0) return false;
        return true;
    }
    private static List<ItemStack> itemStacks(ItemStackHandler inv) {
        List<ItemStack> result = new ArrayList<>();
        for (int i = 0; i < inv.getSlots(); i++) result.add(inv.getStackInSlot(i));
        return result;
    }
    private static List<FluidStack> fluidStacks(FluidTank[] tanks) { return Arrays.stream(tanks).map(FluidTank::getFluid).toList(); }
    private ItemStackHandler inventory(int size) {
        return new ItemStackHandler(size) {
            @Override protected void onContentsChanged(int slot) { changed(); }
        };
    }
    private FluidTank[] tanks(int count) {
        FluidTank[] tanks = new FluidTank[count];
        for (int i = 0; i < count; i++) tanks[i] = new FluidTank(TANK_CAPACITY) {
            @Override protected void onContentsChanged() { changed(); }
        };
        return tanks;
    }
    private void changed() {
        inputRevision++;
        if (batching || owner == null) return;
        owner.setChanged();
        if (level() != null && !level().isClientSide()) {
            if (logic != null) logic.requestImmediateSearch();
            owner.sendData();
        }
    }
    public void write(CompoundTag parent, HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("Inputs", inputs.serializeNBT(registries));
        tag.put("Outputs", outputs.serializeNBT(registries));
        for (int i = 0; i < fluidInputs.length; i++) tag.put("FluidIn" + i, fluidInputs[i].writeToNBT(registries, new CompoundTag()));
        for (int i = 0; i < fluidOutputs.length; i++) tag.put("FluidOut" + i, fluidOutputs[i].writeToNBT(registries, new CompoundTag()));
        tag.putDouble("WorkCredit", workCredit);
        tag.putInt("Circuit", circuit);
        wear.save(tag);
        logic.save(tag, registries);
        parent.put("CEProcessing", tag);
    }
    public void read(CompoundTag parent, HolderLookup.Provider registries) {
        if (!parent.contains("CEProcessing")) return;
        CompoundTag tag = parent.getCompound("CEProcessing");
        batching = true;
        try {
            inputs.deserializeNBT(registries, tag.getCompound("Inputs"));
            outputs.deserializeNBT(registries, tag.getCompound("Outputs"));
            for (int i = 0; i < fluidInputs.length; i++) fluidInputs[i].readFromNBT(registries, tag.getCompound("FluidIn" + i));
            for (int i = 0; i < fluidOutputs.length; i++) fluidOutputs[i].readFromNBT(registries, tag.getCompound("FluidOut" + i));
            workCredit = Math.max(0, Math.min(16, tag.getDouble("WorkCredit")));
            circuit = Math.max(0, Math.min(32, tag.getInt("Circuit")));
            wear.load(tag);
            logic.load(tag, registries);
            clientProgress = logic.progress();
            activeRecipe = null;
        } finally { batching = false; }
    }
    public void destroy() {
        if (level() == null || level().isClientSide()) return;
        ItemHelper.dropContents(level(), owner.getBlockPos(), inputs);
        ItemHelper.dropContents(level(), owner.getBlockPos(), outputs);
    }
    public boolean goggles(List<Component> tooltip) {
        tooltip.add(Component.literal("Process: " + type.displayName()));
        tooltip.add(net.mads.industron.machine.MachineProcessingTooltip.tier(KineticProcessingRules.TIER));
        net.mads.industron.machine.MachineProcessingTooltip.processing(tooltip, net.mads.industron.integration.create.kinetic.KineticProcessingRules.PROFILE);
        CERecipeExecution execution = logic.execution();
        int min = execution == null ? minimumRpm : Math.max(minimumRpm, execution.machineData().getInt("MinRpm"));
        int max = execution == null ? KineticProcessingRules.MAX_RPM : Math.min(KineticProcessingRules.MAX_RPM, execution.machineData().getInt("MaxRpm"));
        tooltip.add(Component.translatable("tooltip.industron.kinetic_rpm", min, max));
        tooltip.add(Component.translatable("tooltip.industron.kinetic_progress", Math.round(progress(0) * 100)));
        if (!rpmReady()) tooltip.add(Component.translatable("tooltip.industron.kinetic_not_fast_enough"));
        tooltip.add(Component.translatable("tooltip.industron.kinetic_circuit", circuit));
        tooltip.add(Component.translatable("tooltip.industron.kinetic_controls"));
        for (FluidTank tank : fluidInputs) if (!tank.isEmpty()) tooltip.add(tank.getFluid().getHoverName().copy().append(" : " + tank.getFluidAmount() + " mB"));
        for (FluidTank tank : fluidOutputs) if (!tank.isEmpty()) tooltip.add(tank.getFluid().getHoverName().copy().append(" : " + tank.getFluidAmount() + " mB"));
        return true;
    }

    private final class Items extends CombinedInvWrapper {
        Items() { super(inputs, outputs); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot < inputs.getSlots() ? super.insertItem(slot, stack, simulate) : stack;
        }
        @Override public ItemStack extractItem(int slot, int count, boolean simulate) {
            return slot < inputs.getSlots() ? ItemStack.EMPTY : super.extractItem(slot, count, simulate);
        }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return slot < inputs.getSlots(); }
    }
    private final class Fluids implements IFluidHandler {
        private final boolean recoverInputs;
        Fluids(boolean recoverInputs) { this.recoverInputs = recoverInputs; }
        public int getTanks() { return fluidInputs.length + fluidOutputs.length; }
        private FluidTank tank(int i) { return i < fluidInputs.length ? fluidInputs[i] : fluidOutputs[i - fluidInputs.length]; }
        public FluidStack getFluidInTank(int i) { return tank(i).getFluid(); }
        public int getTankCapacity(int i) { return TANK_CAPACITY; }
        public boolean isFluidValid(int i, FluidStack stack) { return i < fluidInputs.length; }
        public int fill(FluidStack stack, FluidAction action) {
            if (stack.isEmpty()) return 0;
            FluidStack remaining = stack.copy();
            for (int pass = 0; pass < 2; pass++) for (FluidTank tank : fluidInputs) {
                if (tank.isEmpty() != (pass == 1)) continue;
                remaining.shrink(tank.fill(remaining, action));
                if (remaining.isEmpty()) return stack.getAmount();
            }
            return stack.getAmount() - remaining.getAmount();
        }
        public FluidStack drain(FluidStack requested, FluidAction action) {
            FluidStack result = FluidStack.EMPTY;
            FluidTank[] source = recoverInputs && !logic.isProcessing() ? fluidInputs : fluidOutputs;
            int left = requested.getAmount();
            for (FluidTank tank : source) {
                if (left <= 0) break;
                if (!FluidStack.isSameFluidSameComponents(requested, tank.getFluid())) continue;
                FluidStack got = tank.drain(left, action);
                if (result.isEmpty()) result = got; else result.grow(got.getAmount());
                left -= got.getAmount();
            }
            return result;
        }
        public FluidStack drain(int amount, FluidAction action) {
            FluidTank[] source = recoverInputs && !logic.isProcessing() ? fluidInputs : fluidOutputs;
            for (FluidTank tank : source) if (!tank.isEmpty()) return drain(tank.getFluid().copyWithAmount(amount), action);
            return FluidStack.EMPTY;
        }
    }
}
