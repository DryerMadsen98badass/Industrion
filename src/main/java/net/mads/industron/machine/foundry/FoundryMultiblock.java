package net.mads.industron.machine.foundry;

import net.mads.industron.machine.machines.electric.multiblock.MultiblockAbility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Variable-size, open-top Foundry structure matcher.
 *
 * <p>Outer footprint is exactly 3x3, 5x5, 7x7 or 9x9. The base is one solid square layer.
 * Above it is an open chamber surrounded by complete perimeter walls. Wall height has no artificial
 * maximum; the world build height is the only limit.</p>
 */
public final class FoundryMultiblock {
    private static final int[] OUTER_SIZES_DESCENDING = {9, 7, 5, 3};

    /** Per-validation memoization avoids reading the same cells for every candidate footprint. */
    private static final class View {
        private final Level world;
        private final Map<BlockPos, BlockState> states = new HashMap<>();
        View(Level world) { this.world = world; }
        int getMinBuildHeight() { return world.getMinBuildHeight(); }
        int getMaxBuildHeight() { return world.getMaxBuildHeight(); }
        boolean hasChunkAt(BlockPos pos) { return world.hasChunkAt(pos); }
        BlockState getBlockState(BlockPos pos) {
            BlockState cached = states.get(pos);
            if (cached != null) return cached;
            if (states.size() >= 8192) states.clear();
            BlockState state = world.getBlockState(pos);
            states.put(pos.immutable(), state);
            return state;
        }
    }

    private FoundryMultiblock() {
    }

    public static Match tryMatch(Level world, BlockPos controllerPos, Direction facing) {
        if (world == null) return Match.failed();
        View level = new View(world);
        if (level == null || controllerPos == null || facing == null || facing.getAxis() == Direction.Axis.Y) {
            return Match.failed();
        }

        // Try the nearest possible base first. Intermediate wall layers fail the solid-base
        // check because their interior is air, while the real base is a complete square. This
        // also avoids treating unrelated Foundry bricks below the actual floor as part of it.
        int minimum = level.getMinBuildHeight();
        for (int baseY = controllerPos.getY() - 1; baseY >= minimum; baseY--) {
            BlockPos belowController = new BlockPos(controllerPos.getX(), baseY, controllerPos.getZ());
            if (!level.hasChunkAt(belowController)
                    || !isAnyFoundryStructuralMember(level.getBlockState(belowController))) {
                break;
            }

            for (int size : OUTER_SIZES_DESCENDING) {
                for (int offset = 0; offset < size; offset++) {
                    Bounds bounds = boundsFor(controllerPos, facing, size, offset);
                    Match match = validate(level, controllerPos, facing, baseY, bounds);
                    if (match.matched()) {
                        return match;
                    }
                }
            }
        }

        return Match.failed();
    }

    private static Match validate(
            View level,
            BlockPos controllerPos,
            Direction facing,
            int baseY,
            Bounds bounds
    ) {
        if (!bounds.onPerimeter(controllerPos) || !bounds.controllerFacesOutward(controllerPos, facing)) {
            return Match.failed();
        }

        Map<ResourceLocation, Integer> brickCounts = new HashMap<>();
        Map<ResourceLocation, FoundryBrickResolver.BrickInfo> bricksById = new HashMap<>();
        List<BlockPos> brickPositions = new ArrayList<>();
        Set<BlockPos> specialParts = new LinkedHashSet<>();
        Map<MultiblockAbility, List<BlockPos>> abilities = new EnumMap<>(MultiblockAbility.class);

        // Base/floor: only real Foundry bricks and Fluid Output Hatches are allowed.
        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                BlockPos pos = new BlockPos(x, baseY, z);
                BlockState state = safeState(level, pos);
                if (state == null) {
                    return Match.failed();
                }

                FoundryBrickResolver.BrickInfo brick = FoundryBrickResolver.resolve(state).orElse(null);
                if (brick != null) {
                    addBrick(brick, pos, brickCounts, bricksById, brickPositions);
                    continue;
                }

                FoundryPartType partType = partType(state);
                if (partType == null || !partType.allowedInBase()) {
                    return Match.failed();
                }
                specialParts.add(pos);
                addAbility(partType, pos, abilities);
            }
        }

        int topY = baseY;
        boolean sawWallLayer = false;
        int y = baseY + 1;
        int maxYExclusive = level.getMaxBuildHeight();

        while (y < maxYExclusive) {
            WallLayer layer = inspectWallLayer(
                    level,
                    controllerPos,
                    facing,
                    bounds,
                    y,
                    brickCounts,
                    bricksById,
                    brickPositions,
                    specialParts,
                    abilities
            );

            if (layer == WallLayer.FULL) {
                sawWallLayer = true;
                topY = y;
                y++;
                continue;
            }

            if (y <= controllerPos.getY()) {
                return Match.failed();
            }

            if (layer == WallLayer.INCOMPLETE_FOUNDRY_LAYER) {
                return Match.failed();
            }

            // First level above the controller with no Foundry structural members marks the open top.
            break;
        }

        if (!sawWallLayer || controllerPos.getY() > topY) {
            return Match.failed();
        }

        // All I/O abilities are optional: the controller GUI supports manual insertion/extraction.

        if (brickCounts.isEmpty()) {
            return Match.failed();
        }

        ResourceLocation dominantBrickId = brickCounts.entrySet().stream()
                .sorted(Comparator
                        .<Map.Entry<ResourceLocation, Integer>>comparingInt(Map.Entry::getValue)
                        .reversed()
                        .thenComparing(entry -> entry.getKey().toString()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
        if (dominantBrickId == null) {
            return Match.failed();
        }

        FoundryBrickResolver.BrickInfo dominantBrick = bricksById.get(dominantBrickId);
        if (dominantBrick == null) {
            return Match.failed();
        }

        specialParts.add(controllerPos);
        int insideWidth = bounds.size() - 2;
        int insideHeight = topY - baseY;
        int insideArea = insideWidth * insideWidth;
        int insideVolume = insideArea * insideHeight;

        return new Match(
                true,
                bounds.size(),
                bounds.minX(),
                bounds.minZ(),
                baseY,
                topY,
                insideWidth,
                insideHeight,
                insideArea,
                insideVolume,
                dominantBrick.material().id(),
                dominantBrick.modelId(),
                List.copyOf(brickPositions),
                List.copyOf(specialParts),
                copyAbilities(abilities)
        );
    }

    private static WallLayer inspectWallLayer(
            View level,
            BlockPos controllerPos,
            Direction facing,
            Bounds bounds,
            int y,
            Map<ResourceLocation, Integer> brickCounts,
            Map<ResourceLocation, FoundryBrickResolver.BrickInfo> bricksById,
            List<BlockPos> brickPositions,
            Set<BlockPos> specialParts,
            Map<MultiblockAbility, List<BlockPos>> abilities
    ) {
        List<PendingBrick> pendingBricks = new ArrayList<>();
        List<PendingPart> pendingParts = new ArrayList<>();
        boolean full = true;
        boolean anyFoundryMember = false;

        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                if (!bounds.isPerimeter(x, z)) {
                    continue;
                }

                BlockPos pos = new BlockPos(x, y, z);
                BlockState state = safeState(level, pos);
                if (state == null) {
                    return WallLayer.INCOMPLETE_FOUNDRY_LAYER;
                }

                boolean isControllerPosition = pos.equals(controllerPos);
                FoundryBrickResolver.BrickInfo brick = FoundryBrickResolver.resolve(state).orElse(null);
                FoundryPartType partType = partType(state);

                if (brick != null) {
                    anyFoundryMember = true;
                    if (isControllerPosition) {
                        full = false;
                    } else {
                        pendingBricks.add(new PendingBrick(brick, pos));
                    }
                    continue;
                }

                if (partType != null) {
                    anyFoundryMember = true;
                    if (isControllerPosition) {
                        if (partType != FoundryPartType.CONTROLLER
                                || !(state.getBlock() instanceof FoundryPartBlock foundryBlock)
                                || state.getValue(FoundryPartBlock.FACING) != facing
                                || !foundryBlock.partType().isController()) {
                            full = false;
                        } else {
                            pendingParts.add(new PendingPart(partType, pos));
                        }
                    } else if (partType.allowedInWall()) {
                        pendingParts.add(new PendingPart(partType, pos));
                    } else {
                        full = false;
                    }
                    continue;
                }

                full = false;
            }
        }

        if (!full) {
            return anyFoundryMember ? WallLayer.INCOMPLETE_FOUNDRY_LAYER : WallLayer.OPEN_TOP;
        }

        // The chamber itself is empty for now. Future Foundry contents live in controller data,
        // not as blocks placed inside the structure.
        for (int x = bounds.minX() + 1; x <= bounds.maxX() - 1; x++) {
            for (int z = bounds.minZ() + 1; z <= bounds.maxZ() - 1; z++) {
                BlockPos pos = new BlockPos(x, y, z);
                BlockState state = safeState(level, pos);
                if (state == null || !state.isAir()) {
                    return WallLayer.INCOMPLETE_FOUNDRY_LAYER;
                }
            }
        }

        // Only commit counts/abilities after the complete layer has been validated.
        for (PendingBrick pending : pendingBricks) {
            addBrick(pending.brick(), pending.pos(), brickCounts, bricksById, brickPositions);
        }
        for (PendingPart pending : pendingParts) {
            specialParts.add(pending.pos());
            addAbility(pending.type(), pending.pos(), abilities);
        }
        return WallLayer.FULL;
    }

    private static void addBrick(
            FoundryBrickResolver.BrickInfo brick,
            BlockPos pos,
            Map<ResourceLocation, Integer> brickCounts,
            Map<ResourceLocation, FoundryBrickResolver.BrickInfo> bricksById,
            List<BlockPos> brickPositions
    ) {
        brickCounts.merge(brick.blockId(), 1, Integer::sum);
        bricksById.putIfAbsent(brick.blockId(), brick);
        brickPositions.add(pos);
    }

    private static void addAbility(
            FoundryPartType partType,
            BlockPos pos,
            Map<MultiblockAbility, List<BlockPos>> abilities
    ) {
        MultiblockAbility ability = partType.ability();
        if (ability != null) {
            abilities.computeIfAbsent(ability, ignored -> new ArrayList<>()).add(pos);
        }
    }

    private static int abilityCount(Map<MultiblockAbility, List<BlockPos>> abilities, MultiblockAbility ability) {
        return abilities.getOrDefault(ability, List.of()).size();
    }

    private static Map<MultiblockAbility, List<BlockPos>> copyAbilities(
            Map<MultiblockAbility, List<BlockPos>> abilities
    ) {
        Map<MultiblockAbility, List<BlockPos>> result = new EnumMap<>(MultiblockAbility.class);
        abilities.forEach((ability, positions) -> result.put(ability, List.copyOf(positions)));
        return Map.copyOf(result);
    }

    private static boolean isAnyFoundryStructuralMember(BlockState state) {
        return FoundryBrickResolver.resolve(state).isPresent() || partType(state) != null;
    }

    private static FoundryPartType partType(BlockState state) {
        return state != null && state.getBlock() instanceof FoundryPartBlock foundryBlock
                ? foundryBlock.partType()
                : null;
    }

    private static BlockState safeState(View level, BlockPos pos) {
        return level.hasChunkAt(pos) ? level.getBlockState(pos) : null;
    }

    private static Bounds boundsFor(BlockPos controllerPos, Direction facing, int size, int offset) {
        return switch (facing) {
            case NORTH -> new Bounds(
                    controllerPos.getX() - offset,
                    controllerPos.getX() - offset + size - 1,
                    controllerPos.getZ(),
                    controllerPos.getZ() + size - 1,
                    size
            );
            case SOUTH -> new Bounds(
                    controllerPos.getX() - offset,
                    controllerPos.getX() - offset + size - 1,
                    controllerPos.getZ() - size + 1,
                    controllerPos.getZ(),
                    size
            );
            case WEST -> new Bounds(
                    controllerPos.getX(),
                    controllerPos.getX() + size - 1,
                    controllerPos.getZ() - offset,
                    controllerPos.getZ() - offset + size - 1,
                    size
            );
            case EAST -> new Bounds(
                    controllerPos.getX() - size + 1,
                    controllerPos.getX(),
                    controllerPos.getZ() - offset,
                    controllerPos.getZ() - offset + size - 1,
                    size
            );
            default -> throw new IllegalArgumentException("Foundry controller must face horizontally");
        };
    }

    private enum WallLayer {
        FULL,
        OPEN_TOP,
        INCOMPLETE_FOUNDRY_LAYER
    }

    private record PendingBrick(FoundryBrickResolver.BrickInfo brick, BlockPos pos) {
    }

    private record PendingPart(FoundryPartType type, BlockPos pos) {
    }

    private record Bounds(int minX, int maxX, int minZ, int maxZ, int size) {
        boolean isPerimeter(int x, int z) {
            return x == minX || x == maxX || z == minZ || z == maxZ;
        }

        boolean onPerimeter(BlockPos pos) {
            return pos.getX() >= minX && pos.getX() <= maxX
                    && pos.getZ() >= minZ && pos.getZ() <= maxZ
                    && isPerimeter(pos.getX(), pos.getZ());
        }

        boolean controllerFacesOutward(BlockPos pos, Direction facing) {
            return switch (facing) {
                case NORTH -> pos.getZ() == minZ;
                case SOUTH -> pos.getZ() == maxZ;
                case WEST -> pos.getX() == minX;
                case EAST -> pos.getX() == maxX;
                default -> false;
            };
        }
    }

    public record Match(
            boolean matched,
            int outerSize,
            int minX,
            int minZ,
            int baseY,
            int topY,
            int insideWidth,
            int insideHeight,
            int insideArea,
            int insideVolume,
            String dominantMaterialId,
            ResourceLocation dominantCasingModel,
            List<BlockPos> brickPositions,
            List<BlockPos> specialPartPositions,
            Map<MultiblockAbility, List<BlockPos>> abilityPositions
    ) {
        public static Match failed() {
            return new Match(
                    false,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    "",
                    null,
                    List.of(),
                    List.of(),
                    Map.of()
            );
        }
    }
}
