package net.mads.industron.material.chemistry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Deterministic Phase 10 structure generator for small formula-sized materials.
 *
 * <p>This does not try to enumerate every isomer. It provides one stable, validateable default
 * structure when the composition is simple enough to be represented as an atom graph.</p>
 */
public final class ChemicalStructureGenerator {
    private static final int MAX_AUTOMATIC_ATOMS = 64;

    private ChemicalStructureGenerator() {
    }

    public static Optional<ChemicalStructure> generate(MaterialSnapshot material, Map<String, MaterialSnapshot> registry) {
        if (material == null) return Optional.empty();
        if (material.structure().isPresent()) return material.structure();
        Map<String, MaterialSnapshot> safeRegistry = registry == null ? Map.of() : registry;
        if (material.composition().isEmpty()) {
            return Optional.of(ChemicalStructure.builder(ChemicalStructure.Topology.ATOMIC)
                    .atom(material.id() + "_1", material.id(), 0)
                    .build());
        }

        try {
            ChemicalStructure.Topology topology = ChemicalTopologyResolver.resolve(material, safeRegistry);
            if (topology == ChemicalStructure.Topology.PHYSICAL_MIXTURE || topology == ChemicalStructure.Topology.UNKNOWN) {
                return Optional.empty();
            }

            CompositionResolver resolver = new CompositionResolver(safeRegistry);
            Optional<ChemicalFormula> formula = resolver.atomic(material)
                    .flatMap(vector -> ChemicalFormula.stoichiometricFromAtomicVector(vector, 0));
            if (formula.isEmpty()) return Optional.empty();
            int atomCount = formula.orElseThrow().atoms().values().stream().mapToInt(Integer::intValue).sum();
            if (atomCount <= 0 || atomCount > MAX_AUTOMATIC_ATOMS) return Optional.empty();

            ChemicalStructure.Builder builder = ChemicalStructure.builder(topology);
            Map<String, Integer> chargeBudget = formalCharges(topology, material, safeRegistry);
            java.util.List<String> atomIds = new java.util.ArrayList<>();
            for (Map.Entry<String, Integer> entry : formula.orElseThrow().atoms().entrySet()) {
                int charge = chargeBudget.getOrDefault(entry.getKey(), 0);
                for (int i = 1; i <= entry.getValue(); i++) {
                    String atomId = entry.getKey() + "_" + i;
                    atomIds.add(atomId);
                    builder.atom(atomId, entry.getKey(), charge);
                }
            }

            BondType bondType = switch (topology) {
                case IONIC_LATTICE -> BondType.IONIC;
                case METALLIC_LATTICE -> BondType.METALLIC;
                default -> BondType.COVALENT;
            };
            BondOrder order = switch (topology) {
                case IONIC_LATTICE -> BondOrder.IONIC;
                case METALLIC_LATTICE -> BondOrder.METALLIC;
                default -> BondOrder.SINGLE;
            };
            for (int i = 1; i < atomIds.size(); i++) {
                builder.bond(atomIds.get(i - 1), atomIds.get(i), bondType, order);
            }

            ChemicalStructure structure = builder.build();
            return ChemicalStructureValidator.validate(
                    new MaterialSnapshot(
                            material.id(), material.displayName(), material.color(), material.phase(),
                            material.composition(), Optional.of(structure), material.properties(),
                            material.classifications(), material.tierIndex(), material.tierName(),
                            material.sources(), material.backingMaterial()
                    ),
                    safeRegistry
            ).valid() ? Optional.of(structure) : Optional.empty();
        } catch (RuntimeException ignored) {
            // During material bootstrap, generated/bridge classes may reference materials before
            // the registry has finished building. Treat that as "no generated structure yet".
            return Optional.empty();
        }
    }

    private static Map<String, Integer> formalCharges(
            ChemicalStructure.Topology topology,
            MaterialSnapshot material,
            Map<String, MaterialSnapshot> registry
    ) {
        if (topology != ChemicalStructure.Topology.IONIC_LATTICE) return Map.of();
        Map<String, Integer> result = new LinkedHashMap<>();
        for (CompositionEntry component : material.composition()) {
            MaterialSnapshot child = registry.get(component.substanceId());
            if (child == null) continue;
            int charge = (int) Math.round(child.property("preferredioncharge"));
            if (charge != 0) result.put(child.id(), charge);
        }
        return Map.copyOf(result);
    }
}
