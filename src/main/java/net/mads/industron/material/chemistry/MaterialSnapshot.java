package net.mads.industron.material.chemistry;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record MaterialSnapshot(
        String id,
        String displayName,
        int color,
        ChemistryPhase phase,
        List<CompositionEntry> composition,
        Optional<ChemicalStructure> structure,
        Map<String, Double> properties,
        Set<MaterialClassification> classifications,
        int tierIndex,
        String tierName,
        Set<MaterialSource> sources,
        Object backingMaterial
) {
    public MaterialSnapshot {
        id = normalize(id);
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        phase = phase == null ? ChemistryPhase.UNKNOWN : phase;
        composition = composition == null ? List.of() : List.copyOf(composition);
        structure = structure == null ? Optional.empty() : structure;
        properties = properties == null ? Map.of() : Map.copyOf(properties);
        classifications = classifications == null ? Set.of() : Set.copyOf(classifications);
        tierIndex = Math.max(0, tierIndex);
        tierName = tierName == null || tierName.isBlank() ? "ULV" : tierName;
        sources = sources == null ? Set.of() : Set.copyOf(sources);
    }

    public double property(String name) {
        return properties.getOrDefault(normalize(name), 0.0);
    }

    public boolean hasSource(MaterialSourceType type) {
        return sources.stream().anyMatch(source -> source.type() == type);
    }

    public boolean naturallyAcquired() {
        return hasSource(MaterialSourceType.WORLD_DEPOSIT)
                || hasSource(MaterialSourceType.NATURAL_FLUID_DEPOSIT)
                || hasSource(MaterialSourceType.NATURAL_GAS_DEPOSIT)
                || hasSource(MaterialSourceType.STRUCTURE_LOOT)
                || hasSource(MaterialSourceType.BIOLOGICAL_EXTRACTION)
                || hasSource(MaterialSourceType.EXTERNAL_MAPPING);
    }

    private static String normalize(String value) {
        Objects.requireNonNull(value, "value");
        String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.isEmpty()) throw new IllegalArgumentException("value cannot be blank");
        return normalized;
    }
}
