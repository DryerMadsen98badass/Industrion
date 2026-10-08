package net.mads.industron.integration.jade;

import net.mads.industron.Industron;
import net.mads.industron.energy.CEEnergyContainer;
import net.mads.industron.energy.CreativeEnergyBlock;
import net.mads.industron.energy.CreativeEnergyBlockEntity;
import net.mads.industron.energy.EnergyWireBlock;
import net.mads.industron.energy.EnergyWireBlockEntity;
import net.mads.industron.machine.MachinePortBlock;
import net.mads.industron.machine.MachinePortBlockEntity;
import net.mads.industron.machine.KineticRpmError;
import net.mads.industron.machine.MachineDrive;
import net.mads.industron.machine.MachineTierStats;
import net.mads.industron.machine.SingleBlockMachineBlock;
import net.mads.industron.machine.SingleBlockMachineBlockEntity;
import net.mads.industron.machine.runtime.CERecipeExecution;
import net.mads.industron.machine.runtime.CERecipeLogic;
import net.mads.industron.machine.runtime.CERecipeLogicMachine;
import net.mads.industron.material.organism.cooking.CampfireFuelAccess;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockControllerBlock;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockControllerBlockEntity;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockRegistry;
import net.mads.industron.recipe.CEChancedFluidInput;
import net.mads.industron.recipe.CEChancedFluidOutput;
import net.mads.industron.recipe.CEChancedItemInput;
import net.mads.industron.recipe.CERecipe;
import net.mads.industron.recipe.CERecipeLookup;
import net.mads.industron.recipe.ChemicalBalanceRange;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.Accessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.JadeIds;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.config.IWailaConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ViewGroup;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@WailaPlugin(Industron.MOD_ID)
public class IndustronJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(EnergyStorageProvider.INSTANCE, MachinePortBlockEntity.class);
        registration.registerBlockDataProvider(KineticPortProvider.INSTANCE, MachinePortBlockEntity.class);
        registration.registerBlockDataProvider(EnergyStorageProvider.INSTANCE, CreativeEnergyBlockEntity.class);
        registration.registerBlockDataProvider(MachineInfoProvider.INSTANCE, BlockEntity.class);
        registration.registerBlockDataProvider(WireProvider.INSTANCE, BlockEntity.class);
        registration.registerBlockDataProvider(CampfireFuelProvider.INSTANCE, CampfireBlockEntity.class);
        registration.registerItemStorage(EmptyItemStorageProvider.INSTANCE, SingleBlockMachineBlock.class);
        registration.registerItemStorage(EmptyItemStorageProvider.INSTANCE, MultiblockControllerBlock.class);
        registration.registerFluidStorage(EmptyFluidStorageProvider.INSTANCE, SingleBlockMachineBlock.class);
        registration.registerFluidStorage(EmptyFluidStorageProvider.INSTANCE, MultiblockControllerBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(EnergyStorageProvider.INSTANCE, Block.class);
        registration.registerBlockComponent(KineticPortProvider.INSTANCE, MachinePortBlock.class);
        registration.registerBlockComponent(MachineInfoProvider.INSTANCE, Block.class);
        registration.registerBlockComponent(WireProvider.INSTANCE, EnergyWireBlock.class);
        registration.registerBlockComponent(CampfireFuelProvider.INSTANCE, CampfireBlock.class);
        registration.addBeforeTooltipCollectCallback((theme, accessor) -> {
            if (accessor instanceof BlockAccessor blockAccessor
                    && (blockAccessor.getBlock() instanceof SingleBlockMachineBlock
                    || blockAccessor.getBlock() instanceof MultiblockControllerBlock
                    || net.mads.industron.machine.machines.kinetic.KineticMachines.isMachine(blockAccessor.getBlock()))) {
                accessor.getServerData().remove(JadeIds.UNIVERSAL_ITEM_STORAGE.toString());
                accessor.getServerData().remove(JadeIds.UNIVERSAL_FLUID_STORAGE.toString());
            }
            return true;
        });
        registration.addTooltipCollectedCallback((box, accessor) -> {
            if (accessor instanceof BlockAccessor blockAccessor
                    && (blockAccessor.getBlock() instanceof SingleBlockMachineBlock
                    || blockAccessor.getBlock() instanceof MultiblockControllerBlock
                    || net.mads.industron.machine.machines.kinetic.KineticMachines.isMachine(blockAccessor.getBlock()))) {
                ITooltip tooltip = box.getTooltip();
                List<IElement> objectName = List.copyOf(tooltip.get(JadeIds.CORE_OBJECT_NAME));
                List<IElement> modName = List.copyOf(tooltip.get(JadeIds.CORE_MOD_NAME));
                tooltip.clear();
                if (!objectName.isEmpty()) {
                    tooltip.add(objectName);
                }

                IPluginConfig config = IWailaConfig.get().getPlugin();
                if (blockAccessor.getBlock() instanceof MultiblockControllerBlock) {
                    MultiblockStatusProvider.INSTANCE.appendTooltip(tooltip, blockAccessor, config);
                }
                MachineInfoProvider.INSTANCE.appendTooltip(tooltip, blockAccessor, config);

                if (!modName.isEmpty()) {
                    tooltip.add(modName);
                }
            }
        });
    }

    private enum CampfireFuelProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        INSTANCE;

        private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
                Industron.MOD_ID,
                "campfire_fuel"
        );

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData().getCompound(UID.toString());
            if (!data.contains("FuelCount")) {
                return;
            }

            tooltip.add(Component.literal("Fuel: " + data.getInt("FuelCount") + " / 4")
                    .withStyle(ChatFormatting.GRAY));

            int ticks = data.getInt("BurnTicks");
            if (ticks > 0) {
                int seconds = (ticks + 19) / 20;
                int minutes = seconds / 60;
                int remainingSeconds = seconds % 60;
                String time = minutes > 0
                        ? minutes + "m " + remainingSeconds + "s"
                        : remainingSeconds + "s";
                tooltip.add(Component.literal("Burn: " + time).withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        @Override
        public void appendServerData(CompoundTag root, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof CampfireBlockEntity fire)
                    || !(fire instanceof CampfireFuelAccess access)) {
                return;
            }

            int count = access.industron$fuelTicks() > 0 ? 1 : 0;
            for (ItemStack stack : access.industron$fuelItems()) {
                if (!stack.isEmpty()) {
                    count++;
                }
            }

            CompoundTag data = new CompoundTag();
            data.putInt("FuelCount", Math.min(4, count));
            data.putInt("BurnTicks", Math.max(0, access.industron$fuelTicks()));
            root.put(UID.toString(), data);
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }

    private enum EmptyItemStorageProvider implements IServerExtensionProvider<ItemStack> {
        INSTANCE;

        private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
                Industron.MOD_ID,
                "hidden_machine_item_storage"
        );

        @Override
        public List<ViewGroup<ItemStack>> getGroups(Accessor<?> accessor) {
            return List.of();
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }

    private enum EmptyFluidStorageProvider implements IServerExtensionProvider<CompoundTag> {
        INSTANCE;

        private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
                Industron.MOD_ID,
                "hidden_machine_fluid_storage"
        );

        @Override
        public List<ViewGroup<CompoundTag>> getGroups(Accessor<?> accessor) {
            return List.of();
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }

    private enum EnergyStorageProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        INSTANCE;

        private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, "ce_energy_storage");

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (accessor.getBlock() instanceof SingleBlockMachineBlock) {
                return;
            }

            CompoundTag data = accessor.getServerData().getCompound(UID.toString());
            if (data.contains("Capacity")) {
                appendEnergyTooltip(tooltip, data);
                return;
            }

            CEEnergyContainer energy = energyContainer(accessor);
            if (energy == null || energy.capacity() <= 0) {
                return;
            }

            CompoundTag fallback = new CompoundTag();
            writeEnergy(fallback, energy);
            appendEnergyTooltip(tooltip, fallback);
        }

        private static void appendEnergyTooltip(ITooltip tooltip, CompoundTag data) {
            appendEnergyTooltip(tooltip, data, 0xFFFFFFFF);
        }

        private static void appendEnergyTooltip(ITooltip tooltip, CompoundTag data, int textColor) {
            long stored = data.getLong("Stored");
            long capacity = data.getLong("Capacity");
            float progress = capacity <= 0L ? 0.0F : (float) Math.min(1.0D, stored / (double) capacity);
            tooltip.add(IElementHelper.get().progress(
                    progress,
                    Component.literal("Energy: " + stored + " / " + capacity + " CE"),
                    IElementHelper.get().progressStyle().color(0xFFEEE600, 0xFFEEE600).textColor(textColor),
                    BoxStyle.getNestedBox(),
                    true
            ));
        }

        @Override
        public void appendServerData(CompoundTag root, BlockAccessor accessor) {
            CEEnergyContainer energy = energyContainer(accessor);
            if (energy == null || energy.capacity() <= 0) {
                return;
            }

            CompoundTag data = root.getCompound(UID.toString());
            writeEnergy(data, energy);
            root.put(UID.toString(), data);
        }

        private static CEEnergyContainer energyContainer(BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof MachinePortBlockEntity port) {
                return port.ceContainer();
            }
            if (accessor.getBlockEntity() instanceof CreativeEnergyBlockEntity creative) {
                return creative.ceContainer();
            }
            return null;
        }

        private static void writeEnergy(CompoundTag data, CEEnergyContainer energy) {
            data.putLong("Stored", energy.stored());
            data.putLong("Capacity", energy.capacity());
            data.putString("Tier", energy.tier().displayName());
            data.putLong("Voltage", energy.voltage());
            data.putLong("InputAmps", energy.maxInputAmps());
            data.putLong("OutputAmps", energy.maxOutputAmps());
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }

    private enum KineticPortProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        INSTANCE;

        private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
                Industron.MOD_ID,
                "kinetic_port"
        );

        @Override
        public void appendTooltip(
                ITooltip tooltip,
                BlockAccessor accessor,
                IPluginConfig config
        ) {
            CompoundTag data = accessor.getServerData().getCompound(UID.toString());
            String mode = data.getString("Mode");
            if ("INPUT".equals(mode)) {
                tooltip.add(Component.literal(
                        "Kinetic Input: " + formatNumber(data.getDouble("SuPerRpm")) + " SU/RPM"
                ).withStyle(ChatFormatting.RED));
                tooltip.add(Component.literal(
                        "Speed: " + data.getInt("Rpm") + " RPM"
                ).withStyle(ChatFormatting.GRAY));
            } else if ("OUTPUT".equals(mode)) {
                tooltip.add(Component.literal(
                        "Kinetic Output: " + formatNumber(data.getDouble("SuPerRpm")) + " SU/RPM"
                ).withStyle(ChatFormatting.GREEN));
                tooltip.add(Component.literal(
                        "Output Speed: " + data.getInt("Rpm") + " RPM"
                ).withStyle(ChatFormatting.GRAY));
            }
        }

        @Override
        public void appendServerData(CompoundTag root, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof MachinePortBlockEntity port)
                    || !(accessor.getBlock() instanceof MachinePortBlock block)
                    || !block.isKineticPort()) {
                return;
            }

            CompoundTag data = root.getCompound(UID.toString());
            if (block.abilities().contains(
                    net.mads.industron.machine.machines.electric.multiblock.MultiblockAbility.KINETIC_INPUT
            )) {
                data.putString("Mode", "INPUT");
                data.putInt("Rpm", port.kineticRpm());
            } else {
                data.putString("Mode", "OUTPUT");
                data.putInt("Rpm", port.generatedRpm());
            }
            data.putDouble("SuPerRpm", port.kineticStressPerRpm());
            root.put(UID.toString(), data);
        }

        private static String formatNumber(double value) {
            return BigDecimal.valueOf(value)
                    .stripTrailingZeros()
                    .toPlainString();
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }

    private enum MachineInfoProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        INSTANCE;

        private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, "machine_info");

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!(accessor.getBlock() instanceof SingleBlockMachineBlock)
                    && !(accessor.getBlock() instanceof MultiblockControllerBlock)
                    && !net.mads.industron.machine.machines.kinetic.KineticMachines.isMachine(accessor.getBlock())) {
                return;
            }
            CompoundTag data = accessor.getServerData().getCompound(UID.toString());
            if (accessor.getBlockEntity() instanceof CERecipeLogicMachine machine) {
                writeMachineData(data, machine, accessor.getLevel());
            }

            tooltip.remove(JadeIds.UNIVERSAL_ITEM_STORAGE);
            tooltip.remove(JadeIds.UNIVERSAL_FLUID_STORAGE);
            int[] rackRemaining = data.getIntArray("RackRemainingTicks");
            for (int slot = 0; slot < rackRemaining.length; slot++) {
                if (rackRemaining[slot] <= 0) continue;
                int seconds = (rackRemaining[slot] + 19) / 20;
                String time = seconds >= 60 ? seconds / 60 + "m " + seconds % 60 + "s" : seconds + "s";
                tooltip.add(Component.literal("Slot " + (slot + 1) + " — Remaining: " + time)
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
            if (accessor.getBlockEntity() instanceof net.mads.industron.integration.create.kinetic.CEKineticRecipeHost host) {
                tooltip.add(Component.literal("Process: " + host.ceProcessing().recipeType().displayName()).withStyle(ChatFormatting.AQUA));
                tooltip.add(net.mads.industron.machine.MachineProcessingTooltip.tier(net.mads.industron.integration.create.kinetic.KineticProcessingRules.TIER));
                var processingLines=new java.util.ArrayList<Component>();
                net.mads.industron.machine.MachineProcessingTooltip.processing(processingLines,net.mads.industron.integration.create.kinetic.KineticProcessingRules.PROFILE);
                processingLines.forEach(tooltip::add);
            }
            if(accessor.getBlock() instanceof SingleBlockMachineBlock block && block.instance()!=null) {
                if(block.instance().tier()!=net.mads.industron.machine.MachineTier.NONE)
                    tooltip.add(net.mads.industron.machine.MachineProcessingTooltip.tier(block.instance().tier()));
                var processingLines=new java.util.ArrayList<Component>();
                net.mads.industron.machine.MachineProcessingTooltip.processing(processingLines,block.instance().definition().processingProfile(block.instance().tier()));
                processingLines.forEach(tooltip::add);
            }
            if(accessor.getBlockEntity() instanceof MultiblockControllerBlockEntity controller && controller.formedTier()!=null)
                tooltip.add(net.mads.industron.machine.MachineProcessingTooltip.tier(controller.formedTier()));
            appendRecipeIo(tooltip, data, "Inputs", "MachineItemInputs", "MachineFluidInputs", ChatFormatting.AQUA);
            appendRecipeIo(tooltip, data, "Outputs", "MachineItemOutputs", "MachineFluidOutputs", ChatFormatting.GOLD);

            int resourcePerTick = data.getInt("MachineResourcePerTick");
            long voltage = data.getLong("MachineEnergyVoltage");
            if (resourcePerTick != 0 && voltage > 0L) {
                tooltip.add(Component.literal("CE/t: " + Math.abs(resourcePerTick))
                        .withStyle(resourcePerTick > 0 ? ChatFormatting.RED : ChatFormatting.GREEN));
            } else if (resourcePerTick != 0) {
                tooltip.add(Component.literal(
                        (resourcePerTick > 0 ? "Steam Consumption: " : "Steam Production: ")
                                + Math.abs(resourcePerTick) + " mB/t"
                ).withStyle(resourcePerTick > 0 ? ChatFormatting.RED : ChatFormatting.GREEN));
            }

            appendKineticTooltip(tooltip, data);
            int parallel = data.getInt("MachineParallel");
            if (parallel > 0 && data.getInt("MachineProgressTotal") > 0) {
                tooltip.add(Component.literal("Performing " + parallel + (parallel == 1 ? " Parallel" : " Parallels"))
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            }

            if (data.contains("MachineCbHundredths")) {
                tooltip.add(Component.literal("CB: " + ChemicalBalanceRange.formatHundredths(data.getInt("MachineCbHundredths")))
                        .withStyle(ChatFormatting.AQUA));
                tooltip.add(Component.literal("Neutralizing: 1.00/s").withStyle(ChatFormatting.GRAY));
            }

            if (data.contains("MachineSafeCbMin") && data.contains("MachineSafeCbMax")) {
                tooltip.add(Component.literal(
                        "Safe CB: " + ChemicalBalanceRange.formatHundredths(data.getInt("MachineSafeCbMin"))
                                + " - " + ChemicalBalanceRange.formatHundredths(data.getInt("MachineSafeCbMax"))
                ).withStyle(ChatFormatting.AQUA));
            }

            if (data.contains("MachineMaxDurability")) {
                tooltip.add(Component.literal(
                        "Durability: " + formatHundredths(data.getLong("MachineDurabilityHundredths"))
                                + " / " + data.getInt("MachineMaxDurability")
                ).withStyle(ChatFormatting.GRAY));
                int corrosion = data.getInt("MachineCorrosionHundredthsPerTick");
                if (corrosion > 0) {
                    tooltip.add(Component.literal(
                            "Corrosion: -" + formatHundredths(corrosion) + "/tick"
                    ).withStyle(ChatFormatting.RED));
                }
            }

            int total = data.getInt("MachineProgressTotal");
            if (total > 0) {
                int done = Math.min(total, Math.max(0, data.getInt("MachineProgress")));
                float progress = Math.max(0.0F, Math.min(1.0F, (float) done / total));
                tooltip.add(IElementHelper.get().progress(
                        progress,
                        Component.literal("Duration: " + done + " / " + total + " ticks"),
                        IElementHelper.get().progressStyle().color(0xFFCC3333, 0xFF8B1A1A).textColor(0xFFFFFFFF),
                        BoxStyle.getNestedBox(),
                        true
                ));
            }

            String status = data.getString("MachineStatus");
            boolean kineticError = data.contains("MachineKineticError")
                    && !"NONE".equals(data.getString("MachineKineticError"));
            if (!status.isBlank()
                    && !"IDLE".equals(status)
                    && !"WORKING".equals(status)
                    && !(kineticError && "WAITING_FOR_RESOURCE".equals(status))) {
                tooltip.add(Component.literal(formatStatus(status)).withStyle(ChatFormatting.RED));
            }

            long energyCapacity = data.getLong("MachineEnergyCapacity");
            if (energyCapacity > 0) {
                CompoundTag energyData = new CompoundTag();
                energyData.putLong("Stored", data.getLong("MachineEnergyStored"));
                energyData.putLong("Capacity", energyCapacity);
                EnergyStorageProvider.appendEnergyTooltip(tooltip, energyData, 0xFFFFFFFF);
            }

            int steamCapacity = data.getInt("MachineSteamCapacity");
            if (steamCapacity > 0) {
                int stored = Math.min(steamCapacity, Math.max(0, data.getInt("MachineSteamStored")));
                tooltip.add(IElementHelper.get().progress(
                        stored / (float) steamCapacity,
                        Component.literal("Steam: " + stored + " / " + steamCapacity + " mB"),
                        IElementHelper.get().progressStyle().color(0xFF4DA6D8, 0xFF28769F).textColor(0xFFFFFFFF),
                        BoxStyle.getNestedBox(),
                        true
                ));
            }
        }

        @Override
        public void appendServerData(CompoundTag root, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof CERecipeLogicMachine machine)) {
                return;
            }

            CompoundTag data = root.getCompound(UID.toString());
            writeMachineData(data, machine, accessor.getLevel());
            if (machine instanceof SingleBlockMachineBlockEntity single) {
                data.putIntArray("RackRemainingTicks", single.dryingRackRemainingTicks());
            }
            root.put(UID.toString(), data);
        }


        private static List<ItemStack> displayItemInputs(CERecipe recipe) {
            List<ItemStack> stacks = new ArrayList<>();
            for (SizedIngredient input : recipe.itemInputs()) {
                ItemStack[] candidates = input.getItems();
                if (candidates.length > 0) stacks.add(candidates[0].copyWithCount(input.count()));
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

        private static List<ItemStack> displayItemOutputs(CERecipe recipe) {
            List<ItemStack> stacks = new ArrayList<>();
            recipe.itemOutputs().stream().map(output -> output.stack().copy()).filter(stack -> !stack.isEmpty()).forEach(stacks::add);
            return stacks;
        }

        private static List<FluidStack> displayFluidOutputs(CERecipe recipe) {
            List<FluidStack> stacks = new ArrayList<>();
            recipe.fluidOutputs().stream().map(FluidStack::copy).filter(stack -> !stack.isEmpty()).forEach(stacks::add);
            for (CEChancedFluidOutput output : recipe.chancedFluidOutputs()) {
                FluidStack stack = output.stack().copy();
                if (!stack.isEmpty()) stacks.add(stack);
            }
            return stacks;
        }

        private static void writeMachineData(CompoundTag data, CERecipeLogicMachine machine, Level level) {
            CERecipeLogic logic = machine.recipeLogic();
            CERecipeExecution execution = logic.execution();
            data.putInt("MachineProgress", logic.progress());
            data.putInt("MachineProgressTotal", logic.duration());
            int resourcePerTick = logic.resourcePerTick();
            if (machine instanceof MultiblockControllerBlockEntity controller) {
                resourcePerTick = saturatingInt(controller.displayResourcePerTick());
            }
            data.putInt("MachineResourcePerTick", resourcePerTick);
            data.putInt("MachineParallel", execution == null ? 0 : execution.parallel());
            CERecipe displayRecipe = execution == null || level == null
                    ? null
                    : CERecipeLookup.byId(level.getRecipeManager(), execution.recipeId()).map(holder -> holder.value()).orElse(null);
            writeItems(data, "MachineItemInputs", displayRecipe == null ? List.of() : displayItemInputs(displayRecipe));
            writeFluids(data, "MachineFluidInputs", displayRecipe == null ? List.of() : displayFluidInputs(displayRecipe));
            writeItems(data, "MachineItemOutputs", displayRecipe == null ? List.of() : displayItemOutputs(displayRecipe));
            writeFluids(data, "MachineFluidOutputs", displayRecipe == null ? List.of() : displayFluidOutputs(displayRecipe));
            data.putString("MachineStatus", logic.status().name());
            writeKineticData(data, machine);

            if (!(machine instanceof MultiblockControllerBlockEntity)) {
                data.remove("MachineCbHundredths");
                data.remove("MachineSafeCbMin");
                data.remove("MachineSafeCbMax");
                data.remove("MachineDurabilityHundredths");
                data.remove("MachineMaxDurability");
                data.remove("MachineCorrosionHundredthsPerTick");
            }

            if (machine instanceof SingleBlockMachineBlockEntity singleBlock
                    && singleBlock.energyCapacity() > 0) {
                CEEnergyContainer energy = singleBlock.ceContainer();
                data.putLong("MachineEnergyStored", singleBlock.energyStored());
                data.putLong("MachineEnergyCapacity", singleBlock.energyCapacity());
                data.putString("MachineEnergyTier", energy.tier().displayName());
                data.putLong("MachineEnergyVoltage", energy.voltage());
                data.putLong("MachineEnergyInputAmps", energy.maxInputAmps());
                data.remove("MachineSteamStored");
                data.remove("MachineSteamCapacity");
            } else if (machine instanceof SingleBlockMachineBlockEntity singleBlock
                    && singleBlock.steamCapacity() > 0) {
                data.putInt("MachineSteamStored", singleBlock.steamStored());
                data.putInt("MachineSteamCapacity", singleBlock.steamCapacity());
                data.remove("MachineEnergyStored");
                data.remove("MachineEnergyCapacity");
                data.remove("MachineEnergyTier");
                data.remove("MachineEnergyVoltage");
                data.remove("MachineEnergyInputAmps");
            } else if (machine instanceof MultiblockControllerBlockEntity controller) {
                boolean electric = controller.getBlockState().getBlock() instanceof MultiblockControllerBlock block
                        && MultiblockRegistry.byController(block.controllerId())
                        .map(definition -> definition.drive() == MachineDrive.ELECTRIC)
                        .orElse(false);
                if (electric) {
                    data.putLong("MachineEnergyVoltage", 1L);
                } else {
                    data.remove("MachineEnergyVoltage");
                }
                if (controller.getLevel() == null || !controller.getLevel().isClientSide()) {
                    if (controller.hasCbHatch()) {
                        data.putInt("MachineCbHundredths", controller.machineChemicalBalanceHundredths());
                    } else {
                        data.remove("MachineCbHundredths");
                    }
                    controller.safeChemicalBalanceRange().ifPresentOrElse(range -> {
                        data.putInt("MachineSafeCbMin", range.minHundredths());
                        data.putInt("MachineSafeCbMax", range.maxHundredths());
                    }, () -> {
                        data.remove("MachineSafeCbMin");
                        data.remove("MachineSafeCbMax");
                    });
                    if (controller.hasMachineDurability()) {
                        data.putLong("MachineDurabilityHundredths", controller.machineDurabilityHundredths());
                        data.putInt("MachineMaxDurability", controller.maxMachineDurability());
                        data.putInt("MachineCorrosionHundredthsPerTick", controller.corrosionDamageHundredthsPerTick());
                    } else {
                        data.remove("MachineDurabilityHundredths");
                        data.remove("MachineMaxDurability");
                        data.remove("MachineCorrosionHundredthsPerTick");
                    }
                }
                data.remove("MachineEnergyStored");
                data.remove("MachineEnergyCapacity");
                data.remove("MachineEnergyTier");
                data.remove("MachineEnergyInputAmps");
                data.remove("MachineSteamStored");
                data.remove("MachineSteamCapacity");
            } else {
                data.remove("MachineEnergyStored");
                data.remove("MachineEnergyCapacity");
                data.remove("MachineEnergyTier");
                data.remove("MachineEnergyVoltage");
                data.remove("MachineEnergyInputAmps");
                data.remove("MachineSteamStored");
                data.remove("MachineSteamCapacity");
            }
        }

        private static int saturatingInt(long value) {
            if (value > Integer.MAX_VALUE) return Integer.MAX_VALUE;
            if (value < Integer.MIN_VALUE) return Integer.MIN_VALUE;
            return (int) value;
        }

        private static void appendKineticTooltip(
                ITooltip tooltip,
                CompoundTag data
        ) {
            String mode = data.getString("MachineKineticMode");
            if ("INPUT".equals(mode)) {
                if (data.contains("MachineKineticSuPerRpm")) {
                    tooltip.add(Component.literal(
                            "Kinetic Input: "
                                    + formatNumber(data.getDouble("MachineKineticSuPerRpm"))
                                    + " SU/RPM"
                    ).withStyle(ChatFormatting.RED));
                }
                tooltip.add(Component.literal(
                        "Speed: " + data.getInt("MachineKineticRpm") + " RPM"
                ).withStyle(ChatFormatting.GRAY));
                if (data.contains("MachineKineticMinRpm")) {
                    tooltip.add(Component.literal(
                            "Minimum Speed: " + data.getInt("MachineKineticMinRpm") + " RPM"
                    ).withStyle(ChatFormatting.GRAY));
                }
                if (data.contains("MachineKineticMaxRpm")) {
                    tooltip.add(Component.literal(
                            "Maximum Speed: " + data.getInt("MachineKineticMaxRpm") + " RPM"
                    ).withStyle(ChatFormatting.GRAY));
                }

                String error = data.getString("MachineKineticError");
                if (KineticRpmError.INSUFFICIENT.name().equals(error)) {
                    tooltip.add(Component.literal("Insufficient RPM").withStyle(ChatFormatting.RED));
                } else if (KineticRpmError.TOO_AGGRESSIVE.name().equals(error)) {
                    tooltip.add(Component.literal("Too Aggressive RPM").withStyle(ChatFormatting.RED));
                }
            } else if ("OUTPUT".equals(mode)) {
                if (data.contains("MachineKineticSuPerRpm")) {
                    tooltip.add(Component.literal(
                            "Kinetic Output: "
                                    + formatNumber(data.getDouble("MachineKineticSuPerRpm"))
                                    + " SU/RPM"
                    ).withStyle(ChatFormatting.GREEN));
                }
                tooltip.add(Component.literal(
                        "Output Speed: " + data.getInt("MachineKineticRpm") + " RPM"
                ).withStyle(ChatFormatting.GRAY));
            }
        }

        private static void writeKineticData(
                CompoundTag data,
                CERecipeLogicMachine machine
        ) {
            clearKineticData(data);

            if (machine instanceof SingleBlockMachineBlockEntity singleBlock
                    && singleBlock.getBlockState().getBlock() instanceof SingleBlockMachineBlock block
                    && block.instance() != null) {
                if (block.instance().definition().usesKineticInput()) {
                    data.putString("MachineKineticMode", "INPUT");
                    data.putDouble("MachineKineticSuPerRpm", singleBlock.kineticSuPerRpm());
                    data.putInt("MachineKineticRpm", singleBlock.kineticRpm());
                    singleBlock.kineticMinimumRpm().ifPresent(value ->
                            data.putInt("MachineKineticMinRpm", value));
                    singleBlock.kineticMaximumRpm().ifPresent(value ->
                            data.putInt("MachineKineticMaxRpm", value));
                    data.putString("MachineKineticError", singleBlock.kineticRpmError().name());
                } else if (block.instance().definition().usesKineticOutput()) {
                    data.putString("MachineKineticMode", "OUTPUT");
                    data.putDouble("MachineKineticSuPerRpm", singleBlock.kineticSuPerRpm());
                    data.putInt("MachineKineticRpm", singleBlock.kineticOutputRpm());
                }
                return;
            }

            if (machine instanceof MultiblockControllerBlockEntity controller
                    && controller.isFormed()) {
                if (controller.usesKineticInput()) {
                    data.putString("MachineKineticMode", "INPUT");
                    data.putInt("MachineKineticRpm", controller.kineticInputRpm());
                    controller.kineticMinimumRpm().ifPresent(value ->
                            data.putInt("MachineKineticMinRpm", value));
                    controller.kineticMaximumRpm().ifPresent(value ->
                            data.putInt("MachineKineticMaxRpm", value));
                    data.putString("MachineKineticError", controller.kineticRpmError().name());
                } else if (controller.usesKineticOutput()) {
                    data.putString("MachineKineticMode", "OUTPUT");
                    data.putInt("MachineKineticRpm", controller.kineticOutputRpm());
                }
            }
        }

        private static void clearKineticData(CompoundTag data) {
            data.remove("MachineKineticMode");
            data.remove("MachineKineticSuPerRpm");
            data.remove("MachineKineticRpm");
            data.remove("MachineKineticMinRpm");
            data.remove("MachineKineticMaxRpm");
            data.remove("MachineKineticError");
        }

        private static String formatNumber(double value) {
            return BigDecimal.valueOf(value)
                    .stripTrailingZeros()
                    .toPlainString();
        }

        private static String formatHundredths(long value) {
            return BigDecimal.valueOf(value, 2)
                    .stripTrailingZeros()
                    .toPlainString();
        }

        private static String formatStatus(String status) {
            return switch (status) {
                case "WAITING_FOR_RESOURCE" -> "Waiting for power or steam";
                case "WAITING_FOR_OUTPUT" -> "Output full";
                case "WAITING_FOR_CB" -> "Waiting for correct CB";
                case "WAITING_FOR_RPM" -> "Waiting for correct RPM";
                case "MACHINE_INVALID" -> "Machine invalid";
                default -> status;
            };
        }

        private static void appendRecipeIo(
                ITooltip tooltip,
                CompoundTag data,
                String heading,
                String itemKey,
                String fluidKey,
                ChatFormatting color
        ) {
            ListTag items = data.getList(itemKey, Tag.TAG_COMPOUND);
            ListTag fluids = data.getList(fluidKey, Tag.TAG_COMPOUND);
            if (items.isEmpty() && fluids.isEmpty()) {
                return;
            }

            tooltip.add(Component.literal(heading).withStyle(color));
            for (Tag entry : items) {
                CompoundTag stackData = (CompoundTag) entry;
                ResourceLocation id = ResourceLocation.tryParse(stackData.getString("Id"));
                if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
                    continue;
                }
                ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(id));
                int amount = Math.max(1, stackData.getInt("Amount"));
                tooltip.add(List.of(
                        IElementHelper.get().smallItem(stack),
                        IElementHelper.get().text(Component.literal("x" + amount + "[" + stack.getHoverName().getString() + "]"))
                ));
            }
            for (Tag entry : fluids) {
                CompoundTag fluidData = (CompoundTag) entry;
                ResourceLocation id = ResourceLocation.tryParse(fluidData.getString("Id"));
                if (id == null || !BuiltInRegistries.FLUID.containsKey(id)) {
                    continue;
                }
                FluidStack fluid = new FluidStack(BuiltInRegistries.FLUID.get(id), Math.max(1, fluidData.getInt("Amount")));
                ItemStack icon = new ItemStack(fluid.getFluid().getBucket());
                if (icon.isEmpty() || icon.is(Items.AIR)) {
                    icon = new ItemStack(Items.BUCKET);
                }
                tooltip.add(List.of(
                        IElementHelper.get().smallItem(icon),
                        IElementHelper.get().text(Component.literal("x" + fluid.getAmount() + "[" + fluid.getHoverName().getString() + "]"))
                ));
            }
        }

        private static void writeItems(CompoundTag data, String key, List<ItemStack> stacks) {
            ListTag list = new ListTag();
            for (ItemStack stack : stacks) {
                if (stack.isEmpty()) {
                    continue;
                }
                CompoundTag entry = new CompoundTag();
                entry.putString("Id", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
                entry.putInt("Amount", stack.getCount());
                list.add(entry);
            }
            data.put(key, list);
        }

        private static void writeFluids(CompoundTag data, String key, List<FluidStack> stacks) {
            ListTag list = new ListTag();
            for (FluidStack stack : stacks) {
                if (stack.isEmpty()) {
                    continue;
                }
                CompoundTag entry = new CompoundTag();
                entry.putString("Id", BuiltInRegistries.FLUID.getKey(stack.getFluid()).toString());
                entry.putInt("Amount", stack.getAmount());
                list.add(entry);
            }
            data.put(key, list);
        }

        private static String voltageName(long voltage) {
            return MachineTierStats.tierForVoltage(voltage).displayName();
        }

        private static String oneDecimal(double value) {
            return String.format(Locale.ROOT, "%.1f", value);
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }

    private enum WireProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        INSTANCE;

        private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, "ce_wire");

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!(accessor.getBlock() instanceof EnergyWireBlock)) {
                return;
            }

            CompoundTag data = accessor.getServerData().getCompound(UID.toString());
            CompoundTag cableData = data.contains("cableData", Tag.TAG_COMPOUND)
                    ? data.getCompound("cableData")
                    : new CompoundTag();
            EnergyWireBlock wireBlock = (EnergyWireBlock) accessor.getBlock();
            EnergyWireBlockEntity wireEntity = accessor.getBlockEntity() instanceof EnergyWireBlockEntity wire
                    ? wire
                    : null;
            long currentVoltage = cableData.contains("currentVoltage")
                    ? cableData.getLong("currentVoltage")
                    : wireEntity == null ? 0L : wireEntity.currentVoltage();
            double currentAmperage = cableData.contains("currentAmperage")
                    ? cableData.getDouble("currentAmperage")
                    : wireEntity == null ? 0.0D : wireEntity.averageAmperage();
            double maxAmperage = cableData.contains("maxAmperage")
                    ? cableData.getDouble("maxAmperage")
                    : wireBlock.maxAmps();

            String voltage = currentVoltage > 0L ? voltageName(currentVoltage) : "None";
            String amperage = oneDecimal(currentAmperage) + "A / " + oneDecimal(maxAmperage) + "A";

            tooltip.add(Component.literal("Voltage: " + voltage).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("Amperage: " + amperage).withStyle(ChatFormatting.AQUA));
        }

        @Override
        public void appendServerData(CompoundTag root, BlockAccessor accessor) {
            if (!(accessor.getBlock() instanceof EnergyWireBlock)) {
                return;
            }
            CompoundTag data = root.getCompound(UID.toString());
            BlockEntity blockEntity = accessor.getLevel().getBlockEntity(accessor.getPosition());
            if (blockEntity instanceof EnergyWireBlockEntity wire) {
                CompoundTag cableData = new CompoundTag();
                cableData.putLong("currentVoltage", wire.currentVoltage());
                cableData.putLong("maxVoltage", wire.maxVoltage());
                cableData.putDouble("currentAmperage", wire.averageAmperage());
                cableData.putDouble("maxAmperage", wire.maxAmperage());
                cableData.putInt("temperature", wire.temperature());
                data.put("cableData", cableData);
            }
            root.put(UID.toString(), data);
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }

        private static String voltageName(long voltage) {
            return MachineTierStats.tierForVoltage(voltage).displayName();
        }

        private static String oneDecimal(double value) {
            return String.format(Locale.ROOT, "%.1f", value);
        }

    }

    private enum MultiblockStatusProvider implements IBlockComponentProvider {
        INSTANCE;

        private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, "multiblock_status");

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            boolean formed = accessor.getBlockState().hasProperty(MultiblockControllerBlock.FORMED)
                    && accessor.getBlockState().getValue(MultiblockControllerBlock.FORMED);

            tooltip.add(Component.literal(formed ? "Formed" : "Unformed")
                    .withStyle(formed ? ChatFormatting.GREEN : ChatFormatting.RED));
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }

    }

}
