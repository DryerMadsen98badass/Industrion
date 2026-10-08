package net.mads.industron.machine.foundry;

import net.mads.industron.machine.machines.electric.multiblock.MultiblockAbility;
import net.mads.industron.Industron;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.menu.FoundryMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import net.mads.industron.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Shared block entity for the Foundry controller and its bufferless abilities.
 *
 * <p>Only controllers own inventory and thermal state. Bus capabilities delegate without buffers.</p>
 */
public class FoundryBlockEntity extends BlockEntity implements MenuProvider {
    private static final int VALIDATION_INTERVAL_TICKS = 300;

    private boolean formed;
    private boolean dirty = true;
    private int validationCooldown;
    private BlockPos controllerPos;
    private ResourceLocation formedCasingModel;

    // Runtime data survives loss of formation. Item storage is allocated only for a controller.
    private FoundryItemStorage items;
    private FoundryFluidStorage fluids;
    private boolean fluidSyncPending;
    private int fluidSyncCooldown;
    private final IFluidHandler fluidInputProxy = new FluidProxy(true);
    private final IFluidHandler fluidOutputProxy = new FluidProxy(false);
    private double currentTemperature = FoundryThermalRules.AMBIENT_C;
    private int lastInsideVolume = 1;
    private int lastBrickCount = 1;
    private int minX;
    private int minZ;
    private int safeTemperature;
    private int damagedBrickCount;
    private int mostDamagedPercent;
    private int thermalTicks;
    private final FoundryMelting melting = new FoundryMelting();
    private boolean receivingHeat;
    private BlockPos heatSourcePos;
    private FoundryHeatSource cachedHeatSource;
    private long cachedHeatRevision;
    private final Map<BlockPos, BrickDamage> brickDamage = new HashMap<>();
    private final Map<Integer, Double> pendingExposure = new HashMap<>();
    private final Set<Integer> brickLimits = new HashSet<>();
    private final IItemHandler inputProxy = new IItemHandler() {
        private FoundryItemStorage storage() {
            FoundryBlockEntity controller = inputController();
            return controller == null ? null : controller.itemStorage();
        }
        @Override public int getSlots() {
            FoundryItemStorage storage = storage();
            return storage == null ? 0 : storage.capacity();
        }
        @Override public ItemStack getStackInSlot(int slot) {
            FoundryItemStorage storage = storage();
            return storage == null ? ItemStack.EMPTY : storage.getStackInSlot(slot).copy();
        }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            FoundryItemStorage storage = storage();
            return storage == null ? stack : storage.insertItem(slot, stack, simulate);
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        @Override public int getSlotLimit(int slot) {
            FoundryItemStorage storage = storage();
            return storage == null ? 0 : storage.getSlotLimit(slot);
        }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            FoundryItemStorage storage = storage();
            return storage != null && storage.isItemValid(slot, stack);
        }
    };

    // Controller-only structure data.
    private int outerSize;
    private int baseY;
    private int topY;
    private int insideWidth;
    private int insideHeight;
    private int insideArea;
    private int insideVolume;
    private String dominantMaterialId = "";
    private List<BlockPos> formedSpecialParts = List.of();
    private List<BlockPos> formedBrickPositions = List.of();
    private Map<MultiblockAbility, List<BlockPos>> abilityPositions = Map.of();

    @Override public void setRemoved() {
        net.mads.industron.runtime.StructureWatch.unregister(this);
        super.setRemoved();
    }

    public FoundryBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.FOUNDRY_PART.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FoundryBlockEntity foundry) {
        long performanceStart = net.mads.industron.debug.CEPerformanceProfiler.begin(level);
        try {
        if (level.isClientSide()
                || !(state.getBlock() instanceof FoundryPartBlock block)
                || !block.partType().isController()) {
            return;
        }

        if (foundry.validationCooldown > 0) {
            foundry.validationCooldown--;
        }
        if (foundry.dirty || foundry.validationCooldown <= 0) {
            foundry.validateStructure(state);
            foundry.validationCooldown = VALIDATION_INTERVAL_TICKS + (int) Math.floorMod(pos.asLong(), 61L);
        }
        foundry.tickThermalState();
        if (foundry.formed) foundry.melting.tick(foundry);
        if (foundry.fluidSyncCooldown > 0) foundry.fluidSyncCooldown--;
        if (foundry.fluidSyncPending && foundry.fluidSyncCooldown == 0) {
            foundry.fluidSyncPending = false;
            foundry.fluidSyncCooldown = 10;
            foundry.syncToClient();
        }
            } finally {
            net.mads.industron.debug.CEPerformanceProfiler.record(net.mads.industron.debug.CEPerformanceProfiler.Metric.FOUNDRY_TICK, performanceStart);
        }
    }

    public boolean isController() {
        return getBlockState().getBlock() instanceof FoundryPartBlock block && block.partType().isController();
    }

    public FoundryItemStorage itemStorage() {
        if (!isController()) throw new IllegalStateException("Foundry abilities do not own storage");
        if (items == null) items = new FoundryItemStorage(this::setChanged,
                () -> level != null && !level.isClientSide() && formed && !isRemoved());
        return items;
    }

    public FoundryFluidStorage fluidStorage() {
        if (!isController()) throw new IllegalStateException("Foundry abilities do not own fluid storage");
        if (fluids == null) fluids = new FoundryFluidStorage(
                () -> { setChanged(); fluidSyncPending = true; },
                () -> level != null && !level.isClientSide() && formed && !isRemoved(),
                () -> level != null && !level.isClientSide() && !isRemoved());
        fluids.temperatureSource(this::temperature);
        return fluids;
    }

    @Nullable
    public IFluidHandler fluidCapability() {
        if (!(getBlockState().getBlock() instanceof FoundryPartBlock block)) return null;
        return switch (block.partType()) {
            case FLUID_INPUT_HATCH -> fluidInputProxy;
            case FLUID_OUTPUT_HATCH -> fluidOutputProxy;
            default -> null;
        };
    }

    private final class FluidProxy implements IFluidHandler {
        private final boolean input;
        private FluidProxy(boolean input) { this.input = input; }
        @Nullable private FoundryFluidStorage storage() {
            if (level == null || level.isClientSide() || isRemoved() || !formed || controllerPos == null
                    || !level.hasChunkAt(controllerPos)) return null;
            MultiblockAbility ability = input ? MultiblockAbility.FLUID_INPUT : MultiblockAbility.FLUID_OUTPUT;
            if (level.getBlockEntity(controllerPos) instanceof FoundryBlockEntity controller
                    && controller.isController() && controller.formed && !controller.isRemoved()
                    && controller.abilityPositions(ability).contains(worldPosition)) return controller.fluidStorage();
            return null;
        }
        @Override public int getTanks() {
            FoundryFluidStorage storage = storage();
            return storage == null ? 0 : storage.getTanks();
        }
        @Override public FluidStack getFluidInTank(int tank) {
            FoundryFluidStorage storage = storage();
            return storage == null ? FluidStack.EMPTY : storage.getFluidInTank(tank);
        }
        @Override public int getTankCapacity(int tank) {
            FoundryFluidStorage storage = storage();
            return storage == null ? 0 : storage.capacity();
        }
        @Override public boolean isFluidValid(int tank, FluidStack stack) {
            FoundryFluidStorage storage = storage();
            return input && storage != null && storage.isFluidValid(tank, stack);
        }
        @Override public int fill(FluidStack stack, FluidAction action) {
            FoundryFluidStorage storage = storage();
            return !input || storage == null ? 0 : storage.fill(stack, action);
        }
        @Override public FluidStack drain(FluidStack stack, FluidAction action) {
            FoundryFluidStorage storage = storage();
            return input || storage == null || !drainHeatSafe(storage) ? FluidStack.EMPTY : storage.drain(stack, action);
        }
        @Override public FluidStack drain(int amount, FluidAction action) {
            FoundryFluidStorage storage = storage();
            return input || storage == null || !drainHeatSafe(storage) ? FluidStack.EMPTY : storage.drain(amount, action);
        }
        private boolean drainHeatSafe(FoundryFluidStorage storage) {
            return !(getBlockState().getBlock() instanceof FoundryDrainBlock drain)
                    || FoundryMetallurgy.temperature(storage.getFluidInTank(0)) <= drain.clay().properties().maxOperatingTemperature();
        }
    }

    @Nullable
    public IItemHandler itemCapability() {
        return getBlockState().getBlock() instanceof FoundryPartBlock block
                && block.partType() == FoundryPartType.ITEM_INPUT_BUS ? inputProxy : null;
    }

    @Nullable
    private FoundryBlockEntity inputController() {
        if (level == null || level.isClientSide() || isRemoved() || !formed || controllerPos == null
                || !level.hasChunkAt(controllerPos)) return null;
        if (level.getBlockEntity(controllerPos) instanceof FoundryBlockEntity controller
                && controller.isController() && controller.formed && !controller.isRemoved()
                && controller.abilityPositions(MultiblockAbility.ITEM_INPUT).contains(worldPosition)) return controller;
        return null;
    }

    public int meltingDisplayValue(int slot, int field) { return melting.displayValue(this, slot, field); }
    public double temperature() { return currentTemperature; }
    public int safeTemperature() { return safeTemperature; }
    public int damagedBrickCount() { return damagedBrickCount; }
    public int mostDamagedPercent() { return mostDamagedPercent; }
    public boolean receivingHeat() { return receivingHeat; }
    public int insideDepth() { return insideWidth; }

    @Override public Component getDisplayName() { return Component.translatableWithFallback("gui.industron.foundry.title", "Foundry"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return isController() ? new FoundryMenu(id, inventory, this) : null;
    }

    /** Drops stored items but deliberately discards the molten bath when the controller is removed. */
    public void dropStoredItemsAndDiscardFluids() {
        if (level == null || level.isClientSide() || !isController()) return;
        if (items != null) {
            items.dropAll(stack -> Block.popResource(level, worldPosition, stack));
        }
        if (fluids != null) {
            fluids.clear();
        }
    }

    private void findHeatSource() {
        heatSourcePos = null;
        cachedHeatSource = null;
        if (level == null || !formed) return;
        // Heater is two layers high and must cover the exact footprint directly below the base.
        for (int y = baseY - 2; y < baseY; y++) {
            for (int x = minX; x < minX + outerSize; x++) {
                for (int z = minZ; z < minZ + outerSize; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof FoundryHeatSource source
                            && source.matchesFoundryFootprint(minX, minZ, baseY, outerSize)) {
                        heatSourcePos = pos;
                        cachedHeatSource = source;
                        cachedHeatRevision = source.foundryHeatRevision();
                        return;
                    }
                }
            }
        }
    }

    private void refreshBricks() {
        if (level == null) return;
        Set<BlockPos> positions = new HashSet<>(formedBrickPositions);
        boolean removed = brickDamage.keySet().removeIf(pos -> !positions.contains(pos));
        brickLimits.clear();
        safeTemperature = Integer.MAX_VALUE;
        for (BlockPos pos : formedBrickPositions) {
            FoundryBrickResolver.BrickInfo brick = FoundryBrickResolver.resolve(level.getBlockState(pos)).orElse(null);
            if (brick == null) continue;
            BrickDamage previous = brickDamage.get(pos);
            if (previous != null && !previous.blockId().equals(brick.blockId())) {
                brickDamage.remove(pos);
                removed = true;
            }
            int limit = brick.maxOperatingTemperature();
            brickLimits.add(limit);
            safeTemperature = Math.min(safeTemperature, limit);
        }
        if (safeTemperature == Integer.MAX_VALUE) safeTemperature = 0;
        lastInsideVolume = insideVolume;
        lastBrickCount = formedBrickPositions.size();
        updateDamageSummary();
        if (removed) setChanged();
    }

    private void tickThermalState() {
        if (level == null || level.isClientSide()) return;
        double heat = 0.0;
        double maximum = FoundryThermalRules.AMBIENT_C;
        if (formed && heatSourcePos != null && level.hasChunkAt(heatSourcePos)
                && level.getBlockEntity(heatSourcePos) instanceof FoundryHeatSource source) {
            boolean linked = true;
            if (source != cachedHeatSource || source.foundryHeatRevision() != cachedHeatRevision) {
                linked = source.matchesFoundryFootprint(minX, minZ, baseY, outerSize);
                cachedHeatSource = source;
                cachedHeatRevision = source.foundryHeatRevision();
                if (!linked) heatSourcePos = null;
            }
            if (linked) {
                heat = source.availableFoundryHeatPerTick();
                maximum = source.maximumFoundryTemperature();
            }
            if (!Double.isFinite(heat) || !Double.isFinite(maximum)) {
                heat = 0.0;
                maximum = FoundryThermalRules.AMBIENT_C;
            }
        }
        receivingHeat = heat > 0.0 && currentTemperature < maximum;
        double next = FoundryThermalRules.nextTemperature(currentTemperature, heat, maximum,
                lastInsideVolume, lastBrickCount);
        if (next != currentTemperature) {
            currentTemperature = next;
            setChanged();
        }
        if (formed) {
            // Integrate exact exposure per material limit each tick; walk bricks only in batches.
            for (int limit : brickLimits) {
                double damage = FoundryThermalRules.brickDamagePerTick(currentTemperature, limit);
                if (damage > 0.0) pendingExposure.merge(limit, damage, Double::sum);
            }
        }
        if (++thermalTicks >= FoundryThermalRules.DAMAGE_INTERVAL_TICKS) {
            thermalTicks = 0;
            if (formed) applyBrickDamage();
        }
    }

    private void applyBrickDamage() {
        if (level == null || !formed) return;
        if (pendingExposure.isEmpty() && brickDamage.values().stream()
                .noneMatch(damage -> damage.amount() >= FoundryThermalRules.DAMAGE_THRESHOLD)) return;
        boolean changed = false;
        BlockPos destroyedPos = null;
        FoundryBrickResolver.BrickInfo destroyedBrick = null;
        for (BlockPos pos : formedBrickPositions) {
            if (!level.hasChunkAt(pos)) continue;
            BlockState state = level.getBlockState(pos);
            FoundryBrickResolver.BrickInfo brick = FoundryBrickResolver.resolve(state).orElse(null);
            if (brick == null) {
                changed |= brickDamage.remove(pos) != null;
                continue;
            }
            BrickDamage previous = brickDamage.get(pos);
            double damage = previous != null && previous.blockId().equals(brick.blockId()) ? previous.amount() : 0.0;
            damage += pendingExposure.getOrDefault(brick.maxOperatingTemperature(), 0.0);
            if (damage <= 0.0) continue;
            changed = true;
            brickDamage.put(pos.immutable(), new BrickDamage(brick.blockId(), damage));
            if (destroyedPos == null && damage >= FoundryThermalRules.DAMAGE_THRESHOLD) {
                destroyedPos = pos;
                destroyedBrick = brick;
            }
        }
        pendingExposure.clear();
        // Finish accounting for every brick before the first hole invalidates the structure.
        if (destroyedPos != null && destroyedBrick != null) {
            ResourceLocation itemId = destroyedBrick.material().hasExistingPart(MaterialPart.CRACKED_BRICK)
                    ? destroyedBrick.material().existingPart(MaterialPart.CRACKED_BRICK)
                    : ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID,
                            MaterialPart.CRACKED_BRICK.registryName(destroyedBrick.material()));
            var item = BuiltInRegistries.ITEM.get(itemId);
            if (level.removeBlock(destroyedPos, false)) {
                brickDamage.remove(destroyedPos);
                if (item != Items.AIR) Block.popResource(level, destroyedPos, new ItemStack(item));
                else Industron.LOGGER.warn("Thermally destroyed Foundry brick {} at {} has no cracked item {}",
                        destroyedBrick.blockId(), destroyedPos, itemId);
                updateDamageSummary();
                clearFormation();
                markStructureDirty();
                setChanged();
                return;
            }
        }
        updateDamageSummary();
        if (changed) setChanged();
    }

    private void updateDamageSummary() {
        damagedBrickCount = 0;
        mostDamagedPercent = 0;
        for (BlockPos pos : formedBrickPositions) {
            BrickDamage damage = brickDamage.get(pos);
            if (damage != null && damage.amount() > 0.0) {
                damagedBrickCount++;
                mostDamagedPercent = Math.max(mostDamagedPercent,
                        Math.min(100, (int) Math.ceil(damage.amount() * 100.0 / FoundryThermalRules.DAMAGE_THRESHOLD)));
            }
        }
    }

    private record BrickDamage(ResourceLocation blockId, double amount) {}

    public boolean isFormed() {
        return formed;
    }

    @Nullable
    public BlockPos controllerPos() {
        return controllerPos;
    }

    @Nullable
    public ResourceLocation formedCasingModel() {
        return formedCasingModel;
    }

    public int outerSize() {
        return outerSize;
    }

    public int insideWidth() {
        return insideWidth;
    }

    public int insideHeight() {
        return insideHeight;
    }

    public int insideArea() {
        return insideArea;
    }

    public int insideVolume() {
        return insideVolume;
    }

    public String dominantMaterialId() {
        return dominantMaterialId;
    }

    public List<BlockPos> formedBrickPositions() {
        return formedBrickPositions;
    }

    public List<BlockPos> abilityPositions(MultiblockAbility ability) {
        return abilityPositions.getOrDefault(ability, List.of());
    }

    public void markStructureDirty() {
        dirty = true;
        validationCooldown = 0;
        setChanged();
    }

    /** Called on either a controller or an attached ability block. */
    public void markControllerDirty() {
        if (level == null || level.isClientSide()) {
            return;
        }
        if (getBlockState().getBlock() instanceof FoundryPartBlock block && block.partType().isController()) {
            markStructureDirty();
            return;
        }
        if (controllerPos != null && level.getBlockEntity(controllerPos) instanceof FoundryBlockEntity controller) {
            controller.markStructureDirty();
        }
    }

    public void disassemble() {
        if (level == null || level.isClientSide()) {
            return;
        }
        clearFormation();
    }

    private void validateStructure(BlockState state) {
        long performanceStart = net.mads.industron.debug.CEPerformanceProfiler.begin(level);
        try {
        // Charge the old structure before changing its membership or material limits.
        applyBrickDamage();
        dirty = false;
        if (level == null
                || !(state.getBlock() instanceof FoundryPartBlock block)
                || !block.partType().isController()) {
            clearFormation();
            return;
        }

        Direction facing = state.getValue(FoundryPartBlock.FACING);
        FoundryMultiblock.Match match = FoundryMultiblock.tryMatch(level, worldPosition, facing);
        if (!match.matched()) {
            clearFormation();
            return;
        }

        applyFormation(match);
        if (formed) {
            net.mads.industron.runtime.StructureWatch.register(this, BlockPos.betweenClosed(
                    match.minX() - 1, match.baseY() - 1, match.minZ() - 1,
                    match.minX() + match.outerSize(), match.topY() + 1, match.minZ() + match.outerSize()));
        }
            } finally {
            net.mads.industron.debug.CEPerformanceProfiler.record(net.mads.industron.debug.CEPerformanceProfiler.Metric.STRUCTURE_VALIDATION, performanceStart);
        }
    }

    private void applyFormation(FoundryMultiblock.Match match) {
        if (level == null) {
            return;
        }

        List<BlockPos> previousParts = formedSpecialParts;
        List<BlockPos> nextParts = match.specialPartPositions();

        for (BlockPos oldPos : previousParts) {
            if (!nextParts.contains(oldPos)
                    && level.getBlockEntity(oldPos) instanceof FoundryBlockEntity part
                    && worldPosition.equals(part.controllerPos)) {
                part.detachFromController();
            }
        }

        boolean controllerChanged = !formed
                || !Objects.equals(formedCasingModel, match.dominantCasingModel())
                || outerSize != match.outerSize()
                || baseY != match.baseY()
                || topY != match.topY()
                || insideWidth != match.insideWidth()
                || insideHeight != match.insideHeight()
                || insideArea != match.insideArea()
                || insideVolume != match.insideVolume()
                || !Objects.equals(dominantMaterialId, match.dominantMaterialId())
                || !formedSpecialParts.equals(nextParts);

        formed = true;
        controllerPos = worldPosition;
        formedCasingModel = match.dominantCasingModel();
        outerSize = match.outerSize();
        minX = match.minX();
        minZ = match.minZ();
        baseY = match.baseY();
        topY = match.topY();
        insideWidth = match.insideWidth();
        insideHeight = match.insideHeight();
        insideArea = match.insideArea();
        insideVolume = match.insideVolume();
        dominantMaterialId = match.dominantMaterialId();
        formedSpecialParts = List.copyOf(nextParts);
        formedBrickPositions = List.copyOf(match.brickPositions());
        abilityPositions = match.abilityPositions();
        itemStorage().capacity(FoundryThermalRules.itemSlots(insideVolume));
        fluidStorage().resize(insideVolume);
        applyBrickDamage();
        if (!formed) return;
        refreshBricks();
        if (!formed) return;
        findHeatSource();

        for (BlockPos partPos : formedSpecialParts) {
            if (partPos.equals(worldPosition)) {
                continue;
            }
            if (level.getBlockEntity(partPos) instanceof FoundryBlockEntity part) {
                part.attachToController(worldPosition, formedCasingModel);
                if (controllerChanged) level.invalidateCapabilities(partPos);
            }
        }

        if (controllerChanged) {
            syncToClient();
        }
    }

    private void clearFormation() {
        if (level != null) {
            for (BlockPos partPos : formedSpecialParts) {
                if (partPos.equals(worldPosition)) {
                    continue;
                }
                if (level.getBlockEntity(partPos) instanceof FoundryBlockEntity part
                        && worldPosition.equals(part.controllerPos)) {
                    part.detachFromController();
                }
            }
        }

        boolean changed = formed
                || controllerPos != null
                || formedCasingModel != null
                || !formedSpecialParts.isEmpty();

        formed = false;
        receivingHeat = false;
        heatSourcePos = null;
        cachedHeatSource = null;
        safeTemperature = 0;
        brickLimits.clear();
        pendingExposure.clear();
        if (items != null) items.capacity(0);
        controllerPos = null;
        formedCasingModel = null;
        outerSize = 0;
        baseY = 0;
        topY = 0;
        insideWidth = 0;
        insideHeight = 0;
        insideArea = 0;
        insideVolume = 0;
        dominantMaterialId = "";
        formedSpecialParts = List.of();
        formedBrickPositions = List.of();
        abilityPositions = Map.of();

        if (changed) {
            syncToClient();
        }
    }

    private void attachToController(BlockPos controller, @Nullable ResourceLocation casingModel) {
        boolean changed = !formed
                || !Objects.equals(controllerPos, controller)
                || !Objects.equals(formedCasingModel, casingModel);
        formed = true;
        controllerPos = controller;
        formedCasingModel = casingModel;
        if (changed) {
            syncToClient();
        }
    }

    private void detachFromController() {
        boolean changed = formed || controllerPos != null || formedCasingModel != null;
        formed = false;
        controllerPos = null;
        formedCasingModel = null;
        if (changed) {
            syncToClient();
        }
    }

    private void syncToClient() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.invalidateCapabilities(worldPosition);
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeState(tag, true);
        if (isController()) {
            tag.putDouble("Temperature", currentTemperature);
            tag.putInt("LastInsideVolume", lastInsideVolume);
            tag.putInt("LastBrickCount", lastBrickCount);
            tag.put("FoundryItems", itemStorage().save(registries));
            tag.put("FoundryFluids", fluidStorage().save(registries));
            ListTag damages = new ListTag();
            brickDamage.forEach((pos, damage) -> {
                CompoundTag entry = new CompoundTag();
                entry.putLong("Pos", pos.asLong());
                entry.putString("Block", damage.blockId().toString());
                entry.putDouble("Damage", damage.amount());
                damages.add(entry);
            });
            tag.put("BrickDamage", damages);
            ListTag exposure = new ListTag();
            pendingExposure.forEach((limit, amount) -> {
                CompoundTag entry = new CompoundTag();
                entry.putInt("Limit", limit);
                entry.putDouble("Amount", amount);
                exposure.add(entry);
            });
            tag.put("PendingExposure", exposure);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readState(tag, true);
        if (level == null || !level.isClientSide()) formed = false;
        if (isController() && (level == null || !level.isClientSide())) {
            double temperature = tag.contains("Temperature") ? tag.getDouble("Temperature") : FoundryThermalRules.AMBIENT_C;
            currentTemperature = Double.isFinite(temperature) ? Math.max(FoundryThermalRules.AMBIENT_C, temperature) : FoundryThermalRules.AMBIENT_C;
            lastInsideVolume = Math.max(1, tag.getInt("LastInsideVolume"));
            lastBrickCount = Math.max(1, tag.getInt("LastBrickCount"));
            itemStorage().load(tag.getList("FoundryItems", Tag.TAG_COMPOUND), registries);
            itemStorage().capacity(0); // Revalidate before allowing insertion after load.
            fluidStorage().load(tag.getCompound("FoundryFluids"), registries);
            brickDamage.clear();
            ListTag damages = tag.getList("BrickDamage", Tag.TAG_COMPOUND);
            for (int i = 0; i < damages.size(); i++) {
                CompoundTag entry = damages.getCompound(i);
                ResourceLocation block = ResourceLocation.tryParse(entry.getString("Block"));
                double amount = entry.getDouble("Damage");
                if (block != null && Double.isFinite(amount) && amount > 0.0)
                    brickDamage.put(BlockPos.of(entry.getLong("Pos")), new BrickDamage(block, amount));
            }
            pendingExposure.clear();
            ListTag exposure = tag.getList("PendingExposure", Tag.TAG_COMPOUND);
            for (int i = 0; i < exposure.size(); i++) {
                CompoundTag entry = exposure.getCompound(i);
                double amount = entry.getDouble("Amount");
                if (Double.isFinite(amount) && amount > 0.0) pendingExposure.put(entry.getInt("Limit"), amount);
            }
        }
        dirty = true;
        validationCooldown = 0;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        // Do not call a persistence serializer here: inventory and brick maps stay server-side.
        CompoundTag tag = new CompoundTag();
        writeState(tag, false);
        if (isController()) tag.put("FoundryFluids", fluidStorage().save(registries));
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        readState(tag, false);
        if (isController() && tag.contains("FoundryFluids")) fluidStorage().load(tag.getCompound("FoundryFluids"), registries);
    }

    @Override
    public void onDataPacket(
            Connection connection,
            ClientboundBlockEntityDataPacket packet,
            HolderLookup.Provider registries
    ) {
        handleUpdateTag(packet.getTag(), registries);
    }

    private void writeState(CompoundTag tag, boolean includeControllerRuntime) {
        tag.putBoolean("Formed", formed);
        if (controllerPos != null) {
            tag.putLong("ControllerPos", controllerPos.asLong());
        }
        if (formedCasingModel != null) {
            tag.putString("FormedCasingModel", formedCasingModel.toString());
        }

        if (!includeControllerRuntime) {
            return;
        }

        tag.putInt("OuterSize", outerSize);
        tag.putInt("BaseY", baseY);
        tag.putInt("TopY", topY);
        tag.putInt("InsideWidth", insideWidth);
        tag.putInt("InsideHeight", insideHeight);
        tag.putInt("InsideArea", insideArea);
        tag.putInt("InsideVolume", insideVolume);
        tag.putString("DominantMaterial", dominantMaterialId);
        tag.putLongArray("FormedSpecialParts", formedSpecialParts.stream().mapToLong(BlockPos::asLong).toArray());
    }

    private void readState(CompoundTag tag, boolean includeControllerRuntime) {
        formed = tag.getBoolean("Formed");
        controllerPos = tag.contains("ControllerPos") ? BlockPos.of(tag.getLong("ControllerPos")) : null;
        formedCasingModel = tag.contains("FormedCasingModel")
                ? ResourceLocation.parse(tag.getString("FormedCasingModel"))
                : null;

        if (!includeControllerRuntime) {
            return;
        }

        outerSize = tag.getInt("OuterSize");
        baseY = tag.getInt("BaseY");
        topY = tag.getInt("TopY");
        insideWidth = tag.getInt("InsideWidth");
        insideHeight = tag.getInt("InsideHeight");
        insideArea = tag.getInt("InsideArea");
        insideVolume = tag.getInt("InsideVolume");
        dominantMaterialId = tag.getString("DominantMaterial");

        long[] positions = tag.getLongArray("FormedSpecialParts");
        List<BlockPos> loadedParts = new ArrayList<>(positions.length);
        for (long packed : positions) {
            loadedParts.add(BlockPos.of(packed));
        }
        formedSpecialParts = List.copyOf(loadedParts);
    }
}
