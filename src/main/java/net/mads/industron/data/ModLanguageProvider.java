package net.mads.industron.data;

import net.mads.industron.Industron;
import net.mads.industron.block.SimpleBlockDefinition;
import net.mads.industron.block.SimpleBlockVariant;
import net.mads.industron.block.SimpleBlocks;
import net.mads.industron.energy.EnergyWireBlock;
import net.mads.industron.energy.WireThickness;
import net.mads.industron.item.SimpleItems;
import net.mads.industron.machine.MachineDefinition;
import net.mads.industron.block.coils.CoilDefinition;
import net.mads.industron.block.coils.CoilDefinitions;
import net.mads.industron.machine.MachinePortType;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.machine.SingleBlockMachineInstance;
import net.mads.industron.machine.StaticMachinePortType;
import net.mads.industron.machine.foundry.FoundryPartType;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockDefinitions;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.defenitions.IndustrialMaterials;
import net.mads.industron.material.defenitions.PlantMaterials;
import net.mads.industron.material.plant.PlantMaterialGenerator;
import net.mads.industron.material.plant.PlantProcessingPlanner;
import net.mads.industron.material.plant.PlantPart;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.MaterialOreHost;
import net.mads.industron.material.recipes.MaterialCasingGenerator;
import net.mads.industron.material.structure.StructureBlockDefinition;
import net.mads.industron.material.structure.StructureMaterial;
import net.mads.industron.material.structure.StoneMaterial;
import net.mads.industron.material.structure.WoodMaterial;
import net.mads.industron.material.structure.StructureMaterialGenerator;
import net.mads.industron.material.structure.StructureMaterials;
import net.mads.industron.registry.FluidRegistry;
import net.mads.industron.recipe.recipes.assembly.ToolDefinitions;
import net.mads.industron.transport.FluidTransportTier;
import net.mads.industron.transport.color.PipeColorDefinitions;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModLanguageProvider extends LanguageProvider {
    public ModLanguageProvider(PackOutput output) {
        super(output, Industron.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup.industron", "Industron");
        add(blockKey("lathe"), "Lathe");
        add(blockKey("mechanical_centrifuge"), "Mechanical Centrifuge");
        add(blockKey("mechanical_sifter"), "Mechanical Sifter");
        add(blockKey("pulverizer"), "Pulverizer");
        add(blockKey("wire_drawing_machine"), "Wire Drawing Machine");
        add(blockKey("winding_machine"), "Winding Machine");
        add(blockKey("mechanical_bender"), "Mechanical Bender");
        add(blockKey("magnetic_separator"), "Magnetic Separator");
        add("tooltip.industron.kinetic_rpm", "Operating speed: %s–%s RPM");
        add("tooltip.industron.kinetic_progress", "Progress: %s%%");
        add("tooltip.industron.kinetic_not_fast_enough", "Rotation is stopped or outside the required RPM range.");
        add("tooltip.industron.kinetic_circuit", "Circuit setting: %s");
        add("message.industron.kinetic_circuit", "Circuit setting: %s (0 = none)");
        add("tooltip.industron.kinetic_controls", "Empty hand: retrieve output. Sneak: retrieve input. Sneak-click back: circuit.");
        add(itemKey("machine_control_schedule"), "Machine Control Schedule");
        add(itemKey("multiblock_dev_tool"), "Multiblock Dev Tool");
        add(itemKey("creative_goggles"), "Creative Goggles");
        add(itemKey("climate_instrument"), "Climate Instrument");
        add(itemKey("wool_hood"), "Wool Hood");
        add(itemKey("wool_coat"), "Wool Coat");
        add(itemKey("wool_trousers"), "Wool Trousers");
        add(itemKey("wool_boots"), "Wool Boots");

        // Vanilla stick is the Oak material's existing STICK form in Industron.
        add("item.minecraft.stick", "Oak Stick");
        add(blockKey("basic_assembly_workbench"), "Assembly Workbench");
        add(blockKey("assembly_workbench"), "Industrial Assembly Workbench");
        add("gui.industron.machine_control_schedule", "Machine Control Schedule");
        add("gui.industron.stone_shaping", "Stone Shaping");
        add("config.jade.plugin_industron.machine_info", "Machine Information");
        add("config.jade.plugin_industron.multiblock_status", "Multiblock Status");
        add("config.jade.plugin_industron.ce_wire", "CE Wire");
        add("config.jade.plugin_industron.kinetic_port", "Kinetic Port");
        add("config.jade.plugin_industron.ce_energy_storage", "CE Energy Storage");

        addSimpleItems();
        addSimpleBlocks();
        addFluidTransport();
        addMultiblockControllers();
        addFoundryBlocks();
        addMachineBlocks();
        addCoils();
        addMaterials();
        addStructureMaterials();
        for (var definition : net.mads.industron.material.organism.BiologicalItemCatalog.ALL) {
            add(itemKey(definition.id()), definition.displayName());
        }
        for (var definition : net.mads.industron.material.organism.OrganismItemCatalog.generated()) {
            add(itemKey(definition.itemId().substring("industron:".length())), definition.displayName());
        }
        addPlantMaterials();
        addComposedTools();
        addFluids();
    }

    private void addSimpleItems() {
        SimpleItems.ALL.forEach(definition -> add(itemKey(definition.id()), definition.displayName()));
    }

    private void addSimpleBlocks() {
        for (SimpleBlockDefinition definition : SimpleBlocks.ALL) {
            add(blockKey(definition.id()), definition.displayName());
            for (SimpleBlockVariant variant : definition.variants()) {
                add(blockKey(definition.variantId(variant)), definition.variantDisplayName(variant));
            }
        }
        SimpleBlocks.ACTIVE.forEach(definition -> add(blockKey(definition.id()), definition.displayName()));
    }

    private void addFluidTransport() {
        add(itemKey("fired_bucket"), "Fired Bucket");
        add(itemKey("fired_water_bucket"), "Fired Water Bucket");
        add(itemKey("fired_lava_bucket"), "Fired Lava Bucket");

        for (FluidTransportTier tier : FluidTransportTier.all()) {
            add(blockKey(tier.pipeId()), tier.pipeDisplayName());
            add(blockKey(tier.glassPipeId()), tier.glassPipeDisplayName());
            add(blockKey(tier.pumpId()), tier.pumpDisplayName());
            add(blockKey(tier.tankId()), tier.tankDisplayName());
        }
        for (PipeColorDefinitions.PipeFamily family : PipeColorDefinitions.allFamilies()) {
            for (DyeColor color : DyeColor.values()) {
                add(blockKey(family.coloredPipeId(color)), family.coloredDisplayName(color));
                add(blockKey(family.coloredGlassPipeId(color)), family.coloredGlassDisplayName(color));
            }
        }
    }

    private void addMultiblockControllers() {
        MultiblockDefinitions.controllers().forEach(controller -> add(blockKey(controller.registryName()), controller.displayName()));
    }

    private void addFoundryBlocks() {
        for (var clay : net.mads.industron.material.defenitions.IndustrialMaterials.ALL) {
            if (!clay.isClayMaterial()) continue;
            add(blockKey(clay.id() + "_caster"), clay.displayName() + " Caster");
            add(blockKey(clay.id() + "_faucet"), clay.displayName() + " Faucet");
        }
        for (FoundryPartType type : FoundryPartType.ALL) {
            add(blockKey(type.id()), type.displayName());
        }
    }

    private void addMachineBlocks() {
        for (MachineTier tier : MachineTier.ALL) {
            add(blockKey(tier.casingRegistryName()), tier.casingDisplayName());
            for (MachinePortType portType : MachinePortType.ALL) {
                add(blockKey(portType.registryName(tier)), portType.displayName(tier));
            }
        }
        for (MaterialCasingGenerator.GeneratedCasing generated : MaterialCasingGenerator.ALL) {
            add(blockKey(generated.registryName()), generated.displayName());
        }
        for (StaticMachinePortType portType : StaticMachinePortType.ALL) {
            add(blockKey(portType.id()), portType.displayName());
        }
        for (SingleBlockMachineInstance machine : MachineDefinition.INSTANCES) {
            add(blockKey(machine.registryName()), machine.displayName());
        }
    }

    private void addCoils() {
        for (CoilDefinition definition : CoilDefinitions.ALL) {
            add(blockKey(definition.blockId()), definition.displayName());
        }
    }

    private void addMaterials() {
        for (IndustrialMaterial material : IndustrialMaterials.ALL) {
            for (WireThickness thickness : WireThickness.ALL) {
                if (material.has(thickness.materialPart())) {
                    add(blockKey(EnergyWireBlock.registryName(material, thickness, false)), EnergyWireBlock.displayName(material, thickness, false));
                    add(blockKey(EnergyWireBlock.registryName(material, thickness, true)), EnergyWireBlock.displayName(material, thickness, true));
                }
            }
            for (var stoneSource : material.stoneSources()) {
                if (!stoneSource.isExisting()) {
                    add(blockKey(stoneSource.registryName(material)), stoneSource.displayName(material));
                }
            }
            if (MaterialOreHost.hasNaturalOre(material)) {
                for (MaterialOreHost host : MaterialOreHost.compatibleHosts(material)) {
                    for (boolean small : new boolean[]{false, true}) {
                        if (host.shouldGenerate(material, small)) {
                            add(blockKey(host.registryName(material, small)), host.displayName(material, small));
                        }
                    }
                }
            }
            for (MaterialPart part : material.parts()) {
                if (material.hasExistingPart(part) || part.isFluid()
                        || (part.isOre() && MaterialOreHost.hasNaturalOre(material))
                        || WireThickness.ALL.stream().anyMatch(thickness -> thickness.materialPart() == part)) {
                    continue;
                }
                String translationKey = part.isBlock()
                        ? blockKey(part.registryName(material))
                        : itemKey(part.registryName(material));
                add(translationKey, part.readableName(material));
                if (net.mads.industron.material.MaterialFormGenerator.hasMagneticVariant(material, part)) {
                    add(itemKey(part.magneticRegistryName(material)), part.magneticReadableName(material));
                }
            }
        }
    }

    private void addStructureMaterials() {
        for (StructureMaterial material : StructureMaterials.ALL) {
            for (StructureBlockDefinition definition : StructureMaterialGenerator.generatedBlockDefinitions(material)) {
                add(blockKey(definition.registryName()), definition.displayName());
            }
            for (MaterialPart part : StructureMaterialGenerator.generatedItemForms(material)) {
                add(itemKey(part.registryName(material)), part.readableName(material));
            }

            if (material instanceof StoneMaterial stone
                    && stone.generatedForms().contains(MaterialPart.PEBBLE)) {
                add(blockKey(stone.id() + "_loose_pebble"), MaterialPart.PEBBLE.readableName(stone));
            }
            if (material instanceof WoodMaterial wood
                    && (wood.generatedForms().contains(MaterialPart.STICK) || wood.hasExistingPart(MaterialPart.STICK))) {
                add(blockKey(wood.id() + "_fallen_stick"), MaterialPart.STICK.readableName(wood));
            }
        }
    }


    private void addPlantMaterials() {
        for (var material : net.mads.industron.material.plant.PlantStorage.materials())
            add("block.industron." + material.storageBlockId(), material.displayName() + " Stack");
        for (var material : PlantMaterials.ALL) {
            for (PlantPart part : PlantMaterialGenerator.generatedItemForms(material)) {
                add(itemKey(part.registryName(material)), part.readableName(material));
            }
        }
        for (var intermediate : PlantProcessingPlanner.allRequiredIntermediates()) {
            if (intermediate.isSolid()) {
                add(itemKey(intermediate.id()), intermediate.displayName());
            }
        }
    }

    private void addComposedTools() {
        ToolDefinitions.ALL.stream()
                .filter(net.mads.industron.recipe.recipetypes.assembly.ToolDefinition::isAssembledTool)
                .filter(net.mads.industron.recipe.recipetypes.assembly.ToolDefinition::isFinishedToolEnabled)
                .forEach(definition -> add(itemKey(definition.id()), definition.displayName()));
    }

    private void addFluids() {
        for (FluidRegistry.RegisteredFluid fluid : FluidRegistry.allFluids()) {
            String registryName = fluid.definition().registryName();
            String localizedName = fluid.definition().localizedName();
            add("fluid_type." + Industron.MOD_ID + "." + registryName, localizedName);
            add("fluid." + Industron.MOD_ID + "." + registryName, localizedName);
            add(itemKey(fluid.definition().bucketName()), fluid.definition().bucketDisplayName());
            add(itemKey(fluid.firedBucket().getId().getPath()), "Fired " + fluid.definition().bucketDisplayName());
        }
    }

    private static String blockKey(String id) {
        return "block." + Industron.MOD_ID + "." + id;
    }

    private static String itemKey(String id) {
        return "item." + Industron.MOD_ID + "." + id;
    }
}
