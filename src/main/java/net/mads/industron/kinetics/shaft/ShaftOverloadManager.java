package net.mads.industron.kinetics.shaft;

import net.mads.industron.Industron;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Event-driven shaft failure tracking. No kinetic network is scanned every tick.
 * Metal shafts (which currently have no finite limit) never enter this manager.
 */
@EventBusSubscriber(modid = Industron.MOD_ID)
public final class ShaftOverloadManager {
    private static final int FAILURE_INTERVAL_TICKS = 20;
    private static final Map<ServerLevel, LevelState> LEVELS = new IdentityHashMap<>();

    private ShaftOverloadManager() {
    }

    public static void track(MaterialShaftBlockEntity shaft) {
        if (!(shaft.getLevel() instanceof ServerLevel level)) {
            return;
        }

        ShaftLimits limits = shaft.shaftLimits();
        Long networkId = shaft.network;
        if (!limits.hasAnyLimit() || networkId == null || shaft.isRemoved()) {
            unregister(shaft);
            return;
        }

        LEVELS.computeIfAbsent(level, ignored -> new LevelState())
                .track(shaft, networkId, limits);
    }

    public static void updateNetworkStress(MaterialShaftBlockEntity shaft, float currentStress) {
        if (!(shaft.getLevel() instanceof ServerLevel level)) {
            return;
        }
        track(shaft);
        LevelState levelState = LEVELS.get(level);
        if (levelState == null || shaft.network == null) {
            return;
        }
        NetworkState network = levelState.networks.get(shaft.network);
        if (network != null) {
            network.currentStress = currentStress;
        }
    }

    public static void unregister(MaterialShaftBlockEntity shaft) {
        if (!(shaft.getLevel() instanceof ServerLevel level)) {
            return;
        }
        LevelState state = LEVELS.get(level);
        if (state == null) {
            return;
        }
        state.remove(shaft.getBlockPos());
        if (state.isEmpty()) {
            LEVELS.remove(level);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (level.getGameTime() % FAILURE_INTERVAL_TICKS != 0) {
            return;
        }

        LevelState state = LEVELS.get(level);
        if (state == null) {
            return;
        }
        state.breakOneOverloadedShaftPerNetwork(level);
        if (state.isEmpty()) {
            LEVELS.remove(level);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            LEVELS.remove(level);
        }
    }

    private static final class LevelState {
        private final Map<Long, NetworkState> networks = new HashMap<>();
        private final Map<BlockPos, TrackedShaft> tracked = new HashMap<>();

        void track(MaterialShaftBlockEntity shaft, long networkId, ShaftLimits limits) {
            BlockPos pos = shaft.getBlockPos().immutable();
            TrackedShaft desired = new TrackedShaft(networkId, limits.maxRpm(), limits.maxNetworkStress());
            TrackedShaft previous = tracked.get(pos);

            if (!Objects.equals(previous, desired)) {
                if (previous != null) {
                    removeFromNetwork(pos, previous);
                }
                tracked.put(pos, desired);
                NetworkState network = networks.computeIfAbsent(networkId, ignored -> new NetworkState());
                network.allLimited.add(pos);
                if (Float.isFinite(limits.maxNetworkStress())) {
                    network.stressLimited
                            .computeIfAbsent(limits.maxNetworkStress(), ignored -> new RandomizedPositionSet())
                            .add(pos);
                }
            }

            NetworkState network = networks.computeIfAbsent(networkId, ignored -> new NetworkState());
            network.currentStress = shaft.currentNetworkStress();
            if (limits.isRpmExceeded(shaft.getTheoreticalSpeed())) {
                network.overSpeed.add(pos);
            } else {
                network.overSpeed.remove(pos);
            }
        }

        void remove(BlockPos pos) {
            TrackedShaft previous = tracked.remove(pos);
            if (previous != null) {
                removeFromNetwork(pos, previous);
            }
        }

        boolean isEmpty() {
            return tracked.isEmpty();
        }

        void breakOneOverloadedShaftPerNetwork(ServerLevel level) {
            // A break can immediately make Create split/rebuild the kinetic network and call back
            // into this manager, so iterate over a stable copy of the network ids.
            List<Long> ids = new ArrayList<>(networks.keySet());
            for (Long networkId : ids) {
                NetworkState network = networks.get(networkId);
                if (network == null || network.allLimited.isEmpty()) {
                    networks.remove(networkId);
                    continue;
                }

                FailureKind kind;
                BlockPos candidate;
                if (!network.overSpeed.isEmpty()) {
                    kind = FailureKind.RPM;
                    candidate = network.overSpeed.random(level.random);
                } else {
                    kind = FailureKind.NETWORK_STRESS;
                    candidate = network.randomStressOverload(level.random);
                }

                if (candidate == null) {
                    continue;
                }

                candidate = validateCandidate(level, networkId, candidate, kind);
                if (candidate == null) {
                    continue;
                }

                // Remove first so synchronous block-entity removal cannot leave this candidate in
                // the random pools. Create will then rebuild/split the kinetic network normally.
                remove(candidate);
                level.destroyBlock(candidate, true);
            }
        }

        private BlockPos validateCandidate(
                ServerLevel level,
                long networkId,
                BlockPos candidate,
                FailureKind kind
        ) {
            for (int attempts = 0; attempts < 8 && candidate != null; attempts++) {
                BlockEntity blockEntity = level.getBlockEntity(candidate);
                if (blockEntity instanceof MaterialShaftBlockEntity shaft
                        && shaft.network != null
                        && shaft.network == networkId
                        && isStillOverloaded(shaft, kind)) {
                    return candidate;
                }

                remove(candidate);
                NetworkState network = networks.get(networkId);
                if (network == null) {
                    return null;
                }
                candidate = kind == FailureKind.RPM
                        ? network.overSpeed.random(level.random)
                        : network.randomStressOverload(level.random);
            }
            return null;
        }

        private boolean isStillOverloaded(MaterialShaftBlockEntity shaft, FailureKind kind) {
            ShaftLimits limits = shaft.shaftLimits();
            if (kind == FailureKind.RPM) {
                return limits.isRpmExceeded(shaft.getTheoreticalSpeed());
            }
            NetworkState network = shaft.network == null ? null : networks.get(shaft.network);
            return network != null && limits.isNetworkStressExceeded(network.currentStress);
        }

        private void removeFromNetwork(BlockPos pos, TrackedShaft trackedShaft) {
            NetworkState network = networks.get(trackedShaft.networkId());
            if (network == null) {
                return;
            }
            network.allLimited.remove(pos);
            network.overSpeed.remove(pos);
            if (Float.isFinite(trackedShaft.maxNetworkStress())) {
                RandomizedPositionSet bucket = network.stressLimited.get(trackedShaft.maxNetworkStress());
                if (bucket != null) {
                    bucket.remove(pos);
                    if (bucket.isEmpty()) {
                        network.stressLimited.remove(trackedShaft.maxNetworkStress());
                    }
                }
            }
            if (network.allLimited.isEmpty()) {
                networks.remove(trackedShaft.networkId());
            }
        }
    }

    private static final class NetworkState {
        private final RandomizedPositionSet allLimited = new RandomizedPositionSet();
        private final RandomizedPositionSet overSpeed = new RandomizedPositionSet();
        private final Map<Float, RandomizedPositionSet> stressLimited = new LinkedHashMap<>();
        private float currentStress;

        BlockPos randomStressOverload(RandomSource random) {
            int totalCandidates = 0;
            for (Map.Entry<Float, RandomizedPositionSet> entry : stressLimited.entrySet()) {
                if (currentStress > entry.getKey()) {
                    totalCandidates += entry.getValue().size();
                }
            }
            if (totalCandidates <= 0) {
                return null;
            }

            int index = random.nextInt(totalCandidates);
            for (Map.Entry<Float, RandomizedPositionSet> entry : stressLimited.entrySet()) {
                if (currentStress <= entry.getKey()) {
                    continue;
                }
                RandomizedPositionSet positions = entry.getValue();
                if (index < positions.size()) {
                    return positions.get(index);
                }
                index -= positions.size();
            }
            return null;
        }
    }

    /** O(1) add/remove and O(1) random selection. */
    private static final class RandomizedPositionSet {
        private final List<BlockPos> values = new ArrayList<>();
        private final Map<BlockPos, Integer> indices = new HashMap<>();

        void add(BlockPos pos) {
            BlockPos immutable = pos.immutable();
            if (indices.containsKey(immutable)) {
                return;
            }
            indices.put(immutable, values.size());
            values.add(immutable);
        }

        void remove(BlockPos pos) {
            Integer removedIndex = indices.remove(pos);
            if (removedIndex == null) {
                return;
            }
            int lastIndex = values.size() - 1;
            BlockPos last = values.remove(lastIndex);
            if (removedIndex != lastIndex) {
                values.set(removedIndex, last);
                indices.put(last, removedIndex);
            }
        }

        boolean isEmpty() {
            return values.isEmpty();
        }

        int size() {
            return values.size();
        }

        BlockPos get(int index) {
            return values.get(index);
        }

        BlockPos random(RandomSource random) {
            return values.isEmpty() ? null : values.get(random.nextInt(values.size()));
        }
    }

    private record TrackedShaft(long networkId, float maxRpm, float maxNetworkStress) {
    }

    private enum FailureKind {
        RPM,
        NETWORK_STRESS
    }
}
