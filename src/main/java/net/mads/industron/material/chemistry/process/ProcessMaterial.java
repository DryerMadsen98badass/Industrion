package net.mads.industron.material.chemistry.process;

import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.chemistry.ChemistryPhase;

import java.util.Objects;

public record ProcessMaterial(
        String materialId,
        ChemistryPhase phase,
        long milliUnits,
        int tierIndex,
        String tierName,
        boolean guaranteed,
        Object backingMaterial,
        MaterialPart part
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
        this(materialId, phase, milliUnits, tierIndex, tierName, guaranteed, backingMaterial, null);
    }

    public ProcessMaterial {
        materialId = Objects.requireNonNull(materialId).trim().toLowerCase(java.util.Locale.ROOT);
        phase = phase == null ? ChemistryPhase.UNKNOWN : phase;
        if (milliUnits <= 0) throw new IllegalArgumentException("milliUnits must be positive");
        tierIndex = Math.max(0, tierIndex);
        tierName = tierName == null ? "ULV" : tierName;
        if (part != null && part.isFluid() != phase.isFluidLike()) {
            throw new IllegalArgumentException("Material part/phase mismatch for " + materialId + ": " + part + " / " + phase);
        }
    }

    public static ProcessMaterial item(String id, int amount, int tierIndex, String tierName, Object backing) {
        return new ProcessMaterial(id, ChemistryPhase.SOLID, Math.multiplyExact(amount, 1000L), tierIndex, tierName, true, backing, null);
    }

    public static ProcessMaterial item(String id, int amount, int tierIndex, String tierName, Object backing, MaterialPart part) {
        return new ProcessMaterial(id, ChemistryPhase.SOLID, Math.multiplyExact(amount, 1000L), tierIndex, tierName, true, backing, part);
    }

    public static ProcessMaterial fluid(String id, ChemistryPhase phase, long milliBuckets, int tierIndex, String tierName, Object backing) {
        return new ProcessMaterial(id, phase, milliBuckets, tierIndex, tierName, true, backing, null);
    }

    public static ProcessMaterial fluid(String id, ChemistryPhase phase, long milliBuckets, int tierIndex, String tierName, Object backing, MaterialPart part) {
        return new ProcessMaterial(id, phase, milliBuckets, tierIndex, tierName, true, backing, part);
    }

    public double units() { return milliUnits / 1000.0; }

    public int itemAmountExact() {
        if (phase.isFluidLike() || milliUnits % 1000 != 0) throw new IllegalStateException("Not an exact item amount");
        return Math.toIntExact(milliUnits / 1000);
    }

    public int milliBucketsExact() {
        if (!phase.isFluidLike()) throw new IllegalStateException("Not a fluid/gas amount");
        return Math.toIntExact(milliUnits);
    }
}
