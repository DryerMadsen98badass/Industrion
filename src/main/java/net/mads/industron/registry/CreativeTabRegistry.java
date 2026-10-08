package net.mads.industron.registry;

import net.mads.industron.Industron;
import net.mads.industron.transport.FluidTransportRegistrations;
import net.mads.industron.transport.color.ColoredFluidPipeRegistrations;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CreativeTabRegistry {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Industron.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = CREATIVE_MODE_TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.industron"))
                    .icon(CreativeTabRegistry::icon)
                    .displayItems((parameters, output) -> {
                        output.accept(ItemRegistry.MACHINE_CONTROL_SCHEDULE.get());
                        output.accept(ItemRegistry.MULTIBLOCK_DEV_TOOL.get());
                        output.accept(ItemRegistry.CREATIVE_GOGGLES.get());
                        output.accept(ItemRegistry.CLIMATE_INSTRUMENT.get());
                        output.accept(ItemRegistry.WOOL_HOOD.get());
                        output.accept(ItemRegistry.WOOL_COAT.get());
                        output.accept(ItemRegistry.WOOL_TROUSERS.get());
                        output.accept(ItemRegistry.WOOL_BOOTS.get());

                        net.mads.industron.machine.machines.kinetic.KineticMachines.items().forEach(item -> output.accept(item.get()));
                        output.accept(ItemRegistry.ASSEMBLY_WORKBENCH.get());
                        output.accept(ItemRegistry.INDUSTRIAL_ASSEMBLY_WORKBENCH.get());
                        ItemRegistry.getAllFoundryPartItems().forEach(item -> output.accept(item.get()));
                        net.mads.industron.machine.foundry.CastingRegistry.BLOCK_ITEMS.forEach(item -> output.accept(item.get()));
                        net.mads.industron.machine.foundry.CastingRegistry.MOLDS.values()
                                .forEach(item -> output.accept(item.get()));
                        net.mads.industron.machine.foundry.CastingRegistry.moldStageItems()
                                .forEach(item -> output.accept(item.get()));
                        ItemRegistry.getAllMultiblockControllerItems().forEach(item -> output.accept(item.get()));
                        ItemRegistry.getAllSingleBlockMachineItems().forEach(item -> output.accept(item.get()));
                        ItemRegistry.getAllCoilItems().forEach(item -> output.accept(item.get()));
                        ItemRegistry.getAllMachineCasingItems().forEach(item -> output.accept(item.get()));
                        ItemRegistry.getAllEnergyWireItems().forEach(item -> output.accept(item.get()));
                        ItemRegistry.getAllInsulatedEnergyWireItems().forEach(item -> output.accept(item.get()));
                        output.accept(ItemRegistry.CREATIVE_ENERGY_PROVIDER.get());
                        output.accept(ItemRegistry.CREATIVE_ENERGY_CONSUMER.get());
                        FluidTransportRegistrations.allItems().forEach(registration -> registration.visibleItems().forEach(item -> output.accept(item.get())));
                        ColoredFluidPipeRegistrations.allItems().forEach(registration -> output.accept(registration.pipe().get()));
                        ItemRegistry.getAllSimpleItems().forEach(item -> output.accept(item.get()));
                        // Finished composed tools now have valid canonical default components, so
                        // they can be exposed as normal items outside the Assembly Workbench too.
                        ItemRegistry.getAllComposedTools().forEach(item -> output.accept(item.get()));
                        ItemRegistry.getAllSimpleBlockItems().forEach(item -> output.accept(item.get()));
                        ItemRegistry.getAllSimpleBlockVariantItems().forEach(item -> output.accept(item.get()));
                        ItemRegistry.getAllMachinePortItems().forEach(item -> output.accept(item.get()));
                        ItemRegistry.getAllStaticMachinePortItems().forEach(item -> output.accept(item.get()));
                        ItemRegistry.getAllMaterialItems().forEach(item -> output.accept(item.get()));
                        ItemRegistry.getAllMaterialStoneItems().forEach(item -> output.accept(item.get()));
                        ItemRegistry.getAllStructureMaterialBlockItems().forEach(item -> output.accept(item.get()));
                        ItemRegistry.getAllStructureMaterialFormItems().forEach(item -> output.accept(item.get()));
                        ItemRegistry.ORGANISM_ITEMS.values().forEach(item -> output.accept(item.get()));
                        ItemRegistry.BIOLOGICAL_ITEMS.values().forEach(item -> output.accept(item.get()));
                        ItemRegistry.getAllPlantMaterialItems().forEach(item -> output.accept(item.get()));
                        ItemRegistry.getAllPlantProcessIntermediateItems().forEach(item -> output.accept(item.get()));
                        FluidRegistry.getAllBucketItems().forEach(item -> output.accept(item.get()));
                    })
                    .build()
    );

    private CreativeTabRegistry() {
    }

    public static void register(IEventBus modEventBus) { CREATIVE_MODE_TABS.register(modEventBus); }

    private static ItemStack icon() {
        return ItemRegistry.getAllMaterialItems().stream()
                .findFirst()
                .map(item -> new ItemStack(item.get()))
                .orElseGet(() -> new ItemStack(ItemRegistry.MACHINE_CONTROL_SCHEDULE.get()));
    }
}
