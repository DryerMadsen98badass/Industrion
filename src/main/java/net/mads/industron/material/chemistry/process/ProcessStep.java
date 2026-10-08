package net.mads.industron.material.chemistry.process;

import net.mads.industron.machine.MachineTier;

import java.util.List;
import java.util.Objects;

public record ProcessStep(
        String id,
        ProcessKind kind,
        List<ProcessMaterial> inputs,
        List<ProcessMaterial> outputs,
        List<ProcessRequirement> requirements,
        int requiredTemperature,
        int minimumTierIndex,
        String explanation
) {
    public ProcessStep(
            String id,
            ProcessKind kind,
            List<ProcessMaterial> inputs,
            List<ProcessMaterial> outputs,
            List<ProcessRequirement> requirements,
            String explanation
    ) {
        this(id, kind, inputs, outputs, requirements, 0, 0, explanation);
    }

    /** Backwards-compatible temperature constructor. */
    public ProcessStep(
            String id,
            ProcessKind kind,
            List<ProcessMaterial> inputs,
            List<ProcessMaterial> outputs,
            List<ProcessRequirement> requirements,
            int requiredTemperature,
            String explanation
    ) {
        this(id, kind, inputs, outputs, requirements, requiredTemperature, 0, explanation);
    }

    public ProcessStep {
        id = Objects.requireNonNull(id).trim().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_/.-]+", "_");
        if (id.isEmpty()) throw new IllegalArgumentException("id cannot be blank");
        kind = Objects.requireNonNull(kind);
        inputs = inputs == null ? List.of() : List.copyOf(inputs);
        outputs = outputs == null ? List.of() : List.copyOf(outputs);
        requirements = requirements == null ? List.of() : List.copyOf(requirements);
        requiredTemperature = Math.max(0, requiredTemperature);
        minimumTierIndex = Math.max(0, minimumTierIndex);
        explanation = explanation == null ? "" : explanation;
        if (outputs.isEmpty()) throw new IllegalArgumentException("process step needs at least one guaranteed output");
    }

    public long inputMilliUnits() { return inputs.stream().filter(ProcessMaterial::guaranteed).mapToLong(ProcessMaterial::milliUnits).sum(); }
    public long outputMilliUnits() { return outputs.stream().filter(ProcessMaterial::guaranteed).mapToLong(ProcessMaterial::milliUnits).sum(); }
    public boolean balanced() { return inputMilliUnits() == outputMilliUnits(); }
    public int durationTicks() {
        long base = (Math.multiplyExact(outputMilliUnits(), 600L) + 999L) / 1000L;
        int percent = switch (kind) {
            case MIXING, CENTRIFUGING, MAGNETIC_SEPARATION, FILTRATION -> 75;
            case SMELTING, MELTING, DRYING, EVAPORATION, VAPORIZATION,
                    CONDENSATION, LIQUEFACTION, FREEZING -> 100;
            case DISTILLATION, FRACTIONATION, ELECTROLYSIS, ELECTROREFINING, ELECTROWINNING -> 180;
            case ROASTING, CALCINATION, CHEMICAL_REACTION, DISSOLUTION, LEACHING,
                    SOLVENT_EXTRACTION, POLYMERIZATION, PYROLYSIS, CRACKING, REFORMING -> 150;
            default -> 125;
        };
        return Math.max(1, Math.toIntExact((Math.multiplyExact(base, percent) + 99L) / 100L));
    }
    public int highestOutputTier() { return outputs.stream().filter(ProcessMaterial::guaranteed).mapToInt(ProcessMaterial::tierIndex).max().orElse(0); }
    public int recipeTierIndex() {
        int processFloor = switch (kind) {
            case DISTILLATION, FRACTIONATION, ELECTROLYSIS, ELECTROREFINING, ELECTROWINNING,
                    SOLVENT_EXTRACTION, CRACKING, REFORMING, POLYMERIZATION -> 1;
            default -> 0;
        };
        return Math.max(minimumTierIndex, Math.max(processFloor, highestOutputTier() - 1));
    }
    public String recipeTierName() {
        int index = recipeTierIndex();
        return index < MachineTier.ALL.size() ? MachineTier.ALL.get(index).displayName() : "TIER_" + index;
    }
    public void requireBalanced() {
        if (!balanced()) throw new IllegalStateException("Unbalanced process " + id + ": " + inputMilliUnits() + " != " + outputMilliUnits());
    }
}
