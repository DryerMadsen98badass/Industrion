package net.mads.industron;

import net.mads.industron.data.ModDataGenerators;
import net.mads.industron.compat.create.StructureCreateBehaviours;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockDefinitions;
import net.mads.industron.machine.machines.kinetic.KineticMachineStress;
import net.mads.industron.network.CENetwork;
import net.mads.industron.registry.BlockEntityRegistry;
import net.mads.industron.registry.BlockRegistry;
import net.mads.industron.registry.CreativeTabRegistry;
import net.mads.industron.registry.FluidRegistry;
import net.mads.industron.registry.ItemRegistry;
import net.mads.industron.registry.MenuRegistry;
import net.mads.industron.registry.RecipeRegistry;
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

        // Validate pure definitions before deferred registries consume them.
        IndustronValidation.validateOrThrow(ValidationStage.STARTUP);

        // Register all registries
        FluidRegistry.register(modEventBus);
        BlockRegistry.register(modEventBus);
        ItemRegistry.register(modEventBus);
        BlockEntityRegistry.register(modEventBus);
        MenuRegistry.register(modEventBus);
        RecipeRegistry.register(modEventBus);
        CreativeTabRegistry.register(modEventBus);
        IndustronPartialModels.init();

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
    }
}
