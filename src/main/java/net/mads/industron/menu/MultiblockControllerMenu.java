package net.mads.industron.menu;

import net.mads.industron.machine.machines.electric.multiblock.MultiblockControllerBlockEntity;
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

public class MultiblockControllerMenu extends AbstractContainerMenu {
    public static final int BUTTON_TOGGLE_MACHINE = 1000;

    private final MultiblockControllerBlockEntity blockEntity;
    private final ContainerData data;
    private final ContainerData machineData;
    private final int playerInventoryStart;

    public MultiblockControllerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(
                containerId,
                playerInventory,
                clientBlockEntity(playerInventory, buffer),
                new SimpleContainerData(CERecipeMenuData.COUNT),
                new SimpleContainerData(MultiblockControllerMenuData.COUNT)
        );
    }

    public MultiblockControllerMenu(
            int containerId,
            Inventory playerInventory,
            MultiblockControllerBlockEntity blockEntity
    ) {
        this(containerId, playerInventory, blockEntity, serverData(blockEntity), new MultiblockControllerMenuData(blockEntity));
    }

    private MultiblockControllerMenu(
            int containerId,
            Inventory playerInventory,
            MultiblockControllerBlockEntity blockEntity,
            ContainerData data,
            ContainerData machineData
    ) {
        super(MenuRegistry.MULTIBLOCK_CONTROLLER.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = data;
        this.machineData = machineData;
        addDataSlots(data);
        addDataSlots(machineData);
        this.playerInventoryStart = slots.size();
        addPlayerInventory(playerInventory);
    }

    private static MultiblockControllerBlockEntity clientBlockEntity(
            Inventory playerInventory,
            RegistryFriendlyByteBuf buffer
    ) {
        if (buffer == null) {
            return null;
        }

        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        return blockEntity instanceof MultiblockControllerBlockEntity controller ? controller : null;
    }

    private static ContainerData serverData(MultiblockControllerBlockEntity controller) {
        return new CERecipeMenuData(controller, controller::isFormed);
    }

    private void addPlayerInventory(Inventory playerInventory) {
        int x = 49;
        int y = 166;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, x + column * 18, y + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, x + column * 18, y + 58));
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (blockEntity == null || id != BUTTON_TOGGLE_MACHINE) {
            return false;
        }

        blockEntity.toggleMachineEnabled();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        if (index >= playerInventoryStart
                && !moveItemStackTo(original, playerInventoryStart, slots.size(), false)) {
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
    public boolean stillValid(Player player) {
        if (blockEntity == null) {
            return true;
        }

        BlockPos pos = blockEntity.getBlockPos();
        return !blockEntity.isRemoved()
                && player.level() == blockEntity.getLevel()
                && player.level().getBlockEntity(pos) == blockEntity
                && player.distanceToSqr(
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D
        ) <= 64.0D;
    }

    public MultiblockControllerBlockEntity blockEntity() {
        return blockEntity;
    }

    public boolean formed() {
        return data.get(CERecipeMenuData.FORMED) == 1;
    }

    public boolean processing() {
        return data.get(CERecipeMenuData.PROCESSING) == 1;
    }

    public int progress() {
        return data.get(CERecipeMenuData.PROGRESS);
    }

    public int duration() {
        return data.get(CERecipeMenuData.DURATION);
    }

    public boolean machineEnabled() {
        return data.get(CERecipeMenuData.ENABLED) == 1;
    }

    public int parallel() {
        return Math.max(1, data.get(CERecipeMenuData.PARALLEL));
    }

    public int resourcePerTick() {
        return data.get(CERecipeMenuData.RESOURCE_PER_TICK);
    }

    public boolean hasDurability() {
        return hasFlag(MultiblockControllerMenuData.FLAG_DURABILITY);
    }

    public long durabilityHundredths() {
        long low = Integer.toUnsignedLong(machineData.get(MultiblockControllerMenuData.DURABILITY_LOW));
        long high = Integer.toUnsignedLong(machineData.get(MultiblockControllerMenuData.DURABILITY_HIGH));
        return low | high << 32;
    }

    public int maxDurability() {
        return Math.max(0, machineData.get(MultiblockControllerMenuData.MAX_DURABILITY));
    }

    public int corrosionPerTickHundredths() {
        return Math.max(0, machineData.get(MultiblockControllerMenuData.CORROSION_PER_TICK));
    }

    public boolean hasSafeChemicalBalanceRange() {
        return hasFlag(MultiblockControllerMenuData.FLAG_SAFE_CB);
    }

    public boolean hasCbHatch() {
        return hasFlag(MultiblockControllerMenuData.FLAG_CB_HATCH);
    }

    public int machineChemicalBalanceHundredths() {
        return machineData.get(MultiblockControllerMenuData.MACHINE_CB);
    }

    public int safeChemicalBalanceMinimumHundredths() {
        return machineData.get(MultiblockControllerMenuData.SAFE_CB_MIN);
    }

    public int safeChemicalBalanceMaximumHundredths() {
        return machineData.get(MultiblockControllerMenuData.SAFE_CB_MAX);
    }

    private boolean hasFlag(int flag) {
        return (machineData.get(MultiblockControllerMenuData.FLAGS) & flag) != 0;
    }

}
