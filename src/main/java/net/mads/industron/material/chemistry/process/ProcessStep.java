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
        this(id, kind, inputs, outputs, requirements, 0, explanation);
    }

    public ProcessStep {
        id = Objects.requireNonNull(id).trim().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_/.-]+", "_");
        if (id.isEmpty()) throw new IllegalArgumentException("id cannot be blank");
        kind = Objects.requireNonNull(kind);
        inputs = inputs == null ? List.of() : List.copyOf(inputs);
        outputs = outputs == null ? List.of() : List.copyOf(outputs);
        requirements = requirements == null ? List.of() : List.copyOf(requirements);
        requiredTemperature = Math.max(0, requiredTemperature);
        explanation = explanation == null ? "" : explanation;
        if (outputs.isEmpty()) throw new IllegalArgumentException("process step needs at least one guaranteed output");
    }

    public long inputMilliUnits() { return inputs.stream().filter(ProcessMaterial::guaranteed).mapToLong(ProcessMaterial::milliUnits).sum(); }
    public long outputMilliUnits() { return outputs.stream().filter(ProcessMaterial::guaranteed).mapToLong(ProcessMaterial::milliUnits).sum(); }
    public boolean balanced() { return inputMilliUnits() == outputMilliUnits(); }
    public int durationTicks() { return Math.max(1, Math.toIntExact((Math.multiplyExact(outputMilliUnits(), 600L) + 999L) / 1000L)); }
    public int highestOutputTier() { return outputs.stream().filter(ProcessMaterial::guaranteed).mapToInt(ProcessMaterial::tierIndex).max().orElse(0); }
    public int recipeTierIndex() { return Math.max(0, highestOutputTier() - 1); }
    public String recipeTierName() {
        int index = recipeTierIndex();
        return index < MachineTier.ALL.size() ? MachineTier.ALL.get(index).displayName() : "TIER_" + index;
    }
    public void requireBalanced() {
        if (!balanced()) throw new IllegalStateException("Unbalanced process " + id + ": " + inputMilliUnits() + " != " + outputMilliUnits());
    }
}
