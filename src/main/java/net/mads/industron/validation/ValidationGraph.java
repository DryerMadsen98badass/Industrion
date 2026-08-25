package net.mads.industron.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/** Generic identity-based cycle checker reusable by composition, component and assembly graphs. */
public final class ValidationGraph {
    private ValidationGraph() {
    }

    public static <T> void detectCycles(
            Collection<T> roots,
            Function<T, String> name,
            Function<T, ? extends Collection<T>> dependencies,
            ValidationSubsystem subsystem,
            ValidationCode cycleCode,
            ValidationCollector diagnostics
    ) {
        Objects.requireNonNull(roots, "roots");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(dependencies, "dependencies");
        Objects.requireNonNull(subsystem, "subsystem");
        Objects.requireNonNull(cycleCode, "cycleCode");
        Objects.requireNonNull(diagnostics, "diagnostics");

        Map<T, State> state = new IdentityHashMap<>();
        List<T> path = new ArrayList<>();
        for (T root : roots) {
            if (root != null && state.get(root) == null) {
                visit(root, name, dependencies, subsystem, cycleCode, diagnostics, state, path);
            }
        }
    }


    public static <T> void detectUnknownReferences(
            Collection<T> knownNodes,
            Function<T, String> name,
            Function<T, ? extends Collection<T>> dependencies,
            ValidationSubsystem subsystem,
            ValidationCode referenceCode,
            ValidationCollector diagnostics
    ) {
        Objects.requireNonNull(knownNodes, "knownNodes");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(dependencies, "dependencies");
        Objects.requireNonNull(subsystem, "subsystem");
        Objects.requireNonNull(referenceCode, "referenceCode");
        Objects.requireNonNull(diagnostics, "diagnostics");

        Map<T, Boolean> known = new IdentityHashMap<>();
        for (T node : knownNodes) {
            if (node != null) known.put(node, Boolean.TRUE);
        }

        for (T node : knownNodes) {
            if (node == null) continue;
            Collection<T> nextNodes = dependencies.apply(node);
            if (nextNodes == null) continue;
            for (T next : nextNodes) {
                if (next == null || known.containsKey(next)) continue;
                diagnostics.error(
                        subsystem,
                        referenceCode,
                        safeName(node, name),
                        "Unknown dependency reference " + safeName(next, name)
                );
            }
        }
    }

    private static <T> void visit(
            T node,
            Function<T, String> name,
            Function<T, ? extends Collection<T>> dependencies,
            ValidationSubsystem subsystem,
            ValidationCode cycleCode,
            ValidationCollector diagnostics,
            Map<T, State> state,
            List<T> path
    ) {
        state.put(node, State.VISITING);
        path.add(node);

        Collection<T> nextNodes = dependencies.apply(node);
        if (nextNodes != null) {
            for (T next : nextNodes) {
                if (next == null) continue;
                State nextState = state.get(next);
                if (nextState == State.VISITING) {
                    int start = identityIndexOf(path, next);
                    List<String> names = new ArrayList<>();
                    for (int i = Math.max(0, start); i < path.size(); i++) {
                        names.add(safeName(path.get(i), name));
                    }
                    names.add(safeName(next, name));
                    diagnostics.error(
                            subsystem,
                            cycleCode,
                            safeName(node, name),
                            "Cycle detected: " + String.join(" -> ", names)
                    );
                    continue;
                }
                if (nextState == null) {
                    visit(next, name, dependencies, subsystem, cycleCode, diagnostics, state, path);
                }
            }
        }

        path.remove(path.size() - 1);
        state.put(node, State.VISITED);
    }

    private static <T> int identityIndexOf(List<T> values, T target) {
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i) == target) return i;
        }
        return -1;
    }

    private static <T> String safeName(T value, Function<T, String> name) {
        String result = name.apply(value);
        return result == null || result.isBlank() ? value.getClass().getSimpleName() : result;
    }

    private enum State {
        VISITING,
        VISITED
    }
}
