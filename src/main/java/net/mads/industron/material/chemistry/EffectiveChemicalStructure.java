package net.mads.industron.material.chemistry;

import java.util.Map;
import java.util.Optional;

/** Single resolver for the explicit-or-generated structure used by all chemistry consumers. */
public final class EffectiveChemicalStructure {
    private EffectiveChemicalStructure() {
    }

    public static Optional<ChemicalStructure> resolve(
            MaterialSnapshot snapshot,
            Map<String, MaterialSnapshot> registry
    ) {
        if (snapshot == null) return Optional.empty();
        if (snapshot.structure().isPresent()) return snapshot.structure();
        return ChemicalStructureGenerator.generate(snapshot, registry == null ? Map.of() : registry);
    }

    /** Returns the same snapshot when no structure can be generated, otherwise a copy carrying it. */
    public static MaterialSnapshot apply(
            MaterialSnapshot snapshot,
            Map<String, MaterialSnapshot> registry
    ) {
        if (snapshot == null || snapshot.structure().isPresent()) return snapshot;
        Optional<ChemicalStructure> structure = resolve(snapshot, registry);
        if (structure.isEmpty()) return snapshot;
        return new MaterialSnapshot(
                snapshot.id(),
                snapshot.displayName(),
                snapshot.color(),
                snapshot.phase(),
                snapshot.composition(),
                structure,
                snapshot.properties(),
                snapshot.classifications(),
                snapshot.tierIndex(),
                snapshot.tierName(),
                snapshot.sources(),
                snapshot.backingMaterial()
        );
    }
}
