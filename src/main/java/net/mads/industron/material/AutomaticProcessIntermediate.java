package net.mads.industron.material;

import net.mads.industron.material.chemistry.ChemistryPhase;
import net.mads.industron.material.chemistry.MaterialAnalysis;
import net.mads.industron.material.chemistry.MaterialSnapshot;
import net.mads.industron.material.chemistry.MaterialSource;
import net.mads.industron.material.chemistry.MaterialSourceType;
import net.mads.industron.material.structure.WoodMaterial;

import java.util.Map;
import java.util.Optional;

/** Stable identity convention for system-owned chemistry intermediates. */
public final class AutomaticProcessIntermediate {
    /** Legacy source id kept for already-existing dust-processing intermediates. */
    public static final String SOURCE_PREFIX = "automatic_dust_processing:";
    /** General source id for intermediates that are not specific to compound DUST processing. */
    public static final String GENERAL_SOURCE_PREFIX = "automatic_processing:";

    public enum Kind {
        ROASTED_DUST("roasted_dust", "Roasted", ChemistryPhase.SOLID, MaterialPart.DUST, false),
        SLURRY("slurry", "Slurry", ChemistryPhase.LIQUID, MaterialPart.LIQUID, true),
        SOLUTION("solution", "Solution", ChemistryPhase.LIQUID, MaterialPart.LIQUID, true),
        REACTION_MIXTURE("reaction_mixture", "Reaction Mixture", ChemistryPhase.LIQUID, MaterialPart.LIQUID, true),
        PYROLYSATE("pyrolysate", "Pyrolysate", ChemistryPhase.LIQUID, MaterialPart.LIQUID, true);

        private final String suffix;
        private final String displaySuffix;
        private final ChemistryPhase phase;
        private final MaterialPart part;
        private final boolean physicalMixture;

        Kind(String suffix, String displaySuffix, ChemistryPhase phase, MaterialPart part, boolean physicalMixture) {
            this.suffix = suffix;
            this.displaySuffix = displaySuffix;
            this.phase = phase;
            this.part = part;
            this.physicalMixture = physicalMixture;
        }

        public String materialId(IndustrialSubstance parent) {
            return parent.id() + "_" + suffix;
        }

        public String displayName(IndustrialSubstance parent) {
            // Preserve the legacy "Roasted <material>" wording for the existing compound-DUST
            // graph, while structure-material intermediates use the source-first naming contract.
            if (this == ROASTED_DUST && !(parent instanceof WoodMaterial)) {
                return displaySuffix + " " + parent.displayName();
            }
            return parent.displayName() + " " + displaySuffix;
        }

        public String sourceId(IndustrialSubstance parent) {
            // Keep the old source id only for the existing IndustrialMaterial compound-DUST graph.
            // Structure-material chemistry (currently wood) is generic automatic processing, even
            // when it happens to use a slurry/solution stage with the same shared Kind.
            String prefix = parent instanceof WoodMaterial || this == PYROLYSATE
                    ? GENERAL_SOURCE_PREFIX
                    : SOURCE_PREFIX;
            return prefix + parent.id() + ":" + suffix;
        }

        public ChemistryPhase phase() {
            return phase;
        }

        public MaterialPart part() {
            return part;
        }

        public boolean physicalMixture() {
            return physicalMixture;
        }
    }

    private AutomaticProcessIntermediate() {
    }

    public static boolean isAutomatic(MaterialSnapshot material) {
        return material.sources().stream().anyMatch(source ->
                source.type() == MaterialSourceType.PROCESS_OUTPUT
                        && (source.id().startsWith(SOURCE_PREFIX)
                        || source.id().startsWith(GENERAL_SOURCE_PREFIX))
        );
    }

    public static boolean isDustProcessingIntermediate(MaterialSnapshot material) {
        return material.sources().stream().anyMatch(source ->
                source.type() == MaterialSourceType.PROCESS_OUTPUT
                        && source.id().startsWith(SOURCE_PREFIX)
        );
    }

    public static Optional<MaterialAnalysis> find(
            IndustrialSubstance parent,
            Kind kind,
            Map<String, MaterialAnalysis> registry
    ) {
        String sourceId = kind.sourceId(parent);
        return registry.values().stream()
                .filter(candidate -> candidate.source().sources().stream().anyMatch(source -> matches(source, sourceId)))
                .sorted(java.util.Comparator.comparing(candidate -> candidate.source().id()))
                .findFirst();
    }

    private static boolean matches(MaterialSource source, String expectedId) {
        return source.type() == MaterialSourceType.PROCESS_OUTPUT && source.id().equals(expectedId);
    }
}
