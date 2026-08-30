package net.mads.industron.material.chemistry.geology;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Runtime geology view of a fictional ore mineral.
 *
 * <p>The mineral itself is an {@code IndustrialMaterial}; this record only adds geology/process
 * metadata derived from the registered material. It deliberately does not use real-world labels
 * such as oxide/sulfide/salt. Those names are not part of Industron's fictional chemistry.</p>
 */
public record OreMineral(
        String id,
        String displayName,
        String materialId,
        Map<String, Integer> recoverableMaterials,
        Set<String> gangueMaterialIds,
        boolean magnetic,
        double processingDifficulty
) {
    public OreMineral {
        id = normalize(id);
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        materialId = normalize(materialId);
        recoverableMaterials = Map.copyOf(recoverableMaterials == null ? Map.of() : recoverableMaterials);
        gangueMaterialIds = Set.copyOf(gangueMaterialIds == null ? Set.of() : gangueMaterialIds);
        processingDifficulty = Math.max(0.0D, Math.min(100.0D, processingDifficulty));
    }

    private static String normalize(String value) {
        Objects.requireNonNull(value, "value");
        String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.isBlank()) throw new IllegalArgumentException("blank id");
        return normalized;
    }
}
