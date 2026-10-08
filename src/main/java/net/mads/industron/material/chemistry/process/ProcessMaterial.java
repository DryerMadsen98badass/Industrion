package net.mads.industron.material.chemistry.process;

import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.MaterialUnits;
import net.mads.industron.material.chemistry.ChemistryPhase;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public record ProcessMaterial(
        String materialId,
        ChemistryPhase phase,
        long milliUnits,
        int tierIndex,
        String tierName,
        boolean guaranteed,
        Object backingMaterial,
        MaterialPart part,
        ProcessSubstanceState substanceState,
        Map<String, Double> processProperties
) {
    public ProcessMaterial(
            String materialId,
            ChemistryPhase phase,
            long milliUnits,
            int tierIndex,
            String tierName,
            boolean guaranteed,
            Object backingMaterial
    ) {
        this(materialId, phase, milliUnits, tierIndex, tierName, guaranteed, backingMaterial, null,
                ProcessSubstanceState.unknown(), Map.of());
    }

    public ProcessMaterial(
            String materialId,
            ChemistryPhase phase,
            long milliUnits,
            int tierIndex,
            String tierName,
            boolean guaranteed,
            Object backingMaterial,
            MaterialPart part
    ) {
        this(materialId, phase, milliUnits, tierIndex, tierName, guaranteed, backingMaterial, part,
                ProcessSubstanceState.unknown(), Map.of());
    }

    public ProcessMaterial(
            String materialId,
            ChemistryPhase phase,
            long milliUnits,
            int tierIndex,
            String tierName,
            boolean guaranteed,
            Object backingMaterial,
            MaterialPart part,
            ProcessSubstanceState substanceState
    ) {
        this(materialId, phase, milliUnits, tierIndex, tierName, guaranteed, backingMaterial, part,
                substanceState, Map.of());
    }

    public ProcessMaterial {
        materialId = Objects.requireNonNull(materialId).trim().toLowerCase(Locale.ROOT);
        phase = phase == null ? ChemistryPhase.UNKNOWN : phase;
        if (milliUnits <= 0) throw new IllegalArgumentException("milliUnits must be positive");
        tierIndex = Math.max(0, tierIndex);
        tierName = tierName == null ? "ULV" : tierName;
        substanceState = substanceState == null ? ProcessSubstanceState.unknown() : substanceState;
        processProperties = normalizeProperties(processProperties);
        if (part != null && part.isFluid() != phase.isFluidLike()) {
            throw new IllegalArgumentException("Material part/phase mismatch for " + materialId + ": " + part + " / " + phase);
        }
    }

    public static ProcessMaterial item(String id, int amount, int tierIndex, String tierName, Object backing) {
        return new ProcessMaterial(id, ChemistryPhase.SOLID, Math.multiplyExact(amount, 1000L), tierIndex, tierName, true, backing, null,
                ProcessSubstanceState.unknown(), Map.of());
    }

    public static ProcessMaterial item(String id, int amount, int tierIndex, String tierName, Object backing, MaterialPart part) {
        return new ProcessMaterial(id, ChemistryPhase.SOLID, Math.multiplyExact(amount, 1000L), tierIndex, tierName, true, backing, part,
                ProcessSubstanceState.unknown(), Map.of());
    }

    public static ProcessMaterial item(
            String id,
            int amount,
            int tierIndex,
            String tierName,
            Object backing,
            MaterialPart part,
            ProcessSubstanceState state
    ) {
        return new ProcessMaterial(id, ChemistryPhase.SOLID, Math.multiplyExact(amount, 1000L), tierIndex, tierName, true, backing, part, state, Map.of());
    }

    public static ProcessMaterial fluid(String id, ChemistryPhase phase, long milliBuckets, int tierIndex, String tierName, Object backing) {
        return new ProcessMaterial(id, phase, storedFluidAmount(phase, milliBuckets), tierIndex, tierName, true, backing, null,
                ProcessSubstanceState.unknown(), Map.of());
    }

    public static ProcessMaterial fluid(String id, ChemistryPhase phase, long milliBuckets, int tierIndex, String tierName, Object backing, MaterialPart part) {
        return new ProcessMaterial(id, phase, storedFluidAmount(phase, milliBuckets), tierIndex, tierName, true, backing, part,
                ProcessSubstanceState.unknown(), Map.of());
    }

    public static ProcessMaterial fluid(
            String id,
            ChemistryPhase phase,
            long milliBuckets,
            int tierIndex,
            String tierName,
            Object backing,
            MaterialPart part,
            ProcessSubstanceState state
    ) {
        return new ProcessMaterial(id, phase, storedFluidAmount(phase, milliBuckets), tierIndex, tierName, true, backing, part, state, Map.of());
    }

    private static long storedFluidAmount(ChemistryPhase phase, long milliBuckets) {
        // Material mass is stored in milli-units independent of phase. Serialized volume is
        // phase-aware: one LIQUID/MOLTEN unit = 144 mB while one GAS unit = 576 mB.
        return MaterialUnits.millibucketsToMilliUnits(milliBuckets, phase);
    }

    public boolean isPhysicalMixture() {
        return substanceState.isPhysicalMixture();
    }

    public boolean isBondedSubstance() {
        return substanceState.isBondedSubstance();
    }

    /** Returns a calculated/process property, or NaN when that property is unavailable. */
    public double processProperty(String id) {
        if (id == null) return Double.NaN;
        return processProperties.getOrDefault(id.trim().toLowerCase(Locale.ROOT), Double.NaN);
    }

    public double units() { return milliUnits / 1000.0; }

    public int itemAmountExact() {
        if (phase.isFluidLike() || milliUnits % 1000 != 0) throw new IllegalStateException("Not an exact item amount");
        return Math.toIntExact(milliUnits / 1000);
    }

    public int milliBucketsExact() {
        if (!phase.isFluidLike()) throw new IllegalStateException("Not a fluid/gas amount");
        return MaterialUnits.milliUnitsToMillibuckets(milliUnits, phase);
    }

    private static Map<String, Double> normalizeProperties(Map<String, Double> input) {
        if (input == null || input.isEmpty()) return Map.of();
        Map<String, Double> result = new LinkedHashMap<>();
        input.forEach((key, value) -> {
            if (key == null || value == null || !Double.isFinite(value)) return;
            result.put(key.trim().toLowerCase(Locale.ROOT), value);
        });
        return Map.copyOf(result);
    }
}
