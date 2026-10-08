package net.mads.industron.material.chemistry.process;

import net.mads.industron.material.AutomaticProcessIntermediate;
import net.mads.industron.material.chemistry.ChemicalStructure;
import net.mads.industron.material.chemistry.ChemistryPhase;
import net.mads.industron.material.chemistry.CompositionEntry;
import net.mads.industron.material.chemistry.MaterialAnalysis;
import net.mads.industron.material.chemistry.MaterialClassification;
import net.mads.industron.material.chemistry.MaterialSource;
import net.mads.industron.material.chemistry.MaterialSourceType;

import java.util.Locale;
import java.util.Objects;

/**
 * Process-time state of a substance, separate from its coarse phase.
 *
 * <p>This lets registered materials and future runtime Foundry payloads represent the difference
 * between a powder blend, solution, suspension, phase-separated bath, molten physical mixture and
 * an actually bonded substance/alloy.</p>
 */
public record ProcessSubstanceState(
        ChemicalStructure.Topology topology,
        MixtureKind mixtureKind
) {
    public enum MixtureKind {
        NONE,
        POWDER_MIXTURE,
        LIQUID_MIXTURE,
        SOLUTION,
        SUSPENSION,
        PHASE_SEPARATED,
        MOLTEN_MIXTURE,
        GAS_MIXTURE,
        REACTION_MIXTURE,
        UNKNOWN
    }

    public ProcessSubstanceState {
        topology = topology == null ? ChemicalStructure.Topology.UNKNOWN : topology;
        mixtureKind = mixtureKind == null ? MixtureKind.UNKNOWN : mixtureKind;
        if (topology != ChemicalStructure.Topology.PHYSICAL_MIXTURE
                && mixtureKind != MixtureKind.NONE
                && mixtureKind != MixtureKind.UNKNOWN) {
            throw new IllegalArgumentException("Only PHYSICAL_MIXTURE may use mixture kind " + mixtureKind);
        }
    }

    public static ProcessSubstanceState unknown() {
        return new ProcessSubstanceState(ChemicalStructure.Topology.UNKNOWN, MixtureKind.UNKNOWN);
    }

    public static ProcessSubstanceState bonded(ChemicalStructure.Topology topology) {
        Objects.requireNonNull(topology, "topology");
        if (topology == ChemicalStructure.Topology.PHYSICAL_MIXTURE) {
            throw new IllegalArgumentException("Use physicalMixture(...) for PHYSICAL_MIXTURE");
        }
        return new ProcessSubstanceState(topology, MixtureKind.NONE);
    }

    public static ProcessSubstanceState physicalMixture(MixtureKind kind) {
        if (kind == null || kind == MixtureKind.NONE || kind == MixtureKind.UNKNOWN) {
            throw new IllegalArgumentException("Physical mixture requires a concrete mixture kind");
        }
        return new ProcessSubstanceState(ChemicalStructure.Topology.PHYSICAL_MIXTURE, kind);
    }

    public boolean isPhysicalMixture() {
        return topology == ChemicalStructure.Topology.PHYSICAL_MIXTURE;
    }

    public boolean isBondedSubstance() {
        return switch (topology) {
            case DISCRETE_MOLECULE, IONIC_LATTICE, METALLIC_LATTICE, NETWORK,
                    POLYMER_CHAIN, POLYMER_NETWORK -> true;
            default -> false;
        };
    }

    public static ProcessSubstanceState fromAnalysis(MaterialAnalysis analysis) {
        ChemicalStructure.Topology topology = topologyOf(analysis);
        if (topology != ChemicalStructure.Topology.PHYSICAL_MIXTURE) {
            return new ProcessSubstanceState(topology, MixtureKind.NONE);
        }

        MixtureKind automatic = automaticIntermediateKind(analysis);
        if (automatic != null) return physicalMixture(automatic);

        ChemistryPhase phase = analysis.phase();
        if (phase == ChemistryPhase.MOLTEN) return physicalMixture(MixtureKind.MOLTEN_MIXTURE);
        if (phase == ChemistryPhase.GAS || phase == ChemistryPhase.PLASMA) return physicalMixture(MixtureKind.GAS_MIXTURE);
        if (phase == ChemistryPhase.MIXED) return physicalMixture(MixtureKind.PHASE_SEPARATED);
        if (phase == ChemistryPhase.SOLID) return physicalMixture(MixtureKind.POWDER_MIXTURE);
        if (phase == ChemistryPhase.LIQUID) {
            boolean hasSolid = false;
            boolean hasFluid = false;
            for (CompositionEntry component : analysis.source().composition()) {
                if (component.phase() == ChemistryPhase.SOLID) hasSolid = true;
                if (component.phase().isFluidLike()) hasFluid = true;
            }
            if (hasSolid && hasFluid) return physicalMixture(MixtureKind.SUSPENSION);
            return physicalMixture(MixtureKind.LIQUID_MIXTURE);
        }
        return new ProcessSubstanceState(topology, MixtureKind.UNKNOWN);
    }

    public static ChemicalStructure.Topology topologyOf(MaterialAnalysis analysis) {
        if (analysis.source().structure().isPresent()) {
            return analysis.source().structure().orElseThrow().topology();
        }
        if (analysis.classifications().contains(MaterialClassification.PHYSICAL_MIXTURE)) {
            return ChemicalStructure.Topology.PHYSICAL_MIXTURE;
        }
        if (analysis.classifications().contains(MaterialClassification.ALLOY)) {
            return ChemicalStructure.Topology.METALLIC_LATTICE;
        }
        if (analysis.classifications().contains(MaterialClassification.IONIC_COMPOUND)) {
            return ChemicalStructure.Topology.IONIC_LATTICE;
        }
        if (analysis.classifications().contains(MaterialClassification.POLYMER)) {
            return ChemicalStructure.Topology.POLYMER_NETWORK;
        }
        if (analysis.source().composition().isEmpty()) {
            return ChemicalStructure.Topology.ATOMIC;
        }
        return ChemicalStructure.Topology.NETWORK;
    }

    private static MixtureKind automaticIntermediateKind(MaterialAnalysis analysis) {
        for (MaterialSource source : analysis.source().sources()) {
            if (source.type() != MaterialSourceType.PROCESS_OUTPUT
                    || !(source.id().startsWith(AutomaticProcessIntermediate.SOURCE_PREFIX)
                    || source.id().startsWith(AutomaticProcessIntermediate.GENERAL_SOURCE_PREFIX))) {
                continue;
            }
            String normalized = source.id().toLowerCase(Locale.ROOT);
            if (normalized.endsWith(":solution")) return MixtureKind.SOLUTION;
            if (normalized.endsWith(":slurry")) return MixtureKind.SUSPENSION;
            if (normalized.endsWith(":reaction_mixture")) return MixtureKind.REACTION_MIXTURE;
            if (normalized.endsWith(":pyrolysate")) return MixtureKind.LIQUID_MIXTURE;
        }
        return null;
    }
}
