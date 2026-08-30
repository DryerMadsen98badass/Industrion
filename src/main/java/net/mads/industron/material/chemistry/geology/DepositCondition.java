package net.mads.industron.material.chemistry.geology;

import java.util.Set;

public record DepositCondition(
        Set<String> dimensions,
        Set<String> biomeTags,
        Set<String> hostRockTags,
        Set<String> nearbyStructureTags,
        Set<TerrainProfile> terrainProfiles,
        int minimumY,
        int maximumY,
        int preferredY,
        int verticalSpread
) {
    public DepositCondition(
            Set<String> dimensions,
            Set<String> biomeTags,
            Set<String> hostRockTags,
            Set<String> nearbyStructureTags,
            int minimumY,
            int maximumY
    ) {
        this(
                dimensions,
                biomeTags,
                hostRockTags,
                nearbyStructureTags,
                Set.of(TerrainProfile.ANY),
                minimumY,
                maximumY,
                minimumY + Math.floorDiv(maximumY - minimumY, 2),
                Math.max(1, Math.floorDiv(maximumY - minimumY, 3))
        );
    }

    public DepositCondition {
        dimensions = dimensions == null ? Set.of() : Set.copyOf(dimensions);
        biomeTags = biomeTags == null ? Set.of() : Set.copyOf(biomeTags);
        hostRockTags = hostRockTags == null ? Set.of() : Set.copyOf(hostRockTags);
        nearbyStructureTags = nearbyStructureTags == null ? Set.of() : Set.copyOf(nearbyStructureTags);
        terrainProfiles = terrainProfiles == null || terrainProfiles.isEmpty()
                ? Set.of(TerrainProfile.ANY)
                : Set.copyOf(terrainProfiles);
        if (minimumY > maximumY) throw new IllegalArgumentException("minimumY > maximumY");
        preferredY = Math.max(minimumY, Math.min(maximumY, preferredY));
        verticalSpread = Math.max(1, verticalSpread);
    }
}
