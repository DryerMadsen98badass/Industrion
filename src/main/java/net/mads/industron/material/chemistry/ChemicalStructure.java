package net.mads.industron.material.chemistry;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ChemicalStructure {
    public enum Topology {
        ATOMIC,
        DISCRETE_MOLECULE,
        IONIC_LATTICE,
        METALLIC_LATTICE,
        NETWORK,
        POLYMER_CHAIN,
        POLYMER_NETWORK,
        PHYSICAL_MIXTURE,
        UNKNOWN
    }

    private final Topology topology;
    private final Map<String, ChemicalAtom> atoms;
    private final List<ChemicalBond> bonds;
    private final String repeatUnitId;
    private final int repeatCount;
    private final double chainFlexibility;
    private final double crosslinkDensity;

    private ChemicalStructure(Builder builder) {
        topology = Objects.requireNonNull(builder.topology, "topology");
        atoms = Collections.unmodifiableMap(new LinkedHashMap<>(builder.atoms));
        bonds = List.copyOf(builder.bonds);
        repeatUnitId = builder.repeatUnitId;
        repeatCount = builder.repeatCount;
        chainFlexibility = clamp(builder.chainFlexibility);
        crosslinkDensity = clamp(builder.crosslinkDensity);
        validate();
    }

    public static Builder builder(Topology topology) {
        return new Builder(topology);
    }

    public static ChemicalStructure physicalMixture() {
        return builder(Topology.PHYSICAL_MIXTURE).build();
    }

    public Topology topology() { return topology; }
    public Collection<ChemicalAtom> atoms() { return atoms.values(); }
    public List<ChemicalBond> bonds() { return bonds; }
    public String repeatUnitId() { return repeatUnitId; }
    public int repeatCount() { return repeatCount; }
    public double chainFlexibility() { return chainFlexibility; }
    public double crosslinkDensity() { return crosslinkDensity; }

    public int netCharge() {
        return atoms.values().stream().mapToInt(ChemicalAtom::formalCharge).sum();
    }

    public Map<String, Integer> formula() {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (ChemicalAtom atom : atoms.values()) result.merge(atom.elementId(), 1, Integer::sum);
        return Collections.unmodifiableMap(result);
    }

    public boolean isConnected() {
        if (atoms.size() <= 1 || topology == Topology.PHYSICAL_MIXTURE) return true;
        Map<String, Set<String>> graph = new HashMap<>();
        atoms.keySet().forEach(id -> graph.put(id, new HashSet<>()));
        for (ChemicalBond bond : bonds) {
            graph.get(bond.firstAtom()).add(bond.secondAtom());
            graph.get(bond.secondAtom()).add(bond.firstAtom());
        }
        Set<String> seen = new HashSet<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        queue.add(atoms.keySet().iterator().next());
        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            if (!seen.add(current)) continue;
            queue.addAll(graph.get(current));
        }
        return seen.size() == atoms.size();
    }

    private void validate() {
        Set<String> unique = new HashSet<>();
        for (ChemicalBond bond : bonds) {
            if (!atoms.containsKey(bond.firstAtom()) || !atoms.containsKey(bond.secondAtom())) {
                throw new IllegalArgumentException("Bond references an unknown atom: " + bond);
            }
            String key = bond.firstAtom().compareTo(bond.secondAtom()) < 0
                    ? bond.firstAtom() + "|" + bond.secondAtom()
                    : bond.secondAtom() + "|" + bond.firstAtom();
            if (!unique.add(key)) throw new IllegalArgumentException("Duplicate bond between " + key);
        }
        if (topology != Topology.PHYSICAL_MIXTURE && !isConnected()) {
            throw new IllegalArgumentException("Chemical structure is disconnected; use PHYSICAL_MIXTURE when intentional");
        }
        if ((topology == Topology.POLYMER_CHAIN || topology == Topology.POLYMER_NETWORK) && repeatCount < 1) {
            throw new IllegalArgumentException("Polymer structures require repeatCount >= 1");
        }
    }

    private static double clamp(double value) {
        if (!Double.isFinite(value)) return 0;
        return Math.max(0, Math.min(1, value));
    }

    public static final class Builder {
        private final Topology topology;
        private final Map<String, ChemicalAtom> atoms = new LinkedHashMap<>();
        private final List<ChemicalBond> bonds = new ArrayList<>();
        private String repeatUnitId;
        private int repeatCount;
        private double chainFlexibility;
        private double crosslinkDensity;

        private Builder(Topology topology) { this.topology = Objects.requireNonNull(topology); }

        public Builder atom(String atomId, String elementId) {
            return atom(atomId, elementId, 0);
        }

        public Builder atom(String atomId, String elementId, int formalCharge) {
            ChemicalAtom atom = new ChemicalAtom(atomId, elementId, formalCharge);
            if (atoms.putIfAbsent(atom.id(), atom) != null) throw new IllegalArgumentException("Duplicate atom id " + atom.id());
            return this;
        }

        public Builder bond(String first, String second, BondType type, BondOrder order) {
            bonds.add(new ChemicalBond(first, second, type, order));
            return this;
        }

        public Builder repeatUnit(String id, int count) {
            repeatUnitId = Objects.requireNonNull(id).trim().toLowerCase(java.util.Locale.ROOT);
            repeatCount = count;
            return this;
        }

        public Builder chainFlexibility(double value) { chainFlexibility = value; return this; }
        public Builder crosslinkDensity(double value) { crosslinkDensity = value; return this; }
        public ChemicalStructure build() { return new ChemicalStructure(this); }
    }
}
