package net.mads.industron.machine;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.mads.industron.recipe.CEChancedFluidInput;
import net.mads.industron.recipe.CEChancedFluidOutput;
import net.mads.industron.recipe.CEChancedItemOutput;
import net.mads.industron.recipe.CEChancedItemInput;
import net.mads.industron.recipe.CERecipe;
import net.mads.industron.recipe.CERecipeInput;
import net.mads.industron.recipe.CERecipeLookup;
import net.mads.industron.recipe.CERecipeTypes;
import net.mads.industron.recipe.CEToolRequirement;
import net.mads.industron.recipe.RecipeTypeDefinition;
import net.mads.industron.energy.CEEnergyContainer;
import net.mads.industron.energy.CEEnergyStorage;
import net.mads.industron.fluid.IndustrialFluids;
import net.mads.industron.fluid.IndustrialFluidLookup;
import net.mads.industron.gui.ProgressBar;
import net.mads.industron.menu.SingleBlockMachineMenu;
import net.mads.industron.machine.interaction.BlockInteraction;
import net.mads.industron.machine.interaction.InteractionContext;
import net.mads.industron.machine.interaction.InteractionPhase;
import net.mads.industron.machine.interaction.InteractionRuntime;
import net.mads.industron.machine.interaction.InteractionWearStore;
import net.mads.industron.machine.interaction.MachineModifier;
import net.mads.industron.machine.SingleBlockDefinition.MachineSide;
import net.mads.industron.machine.interaction.ConditionFailure;
import net.mads.industron.machine.runtime.CERecipeExecution;
import net.mads.industron.machine.runtime.CERecipeLogic;
import net.mads.industron.machine.runtime.CERecipeLogicHost;
import net.mads.industron.machine.runtime.CERecipeLogicMachine;
import net.mads.industron.machine.runtime.CERecipeTickResult;
import net.mads.industron.machine.runtime.CERecipeStatus;
import net.mads.industron.machine.control.MachineControlSchedule;
import net.mads.industron.machine.control.MachineControlScheduleHost;
import net.mads.industron.machine.control.MachineControlContext;
import net.mads.industron.machine.control.MachineControlSnapshot;
import net.mads.industron.machine.control.MachineControlTarget;
import net.mads.industron.machine.control.MachineControlVariableStore;
import net.mads.industron.machine.tree.TreeExtractionSavedData;
import net.mads.industron.machine.foundry.FoundryComponents;
import net.mads.industron.machine.foundry.FoundryMetallurgy;
import net.mads.industron.machine.foundry.MixtureCentrifuging;
import net.mads.industron.machine.foundry.MoltenRatio;
import net.mads.industron.registry.BlockEntityRegistry;
import net.mads.industron.registry.FluidRegistry;
import net.mads.industron.registry.ItemRegistry;
import net.mads.industron.material.ClayMaterialRules;
import net.mads.industron.material.MaterialItem;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.recipe.recipetypes.FuelRecipeLookup;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyTools;
import net.mads.industron.recipe.recipetypes.assembly.ToolVariantDefinition;
import net.mads.industron.recipe.recipetypes.assembly.input.AssemblyUseState;
import net.mads.industron.network.AssemblyNextStepPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class SingleBlockMachineBlockEntity extends GeneratingKineticBlockEntity implements Clearable, MenuProvider, CERecipeLogicMachine, MachineControlTarget, MachineControlScheduleHost {
    private final net.mads.industron.machine.runtime.AsyncRecipeSearch asyncSearch = new net.mads.industron.machine.runtime.AsyncRecipeSearch();

    private static final int BLOCKED_VENT_EXPLOSION_TICKS = 100;
    private static final int MAX_ITEM_INPUT_SLOTS = 16;
    private static final int MAX_ITEM_OUTPUT_SLOTS = 16;
    private static final int MAX_FLUID_INPUT_TANKS = 8;
    private static final int MAX_FLUID_OUTPUT_TANKS = 8;
    private static final int RECIPE_FLUID_TANK_CAPACITY = 16_000;
    private static final int BASIN_FLUID_TANK_CAPACITY = 1_000;
    private static final String KINETIC_OUTPUT_RPM_DATA = "KineticOutputRpm";
    private static final int KILN_MAX_TEMPERATURE = 1000;
    private static final List<RecipeTypeDefinition> KILN_PROCESS_TYPES = List.of(
            CERecipeTypes.KILN_FIRING,
            CERecipeTypes.HEATING
    );

    private final ItemStackHandler inputItems = createInventory(MAX_ITEM_INPUT_SLOTS, true);
    private final ItemStackHandler outputItems = createInventory(MAX_ITEM_OUTPUT_SLOTS, false);
    private final FluidTank[] inputFluids = createFluidTanks(MAX_FLUID_INPUT_TANKS, true);
    private final FluidTank[] outputFluids = createFluidTanks(MAX_FLUID_OUTPUT_TANKS, false);
    // Only used by unidentified MOLTEN_MIXTURE runtime centrifuging. Ordinary recipes never wait on this.
    private final long[] unidentifiedMixtureLastChangeTick = new long[MAX_FLUID_INPUT_TANKS];
    private final FluidTank steamTank = createSteamTank();
    private static final TagKey<Block> SPRINKLER_BLACKLIST = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("industron", "sprinkler_blacklist")
    );

    private final InteractionWearStore interactionWear = new InteractionWearStore();
    private final EnumMap<Direction, MachineControlSchedule> machineControlSchedules = new EnumMap<>(Direction.class);
    private final MachineControlVariableStore machineControlVariables = new MachineControlVariableStore();
    private int machineControlOutputSignature = -1;
    private long machineControlSnapshotTick = Long.MIN_VALUE;
    private MachineControlSnapshot machineControlSnapshot;
    private long machineControlInputRevision;
    private long machineControlCachedInputRevision = Long.MIN_VALUE;
    private List<ItemStack> machineControlCachedItemInputs = List.of();
    private List<FluidStack> machineControlCachedFluidInputs = List.of();
    private final List<BlockPos> sprinklerTargets = new ArrayList<>();
    private int sprinklerTargetIndex;
    private final CERecipeLogic recipeLogic = new CERecipeLogic(new RecipeHost());
    private final CEEnergyStorage ceContainer = new CEEnergyStorage(
            MachineTier.LV,
            Long.MAX_VALUE,
            this::acceptsEnergyInput,
            this::allowsEnergyOutput,
            ignored -> setChangedAndSync(),
            ignored -> explodeFromOvervoltage(),
            () -> level == null ? Long.MIN_VALUE : level.getGameTime()
    ) {
        @Override
        public MachineTier tier() {
            SingleBlockMachineInstance instance = instance();
            return instance == null || !instance.tier().isElectric()
                    ? MachineTier.LV
                    : instance.tier();
        }

        @Override
        public long getEnergyCapacity() {
            return energyCapacity();
        }

        @Override
        public long getEnergyStored() {
            return energyStored;
        }

        @Override
        public long getInputVoltage() {
            return MachineTierStats.ceTier(tier());
        }

        @Override
        public long getOutputVoltage() {
            return MachineTierStats.ceTier(tier());
        }

        @Override
        public long voltage() {
            return getInputVoltage();
        }

        @Override
        public long changeEnergy(long differenceAmount) {
            long previous = energyStored;
            energyStored = Math.max(0L, Math.min(energyStored + differenceAmount, getEnergyCapacity()));
            if (energyStored != previous) {
                setChangedAndSync();
            }
            return energyStored - previous;
        }
    };

    private int blockedVentTicks;
    private long energyStored;
    private int overlayFrame;
    private int overlayFrameTicks;
    private int recipeSyncCooldown;
    private int circuit;
    private ResourceLocation preferredRecipeId;
    private int temperature;
    private int temperatureTickCounter;
    private long temperatureOperationTicks;
    private float lastGeneratedSpeed = Float.NaN;
    private boolean machineEnabled = true;
    private boolean clientSyncPending;
    private boolean cosmeticSyncPending;
    private final int[] primitiveProgress = new int[6];
    private double kilnFuelUnits;
    private UUID manualToolWorker;
    private int manualToolTicks;
    private int manualToolUses;
    private ResourceLocation manualRecipeId;

    public SingleBlockMachineBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState blockState
    ) {
        super(type, pos, blockState);
    }

    public SingleBlockMachineBlockEntity(
            BlockPos pos,
            BlockState blockState
    ) {
        this(
                BlockEntityRegistry.SINGLE_BLOCK_MACHINE.get(),
                pos,
                blockState
        );
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
    }

    @Override
    public void tick() {
        super.tick();

        if (level != null && !level.isClientSide()) {
            try {
                serverTick();
            } finally {
                flushClientSync();
            }
        }
    }

    public void serverTick() {
        long performanceStart = net.mads.industron.debug.CEPerformanceProfiler.begin(getLevel());
        try {
        Level level = getLevel();
        if (level == null) {
            return;
        }

        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof SingleBlockMachineBlock block)) {
            return;
        }

        SingleBlockMachineInstance instance = block.instance();
        if (tickPrimitive(instance)) {
            return;
        }
        applyMachineControlSchedules();
        tickTemperature(instance);
        if (machineEnabled) {
            wakeRuntimeMixtureSearchIfReady(level, instance);
            recipeLogic.serverTick();
        }
        refreshGeneratedRotation();

        SingleBlockDefinition.TemperatureSettings temperatureSettings = instance.definition().temperature();
        boolean conditionHeated = temperatureSettings != null && temperatureSettings.usesHeatConditions();
        if (machineEnabled && conditionHeated) {
            // Heat-only generators (e.g. Solar Boiler) have no fuel recipe execution.
            int steamToProduce = instance.definition().resource() == SingleBlockMachineResource.STEAM
                    && instance.definition().resourceMode() == SingleBlockMachineResourceMode.PRODUCES
                    && instance.definition().recipeTypes().isEmpty()
                    ? instance.steamUsage() : 0;
            runTemperatureOperations(instance, steamToProduce);
        }

        boolean active = machineEnabled
                && (recipeLogic.isActive() || (conditionHeated && meetsTemperatureRequirement(instance)));

        // Overlay textures animate in the client atlas; only ACTIVE is gameplay state.
        if (state.getValue(SingleBlockMachineBlock.ACTIVE) != active) {
            state = state.setValue(SingleBlockMachineBlock.ACTIVE, active);
            level.setBlock(getBlockPos(), state, Block.UPDATE_ALL);
        }

        refreshMachineControlRedstoneOutputs();

        if (instance == null
                || instance.definition().power() != SingleBlockMachinePower.STEAM
                || instance.definition().resourceMode() != SingleBlockMachineResourceMode.CONSUMES
                || !active) {
            blockedVentTicks = 0;
            return;
        }

        if (!isVentBlocked(level, getBlockPos().above())) {
            blockedVentTicks = 0;
            return;
        }

        blockedVentTicks++;
        if (blockedVentTicks >= BLOCKED_VENT_EXPLOSION_TICKS) {
            blockedVentTicks = 0;
            level.explode(
                    null,
                    getBlockPos().getX() + 0.5D,
                    getBlockPos().getY() + 0.5D,
                    getBlockPos().getZ() + 0.5D,
                    instance.tier().steamExplosionPower(),
                    Level.ExplosionInteraction.TNT
            );
        }
            } finally {
            net.mads.industron.debug.CEPerformanceProfiler.record(net.mads.industron.debug.CEPerformanceProfiler.Metric.SINGLEBLOCK_TICK, performanceStart);
        }
    }

    private boolean tickPrimitive(SingleBlockMachineInstance instance) {
        if (instance == null) return false;
        String id = instance.definition().id();
        if (id.endsWith("_drying_rack")) {
            tickDryingRack();
            return true;
        }
        if (id.endsWith("_brick_mold")) {
            tickManualToolRecipe(CERecipeTypes.BRICK_MOLDING, "Brick Molding");
            return true;
        }
        if (id.endsWith("_basin")) {
            tickBasinDroppedItems();
            tickBasinManualToolRecipe();
            return true;
        }
        if (id.endsWith("_kiln")) {
            tickKiln();
            return true;
        }
        return false;
    }

    /** Remaining processing ticks per rack slot, sampled for Jade on the server. */
    public int[] dryingRackRemainingTicks() {
        int[] remaining = new int[4];
        SingleBlockMachineInstance current = instance();
        if (current == null || !current.definition().id().endsWith("_drying_rack")) return remaining;
        for (int slot = 0; slot < remaining.length; slot++) {
            Optional<CERecipe> recipe = rackDryingRecipe(inputItems.getStackInSlot(slot)).map(RecipeHolder::value);
            if (recipe.isPresent()) remaining[slot] = Math.max(0,
                    recipe.get().duration().orElse(ClayMaterialRules.SUN_DRYING_DURATION_TICKS) - primitiveProgress[slot]);
        }
        return remaining;
    }

    private void tickDryingRack() {
        Level level = getLevel();
        if (level == null) return;
        boolean raining = level.isRainingAt(worldPosition.above());
        boolean sunlight = level.isDay() && level.canSeeSky(worldPosition.above()) && !raining;
        for (int slot = 0; slot < 4; slot++) {
            ItemStack input = inputItems.getStackInSlot(slot);
            if (input.isEmpty() || !outputItems.getStackInSlot(slot).isEmpty()) {
                primitiveProgress[slot] = 0;
                continue;
            }

            Optional<CERecipe> recipe = rackDryingRecipe(input).map(RecipeHolder::value);
            if (recipe.isEmpty()) {
                primitiveProgress[slot] = 0;
                continue;
            }

            int duration = recipe.get().duration().orElse(ClayMaterialRules.SUN_DRYING_DURATION_TICKS);
            boolean cooling = net.mads.industron.material.recipes.CastingRecipes.canRackCool(input);

            if (cooling) {
                primitiveProgress[slot]++;
            } else if (raining && primitiveProgress[slot] > 0) {
                primitiveProgress[slot] = Math.max(0, primitiveProgress[slot] - 4);
                if (primitiveProgress[slot] == 0) {
                    inputItems.setStackInSlot(slot, ItemStack.EMPTY);
                    setChanged();
                    continue;
                }
            } else if (sunlight) {
                primitiveProgress[slot]++;
            }

            if (primitiveProgress[slot] >= duration) {
                outputItems.setStackInSlot(slot, recipe.get().itemOutputs().getFirst().stack().copy());
                inputItems.extractItem(slot, 1, false);
                primitiveProgress[slot] = 0;
            }
            setChanged();
        }
    }

    private Optional<RecipeHolder<CERecipe>> rackDryingRecipe(ItemStack stack) {
        Level level = getLevel();
        if (level == null || stack.isEmpty()) return Optional.empty();
        CERecipeInput input = CERecipeInput.of(List.of(stack), List.of());
        return CERecipeLookup.candidatesByTypes(level.getRecipeManager(),
                        List.of(CERecipeTypes.RACK_DRYING.id()), input).stream()
                .filter(holder -> validRackDryingRecipe(holder.value()))
                .filter(holder -> holder.value().matches(input, level))
                .findFirst();
    }

    private static boolean validRackDryingRecipe(CERecipe recipe) {
        return recipe.itemInputs().size() == 1
                && recipe.itemInputs().getFirst().count() == 1
                && recipe.chancedItemInputs().isEmpty()
                && recipe.notConsumableItems().isEmpty()
                && recipe.fluidInputs().isEmpty()
                && recipe.chancedFluidInputs().isEmpty()
                && recipe.notConsumableFluids().isEmpty()
                && recipe.itemOutputs().size() == 1
                && recipe.itemOutputs().getFirst().guaranteed()
                && recipe.itemOutputs().getFirst().stack().getCount() == 1
                && recipe.fluidOutputs().isEmpty()
                && recipe.chancedFluidOutputs().isEmpty();
    }

    /** Keeps the Basin's one active manual operation bound to the recipe type that started it. */
    private void tickBasinManualToolRecipe() {
        if (manualRecipeId == null || getLevel() == null) return;
        Optional<RecipeHolder<CERecipe>> holder = CERecipeLookup.byId(getLevel().getRecipeManager(), manualRecipeId);
        if (holder.isEmpty()) {
            manualRecipeId = null;
            manualToolWorker = null;
            manualToolTicks = 0;
            manualToolUses = 0;
            return;
        }

        ResourceLocation typeId = holder.get().value().recipeType();
        if (typeId.equals(CERecipeTypes.BASIN_MORTARING.id())) {
            tickManualToolRecipe(CERecipeTypes.BASIN_MORTARING, "Basin Mortaring");
        } else if (typeId.equals(CERecipeTypes.BASIN_MIXING.id())) {
            tickManualToolRecipe(CERecipeTypes.BASIN_MIXING, "Basin Mixing");
        } else {
            manualRecipeId = null;
            manualToolWorker = null;
            manualToolTicks = 0;
            manualToolUses = 0;
        }
    }

    /** Shared timed-tool runtime for every direct primitive recipe type. */
    private void tickManualToolRecipe(RecipeTypeDefinition type, String title) {
        if (manualToolWorker == null || manualRecipeId == null || !(getLevel() instanceof ServerLevel serverLevel)) return;
        SingleBlockMachineInstance machine = instance();
        if (machine == null) {
            manualToolWorker = null;
            return;
        }
        ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(manualToolWorker);
        Optional<RecipeHolder<CERecipe>> recipeHolder = CERecipeLookup.byId(serverLevel.getRecipeManager(), manualRecipeId)
                .filter(holder -> holder.value().recipeType().equals(type.id()))
                .filter(holder -> holder.value().matches(recipeInput(machine), serverLevel));
        CEToolRequirement requirement = recipeHolder
                .flatMap(holder -> holder.value().tools().stream().findFirst())
                .orElse(null);
        ToolVariantDefinition tool = player == null || requirement == null
                ? null
                : AssemblyTools.find(requirement.toolId(), player.getMainHandItem());
        if (player == null || tool == null
                || !manualToolMeetsTier(recipeHolder.map(RecipeHolder::value).orElse(null), tool)
                || !AssemblyUseState.isHeld(player)
                || player.distanceToSqr(worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5) > 36) {
            interruptManualTool(player);
            return;
        }

        int toolDuration = Math.max(1, tool.useTimeTicks());
        manualToolTicks = Math.min(toolDuration, manualToolTicks + 1);
        sendManualToolProgress(player, title, requirement, manualToolTicks, toolDuration);
        if (manualToolTicks < toolDuration) return;

        if (!player.isCreative() && player.getMainHandItem().isDamageableItem()) {
            player.getMainHandItem().hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        }
        manualToolUses++;
        if (manualToolUses >= requirement.amount()) {
            if (completeManualRecipe(recipeHolder.orElseThrow())) {
                manualToolUses = 0;
                manualRecipeId = null;
                PacketDistributor.sendToPlayer(player, AssemblyNextStepPayload.clear());
            }
        } else {
            PacketDistributor.sendToPlayer(player, AssemblyNextStepPayload.show(
                    title,
                    serverLevel.dimension().location().toString(),
                    worldPosition,
                    manualToolStep(requirement)
            ));
        }
        manualToolWorker = null;
        manualToolTicks = 0;
        setChangedAndSync();
    }

    private boolean completeManualRecipe(RecipeHolder<CERecipe> holder) {
        SingleBlockMachineInstance instance = instance();
        Level level = getLevel();
        if (instance == null || level == null) return false;
        CERecipe recipe = holder.value();
        CERecipeInput input = recipeInput(instance);
        if (!recipe.matches(input, level)) return false;
        List<ItemStack> itemResults = rollItemOutputs(recipe, input.processingTier());
        List<FluidStack> fluidResults = rollFluidOutputs(recipe, input.processingTier());
        if (!canFitItems(itemResults) || !canFitFluids(fluidResults)) return false;
        consumeInputs(recipe, input);
        itemResults.forEach(this::produceItemOutput);
        fluidResults.forEach(this::produceFluidOutput);
        preferredRecipeId = holder.id();
        return true;
    }

    private void interruptManualTool(ServerPlayer player) {
        if (player != null) PacketDistributor.sendToPlayer(player, AssemblyNextStepPayload.clear());
        manualToolWorker = null;
        manualToolTicks = 0;
        manualRecipeId = null;
    }

    /** Resolves the molding recipe from the material form; required counts remain recipe data. */
    private Optional<RecipeHolder<CERecipe>> moldRecipeFor(ItemStack clay) {
        Level level = getLevel();
        if (level == null || clay.isEmpty()) return Optional.empty();
        return CERecipeLookup.byType(level.getRecipeManager(), CERecipeTypes.BRICK_MOLDING).stream()
                .filter(holder -> !holder.value().itemInputs().isEmpty())
                .filter(holder -> holder.value().itemInputs().getFirst().ingredient().test(clay))
                .findFirst();
    }

    private void sendManualToolProgress(
            ServerPlayer player,
            String title,
            CEToolRequirement requirement,
            int progress,
            int duration
    ) {
        ServerLevel level = player.serverLevel();
        PacketDistributor.sendToPlayer(player, AssemblyNextStepPayload.showToolProgress(
                title,
                level.dimension().location().toString(),
                worldPosition,
                manualToolStep(requirement),
                progress,
                duration
        ));
    }

    private String manualToolStep(CEToolRequirement requirement) {
        int currentUse = Math.min(requirement.amount(), manualToolUses + 1);
        return requirement.displayName() + " " + currentUse + "/" + requirement.amount();
    }

    private static final double KILN_FUEL_UNITS_PER_TICK = 0.1D;

    private void tickKiln() {
        migrateLegacyKilnOutputs();
        ItemStack fuel = inputItems.getStackInSlot(6);

        // Fuel Units are buffered directly. Small fractional fuels therefore retain their value
        // across multiple items instead of being rounded independently to whole burn ticks.
        while (kilnFuelUnits + 1.0E-9D < KILN_FUEL_UNITS_PER_TICK && !fuel.isEmpty()) {
            double units = kilnFuelUnits(fuel);
            if (units <= 0.0D) break;
            fuel.shrink(1);
            kilnFuelUnits += units;
        }
        if (kilnFuelUnits + 1.0E-9D < KILN_FUEL_UNITS_PER_TICK) return;
        kilnFuelUnits = Math.max(0.0D, kilnFuelUnits - KILN_FUEL_UNITS_PER_TICK);

        for (int slot = 0; slot < 6; slot++) {
            ItemStack chamber = inputItems.getStackInSlot(slot);
            Optional<RecipeHolder<CERecipe>> holder = kilnProcessRecipe(chamber);
            if (holder.isEmpty()) {
                primitiveProgress[slot] = 0;
                continue;
            }

            CERecipe recipe = holder.get().value();
            int duration = recipe.duration().orElse(1);
            if (++primitiveProgress[slot] < duration) continue;

            CEChancedItemOutput output = recipe.itemOutputs().getFirst();
            inputItems.setStackInSlot(slot, output.stack().copy());
            primitiveProgress[slot] = 0;
        }
    }

    private static ItemStack materialForm(ItemStack source, MaterialPart part) {
        MaterialItem item = (MaterialItem) source.getItem();
        return new ItemStack(ItemRegistry.MATERIAL_ITEMS.get(item.material().id()).get(part).get());
    }

    private double kilnFuelUnits(ItemStack stack) {
        return FuelRecipeLookup.itemFuelUnits(getLevel(), stack);
    }

    public boolean isKiln() {
        SingleBlockMachineInstance current = instance();
        return current != null && current.definition().id().endsWith("_kiln");
    }

    private boolean isKilnProcessInput(ItemStack stack) {
        return kilnProcessRecipe(stack).isPresent();
    }

    private boolean isKilnExtractableOutput(ItemStack stack) {
        Level level = getLevel();
        if (level == null || stack.isEmpty()) return false;
        return KILN_PROCESS_TYPES.stream()
                .flatMap(type -> CERecipeLookup.byType(level.getRecipeManager(), type).stream())
                .map(RecipeHolder::value)
                .filter(SingleBlockMachineBlockEntity::validKilnProcessRecipe)
                .flatMap(recipe -> recipe.itemOutputs().stream())
                .anyMatch(output -> ItemStack.isSameItemSameComponents(output.stack(), stack));
    }

    private Optional<RecipeHolder<CERecipe>> kilnProcessRecipe(ItemStack stack) {
        Level level = getLevel();
        if (level == null || stack.isEmpty()) return Optional.empty();
        CERecipeInput input = new CERecipeInput(
                List.of(stack),
                List.of(),
                Optional.empty(),
                java.util.Set.of(),
                Optional.of(MachineTier.LV),
                Optional.empty(),
                Optional.empty(),
                MachineDrive.NONE,
                0,
                KILN_MAX_TEMPERATURE
        );
        return KILN_PROCESS_TYPES.stream()
                .flatMap(type -> CERecipeLookup.byType(level.getRecipeManager(), type).stream())
                .filter(holder -> validKilnProcessRecipe(holder.value()))
                .filter(holder -> holder.value().matches(input, level))
                .findFirst();
    }

    private static boolean validKilnProcessRecipe(CERecipe recipe) {
        if (recipe.requiredTemp().orElse(0) > KILN_MAX_TEMPERATURE) return false;
        return recipe.itemInputs().size() == 1
                && recipe.itemInputs().getFirst().count() == 1
                && recipe.chancedItemInputs().isEmpty()
                && recipe.notConsumableItems().isEmpty()
                && recipe.fluidInputs().isEmpty()
                && recipe.chancedFluidInputs().isEmpty()
                && recipe.notConsumableFluids().isEmpty()
                && recipe.itemOutputs().size() == 1
                && recipe.itemOutputs().getFirst().guaranteed()
                && recipe.itemOutputs().getFirst().stack().getCount() == 1
                && recipe.fluidOutputs().isEmpty()
                && recipe.chancedFluidOutputs().isEmpty();
    }

    private boolean isKilnFuel(ItemStack stack) {
        return kilnFuelUnits(stack) > 0.0D;
    }

    /** Moves pre-shared-slot kiln outputs into chambers as space becomes available. */
    private void migrateLegacyKilnOutputs() {
        for (int legacySlot = 0; legacySlot < 6; legacySlot++) {
            ItemStack legacyOutput = outputItems.getStackInSlot(legacySlot);
            if (legacyOutput.isEmpty()) continue;

            int target = inputItems.getStackInSlot(legacySlot).isEmpty()
                    ? legacySlot
                    : firstEmptyKilnChamber();
            if (target < 0) return;

            inputItems.setStackInSlot(target, legacyOutput.copy());
            outputItems.setStackInSlot(legacySlot, ItemStack.EMPTY);
        }
    }

    private int firstEmptyKilnChamber() {
        for (int slot = 0; slot < 6; slot++) {
            if (inputItems.getStackInSlot(slot).isEmpty()) return slot;
        }
        return -1;
    }

    public boolean primitiveInsert(Player player, InteractionHand hand, ItemStack held) {
        SingleBlockMachineInstance instance = instance();
        if (instance == null || held.isEmpty()) return false;
        String id = instance.definition().id();
        if (id.endsWith("_drying_rack") && rackDryingRecipe(held).isPresent()) {
            for (int slot = 0; slot < 4; slot++) if (inputItems.getStackInSlot(slot).isEmpty()) {
                inputItems.setStackInSlot(slot, held.copyWithCount(1));
                if (!player.isCreative()) held.shrink(1);
                return true;
            }
        }
        // Generated molding recipes resolve material definitions, including vanilla .existing forms.
        if (id.endsWith("_brick_mold") && moldRecipeFor(held).isPresent()) {
            ItemStack stored = inputItems.getStackInSlot(0);
            Optional<RecipeHolder<CERecipe>> moldingRecipe = moldRecipeFor(stored.isEmpty() ? held : stored);
            int requiredClay = moldingRecipe
                    .filter(recipe -> !recipe.value().itemInputs().isEmpty())
                    .map(recipe -> recipe.value().itemInputs().getFirst().count())
                    .orElse(0);
            if ((stored.isEmpty() || ItemStack.isSameItemSameComponents(stored, held))
                    && requiredClay > 0 && stored.getCount() < requiredClay) {
                if (stored.isEmpty()) {
                    inputItems.setStackInSlot(0, held.copyWithCount(1));
                    manualToolUses = 0;
                } else {
                    ItemStack increased = stored.copy();
                    increased.grow(1);
                    inputItems.setStackInSlot(0, increased);
                }
                if (!player.isCreative()) held.shrink(1);
                return true;
            }
        }
        if (id.endsWith("_brick_mold")) {
            return beginManualTool(player, held, CERecipeTypes.BRICK_MOLDING, "Brick Molding");
        }
        if (id.endsWith("_basin")) {
            // A valid tool always means work. Tool interaction never extracts basin contents.
            if (beginManualTool(player, held, CERecipeTypes.BASIN_MORTARING, "Basin Mortaring")) return true;
            if (beginManualTool(player, held, CERecipeTypes.BASIN_MIXING, "Basin Mixing")) return true;
            if (FluidUtil.interactWithFluidHandler(player, hand, new BasinInteractionFluidHandler())) {
                manualToolUses = 0;
                setChangedAndSync();
                return true;
            }
            return insertBasinItem(player, held);
        }
        return false;
    }

    private boolean beginManualTool(
            Player player,
            ItemStack held,
            RecipeTypeDefinition type,
            String title
    ) {
        Optional<RecipeHolder<CERecipe>> holder = matchingManualRecipe(type);
        Optional<CEToolRequirement> requirement = holder
                .flatMap(recipe -> recipe.value().tools().stream().findFirst())
                .filter(tool -> tool.matches(held));
        if (holder.isEmpty() || requirement.isEmpty()) return false;
        ToolVariantDefinition resolvedTool = AssemblyTools.find(requirement.get().toolId(), held);
        if (resolvedTool == null || !manualToolMeetsTier(holder.get().value(), resolvedTool)) return false;
        boolean continuing = player.getUUID().equals(manualToolWorker)
                && holder.get().id().equals(manualRecipeId);
        if (!continuing) {
            if (manualRecipeId != null && !manualRecipeId.equals(holder.get().id())) manualToolUses = 0;
            manualToolTicks = 0;
        }
        manualToolWorker = player.getUUID();
        manualRecipeId = holder.get().id();
        if (player instanceof ServerPlayer serverPlayer) {
            sendManualToolProgress(
                    serverPlayer,
                    title,
                    requirement.get(),
                    manualToolTicks,
                    Math.max(1, resolvedTool.useTimeTicks())
            );
        }
        return true;
    }

    private boolean manualToolMeetsTier(CERecipe recipe, ToolVariantDefinition tool) {
        if (recipe == null || tool == null) return false;
        return recipe.requiredTier()
                .map(required -> MachineTierStats.isAtLeast(tool.tier(), required))
                .orElse(true);
    }

    private Optional<RecipeHolder<CERecipe>> matchingManualRecipe(RecipeTypeDefinition type) {
        SingleBlockMachineInstance instance = instance();
        Level level = getLevel();
        if (instance == null || level == null) return Optional.empty();
        CERecipeInput input = recipeInput(instance);
        return CERecipeLookup.byType(level.getRecipeManager(), type).stream()
                .filter(holder -> !holder.value().tools().isEmpty())
                .filter(holder -> holder.value().matches(input, level))
                .filter(holder -> canFitItems(possibleItemOutputs(holder.value())))
                .filter(holder -> canFitFluids(possibleFluidOutputs(holder.value())))
                .findFirst();
    }

    private boolean insertBasinItem(Player player, ItemStack held) {
        ItemStack oneItem = held.copyWithCount(1);
        ItemStack remainder = insertBasinStack(oneItem);
        if (remainder.getCount() == oneItem.getCount()) return false;
        if (!player.isCreative()) held.shrink(1);
        manualToolUses = 0;
        setChangedAndSync();
        return true;
    }

    /** Pulls every dropped item whose center is physically inside the Basin opening. */
    private void tickBasinDroppedItems() {
        Level level = getLevel();
        if (level == null || level.isClientSide()) return;
        double minX = worldPosition.getX() + 3.0D / 16.0D;
        double minY = worldPosition.getY() + 2.0D / 16.0D;
        double minZ = worldPosition.getZ() + 3.0D / 16.0D;
        AABB opening = new AABB(
                minX, minY, minZ,
                worldPosition.getX() + 13.0D / 16.0D,
                worldPosition.getY() + 12.0D / 16.0D,
                worldPosition.getZ() + 13.0D / 16.0D
        );
        boolean changed = false;
        for (ItemEntity entity : level.getEntitiesOfClass(
                ItemEntity.class,
                opening,
                candidate -> candidate.isAlive() && !candidate.getItem().isEmpty()
        )) {
            ItemStack original = entity.getItem();
            ItemStack remainder = insertBasinStack(original);
            if (remainder.getCount() == original.getCount()) continue;
            if (remainder.isEmpty()) {
                entity.discard();
            } else {
                entity.setItem(remainder);
            }
            changed = true;
        }
        if (changed) {
            manualToolUses = 0;
            setChangedAndSync();
        }
    }

    /** Shared unfiltered Basin insertion used by players and dropped ItemEntities. */
    private ItemStack insertBasinStack(ItemStack offered) {
        ItemStack remainder = offered.copy();
        for (int slot = 0; slot < itemInputSlotCount() && !remainder.isEmpty(); slot++) {
            remainder = inputItems.insertItem(slot, remainder, false);
        }
        return remainder;
    }

    public boolean primitiveExtract(Player player) {
        SingleBlockMachineInstance instance = instance();
        if (instance == null || instance.definition().id().endsWith("_kiln")) return false;
        String id = instance.definition().id();
        int slots = id.endsWith("_drying_rack") ? 4 : itemOutputSlotCount();
        for (int slot = 0; slot < slots; slot++) {
            ItemStack result = outputItems.extractItem(slot, 1, false);
            if (!result.isEmpty()) {
                if ((id.endsWith("_brick_mold") || id.endsWith("_basin")) && player instanceof ServerPlayer serverPlayer) {
                    PacketDistributor.sendToPlayer(serverPlayer, AssemblyNextStepPayload.clear());
                }
                if (!player.getInventory().add(result)) player.drop(result, false);
                return true;
            }
        }
        if (id.endsWith("_drying_rack")) {
            for (int slot = 0; slot < 4; slot++) {
                ItemStack returned = inputItems.extractItem(slot, 1, false);
                if (returned.isEmpty()) continue;
                primitiveProgress[slot] = 0;
                if (!player.getInventory().add(returned)) player.drop(returned, false);
                setChangedAndSync();
                return true;
            }
        }
        if (id.endsWith("_basin")) {
            for (int slot = itemInputSlotCount() - 1; slot >= 0; slot--) {
                ItemStack returned = inputItems.extractItem(slot, 1, false);
                if (returned.isEmpty()) continue;
                if (!player.getInventory().add(returned)) player.drop(returned, false);
                manualToolUses = 0;
                interruptManualTool(player instanceof ServerPlayer serverPlayer ? serverPlayer : null);
                setChangedAndSync();
                return true;
            }
        }
        return false;
    }

    private void explodeFromOvervoltage() {
        if (level == null || level.isClientSide()) {
            return;
        }

        level.explode(
                null,
                getBlockPos().getX() + 0.5D,
                getBlockPos().getY() + 0.5D,
                getBlockPos().getZ() + 0.5D,
                4.0F,
                Level.ExplosionInteraction.TNT
        );
    }

    public IItemHandler itemCapability(Direction side) {
        SingleBlockMachineInstance instance = instance();
        if (instance == null || (side == Direction.UP
                && instance.definition().power() == SingleBlockMachinePower.STEAM)) return null;
        if (side == null) return new MachineItemHandler(null);
        MachineSide machineSide = relativeSide(side);
        if (instance == null || (!instance.definition().allowsItemInput(machineSide)
                && !instance.definition().allowsItemOutput(machineSide))) return null;
        return new MachineItemHandler(machineSide);
    }

    public IFluidHandler fluidCapability(Direction side) {
        SingleBlockMachineInstance instance = instance();
        if (instance == null
                || (instance.definition().resource() != SingleBlockMachineResource.STEAM
                && inputFluidSlotCount() <= 0
                && outputFluidSlotCount() <= 0)) {
            return null;
        }

        if (side == Direction.UP
                && instance.definition().power() == SingleBlockMachinePower.STEAM
                && instance.definition().resourceMode() == SingleBlockMachineResourceMode.CONSUMES) {
            return null;
        }

        if (side == null) return new MachineFluidHandler(null);
        MachineSide machineSide = relativeSide(side);
        if (!instance.definition().allowsFluidInput(machineSide)
                && !instance.definition().allowsFluidOutput(machineSide)) return null;
        return new MachineFluidHandler(machineSide);
    }

    private MachineSide relativeSide(Direction side) {
        if (side == Direction.UP) return MachineSide.TOP;
        if (side == Direction.DOWN) return MachineSide.BOTTOM;
        Direction facing = getBlockState().getValue(SingleBlockMachineBlock.FACING);
        if (side == facing) return MachineSide.FRONT;
        if (side == facing.getOpposite()) return MachineSide.BACK;
        if (side == facing.getClockWise()) return MachineSide.RIGHT;
        return MachineSide.LEFT;
    }

    public CEEnergyContainer ceContainer() {
        return ceContainer;
    }

    public ItemStackHandler inputItems() {
        return inputItems;
    }

    public ItemStackHandler outputItems() {
        return outputItems;
    }

    public List<FluidTank> inputFluidTanks() {
        return visibleTanks(inputFluids, inputFluidSlotCount());
    }

    public List<FluidTank> outputFluidTanks() {
        return visibleTanks(outputFluids, outputFluidSlotCount());
    }

    public int itemInputSlotCount() {
        SingleBlockMachineInstance instance = instance();
        return instance == null ? 0 : Math.min(MAX_ITEM_INPUT_SLOTS, instance.definition().slots().itemInputs());
    }

    public int itemOutputSlotCount() {
        SingleBlockMachineInstance instance = instance();
        return instance == null ? 0 : Math.min(MAX_ITEM_OUTPUT_SLOTS, instance.definition().slots().itemOutputs());
    }

    public int inputFluidSlotCount() {
        SingleBlockMachineInstance instance = instance();
        return instance == null ? 0 : Math.min(MAX_FLUID_INPUT_TANKS, instance.definition().slots().fluidInputs());
    }

    public int outputFluidSlotCount() {
        SingleBlockMachineInstance instance = instance();
        return instance == null ? 0 : Math.min(MAX_FLUID_OUTPUT_TANKS, instance.definition().slots().fluidOutputs());
    }

    public int steamStored() {
        return steamTank.getFluidAmount();
    }

    public int steamCapacity() {
        SingleBlockMachineInstance instance = instance();
        return instance == null ? 0 : instance.steamCapacity();
    }

    public int steamUsage() {
        SingleBlockMachineInstance instance = instance();
        return instance == null ? 0 : instance.steamUsage();
    }

    public int temperature() {
        return temperature;
    }

    public int minimumOperatingTemperature() {
        SingleBlockMachineInstance instance = instance();
        SingleBlockDefinition.TemperatureSettings settings = instance == null
                ? null
                : instance.definition().temperature();
        return settings == null ? 0 : settings.minimumOperatingTemperature();
    }

    public int maximumTemperature() {
        SingleBlockMachineInstance instance = instance();
        SingleBlockDefinition.TemperatureSettings settings = instance == null
                ? null
                : instance.definition().temperature();
        return settings == null ? 0 : settings.maximumTemperature();
    }

    public long energyStored() {
        return energyStored;
    }

    public long energyCapacity() {
        SingleBlockMachineInstance instance = instance();
        if (instance == null || instance.definition().power() != SingleBlockMachinePower.ELECTRIC) {
            return 0;
        }

        return MachineTierStats.ceCapacity(instance.tier());
    }

    public int energyUsage() {
        SingleBlockMachineInstance instance = instance();
        if (instance == null) {
            return 0;
        }

        return instance.energyUsage();
    }

    private boolean acceptsEnergyInput() {
        SingleBlockMachineInstance instance = instance();
        return instance != null
                && instance.definition().resource() == SingleBlockMachineResource.ENERGY
                && instance.definition().resourceMode() == SingleBlockMachineResourceMode.CONSUMES;
    }

    private boolean allowsEnergyOutput() {
        SingleBlockMachineInstance instance = instance();
        return instance != null
                && instance.definition().resource() == SingleBlockMachineResource.ENERGY
                && instance.definition().resourceMode() == SingleBlockMachineResourceMode.PRODUCES;
    }

    public int progress() {
        return recipeLogic.progress();
    }

    public int progressTotal() {
        return recipeLogic.duration();
    }

    public ProgressBar progressBar() {
        SingleBlockMachineInstance instance = instance();
        return instance == null ? ProgressBar.ARROW : instance.definition().resolvedProgressBar();
    }

    public String activeRecipeDisplay() {
        CERecipeExecution execution = recipeLogic.execution();
        return execution == null ? "" : execution.recipeType().toString();
    }

    private Optional<RecipeHolder<CERecipe>> findRecipe(Level level, SingleBlockMachineInstance instance) {
        return findRecipe(level, instance, false); // Control previews must not consume a pending search.
    }
    private Optional<RecipeHolder<CERecipe>> findRecipe(Level level, SingleBlockMachineInstance instance, boolean asynchronous) {
        long performanceStart = net.mads.industron.debug.CEPerformanceProfiler.begin(level);
        try {
        CERecipeInput input = recipeInput(instance);
        InteractionContext context = interactionContext(level, instance);

        if (preferredRecipeId != null) {
            Optional<RecipeHolder<CERecipe>> preferred = CERecipeLookup.preferred(
                            level.getRecipeManager(),
                            preferredRecipeId,
                            java.util.Set.copyOf(instance.definition().recipeTypes())
                    )
                    .filter(holder -> holder.value().tools().isEmpty())
                    .filter(holder -> validGeneratorFuel(instance, holder.value()))
                    .filter(this::runtimeMixtureReady)
                    .filter(holder -> instance.definition().usesKineticInput()
                        ? holder.value().matchesIgnoringRpm(input, level)
                        : holder.value().matches(input, level))
                    .filter(holder -> holder.value().chemicalBalanceRange().isEmpty())
                    .filter(holder -> kineticRecipeReady(instance, holder.value()))
                    .filter(holder -> treeSourceMatches(level, instance, holder.value()))
                    .filter(holder -> startInteractionsReady(instance, holder.value(), context));

            if (preferred.isPresent()) {
                if (asynchronous) asyncSearch.discardFinished();
                return preferred;
            }
        }

        return (asynchronous && !level.isClientSide()
                ? asyncSearch.candidates(level.getRecipeManager(), CERecipeLookup.candidatesByTypes(level.getRecipeManager(), instance.definition().recipeTypes(), input), input, machineControlInputRevision)
                : CERecipeLookup.candidatesByTypes(level.getRecipeManager(), instance.definition().recipeTypes(), input))
                .stream()
                .filter(holder -> holder.value().tools().isEmpty())
                .filter(holder -> validGeneratorFuel(instance, holder.value()))
                .filter(this::runtimeMixtureReady)
                .filter(holder -> instance.definition().usesKineticInput()
                        ? holder.value().matchesIgnoringRpm(input, level)
                        : holder.value().matches(input, level))
                .filter(holder -> holder.value().chemicalBalanceRange().isEmpty())
                .filter(holder -> kineticRecipeReady(instance, holder.value()))
                .filter(holder -> treeSourceMatches(level, instance, holder.value()))
                .filter(holder -> startInteractionsReady(instance, holder.value(), context))
                .sorted(java.util.Comparator.comparing(holder -> holder.id().toString()))
                .findFirst();
            } finally {
            net.mads.industron.debug.CEPerformanceProfiler.record(net.mads.industron.debug.CEPerformanceProfiler.Metric.RECIPE_LOOKUP, performanceStart);
        }
    }

    private static boolean isSteamFuelGenerator(SingleBlockMachineInstance instance) {
        return instance.definition().matchesRecipeType(CERecipeTypes.FUEL.id())
                && instance.definition().resource() == SingleBlockMachineResource.STEAM
                && instance.definition().resourceMode() == SingleBlockMachineResourceMode.PRODUCES;
    }

    private static boolean validGeneratorFuel(SingleBlockMachineInstance instance, CERecipe recipe) {
        if (!isSteamFuelGenerator(instance)) return true;
        double units = recipe.fuelUnits().orElse(0.0D);
        if (!Double.isFinite(units) || units <= 0.0D
                || !recipe.chancedItemInputs().isEmpty() || !recipe.chancedFluidInputs().isEmpty()) {
            return false;
        }
        if (instance.definition().slots().itemInputs() > 0) {
            return recipe.itemInputs().size() == 1
                    && recipe.itemInputs().getFirst().count() == 1
                    && recipe.fluidInputs().isEmpty();
        }
        // Liquid fuel recipes use one 144 mB material unit; gas fuels use a different unit.
        return recipe.itemInputs().isEmpty() && recipe.fluidInputs().size() == 1
                && recipe.fluidInputs().getFirst().amount() ==
                net.mads.industron.material.MaterialUnits.LIQUID_MILLIBUCKETS_PER_UNIT;
    }

    private void wakeRuntimeMixtureSearchIfReady(Level level, SingleBlockMachineInstance instance) {
        if (recipeLogic.isProcessing()
                || !instance.definition().recipeTypes().contains(CERecipeTypes.CENTRIFUGING.id())) {
            return;
        }

        for (int i = 0; i < inputFluidSlotCount(); i++) {
            FluidStack fluid = inputFluids[i].getFluid();
            if (!FoundryMetallurgy.unidentified(fluid) || fluid.isEmpty()) continue;

            MoltenRatio ratio = fluid.get(FoundryComponents.COMPOSITION.get());
            if (ratio == null || ratio.weights().size() < 2) continue;

            int preferred = MixtureCentrifuging.preferredBatchAmount(ratio);
            if (fluid.getAmount() >= preferred
                    || level.getGameTime() > unidentifiedMixtureLastChangeTick[i]) {
                recipeLogic.requestImmediateSearch();
                return;
            }
        }
    }

    /**
     * Small-batch waiting is exclusive to dynamically generated unidentified-mixture recipes.
     * Registered centrifuge recipes and every other recipe type keep their normal start behavior.
     */
    private boolean runtimeMixtureReady(RecipeHolder<CERecipe> holder) {
        Optional<MixtureCentrifuging.RuntimeKey> runtime = MixtureCentrifuging.runtimeKey(holder.id());
        if (runtime.isEmpty()) return true;

        Level currentLevel = getLevel();
        if (currentLevel == null) return false;

        MixtureCentrifuging.RuntimeKey key = runtime.get();
        for (int i = 0; i < inputFluidSlotCount(); i++) {
            FluidStack fluid = inputFluids[i].getFluid();
            if (!FoundryMetallurgy.unidentified(fluid) || fluid.getAmount() < key.amount()) continue;

            MoltenRatio ratio = fluid.get(FoundryComponents.COMPOSITION.get());
            if (ratio == null || !ratio.equals(key.ratio())) continue;
            if (FoundryMetallurgy.temperature(fluid) != key.temperature()) continue;

            int preferred = MixtureCentrifuging.preferredBatchAmount(ratio);
            if (fluid.getAmount() >= preferred) return true;

            // Below the preferred ~1-second batch: only start after the mixture stopped changing.
            if (currentLevel.getGameTime() > unidentifiedMixtureLastChangeTick[i]) return true;
        }
        return false;
    }

    private boolean treeSourceMatches(
            Level level,
            SingleBlockMachineInstance instance,
            CERecipe recipe
    ) {
        if (recipe.treeSource().isEmpty()) {
            return true;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        Optional<BlockInteraction> treeInteraction = instance.definition().blockInteractions()
                .stream()
                .filter(interaction -> interaction.type() == BlockInteraction.Type.TREE_EXTRACT)
                .findFirst();

        if (treeInteraction.isEmpty()) {
            treeInteraction = recipe.blockInteractions()
                    .stream()
                    .filter(interaction -> interaction.type() == BlockInteraction.Type.TREE_EXTRACT)
                    .findFirst();
        }

        if (treeInteraction.isEmpty()) {
            return false;
        }

        Direction facing = getBlockState().hasProperty(SingleBlockMachineBlock.FACING)
                ? getBlockState().getValue(SingleBlockMachineBlock.FACING)
                : Direction.NORTH;

        BlockPos rootPos = treeInteraction.get().pos().rotate(
                getBlockPos(),
                facing
        );

        return TreeExtractionSavedData.get(serverLevel)
                .getTree(rootPos)
                .map(tree -> tree.logId().equals(recipe.treeSource().get()))
                .orElse(false);
    }

    private boolean startInteractionsReady(SingleBlockMachineInstance instance, CERecipe recipe, InteractionContext context) {
        return InteractionRuntime.conditionsMatch(instance.definition().conditions(), context, InteractionPhase.ON_START)
                && InteractionRuntime.conditionsMatch(recipe.conditions(), context, InteractionPhase.ON_START)
                && InteractionRuntime.interactionsMatch(instance.definition().blockInteractions(), context, InteractionPhase.ON_START)
                && InteractionRuntime.interactionsMatch(recipe.blockInteractions(), context, InteractionPhase.ON_START)
                && InteractionRuntime.interactionsMatch(instance.definition().blockInteractions(), context, InteractionPhase.ON_COMPLETE)
                && InteractionRuntime.interactionsMatch(recipe.blockInteractions(), context, InteractionPhase.ON_COMPLETE);
    }

    private CERecipeInput recipeInput(SingleBlockMachineInstance instance) {
        MachineDrive drive = switch (instance.definition().power()) {
            case ELECTRIC -> MachineDrive.ELECTRIC;
            case STEAM -> MachineDrive.STEAM;
            case KINETIC -> instance.definition().usesKineticOutput()
                    ? MachineDrive.KINETIC_OUTPUT
                    : MachineDrive.KINETIC;
            case NONE -> MachineDrive.NONE;
        };
        Optional<MachineTier> processingTier = drive == MachineDrive.NONE || instance.tier() == MachineTier.NONE
                ? Optional.empty()
                : Optional.of(instance.tier().recipeTier());
        int rpm = drive.usesKineticInput() ? kineticRpm() : 0;
        int availableHeat = instance.definition().matchesRecipeType(CERecipeTypes.HEATING.id())
                ? 500 * (MachineTierStats.tierIndex(instance.tier().recipeTier()) + 1)
                : 0;

        return new CERecipeInput(
                visibleItemInputs(),
                visibleFluidInputs(),
                circuit > 0 ? Optional.of(circuit) : Optional.empty(),
                java.util.Set.of(),
                processingTier,
                drive.usesKineticInput() ? processingTier : Optional.empty(),
                drive == MachineDrive.ELECTRIC ? processingTier : Optional.empty(),
                drive,
                rpm,
                availableHeat
        );
    }

    public int kineticRpm() {
        return Math.round(Math.abs(getSpeed()));
    }

    public int recipeMinimumRpm() {
        return activeRecipeForKineticLimits().flatMap(CERecipe::minRpm).orElse(0);
    }

    public int recipeMaximumRpm() {
        return activeRecipeForKineticLimits().flatMap(CERecipe::maxRpm).orElse(0);
    }

    public int machineRpm() {
        SingleBlockMachineInstance instance = instance();
        return instance != null && instance.definition().usesKineticInput() ? kineticRpm() : 0;
    }

    public double kineticSuPerRpm() {
        SingleBlockMachineInstance instance = instance();
        return instance == null ? 0.0D : instance.kineticSuPerRpm();
    }

    public Optional<Integer> kineticMinimumRpm() {
        return effectiveMinimumRpm(activeRecipeForKineticLimits().orElse(null));
    }

    public Optional<Integer> kineticMaximumRpm() {
        return effectiveMaximumRpm(activeRecipeForKineticLimits().orElse(null));
    }

    public KineticRpmError kineticRpmError() {
        SingleBlockMachineInstance instance = instance();
        if (instance == null || !instance.definition().usesKineticInput()) {
            return KineticRpmError.NONE;
        }
        return kineticRpmError(activeRecipeForKineticLimits().orElse(null));
    }

    public int kineticOutputRpm() {
        if (!machineEnabled) {
            return 0;
        }

        SingleBlockMachineInstance instance = instance();
        if (instance == null || !instance.definition().usesKineticOutput()) {
            return 0;
        }

        CERecipeExecution execution = recipeLogic.execution();
        if (!recipeLogic.isActive() || execution == null) {
            return 0;
        }
        return Math.max(0, Math.min(
                CERecipe.DEFAULT_MAX_RPM,
                execution.machineData().getInt(KINETIC_OUTPUT_RPM_DATA)
        ));
    }

    private Optional<CERecipe> activeRecipeForKineticLimits() {
        CERecipeExecution execution = recipeLogic.execution();
        return execution == null
                ? Optional.empty()
                : recipeById(execution.recipeId());
    }

    private boolean hasKineticInputPower() {
        SingleBlockMachineInstance instance = instance();
        return instance != null
                && instance.definition().usesKineticInput()
                && Math.abs(getSpeed()) >= 1.0F
                && isSpeedRequirementFulfilled();
    }

    private KineticRpmError kineticRpmError(@Nullable CERecipe recipe) {
        int rpm = kineticRpm();
        int minimum = effectiveMinimumRpm(recipe).orElse(1);
        int maximum = effectiveMaximumRpm(recipe).orElse(CERecipe.DEFAULT_MAX_RPM);

        if (rpm > maximum) {
            return KineticRpmError.TOO_AGGRESSIVE;
        }
        if (!hasKineticInputPower() || rpm < minimum) {
            return KineticRpmError.INSUFFICIENT;
        }
        return KineticRpmError.NONE;
    }

    private Optional<Integer> effectiveMinimumRpm(@Nullable CERecipe recipe) {
        SingleBlockMachineInstance instance = instance();
        Optional<Integer> machineMinimum = instance == null
                ? Optional.empty()
                : instance.definition().minRpm();
        Optional<Integer> recipeMinimum = recipe == null
                ? Optional.empty()
                : recipe.minRpm();
        if (machineMinimum.isPresent() && recipeMinimum.isPresent()) {
            return Optional.of(Math.max(machineMinimum.get(), recipeMinimum.get()));
        }
        return machineMinimum.isPresent() ? machineMinimum : recipeMinimum;
    }

    private Optional<Integer> effectiveMaximumRpm(@Nullable CERecipe recipe) {
        SingleBlockMachineInstance instance = instance();
        Optional<Integer> machineMaximum = instance == null
                ? Optional.empty()
                : instance.definition().maxRpm();
        Optional<Integer> recipeMaximum = recipe == null
                ? Optional.empty()
                : recipe.maxRpm();
        if (machineMaximum.isPresent() && recipeMaximum.isPresent()) {
            return Optional.of(Math.min(machineMaximum.get(), recipeMaximum.get()));
        }
        return machineMaximum.isPresent() ? machineMaximum : recipeMaximum;
    }

    private boolean kineticRecipeReady(
            SingleBlockMachineInstance instance,
            CERecipe recipe
    ) {
        if (instance.definition().usesKineticInput()) {
            int minimum = effectiveMinimumRpm(recipe).orElse(1);
            int maximum = effectiveMaximumRpm(recipe).orElse(CERecipe.DEFAULT_MAX_RPM);
            return minimum <= maximum;
        }
        if (instance.definition().usesKineticOutput()) {
            return resolvedOutputRpm(instance, recipe) > 0;
        }
        return true;
    }

    private static int resolvedOutputRpm(
            SingleBlockMachineInstance instance,
            CERecipe recipe
    ) {
        return recipe.outputRpm()
                .or(() -> instance.definition().outputRpm())
                .orElse(0);
    }

    private void refreshGeneratedRotation() {
        SingleBlockMachineInstance instance = instance();
        if (level == null
                || level.isClientSide()
                || instance == null
                || !instance.definition().usesKineticOutput()) {
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
    public float getGeneratedSpeed() {
        SingleBlockMachineInstance instance = instance();
        if (instance == null || !instance.definition().usesKineticOutput()) {
            return 0.0F;
        }

        int rpm = kineticOutputRpm();
        if (rpm <= 0
                || !(getBlockState().getBlock() instanceof SingleBlockMachineBlock block)) {
            return 0.0F;
        }

        Direction output = block.kineticDirection(getBlockState());
        return output == null ? 0.0F : convertToDirection(rpm, output);
    }

    @Override
    public float calculateStressApplied() {
        SingleBlockMachineInstance instance = instance();
        if (instance != null && instance.definition().usesKineticInput()) {
            float stress = (float) Math.min(Float.MAX_VALUE, instance.kineticSuPerRpm());
            this.lastStressApplied = stress;
            return stress;
        }
        return super.calculateStressApplied();
    }

    @Override
    public float calculateAddedStressCapacity() {
        SingleBlockMachineInstance instance = instance();
        if (instance != null && instance.definition().usesKineticOutput()) {
            float capacity = (float) Math.min(Float.MAX_VALUE, instance.kineticSuPerRpm());
            this.lastCapacityProvided = capacity;
            return capacity;
        }
        return super.calculateAddedStressCapacity();
    }

    private int adjustedDuration(
            SingleBlockMachineInstance instance,
            CERecipe recipe,
            CERecipeInput input
    ) {
        // A FUEL recipe contains an amount of heat, not a processing duration.
        // Preserve that amount across tier/drive/rate changes: one Fuel Unit supplies
        // nominally 1600 mB steam, before startup/idle losses and tick rounding.
        // A faster burner uses fuel faster.
        // In particular, the Steam tier's slower processing must not multiply free boiler heat.
        if (isSteamFuelGenerator(instance)) {
            double ticks = recipe.fuelUnits().orElse(0.0D) * 1600.0D / Math.max(1, instance.steamUsage());
            return (int) Math.max(1.0D, Math.min(Integer.MAX_VALUE, Math.ceil(ticks)));
        }

        MachineTier runtimeTier = input.processingTier()
                .orElse(instance.tier().recipeTier());
        int durationRpm = input.rpm();
        if (instance.definition().usesKineticInput()) {
            int minimum = effectiveMinimumRpm(recipe).orElse(1);
            int maximum = effectiveMaximumRpm(recipe).orElse(CERecipe.DEFAULT_MAX_RPM);
            durationRpm = Math.max(minimum, Math.min(maximum, Math.max(1, durationRpm)));
        }
        int duration = recipe.runtimeDuration(runtimeTier, input.drive(), durationRpm, input);

        /*
         * Generator recipe types do not always define a required recipe tier. In that
         * case, use the generator definition's first generated tier as the baseline so
         * every higher machine tier still halves the recipe duration, matching electric
         * singleblock overclocking. Recipes with an explicit required tier are already
         * overclocked by CERecipe.runtimeDuration(...), so they must not be divided twice.
         */
        if (instance.definition().usesKineticOutput() && recipe.requiredTier().isEmpty()) {
            int factor = MachineTierStats.tierOverclockFactor(
                    instance.definition().startTier().recipeTier(),
                    instance.tier().recipeTier()
            );
            duration = Math.max(1, (duration + factor - 1) / factor);
        }

        duration = instance.definition().processingProfile(instance.tier()).duration(duration);

        return Math.max(1, duration);
    }

    private static int resourcePerTick(SingleBlockMachineInstance instance, CERecipe recipe) {
        int amount;

        if (instance.definition().resource() == SingleBlockMachineResource.STEAM) {
            amount = instance.steamUsage();
        } else if (instance.definition().resource() == SingleBlockMachineResource.ENERGY) {
            amount = instance.energyUsage();
        } else {
            return 0;
        }

        return instance.definition().resourceMode() == SingleBlockMachineResourceMode.PRODUCES
                ? -Math.abs(amount)
                : Math.abs(amount);
    }

    private Optional<CERecipe> recipeById(ResourceLocation recipeId) {
        Level level = getLevel();
        if (level == null) {
            return Optional.empty();
        }

        return CERecipeLookup.byId(level.getRecipeManager(), recipeId).map(RecipeHolder::value);
    }

    private boolean canProcessResource(SingleBlockMachineInstance instance, int amount) {
        if (instance.definition().usesKineticInput()
                && !hasKineticInputPower()) {
            return false;
        }

        if (amount == 0) {
            return true;
        }

        if (instance.definition().resource() == SingleBlockMachineResource.STEAM) {
            return amount > 0
                    ? steamTank.getFluidAmount() >= amount
                    : steamTank.getFluidAmount() - amount <= steamTank.getCapacity();
        }

        if (instance.definition().resource() == SingleBlockMachineResource.ENERGY) {
            return amount > 0
                    ? energyStored >= amount
                    : energyStored - amount <= energyCapacity();
        }

        return true;
    }

    private List<ItemStack> rollItemOutputs(CERecipe recipe, int parallel, Optional<MachineTier> runtimeTier) {
        List<ItemStack> outputs = new ArrayList<>();
        for (int run = 0; run < Math.max(1, parallel); run++) outputs.addAll(rollItemOutputsOne(recipe, runtimeTier));
        return outputs;
    }

    private List<ItemStack> rollItemOutputsOne(
            CERecipe recipe,
            Optional<MachineTier> runtimeTier
    ) {
        List<ItemStack> outputs = new ArrayList<>();
        Level level = getLevel();

        if (level == null) {
            return outputs;
        }

        for (CEChancedItemOutput output : recipe.itemOutputs()) {
            int chance = output.effectiveChance(runtimeTier, recipe.requiredTier());
            if (chance >= CEChancedItemOutput.MAX_CHANCE
                    || level.random.nextInt(CEChancedItemOutput.MAX_CHANCE) < chance) {
                outputs.add(output.stack().copy());
            }
        }

        return outputs;
    }


    private List<ItemStack> rollItemOutputs(CERecipe recipe, Optional<MachineTier> runtimeTier) { return rollItemOutputs(recipe, 1, runtimeTier); }
    private List<FluidStack> rollFluidOutputs(CERecipe recipe, Optional<MachineTier> runtimeTier) { return rollFluidOutputs(recipe, 1, runtimeTier); }

    private static List<ItemStack> possibleItemOutputs(CERecipe recipe) {
        return recipe.itemOutputs().stream()
                .map(CEChancedItemOutput::stack)
                .map(ItemStack::copy)
                .toList();
    }

    private static List<FluidStack> possibleFluidOutputs(CERecipe recipe) {
        List<FluidStack> outputs = recipe.fluidOutputs().stream().map(FluidStack::copy).collect(Collectors.toCollection(ArrayList::new));
        recipe.chancedFluidOutputs().stream().map(output -> output.stack().copy()).forEach(outputs::add);
        return outputs;
    }

    private List<FluidStack> rollFluidOutputs(CERecipe recipe, int parallel, Optional<MachineTier> runtimeTier) {
        List<FluidStack> outputs = new ArrayList<>();
        for (int run = 0; run < Math.max(1, parallel); run++) outputs.addAll(rollFluidOutputsOne(recipe, runtimeTier));
        return outputs;
    }

    private List<FluidStack> rollFluidOutputsOne(CERecipe recipe, Optional<MachineTier> runtimeTier) {
        List<FluidStack> outputs = recipe.fluidOutputs().stream().map(FluidStack::copy).collect(Collectors.toCollection(ArrayList::new));
        Level level = getLevel();
        if (level == null) return outputs;
        for (CEChancedFluidOutput output : recipe.chancedFluidOutputs()) {
            int chance = output.effectiveChance(runtimeTier, recipe.requiredTier());
            if (chance >= CEChancedFluidOutput.MAX_CHANCE || level.random.nextInt(CEChancedFluidOutput.MAX_CHANCE) < chance) {
                outputs.add(output.stack().copy());
            }
        }
        return outputs;
    }

    private boolean canFitItems(List<ItemStack> outputs) {
        List<ItemStack> simulated = new ArrayList<>();

        for (int slot = 0; slot < itemOutputSlotCount(); slot++) {
            simulated.add(outputItems.getStackInSlot(slot).copy());
        }

        for (ItemStack output : outputs) {
            ItemStack remaining = output.copy();

            for (int slot = 0; slot < simulated.size() && !remaining.isEmpty(); slot++) {
                ItemStack current = simulated.get(slot);

                if (current.isEmpty()) {
                    int moved = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                    simulated.set(slot, remaining.copyWithCount(moved));
                    remaining.shrink(moved);
                } else if (ItemStack.isSameItemSameComponents(current, remaining)) {
                    int moved = Math.min(remaining.getCount(), current.getMaxStackSize() - current.getCount());
                    current.grow(moved);
                    remaining.shrink(moved);
                }
            }

            if (!remaining.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    private boolean canFitFluids(List<FluidStack> outputs) {
        List<FluidStack> simulated = new ArrayList<>();

        for (int slot = 0; slot < outputFluidSlotCount(); slot++) {
            simulated.add(outputFluids[slot].getFluid().copy());
        }

        for (FluidStack output : outputs) {
            FluidStack remaining = output.copy();

            for (int slot = 0; slot < simulated.size() && !remaining.isEmpty(); slot++) {
                FluidStack current = simulated.get(slot);
                int capacity = outputFluids[slot].getCapacity();

                if (current.isEmpty()) {
                    int moved = Math.min(remaining.getAmount(), capacity);
                    simulated.set(slot, remaining.copyWithAmount(moved));
                    remaining.shrink(moved);
                } else if (FluidStack.isSameFluidSameComponents(current, remaining)) {
                    int moved = Math.min(remaining.getAmount(), capacity - current.getAmount());
                    current.grow(moved);
                    remaining.shrink(moved);
                }
            }

            if (!remaining.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    private int maxRecipeParallel(SingleBlockMachineInstance instance, CERecipe recipe, CERecipeInput input) {
        int max = Math.max(1, instance.definition().maxParallel());
        for (int parallel = max; parallel > 1; parallel--) {
            if (hasParallelConsumables(recipe, input, parallel)) return parallel;
        }
        return 1;
    }

    private boolean hasParallelConsumables(CERecipe recipe, CERecipeInput input, int parallel) {
        List<ItemStack> items = input.items().stream().map(ItemStack::copy).collect(Collectors.toCollection(ArrayList::new));
        List<FluidStack> fluids = input.fluids().stream().map(FluidStack::copy).collect(Collectors.toCollection(ArrayList::new));
        for (SizedIngredient req : recipe.itemInputs()) if (!takeItems(items, req.ingredient(), req.count() * parallel)) return false;
        for (CEChancedItemInput req : recipe.chancedItemInputs()) if (!takeItems(items, req.ingredient().ingredient(), req.ingredient().count() * parallel)) return false;
        if (recipe.furnaceFuel()) {
            ItemStack fuel = recipe.furnaceFuelStack(input).orElse(ItemStack.EMPTY);
            if (fuel.isEmpty() || !takeItems(items, Ingredient.of(fuel.getItem()), parallel)) return false;
        }
        for (SizedFluidIngredient req : recipe.fluidInputs()) if (!takeFluids(fluids, req.ingredient(), req.amount() * parallel)) return false;
        for (CEChancedFluidInput req : recipe.chancedFluidInputs()) if (!takeFluids(fluids, req.ingredient().ingredient(), req.ingredient().amount() * parallel)) return false;
        return true;
    }

    private static boolean takeItems(List<ItemStack> stacks, net.minecraft.world.item.crafting.Ingredient ingredient, int amount) {
        int remaining = amount;
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty() && ingredient.test(stack)) { int take = Math.min(remaining, stack.getCount()); stack.shrink(take); remaining -= take; if (remaining <= 0) return true; }
        }
        return remaining <= 0;
    }

    private static boolean takeFluids(List<FluidStack> stacks, net.neoforged.neoforge.fluids.crafting.FluidIngredient ingredient, int amount) {
        int remaining = amount;
        for (FluidStack stack : stacks) {
            if (!stack.isEmpty() && ingredient.test(stack)) { int take = Math.min(remaining, stack.getAmount()); stack.shrink(take); remaining -= take; if (remaining <= 0) return true; }
        }
        return remaining <= 0;
    }

    private static int multiplyClamped(int value, int multiplier) {
        long result = (long) value * Math.max(1, multiplier);
        if (result > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        if (result < Integer.MIN_VALUE) return Integer.MIN_VALUE;
        return (int) result;
    }

    private void consumeInputs(CERecipe recipe, CERecipeInput input) {
        if (recipe.furnaceFuel()) {
            recipe.furnaceFuelStack(input).ifPresent(this::consumeOneFuelItem);
        }
        recipe.itemInputs().forEach(this::consumeItemInput);
        recipe.chancedItemInputs().forEach(chancedInput ->
                consumeChancedItemInput(chancedInput, input.processingTier(), recipe.requiredTier()));
        recipe.fluidInputs().forEach(this::consumeFluidInput);
        recipe.chancedFluidInputs().forEach(chancedInput ->
                consumeChancedFluidInput(chancedInput, input.processingTier(), recipe.requiredTier()));
    }

    private void consumeOneFuelItem(ItemStack fuel) {
        for (int slot = 0; slot < itemInputSlotCount(); slot++) {
            ItemStack stack = inputItems.getStackInSlot(slot);
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, fuel)) {
                inputItems.extractItem(slot, 1, false);
                return;
            }
        }
    }

    private void consumeItemInput(SizedIngredient input) {
        consumeItemInput(input.ingredient(), input.count());
    }

    private void consumeItemInput(Ingredient ingredient, int count) {
        int remaining = count;

        for (int slot = 0; slot < itemInputSlotCount() && remaining > 0; slot++) {
            ItemStack stack = inputItems.getStackInSlot(slot);

            if (stack.isEmpty() || !ingredient.test(stack)) {
                continue;
            }

            int extracted = Math.min(remaining, stack.getCount());
            inputItems.extractItem(slot, extracted, false);
            remaining -= extracted;
        }
    }

    private void consumeChancedItemInput(
            CEChancedItemInput input,
            Optional<MachineTier> runtimeTier,
            Optional<MachineTier> baselineTier
    ) {
        int chance = input.effectiveChance(runtimeTier, baselineTier);
        int amount = 0;
        for (int i = 0; i < input.ingredient().count(); i++) {
            if (chance >= CEChancedItemInput.MAX_CHANCE || level.random.nextInt(CEChancedItemInput.MAX_CHANCE) < chance) {
                amount++;
            }
        }

        if (amount > 0) {
            consumeItemInput(input.ingredient().ingredient(), amount);
        }
    }


    private void consumeChancedFluidInput(
            CEChancedFluidInput input,
            Optional<MachineTier> runtimeTier,
            Optional<MachineTier> baselineTier
    ) {
        int chance = input.effectiveChance(runtimeTier, baselineTier);
        if (chance >= CEChancedFluidInput.MAX_CHANCE || level.random.nextInt(CEChancedFluidInput.MAX_CHANCE) < chance) {
            consumeFluidInput(input.ingredient());
        }
    }

    private void produceOutputs(CERecipeExecution execution) {
        execution.itemOutputs().forEach(this::produceItemOutput);
        execution.fluidOutputs().forEach(this::produceFluidOutput);
    }

    private InteractionContext interactionContext(Level level, SingleBlockMachineInstance instance) {
        Direction facing = getBlockState().hasProperty(SingleBlockMachineBlock.FACING)
                ? getBlockState().getValue(SingleBlockMachineBlock.FACING)
                : Direction.NORTH;

        return new InteractionContext() {
            @Override
            public Level level() {
                return level;
            }

            @Override
            public BlockPos origin() {
                return getBlockPos();
            }

            @Override
            public Direction facing() {
                return facing;
            }

            @Override
            public List<ItemStack> itemInputs() {
                return visibleItemInputs();
            }

            @Override
            public List<FluidStack> fluidInputs() {
                return visibleFluidInputs();
            }

            @Override
            public InteractionWearStore wearStore() {
                return interactionWear;
            }

            @Override
            public Map<String, net.mads.industron.machine.interaction.MachineArea.Resolved> areas() {
                return instance.definition().areas().stream().collect(Collectors.toUnmodifiableMap(
                        net.mads.industron.machine.interaction.MachineArea::name,
                        area -> area.resolve(getBlockPos(), facing, instance.tier(), instance.definition().startTier())
                ));
            }
        };
    }

    private void produceItemOutput(ItemStack output) {
        ItemStack remaining = output.copy();

        for (int slot = 0; slot < itemOutputSlotCount() && !remaining.isEmpty(); slot++) {
            remaining = outputItems.insertItem(slot, remaining, false);
        }
    }

    private void consumeFluidInput(SizedFluidIngredient input) {
        int remaining = input.amount();

        for (int i = 0; i < inputFluidSlotCount() && remaining > 0; i++) {
            FluidStack stack = inputFluids[i].getFluid();

            if (stack.isEmpty() || !input.ingredient().test(stack)) {
                continue;
            }

            FluidStack drained = inputFluids[i].drain(remaining, FluidAction.EXECUTE);
            remaining -= drained.getAmount();
        }
    }

    private void produceFluidOutput(FluidStack output) {
        FluidStack remaining = output.copy();

        for (int i = 0; i < outputFluidSlotCount() && !remaining.isEmpty(); i++) {
            int filled = outputFluids[i].fill(remaining, FluidAction.EXECUTE);
            remaining = remaining.copyWithAmount(remaining.getAmount() - filled);
        }
    }

    private List<ItemStack> visibleItemInputs() {
        List<ItemStack> stacks = new ArrayList<>();

        for (int i = 0; i < itemInputSlotCount(); i++) {
            stacks.add(inputItems.getStackInSlot(i));
        }

        return stacks;
    }

    private List<FluidStack> visibleFluidInputs() {
        List<FluidStack> stacks = new ArrayList<>();

        for (int i = 0; i < inputFluidSlotCount(); i++) {
            stacks.add(inputFluids[i].getFluid());
        }

        return stacks;
    }

    private void tickTemperature(SingleBlockMachineInstance instance) {
        SingleBlockDefinition.TemperatureSettings settings = instance.definition().temperature();
        if (settings == null) {
            if (temperature != 0 || temperatureTickCounter != 0 || temperatureOperationTicks != 0L) {
                temperature = 0;
                temperatureTickCounter = 0;
                temperatureOperationTicks = 0L;
                setChangedAndSync();
            }
            return;
        }

        temperatureTickCounter++;
        if (temperatureTickCounter < settings.changeIntervalTicks()) {
            return;
        }

        temperatureTickCounter = 0;
        int previous = temperature;

        boolean heating = machineEnabled && (settings.usesHeatConditions()
                ? heatConditionsMatch(instance, settings)
                : recipeLogic.isActive());

        if (heating) {
            temperature = Math.min(
                    settings.maximumTemperature(),
                    temperature + settings.heatingAmount()
            );
        } else {
            temperature = Math.max(0, temperature - settings.coolingAmount());
        }

        if (temperature != previous) {
            setChanged();
            cosmeticSyncPending = true;
        }
    }

    private boolean heatConditionsMatch(
            SingleBlockMachineInstance instance,
            SingleBlockDefinition.TemperatureSettings settings
    ) {
        Level level = getLevel();
        if (level == null || settings.heatConditions().isEmpty()) {
            return false;
        }

        InteractionContext context = interactionContext(level, instance);
        return settings.heatConditions().stream().allMatch(condition ->
                condition.matches(context)
        );
    }

    private boolean meetsTemperatureRequirement(SingleBlockMachineInstance instance) {
        SingleBlockDefinition.TemperatureSettings settings = instance.definition().temperature();
        return settings == null || temperature >= settings.minimumOperatingTemperature();
    }

    /**
     * Runs the operations nested inside Option.temperature(...). All due inputs and outputs,
     * including steam production, are simulated first and are then committed together.
     * Returning false only skips production for this tick; it does not pause the fuel recipe.
     */
    private boolean runTemperatureOperations(SingleBlockMachineInstance instance, int steamToProduce) {
        SingleBlockDefinition.TemperatureSettings settings = instance.definition().temperature();
        if (settings == null || !meetsTemperatureRequirement(instance)) {
            return false;
        }

        if (instance.definition().steamConversionRecipe() != null) {
            return runSteamConversionRecipe(instance, steamToProduce);
        }

        long operationTick = temperatureOperationTicks;
        List<SingleBlockDefinition.StackRequirement> itemInputs = due(settings.inputItems(), operationTick);
        List<SingleBlockDefinition.StackRequirement> fluidInputs = due(settings.inputFluids(), operationTick);
        List<SingleBlockDefinition.StackRequirement> itemOutputs = due(settings.outputItems(), operationTick);
        List<SingleBlockDefinition.StackRequirement> fluidOutputs = due(settings.outputFluids(), operationTick);

        if (!hasRequiredItems(itemInputs)
                || !hasRequiredFluids(fluidInputs)
                || !canFitFixedItems(itemOutputs)
                || !canFitFixedFluids(fluidOutputs)
                || !canFitSteam(steamToProduce)) {
            return false;
        }

        consumeFixedItems(itemInputs);
        consumeFixedFluids(fluidInputs);
        produceFixedItems(itemOutputs);
        produceFixedFluids(fluidOutputs);
        produceSteam(steamToProduce);
        temperatureOperationTicks++;
        setChangedAndSync();
        return true;
    }

    /** Fuel/solar heating and water conversion are independent; only successful conversion drains water. */
    private boolean runSteamConversionRecipe(SingleBlockMachineInstance instance, int steamToProduce) {
        if (steamToProduce <= 0 || level == null) return false;
        CERecipe recipe = recipeById(instance.definition().steamConversionRecipe()).orElse(null);
        if (recipe == null || !recipe.recipeType().equals(CERecipeTypes.EVAPORATION.id())
                || recipe.fluidInputs().size() != 1 || recipe.fluidOutputs().size() != 1
                || !recipe.itemInputs().isEmpty() || !recipe.itemOutputs().isEmpty()
                || !recipe.chancedFluidInputs().isEmpty() || !recipe.chancedFluidOutputs().isEmpty()
                || recipe.requiredTier().filter(t -> !MachineTierStats.isAtLeast(instance.tier().recipeTier(), t)).isPresent()
                || temperature < recipe.requiredTemp().orElse(0)) return false;
        FluidRegistry.RegisteredFluid steam = FluidRegistry.CHEMICAL_FLUIDS.get(IndustrialFluids.STEAM.registryName());
        FluidStack output = recipe.fluidOutputs().getFirst();
        if (steam == null || output.getFluid() != steam.source().get() || output.getAmount() <= 0
                || !canFitSteam(steamToProduce)) return false;
        var input = recipe.fluidInputs().getFirst();
        long scaled = (long) steamToProduce * input.amount();
        // Rates must represent whole mB of input; never round water down or create free steam.
        if (scaled % output.getAmount() != 0 || scaled / output.getAmount() > Integer.MAX_VALUE) return false;
        int water = (int) (scaled / output.getAmount());
        if (water <= 0) return false;
        int available = java.util.Arrays.stream(inputFluids).limit(inputFluidSlotCount()).filter(t -> input.ingredient().test(t.getFluid()))
                .mapToInt(FluidTank::getFluidAmount).sum();
        InteractionContext context = interactionContext(level, instance);
        if (available < water
                || !InteractionRuntime.conditionsMatch(recipe.conditions(), context, InteractionPhase.WHILE_PROCESSING)
                || !InteractionRuntime.interactionsMatch(recipe.blockInteractions(), context, InteractionPhase.WHILE_PROCESSING)) return false;
        if (!InteractionRuntime.applyInteractions(recipe.blockInteractions(), context, InteractionPhase.WHILE_PROCESSING)) return false;
        for (int i = 0; i < inputFluidSlotCount() && water > 0; i++) {
            FluidTank tank = inputFluids[i];
            if (input.ingredient().test(tank.getFluid())) water -= tank.drain(water, FluidAction.EXECUTE).getAmount();
        }
        produceSteam(steamToProduce);
        temperatureOperationTicks++;
        setChangedAndSync();
        return true;
    }

    private static List<SingleBlockDefinition.StackRequirement> due(
            List<SingleBlockDefinition.StackRequirement> requirements,
            long tick
    ) {
        return requirements.stream()
                .filter(requirement -> tick % requirement.durationTicks() == 0L)
                .toList();
    }

    private boolean hasRequiredItems(List<SingleBlockDefinition.StackRequirement> requirements) {
        Map<Item, Integer> needed = new HashMap<>();
        for (SingleBlockDefinition.StackRequirement requirement : requirements) {
            Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(requirement.id())).orElse(null);
            if (item == null) {
                return false;
            }
            needed.merge(item, requirement.amount(), Integer::sum);
        }

        for (Map.Entry<Item, Integer> entry : needed.entrySet()) {
            int found = 0;
            for (int slot = 0; slot < itemInputSlotCount(); slot++) {
                ItemStack stack = inputItems.getStackInSlot(slot);
                if (stack.is(entry.getKey())) {
                    found += stack.getCount();
                }
            }
            if (found < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    private boolean hasRequiredFluids(List<SingleBlockDefinition.StackRequirement> requirements) {
        Map<net.minecraft.world.level.material.Fluid, Integer> needed = new HashMap<>();
        for (SingleBlockDefinition.StackRequirement requirement : requirements) {
            net.minecraft.world.level.material.Fluid fluid = BuiltInRegistries.FLUID
                    .getOptional(ResourceLocation.parse(requirement.id()))
                    .orElse(null);
            if (fluid == null) {
                return false;
            }
            needed.merge(fluid, requirement.amount(), Integer::sum);
        }

        for (Map.Entry<net.minecraft.world.level.material.Fluid, Integer> entry : needed.entrySet()) {
            int found = 0;
            for (int tank = 0; tank < inputFluidSlotCount(); tank++) {
                FluidStack stack = inputFluids[tank].getFluid();
                if (!stack.isEmpty() && stack.getFluid() == entry.getKey()) {
                    found += stack.getAmount();
                }
            }
            if (found < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    private boolean canFitFixedItems(List<SingleBlockDefinition.StackRequirement> requirements) {
        List<ItemStack> stacks = new ArrayList<>();
        for (SingleBlockDefinition.StackRequirement requirement : requirements) {
            Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(requirement.id())).orElse(null);
            if (item == null) {
                return false;
            }
            stacks.add(new ItemStack(item, requirement.amount()));
        }
        return canFitItems(stacks);
    }

    private boolean canFitFixedFluids(List<SingleBlockDefinition.StackRequirement> requirements) {
        List<FluidStack> stacks = new ArrayList<>();
        for (SingleBlockDefinition.StackRequirement requirement : requirements) {
            net.minecraft.world.level.material.Fluid fluid = BuiltInRegistries.FLUID
                    .getOptional(ResourceLocation.parse(requirement.id()))
                    .orElse(null);
            if (fluid == null) {
                return false;
            }
            stacks.add(new FluidStack(fluid, requirement.amount()));
        }
        return canFitFluids(stacks);
    }

    private boolean canFitSteam(int amount) {
        return amount <= 0 || steamTank.getFluidAmount() + amount <= steamTank.getCapacity();
    }

    private void consumeFixedItems(List<SingleBlockDefinition.StackRequirement> requirements) {
        for (SingleBlockDefinition.StackRequirement requirement : requirements) {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(requirement.id()));
            int remaining = requirement.amount();
            for (int slot = 0; slot < itemInputSlotCount() && remaining > 0; slot++) {
                ItemStack stack = inputItems.getStackInSlot(slot);
                if (!stack.is(item)) {
                    continue;
                }
                int extracted = Math.min(remaining, stack.getCount());
                inputItems.extractItem(slot, extracted, false);
                remaining -= extracted;
            }
        }
    }

    private void consumeFixedFluids(List<SingleBlockDefinition.StackRequirement> requirements) {
        for (SingleBlockDefinition.StackRequirement requirement : requirements) {
            net.minecraft.world.level.material.Fluid fluid = BuiltInRegistries.FLUID
                    .get(ResourceLocation.parse(requirement.id()));
            int remaining = requirement.amount();
            for (int tank = 0; tank < inputFluidSlotCount() && remaining > 0; tank++) {
                FluidStack stack = inputFluids[tank].getFluid();
                if (stack.isEmpty() || stack.getFluid() != fluid) {
                    continue;
                }
                FluidStack drained = inputFluids[tank].drain(remaining, FluidAction.EXECUTE);
                remaining -= drained.getAmount();
            }
        }
    }

    private void produceFixedItems(List<SingleBlockDefinition.StackRequirement> requirements) {
        for (SingleBlockDefinition.StackRequirement requirement : requirements) {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(requirement.id()));
            produceItemOutput(new ItemStack(item, requirement.amount()));
        }
    }

    private void produceFixedFluids(List<SingleBlockDefinition.StackRequirement> requirements) {
        for (SingleBlockDefinition.StackRequirement requirement : requirements) {
            net.minecraft.world.level.material.Fluid fluid = BuiltInRegistries.FLUID
                    .get(ResourceLocation.parse(requirement.id()));
            produceFluidOutput(new FluidStack(fluid, requirement.amount()));
        }
    }

    private void produceSteam(int amount) {
        if (amount <= 0) {
            return;
        }

        FluidRegistry.RegisteredFluid steam =
                FluidRegistry.CHEMICAL_FLUIDS.get(IndustrialFluids.STEAM.registryName());
        if (steam != null) {
            steamTank.fill(new FluidStack(steam.source().get(), amount), FluidAction.EXECUTE);
        }
    }

    private void resetOverlay() {
        overlayFrame = 0;
        overlayFrameTicks = 0;
    }

    private void applyMachineControlSchedules() {
        if (machineControlSchedules.isEmpty()) return;
        boolean enabled = evaluateAllMachineControlSchedules();
        boolean variablesChanged = false;
        for (Direction side : machineControlSchedules.keySet()) {
            MachineControlSchedule schedule = machineControlSchedule(side);
            if (schedule != null) variablesChanged |= schedule.consumeRuntimeDirty();
        }
        setMachineEnabled(enabled);
        if (variablesChanged) setChangedAndSync();
    }

    @Override
    public boolean isMachineEnabled() {
        return machineEnabled;
    }

    @Override
    public void setMachineEnabled(boolean enabled) {
        if (machineEnabled == enabled) {
            return;
        }

        machineEnabled = enabled;
        if (!enabled) {
            resetOverlay();
            Level level = getLevel();
            BlockState state = getBlockState();
            if (level != null
                    && state.hasProperty(SingleBlockMachineBlock.ACTIVE)
                    && state.getValue(SingleBlockMachineBlock.ACTIVE)) {
                level.setBlock(
                        getBlockPos(),
                        state.setValue(SingleBlockMachineBlock.ACTIVE, false),
                        Block.UPDATE_ALL
                );
            }
        }

        setChangedAndSync();
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
        return true;
    }

    @Override
    public void machineControlSchedulesChanged() {
        Level level = getLevel();
        if (level != null && !level.isClientSide()) {
            applyMachineControlSchedules();
            refreshMachineControlRedstoneOutputs();
        }
        setChangedAndSync();
    }

    @Override
    public MachineControlTarget machineControlTarget() {
        return this;
    }

    @Override
    public MachineControlContext machineControlContext(int redstoneInput) {
        return currentMachineControlSnapshot().withRedstoneInput(redstoneInput);
    }

    private MachineControlSnapshot currentMachineControlSnapshot() {
        Level level = getLevel();
        long tick = level == null ? Long.MIN_VALUE : level.getGameTime();
        if (machineControlSnapshot != null && machineControlSnapshotTick == tick) return machineControlSnapshot;
        machineControlSnapshotTick = tick;
        machineControlSnapshot = MachineControlSnapshot.builder(tick)
                .inputRevision(machineControlInputRevision)
                .machineRunning(machineEnabled && recipeLogic.status() == CERecipeStatus.WORKING)
                .hasActiveRecipe(recipeLogic.execution() != null)
                .recipeProgress(progress())
                .recipeDuration(progressTotal())
                .energy(this::energyStored, this::energyCapacity)
                .steam(this::steamStored, this::steamCapacity)
                .diagnostics(this::machineControlDiagnosticsSnapshot)
                .itemInputs(this::cachedMachineControlItemInputs)
                .fluidInputs(this::cachedMachineControlFluidInputs)
                .world(this::machineControlWorldSnapshot)
                .temperature(temperature())
                .rpm(recipeMinimumRpm(), recipeMaximumRpm(), machineRpm())
                .build();
        return machineControlSnapshot;
    }

    private MachineControlSnapshot.World machineControlWorldSnapshot() {
        Level level = getLevel();
        if (level == null) return MachineControlSnapshot.World.NONE;
        return new MachineControlSnapshot.World(
                level.isRaining(),
                level.isThundering(),
                level.canSeeSky(worldPosition.above()),
                level.isDay()
        );
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
        machineControlCachedItemInputs = visibleItemInputs().stream().map(ItemStack::copy).toList();
        machineControlCachedFluidInputs = visibleFluidInputs().stream().map(FluidStack::copy).toList();
    }

    private void machineControlInputsChanged() {
        machineControlInputRevision++;
        machineControlCachedInputRevision = Long.MIN_VALUE;
        setChangedAndSync();
    }

    private MachineControlSnapshot.Diagnostics machineControlDiagnosticsSnapshot() {
        SingleBlockMachineInstance instance = instance();
        if (instance == null) return MachineControlSnapshot.Diagnostics.NONE;
        CERecipeExecution execution = recipeLogic.execution();
        if (execution != null) {
            return new MachineControlSnapshot.Diagnostics(
                    false,
                    execution.resourcePerTick() > 0 && !canProcessResource(instance, execution.resourcePerTick()),
                    recipeLogic.status() == CERecipeStatus.WAITING_FOR_OUTPUT
            );
        }

        Optional<RecipeHolder<CERecipe>> candidate = machineControlRecipeCandidate();
        if (candidate.isEmpty()) return new MachineControlSnapshot.Diagnostics(true, false, false);
        CERecipe recipe = candidate.get().value();
        int resourcePerTick = resourcePerTick(instance, recipe);
        List<ItemStack> itemOutputs = recipe.itemOutputs().stream().map(output -> output.stack().copy()).toList();
        List<FluidStack> fluidOutputs = possibleFluidOutputs(recipe);
        return new MachineControlSnapshot.Diagnostics(
                false,
                resourcePerTick > 0 && !canProcessResource(instance, resourcePerTick),
                !canFitItems(itemOutputs) || !canFitFluids(fluidOutputs)
        );
    }

    public int machineControlSignal(Direction side) {
        return side == null ? 0 : machineControlRedstoneOutput(side);
    }

    private void refreshMachineControlRedstoneOutputs() {
        Level level = getLevel();
        if (level == null || level.isClientSide()) return;
        int signature = 0;
        for (Direction side : Direction.values()) {
            signature |= (machineControlSignal(side) & 15) << (side.get3DDataValue() * 4);
        }
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

    private Optional<RecipeHolder<CERecipe>> machineControlRecipeCandidate() {
        Level level = getLevel();
        SingleBlockMachineInstance instance = instance();
        return level == null || instance == null ? Optional.empty() : findRecipe(level, instance);
    }

    private boolean machineControlOutputBlocked() {
        if (recipeLogic.status() == CERecipeStatus.WAITING_FOR_OUTPUT) return true;
        if (recipeLogic.execution() != null) return false;
        return machineControlRecipeCandidate().map(holder -> {
            CERecipe recipe = holder.value();
            List<ItemStack> itemOutputs = recipe.itemOutputs().stream().map(output -> output.stack().copy()).toList();
            List<FluidStack> fluidOutputs = possibleFluidOutputs(recipe);
            return !canFitItems(itemOutputs) || !canFitFluids(fluidOutputs);
        }).orElse(false);
    }

    private boolean machineControlMissingEnergy() {
        SingleBlockMachineInstance instance = instance();
        if (instance == null) return false;
        CERecipeExecution execution = recipeLogic.execution();
        if (execution != null) return execution.resourcePerTick() > 0 && !canProcessResource(instance, execution.resourcePerTick());
        return machineControlRecipeCandidate()
                .map(holder -> resourcePerTick(instance, holder.value()))
                .filter(amount -> amount > 0)
                .map(amount -> !canProcessResource(instance, amount))
                .orElse(false);
    }

    private boolean machineControlMissingInput() {
        return recipeLogic.execution() == null && machineControlRecipeCandidate().isEmpty();
    }

    private int machineControlItemCount(String filter) {
        if (filter == null || filter.isBlank()) return visibleItemInputs().stream().mapToInt(ItemStack::getCount).sum();
        return visibleItemInputs().stream()
                .filter(stack -> machineControlItemMatchesFilter(stack, filter))
                .mapToInt(ItemStack::getCount)
                .sum();
    }

    private int machineControlFluidAmount(String filter) {
        if (filter == null || filter.isBlank()) return visibleFluidInputs().stream().mapToInt(FluidStack::getAmount).sum();
        return visibleFluidInputs().stream()
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
            TagKey<Item> tag = TagKey.create(Registries.ITEM, id);
            return visibleItemInputs().stream().anyMatch(stack -> stack.is(tag));
        }
        return visibleItemInputs().stream().anyMatch(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(id));
    }

    private boolean machineControlFluidMatches(String value, boolean tagMatch) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) return false;
        if (tagMatch) {
            TagKey<net.minecraft.world.level.material.Fluid> tag = TagKey.create(Registries.FLUID, id);
            return visibleFluidInputs().stream().anyMatch(stack -> stack.getFluid().builtInRegistryHolder().is(tag));
        }
        return visibleFluidInputs().stream().anyMatch(stack -> BuiltInRegistries.FLUID.getKey(stack.getFluid()).equals(id));
    }

    public CERecipeLogic recipeLogic() {
        return recipeLogic;
    }

    private final class RecipeHost implements CERecipeLogicHost {
        @Override public boolean recipeSearchPending() { return asyncSearch.pending(); }
        @Override
        public boolean recipeMachineReady() {
            return instance() != null;
        }

        @Override
        public Optional<CERecipeExecution> findAndConsumeRecipeInputs() {
            Level level = getLevel();
            SingleBlockMachineInstance instance = instance();

            if (level == null || instance == null) {
                return Optional.empty();
            }

            Optional<RecipeHolder<CERecipe>> holder = findRecipe(level, instance, true);
            if (holder.isEmpty()) {
                return Optional.empty();
            }

            CERecipe recipe = holder.get().value();
            CERecipeInput input = recipeInput(instance);
            int resourcePerTick = resourcePerTick(instance, recipe);
            InteractionContext context = interactionContext(level, instance);

            Optional<MachineModifier> machineModifier =
                    InteractionRuntime.firstMatchingModifier(instance.definition().modifiers(), context);
            Optional<MachineModifier> recipeModifier =
                    InteractionRuntime.firstMatchingModifier(recipe.modifiers(), context);

            resourcePerTick = InteractionRuntime.adjustedResource(
                    resourcePerTick,
                    instance.definition().resource() == SingleBlockMachineResource.STEAM,
                    machineModifier,
                    recipeModifier
            );

            int parallel = maxRecipeParallel(instance, recipe, input);
            int baseResourcePerTick = resourcePerTick;
            resourcePerTick = multiplyClamped(baseResourcePerTick, parallel);
            while (parallel > 1 && !canProcessResource(instance, resourcePerTick)) {
                parallel--;
                resourcePerTick = multiplyClamped(baseResourcePerTick, parallel);
            }
            if (!canProcessResource(instance, resourcePerTick)) return Optional.empty();

            List<ItemStack> itemOutputs = rollItemOutputs(recipe, parallel, input.processingTier());
            List<FluidStack> fluidOutputs = rollFluidOutputs(recipe, parallel, input.processingTier());
            while (parallel > 1 && (!canFitItems(itemOutputs) || !canFitFluids(fluidOutputs))) {
                parallel--;
                resourcePerTick = multiplyClamped(baseResourcePerTick, parallel);
                itemOutputs = rollItemOutputs(recipe, parallel, input.processingTier());
                fluidOutputs = rollFluidOutputs(recipe, parallel, input.processingTier());
            }
            if (!canFitItems(itemOutputs) || !canFitFluids(fluidOutputs)) return Optional.empty();

            InteractionRuntime.applyInteractions(
                    instance.definition().blockInteractions(),
                    context,
                    InteractionPhase.ON_START
            );
            InteractionRuntime.applyInteractions(
                    recipe.blockInteractions(),
                    context,
                    InteractionPhase.ON_START
            );

            ItemStack furnaceFuelInput = recipe.furnaceFuelStack(input).orElse(ItemStack.EMPTY);
            for (int run = 0; run < parallel; run++) consumeInputs(recipe, input);
            preferredRecipeId = holder.get().id();

            CompoundTag machineData = new CompoundTag();
            if (instance.definition().usesKineticOutput()) {
                machineData.putInt(
                        KINETIC_OUTPUT_RPM_DATA,
                        resolvedOutputRpm(instance, recipe)
                );
            }

            return Optional.of(new CERecipeExecution(
                    holder.get().id(),
                    recipe.recipeType(),
                    InteractionRuntime.adjustedDuration(
                            adjustedDuration(instance, recipe, input),
                            machineModifier,
                            recipeModifier
                    ),
                    resourcePerTick,
                    parallel,
                    recipe.furnaceFuel()
                            ? List.of(furnaceFuelInput.copyWithCount(1))
                            : displayItemInputs(recipe),
                    displayFluidInputs(recipe),
                    itemOutputs,
                    fluidOutputs,
                    machineData
            ));
        }

        @Override
        public CERecipeTickResult consumeRecipeTick(CERecipeExecution execution) {
            SingleBlockMachineInstance instance = instance();
            Level level = getLevel();

            if (instance == null || level == null) {
                return CERecipeTickResult.CANCEL;
            }

            Optional<CERecipe> recipe = recipeById(execution.recipeId());

            if (instance.definition().usesKineticInput()) {
                if (recipe.isEmpty()) {
                    return CERecipeTickResult.CANCEL;
                }
                if (!hasKineticInputPower()
                        || kineticRpmError(recipe.get()) != KineticRpmError.NONE) {
                    return CERecipeTickResult.WAIT_FOR_RPM;
                }
            }

            InteractionContext context = interactionContext(level, instance);

            ConditionFailure machineFailure = InteractionRuntime.failedConditionBehavior(
                    instance.definition().conditions(),
                    context,
                    InteractionPhase.WHILE_PROCESSING
            );

            ConditionFailure recipeFailure = recipe
                    .map(value -> InteractionRuntime.failedConditionBehavior(
                            value.conditions(),
                            context,
                            InteractionPhase.WHILE_PROCESSING
                    ))
                    .orElse(null);

            ConditionFailure failure = machineFailure != null ? machineFailure : recipeFailure;

            if (failure != null) {
                return failure == ConditionFailure.CANCEL
                        ? CERecipeTickResult.CANCEL
                        : failure == ConditionFailure.RESET
                        ? CERecipeTickResult.WAIT_FOR_RESOURCE
                        : CERecipeTickResult.PAUSE;
            }

            int amount = execution.resourcePerTick();

            /*
             * Operations nested inside Option.temperature(...) must run even when this
             * machine does not use the separate energy/steam resource system. A tierless
             * boiler normally has resourcePerTick == 0 and uses nested inputFluid and
             * outputFluid operations for water -> steam conversion.
             */
            if (instance.definition().hasTemperature()) {
                int steamToProduce =
                        instance.definition().resource() == SingleBlockMachineResource.STEAM && amount < 0
                                ? -amount
                                : 0;
                runTemperatureOperations(instance, steamToProduce);
            }

            if (amount == 0) {
                tickSprinklerInteractions(instance, context);
                return CERecipeTickResult.CONTINUE;
            }

            if (instance.definition().resource() == SingleBlockMachineResource.STEAM) {
                if (amount > 0) {
                    if (steamTank.getFluidAmount() < amount) {
                        return CERecipeTickResult.WAIT_FOR_RESOURCE;
                    }

                    steamTank.drain(amount, FluidAction.EXECUTE);
                } else if (!instance.definition().hasTemperature()) {
                    int produced = -amount;
                    FluidRegistry.RegisteredFluid steam =
                            FluidRegistry.CHEMICAL_FLUIDS.get(IndustrialFluids.STEAM.registryName());

                    if (steam == null) {
                        return CERecipeTickResult.CANCEL;
                    }

                    if (steamTank.getFluidAmount() + produced > steamTank.getCapacity()) {
                        return CERecipeTickResult.WAIT_FOR_RESOURCE;
                    }

                    steamTank.fill(
                            new FluidStack(steam.source().get(), produced),
                            FluidAction.EXECUTE
                    );
                }

                tickSprinklerInteractions(instance, context);
                return CERecipeTickResult.CONTINUE;
            }

            if (amount > 0) {
                if (energyStored < amount) {
                    return CERecipeTickResult.WAIT_FOR_RESOURCE;
                }

                ceContainer.extract(amount, false);
            } else {
                int produced = -amount;

                if (energyStored + produced > energyCapacity()) {
                    return CERecipeTickResult.WAIT_FOR_RESOURCE;
                }

                ceContainer.insert(produced, false);
            }

            tickSprinklerInteractions(instance, context);
            return CERecipeTickResult.CONTINUE;
        }

        @Override
        public boolean resetDurationWhenResourceMissing() {
            SingleBlockMachineInstance instance = instance();
            return instance == null || !instance.definition().noDurationReset();
        }

        @Override
        public boolean canCompleteRecipe(CERecipeExecution execution) {
            SingleBlockMachineInstance instance = instance();
            Level level = getLevel();
            Optional<CERecipe> recipe = recipeById(execution.recipeId());

            if (instance == null || level == null || recipe.isEmpty()) {
                return false;
            }

            InteractionContext context = interactionContext(level, instance);

            return canFitItems(execution.itemOutputs())
                    && canFitFluids(execution.fluidOutputs())
                    && InteractionRuntime.conditionsMatch(
                    instance.definition().conditions(),
                    context,
                    InteractionPhase.ON_COMPLETE
            )
                    && InteractionRuntime.conditionsMatch(
                    recipe.get().conditions(),
                    context,
                    InteractionPhase.ON_COMPLETE
            )
                    && InteractionRuntime.interactionsMatch(
                    instance.definition().blockInteractions(),
                    context,
                    InteractionPhase.ON_COMPLETE
            )
                    && InteractionRuntime.interactionsMatch(
                    recipe.get().blockInteractions(),
                    context,
                    InteractionPhase.ON_COMPLETE
            );
        }

        @Override
        public boolean completeRecipe(CERecipeExecution execution) {
            SingleBlockMachineInstance instance = instance();
            Level level = getLevel();
            Optional<CERecipe> recipe = recipeById(execution.recipeId());

            if (instance == null || level == null || recipe.isEmpty()) {
                return false;
            }

            InteractionContext context = interactionContext(level, instance);
            if (!InteractionRuntime.applyInteractions(
                    instance.definition().blockInteractions(),
                    context,
                    InteractionPhase.ON_COMPLETE
            ) || !InteractionRuntime.applyInteractions(
                    recipe.get().blockInteractions(),
                    context,
                    InteractionPhase.ON_COMPLETE
            )) {
                return false;
            }

            produceOutputs(execution);
            return true;
        }

        @Override
        public void onRecipeLogicChanged(boolean activeChanged) {
            if (!recipeLogic.isActive()) {
                resetOverlay();
            }

            refreshGeneratedRotation();
            setChanged();

            if (activeChanged || recipeSyncCooldown <= 0) {
                recipeSyncCooldown = 10;
                syncToClient();
            } else {
                recipeSyncCooldown--;
            }
        }
    }

    private SingleBlockMachineInstance instance() {
        return getBlockState().getBlock() instanceof SingleBlockMachineBlock block
                ? block.instance()
                : null;
    }

    private static boolean isVentBlocked(
            Level level,
            BlockPos pos
    ) {
        BlockState state = level.getBlockState(pos);

        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return false;
        }

        return !state.getCollisionShape(level, pos).isEmpty();
    }

    private ItemStackHandler createInventory(int slots, boolean machineControlInput) {
        return new ItemStackHandler(slots) {
            @Override
            public int getSlotLimit(int slot) {
                SingleBlockMachineInstance current = instance();
                if (current == null) return super.getSlotLimit(slot);
                String id = current.definition().id();
                if (id.endsWith("_drying_rack") && slot < 4) return 1;
                // The molding input count is declared by its recipe; right-click insertion enforces it.
                if (id.endsWith("_brick_mold") && slot == 0 && !machineControlInput) return 1;
                if (id.endsWith("_kiln") && slot < 6) return 1;
                return super.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                SingleBlockMachineInstance current = instance();
                if (current != null && current.definition().id().endsWith("_drying_rack") && machineControlInput
                        && net.mads.industron.material.recipes.CastingRecipes.isHot(stack)) {
                    return net.mads.industron.material.recipes.CastingRecipes.canRackCool(stack);
                }
                if (current != null && current.definition().id().endsWith("_kiln") && machineControlInput) {
                    if (slot >= 0 && slot < 6) return isKilnProcessInput(stack);
                    if (slot == 6) return isKilnFuel(stack);
                    return false;
                }
                return super.isItemValid(slot, stack);
            }

            @Override
            protected void onContentsChanged(int slot) {
                if (machineControlInput) {
                    machineControlInputsChanged();
                } else {
                    machineDataChanged();
                }
            }
        };
    }

    private FluidTank createSteamTank() {
        return new FluidTank(1_000_000) {
            @Override
            public int getCapacity() {
                return steamCapacity();
            }

            @Override
            public int getTankCapacity(int tank) {
                return tank == 0 ? steamCapacity() : 0;
            }

            @Override
            public boolean isFluidValid(FluidStack stack) {
                FluidRegistry.RegisteredFluid steam =
                        FluidRegistry.CHEMICAL_FLUIDS.get(IndustrialFluids.STEAM.registryName());

                return steam != null
                        && !stack.isEmpty()
                        && stack.getFluid() == steam.source().get();
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                if (!isFluidValid(resource)) {
                    return 0;
                }

                int space = Math.max(0, getCapacity() - getFluidAmount());

                if (space <= 0) {
                    return 0;
                }

                return super.fill(
                        resource.copyWithAmount(Math.min(resource.getAmount(), space)),
                        action
                );
            }

            @Override
            protected void onContentsChanged() {
                machineControlInputsChanged();
            }
        };
    }

    /** Player-container view of a Basin: fill inputs, drain outputs first, then recover inputs. */
    private final class BasinInteractionFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return inputFluidSlotCount() + outputFluidSlotCount();
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            FluidTank resolved = basinTank(tank);
            return resolved == null ? FluidStack.EMPTY : resolved.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            FluidTank resolved = basinTank(tank);
            return resolved == null ? 0 : resolved.getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank >= 0 && tank < inputFluidSlotCount() && basinAcceptsLiquid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!basinAcceptsLiquid(resource)) return 0;
            for (int index = 0; index < inputFluidSlotCount(); index++) {
                FluidStack stored = inputFluids[index].getFluid();
                if (!stored.isEmpty() && FluidStack.isSameFluidSameComponents(stored, resource)) {
                    int filled = inputFluids[index].fill(resource, action);
                    if (filled > 0 && action == FluidAction.EXECUTE) manualToolUses = 0;
                    return filled;
                }
            }
            for (int index = 0; index < inputFluidSlotCount(); index++) {
                if (inputFluids[index].isEmpty()) {
                    int filled = inputFluids[index].fill(resource, action);
                    if (filled > 0 && action == FluidAction.EXECUTE) manualToolUses = 0;
                    return filled;
                }
            }
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) return FluidStack.EMPTY;
            FluidStack drained = drainMatching(outputFluids, outputFluidSlotCount(), resource, action);
            if (drained.isEmpty()) {
                drained = drainMatching(inputFluids, inputFluidSlotCount(), resource, action);
                if (!drained.isEmpty() && action == FluidAction.EXECUTE) manualToolUses = 0;
            }
            return drained;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0) return FluidStack.EMPTY;
            FluidStack drained = drainFirst(outputFluids, outputFluidSlotCount(), maxDrain, action);
            if (drained.isEmpty()) {
                drained = drainFirst(inputFluids, inputFluidSlotCount(), maxDrain, action);
                if (!drained.isEmpty() && action == FluidAction.EXECUTE) manualToolUses = 0;
            }
            return drained;
        }

        private FluidTank basinTank(int tank) {
            if (tank < 0) return null;
            if (tank < inputFluidSlotCount()) return inputFluids[tank];
            int output = tank - inputFluidSlotCount();
            return output < outputFluidSlotCount() ? outputFluids[output] : null;
        }

        private FluidStack drainMatching(
                FluidTank[] tanks,
                int count,
                FluidStack resource,
                FluidAction action
        ) {
            for (int index = 0; index < count; index++) {
                FluidStack drained = tanks[index].drain(resource, action);
                if (!drained.isEmpty()) return drained;
            }
            return FluidStack.EMPTY;
        }

        private FluidStack drainFirst(FluidTank[] tanks, int count, int maxDrain, FluidAction action) {
            for (int index = 0; index < count; index++) {
                FluidStack drained = tanks[index].drain(maxDrain, action);
                if (!drained.isEmpty()) return drained;
            }
            return FluidStack.EMPTY;
        }
    }

    /** Unknown/vanilla fluids are treated as liquids; registered Industron gases are rejected. */
    private static boolean basinAcceptsLiquid(FluidStack candidate) {
        if (candidate.isEmpty()) return false;
        var definition = IndustrialFluidLookup.find(candidate);
        return definition == null || !definition.isGas();
    }

    public int circuit() {
        return circuit;
    }

    public void adjustCircuit(int amount) {
        if (amount == 0) {
            return;
        }
        setCircuit(Math.floorMod(circuit + amount, 33));
    }

    public void resetCircuit() {
        setCircuit(0);
    }

    public void setCircuit(int circuit) {
        int next = Math.max(0, Math.min(32, circuit));
        if (this.circuit == next) {
            return;
        }
        this.circuit = next;
        asyncSearch.invalidate();
        preferredRecipeId = null;
        setChangedAndSync();
    }

    private void machineDataChanged() {
        setChangedAndSync();
    }

    private void setChangedAndSync() {
        setChanged();
        syncToClient();
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide()) clientSyncPending = true;
    }

    private void flushClientSync() {
        if (level == null || level.isClientSide()) return;
        if (!clientSyncPending && (!cosmeticSyncPending || level.getGameTime() % 10 != 0)) return;
        cosmeticSyncPending = false;
        clientSyncPending = false;
        BlockState state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, 3);
    }

    private void tickSprinklerInteractions(
            SingleBlockMachineInstance instance,
            InteractionContext context
    ) {
        int processingTick = recipeLogic.progress() + 1;

        for (BlockInteraction interaction : instance.definition().blockInteractions()) {
            if (interaction.type() != BlockInteraction.Type.SPRINKLER
                    || processingTick % interaction.interval() != 0) {
                continue;
            }

            int actions = sprinklerActions(instance, interaction);
            runSprinklerInterval(interaction, context, actions);
        }
    }

    private static int sprinklerActions(
            SingleBlockMachineInstance instance,
            BlockInteraction interaction
    ) {
        int actual = MachineTierStats.tierIndex(instance.tier().recipeTier());
        int start = MachineTierStats.tierIndex(instance.definition().startTier().recipeTier());
        int actions = interaction.actionsPerInterval();

        for (int step = 0; step < Math.max(0, actual - start); step++) {
            actions = Math.multiplyExact(actions, interaction.actionMultiplierPerTier());
        }

        return actions;
    }

    private void runSprinklerInterval(
            BlockInteraction interaction,
            InteractionContext context,
            int actions
    ) {
        if (!(context.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (sprinklerTargets.isEmpty() || sprinklerTargetIndex >= sprinklerTargets.size()) {
            rebuildSprinklerTargets(interaction, context);
        }

        for (int action = 0; action < actions && sprinklerTargetIndex < sprinklerTargets.size(); action++) {
            BlockPos target = sprinklerTargets.get(sprinklerTargetIndex++);
            applySprinklerBonemeal(serverLevel, target);
        }

        if (sprinklerTargetIndex >= sprinklerTargets.size()) {
            sprinklerTargets.clear();
            sprinklerTargetIndex = 0;
        }
    }

    private void rebuildSprinklerTargets(
            BlockInteraction interaction,
            InteractionContext context
    ) {
        sprinklerTargets.clear();
        sprinklerTargetIndex = 0;

        interaction.area()
                .map(context.areas()::get)
                .ifPresent(area -> area.positions().stream()
                        .filter(pos -> isValidSprinklerTarget(context.level(), pos))
                        .forEach(sprinklerTargets::add));
    }

    private static boolean isValidSprinklerTarget(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(SPRINKLER_BLACKLIST) || isBuiltInSprinklerBlacklist(state)) {
            return false;
        }

        return state.getBlock() instanceof BonemealableBlock bonemealable
                && bonemealable.isValidBonemealTarget(level, pos, state);
    }

    private static boolean isBuiltInSprinklerBlacklist(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.MOSS_BLOCK)
                || state.is(Blocks.CRIMSON_NYLIUM)
                || state.is(Blocks.WARPED_NYLIUM)
                || state.is(Blocks.NETHERRACK);
    }

    private static void applySprinklerBonemeal(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!isValidSprinklerTarget(level, pos)
                || !(state.getBlock() instanceof BonemealableBlock bonemealable)) {
            return;
        }

        if (bonemealable.isBonemealSuccess(level, level.random, pos, state)) {
            bonemealable.performBonemeal(level, level.random, pos, state);
            level.levelEvent(1505, pos, 0);
        }
    }


    @Override
    protected void write(
            CompoundTag tag,
            HolderLookup.Provider registries,
            boolean clientPacket
    ) {
        tag.put("InputItems", inputItems.serializeNBT(registries));
        tag.put("OutputItems", outputItems.serializeNBT(registries));
        tag.put("SteamTank", steamTank.writeToNBT(registries, new CompoundTag()));

        saveTanks(tag, "InputFluids", inputFluids, registries);
        saveTanks(tag, "OutputFluids", outputFluids, registries);

        tag.putLong("EnergyStored", energyStored);
        tag.putBoolean("MachineEnabled", machineEnabled);
        saveMachineControlSchedules(tag);
        tag.putInt("Circuit", circuit);
        tag.putInt("Temperature", temperature);
        tag.putInt("TemperatureTickCounter", temperatureTickCounter);
        tag.putLong("TemperatureOperationTicks", temperatureOperationTicks);
        tag.putInt("SprinklerTargetIndex", sprinklerTargetIndex);
        tag.putIntArray("PrimitiveProgress", primitiveProgress);
        tag.putDouble("KilnFuelUnits", kilnFuelUnits);
        tag.putInt("ManualToolTicks", manualToolTicks);
        tag.putInt("ManualToolUses", manualToolUses);
        if (manualToolWorker != null) tag.putUUID("ManualToolWorker", manualToolWorker);
        if (manualRecipeId != null) tag.putString("ManualRecipe", manualRecipeId.toString());

        if (preferredRecipeId != null) {
            tag.putString("PreferredRecipe", preferredRecipeId.toString());
        }

        interactionWear.save(tag);
        recipeLogic.save(tag, registries);
        super.write(tag, registries, clientPacket);
    }

    @Override
    protected void read(
            CompoundTag tag,
            HolderLookup.Provider registries,
            boolean clientPacket
    ) {
        if (tag.contains("InputItems")) {
            inputItems.deserializeNBT(registries, tag.getCompound("InputItems"));
        }

        if (tag.contains("OutputItems")) {
            outputItems.deserializeNBT(registries, tag.getCompound("OutputItems"));
        }

        if (tag.contains("SteamTank")) {
            steamTank.readFromNBT(registries, tag.getCompound("SteamTank"));
        }

        loadTanks(tag, "InputFluids", inputFluids, registries);
        loadTanks(tag, "OutputFluids", outputFluids, registries);

        machineEnabled = !tag.contains("MachineEnabled") || tag.getBoolean("MachineEnabled");
        loadMachineControlSchedules(tag);

        energyStored = Math.max(
                0L,
                Math.min(tag.getLong("EnergyStored"), energyCapacity())
        );

        circuit = Math.max(0, Math.min(32, tag.getInt("Circuit")));
        temperature = Math.max(0, tag.getInt("Temperature"));
        temperatureTickCounter = Math.max(0, tag.getInt("TemperatureTickCounter"));
        temperatureOperationTicks = Math.max(0L, tag.getLong("TemperatureOperationTicks"));
        sprinklerTargetIndex = Math.max(0, tag.getInt("SprinklerTargetIndex"));
        sprinklerTargets.clear();
        int[] savedPrimitiveProgress = tag.getIntArray("PrimitiveProgress");
        System.arraycopy(savedPrimitiveProgress, 0, primitiveProgress, 0, Math.min(savedPrimitiveProgress.length, primitiveProgress.length));
        kilnFuelUnits = tag.contains("KilnFuelUnits")
                ? Math.max(0.0D, tag.getDouble("KilnFuelUnits"))
                : Math.max(0.0D, tag.getInt("KilnFuelTicks") / 10.0D);
        manualToolTicks = Math.max(0, tag.contains("ManualToolTicks")
                ? tag.getInt("ManualToolTicks") : tag.getInt("MoldWorkTicks"));
        manualToolUses = Math.max(0, tag.contains("ManualToolUses")
                ? tag.getInt("ManualToolUses") : tag.getInt("MoldToolUses"));
        manualToolWorker = tag.hasUUID("ManualToolWorker")
                ? tag.getUUID("ManualToolWorker")
                : (tag.hasUUID("MoldWorker") ? tag.getUUID("MoldWorker") : null);
        manualRecipeId = tag.contains("ManualRecipe")
                ? ResourceLocation.tryParse(tag.getString("ManualRecipe"))
                : null;

        preferredRecipeId = tag.contains("PreferredRecipe")
                ? ResourceLocation.parse(tag.getString("PreferredRecipe"))
                : null;

        interactionWear.load(tag);
        recipeLogic.load(tag, registries);
        super.read(tag, registries, clientPacket);
    }

    /** Drops stored item contents and deliberately discards all stored fluids before block removal. */
    public void dropStoredItemsAndDiscardFluids() {
        if (level == null || level.isClientSide()) {
            return;
        }

        dropAndClear(inputItems);
        dropAndClear(outputItems);
        clearStoredFluids();
        setChanged();
    }

    private void dropAndClear(ItemStackHandler inventory) {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Block.popResource(level, worldPosition, stack.copy());
                inventory.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    private void clearStoredFluids() {
        for (FluidTank tank : inputFluids) {
            tank.setFluid(FluidStack.EMPTY);
        }
        for (FluidTank tank : outputFluids) {
            tank.setFluid(FluidStack.EMPTY);
        }
        steamTank.setFluid(FluidStack.EMPTY);
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < inputItems.getSlots(); i++) {
            inputItems.setStackInSlot(i, ItemStack.EMPTY);
        }

        for (int i = 0; i < outputItems.getSlots(); i++) {
            outputItems.setStackInSlot(i, ItemStack.EMPTY);
        }

        clearStoredFluids();
    }

    @Override
    public Component getDisplayName() {
        SingleBlockMachineInstance instance = instance();

        return Component.literal(
                instance == null
                        ? "Singleblock Machine"
                        : instance.displayName()
        );
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId,
            Inventory playerInventory,
            Player player
    ) {
        return new SingleBlockMachineMenu(
                containerId,
                playerInventory,
                this
        );
    }

    private class MachineItemHandler extends CombinedInvWrapper {
        private final MachineSide side;

        private MachineItemHandler(MachineSide side) {
            super(inputItems, outputItems);
            this.side = side;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (side != null && !instance().definition().allowsItemOutput(side)) return ItemStack.EMPTY;
            if (isKiln()) {
                if (slot < 0 || slot >= 6) return ItemStack.EMPTY;
                ItemStack stored = inputItems.getStackInSlot(slot);
                if (!isKilnExtractableOutput(stored)) return ItemStack.EMPTY;
                return inputItems.extractItem(slot, amount, simulate);
            }

            int outputStart = MAX_ITEM_INPUT_SLOTS;
            if (slot < outputStart || slot >= outputStart + itemOutputSlotCount()) {
                return ItemStack.EMPTY;
            }

            return super.extractItem(slot, amount, simulate);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (side != null && !instance().definition().allowsItemInput(side)) return stack;
            if (slot < 0 || slot >= itemInputSlotCount()) {
                return stack;
            }
            if (isKiln()) {
                if (slot < 6 && !isKilnProcessInput(stack)) return stack;
                if (slot == 6 && !isKilnFuel(stack)) return stack;
            }

            return super.insertItem(slot, stack, simulate);
        }
    }

    private FluidTank[] createFluidTanks(int count, boolean machineControlInput) {
        FluidTank[] tanks = new FluidTank[count];

        for (int i = 0; i < count; i++) {
            final int tankIndex = i;
            tanks[i] = new FluidTank(RECIPE_FLUID_TANK_CAPACITY) {
                @Override
                public int getCapacity() {
                    SingleBlockMachineInstance current = instance();
                    return machineControlInput
                            && current != null
                            && current.definition().id().endsWith("_basin")
                            ? BASIN_FLUID_TANK_CAPACITY
                            : RECIPE_FLUID_TANK_CAPACITY;
                }

                @Override
                public int getTankCapacity(int tank) {
                    return tank == 0 ? getCapacity() : 0;
                }

                @Override
                public int fill(FluidStack resource, FluidAction action) {
                    int accepted = Math.min(resource.getAmount(), Math.max(0, getCapacity() - getFluidAmount()));
                    return accepted <= 0 ? 0 : super.fill(resource.copyWithAmount(accepted), action);
                }

                @Override
                protected void onContentsChanged() {
                    if (machineControlInput) {
                        if (level != null && !level.isClientSide() && FoundryMetallurgy.unidentified(getFluid())) {
                            unidentifiedMixtureLastChangeTick[tankIndex] = level.getGameTime();
                        }
                        machineControlInputsChanged();
                    } else {
                        machineDataChanged();
                    }
                }
            };
        }

        return tanks;
    }

    private static List<FluidTank> visibleTanks(FluidTank[] tanks, int count) {
        List<FluidTank> visible = new ArrayList<>();

        for (int i = 0; i < Math.min(count, tanks.length); i++) {
            visible.add(tanks[i]);
        }

        return visible;
    }

    private static void saveTanks(
            CompoundTag tag,
            String key,
            FluidTank[] tanks,
            HolderLookup.Provider registries
    ) {
        CompoundTag tanksTag = new CompoundTag();

        for (int i = 0; i < tanks.length; i++) {
            tanksTag.put(
                    Integer.toString(i),
                    tanks[i].writeToNBT(registries, new CompoundTag())
            );
        }

        tag.put(key, tanksTag);
    }

    private static void loadTanks(
            CompoundTag tag,
            String key,
            FluidTank[] tanks,
            HolderLookup.Provider registries
    ) {
        if (!tag.contains(key)) {
            return;
        }

        CompoundTag tanksTag = tag.getCompound(key);

        for (int i = 0; i < tanks.length; i++) {
            String index = Integer.toString(i);

            if (tanksTag.contains(index)) {
                tanks[i].readFromNBT(
                        registries,
                        tanksTag.getCompound(index)
                );
            }
        }
    }

    private class MachineFluidHandler implements IFluidHandler {
        private final MachineSide side;

        private MachineFluidHandler(MachineSide side) {
            this.side = side;
        }

        @Override
        public int getTanks() {
            return (instance() != null
                    && instance().definition().resource() == SingleBlockMachineResource.STEAM
                    ? 1
                    : 0)
                    + inputFluidSlotCount()
                    + outputFluidSlotCount();
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            FluidTank fluidTank = tank(tank);
            return fluidTank == null
                    ? FluidStack.EMPTY
                    : fluidTank.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            FluidTank fluidTank = tank(tank);
            return fluidTank == null
                    ? 0
                    : fluidTank.getTankCapacity(0);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            FluidTank fluidTank = tank(tank);
            return fluidTank != null
                    && (!isBasinMachine() || basinAcceptsLiquid(stack))
                    && fluidTank.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (side != null && !instance().definition().allowsFluidInput(side)) return 0;
            if (resource.isEmpty()) {
                return 0;
            }
            if (isBasinMachine() && !basinAcceptsLiquid(resource)) return 0;

            if (steamTank.isFluidValid(resource)) {
                return steamTankAcceptsInput()
                        ? steamTank.fill(resource, action)
                        : 0;
            }

            for (int i = 0; i < inputFluidSlotCount(); i++) {
                FluidStack stored = inputFluids[i].getFluid();

                if (!stored.isEmpty()
                        && FluidStack.isSameFluidSameComponents(stored, resource)) {
                    return inputFluids[i].fill(resource, action);
                }
            }

            for (int i = 0; i < inputFluidSlotCount(); i++) {
                if (inputFluids[i].isEmpty()) {
                    return inputFluids[i].fill(resource, action);
                }
            }

            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (side != null && !instance().definition().allowsFluidOutput(side)) return FluidStack.EMPTY;
            if (resource.isEmpty()) {
                return FluidStack.EMPTY;
            }

            if (steamTankAllowsOutput() && steamTank.isFluidValid(resource)) {
                FluidStack drained = steamTank.drain(resource, action);

                if (!drained.isEmpty()) {
                    return drained;
                }
            }

            for (int i = 0; i < outputFluidSlotCount(); i++) {
                FluidStack drained = outputFluids[i].drain(resource, action);

                if (!drained.isEmpty()) {
                    return drained;
                }
            }

            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (side != null && !instance().definition().allowsFluidOutput(side)) return FluidStack.EMPTY;
            if (steamTankAllowsOutput()) {
                FluidStack drained = steamTank.drain(maxDrain, action);

                if (!drained.isEmpty()) {
                    return drained;
                }
            }

            for (int i = 0; i < outputFluidSlotCount(); i++) {
                FluidStack drained = outputFluids[i].drain(maxDrain, action);

                if (!drained.isEmpty()) {
                    return drained;
                }
            }

            return FluidStack.EMPTY;
        }

        private boolean steamTankVisible() {
            return instance() != null
                    && instance().definition().resource() == SingleBlockMachineResource.STEAM;
        }

        private boolean isBasinMachine() {
            return instance() != null && instance().definition().id().endsWith("_basin");
        }

        private boolean steamTankAcceptsInput() {
            return steamTankVisible()
                    && instance().definition().resourceMode() == SingleBlockMachineResourceMode.CONSUMES;
        }

        private boolean steamTankAllowsOutput() {
            return steamTankVisible()
                    && instance().definition().resourceMode() == SingleBlockMachineResourceMode.PRODUCES;
        }

        private FluidTank tank(int tank) {
            int index = tank;

            if (steamTankVisible()) {
                if (index == 0) {
                    return steamTank;
                }

                index--;
            }

            if (index < inputFluidSlotCount()) {
                return inputFluids[index];
            }

            index -= inputFluidSlotCount();

            if (index < outputFluidSlotCount()) {
                return outputFluids[index];
            }

            return null;
        }
    }

    private static List<ItemStack> displayItemInputs(CERecipe recipe) {
        List<ItemStack> stacks = new ArrayList<>();
        for (SizedIngredient input : recipe.itemInputs()) {
            ItemStack[] candidates = input.getItems();
            if (candidates.length > 0) {
                stacks.add(candidates[0].copyWithCount(input.count()));
            }
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
}
