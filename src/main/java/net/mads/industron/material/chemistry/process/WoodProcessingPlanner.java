package net.mads.industron.material.chemistry.process;

import net.mads.industron.material.AutomaticProcessIntermediate;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.chemistry.ChemicalStructure;
import net.mads.industron.material.chemistry.ChemistryDiagnostic;
import net.mads.industron.material.chemistry.ChemistryPhase;
import net.mads.industron.material.chemistry.ChemistryStatus;
import net.mads.industron.material.chemistry.MaterialAnalysis;
import net.mads.industron.material.chemistry.MaterialClassification;
import net.mads.industron.material.structure.WoodMaterial;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Deterministic automatic chemistry for {@link WoodMaterial} definitions.
 *
 * <p>The wood id/name never chooses a route. A newly defined wood gets routes only from its
 * .contains(...) graph plus calculated topology/properties. Multiple valid families may coexist;
 * no random choice is made. Registry-backed intermediates are requested only by a route that
 * survives chemistry, reagent and RecipeType representability checks.</p>
 */
public final class WoodProcessingPlanner {
    public record ChemicalPreview(
            SeparationClassifier.Decision decision,
            ProcessChainProfile profile,
            List<ProcessMaterial> outputs,
            String catalystId,
            String activatorId
    ) {
        public ChemicalPreview {
            outputs = List.copyOf(outputs == null ? List.of() : outputs);
        }
    }

    public record PyrolysisPreview(
            boolean direct,
            ProcessKind recoveryKind,
            List<ProcessMaterial> recoveryOutputs,
            int processingTier,
            int temperature,
            int recoveryTemperature,
            String explanation
    ) {
        public PyrolysisPreview {
            recoveryOutputs = List.copyOf(recoveryOutputs == null ? List.of() : recoveryOutputs);
            explanation = explanation == null ? "" : explanation;
        }
    }

    public record Preview(
            Optional<ChemicalPreview> chemical,
            Optional<PyrolysisPreview> pyrolysis
    ) {
        public Preview {
            chemical = chemical == null ? Optional.empty() : chemical;
            pyrolysis = pyrolysis == null ? Optional.empty() : pyrolysis;
        }

        public Set<AutomaticProcessIntermediate.Kind> requiredIntermediates() {
            LinkedHashSet<AutomaticProcessIntermediate.Kind> result = new LinkedHashSet<>();
            chemical.ifPresent(value -> {
                for (ProcessChainProfile.Stage stage : value.profile().stages()) {
                    result.add(kindForStage(stage));
                }
            });
            pyrolysis.filter(value -> !value.direct())
                    .ifPresent(value -> result.add(AutomaticProcessIntermediate.Kind.PYROLYSATE));
            return Collections.unmodifiableSet(new LinkedHashSet<>(result));
        }
    }

    private WoodProcessingPlanner() {
    }

    public static Preview preview(
            WoodMaterial wood,
            MaterialAnalysis analysis,
            Map<String, MaterialAnalysis> registry
    ) {
        if (wood == null || analysis == null || registry == null
                || !wood.generatedForms().contains(MaterialPart.WOOD_PULP)
                || wood.components().isEmpty()) {
            return new Preview(Optional.empty(), Optional.empty());
        }

        List<ProcessMaterial> outputs = componentOutputs(analysis, registry);
        if (outputs.size() < 2) {
            return new Preview(Optional.empty(), Optional.empty());
        }

        Optional<ChemicalPreview> chemical = previewChemical(analysis, outputs, registry);
        Optional<PyrolysisPreview> pyrolysis = previewPyrolysis(analysis, outputs, registry);
        return new Preview(chemical, pyrolysis);
    }

    public static ProcessPlanner.Result plan(
            WoodMaterial wood,
            MaterialAnalysis analysis,
            Map<String, MaterialAnalysis> registry
    ) {
        List<ProcessPlan> plans = new ArrayList<>();
        List<ChemistryDiagnostic> diagnostics = new ArrayList<>();
        Preview preview = preview(wood, analysis, registry);

        preview.chemical().ifPresent(value -> buildChemicalPlan(
                wood, analysis, value, registry, diagnostics).ifPresent(plans::add));
        preview.pyrolysis().ifPresent(value -> buildPyrolysisPlan(
                wood, analysis, value, registry, diagnostics).ifPresent(plans::add));

        if (plans.isEmpty() && (!wood.components().isEmpty())) {
            diagnostics.add(new ChemistryDiagnostic(
                    ChemistryStatus.OK_WITH_REQUIREMENTS,
                    wood.id(),
                    "WOOD_NO_REPRESENTABLE_PROCESS_ROUTE",
                    "The wood composition was analyzed, but no deterministic chemistry route is both physically selected and representable by the registered RecipeTypes.",
                    List.of("Change the composition/properties or add the missing generic process capability; do not hardcode a route for this wood id."),
                    ""
            ));
        }
        return new ProcessPlanner.Result(plans, diagnostics);
    }

    private static Optional<ChemicalPreview> previewChemical(
            MaterialAnalysis analysis,
            List<ProcessMaterial> outputs,
            Map<String, MaterialAnalysis> registry
    ) {
        SeparationClassifier.Decision decision = new SeparationClassifier().classify(analysis, outputs, registry);
        ProcessChainProfile profile = ProcessChainProfile.forAnalysis(analysis, decision.route());
        if (!profile.enabled()) return Optional.empty();

        // Wood owns no generic molten form. A custom wood whose calculated chemistry genuinely
        // requires a molten lattice route is therefore left without this alternative instead of
        // inventing a fake molten wood fluid.
        if (decision.route() == SeparationClassifier.Route.MOLTEN_ELECTROLYSIS
                || decision.route() == SeparationClassifier.Route.MOLTEN_ELECTROREFINING
                || decision.route() == SeparationClassifier.Route.MOLTEN_CHEMICAL_REACTION
                || decision.route() == SeparationClassifier.Route.NO_VALID_PHYSICAL_SEPARATION) {
            return Optional.empty();
        }

        int tier = ProcessChainProfile.processingTierIndex(analysis, decision.route());
        ProcessPlanner.CompositeRoutePreview routePreview = new ProcessPlanner.CompositeRoutePreview(
                decision, profile, outputs
        );
        FictionalReagentResolver reagents = new FictionalReagentResolver();

        String catalystId = null;
        if (ProcessPlanner.compositeRouteRequiresCatalyst(analysis, routePreview)) {
            catalystId = reagents.resolveCatalyst(analysis, registry, tier).orElse(null);
            if (catalystId == null) return Optional.empty();
        }

        String activatorId = null;
        if (profile.requiresActivator()) {
            activatorId = reagents.resolveActivator(
                    analysis,
                    registry,
                    catalystId == null ? Set.of() : Set.of(catalystId),
                    tier
            ).orElse(null);
            if (activatorId == null) return Optional.empty();
        }

        if (ProcessPlanner.compositeRouteRepresentabilityProblem(
                routePreview, catalystId, activatorId, registry).isPresent()) {
            return Optional.empty();
        }
        return Optional.of(new ChemicalPreview(decision, profile, outputs, catalystId, activatorId));
    }

    private static Optional<PyrolysisPreview> previewPyrolysis(
            MaterialAnalysis analysis,
            List<ProcessMaterial> coldOutputs,
            Map<String, MaterialAnalysis> registry
    ) {
        if (analysis.phase() != ChemistryPhase.SOLID || coldOutputs.size() < 2) return Optional.empty();
        ChemicalStructure.Topology topology = ProcessSubstanceState.topologyOf(analysis);
        if (topology == ChemicalStructure.Topology.PHYSICAL_MIXTURE
                || topology == ChemicalStructure.Topology.METALLIC_LATTICE
                || topology == ChemicalStructure.Topology.IONIC_LATTICE
                || analysis.classifications().contains(MaterialClassification.ALLOY)
                || analysis.classifications().contains(MaterialClassification.IONIC_COMPOUND)) {
            return Optional.empty();
        }

        int temperature = pyrolysisTemperature(analysis);
        int tier = Math.max(0, analysis.calculatedTierIndex());

        // If pyrolysis can expose the declared top-level components directly within the existing
        // RecipeType IO limits, do that. Creating a liquid <Wood> Pyrolysate only to immediately
        // crystallize the same intact solid components is a fake intermediate and violates the
        // lazy-intermediate contract.
        if (ProcessPlanner.ioCapacityProblem(
                ProcessKind.PYROLYSIS,
                ChemistryPhase.SOLID,
                coldOutputs,
                List.of()
        ).isEmpty()) {
            return Optional.of(new PyrolysisPreview(
                    true,
                    null,
                    coldOutputs,
                    tier,
                    temperature,
                    0,
                    "The calculated bonded wood composition can be thermally decomposed directly into "
                            + "its declared top-level components within the existing Pyrolysis IO limits."
            ));
        }

        ProcessKind recoveryKind;
        List<ProcessMaterial> recoveryOutputs;
        List<ProcessMaterial> naturalFluidOutputs = naturalFluidOutputs(coldOutputs, registry);
        if (naturalFluidOutputs.size() == coldOutputs.size()) {
            recoveryKind = boilingRecovery(naturalFluidOutputs, registry).orElse(null);
            recoveryOutputs = naturalFluidOutputs;
            if (recoveryKind == null) {
                double polaritySpread = propertySpread(coldOutputs, registry, "polarity");
                if (polaritySpread < 8.0D) return Optional.empty();
                recoveryKind = ProcessKind.SOLVENT_EXTRACTION;
                recoveryOutputs = coldOutputs;
            }
        } else {
            // A staged pyrolysate is only valid when a real downstream separation family exists.
            // Do not invent "liquid -> several unrelated solids" crystallization merely to fit IO.
            double polaritySpread = propertySpread(coldOutputs, registry, "polarity");
            if (polaritySpread < 8.0D) return Optional.empty();
            recoveryKind = ProcessKind.SOLVENT_EXTRACTION;
            recoveryOutputs = coldOutputs;
        }

        if (ProcessPlanner.ioCapacityProblem(
                ProcessKind.PYROLYSIS,
                ChemistryPhase.SOLID,
                ProcessPlanner.oneVirtualOutput(ChemistryPhase.LIQUID),
                List.of()
        ).isPresent()) return Optional.empty();
        if (ProcessPlanner.ioCapacityProblem(
                recoveryKind,
                ChemistryPhase.LIQUID,
                recoveryOutputs,
                List.of()
        ).isPresent()) return Optional.empty();

        int recoveryTemperature = recoveryTemperature(recoveryKind, recoveryOutputs, registry);
        String explanation = "A solid non-metallic bonded wood composition supports thermal decomposition. "
                + "A registry-backed pyrolysate is required because the products cannot be exposed directly; "
                + "the calculated downstream properties select " + recoveryKind + ".";
        return Optional.of(new PyrolysisPreview(
                false, recoveryKind, recoveryOutputs, tier, temperature, recoveryTemperature, explanation));
    }

    private static Optional<ProcessPlan> buildPyrolysisPlan(
            WoodMaterial wood,
            MaterialAnalysis analysis,
            PyrolysisPreview preview,
            Map<String, MaterialAnalysis> registry,
            List<ChemistryDiagnostic> diagnostics
    ) {
        long units = totalUnits(preview.recoveryOutputs());
        ProcessMaterial feed = ProcessPlanner.processMaterial(analysis, units, MaterialPart.WOOD_PULP);
        ProcessPlan.Builder plan = ProcessPlan.builder(wood.id() + "_wood_pyrolysis_processing");

        if (preview.direct()) {
            plan.step(new ProcessStep(
                    "materials/" + wood.id() + "/pyrolysis_recovery",
                    ProcessKind.PYROLYSIS,
                    List.of(feed),
                    preview.recoveryOutputs(),
                    List.of(),
                    preview.temperature(),
                    preview.processingTier(),
                    preview.explanation()
            ));
            return Optional.of(plan.build());
        }

        MaterialAnalysis pyrolysate = AutomaticProcessIntermediate.find(
                wood, AutomaticProcessIntermediate.Kind.PYROLYSATE, registry).orElse(null);
        if (pyrolysate == null) {
            diagnostics.add(missingIntermediate(analysis, AutomaticProcessIntermediate.Kind.PYROLYSATE));
            return Optional.empty();
        }

        ProcessMaterial liquid = ProcessPlanner.processMaterial(pyrolysate, units, MaterialPart.LIQUID);
        plan.step(new ProcessStep(
                "materials/" + wood.id() + "/pyrolysis_to_pyrolysate",
                ProcessKind.PYROLYSIS,
                List.of(feed),
                List.of(liquid),
                List.of(),
                preview.temperature(),
                preview.processingTier(),
                "Calculated thermal decomposition creates the registry-backed " + wood.displayName()
                        + " Pyrolysate. The intermediate exists only because this selected route consumes it."
        ));
        plan.step(new ProcessStep(
                "materials/" + wood.id() + "/recover_from_pyrolysate",
                preview.recoveryKind(),
                List.of(liquid),
                preview.recoveryOutputs(),
                List.of(),
                preview.recoveryTemperature(),
                preview.processingTier(),
                preview.explanation()
        ));
        return Optional.of(plan.build());
    }

    private static Optional<ProcessPlan> buildChemicalPlan(
            WoodMaterial wood,
            MaterialAnalysis analysis,
            ChemicalPreview preview,
            Map<String, MaterialAnalysis> registry,
            List<ChemistryDiagnostic> diagnostics
    ) {
        long units = totalUnits(preview.outputs());
        ProcessMaterial current = ProcessPlanner.processMaterial(analysis, units, MaterialPart.WOOD_PULP);
        ProcessPlan.Builder plan = ProcessPlan.builder(wood.id() + "_wood_chemical_processing");
        int tier = ProcessChainProfile.processingTierIndex(analysis, preview.decision().route());

        if (preview.profile().targetSteps() <= 1) {
            ProcessKind direct = switch (preview.decision().route()) {
                case CENTRIFUGING -> ProcessKind.CENTRIFUGING;
                case MAGNETIC_SEPARATION -> ProcessKind.MAGNETIC_SEPARATION;
                case DIRECT_ELECTROLYSIS -> ProcessKind.ELECTROLYSIS;
                case CHEMICAL_REACTION -> ProcessKind.CHEMICAL_REACTION;
                default -> null;
            };
            if (direct == null) return Optional.empty();
            List<ProcessRequirement> requirements = direct == ProcessKind.CHEMICAL_REACTION
                    ? ProcessPlanner.catalystRequirements(preview.catalystId(), registry)
                    : List.of();
            plan.step(new ProcessStep(
                    "materials/" + wood.id() + "/chemical_recovery",
                    direct,
                    List.of(current),
                    preview.outputs(),
                    requirements,
                    0,
                    tier,
                    preview.profile().explanation() + " " + preview.decision().explanation()
            ));
            return Optional.of(plan.build());
        }

        for (ProcessChainProfile.Stage stage : preview.profile().stages()) {
            AutomaticProcessIntermediate.Kind kind = kindForStage(stage);
            MaterialAnalysis intermediate = AutomaticProcessIntermediate.find(wood, kind, registry).orElse(null);
            if (intermediate == null) {
                diagnostics.add(missingIntermediate(analysis, kind));
                return Optional.empty();
            }

            ProcessMaterial next = ProcessPlanner.processMaterial(intermediate, units, kind.part());
            ProcessKind process;
            List<ProcessRequirement> requirements = List.of();
            String suffix;
            switch (stage) {
                case ROASTED_DUST -> {
                    process = ProcessKind.ROASTING;
                    suffix = "roast_pulp";
                }
                case SLURRY -> {
                    process = ProcessKind.LEACHING;
                    suffix = "leach_to_slurry";
                    requirements = ProcessPlanner.leachingRequirements(preview.profile());
                }
                case SOLUTION -> {
                    boolean fromSolid = current.phase() == ChemistryPhase.SOLID;
                    process = fromSolid ? ProcessKind.DISSOLUTION : ProcessKind.CHEMICAL_REACTION;
                    suffix = fromSolid ? "dissolve_to_solution" : "condition_slurry_to_solution";
                    requirements = fromSolid
                            ? ProcessPlanner.leachingRequirements(preview.profile())
                            : ProcessPlanner.catalystRequirements(preview.catalystId(), registry);
                }
                case REACTION_MIXTURE -> {
                    process = ProcessKind.CHEMICAL_REACTION;
                    suffix = "activate_reaction_mixture";
                    requirements = ProcessPlanner.mergeRequirements(
                            ProcessPlanner.activatorRequirements(preview.activatorId(), registry),
                            ProcessPlanner.catalystRequirements(preview.catalystId(), registry)
                    );
                }
                default -> throw new IllegalStateException("Unhandled wood chemistry stage " + stage);
            }
            plan.step(new ProcessStep(
                    "materials/" + wood.id() + "/" + suffix,
                    process,
                    List.of(current),
                    List.of(next),
                    requirements,
                    0,
                    tier,
                    "Deterministic wood chemistry stage " + stage + "; the intermediate was registered only because this route selected it."
            ));
            current = next;
        }

        ProcessKind recovery = switch (preview.decision().route()) {
            case LEACHING_ELECTROWINNING -> ProcessKind.ELECTROWINNING;
            case LEACHING, CHEMICAL_REACTION -> ProcessKind.CHEMICAL_REACTION;
            default -> null;
        };
        if (recovery == null) return Optional.empty();
        plan.step(new ProcessStep(
                "materials/" + wood.id() + "/recover_components",
                recovery,
                List.of(current),
                preview.outputs(),
                recovery == ProcessKind.CHEMICAL_REACTION
                        ? ProcessPlanner.catalystRequirements(preview.catalystId(), registry)
                        : List.of(),
                0,
                tier,
                preview.profile().explanation() + " Final recovery follows the calculated component chemistry."
        ));
        return Optional.of(plan.build());
    }

    private static List<ProcessMaterial> componentOutputs(
            MaterialAnalysis analysis,
            Map<String, MaterialAnalysis> registry
    ) {
        List<Map.Entry<String, Long>> weights = ProcessPlanner.topLevelComponentWeights(analysis, registry);
        if (weights.isEmpty()) return List.of();
        long divisor = 0L;
        for (Map.Entry<String, Long> entry : weights) {
            divisor = gcd(divisor, entry.getValue());
        }
        divisor = Math.max(1L, divisor);

        List<ProcessMaterial> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : weights) {
            MaterialAnalysis child = registry.get(entry.getKey());
            if (child == null) return List.of();
            MaterialPart part = ProcessPlanner.outputPart(child, child.source().backingMaterial());
            if (part == null) return List.of();
            long amount = entry.getValue() / divisor;
            if (amount <= 0) return List.of();
            result.add(ProcessPlanner.processMaterial(child, amount, part));
        }
        return List.copyOf(result);
    }

    private static List<ProcessMaterial> naturalFluidOutputs(
            List<ProcessMaterial> coldOutputs,
            Map<String, MaterialAnalysis> registry
    ) {
        List<ProcessMaterial> result = new ArrayList<>();
        for (ProcessMaterial cold : coldOutputs) {
            MaterialAnalysis child = registry.get(cold.materialId());
            if (child == null || !(child.source().backingMaterial() instanceof IndustrialMaterial material)) {
                return List.of();
            }
            MaterialPart part;
            if ((child.phase() == ChemistryPhase.GAS || child.phase() == ChemistryPhase.PLASMA)
                    && material.has(MaterialPart.GAS)) {
                part = MaterialPart.GAS;
            } else if (child.phase() == ChemistryPhase.LIQUID && material.has(MaterialPart.LIQUID)) {
                part = MaterialPart.LIQUID;
            } else {
                return List.of();
            }
            result.add(ProcessPlanner.processMaterial(child, cold.milliUnits() / 1000L, part));
        }
        return List.copyOf(result);
    }

    private static Optional<ProcessKind> boilingRecovery(
            List<ProcessMaterial> outputs,
            Map<String, MaterialAnalysis> registry
    ) {
        if (outputs.stream().anyMatch(value -> value.phase() == ChemistryPhase.GAS
                || value.phase() == ChemistryPhase.PLASMA)) {
            return Optional.of(ProcessKind.DISTILLATION);
        }
        List<Double> boiling = propertyValues(outputs, registry, "boilingpoint").stream().sorted().toList();
        if (boiling.size() >= 2) {
            double spread = boiling.getLast() - boiling.getFirst();
            double minimumGap = Double.POSITIVE_INFINITY;
            for (int index = 1; index < boiling.size(); index++) {
                minimumGap = Math.min(minimumGap, boiling.get(index) - boiling.get(index - 1));
            }
            if (spread >= 5.0D && minimumGap <= 35.0D) return Optional.of(ProcessKind.FRACTIONATION);
            if (spread >= 15.0D) return Optional.of(ProcessKind.DISTILLATION);
        }
        double volatilitySpread = propertySpread(outputs, registry, "volatility");
        if (volatilitySpread >= 30.0D) return Optional.of(ProcessKind.DISTILLATION);
        if (volatilitySpread >= 8.0D) return Optional.of(ProcessKind.FRACTIONATION);
        return Optional.empty();
    }

    private static double propertySpread(
            List<ProcessMaterial> values,
            Map<String, MaterialAnalysis> registry,
            String property
    ) {
        List<Double> resolved = propertyValues(values, registry, property);
        if (resolved.size() < 2) return 0.0D;
        double min = resolved.stream().mapToDouble(Double::doubleValue).min().orElse(0.0D);
        double max = resolved.stream().mapToDouble(Double::doubleValue).max().orElse(0.0D);
        return max - min;
    }

    private static List<Double> propertyValues(
            List<ProcessMaterial> values,
            Map<String, MaterialAnalysis> registry,
            String property
    ) {
        List<Double> result = new ArrayList<>();
        for (ProcessMaterial value : values) {
            double resolved = value.processProperty(property);
            if (!Double.isFinite(resolved)) {
                MaterialAnalysis child = registry.get(value.materialId());
                if (child != null) resolved = child.properties().get(property);
            }
            if (Double.isFinite(resolved)) result.add(resolved);
        }
        return List.copyOf(result);
    }

    private static int recoveryTemperature(
            ProcessKind recoveryKind,
            List<ProcessMaterial> outputs,
            Map<String, MaterialAnalysis> registry
    ) {
        if (recoveryKind != ProcessKind.DISTILLATION && recoveryKind != ProcessKind.FRACTIONATION) {
            return 0;
        }
        List<Double> boiling = propertyValues(outputs, registry, "boilingpoint");
        if (boiling.isEmpty()) return 0;
        double selected = recoveryKind == ProcessKind.DISTILLATION
                ? boiling.stream().mapToDouble(Double::doubleValue).max().orElse(0.0D)
                : boiling.stream().mapToDouble(Double::doubleValue).average().orElse(0.0D);
        if (!Double.isFinite(selected)) return 0;
        return Math.max(1, (int) Math.round(selected));
    }

    private static int pyrolysisTemperature(MaterialAnalysis analysis) {
        double stability = normalized(analysis.properties().get("chemicalstability"));
        double bond = normalized(analysis.properties().get("bondstrength"));
        double volatility = normalized(analysis.properties().get("volatility"));
        int calculated = (int) Math.round(180.0D + stability * 2.2D + bond * 1.3D + (100.0D - volatility) * 0.5D);
        return Math.max(180, Math.min(900, calculated));
    }

    private static long totalUnits(List<ProcessMaterial> outputs) {
        long milliUnits = outputs.stream().mapToLong(ProcessMaterial::milliUnits).sum();
        if (milliUnits <= 0 || milliUnits % 1000L != 0L) {
            throw new IllegalStateException("Wood processing output units are not exact material units: " + milliUnits);
        }
        return milliUnits / 1000L;
    }

    static AutomaticProcessIntermediate.Kind kindForStage(ProcessChainProfile.Stage stage) {
        return switch (stage) {
            case ROASTED_DUST -> AutomaticProcessIntermediate.Kind.ROASTED_DUST;
            case SLURRY -> AutomaticProcessIntermediate.Kind.SLURRY;
            case SOLUTION -> AutomaticProcessIntermediate.Kind.SOLUTION;
            case REACTION_MIXTURE -> AutomaticProcessIntermediate.Kind.REACTION_MIXTURE;
        };
    }

    private static ChemistryDiagnostic missingIntermediate(
            MaterialAnalysis owner,
            AutomaticProcessIntermediate.Kind kind
    ) {
        return new ChemistryDiagnostic(
                ChemistryStatus.CHANGE_REQUIRED,
                owner.source().id(),
                "MISSING_" + kind.name(),
                "The selected deterministic wood route requires " + kind.name().toLowerCase(java.util.Locale.ROOT)
                        + ", but that on-demand registry material is missing.",
                List.of("Generate automatic intermediates before registry freeze; do not create unused intermediates for every wood."),
                ""
        );
    }

    private static double normalized(double value) {
        if (!Double.isFinite(value)) return 0.0D;
        return Math.max(0.0D, Math.min(100.0D, value));
    }

    private static long gcd(long left, long right) {
        left = Math.abs(left);
        right = Math.abs(right);
        while (right != 0L) {
            long remainder = left % right;
            left = right;
            right = remainder;
        }
        return left;
    }
}
