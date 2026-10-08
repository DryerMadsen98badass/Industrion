package net.mads.industron.machine.interaction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mads.industron.Industron;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/** World condition used by a machine or one CE recipe. */
public record MachineCondition(
        Kind kind,
        Optional<ResourceLocation> id,
        Optional<RelativePos> pos,
        int min,
        int max,
        InteractionPhase check,
        ConditionFailure onFailure,
        List<MachineCondition> children,
        Optional<String> area,
        AreaMatch areaMatch
) {
    public static final Codec<MachineCondition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(Kind::valueOf, Kind::name).fieldOf("kind").forGetter(MachineCondition::kind),
            ResourceLocation.CODEC.optionalFieldOf("id").forGetter(MachineCondition::id),
            RelativePos.CODEC.optionalFieldOf("pos").forGetter(MachineCondition::pos),
            Codec.INT.optionalFieldOf("min", Integer.MIN_VALUE).forGetter(MachineCondition::min),
            Codec.INT.optionalFieldOf("max", Integer.MAX_VALUE).forGetter(MachineCondition::max),
            Codec.STRING.xmap(InteractionPhase::valueOf, InteractionPhase::name).optionalFieldOf("check", InteractionPhase.ON_START).forGetter(MachineCondition::check),
            Codec.STRING.xmap(ConditionFailure::valueOf, ConditionFailure::name).optionalFieldOf("on_failure", ConditionFailure.CANCEL).forGetter(MachineCondition::onFailure),
            Codec.lazyInitialized(() -> MachineCondition.CODEC).listOf().optionalFieldOf("children", List.of()).forGetter(MachineCondition::children),
            Codec.STRING.optionalFieldOf("area").forGetter(MachineCondition::area),
            Codec.STRING.xmap(AreaMatch::valueOf, AreaMatch::name).optionalFieldOf("area_match", AreaMatch.ALL).forGetter(MachineCondition::areaMatch)
    ).apply(instance, MachineCondition::new));

    /** Backwards-compatible constructor for one-position conditions. */
    public MachineCondition(Kind kind, Optional<ResourceLocation> id, Optional<RelativePos> pos,
                            int min, int max, InteractionPhase check, ConditionFailure onFailure,
                            List<MachineCondition> children) {
        this(kind, id, pos, min, max, check, onFailure, children, Optional.empty(), AreaMatch.ALL);
    }

    public MachineCondition {
        children = List.copyOf(children);
    }

    public static MachineCondition weather(Weather weather) {
        return base(Kind.WEATHER, Optional.of(ResourceLocation.withDefaultNamespace(weather.name().toLowerCase())), 0, 0);
    }

    public static RangeBuilder time() { return new RangeBuilder(Kind.TIME); }

    public static MachineCondition time(int startTime, int endTime) {
        if (startTime < 0 || startTime > 23999 || endTime < 0 || endTime > 23999) {
            throw new IllegalArgumentException("Time must be between 0 and 23999");
        }
        return base(Kind.TIME, Optional.empty(), startTime, endTime);
    }

    public static RangeBuilder height() { return new RangeBuilder(Kind.HEIGHT); }
    public static RangeBuilder redstone() { return new RangeBuilder(Kind.REDSTONE); }
    public static RangeBuilder light() { return new RangeBuilder(Kind.LIGHT); }
    public static MachineCondition biome(String biomeId) { return idCondition(Kind.BIOME, biomeId); }
    public static MachineCondition biomeTag(String tagId) { return idCondition(Kind.BIOME_TAG, tagId); }
    public static MachineCondition dimension(String dimensionId) { return idCondition(Kind.DIMENSION, dimensionId); }
    public static MachineCondition canSeeSky() { return base(Kind.CAN_SEE_SKY, Optional.empty(), 0, 0); }
    public static MachineCondition isWaterlogged() { return base(Kind.WATERLOGGED, Optional.empty(), 0, 0); }

    /** Requires daytime and an unobstructed view of the sky at the checked position(s). */
    public static MachineCondition daylight() { return base(Kind.DAYLIGHT, Optional.empty(), 0, 0); }

    public static MachineCondition all(MachineCondition... conditions) {
        return new MachineCondition(Kind.ALL, Optional.empty(), Optional.empty(), 0, 0,
                InteractionPhase.ON_START, ConditionFailure.CANCEL, List.of(conditions));
    }

    public static MachineCondition any(MachineCondition... conditions) {
        return new MachineCondition(Kind.ANY, Optional.empty(), Optional.empty(), 0, 0,
                InteractionPhase.ON_START, ConditionFailure.CANCEL, List.of(conditions));
    }

    public static MachineCondition not(MachineCondition condition) {
        return new MachineCondition(Kind.NOT, Optional.empty(), Optional.empty(), 0, 0,
                InteractionPhase.ON_START, ConditionFailure.CANCEL, List.of(condition));
    }

    /** Evaluates one-position conditions using controller-relative coordinates. */
    public boolean matches(Level level, BlockPos origin, net.minecraft.core.Direction facing) {
        return matchesAt(level, pos.map(relative -> relative.rotate(origin, facing)).orElse(origin), origin, facing);
    }

    /** Evaluates this condition with access to named machine areas. */
    public boolean matches(InteractionContext context) {
        if (kind == Kind.ALL) {
            return children.stream().allMatch(child -> child.matches(context));
        }
        if (kind == Kind.ANY) {
            return children.stream().anyMatch(child -> child.matches(context));
        }
        if (kind == Kind.NOT) {
            return children.isEmpty() || !children.getFirst().matches(context);
        }
        if (area.isEmpty()) {
            return matches(context.level(), context.origin(), context.facing());
        }
        MachineArea.Resolved resolved = context.areas().get(area.get());
        if (resolved == null || resolved.positions().isEmpty()) return false;
        return areaMatch == AreaMatch.ALL
                ? resolved.positions().stream().allMatch(pos -> matchesAt(context.level(), pos, context.origin(), context.facing()))
                : resolved.positions().stream().anyMatch(pos -> matchesAt(context.level(), pos, context.origin(), context.facing()));
    }

    private boolean matchesAt(Level level, BlockPos target, BlockPos origin, net.minecraft.core.Direction facing) {
        return switch (kind) {
            case WEATHER -> matchesWeather(level);
            case TIME -> inRange((int) (level.getDayTime() % 24000L));
            case HEIGHT -> inRange(target.getY());
            case REDSTONE -> inRange(level.getBestNeighborSignal(target));
            case LIGHT -> inRange(level.getBrightness(LightLayer.BLOCK, target));
            case BIOME -> id.map(value -> level.getBiome(target).is(ResourceKey.create(Registries.BIOME, value))).orElse(false);
            case BIOME_TAG -> id.map(value -> level.getBiome(target).is(TagKey.create(Registries.BIOME, value))).orElse(false);
            case DIMENSION -> id.map(value -> level.dimension().location().equals(value)).orElse(false);
            case CAN_SEE_SKY -> level.canSeeSky(target);
            case WATERLOGGED -> {
                var state = level.getBlockState(target);
                yield state.hasProperty(BlockStateProperties.WATERLOGGED)
                        && state.getValue(BlockStateProperties.WATERLOGGED);
            }
            case DAYLIGHT -> level.dimensionType().hasSkyLight()
                    && !level.dimensionType().hasCeiling()
                    && level.isDay() && level.canSeeSky(target);
            case ALL -> children.stream().allMatch(child -> child.matches(level, origin, facing));
            case ANY -> children.stream().anyMatch(child -> child.matches(level, origin, facing));
            case NOT -> children.isEmpty() || !children.getFirst().matches(level, origin, facing);
        };
    }

    /** Uses a named MachineArea. */
    public MachineCondition inArea(String areaName) {
        return inArea(areaName, AreaMatch.ALL);
    }

    /** Uses a named MachineArea and chooses whether every or any position must match. */
    public MachineCondition inArea(String areaName, AreaMatch match) {
        if (areaName == null || areaName.isBlank()) throw new IllegalArgumentException("Area name cannot be blank");
        return new MachineCondition(kind, id, pos, min, max, check, onFailure, children,
                Optional.of(areaName), match);
    }

    /** Uses one directional position. */
    public MachineCondition front(int amount) { return atDirectional(0, 0, amount); }
    public MachineCondition back(int amount) { return atDirectional(0, 0, -amount); }
    public MachineCondition left(int amount) { return atDirectional(-amount, 0, 0); }
    public MachineCondition right(int amount) { return atDirectional(amount, 0, 0); }
    public MachineCondition top(int amount) { return atDirectional(0, amount, 0); }
    public MachineCondition bottom(int amount) { return atDirectional(0, -amount, 0); }

    /** Legacy coordinate method. Prefer directional methods or .inArea(...). */
    public MachineCondition at(int x, int y, int z) { return atDirectional(x, y, z); }

    private MachineCondition atDirectional(int x, int y, int z) {
        return new MachineCondition(kind, id, Optional.of(new RelativePos(x, y, z)), min, max, check, onFailure,
                children, area, areaMatch);
    }

    public MachineCondition check(InteractionPhase check) {
        return new MachineCondition(kind, id, pos, min, max, check, onFailure, children, area, areaMatch);
    }

    public MachineCondition onFailure(ConditionFailure onFailure) {
        return new MachineCondition(kind, id, pos, min, max, check, onFailure, children, area, areaMatch);
    }

    private boolean matchesWeather(Level level) {
        String value = id.map(ResourceLocation::getPath).orElse("");
        return switch (value) {
            case "clear" -> !level.isRaining() && !level.isThundering();
            case "rain" -> level.isRaining() && !level.isThundering();
            case "thunder" -> level.isThundering();
            default -> false;
        };
    }

    private boolean inRange(int value) {
        if (kind == Kind.TIME && min > max) return value >= min || value <= max;
        return value >= min && value <= max;
    }

    private static MachineCondition idCondition(Kind kind, String rawId) {
        return base(kind, Optional.of(id(rawId)), 0, 0);
    }

    private static MachineCondition base(Kind kind, Optional<ResourceLocation> id, int min, int max) {
        return new MachineCondition(kind, id, Optional.empty(), min, max,
                InteractionPhase.ON_START, ConditionFailure.CANCEL, List.of());
    }

    private static ResourceLocation id(String id) {
        return id.contains(":") ? ResourceLocation.parse(id) : ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, id);
    }

    public enum Kind { WEATHER, TIME, HEIGHT, REDSTONE, LIGHT, BIOME, BIOME_TAG, DIMENSION, CAN_SEE_SKY, WATERLOGGED, DAYLIGHT, ALL, ANY, NOT }

    /** Builder for conditions using numeric ranges. */
    public static final class RangeBuilder {
        private final Kind kind;
        private RangeBuilder(Kind kind) { this.kind = kind; }
        public MachineCondition between(int min, int max) { return base(kind, Optional.empty(), min, max); }
        public MachineCondition minimum(int min) { return between(min, Integer.MAX_VALUE); }
        public MachineCondition maximum(int max) { return between(Integer.MIN_VALUE, max); }
    }
}
