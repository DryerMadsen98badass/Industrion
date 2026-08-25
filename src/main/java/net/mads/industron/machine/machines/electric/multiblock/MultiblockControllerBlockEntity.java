package net.mads.industron.machine.machines.electric.multiblock;

import com.simibubi.create.content.kinetics.fan.AirCurrent;
import com.simibubi.create.content.kinetics.fan.IAirCurrentSource;
import com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import net.mads.industron.block.ActiveBlockDefinition;
import net.mads.industron.energy.CEEnergyContainer;
import net.mads.industron.debug.CEPerformanceProfiler;
import net.mads.industron.fluid.IndustrialFluid;
import net.mads.industron.fluid.IndustrialFluidLookup;
import net.mads.industron.fluid.IndustrialFluids;
import net.mads.industron.gui.ProgressBar;
import net.mads.industron.machine.MachineDrive;
import net.mads.industron.machine.KineticRpmError;
import net.mads.industron.machine.MachinePortBlockEntity;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.machine.MachineTierStats;
import net.mads.industron.machine.FireboxBlock;
import net.mads.industron.machine.interaction.InteractionContext;
import net.mads.industron.machine.interaction.InteractionPhase;
import net.mads.industron.machine.interaction.InteractionRuntime;
import net.mads.industron.machine.interaction.InteractionWearStore;
import net.mads.industron.machine.interaction.MachineModifier;
import net.mads.industron.machine.interaction.ConditionFailure;
import net.mads.industron.machine.runtime.CERecipeExecution;
import net.mads.industron.machine.runtime.CERecipeLogic;
import net.mads.industron.machine.runtime.CERecipeLogicHost;
import net.mads.industron.machine.runtime.CERecipeLogicMachine;
import net.mads.industron.machine.runtime.CERecipeTickResult;
import net.mads.industron.machine.runtime.CERecipeStatus;
import net.mads.industron.machine.control.MachineControlContext;
import net.mads.industron.machine.control.MachineControlSnapshot;
import net.mads.industron.machine.control.MachineControlTarget;
import net.mads.industron.machine.coil.CoilBlock;
import net.mads.industron.recipe.CERecipe;
import net.mads.industron.recipe.CEChancedFluidInput;
import net.mads.industron.recipe.CEChancedFluidOutput;
import net.mads.industron.recipe.CEChancedItemInput;
import net.mads.industron.recipe.CERecipeInput;
import net.mads.industron.recipe.CERecipeLookup;
import net.mads.industron.recipe.CERecipeTypes;
import net.mads.industron.recipe.ChemicalBalanceRange;
import net.mads.industron.registry.BlockEntityRegistry;
import net.mads.industron.registry.FluidRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.mads.industron.menu.MultiblockControllerMenu;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class MultiblockControllerBlockEntity extends BlockEntity implements MenuProvider, CERecipeLogicMachine, MachineControlTarget {
    private static final int VALIDATION_INTERVAL = 40;
    private static final int IDLE_RECIPE_CHECK_INTERVAL = 20;
    private static final int ACTIVE_SYNC_INTERVAL = 20;
    private static final int EXTERNAL_OPERATION_DURATION = 20;
    private static final int MUFFLER_SMOKE_INTERVAL = 10;
    private static final int MUFFLER_SMOKE_HEIGHT = 20;
    private static final int MUFFLER_SMOKE_STEP = 4;
    private static final String KINETIC_OUTPUT_RPM_DATA = "KineticOutputRpm";
    private static final String DIRTY_ASSEMBLER_SUCCESS_DATA = "DirtyAssemblerSuccess";
    private static final String SEQUENTIAL_FLUID_OUTPUT_INDEXES_DATA = "SequentialFluidOutputIndexes";
    private static final String CB_VALUE_DATA = "ChemicalBalanceTenThousandths";
    private static final String CB_HATCH_PRESENT_DATA = "CbHatchPresent";
    private static final String LEGACY_PH_VALUE_DATA = "PhTenThousandths";
    private static final String LEGACY_PH_HATCH_PRESENT_DATA = "PhHatchPresent";
    private static final String LEGACY_PH_WEIGHT_DATA = "PhWeightMb";
    private static final String LEGACY_PH_WEIGHTED_SUM_DATA = "PhWeightedHundredthsMb";
    private static final String MACHINE_DURABILITY_DATA = "MachineDurabilityHundredths";
    private static final int CB_NEUTRALIZE_INTERVAL = 1;
    private static final long CB_UNITS_PER_HUNDREDTH = 100L;
    private static final long CB_NEUTRAL_TEN_THOUSANDTHS = ChemicalBalanceRange.NEUTRAL_HUNDREDTHS * CB_UNITS_PER_HUNDREDTH;
    private static final long CB_MIN_TEN_THOUSANDTHS = ChemicalBalanceRange.MIN_HUNDREDTHS * CB_UNITS_PER_HUNDREDTH;
    private static final long CB_MAX_TEN_THOUSANDTHS = ChemicalBalanceRange.MAX_HUNDREDTHS * CB_UNITS_PER_HUNDREDTH;
    // 0.05 CB per tick = 1.00 CB per second at 20 TPS.
    private static final long CB_NEUTRALIZE_STEP = 5L * CB_UNITS_PER_HUNDREDTH;

    private boolean formed;
    private boolean dirty = true;
    private int validationCooldown;
    private int recipeCheckCooldown;
    private String formedVariant = "";
    private int variantLevel;
    private MachineTier formedTier;
    private int formedCoilHeat;
    private int formedCoilCount;
    private List<BlockPos> formedPositions = List.of();
    private Set<BlockPos> formedPositionSet = Set.of();
    private Set<BlockPos> formedPartPositions = Set.of();
    private Map<MultiblockAbility, List<BlockPos>> abilityPositions = new EnumMap<>(MultiblockAbility.class);
    private Map<MultiblockAbility, List<BlockPos>> sortedAbilityPositions = new EnumMap<>(MultiblockAbility.class);
    private Map<Integer, BlockPos> sequentialInputPositions = Map.of();
    private Set<BlockPos> sequentialInputPositionSet = Set.of();
    private Map<Integer, BlockPos> sequentialOutputPositions = Map.of();
    private Map<BlockPos, ResourceLocation> formedOverlayModels = Map.of();
    private List<MachinePortBlockEntity> machineControlRedstonePorts = List.of();
    private boolean cbHatchPresent;
    private long chemicalBalanceTenThousandths = CB_NEUTRAL_TEN_THOUSANDTHS;
    private long machineDurabilityHundredths = -1L;
    private int recipeProgress;
    private int recipeDuration;
    private int activeCEt;
    private int activeSyncCooldown;
    private int overlayFrame;
    private int overlayFrameTicks;
    private int activeBlockFrame;
    private int activeBlockFrameTicks;
    private ResourceLocation preferredRecipeId;
    private int externalActiveTicks;
    private int externalCEt;
    private int externalWarmupTicks;
    private final InteractionWearStore interactionWear = new InteractionWearStore();
    private final CERecipeLogic recipeLogic = new CERecipeLogic(new RecipeHost());
    private boolean machineEnabled = true;
    private long machineControlSnapshotTick = Long.MIN_VALUE;
    private MachineControlSnapshot machineControlSnapshot;
    private long machineControlInputRevision;
    private long machineControlCachedInputRevision = Long.MIN_VALUE;
    private List<ItemStack> machineControlCachedItemInputs = List.of();
    private List<FluidStack> machineControlCachedFluidInputs = List.of();
    private long fanInputCacheTick = Long.MIN_VALUE;
    private FanInputStats fanInputCache = FanInputStats.NONE;
    private boolean clientSyncPending;

    public MultiblockControllerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.MULTIBLOCK_CONTROLLER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MultiblockControllerBlockEntity controller) {
        if (level.isClientSide()) {
            return;
        }

        long profileStart = CEPerformanceProfiler.begin(level);
        try {
            if (controller.validationCooldown > 0) {
                controller.validationCooldown--;
            }

            if (controller.dirty || controller.validationCooldown <= 0) {
                controller.validateStructure(state);
                controller.validationCooldown = VALIDATION_INTERVAL;
            }

            controller.tickChemicalBalanceSystem();
            if (controller.tickMachineDurability()) {
                return;
            }
            controller.applyMachineControlSchedules();
            controller.tickRecipe();
            controller.tickMufflerSmoke();
            controller.tickOverlayFrame();
            controller.tickActiveBlockFrame();
            controller.syncKineticOutputPorts();
        } finally {
            controller.flushClientSync();
            CEPerformanceProfiler.record(CEPerformanceProfiler.Metric.MULTIBLOCK_TICK, profileStart);
        }
    }

    public boolean isFormed() {
        return formed;
    }

    public String formedVariant() {
        return formedVariant;
    }

    public int variantLevel() {
        return variantLevel;
    }

    public MachineTier formedTier() {
        return formedTier;
    }

    public int formedCoilHeat() {
        return formedCoilHeat;
    }

    public int formedCoilCount() {
        return formedCoilCount;
    }

    public boolean hasFormedPosition(BlockPos pos) {
        return formedPositionSet.contains(pos);
    }

    public boolean consumeExternalHeatEnergy(int energyPerTick, int ticks) {
        if (level == null || !formed || !machineEnabled || energyPerTick <= 0 || ticks <= 0) {
            return false;
        }
        MultiblockDefinition definition = currentDefinition();
        if (definition == null || !definition.externalHeatSource()) {
            return false;
        }

        if (!transferEnergy(energyPerTick, true)) {
            return false;
        }

        boolean wasExternallyActive = externallyActive();
        boolean energyChanged = activeCEt != energyPerTick || externalCEt != energyPerTick;
        externalActiveTicks = Math.max(externalActiveTicks, ticks + 2);
        externalCEt = energyPerTick;
        activeCEt = energyPerTick;
        if (!recipeLogic.isProcessing() && !wasExternallyActive) {
            recipeProgress = 0;
            recipeDuration = externalOperationDuration(definition);
            externalWarmupTicks = 0;
            activeSyncCooldown = 0;
        } else if (energyChanged) {
            activeSyncCooldown = 0;
        }
        setActive(true);
        return true;
    }

    public boolean externalHeatReady() {
        MultiblockDefinition definition = currentDefinition();
        return definition != null
                && definition.externalHeatSource()
                && externallyActive()
                && externalWarmupTicks >= externalOperationDuration(definition);
    }

    public boolean canRunTier(MachineTier requiredTier) {
        MultiblockDefinition definition = currentDefinition();
        return formed && definition != null && processingTier(definition).filter(actualTier -> MachineTierStats.isAtLeast(actualTier, requiredTier)).isPresent();
    }

    public List<BlockPos> abilityPositions(MultiblockAbility ability) {
        return abilityPositions.getOrDefault(ability, List.of());
    }

    public boolean hasCbHatch() {
        return formed && cbHatchPresent;
    }

    public int machineChemicalBalanceHundredths() {
        long rounded = Math.round(chemicalBalanceTenThousandths / (double) CB_UNITS_PER_HUNDREDTH);
        return (int) Math.max(ChemicalBalanceRange.MIN_HUNDREDTHS, Math.min(ChemicalBalanceRange.MAX_HUNDREDTHS, rounded));
    }

    public int recipeMinimumChemicalBalanceHundredths() {
        return activeRecipe().flatMap(CERecipe::chemicalBalanceRange)
                .map(ChemicalBalanceRange::minHundredths)
                .orElse(ChemicalBalanceRange.NEUTRAL_HUNDREDTHS);
    }

    public int recipeMaximumChemicalBalanceHundredths() {
        return activeRecipe().flatMap(CERecipe::chemicalBalanceRange)
                .map(ChemicalBalanceRange::maxHundredths)
                .orElse(ChemicalBalanceRange.NEUTRAL_HUNDREDTHS);
    }

    public Optional<ChemicalBalanceRange> safeChemicalBalanceRange() {
        MultiblockDefinition definition = currentDefinition();
        return definition == null ? Optional.empty() : definition.chemicalBalanceRange();
    }

    public boolean hasMachineDurability() {
        MultiblockDefinition definition = currentDefinition();
        return definition != null && definition.machineDurability().isPresent();
    }

    public int maxMachineDurability() {
        MultiblockDefinition definition = currentDefinition();
        return definition == null ? 0 : definition.machineDurability().orElse(0);
    }

    public long machineDurabilityHundredths() {
        initializeMachineDurability(currentDefinition());
        return Math.max(0L, machineDurabilityHundredths);
    }

    public int corrosionDamageHundredthsPerTick() {
        MultiblockDefinition definition = currentDefinition();
        if (!formed || definition == null || definition.chemicalBalanceRange().isEmpty() || definition.machineDurability().isEmpty()) {
            return 0;
        }

        ChemicalBalanceRange range = definition.chemicalBalanceRange().orElseThrow();
        int currentCb = machineChemicalBalanceHundredths();
        if (currentCb < range.minHundredths()) {
            return range.minHundredths() - currentCb;
        }
        if (currentCb > range.maxHundredths()) {
            return currentCb - range.maxHundredths();
        }
        return 0;
    }

    public void markStructureDirty() {
        dirty = true;
        validationCooldown = 0;
        setChanged();
    }

    public void clearFormation() {
        if (formationAlreadyClear()) return;
        setCoilsActive(false);
        setFireboxesActive(false);
        detachParts();
        formed = false;
        formedVariant = "";
        variantLevel = 0;
        formedTier = null;
        formedCoilHeat = 0;
        formedCoilCount = 0;
        formedPositions = List.of();
        formedPositionSet = Set.of();
        formedPartPositions = Set.of();
        abilityPositions = new EnumMap<>(MultiblockAbility.class);
        sortedAbilityPositions = new EnumMap<>(MultiblockAbility.class);
        sequentialInputPositions = Map.of();
        sequentialInputPositionSet = Set.of();
        sequentialOutputPositions = Map.of();
        formedOverlayModels = Map.of();
        machineControlRedstonePorts = List.of();
        cbHatchPresent = false;
        resetChemicalBalanceState();
        markMachineControlInputsDirty();
        externalActiveTicks = 0;
        externalCEt = 0;
        externalWarmupTicks = 0;
        activeBlockFrame = 0;
        activeBlockFrameTicks = 0;
        recipeLogic.cancel();
        clearActiveRecipe();
        updateBlockFormedState(false);
        updateBlockActiveState(false);
        setChanged();
        syncToClient();
    }

    private boolean formationAlreadyClear() {
        if (formed
                || !formedVariant.isEmpty()
                || variantLevel != 0
                || formedTier != null
                || formedCoilHeat != 0
                || formedCoilCount != 0
                || !formedPositions.isEmpty()
                || !formedPositionSet.isEmpty()
                || !formedPartPositions.isEmpty()
                || !abilityPositions.isEmpty()
                || !sortedAbilityPositions.isEmpty()
                || !sequentialInputPositions.isEmpty()
                || !sequentialInputPositionSet.isEmpty()
                || !sequentialOutputPositions.isEmpty()
                || !formedOverlayModels.isEmpty()
                || !machineControlRedstonePorts.isEmpty()
                || cbHatchPresent
                || chemicalBalanceTenThousandths != CB_NEUTRAL_TEN_THOUSANDTHS
                || externalActiveTicks != 0
                || externalCEt != 0
                || externalWarmupTicks != 0
                || recipeProgress != 0
                || recipeDuration != 0
                || activeCEt != 0
                || recipeLogic.execution() != null
                || activeBlockFrame != 0
                || activeBlockFrameTicks != 0) {
            return false;
        }
        BlockState state = getBlockState();
        return !(state.getBlock() instanceof MultiblockControllerBlock)
                || (!state.getValue(MultiblockControllerBlock.FORMED)
                && !state.getValue(MultiblockControllerBlock.ACTIVE));
    }

    private void validateStructure(BlockState state) {
        boolean structureWasDirty = dirty;
        dirty = false;
        if (level == null || !(state.getBlock() instanceof MultiblockControllerBlock controllerBlock)) {
            clearFormation();
            return;
        }

        Direction facing = state.getValue(MultiblockControllerBlock.FACING);
        MultiblockDefinition definition = currentDefinition(controllerBlock);
        if (definition == null) {
            clearFormation();
            return;
        }

        MultiblockMatchResult result = definition.tryMatch(level, worldPosition, facing);
        if (!result.matched() || !mufflersOpen(result.abilityPositions())) {
            clearFormation();
            return;
        }

        if (matchesCurrentFormation(result)) {
            if (!repairFormationParts(result)) {
                form(result);
                return;
            }
            refreshMachineControlRedstonePorts();
            if (structureWasDirty) markMachineControlInputsDirty();
            return;
        }

        form(result);
    }

    private boolean matchesCurrentFormation(MultiblockMatchResult result) {
        if (!formed
                || !formedVariant.equals(result.variant())
                || variantLevel != result.variantLevel()
                || formedTier != result.tier()
                || formedCoilHeat != result.coilHeat()
                || formedCoilCount != result.coilCount()
                || !formedPositions.equals(result.positions())
                || !abilityPositions.equals(result.abilityPositions())
                || !sequentialInputPositions.equals(result.sequentialInputPositions())
                || !sequentialOutputPositions.equals(result.sequentialOutputPositions())
                || !formedOverlayModels.equals(result.overlayModels())) {
            return false;
        }
        return true;
    }

    private boolean repairFormationParts(MultiblockMatchResult result) {
        if (level == null) return false;
        boolean repaired = false;
        for (BlockPos partPos : formedPartPositions) {
            if (!(level.getBlockEntity(partPos) instanceof MultiblockPart part)) return false;
            if (part instanceof MachinePortBlockEntity port) {
                ResourceLocation overlay = result.overlayModels().get(partPos);
                if (!java.util.Objects.equals(port.assembledOverlayModel(), overlay)) {
                    port.setAssembledOverlayModel(overlay);
                    repaired = true;
                }
            }
            if (!worldPosition.equals(part.controllerPos())) {
                part.attachToMultiblock(worldPosition);
                repaired = true;
            }
        }
        if (repaired) markMachineControlInputsDirty();
        return true;
    }

    private void form(MultiblockMatchResult result) {
        boolean keepActive = isProcessing();
        setCoilsActive(false);
        setFireboxesActive(false);
        detachParts();
        formed = true;
        formedVariant = result.variant();
        variantLevel = result.variantLevel();
        formedTier = result.tier();
        formedCoilHeat = result.coilHeat();
        formedCoilCount = result.coilCount();
        formedPositions = result.positions();
        formedPositionSet = Set.copyOf(result.positions());
        formedPartPositions = collectFormedPartPositions(result.positions());
        abilityPositions = result.abilityPositions();
        sortedAbilityPositions = sortAbilityPositions(result.abilityPositions());
        sequentialInputPositions = result.sequentialInputPositions();
        sequentialInputPositionSet = Set.copyOf(result.sequentialInputPositions().values());
        sequentialOutputPositions = result.sequentialOutputPositions();
        formedOverlayModels = result.overlayModels();
        cbHatchPresent = !abilityPositions(MultiblockAbility.CB_INPUT).isEmpty();
        initializeMachineDurability(currentDefinition());
        if (!cbHatchPresent) {
            resetChemicalBalanceState();
        }
        markMachineControlInputsDirty();
        updateBlockFormedState(true);
        for (BlockPos partPos : formedPositions) {
            if (partPos.equals(worldPosition)) {
                continue;
            }

            if (level != null && level.getBlockEntity(partPos) instanceof MultiblockPart part) {
                if (part instanceof MachinePortBlockEntity port) {
                    port.setAssembledOverlayModel(result.overlayModels().get(partPos));
                }
                part.attachToMultiblock(worldPosition);
            }
        }

        refreshMachineControlRedstonePorts();
        setChanged();
        syncToClient();
        if (keepActive && machineEnabled) {
            setActive(true);
        }
    }

    private boolean mufflersOpen(Map<MultiblockAbility, List<BlockPos>> positionsByAbility) {
        if (level == null) {
            return false;
        }
        for (BlockPos pos : positionsByAbility.getOrDefault(MultiblockAbility.MUFFLER, List.of())) {
            if (!level.getBlockState(pos.above()).isAir()) {
                return false;
            }
        }
        return true;
    }

    private boolean mufflersOpen() {
        return mufflersOpen(abilityPositions);
    }

    private void tickMufflerSmoke() {
        if (!(level instanceof ServerLevel serverLevel)
                || !formed
                || recipeLogic.status() != CERecipeStatus.WORKING
                || level.getGameTime() % MUFFLER_SMOKE_INTERVAL != 0L) {
            return;
        }
        for (BlockPos pos : abilityPositions(MultiblockAbility.MUFFLER)) {
            if (!level.getBlockState(pos.above()).isAir()) {
                continue;
            }
            for (int height = 0; height < MUFFLER_SMOKE_HEIGHT; height += MUFFLER_SMOKE_STEP) {
                serverLevel.sendParticles(
                        ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                        pos.getX() + 0.5D,
                        pos.getY() + 1.1D + height,
                        pos.getZ() + 0.5D,
                        1,
                        0.28D,
                        0.35D,
                        0.28D,
                        0.015D
                );
            }
        }
    }

    public void applyMachineControlSchedulesNow() {
        if (level != null && !level.isClientSide()) applyMachineControlSchedules();
    }

    private void applyMachineControlSchedules() {
        if (level == null || !formed) return;

        boolean foundSchedule = false;
        boolean enabled = true;
        for (MachinePortBlockEntity port : machineControlRedstonePorts) {
            if (port.isRemoved() || !worldPosition.equals(port.controllerPos())) continue;
            if (!port.machineControlSchedules().isEmpty()) {
                foundSchedule = true;
                if (!port.evaluateAllMachineControlSchedules()) enabled = false;
            }
        }
        if (foundSchedule) setMachineEnabled(enabled);
    }

    private void refreshMachineControlRedstonePorts() {
        if (level == null || !formed) {
            machineControlRedstonePorts = List.of();
            return;
        }
        List<MachinePortBlockEntity> ports = new ArrayList<>();
        for (BlockPos pos : abilityPositions(MultiblockAbility.REDSTONE)) {
            if (level.getBlockEntity(pos) instanceof MachinePortBlockEntity port && !ports.contains(port)) ports.add(port);
        }
        machineControlRedstonePorts = List.copyOf(ports);
    }

    private void tickRecipe() {
        long profileStart = CEPerformanceProfiler.begin(level);
        try {
            tickRecipeInner();
        } finally {
            CEPerformanceProfiler.record(CEPerformanceProfiler.Metric.MULTIBLOCK_RECIPE_TICK, profileStart);
        }
    }

    private void tickRecipeInner() {
        if (level == null || !formed || !(getBlockState().getBlock() instanceof MultiblockControllerBlock controllerBlock)) {
            recipeLogic.cancel();
            clearActiveRecipe();
            setActive(false);
            return;
        }

        MultiblockDefinition definition = currentDefinition(controllerBlock);
        if (!machineEnabled) {
            setActive(false);
            return;
        }

        if (definition == null || definition.recipeTypes().isEmpty()) {
            recipeLogic.cancel();
            tickExternalOperation(definition);
            return;
        }
        recipeLogic.serverTick();
    }

    private boolean externallyActive() {
        return externalActiveTicks > 0;
    }

    private void tickExternalOperation(@Nullable MultiblockDefinition definition) {
        if (definition == null || !definition.externalHeatSource() || !externallyActive()) {
            externalCEt = 0;
            externalWarmupTicks = 0;
            clearActiveRecipe();
            setActive(false);
            return;
        }

        if (!processEnergy(externalCEt)) {
            externalActiveTicks = 0;
            externalCEt = 0;
            externalWarmupTicks = 0;
            clearActiveRecipe();
            setActive(false);
            return;
        }

        externalActiveTicks--;
        int duration = externalOperationDuration(definition);
        if (recipeDuration != duration) {
            recipeDuration = duration;
        }
        recipeProgress = recipeDuration <= 1 ? 0 : (recipeProgress + 1) % recipeDuration;
        if (externalWarmupTicks < recipeDuration) {
            externalWarmupTicks++;
        }
        activeCEt = externalCEt;
        setActive(true);
        setChanged();
        syncActiveProgress();
    }

    private static int externalOperationDuration(MultiblockDefinition definition) {
        MultiblockDefinition.InputOnlyDisplay display = definition.inputOnlyDisplay();
        return display == null ? EXTERNAL_OPERATION_DURATION : display.durationTicks();
    }

    private void tickChemicalBalanceSystem() {
        if (level == null || !hasCbHatch()) {
            return;
        }

        boolean changed = false;
        if (level.getGameTime() % CB_NEUTRALIZE_INTERVAL == 0L) {
            changed |= neutralizeChemicalBalance();
        }

        for (BlockPos pos : sortedAbilityPositions(MultiblockAbility.CB_INPUT)) {
            if (!(level.getBlockEntity(pos) instanceof MachinePortBlockEntity port)) {
                continue;
            }
            for (FluidTank tank : port.fluidTanks()) {
                FluidStack stack = tank.getFluid();
                IndustrialFluid fluid = IndustrialFluidLookup.find(stack);
                if (stack.isEmpty() || fluid == null || !fluid.hasChemicalBalance()) {
                    continue;
                }
                FluidStack drained = tank.drain(Math.min(fluid.cbDrainPerTickMb(), stack.getAmount()), FluidAction.EXECUTE);
                if (!drained.isEmpty()) {
                    addChemicalBalanceFluid(fluid.chemicalBalanceHundredths().orElseThrow(), drained.getAmount());
                    changed = true;
                }
            }
        }

        if (changed) {
            markMachineControlInputsDirty();
            machineControlSnapshot = null;
            machineControlSnapshotTick = Long.MIN_VALUE;
            setChanged();
        }
    }

    private void addChemicalBalanceFluid(int chemicalBalanceHundredths, int amountMb) {
        if (amountMb <= 0) {
            return;
        }

        long neutralOffsetHundredths = chemicalBalanceHundredths - (long) ChemicalBalanceRange.NEUTRAL_HUNDREDTHS;
        long changeTenThousandths = neutralOffsetHundredths * amountMb;
        chemicalBalanceTenThousandths = clampChemicalBalance(chemicalBalanceTenThousandths + changeTenThousandths);
    }

    private boolean neutralizeChemicalBalance() {
        if (chemicalBalanceTenThousandths == CB_NEUTRAL_TEN_THOUSANDTHS) {
            return false;
        }

        if (chemicalBalanceTenThousandths < CB_NEUTRAL_TEN_THOUSANDTHS) {
            chemicalBalanceTenThousandths = Math.min(CB_NEUTRAL_TEN_THOUSANDTHS, chemicalBalanceTenThousandths + CB_NEUTRALIZE_STEP);
        } else {
            chemicalBalanceTenThousandths = Math.max(CB_NEUTRAL_TEN_THOUSANDTHS, chemicalBalanceTenThousandths - CB_NEUTRALIZE_STEP);
        }
        return true;
    }

    private void resetChemicalBalanceState() {
        chemicalBalanceTenThousandths = CB_NEUTRAL_TEN_THOUSANDTHS;
    }

    private static long clampChemicalBalance(long value) {
        return Math.max(CB_MIN_TEN_THOUSANDTHS, Math.min(CB_MAX_TEN_THOUSANDTHS, value));
    }

    private void initializeMachineDurability(@Nullable MultiblockDefinition definition) {
        if (definition == null || definition.machineDurability().isEmpty()) {
            machineDurabilityHundredths = -1L;
            return;
        }

        long maximum = definition.machineDurability().orElseThrow() * 100L;
        if (machineDurabilityHundredths < 0L) {
            machineDurabilityHundredths = maximum;
        } else {
            machineDurabilityHundredths = Math.min(machineDurabilityHundredths, maximum);
        }
    }

    private boolean tickMachineDurability() {
        MultiblockDefinition definition = currentDefinition();
        if (!formed || definition == null || definition.machineDurability().isEmpty()) {
            return false;
        }

        initializeMachineDurability(definition);
        if (machineDurabilityHundredths <= 0L) {
            destroyFromCorrosion();
            return true;
        }

        int damage = corrosionDamageHundredthsPerTick();
        if (damage <= 0) {
            return false;
        }

        machineDurabilityHundredths = Math.max(0L, machineDurabilityHundredths - damage);
        setChanged();
        if (machineDurabilityHundredths > 0L) {
            return false;
        }

        destroyFromCorrosion();
        return true;
    }

    private void destroyFromCorrosion() {
        if (level == null || level.isClientSide()) {
            return;
        }

        List<BlockPos> parts = new ArrayList<>(formedPositions);
        parts.remove(worldPosition);
        parts.removeIf(pos -> level.getBlockState(pos).isAir());
        int breakCount = parts.isEmpty() ? 0 : Math.max(1, (int) Math.ceil(parts.size() * 0.10D));

        recipeLogic.cancel();
        clearActiveRecipe();
        setCoilsActive(false);
        setFireboxesActive(false);
        detachParts();
        formed = false;
        machineEnabled = false;

        for (int index = 0; index < breakCount && !parts.isEmpty(); index++) {
            BlockPos target = parts.remove(level.random.nextInt(parts.size()));
            level.destroyBlock(target, false);
        }
        level.destroyBlock(worldPosition, false);
    }

    private boolean recipeChemicalBalanceCompatible(MultiblockDefinition definition, CERecipe recipe) {
        if (definition.chemicalBalanceRange().isEmpty() || recipe.chemicalBalanceRange().isEmpty()) {
            return true;
        }

        ChemicalBalanceRange machineRange = definition.chemicalBalanceRange().orElseThrow();
        ChemicalBalanceRange recipeRange = recipe.chemicalBalanceRange().orElseThrow();
        return recipeRange.maxHundredths() >= machineRange.minHundredths()
                && recipeRange.minHundredths() <= machineRange.maxHundredths();
    }

    private boolean chemicalBalanceMatches(CERecipe recipe) {
        return recipe.chemicalBalanceRange().map(range -> range.containsHundredths(machineChemicalBalanceHundredths())).orElse(true);
    }

    private void syncKineticOutputPorts() {
        if (level == null) {
            return;
        }

        int rpm = formed ? kineticOutputRpm() : 0;
        for (BlockPos pos : sortedAbilityPositions(MultiblockAbility.KINETIC_OUTPUT)) {
            if (level.getBlockEntity(pos) instanceof MachinePortBlockEntity port) {
                port.setGeneratedRpm(rpm);
            }
        }
    }

    private void tickOverlayFrame() {
        if (level == null || !(getBlockState().getBlock() instanceof MultiblockControllerBlock controllerBlock)) {
            return;
        }

        BlockState state = getBlockState();
        if (!state.hasProperty(MultiblockControllerBlock.ACTIVE) || !state.hasProperty(MultiblockControllerBlock.OVERLAY_FRAME)) {
            return;
        }

        boolean active = state.getValue(MultiblockControllerBlock.ACTIVE);
        int frames = controllerBlock.definition().activeOverlayFrameCount();
        if (!active || frames <= 1) {
            overlayFrame = 0;
            overlayFrameTicks = 0;
            if (state.getValue(MultiblockControllerBlock.OVERLAY_FRAME) != 0) {
                level.setBlock(worldPosition, state.setValue(MultiblockControllerBlock.OVERLAY_FRAME, 0), 3);
            }
            return;
        }

        overlayFrameTicks++;
        if (overlayFrameTicks >= 5) {
            overlayFrameTicks = 0;
            overlayFrame = (overlayFrame + 1) % Math.min(frames, 10);
        }

        if (state.getValue(MultiblockControllerBlock.OVERLAY_FRAME) != overlayFrame) {
            level.setBlock(worldPosition, state.setValue(MultiblockControllerBlock.OVERLAY_FRAME, overlayFrame), 3);
        }
    }

    private void tickActiveBlockFrame() {
        if (level == null) {
            return;
        }

        boolean hasAnimatedFirebox = false;
        for (BlockPos partPos : formedPositions) {
            BlockState state = level.getBlockState(partPos);
            if (!(state.getBlock() instanceof FireboxBlock firebox)
                    || !state.hasProperty(FireboxBlock.ACTIVE)
                    || !state.getValue(FireboxBlock.ACTIVE)
                    || !state.hasProperty(FireboxBlock.OVERLAY_FRAME)) {
                continue;
            }

            ActiveBlockDefinition definition = firebox.definition();
            int frames = definition == null ? 1 : Math.min(definition.activeFrameCount(), 10);
            if (frames > 1) {
                hasAnimatedFirebox = true;
            }
        }

        if (!hasAnimatedFirebox) {
            activeBlockFrame = 0;
            activeBlockFrameTicks = 0;
        } else {
            activeBlockFrameTicks++;
            if (activeBlockFrameTicks >= 5) {
                activeBlockFrameTicks = 0;
                activeBlockFrame = (activeBlockFrame + 1) % 10;
            }
        }

        for (BlockPos partPos : formedPositions) {
            BlockState state = level.getBlockState(partPos);
            if (!(state.getBlock() instanceof FireboxBlock firebox)
                    || !state.hasProperty(FireboxBlock.ACTIVE)
                    || !state.hasProperty(FireboxBlock.OVERLAY_FRAME)) {
                continue;
            }

            boolean active = state.getValue(FireboxBlock.ACTIVE);
            ActiveBlockDefinition definition = firebox.definition();
            int frames = definition == null ? 1 : Math.min(definition.activeFrameCount(), 10);
            int frame = active && frames > 1 ? activeBlockFrame % frames : 0;
            if (state.getValue(FireboxBlock.OVERLAY_FRAME) != frame) {
                level.setBlock(partPos, state.setValue(FireboxBlock.OVERLAY_FRAME, frame), 3);
            }
        }
    }

    private void syncActiveProgress() {
        if (activeSyncCooldown > 0) {
            activeSyncCooldown--;
            return;
        }

        activeSyncCooldown = ACTIVE_SYNC_INTERVAL;
        syncToClient();
    }

    private Optional<RecipeHolder<CERecipe>> matchingRecipe(MultiblockDefinition definition, CERecipeInput input) {
        long profileStart = CEPerformanceProfiler.begin(level);
        try {
            return matchingRecipeInner(definition, input);
        } finally {
            CEPerformanceProfiler.record(CEPerformanceProfiler.Metric.RECIPE_LOOKUP, profileStart);
        }
    }

    private Optional<RecipeHolder<CERecipe>> matchingRecipeInner(MultiblockDefinition definition, CERecipeInput input) {
        if (level == null) {
            return Optional.empty();
        }

        if (definition.sequencedInput()) {
            return matchingSequencedRecipe(definition, input);
        }

        if (preferredRecipeId != null) {
            Optional<RecipeHolder<CERecipe>> preferred = CERecipeLookup.preferred(
                            level.getRecipeManager(),
                            preferredRecipeId,
                            Set.copyOf(definition.recipeTypes())
                    )
                    .filter(recipe -> definition.drive() == MachineDrive.KINETIC
                            ? recipe.value().matchesIgnoringRpm(input, level)
                            : recipe.value().matches(input, level))
                    .filter(recipe -> !definition.sequencedOutput() || sequencedFluidOutputsSupported(recipe.value()))
                    .filter(recipe -> recipe.value().chemicalBalanceRange().isEmpty() || hasCbHatch())
                    .filter(recipe -> recipeChemicalBalanceCompatible(definition, recipe.value()))
                    .filter(recipe -> rpmRangesCompatible(definition, recipe.value()))
                    .filter(recipe -> definition.drive() != MachineDrive.KINETIC_OUTPUT
                            || resolvedOutputRpm(definition, recipe.value()) > 0);
            if (preferred.isPresent()) {
                return preferred;
            }
        }

        return CERecipeLookup.candidatesByTypes(level.getRecipeManager(), definition.recipeTypes(), input)
                .stream()
                .filter(recipe -> definition.drive() == MachineDrive.KINETIC
                        ? recipe.value().matchesIgnoringRpm(input, level)
                        : recipe.value().matches(input, level))
                .filter(recipe -> !definition.sequencedOutput() || sequencedFluidOutputsSupported(recipe.value()))
                .filter(recipe -> recipe.value().chemicalBalanceRange().isEmpty() || hasCbHatch())
                .filter(recipe -> recipeChemicalBalanceCompatible(definition, recipe.value()))
                .filter(recipe -> rpmRangesCompatible(definition, recipe.value()))
                .filter(recipe -> definition.drive() != MachineDrive.KINETIC_OUTPUT
                        || resolvedOutputRpm(definition, recipe.value()) > 0)
                .sorted(Comparator.comparing(recipe -> recipe.id().toString()))
                .findFirst();
    }

    private Optional<RecipeHolder<CERecipe>> matchingSequencedRecipe(
            MultiblockDefinition definition,
            CERecipeInput input
    ) {
        if (level == null) {
            return Optional.empty();
        }

        return CERecipeLookup.byTypes(level.getRecipeManager(), definition.recipeTypes())
                .stream()
                .filter(recipe -> definition.drive() == MachineDrive.KINETIC
                        ? recipe.value().matchesIgnoringRpm(input, level)
                        : recipe.value().matches(input, level))
                .filter(recipe -> sequencedItemInputsMatch(recipe.value(), 1))
                .filter(recipe -> !definition.sequencedOutput() || sequencedFluidOutputsSupported(recipe.value()))
                .filter(recipe -> recipe.value().chemicalBalanceRange().isEmpty() || hasCbHatch())
                .filter(recipe -> recipeChemicalBalanceCompatible(definition, recipe.value()))
                .filter(recipe -> rpmRangesCompatible(definition, recipe.value()))
                .filter(recipe -> definition.drive() != MachineDrive.KINETIC_OUTPUT
                        || resolvedOutputRpm(definition, recipe.value()) > 0)
                .sorted(Comparator
                        .<RecipeHolder<CERecipe>>comparingInt(recipe -> recipe.value().itemInputs().size())
                        .reversed()
                        .thenComparing(
                                Comparator.<RecipeHolder<CERecipe>>comparingInt(
                                        recipe -> totalItemInputCount(recipe.value())
                                ).reversed()
                        )
                        .thenComparing(recipe -> recipe.id().toString()))
                .findFirst();
    }

    private boolean sequencedFluidOutputsSupported(CERecipe recipe) {
        int outputCount = recipe.fluidOutputs().size() + recipe.chancedFluidOutputs().size();
        return outputCount > 0 && outputCount <= sequentialOutputPositions.size();
    }

    private boolean sequencedItemInputsMatch(CERecipe recipe, int multiplier) {
        if (recipe.itemInputs().size() < 2
                || !recipe.chancedItemInputs().isEmpty()
                || !recipe.notConsumableItems().isEmpty()
                || recipe.treeSource().isPresent()) {
            return false;
        }

        int sequentialCount = recipe.itemInputs().size() - 1;
        if (sequentialCount > sequentialInputPositions.size()) {
            return false;
        }

        if (!hasIngredient(baseItemInputPorts(), recipe.itemInputs().getFirst(), multiplier)) {
            return false;
        }

        for (int index = 1; index <= sequentialCount; index++) {
            Optional<MachinePortBlockEntity> port = sequentialInputPort(index);
            if (port.isEmpty()
                    || !hasIngredient(List.of(port.get()), recipe.itemInputs().get(index), multiplier)) {
                return false;
            }
        }
        return true;
    }

    private static int totalItemInputCount(CERecipe recipe) {
        long total = 0L;
        for (SizedIngredient input : recipe.itemInputs()) {
            total += input.count();
            if (total >= Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
        }
        return (int) total;
    }

    private List<MachinePortBlockEntity> baseItemInputPorts() {
        if (level == null) {
            return List.of();
        }

        List<MachinePortBlockEntity> ports = new ArrayList<>();
        for (BlockPos pos : sortedAbilityPositions(MultiblockAbility.ITEM_INPUT)) {
            if (sequentialInputPositionSet.contains(pos)) {
                continue;
            }
            if (level.getBlockEntity(pos) instanceof MachinePortBlockEntity port
                    && port.items().getSlots() > 0) {
                ports.add(port);
            }
        }
        return ports;
    }

    private Optional<MachinePortBlockEntity> sequentialInputPort(int index) {
        if (level == null) {
            return Optional.empty();
        }
        BlockPos pos = sequentialInputPositions.get(index);
        if (pos == null) {
            return Optional.empty();
        }
        if (level.getBlockEntity(pos) instanceof MachinePortBlockEntity port
                && port.items().getSlots() > 0) {
            return Optional.of(port);
        }
        return Optional.empty();
    }

    private Optional<MachinePortBlockEntity> sequentialOutputPort(int index) {
        if (level == null) {
            return Optional.empty();
        }
        BlockPos pos = sequentialOutputPositions.get(index);
        if (pos == null) {
            return Optional.empty();
        }
        if (level.getBlockEntity(pos) instanceof MachinePortBlockEntity port
                && !port.fluidTanks().isEmpty()) {
            return Optional.of(port);
        }
        return Optional.empty();
    }

    private static boolean hasIngredient(
            List<MachinePortBlockEntity> ports,
            SizedIngredient input,
            int multiplier
    ) {
        int remaining = multiplyClamped(input.count(), multiplier);
        return consumeFromStacks(itemStacks(ports), input, remaining) <= 0;
    }

    private Optional<MachineTier> highestPortTier(MultiblockAbility ability) {
        if (level == null) {
            return Optional.empty();
        }

        MachineTier best = null;
        for (BlockPos pos : sortedAbilityPositions(ability)) {
            if (level.getBlockEntity(pos) instanceof MachinePortBlockEntity port) {
                best = best == null ? port.tier() : MachineTierStats.max(best, port.tier());
            }
        }
        return Optional.ofNullable(best);
    }

    private Optional<MachineTier> lowestPortTier(MultiblockAbility ability) {
        if (level == null) {
            return Optional.empty();
        }

        MachineTier lowest = null;
        for (BlockPos pos : sortedAbilityPositions(ability)) {
            if (level.getBlockEntity(pos) instanceof MachinePortBlockEntity port) {
                MachineTier tier = port.tier();
                if (tier != null && (lowest == null
                        || MachineTierStats.tierIndex(tier) < MachineTierStats.tierIndex(lowest))) {
                    lowest = tier;
                }
            }
        }
        return Optional.ofNullable(lowest);
    }

    private Optional<MachineTier> processingTier(MultiblockDefinition definition) {
        Optional<MachineTier> tierFromPower = switch (definition.drive()) {
            case ELECTRIC -> highestPortTier(MultiblockAbility.ENERGY_INPUT);
            case KINETIC -> highestPortTier(MultiblockAbility.KINETIC_INPUT);
            case KINETIC_OUTPUT -> highestPortTier(MultiblockAbility.KINETIC_OUTPUT);
            case STEAM, NONE -> Optional.empty();
        };

        if (tierFromPower.isPresent()) {
            MachineTier actualTier = tierFromPower.get().recipeTier();
            if (definition.hasTierRestriction() && !definition.supportsTier(actualTier)) {
                return Optional.empty();
            }
            return Optional.of(actualTier);
        }

        return Optional.of(definition.hasTierRestriction()
                ? definition.maxTier().recipeTier()
                : MachineTier.LV);
    }

    public int kineticInputRpm() {
        if (level == null) {
            return 0;
        }

        int rpm = 0;
        for (BlockPos pos : sortedAbilityPositions(MultiblockAbility.KINETIC_INPUT)) {
            if (level.getBlockEntity(pos) instanceof MachinePortBlockEntity port) {
                rpm = Math.max(rpm, port.kineticRpm());
            }
        }
        return rpm;
    }

    private boolean kineticInputPowerReady() {
        if (level == null) {
            return false;
        }

        for (BlockPos pos : sortedAbilityPositions(MultiblockAbility.KINETIC_INPUT)) {
            if (level.getBlockEntity(pos) instanceof MachinePortBlockEntity port
                    && port.hasKineticInputPower()) {
                return true;
            }
        }
        return false;
    }

    public int recipeMinimumRpm() {
        return activeRecipe().flatMap(CERecipe::minRpm).orElse(0);
    }

    public int recipeMaximumRpm() {
        return activeRecipe().flatMap(CERecipe::maxRpm).orElse(0);
    }

    public boolean usesKineticInput() {
        MultiblockDefinition definition = currentDefinition();
        return definition != null && definition.drive() == MachineDrive.KINETIC;
    }

    public boolean usesKineticOutput() {
        MultiblockDefinition definition = currentDefinition();
        return definition != null && definition.drive() == MachineDrive.KINETIC_OUTPUT;
    }

    public Optional<Integer> kineticMinimumRpm() {
        MultiblockDefinition definition = currentDefinition();
        if (definition == null || definition.drive() != MachineDrive.KINETIC) {
            return Optional.empty();
        }
        return effectiveMinRpm(definition, activeRecipe());
    }

    public Optional<Integer> kineticMaximumRpm() {
        MultiblockDefinition definition = currentDefinition();
        if (definition == null || definition.drive() != MachineDrive.KINETIC) {
            return Optional.empty();
        }
        return effectiveMaxRpm(definition, activeRecipe());
    }

    public KineticRpmError kineticRpmError() {
        MultiblockDefinition definition = currentDefinition();
        if (definition == null || definition.drive() != MachineDrive.KINETIC) {
            return KineticRpmError.NONE;
        }

        int rpm = kineticInputRpm();
        int maximum = effectiveMaxRpm(definition, activeRecipe()).orElse(CERecipe.DEFAULT_MAX_RPM);
        if (rpm > maximum || rpm > CERecipe.DEFAULT_MAX_RPM) {
            return KineticRpmError.TOO_AGGRESSIVE;
        }

        int minimum = effectiveMinRpm(definition, activeRecipe()).orElse(1);
        return !kineticInputPowerReady() || rpm < minimum
                ? KineticRpmError.INSUFFICIENT
                : KineticRpmError.NONE;
    }

    public int kineticOutputRpm() {
        MultiblockDefinition definition = currentDefinition();
        CERecipeExecution execution = recipeLogic.execution();
        if (!machineEnabled
                || definition == null
                || definition.drive() != MachineDrive.KINETIC_OUTPUT
                || !recipeLogic.isActive()
                || execution == null) {
            return 0;
        }
        return Math.max(0, Math.min(
                CERecipe.DEFAULT_MAX_RPM,
                execution.machineData().getInt(KINETIC_OUTPUT_RPM_DATA)
        ));
    }

    private Optional<CERecipe> activeRecipe() {
        CERecipeExecution execution = recipeLogic.execution();
        return execution == null ? Optional.empty() : recipeById(execution.recipeId());
    }

    private static Optional<Integer> effectiveMinRpm(
            MultiblockDefinition definition,
            Optional<CERecipe> recipe
    ) {
        Optional<Integer> machine = definition.minRpm();
        Optional<Integer> recipeValue = recipe.flatMap(CERecipe::minRpm);
        if (machine.isPresent() && recipeValue.isPresent()) {
            return Optional.of(Math.max(machine.get(), recipeValue.get()));
        }
        return machine.isPresent() ? machine : recipeValue;
    }

    private static Optional<Integer> effectiveMaxRpm(
            MultiblockDefinition definition,
            Optional<CERecipe> recipe
    ) {
        Optional<Integer> machine = definition.maxRpm();
        Optional<Integer> recipeValue = recipe.flatMap(CERecipe::maxRpm);
        if (machine.isPresent() && recipeValue.isPresent()) {
            return Optional.of(Math.min(machine.get(), recipeValue.get()));
        }
        return machine.isPresent() ? machine : recipeValue;
    }

    private static int resolvedOutputRpm(
            MultiblockDefinition definition,
            CERecipe recipe
    ) {
        return recipe.outputRpm()
                .or(() -> definition.outputRpm())
                .orElse(0);
    }

    private static MachineTier runtimeTier(CERecipe recipe, CERecipeInput input) {
        return input.processingTier()
                .or(() -> recipe.requiredTier())
                .orElse(MachineTier.ULV);
    }

    private static int machineResourcePerTick(
            MultiblockDefinition definition,
            MachineTier runtimeTier
    ) {
        return switch (definition.drive()) {
            case ELECTRIC -> MachineTierStats.machineEnergyUsage(
                    definition.energyUsage(),
                    runtimeTier
            );
            case STEAM -> definition.steamUsage();
            case KINETIC, KINETIC_OUTPUT, NONE -> 0;
        };
    }

    private static boolean machineRpmMatches(MultiblockDefinition definition, int rpm) {
        if (definition.drive() != MachineDrive.KINETIC) {
            return true;
        }
        if (rpm < 1 || rpm > CERecipe.DEFAULT_MAX_RPM) {
            return false;
        }
        if (definition.minRpm().isPresent() && rpm < definition.minRpm().get()) {
            return false;
        }
        return definition.maxRpm().isEmpty() || rpm <= definition.maxRpm().get();
    }

    private static boolean recipeRpmMatches(CERecipe recipe, int rpm) {
        if (rpm < 1 || rpm > CERecipe.DEFAULT_MAX_RPM) {
            return false;
        }
        if (recipe.minRpm().isPresent() && rpm < recipe.minRpm().get()) {
            return false;
        }
        return recipe.maxRpm().isEmpty() || rpm <= recipe.maxRpm().get();
    }

    private static boolean rpmRangesCompatible(
            MultiblockDefinition definition,
            CERecipe recipe
    ) {
        if (definition.drive() != MachineDrive.KINETIC) {
            return true;
        }
        int minimum = effectiveMinRpm(definition, Optional.of(recipe)).orElse(1);
        int maximum = effectiveMaxRpm(definition, Optional.of(recipe)).orElse(CERecipe.DEFAULT_MAX_RPM);
        return minimum <= maximum;
    }

    private static int executionRpm(
            MultiblockDefinition definition,
            CERecipe recipe,
            int liveRpm
    ) {
        if (definition.drive() != MachineDrive.KINETIC
                || (machineRpmMatches(definition, liveRpm) && recipeRpmMatches(recipe, liveRpm))) {
            return liveRpm;
        }

        int minimum = effectiveMinRpm(definition, Optional.of(recipe)).orElse(1);
        int maximum = effectiveMaxRpm(definition, Optional.of(recipe)).orElse(CERecipe.DEFAULT_MAX_RPM);
        return Math.max(minimum, Math.min(maximum, Math.max(1, liveRpm)));
    }

    private boolean canProcessEnergy(int signedCEt) {
        return transferEnergy(signedCEt, true);
    }

    private boolean processEnergy(int signedCEt) {
        return transferEnergy(signedCEt, false);
    }

    private boolean transferEnergy(int signedCEt, boolean simulate) {
        long profileStart = CEPerformanceProfiler.begin(level);
        try {
            return transferEnergyInner(signedCEt, simulate);
        } finally {
            CEPerformanceProfiler.record(CEPerformanceProfiler.Metric.ENERGY_TRANSFER, profileStart);
        }
    }

    private boolean transferEnergyInner(int signedCEt, boolean simulate) {
        if (signedCEt == 0) {
            return true;
        }

        MultiblockAbility ability = signedCEt > 0 ? MultiblockAbility.ENERGY_INPUT : MultiblockAbility.ENERGY_OUTPUT;
        int remaining = Math.abs(signedCEt);
        for (BlockPos pos : abilityPositions(ability)) {
            if (level == null || !(level.getBlockEntity(pos) instanceof MachinePortBlockEntity port)) {
                continue;
            }

            CEEnergyContainer container = port.ceContainer();
            if (container == null) {
                continue;
            }

            long moved;
            if (signedCEt > 0) {
                long inputVoltage = port.displayInputVoltage();
                long portLimit = Math.max(0L, saturatedMultiply(inputVoltage, container.getInputAmperage()));
                if (portLimit <= 0) {
                    continue;
                }
                moved = container.extract(Math.min(remaining, portLimit), simulate);
                if (!simulate && moved > 0) {
                    port.recordEnergyInputLoad(moved, inputVoltage);
                }
            } else {
                moved = container.insert(remaining, simulate);
            }
            remaining -= (int) moved;
            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private static Fluid steamFluid() {
        FluidRegistry.RegisteredFluid steam =
                FluidRegistry.CHEMICAL_FLUIDS.get(IndustrialFluids.STEAM.registryName());
        return steam == null ? null : steam.source().get();
    }

    private boolean transferSteam(int amount, boolean simulate) {
        if (amount <= 0) {
            return true;
        }

        Fluid steam = steamFluid();
        if (steam == null) {
            return false;
        }

        int remaining = amount;
        FluidAction action = simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE;
        for (MachinePortBlockEntity port : fluidPorts(MultiblockAbility.FLUID_INPUT)) {
            boolean changed = false;
            for (FluidTank tank : port.fluidTanks()) {
                FluidStack drained = tank.drain(new FluidStack(steam, remaining), action);
                if (drained.isEmpty()) {
                    continue;
                }
                remaining -= drained.getAmount();
                changed = true;
                if (remaining <= 0) {
                    if (!simulate && changed) {
                        port.syncToClient();
                    }
                    return true;
                }
            }
            if (!simulate && changed) {
                port.syncToClient();
            }
        }
        return false;
    }

    private boolean canProcessMachineResource(MultiblockDefinition definition, int amount) {
        if (amount <= 0) {
            return true;
        }
        return switch (definition.drive()) {
            case ELECTRIC -> canProcessEnergy(amount);
            case STEAM -> transferSteam(amount, true);
            case KINETIC, KINETIC_OUTPUT, NONE -> true;
        };
    }

    private boolean processMachineResource(MultiblockDefinition definition, int amount) {
        if (amount <= 0) {
            return true;
        }
        return switch (definition.drive()) {
            case ELECTRIC -> canProcessEnergy(amount) && processEnergy(amount);
            case STEAM -> transferSteam(amount, true) && transferSteam(amount, false);
            case KINETIC, KINETIC_OUTPUT, NONE -> true;
        };
    }

    private List<MachinePortBlockEntity> itemPorts(MultiblockAbility ability) {
        if (level == null) {
            return List.of();
        }

        List<MachinePortBlockEntity> ports = new ArrayList<>();
        for (BlockPos pos : sortedAbilityPositions(ability)) {
            if (level.getBlockEntity(pos) instanceof MachinePortBlockEntity port && port.items().getSlots() > 0) {
                ports.add(port);
            }
        }
        return ports;
    }

    private List<MachinePortBlockEntity> fluidPorts(MultiblockAbility ability) {
        if (level == null) {
            return List.of();
        }

        List<MachinePortBlockEntity> ports = new ArrayList<>();
        for (BlockPos pos : sortedAbilityPositions(ability)) {
            if (level.getBlockEntity(pos) instanceof MachinePortBlockEntity port && !port.fluidTanks().isEmpty()) {
                ports.add(port);
            }
        }
        return ports;
    }

    private List<MachinePortBlockEntity> outputPorts(MultiblockAbility ability, InputRoute route) {
        List<MachinePortBlockEntity> ports = ability == MultiblockAbility.ITEM_OUTPUT ? itemPorts(ability) : fluidPorts(ability);
        if (!route.usesColor()) {
            return ports;
        }
        return ports.stream()
                .filter(port -> MachinePortBlockEntity.colorsConnect(routeColor(port), route.color()))
                .toList();
    }

    private static List<InputRoute> inputRoutes(List<MachinePortBlockEntity> itemPorts, List<MachinePortBlockEntity> fluidPorts) {
        List<InputRoute> routes = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        List<MachinePortBlockEntity> allPorts = new ArrayList<>();
        allPorts.addAll(itemPorts);
        allPorts.addAll(fluidPorts);
        if (allPorts.isEmpty()) {
            routes.add(new InputRoute(Optional.empty(), DyeColor.GRAY, false, itemPorts, fluidPorts));
            return routes;
        }

        for (MachinePortBlockEntity port : allPorts) {
            int circuit = port.circuit();
            DyeColor color = routeColor(port);
            String key = circuit + ":" + color.getId();
            if (!seen.add(key)) {
                continue;
            }

            List<MachinePortBlockEntity> routeItemPorts = itemPorts.stream()
                    .filter(candidate -> candidate.circuit() == circuit && inputColorsConnect(routeColor(candidate), color))
                    .toList();
            List<MachinePortBlockEntity> routeFluidPorts = fluidPorts.stream()
                    .filter(candidate -> candidate.circuit() == circuit && inputColorsConnect(routeColor(candidate), color))
                    .toList();
            routes.add(new InputRoute(circuit > 0 ? Optional.of(circuit) : Optional.empty(), color, true, routeItemPorts, routeFluidPorts));
        }
        return routes;
    }

    private static boolean inputColorsConnect(DyeColor inputColor, DyeColor routeColor) {
        return routeColor == DyeColor.GRAY ? inputColor == DyeColor.GRAY : MachinePortBlockEntity.colorsConnect(inputColor, routeColor);
    }

    private static DyeColor routeColor(MachinePortBlockEntity port) {
        return port.supportsIoColor() ? port.ioColor() : DyeColor.GRAY;
    }

    private List<BlockPos> sortedAbilityPositions(MultiblockAbility ability) {
        return sortedAbilityPositions.getOrDefault(ability, List.of());
    }

    private static Map<MultiblockAbility, List<BlockPos>> sortAbilityPositions(
            Map<MultiblockAbility, List<BlockPos>> positionsByAbility
    ) {
        Map<MultiblockAbility, List<BlockPos>> sorted = new EnumMap<>(MultiblockAbility.class);
        positionsByAbility.forEach((ability, positions) -> sorted.put(ability, sortedPositions(positions)));
        return sorted;
    }

    private Set<BlockPos> collectFormedPartPositions(List<BlockPos> positions) {
        if (level == null) return Set.of();
        Set<BlockPos> parts = new HashSet<>();
        for (BlockPos pos : positions) {
            if (!pos.equals(worldPosition) && level.getBlockEntity(pos) instanceof MultiblockPart) {
                parts.add(pos);
            }
        }
        return Set.copyOf(parts);
    }

    private static List<BlockPos> sortedPositions(List<BlockPos> positions) {
        return positions.stream()
                .sorted(Comparator.comparingInt((BlockPos pos) -> pos.getX())
                        .thenComparingInt(pos -> pos.getY())
                        .thenComparingInt(pos -> pos.getZ()))
                .toList();
    }

    private static List<ItemStack> itemStacks(List<MachinePortBlockEntity> ports) {
        List<ItemStack> stacks = new ArrayList<>();
        for (MachinePortBlockEntity port : ports) {
            ItemStackHandler handler = port.items();
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    stacks.add(stack.copy());
                }
            }
        }
        return stacks;
    }

    private static List<FluidStack> fluidStacks(List<MachinePortBlockEntity> ports) {
        List<FluidStack> stacks = new ArrayList<>();
        for (MachinePortBlockEntity port : ports) {
            for (FluidTank tank : port.fluidTanks()) {
                FluidStack stack = tank.getFluid();
                if (!stack.isEmpty()) {
                    stacks.add(stack.copy());
                }
            }
        }
        return stacks;
    }

    private FanInputStats fanInputStats(MultiblockDefinition definition) {
        if (level == null) {
            return FanInputStats.NONE;
        }
        long gameTime = level.getGameTime();
        if (fanInputCacheTick != Long.MIN_VALUE && gameTime - fanInputCacheTick < 5L) {
            return fanInputCache;
        }
        BlockState state = getBlockState();
        if (!state.hasProperty(MultiblockControllerBlock.FACING)) {
            fanInputCacheTick = gameTime;
            fanInputCache = FanInputStats.NONE;
            return fanInputCache;
        }

        Direction facing = state.getValue(MultiblockControllerBlock.FACING);
        int smoking = 0;
        int blasting = 0;
        for (BlockPos pos : definition.worldInteractionPositions(
                worldPosition,
                facing,
                MultiblockDefinition.WorldInteractionType.FAN_INPUT
        )) {
            FanProcessingType type = fanProcessingAt(pos);
            if (type == AllFanProcessingTypes.SMOKING) {
                smoking++;
            } else if (type == AllFanProcessingTypes.BLASTING) {
                blasting++;
            }
        }
        fanInputCacheTick = gameTime;
        fanInputCache = new FanInputStats(smoking, blasting);
        return fanInputCache;
    }

    @Nullable
    private FanProcessingType fanProcessingAt(BlockPos targetPos) {
        if (level == null) {
            return null;
        }
        for (Direction searchDirection : Direction.values()) {
            for (int distance = 1; distance <= 32; distance++) {
                BlockPos sourcePos = targetPos.relative(searchDirection, distance);
                BlockEntity blockEntity = level.getBlockEntity(sourcePos);
                if (!(blockEntity instanceof IAirCurrentSource source)) {
                    continue;
                }
                AirCurrent airCurrent = source.getAirCurrent();
                if (airCurrent == null || airCurrent.direction == null) {
                    continue;
                }

                BlockPos airCurrentPos = source.getAirCurrentPos();
                int dx = targetPos.getX() - airCurrentPos.getX();
                int dy = targetPos.getY() - airCurrentPos.getY();
                int dz = targetPos.getZ() - airCurrentPos.getZ();
                Direction direction = airCurrent.direction;
                int blockDistance = dx * direction.getStepX()
                        + dy * direction.getStepY()
                        + dz * direction.getStepZ();
                if (blockDistance <= 0) {
                    continue;
                }

                int lateralX = dx - blockDistance * direction.getStepX();
                int lateralY = dy - blockDistance * direction.getStepY();
                int lateralZ = dz - blockDistance * direction.getStepZ();
                if (lateralX != 0 || lateralY != 0 || lateralZ != 0) {
                    continue;
                }

                FanProcessingType type = airCurrent.getTypeAt(blockDistance - 1.0F);
                if (type != null) {
                    return type;
                }
            }
        }
        return null;
    }

    private int applyMufflerAndFanDuration(
            int duration,
            FanInputStats fanStats
    ) {
        float mufflerMultiplier = lowestPortTier(MultiblockAbility.MUFFLER)
                .map(MachineTierStats::mufflerDurationMultiplier)
                .orElse(1.0F);
        float speedDivisor = 1.0F + fanStats.speedBonus();
        return Math.max(1, Math.round(duration * mufflerMultiplier / speedDivisor));
    }

    private record FanInputStats(int smoking, int blasting) {
        private static final FanInputStats NONE = new FanInputStats(0, 0);

        private int activeInputs() {
            return smoking + blasting;
        }

        private float speedBonus() {
            float raw = smoking * 0.05F + blasting * 0.10F - 0.05F;
            return Math.max(0.0F, raw);
        }
    }

    private List<ItemEntity> worldInputEntities(MultiblockDefinition definition) {
        if (level == null) {
            return List.of();
        }

        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof MultiblockControllerBlock)
                || !state.hasProperty(MultiblockControllerBlock.FACING)) {
            return List.of();
        }

        Direction facing = state.getValue(MultiblockControllerBlock.FACING);
        List<ItemEntity> entities = new ArrayList<>();
        Set<Integer> seenEntityIds = new HashSet<>();

        for (BlockPos inputPos : definition.worldInteractionPositions(
                worldPosition,
                facing,
                MultiblockDefinition.WorldInteractionType.INPUT
        )) {
            for (ItemEntity entity : level.getEntitiesOfClass(
                    ItemEntity.class,
                    new AABB(inputPos),
                    entity -> entity.isAlive() && !entity.getItem().isEmpty()
            )) {
                if (seenEntityIds.add(entity.getId())) {
                    entities.add(entity);
                }
            }
        }

        return entities;
    }

    private static List<ItemStack> worldItemStacks(List<ItemEntity> entities) {
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemEntity entity : entities) {
            ItemStack stack = entity.getItem();
            if (!stack.isEmpty()) {
                stacks.add(stack.copy());
            }
        }
        return stacks;
    }

    private static boolean consumeInputs(
            List<MachinePortBlockEntity> ports,
            List<ItemEntity> worldEntities,
            CERecipe recipe,
            int multiplier,
            Optional<MachineTier> runtimeTier,
            RandomSource random
    ) {
        return consumeInputs(ports, worldEntities, recipe, multiplier, runtimeTier, random, false);
    }

    private static boolean canConsumeInputs(
            List<MachinePortBlockEntity> ports,
            List<ItemEntity> worldEntities,
            CERecipe recipe,
            int multiplier,
            Optional<MachineTier> runtimeTier
    ) {
        return consumeInputs(ports, worldEntities, recipe, multiplier, runtimeTier, null, true);
    }

    private static boolean consumeInputs(
            List<MachinePortBlockEntity> ports,
            List<ItemEntity> worldEntities,
            CERecipe recipe,
            int multiplier,
            Optional<MachineTier> runtimeTier,
            @Nullable RandomSource random,
            boolean simulate
    ) {
        List<ItemStack> simulatedPortStacks = new ArrayList<>();
        List<ItemStack> simulatedWorldStacks = new ArrayList<>();

        if (simulate) {
            for (MachinePortBlockEntity port : ports) {
                ItemStackHandler handler = port.items();
                for (int slot = 0; slot < handler.getSlots(); slot++) {
                    simulatedPortStacks.add(handler.getStackInSlot(slot).copy());
                }
            }
            for (ItemEntity entity : worldEntities) {
                simulatedWorldStacks.add(entity.getItem().copy());
            }
        }

        for (SizedIngredient input : recipe.itemInputs()) {
            int remaining = multiplyClamped(input.count(), multiplier);

            if (simulate) {
                remaining = consumeFromStacks(simulatedPortStacks, input, remaining);
                remaining = consumeFromStacks(simulatedWorldStacks, input, remaining);
            } else {
                remaining = consumeFromPorts(ports, input, remaining);
                remaining = consumeFromWorldEntities(worldEntities, input, remaining);
            }

            if (remaining > 0) {
                return false;
            }
        }

        for (CEChancedItemInput input : recipe.chancedItemInputs()) {
            int remaining = simulate
                    ? multiplyClamped(input.ingredient().count(), multiplier)
                    : rolledChancedInputCount(input, multiplier, runtimeTier, recipe.requiredTier(), random);
            if (remaining <= 0) {
                continue;
            }

            if (simulate) {
                remaining = consumeFromStacks(simulatedPortStacks, input.ingredient().ingredient(), remaining);
                remaining = consumeFromStacks(simulatedWorldStacks, input.ingredient().ingredient(), remaining);
            } else {
                remaining = consumeFromPorts(ports, input.ingredient().ingredient(), remaining);
                remaining = consumeFromWorldEntities(worldEntities, input.ingredient().ingredient(), remaining);
            }

            if (remaining > 0) {
                return false;
            }
        }

        return true;
    }

    private boolean canConsumeSequencedInputs(CERecipe recipe, int multiplier) {
        return sequencedItemInputsMatch(recipe, multiplier);
    }

    private Optional<List<ItemStack>> consumeSequencedInputs(CERecipe recipe, int multiplier) {
        if (!canConsumeSequencedInputs(recipe, multiplier)) {
            return Optional.empty();
        }

        List<ItemStack> consumed = new ArrayList<>();
        SizedIngredient baseInput = recipe.itemInputs().getFirst();
        int remaining = consumeFromPortsRecording(
                baseItemInputPorts(),
                baseInput,
                multiplyClamped(baseInput.count(), multiplier),
                consumed
        );
        if (remaining > 0) {
            return Optional.empty();
        }

        for (int index = 1; index < recipe.itemInputs().size(); index++) {
            Optional<MachinePortBlockEntity> port = sequentialInputPort(index);
            if (port.isEmpty()) {
                return Optional.empty();
            }
            SizedIngredient input = recipe.itemInputs().get(index);
            remaining = consumeFromPortsRecording(
                    List.of(port.get()),
                    input,
                    multiplyClamped(input.count(), multiplier),
                    consumed
            );
            if (remaining > 0) {
                return Optional.empty();
            }
        }
        return Optional.of(List.copyOf(consumed));
    }

    private static int rolledChancedInputCount(
            CEChancedItemInput input,
            int multiplier,
            Optional<MachineTier> runtimeTier,
            Optional<MachineTier> baselineTier,
            @Nullable RandomSource random
    ) {
        int rolls = multiplyClamped(input.ingredient().count(), multiplier);
        int chance = input.effectiveChance(runtimeTier, baselineTier);
        if (chance >= CEChancedItemInput.MAX_CHANCE) {
            return rolls;
        }
        if (chance <= 0 || random == null) {
            return 0;
        }

        int amount = 0;
        for (int i = 0; i < rolls; i++) {
            if (random.nextInt(CEChancedItemInput.MAX_CHANCE) < chance) {
                amount++;
            }
        }
        return amount;
    }

    private static int consumeFromStacks(
            List<ItemStack> stacks,
            SizedIngredient input,
            int remaining
    ) {
        return consumeFromStacks(stacks, input.ingredient(), remaining);
    }

    private static int consumeFromStacks(
            List<ItemStack> stacks,
            Ingredient ingredient,
            int remaining
    ) {
        for (ItemStack stack : stacks) {
            if (remaining <= 0) {
                break;
            }
            if (stack.isEmpty() || !ingredient.test(stack)) {
                continue;
            }

            int taken = Math.min(remaining, stack.getCount());
            stack.shrink(taken);
            remaining -= taken;
        }
        return remaining;
    }

    private static int consumeFromPorts(
            List<MachinePortBlockEntity> ports,
            SizedIngredient input,
            int remaining
    ) {
        return consumeFromPorts(ports, input.ingredient(), remaining);
    }

    private static int consumeFromPorts(
            List<MachinePortBlockEntity> ports,
            Ingredient ingredient,
            int remaining
    ) {
        for (MachinePortBlockEntity port : ports) {
            ItemStackHandler handler = port.items();
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                if (remaining <= 0) {
                    return 0;
                }

                ItemStack stack = handler.getStackInSlot(slot);
                if (stack.isEmpty() || !ingredient.test(stack)) {
                    continue;
                }

                int taken = Math.min(remaining, stack.getCount());
                stack.shrink(taken);
                handler.setStackInSlot(slot, stack);
                remaining -= taken;
            }
        }
        return remaining;
    }

    private static int consumeFromPortsRecording(
            List<MachinePortBlockEntity> ports,
            SizedIngredient input,
            int remaining,
            List<ItemStack> consumed
    ) {
        Ingredient ingredient = input.ingredient();
        for (MachinePortBlockEntity port : ports) {
            ItemStackHandler handler = port.items();
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                if (remaining <= 0) {
                    return 0;
                }

                ItemStack stack = handler.getStackInSlot(slot);
                if (stack.isEmpty() || !ingredient.test(stack)) {
                    continue;
                }

                int taken = Math.min(remaining, stack.getCount());
                consumed.add(stack.copyWithCount(taken));
                stack.shrink(taken);
                handler.setStackInSlot(slot, stack);
                remaining -= taken;
            }
        }
        return remaining;
    }

    private static int consumeFromWorldEntities(
            List<ItemEntity> entities,
            SizedIngredient input,
            int remaining
    ) {
        return consumeFromWorldEntities(entities, input.ingredient(), remaining);
    }

    private static int consumeFromWorldEntities(
            List<ItemEntity> entities,
            Ingredient ingredient,
            int remaining
    ) {
        for (ItemEntity entity : entities) {
            if (remaining <= 0) {
                return 0;
            }

            ItemStack stack = entity.getItem();
            if (stack.isEmpty() || !ingredient.test(stack)) {
                continue;
            }

            int taken = Math.min(remaining, stack.getCount());
            stack.shrink(taken);
            remaining -= taken;

            if (stack.isEmpty()) {
                entity.discard();
            } else {
                entity.setItem(stack);
            }
        }
        return remaining;
    }

    private static boolean consumeFluidInputs(
            List<MachinePortBlockEntity> ports,
            CERecipe recipe,
            int multiplier,
            Optional<MachineTier> runtimeTier,
            RandomSource random
    ) {
        return consumeFluidInputs(ports, recipe, multiplier, runtimeTier, random, FluidAction.EXECUTE);
    }

    private static boolean canConsumeFluidInputs(
            List<MachinePortBlockEntity> ports,
            CERecipe recipe,
            int multiplier,
            Optional<MachineTier> runtimeTier
    ) {
        return consumeFluidInputs(ports, recipe, multiplier, runtimeTier, null, FluidAction.SIMULATE);
    }

    private static boolean consumeFluidInputs(
            List<MachinePortBlockEntity> ports,
            CERecipe recipe,
            int multiplier,
            Optional<MachineTier> runtimeTier,
            @Nullable RandomSource random,
            FluidAction action
    ) {
        for (SizedFluidIngredient input : recipe.fluidInputs()) {
            if (!drainFluidRequirement(ports, input, multiplyClamped(input.amount(), multiplier), action)) return false;
        }
        for (CEChancedFluidInput input : recipe.chancedFluidInputs()) {
            int amount;
            if (action == FluidAction.SIMULATE) {
                amount = multiplyClamped(input.ingredient().amount(), multiplier);
            } else {
                amount = 0;
                int chance = input.effectiveChance(runtimeTier, recipe.requiredTier());
                for (int run = 0; run < Math.max(1, multiplier); run++) {
                    if (chance >= CEChancedFluidInput.MAX_CHANCE || random.nextInt(CEChancedFluidInput.MAX_CHANCE) < chance) {
                        amount = Math.min(Integer.MAX_VALUE, amount + input.ingredient().amount());
                    }
                }
            }
            if (amount > 0 && !drainFluidRequirement(ports, input.ingredient(), amount, action)) return false;
        }
        return true;
    }

    private static boolean drainFluidRequirement(
            List<MachinePortBlockEntity> ports,
            SizedFluidIngredient input,
            int amount,
            FluidAction action
    ) {
        int remaining = amount;
        for (MachinePortBlockEntity port : ports) {
            for (FluidTank tank : port.fluidTanks()) {
                FluidStack stack = tank.getFluid();
                if (stack.isEmpty() || !input.ingredient().test(stack)) continue;
                FluidStack drained = tank.drain(remaining, action);
                remaining -= drained.getAmount();
                if (remaining <= 0) return true;
            }
        }
        return remaining <= 0;
    }

    private boolean canRouteItemOutputs(
            MultiblockDefinition definition,
            List<MachinePortBlockEntity> outputPorts,
            List<ItemStack> outputs
    ) {
        if (outputs.isEmpty()) {
            return true;
        }

        /*
         * En eksisterende output bus har absolutt prioritet.
         * Hvis den finnes, må alle outputs få plass der.
         */
        if (!outputPorts.isEmpty()) {
            return canFitItemOutputs(outputPorts, outputs);
        }

        /*
         * World output brukes bare når multiblocken ikke har
         * noen gyldig output bus for denne ruten.
         */
        return !worldOutputPositions(definition).isEmpty();
    }

    private List<BlockPos> worldOutputPositions(MultiblockDefinition definition) {
        BlockState state = getBlockState();
        if (!state.hasProperty(MultiblockControllerBlock.FACING)) {
            return List.of();
        }

        Direction facing = state.getValue(MultiblockControllerBlock.FACING);
        return definition.worldInteractionPositions(
                worldPosition,
                facing,
                MultiblockDefinition.WorldInteractionType.OUTPUT
        );
    }

    private void dropItemOutputsInWorld(
            MultiblockDefinition definition,
            List<ItemStack> outputs
    ) {
        if (level == null || outputs.isEmpty()) {
            return;
        }

        List<BlockPos> positions = worldOutputPositions(definition);
        if (positions.isEmpty()) {
            return;
        }

        int index = 0;
        for (ItemStack output : outputs) {
            if (output.isEmpty()) {
                continue;
            }

            BlockPos pos = positions.get(index % positions.size());
            ItemEntity entity = new ItemEntity(
                    level,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.5D,
                    pos.getZ() + 0.5D,
                    output.copy()
            );
            entity.setDefaultPickUpDelay();
            entity.setDeltaMovement(0.0D, 0.0D, 0.0D);
            level.addFreshEntity(entity);
            index++;
        }
    }

    private static boolean canFitItemOutputs(List<MachinePortBlockEntity> ports, List<ItemStack> outputs) {
        return insertItemOutputs(ports, outputs, true);
    }

    private static boolean insertItemOutputs(List<MachinePortBlockEntity> ports, List<ItemStack> outputs, boolean simulate) {
        for (ItemStack output : outputs) {
            ItemStack remaining = output.copy();
            for (MachinePortBlockEntity port : ports) {
                ItemStackHandler handler = port.items();
                for (int slot = 0; slot < handler.getSlots(); slot++) {
                    remaining = handler.insertItem(slot, remaining, simulate);
                    if (remaining.isEmpty()) {
                        break;
                    }
                }
                if (remaining.isEmpty()) {
                    break;
                }
            }

            if (!remaining.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean canFitFluidOutputs(List<MachinePortBlockEntity> ports, List<FluidStack> outputs) {
        if (outputs.isEmpty()) {
            return true;
        }
        if (ports.isEmpty()) {
            return false;
        }

        List<SimulatedTank> tanks = new ArrayList<>();
        for (MachinePortBlockEntity port : ports) {
            for (FluidTank tank : port.fluidTanks()) {
                tanks.add(new SimulatedTank(tank.getFluid().copy(), tank.getCapacity()));
            }
        }

        for (FluidStack output : outputs) {
            FluidStack remaining = output.copy();
            for (SimulatedTank tank : tanks) {
                remaining = tank.fill(remaining);
                if (remaining.isEmpty()) {
                    break;
                }
            }
            if (!remaining.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean insertFluidOutputs(List<MachinePortBlockEntity> ports, List<FluidStack> outputs, boolean simulate) {
        for (FluidStack output : outputs) {
            FluidStack remaining = output.copy();
            for (MachinePortBlockEntity port : ports) {
                for (FluidTank tank : port.fluidTanks()) {
                    int filled = tank.fill(remaining, simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE);
                    remaining.shrink(filled);
                    if (remaining.isEmpty()) {
                        break;
                    }
                }
                if (remaining.isEmpty()) {
                    break;
                }
            }
            if (!remaining.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static List<ItemStack> guaranteedItemOutputs(CERecipe recipe) {
        return recipe.itemOutputs().stream()
                .filter(output -> output.guaranteed() && !output.stack().isEmpty())
                .map(output -> output.stack().copy())
                .toList();
    }

    private static List<FluidStack> possibleFluidOutputs(CERecipe recipe) {
        List<FluidStack> outputs = recipe.fluidOutputs().stream().filter(output -> !output.isEmpty()).map(FluidStack::copy).collect(Collectors.toCollection(ArrayList::new));
        recipe.chancedFluidOutputs().stream().map(output -> output.stack().copy()).forEach(outputs::add);
        return outputs;
    }

    private List<FluidStack> rollFluidOutputs(CERecipe recipe, int parallel, Optional<MachineTier> runtimeTier) {
        if (level == null) return List.of();
        List<FluidStack> outputs = new ArrayList<>();
        for (int run = 0; run < Math.max(1, parallel); run++) {
            recipe.fluidOutputs().stream().filter(output -> !output.isEmpty()).map(FluidStack::copy).forEach(outputs::add);
            for (CEChancedFluidOutput output : recipe.chancedFluidOutputs()) {
                int chance = output.effectiveChance(runtimeTier, recipe.requiredTier());
                if (chance >= CEChancedFluidOutput.MAX_CHANCE || level.random.nextInt(CEChancedFluidOutput.MAX_CHANCE) < chance) {
                    outputs.add(output.stack().copy());
                }
            }
        }
        return outputs;
    }

    private SequentialFluidOutputs rollSequentialFluidOutputs(
            CERecipe recipe,
            int parallel,
            Optional<MachineTier> runtimeTier
    ) {
        if (level == null) {
            return SequentialFluidOutputs.empty();
        }

        int safeParallel = Math.max(1, parallel);
        List<FluidStack> outputs = new ArrayList<>();
        List<Integer> indexes = new ArrayList<>();
        int outputIndex = 1;

        for (FluidStack output : recipe.fluidOutputs()) {
            if (!output.isEmpty()) {
                outputs.add(output.copyWithAmount(multiplyClamped(output.getAmount(), safeParallel)));
                indexes.add(outputIndex);
            }
            outputIndex++;
        }

        for (CEChancedFluidOutput output : recipe.chancedFluidOutputs()) {
            int chance = output.effectiveChance(runtimeTier, recipe.requiredTier());
            int successes = 0;
            for (int run = 0; run < safeParallel; run++) {
                if (chance >= CEChancedFluidOutput.MAX_CHANCE
                        || level.random.nextInt(CEChancedFluidOutput.MAX_CHANCE) < chance) {
                    successes++;
                }
            }
            if (successes > 0 && !output.stack().isEmpty()) {
                outputs.add(output.stack().copyWithAmount(multiplyClamped(output.stack().getAmount(), successes)));
                indexes.add(outputIndex);
            }
            outputIndex++;
        }

        return new SequentialFluidOutputs(outputs, indexes.stream().mapToInt(Integer::intValue).toArray());
    }

    private boolean canFitSequentialFluidOutputs(SequentialFluidOutputs routedOutputs) {
        if (routedOutputs.outputs().size() != routedOutputs.indexes().length) {
            return false;
        }
        for (int i = 0; i < routedOutputs.outputs().size(); i++) {
            Optional<MachinePortBlockEntity> port = sequentialOutputPort(routedOutputs.indexes()[i]);
            if (port.isEmpty()
                    || !canFitFluidOutputs(List.of(port.get()), List.of(routedOutputs.outputs().get(i)))) {
                return false;
            }
        }
        return true;
    }

    private boolean insertSequentialFluidOutputs(SequentialFluidOutputs routedOutputs, boolean simulate) {
        if (routedOutputs.outputs().size() != routedOutputs.indexes().length) {
            return false;
        }
        for (int i = 0; i < routedOutputs.outputs().size(); i++) {
            Optional<MachinePortBlockEntity> port = sequentialOutputPort(routedOutputs.indexes()[i]);
            if (port.isEmpty()
                    || !insertFluidOutputs(List.of(port.get()), List.of(routedOutputs.outputs().get(i)), simulate)) {
                return false;
            }
        }
        return true;
    }

    private static SequentialFluidOutputs sequentialFluidOutputs(CERecipeExecution execution) {
        return new SequentialFluidOutputs(
                execution.fluidOutputs(),
                execution.machineData().getIntArray(SEQUENTIAL_FLUID_OUTPUT_INDEXES_DATA)
        );
    }

    private static List<ItemStack> displayItemInputs(CERecipe recipe) {
        List<ItemStack> stacks = new ArrayList<>();
        for (SizedIngredient input : recipe.itemInputs()) {
            ItemStack[] candidates = input.getItems();
            if (candidates.length == 0) {
                continue;
            }
            stacks.add(candidates[0].copyWithCount(input.count()));
        }
        for (CEChancedItemInput input : recipe.chancedItemInputs()) {
            ItemStack[] candidates = input.ingredient().getItems();
            if (candidates.length > 0) stacks.add(candidates[0].copyWithCount(input.ingredient().count()));
        }
        for (SizedIngredient input : recipe.notConsumableItems()) {
            ItemStack[] candidates = input.getItems();
            if (candidates.length > 0) stacks.add(candidates[0].copyWithCount(input.count()));
        }
        return stacks;
    }

    private static List<ItemStack> displaySequencedItemInputs(CERecipe recipe) {
        List<ItemStack> stacks = new ArrayList<>();
        for (SizedIngredient input : recipe.itemInputs()) {
            ItemStack[] candidates = input.getItems();
            if (candidates.length == 0 || candidates[0].isEmpty()) {
                continue;
            }

            ItemStack candidate = candidates[0];
            int remaining = input.count();
            int maxStackSize = Math.max(1, candidate.getMaxStackSize());
            while (remaining > 0) {
                int amount = Math.min(remaining, maxStackSize);
                stacks.add(candidate.copyWithCount(amount));
                remaining -= amount;
            }
        }
        return stacks;
    }

    private static List<FluidStack> displayFluidInputs(CERecipe recipe) {
        List<FluidStack> stacks = new ArrayList<>();
        for (SizedFluidIngredient input : recipe.fluidInputs()) {
            FluidStack[] candidates = input.getFluids();
            if (candidates.length > 0) stacks.add(candidates[0].copy());
        }
        for (CEChancedFluidInput input : recipe.chancedFluidInputs()) {
            FluidStack[] candidates = input.ingredient().getFluids();
            if (candidates.length > 0) stacks.add(candidates[0].copy());
        }
        for (SizedFluidIngredient input : recipe.notConsumableFluids()) {
            FluidStack[] candidates = input.getFluids();
            if (candidates.length > 0) stacks.add(candidates[0].copy());
        }
        return stacks;
    }

    private static List<ItemStack> copyItems(List<ItemStack> stacks) {
        return stacks.stream()
                .filter(stack -> !stack.isEmpty())
                .map(ItemStack::copy)
                .toList();
    }

    private static List<FluidStack> copyFluids(List<FluidStack> stacks) {
        return stacks.stream()
                .filter(stack -> !stack.isEmpty())
                .map(FluidStack::copy)
                .toList();
    }

    private static List<ItemStack> multiplyItems(List<ItemStack> stacks, int multiplier) {
        int safeMultiplier = Math.max(1, multiplier);
        return stacks.stream()
                .filter(stack -> !stack.isEmpty())
                .map(stack -> {
                    ItemStack copy = stack.copy();
                    copy.setCount(multiplyClamped(copy.getCount(), safeMultiplier));
                    return copy;
                })
                .toList();
    }

    private static List<FluidStack> multiplyFluids(List<FluidStack> stacks, int multiplier) {
        int safeMultiplier = Math.max(1, multiplier);
        return stacks.stream()
                .filter(stack -> !stack.isEmpty())
                .map(stack -> stack.copyWithAmount(multiplyClamped(stack.getAmount(), safeMultiplier)))
                .toList();
    }

    private static int multiplyClamped(int value, int multiplier) {
        long result = (long) Math.max(0, value) * Math.max(1, multiplier);
        return (int) Math.min(Integer.MAX_VALUE, result);
    }

    private void clearActiveRecipe() {
        recipeProgress = 0;
        recipeDuration = 0;
        activeCEt = 0;
        activeSyncCooldown = 0;
    }

    private void detachParts() {
        if (level == null) {
            return;
        }

        for (BlockPos partPos : formedPositions) {
            if (partPos.equals(worldPosition)) {
                continue;
            }

            if (level.getBlockEntity(partPos) instanceof MultiblockPart part && worldPosition.equals(part.controllerPos())) {
                if (part instanceof MachinePortBlockEntity port) {
                    port.setAssembledOverlayModel(null);
                }
                part.detachFromMultiblock();
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Formed", formed);
        tag.putBoolean("MachineEnabled", machineEnabled);
        tag.putString("FormedVariant", formedVariant);
        tag.putInt("VariantLevel", variantLevel);
        if (formedTier != null) {
            tag.putString("FormedTier", formedTier.id());
        }
        tag.putInt("FormedCoilHeat", formedCoilHeat);
        tag.putInt("FormedCoilCount", formedCoilCount);
        tag.putInt("RecipeProgress", recipeProgress);
        tag.putInt("RecipeDuration", recipeDuration);
        tag.putInt("ActiveCEt", activeCEt);
        tag.putInt("ExternalCEt", externalCEt);
        tag.putBoolean(CB_HATCH_PRESENT_DATA, cbHatchPresent);
        tag.putLong(CB_VALUE_DATA, chemicalBalanceTenThousandths);
        if (machineDurabilityHundredths >= 0L) {
            tag.putLong(MACHINE_DURABILITY_DATA, machineDurabilityHundredths);
        }
        if (preferredRecipeId != null) {
            tag.putString("PreferredRecipe", preferredRecipeId.toString());
        }
        interactionWear.save(tag);
        recipeLogic.save(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        formed = tag.getBoolean("Formed");
        machineEnabled = !tag.contains("MachineEnabled") || tag.getBoolean("MachineEnabled");
        formedVariant = tag.getString("FormedVariant");
        variantLevel = tag.getInt("VariantLevel");
        formedTier = null;
        if (tag.contains("FormedTier")) {
            String tierId = tag.getString("FormedTier");
            for (MachineTier tier : MachineTier.ALL) {
                if (tier.id().equals(tierId)) {
                    formedTier = tier;
                    break;
                }
            }
        }
        formedCoilHeat = tag.getInt("FormedCoilHeat");
        formedCoilCount = tag.getInt("FormedCoilCount");
        recipeProgress = tag.getInt("RecipeProgress");
        recipeDuration = tag.getInt("RecipeDuration");
        activeCEt = tag.getInt("ActiveCEt");
        externalCEt = tag.getInt("ExternalCEt");
        cbHatchPresent = tag.contains(CB_HATCH_PRESENT_DATA)
                ? tag.getBoolean(CB_HATCH_PRESENT_DATA)
                : tag.getBoolean(LEGACY_PH_HATCH_PRESENT_DATA);
        if (tag.contains(CB_VALUE_DATA)) {
            chemicalBalanceTenThousandths = clampChemicalBalance(tag.getLong(CB_VALUE_DATA));
        } else if (tag.contains(LEGACY_PH_VALUE_DATA)) {
            chemicalBalanceTenThousandths = legacyPhToChemicalBalance(tag.getLong(LEGACY_PH_VALUE_DATA));
        } else {
            loadLegacyChemicalBalanceFromPh(tag);
        }
        machineDurabilityHundredths = tag.contains(MACHINE_DURABILITY_DATA)
                ? Math.max(0L, tag.getLong(MACHINE_DURABILITY_DATA))
                : -1L;
        preferredRecipeId = tag.contains("PreferredRecipe") ? ResourceLocation.parse(tag.getString("PreferredRecipe")) : null;
        interactionWear.load(tag);
        recipeLogic.load(tag, registries);
        dirty = true;
    }


    private void loadLegacyChemicalBalanceFromPh(CompoundTag tag) {
        long legacyWeightMb = Math.max(0L, tag.getLong(LEGACY_PH_WEIGHT_DATA));
        long legacyWeightedHundredthsMb = Math.max(0L, tag.getLong(LEGACY_PH_WEIGHTED_SUM_DATA));
        if (legacyWeightMb <= 0L) {
            resetChemicalBalanceState();
            return;
        }

        long scaledSum = saturatedMultiply(legacyWeightedHundredthsMb, CB_UNITS_PER_HUNDREDTH);
        long roundedLegacyPh = saturatedAdd(scaledSum, legacyWeightMb / 2L) / legacyWeightMb;
        chemicalBalanceTenThousandths = legacyPhToChemicalBalance(roundedLegacyPh);
    }

    private static long legacyPhToChemicalBalance(long legacyPhTenThousandths) {
        double ph = legacyPhTenThousandths / 10_000.0D;
        double cb = (7.0D - ph) / 7.0D * 100.0D;
        long cbTenThousandths = Math.round(cb * 10_000.0D);
        return clampChemicalBalance(cbTenThousandths);
    }

    private static long saturatedMultiply(long first, long second) {
        if (first > 0L && second > Long.MAX_VALUE / first) {
            return Long.MAX_VALUE;
        }
        return first * second;
    }

    private static long saturatedAdd(long first, long second) {
        if (second > 0L && first > Long.MAX_VALUE - second) {
            return Long.MAX_VALUE;
        }
        return first + second;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public boolean isMachineEnabled() {
        return machineEnabled;
    }

    @Override
    public MachineControlContext machineControlContext(int redstoneInput) {
        return currentMachineControlSnapshot().withRedstoneInput(redstoneInput);
    }

    private MachineControlSnapshot currentMachineControlSnapshot() {
        long tick = level == null ? Long.MIN_VALUE : level.getGameTime();
        if (machineControlSnapshot != null && machineControlSnapshotTick == tick) return machineControlSnapshot;
        machineControlSnapshotTick = tick;
        machineControlSnapshot = MachineControlSnapshot.builder(tick)
                .inputRevision(machineControlInputRevision)
                .machineRunning(machineEnabled && recipeLogic.status() == CERecipeStatus.WORKING)
                .hasActiveRecipe(activeRecipeId() != null)
                .recipeProgress(recipeProgress())
                .recipeDuration(recipeDuration())
                .energy(this::machineControlEnergySnapshot)
                .steam(this::machineControlSteamSnapshot)
                .diagnostics(this::machineControlDiagnosticsSnapshot)
                .itemInputs(this::cachedMachineControlItemInputs)
                .fluidInputs(this::cachedMachineControlFluidInputs)
                .multiblockFormed(formed)
                .world(this::machineControlWorldSnapshot)
                .temperature(formedCoilHeat)
                .chemicalBalance(recipeMinimumChemicalBalanceHundredths(), recipeMaximumChemicalBalanceHundredths(), machineChemicalBalanceHundredths())
                .rpm(recipeMinimumRpm(), recipeMaximumRpm(), usesKineticInput() ? kineticInputRpm() : 0)
                .build();
        return machineControlSnapshot;
    }

    private MachineControlSnapshot.World machineControlWorldSnapshot() {
        if (level == null) return MachineControlSnapshot.World.NONE;
        return new MachineControlSnapshot.World(
                level.isRaining(),
                level.isThundering(),
                level.canSeeSky(worldPosition.above()),
                level.isDay()
        );
    }

    private MachineControlSnapshot.Diagnostics machineControlDiagnosticsSnapshot() {
        MachineControlDiagnostics diagnostics = machineControlDiagnostics();
        return new MachineControlSnapshot.Diagnostics(
                diagnostics.missingInput(),
                diagnostics.missingEnergy(),
                diagnostics.outputBlocked()
        );
    }

    public void markMachineControlInputsDirty() {
        machineControlInputRevision++;
        machineControlCachedInputRevision = Long.MIN_VALUE;
    }

    private List<ItemStack> cachedMachineControlItemInputs() {
        refreshMachineControlInputCaches();
        return machineControlCachedItemInputs;
    }

    private List<FluidStack> cachedMachineControlFluidInputs() {
        refreshMachineControlInputCaches();
        return machineControlCachedFluidInputs;
    }

    private void refreshMachineControlInputCaches() {
        if (machineControlCachedInputRevision == machineControlInputRevision) return;
        machineControlCachedInputRevision = machineControlInputRevision;
        machineControlCachedItemInputs = machineControlItemInputs().stream().map(ItemStack::copy).toList();
        machineControlCachedFluidInputs = machineControlFluidInputs().stream().map(FluidStack::copy).toList();
    }

    private List<ItemStack> machineControlItemInputs() {
        List<MachinePortBlockEntity> ports = new ArrayList<>();
        ports.addAll(itemPorts(MultiblockAbility.ITEM_INPUT));
        for (MachinePortBlockEntity port : itemPorts(MultiblockAbility.IO_INTERFACE)) if (!ports.contains(port)) ports.add(port);
        return itemStacks(ports);
    }

    private List<FluidStack> machineControlFluidInputs() {
        List<MachinePortBlockEntity> ports = new ArrayList<>();
        ports.addAll(fluidPorts(MultiblockAbility.FLUID_INPUT));
        for (MachinePortBlockEntity port : fluidPorts(MultiblockAbility.IO_INTERFACE)) if (!ports.contains(port)) ports.add(port);
        return fluidStacks(ports);
    }

    private MachineControlSnapshot.Energy machineControlEnergySnapshot() {
        long stored = 0L;
        long capacity = 0L;
        for (MachinePortBlockEntity port : itemlessEnergyPorts()) {
            stored = saturatedAdd(stored, port.storedCE());
            capacity = saturatedAdd(capacity, port.capacityCE());
        }
        return new MachineControlSnapshot.Energy(stored, capacity);
    }

    private MachineControlSnapshot.Steam machineControlSteamSnapshot() {
        return new MachineControlSnapshot.Steam(machineControlSteamStored(), machineControlSteamCapacity());
    }

    private int machineControlSteamStored() {
        Fluid steam = steamFluid();
        if (steam == null) return 0;
        return machineControlFluidInputs().stream()
                .filter(stack -> stack.getFluid() == steam)
                .mapToInt(FluidStack::getAmount)
                .sum();
    }

    private int machineControlSteamCapacity() {
        MultiblockDefinition definition = currentDefinition();
        if (definition == null || definition.drive() != MachineDrive.STEAM) return 0;
        return fluidPorts(MultiblockAbility.FLUID_INPUT).stream()
                .flatMap(port -> port.fluidTanks().stream())
                .mapToInt(FluidTank::getCapacity)
                .sum();
    }

    private long machineControlEnergyStored() {
        return itemlessEnergyPorts().stream().mapToLong(MachinePortBlockEntity::storedCE).sum();
    }

    private long machineControlEnergyCapacity() {
        return itemlessEnergyPorts().stream().mapToLong(MachinePortBlockEntity::capacityCE).sum();
    }

    private List<MachinePortBlockEntity> itemlessEnergyPorts() {
        List<MachinePortBlockEntity> result = new ArrayList<>();
        if (level == null) return result;
        for (BlockPos pos : abilityPositions(MultiblockAbility.ENERGY_INPUT)) {
            if (level.getBlockEntity(pos) instanceof MachinePortBlockEntity port && !result.contains(port)) result.add(port);
        }
        return result;
    }

    private MachineControlDiagnostics machineControlDiagnostics() {
        MultiblockDefinition definition = currentDefinition();
        if (level == null || !formed || definition == null) {
            return new MachineControlDiagnostics(false, false, false);
        }

        CERecipeExecution execution = recipeLogic.execution();
        if (execution != null) {
            boolean missingEnergy = execution.resourcePerTick() > 0
                    && !canProcessMachineResource(definition, execution.resourcePerTick());
            return new MachineControlDiagnostics(
                    false,
                    missingEnergy,
                    recipeLogic.status() == CERecipeStatus.WAITING_FOR_OUTPUT
            );
        }

        Optional<MachineTier> processingTier = processingTier(definition);
        int rpm = definition.drive() == MachineDrive.KINETIC ? kineticInputRpm() : 0;
        if (!machineRpmMatches(definition, rpm)) {
            return new MachineControlDiagnostics(false, false, false);
        }
        Optional<MachineTier> kineticTier = definition.drive() == MachineDrive.KINETIC ? processingTier : Optional.empty();
        Optional<MachineTier> energyTier = definition.drive() == MachineDrive.ELECTRIC ? processingTier : Optional.empty();
        boolean matchedRecipe = false;
        boolean missingEnergy = false;
        boolean outputBlocked = false;

        List<MachinePortBlockEntity> diagnosticItemPorts = itemPorts(MultiblockAbility.ITEM_INPUT);
        List<MachinePortBlockEntity> diagnosticFluidPorts = fluidPorts(MultiblockAbility.FLUID_INPUT);
        List<InputRoute> diagnosticRoutes = definition.sequencedInput()
                ? List.of(new InputRoute(Optional.empty(), DyeColor.GRAY, false, diagnosticItemPorts, diagnosticFluidPorts))
                : inputRoutes(diagnosticItemPorts, diagnosticFluidPorts);

        for (InputRoute route : diagnosticRoutes) {
            List<ItemEntity> worldInputEntities = definition.sequencedInput()
                    ? List.of()
                    : worldInputEntities(definition);
            List<ItemStack> availableItems = new ArrayList<>(itemStacks(route.itemPorts()));
            availableItems.addAll(worldItemStacks(worldInputEntities));
            List<FluidStack> availableFluids = fluidStacks(route.fluidPorts());
            InteractionContext interactionContext = interactionContext(availableItems, availableFluids);
            CERecipeInput input = new CERecipeInput(
                    availableItems,
                    availableFluids,
                    route.circuit(),
                    Set.copyOf(definition.logicIds()),
                    processingTier,
                    kineticTier,
                    energyTier,
                    definition.drive(),
                    rpm,
                    formedCoilHeat
            );
            Optional<RecipeHolder<CERecipe>> match = matchingRecipe(definition, input);
            if (match.isEmpty()) continue;
            matchedRecipe = true;
            CERecipe recipe = match.get().value();
            if (!InteractionRuntime.conditionsMatch(definition.conditions(), interactionContext, InteractionPhase.ON_START)
                    || !InteractionRuntime.conditionsMatch(recipe.conditions(), interactionContext, InteractionPhase.ON_START)
                    || !InteractionRuntime.interactionsMatch(definition.blockInteractions(), interactionContext, InteractionPhase.ON_START)
                    || !InteractionRuntime.interactionsMatch(recipe.blockInteractions(), interactionContext, InteractionPhase.ON_START)) {
                continue;
            }
            MachineTier runtimeTier = runtimeTier(recipe, input);
            int resourcePerTick = InteractionRuntime.adjustedResource(
                    machineResourcePerTick(definition, runtimeTier),
                    definition.drive() == MachineDrive.STEAM,
                    InteractionRuntime.firstMatchingModifier(definition.modifiers(), interactionContext),
                    InteractionRuntime.firstMatchingModifier(recipe.modifiers(), interactionContext)
            );
            missingEnergy |= resourcePerTick > 0 && !canProcessMachineResource(definition, resourcePerTick);
            List<ItemStack> itemOutputs = recipe.itemOutputs().stream().map(output -> output.stack().copy()).toList();
            List<FluidStack> fluidOutputs = possibleFluidOutputs(recipe);
            outputBlocked |= !canRouteItemOutputs(definition, outputPorts(MultiblockAbility.ITEM_OUTPUT, route), itemOutputs)
                    || !canFitFluidOutputs(outputPorts(MultiblockAbility.FLUID_OUTPUT, route), fluidOutputs);
        }
        return new MachineControlDiagnostics(!matchedRecipe, missingEnergy, outputBlocked);
    }

    private int machineControlItemCount(String filter) {
        if (filter == null || filter.isBlank()) return machineControlItemInputs().stream().mapToInt(ItemStack::getCount).sum();
        return machineControlItemInputs().stream()
                .filter(stack -> machineControlItemMatchesFilter(stack, filter))
                .mapToInt(ItemStack::getCount)
                .sum();
    }

    private int machineControlFluidAmount(String filter) {
        if (filter == null || filter.isBlank()) return machineControlFluidInputs().stream().mapToInt(FluidStack::getAmount).sum();
        return machineControlFluidInputs().stream()
                .filter(stack -> machineControlFluidMatchesFilter(stack, filter))
                .mapToInt(FluidStack::getAmount)
                .sum();
    }

    private boolean machineControlItemMatchesFilter(ItemStack stack, String filter) {
        for (String raw : filter.split("[,;\n]+")) {
            String token = raw.trim();
            if (token.isEmpty()) continue;
            boolean tag = token.startsWith("#");
            ResourceLocation id = ResourceLocation.tryParse(tag ? token.substring(1) : token);
            if (id == null) continue;
            if (tag && stack.is(TagKey.create(Registries.ITEM, id))) return true;
            if (!tag && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(id)) return true;
        }
        return false;
    }

    private boolean machineControlFluidMatchesFilter(FluidStack stack, String filter) {
        for (String raw : filter.split("[,;\n]+")) {
            String token = raw.trim();
            if (token.isEmpty()) continue;
            boolean tag = token.startsWith("#");
            ResourceLocation id = ResourceLocation.tryParse(tag ? token.substring(1) : token);
            if (id == null) continue;
            if (tag && stack.getFluid().builtInRegistryHolder().is(TagKey.create(Registries.FLUID, id))) return true;
            if (!tag && BuiltInRegistries.FLUID.getKey(stack.getFluid()).equals(id)) return true;
        }
        return false;
    }

    private boolean machineControlItemMatches(String value, boolean tagMatch) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) return false;
        if (tagMatch) {
            TagKey<net.minecraft.world.item.Item> tag = TagKey.create(Registries.ITEM, id);
            return machineControlItemInputs().stream().anyMatch(stack -> stack.is(tag));
        }
        return machineControlItemInputs().stream().anyMatch(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(id));
    }

    private boolean machineControlFluidMatches(String value, boolean tagMatch) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) return false;
        if (tagMatch) {
            TagKey<Fluid> tag = TagKey.create(Registries.FLUID, id);
            return machineControlFluidInputs().stream().anyMatch(stack -> stack.getFluid().builtInRegistryHolder().is(tag));
        }
        return machineControlFluidInputs().stream().anyMatch(stack -> BuiltInRegistries.FLUID.getKey(stack.getFluid()).equals(id));
    }

    @Override
    public void setMachineEnabled(boolean enabled) {
        if (machineEnabled == enabled) {
            return;
        }

        machineEnabled = enabled;
        if (!enabled) {
            setActive(false);
        }
        setChanged();
        syncToClient();
    }

    public void setActive(boolean active) {
        boolean targetActive = active && formed && machineEnabled;
        boolean changed = updateBlockActiveState(targetActive);
        boolean coilChanged = setCoilsActive(targetActive);
        boolean fireboxChanged = setFireboxesActive(targetActive);
        if (changed || coilChanged || fireboxChanged) {
            setChanged();
            syncToClient();
        }
    }

    private void updateBlockFormedState(boolean formed) {
        if (level == null) {
            return;
        }

        BlockState state = getBlockState();
        if (!state.hasProperty(MultiblockControllerBlock.FORMED) || state.getValue(MultiblockControllerBlock.FORMED) == formed) {
            return;
        }

        level.setBlock(worldPosition, state.setValue(MultiblockControllerBlock.FORMED, formed), 3);
    }

    private boolean updateBlockActiveState(boolean active) {
        if (level == null) {
            return false;
        }

        BlockState state = getBlockState();
        if (!state.hasProperty(MultiblockControllerBlock.ACTIVE)) {
            return false;
        }

        BlockState updated = state.setValue(MultiblockControllerBlock.ACTIVE, active);
        if (!active && updated.hasProperty(MultiblockControllerBlock.OVERLAY_FRAME)) {
            overlayFrame = 0;
            overlayFrameTicks = 0;
            updated = updated.setValue(MultiblockControllerBlock.OVERLAY_FRAME, 0);
        }

        if (updated.equals(state)) {
            return false;
        }

        level.setBlock(worldPosition, updated, 3);
        return true;
    }

    private boolean setCoilsActive(boolean active) {
        if (level == null) {
            return false;
        }

        boolean changed = false;
        for (BlockPos partPos : formedPositions) {
            BlockState state = level.getBlockState(partPos);
            if (!(state.getBlock() instanceof CoilBlock) || !state.hasProperty(CoilBlock.ACTIVE) || state.getValue(CoilBlock.ACTIVE) == active) {
                continue;
            }

            level.setBlock(partPos, state.setValue(CoilBlock.ACTIVE, active), 3);
            changed = true;
        }
        return changed;
    }

    private boolean setFireboxesActive(boolean active) {
        if (level == null) {
            return false;
        }

        boolean changed = false;
        for (BlockPos partPos : formedPositions) {
            BlockState state = level.getBlockState(partPos);
            if (!(state.getBlock() instanceof FireboxBlock) || !state.hasProperty(FireboxBlock.ACTIVE) || state.getValue(FireboxBlock.ACTIVE) == active) {
                continue;
            }

            BlockState updated = state.setValue(FireboxBlock.ACTIVE, active);
            if (!active && updated.hasProperty(FireboxBlock.OVERLAY_FRAME)) {
                updated = updated.setValue(FireboxBlock.OVERLAY_FRAME, 0);
            }
            level.setBlock(partPos, updated, 3);
            changed = true;
        }
        return changed;
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide()) clientSyncPending = true;
    }

    private void flushClientSync() {
        if (!clientSyncPending || level == null || level.isClientSide()) return;
        clientSyncPending = false;
        BlockState state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, 3);
    }

    public boolean isProcessing() {
        if (!machineEnabled) {
            return false;
        }

        MultiblockDefinition definition = currentDefinition();
        return recipeLogic.isProcessing()
                || (definition != null && definition.externalHeatSource() && externallyActive());
    }

    @Nullable
    private MultiblockDefinition currentDefinition() {
        if (!(getBlockState().getBlock() instanceof MultiblockControllerBlock controllerBlock)) {
            return null;
        }
        return currentDefinition(controllerBlock);
    }

    @Nullable
    private static MultiblockDefinition currentDefinition(MultiblockControllerBlock controllerBlock) {
        return MultiblockRegistry.byController(controllerBlock.controllerId()).orElse(null);
    }

    public int recipeProgress() {
        return recipeLogic.isProcessing() ? recipeLogic.progress() : recipeProgress;
    }

    public int recipeDuration() {
        return recipeLogic.isProcessing() ? recipeLogic.duration() : recipeDuration;
    }

    public int recipeRemaining() {
        return recipeLogic.isProcessing()
                ? recipeLogic.remaining()
                : Math.max(0, recipeDuration - recipeProgress);
    }

    public int activeCEt() {
        if (!machineEnabled) {
            return 0;
        }
        return recipeLogic.isProcessing() ? recipeLogic.resourcePerTick() : activeCEt;
    }

    public int activeParallel() {
        return recipeLogic.parallel();
    }

    @Nullable
    public ResourceLocation activeRecipeId() {
        CERecipeExecution execution = recipeLogic.execution();
        return execution == null ? null : execution.recipeId();
    }

    public List<ItemStack> activeItemInputs() {
        CERecipeExecution execution = recipeLogic.execution();
        return execution == null ? List.of() : execution.itemInputs();
    }

    public List<FluidStack> activeFluidInputs() {
        CERecipeExecution execution = recipeLogic.execution();
        return execution == null ? List.of() : execution.fluidInputs();
    }

    public List<ItemStack> activeItemOutputs() {
        CERecipeExecution execution = recipeLogic.execution();
        return execution == null ? List.of() : execution.itemOutputs();
    }

    public List<FluidStack> activeFluidOutputs() {
        CERecipeExecution execution = recipeLogic.execution();
        return execution == null ? List.of() : execution.fluidOutputs();
    }

    @Override
    public Component getDisplayName() {
        if (getBlockState().getBlock() instanceof MultiblockControllerBlock controller) {
            return Component.translatable(controller.getDescriptionId());
        }
        return Component.literal("Multiblock Controller");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MultiblockControllerMenu(containerId, playerInventory, this);
    }

    @Override
    public CERecipeLogic recipeLogic() {
        return recipeLogic;
    }

    public ProgressBar progressBar() {
        MultiblockDefinition definition = currentDefinition();
        return definition == null ? ProgressBar.ARROW : definition.progressBar();
    }

    private Optional<CERecipe> recipeById(ResourceLocation recipeId) {
        if (level == null) {
            return Optional.empty();
        }
        return level.getRecipeManager()
                .byKey(recipeId)
                .filter(holder -> holder.value() instanceof CERecipe)
                .map(holder -> (CERecipe) holder.value());
    }

    private InteractionContext interactionContext(List<ItemStack> items, List<FluidStack> fluids) {
        Direction facing = getBlockState().hasProperty(MultiblockControllerBlock.FACING)
                ? getBlockState().getValue(MultiblockControllerBlock.FACING)
                : Direction.NORTH;
        return new InteractionContext() {
            @Override
            public Level level() {
                return level;
            }

            @Override
            public BlockPos origin() {
                return worldPosition;
            }

            @Override
            public Direction facing() {
                return facing;
            }

            @Override
            public List<ItemStack> itemInputs() {
                return items;
            }

            @Override
            public List<FluidStack> fluidInputs() {
                return fluids;
            }

            @Override
            public InteractionWearStore wearStore() {
                return interactionWear;
            }

            @Override
            public Map<String, net.mads.industron.machine.interaction.MachineArea.Resolved> areas() {
                MultiblockDefinition definition = currentDefinition();
                if (definition == null) {
                    return Map.of();
                }
                MachineTier tier = processingTier(definition).orElse(MachineTier.ULV);
                return definition.areas().stream().collect(Collectors.toUnmodifiableMap(
                        net.mads.industron.machine.interaction.MachineArea::name,
                        area -> area.resolve(worldPosition, facing, tier, MachineTier.ULV)
                ));
            }
        };
    }

    private Optional<CERecipeExecution> prepareRecipeExecution(MultiblockDefinition definition) {
        if (level == null) {
            return Optional.empty();
        }

        Optional<MachineTier> processingTier = processingTier(definition);
        int rpm = definition.drive() == MachineDrive.KINETIC ? kineticInputRpm() : 0;

        List<MachinePortBlockEntity> inputPorts = itemPorts(MultiblockAbility.ITEM_INPUT);
        List<MachinePortBlockEntity> fluidInputPorts = fluidPorts(MultiblockAbility.FLUID_INPUT);
        Optional<MachineTier> kineticTier = definition.drive() == MachineDrive.KINETIC
                ? processingTier
                : Optional.empty();
        Optional<MachineTier> energyTier = definition.drive() == MachineDrive.ELECTRIC
                ? processingTier
                : Optional.empty();

        List<InputRoute> routes = definition.sequencedInput()
                ? List.of(new InputRoute(Optional.empty(), DyeColor.GRAY, false, inputPorts, fluidInputPorts))
                : inputRoutes(inputPorts, fluidInputPorts);

        for (InputRoute route : routes) {
            List<ItemEntity> worldInputEntities = definition.sequencedInput()
                    ? List.of()
                    : worldInputEntities(definition);
            List<ItemStack> availableItems = new ArrayList<>(itemStacks(route.itemPorts()));
            availableItems.addAll(worldItemStacks(worldInputEntities));
            List<FluidStack> availableFluids = fluidStacks(route.fluidPorts());
            InteractionContext interactionContext = interactionContext(availableItems, availableFluids);
            CERecipeInput input = new CERecipeInput(
                    availableItems,
                    availableFluids,
                    route.circuit(),
                    Set.copyOf(definition.logicIds()),
                    processingTier,
                    kineticTier,
                    energyTier,
                    definition.drive(),
                    rpm,
                    formedCoilHeat
            );

            Optional<RecipeHolder<CERecipe>> match = matchingRecipe(definition, input);
            if (match.isEmpty()) {
                continue;
            }

            CERecipe recipe = match.get().value();
            FanInputStats fanStats = fanInputStats(definition);
            if (definition.worldInteractions().stream()
                    .anyMatch(interaction -> interaction.type() == MultiblockDefinition.WorldInteractionType.FAN_INPUT)
                    && fanStats.activeInputs() == 0) {
                continue;
            }
            if (!mufflersOpen()) {
                continue;
            }
            if (!InteractionRuntime.conditionsMatch(definition.conditions(), interactionContext, InteractionPhase.ON_START)
                    || !InteractionRuntime.conditionsMatch(recipe.conditions(), interactionContext, InteractionPhase.ON_START)
                    || !InteractionRuntime.interactionsMatch(definition.blockInteractions(), interactionContext, InteractionPhase.ON_START)
                    || !InteractionRuntime.interactionsMatch(recipe.blockInteractions(), interactionContext, InteractionPhase.ON_START)
                    || !InteractionRuntime.interactionsMatch(definition.blockInteractions(), interactionContext, InteractionPhase.ON_COMPLETE)
                    || !InteractionRuntime.interactionsMatch(recipe.blockInteractions(), interactionContext, InteractionPhase.ON_COMPLETE)) {
                continue;
            }

            List<MachinePortBlockEntity> outputPorts = outputPorts(MultiblockAbility.ITEM_OUTPUT, route);
            List<MachinePortBlockEntity> fluidOutputPorts = outputPorts(MultiblockAbility.FLUID_OUTPUT, route);
            MachineTier runtimeTier = runtimeTier(recipe, input);
            int parallel = maxRecipeParallel(definition);
            while (parallel > 1 && !((definition.sequencedInput() ? canConsumeSequencedInputs(recipe, parallel)
                    : canConsumeInputs(route.itemPorts(), worldInputEntities, recipe, parallel, input.processingTier()))
                    && canConsumeFluidInputs(route.fluidPorts(), recipe, parallel, input.processingTier()))) {
                parallel--;
            }
            Optional<MachineModifier> machineModifier =
                    InteractionRuntime.firstMatchingModifier(definition.modifiers(), interactionContext);
            Optional<MachineModifier> recipeModifier =
                    InteractionRuntime.firstMatchingModifier(recipe.modifiers(), interactionContext);
            int baseResourcePerTick = InteractionRuntime.adjustedResource(
                    machineResourcePerTick(definition, runtimeTier),
                    definition.drive() == MachineDrive.STEAM,
                    machineModifier,
                    recipeModifier
            );
            int resourcePerTick = baseResourcePerTick;
            List<ItemStack> plannedItemOutputs = rollItemOutputs(recipe, parallel, input.processingTier());
            SequentialFluidOutputs sequentialFluidOutputs = definition.sequencedOutput()
                    ? rollSequentialFluidOutputs(recipe, parallel, input.processingTier())
                    : SequentialFluidOutputs.empty();
            List<FluidStack> plannedFluidOutputs = definition.sequencedOutput()
                    ? sequentialFluidOutputs.outputs()
                    : rollFluidOutputs(recipe, parallel, input.processingTier());
            boolean fluidOutputsFit = definition.sequencedOutput()
                    ? canFitSequentialFluidOutputs(sequentialFluidOutputs)
                    : canFitFluidOutputs(fluidOutputPorts, plannedFluidOutputs);

            if (!canRouteItemOutputs(definition, outputPorts, plannedItemOutputs)
                    || !fluidOutputsFit
                    || !canProcessMachineResource(definition, resourcePerTick)
                    || !(definition.sequencedInput()
                    ? canConsumeSequencedInputs(recipe, parallel)
                    : canConsumeInputs(route.itemPorts(), worldInputEntities, recipe, parallel, input.processingTier()))
                    || !canConsumeFluidInputs(route.fluidPorts(), recipe, parallel, input.processingTier())) {
                continue;
            }
            Optional<List<ItemStack>> consumedSequencedItems = definition.sequencedInput()
                    ? consumeSequencedInputs(recipe, parallel)
                    : Optional.empty();
            boolean consumedItems = definition.sequencedInput()
                    ? consumedSequencedItems.isPresent()
                    : consumeInputs(route.itemPorts(), worldInputEntities, recipe, parallel, input.processingTier(), level.random);
            if (!consumedItems
                    || !consumeFluidInputs(route.fluidPorts(), recipe, parallel, input.processingTier(), level.random)) {
                continue;
            }

            InteractionRuntime.applyInteractions(definition.blockInteractions(), interactionContext, InteractionPhase.ON_START);
            InteractionRuntime.applyInteractions(recipe.blockInteractions(), interactionContext, InteractionPhase.ON_START);
            CompoundTag machineData = new CompoundTag();
            machineData.putBoolean("UsesIoColor", route.usesColor());
            machineData.putInt("IoColor", route.color().getId());
            if (definition.sequencedOutput()) {
                machineData.putIntArray(SEQUENTIAL_FLUID_OUTPUT_INDEXES_DATA, sequentialFluidOutputs.indexes());
            }
            if (isDirtyAssemblerRecipe(recipe)) {
                machineData.putBoolean(DIRTY_ASSEMBLER_SUCCESS_DATA, !plannedItemOutputs.isEmpty());
            }
            if (definition.drive() == MachineDrive.KINETIC_OUTPUT) {
                machineData.putInt(
                        KINETIC_OUTPUT_RPM_DATA,
                        resolvedOutputRpm(definition, recipe)
                );
            }
            preferredRecipeId = match.get().id();
            return Optional.of(new CERecipeExecution(
                    match.get().id(),
                    recipe.recipeType(),
                    applyMufflerAndFanDuration(
                            InteractionRuntime.adjustedDuration(
                                    recipe.runtimeDuration(
                                            runtimeTier,
                                            definition.drive(),
                                            executionRpm(definition, recipe, rpm),
                                            input
                                    ),
                                    machineModifier,
                                    recipeModifier
                            ),
                            fanStats
                    ),
                    resourcePerTick,
                    parallel,
                    definition.sequencedInput()
                            ? consumedSequencedItems.orElseGet(() -> displaySequencedItemInputs(recipe))
                            : displayItemInputs(recipe),
                    displayFluidInputs(recipe),
                    plannedItemOutputs,
                    plannedFluidOutputs,
                    machineData
            ));
        }
        return Optional.empty();
    }

    private InputRoute executionRoute(CERecipeExecution execution) {
        CompoundTag data = execution.machineData();
        return data.getBoolean("UsesIoColor")
                ? InputRoute.outputOnly(DyeColor.byId(data.getInt("IoColor")))
                : InputRoute.uncolored();
    }

    private List<ItemStack> rollItemOutputs(
            CERecipe recipe,
            int parallel,
            Optional<MachineTier> runtimeTier
    ) {
        if (level == null) {
            return List.of();
        }
        List<ItemStack> outputs = new ArrayList<>();
        for (int run = 0; run < Math.max(1, parallel); run++) {
            for (net.mads.industron.recipe.CEChancedItemOutput output : recipe.itemOutputs()) {
                int chance = output.effectiveChance(runtimeTier, recipe.requiredTier());
                if (chance >= net.mads.industron.recipe.CEChancedItemOutput.MAX_CHANCE
                        || level.random.nextInt(net.mads.industron.recipe.CEChancedItemOutput.MAX_CHANCE)
                        < chance) {
                    outputs.add(output.stack().copy());
                }
            }
        }
        return outputs;
    }

    private static boolean isDirtyAssemblerRecipe(CERecipe recipe) {
        return false;
    }

    private static List<ItemStack> dirtyAssemblerRefundItems(
            CERecipeExecution execution,
            CERecipe recipe
    ) {
        if (recipe.itemInputs().isEmpty()) {
            return List.of();
        }

        int baseItemsToSkip = multiplyClamped(
                recipe.itemInputs().getFirst().count(),
                execution.parallel()
        );
        List<ItemStack> refund = new ArrayList<>();
        for (ItemStack consumed : execution.itemInputs()) {
            if (consumed.isEmpty()) {
                continue;
            }

            int skipped = Math.min(baseItemsToSkip, consumed.getCount());
            baseItemsToSkip -= skipped;
            int refundable = consumed.getCount() - skipped;
            if (refundable > 0) {
                refund.add(consumed.copyWithCount(refundable));
            }
        }
        return List.copyOf(refund);
    }

    private int maxRecipeParallel(MultiblockDefinition definition) {
        if (!definition.usesParallelHatches()) return Math.max(1, definition.maxParallel());
        int capacity = 0;
        for (BlockPos pos : formedPositions) {
            if (level != null && level.getBlockEntity(pos) instanceof ParallelHatchProvider provider) {
                capacity = Math.min(Integer.MAX_VALUE, capacity + Math.max(0, provider.parallelCapacity()));
            }
        }
        return Math.max(1, capacity);
    }

    private final class RecipeHost implements CERecipeLogicHost {
        @Override
        public boolean recipeMachineReady() {
            MultiblockDefinition definition = currentDefinition();
            return level != null
                    && formed
                    && definition != null
                    && !definition.recipeTypes().isEmpty();
        }

        @Override
        public Optional<CERecipeExecution> findAndConsumeRecipeInputs() {
            MultiblockDefinition definition = currentDefinition();
            return definition == null ? Optional.empty() : prepareRecipeExecution(definition);
        }

        @Override
        public CERecipeTickResult consumeRecipeTick(CERecipeExecution execution) {
            MultiblockDefinition definition = currentDefinition();
            Optional<CERecipe> recipe = recipeById(execution.recipeId());
            InteractionContext context = interactionContext(execution.itemInputs(), execution.fluidInputs());
            if (definition == null || recipe.isEmpty()) {
                return CERecipeTickResult.WAIT_FOR_RESOURCE;
            }
            if (!mufflersOpen()) {
                return CERecipeTickResult.WAIT_FOR_RESOURCE;
            }
            if (definition.worldInteractions().stream()
                    .anyMatch(interaction -> interaction.type() == MultiblockDefinition.WorldInteractionType.FAN_INPUT)
                    && fanInputStats(definition).activeInputs() == 0) {
                return CERecipeTickResult.WAIT_FOR_RESOURCE;
            }

            if (recipe.get().chemicalBalanceRange().isPresent()
                    && (!hasCbHatch() || !chemicalBalanceMatches(recipe.get()))) {
                return CERecipeTickResult.WAIT_FOR_CB;
            }

            if (definition.drive() == MachineDrive.KINETIC) {
                int rpm = kineticInputRpm();
                if (!kineticInputPowerReady()
                        || !machineRpmMatches(definition, rpm)
                        || !recipeRpmMatches(recipe.get(), rpm)) {
                    return CERecipeTickResult.WAIT_FOR_RPM;
                }
            }

            if (definition.drive() != MachineDrive.NONE && recipe.get().requiredTier().isPresent()) {
                Optional<MachineTier> actualTier = processingTier(definition);
                if (actualTier.isEmpty()
                        || !MachineTierStats.isAtLeast(actualTier.get(), recipe.get().requiredTier().get())) {
                    return CERecipeTickResult.WAIT_FOR_RESOURCE;
                }
            }

            ConditionFailure machineFailure = InteractionRuntime.failedConditionBehavior(
                    definition.conditions(),
                    context,
                    InteractionPhase.WHILE_PROCESSING
            );
            ConditionFailure recipeFailure = InteractionRuntime.failedConditionBehavior(
                    recipe.get().conditions(),
                    context,
                    InteractionPhase.WHILE_PROCESSING
            );
            ConditionFailure failure = machineFailure != null ? machineFailure : recipeFailure;
            if (failure != null) {
                return failure == ConditionFailure.CANCEL
                        ? CERecipeTickResult.CANCEL
                        : failure == ConditionFailure.RESET
                        ? CERecipeTickResult.WAIT_FOR_RESOURCE
                        : CERecipeTickResult.PAUSE;
            }
            return processMachineResource(definition, execution.resourcePerTick())
                    ? CERecipeTickResult.CONTINUE
                    : CERecipeTickResult.WAIT_FOR_RESOURCE;
        }

        @Override
        public boolean canCompleteRecipe(CERecipeExecution execution) {
            MultiblockDefinition definition = currentDefinition();
            if (definition == null) {
                return false;
            }
            Optional<CERecipe> recipe = recipeById(execution.recipeId());
            if (recipe.isEmpty()) {
                return false;
            }
            InteractionContext context = interactionContext(execution.itemInputs(), execution.fluidInputs());
            InputRoute route = executionRoute(execution);
            List<MachinePortBlockEntity> itemOutputPorts = outputPorts(MultiblockAbility.ITEM_OUTPUT, route);
            List<ItemStack> itemOutputs = isDirtyAssemblerRecipe(recipe.get())
                    && !execution.machineData().getBoolean(DIRTY_ASSEMBLER_SUCCESS_DATA)
                    ? dirtyAssemblerRefundItems(execution, recipe.get())
                    : execution.itemOutputs();
            boolean itemOutputsFit = isDirtyAssemblerRecipe(recipe.get())
                    && !execution.machineData().getBoolean(DIRTY_ASSEMBLER_SUCCESS_DATA)
                    ? !itemOutputPorts.isEmpty() && canFitItemOutputs(itemOutputPorts, itemOutputs)
                    : canRouteItemOutputs(definition, itemOutputPorts, itemOutputs);
            boolean fluidOutputsFit = definition.sequencedOutput()
                    ? canFitSequentialFluidOutputs(sequentialFluidOutputs(execution))
                    : canFitFluidOutputs(
                            outputPorts(MultiblockAbility.FLUID_OUTPUT, route),
                            execution.fluidOutputs()
                    );
            return itemOutputsFit && fluidOutputsFit
                    && InteractionRuntime.conditionsMatch(definition.conditions(), context, InteractionPhase.ON_COMPLETE)
                    && InteractionRuntime.conditionsMatch(recipe.get().conditions(), context, InteractionPhase.ON_COMPLETE)
                    && InteractionRuntime.interactionsMatch(definition.blockInteractions(), context, InteractionPhase.ON_COMPLETE)
                    && InteractionRuntime.interactionsMatch(recipe.get().blockInteractions(), context, InteractionPhase.ON_COMPLETE);
        }

        @Override
        public boolean completeRecipe(CERecipeExecution execution) {
            MultiblockDefinition definition = currentDefinition();
            if (definition == null) {
                return false;
            }
            Optional<CERecipe> recipe = recipeById(execution.recipeId());
            if (recipe.isEmpty()) {
                return false;
            }

            InteractionContext context = interactionContext(execution.itemInputs(), execution.fluidInputs());
            if (!InteractionRuntime.applyInteractions(definition.blockInteractions(), context, InteractionPhase.ON_COMPLETE)
                    || !InteractionRuntime.applyInteractions(recipe.get().blockInteractions(), context, InteractionPhase.ON_COMPLETE)) {
                return false;
            }

            if (isDirtyAssemblerRecipe(recipe.get())
                    && !execution.machineData().getBoolean(DIRTY_ASSEMBLER_SUCCESS_DATA)) {
                InputRoute route = executionRoute(execution);
                List<MachinePortBlockEntity> itemOutputPorts =
                        outputPorts(MultiblockAbility.ITEM_OUTPUT, route);
                return !itemOutputPorts.isEmpty()
                        && insertItemOutputs(
                        itemOutputPorts,
                        dirtyAssemblerRefundItems(execution, recipe.get()),
                        false
                );
            }

            InputRoute route = executionRoute(execution);
            List<MachinePortBlockEntity> itemOutputPorts =
                    outputPorts(MultiblockAbility.ITEM_OUTPUT, route);
            if (itemOutputPorts.isEmpty()) {
                dropItemOutputsInWorld(definition, execution.itemOutputs());
            } else {
                insertItemOutputs(itemOutputPorts, execution.itemOutputs(), false);
            }
            boolean insertedFluids = definition.sequencedOutput()
                    ? insertSequentialFluidOutputs(sequentialFluidOutputs(execution), false)
                    : insertFluidOutputs(
                            outputPorts(MultiblockAbility.FLUID_OUTPUT, route),
                            execution.fluidOutputs(),
                            false
                    );
            return insertedFluids;
        }

        @Override
        public void onRecipeLogicChanged(boolean activeChanged) {
            setActive(machineEnabled && recipeLogic.isActive());
            setChanged();
            if (activeChanged) {
                syncToClient();
            } else {
                syncActiveProgress();
            }
        }
    }

    private record SequentialFluidOutputs(List<FluidStack> outputs, int[] indexes) {
        private SequentialFluidOutputs {
            outputs = outputs.stream().map(FluidStack::copy).toList();
            indexes = indexes.clone();
        }

        private static SequentialFluidOutputs empty() {
            return new SequentialFluidOutputs(List.of(), new int[0]);
        }
    }

    private static final class SimulatedTank {
        private FluidStack fluid;
        private final int capacity;

        private SimulatedTank(FluidStack fluid, int capacity) {
            this.fluid = fluid;
            this.capacity = capacity;
        }

        FluidStack fill(FluidStack stack) {
            if (stack.isEmpty()) {
                return FluidStack.EMPTY;
            }
            if (!fluid.isEmpty() && !FluidStack.isSameFluidSameComponents(fluid, stack)) {
                return stack;
            }

            int space = capacity - fluid.getAmount();
            if (space <= 0) {
                return stack;
            }

            int filled = Math.min(space, stack.getAmount());
            FluidStack remaining = stack.copyWithAmount(stack.getAmount() - filled);
            if (fluid.isEmpty()) {
                fluid = stack.copyWithAmount(filled);
            } else {
                fluid.grow(filled);
            }
            return remaining.isEmpty() ? FluidStack.EMPTY : remaining;
        }
    }

    private record MachineControlDiagnostics(boolean missingInput, boolean missingEnergy, boolean outputBlocked) { }

    private record InputRoute(Optional<Integer> circuit, DyeColor color, boolean usesColor, List<MachinePortBlockEntity> itemPorts, List<MachinePortBlockEntity> fluidPorts) {
        private static InputRoute uncolored() {
            return new InputRoute(Optional.empty(), DyeColor.GRAY, false, List.of(), List.of());
        }

        private static InputRoute outputOnly(DyeColor color) {
            return new InputRoute(Optional.empty(), color, true, List.of(), List.of());
        }
    }

}
