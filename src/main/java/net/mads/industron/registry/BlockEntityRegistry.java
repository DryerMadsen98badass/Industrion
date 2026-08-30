package net.mads.industron.registry;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlockEntity;
import net.mads.industron.Industron;
import net.mads.industron.recipe.recipetypes.assembly.workbench.AssemblyWorkbenchBlockEntity;
import net.mads.industron.compat.create.BlazeBurnerFuelHandler;
import net.mads.industron.energy.CreativeEnergyBlockEntity;
import net.mads.industron.energy.EnergyWireBlockEntity;
import net.mads.industron.machine.MachinePortBlockEntity;
import net.mads.industron.machine.SingleBlockMachineBlockEntity;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockControllerBlockEntity;
import net.mads.industron.transport.FluidTransportRegistrations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.stream.Stream;

public class BlockEntityRegistry {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Industron.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MachinePortBlockEntity>> MACHINE_PORT = BLOCK_ENTITIES.register("machine_port", () ->
            BlockEntityType.Builder.of(MachinePortBlockEntity::new, allMachinePortBlocks()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MultiblockControllerBlockEntity>> MULTIBLOCK_CONTROLLER = BLOCK_ENTITIES.register("multiblock_controller", () ->
            BlockEntityType.Builder.of(MultiblockControllerBlockEntity::new, allControllerBlocks()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SingleBlockMachineBlockEntity>> SINGLE_BLOCK_MACHINE = BLOCK_ENTITIES.register("single_block_machine", () ->
            BlockEntityType.Builder.of(SingleBlockMachineBlockEntity::new, allSingleBlockMachineBlocks()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CreativeEnergyBlockEntity>> CREATIVE_ENERGY = BLOCK_ENTITIES.register("creative_energy", () ->
            BlockEntityType.Builder.of(CreativeEnergyBlockEntity::new, BlockRegistry.CREATIVE_ENERGY_PROVIDER.get(), BlockRegistry.CREATIVE_ENERGY_CONSUMER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyWireBlockEntity>> ENERGY_WIRE = BLOCK_ENTITIES.register("energy_wire", () ->
            BlockEntityType.Builder.of(EnergyWireBlockEntity::new, allEnergyWireBlocks()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AssemblyWorkbenchBlockEntity>> ASSEMBLY_WORKBENCH = BLOCK_ENTITIES.register("assembly_workbench", () ->
            BlockEntityType.Builder.of(AssemblyWorkbenchBlockEntity::new, BlockRegistry.ASSEMBLY_WORKBENCH.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SlidingDoorBlockEntity>> STRUCTURE_SLIDING_DOOR = BLOCK_ENTITIES.register("structure_sliding_door", () ->
            BlockEntityType.Builder.of(BlockEntityRegistry::createStructureSlidingDoorBlockEntity, allStructureSlidingDoorBlocks()).build(null));

    static {
        FluidTransportRegistrations.registerBlockEntities(BLOCK_ENTITIES);
    }

    public static void register(IEventBus modEventBus) { BLOCK_ENTITIES.register(modEventBus); }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MACHINE_PORT.get(), (port, side) -> port.itemCapability());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, MACHINE_PORT.get(), (port, side) -> port.fluidCapability());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SINGLE_BLOCK_MACHINE.get(), (machine, side) -> machine.itemCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, SINGLE_BLOCK_MACHINE.get(), (machine, side) -> machine.fluidCapability(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, AllBlockEntityTypes.HEATER.get(), (burner, side) -> BlazeBurnerFuelHandler.fluidCapability(burner));
        FluidTransportRegistrations.allBlockEntities().forEach(registration ->
                event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, registration.tank().get(), (tank, side) -> tank.fluidCapability())
        );
    }

    private static Block[] allMachinePortBlocks() {
        return Stream.concat(BlockRegistry.getAllMachinePorts().stream(), BlockRegistry.getAllStaticMachinePorts().stream()).map(DeferredHolder::get).toArray(Block[]::new);
    }

    private static Block[] allControllerBlocks() {
        return BlockRegistry.getAllMultiblockControllers().stream().map(DeferredHolder::get).toArray(Block[]::new);
    }

    private static Block[] allSingleBlockMachineBlocks() {
        return BlockRegistry.getAllSingleBlockMachines().stream().map(DeferredHolder::get).toArray(Block[]::new);
    }

    private static Block[] allEnergyWireBlocks() {
        return Stream.concat(BlockRegistry.getAllEnergyWires().stream(), BlockRegistry.getAllInsulatedEnergyWires().stream()).map(DeferredHolder::get).toArray(Block[]::new);
    }

    private static SlidingDoorBlockEntity createStructureSlidingDoorBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new SlidingDoorBlockEntity(STRUCTURE_SLIDING_DOOR.get(), pos, state);
    }

    private static Block[] allStructureSlidingDoorBlocks() {
        return BlockRegistry.getAllStructureSlidingDoorBlocks().stream()
                .map(DeferredHolder::get)
                .toArray(Block[]::new);
    }
}
