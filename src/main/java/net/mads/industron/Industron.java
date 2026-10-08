package net.mads.industron;

import com.simibubi.create.content.equipment.goggles.GogglesItem;
import net.mads.industron.data.ModDataGenerators;
import net.mads.industron.item.CreativeGogglesItem;
import net.mads.industron.compat.create.StructureCreateBehaviours;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockDefinitions;
import net.mads.industron.machine.machines.kinetic.KineticMachineStress;
import net.mads.industron.network.CENetwork;
import net.mads.industron.registry.BlockEntityRegistry;
import net.mads.industron.registry.BlockRegistry;
import net.mads.industron.registry.CreativeTabRegistry;
import net.mads.industron.registry.FluidRegistry;
import net.mads.industron.registry.EntityRegistry;
import net.mads.industron.registry.ItemRegistry;
import net.mads.industron.registry.MenuRegistry;
import net.mads.industron.registry.RecipeRegistry;
import net.mads.industron.registry.WorldgenRegistry;
import net.mads.industron.transport.color.ColoredFluidPipeRegistrations;
import net.mads.industron.validation.IndustronValidation;
import net.mads.industron.validation.ValidationStage;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod("industron")
public class Industron {
    public static final String MOD_ID = "industron";
    public static final Logger LOGGER = LoggerFactory.getLogger("Industron");
    private final ModContainer container;

    public Industron(IEventBus modEventBus, ModContainer container) {
        this.container = container;
        net.mads.industron.debug.StartupMemory.log("mod constructor entry");

        // Biological feed aliases and selected chemistry intermediates must exist before any registry reads ALL.
        net.mads.industron.material.organism.BiologicalChemistryBridge.initialize();

        // Validate pure definitions before deferred registries consume them.
        IndustronValidation.validateOrThrow(ValidationStage.STARTUP);
        net.mads.industron.debug.StartupMemory.log("definitions validated");

        // Register all registries
        net.mads.industron.machine.foundry.FoundryComponents.register(modEventBus);
        net.mads.industron.tool.ToolComponents.register(modEventBus);
        net.mads.industron.tool.EquipmentLootRegistry.register(modEventBus);
        net.mads.industron.machine.foundry.CastingRegistry.register(modEventBus);
        FluidRegistry.register(modEventBus);
        BlockRegistry.register(modEventBus);
        EntityRegistry.register(modEventBus);
        net.mads.industron.climate.ClothingRegistry.register(modEventBus);
        ItemRegistry.register(modEventBus);
        BlockEntityRegistry.register(modEventBus);
        MenuRegistry.register(modEventBus);
        RecipeRegistry.register(modEventBus);
        WorldgenRegistry.register(modEventBus);
        CreativeTabRegistry.register(modEventBus);
        IndustronPartialModels.init();
        net.mads.industron.debug.StartupMemory.log("registrations declared");

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(ModDataGenerators::gatherData);
        modEventBus.addListener(FluidRegistry::registerCapabilities);
        modEventBus.addListener(BlockEntityRegistry::registerCapabilities);
        modEventBus.addListener(CENetwork::register);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Industron is loading!");

        MultiblockDefinitions.bootstrap();
        event.enqueueWork(KineticMachineStress::register);
        event.enqueueWork(ColoredFluidPipeRegistrations::registerEncasingVariants);
        event.enqueueWork(StructureCreateBehaviours::register);
        event.enqueueWork(() -> GogglesItem.addIsWearingPredicate(CreativeGogglesItem::isWearing));
    }
}
