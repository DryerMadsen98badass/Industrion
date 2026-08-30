package net.mads.industron.registry;

import com.simibubi.create.content.decoration.MetalScaffoldingBlockItem;
import com.simibubi.create.content.decoration.bracket.BracketBlockItem;
import net.mads.industron.Industron;
import net.mads.industron.block.SimpleBlockDefinition;
import net.mads.industron.block.SimpleBlocks;
import net.mads.industron.block.SimpleBlockVariant;
import net.mads.industron.energy.EnergyWireBlock;
import net.mads.industron.energy.WireThickness;
import net.mads.industron.item.FiredBucketItem;
import net.mads.industron.item.MultiblockDevToolItem;
import net.mads.industron.item.SimpleItemDefinition;
import net.mads.industron.item.SimpleItems;
import net.mads.industron.machine.MachineDefinition;
import net.mads.industron.machine.MachinePortType;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.machine.SingleBlockMachineInstance;
import net.mads.industron.machine.StaticMachinePortType;
import net.mads.industron.machine.control.MachineControlScheduleItem;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockRegistrations;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.defenitions.IndustrialMaterials;
import net.mads.industron.material.MaterialItem;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.MaterialOreHost;
import net.mads.industron.material.MaterialFormGenerator;
import net.mads.industron.material.recipes.MaterialCasingGenerator;
import net.mads.industron.material.structure.StructureBlockDefinition;
import net.mads.industron.material.structure.StructureMaterial;
import net.mads.industron.material.structure.StructureMaterialGenerator;
import net.mads.industron.material.structure.StructureMaterialItem;
import net.mads.industron.material.structure.StructureMaterials;
import net.mads.industron.transport.FluidTransportRegistrations;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ItemRegistry {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, Industron.MOD_ID);

    public static final DeferredHolder<Item, FiredBucketItem> FIRED_BUCKET =
            ITEMS.register("fired_bucket", () -> new FiredBucketItem(Fluids.EMPTY, new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, MachineControlScheduleItem> MACHINE_CONTROL_SCHEDULE =
            ITEMS.register("machine_control_schedule", () -> new MachineControlScheduleItem(new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, MultiblockDevToolItem> MULTIBLOCK_DEV_TOOL =
            ITEMS.register("multiblock_dev_tool", () -> new MultiblockDevToolItem(new Item.Properties().stacksTo(1)));

    public static final Map<String, DeferredHolder<Item, BlockItem>> MACHINE_CASINGS = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<Item, BlockItem>> MATERIAL_MACHINE_CASINGS = new LinkedHashMap<>();
    public static final Map<String, Map<MachinePortType, DeferredHolder<Item, BlockItem>>> MACHINE_PORTS = new LinkedHashMap<>();
    public static final Map<StaticMachinePortType, DeferredHolder<Item, BlockItem>> STATIC_MACHINE_PORTS = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<Item, BlockItem>> MULTIBLOCK_CONTROLLERS = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<Item, BlockItem>> SINGLE_BLOCK_MACHINES = new LinkedHashMap<>();
    public static final Map<String, Map<String, DeferredHolder<Item, BlockItem>>> MATERIAL_STONE_ITEMS = new LinkedHashMap<>();
    public static final Map<String, Map<String, DeferredHolder<Item, BlockItem>>> MATERIAL_ORE_HOST_ITEMS = new LinkedHashMap<>();
    public static final Map<String, Map<MaterialPart, DeferredHolder<Item, ? extends Item>>> MATERIAL_ITEMS = new LinkedHashMap<>();
    public static final Map<String, Map<MaterialPart, DeferredHolder<Item, ? extends Item>>> MAGNETIC_MATERIAL_ITEMS = new LinkedHashMap<>();
    public static final Map<String, Map<WireThickness, DeferredHolder<Item, BlockItem>>> ENERGY_WIRES = new LinkedHashMap<>();
    public static final Map<String, Map<WireThickness, DeferredHolder<Item, BlockItem>>> INSULATED_ENERGY_WIRES = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<Item, BlockItem>> STRUCTURE_MATERIAL_BLOCK_ITEMS = new LinkedHashMap<>();
    public static final Map<String, Map<MaterialPart, DeferredHolder<Item, StructureMaterialItem>>> STRUCTURE_MATERIAL_FORM_ITEMS = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<Item, Item>> SIMPLE_ITEMS = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<Item, BlockItem>> SIMPLE_BLOCK_ITEMS = new LinkedHashMap<>();
    public static final Map<String, Map<SimpleBlockVariant, DeferredHolder<Item, BlockItem>>> SIMPLE_BLOCK_VARIANT_ITEMS = new LinkedHashMap<>();

    public static final DeferredHolder<Item, BlockItem> CREATIVE_ENERGY_PROVIDER =
            ITEMS.register("creative_energy_provider", () -> new BlockItem(BlockRegistry.CREATIVE_ENERGY_PROVIDER.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> CREATIVE_ENERGY_CONSUMER =
            ITEMS.register("creative_energy_consumer", () -> new BlockItem(BlockRegistry.CREATIVE_ENERGY_CONSUMER.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> ASSEMBLY_WORKBENCH =
            ITEMS.register("assembly_workbench", () -> new BlockItem(BlockRegistry.ASSEMBLY_WORKBENCH.get(), new Item.Properties()));

    static {
        FluidTransportRegistrations.registerItems(ITEMS);
        registerSimpleBlockItems();
        registerSimpleItems();
        registerMultiblockControllerItems();
        registerSingleBlockMachineItems();
        registerTieredItems();
        registerStaticMachinePortItems();
        registerMaterialItems();
        registerMaterialMachineCasingItems();
        registerStructureMaterialItems();
    }

    private ItemRegistry() {
    }

    private static void registerSimpleBlockItems() {
        for (SimpleBlockDefinition definition : SimpleBlocks.ALL) {
            DeferredHolder<Item, BlockItem> baseItem = ITEMS.register(definition.id(), () -> new BlockItem(BlockRegistry.getSimpleBlock(definition.id()).get(), new Item.Properties()));
            SIMPLE_BLOCK_ITEMS.put(definition.id(), baseItem);
            Map<SimpleBlockVariant, DeferredHolder<Item, BlockItem>> variantItems = new LinkedHashMap<>();
            for (SimpleBlockVariant variant : definition.variants()) {
                String registryName = definition.variantId(variant);
                variantItems.put(variant, ITEMS.register(registryName, () -> new BlockItem(BlockRegistry.getSimpleBlockVariant(definition.id(), variant).get(), new Item.Properties())));
            }
            SIMPLE_BLOCK_VARIANT_ITEMS.put(definition.id(), variantItems);
        }
    }

    private static void registerSimpleItems() {
        for (SimpleItemDefinition definition : SimpleItems.ALL) {
            SIMPLE_ITEMS.put(definition.id(), ITEMS.register(definition.id(), () -> {
                Item.Properties properties = new Item.Properties();
                if (definition.hasDurability()) {
                    properties.durability(definition.durability());
                }
                return new Item(properties);
            }));
        }
    }

    private static void registerMultiblockControllerItems() {
        MultiblockRegistrations.registerControllerItems(ITEMS, MULTIBLOCK_CONTROLLERS, BlockRegistry.MULTIBLOCK_CONTROLLERS);
    }

    private static void registerSingleBlockMachineItems() {
        for (SingleBlockMachineInstance instance : MachineDefinition.INSTANCES) {
            SINGLE_BLOCK_MACHINES.put(instance.registryName(), ITEMS.register(instance.registryName(), () -> new BlockItem(BlockRegistry.getSingleBlockMachine(instance.registryName()).get(), new Item.Properties())));
        }
    }

    private static void registerTieredItems() {
        for (MachineTier tier : MachineTier.ALL) {
            MACHINE_CASINGS.put(tier.id(), ITEMS.register(tier.casingRegistryName(), () -> new BlockItem(BlockRegistry.MACHINE_CASINGS.get(tier.id()).get(), new Item.Properties())));
            Map<MachinePortType, DeferredHolder<Item, BlockItem>> ports = new LinkedHashMap<>();
            for (MachinePortType portType : MachinePortType.ALL) {
                ports.put(portType, ITEMS.register(portType.registryName(tier), () -> new BlockItem(BlockRegistry.MACHINE_PORTS.get(tier.id()).get(portType).get(), new Item.Properties())));
            }
            MACHINE_PORTS.put(tier.id(), ports);
        }
    }

    private static void registerStaticMachinePortItems() {
        for (StaticMachinePortType portType : StaticMachinePortType.ALL) {
            STATIC_MACHINE_PORTS.put(portType, ITEMS.register(portType.id(), () -> new BlockItem(BlockRegistry.STATIC_MACHINE_PORTS.get(portType).get(), new Item.Properties())));
        }
    }

    private static void registerMaterialItems() {
        for (IndustrialMaterial material : IndustrialMaterials.ALL) {
            Map<String, DeferredHolder<Item, BlockItem>> stoneItems = new LinkedHashMap<>();
            for (var stoneSource : material.stoneSources()) {
                if (!stoneSource.isExisting()) {
                    stoneItems.put(stoneSource.id(), ITEMS.register(stoneSource.registryName(material), () -> new BlockItem(BlockRegistry.getMaterialStoneBlock(material, stoneSource.id()).get(), new Item.Properties())));
                }
            }
            MATERIAL_STONE_ITEMS.put(material.id(), stoneItems);

            Map<String, DeferredHolder<Item, BlockItem>> oreHostItems = new LinkedHashMap<>();
            if (MaterialOreHost.hasNaturalOre(material)) {
                for (MaterialOreHost host : MaterialOreHost.compatibleHosts(material)) {
                    for (boolean small : new boolean[]{false, true}) {
                        var block = BlockRegistry.getMaterialOreHostBlock(material, host, small);
                        if (block == null) {
                            continue;
                        }
                        String key = host.key(small);
                        oreHostItems.put(
                                key,
                                ITEMS.register(host.registryName(material, small), () -> new BlockItem(block.get(), new Item.Properties()))
                        );
                    }
                }
            }
            MATERIAL_ORE_HOST_ITEMS.put(material.id(), oreHostItems);

            Map<MaterialPart, DeferredHolder<Item, ? extends Item>> items = new LinkedHashMap<>();
            Map<MaterialPart, DeferredHolder<Item, ? extends Item>> magneticItems = new LinkedHashMap<>();

            Map<WireThickness, DeferredHolder<Item, BlockItem>> wires = new LinkedHashMap<>();
            Map<WireThickness, DeferredHolder<Item, BlockItem>> insulatedWires = new LinkedHashMap<>();
            Map<WireThickness, DeferredHolder<Block, EnergyWireBlock>> materialWireBlocks = BlockRegistry.ENERGY_WIRES.get(material.id());
            Map<WireThickness, DeferredHolder<Block, EnergyWireBlock>> materialInsulatedWireBlocks = BlockRegistry.INSULATED_ENERGY_WIRES.get(material.id());
            if (materialWireBlocks != null && materialInsulatedWireBlocks != null) {
                for (WireThickness thickness : WireThickness.ALL) {
                    if (!materialWireBlocks.containsKey(thickness)) {
                        continue;
                    }
                    String wireName = EnergyWireBlock.registryName(material, thickness, false);
                    String insulatedWireName = EnergyWireBlock.registryName(material, thickness, true);
                    wires.put(thickness, ITEMS.register(wireName, () -> new BlockItem(materialWireBlocks.get(thickness).get(), new Item.Properties())));
                    insulatedWires.put(thickness, ITEMS.register(insulatedWireName, () -> new BlockItem(materialInsulatedWireBlocks.get(thickness).get(), new Item.Properties())));
                }
                ENERGY_WIRES.put(material.id(), wires);
                INSULATED_ENERGY_WIRES.put(material.id(), insulatedWires);
            }

            for (MaterialPart part : material.parts()) {
                if (part.isOre() && MaterialOreHost.hasNaturalOre(material)) {
                    continue;
                }
                if (material.hasExistingPart(part) || part.isFluid() || isFunctionalWirePart(part)) {
                    continue;
                }
                DeferredHolder<Item, ? extends Item> item = part.isBlock()
                        ? ITEMS.register(part.registryName(material), () -> new BlockItem(BlockRegistry.getMaterialBlock(material, part).get(), new Item.Properties()))
                        : ITEMS.register(part.registryName(material), () -> new MaterialItem(material, part));
                items.put(part, item);

                if (MaterialFormGenerator.hasMagneticVariant(material, part)) {
                    magneticItems.put(part, ITEMS.register(
                            part.magneticRegistryName(material),
                            () -> new MaterialItem(material, part, true)
                    ));
                }
            }
            MATERIAL_ITEMS.put(material.id(), items);
            MAGNETIC_MATERIAL_ITEMS.put(material.id(), magneticItems);
        }
    }

    private static void registerMaterialMachineCasingItems() {
        for (MaterialCasingGenerator.GeneratedCasing generated : MaterialCasingGenerator.ALL) {
            MATERIAL_MACHINE_CASINGS.put(
                    generated.registryName(),
                    ITEMS.register(
                            generated.registryName(),
                            () -> new BlockItem(
                                    BlockRegistry.getMaterialMachineCasing(generated.registryName()).get(),
                                    new Item.Properties()
                            )
                    )
            );
        }
    }

    private static void registerStructureMaterialItems() {
        for (StructureMaterial material : StructureMaterials.ALL) {
            for (StructureBlockDefinition definition : StructureMaterialGenerator.generatedBlockDefinitions(material)) {
                STRUCTURE_MATERIAL_BLOCK_ITEMS.put(
                        definition.registryName(),
                        ITEMS.register(definition.registryName(), () -> {
                            Block block = BlockRegistry.getStructureMaterialBlock(definition.registryName()).get();
                            return switch (definition.modelKind()) {
                                case CREATE_BRACKET -> new BracketBlockItem(block, new Item.Properties());
                                case CREATE_SCAFFOLD -> new MetalScaffoldingBlockItem(block, new Item.Properties());
                                default -> new BlockItem(block, new Item.Properties());
                            };
                        })
                );
            }

            Map<MaterialPart, DeferredHolder<Item, StructureMaterialItem>> forms = new LinkedHashMap<>();
            for (MaterialPart part : StructureMaterialGenerator.generatedItemForms(material)) {
                forms.put(part, ITEMS.register(
                        part.registryName(material),
                        () -> new StructureMaterialItem(material, part)
                ));
            }
            STRUCTURE_MATERIAL_FORM_ITEMS.put(material.id(), forms);
        }
    }

    private static boolean isFunctionalWirePart(MaterialPart part) {
        return WireThickness.ALL.stream().anyMatch(thickness -> thickness.materialPart() == part);
    }

    public static void register(IEventBus modEventBus) { ITEMS.register(modEventBus); }
    public static DeferredHolder<Item, ? extends Item> getMaterialItem(IndustrialMaterial material, MaterialPart part) { return MATERIAL_ITEMS.get(material.id()).get(part); }
    public static DeferredHolder<Item, ? extends Item> getMagneticMaterialItem(IndustrialMaterial material, MaterialPart part) { return MAGNETIC_MATERIAL_ITEMS.get(material.id()).get(part); }
    public static DeferredHolder<Item, Item> getSimpleItem(String id) { return SIMPLE_ITEMS.get(id); }
    public static DeferredHolder<Item, BlockItem> getSimpleBlockItem(String id) { return SIMPLE_BLOCK_ITEMS.get(id); }
    public static DeferredHolder<Item, BlockItem> getSimpleBlockVariantItem(String baseId, SimpleBlockVariant variant) { return SIMPLE_BLOCK_VARIANT_ITEMS.get(baseId).get(variant); }
    public static DeferredHolder<Item, BlockItem> getStructureMaterialBlockItem(String id) { return STRUCTURE_MATERIAL_BLOCK_ITEMS.get(id); }
    public static DeferredHolder<Item, StructureMaterialItem> getStructureMaterialFormItem(StructureMaterial material, MaterialPart part) {
        Map<MaterialPart, DeferredHolder<Item, StructureMaterialItem>> forms = STRUCTURE_MATERIAL_FORM_ITEMS.get(material.id());
        return forms == null ? null : forms.get(part);
    }

    public static Collection<DeferredHolder<Item, ? extends Item>> getAllMaterialItems() {
        java.util.List<DeferredHolder<Item, ? extends Item>> result = new java.util.ArrayList<>();
        MATERIAL_ITEMS.values().forEach(items -> result.addAll(items.values()));
        MAGNETIC_MATERIAL_ITEMS.values().forEach(items -> result.addAll(items.values()));
        MATERIAL_ORE_HOST_ITEMS.values().forEach(items -> result.addAll(items.values()));
        return java.util.List.copyOf(result);
    }
    public static Collection<DeferredHolder<Item, BlockItem>> getAllMaterialStoneItems() { return MATERIAL_STONE_ITEMS.values().stream().flatMap(items -> items.values().stream()).toList(); }
    public static Collection<DeferredHolder<Item, BlockItem>> getAllMaterialOreHostItems() { return MATERIAL_ORE_HOST_ITEMS.values().stream().flatMap(items -> items.values().stream()).toList(); }

    public static DeferredHolder<Item, BlockItem> getMaterialOreHostItem(IndustrialMaterial material, MaterialOreHost host, boolean small) {
        Map<String, DeferredHolder<Item, BlockItem>> items = MATERIAL_ORE_HOST_ITEMS.get(material.id());
        return items == null ? null : items.get(host.key(small));
    }
    public static Collection<DeferredHolder<Item, BlockItem>> getAllMachineCasingItems() {
        return java.util.stream.Stream.concat(
                MACHINE_CASINGS.values().stream(),
                MATERIAL_MACHINE_CASINGS.values().stream()
        ).toList();
    }
    public static Collection<DeferredHolder<Item, BlockItem>> getAllMachinePortItems() { return MACHINE_PORTS.values().stream().flatMap(ports -> ports.values().stream()).toList(); }
    public static Collection<DeferredHolder<Item, BlockItem>> getAllStaticMachinePortItems() { return STATIC_MACHINE_PORTS.values(); }
    public static Collection<DeferredHolder<Item, BlockItem>> getAllMultiblockControllerItems() { return MULTIBLOCK_CONTROLLERS.values(); }
    public static Collection<DeferredHolder<Item, BlockItem>> getAllSingleBlockMachineItems() { return SINGLE_BLOCK_MACHINES.values(); }
    public static Collection<DeferredHolder<Item, Item>> getAllSimpleItems() { return SIMPLE_ITEMS.values(); }
    public static Collection<DeferredHolder<Item, BlockItem>> getAllSimpleBlockItems() { return SIMPLE_BLOCK_ITEMS.values(); }
    public static Collection<DeferredHolder<Item, BlockItem>> getAllSimpleBlockVariantItems() { return SIMPLE_BLOCK_VARIANT_ITEMS.values().stream().flatMap(variants -> variants.values().stream()).toList(); }
    public static Collection<DeferredHolder<Item, BlockItem>> getAllEnergyWireItems() { return ENERGY_WIRES.values().stream().flatMap(wires -> wires.values().stream()).toList(); }
    public static Collection<DeferredHolder<Item, BlockItem>> getAllInsulatedEnergyWireItems() { return INSULATED_ENERGY_WIRES.values().stream().flatMap(wires -> wires.values().stream()).toList(); }
    public static Collection<DeferredHolder<Item, BlockItem>> getAllStructureMaterialBlockItems() { return STRUCTURE_MATERIAL_BLOCK_ITEMS.values(); }
    public static Collection<DeferredHolder<Item, StructureMaterialItem>> getAllStructureMaterialFormItems() { return STRUCTURE_MATERIAL_FORM_ITEMS.values().stream().flatMap(items -> items.values().stream()).toList(); }
}
