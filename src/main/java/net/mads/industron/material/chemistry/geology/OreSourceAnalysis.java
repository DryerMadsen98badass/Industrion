package net.mads.industron.material.chemistry.geology;

import net.mads.industron.material.MaterialOrePolicy;
import net.mads.industron.material.chemistry.ChemicalStructure;

import java.util.List;
import java.util.Objects;

/** Source/geology decision for one elemental material. */
public record OreSourceAnalysis(
        String materialId,
        String displayName,
        int atomicNumber,
        String tierName,
        MaterialOrePolicy.DimensionBand dimensionBand,
        Status status,
        SourceKind sourceKind,
        String detail,
        String coveringMineralId,
        ChemicalStructure.Topology suggestedTopology,
        String suggestedCompanionId,
        int suggestedTargetAmount,
        int suggestedCompanionAmount,
        String suggestedContains,
        SuggestedGeology suggestedGeology
) {
    public enum Status {
        COVERED,
        NO_ORE_REQUIRED,
        MISSING_ORE_MINERAL,
        BLOCKED
    }

    public enum SourceKind {
        DEDICATED_ORE,
        STONE_TRACE,
        EXTERNAL_MAPPING,
        ATMOSPHERIC_GAS,
        NATURAL_FLUID_RESERVOIR,
        NATURAL_GAS_RESERVOIR,
        RADIOACTIVE_DECAY,
        REACTOR_PRODUCTION,
        PROCESS_BYPRODUCT,
        SYNTHETIC_ONLY,
        MISSING
    }

    /** Projected geology for an anonymous report suggestion; it is not registered worldgen. */
    public record SuggestedGeology(
            String tierName,
            MaterialOrePolicy.DimensionBand dimensionBand,
            DepositGeometry geometry,
            int minimumY,
            int maximumY,
            int preferredY,
            List<String> biomeTags,
            List<String> hostBlocks
    ) {
        public SuggestedGeology {
            tierName = tierName == null || tierName.isBlank() ? "ULV" : tierName;
            dimensionBand = Objects.requireNonNull(dimensionBand, "dimensionBand");
            geometry = Objects.requireNonNullElse(geometry, DepositGeometry.VEIN);
            if (minimumY > maximumY) throw new IllegalArgumentException("minimumY > maximumY");
            preferredY = Math.max(minimumY, Math.min(maximumY, preferredY));
            biomeTags = biomeTags == null ? List.of() : List.copyOf(biomeTags);
            hostBlocks = hostBlocks == null ? List.of() : List.copyOf(hostBlocks);
        }
    }

    public OreSourceAnalysis {
        materialId = normalize(materialId);
        displayName = displayName == null || displayName.isBlank() ? materialId : displayName;
        atomicNumber = Math.max(0, atomicNumber);
        tierName = tierName == null || tierName.isBlank() ? "ULV" : tierName;
        dimensionBand = Objects.requireNonNull(dimensionBand, "dimensionBand");
        status = Objects.requireNonNull(status, "status");
        sourceKind = Objects.requireNonNull(sourceKind, "sourceKind");
        detail = detail == null ? "" : detail;
        coveringMineralId = coveringMineralId == null ? "" : normalizeNullable(coveringMineralId);
        suggestedTopology = suggestedTopology == null ? ChemicalStructure.Topology.UNKNOWN : suggestedTopology;
        suggestedCompanionId = suggestedCompanionId == null ? "" : normalizeNullable(suggestedCompanionId);
        suggestedTargetAmount = Math.max(0, suggestedTargetAmount);
        suggestedCompanionAmount = Math.max(0, suggestedCompanionAmount);
        suggestedContains = suggestedContains == null ? "" : suggestedContains;
    }

    public boolean missing() {
        return status == Status.MISSING_ORE_MINERAL || status == Status.BLOCKED;
    }

    private static String normalize(String value) {
        Objects.requireNonNull(value, "value");
        String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.isBlank()) throw new IllegalArgumentException("blank material id");
        return normalized;
    }

    private static String normalizeNullable(String value) {
        String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);
        return normalized.isBlank() ? "" : normalized;
    }
}
