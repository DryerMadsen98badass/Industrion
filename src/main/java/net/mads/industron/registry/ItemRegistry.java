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
import net.mads.industron.item.CreativeGogglesItem;
import net.mads.industron.item.MultiblockDevToolItem;
import net.mads.industron.item.SimpleItemDefinition;
import net.mads.industron.item.SimpleItems;
import net.mads.industron.machine.MachineDefinition;
import net.mads.industron.block.coils.CoilDefinition;
import net.mads.industron.block.coils.CoilDefinitions;
import net.mads.industron.machine.MachinePortType;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.machine.SingleBlockMachineInstance;
import net.mads.industron.machine.StaticMachinePortType;
import net.mads.industron.machine.foundry.FoundryPartType;
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
import net.mads.industron.material.defenitions.PlantMaterials;
import net.mads.industron.material.plant.PlantMaterial;
import net.mads.industron.material.plant.PlantMaterialGenerator;
import net.mads.industron.material.plant.PlantMaterialItem;
import net.mads.industron.material.plant.PlantProcessIntermediate;
import net.mads.industron.material.plant.PlantProcessIntermediateItem;
import net.mads.industron.material.plant.PlantFertilizerItem;
import net.mads.industron.material.plant.PlantProcessingPlanner;
import net.mads.industron.material.plant.PlantPart;
import net.mads.industron.material.structure.StructureMaterial;
import net.mads.industron.material.structure.StructureMaterialGenerator;
import net.mads.industron.material.structure.StructureMaterialItem;
import net.mads.industron.material.structure.StructureWoodBoatItem;
import net.mads.industron.material.structure.WoodMaterial;
import net.mads.industron.material.structure.StructureMaterials;
import net.mads.industron.transport.FluidTransportRegistrations;
import net.mads.industron.recipe.recipes.assembly.ToolDefinitions;
import net.mads.industron.recipe.recipetypes.assembly.ToolDefinition;
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
    public static final DeferredHolder<Item, CreativeGogglesItem> CREATIVE_GOGGLES =
            ITEMS.register("creative_goggles", () -> new CreativeGogglesItem(new Item.Properties()));

    public static final DeferredHolder<Item, net.mads.industron.item.ClimateInstrumentItem> CLIMATE_INSTRUMENT =
            ITEMS.register("climate_instrument", () -> new net.mads.industron.item.ClimateInstrumentItem(new Item.Properties()));

    public static final DeferredHolder<Item, net.minecraft.world.item.ArmorItem> WOOL_HOOD = ITEMS.register("wool_hood",
        () -> new net.minecraft.world.item.ArmorItem(net.mads.industron.climate.ClothingRegistry.WOOL,net.minecraft.world.item.ArmorItem.Type.HELMET,
            new Item.Properties().durability(net.minecraft.world.item.ArmorItem.Type.HELMET.getDurability(5))));
    public static final DeferredHolder<Item, net.minecraft.world.item.ArmorItem> WOOL_COAT = ITEMS.register("wool_coat",
        () -> new net.minecraft.world.item.ArmorItem(net.mads.industron.climate.ClothingRegistry.WOOL,net.minecraft.world.item.ArmorItem.Type.CHESTPLATE,
            new Item.Properties().durability(net.minecraft.world.item.ArmorItem.Type.CHESTPLATE.getDurability(5))));
    public static final DeferredHolder<Item, net.minecraft.world.item.ArmorItem> WOOL_TROUSERS = ITEMS.register("wool_trousers",
        () -> new net.minecraft.world.item.ArmorItem(net.mads.industron.climate.ClothingRegistry.WOOL,net.minecraft.world.item.ArmorItem.Type.LEGGINGS,
            new Item.Properties().durability(net.minecraft.world.item.ArmorItem.Type.LEGGINGS.getDurability(5))));
    public static final DeferredHolder<Item, net.minecraft.world.item.ArmorItem> WOOL_BOOTS = ITEMS.register("wool_boots",
        () -> new net.minecraft.world.item.ArmorItem(net.mads.industron.climate.ClothingRegistry.WOOL,net.minecraft.world.item.ArmorItem.Type.BOOTS,
            new Item.Properties().durability(net.minecraft.world.item.ArmorItem.Type.BOOTS.getDurability(5))));

    public static final Map<String, DeferredHolder<Item, BlockItem>> MACHINE_CASINGS = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<Item, BlockItem>> MATERIAL_MACHINE_CASINGS = new LinkedHashMap<>();
    public static final Map<String, Map<MachinePortType, DeferredHolder<Item, BlockItem>>> MACHINE_PORTS = new LinkedHashMap<>();
    public static final Map<StaticMachinePortType, DeferredHolder<Item, BlockItem>> STATIC_MACHINE_PORTS = new LinkedHashMap<>();
    public static final Map<FoundryPartType, DeferredHolder<Item, BlockItem>> FOUNDRY_PARTS = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<Item, BlockItem>> MULTIBLOCK_CONTROLLERS = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<Item, BlockItem>> SINGLE_BLOCK_MACHINES = new LinkedHashMap<>();
    public static final Map<String, Map<String, DeferredHolder<Item, BlockItem>>> MATERIAL_STONE_ITEMS = new LinkedHashMap<>();
    public static final Map<String, Map<String, DeferredHolder<Item, BlockItem>>> MATERIAL_ORE_HOST_ITEMS = new LinkedHashMap<>();
    public static final Map<String, Map<MaterialPart, DeferredHolder<Item, ? extends Item>>> MATERIAL_ITEMS = new LinkedHashMap<>();
    public static final Map<String, Map<MaterialPart, DeferredHolder<Item, ? extends Item>>> MAGNETIC_MATERIAL_ITEMS = new LinkedHashMap<>();
    public static final Map<String, Map<WireThickness, DeferredHolder<Item, BlockItem>>> ENERGY_WIRES = new LinkedHashMap<>();
    public static final Map<String, Map<WireThickness, DeferredHolder<Item, BlockItem>>> INSULATED_ENERGY_WIRES = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<Item, BlockItem>> STRUCTURE_MATERIAL_BLOCK_ITEMS = new LinkedHashMap<>();
    public static final Map<String, Map<MaterialPart, DeferredHolder<Item, ? extends Item>>> STRUCTURE_MATERIAL_FORM_ITEMS = new LinkedHashMap<>();
    public static final Map<String, Map<PlantPart, DeferredHolder<Item, PlantMaterialItem>>> PLANT_MATERIAL_ITEMS = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<Item, PlantProcessIntermediateItem>> PLANT_PROCESS_INTERMEDIATE_ITEMS = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<Item, Item>> SIMPLE_ITEMS = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<Item, Item>> COMPOSED_TOOLS = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<Item, BlockItem>> SIMPLE_BLOCK_ITEMS = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<Item, BlockItem>> COILS = new LinkedHashMap<>();
    public static final Map<String, Map<SimpleBlockVariant, DeferredHolder<Item, BlockItem>>> SIMPLE_BLOCK_VARIANT_ITEMS = new LinkedHashMap<>();

    public static final DeferredHolder<Item, BlockItem> CREATIVE_ENERGY_PROVIDER =
            ITEMS.register("creative_energy_provider", () -> new BlockItem(BlockRegistry.CREATIVE_ENERGY_PROVIDER.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> CREATIVE_ENERGY_CONSUMER =
            ITEMS.register("creative_energy_consumer", () -> new BlockItem(BlockRegistry.CREATIVE_ENERGY_CONSUMER.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> ASSEMBLY_WORKBENCH =
            ITEMS.register("basic_assembly_workbench", () -> new BlockItem(BlockRegistry.ASSEMBLY_WORKBENCH.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> INDUSTRIAL_ASSEMBLY_WORKBENCH =
            ITEMS.register("assembly_workbench", () -> new BlockItem(BlockRegistry.INDUSTRIAL_ASSEMBLY_WORKBENCH.get(), new Item.Properties()));

    static {
        FluidTransportRegistrations.registerItems(ITEMS);
        net.mads.industron.machine.machines.kinetic.KineticMachines.registerItems(ITEMS);
        registerSimpleBlockItems();
        registerSimpleItems();
        registerComposedTools();
        registerMultiblockControllerItems();
        registerSingleBlockMachineItems();
        registerCoilItems();
        registerTieredItems();
        registerStaticMachinePortItems();
        registerFoundryPartItems();
        registerMaterialItems();
        registerMaterialMachineCasingItems();
        registerStructureMaterialItems();
        registerPlantMaterialItems();
        registerPlantProcessIntermediateItems();
    }

    public static final Map<String, DeferredHolder<Item, net.mads.industron.material.organism.BiologicalMaterialItem>> BIOLOGICAL_ITEMS = new LinkedHashMap<>();

    static {
        for (var definition : net.mads.industron.material.organism.BiologicalItemCatalog.ALL) {
            BIOLOGICAL_ITEMS.put(definition.id(), ITEMS.register(definition.id(),
                    () -> new net.mads.industron.material.organism.BiologicalMaterialItem(definition)));
        }
    }

    public static final Map<String, DeferredHolder<Item, net.mads.industron.material.organism.OrganismMaterialItem>> ORGANISM_ITEMS = new LinkedHashMap<>();
    static {
        for (var definition : net.mads.industron.material.organism.OrganismItemCatalog.generated()) {
            String id=definition.itemId().substring("industron:".length());
            ORGANISM_ITEMS.put(definition.itemId(), ITEMS.register(id,
                    () -> new net.mads.industron.material.organism.OrganismMaterialItem(definition)));
        }
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
                if (definition.id().equals("portal_activator")) {
                    return new net.mads.industron.progression.PortalActivatorItem(properties);
                }
                if (definition.id().equals("burnt_bread") || definition.id().equals("burnt_potato"))
                    properties.food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(1).saturationModifier(0).build());
                return new Item(properties);
            }));
        }
    }

    private static void registerComposedTools() {
        for (ToolDefinition definition : ToolDefinitions.ALL) {
            if (!definition.isAssembledTool() || !definition.isFinishedToolEnabled()) continue;
            COMPOSED_TOOLS.put(
                    definition.id(),
                    ITEMS.register(
                            definition.id(),
                            () -> net.mads.industron.tool.EquipmentItems.create(definition, new Item.Properties())
                    )
            );
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

    private static void registerCoilItems() {
        for (CoilDefinition definition : CoilDefinitions.ALL) {
            COILS.put(
                    definition.id(),
                    ITEMS.register(
                            definition.itemId(),
                            () -> new BlockItem(BlockRegistry.getCoil(definition.id()).get(), new Item.Properties())
                    )
            );
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

    private static void registerFoundryPartItems() {
        for (FoundryPartType type : FoundryPartType.ALL) {
            FOUNDRY_PARTS.put(
                    type,
                    ITEMS.register(type.id(), () -> new BlockItem(BlockRegistry.getFoundryPart(type).get(), new Item.Properties()))
            );
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

            Map<MaterialPart, DeferredHolder<Item, ? extends Item>> forms = new LinkedHashMap<>();
            for (MaterialPart part : StructureMaterialGenerator.generatedItemForms(material)) {
                forms.put(part, ITEMS.register(
                        part.registryName(material),
                        () -> createStructureMaterialFormItem(material, part)
                ));
            }
            STRUCTURE_MATERIAL_FORM_ITEMS.put(material.id(), forms);
        }
    }

    private static Item createStructureMaterialFormItem(StructureMaterial material, MaterialPart part) {
        if (material instanceof WoodMaterial wood) {
            if (part == MaterialPart.BOAT) {
                var entity = EntityRegistry.boat(wood);
                if (entity == null) {
                    throw new IllegalStateException("Missing generated boat entity for " + wood.id());
                }
                return new StructureWoodBoatItem(entity::get, new Item.Properties().stacksTo(1));
            }
            if (part == MaterialPart.CHEST_BOAT) {
                var entity = EntityRegistry.chestBoat(wood);
                if (entity == null) {
                    throw new IllegalStateException("Missing generated chest-boat entity for " + wood.id());
                }
                return new StructureWoodBoatItem(entity::get, new Item.Properties().stacksTo(1));
            }
        }
        return new StructureMaterialItem(material, part);
    }



    private static void registerPlantMaterialItems() {
        for (PlantMaterial material : PlantMaterials.ALL) {
            Map<PlantPart, DeferredHolder<Item, PlantMaterialItem>> forms = new LinkedHashMap<>();
            for (PlantPart part : PlantMaterialGenerator.generatedItemForms(material)) {
                forms.put(part, ITEMS.register(
                        part.registryName(material),
                        () -> new PlantMaterialItem(material, part)
                ));
            }
            PLANT_MATERIAL_ITEMS.put(material.id(), forms);
        }
    }

    private static void registerPlantProcessIntermediateItems() {
        for (PlantProcessIntermediate intermediate : PlantProcessingPlanner.allRequiredIntermediates()) {
            if (!intermediate.isSolid()) continue;
            DeferredHolder<Item, PlantProcessIntermediateItem> holder = ITEMS.register(
                    intermediate.id(),
                    () -> PlantProcessingPlanner.FERTILIZER.equals(intermediate.suffix())
                            ? new PlantFertilizerItem(intermediate)
                            : new PlantProcessIntermediateItem(intermediate)
            );
            DeferredHolder<Item, PlantProcessIntermediateItem> previous =
                    PLANT_PROCESS_INTERMEDIATE_ITEMS.putIfAbsent(intermediate.id(), holder);
            if (previous != null) {
                throw new IllegalStateException("Duplicate plant process intermediate item: " + intermediate.id());
            }
        }
    }

    private static boolean isFunctionalWirePart(MaterialPart part) {
        return WireThickness.ALL.stream().anyMatch(thickness -> thickness.materialPart() == part);
    }

    public static void register(IEventBus modEventBus) { ITEMS.register(modEventBus); }
    public static DeferredHolder<Item, ? extends Item> getMaterialItem(IndustrialMaterial material, MaterialPart part) { return MATERIAL_ITEMS.get(material.id()).get(part); }
    public static DeferredHolder<Item, ? extends Item> getMagneticMaterialItem(IndustrialMaterial material, MaterialPart part) { return MAGNETIC_MATERIAL_ITEMS.get(material.id()).get(part); }
    public static DeferredHolder<Item, Item> getSimpleItem(String id) { return SIMPLE_ITEMS.get(id); }
    public static DeferredHolder<Item, Item> getComposedTool(String id) { return COMPOSED_TOOLS.get(id); }
    public static DeferredHolder<Item, BlockItem> getSimpleBlockItem(String id) { return SIMPLE_BLOCK_ITEMS.get(id); }
    public static DeferredHolder<Item, BlockItem> getSimpleBlockVariantItem(String baseId, SimpleBlockVariant variant) { return SIMPLE_BLOCK_VARIANT_ITEMS.get(baseId).get(variant); }
    public static DeferredHolder<Item, BlockItem> getStructureMaterialBlockItem(String id) { return STRUCTURE_MATERIAL_BLOCK_ITEMS.get(id); }
    public static DeferredHolder<Item, PlantMaterialItem> getPlantMaterialItem(PlantMaterial material, PlantPart part) {
        Map<PlantPart, DeferredHolder<Item, PlantMaterialItem>> forms = PLANT_MATERIAL_ITEMS.get(material.id());
        return forms == null ? null : forms.get(part);
    }
    public static DeferredHolder<Item, PlantProcessIntermediateItem> getPlantProcessIntermediateItem(String id) {
        return PLANT_PROCESS_INTERMEDIATE_ITEMS.get(id);
    }
    public static DeferredHolder<Item, BlockItem> getCoilItem(String id) { return COILS.get(id); }
    public static DeferredHolder<Item, ? extends Item> getStructureMaterialFormItem(StructureMaterial material, MaterialPart part) {
        Map<MaterialPart, DeferredHolder<Item, ? extends Item>> forms = STRUCTURE_MATERIAL_FORM_ITEMS.get(material.id());
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
    public static Collection<DeferredHolder<Item, BlockItem>> getAllFoundryPartItems() { return FOUNDRY_PARTS.values(); }
    public static Collection<DeferredHolder<Item, BlockItem>> getAllMultiblockControllerItems() { return MULTIBLOCK_CONTROLLERS.values(); }
    public static Collection<DeferredHolder<Item, BlockItem>> getAllSingleBlockMachineItems() { return SINGLE_BLOCK_MACHINES.values(); }
    public static Collection<DeferredHolder<Item, BlockItem>> getAllCoilItems() { return COILS.values(); }
    public static Collection<DeferredHolder<Item, Item>> getAllSimpleItems() { return SIMPLE_ITEMS.values(); }
    public static Collection<DeferredHolder<Item, Item>> getAllComposedTools() { return COMPOSED_TOOLS.values(); }
    public static Collection<DeferredHolder<Item, BlockItem>> getAllSimpleBlockItems() { return SIMPLE_BLOCK_ITEMS.values(); }
    public static Collection<DeferredHolder<Item, BlockItem>> getAllSimpleBlockVariantItems() { return SIMPLE_BLOCK_VARIANT_ITEMS.values().stream().flatMap(variants -> variants.values().stream()).toList(); }
    public static Collection<DeferredHolder<Item, BlockItem>> getAllEnergyWireItems() { return ENERGY_WIRES.values().stream().flatMap(wires -> wires.values().stream()).toList(); }
    public static Collection<DeferredHolder<Item, BlockItem>> getAllInsulatedEnergyWireItems() { return INSULATED_ENERGY_WIRES.values().stream().flatMap(wires -> wires.values().stream()).toList(); }
    public static Collection<DeferredHolder<Item, BlockItem>> getAllStructureMaterialBlockItems() { return STRUCTURE_MATERIAL_BLOCK_ITEMS.values(); }
    public static Collection<DeferredHolder<Item, ? extends Item>> getAllStructureMaterialFormItems() { return STRUCTURE_MATERIAL_FORM_ITEMS.values().stream().flatMap(items -> items.values().stream()).toList(); }
    public static Collection<DeferredHolder<Item, PlantMaterialItem>> getAllPlantMaterialItems() { return PLANT_MATERIAL_ITEMS.values().stream().flatMap(items -> items.values().stream()).toList(); }
    public static Collection<DeferredHolder<Item, PlantProcessIntermediateItem>> getAllPlantProcessIntermediateItems() { return PLANT_PROCESS_INTERMEDIATE_ITEMS.values(); }
}
