package net.mads.industron.client;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorRenderer;
import com.simibubi.create.content.fluids.tank.FluidTankRenderer;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.simibubi.create.foundation.model.ModelSwapper;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import dev.engine_room.flywheel.lib.model.Models;
import net.createmod.catnip.lang.FontHelper;
import net.mads.industron.Industron;
import net.mads.industron.IndustronPartialModels;
import net.mads.industron.block.SimpleBlockDefinition;
import net.mads.industron.block.SimpleBlocks;
import net.mads.industron.client.model.EnergyWireDynamicModel;
import net.mads.industron.client.model.FluidTransportPipeAttachmentModel;
import net.mads.industron.client.model.FluidTransportTankModel;
import net.mads.industron.client.model.MetalStructureConnectedModels;
import net.mads.industron.client.model.ModelSharingCache;
import net.mads.industron.client.tool.ComposedToolRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.mads.industron.client.screen.MachineControlScheduleScreen;
import net.mads.industron.client.screen.MachinePortScreen;
import net.mads.industron.client.screen.MultiblockControllerScreen;
import net.mads.industron.client.screen.SingleBlockMachineScreen;
import net.mads.industron.energy.EnergyWireBlock;
import net.mads.industron.machine.MachineCasingBlock;
import net.mads.industron.machine.MaterialMachineCasingBlock;
import net.mads.industron.machine.MachineModelTintResolver;
import net.mads.industron.machine.MachinePortBlock;
import net.mads.industron.machine.MachinePortBlockEntity;
import net.mads.industron.machine.SingleBlockDefinition;
import net.mads.industron.machine.SingleBlockMachineBlock;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockControllerBlock;
import net.mads.industron.kinetics.shaft.AbstractMaterialShaftBlock;
import net.mads.industron.material.MaterialItem;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.MaterialPartBlock;
import net.mads.industron.material.defenitions.ClayMaterials;
import net.mads.industron.material.defenitions.WoodMaterials;
import net.mads.industron.registry.BlockEntityRegistry;
import net.mads.industron.registry.BlockRegistry;
import net.mads.industron.registry.FluidRegistry;
import net.mads.industron.registry.ItemRegistry;
import net.mads.industron.registry.EntityRegistry;
import net.mads.industron.registry.MenuRegistry;
import net.mads.industron.transport.FluidTransportGlassPipeRenderer;
import net.mads.industron.transport.FluidTransportPumpRenderer;
import net.mads.industron.transport.FluidTransportRegistrations;
import net.mads.industron.transport.color.ColoredCreateGlassPipeRenderer;
import net.mads.industron.transport.color.ColoredFluidPipeRegistrations;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.model.DynamicFluidContainerModel;

@EventBusSubscriber(modid = Industron.MOD_ID, value = Dist.CLIENT, bus = Bus.MOD)
public class ClientSetup {
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        net.mads.industron.debug.StartupMemory.log("client setup");
        net.mads.industron.climate.ClimateContext.client = () -> {var mc=net.minecraft.client.Minecraft.getInstance();return mc.isSameThread()?mc.level:null;};
        IndustronSpriteShifts.init();
        event.enqueueWork(() -> SimpleBlockEntityVisualizer.builder(net.mads.industron.machine.machines.kinetic.KineticMachines.LATHE_ENTITY.get())
                .factory(net.mads.industron.client.kinetic.KineticMachineVisual::new)
                .skipVanillaRender(blockEntity -> false).apply());
        event.enqueueWork(() -> SimpleBlockEntityVisualizer.builder(net.mads.industron.machine.machines.kinetic.KineticMachines.CENTRIFUGE_ENTITY.get())
                .factory(net.mads.industron.client.kinetic.KineticMachineVisual::new)
                .skipVanillaRender(blockEntity -> false).apply());
        event.enqueueWork(() -> SimpleBlockEntityVisualizer.builder(net.mads.industron.machine.machines.kinetic.KineticMachines.MECHANICALSIFTER_ENTITY.get())
                .factory(net.mads.industron.client.kinetic.KineticMachineVisual::new)
                .skipVanillaRender(blockEntity -> false).apply());
        event.enqueueWork(() -> SimpleBlockEntityVisualizer.builder(net.mads.industron.machine.machines.kinetic.KineticMachines.PULVERIZER_ENTITY.get())
                .factory(net.mads.industron.client.kinetic.KineticMachineVisual::new)
                .skipVanillaRender(blockEntity -> false).apply());
        event.enqueueWork(() -> SimpleBlockEntityVisualizer.builder(net.mads.industron.machine.machines.kinetic.KineticMachines.WIREDRAWINGMACHINE_ENTITY.get())
                .factory(net.mads.industron.client.kinetic.KineticMachineVisual::new)
                .skipVanillaRender(blockEntity -> false).apply());
        event.enqueueWork(() -> SimpleBlockEntityVisualizer.builder(net.mads.industron.machine.machines.kinetic.KineticMachines.WINDINGMACHINE_ENTITY.get())
                .factory(net.mads.industron.client.kinetic.KineticMachineVisual::new)
                .skipVanillaRender(blockEntity -> false).apply());
        event.enqueueWork(() -> SimpleBlockEntityVisualizer.builder(net.mads.industron.machine.machines.kinetic.KineticMachines.MECHANICALBENDER_ENTITY.get())
                .factory(net.mads.industron.client.kinetic.KineticMachineVisual::new)
                .skipVanillaRender(blockEntity -> false).apply());
        event.enqueueWork(() -> SimpleBlockEntityVisualizer.builder(net.mads.industron.machine.machines.kinetic.KineticMachines.MAGNETICSEPARATOR_ENTITY.get())
                .factory(net.mads.industron.client.kinetic.KineticMachineVisual::new)
                .skipVanillaRender(blockEntity -> false).apply());
        event.enqueueWork(() -> net.mads.industron.machine.machines.kinetic.KineticMachines.blocks().forEach(holder ->
                TooltipModifier.REGISTRY.register(holder.get().asItem(), new KineticStats(holder.get()))));
        event.enqueueWork(() -> {
            IndustronPartialModels.initClient();
            MetalStructureConnectedModels.init();
            registerFluidPipeRenderLayers();
            registerFallenStickRenderLayers();
            registerMaterialShaftTooltips();
        });
        event.enqueueWork(() -> TooltipModifier.REGISTRY.register(
                ItemRegistry.MACHINE_CONTROL_SCHEDULE.get(),
                new ItemDescription.Modifier(
                        ItemRegistry.MACHINE_CONTROL_SCHEDULE.get(),
                        FontHelper.Palette.STANDARD_CREATE
                )
        ));
        FluidTransportRegistrations.allItems().forEach(registration ->
                event.enqueueWork(() -> TooltipModifier.REGISTRY.register(
                        registration.pump().get(),
                        new KineticStats(FluidTransportRegistrations.blocks(registration.tier()).pump().get())
                ))
        );
        FluidTransportRegistrations.allBlockEntities().forEach(registration ->
                event.enqueueWork(() -> SimpleBlockEntityVisualizer.builder(registration.pump().get())
                        .factory(SingleAxisRotatingVisual.ofZ(AllPartialModels.MECHANICAL_PUMP_COG))
                        .skipVanillaRender(blockEntity -> false)
                        .apply())
        );
        event.enqueueWork(() -> SimpleBlockEntityVisualizer.builder(BlockEntityRegistry.MATERIAL_SHAFT.get())
                .factory((context, blockEntity, partialTick) -> new MaterialShaftVisual(
                        context,
                        blockEntity,
                        partialTick,
                        Models.partial(IndustronPartialModels.materialShaft(blockEntity.getBlockState().getBlock()))
                ))
                .skipVanillaRender(blockEntity -> false)
                .apply());
    }

    private static void registerMaterialShaftTooltips() {
        net.minecraft.core.registries.BuiltInRegistries.BLOCK.forEach(block -> {
            if (!(block instanceof AbstractMaterialShaftBlock shaft)) return;
            if (!shaft.shaftLimits().hasAnyLimit()) return;
            Item item = block.asItem();
            if (item == net.minecraft.world.item.Items.AIR) return;

            TooltipModifier.REGISTRY.register(item, tooltip -> {
                var lines = tooltip.getToolTip();
                if (shaft.shaftLimits().limitsRpm()) {
                    lines.add(net.minecraft.network.chat.Component.literal(
                            "Max RPM: " + Math.round(shaft.shaftLimits().maxRpm())
                    ).withStyle(net.minecraft.ChatFormatting.GRAY));
                }
                if (shaft.shaftLimits().limitsNetworkStress()) {
                    lines.add(net.minecraft.network.chat.Component.literal(
                            "Max Stress: " + Math.round(shaft.shaftLimits().maxNetworkStress()) + " SU"
                    ).withStyle(net.minecraft.ChatFormatting.GRAY));
                }
            });
        });
    }

    @SuppressWarnings("deprecation")
    private static void registerFallenStickRenderLayers() {
        BlockRegistry.FALLEN_STICK_BLOCKS.values().forEach(holder ->
                ItemBlockRenderTypes.setRenderLayer(holder.get(), RenderType.cutout())
        );
    }

    @SuppressWarnings("deprecation")
    private static void registerFluidPipeRenderLayers() {
        FluidTransportRegistrations.allBlocks().forEach(registration ->
                ItemBlockRenderTypes.setRenderLayer(registration.glassPipe().get(), RenderType.cutoutMipped())
        );
        ColoredFluidPipeRegistrations.allBlocks().forEach(registration ->
                ItemBlockRenderTypes.setRenderLayer(registration.glassPipe().get(), RenderType.cutoutMipped())
        );
    }

    @SubscribeEvent
    public static void modifyBakedModels(ModelEvent.ModifyBakingResult event) {
        net.mads.industron.debug.StartupMemory.log("model swapping entry (base baking already completed)");
        long started = System.nanoTime();
        net.mads.industron.client.model.ItemGeometrySharing.apply(event);
        ModelSharingCache<BakedModel> sharing = new ModelSharingCache<>();
        BlockRegistry.getAllEnergyWires().forEach(holder -> swapEnergyWireModel(event, holder.get(), sharing));
        BlockRegistry.getAllInsulatedEnergyWires().forEach(holder -> swapEnergyWireModel(event, holder.get(), sharing));

        FluidTransportRegistrations.allBlocks().forEach(registration -> {
            String pipeModelId = registration.tier().pipeId();
            swapPipeModel(event, registration.pipe().get(), pipeModelId, sharing);
            swapPipeModel(event, registration.glassPipe().get(), pipeModelId, sharing);
            swapPipeModel(event, registration.pump().get(), pipeModelId, sharing);
            ModelSwapper.swapModels(event.getModels(), ModelSwapper.getAllBlockStateModelLocations(registration.tank().get()), model -> sharing.wrap(registration.tier(), model, original -> new FluidTransportTankModel(original, registration.tier())));
        });

        ColoredFluidPipeRegistrations.allBlocks().forEach(registration -> {
            String pipeModelId = registration.modelId();
            swapPipeModel(event, registration.pipe().get(), pipeModelId, sharing);
            swapPipeModel(event, registration.glassPipe().get(), pipeModelId, sharing);
        });
        Industron.LOGGER.info("Industron model sharing: {} swaps, {} unique wrappers, {} avoided allocations, {} model entries, {} ms",
                sharing.requests(), sharing.created(), sharing.requests() - sharing.created(),
                event.getModels().size(), (System.nanoTime() - started) / 1_000_000);
        net.mads.industron.debug.StartupMemory.log("model swapping finished");
    }

    private static void swapPipeModel(ModelEvent.ModifyBakingResult event, Block block, String modelId,
                                      ModelSharingCache<BakedModel> sharing) {
        ModelSwapper.swapModels(event.getModels(), ModelSwapper.getAllBlockStateModelLocations(block),
                model -> sharing.wrap("pipe:" + modelId, model,
                        original -> new FluidTransportPipeAttachmentModel(original, modelId)));
    }

    private static void swapEnergyWireModel(ModelEvent.ModifyBakingResult event, EnergyWireBlock wire,
                                          ModelSharingCache<BakedModel> sharing) {
        String key = "wire:" + wire.thickness().id() + ":" + wire.insulated();
        ModelSwapper.swapModels(
                event.getModels(),
                ModelSwapper.getAllBlockStateModelLocations(wire),
                model -> sharing.wrap(key, model, original -> new EnergyWireDynamicModel(original, wire.thickness(), wire.insulated()))
        );
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.HOTBAR,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, "multiblock_dev_tool"),
                MultiblockDevToolSelectionHandler::renderOverlay
        );
        event.registerAbove(
                VanillaGuiLayers.HOTBAR,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, "assembly_next_step"),
                AssemblyNextStepOverlay::renderOverlay
        );
        event.registerAbove(
                VanillaGuiLayers.HOTBAR,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, "creative_goggles_anvil"),
                CreativeGogglesAnvilOverlay::renderOverlay
        );
        event.registerAbove(VanillaGuiLayers.HOTBAR,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID,"climate"),ClimateClient::renderOverlay);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BlockEntityRegistry.SINGLE_BLOCK_MACHINE.get(), PrimitiveSingleBlockRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.MATERIAL_SHAFT.get(), MaterialShaftRenderer::new);
        event.registerBlockEntityRenderer(net.mads.industron.machine.machines.kinetic.KineticMachines.LATHE_ENTITY.get(),
                net.mads.industron.client.kinetic.KineticMachineRenderer::new);
        event.registerBlockEntityRenderer(net.mads.industron.machine.machines.kinetic.KineticMachines.CENTRIFUGE_ENTITY.get(),
                net.mads.industron.client.kinetic.KineticMachineRenderer::new);
        event.registerBlockEntityRenderer(net.mads.industron.machine.machines.kinetic.KineticMachines.MECHANICALSIFTER_ENTITY.get(),
                net.mads.industron.client.kinetic.KineticMachineRenderer::new);
        event.registerBlockEntityRenderer(net.mads.industron.machine.machines.kinetic.KineticMachines.PULVERIZER_ENTITY.get(),
                net.mads.industron.client.kinetic.KineticMachineRenderer::new);
        event.registerBlockEntityRenderer(net.mads.industron.machine.machines.kinetic.KineticMachines.WIREDRAWINGMACHINE_ENTITY.get(),
                net.mads.industron.client.kinetic.KineticMachineRenderer::new);
        event.registerBlockEntityRenderer(net.mads.industron.machine.machines.kinetic.KineticMachines.WINDINGMACHINE_ENTITY.get(),
                net.mads.industron.client.kinetic.KineticMachineRenderer::new);
        event.registerBlockEntityRenderer(net.mads.industron.machine.machines.kinetic.KineticMachines.MECHANICALBENDER_ENTITY.get(),
                net.mads.industron.client.kinetic.KineticMachineRenderer::new);
        event.registerBlockEntityRenderer(net.mads.industron.machine.machines.kinetic.KineticMachines.MAGNETICSEPARATOR_ENTITY.get(),
                net.mads.industron.client.kinetic.KineticMachineRenderer::new);

        event.registerBlockEntityRenderer(BlockEntityRegistry.ASSEMBLY_WORKBENCH.get(), AssemblyWorkbenchRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.STRUCTURE_WOOD_CHEST.get(), StructureWoodChestRenderer::new);
        for (var wood : WoodMaterials.ALL) {
            var boat = EntityRegistry.boat(wood);
            if (boat != null) {
                registerStructureWoodBoatRenderer(event, boat.get(), wood, false);
            }
            var chestBoat = EntityRegistry.chestBoat(wood);
            if (chestBoat != null) {
                registerStructureWoodBoatRenderer(event, chestBoat.get(), wood, true);
            }
        }
        event.registerBlockEntityRenderer(BlockEntityRegistry.MACHINE_PORT.get(), MachinePortOverlayRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.FOUNDRY_PART.get(), FoundryCasingRenderer::new);
        event.registerBlockEntityRenderer(net.mads.industron.machine.foundry.CastingRegistry.ENTITY.get(), CastingRenderer::new);
        event.registerBlockEntityRenderer(BlockEntityRegistry.MULTIBLOCK_CONTROLLER.get(), MultiblockControllerCasingRenderer::new);
        event.registerBlockEntityRenderer(
                BlockEntityRegistry.STRUCTURE_SLIDING_DOOR.get(),
                SlidingDoorRenderer::new
        );
        FluidTransportRegistrations.allBlockEntities().forEach(registration -> {
            event.registerBlockEntityRenderer(registration.glassPipe().get(), FluidTransportGlassPipeRenderer::new);
            event.registerBlockEntityRenderer(registration.pump().get(), FluidTransportPumpRenderer::new);
            event.registerBlockEntityRenderer(registration.tank().get(), FluidTankRenderer::new);
        });
        event.registerBlockEntityRenderer(
                ColoredFluidPipeRegistrations.createGlassPipeBlockEntity().get(),
                ColoredCreateGlassPipeRenderer::new
        );
    }


    private static void registerStructureWoodBoatRenderer(
            EntityRenderersEvent.RegisterRenderers event,
            net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.vehicle.Boat> entityType,
            net.mads.industron.material.structure.WoodMaterial wood,
            boolean chestBoat
    ) {
        event.<net.minecraft.world.entity.vehicle.Boat>registerEntityRenderer(
                entityType,
                context -> new StructureWoodBoatRenderer(context, wood, chestBoat)
        );
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> tintIndex == 0 ? 0xFF4A8CFF : -1, ItemRegistry.CREATIVE_GOGGLES.get());
        for (var holder : net.mads.industron.machine.foundry.CastingRegistry.MOLDS.values()) {
            event.register((stack, tint) -> tint == 0
                    && stack.getItem() instanceof net.mads.industron.machine.foundry.CastingMoldItem mold
                    ? opaque(mold.clay().color()) : -1, holder.get());
        }
        for (var holder : net.mads.industron.machine.foundry.CastingRegistry.moldStageItems()) {
            event.register((stack, tint) -> tint == 0
                    && stack.getItem() instanceof net.mads.industron.machine.foundry.CastingMoldStageItem mold
                    ? opaque(mold.clay().color()) : -1, holder.get());
        }
        for (var holder : net.mads.industron.machine.foundry.CastingRegistry.BLOCK_ITEMS) {
            event.register((stack, tint) -> tint == 0 && stack.getItem() instanceof BlockItem item
                    ? castingColor(item.getBlock()) : -1, holder.get());
        }
        Item[] materialItems = ItemRegistry.getAllMaterialItems().stream().map(item -> item.get()).toArray(Item[]::new);
        Item[] fluidBuckets = FluidRegistry.getAllBucketItems().stream().map(item -> item.get()).toArray(Item[]::new);
        Item[] machineCasings = ItemRegistry.getAllMachineCasingItems().stream().map(item -> item.get()).toArray(Item[]::new);
        Item[] energyWires = ItemRegistry.getAllEnergyWireItems().stream().map(item -> item.get()).toArray(Item[]::new);
        Item[] insulatedEnergyWires = ItemRegistry.getAllInsulatedEnergyWireItems().stream().map(item -> item.get()).toArray(Item[]::new);
        Item[] machinePorts = ItemRegistry.getAllMachinePortItems().stream().map(item -> item.get()).toArray(Item[]::new);
        Item[] staticMachinePorts = ItemRegistry.getAllStaticMachinePortItems().stream().map(item -> item.get()).toArray(Item[]::new);
        Item[] foundryParts = ItemRegistry.getAllFoundryPartItems().stream().map(item -> item.get()).toArray(Item[]::new);
        Item[] multiblockControllers = ItemRegistry.getAllMultiblockControllerItems().stream().map(item -> item.get()).toArray(Item[]::new);
        Item[] singleBlockMachines = ItemRegistry.getAllSingleBlockMachineItems().stream().map(item -> item.get()).toArray(Item[]::new);

        event.register((stack, tintIndex) -> {
            if (!isMaterialTintLayer(tintIndex)) {
                return -1;
            }
            Item item = stack.getItem();
            if (item instanceof MaterialItem materialItem) {
                return materialItemColor(materialItem, tintIndex);
            }
            if (item instanceof BlockItem blockItem && blockItem.getBlock() instanceof MaterialPartBlock materialBlock) {
                return opaque(materialBlock.material().color());
            }
            return -1;
        }, materialItems);

        event.register((stack, tintIndex) -> {
            if (tintIndex != 1) {
                return -1;
            }
            for (FluidRegistry.RegisteredFluid fluid : FluidRegistry.allFluids()) {
                if (stack.is(fluid.bucket().get())) {
                    return fluid.type().get().color();
                }
            }
            return new DynamicFluidContainerModel.Colors().getColor(stack, tintIndex);
        }, fluidBuckets);

        event.register((stack, tintIndex) -> {
            if (tintIndex != 0 || !(stack.getItem() instanceof BlockItem blockItem)
                    || !(blockItem.getBlock() instanceof MachineCasingBlock casing)) {
                return -1;
            }
            return opaque(casing instanceof MaterialMachineCasingBlock materialCasing
                    ? materialCasing.material().color()
                    : casing.tier().color());
        }, machineCasings);
        event.register((stack, tintIndex) -> tintIndex == 0 && stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof EnergyWireBlock wire ? opaque(wire.material().color()) : -1, merge(energyWires, insulatedEnergyWires));
        event.register((stack, tintIndex) -> {
            if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof MachinePortBlock port) {
                if (tintIndex == 0 && port.usesTint()) {
                    return opaque(port.tintColor());
                }
                if (tintIndex == 1 && port.hasTier() && portColorable(port)) {
                    return dyeColor(DyeColor.GRAY);
                }
            }
            return -1;
        }, merge(machinePorts, staticMachinePorts));
        event.register((stack, tintIndex) -> tintIndex == 0 ? opaque(ClayMaterials.AERUM.color()) : -1, foundryParts);
        event.register((stack, tintIndex) -> stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof MultiblockControllerBlock controller && controller.usesTint(tintIndex) ? opaque(controller.tintColor(tintIndex)) : -1, multiblockControllers);
        event.register((stack, tintIndex) -> stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof SingleBlockMachineBlock machine ? singleBlockMachineColor(machine, tintIndex) : -1, singleBlockMachines);

        for (SimpleBlockDefinition definition : SimpleBlocks.ALL) {
            if (!definition.hasColor()) {
                continue;
            }
            Item[] simpleBlockItems = merge(
                    new Item[]{ItemRegistry.getSimpleBlockItem(definition.id()).get()},
                    definition.variants().stream().map(variant -> ItemRegistry.getSimpleBlockVariantItem(definition.id(), variant).get()).toArray(Item[]::new)
            );
            event.register((stack, tintIndex) -> tintIndex == 0 ? definition.blockColor() : -1, simpleBlockItems);
        }
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> {
            if (!(state.getBlock() instanceof net.mads.industron.material.plant.PlantStorageBlock storage) || tintIndex != 0) return -1;
            var source = storage.plantItem();
            if (source.getItem() instanceof net.minecraft.world.item.BlockItem blockItem)
                return net.minecraft.client.Minecraft.getInstance().getBlockColors().getColor(blockItem.getBlock().defaultBlockState(), level, pos, 0);
            return net.minecraft.client.Minecraft.getInstance().getItemColors().getColor(source, 0);
        }, BlockRegistry.PLANT_STORAGE_BLOCKS.values().stream().map(holder -> holder.get()).toArray(net.minecraft.world.level.block.Block[]::new));
        for (var group : java.util.List.of(net.mads.industron.machine.foundry.CastingRegistry.CASTERS,
                net.mads.industron.machine.foundry.CastingRegistry.FAUCETS,
                net.mads.industron.machine.foundry.CastingRegistry.DRAINS)) {
            for (var holder : group.values()) event.register((state, level, pos, tint) ->
                    tint == 0 ? castingColor(state.getBlock()) : -1, holder.get());
        }
        Block[] materialBlocks = BlockRegistry.getAllMaterialBlocks().stream().map(block -> block.get()).toArray(Block[]::new);
        Block[] machineCasings = BlockRegistry.getAllMachineCasings().stream().map(block -> block.get()).toArray(Block[]::new);
        Block[] energyWires = BlockRegistry.getAllEnergyWires().stream().map(block -> block.get()).toArray(Block[]::new);
        Block[] insulatedEnergyWires = BlockRegistry.getAllInsulatedEnergyWires().stream().map(block -> block.get()).toArray(Block[]::new);
        Block[] machinePorts = BlockRegistry.getAllMachinePorts().stream().map(block -> block.get()).toArray(Block[]::new);
        Block[] staticMachinePorts = BlockRegistry.getAllStaticMachinePorts().stream().map(block -> block.get()).toArray(Block[]::new);
        Block[] foundryParts = BlockRegistry.getAllFoundryParts().stream().map(block -> block.get()).toArray(Block[]::new);
        Block[] multiblockControllers = BlockRegistry.getAllMultiblockControllers().stream().map(block -> block.get()).toArray(Block[]::new);
        Block[] singleBlockMachines = BlockRegistry.getAllSingleBlockMachines().stream().map(block -> block.get()).toArray(Block[]::new);

        event.register((state, level, pos, tintIndex) -> isMaterialTintLayer(tintIndex) && state.getBlock() instanceof MaterialPartBlock materialBlock ? opaque(materialBlock.material().color()) : -1, materialBlocks);
        event.register((state, level, pos, tintIndex) -> {
            if (tintIndex != 0 || !(state.getBlock() instanceof MachineCasingBlock casing)) {
                return -1;
            }
            return opaque(casing instanceof MaterialMachineCasingBlock materialCasing
                    ? materialCasing.material().color()
                    : casing.tier().color());
        }, machineCasings);
        event.register((state, level, pos, tintIndex) -> tintIndex == 0 && state.getBlock() instanceof EnergyWireBlock wire && !wire.insulated() ? opaque(wire.material().color()) : -1, merge(energyWires, insulatedEnergyWires));
        event.register((state, level, pos, tintIndex) -> {
            if (state.getBlock() instanceof MachinePortBlock port) {
                if (tintIndex == 0 && port.usesTint()) {
                    return opaque(port.tintColor());
                }
                if (tintIndex == 1 && port.hasTier() && level != null && pos != null && level.getBlockEntity(pos) instanceof MachinePortBlockEntity portEntity && portEntity.supportsIoColor()) {
                    return dyeColor(portEntity.ioColor());
                }
            }
            return -1;
        }, merge(machinePorts, staticMachinePorts));
        event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? opaque(ClayMaterials.AERUM.color()) : -1, foundryParts);
        event.register((state, level, pos, tintIndex) -> state.getBlock() instanceof MultiblockControllerBlock controller && controller.usesTint(tintIndex) ? opaque(controller.tintColor(tintIndex)) : -1, multiblockControllers);
        event.register((state, level, pos, tintIndex) -> state.getBlock() instanceof SingleBlockMachineBlock machine ? singleBlockMachineColor(machine, tintIndex) : -1, singleBlockMachines);

        for (SimpleBlockDefinition definition : SimpleBlocks.ALL) {
            if (!definition.hasColor()) {
                continue;
            }
            Block[] simpleBlocks = merge(
                    new Block[]{BlockRegistry.getSimpleBlock(definition.id()).get()},
                    definition.variants().stream().map(variant -> BlockRegistry.getSimpleBlockVariant(definition.id(), variant).get()).toArray(Block[]::new)
            );
            event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? definition.blockColor() : -1, simpleBlocks);
        }
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        Item[] composedTools = ItemRegistry.getAllComposedTools().stream()
                .map(holder -> (Item) holder.get())
                .toArray(Item[]::new);
        if (composedTools.length > 0) {
            ComposedToolRenderer renderer = new ComposedToolRenderer();
            event.registerItem(new IClientItemExtensions() {
                @Override
                public BlockEntityWithoutLevelRenderer getCustomRenderer() { return renderer; }
                @Override
                public int getArmorLayerTintColor(net.minecraft.world.item.ItemStack stack,
                        net.minecraft.world.entity.LivingEntity entity,
                        net.minecraft.world.item.ArmorMaterial.Layer layer, int layerIdx, int fallbackColor) {
                    return net.mads.industron.tool.EquipmentStats.colour(stack);
                }
                @Override
                public net.minecraft.client.model.HumanoidModel.ArmPose getArmPose(
                        net.minecraft.world.entity.LivingEntity entity,
                        net.minecraft.world.InteractionHand hand, net.minecraft.world.item.ItemStack stack) {
                    if (entity.isUsingItem() && entity.getUsedItemHand() == hand) {
                        if (stack.getItem() instanceof net.minecraft.world.item.BowItem)
                            return net.minecraft.client.model.HumanoidModel.ArmPose.BOW_AND_ARROW;
                        if (stack.getItem() instanceof net.minecraft.world.item.CrossbowItem)
                            return net.minecraft.client.model.HumanoidModel.ArmPose.CROSSBOW_CHARGE;
                        if (stack.getItem() instanceof net.minecraft.world.item.ShieldItem)
                            return net.minecraft.client.model.HumanoidModel.ArmPose.BLOCK;
                    }
                    if (stack.getItem() instanceof net.minecraft.world.item.CrossbowItem
                            && net.minecraft.world.item.CrossbowItem.isCharged(stack))
                        return net.minecraft.client.model.HumanoidModel.ArmPose.CROSSBOW_HOLD;
                    return null;
                }
            }, composedTools);
        }

        Item[] structureWoodChests = ItemRegistry.getAllStructureMaterialBlockItems().stream()
                .map(holder -> (Item) holder.get())
                .filter(item -> item instanceof BlockItem blockItem
                        && blockItem.getBlock() instanceof net.mads.industron.material.structure.StructureWoodChestBlock)
                .toArray(Item[]::new);
        if (structureWoodChests.length > 0) {
            StructureWoodChestItemRenderer renderer = new StructureWoodChestItemRenderer();
            event.registerItem(new IClientItemExtensions() {
                @Override
                public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    return renderer;
                }
            }, structureWoodChests);
        }

        for (FluidRegistry.RegisteredFluid fluid : FluidRegistry.allFluids()) {
            event.registerFluidType(new IClientFluidTypeExtensions() {
                @Override
                public int getTintColor() {
                    return fluid.type().get().color();
                }

                @Override
                public net.minecraft.resources.ResourceLocation getStillTexture() {
                    return fluid.type().get().texture();
                }

                @Override
                public net.minecraft.resources.ResourceLocation getFlowingTexture() {
                    return fluid.type().get().texture();
                }
            }, fluid.type());
        }
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(MenuRegistry.FOUNDRY.get(), net.mads.industron.client.screen.FoundryScreen::new);
        event.register(MenuRegistry.MACHINE_PORT.get(), MachinePortScreen::new);
        event.register(MenuRegistry.MULTIBLOCK_CONTROLLER.get(), MultiblockControllerScreen::new);
        event.register(MenuRegistry.SINGLE_BLOCK_MACHINE.get(), SingleBlockMachineScreen::new);
        event.register(MenuRegistry.MACHINE_CONTROL_SCHEDULE.get(), MachineControlScheduleScreen::new);
        event.register(MenuRegistry.STONE_SHAPING.get(), net.mads.industron.client.screen.StoneShapingScreen::new);
    }

    private static int castingColor(Block block) {
        if (block instanceof net.mads.industron.machine.foundry.CastingBlock caster)
            return caster.clay().hasExistingPart(MaterialPart.BRICKS) ? -1 : opaque(caster.clay().color());
        if (block instanceof net.mads.industron.machine.foundry.FoundryDrainBlock drain) return opaque(drain.clay().color());
        return -1;
    }

    private static boolean isMaterialTintLayer(int tintIndex) {
        return tintIndex == 0 || tintIndex == 1;
    }

    private static int materialItemColor(MaterialItem item, int tintIndex) {
        if (tintIndex == 0) {
            int color = item.material().color();
            if (usesDarkerCastTint(item.part())) color = darken(color, 0.65F);
            return opaque(color);
        }
        if (tintIndex == 1 && hasSecondaryItemLayer(item)) {
            return opaque(item.material().properties().highlightColor());
        }
        return -1;
    }

    private static boolean hasSecondaryItemLayer(MaterialItem item) {
        if (item.material().hasCustomPartTexture(item.part())) return false;
        MaterialPart texturePart = net.mads.industron.material.MaterialVariantResolver.coldTexturePart(item.part());
        return net.mads.industron.material.MaterialVariantResolver.itemTextures(item.material(), texturePart)
                .flatMap(net.mads.industron.material.MaterialVariantResolver.ItemTextureSet::secondary)
                .isPresent();
    }

    private static int singleBlockMachineColor(SingleBlockMachineBlock machine, int tintIndex) {
        SingleBlockDefinition.MachineSide side = SingleBlockDefinition.MachineSide.fromTintIndex(tintIndex);
        if (side == null) {
            return -1;
        }
        Integer customColor = machine.instance().definition().sideTextureColor(side);
        if (customColor != null) {
            return opaque(customColor);
        }
        if (tintIndex == 0) {
            Integer modelColor = MachineModelTintResolver.resolve(machine.instance().definition().model());
            if (modelColor != null) {
                return opaque(modelColor);
            }
        }
        if (machine.instance().definition().sideTexture(side) == null && (machine.instance().tier().isElectric() || machine.instance().tier() == net.mads.industron.machine.MachineTier.STEAM_LV)) {
            return opaque(machine.instance().tier().color());
        }
        return -1;
    }

    private static int opaque(int color) {
        return 0xFF000000 | color;
    }

    private static Item[] merge(Item[] first, Item[] second) {
        Item[] merged = new Item[first.length + second.length];
        System.arraycopy(first, 0, merged, 0, first.length);
        System.arraycopy(second, 0, merged, first.length, second.length);
        return merged;
    }

    private static Block[] merge(Block[] first, Block[] second) {
        Block[] merged = new Block[first.length + second.length];
        System.arraycopy(first, 0, merged, 0, first.length);
        System.arraycopy(second, 0, merged, first.length, second.length);
        return merged;
    }

    private static boolean usesDarkerCastTint(MaterialPart part) {
        return switch (part) {
            case CAST_NUGGET,
                 CAST_BLOCK,
                 CAST_PLATE,
                 CAST_ROD,
                 CAST_LONG_ROD,
                 CAST_BOLT,
                 CAST_SCREW,
                 CAST_RING,
                 CAST_SMALL_RING,
                 CAST_LARGE_RING,
                 CAST_GEAR,
                 CAST_SMALL_GEAR,
                 CAST_BEARING_BALL,
                 CAST_BEARING,
                 CAST_ROTOR,
                 HOT_CAST_NUGGET_MOLD,
                 HOT_CAST_BEARING_BALL_MOLD,
                 HOT_CAST_ROTOR_MOLD,
                 HOT_CAST_INGOT_MOLD,
                 HOT_CAST_PLATE_MOLD,
                 HOT_CAST_ROD_MOLD,
                 HOT_CAST_LONG_ROD_MOLD,
                 HOT_CAST_BOLT_MOLD,
                 HOT_CAST_RING_MOLD,
                 HOT_CAST_SMALL_RING_MOLD,
                 HOT_CAST_LARGE_RING_MOLD,
                 HOT_CAST_GEAR_MOLD,
                 HOT_CAST_SMALL_GEAR_MOLD,
                 HOT_CAST_BEARING_MOLD,
                 HOT_CAST_SCREW_MOLD -> true;
            default -> false;
        };
    }

    private static int darken(int color, float multiplier) {
        int red = (int) (((color >> 16) & 0xFF) * multiplier);
        int green = (int) (((color >> 8) & 0xFF) * multiplier);
        int blue = (int) ((color & 0xFF) * multiplier);
        return (red << 16) | (green << 8) | blue;
    }

    private static boolean portColorable(MachinePortBlock port) {
        return port.abilities().contains(net.mads.industron.machine.machines.electric.multiblock.MultiblockAbility.ITEM_INPUT)
                || port.abilities().contains(net.mads.industron.machine.machines.electric.multiblock.MultiblockAbility.ITEM_OUTPUT)
                || port.abilities().contains(net.mads.industron.machine.machines.electric.multiblock.MultiblockAbility.FLUID_INPUT)
                || port.abilities().contains(net.mads.industron.machine.machines.electric.multiblock.MultiblockAbility.FLUID_OUTPUT)
                || port.abilities().contains(net.mads.industron.machine.machines.electric.multiblock.MultiblockAbility.IO_INTERFACE);
    }

    private static int dyeColor(DyeColor color) {
        return switch (color) {
            case WHITE -> 0xFFF9FFFE;
            case ORANGE -> 0xFFF9801D;
            case MAGENTA -> 0xFFC74EBD;
            case LIGHT_BLUE -> 0xFF3AB3DA;
            case YELLOW -> 0xFFFED83D;
            case LIME -> 0xFF80C71F;
            case PINK -> 0xFFF38BAA;
            case GRAY -> 0xFF474F52;
            case LIGHT_GRAY -> 0xFF9D9D97;
            case CYAN -> 0xFF169C9C;
            case PURPLE -> 0xFF8932B8;
            case BLUE -> 0xFF3C44AA;
            case BROWN -> 0xFF835432;
            case GREEN -> 0xFF5E7C16;
            case RED -> 0xFFB02E26;
            case BLACK -> 0xFF1D1D21;
        };
    }
}
