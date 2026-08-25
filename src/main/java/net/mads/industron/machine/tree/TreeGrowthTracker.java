package net.mads.industron.machine.tree;

import net.mads.industron.Industron;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.BlockGrowFeatureEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

@EventBusSubscriber(modid = Industron.MOD_ID)
public final class TreeGrowthTracker {

    private static final int GROWTH_SCAN_DELAY_TICKS = 2;
    private static final int MAX_TREE_LOGS = 1024;
    private static final int MAX_HORIZONTAL_DISTANCE = 24;
    private static final int MAX_VERTICAL_DISTANCE = 64;

    private static final Map<ServerLevel, Map<BlockPos, Integer>> PENDING_GROWTHS =
            new IdentityHashMap<>();

    private TreeGrowthTracker() {
    }

    @SubscribeEvent
    public static void onTreeGrowthStarted(BlockGrowFeatureEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        PENDING_GROWTHS
                .computeIfAbsent(level, ignored -> new HashMap<>())
                .put(
                        event.getPos().immutable(),
                        GROWTH_SCAN_DELAY_TICKS
                );
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        Map<BlockPos, Integer> pending = PENDING_GROWTHS.get(level);

        if (pending == null || pending.isEmpty()) {
            return;
        }

        List<BlockPos> ready = new ArrayList<>();
        Map<BlockPos, Integer> updated = new HashMap<>();

        for (Map.Entry<BlockPos, Integer> entry : pending.entrySet()) {
            int ticksRemaining = entry.getValue() - 1;

            if (ticksRemaining <= 0) {
                ready.add(entry.getKey());
            } else {
                updated.put(entry.getKey(), ticksRemaining);
            }
        }

        if (updated.isEmpty()) {
            PENDING_GROWTHS.remove(level);
        } else {
            PENDING_GROWTHS.put(level, updated);
        }

        for (BlockPos growthPos : ready) {
            registerGrownTree(level, growthPos);
        }
    }

    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        BlockPos brokenPos = event.getPos();

        if (!TreeExtractionSavedData.get(level).hasTree(brokenPos)) {
            return;
        }

        TreeExtractionSavedData.get(level).removeRoot(brokenPos);
    }

    private static void registerGrownTree(
            ServerLevel level,
            BlockPos growthPos
    ) {
        BlockPos seedLog = findSeedLog(level, growthPos);

        if (seedLog == null) {
            return;
        }

        Set<BlockPos> logs = collectTreeLogs(
                level,
                seedLog,
                growthPos
        );

        if (logs.isEmpty()) {
            return;
        }

        Set<BlockPos> roots = findRoots(logs, growthPos);

        if (roots.isEmpty()) {
            return;
        }

        BlockState rootState = level.getBlockState(roots.iterator().next());

        if (!rootState.is(BlockTags.LOGS)) {
            return;
        }

        ResourceLocation logId = BuiltInRegistries.BLOCK.getKey(
                rootState.getBlock()
        );

        TreeExtractionSavedData.get(level).registerTree(
                logId,
                logs.size(),
                roots
        );
    }

    private static BlockPos findSeedLog(
            ServerLevel level,
            BlockPos growthPos
    ) {
        BlockPos bestPos = null;
        int bestDistance = Integer.MAX_VALUE;

        for (int yOffset = -1; yOffset <= 12; yOffset++) {
            for (int xOffset = -2; xOffset <= 2; xOffset++) {
                for (int zOffset = -2; zOffset <= 2; zOffset++) {
                    BlockPos candidate = growthPos.offset(
                            xOffset,
                            yOffset,
                            zOffset
                    );

                    if (!level.getBlockState(candidate).is(BlockTags.LOGS)) {
                        continue;
                    }

                    int distance =
                            Math.abs(xOffset)
                                    + Math.abs(yOffset)
                                    + Math.abs(zOffset);

                    if (distance < bestDistance) {
                        bestDistance = distance;
                        bestPos = candidate.immutable();
                    }
                }
            }
        }

        return bestPos;
    }

    private static Set<BlockPos> collectTreeLogs(
            ServerLevel level,
            BlockPos seedLog,
            BlockPos growthPos
    ) {
        Set<BlockPos> visited = new LinkedHashSet<>();
        Set<BlockPos> logs = new LinkedHashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();

        queue.add(seedLog);
        visited.add(seedLog);

        while (!queue.isEmpty() && logs.size() < MAX_TREE_LOGS) {
            BlockPos current = queue.remove();

            if (!isInsideTreeScanArea(current, growthPos)) {
                continue;
            }

            BlockState state = level.getBlockState(current);

            if (!state.is(BlockTags.LOGS)) {
                continue;
            }

            logs.add(current.immutable());

            for (int xOffset = -1; xOffset <= 1; xOffset++) {
                for (int yOffset = -1; yOffset <= 1; yOffset++) {
                    for (int zOffset = -1; zOffset <= 1; zOffset++) {
                        if (xOffset == 0
                                && yOffset == 0
                                && zOffset == 0) {
                            continue;
                        }

                        BlockPos next = current.offset(
                                xOffset,
                                yOffset,
                                zOffset
                        );

                        if (visited.add(next.immutable())) {
                            queue.add(next.immutable());
                        }
                    }
                }
            }
        }

        return logs;
    }

    private static boolean isInsideTreeScanArea(
            BlockPos pos,
            BlockPos growthPos
    ) {
        int horizontalX = Math.abs(pos.getX() - growthPos.getX());
        int horizontalZ = Math.abs(pos.getZ() - growthPos.getZ());
        int vertical = Math.abs(pos.getY() - growthPos.getY());

        return horizontalX <= MAX_HORIZONTAL_DISTANCE
                && horizontalZ <= MAX_HORIZONTAL_DISTANCE
                && vertical <= MAX_VERTICAL_DISTANCE;
    }

    private static Set<BlockPos> findRoots(
            Set<BlockPos> logs,
            BlockPos growthPos
    ) {
        int lowestY = logs.stream()
                .mapToInt(BlockPos::getY)
                .min()
                .orElse(Integer.MAX_VALUE);

        List<BlockPos> lowestLogs = logs.stream()
                .filter(pos -> pos.getY() == lowestY)
                .sorted(
                        Comparator.comparingInt(
                                pos -> horizontalDistanceSquared(
                                        pos,
                                        growthPos
                                )
                        )
                )
                .toList();

        if (lowestLogs.isEmpty()) {
            return Set.of();
        }

        Set<BlockPos> twoByTwoRoots = findTwoByTwoRoots(lowestLogs);

        if (!twoByTwoRoots.isEmpty()) {
            return twoByTwoRoots;
        }

        return Set.of(lowestLogs.getFirst().immutable());
    }

    private static Set<BlockPos> findTwoByTwoRoots(
            List<BlockPos> lowestLogs
    ) {
        Set<BlockPos> positions = new LinkedHashSet<>(lowestLogs);

        for (BlockPos first : lowestLogs) {
            BlockPos east = first.relative(Direction.EAST);
            BlockPos south = first.relative(Direction.SOUTH);
            BlockPos southEast = east.relative(Direction.SOUTH);

            if (positions.contains(east)
                    && positions.contains(south)
                    && positions.contains(southEast)) {
                return Set.of(
                        first.immutable(),
                        east.immutable(),
                        south.immutable(),
                        southEast.immutable()
                );
            }

            BlockPos west = first.relative(Direction.WEST);
            BlockPos north = first.relative(Direction.NORTH);
            BlockPos northWest = west.relative(Direction.NORTH);

            if (positions.contains(west)
                    && positions.contains(north)
                    && positions.contains(northWest)) {
                return Set.of(
                        first.immutable(),
                        west.immutable(),
                        north.immutable(),
                        northWest.immutable()
                );
            }
        }

        return Set.of();
    }

    private static int horizontalDistanceSquared(
            BlockPos first,
            BlockPos second
    ) {
        int deltaX = first.getX() - second.getX();
        int deltaZ = first.getZ() - second.getZ();

        return deltaX * deltaX + deltaZ * deltaZ;
    }
}