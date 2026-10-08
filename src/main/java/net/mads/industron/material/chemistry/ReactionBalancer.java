package net.mads.industron.material.chemistry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Phase 13 atom/charge conservation validator for reaction plans. */
public final class ReactionBalancer {
    public record Result(boolean balanced, String problem) {
    }

    private ReactionBalancer() {
    }

    public static Result validate(ReactionPlan plan, Map<String, MaterialSnapshot> registry) {
        CompositionResolver resolver = new CompositionResolver(registry);
        try {
            Map<String, CompositionAmount> reactants = vector(plan.reactants(), resolver);
            Map<String, CompositionAmount> products = vector(plan.products(), resolver);
            if (!reactants.equals(products)) {
                return new Result(false, "reaction changes conserved composition: reactants="
                        + reactants + ", products=" + products);
            }
            int chargeIn = charge(plan.reactants(), registry);
            int chargeOut = charge(plan.products(), registry);
            if (chargeIn != chargeOut) {
                return new Result(false, "reaction changes net charge: " + chargeIn + " -> " + chargeOut);
            }
            return new Result(true, "");
        } catch (RuntimeException error) {
            return new Result(false, error.getMessage());
        }
    }

    private static Map<String, CompositionAmount> vector(
            List<ReactionParticipant> participants,
            CompositionResolver resolver
    ) {
        Map<String, CompositionAmount> result = new LinkedHashMap<>();
        for (ReactionParticipant participant : participants) {
            CompositionVector unit = resolver.conserved(participant.materialId());
            CompositionAmount amount = CompositionAmount.of(participant.milliUnits());
            unit.entries().forEach((id, value) -> result.merge(id, value.multiply(amount), CompositionAmount::add));
        }
        result.entrySet().removeIf(entry -> entry.getValue().isZero());
        return Map.copyOf(result);
    }

    private static int charge(List<ReactionParticipant> participants, Map<String, MaterialSnapshot> registry) {
        int total = 0;
        for (ReactionParticipant participant : participants) {
            MaterialSnapshot snapshot = registry.get(participant.materialId());
            if (snapshot == null) continue;
            var structure = EffectiveChemicalStructure.resolve(snapshot, registry);
            if (structure.isEmpty()) continue;
            long units = participant.milliUnits() / 1000L;
            if (participant.milliUnits() % 1000L != 0L) {
                throw new IllegalStateException("charged reaction participant must use whole material units: " + participant.materialId());
            }
            total = Math.toIntExact(total + structure.orElseThrow().netCharge() * units);
        }
        return total;
    }
}
