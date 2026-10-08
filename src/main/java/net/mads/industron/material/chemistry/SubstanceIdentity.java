package net.mads.industron.material.chemistry;

import java.util.Map;
import java.util.Optional;

/** Canonical identity data for one underlying substance, independent of item/fluid form. */
public record SubstanceIdentity(
        String materialId,
        CompositionVector directComposition,
        CompositionVector conservedComposition,
        Optional<CompositionVector> atomicComposition,
        Optional<String> structureSignature,
        ChemistryPhase phase
) {
    public SubstanceIdentity {
        if (materialId == null || materialId.isBlank()) throw new IllegalArgumentException("materialId cannot be blank");
        materialId = materialId.trim().toLowerCase(java.util.Locale.ROOT);
        directComposition = directComposition == null ? CompositionVector.EMPTY : directComposition;
        conservedComposition = conservedComposition == null ? CompositionVector.EMPTY : conservedComposition;
        atomicComposition = atomicComposition == null ? Optional.empty() : atomicComposition;
        structureSignature = structureSignature == null ? Optional.empty() : structureSignature;
        phase = phase == null ? ChemistryPhase.UNKNOWN : phase;
    }

    public static SubstanceIdentity of(MaterialSnapshot snapshot, Map<String, MaterialSnapshot> registry) {
        CompositionResolver resolver = new CompositionResolver(registry);
        return new SubstanceIdentity(
                snapshot.id(),
                resolver.direct(snapshot),
                resolver.conserved(snapshot),
                resolver.atomic(snapshot),
                EffectiveChemicalStructure.resolve(snapshot, registry).map(ChemicalStructure::canonicalSignature),
                snapshot.phase()
        );
    }

    public String signature() {
        return "id=" + materialId
                + "|direct=" + directComposition.signature()
                + "|conserved=" + conservedComposition.signature()
                + "|atomic=" + atomicComposition.map(CompositionVector::signature).orElse("unknown")
                + "|structure=" + structureSignature.orElse("none")
                + "|phase=" + phase.name();
    }
}
