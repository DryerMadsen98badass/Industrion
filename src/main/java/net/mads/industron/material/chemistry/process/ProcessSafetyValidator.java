package net.mads.industron.material.chemistry.process;

import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.structure.StructureMaterial;
import net.mads.industron.material.chemistry.ChemistryDiagnostic;
import net.mads.industron.material.chemistry.ChemistryStatus;
import net.mads.industron.material.chemistry.CompositionAmount;
import net.mads.industron.material.chemistry.CompositionResolver;
import net.mads.industron.material.chemistry.CompositionVector;
import net.mads.industron.material.chemistry.MaterialAnalysis;
import net.mads.industron.material.chemistry.MaterialSnapshot;
import net.mads.industron.material.chemistry.ReactionPlan;
import net.mads.industron.material.chemistry.ReactionParticipant;
import net.mads.industron.material.chemistry.ReactionSolver;
import net.mads.industron.recipe.RecipeTypeDefinition;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Atom-exact mass and directed-cycle validation performed before recipe registration. */
public final class ProcessSafetyValidator {
    public record Result(List<ProcessPlan> plans, List<ChemistryDiagnostic> diagnostics) {
        public Result {
            plans = List.copyOf(plans);
            diagnostics = List.copyOf(diagnostics);
        }
    }

    public Result validate(List<ProcessPlan> candidates, Map<String, MaterialAnalysis> registry) {
        List<ProcessPlan> balanced = new ArrayList<>();
        List<ChemistryDiagnostic> diagnostics = new ArrayList<>();

        for (ProcessPlan plan : candidates) {
            String problem = conservationProblem(plan, registry);
            if (problem == null) {
                balanced.add(plan);
            } else {
                diagnostics.add(blocked(plan, "PROCESS_MASS_BALANCE", problem));
            }
        }

        Set<String> cyclicPlans = cyclicPlans(balanced, registry);
        if (cyclicPlans.isEmpty()) return new Result(balanced, diagnostics);

        List<ProcessPlan> acyclic = new ArrayList<>();
        for (ProcessPlan plan : balanced) {
            if (!cyclicPlans.contains(plan.id())) {
                acyclic.add(plan);
                continue;
            }
            diagnostics.add(blocked(
                    plan,
                    "PROCESS_GRAPH_CYCLE",
                    "The generated route participates in a directed material/form cycle. The whole plan was blocked before emission."
            ));
        }
        return new Result(acyclic, diagnostics);
    }

    private static String conservationProblem(ProcessPlan plan, Map<String, MaterialAnalysis> registry) {
        String continuity = continuityProblem(plan);
        if (continuity != null) return continuity;

        for (ProcessStep step : plan.steps()) {
            java.util.Optional<String> semanticProblem = ProcessSemantics.validate(step);
            if (semanticProblem.isPresent()) {
                return "Step " + step.id() + " is physically invalid: " + semanticProblem.orElseThrow() + ".";
            }
            ProcessRecipeResolver.Result processResolution = new ProcessRecipeResolver()
                    .resolveExact(step.kind(), ProcessIntent.fromStep(step));
            if (processResolution.resolution().isEmpty()) {
                return "Step " + step.id() + " has no valid exact RecipeType/process mapping: "
                        + processResolution.diagnostic();
            }
            RecipeTypeDefinition recipeType = processResolution.resolution().orElseThrow().recipeType();
            String ioProblem = ioProblem(step, recipeType);
            if (ioProblem != null) return "Step " + step.id() + " cannot be emitted: " + ioProblem;
            if (recipeType.requiresCoilTemperature() && step.requiredTemperature() <= 0) {
                return "Step " + step.id() + " uses coil-heated " + recipeType.id()
                        + " without a positive temperature.";
            }
            if (step.outputs().stream().anyMatch(output -> !output.guaranteed())) {
                return "Step " + step.id() + " contains a non-guaranteed chemistry output; chance outputs are not allowed in balanced chemistry routes.";
            }
            try {
                Map<String, CompositionAmount> inputs = conservedVector(step.inputs(), registry);
                Map<String, CompositionAmount> outputs = conservedVector(step.outputs(), registry);
                if (!inputs.equals(outputs)) {
                    return "Step " + step.id() + " changes the conserved composition vector: input="
                            + inputs + ", output=" + outputs + ".";
                }
            } catch (IllegalStateException | ArithmeticException exception) {
                return "Step " + step.id() + " could not be flattened safely: " + exception.getMessage();
            }
        }
        return null;
    }

    private static String ioProblem(ProcessStep step, RecipeTypeDefinition type) {
        int itemInputs = countSolid(step.inputs());
        int fluidInputs = countFluid(step.inputs());
        int itemOutputs = countSolid(step.outputs());
        int fluidOutputs = countFluid(step.outputs());
        for (ProcessRequirement requirement : step.requirements()) {
            if (requirement.role() == ProcessRequirement.Role.CHEMICAL_BALANCE) continue;
            if (requirement.requiredPhase().isFluidLike()) fluidInputs++;
            else itemInputs++;
        }
        if (itemInputs > type.maxItemInputs() || itemOutputs > type.maxItemOutputs()
                || fluidInputs > type.maxFluidInputs() || fluidOutputs > type.maxFluidOutputs()) {
            return "planned IO exceeds existing " + type.id() + " limits: items "
                    + itemInputs + "->" + itemOutputs + ", fluids " + fluidInputs + "->" + fluidOutputs
                    + "; max " + type.maxItemInputs() + "->" + type.maxItemOutputs() + ", "
                    + type.maxFluidInputs() + "->" + type.maxFluidOutputs() + ".";
        }
        for (ProcessMaterial material : concat(step.inputs(), step.outputs())) {
            try {
                if (material.phase().isFluidLike()) material.milliBucketsExact();
                else material.itemAmountExact();
            } catch (RuntimeException error) {
                return "material " + material.materialId() + " has a non-serializable amount: " + error.getMessage();
            }
        }
        return null;
    }

    private static int countSolid(List<ProcessMaterial> materials) {
        return (int) materials.stream().filter(value -> !value.phase().isFluidLike()).count();
    }

    private static int countFluid(List<ProcessMaterial> materials) {
        return (int) materials.stream().filter(value -> value.phase().isFluidLike()).count();
    }

    private static List<ProcessMaterial> concat(List<ProcessMaterial> first, List<ProcessMaterial> second) {
        List<ProcessMaterial> result = new ArrayList<>(first.size() + second.size());
        result.addAll(first);
        result.addAll(second);
        return result;
    }

    private static Map<String, CompositionAmount> conservedVector(
            List<ProcessMaterial> materials,
            Map<String, MaterialAnalysis> registry
    ) {
        CompositionResolver resolver = new CompositionResolver(snapshots(registry));
        Map<String, CompositionAmount> result = new LinkedHashMap<>();
        for (ProcessMaterial processMaterial : materials) {
            if (!processMaterial.guaranteed()) continue;
            CompositionVector unitVector = resolver.conserved(processMaterial.materialId());
            CompositionAmount amount = CompositionAmount.of(processMaterial.milliUnits());
            for (Map.Entry<String, CompositionAmount> atom : unitVector.entries().entrySet()) {
                result.merge(atom.getKey(), atom.getValue().multiply(amount), CompositionAmount::add);
            }
        }
        result.entrySet().removeIf(entry -> entry.getValue().isZero());
        return Map.copyOf(result);
    }

    private static Map<String, MaterialSnapshot> snapshots(Map<String, MaterialAnalysis> registry) {
        Map<String, MaterialSnapshot> result = new LinkedHashMap<>();
        for (MaterialAnalysis analysis : registry.values()) {
            result.put(analysis.source().id(), analysis.source());
        }
        return Map.copyOf(result);
    }

    /**
     * Second-pass reachability for automatic composite processing (DUST and WOOD_PULP). Local
     * mass/semantic validation is not enough when a route exposes another processable compound:
     * that fraction must itself have a surviving automatic plan down to valid terminal components.
     *
     * <p>The pass is iterative because removing one downstream plan can make its parent unreachable.
     * A plan participates only when its first input is the exact automatic feed form declared by the
     * source definition, so unrelated synthesis plans are not reinterpreted as decomposition.</p>
     */
    public Result validateCompleteChains(List<ProcessPlan> candidates, Map<String, MaterialAnalysis> registry) {
        List<ProcessPlan> remaining = new ArrayList<>(candidates == null ? List.of() : candidates);
        List<ChemistryDiagnostic> diagnostics = new ArrayList<>();

        boolean changed;
        do {
            changed = false;
            Map<String, List<ProcessPlan>> bySource = new LinkedHashMap<>();
            for (ProcessPlan plan : remaining) {
                String source = compositeProcessingSource(plan, registry);
                if (source != null) bySource.computeIfAbsent(source, ignored -> new ArrayList<>()).add(plan);
            }

            Set<String> blockedPlanIds = new LinkedHashSet<>();
            for (ProcessPlan plan : remaining) {
                String source = compositeProcessingSource(plan, registry);
                if (source == null) continue;

                String problem = downstreamProblem(plan, bySource, registry);
                if (problem == null) continue;
                blockedPlanIds.add(plan.id());
                diagnostics.add(blocked(plan, "PROCESS_DOWNSTREAM_DEAD_END", problem));
            }

            if (!blockedPlanIds.isEmpty()) {
                remaining.removeIf(plan -> blockedPlanIds.contains(plan.id()));
                changed = true;
            }
        } while (changed);

        return new Result(remaining, diagnostics);
    }

    private static String downstreamProblem(
            ProcessPlan plan,
            Map<String, List<ProcessPlan>> bySource,
            Map<String, MaterialAnalysis> registry
    ) {
        if (plan.steps().isEmpty()) return "Automatic composite-processing plan has no steps.";
        ProcessStep last = plan.steps().getLast();
        for (ProcessMaterial output : last.outputs()) {
            if (!output.guaranteed()) continue;
            MaterialAnalysis analysis = registry.get(output.materialId());
            if (analysis == null) {
                return "Final output " + output.materialId() + " is not registered, so the chain cannot reach a valid recovered terminal component.";
            }
            if (isTerminalRecoveredComponent(analysis)) continue;

            Object backing = analysis.source().backingMaterial();
            if (!ProcessPlanner.isAutomaticCompositeProcessingTarget(backing)) {
                return "Final output " + output.materialId()
                        + " is composite but has no valid registered terminal representation and is not an automatic processing target.";
            }
            if (!bySource.containsKey(output.materialId()) || bySource.get(output.materialId()).isEmpty()) {
                return "Final output " + output.materialId()
                        + " is still an automatic processing target, but its downstream processing plan is missing or blocked.";
            }
        }
        return null;
    }

    private static boolean isTerminalRecoveredComponent(MaterialAnalysis analysis) {
        if (isElementalLeaf(analysis)) return true;
        Object backing = analysis.source().backingMaterial();

        if (backing instanceof StructureMaterial structure) {
            // Wood owns an explicit WOOD_PULP chemistry feed and therefore must continue through
            // its generated processing graph. Other structure definitions remain valid terminal
            // .contains(...) components when they have a concrete registered form.
            return !ProcessPlanner.isAutomaticStructureProcessingTarget(structure)
                    && ProcessPlanner.outputPart(analysis, backing) != null;
        }

        if (backing instanceof IndustrialMaterial material) {
            // Clay and any other non-auto-decomposition material may be a declared component in
            // .contains(...). If it has a concrete recipe representation, recovering that component
            // is a valid terminal state. Only automatic composite targets must continue.
            return !ProcessPlanner.isAutomaticDustProcessingTarget(material)
                    && ProcessPlanner.outputPart(analysis, backing) != null;
        }
        return false;
    }

    private static boolean isElementalLeaf(MaterialAnalysis analysis) {
        return analysis.classifications().contains(net.mads.industron.material.chemistry.MaterialClassification.ELEMENT)
                || (analysis.source().backingMaterial() instanceof IndustrialMaterial material
                && material.atomicNumber() > 0);
    }

    private static String compositeProcessingSource(
            ProcessPlan plan,
            Map<String, MaterialAnalysis> registry
    ) {
        if (plan == null || plan.steps().isEmpty()) return null;
        ProcessStep first = plan.steps().getFirst();
        if (first.inputs().size() != 1) return null;
        ProcessMaterial input = first.inputs().getFirst();
        MaterialAnalysis analysis = registry.get(input.materialId());
        if (analysis != null) {
            MaterialPart expected = ProcessPlanner.automaticProcessingInputPart(analysis.source().backingMaterial());
            if (expected != null && input.part() == expected) return input.materialId();
        }

        // Compatibility for older compound-DUST plans/tests that identify the decomposition family
        // by its stable plan id before the source backing definition is present in the local map.
        String target = plan.targetMaterialId();
        boolean legacyDustPlan = target.endsWith("_ore_dust_processing")
                || target.endsWith("_mineral_dust_processing")
                || target.endsWith("_composite_dust_processing");
        return legacyDustPlan && input.part() == MaterialPart.DUST ? input.materialId() : null;
    }

    private static String continuityProblem(ProcessPlan plan) {
        List<ProcessStep> steps = plan.steps();
        for (int index = 1; index < steps.size(); index++) {
            List<ProcessMaterial> previousOutputs = steps.get(index - 1).outputs();
            List<ProcessMaterial> currentInputs = steps.get(index).inputs();
            if (previousOutputs.size() != currentInputs.size()) {
                return "Plan " + plan.id() + " is disconnected between " + steps.get(index - 1).id()
                        + " and " + steps.get(index).id() + ": previous outputs=" + describe(previousOutputs)
                        + ", next inputs=" + describe(currentInputs) + ".";
            }
            for (int materialIndex = 0; materialIndex < previousOutputs.size(); materialIndex++) {
                ProcessMaterial output = previousOutputs.get(materialIndex);
                ProcessMaterial input = currentInputs.get(materialIndex);
                if (!sameStream(output, input)) {
                    return "Plan " + plan.id() + " is disconnected between " + steps.get(index - 1).id()
                            + " and " + steps.get(index).id() + ": " + stream(output)
                            + " does not feed " + stream(input) + ".";
                }
            }
        }
        return null;
    }

    private static boolean sameStream(ProcessMaterial left, ProcessMaterial right) {
        return left.materialId().equals(right.materialId())
                && left.phase() == right.phase()
                && left.part() == right.part()
                && left.milliUnits() == right.milliUnits();
    }

    private static String describe(List<ProcessMaterial> materials) {
        return materials.stream().map(ProcessSafetyValidator::stream).toList().toString();
    }

    private static String stream(ProcessMaterial material) {
        return material.materialId() + "@" + (material.part() == null ? material.phase() : material.part())
                + " x" + material.milliUnits();
    }

    private static Set<String> cyclicPlans(
            List<ProcessPlan> plans,
            Map<String, MaterialAnalysis> registry
    ) {
        Map<String, Set<String>> graph = new LinkedHashMap<>();
        List<OwnedEdge> edges = new ArrayList<>();
        for (ProcessPlan plan : plans) {
            // A direct ReactionSolver-backed components -> registered substance formation is an
            // intentional reversible chemistry direction. It is already atom/conserved-vector
            // balanced and costs a real timed process, so it must not make the inverse decomposition
            // graph look like an exploit cycle. All other generated edges still participate.
            if (isDirectBalancedFormation(plan, registry)) continue;
            for (ProcessStep step : plan.steps()) {
                for (ProcessMaterial input : step.inputs()) {
                    String from = node(input);
                    graph.computeIfAbsent(from, ignored -> new LinkedHashSet<>());
                    for (ProcessMaterial output : step.outputs()) {
                        String to = node(output);
                        graph.computeIfAbsent(to, ignored -> new LinkedHashSet<>());
                        graph.get(from).add(to);
                        edges.add(new OwnedEdge(from, to, plan.id()));
                    }
                }
            }
        }

        Set<String> cyclic = new HashSet<>();
        for (OwnedEdge edge : edges) {
            if (edge.from().equals(edge.to()) || reachable(edge.to(), edge.from(), graph)) {
                cyclic.add(edge.planId());
            }
        }
        return Set.copyOf(cyclic);
    }


    private static boolean isDirectBalancedFormation(
            ProcessPlan plan,
            Map<String, MaterialAnalysis> registry
    ) {
        if (plan == null || plan.steps().size() != 1) return false;
        ProcessStep step = plan.steps().getFirst();
        if (step.kind() != ProcessKind.MIXING
                && step.kind() != ProcessKind.ALLOYING
                && step.kind() != ProcessKind.POLYMERIZATION
                && step.kind() != ProcessKind.CHEMICAL_REACTION) {
            return false;
        }

        MaterialAnalysis target = registry.get(plan.targetMaterialId());
        if (target == null || step.outputs().size() != 1) return false;
        ReactionPlan solved = ReactionSolver.synthesize(target.source(), snapshots(registry)).orElse(null);
        if (solved == null) return false;

        ProcessMaterial output = step.outputs().getFirst();
        ReactionParticipant product = solved.products().stream()
                .filter(value -> value.materialId().equals(plan.targetMaterialId()))
                .findFirst()
                .orElse(null);
        if (product == null
                || !output.materialId().equals(product.materialId())
                || output.milliUnits() != product.milliUnits()) {
            return false;
        }

        return participantAmounts(step.inputs()).equals(reactionAmounts(solved.reactants()));
    }

    private static Map<String, Long> participantAmounts(List<ProcessMaterial> materials) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (ProcessMaterial material : materials) {
            result.merge(material.materialId(), material.milliUnits(), Math::addExact);
        }
        return Map.copyOf(result);
    }

    private static Map<String, Long> reactionAmounts(List<ReactionParticipant> participants) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (ReactionParticipant participant : participants) {
            result.merge(participant.materialId(), participant.milliUnits(), Math::addExact);
        }
        return Map.copyOf(result);
    }

    private static boolean reachable(
            String start,
            String target,
            Map<String, Set<String>> graph
    ) {
        ArrayDeque<String> pending = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        pending.add(start);
        while (!pending.isEmpty()) {
            String current = pending.removeFirst();
            if (!visited.add(current)) continue;
            if (current.equals(target)) return true;
            for (String next : graph.getOrDefault(current, Set.of())) {
                pending.addLast(next);
            }
        }
        return false;
    }

    private static String node(ProcessMaterial material) {
        String form = material.part() == null ? material.phase().name() : material.part().name();
        return material.materialId() + "@" + form;
    }

    private static ChemistryDiagnostic blocked(ProcessPlan plan, String code, String message) {
        return new ChemistryDiagnostic(
                ChemistryStatus.IMPOSSIBLE,
                plan.targetMaterialId(),
                code,
                message,
                List.of("Correct the material composition or route direction; increasing energy, tier or duration cannot make a material-creating loop safe."),
                ""
        );
    }

    private record OwnedEdge(String from, String to, String planId) {
    }
}
