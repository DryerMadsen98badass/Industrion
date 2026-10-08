package net.mads.industron.registry;

import com.simibubi.create.content.decoration.MetalLadderBlock;
import com.simibubi.create.content.decoration.MetalScaffoldingBlock;
import com.simibubi.create.content.decoration.TrainTrapdoorBlock;
import com.simibubi.create.content.decoration.bracket.BracketBlock;
import com.simibubi.create.content.decoration.palettes.ConnectedGlassPaneBlock;
import com.simibubi.create.content.decoration.palettes.WindowBlock;
import net.mads.industron.Industron;
import net.mads.industron.recipe.recipetypes.assembly.workbench.AssemblyWorkbenchBlock;
import net.mads.industron.block.DirectionalSimpleBlock;
import net.mads.industron.block.SimpleBlockDefinition;
import net.mads.industron.block.SimpleBlocks;
import net.mads.industron.block.SimpleBlockVariant;
import net.mads.industron.block.PebbleWorldgenBlock;
import net.mads.industron.block.FallenStickBlock;
import net.mads.industron.energy.CreativeEnergyBlock;
import net.mads.industron.energy.EnergyWireBlock;
import net.mads.industron.energy.WireThickness;
import net.mads.industron.machine.MachineCasingBlock;
import net.mads.industron.machine.MaterialMachineCasingBlock;
import net.mads.industron.block.coils.CoilBlock;
import net.mads.industron.block.BlockStrength;
import net.mads.industron.block.coils.CoilDefinition;
import net.mads.industron.block.coils.CoilDefinitions;
import net.mads.industron.machine.MachineDefinition;
import net.mads.industron.machine.MachinePortBlock;
import net.mads.industron.machine.MachinePortType;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.machine.SingleBlockMachineBlock;
import net.mads.industron.machine.WaterloggableSingleBlockMachineBlock;
import net.mads.industron.machine.SingleBlockMachineInstance;
import net.mads.industron.machine.StaticMachinePortType;
import net.mads.industron.machine.foundry.FoundryPartBlock;
import net.mads.industron.machine.foundry.FoundryPartType;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockControllerBlock;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockRegistrations;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.defenitions.IndustrialMaterials;
import net.mads.industron.material.defenitions.StoneMaterials;
import net.mads.industron.material.defenitions.WoodMaterials;
import net.mads.industron.material.MaterialBlock;
import net.mads.industron.material.IndustrialMaterialShaftBlock;
import net.mads.industron.material.MaterialFireboxBlock;
import net.mads.industron.material.MaterialSlabBlock;
import net.mads.industron.material.MaterialStairBlock;
import net.mads.industron.material.MaterialWallBlock;
import net.mads.industron.material.MaterialOreHost;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.recipes.MaterialCasingGenerator;
import net.mads.industron.material.structure.StructureBlockDefinition;
import net.mads.industron.material.structure.GemMaterial;
import net.mads.industron.material.structure.StructureMaterial;
import net.mads.industron.material.structure.StructureMaterialGenerator;
import net.mads.industron.material.structure.StructureSlidingDoorBlock;
import net.mads.industron.material.structure.StructureWoodChestBlock;
import net.mads.industron.material.structure.StructureWoodBarrelBlock;
import net.mads.industron.material.structure.StructureWoodChiseledBookshelfBlock;
import net.mads.industron.material.structure.StructureWoodShaftBlock;
import net.mads.industron.material.structure.MetalMaterial;
import net.mads.industron.material.structure.StructureMaterials;
import net.mads.industron.material.structure.StoneMaterial;
import net.mads.industron.material.structure.WoodMaterial;
import net.mads.industron.transport.FluidTransportRegistrations;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.CopperBulbBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BlockRegistry {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, Industron.MOD_ID);

    public static final Map<String, DeferredHolder<Block, MachineCasingBlock>> MACHINE_CASINGS = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<Block, MaterialMachineCasingBlock>> MATERIAL_MACHINE_CASINGS = new LinkedHashMap<>();
    public static final Map<String, Map<MachinePortType, DeferredHolder<Block, MachinePortBlock>>> MACHINE_PORTS = new LinkedHashMap<>();
    public static final Map<StaticMachinePortType, DeferredHolder<Block, MachinePortBlock>> STATIC_MACHINE_PORTS = new LinkedHashMap<>();
    public static final Map<FoundryPartType, DeferredHolder<Block, FoundryPartBlock>> FOUNDRY_PARTS = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<Block, MultiblockControllerBlock>> MULTIBLOCK_CONTROLLERS = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<Block, SingleBlockMachineBlock>> SINGLE_BLOCK_MACHINES = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<Block, Block>> SIMPLE_BLOCKS = new LinkedHashMap<>();
    public static final Map<String, Map<SimpleBlockVariant, DeferredHolder<Block, ? extends Block>>> SIMPLE_BLOCK_VARIANTS = new LinkedHashMap<>();
    public static final Map<String, Map<String, DeferredHolder<Block, Block>>> MATERIAL_STONE_BLOCKS = new LinkedHashMap<>();
    public static final Map<String, Map<String, DeferredHolder<Block, ? extends Block>>> MATERIAL_ORE_HOST_BLOCKS = new LinkedHashMap<>();
    public static final Map<String, Map<MaterialPart, DeferredHolder<Block, ? extends Block>>> MATERIAL_BLOCKS = new LinkedHashMap<>();
    public static final Map<String, Map<WireThickness, DeferredHolder<Block, EnergyWireBlock>>> ENERGY_WIRES = new LinkedHashMap<>();
    public static final Map<String, Map<WireThickness, DeferredHolder<Block, EnergyWireBlock>>> INSULATED_ENERGY_WIRES = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<Block, ? extends Block>> STRUCTURE_MATERIAL_BLOCKS = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<Block, CoilBlock>> COILS = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<Block, PebbleWorldgenBlock>> PEBBLE_WORLDGEN_BLOCKS = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<Block, FallenStickBlock>> FALLEN_STICK_BLOCKS = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<Block, net.mads.industron.material.plant.PlantStorageBlock>> PLANT_STORAGE_BLOCKS = new LinkedHashMap<>();

    public static final DeferredHolder<Block, CreativeEnergyBlock> CREATIVE_ENERGY_PROVIDER =
            BLOCKS.register("creative_energy_provider", () -> new CreativeEnergyBlock(true));
    public static final DeferredHolder<Block, CreativeEnergyBlock> CREATIVE_ENERGY_CONSUMER =
            BLOCKS.register("creative_energy_consumer", () -> new CreativeEnergyBlock(false));
    /** New entry-level Assembly Workbench (Workbench Level 1). */
    public static final DeferredHolder<Block, AssemblyWorkbenchBlock> ASSEMBLY_WORKBENCH =
            BLOCKS.register(
                    "basic_assembly_workbench",
                    () -> new AssemblyWorkbenchBlock(
                            BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE)
                                    .strength(2.0F)
                    )
            );
    /** Existing smithing-table-based workbench, preserved at its old registry id and promoted to Level 2. */
    public static final DeferredHolder<Block, AssemblyWorkbenchBlock> INDUSTRIAL_ASSEMBLY_WORKBENCH =
            BLOCKS.register(
                    "assembly_workbench",
                    () -> new AssemblyWorkbenchBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SMITHING_TABLE))
            );

    static {
        FluidTransportRegistrations.registerBlocks(BLOCKS);
        net.mads.industron.machine.machines.kinetic.KineticMachines.registerBlocks(BLOCKS);
        registerFoundryParts();
        registerMultiblockControllers();
        registerSingleBlockMachines();
        registerCoils();
        registerTieredBlocks();
        registerStaticMachinePorts();
        registerMaterialBlocks();
        registerMaterialMachineCasings();
        registerMaterialEnergyWires();
        registerStructureMaterialBlocks();
        registerPebbleWorldgenBlocks();
        registerFallenStickBlocks();
        for (var material : net.mads.industron.material.plant.PlantStorage.materials())
            PLANT_STORAGE_BLOCKS.put(material.id(), BLOCKS.register(material.storageBlockId(),
                () -> new net.mads.industron.material.plant.PlantStorageBlock(material)));
        registerSimpleBlocks();
    }

    private BlockRegistry() {
    }

    private static void registerMultiblockControllers() {
        MultiblockRegistrations.registerControllerBlocks(BLOCKS, MULTIBLOCK_CONTROLLERS);
    }

    private static void registerFoundryParts() {
        for (FoundryPartType type : FoundryPartType.ALL) {
            FOUNDRY_PARTS.put(type, BLOCKS.register(type.id(), () -> new FoundryPartBlock(type)));
        }
    }

    private static void registerSingleBlockMachines() {
        for (SingleBlockMachineInstance instance : MachineDefinition.INSTANCES) {
            SINGLE_BLOCK_MACHINES.put(instance.registryName(), BLOCKS.register(instance.registryName(), () ->
                    instance.definition().waterloggable()
                            ? new WaterloggableSingleBlockMachineBlock(instance)
                            : new SingleBlockMachineBlock(instance)));
        }
    }

    private static void registerCoils() {
        for (CoilDefinition definition : CoilDefinitions.ALL) {
            COILS.put(
                    definition.id(),
                    BLOCKS.register(definition.blockId(), () -> new CoilBlock(definition))
            );
        }
    }

    private static void registerTieredBlocks() {
        for (MachineTier tier : MachineTier.ALL) {
            MACHINE_CASINGS.put(tier.id(), BLOCKS.register(tier.casingRegistryName(), () -> new MachineCasingBlock(tier)));

            Map<MachinePortType, DeferredHolder<Block, MachinePortBlock>> ports = new LinkedHashMap<>();
            for (MachinePortType portType : MachinePortType.ALL) {
                ports.put(portType, BLOCKS.register(portType.registryName(tier), () -> new MachinePortBlock(tier, portType)));
            }
            MACHINE_PORTS.put(tier.id(), ports);
        }
    }

    private static void registerStaticMachinePorts() {
        for (StaticMachinePortType portType : StaticMachinePortType.ALL) {
            STATIC_MACHINE_PORTS.put(portType, BLOCKS.register(portType.id(), () -> new MachinePortBlock(portType)));
        }
    }

    private static void registerMaterialBlocks() {
        for (IndustrialMaterial material : IndustrialMaterials.ALL) {
            Map<String, DeferredHolder<Block, Block>> stoneBlocks = new LinkedHashMap<>();
            for (var stoneSource : material.stoneSources()) {
                if (!stoneSource.isExisting()) {
                    stoneBlocks.put(stoneSource.id(), BLOCKS.register(
                            stoneSource.registryName(material),
                            () -> {
                                BlockStrength strength = stoneSource.strength()
                                        .orElse(BlockStrength.of(1.5F, 6.0F));
                                return new Block(BlockBehaviour.Properties.of()
                                        .strength(strength.hardness(), strength.resistance())
                                        .requiresCorrectToolForDrops()
                                        .sound(SoundType.STONE));
                            }
                    ));
                }
            }
            MATERIAL_STONE_BLOCKS.put(material.id(), stoneBlocks);

            Map<String, DeferredHolder<Block, ? extends Block>> oreHostBlocks = new LinkedHashMap<>();
            if (MaterialOreHost.hasNaturalOre(material)) {
                for (MaterialOreHost host : MaterialOreHost.compatibleHosts(material)) {
                    for (boolean small : new boolean[]{false, true}) {
                        if (!host.shouldGenerate(material, small)) {
                            continue;
                        }
                        String key = host.key(small);
                        String registryName = host.registryName(material, small);
                        oreHostBlocks.put(
                                key,
                                BLOCKS.register(registryName, () -> new MaterialBlock(material, small ? MaterialPart.SMALL_ORE : MaterialPart.ORE))
                        );
                    }
                }
            }
            MATERIAL_ORE_HOST_BLOCKS.put(material.id(), oreHostBlocks);

            Map<MaterialPart, DeferredHolder<Block, ? extends Block>> blocks = new LinkedHashMap<>();
            for (MaterialPart part : material.parts()) {
                if (part.isOre() && MaterialOreHost.hasNaturalOre(material)) {
                    continue;
                }
                if (!material.hasExistingPart(part) && part.isBlock()) {
                    blocks.put(part, BLOCKS.register(part.registryName(material), () -> createMaterialPartBlock(material, part)));
                }
            }
            MATERIAL_BLOCKS.put(material.id(), blocks);
        }
    }

    private static Block createMaterialPartBlock(IndustrialMaterial material, MaterialPart part) {
        return switch (part) {
            case SHAFT -> new IndustrialMaterialShaftBlock(material);
            case FIREBOX -> new MaterialFireboxBlock(material);
            case BRICK_SLAB -> new MaterialSlabBlock(material, part);
            case BRICK_STAIRS -> new MaterialStairBlock(material, part);
            case BRICK_WALL -> new MaterialWallBlock(material, part);
            default -> new MaterialBlock(material, part);
        };
    }

    private static void registerMaterialMachineCasings() {
        for (MaterialCasingGenerator.GeneratedCasing generated : MaterialCasingGenerator.ALL) {
            MATERIAL_MACHINE_CASINGS.put(
                    generated.registryName(),
                    BLOCKS.register(
                            generated.registryName(),
                            () -> new MaterialMachineCasingBlock(
                                    generated.material(),
                                    generated.definition(),
                                    generated.tier()
                            )
                    )
            );
        }
    }

    private static void registerMaterialEnergyWires() {
        for (IndustrialMaterial material : IndustrialMaterials.ALL) {
            Map<WireThickness, DeferredHolder<Block, EnergyWireBlock>> wires = new LinkedHashMap<>();
            Map<WireThickness, DeferredHolder<Block, EnergyWireBlock>> insulatedWires = new LinkedHashMap<>();
            for (WireThickness thickness : WireThickness.ALL) {
                if (!material.has(thickness.materialPart())) {
                    continue;
                }
                wires.put(thickness, BLOCKS.register(
                        EnergyWireBlock.registryName(material, thickness, false),
                        () -> new EnergyWireBlock(material, thickness, false)
                ));
                insulatedWires.put(thickness, BLOCKS.register(
                        EnergyWireBlock.registryName(material, thickness, true),
                        () -> new EnergyWireBlock(material, thickness, true)
                ));
            }
            if (!wires.isEmpty()) {
                ENERGY_WIRES.put(material.id(), wires);
                INSULATED_ENERGY_WIRES.put(material.id(), insulatedWires);
            }
        }
    }

    private static void registerStructureMaterialBlocks() {
        for (StructureMaterial material : StructureMaterials.ALL) {
            for (StructureBlockDefinition definition : StructureMaterialGenerator.generatedBlockDefinitions(material)) {
                if (STRUCTURE_MATERIAL_BLOCKS.containsKey(definition.registryName())) {
                    throw new IllegalStateException("Duplicate structure material block id: " + definition.registryName());
                }
                STRUCTURE_MATERIAL_BLOCKS.put(
                        definition.registryName(),
                        BLOCKS.register(definition.registryName(), () -> createStructureMaterialBlock(definition))
                );
            }
        }
    }

    private static Block createStructureMaterialBlock(StructureBlockDefinition definition) {
        BlockBehaviour.Properties properties = structureProperties(definition);
        return switch (definition.shape()) {
            case CUBE -> definition.modelKind() == StructureBlockDefinition.ModelKind.CUTOUT_CUBE
                    ? new Block(properties.noOcclusion())
                    : new Block(properties);
            case FALLING -> new ColoredFallingBlock(
                    new ColorRGBA((definition.material().color() << 8) | 0xFF),
                    properties
            );
            case PILLAR -> new RotatedPillarBlock(properties);
            case SLAB -> new SlabBlock(properties);
            case STAIRS -> new StairBlock(resolveStructureBaseBlock(definition).defaultBlockState(), properties);
            case WALL -> new WallBlock(properties);
            case FENCE -> new FenceBlock(properties);
            case FENCE_GATE -> {
                WoodMaterial wood = requireWood(definition);
                yield new FenceGateBlock(wood.model().woodType(), properties);
            }
            case BUTTON -> {
                WoodMaterial wood = requireWood(definition);
                yield new ButtonBlock(wood.model().blockSetType(), 30, properties);
            }
            case PRESSURE_PLATE -> {
                WoodMaterial wood = requireWood(definition);
                yield new PressurePlateBlock(wood.model().blockSetType(), properties);
            }
            case DOOR -> definition.modelKind() == StructureBlockDefinition.ModelKind.CREATE_DOOR
                    ? StructureSlidingDoorBlock.fromCreateModel(
                            properties.noOcclusion(),
                            definition.modelTemplate().orElseThrow(() ->
                                    new IllegalStateException(
                                            "Missing Create door model family for " + definition.registryName()
                                    )
                            )
                    )
                    : new DoorBlock(blockSetType(definition), properties.noOcclusion());
            case TRAPDOOR -> definition.modelKind() == StructureBlockDefinition.ModelKind.CREATE_TRAIN_TRAPDOOR
                    ? TrainTrapdoorBlock.metal(properties.noOcclusion())
                    : new TrapDoorBlock(blockSetType(definition), properties.noOcclusion());
            case LEAVES -> new Block(properties.noOcclusion());
            case SAPLING -> new Block(saplingProperties(definition, properties));
            case WINDOW -> new WindowBlock(
                    properties.noOcclusion(),
                    definition.texture("end_1").isPresent()
            );
            case BOOKSHELF -> new Block(properties);
            case BARREL -> new StructureWoodBarrelBlock(requireWood(definition), properties);
            case CHISELED_BOOKSHELF -> new StructureWoodChiseledBookshelfBlock(requireWood(definition), properties);
            case CHEST -> new StructureWoodChestBlock(requireWood(definition), properties.noOcclusion());
            case BARS -> new IronBarsBlock(properties.noOcclusion());
            case WINDOW_PANE -> new ConnectedGlassPaneBlock(properties.noOcclusion());
            case BRACKET -> new BracketBlock(properties.noOcclusion());
            case BULB -> new CopperBulbBlock(properties
                    .lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 15 : 0));
            case LADDER -> definition.material() instanceof WoodMaterial
                    ? new LadderBlock(properties.noOcclusion())
                    : new MetalLadderBlock(properties.noOcclusion());
            case SCAFFOLD -> new MetalScaffoldingBlock(properties.noOcclusion());
            case SHAFT -> new StructureWoodShaftBlock(requireWood(definition), properties);
        };
    }


    private static BlockBehaviour.Properties saplingProperties(
            StructureBlockDefinition definition,
            BlockBehaviour.Properties properties
    ) {
        if (structureStrength(definition) == null) {
            properties.instabreak();
        }
        return properties.noCollission().noOcclusion();
    }

    private static BlockBehaviour.Properties structureProperties(StructureBlockDefinition definition) {
        BlockStrength explicitStrength = structureStrength(definition);

        if (definition.material() instanceof WoodMaterial) {
            BlockStrength strength = explicitStrength != null
                    ? explicitStrength
                    : BlockStrength.of(2.0F, 3.0F);
            return BlockBehaviour.Properties.of()
                    .strength(strength.hardness(), strength.resistance())
                    .sound(SoundType.WOOD);
        }
        if (definition.material() instanceof MetalMaterial) {
            BlockStrength strength = explicitStrength != null
                    ? explicitStrength
                    : BlockStrength.of(5.0F, 6.0F);
            SoundType sound = isConnectedRoof(definition) ? SoundType.COPPER : SoundType.METAL;
            return BlockBehaviour.Properties.of()
                    .strength(strength.hardness(), strength.resistance())
                    .requiresCorrectToolForDrops()
                    .sound(sound);
        }
        if (definition.shape() == StructureBlockDefinition.Shape.FALLING) {
            BlockStrength strength = explicitStrength != null
                    ? explicitStrength
                    : BlockStrength.of(0.6F, 0.6F);
            return BlockBehaviour.Properties.of()
                    .strength(strength.hardness(), strength.resistance())
                    .sound(SoundType.GRAVEL);
        }

        BlockStrength strength = explicitStrength != null
                ? explicitStrength
                : BlockStrength.of(1.5F, 6.0F);
        return BlockBehaviour.Properties.of()
                .strength(strength.hardness(), strength.resistance())
                .requiresCorrectToolForDrops()
                .sound(SoundType.STONE);
    }

    private static BlockStrength structureStrength(StructureBlockDefinition definition) {
        MaterialPart part = definition.part().orElse(null);
        if (definition.material() instanceof StoneMaterial stone) {
            return stone.strengthFor(part).orElse(null);
        }
        if (definition.material() instanceof WoodMaterial wood) {
            return wood.strengthFor(part).orElse(null);
        }
        return null;
    }

    private static boolean isConnectedRoof(StructureBlockDefinition definition) {
        if (definition.texture("connected").isEmpty()) {
            return false;
        }
        return switch (definition.shape()) {
            case CUBE, SLAB, STAIRS -> true;
            default -> false;
        };
    }

    private static BlockSetType blockSetType(StructureBlockDefinition definition) {
        if (definition.material() instanceof WoodMaterial wood) {
            return wood.model().blockSetType();
        }
        if (definition.material() instanceof MetalMaterial) {
            return BlockSetType.IRON;
        }
        throw new IllegalStateException(definition.registryName() + " requires a door/trapdoor block-set type");
    }

    private static WoodMaterial requireWood(StructureBlockDefinition definition) {
        if (definition.material() instanceof WoodMaterial wood) {
            return wood;
        }
        throw new IllegalStateException(definition.registryName() + " requires a wood material");
    }

    private static Block resolveStructureBaseBlock(StructureBlockDefinition definition) {
        if (definition.basePart().isPresent()) {
            MaterialPart part = definition.basePart().get();
            if (definition.material().hasExistingPart(part)) {
                return BuiltInRegistries.BLOCK.getOptional(definition.material().existingPart(part))
                        .orElseThrow(() -> new IllegalStateException(
                                "Missing existing block " + definition.material().existingPart(part)
                        ));
            }

            // The gem base block belongs to the normal material system, not to
            // structure_sets. Generated gem stairs therefore resolve their
            // state base from MATERIAL_BLOCKS instead of STRUCTURE_MATERIAL_BLOCKS.
            if (definition.material() instanceof GemMaterial gem
                    && part == MaterialPart.BLOCK) {
                DeferredHolder<Block, ? extends Block> holder = MATERIAL_BLOCKS
                        .getOrDefault(gem.id(), Map.of())
                        .get(MaterialPart.BLOCK);
                if (holder == null) {
                    throw new IllegalStateException(
                            "Generated gem base block not registered: " + gem.id()
                    );
                }
                return holder.get();
            }
        }

        String baseId = definition.baseRegistryName()
                .orElseThrow(() -> new IllegalStateException("No base block for " + definition.registryName()));
        DeferredHolder<Block, ? extends Block> holder = STRUCTURE_MATERIAL_BLOCKS.get(baseId);
        if (holder == null) {
            throw new IllegalStateException("Generated base block not registered: " + baseId);
        }
        return holder.get();
    }


    private static void registerPebbleWorldgenBlocks() {
        for (StoneMaterial stone : StoneMaterials.ALL) {
            if (!stone.generatedForms().contains(MaterialPart.PEBBLE)) continue;
            String id = stone.id() + "_loose_pebble";
            PEBBLE_WORLDGEN_BLOCKS.put(stone.id(), BLOCKS.register(id, () -> new PebbleWorldgenBlock(stone)));
        }
    }

    private static void registerFallenStickBlocks() {
        for (WoodMaterial wood : WoodMaterials.ALL) {
            if (!wood.generatedForms().contains(MaterialPart.STICK) && !wood.hasExistingPart(MaterialPart.STICK)) continue;
            String id = wood.id() + "_fallen_stick";
            FALLEN_STICK_BLOCKS.put(wood.id(), BLOCKS.register(id, () -> new FallenStickBlock(wood)));
        }
    }

    private static void registerSimpleBlocks() {
        for (SimpleBlockDefinition definition : SimpleBlocks.ALL) {
            DeferredHolder<Block, Block> baseBlock = BLOCKS.register(definition.id(), () -> createSimpleBlock(definition));
            SIMPLE_BLOCKS.put(definition.id(), baseBlock);
            Map<SimpleBlockVariant, DeferredHolder<Block, ? extends Block>> variants = new LinkedHashMap<>();
            for (SimpleBlockVariant variant : definition.variants()) {
                variants.put(variant, registerSimpleBlockVariant(definition, variant, baseBlock));
            }
            SIMPLE_BLOCK_VARIANTS.put(definition.id(), variants);
        }
    }

    private static Block createSimpleBlock(SimpleBlockDefinition definition) {
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
                .strength(definition.hardness(), definition.resistance())
                .requiresCorrectToolForDrops()
                .sound(SoundType.STONE);
        return definition.hasFaceTextures() ? new DirectionalSimpleBlock(properties) : new Block(properties);
    }

    private static DeferredHolder<Block, ? extends Block> registerSimpleBlockVariant(SimpleBlockDefinition definition, SimpleBlockVariant variant, DeferredHolder<Block, Block> baseBlock) {
        String registryName = definition.variantId(variant);
        return switch (variant) {
            case SLAB -> BLOCKS.register(registryName, () -> new SlabBlock(copyProperties(baseBlock.get())));
            case STAIR -> BLOCKS.register(registryName, () -> new StairBlock(baseBlock.get().defaultBlockState(), copyProperties(baseBlock.get())));
            case WALL -> BLOCKS.register(registryName, () -> new WallBlock(copyProperties(baseBlock.get())));
            case FENCE -> BLOCKS.register(registryName, () -> new FenceBlock(copyProperties(baseBlock.get())));
            case FENCE_GATE -> BLOCKS.register(registryName, () -> new FenceGateBlock(WoodType.OAK, copyProperties(baseBlock.get())));
            case BUTTON -> BLOCKS.register(registryName, () -> new ButtonBlock(BlockSetType.OAK, 30, copyProperties(baseBlock.get())));
            case PRESSURE_PLATE -> BLOCKS.register(registryName, () -> new PressurePlateBlock(BlockSetType.OAK, copyProperties(baseBlock.get())));
        };
    }

    private static BlockBehaviour.Properties copyProperties(Block block) {
        return BlockBehaviour.Properties.ofFullCopy(block);
    }

    public static void register(IEventBus modEventBus) { BLOCKS.register(modEventBus); }

    public static DeferredHolder<Block, ? extends Block> getMaterialBlock(IndustrialMaterial material, MaterialPart part) { return MATERIAL_BLOCKS.get(material.id()).get(part); }
    public static DeferredHolder<Block, MaterialMachineCasingBlock> getMaterialMachineCasing(String id) { return MATERIAL_MACHINE_CASINGS.get(id); }
    public static DeferredHolder<Block, Block> getMaterialStoneBlock(IndustrialMaterial material, String stoneId) { return MATERIAL_STONE_BLOCKS.get(material.id()).get(stoneId); }

    public static DeferredHolder<Block, ? extends Block> getMaterialOreHostBlock(IndustrialMaterial material, MaterialOreHost host, boolean small) {
        Map<String, DeferredHolder<Block, ? extends Block>> blocks = MATERIAL_ORE_HOST_BLOCKS.get(material.id());
        return blocks == null ? null : blocks.get(host.key(small));
    }
    public static DeferredHolder<Block, Block> getSimpleBlock(String id) { return SIMPLE_BLOCKS.get(id); }
    public static DeferredHolder<Block, ? extends Block> getSimpleBlockVariant(String baseId, SimpleBlockVariant variant) { return SIMPLE_BLOCK_VARIANTS.get(baseId).get(variant); }
    public static DeferredHolder<Block, SingleBlockMachineBlock> getSingleBlockMachine(String id) { return SINGLE_BLOCK_MACHINES.get(id); }
    public static DeferredHolder<Block, ? extends Block> getStructureMaterialBlock(String id) { return STRUCTURE_MATERIAL_BLOCKS.get(id); }
    public static DeferredHolder<Block, PebbleWorldgenBlock> getPebbleWorldgenBlock(StoneMaterial stone) { return PEBBLE_WORLDGEN_BLOCKS.get(stone.id()); }
    public static DeferredHolder<Block, FallenStickBlock> getFallenStickBlock(WoodMaterial wood) { return FALLEN_STICK_BLOCKS.get(wood.id()); }
    public static DeferredHolder<Block, CoilBlock> getCoil(String id) { return COILS.get(id); }
    public static DeferredHolder<Block, FoundryPartBlock> getFoundryPart(FoundryPartType type) { return FOUNDRY_PARTS.get(type); }

    public static Collection<DeferredHolder<Block, ? extends Block>> getAllStructureSlidingDoorBlocks() {
        Collection<DeferredHolder<Block, ? extends Block>> blocks = new java.util.ArrayList<>();
        for (StructureMaterial material : StructureMaterials.ALL) {
            for (StructureBlockDefinition definition : StructureMaterialGenerator.generatedBlockDefinitions(material)) {
                if (definition.modelKind() != StructureBlockDefinition.ModelKind.CREATE_DOOR) {
                    continue;
                }
                DeferredHolder<Block, ? extends Block> holder = STRUCTURE_MATERIAL_BLOCKS.get(definition.registryName());
                if (holder != null) {
                    blocks.add(holder);
                }
            }
        }
        return blocks;
    }

    public static Collection<DeferredHolder<Block, CoilBlock>> getAllCoils() { return COILS.values(); }
    public static Collection<DeferredHolder<Block, Block>> getAllSimpleBlocks() { return SIMPLE_BLOCKS.values(); }
    public static Collection<DeferredHolder<Block, ? extends Block>> getAllSimpleBlockVariants() { return SIMPLE_BLOCK_VARIANTS.values().stream().flatMap(variants -> variants.values().stream()).toList(); }
    public static Collection<DeferredHolder<Block, Block>> getAllMaterialStoneBlocks() { return MATERIAL_STONE_BLOCKS.values().stream().flatMap(blocks -> blocks.values().stream()).toList(); }
    public static Collection<DeferredHolder<Block, ? extends Block>> getAllMaterialBlocks() {
        return java.util.stream.Stream.concat(
                MATERIAL_BLOCKS.values().stream().flatMap(blocks -> blocks.values().stream()),
                MATERIAL_ORE_HOST_BLOCKS.values().stream().flatMap(blocks -> blocks.values().stream())
        ).toList();
    }
    public static Collection<DeferredHolder<Block, ? extends MachineCasingBlock>> getAllMachineCasings() {
        java.util.List<DeferredHolder<Block, ? extends MachineCasingBlock>> result = new java.util.ArrayList<>();
        result.addAll(MACHINE_CASINGS.values());
        result.addAll(MATERIAL_MACHINE_CASINGS.values());
        return java.util.List.copyOf(result);
    }
    public static Collection<DeferredHolder<Block, MachinePortBlock>> getAllMachinePorts() { return MACHINE_PORTS.values().stream().flatMap(ports -> ports.values().stream()).toList(); }
    public static Collection<DeferredHolder<Block, MachinePortBlock>> getAllStaticMachinePorts() { return STATIC_MACHINE_PORTS.values(); }
    public static Collection<DeferredHolder<Block, FoundryPartBlock>> getAllFoundryParts() { return FOUNDRY_PARTS.values(); }
    public static Collection<DeferredHolder<Block, MultiblockControllerBlock>> getAllMultiblockControllers() { return MULTIBLOCK_CONTROLLERS.values(); }
    public static Collection<DeferredHolder<Block, SingleBlockMachineBlock>> getAllSingleBlockMachines() { return SINGLE_BLOCK_MACHINES.values(); }
    public static Collection<DeferredHolder<Block, EnergyWireBlock>> getAllEnergyWires() { return ENERGY_WIRES.values().stream().flatMap(wires -> wires.values().stream()).toList(); }
    public static Collection<DeferredHolder<Block, EnergyWireBlock>> getAllInsulatedEnergyWires() { return INSULATED_ENERGY_WIRES.values().stream().flatMap(wires -> wires.values().stream()).toList(); }
    public static Collection<DeferredHolder<Block, ? extends Block>> getAllStructureMaterialBlocks() { return STRUCTURE_MATERIAL_BLOCKS.values(); }
}
