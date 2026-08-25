package net.mads.industron.data;

import net.mads.industron.Industron;
import net.mads.industron.validation.IndustronValidation;
import net.mads.industron.validation.ValidationStage;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.data.event.GatherDataEvent;

public class ModDataGenerators {
    private ModDataGenerators() {
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        IndustronValidation.validateOrThrow(ValidationStage.DATAGEN);

        if (event.includeServer()) {
            addProvider(event, new MaterialPropertyDebugProvider(event.getGenerator().getPackOutput()));
            MaterialBlockTagProvider blockTags = new MaterialBlockTagProvider(
                    event.getGenerator().getPackOutput(),
                    event.getLookupProvider(),
                    event.getExistingFileHelper()
            );
            addProvider(event, blockTags);
            addProvider(event, new MaterialItemTagProvider(
                    event.getGenerator().getPackOutput(),
                    event.getLookupProvider(),
                    blockTags.contentsGetter(),
                    event.getExistingFileHelper()
            ));
            addProvider(event, new CERecipeProvider(event.getGenerator().getPackOutput(), event.getLookupProvider()));
            addProvider(event, new SimpleBlockRecipeProvider(event.getGenerator().getPackOutput()));
            addProvider(event, new MaterialStoneLootProvider(event.getGenerator().getPackOutput()));
            addProvider(event, new SimpleBlockLootProvider(event.getGenerator().getPackOutput()));
            addProvider(event, new StructureMaterialLootProvider(event.getGenerator().getPackOutput()));
            addProvider(event, new SingleBlockMachineLootProvider(event.getGenerator().getPackOutput()));
            addProvider(event, new MaterialCasingLootProvider(event.getGenerator().getPackOutput()));
            addProvider(event, new MultiblockControlerLootTableProvider(event.getGenerator().getPackOutput(), event.getLookupProvider()));
        }

        if (event.includeClient()) {
            addProvider(event, new StructureMaterialTextureProvider(event.getGenerator().getPackOutput()));
            addProvider(event, new StructureMaterialBlockStateProvider(event.getGenerator().getPackOutput(), event.getExistingFileHelper()));
            addProvider(event, new MetalStructureModelProvider(event.getGenerator().getPackOutput()));
            addProvider(event, new StructureMaterialItemModelProvider(event.getGenerator().getPackOutput(), event.getExistingFileHelper()));
            addProvider(event, new FluidTransportModelProvider(event.getGenerator().getPackOutput()));
            addProvider(event, new ModLanguageProvider(event.getGenerator().getPackOutput()));
            addProvider(event, new MaterialItemModelProvider(event.getGenerator().getPackOutput(), event.getExistingFileHelper()));
            addProvider(event, new MaterialBlockStateProvider(event.getGenerator().getPackOutput(), event.getExistingFileHelper()));
            addProvider(event, new MachineCasingModelProvider(event.getGenerator().getPackOutput()));
            addProvider(event, new SingleBlockMachineModelProvider(event.getGenerator().getPackOutput()));
            addProvider(event, new MachinePortModelProvider(event.getGenerator().getPackOutput()));
            addProvider(event, new EnergyWireModelProvider(event.getGenerator().getPackOutput()));
        }

        Industron.LOGGER.info("Generating Industron data");
    }
    private static void addProvider(GatherDataEvent event, net.minecraft.data.DataProvider provider) {
        event.addProvider(new TimedDataProvider(provider));
    }

}
