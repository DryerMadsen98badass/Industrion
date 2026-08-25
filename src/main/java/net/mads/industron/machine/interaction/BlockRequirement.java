package net.mads.industron.machine.interaction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mads.industron.Industron;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import java.util.List;
import java.util.Optional;

/** Block or fluid predicate plus per-match interaction settings. */
public record BlockRequirement(
        Kind kind,
        Optional<ResourceLocation> id,
        List<BlockRequirement> children,
        float actionChance,
        int damage,
        float damageChance,
        int breakAfter,
        BlockLoot lootTable
) {
    public static final Codec<BlockRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(Kind::valueOf, Kind::name).fieldOf("kind").forGetter(BlockRequirement::kind),
            ResourceLocation.CODEC.optionalFieldOf("id").forGetter(BlockRequirement::id),
            Codec.lazyInitialized(() -> BlockRequirement.CODEC).listOf().optionalFieldOf("children", List.of()).forGetter(BlockRequirement::children),
            Codec.FLOAT.optionalFieldOf("action_chance", 1.0F).forGetter(BlockRequirement::actionChance),
            Codec.INT.optionalFieldOf("damage", 0).forGetter(BlockRequirement::damage),
            Codec.FLOAT.optionalFieldOf("damage_chance", 1.0F).forGetter(BlockRequirement::damageChance),
            Codec.INT.optionalFieldOf("break_after", 0).forGetter(BlockRequirement::breakAfter),
            BlockLoot.CODEC.optionalFieldOf("loot", BlockLoot.none()).forGetter(BlockRequirement::lootTable)
    ).apply(instance, BlockRequirement::new));

    public BlockRequirement {
        children = List.copyOf(children);
        actionChance = clampChance(actionChance);
        damageChance = clampChance(damageChance);
        damage = Math.max(0, damage);
        breakAfter = Math.max(0, breakAfter);
    }

    /** Matches any non-air, non-fluid block. */
    public static BlockRequirement anyBlock() {
        return new BlockRequirement(Kind.ANY_BLOCK, Optional.empty(), List.of(), 1.0F, 0, 1.0F, 0, BlockLoot.none());
    }

    /** Matches an empty air position. */
    public static BlockRequirement empty() {
        return new BlockRequirement(Kind.EMPTY, Optional.empty(), List.of(), 1.0F, 0, 1.0F, 0, BlockLoot.none());
    }

    /** Matches one exact block id. */
    public static BlockRequirement block(String blockId) {
        return new BlockRequirement(Kind.BLOCK, Optional.of(id(blockId)), List.of(), 1.0F, 0, 1.0F, 0, BlockLoot.none());
    }

    /** Matches one block tag. */
    public static BlockRequirement tag(String tagId) {
        return new BlockRequirement(Kind.TAG, Optional.of(id(tagId)), List.of(), 1.0F, 0, 1.0F, 0, BlockLoot.none());
    }

    /** Matches a fluid occupying the target position. */
    public static BlockRequirement fluid(String fluidId) {
        return new BlockRequirement(Kind.FLUID, Optional.of(id(fluidId)), List.of(), 1.0F, 0, 1.0F, 0, BlockLoot.none());
    }

    /** Matches when any child matches. */
    public static BlockRequirement anyOf(BlockRequirement... requirements) {
        return new BlockRequirement(Kind.ANY_OF, Optional.empty(), List.of(requirements), 1.0F, 0, 1.0F, 0, BlockLoot.none());
    }

    /** Matches when every child matches. */
    public static BlockRequirement allOf(BlockRequirement... requirements) {
        return new BlockRequirement(Kind.ALL_OF, Optional.empty(), List.of(requirements), 1.0F, 0, 1.0F, 0, BlockLoot.none());
    }

    /** Matches when the child does not match. */
    public static BlockRequirement not(BlockRequirement requirement) {
        return new BlockRequirement(Kind.NOT, Optional.empty(), List.of(requirement), 1.0F, 0, 1.0F, 0, BlockLoot.none());
    }

    /** Chance that consume/convert/place action happens after this requirement matches. */
    public BlockRequirement actionChance(float chance) {
        return new BlockRequirement(kind, id, children, chance, damage, damageChance, breakAfter, lootTable);
    }

    /** Damage added to the wear counter when a damage interaction succeeds. */
    public BlockRequirement damage(int damage) {
        return new BlockRequirement(kind, id, children, actionChance, damage, damageChance, breakAfter, lootTable);
    }

    /** Chance that this requirement receives wear during a damage interaction. */
    public BlockRequirement damageChance(float chance) {
        return new BlockRequirement(kind, id, children, actionChance, damage, chance, breakAfter, lootTable);
    }

    /** Wear threshold where a machine breaks this block. */
    public BlockRequirement breakAfter(int breakAfter) {
        return new BlockRequirement(kind, id, children, actionChance, damage, damageChance, breakAfter, lootTable);
    }

    /** Loot rolled when this requirement's block is removed by the machine. */
    public BlockRequirement lootTable(BlockLoot lootTable) {
        return new BlockRequirement(kind, id, children, actionChance, damage, damageChance, breakAfter, lootTable);
    }

    public Optional<BlockRequirement> matchingLeaf(Level level, BlockState state) {
        return switch (kind) {
            case ANY_BLOCK, EMPTY, BLOCK, TAG, FLUID -> matchesSelf(level, state) ? Optional.of(this) : Optional.empty();
            case ANY_OF -> children.stream()
                    .map(child -> child.matchingLeaf(level, state))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst();
            case ALL_OF -> children.stream().allMatch(child -> child.matchingLeaf(level, state).isPresent())
                    ? Optional.of(this) : Optional.empty();
            case NOT -> children.isEmpty() || children.getFirst().matchingLeaf(level, state).isEmpty()
                    ? Optional.of(this) : Optional.empty();
        };
    }

    private boolean matchesSelf(Level level, BlockState state) {
        return switch (kind) {
            case ANY_BLOCK -> !state.isAir() && state.getFluidState().isEmpty();
            case EMPTY -> state.isAir();
            case BLOCK -> id.map(blockId -> BuiltInRegistries.BLOCK.get(blockId) == state.getBlock()).orElse(false);
            case TAG -> id.map(tagId -> state.is(blockTag(tagId))).orElse(false);
            case FLUID -> id.map(fluidId -> state.getFluidState().is(BuiltInRegistries.FLUID.get(fluidId))).orElse(false);
            default -> false;
        };
    }

    private static TagKey<Block> blockTag(ResourceLocation id) {
        return BlockTags.create(id);
    }

    public Block targetBlock() {
        return id.map(BuiltInRegistries.BLOCK::get).orElse(Blocks.AIR);
    }

    private static ResourceLocation id(String id) {
        return id.contains(":") ? ResourceLocation.parse(id) : ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, id);
    }

    private static float clampChance(float chance) {
        return Math.max(0.0F, Math.min(1.0F, chance));
    }

    public enum Kind {
        ANY_BLOCK,
        EMPTY,
        BLOCK,
        TAG,
        FLUID,
        ANY_OF,
        ALL_OF,
        NOT
    }
}
