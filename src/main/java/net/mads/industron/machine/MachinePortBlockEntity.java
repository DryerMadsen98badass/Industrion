package net.mads.industron.machine;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import net.mads.industron.energy.CEEnergyContainer;
import net.mads.industron.energy.CEEnergyNetwork;
import net.mads.industron.energy.CEEnergyStorage;
import net.mads.industron.fluid.IndustrialFluid;
import net.mads.industron.fluid.IndustrialFluidLookup;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockAbility;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockControllerBlockEntity;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockPart;
import net.mads.industron.machine.control.MachineControlSchedule;
import net.mads.industron.machine.control.MachineControlScheduleHost;
import net.mads.industron.machine.control.MachineControlTarget;
import net.mads.industron.machine.control.MachineControlVariableStore;
import net.mads.industron.menu.MachinePortMenu;
import net.mads.industron.recipe.CERecipe;
import net.mads.industron.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;

public class MachinePortBlockEntity extends GeneratingKineticBlockEntity implements MultiblockPart, MenuProvider, MachineControlScheduleHost {
    private static final int CB_HATCH_CAPACITY = 1000;
    private final MachineTier tier;
    private final boolean tieredPort;
    private final Set<MultiblockAbility> abilities;
    private final ItemStackHandler items;
    private final List<FluidTank> fluidTanks;
    private final IItemHandler itemCapability;
    private final IFluidHandler fluidCapability;
    private final long ceCapacity;
    private final CEEnergyStorage ceStorage;
    private final float kineticStressPerRpm;
    private final EnumMap<Direction, MachineControlSchedule> machineControlSchedules = new EnumMap<>(Direction.class);
    private final MachineControlVariableStore machineControlVariables = new MachineControlVariableStore();
    private int machineControlOutputSignature = -1;

    private BlockPos controllerPos;
    private ResourceLocation assembledOverlayModel;
    private DyeColor ioColor = DyeColor.GRAY;
    private int circuit;
    private boolean autoOutput;
    private long lastInputCEt;
    private long lastInputVoltage;
    private long lastInputLoadTick = -1;
    private long lastNetworkInputCEt;
    private long lastNetworkInputVoltage;
    private long lastNetworkInputTick = -1;
    private int generatedRpm;
    private float lastGeneratedSpeed = Float.NaN;
    private boolean clientSyncPending;

    public MachinePortBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.MACHINE_PORT.get(), pos, state);
        MachinePortBlock block = (MachinePortBlock) state.getBlock();
        this.tier = block.effectiveTier();
        this.tieredPort = block.hasTier();
        this.abilities = block.abilities();
        this.items = createItemHandler(itemSlots());
        this.fluidTanks = createFluidTanks();
        this.itemCapability = createItemCapability();
        this.fluidCapability = createFluidCapability();
        this.ceCapacity = energyCapacity();
        this.ceStorage = createEnergyStorage();
        this.kineticStressPerRpm = block.isKineticPort() ? MachineTierStats.kineticStressPerRpm(tier) : 0;
    }

    public ItemStackHandler items() {
        return items;
    }

    public List<FluidTank> fluidTanks() {
        return fluidTanks;
    }

    @Nullable
    public IItemHandler itemCapability() {
        return items.getSlots() > 0 ? itemCapability : null;
    }

    @Nullable
    public IFluidHandler fluidCapability() {
        return fluidTanks.isEmpty() ? null : fluidCapability;
    }

    public boolean supportsBucketInteraction(ItemStack stack) {
        return stack.getItem() instanceof BucketItem
                && !fluidTanks.isEmpty()
                && (abilities.contains(MultiblockAbility.FLUID_INPUT)
                || abilities.contains(MultiblockAbility.CB_INPUT)
                || abilities.contains(MultiblockAbility.FLUID_OUTPUT)
                || abilities.contains(MultiblockAbility.IO_INTERFACE));
    }

    public boolean allowsGuiFluidFill() {
        return abilities.contains(MultiblockAbility.FLUID_INPUT)
                || abilities.contains(MultiblockAbility.CB_INPUT)
                || abilities.contains(MultiblockAbility.IO_INTERFACE);
    }

    public boolean allowsGuiFluidDrain() {
        return abilities.contains(MultiblockAbility.FLUID_OUTPUT)
                || abilities.contains(MultiblockAbility.IO_INTERFACE);
    }

    public boolean interactWithBucket(
            Player player,
            InteractionHand hand,
            ItemStack stack
    ) {
        if (!supportsBucketInteraction(stack)) {
            return false;
        }

        boolean allowFill = abilities.contains(MultiblockAbility.FLUID_INPUT)
                || abilities.contains(MultiblockAbility.CB_INPUT)
                || abilities.contains(MultiblockAbility.IO_INTERFACE);

        boolean allowDrain = allowFill
                || abilities.contains(MultiblockAbility.FLUID_OUTPUT)
                || abilities.contains(MultiblockAbility.IO_INTERFACE);

        boolean interacted = FluidUtil.interactWithFluidHandler(
                player,
                hand,
                new PortFluidHandler(
                        fluidTanks,
                        allowFill,
                        allowDrain
                )
        );

        return interacted;
    }

    public long ceCapacity() {
        return ceCapacity;
    }

    @Nullable
    public CEEnergyContainer ceContainer() {
        return ceStorage;
    }

    public long ceStored() {
        return ceStorage == null ? 0 : ceStorage.stored();
    }

    public void recordEnergyInputLoad(long cePerTick, long voltage) {
        if (level == null || level.isClientSide() || cePerTick <= 0 || voltage <= 0) {
            return;
        }
        lastInputCEt = cePerTick;
        lastInputVoltage = voltage;
        lastInputLoadTick = level.getGameTime();
        stateChanged();
    }

    public void recordEnergyNetworkInput(long cePerTick, long voltage) {
        if (level == null || level.isClientSide() || cePerTick <= 0 || voltage <= 0) {
            return;
        }
        long now = level.getGameTime();
        if (lastNetworkInputTick == now) {
            lastNetworkInputCEt += cePerTick;
            lastNetworkInputVoltage = Math.max(lastNetworkInputVoltage, voltage);
        } else {
            lastNetworkInputCEt = cePerTick;
            lastNetworkInputVoltage = voltage;
            lastNetworkInputTick = now;
        }
        stateChanged();
    }

    public long lastInputCEt() {
        return isInputLoadVisible() ? lastInputCEt : 0;
    }

    public long lastInputVoltage() {
        return isInputLoadVisible() ? lastInputVoltage : 0;
    }

    public long recentNetworkInputVoltage() {
        return isNetworkInputVisible() ? lastNetworkInputVoltage : 0;
    }

    public long displayInputVoltage() {
        long voltage = recentNetworkInputVoltage();
        return voltage > 0 ? voltage : (ceStorage == null ? 0 : ceStorage.getInputVoltage());
    }

    public MachineTier tier() {
        return tier;
    }

    public float kineticStressPerRpm() {
        return kineticStressPerRpm;
    }

    public int kineticRpm() {
        return Math.round(Math.abs(getSpeed()));
    }

    public boolean hasKineticInputPower() {
        return abilities.contains(MultiblockAbility.KINETIC_INPUT)
                && Math.abs(getSpeed()) >= 1.0F
                && isSpeedRequirementFulfilled();
    }

    public int generatedRpm() {
        return generatedRpm;
    }

    public void setGeneratedRpm(int rpm) {
        int next = abilities.contains(MultiblockAbility.KINETIC_OUTPUT)
                ? Math.max(0, Math.min(CERecipe.DEFAULT_MAX_RPM, rpm))
                : 0;
        if (generatedRpm != next) {
            generatedRpm = next;
            stateChanged();
        }
        refreshGeneratedRotation();
    }

    @Override
    public float getGeneratedSpeed() {
        if (!abilities.contains(MultiblockAbility.KINETIC_OUTPUT)
                || generatedRpm <= 0) {
            return 0.0F;
        }

        Direction output = getBlockState().getValue(MachinePortBlock.FACING);
        return convertToDirection(generatedRpm, output);
    }

    @Nullable
    public ResourceLocation assembledOverlayModel() {
        return assembledOverlayModel;
    }

    public void setAssembledOverlayModel(@Nullable ResourceLocation assembledOverlayModel) {
        if (java.util.Objects.equals(this.assembledOverlayModel, assembledOverlayModel)) {
            return;
        }
        this.assembledOverlayModel = assembledOverlayModel;
        stateChanged();
    }

    private static ResourceLocation migrateLegacyOverlayModel(ResourceLocation overlay) {
        String legacyPrefix = "block/casings/casing/";
        String path = overlay.getPath();
        if (!path.startsWith(legacyPrefix)) {
            return overlay;
        }
        return ResourceLocation.fromNamespaceAndPath(overlay.getNamespace(), "block/" + path.substring(legacyPrefix.length()));
    }

    public DyeColor ioColor() {
        return ioColor;
    }

    public int ioColorId() {
        return ioColor.getId();
    }

    public void cycleIoColor() {
        adjustIoColor(1);
    }

    public void adjustIoColor(int amount) {
        setIoColor(DyeColor.byId(Math.floorMod(ioColor.getId() + amount, DyeColor.values().length)));
    }

    public void resetIoColor() {
        setIoColor(DyeColor.GRAY);
    }

    public void setIoColor(DyeColor ioColor) {
        if (!supportsIoColor()) {
            return;
        }
        DyeColor next = ioColor == null ? DyeColor.GRAY : ioColor;
        if (this.ioColor == next) return;
        this.ioColor = next;
        routingChanged();
    }

    public int circuit() {
        return circuit;
    }

    public void adjustCircuit(int amount) {
        if (amount == 0) {
            return;
        }
        int next = Math.floorMod(circuit + amount, 33);
        setCircuit(next);
    }

    public void resetCircuit() {
        setCircuit(0);
    }

    public void setCircuit(int circuit) {
        if (!supportsCircuit()) {
            return;
        }
        int next = Math.max(0, Math.min(32, circuit));
        if (this.circuit == next) return;
        this.circuit = next;
        routingChanged();
    }

    public void cycleCircuit() {
        adjustCircuit(1);
    }

    public boolean autoOutput() {
        return autoOutput;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide()) {
            return;
        }
        try {
            if (abilities.contains(MultiblockAbility.KINETIC_OUTPUT)
                    && generatedRpm != 0
                    && !hasActiveKineticOutputController()) {
                setGeneratedRpm(0);
            }
            refreshGeneratedRotation();
            refreshMachineControlRedstoneOutputs();
            tickAutoOutput();
            if (ceStorage != null && abilities.contains(MultiblockAbility.ENERGY_OUTPUT)) {
                CEEnergyNetwork.outputToAdjacentWires(level, worldPosition, ceStorage);
            }
        } finally {
            flushClientSync();
        }
    }

    private boolean hasActiveKineticOutputController() {
        if (level == null || controllerPos == null) {
            return false;
        }
        return level.getBlockEntity(controllerPos) instanceof MultiblockControllerBlockEntity controller
                && controller.isFormed()
                && controller.usesKineticOutput();
    }

    public void toggleAutoOutput() {
        setAutoOutput(!autoOutput);
    }

    public void setAutoOutput(boolean autoOutput) {
        if (!supportsAutoOutput()) {
            return;
        }
        if (this.autoOutput == autoOutput) return;
        this.autoOutput = autoOutput;
        stateChanged();
    }

    public boolean supportsIoColor() {
        return tieredPort
                && (abilities.contains(MultiblockAbility.ITEM_INPUT)
                || abilities.contains(MultiblockAbility.ITEM_OUTPUT)
                || abilities.contains(MultiblockAbility.FLUID_INPUT)
                || abilities.contains(MultiblockAbility.FLUID_OUTPUT)
                || abilities.contains(MultiblockAbility.IO_INTERFACE));
    }

    public boolean supportsCircuit() {
        return abilities.contains(MultiblockAbility.ITEM_INPUT)
                || abilities.contains(MultiblockAbility.FLUID_INPUT)
                || abilities.contains(MultiblockAbility.IO_INTERFACE);
    }

    public boolean supportsAutoOutput() {
        return abilities.contains(MultiblockAbility.ITEM_OUTPUT)
                || abilities.contains(MultiblockAbility.FLUID_OUTPUT);
    }

    public boolean canConnectColor(DyeColor other) {
        return colorsConnect(ioColor, other);
    }

    public static boolean colorsConnect(DyeColor first, DyeColor second) {
        return first == DyeColor.GRAY || second == DyeColor.GRAY || first == second;
    }

    public void tickAutoOutput() {
        if (level == null || level.isClientSide() || !autoOutput || !supportsAutoOutput()) {
            return;
        }

        Direction facing = getBlockState().getValue(MachinePortBlock.FACING);
        BlockPos targetPos = worldPosition.relative(facing);
        Direction targetSide = facing.getOpposite();

        if (abilities.contains(MultiblockAbility.ITEM_OUTPUT)) {
            exportItems(targetPos, targetSide);
        }
        if (abilities.contains(MultiblockAbility.FLUID_OUTPUT)) {
            exportFluids(targetPos, targetSide);
        }
    }

    @Override
    public Set<MultiblockAbility> abilities() {
        return abilities;
    }

    @Override
    @Nullable
    public MachineTier partTier() {
        return tieredPort ? tier : null;
    }

    @Override
    public void attachToMultiblock(BlockPos controllerPos) {
        this.controllerPos = controllerPos;
        setChanged();
    }

    @Override
    public void detachFromMultiblock() {
        setGeneratedRpm(0);
        this.controllerPos = null;
        setChanged();
    }

    @Override
    public BlockPos controllerPos() {
        return controllerPos;
    }

    @Nullable
    public MultiblockControllerBlockEntity controller() {
        if (level == null || controllerPos == null) {
            return null;
        }

        return level.getBlockEntity(controllerPos) instanceof MultiblockControllerBlockEntity controller
                ? controller
                : null;
    }

    @Override
    public EnumMap<Direction, MachineControlSchedule> machineControlSchedules() {
        return machineControlSchedules;
    }

    @Override
    public MachineControlVariableStore machineControlVariables() {
        return machineControlVariables;
    }

    @Override
    public boolean acceptsMachineControlSchedules() {
        return abilities.contains(MultiblockAbility.REDSTONE);
    }

    @Override
    public void machineControlSchedulesChanged() {
        refreshMachineControlRedstoneOutputs();
        MultiblockControllerBlockEntity controller = controller();
        if (controller != null) controller.applyMachineControlSchedulesNow();
        stateChanged();
    }

    public int machineControlSignal(Direction side) {
        return side == null ? 0 : machineControlRedstoneOutput(side);
    }

    private void refreshMachineControlRedstoneOutputs() {
        if (level == null || level.isClientSide()) return;
        int signature = 0;
        boolean variablesChanged = false;
        for (Direction side : Direction.values()) {
            signature |= (machineControlSignal(side) & 15) << (side.get3DDataValue() * 4);
            MachineControlSchedule schedule = machineControlSchedule(side);
            if (schedule != null) variablesChanged |= schedule.consumeRuntimeDirty();
        }
        if (variablesChanged) stateChanged();
        if (signature == machineControlOutputSignature) return;
        int previous = machineControlOutputSignature;
        machineControlOutputSignature = signature;
        for (Direction side : Direction.values()) {
            int shift = side.get3DDataValue() * 4;
            if (previous < 0 || ((previous >>> shift) & 15) != ((signature >>> shift) & 15)) {
                level.updateNeighborsAt(worldPosition.relative(side), getBlockState().getBlock());
            }
        }
        level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
    }

    public long storedCE() {
        return ceStorage == null ? 0L : ceStorage.stored();
    }

    public long capacityCE() {
        return ceStorage == null ? 0L : ceCapacity;
    }

    @Nullable
    @Override
    public MachineControlTarget machineControlTarget() {
        return controller();
    }

    @Override
    public float calculateStressApplied() {
        if (abilities.contains(MultiblockAbility.KINETIC_INPUT)) {
            this.lastStressApplied = kineticStressPerRpm;
            return kineticStressPerRpm;
        }

        return super.calculateStressApplied();
    }

    @Override
    public float calculateAddedStressCapacity() {
        if (abilities.contains(MultiblockAbility.KINETIC_OUTPUT)) {
            this.lastCapacityProvided = kineticStressPerRpm;
            return kineticStressPerRpm;
        }

        return super.calculateAddedStressCapacity();
    }

    private void refreshGeneratedRotation() {
        if (level == null || level.isClientSide()) {
            return;
        }

        float generatedSpeed = getGeneratedSpeed();
        if (Float.compare(lastGeneratedSpeed, generatedSpeed) == 0) {
            return;
        }

        lastGeneratedSpeed = generatedSpeed;
        updateGeneratedRotation();
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (items.getSlots() > 0) {
            tag.put("Items", items.serializeNBT(registries));
        }
        if (!fluidTanks.isEmpty()) {
            ListTag tanks = new ListTag();
            for (FluidTank tank : fluidTanks) {
                tanks.add(tank.writeToNBT(registries, new CompoundTag()));
            }
            tag.put("FluidTanks", tanks);
        }
        if (controllerPos != null) {
            tag.putInt("ControllerX", controllerPos.getX());
            tag.putInt("ControllerY", controllerPos.getY());
            tag.putInt("ControllerZ", controllerPos.getZ());
        }
        if (assembledOverlayModel != null) {
            tag.putString("AssembledOverlayModel", assembledOverlayModel.toString());
        }
        tag.putInt("IoColor", ioColor.getId());
        tag.putInt("Circuit", circuit);
        tag.putBoolean("AutoOutput", autoOutput);
        saveMachineControlSchedules(tag);
        if (ceStorage != null) {
            tag.putLong("CE", ceStorage.stored());
        }
        tag.putLong("LastInputCEt", lastInputCEt());
        tag.putLong("LastInputVoltage", lastInputVoltage());
        tag.putLong("LastNetworkInputCEt", isNetworkInputVisible() ? lastNetworkInputCEt : 0);
        tag.putLong("LastNetworkInputVoltage", isNetworkInputVisible() ? lastNetworkInputVoltage : 0);
        tag.putLong("LastNetworkInputVoltage", recentNetworkInputVoltage());
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains("Items") && items.getSlots() > 0) {
            items.deserializeNBT(registries, tag.getCompound("Items"));
        }
        if (tag.contains("FluidTanks")) {
            ListTag tanks = tag.getList("FluidTanks", Tag.TAG_COMPOUND);
            for (int i = 0; i < Math.min(tanks.size(), fluidTanks.size()); i++) {
                fluidTanks.get(i).readFromNBT(registries, tanks.getCompound(i));
            }
        }
        if (tag.contains("ControllerX")) {
            controllerPos = new BlockPos(tag.getInt("ControllerX"), tag.getInt("ControllerY"), tag.getInt("ControllerZ"));
        } else {
            controllerPos = null;
        }
        if (tag.contains("AssembledOverlayModel")) {
            assembledOverlayModel = ResourceLocation.parse(tag.getString("AssembledOverlayModel"));
        } else if (tag.contains("AssembledOverlayTexture")) {
            assembledOverlayModel = migrateLegacyOverlayModel(ResourceLocation.parse(tag.getString("AssembledOverlayTexture")));
        } else {
            assembledOverlayModel = null;
        }
        ioColor = tag.contains("IoColor") ? DyeColor.byId(tag.getInt("IoColor")) : DyeColor.GRAY;
        if (!supportsIoColor()) {
            ioColor = DyeColor.GRAY;
        }
        circuit = Math.max(0, Math.min(32, tag.getInt("Circuit")));
        autoOutput = tag.getBoolean("AutoOutput");
        loadMachineControlSchedules(tag);
        if (ceStorage != null) {
            ceStorage.setStored(tag.getLong("CE"));
        }
        lastInputCEt = Math.max(0L, tag.getLong("LastInputCEt"));
        lastInputVoltage = Math.max(0L, tag.getLong("LastInputVoltage"));
        if (lastInputCEt > 0 && lastInputVoltage > 0) {
            lastInputLoadTick = level == null ? 0 : level.getGameTime();
        }
        lastNetworkInputCEt = Math.max(0L, tag.getLong("LastNetworkInputCEt"));
        lastNetworkInputVoltage = Math.max(0L, tag.getLong("LastNetworkInputVoltage"));
        if (lastNetworkInputCEt > 0 && lastNetworkInputVoltage > 0) {
            lastNetworkInputTick = level == null ? 0 : level.getGameTime();
        }
    }

    private int itemSlots() {
        if (abilities.contains(MultiblockAbility.IO_INTERFACE)) {
            return MachineTierStats.ioInterfaceItemSlots(tier);
        }
        if (abilities.contains(MultiblockAbility.ITEM_INPUT) || abilities.contains(MultiblockAbility.ITEM_OUTPUT)) {
            return MachineTierStats.itemBusSlots(tier);
        }
        if (abilities.contains(MultiblockAbility.MUFFLER)) {
            return MachineTierStats.mufflerSlots(tier);
        }
        return 0;
    }

    private List<FluidTank> createFluidTanks() {
        List<FluidTank> tanks = new ArrayList<>();
        boolean cbInput = abilities.contains(MultiblockAbility.CB_INPUT);
        int capacity = cbInput ? CB_HATCH_CAPACITY : MachineTierStats.fluidTankCapacity(tier);
        int tankCount = 0;
        if (abilities.contains(MultiblockAbility.IO_INTERFACE)) {
            tankCount = MachineTierStats.ioInterfaceFluidTanks();
        } else if (abilities.contains(MultiblockAbility.FLUID_INPUT)
                || abilities.contains(MultiblockAbility.CB_INPUT)
                || abilities.contains(MultiblockAbility.FLUID_OUTPUT)) {
            tankCount = 1;
        }

        for (int i = 0; i < tankCount; i++) {
            tanks.add(new FluidTank(capacity) {
                @Override
                public boolean isFluidValid(FluidStack stack) {
                    if (!cbInput) {
                        return super.isFluidValid(stack);
                    }
                    IndustrialFluid fluid = IndustrialFluidLookup.find(stack);
                    return fluid != null && fluid.hasChemicalBalance();
                }

                @Override
                protected void onContentsChanged() {
                    portContentsChanged();
                }
            });
        }
        return tanks;
    }

    private long energyCapacity() {
        if (abilities.contains(MultiblockAbility.ENERGY_INPUT) || abilities.contains(MultiblockAbility.ENERGY_OUTPUT)) {
            return MachineTierStats.ceCapacity(tier);
        }
        return 0;
    }

    @Nullable
    private CEEnergyStorage createEnergyStorage() {
        if (ceCapacity <= 0) {
            return null;
        }
        return new CEEnergyStorage(
                tier,
                ceCapacity,
                () -> abilities.contains(MultiblockAbility.ENERGY_INPUT),
                () -> abilities.contains(MultiblockAbility.ENERGY_OUTPUT),
                ignored -> stateChanged(),
                ignored -> explodeEnergyBlock(),
                () -> level == null ? Long.MIN_VALUE : level.getGameTime()) {
            @Override
            public long getInputAmperage() {
                return abilities.contains(MultiblockAbility.ENERGY_INPUT) ? 2L : 0L;
            }

            @Override
            public long getOutputAmperage() {
                return abilities.contains(MultiblockAbility.ENERGY_OUTPUT) ? 2L : 0L;
            }
        };
    }

    private void explodeEnergyBlock() {
        if (level == null || level.isClientSide()) {
            return;
        }
        level.explode(null, worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D, 4.0F, Level.ExplosionInteraction.TNT);
    }

    private boolean isInputLoadVisible() {
        return level != null && lastInputLoadTick >= 0 && level.getGameTime() - lastInputLoadTick <= 40;
    }

    private boolean isNetworkInputVisible() {
        return level != null && lastNetworkInputTick >= 0 && level.getGameTime() - lastNetworkInputTick <= 40;
    }

    private ItemStackHandler createItemHandler(int slots) {
        return new ItemStackHandler(slots) {
            @Override
            protected void onContentsChanged(int slot) {
                portContentsChanged();
            }
        };
    }

    public void syncToClient() {
        stateChanged();
    }

    private void portContentsChanged() {
        markControllerInputsDirtyIfRelevant();
        stateChanged();
    }

    private void routingChanged() {
        markControllerInputsDirtyIfRelevant();
        stateChanged();
    }

    private void markControllerInputsDirtyIfRelevant() {
        if (level == null || level.isClientSide() || !providesMachineControlInputs()) return;
        MultiblockControllerBlockEntity controller = controller();
        if (controller != null) controller.markMachineControlInputsDirty();
    }

    private boolean providesMachineControlInputs() {
        return abilities.contains(MultiblockAbility.ITEM_INPUT)
                || abilities.contains(MultiblockAbility.FLUID_INPUT)
                || abilities.contains(MultiblockAbility.IO_INTERFACE);
    }

    private void stateChanged() {
        if (level != null && !level.isClientSide()) clientSyncPending = true;
        setChanged();
    }

    private void flushClientSync() {
        if (!clientSyncPending || level == null || level.isClientSide()) return;
        clientSyncPending = false;
        BlockState state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, 3);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachinePortMenu(containerId, playerInventory, this);
    }

    private IItemHandler createItemCapability() {
        boolean allowInsert = abilities.contains(MultiblockAbility.ITEM_INPUT) || abilities.contains(MultiblockAbility.IO_INTERFACE);
        boolean allowExtract = abilities.contains(MultiblockAbility.ITEM_OUTPUT) || abilities.contains(MultiblockAbility.IO_INTERFACE) || abilities.contains(MultiblockAbility.MUFFLER);
        return new PortItemHandler(items, allowInsert, allowExtract);
    }

    private IFluidHandler createFluidCapability() {
        boolean allowFill = abilities.contains(MultiblockAbility.FLUID_INPUT) || abilities.contains(MultiblockAbility.CB_INPUT) || abilities.contains(MultiblockAbility.IO_INTERFACE);
        boolean allowDrain = abilities.contains(MultiblockAbility.FLUID_OUTPUT) || abilities.contains(MultiblockAbility.IO_INTERFACE);
        return new PortFluidHandler(fluidTanks, allowFill, allowDrain);
    }

    private void exportItems(BlockPos targetPos, Direction targetSide) {
        IItemHandler target = level.getCapability(Capabilities.ItemHandler.BLOCK, targetPos, targetSide);
        if (target == null) {
            return;
        }

        for (int slot = 0; slot < items.getSlots(); slot++) {
            ItemStack extracted = items.extractItem(slot, 64, true);
            if (extracted.isEmpty()) {
                continue;
            }

            ItemStack remaining = extracted.copy();
            for (int targetSlot = 0; targetSlot < target.getSlots(); targetSlot++) {
                remaining = target.insertItem(targetSlot, remaining, false);
                if (remaining.isEmpty()) {
                    break;
                }
            }

            int moved = extracted.getCount() - remaining.getCount();
            if (moved > 0) {
                items.extractItem(slot, moved, false);
            }
        }
    }

    private void exportFluids(BlockPos targetPos, Direction targetSide) {
        IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK, targetPos, targetSide);
        if (target == null) {
            return;
        }

        for (FluidTank tank : fluidTanks) {
            FluidStack available = tank.drain(tank.getCapacity(), IFluidHandler.FluidAction.SIMULATE);
            if (available.isEmpty()) {
                continue;
            }

            int filled = target.fill(available, IFluidHandler.FluidAction.EXECUTE);
            if (filled > 0) {
                tank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    private record PortItemHandler(ItemStackHandler items, boolean allowInsert, boolean allowExtract) implements IItemHandler {
        @Override
        public int getSlots() {
            return items.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return items.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return allowInsert ? items.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return allowExtract ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return items.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return allowInsert && items.isItemValid(slot, stack);
        }
    }

    private record PortFluidHandler(List<FluidTank> tanks, boolean allowFill, boolean allowDrain) implements IFluidHandler {
        @Override
        public int getTanks() {
            return tanks.size();
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tanks.get(tank).getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return tanks.get(tank).getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return allowFill && tanks.get(tank).isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!allowFill || resource.isEmpty()) {
                return 0;
            }

            int filled = 0;
            FluidStack remaining = resource.copy();
            for (FluidTank tank : tanks) {
                if (remaining.isEmpty()) {
                    break;
                }

                int tankFilled = tank.fill(remaining, action);
                filled += tankFilled;
                remaining.shrink(tankFilled);
            }
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (!allowDrain || resource.isEmpty()) {
                return FluidStack.EMPTY;
            }

            FluidStack drained = FluidStack.EMPTY;
            int remaining = resource.getAmount();
            for (FluidTank tank : tanks) {
                if (remaining <= 0) {
                    break;
                }

                FluidStack request = resource.copyWithAmount(remaining);
                FluidStack tankDrained = tank.drain(request, action);
                if (tankDrained.isEmpty()) {
                    continue;
                }

                if (drained.isEmpty()) {
                    drained = tankDrained.copy();
                } else {
                    drained.grow(tankDrained.getAmount());
                }
                remaining -= tankDrained.getAmount();
            }
            return drained;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (!allowDrain || maxDrain <= 0) {
                return FluidStack.EMPTY;
            }

            FluidStack drained = FluidStack.EMPTY;
            int remaining = maxDrain;
            for (FluidTank tank : tanks) {
                if (remaining <= 0) {
                    break;
                }

                FluidStack simulated = tank.drain(remaining, FluidAction.SIMULATE);
                if (simulated.isEmpty()) {
                    continue;
                }

                if (!drained.isEmpty() && !FluidStack.isSameFluidSameComponents(drained, simulated)) {
                    continue;
                }

                FluidStack tankDrained = tank.drain(simulated.copyWithAmount(Math.min(remaining, simulated.getAmount())), action);
                if (drained.isEmpty()) {
                    drained = tankDrained.copy();
                } else {
                    drained.grow(tankDrained.getAmount());
                }

                remaining -= tankDrained.getAmount();
            }
            return drained;
        }
    }
}
