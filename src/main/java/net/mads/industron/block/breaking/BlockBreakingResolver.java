package net.mads.industron.block.breaking;

import net.mads.industron.Industron;
import net.mads.industron.block.SimpleBlockDefinition;
import net.mads.industron.block.SimpleBlockVariant;
import net.mads.industron.block.SimpleBlocks;
import net.mads.industron.block.coils.CoilBlock;
import net.mads.industron.energy.EnergyWireBlock;
import net.mads.industron.machine.FireboxBlock;
import net.mads.industron.machine.foundry.CastingBlock;
import net.mads.industron.machine.foundry.FoundryPartBlock;
import net.mads.industron.machine.MachineCasingBlock;
import net.mads.industron.machine.MachinePortBlock;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.machine.SingleBlockMachineBlock;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockControllerBlock;
import net.mads.industron.material.MaterialPartBlock;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.defenitions.IndustrialMaterials;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.structure.StructureBlockDefinition;
import net.mads.industron.material.structure.StructureMaterial;
import net.mads.industron.material.structure.StructureMaterialGenerator;
import net.mads.industron.material.structure.StructureMaterials;
import net.mads.industron.material.structure.WoodMaterial;
import net.mads.industron.registry.BlockRegistry;
import net.mads.industron.recipe.recipes.assembly.Tool;
import net.mads.industron.recipe.recipetypes.assembly.ToolDefinition;
import net.mads.industron.transport.TieredFluidPipe;
import net.mads.industron.transport.TieredFluidPump;
import net.mads.industron.transport.TieredFluidTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;

import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Resolves every block in the game into the unified Industron breaking model. */
public final class BlockBreakingResolver {
    private static volatile Map<Block, ExplicitSource> explicitSources;

    private BlockBreakingResolver() {
    }

    public static BlockBreakingProfile resolve(BlockState state, BlockGetter level, BlockPos pos) {
        float work = state.getDestroySpeed(level, pos);
        if (work < 0.0F) {
            return new BlockBreakingProfile(Set.of(), MachineTier.ULV, work, false);
        }

        ExplicitSource source = explicitSource(state.getBlock());
        if (source != null) {
            Set<String> tools = source.toolTypeIds().isEmpty()
                    ? vanillaToolFamilies(state)
                    : source.toolTypeIds();
            return new BlockBreakingProfile(
                    tools,
                    normalizeTier(source.tier()),
                    work,
                    true
            );
        }

        Set<String> tools = vanillaToolFamilies(state);
        MachineTier tier = vanillaTier(state);
        boolean industronBlock = Industron.MOD_ID.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace());
        return new BlockBreakingProfile(
                tools,
                tier,
                work,
                industronBlock
        );
    }

    /**
     * Explicit class-based sources are checked before the lazily built registry map.
     * These cover all blocks that carry their own material/machine definition directly.
     */
    private static ExplicitSource explicitSource(Block block) {
        if (block instanceof MaterialPartBlock materialBlock) {
            return new ExplicitSource(
                    materialBlock.material().tier(),
                    materialTools(materialBlock.part())
            );
        }
        if (block instanceof MachineCasingBlock casing) {
            return pickaxe(casing.tier());
        }
        if (net.mads.industron.machine.machines.kinetic.KineticMachines.isMachine(block)) {
            return pickaxe(MachineTier.LV);
        }
        if (block instanceof SingleBlockMachineBlock machine && machine.instance() != null) {
            MachineTier tier = machine.instance().tier() == MachineTier.NONE
                    ? machine.instance().definition().breakingTier()
                    : machine.instance().tier().recipeTier();
            return new ExplicitSource(
                    tier,
                    toolIds(machine.instance().definition().miningTools())
            );
        }
        if (block instanceof MachinePortBlock port) {
            return pickaxe(port.effectiveTier().recipeTier());
        }
        if (block instanceof MultiblockControllerBlock controller) {
            return new ExplicitSource(
                    controller.definition().breakingTier(),
                    toolIds(controller.definition().miningTools())
            );
        }
        if (block instanceof CoilBlock coil) {
            return pickaxe(coil.definition().tier());
        }
        if (block instanceof EnergyWireBlock wire) {
            return pickaxe(wire.tier());
        }
        if (block instanceof FireboxBlock firebox && firebox.definition() != null) {
            return new ExplicitSource(
                    firebox.definition().breakingTier(),
                    toolIds(firebox.definition().miningTools())
            );
        }
        if (block == BlockRegistry.ASSEMBLY_WORKBENCH.get()) {
            return new ExplicitSource(
                    MachineTier.ULV,
                    Set.of(Tool.AXE.id())
            );
        }
        if (block instanceof FoundryPartBlock || block instanceof CastingBlock) {
            return pickaxe(MachineTier.ULV);
        }
        if (block instanceof TieredFluidPipe pipe) {
            return pickaxe(pipe.transportTier().material().tier());
        }
        if (block instanceof TieredFluidPump pump) {
            return pickaxe(pump.transportTier().material().tier());
        }
        if (block instanceof TieredFluidTank tank) {
            return pickaxe(tank.transportTier().material().tier());
        }

        return explicitSources().get(block);
    }

    private static Map<Block, ExplicitSource> explicitSources() {
        Map<Block, ExplicitSource> local = explicitSources;
        if (local != null) {
            return local;
        }
        synchronized (BlockBreakingResolver.class) {
            local = explicitSources;
            if (local == null) {
                local = buildExplicitSources();
                explicitSources = local;
            }
        }
        return local;
    }

    private static Map<Block, ExplicitSource> buildExplicitSources() {
        Map<Block, ExplicitSource> sources = new IdentityHashMap<>();

        // Simple blocks and all their generated variants inherit one definition.
        for (SimpleBlockDefinition definition : SimpleBlocks.ALL) {
            put(sources, BlockRegistry.getSimpleBlock(definition.id()).get(), new ExplicitSource(
                    definition.breakingTier(),
                    toolIds(definition.miningTools())
            ));
            for (SimpleBlockVariant variant : definition.variants()) {
                put(sources, BlockRegistry.getSimpleBlockVariant(definition.id(), variant).get(), new ExplicitSource(
                        definition.breakingTier(),
                        toolIds(definition.miningTools())
                ));
            }
        }

        // Generated and existing structure-family blocks inherit the source stone/wood/metal/gem tier.
        for (StructureMaterial material : StructureMaterials.ALL) {
            for (StructureBlockDefinition definition : StructureMaterialGenerator.generatedBlockDefinitions(material)) {
                put(sources, BlockRegistry.getStructureMaterialBlock(definition.registryName()).get(), structureSource(material, definition));
            }
            for (Map.Entry<MaterialPart, net.minecraft.resources.ResourceLocation> entry : material.existingParts().entrySet()) {
                Block existing = BuiltInRegistries.BLOCK.getOptional(entry.getValue()).orElse(null);
                if (existing != null) {
                    putIfAbsent(sources, existing, structureSource(material, entry.getKey()));
                }
            }
        }

        // Existing material block/ore aliases must inherit the material tier too.
        for (IndustrialMaterial material : IndustrialMaterials.ALL) {
            for (Map.Entry<MaterialPart, net.minecraft.resources.ResourceLocation> entry : material.existingParts().entrySet()) {
                if (!entry.getKey().isBlock()) {
                    continue;
                }
                Block existing = BuiltInRegistries.BLOCK.getOptional(entry.getValue()).orElse(null);
                if (existing != null) {
                    putIfAbsent(sources, existing, new ExplicitSource(
                            material.tier(),
                            materialTools(entry.getKey())
                    ));
                }
            }

            Map<String, net.neoforged.neoforge.registries.DeferredHolder<Block, Block>> stoneBlocks =
                    BlockRegistry.MATERIAL_STONE_BLOCKS.get(material.id());
            if (stoneBlocks != null) {
                for (var holder : stoneBlocks.values()) {
                    put(sources, holder.get(), pickaxe(material.tier()));
                }
            }
        }

        return Map.copyOf(sources);
    }

    private static ExplicitSource structureSource(StructureMaterial material, StructureBlockDefinition definition) {
        return structureSource(material, definition.part().orElse(null), definition.shape());
    }

    private static ExplicitSource structureSource(StructureMaterial material, MaterialPart part) {
        return structureSource(material, part, null);
    }

    private static ExplicitSource structureSource(
            StructureMaterial material,
            MaterialPart part,
            StructureBlockDefinition.Shape shape
    ) {
        Set<String> tools;
        boolean naturalSoftForm = shape == StructureBlockDefinition.Shape.LEAVES
                || shape == StructureBlockDefinition.Shape.SAPLING
                || part == MaterialPart.LEAVES
                || part == MaterialPart.SAPLING;

        if (naturalSoftForm) {
            // Leaves/saplings remain hand-breakable like Minecraft, but still inherit material tier
            // when a registered tool is actually used.
            tools = Set.of();
        } else if (material instanceof WoodMaterial) {
            tools = Set.of(Tool.AXE.id());
        } else if (shape == StructureBlockDefinition.Shape.FALLING || part == MaterialPart.GRAVEL) {
            tools = Set.of(Tool.SHOVEL.id());
        } else {
            tools = Set.of(Tool.PICKAXE.id());
        }
        return new ExplicitSource(material.tier(), tools);
    }

    private static Set<String> materialTools(MaterialPart part) {
        if (part == MaterialPart.CLAY_BLOCK) {
            return Set.of(Tool.SHOVEL.id());
        }
        return Set.of(Tool.PICKAXE.id());
    }

    private static Set<String> vanillaToolFamilies(BlockState state) {
        Set<String> result = new LinkedHashSet<>();
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) result.add(Tool.PICKAXE.id());
        if (state.is(BlockTags.MINEABLE_WITH_AXE)) result.add(Tool.AXE.id());
        if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) result.add(Tool.SHOVEL.id());
        if (state.is(BlockTags.MINEABLE_WITH_HOE)) result.add(Tool.HOE.id());
        return Set.copyOf(result);
    }

    /** Minecraft mining tags mapped onto the unified ULV/LV/MV/HV/EV scale. */
    private static MachineTier vanillaTier(BlockState state) {
        if (state.is(BlockTags.INCORRECT_FOR_NETHERITE_TOOL)) {
            return MachineTier.IV;
        }
        if (state.is(Tags.Blocks.NEEDS_NETHERITE_TOOL) || state.is(BlockTags.INCORRECT_FOR_DIAMOND_TOOL)) {
            return MachineTier.EV;
        }
        if (state.is(BlockTags.NEEDS_DIAMOND_TOOL) || state.is(BlockTags.INCORRECT_FOR_IRON_TOOL)) {
            return MachineTier.HV;
        }
        if (state.is(BlockTags.NEEDS_IRON_TOOL) || state.is(BlockTags.INCORRECT_FOR_STONE_TOOL)) {
            return MachineTier.MV;
        }
        if (state.is(BlockTags.NEEDS_STONE_TOOL) || state.is(BlockTags.INCORRECT_FOR_WOODEN_TOOL)) {
            return MachineTier.LV;
        }
        return MachineTier.ULV;
    }

    private static Set<String> toolIds(Set<ToolDefinition> tools) {
        if (tools == null || tools.isEmpty()) {
            return Set.of();
        }
        Set<String> ids = new LinkedHashSet<>();
        for (ToolDefinition tool : tools) {
            ids.add(tool.id());
        }
        return Set.copyOf(ids);
    }

    private static ExplicitSource pickaxe(MachineTier tier) {
        return new ExplicitSource(normalizeTier(tier), Set.of(Tool.PICKAXE.id()));
    }

    private static MachineTier normalizeTier(MachineTier tier) {
        if (tier == null || tier == MachineTier.NONE) {
            return MachineTier.ULV;
        }
        return tier.recipeTier();
    }

    private static void put(Map<Block, ExplicitSource> target, Block block, ExplicitSource source) {
        ExplicitSource previous = target.put(block, source);
        if (previous != null && !previous.equals(source)) {
            throw new IllegalStateException("Conflicting Industron breaking definitions for block "
                    + BuiltInRegistries.BLOCK.getKey(block) + ": " + previous + " vs " + source);
        }
    }

    private static void putIfAbsent(Map<Block, ExplicitSource> target, Block block, ExplicitSource source) {
        ExplicitSource previous = target.putIfAbsent(block, source);
        if (previous != null && !previous.equals(source)) {
            // A block can intentionally appear in more than one structure/material view. The first explicit
            // source remains authoritative; contradictory aliases are left visible to definition validation.
        }
    }

    private record ExplicitSource(
            MachineTier tier,
            Set<String> toolTypeIds
    ) {
        private ExplicitSource {
            tier = normalizeTier(tier);
            toolTypeIds = Set.copyOf(toolTypeIds);
        }
    }
}
