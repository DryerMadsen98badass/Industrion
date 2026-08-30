package net.mads.industron.data;

import com.simibubi.create.AllTags.AllBlockTags;
import net.mads.industron.Industron;
import net.mads.industron.block.ActiveBlockDefinition;
import net.mads.industron.block.SimpleBlocks;
import net.mads.industron.block.MiningTier;
import net.mads.industron.block.MiningTool;
import net.mads.industron.block.SimpleBlockDefinition;
import net.mads.industron.block.SimpleBlockVariant;
import net.mads.industron.machine.MachineDefinition;
import net.mads.industron.machine.MachinePortBlock;
import net.mads.industron.machine.SingleBlockMachineInstance;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.defenitions.IndustrialMaterials;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.MaterialOreHost;
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

    private static final TagKey<Block>
            C_ORE_RATES_SINGULAR =
            cTag("ore_rates/singular");

    private static final Map<MaterialPart, TagKey<Block>> COMMON_MATERIAL_BLOCK_TAGS = Map.of(
            MaterialPart.RAW_BLOCK, cTag("raw_material_blocks"),
            MaterialPart.BLOCK, cTag("storage_blocks"),
            MaterialPart.FRAME, cTag("frames"),
            MaterialPart.CASING, cTag("casings")
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
        addSimpleBlockMiningTags();
        addSimpleBlockConnectionTags();
        addMachinePortMiningTags();
        addMultiblockControllerMiningTags();
        addSingleBlockMachineMiningTags();
        addFluidTransportMiningTags();
        addColorableFluidPipeTags();
        addStructureMaterialTags();

        for (IndustrialMaterial material
                : IndustrialMaterials.ALL) {

            for (Map.Entry<MaterialPart, TagKey<Block>> entry : COMMON_MATERIAL_BLOCK_TAGS.entrySet()) {
                MaterialPart part = entry.getKey();
                if (!material.has(part)) {
                    continue;
                }

                if (material.hasExistingPart(part)) {
                    tag(entry.getValue()).addOptional(material.existingPart(part));
                    continue;
                }

                DeferredHolder<Block, ? extends Block> block = BlockRegistry.MATERIAL_BLOCKS
                        .get(material.id())
                        .get(part);
                if (block != null) {
                    tag(entry.getValue()).add(block.get());
                }
            }

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
                            Set.of(
                                    MiningTool.PICKAXE
                            ),
                            MiningTier.STONE
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

                    addOptionalStoneMiningTags(
                            existingBlockId
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
                            addMiningTags(generated.get(), Set.of(MiningTool.PICKAXE), MiningTier.STONE);
                            addOreTags(generated.get(), material, groundTag);
                        }
                    }
                }
            }
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
                    definition.miningTier()
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
                        definition.miningTier()
                );
            }
        }
    }

    private void addMachinePortMiningTags() {
        for (DeferredHolder<Block, MachinePortBlock> holder
                : BlockRegistry.getAllMachinePorts()) {
            addMiningTags(
                    holder.get(),
                    Set.of(MiningTool.PICKAXE),
                    MiningTier.IRON
            );
        }

        for (DeferredHolder<Block, MachinePortBlock> holder
                : BlockRegistry.getAllStaticMachinePorts()) {
            addMiningTags(
                    holder.get(),
                    Set.of(MiningTool.PICKAXE),
                    MiningTier.IRON
            );
        }
    }

    private void addMultiblockControllerMiningTags() {
        for (DeferredHolder<Block, net.mads.industron.machine.machines.electric.multiblock.MultiblockControllerBlock> holder
                : BlockRegistry.getAllMultiblockControllers()) {
            net.mads.industron.machine.machines.electric.multiblock.MultiblockControllerBlock block = holder.get();
            addMiningTags(
                    block,
                    block.definition().miningTools(),
                    block.definition().miningTier()
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
                    instance.definition().miningTier()
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
            for (StructureBlockDefinition definition : StructureMaterialGenerator.generatedBlockDefinitions(material)) {
                Block block = BlockRegistry.getStructureMaterialBlock(definition.registryName()).get();

                if (material instanceof WoodMaterial) {
                    tag(BlockTags.MINEABLE_WITH_AXE).add(block);
                } else {
                    addMiningTags(block, Set.of(MiningTool.PICKAXE), MiningTier.STONE);
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

    private void addFluidTransportMiningTags() {
        FluidTransportRegistrations.allBlocks().forEach(registration -> addPickaxeStoneTags(
                registration.pipe().get(),
                registration.glassPipe().get(),
                registration.pump().get(),
                registration.tank().get()
        ));
        ColoredFluidPipeRegistrations.allBlocks().forEach(registration -> addPickaxeStoneTags(
                registration.pipe().get(),
                registration.glassPipe().get(),
                registration.encasedPipe().get()
        ));
    }

    private void addPickaxeStoneTags(
            Block... blocks
    ) {
        for (Block block : blocks) {
            addMiningTags(
                    block,
                    Set.of(
                            MiningTool.PICKAXE
                    ),
                    MiningTier.STONE
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
                            MiningTool.PICKAXE
                    ),
                    MiningTier.STONE
            );
        }
    }

    private void addMiningTags(
            Block block,
            Set<MiningTool> tools,
            MiningTier tier
    ) {
        for (MiningTool tool : tools) {
            tag(tool.tag()).add(block);
        }

        addMiningTierTags(
                block,
                tier
        );
    }

    private void addMiningTierTags(
            Block block,
            MiningTier tier
    ) {
        switch (tier) {
            case WOOD -> {
                tag(BlockTags.INCORRECT_FOR_GOLD_TOOL)
                        .add(block);
            }

            case STONE -> {
                tag(BlockTags.NEEDS_STONE_TOOL)
                        .add(block);

                tag(BlockTags.INCORRECT_FOR_WOODEN_TOOL)
                        .add(block);

                tag(BlockTags.INCORRECT_FOR_GOLD_TOOL)
                        .add(block);
            }

            case IRON -> {
                tag(BlockTags.NEEDS_IRON_TOOL)
                        .add(block);

                tag(BlockTags.INCORRECT_FOR_WOODEN_TOOL)
                        .add(block);

                tag(BlockTags.INCORRECT_FOR_GOLD_TOOL)
                        .add(block);

                tag(BlockTags.INCORRECT_FOR_STONE_TOOL)
                        .add(block);
            }

            case DIAMOND -> {
                tag(BlockTags.NEEDS_DIAMOND_TOOL)
                        .add(block);

                tag(BlockTags.INCORRECT_FOR_WOODEN_TOOL)
                        .add(block);

                tag(BlockTags.INCORRECT_FOR_GOLD_TOOL)
                        .add(block);

                tag(BlockTags.INCORRECT_FOR_STONE_TOOL)
                        .add(block);

                tag(BlockTags.INCORRECT_FOR_IRON_TOOL)
                        .add(block);
            }

            case NETHERITE -> {
                tag(BlockTags.INCORRECT_FOR_WOODEN_TOOL)
                        .add(block);

                tag(BlockTags.INCORRECT_FOR_GOLD_TOOL)
                        .add(block);

                tag(BlockTags.INCORRECT_FOR_STONE_TOOL)
                        .add(block);

                tag(BlockTags.INCORRECT_FOR_IRON_TOOL)
                        .add(block);

                tag(BlockTags.INCORRECT_FOR_DIAMOND_TOOL)
                        .add(block);
            }
        }
    }

    private void addOptionalStoneMiningTags(
            ResourceLocation blockId
    ) {
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .addOptional(blockId);

        tag(BlockTags.NEEDS_STONE_TOOL)
                .addOptional(blockId);

        tag(BlockTags.INCORRECT_FOR_WOODEN_TOOL)
                .addOptional(blockId);

        tag(BlockTags.INCORRECT_FOR_GOLD_TOOL)
                .addOptional(blockId);
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
}
