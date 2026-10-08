package net.mads.industron.material.chemistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Deterministic Phase 13 reaction solver for known registered products and decompositions. */
public final class ReactionSolver {
    private ReactionSolver() {
    }

    public static Optional<ReactionPlan> synthesize(MaterialSnapshot product, Map<String, MaterialSnapshot> registry) {
        if (product == null || product.composition().isEmpty()) return Optional.empty();
        long total = directTotal(product);
        List<ReactionParticipant> reactants = new ArrayList<>();
        for (CompositionEntry component : product.composition()) {
            MaterialSnapshot child = registry.get(component.substanceId());
            ChemistryPhase phase = child == null ? component.phase() : child.phase();
            reactants.add(ReactionParticipant.units(component.substanceId(), component.amount(), phase));
        }
        ReactionPlan plan = new ReactionPlan(
                "reaction/synthesize/" + product.id(),
                reactants,
                List.of(ReactionParticipant.units(product.id(), total, product.phase())),
                List.of("form registered substance " + product.id()),
                0,
                synthesisTemperature(product, registry),
                -100,
                100,
                "",
                "Deterministic synthesis plan from direct .contains(...) composition"
        );
        return ReactionBalancer.validate(plan, registry).balanced() ? Optional.of(plan) : Optional.empty();
    }

    public static Optional<ReactionPlan> decompose(MaterialSnapshot source, Map<String, MaterialSnapshot> registry) {
        if (source == null || source.composition().isEmpty()) return Optional.empty();
        long total = directTotal(source);
        List<ReactionParticipant> products = new ArrayList<>();
        for (CompositionEntry component : source.composition()) {
            MaterialSnapshot child = registry.get(component.substanceId());
            ChemistryPhase phase = child == null ? component.phase() : child.phase();
            products.add(ReactionParticipant.units(component.substanceId(), component.amount(), phase));
        }
        ReactionPlan plan = new ReactionPlan(
                "reaction/decompose/" + source.id(),
                List.of(ReactionParticipant.units(source.id(), total, source.phase())),
                products,
                List.of("break registered substance " + source.id()),
                0,
                decompositionTemperature(source, registry),
                -100,
                100,
                "",
                "Deterministic decomposition plan to direct .contains(...) components"
        );
        return ReactionBalancer.validate(plan, registry).balanced() ? Optional.of(plan) : Optional.empty();
    }

    public static List<ReactionPlan> knownTargetReactions(
            List<ReactionParticipant> reactants,
            Map<String, MaterialSnapshot> registry
    ) {
        if (reactants == null || reactants.isEmpty()) return List.of();
        List<ReactionPlan> result = new ArrayList<>();
        for (MaterialSnapshot candidate : registry.values()) {
            if (candidate.composition().isEmpty()) continue;
            Optional<ReactionPlan> synthesis = synthesize(candidate, registry);
            if (synthesis.isEmpty()) continue;
            if (sameParticipants(reactants, synthesis.orElseThrow().reactants())) {
                result.add(synthesis.orElseThrow());
            }
        }
        return List.copyOf(result);
    }

    private static boolean sameParticipants(List<ReactionParticipant> left, List<ReactionParticipant> right) {
        return normalized(left).equals(normalized(right));
    }

    private static Map<String, Long> normalized(List<ReactionParticipant> participants) {
        java.util.Map<String, Long> result = new java.util.TreeMap<>();
        for (ReactionParticipant participant : participants) {
            result.merge(participant.materialId() + "@" + participant.phase(), participant.milliUnits(), Math::addExact);
        }
        return Map.copyOf(result);
    }

    private static long directTotal(MaterialSnapshot snapshot) {
        long total = 0;
        for (CompositionEntry component : snapshot.composition()) {
            total = Math.addExact(total, component.amount());
        }
        return Math.max(1, total);
    }

    private static int synthesisTemperature(MaterialSnapshot product, Map<String, MaterialSnapshot> registry) {
        double bond = product.property("bondstrength");
        double stability = product.property("chemicalstability");
        return (int) Math.round(Math.max(0, bond * 4 + stability * 2));
    }

    private static int decompositionTemperature(MaterialSnapshot source, Map<String, MaterialSnapshot> registry) {
        double bond = source.property("bondstrength");
        double stability = source.property("chemicalstability");
        return (int) Math.round(Math.max(0, 100 + bond * 5 + stability * 3));
    }
}
