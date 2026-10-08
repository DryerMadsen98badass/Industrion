package net.mads.industron.data;

import com.simibubi.create.AllTags.AllBlockTags;
import net.mads.industron.Industron;
import net.mads.industron.block.ActiveBlockDefinition;
import net.mads.industron.block.SimpleBlocks;
import net.mads.industron.recipe.recipes.assembly.Tool;
import net.mads.industron.recipe.recipetypes.assembly.ToolDefinition;
import net.mads.industron.block.SimpleBlockDefinition;
import net.mads.industron.block.SimpleBlockVariant;
import net.mads.industron.machine.MachineDefinition;
import net.mads.industron.machine.MachineCasingBlock;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.machine.MachineTierStats;
import net.mads.industron.machine.MachinePortBlock;
import net.mads.industron.machine.SingleBlockMachineInstance;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.defenitions.IndustrialMaterials;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.MaterialOreHost;
import net.mads.industron.material.recipes.MaterialCasingGenerator;
import net.mads.industron.material.structure.StructureBlockDefinition;
import net.mads.industron.material.structure.StructureMaterial;
import net.mads.industron.material.structure.StructureMaterialGenerator;
import net.mads.industron.material.structure.StructureMaterials;
import net.mads.industron.material.structure.WoodMaterial;
import net.mads.industron.registry.BlockRegistry;
import net.mads.industron.transport.FluidTransportRegistrations;
import net.mads.industron.transport.color.ColoredFluidPipeRegistrations;
import net.mads.industron.transport.color.PipeColorManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class MaterialBlockTagProvider
        extends BlockTagsProvider {

    private static final TagKey<Block> C_ORES =
            cTag("ores");
    private static final TagKey<Block> C_CASINGS = cTag("casings");
    private static final TagKey<Block> C_MACHINE_CASINGS = cTag("machine_casings");
    private static final TagKey<Block> MATERIAL_MACHINE_CASINGS = industronTag("material_machine_casings");

    private static final TagKey<Block>
            C_ORE_RATES_SINGULAR =
            cTag("ore_rates/singular");

    private static final Map<MaterialPart, TagKey<Block>> COMMON_MATERIAL_BLOCK_TAGS = Map.of(
            MaterialPart.RAW_BLOCK, cTag("raw_material_blocks"),
            MaterialPart.BLOCK, cTag("storage_blocks"),
            MaterialPart.FRAME, cTag("frames"),
            MaterialPart.CASING, cTag("casings"),
            MaterialPart.CLAY_BLOCK, cTag("clay_blocks"),
            MaterialPart.BRICKS, cTag("brick_blocks"),
            MaterialPart.FIREBOX, cTag("fireboxes")
    );

    private static final Map<MaterialPart, TagKey<Block>> ORE_GROUND_TAGS = Map.ofEntries(
            Map.entry(MaterialPart.ORE, cTag("ores_in_ground/stone")),
            Map.entry(MaterialPart.SMALL_ORE, cTag("ores_in_ground/stone")),
            Map.entry(MaterialPart.DEEPSLATE_ORE, cTag("ores_in_ground/deepslate")),
            Map.entry(MaterialPart.SMALL_DEEPSLATE_ORE, cTag("ores_in_ground/deepslate")),
            Map.entry(MaterialPart.NETHERRACK_ORE, cTag("ores_in_ground/netherrack")),
            Map.entry(MaterialPart.SMALL_NETHERRACK_ORE, cTag("ores_in_ground/netherrack")),
            Map.entry(MaterialPart.BLACKSTONE_ORE, cTag("ores_in_ground/blackstone")),
            Map.entry(MaterialPart.SMALL_BLACKSTONE_ORE, cTag("ores_in_ground/blackstone")),
            Map.entry(MaterialPart.BASALT_ORE, cTag("ores_in_ground/basalt")),
            Map.entry(MaterialPart.SMALL_BASALT_ORE, cTag("ores_in_ground/basalt")),
            Map.entry(MaterialPart.END_STONE_ORE, cTag("ores_in_ground/end_stone")),
            Map.entry(MaterialPart.SMALL_END_STONE_ORE, cTag("ores_in_ground/end_stone"))
    );

    public MaterialBlockTagProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider>
                    lookupProvider,
            ExistingFileHelper existingFileHelper
    ) {
        super(
                output,
                lookupProvider,
                Industron.MOD_ID,
                existingFileHelper
        );
    }

    @Override
    protected void addTags(
            HolderLookup.Provider provider
    ) {
        for (var holder : net.mads.industron.machine.machines.kinetic.KineticMachines.blocks()) {
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(holder.get());
            addMiningTierTags(holder.get(), MachineTier.LV);
        }
        addSimpleBlockMiningTags();
        addSimpleBlockConnectionTags();
        addMachineCasingMiningTags();
        addMachinePortMiningTags();
        addFoundryPartMiningTags();
        addMultiblockControllerMiningTags();
        addSingleBlockMachineMiningTags();
        addCoilMiningTags();
        addEnergyWireMiningTags();
        addMaterialStoneBlockMiningTags();
        addFluidTransportMiningTags();
        addColorableFluidPipeTags();
        addStructureMaterialTags();
        addMaterialMachineCasingTags();

        for (IndustrialMaterial material
                : IndustrialMaterials.ALL) {

            for (Map.Entry<MaterialPart, TagKey<Block>> entry : COMMON_MATERIAL_BLOCK_TAGS.entrySet()) {
                MaterialPart part = entry.getKey();
                if (!material.has(part)) {
                    continue;
                }

                Set<ToolDefinition> tools = part == MaterialPart.CLAY_BLOCK
                        ? Set.of(Tool.SHOVEL)
                        : Set.of(Tool.PICKAXE);

                if (material.hasExistingPart(part)) {
                    ResourceLocation existing = material.existingPart(part);
                    tag(entry.getValue()).addOptional(existing);
                    addOptionalMiningTags(existing, tools, material.tier());
                    continue;
                }

                DeferredHolder<Block, ? extends Block> block = BlockRegistry.MATERIAL_BLOCKS
                        .get(material.id())
                        .get(part);
                if (block != null) {
                    tag(entry.getValue()).add(block.get());
                    addMiningTags(block.get(), tools, material.tier());
                }
            }

            addClayShapeTags(material);
            addMaterialShaftMiningTags(material);

            for (Map.Entry<
                    MaterialPart,
                    TagKey<Block>
                    > oreGroundTag
                    : ORE_GROUND_TAGS.entrySet()) {

                MaterialPart part =
                        oreGroundTag.getKey();

                if (!material.has(part)) {
                    continue;
                }

                DeferredHolder<
                        Block,
                        ? extends Block
                        > block =
                        BlockRegistry.MATERIAL_BLOCKS
                                .get(material.id())
                                .get(part);

                if (block != null) {
                    addMiningTags(
                            block.get(),
                            Set.of(Tool.PICKAXE),
                            material.tier()
                    );

                    addOreTags(
                            block.get(),
                            material,
                            oreGroundTag.getValue()
                    );
                }

                if (material.hasExistingPart(part)) {
                    ResourceLocation existingBlockId =
                            material.existingPart(part);

                    addOptionalOreMiningTags(
                            existingBlockId,
                            material.tier()
                    );

                    tag(C_ORES)
                            .addOptional(existingBlockId);

                    tag(C_ORE_RATES_SINGULAR)
                            .addOptional(existingBlockId);

                    tag(
                            cTag(
                                    "ores/"
                                            + material.id()
                            )
                    ).addOptional(existingBlockId);

                    tag(oreGroundTag.getValue())
                            .addOptional(existingBlockId);
                }
            }

            if (MaterialOreHost.hasNaturalOre(material)) {
                for (MaterialOreHost host : MaterialOreHost.compatibleHosts(material)) {
                    TagKey<Block> groundTag = cTag("ores_in_ground/" + host.id());
                    for (boolean small : new boolean[]{false, true}) {
                        var generated = BlockRegistry.getMaterialOreHostBlock(material, host, small);
                        if (generated != null) {
                            addMiningTags(generated.get(), Set.of(Tool.PICKAXE), material.tier());
                            addOreTags(generated.get(), material, groundTag);
                        }
                    }
                }
            }
        }
    }


    private void addMaterialShaftMiningTags(IndustrialMaterial material) {
        if (!material.has(MaterialPart.SHAFT)) {
            return;
        }
        if (material.hasExistingPart(MaterialPart.SHAFT)) {
            addOptionalMiningTags(material.existingPart(MaterialPart.SHAFT), Set.of(Tool.PICKAXE), material.tier());
            return;
        }
        DeferredHolder<Block, ? extends Block> shaft = BlockRegistry.MATERIAL_BLOCKS
                .getOrDefault(material.id(), Map.of())
                .get(MaterialPart.SHAFT);
        if (shaft != null) {
            addMiningTags(shaft.get(), Set.of(Tool.PICKAXE), material.tier());
        }
    }

    private void addClayShapeTags(IndustrialMaterial material) {
        if (!material.isClayMaterial()) return;
        addClayShapeTag(material, MaterialPart.BRICK_SLAB, BlockTags.SLABS);
        addClayShapeTag(material, MaterialPart.BRICK_STAIRS, BlockTags.STAIRS);
        addClayShapeTag(material, MaterialPart.BRICK_WALL, BlockTags.WALLS);
    }

    private void addClayShapeTag(IndustrialMaterial material, MaterialPart part, TagKey<Block> shapeTag) {
        if (!material.has(part)) return;
        if (material.hasExistingPart(part)) {
            ResourceLocation existing = material.existingPart(part);
            tag(shapeTag).addOptional(existing);
            addOptionalMiningTags(existing, Set.of(Tool.PICKAXE), material.tier());
            return;
        }
        DeferredHolder<Block, ? extends Block> holder = BlockRegistry.MATERIAL_BLOCKS
                .getOrDefault(material.id(), Map.of())
                .get(part);
        if (holder != null) {
            tag(shapeTag).add(holder.get());
            addMiningTags(holder.get(), Set.of(Tool.PICKAXE), material.tier());
        }
    }

    private void addMaterialMachineCasingTags() {
        for (MaterialCasingGenerator.GeneratedCasing generated : MaterialCasingGenerator.ALL) {
            var holder = BlockRegistry.getMaterialMachineCasing(generated.registryName());
            if (holder == null) {
                throw new IllegalStateException("Missing registered material casing block: " + generated.registryName());
            }
            Block block = holder.get();
            tag(C_CASINGS).add(block);
            tag(C_MACHINE_CASINGS).add(block);
            tag(MATERIAL_MACHINE_CASINGS).add(block);
            tag(industronTag("material_machine_casings/" + generated.definition().id())).add(block);
            tag(industronTag("material_machine_casings/material/" + generated.material().id())).add(block);
            addMiningTags(block, Set.of(Tool.PICKAXE), generated.tier());
        }
    }

    private void addSimpleBlockConnectionTags() {
        for (SimpleBlockDefinition definition
                : SimpleBlocks.ALL) {

            for (SimpleBlockVariant variant
                    : definition.variants()) {

                Block block = BlockRegistry
                        .getSimpleBlockVariant(
                                definition.id(),
                                variant
                        )
                        .get();

                switch (variant) {
                    case WALL -> tag(BlockTags.WALLS)
                            .add(block);

                    case FENCE -> tag(BlockTags.FENCES)
                            .add(block);

                    case FENCE_GATE -> tag(BlockTags.FENCE_GATES)
                            .add(block);

                    default -> {
                    }
                }
            }
        }
    }


    private void addSimpleBlockMiningTags() {
        for (SimpleBlockDefinition definition
                : SimpleBlocks.ALL) {

            addMiningTags(
                    BlockRegistry
                            .getSimpleBlock(
                                    definition.id()
                            )
                            .get(),
                    definition.miningTools(),
                    definition.breakingTier()
            );

            for (SimpleBlockVariant variant
                    : definition.variants()) {

                addMiningTags(
                        BlockRegistry
                                .getSimpleBlockVariant(
                                        definition.id(),
                                        variant
                                )
                                .get(),
                        definition.miningTools(),
                        definition.breakingTier()
                );
            }
        }
    }

    private void addMachineCasingMiningTags() {
        for (DeferredHolder<Block, ? extends MachineCasingBlock> holder : BlockRegistry.getAllMachineCasings()) {
            MachineCasingBlock block = holder.get();
            addMiningTags(block, Set.of(Tool.PICKAXE), block.tier().recipeTier());
        }
    }

    private void addMachinePortMiningTags() {
        for (DeferredHolder<Block, MachinePortBlock> holder
                : BlockRegistry.getAllMachinePorts()) {
            MachinePortBlock block = holder.get();
            addMiningTags(block, Set.of(Tool.PICKAXE), block.effectiveTier().recipeTier());
        }

        for (DeferredHolder<Block, MachinePortBlock> holder
                : BlockRegistry.getAllStaticMachinePorts()) {
            MachinePortBlock block = holder.get();
            addMiningTags(block, Set.of(Tool.PICKAXE), block.effectiveTier().recipeTier());
        }
    }

    private void addFoundryPartMiningTags() {
        for (DeferredHolder<Block, net.mads.industron.machine.foundry.FoundryPartBlock> holder
                : BlockRegistry.getAllFoundryParts()) {
            addMiningTags(holder.get(), Set.of(Tool.PICKAXE), MachineTier.ULV);
        }
    }

    private void addMultiblockControllerMiningTags() {
        for (DeferredHolder<Block, net.mads.industron.machine.machines.electric.multiblock.MultiblockControllerBlock> holder
                : BlockRegistry.getAllMultiblockControllers()) {
            net.mads.industron.machine.machines.electric.multiblock.MultiblockControllerBlock block = holder.get();
            addMiningTags(
                    block,
                    block.definition().miningTools(),
                    block.definition().breakingTier()
            );
        }
    }

    private void addSingleBlockMachineMiningTags() {
        for (SingleBlockMachineInstance instance
                : MachineDefinition.INSTANCES) {

            addMiningTags(
                    BlockRegistry
                            .getSingleBlockMachine(
                                    instance.registryName()
                            )
                            .get(),
                    instance.definition().miningTools(),
                    instance.tier() == MachineTier.NONE
                            ? instance.definition().breakingTier()
                            : instance.tier().recipeTier()
            );
        }
    }


    private void addColorableFluidPipeTags() {
        tag(PipeColorManager.COLORABLE_FLUID_PIPES)
                .addOptional(ResourceLocation.fromNamespaceAndPath("create", "fluid_pipe"))
                .addOptional(ResourceLocation.fromNamespaceAndPath("create", "glass_fluid_pipe"));

        FluidTransportRegistrations.allBlocks().forEach(registration ->
                tag(PipeColorManager.COLORABLE_FLUID_PIPES).add(
                        registration.pipe().get(),
                        registration.glassPipe().get()
                )
        );
        ColoredFluidPipeRegistrations.allBlocks().forEach(registration ->
                tag(PipeColorManager.COLORABLE_FLUID_PIPES).add(
                        registration.pipe().get(),
                        registration.glassPipe().get(),
                        registration.encasedPipe().get()
                )
        );
    }

    private void addStructureMaterialTags() {
        for (StructureMaterial material : StructureMaterials.ALL) {
            // Existing vanilla/Create blocks that represent this material inherit the same
            // Industron tier as generated variants.  The source material owns the tier.
            for (Map.Entry<MaterialPart, ResourceLocation> existing : material.existingParts().entrySet()) {
                MaterialPart part = existing.getKey();
                if (!part.isBlock()) continue;

                Set<ToolDefinition> tools;
                if (material instanceof WoodMaterial) {
                    tools = Set.of(Tool.AXE);
                } else if (part == MaterialPart.GRAVEL) {
                    tools = Set.of(Tool.SHOVEL);
                } else {
                    tools = Set.of(Tool.PICKAXE);
                }
                addOptionalMiningTags(existing.getValue(), tools, material.tier());
            }

            for (StructureBlockDefinition definition : StructureMaterialGenerator.generatedBlockDefinitions(material)) {
                Block block = BlockRegistry.getStructureMaterialBlock(definition.registryName()).get();

                if (material instanceof WoodMaterial) {
                    addMiningTags(block, Set.of(Tool.AXE), material.tier());
                } else if (definition.shape() == StructureBlockDefinition.Shape.FALLING) {
                    addMiningTags(block, Set.of(Tool.SHOVEL), material.tier());
                } else {
                    addMiningTags(block, Set.of(Tool.PICKAXE), material.tier());
                }

                switch (definition.shape()) {
                    case WALL -> tag(BlockTags.WALLS).add(block);
                    case FENCE -> tag(BlockTags.FENCES).add(block);
                    case FENCE_GATE -> tag(BlockTags.FENCE_GATES).add(block);
                    case DOOR -> {
                        tag(BlockTags.DOORS).add(block);
                        if (material instanceof WoodMaterial
                                || definition.modelKind() == StructureBlockDefinition.ModelKind.CREATE_DOOR) {
                            tag(BlockTags.WOODEN_DOORS).add(block);
                        }
                        if (definition.modelKind() == StructureBlockDefinition.ModelKind.CREATE_DOOR) {
                            tag(AllBlockTags.NON_DOUBLE_DOOR.tag).add(block);
                        }
                    }
                    case TRAPDOOR -> {
                        tag(BlockTags.TRAPDOORS).add(block);
                        if (material instanceof WoodMaterial) {
                            tag(BlockTags.WOODEN_TRAPDOORS).add(block);
                        }
                    }
                    case LADDER, SCAFFOLD -> tag(BlockTags.CLIMBABLE).add(block);
                    default -> {
                    }
                }
            }
        }
    }

    private void addCoilMiningTags() {
        for (DeferredHolder<Block, net.mads.industron.block.coils.CoilBlock> coil : BlockRegistry.getAllCoils()) {
            var block = coil.get();
            addMiningTags(block, Set.of(Tool.PICKAXE), block.definition().tier());
        }
    }

    private void addEnergyWireMiningTags() {
        java.util.stream.Stream.concat(
                BlockRegistry.getAllEnergyWires().stream(),
                BlockRegistry.getAllInsulatedEnergyWires().stream()
        ).forEach(holder -> {
            var block = holder.get();
            addMiningTags(block, Set.of(Tool.PICKAXE), block.tier());
        });
    }

    private void addMaterialStoneBlockMiningTags() {
        for (IndustrialMaterial material : IndustrialMaterials.ALL) {
            Map<String, DeferredHolder<Block, Block>> blocks = BlockRegistry.MATERIAL_STONE_BLOCKS.get(material.id());
            if (blocks == null) continue;
            blocks.values().forEach(holder ->
                    addMiningTags(holder.get(), Set.of(Tool.PICKAXE), material.tier())
            );
        }
    }

    private void addFluidTransportMiningTags() {
        FluidTransportRegistrations.allBlocks().forEach(registration -> addPickaxeTierTags(
                registration.tier().material().tier(),
                registration.pipe().get(),
                registration.glassPipe().get(),
                registration.pump().get(),
                registration.tank().get()
        ));
        ColoredFluidPipeRegistrations.allBlocks().forEach(registration -> {
            var transportTier = registration.family().tier();
            MachineTier breakingTier = transportTier == null
                    ? MachineTier.LV
                    : transportTier.material().tier();

            addPickaxeTierTags(
                    breakingTier,
                    registration.pipe().get(),
                    registration.glassPipe().get(),
                    registration.encasedPipe().get()
            );
        });
    }

    private void addPickaxeTierTags(MachineTier tier, Block... blocks) {
        for (Block block : blocks) {
            addMiningTags(block, Set.of(Tool.PICKAXE), tier);
        }
    }

    private void addPickaxeStoneTags(
            Block... blocks
    ) {
        for (Block block : blocks) {
            addMiningTags(
                    block,
                    Set.of(
                            Tool.PICKAXE
                    ),
                    MachineTier.LV
            );
        }
    }

    private void addPickaxeStoneTags(
            Collection<? extends Block> blocks
    ) {
        for (Block block : blocks) {
            addMiningTags(
                    block,
                    Set.of(
                            Tool.PICKAXE
                    ),
                    MachineTier.LV
            );
        }
    }

    private void addMiningToolTags(
            Block block,
            Set<ToolDefinition> tools
    ) {
        for (ToolDefinition tool : tools) {
            TagKey<Block> tagKey = vanillaMiningTag(tool);
            if (tagKey != null) tag(tagKey).add(block);
        }
    }


    private void addMiningTags(
            Block block,
            Set<ToolDefinition> tools,
            MachineTier tier
    ) {
        addMiningToolTags(block, tools);
        addMiningTierTags(block, tier);
    }

    private void addOptionalMiningTags(
            ResourceLocation blockId,
            Set<ToolDefinition> tools,
            MachineTier tier
    ) {
        for (ToolDefinition tool : tools) {
            TagKey<Block> tagKey = vanillaMiningTag(tool);
            if (tagKey != null) tag(tagKey).addOptional(blockId);
        }
        addOptionalMiningTierTags(blockId, tier);
    }


    private static TagKey<Block> vanillaMiningTag(ToolDefinition tool) {
        if (tool == Tool.PICKAXE) return BlockTags.MINEABLE_WITH_PICKAXE;
        if (tool == Tool.AXE) return BlockTags.MINEABLE_WITH_AXE;
        if (tool == Tool.SHOVEL) return BlockTags.MINEABLE_WITH_SHOVEL;
        if (tool == Tool.HOE) return BlockTags.MINEABLE_WITH_HOE;
        return null;
    }

    private void addMiningTierTags(
            Block block,
            MachineTier tier
    ) {
        applyMiningTierTags(tagKey -> tag(tagKey).add(block), tier);
    }

    private void addOptionalMiningTierTags(ResourceLocation blockId, MachineTier tier) {
        applyMiningTierTags(tagKey -> tag(tagKey).addOptional(blockId), tier);
    }

    private void applyMiningTierTags(
            java.util.function.Consumer<TagKey<Block>> add,
            MachineTier tier
    ) {
        MachineTier normalized = tier == null || tier == MachineTier.NONE
                ? MachineTier.ULV
                : tier.recipeTier();
        int index = MachineTierStats.tierIndex(normalized);

        // Gold is not part of Industron's progression and is never a valid tier tool.
        add.accept(BlockTags.INCORRECT_FOR_GOLD_TOOL);

        int lv = MachineTierStats.tierIndex(MachineTier.LV);
        int mv = MachineTierStats.tierIndex(MachineTier.MV);
        int hv = MachineTierStats.tierIndex(MachineTier.HV);
        int ev = MachineTierStats.tierIndex(MachineTier.EV);
        int iv = MachineTierStats.tierIndex(MachineTier.IV);

        if (index == lv) {
            add.accept(BlockTags.NEEDS_STONE_TOOL);
        } else if (index == mv) {
            add.accept(BlockTags.NEEDS_IRON_TOOL);
        } else if (index >= hv && index < ev) {
            add.accept(BlockTags.NEEDS_DIAMOND_TOOL);
        } else if (index == ev) {
            add.accept(Tags.Blocks.NEEDS_NETHERITE_TOOL);
        }

        if (index >= lv) add.accept(BlockTags.INCORRECT_FOR_WOODEN_TOOL);
        if (index >= mv) add.accept(BlockTags.INCORRECT_FOR_STONE_TOOL);
        if (index >= hv) add.accept(BlockTags.INCORRECT_FOR_IRON_TOOL);
        if (index >= ev) add.accept(BlockTags.INCORRECT_FOR_DIAMOND_TOOL);
        if (index >= iv) add.accept(BlockTags.INCORRECT_FOR_NETHERITE_TOOL);
    }

    /** Existing ore aliases receive the same mining tier as their IndustrialMaterial source. */
    private void addOptionalOreMiningTags(
            ResourceLocation blockId,
            MachineTier tier
    ) {
        addOptionalMiningTags(blockId, Set.of(Tool.PICKAXE), tier);
    }

    private void addOreTags(
            Block block,
            IndustrialMaterial material,
            TagKey<Block> groundTag
    ) {
        tag(C_ORES)
                .add(block);

        tag(C_ORE_RATES_SINGULAR)
                .add(block);

        tag(
                cTag(
                        "ores/"
                                + material.id()
                )
        ).add(block);

        tag(groundTag)
                .add(block);
    }

    private static TagKey<Block> cTag(
            String path
    ) {
        return TagKey.create(
                Registries.BLOCK,
                ResourceLocation.fromNamespaceAndPath(
                        "c",
                        path
                )
        );
    }

    private static TagKey<Block> industronTag(String path) {
        return TagKey.create(
                Registries.BLOCK,
                ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, path)
        );
    }
}
