package net.mads.industron.energy;

import net.mads.industron.debug.CEPerformanceProfiler;
import net.mads.industron.machine.MachinePortBlockEntity;
import net.mads.industron.machine.SingleBlockMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.WeakHashMap;

public final class CEEnergyNetwork {
    private static final Map<Level, LevelRouteCache> ROUTE_CACHES = new WeakHashMap<>();

    private CEEnergyNetwork() {
    }

    public static void invalidate(Level level) {
        if (level == null || level.isClientSide()) {
            return;
        }
        LevelRouteCache cache = ROUTE_CACHES.get(level);
        if (cache != null) {
            cache.clear();
        }
    }

    public static long acceptFromWire(Level level, BlockPos sourceWirePos, Direction sourceSide, long voltage, long amperage) {
        if (level == null || level.isClientSide() || voltage <= 0 || amperage <= 0) {
            return 0;
        }

        long usedAmps = 0L;
        for (EnergyRoutePath path : cachedRoutes(level, sourceWirePos, sourceSide)) {
            if (usedAmps >= amperage) {
                break;
            }

            BlockEntity targetBlockEntity = level.getBlockEntity(path.targetPos);
            CEEnergyContainer destination = energyContainer(targetBlockEntity);
            if (destination == null || !destination.inputsEnergy(path.targetSide) || destination.getEnergyCanBeInserted() <= 0) {
                continue;
            }

            long pathVoltage = voltage;
            for (BlockPos wirePos : path.wires) {
                if (level.getBlockEntity(wirePos) instanceof EnergyWireBlockEntity wire
                        && voltage > wire.maxVoltage()) {
                    wire.applyOverVoltage(voltage);
                    pathVoltage = Math.min(pathVoltage, wire.maxVoltage());
                }
            }

            long accepted = destination.acceptEnergyFromNetwork(path.targetSide, pathVoltage, amperage - usedAmps);
            if (accepted <= 0) {
                continue;
            }

            usedAmps += accepted;
            if (targetBlockEntity instanceof MachinePortBlockEntity port) {
                port.recordEnergyNetworkInput(saturatedMultiply(accepted, pathVoltage), pathVoltage);
            }
            for (BlockPos wirePos : path.wires) {
                if (level.getBlockEntity(wirePos) instanceof EnergyWireBlockEntity wire) {
                    wire.incrementAmperage(accepted, voltage);
                }
            }
        }
        return usedAmps;
    }

    public static long outputToAdjacentWires(Level level, BlockPos sourcePos, CEEnergyContainer source) {
        long profileStart = CEPerformanceProfiler.begin(level);
        try {
            return outputToAdjacentWiresInner(level, sourcePos, source);
        } finally {
            CEPerformanceProfiler.record(CEPerformanceProfiler.Metric.WIRE_NETWORK, profileStart);
        }
    }

    private static long outputToAdjacentWiresInner(Level level, BlockPos sourcePos, CEEnergyContainer source) {
        if (level == null || level.isClientSide() || source == null || source.getOutputVoltage() <= 0 || source.getOutputAmperage() <= 0) {
            return 0;
        }

        long remainingAmps = Math.min(source.getEnergyStored() / source.getOutputVoltage(), source.getOutputAmperage());
        long usedAmps = 0L;
        long voltage = source.getOutputVoltage();
        for (Direction direction : Direction.values()) {
            if (remainingAmps <= 0) {
                break;
            }
            if (!source.outputsEnergy(direction)) {
                continue;
            }

            BlockPos targetPos = sourcePos.relative(direction);
            BlockState targetState = level.getBlockState(targetPos);
            long canExtract = Math.min(remainingAmps, source.extract(saturatedMultiply(remainingAmps, voltage), true) / voltage);
            if (canExtract <= 0) {
                continue;
            }

            long accepted;
            if (targetState.getBlock() instanceof EnergyWireBlock
                    && EnergyWireBlock.hasEnabledConnection(level, targetPos, targetState, direction.getOpposite())) {
                accepted = acceptFromWire(level, targetPos, direction.getOpposite(), voltage, canExtract);
            } else {
                CEEnergyContainer destination = energyContainer(level.getBlockEntity(targetPos));
                Direction targetSide = direction.getOpposite();
                accepted = destination != null && destination.inputsEnergy(targetSide)
                        ? destination.acceptEnergyFromNetwork(targetSide, voltage, canExtract)
                        : 0L;
            }
            if (accepted <= 0) {
                continue;
            }

            if (!(targetState.getBlock() instanceof EnergyWireBlock)
                    && level.getBlockEntity(targetPos) instanceof MachinePortBlockEntity port) {
                port.recordEnergyNetworkInput(saturatedMultiply(accepted, voltage), voltage);
            }
            source.extract(saturatedMultiply(accepted, voltage), false);
            usedAmps += accepted;
            remainingAmps -= accepted;
        }
        return usedAmps;
    }

    private static List<EnergyRoutePath> cachedRoutes(Level level, BlockPos start, Direction sourceSide) {
        LevelRouteCache cache = ROUTE_CACHES.computeIfAbsent(level, ignored -> new LevelRouteCache());
        RouteKey key = new RouteKey(start.asLong(), sourceSide);
        List<EnergyRoutePath> cached = cache.routes.get(key);
        if (cached != null) {
            return cached;
        }

        List<EnergyRoutePath> routes = routes(level, start, sourceSide);
        cache.routes.put(key, routes);
        return routes;
    }

    private static List<EnergyRoutePath> routes(Level level, BlockPos start, Direction sourceSide) {
        List<EnergyRoutePath> routes = new ArrayList<>();
        Queue<PathNode> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        queue.add(new PathNode(start, List.of(start)));
        seen.add(start);

        while (!queue.isEmpty()) {
            PathNode node = queue.remove();
            BlockState state = level.getBlockState(node.pos);
            if (!(state.getBlock() instanceof EnergyWireBlock)) {
                continue;
            }

            for (Direction direction : Direction.values()) {
                if (node.pos.equals(start) && direction == sourceSide) {
                    continue;
                }
                if (!EnergyWireBlock.hasEnabledConnection(level, node.pos, state, direction)) {
                    continue;
                }

                BlockPos nextPos = node.pos.relative(direction);
                BlockState nextState = level.getBlockState(nextPos);
                if (nextState.getBlock() instanceof EnergyWireBlock && EnergyWireBlock.wiresConnect(level, node.pos, state, direction, nextPos, nextState)) {
                    if (seen.add(nextPos)) {
                        List<BlockPos> path = new ArrayList<>(node.wires);
                        path.add(nextPos);
                        queue.add(new PathNode(nextPos, path));
                    }
                    continue;
                }

                CEEnergyContainer container = energyContainer(level.getBlockEntity(nextPos));
                Direction targetSide = direction.getOpposite();
                if (container != null && container.inputsEnergy(targetSide)) {
                    routes.add(new EnergyRoutePath(nextPos, targetSide, node.wires));
                }
            }
        }

        routes.sort(java.util.Comparator.comparingInt(path -> path.wires.size()));
        return routes;
    }

    private static CEEnergyContainer energyContainer(BlockEntity blockEntity) {
        if (blockEntity instanceof MachinePortBlockEntity port) {
            return port.ceContainer();
        }
        if (blockEntity instanceof CreativeEnergyBlockEntity creative) {
            return creative.ceContainer();
        }
        if (blockEntity instanceof SingleBlockMachineBlockEntity machine) {
            return machine.ceContainer();
        }
        return null;
    }

    private record PathNode(BlockPos pos, List<BlockPos> wires) {
    }

    private record EnergyRoutePath(BlockPos targetPos, Direction targetSide, List<BlockPos> wires) {
    }

    private record RouteKey(long startPos, Direction sourceSide) {
    }

    private static final class LevelRouteCache {
        private final Map<RouteKey, List<EnergyRoutePath>> routes = new HashMap<>();

        private void clear() {
            routes.clear();
        }
    }

    private static long saturatedMultiply(long first, long second) {
        if (first > 0L && second > Long.MAX_VALUE / first) {
            return Long.MAX_VALUE;
        }
        return first * second;
    }
}
