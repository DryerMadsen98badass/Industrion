package net.mads.industron.machine.machines.kinetic.simple;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.item.ItemHelper;
import net.mads.industron.recipe.SingleItemKineticRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;

import java.util.Optional;

public abstract class AbstractSimpleKineticMachineBlockEntity extends KineticBlockEntity implements Clearable {
    protected final ItemStackHandler inputInv;
    private final ItemStackHandler outputInv;
    private final IItemHandler itemCapability;
    private int timer;
    private int activeDuration;
    private SingleItemKineticRecipe lastRecipe;

    protected AbstractSimpleKineticMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.inputInv = createInputInventory();
        this.outputInv = createInventory();
        this.itemCapability = new MachineInventoryHandler();
    }

    public IItemHandler itemCapability() {
        return itemCapability;
    }

    public boolean isSpinning() {
        return Math.abs(getSpeed()) > 0.001F;
    }

    public boolean hasProcessingInput() {
        return !inputInv.getStackInSlot(0).isEmpty();
    }

    public float processingProgress(float partialTick) {
        if (activeDuration <= 0 || timer <= 0) return 0;
        float speedStep = Math.max(1, Math.abs(getSpeed() / 16F));
        return 1F - Math.max(0, timer - partialTick * speedStep) / activeDuration;
    }

    public boolean insertHeldItem(Player player, ItemStack held) {
        if (held.isEmpty() || isSpinning()) {
            return false;
        }

        if (player.getAbilities().instabuild) {
            ItemStack single = held.copyWithCount(1);
            return insertIntoInputSlots(single).isEmpty();
        }

        ItemStack remainder = insertIntoInputSlots(held.copy());
        if (remainder.getCount() == held.getCount()) {
            return false;
        }
        held.setCount(remainder.getCount());
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) return;
        if (level.isClientSide()) {
            if (timer > 0 && getSpeed() != 0 && isSpeedRequirementFulfilled())
                timer = Math.max(0, timer - Math.max(1, (int) Math.abs(getSpeed() / 16F)));
            return;
        }
        if (getSpeed() == 0 || !isSpeedRequirementFulfilled()) {
            timer = 0;
            return;
        }

        ItemStack input = inputInv.getStackInSlot(0);
        int rpm = Math.round(Math.abs(getSpeed()));
        if (input.isEmpty()) {
            timer = 0;
            return;
        }

        if (lastRecipe == null || !lastRecipe.matchesItem(input) || !lastRecipe.canProcessAtRpm(rpm)) {
            Optional<? extends SingleItemKineticRecipe> found = findRecipe(input, rpm);
            if (found.isEmpty()) {
                timer = 0;
                return;
            }
            lastRecipe = found.get();
        }

        if (!canStartRecipe(lastRecipe) || !canFit(lastRecipe.result())) {
            return;
        }
        if (timer <= 0) {
            timer = lastRecipe.processingDuration();
            activeDuration = timer;
            sendData();
            return;
        }

        timer -= Math.max(1, (int) Math.abs(getSpeed() / 16F));
        if (timer <= 0) {
            consumeRecipeInputs(lastRecipe);
            outputInv.insertItem(0, lastRecipe.result().copy(), false);
            timer = 0;
            contentChanged();
        }
    }

    public boolean extractToPlayer(Player player) {
        if (isSpinning()) {
            return false;
        }
        if (extractSlotToPlayer(outputInv, 0, player)) {
            return true;
        }
        for (int slot = 0; slot < inputInv.getSlots(); slot++) {
            if (extractSlotToPlayer(inputInv, slot, player)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        tag.putInt("Timer", timer);
        tag.putInt("ActiveDuration", activeDuration);
        tag.put("InputInventory", inputInv.serializeNBT(registries));
        tag.put("OutputInventory", outputInv.serializeNBT(registries));
        super.write(tag, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        timer = tag.getInt("Timer");
        activeDuration = tag.getInt("ActiveDuration");
        if (tag.contains("InputInventory")) {
            inputInv.deserializeNBT(registries, tag.getCompound("InputInventory"));
        }
        if (tag.contains("OutputInventory")) {
            outputInv.deserializeNBT(registries, tag.getCompound("OutputInventory"));
        }
        super.read(tag, registries, clientPacket);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        invalidateCapabilities();
    }

    @Override
    public void destroy() {
        super.destroy();
        ItemHelper.dropContents(level, worldPosition, inputInv);
        ItemHelper.dropContents(level, worldPosition, outputInv);
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < inputInv.getSlots(); slot++) {
            inputInv.setStackInSlot(slot, ItemStack.EMPTY);
        }
        outputInv.setStackInSlot(0, ItemStack.EMPTY);
    }

    private ItemStack insertIntoInputSlots(ItemStack stack) {
        ItemStack remainder = stack;
        for (int slot = 0; slot < inputInv.getSlots(); slot++) {
            if (!canAcceptItemInSlot(slot, remainder)) {
                continue;
            }
            remainder = inputInv.insertItem(slot, remainder, false);
            if (remainder.isEmpty()) {
                break;
            }
        }
        return remainder;
    }

    private ItemStackHandler createInputInventory() {
        return new ItemStackHandler(inputSlotCount()) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return canAcceptItemInSlot(slot, stack);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (!isItemValid(slot, stack)) {
                    return stack;
                }
                return super.insertItem(slot, stack, simulate);
            }

            @Override
            protected void onContentsChanged(int slot) {
                contentChanged();
            }
        };
    }

    private ItemStackHandler createInventory() {
        return new ItemStackHandler(1) {
            @Override
            protected void onContentsChanged(int slot) {
                contentChanged();
            }
        };
    }

    private void contentChanged() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            sendData();
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    protected abstract Optional<? extends SingleItemKineticRecipe> findRecipe(ItemStack stack, int rpm);

    protected abstract boolean hasRecipeFor(ItemStack stack);

    protected int inputSlotCount() {
        return 1;
    }

    protected boolean canAcceptItemInSlot(int slot, ItemStack stack) {
        if (slot != 0) {
            return false;
        }
        if (level == null || stack.isEmpty()) {
            return false;
        }
        return hasRecipeFor(stack);
    }

    protected boolean canStartRecipe(SingleItemKineticRecipe recipe) {
        return true;
    }

    protected void consumeRecipeInputs(SingleItemKineticRecipe recipe) {
        inputInv.extractItem(0, recipe.inputCount(), false);
    }

    private boolean canFit(ItemStack result) {
        ItemStack current = outputInv.getStackInSlot(0);
        if (current.isEmpty()) {
            return result.getCount() <= outputInv.getSlotLimit(0);
        }
        return ItemStack.isSameItemSameComponents(current, result)
                && current.getCount() + result.getCount() <= Math.min(current.getMaxStackSize(), outputInv.getSlotLimit(0));
    }

    private boolean extractSlotToPlayer(ItemStackHandler inventory, int slot, Player player) {
        ItemStack extracted = inventory.extractItem(slot, 64, false);
        if (extracted.isEmpty()) {
            return false;
        }
        if (!player.addItem(extracted)) {
            player.drop(extracted, false);
        }
        return true;
    }

    private class MachineInventoryHandler extends CombinedInvWrapper {
        private MachineInventoryHandler() {
            super(inputInv, outputInv);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return inputInv == getHandlerFromIndex(getIndexForSlot(slot))
                    && slot < inputInv.getSlots()
                    && canAcceptItemInSlot(slot, stack)
                    && super.isItemValid(slot, stack);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!isItemValid(slot, stack)) {
                return stack;
            }
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (inputInv == getHandlerFromIndex(getIndexForSlot(slot))) {
                return ItemStack.EMPTY;
            }
            return super.extractItem(slot, amount, simulate);
        }
    }
}
