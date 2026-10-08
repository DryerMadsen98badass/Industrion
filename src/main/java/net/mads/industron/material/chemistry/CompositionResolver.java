package net.mads.industron.material.chemistry;

import net.mads.industron.material.ElementDefinition;
import net.mads.industron.material.IndustrialMaterial;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/** Cycle-safe Phase 8 resolver for direct, conserved and atomic composition. */
public final class CompositionResolver {
    private final Map<String, MaterialSnapshot> registry;

    public CompositionResolver(Map<String, MaterialSnapshot> registry) {
        this.registry = registry == null ? Map.of() : Map.copyOf(registry);
    }

    public CompositionVector direct(MaterialSnapshot snapshot) {
        return snapshot == null ? CompositionVector.EMPTY : CompositionVector.direct(snapshot.composition());
    }

    public CompositionVector conserved(String materialId) {
        return conserved(normalize(materialId), new LinkedHashSet<>());
    }

    public CompositionVector conserved(MaterialSnapshot snapshot) {
        if (snapshot == null) return CompositionVector.EMPTY;
        return conserved(snapshot.id(), new LinkedHashSet<>());
    }

    public Optional<CompositionVector> atomic(String materialId) {
        return atomic(normalize(materialId), new LinkedHashSet<>());
    }

    public Optional<CompositionVector> atomic(MaterialSnapshot snapshot) {
        if (snapshot == null) return Optional.empty();
        return atomic(snapshot.id(), new LinkedHashSet<>());
    }

    private CompositionVector conserved(String id, Set<String> stack) {
        MaterialSnapshot snapshot = require(id);
        if (!stack.add(id)) throw new IllegalStateException("composition cycle at " + id);
        try {
            if (snapshot.composition().isEmpty()) {
                return new CompositionVector(Map.of(id, CompositionAmount.ONE));
            }

            BigInteger total = total(snapshot);
            CompositionVector result = CompositionVector.EMPTY;
            for (CompositionEntry component : snapshot.composition()) {
                CompositionVector child = conserved(component.substanceId(), stack);
                CompositionAmount weight = new CompositionAmount(BigInteger.valueOf(component.amount()), total);
                result = result.add(child.multiply(weight));
            }
            return result;
        } finally {
            stack.remove(id);
        }
    }

    private Optional<CompositionVector> atomic(String id, Set<String> stack) {
        MaterialSnapshot snapshot = require(id);
        if (isAtomicLeaf(snapshot)) {
            return Optional.of(new CompositionVector(Map.of(snapshot.id(), CompositionAmount.ONE)));
        }
        if (snapshot.composition().isEmpty()) return Optional.empty();
        if (!stack.add(id)) throw new IllegalStateException("composition cycle at " + id);
        try {
            BigInteger total = total(snapshot);
            CompositionVector result = CompositionVector.EMPTY;
            for (CompositionEntry component : snapshot.composition()) {
                Optional<CompositionVector> child = atomic(component.substanceId(), stack);
                if (child.isEmpty()) return Optional.empty();
                CompositionAmount weight = new CompositionAmount(BigInteger.valueOf(component.amount()), total);
                result = result.add(child.orElseThrow().multiply(weight));
            }
            return Optional.of(result);
        } finally {
            stack.remove(id);
        }
    }

    public Map<String, String> diagnostics() {
        Map<String, String> problems = new LinkedHashMap<>();
        for (String id : new TreeMap<>(registry).keySet()) {
            try {
                conserved(id);
            } catch (RuntimeException error) {
                problems.put(id, error.getMessage());
            }
        }
        return Map.copyOf(problems);
    }

    private MaterialSnapshot require(String id) {
        MaterialSnapshot snapshot = registry.get(normalize(id));
        if (snapshot == null) throw new IllegalStateException("unregistered material " + id);
        return snapshot;
    }

    private static BigInteger total(MaterialSnapshot snapshot) {
        BigInteger total = BigInteger.ZERO;
        for (CompositionEntry component : snapshot.composition()) {
            if (component.amount() <= 0) {
                throw new IllegalStateException("non-positive .contains(...) amount in " + snapshot.id());
            }
            total = total.add(BigInteger.valueOf(component.amount()));
        }
        if (total.signum() <= 0) throw new IllegalStateException("empty normalized composition for " + snapshot.id());
        return total;
    }

    private static boolean isAtomicLeaf(MaterialSnapshot snapshot) {
        Object backing = snapshot.backingMaterial();
        if (backing instanceof IndustrialMaterial material && material.atomicNumber() > 0) return true;
        if (backing instanceof ElementDefinition element && element.atomicNumber() > 0) return true;
        return snapshot.composition().isEmpty() && snapshot.classifications().contains(MaterialClassification.ELEMENT);
    }

    private static String normalize(String value) {
        if (value == null) throw new IllegalArgumentException("material id cannot be null");
        String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.isEmpty()) throw new IllegalArgumentException("material id cannot be blank");
        return normalized;
    }
}
