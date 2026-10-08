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
        net.mads.industron.material.chemistry.ChemistryBootstrap.runDataReports();

        if (event.includeServer() || event.includeClient()) {
            addProvider(event, new KineticMachineAssetProvider(event.getGenerator().getPackOutput(), event.includeClient(), event.includeServer()));
        }

        if (event.includeServer()) {
            addProvider(event, new OrganismCompositionReportProvider(event.getGenerator().getPackOutput()));
            addProvider(event, new OrganismLootProvider(event.getGenerator().getPackOutput(), event.getExistingFileHelper()));
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
            addProvider(event, new StoneShapingRecipeProvider(event.getGenerator().getPackOutput(), event.getExistingFileHelper()));
            addProvider(event, new ChiselingRecipeProvider(event.getGenerator().getPackOutput()));
            addProvider(event, new SimpleBlockRecipeProvider(event.getGenerator().getPackOutput()));
            addProvider(event, new ClayBrickShapeRecipeProvider(event.getGenerator().getPackOutput()));
            addProvider(event, new MaterialStoneLootProvider(event.getGenerator().getPackOutput()));
            addProvider(event, new MaterialOreLootProvider(event.getGenerator().getPackOutput()));
            addProvider(event, new MaterialShaftLootProvider(event.getGenerator().getPackOutput()));
            addProvider(event, new ClayBlockLootProvider(event.getGenerator().getPackOutput()));
            addProvider(event, new GeologyWorldgenDataProvider(event.getGenerator().getPackOutput()));
            addProvider(event, new PebbleWorldgenLootProvider(event.getGenerator().getPackOutput()));
            addProvider(event, new FallenStickLootProvider(event.getGenerator().getPackOutput()));
            addProvider(event, new SimpleBlockLootProvider(event.getGenerator().getPackOutput()));
            addProvider(event, new StructureMaterialLootProvider(event.getGenerator().getPackOutput()));
            addProvider(event, new AssemblySalvageLootProvider(event.getGenerator().getPackOutput()));
        }

        if (event.includeClient()) {
            addProvider(event, new AnimatedMachineTextureProvider(event.getGenerator().getPackOutput(), event.getExistingFileHelper()));
            addProvider(event, new OrganismAssetProvider(event.getGenerator().getPackOutput(), event.getExistingFileHelper()));
            addProvider(event, new StructureMaterialTextureProvider(event.getGenerator().getPackOutput(), event.getExistingFileHelper()));
            addProvider(event, new PebbleWorldgenModelProvider(event.getGenerator().getPackOutput(), event.getExistingFileHelper()));
            addProvider(event, new FallenStickModelProvider(event.getGenerator().getPackOutput()));
            addProvider(event, new PlantStorageAssetProvider(event.getGenerator().getPackOutput(), event.getExistingFileHelper()));
            addProvider(event, new StructureMaterialBlockStateProvider(event.getGenerator().getPackOutput(), event.getExistingFileHelper()));
            addProvider(event, new MetalStructureModelProvider(event.getGenerator().getPackOutput()));
            addProvider(event, new StructureMaterialItemModelProvider(event.getGenerator().getPackOutput(), event.getExistingFileHelper()));
            addProvider(event, new FluidTransportModelProvider(event.getGenerator().getPackOutput()));
            addProvider(event, new ModLanguageProvider(event.getGenerator().getPackOutput()));
            addProvider(event, new MaterialItemModelProvider(event.getGenerator().getPackOutput(), event.getExistingFileHelper()));
            addProvider(event, new MaterialBlockStateProvider(event.getGenerator().getPackOutput(), event.getExistingFileHelper()));
            addProvider(event, new FireboxModelProvider(event.getGenerator().getPackOutput(), event.getExistingFileHelper()));
            addProvider(event, new MachineCasingModelProvider(event.getGenerator().getPackOutput()));
            addProvider(event, new SingleBlockMachineModelProvider(event.getGenerator().getPackOutput(), event.getExistingFileHelper()));
            addProvider(event, new MachinePortModelProvider(event.getGenerator().getPackOutput()));
            addProvider(event, new FoundryModelProvider(event.getGenerator().getPackOutput(), event.getExistingFileHelper()));
            addProvider(event, new EnergyWireModelProvider(event.getGenerator().getPackOutput()));
            addProvider(event, new CoilModelProvider(event.getGenerator().getPackOutput()));
        }

        Industron.LOGGER.info("Generating Industron data");
    }
    private static void addProvider(GatherDataEvent event, net.minecraft.data.DataProvider provider) {
        event.addProvider(new TimedDataProvider(provider));
    }

}
