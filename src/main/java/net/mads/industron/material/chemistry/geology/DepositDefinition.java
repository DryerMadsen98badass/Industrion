package net.mads.industron.material.chemistry.geology;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Declarative geology occurrence. Mineral chemistry remains on the mineral material itself. */
public record DepositDefinition(
        String id,
        String displayName,
        DepositGeometry geometry,
        List<WeightedMineral> primaryMinerals,
        List<WeightedMineral> secondaryMinerals,
        Map<String, Integer> gangueWeights,
        DepositCondition conditions,
        int minimumSize,
        int maximumSize,
        double grade,
        double minimumGrade,
        double maximumGrade,
        double highGradeCoreChance,
        double rarity,
        List<SurfaceIndicator> surfaceIndicators
) {
    public record WeightedMineral(String mineralId, int weight) {
        public WeightedMineral {
            mineralId = Objects.requireNonNull(mineralId, "mineralId").trim().toLowerCase(java.util.Locale.ROOT);
            if (weight <= 0) throw new IllegalArgumentException("weight");
        }
    }

    /** Compatibility constructor for older definition code. Rarity is interpreted as a relative weight. */
    public DepositDefinition(
            String id,
            String displayName,
            DepositGeometry geometry,
            List<WeightedMineral> primaryMinerals,
            List<WeightedMineral> secondaryMinerals,
            Map<String, Integer> gangueWeights,
            DepositCondition conditions,
            int minimumSize,
            int maximumSize,
            double grade,
            double rarity
    ) {
        this(
                id,
                displayName,
                geometry,
                primaryMinerals,
                secondaryMinerals,
                gangueWeights,
                conditions,
                minimumSize,
                maximumSize,
                grade,
                Math.max(0.001D, grade * 0.45D),
                Math.min(1.0D, grade * 1.55D),
                0.08D,
                rarity,
                List.of(SurfaceIndicator.FLOAT_STONE, SurfaceIndicator.ALTERED_ROCK)
        );
    }

    public DepositDefinition {
        id = Objects.requireNonNull(id, "id").trim().toLowerCase(java.util.Locale.ROOT);
        if (id.isBlank()) throw new IllegalArgumentException("blank deposit id");
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        geometry = Objects.requireNonNull(geometry, "geometry");
        primaryMinerals = List.copyOf(primaryMinerals == null ? List.of() : primaryMinerals);
        secondaryMinerals = List.copyOf(secondaryMinerals == null ? List.of() : secondaryMinerals);
        gangueWeights = Map.copyOf(gangueWeights == null ? Map.of() : gangueWeights);
        conditions = Objects.requireNonNull(conditions, "conditions");
        if (primaryMinerals.isEmpty()) throw new IllegalArgumentException("deposit needs a primary mineral");
        if (minimumSize <= 0 || maximumSize < minimumSize) throw new IllegalArgumentException("invalid size");
        grade = clampGrade(grade);
        minimumGrade = clampGrade(minimumGrade);
        maximumGrade = clampGrade(maximumGrade);
        if (minimumGrade > maximumGrade) {
            double temp = minimumGrade;
            minimumGrade = maximumGrade;
            maximumGrade = temp;
        }
        grade = Math.max(minimumGrade, Math.min(maximumGrade, grade));
        highGradeCoreChance = Math.max(0.0D, Math.min(1.0D, highGradeCoreChance));
        if (!Double.isFinite(rarity) || rarity <= 0.0D) throw new IllegalArgumentException("rarity weight must be > 0");
        surfaceIndicators = List.copyOf(surfaceIndicators == null ? List.of() : surfaceIndicators);
    }

    private static double clampGrade(double value) {
        if (!Double.isFinite(value)) return 0.001D;
        return Math.max(0.001D, Math.min(1.0D, value));
    }
}
