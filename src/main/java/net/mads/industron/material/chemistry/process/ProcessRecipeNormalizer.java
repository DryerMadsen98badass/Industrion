package net.mads.industron.material.chemistry.process;

import java.util.ArrayList;
import java.util.List;

/**
 * Reduces generated process batches to their smallest exact integer recipe.
 *
 * <p>Item amounts are compared as item counts while fluids/gases are compared as serialized mB.
 * Serialized volume is phase-aware: one LIQUID/MOLTEN material unit is 144 mB and one GAS
 * material unit is 576 mB. For example {@code 4 dust -> 576 mB liquid} has a common divisor of
 * {@code 4} and is emitted as {@code 1 dust -> 144 mB liquid}. A recipe is left untouched when every serialized amount does not share
 * a divisor greater than one.</p>
 *
 * <p>The same divisor is applied to every balanced input/output {@link ProcessMaterial}, so material
 * conservation is preserved. Process identity, tier, temperature, requirements and semantics are not
 * changed. Because {@link ProcessStep#durationTicks()} is amount-derived, constructing the reduced
 * step also reduces duration proportionally to the smaller batch instead of silently changing
 * throughput.</p>
 */
public final class ProcessRecipeNormalizer {
    private ProcessRecipeNormalizer() {
    }

    /** Returns {@code step} unchanged when its emitted amounts are already in lowest terms. */
    public static ProcessStep reduceCommonBatch(ProcessStep step) {
        if (step == null) throw new IllegalArgumentException("step cannot be null");

        int divisor = commonDivisor(step);
        if (divisor <= 1) return step;

        return new ProcessStep(
                step.id(),
                step.kind(),
                divide(step.inputs(), divisor),
                divide(step.outputs(), divisor),
                step.requirements(),
                step.requiredTemperature(),
                step.minimumTierIndex(),
                step.explanation()
        );
    }

    /**
     * Greatest common divisor of the amounts that will actually be serialized into the recipe JSON.
     * Items contribute their stack count; fluids/gases contribute their mB amount.
     */
    public static int commonDivisor(ProcessStep step) {
        long divisor = 0L;
        List<ProcessMaterial> materials = concat(step.inputs(), step.outputs());
        for (ProcessMaterial material : materials) {
            long amount = serializedAmount(material);
            divisor = divisor == 0L ? amount : gcd(divisor, amount);
            if (divisor == 1L) return 1;
        }

        // Every chemistry fluid/gas uses material milli-units internally while serialized volume is
        // phase-aware (144 mB liquid/molten, 576 mB gas per full material unit). Keep the divisor
        // compatible with the internal material amount so volume expansion never changes mass.
        for (ProcessMaterial material : materials) {
            if (material.phase().isFluidLike()) {
                divisor = gcd(divisor, material.milliUnits());
                if (divisor == 1L) return 1;
            }
        }
        return divisor <= 1L ? 1 : Math.toIntExact(divisor);
    }

    private static long serializedAmount(ProcessMaterial material) {
        return material.phase().isFluidLike()
                ? material.milliBucketsExact()
                : material.itemAmountExact();
    }

    private static List<ProcessMaterial> divide(List<ProcessMaterial> materials, int divisor) {
        List<ProcessMaterial> reduced = new ArrayList<>(materials.size());
        for (ProcessMaterial material : materials) {
            long reducedMilliUnits = material.milliUnits() / divisor;
            reduced.add(new ProcessMaterial(
                    material.materialId(),
                    material.phase(),
                    reducedMilliUnits,
                    material.tierIndex(),
                    material.tierName(),
                    material.guaranteed(),
                    material.backingMaterial(),
                    material.part(),
                    material.substanceState(),
                    material.processProperties()
            ));
        }
        return List.copyOf(reduced);
    }

    private static long gcd(long a, long b) {
        a = Math.abs(a);
        b = Math.abs(b);
        while (b != 0L) {
            long next = a % b;
            a = b;
            b = next;
        }
        return a;
    }

    private static List<ProcessMaterial> concat(List<ProcessMaterial> first, List<ProcessMaterial> second) {
        List<ProcessMaterial> result = new ArrayList<>(first.size() + second.size());
        result.addAll(first);
        result.addAll(second);
        return result;
    }
}
