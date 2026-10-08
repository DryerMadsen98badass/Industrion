package net.mads.industron.runtime;

import java.util.*;

/** No world objects. Graph edges retain Direction.ordinal order and stable BFS destination order. */
public record EnergyTopologySnapshot(long revision, long start, Map<Long, Node> nodes) {
    public EnergyTopologySnapshot { nodes = Map.copyOf(nodes); }
    public record Edge(long wire, long target, int side, boolean endpoint) {}
    public record Node(List<Edge> edges) { public Node { edges = List.copyOf(edges); } }
    public record Route(long target, int side, List<Long> wires) { public Route { wires = List.copyOf(wires); } }
    public record Result(long revision, List<Route> routes) { public Result { routes = List.copyOf(routes); } }
    public Result routes() {
        List<Route> routes = new ArrayList<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        Map<Long, Long> parents = new HashMap<>();
        parents.put(start, start); queue.add(start);
        while (!queue.isEmpty() && !Thread.currentThread().isInterrupted()) {
            long pos = queue.remove();
            Node node = nodes.get(pos);
            if (node == null) continue;
            for (Edge edge : node.edges) {
                if (edge.endpoint) {
                    List<Long> path = new ArrayList<>();
                    long cursor = pos;
                    while (cursor != start) { path.add(cursor); cursor = parents.get(cursor); }
                    path.add(start); Collections.reverse(path);
                    routes.add(new Route(edge.target, edge.side, path));
                } else if (!parents.containsKey(edge.wire)) {
                    parents.put(edge.wire, pos); queue.add(edge.wire);
                }
            }
        }
        routes.sort(Comparator.comparingInt(route -> route.wires.size()));
        return new Result(revision, routes);
    }
}
