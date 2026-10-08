package net.mads.industron.machine.interaction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mads.industron.Industron;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Optional;

/** A machine or recipe action performed at one position or throughout a named machine area. */
public record BlockInteraction(
        Type type,
        RelativePos pos,
        InteractionPhase when,
        BlockRequirement requirement,
        Optional<ResourceLocation> targetBlockId,
        Optional<String> area,
        AreaSelection selection,
        int limit,
        int interval,
        int actionsPerInterval,
        int actionMultiplierPerTier,
        int minimumMatches
) {
    public static final Codec<BlockInteraction> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(Type::valueOf, Type::name).fieldOf("type").forGetter(BlockInteraction::type),
            RelativePos.CODEC.optionalFieldOf("pos", new RelativePos(0, 0, 0)).forGetter(BlockInteraction::pos),
            Codec.STRING.xmap(InteractionPhase::valueOf, InteractionPhase::name).optionalFieldOf("when", InteractionPhase.ON_COMPLETE).forGetter(BlockInteraction::when),
            BlockRequirement.CODEC.fieldOf("requirement").forGetter(BlockInteraction::requirement),
            ResourceLocation.CODEC.optionalFieldOf("target").forGetter(BlockInteraction::targetBlockId),
            Codec.STRING.optionalFieldOf("area").forGetter(BlockInteraction::area),
            Codec.STRING.xmap(AreaSelection::valueOf, AreaSelection::name).optionalFieldOf("selection", AreaSelection.FIRST).forGetter(BlockInteraction::selection),
            Codec.INT.optionalFieldOf("limit", 1).forGetter(BlockInteraction::limit),
            Codec.INT.optionalFieldOf("interval", 1).forGetter(BlockInteraction::interval),
            Codec.INT.optionalFieldOf("actions_per_interval", 1).forGetter(BlockInteraction::actionsPerInterval),
            Codec.INT.optionalFieldOf("action_multiplier_per_tier", 1).forGetter(BlockInteraction::actionMultiplierPerTier),
            Codec.INT.optionalFieldOf("minimum_matches", 1).forGetter(BlockInteraction::minimumMatches)
    ).apply(instance, BlockInteraction::new));

    public static Builder require() { return new Builder(Type.REQUIRE); }
    public static Builder consume() { return new Builder(Type.CONSUME); }
    public static Builder convert() { return new Builder(Type.CONVERT); }
    public static Builder place() { return new Builder(Type.PLACE); }
    public static Builder damage() { return new Builder(Type.DAMAGE); }

    /** Reads naturally grown tree data from the selected root log. */
    public static Builder treeExtract() { return new Builder(Type.TREE_EXTRACT); }

    /**
     * Repeatedly applies vanilla bonemeal growth to cached targets in a named area.
     * Targets are scanned once per full pass, then processed in deterministic order.
     */
    public static Builder sprinkler() { return new Builder(Type.SPRINKLER); }

    public Block targetBlock() {
        return targetBlockId.map(BuiltInRegistries.BLOCK::get).orElse(Blocks.AIR);
    }

    public enum Type { REQUIRE, CONSUME, CONVERT, PLACE, DAMAGE, TREE_EXTRACT, SPRINKLER }

    /** Fluent builder shown in machine and recipe examples. */
    public static final class Builder {
        private final Type type;
        private RelativePos pos = new RelativePos(0, 0, 0);
        private InteractionPhase when = InteractionPhase.ON_COMPLETE;
        private BlockRequirement requirement = BlockRequirement.anyBlock();
        private Optional<ResourceLocation> targetBlockId = Optional.empty();
        private Optional<String> area = Optional.empty();
        private AreaSelection selection = AreaSelection.FIRST;
        private int limit = 1;
        private int interval = 1;
        private int actionsPerInterval = 1;
        private int actionMultiplierPerTier = 1;
        private int minimumMatches = 1;

        private Builder(Type type) { this.type = type; }

        /** Legacy controller-relative coordinate method. Prefer directional methods or .inArea(...). */
        public Builder at(int x, int y, int z) {
            this.pos = new RelativePos(x, y, z);
            return this;
        }

        /** Targets one block in front of the controller. */
        public Builder front(int amount) { this.pos = new RelativePos(0, 0, amount); return this; }
        public Builder back(int amount) { this.pos = new RelativePos(0, 0, -amount); return this; }
        public Builder left(int amount) { this.pos = new RelativePos(-amount, 0, 0); return this; }
        public Builder right(int amount) { this.pos = new RelativePos(amount, 0, 0); return this; }
        public Builder top(int amount) { this.pos = new RelativePos(0, amount, 0); return this; }
        public Builder bottom(int amount) { this.pos = new RelativePos(0, -amount, 0); return this; }

        /** Uses every matching position from a named MachineArea instead of one position. */
        public Builder inArea(String areaName) {
            if (areaName == null || areaName.isBlank()) throw new IllegalArgumentException("Area name cannot be blank");
            this.area = Optional.of(areaName);
            this.selection = AreaSelection.ALL;
            this.limit = Integer.MAX_VALUE;
            return this;
        }

        public Builder selection(AreaSelection selection) { this.selection = selection; return this; }
        public Builder limit(int limit) {
            if (limit < 1) throw new IllegalArgumentException("Interaction limit must be at least 1");
            this.limit = limit;
            return this;
        }
        /** Runs the continuous interaction once every this many processing ticks. */
        public Builder interval(int ticks) {
            if (ticks < 1) throw new IllegalArgumentException("Interaction interval must be at least 1 tick");
            this.interval = ticks;
            return this;
        }

        /** Sets the number of targets processed at the machine family's first tier. */
        public Builder actionsPerInterval(int actions) {
            if (actions < 1) throw new IllegalArgumentException("Actions per interval must be at least 1");
            this.actionsPerInterval = actions;
            return this;
        }

        /** Multiplies actions once for every generated machine tier above the start tier. */
        public Builder actionMultiplierPerTier(int multiplier) {
            if (multiplier < 1) throw new IllegalArgumentException("Action multiplier must be at least 1");
            this.actionMultiplierPerTier = multiplier;
            return this;
        }

        /** Requires at least this many selected positions to satisfy the requirement. */
        public Builder minimumMatches(int count) {
            if (count < 1) throw new IllegalArgumentException("Minimum matches must be at least 1");
            this.minimumMatches = count;
            return this;
        }

        public Builder when(InteractionPhase when) { this.when = when; return this; }
        public Builder requires(BlockRequirement requirement) { this.requirement = requirement; return this; }
        public Builder to(String blockId) { this.targetBlockId = Optional.of(id(blockId)); return this; }
        public Builder block(String blockId) { return to(blockId); }
        public Builder actionChance(float chance) { this.requirement = this.requirement.actionChance(chance); return this; }

        public BlockInteraction build() {
            if ((type == Type.CONVERT || type == Type.PLACE) && targetBlockId.isEmpty()) {
                throw new IllegalStateException(type + " block interaction needs .to(...) or .block(...)");
            }
            if (type == Type.SPRINKLER && area.isEmpty()) {
                throw new IllegalStateException("SPRINKLER block interaction needs .inArea(...)");
            }
            return new BlockInteraction(type, pos, when, requirement, targetBlockId, area, selection, limit, interval, actionsPerInterval, actionMultiplierPerTier, minimumMatches);
        }
    }

    private static ResourceLocation id(String id) {
        return id.contains(":") ? ResourceLocation.parse(id) : ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, id);
    }
}
