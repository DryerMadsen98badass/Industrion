package net.mads.industron.material.chemistry.process;

import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.AutomaticProcessIntermediate;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.structure.StructureMaterial;
import net.mads.industron.material.structure.WoodMaterial;
import net.mads.industron.material.chemistry.ChemistryDiagnostic;
import net.mads.industron.material.chemistry.ChemistryPhase;
import net.mads.industron.material.chemistry.ChemistryStatus;
import net.mads.industron.material.chemistry.CompositionEntry;
import net.mads.industron.material.chemistry.MaterialAnalysis;
import net.mads.industron.material.chemistry.MaterialClassification;
import net.mads.industron.material.chemistry.MaterialSnapshot;
import net.mads.industron.material.chemistry.MaterialSourceType;
import net.mads.industron.material.chemistry.ReactionPlan;
import net.mads.industron.material.chemistry.ReactionParticipant;
import net.mads.industron.material.chemistry.ReactionSolver;
import net.mads.industron.material.chemistry.geology.GeologyMaterialRoles;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Builds chemistry/process routes from calculated material behaviour.
 *
 * <p>Ore preprocessing before DUST is deliberately outside this planner. Natural ore minerals enter
 * here as their compound DUST. Mechanical preparation never splits a compound. Only the route below
 * DUST may create separate elemental streams, and physical separation is only selected for an actual
 * physical mixture.</p>
 */
public final class ProcessPlanner {
    private final SeparationClassifier separationClassifier = new SeparationClassifier();
    private final FictionalReagentResolver reagentResolver = new FictionalReagentResolver();

    public record Result(List<ProcessPlan> plans, List<ChemistryDiagnostic> diagnostics) {
        public Result {
            plans = List.copyOf(plans);
            diagnostics = List.copyOf(diagnostics);
        }
    }

    public record CompositeRoutePreview(
            SeparationClassifier.Decision decision,
            ProcessChainProfile profile,
            List<ProcessMaterial> outputs
    ) {
        public CompositeRoutePreview {
            outputs = List.copyOf(outputs == null ? List.of() : outputs);
        }
    }

    /**
     * Registry-time preview shared with ProcessIntermediateMaterials. It intentionally performs the
     * same flattening/output-form selection as the real planner so intermediate registration cannot
     * choose a different graph from recipe generation.
     */
    public static Optional<CompositeRoutePreview> previewCompositeDustRoute(
            MaterialAnalysis analysis,
            Map<String, MaterialAnalysis> registry
    ) {
        if (!(analysis.source().backingMaterial() instanceof IndustrialMaterial material)
                || !isAutomaticDustProcessingTarget(material)
                || solidProcessingFeed(material) == null
                || analysis.source().composition().isEmpty()) {
            return Optional.empty();
        }

        Map<String, Long> flattened = new LinkedHashMap<>();
        if (!flattenElements(analysis.source().id(), 1L, registry, flattened, new java.util.LinkedHashSet<>())) {
            return Optional.empty();
        }
        flattened.remove(analysis.source().id());
        if (flattened.isEmpty()) return Optional.empty();

        // Recipe outputs follow the declared top-level .contains(...) graph for every compound, not
        // only for physical mixtures. Recursive flattening above is used for conservation/cycle
        // proof; it must not erase a legal declared component such as wood, stone or clay from the
        // actual recipe graph. A top-level compound DUST can continue through its own downstream
        // automatic plan, while StructureMaterial/clay components may terminate in their concrete
        // registered form.
        List<Map.Entry<String, Long>> orderedComponents = topLevelComponentWeights(analysis, registry);
        if (orderedComponents.isEmpty()) return Optional.empty();

        long divisor = 0L;
        for (Map.Entry<String, Long> component : orderedComponents) {
            divisor = greatestCommonDivisor(divisor, component.getValue());
        }
        divisor = Math.max(1L, divisor);

        List<ProcessMaterial> outputs = new ArrayList<>();
        for (Map.Entry<String, Long> entry : orderedComponents) {
            MaterialAnalysis child = registry.get(entry.getKey());
            if (child == null || !hasMaterialForm(child.source().backingMaterial())) return Optional.empty();
            long amount = entry.getValue() / divisor;
            if (amount <= 0 || amount > Integer.MAX_VALUE) return Optional.empty();
            MaterialPart part = outputPart(child, child.source().backingMaterial());
            if (part == null) return Optional.empty();
            outputs.add(processMaterial(child, amount, part));
        }

        SeparationClassifier.Decision decision = new SeparationClassifier().classify(analysis, outputs, registry);
        ProcessChainProfile profile = ProcessChainProfile.forAnalysis(analysis, decision.route());
        return Optional.of(new CompositeRoutePreview(decision, profile, outputs));
    }

    /**
     * Whether the deterministic composite route contains a CHEMICAL_REACTION step that needs a
     * non-consumed fictional catalyst. This is shared with registry-time intermediate generation so
     * unreachable chains are rejected before their automatic intermediates are registered.
     */
    public static boolean compositeRouteRequiresCatalyst(
            MaterialAnalysis analysis,
            CompositeRoutePreview preview
    ) {
        if (analysis == null || preview == null || !preview.profile().enabled()) return false;
        if (normalized(analysis.properties().get("reactivity")) >= 30.0D) return false;

        return switch (preview.decision().route()) {
            case CHEMICAL_REACTION, LEACHING, MOLTEN_CHEMICAL_REACTION -> true;
            case LEACHING_ELECTROWINNING -> preview.profile().stages().contains(ProcessChainProfile.Stage.SLURRY)
                    || preview.profile().stages().contains(ProcessChainProfile.Stage.REACTION_MIXTURE);
            default -> false;
        };
    }

    /**
     * Registry-time representability check for the already-selected composite route.
     *
     * <p>Automatic intermediate items/fluids are registered before recipes are emitted, so this
     * pass prevents unreachable slurry/solution/etc. from entering the content registry when the
     * existing RecipeType that must perform a selected step cannot represent its IO. Importantly,
     * the machine limits are consulted only <em>after</em> chemistry selected the route; they may
     * block the route but never choose a replacement.</p>
     */
    public static Optional<String> compositeRouteRepresentabilityProblem(
            CompositeRoutePreview preview,
            String catalystId,
            String activatorId,
            Map<String, MaterialAnalysis> registry
    ) {
        if (preview == null || !preview.profile().enabled()) {
            return Optional.of("Composite route is disabled before recipe representability can be checked.");
        }

        List<ProcessRequirement> catalyst = catalystRequirements(catalystId, registry);
        List<ProcessRequirement> activator = activatorRequirements(activatorId, registry);
        List<ProcessMaterial> finalOutputs = preview.outputs();
        if (finalOutputs.isEmpty()) return Optional.of("Composite route has no guaranteed final outputs.");

        SeparationClassifier.Route route = preview.decision().route();
        if (route == SeparationClassifier.Route.CENTRIFUGING
                || route == SeparationClassifier.Route.MAGNETIC_SEPARATION
                || route == SeparationClassifier.Route.THERMAL_REDUCTION
                || route == SeparationClassifier.Route.DIRECT_ELECTROLYSIS) {
            ProcessKind kind = switch (route) {
                case CENTRIFUGING -> ProcessKind.CENTRIFUGING;
                case MAGNETIC_SEPARATION -> ProcessKind.MAGNETIC_SEPARATION;
                case THERMAL_REDUCTION -> ProcessKind.SMELTING;
                case DIRECT_ELECTROLYSIS -> ProcessKind.ELECTROLYSIS;
                default -> throw new IllegalStateException("Unexpected direct route " + route);
            };
            List<ProcessMaterial> representedOutputs = route == SeparationClassifier.Route.THERMAL_REDUCTION
                    ? thermalReductionOutputs(finalOutputs, registry)
                    : finalOutputs;
            return ioCapacityProblem(kind, ChemistryPhase.SOLID, representedOutputs, List.of());
        }

        if (route == SeparationClassifier.Route.MOLTEN_ELECTROLYSIS
                || route == SeparationClassifier.Route.MOLTEN_ELECTROREFINING
                || route == SeparationClassifier.Route.MOLTEN_CHEMICAL_REACTION) {
            Optional<String> melting = ioCapacityProblem(
                    ProcessKind.MELTING,
                    ChemistryPhase.SOLID,
                    oneVirtualOutput(ChemistryPhase.MOLTEN),
                    List.of()
            );
            if (melting.isPresent()) return melting;
            ProcessKind separation = switch (route) {
                case MOLTEN_ELECTROLYSIS -> ProcessKind.ELECTROLYSIS;
                case MOLTEN_ELECTROREFINING -> ProcessKind.ELECTROREFINING;
                case MOLTEN_CHEMICAL_REACTION -> ProcessKind.CHEMICAL_REACTION;
                default -> throw new IllegalStateException("Unexpected molten route " + route);
            };
            return ioCapacityProblem(
                    separation,
                    ChemistryPhase.MOLTEN,
                    finalOutputs,
                    separation == ProcessKind.CHEMICAL_REACTION ? catalyst : List.of()
            );
        }

        ChemistryPhase current = ChemistryPhase.SOLID;
        ProcessChainProfile profile = preview.profile();
        for (ProcessChainProfile.Stage stage : profile.stages()) {
            ProcessKind kind;
            ChemistryPhase next;
            List<ProcessRequirement> requirements;
            switch (stage) {
                case ROASTED_DUST -> {
                    kind = ProcessKind.ROASTING;
                    next = ChemistryPhase.SOLID;
                    requirements = List.of();
                }
                case SLURRY -> {
                    kind = ProcessKind.LEACHING;
                    next = ChemistryPhase.LIQUID;
                    requirements = leachingRequirements(profile);
                }
                case SOLUTION -> {
                    boolean fromSolid = current == ChemistryPhase.SOLID;
                    kind = fromSolid ? ProcessKind.DISSOLUTION : ProcessKind.CHEMICAL_REACTION;
                    next = ChemistryPhase.LIQUID;
                    requirements = fromSolid ? leachingRequirements(profile) : catalyst;
                }
                case REACTION_MIXTURE -> {
                    kind = ProcessKind.CHEMICAL_REACTION;
                    next = ChemistryPhase.LIQUID;
                    requirements = mergeRequirements(activator, catalyst);
                }
                default -> throw new IllegalStateException("Unhandled process stage " + stage);
            }
            Optional<String> problem = ioCapacityProblem(kind, current, oneVirtualOutput(next), requirements);
            if (problem.isPresent()) return problem;
            current = next;
        }

        ProcessKind recovery = switch (route) {
            case LEACHING_ELECTROWINNING -> ProcessKind.ELECTROWINNING;
            case LEACHING, CHEMICAL_REACTION -> ProcessKind.CHEMICAL_REACTION;
            default -> null;
        };
        if (recovery == null) {
            return Optional.of("Selected route " + route + " has no representable terminal recovery process.");
        }
        return ioCapacityProblem(
                recovery,
                current,
                finalOutputs,
                recovery == ProcessKind.CHEMICAL_REACTION ? catalyst : List.of()
        );
    }

    static Optional<String> ioCapacityProblem(
            ProcessKind kind,
            ChemistryPhase inputPhase,
            List<ProcessMaterial> outputs,
            List<ProcessRequirement> requirements
    ) {
        var recipeType = new ProcessRecipeResolver().recipeTypeFor(kind).orElse(null);
        if (recipeType == null) return Optional.of("No registered RecipeType exists for selected process " + kind + ".");

        int itemInputs = inputPhase.isFluidLike() ? 0 : 1;
        int fluidInputs = inputPhase.isFluidLike() ? 1 : 0;
        int itemOutputs = (int) outputs.stream().filter(value -> !value.phase().isFluidLike()).count();
        int fluidOutputs = (int) outputs.stream().filter(value -> value.phase().isFluidLike()).count();
        for (ProcessRequirement requirement : requirements) {
            if (requirement.role() == ProcessRequirement.Role.CHEMICAL_BALANCE) continue;
            if (requirement.requiredPhase().isFluidLike()) fluidInputs++;
            else itemInputs++;
        }

        if (itemInputs > recipeType.maxItemInputs() || itemOutputs > recipeType.maxItemOutputs()
                || fluidInputs > recipeType.maxFluidInputs() || fluidOutputs > recipeType.maxFluidOutputs()) {
            return Optional.of("Selected " + kind + " route exceeds existing " + recipeType.id()
                    + " IO limits: items " + itemInputs + "->" + itemOutputs
                    + ", fluids " + fluidInputs + "->" + fluidOutputs
                    + "; max " + recipeType.maxItemInputs() + "->" + recipeType.maxItemOutputs()
                    + ", " + recipeType.maxFluidInputs() + "->" + recipeType.maxFluidOutputs() + ".");
        }
        return Optional.empty();
    }

    static List<ProcessMaterial> oneVirtualOutput(ChemistryPhase phase) {
        return List.of(new ProcessMaterial(
                "__automatic_process_preview__",
                phase,
                1000,
                0,
                "ULV",
                true,
                null,
                null,
                ProcessSubstanceState.unknown(),
                Map.of()
        ));
    }

    public Result plan(MaterialAnalysis analysis, Map<String, MaterialAnalysis> registry) {
        List<ProcessPlan> plans = new ArrayList<>();
        List<ChemistryDiagnostic> diagnostics = new ArrayList<>();
        MaterialSnapshot material = analysis.source();

        if (AutomaticProcessIntermediate.isAutomatic(material)) {
            // Target-specific roasted/slurry/solution/reaction nodes are graph states, not feeds that
            // recursively invent their own decomposition/synthesis routes.
            return new Result(plans, diagnostics);
        }

        if (material.composition().isEmpty()) {
            // Elemental DUST -> INGOT is deliberately not generated here. The future metal
            // production system must decide reduction/smelting requirements explicitly.
            return new Result(plans, diagnostics);
        }

        if (material.backingMaterial() instanceof WoodMaterial wood) {
            // Wood is a composition-backed structure material. Its WOOD_PULP chemistry is planned
            // from the same calculated properties as compounds, while construction recipes remain
            // owned by the structure/wood systems.
            return WoodProcessingPlanner.plan(wood, analysis, registry);
        }

        IndustrialMaterial industrialBacking = material.backingMaterial() instanceof IndustrialMaterial backing
                ? backing
                : null;
        boolean mineralDustInput = industrialBacking != null && industrialBacking.isMineralDust();
        boolean oreMineral = GeologyMaterialRoles.isOreMineral(analysis);
        boolean hasCompositeFeed = industrialBacking != null && solidProcessingFeed(industrialBacking) != null;
        boolean automaticDustProcessing = hasCompositeFeed
                && industrialBacking != null
                && isAutomaticDustProcessingTarget(industrialBacking);

        // Clay and direct ceramic-brick profiles are closed gameplay families with dedicated
        // material recipes. Their composition still participates in chemistry analysis, but the
        // generic formation/decomposition emitter must not invent DUST/fluid forms they do not own.
        if (industrialBacking != null
                && (industrialBacking.isClayMaterial() || industrialBacking.isCeramicBrickMaterial())) {
            return new Result(plans, diagnostics);
        }

        if (automaticDustProcessing) {
            Optional<ProcessPlan> route = compositeDustRoute(analysis, registry, diagnostics);
            route.ifPresent(plans::add);
            // Decomposition/processing and formation are separate directions. Natural ore/mineral
            // definitions remain input-only, but a synthetic compound may also receive its balanced
            // ReactionSolver-backed formation route below.
        }

        if (mineralDustInput || oreMineral) {
            // mineralDust(...) is input-only by definition, even before any StoneMaterial references
            // it. .contains(...) may describe processing, but must never become an automatic
            // components -> mineral-dust synthesis route.
            return new Result(plans, diagnostics);
        }

        // Preserve the existing generic chemistry behaviour for non-ore compounds. A physical
        // mixture has two distinct directions: naturally/process-acquired mixtures may be
        // separated, while a synthetic mixture is made by MIXING without pretending bonds form.
        if (analysis.classifications().contains(MaterialClassification.PHYSICAL_MIXTURE)) {
            boolean syntheticTarget = !material.naturallyAcquired()
                    && !material.hasSource(MaterialSourceType.PROCESS_OUTPUT)
                    && !material.hasSource(MaterialSourceType.PROCESS_BYPRODUCT);
            if (syntheticTarget) {
                synthesisStep(analysis, registry, ProcessKind.MIXING, List.of(), diagnostics).ifPresent(step ->
                        plans.add(new ProcessPlan(material.id(), List.of(step))));
            } else if (!automaticDustProcessing) {
                physicalSeparation(analysis, registry, null, diagnostics).ifPresent(step ->
                        plans.add(new ProcessPlan(material.id(), List.of(step))));
            }
        } else if (analysis.classifications().contains(MaterialClassification.ALLOY) && !material.naturallyAcquired()) {
            synthesisStep(analysis, registry, ProcessKind.ALLOYING, List.of(), diagnostics).ifPresent(step ->
                    plans.add(new ProcessPlan(material.id(), List.of(step))));
        } else if (analysis.classifications().contains(MaterialClassification.POLYMER) && !material.naturallyAcquired()) {
            List<ProcessRequirement> requirements = new ArrayList<>();
            boolean reachable = true;
            if (analysis.properties().get("chemicalstability") < 35) {
                String catalystId = reagentResolver.resolveCatalyst(analysis, registry).orElse(null);
                if (catalystId == null) {
                    diagnostics.add(problem(analysis, "MISSING_FICTIONAL_CATALYST",
                            "The calculated polymerization route requires a fictional catalyst, but none is available at or below "
                                    + ProcessChainProfile.processingTierName(analysis.calculatedTierIndex()) + " processing."));
                    reachable = false;
                } else {
                    requirements.addAll(fixedAidRequirement(
                            ProcessRequirement.Role.CATALYST, catalystId, registry));
                }
            }
            if (reachable) {
                synthesisStep(analysis, registry, ProcessKind.POLYMERIZATION, requirements, diagnostics).ifPresent(step ->
                        plans.add(new ProcessPlan(material.id(), List.of(step))));
            }
        } else if (!material.naturallyAcquired()
                && !material.hasSource(MaterialSourceType.PROCESS_OUTPUT)
                && !material.hasSource(MaterialSourceType.PROCESS_BYPRODUCT)) {
            environmentRequirements(analysis, registry, diagnostics).ifPresent(requirements ->
                    synthesisStep(
                            analysis,
                            registry,
                            ProcessKind.CHEMICAL_REACTION,
                            requirements,
                            diagnostics
                    ).ifPresent(step -> plans.add(new ProcessPlan(material.id(), List.of(step))))
            );
        }

        return new Result(plans, diagnostics);
    }

    /**
     * Any registered composite DUST -> calculated treatment -> declared component outputs.
     * The input batch equals the sum of the declared top-level .contains(...) amounts. Nested child
     * composition is processed only in that child's own downstream plan.
     */
    private Optional<ProcessPlan> compositeDustRoute(
            MaterialAnalysis analysis,
            Map<String, MaterialAnalysis> registry,
            List<ChemistryDiagnostic> diagnostics
    ) {
        if (!(analysis.source().backingMaterial() instanceof IndustrialMaterial mineral)) {
            diagnostics.add(problem(analysis, "COMPOSITE_NO_BACKING", "Composite material has no IndustrialMaterial backing object."));
            return Optional.empty();
        }
        if (solidProcessingFeed(mineral) == null) {
            diagnostics.add(problem(analysis, "COMPOSITE_NO_FEED", "Composite material has no owned solid process feed."));
            return Optional.empty();
        }

        Map<String, Long> flattened = new LinkedHashMap<>();
        if (!flattenElements(analysis.source().id(), 1L, registry, flattened, new java.util.LinkedHashSet<>())) {
            diagnostics.add(problem(analysis, "COMPOSITE_COMPONENT_ROUTE_BLOCKED",
                    "Composition could not be flattened to registered elemental substances without a cycle or missing component."));
            return Optional.empty();
        }
        flattened.remove(analysis.source().id());
        if (flattened.isEmpty()) {
            diagnostics.add(problem(analysis, "COMPOSITE_NO_RECOVERABLE_COMPONENTS", "Composite material has no recoverable declared components."));
            return Optional.empty();
        }

        boolean physicalMixture = analysis.classifications().contains(MaterialClassification.PHYSICAL_MIXTURE);
        // Preserve the declared .contains(...) level in the emitted recipe. Flattened leaves are
        // still used above for exact conservation and cycle detection, but they are not allowed to
        // bypass legal definition components such as wood/stone/clay or another compound DUST.
        List<Map.Entry<String, Long>> orderedComponents = topLevelComponentWeights(analysis, registry);
        if (orderedComponents.isEmpty()) {
            diagnostics.add(problem(analysis, "COMPOSITE_COMPONENT_ROUTE_BLOCKED",
                    "Top-level .contains(...) components could not be resolved to exact conserved material-unit weights."));
            return Optional.empty();
        }

        long divisor = 0L;
        for (Map.Entry<String, Long> component : orderedComponents) {
            divisor = greatestCommonDivisor(divisor, component.getValue());
        }
        divisor = Math.max(1L, divisor);

        List<ProcessMaterial> componentOutputs = new ArrayList<>();
        long totalUnits = 0L;
        for (Map.Entry<String, Long> entry : orderedComponents) {
            MaterialAnalysis child = registry.get(entry.getKey());
            if (child == null || !hasMaterialForm(child.source().backingMaterial())) {
                diagnostics.add(problem(analysis, "ORE_MISSING_COMPONENT", "Missing registered component " + entry.getKey() + "."));
                return Optional.empty();
            }
            long amount = entry.getValue() / divisor;
            if (amount <= 0 || amount > Integer.MAX_VALUE) {
                diagnostics.add(problem(analysis, "ORE_INVALID_STOICHIOMETRY", "Invalid component amount for " + entry.getKey() + ": " + amount));
                return Optional.empty();
            }
            MaterialPart outputPart = outputPart(child, child.source().backingMaterial());
            if (outputPart == null) {
                diagnostics.add(problem(analysis, "ORE_COMPONENT_FORM_MISSING",
                        "No suitable registered .contains(...) output form exists for " + entry.getKey() + "."));
                return Optional.empty();
            }
            componentOutputs.add(processMaterial(child, amount, outputPart));
            totalUnits = Math.addExact(totalUnits, amount);
        }

        ProcessMaterial compoundDust = processMaterial(analysis, totalUnits, solidProcessingFeed(mineral));
        SeparationClassifier.Decision decision = separationClassifier.classify(analysis, componentOutputs, registry);
        ProcessChainProfile profile = ProcessChainProfile.forAnalysis(analysis, decision.route());
        if (!profile.enabled()) {
            diagnostics.add(new ChemistryDiagnostic(
                    ChemistryStatus.OK_WITH_REQUIREMENTS,
                    analysis.source().id(),
                    "PROCESS_TIER_OR_ROUTE_DEFERRED",
                    profile.explanation(),
                    List.of("Automatic processing is available only through LuV and must have a chemically valid, representable route."),
                    ""
            ));
            return Optional.empty();
        }

        int processingTier = ProcessChainProfile.processingTierIndex(analysis, decision.route());
        CompositeRoutePreview routePreview = new CompositeRoutePreview(decision, profile, componentOutputs);
        String catalystId = null;
        if (compositeRouteRequiresCatalyst(analysis, routePreview)) {
            catalystId = reagentResolver.resolveCatalyst(analysis, registry, processingTier).orElse(null);
            if (catalystId == null) {
                diagnostics.add(problem(analysis, "MISSING_FICTIONAL_CATALYST",
                        "The calculated compound-dust route contains a low-reactivity chemical-reaction step, "
                                + "but no fictional catalyst is available at or below "
                                + ProcessChainProfile.processingTierName(processingTier) + " processing. The whole chain was blocked."));
                return Optional.empty();
            }
        }

        ProcessPlan.Builder plan = ProcessPlan.builder(dustProcessingPlanId(mineral));

        if (physicalMixture) {
            return switch (decision.route()) {
                case CENTRIFUGING -> directRoute(plan, mineral, compoundDust, componentOutputs,
                        ProcessKind.CENTRIFUGING, List.of(), processingTier, decision.explanation());
                case MAGNETIC_SEPARATION -> directRoute(plan, mineral, compoundDust, componentOutputs,
                        ProcessKind.MAGNETIC_SEPARATION, List.of(), processingTier, decision.explanation());
                default -> {
                    diagnostics.add(problem(analysis, "NO_VALID_PHYSICAL_SEPARATION",
                            decision.explanation() + " A physical mixture may not silently fall back to a bond-breaking process."));
                    yield Optional.empty();
                }
            };
        }

        if (decision.route() == SeparationClassifier.Route.MOLTEN_ELECTROLYSIS
                || decision.route() == SeparationClassifier.Route.MOLTEN_ELECTROREFINING
                || decision.route() == SeparationClassifier.Route.MOLTEN_CHEMICAL_REACTION) {
            ProcessKind separation = switch (decision.route()) {
                case MOLTEN_ELECTROLYSIS -> ProcessKind.ELECTROLYSIS;
                case MOLTEN_ELECTROREFINING -> ProcessKind.ELECTROREFINING;
                default -> ProcessKind.CHEMICAL_REACTION;
            };
            return moltenRoute(analysis, mineral, compoundDust, componentOutputs, totalUnits, plan,
                    separation,
                    separation == ProcessKind.CHEMICAL_REACTION
                            ? catalystRequirements(catalystId, registry)
                            : List.of(),
                    processingTier,
                    decision.explanation(), diagnostics);
        }

        if (decision.route() == SeparationClassifier.Route.THERMAL_REDUCTION) {
            return thermalReductionRoute(analysis, registry, plan, compoundDust, componentOutputs, processingTier, diagnostics);
        }

        if (profile.targetSteps() <= 1) {
            ProcessKind directKind = switch (decision.route()) {
                case DIRECT_ELECTROLYSIS -> ProcessKind.ELECTROLYSIS;
                case CHEMICAL_REACTION -> ProcessKind.CHEMICAL_REACTION;
                default -> null;
            };
            if (directKind == null) {
                diagnostics.add(problem(analysis, "DIRECT_ROUTE_NOT_REPRESENTABLE",
                        "The selected " + decision.route() + " route cannot be represented as a one-step dust process without inventing a fake state."));
                return Optional.empty();
            }
            return directRoute(
                    plan,
                    mineral,
                    compoundDust,
                    componentOutputs,
                    directKind,
                    directKind == ProcessKind.CHEMICAL_REACTION
                            ? catalystRequirements(catalystId, registry)
                            : List.of(),
                    processingTier,
                    profile.explanation() + " " + decision.explanation()
            );
        }

        String activatorId = null;
        if (profile.requiresActivator()) {
            activatorId = reagentResolver.resolveActivator(
                    analysis,
                    registry,
                    catalystId == null ? Set.of() : Set.of(catalystId),
                    processingTier
            ).orElse(null);
            if (activatorId == null) {
                diagnostics.add(problem(analysis, "MISSING_FICTIONAL_ACTIVATOR",
                        "The calculated reaction-mixture stage requires a fictional activator, but none is available at or below "
                                + ProcessChainProfile.processingTierName(processingTier) + " processing."));
                return Optional.empty();
            }
        }

        ProcessMaterial current = compoundDust;

        if (profile.stages().contains(ProcessChainProfile.Stage.ROASTED_DUST)) {
            MaterialAnalysis roasted = requiredIntermediate(analysis, mineral, AutomaticProcessIntermediate.Kind.ROASTED_DUST, registry, diagnostics);
            if (roasted == null) return Optional.empty();
            ProcessMaterial next = processMaterial(roasted, totalUnits, MaterialPart.DUST);
            plan.step(new ProcessStep(
                    "materials/" + mineral.id() + "/roast_dust",
                    ProcessKind.ROASTING,
                    List.of(current),
                    List.of(next),
                    List.of(),
                    0,
                    processingTier,
                    "Thermal conditioning weakens the calculated stable/bonded structure without changing its elemental vector."
            ));
            current = next;
        }

        if (profile.stages().contains(ProcessChainProfile.Stage.SLURRY)) {
            MaterialAnalysis slurry = requiredIntermediate(analysis, mineral, AutomaticProcessIntermediate.Kind.SLURRY, registry, diagnostics);
            if (slurry == null) return Optional.empty();
            ProcessMaterial next = processMaterial(slurry, totalUnits, MaterialPart.LIQUID);
            plan.step(new ProcessStep(
                    "materials/" + mineral.id() + "/leach_to_slurry",
                    ProcessKind.LEACHING,
                    List.of(current),
                    List.of(next),
                    leachingRequirements(profile),
                    0,
                    processingTier,
                    "The machine Chemical Balance environment is held " + profile.leachEnvironment().name().toLowerCase(java.util.Locale.ROOT)
                            + " while the solid is converted into the registered slurry state; CB is an environment, not a material input."
            ));
            current = next;
        }

        if (profile.stages().contains(ProcessChainProfile.Stage.SOLUTION)) {
            MaterialAnalysis solution = requiredIntermediate(analysis, mineral, AutomaticProcessIntermediate.Kind.SOLUTION, registry, diagnostics);
            if (solution == null) return Optional.empty();
            ProcessMaterial next = processMaterial(solution, totalUnits, MaterialPart.LIQUID);
            boolean fromSolid = current.phase() == ChemistryPhase.SOLID;
            plan.step(new ProcessStep(
                    "materials/" + mineral.id() + (fromSolid ? "/dissolve_to_solution" : "/condition_slurry_to_solution"),
                    fromSolid ? ProcessKind.DISSOLUTION : ProcessKind.CHEMICAL_REACTION,
                    List.of(current),
                    List.of(next),
                    fromSolid ? leachingRequirements(profile) : catalystRequirements(catalystId, registry),
                    0,
                    processingTier,
                    fromSolid
                            ? "The bonded solid enters the registered solution while the machine maintains the calculated Chemical Balance range."
                            : "The suspension is chemically conditioned into the registered solution state without creating or deleting material units."
            ));
            current = next;
        }

        if (profile.stages().contains(ProcessChainProfile.Stage.REACTION_MIXTURE)) {
            MaterialAnalysis reaction = requiredIntermediate(analysis, mineral, AutomaticProcessIntermediate.Kind.REACTION_MIXTURE, registry, diagnostics);
            if (reaction == null) return Optional.empty();
            ProcessMaterial next = processMaterial(reaction, totalUnits, MaterialPart.LIQUID);
            plan.step(new ProcessStep(
                    "materials/" + mineral.id() + "/activate_reaction_mixture",
                    ProcessKind.CHEMICAL_REACTION,
                    List.of(current),
                    List.of(next),
                    mergeRequirements(
                            activatorRequirements(activatorId, registry),
                            catalystRequirements(catalystId, registry)
                    ),
                    0,
                    processingTier,
                    "Selective chemical conditioning prepares the final recovery state; required activator/catalyst aids are concrete fictional registered materials."
            ));
            current = next;
        }

        ProcessKind recovery = switch (decision.route()) {
            case LEACHING_ELECTROWINNING -> ProcessKind.ELECTROWINNING;
            case LEACHING, CHEMICAL_REACTION -> ProcessKind.CHEMICAL_REACTION;
            default -> throw new IllegalStateException(
                    "Unexpected non-terminal route " + decision.route() + " reached chemical recovery"
            );
        };
        plan.step(new ProcessStep(
                "materials/" + mineral.id() + "/recover_components",
                recovery,
                List.of(current),
                componentOutputs,
                recovery == ProcessKind.CHEMICAL_REACTION
                        ? catalystRequirements(catalystId, registry)
                        : List.of(),
                0,
                processingTier,
                profile.explanation() + " Final recovery uses " + recovery + " because the current registered state and calculated output chemistry support it."
        ));
        return Optional.of(plan.build());
    }

    private static List<ProcessMaterial> thermalReductionOutputs(
            List<ProcessMaterial> outputs,
            Map<String, MaterialAnalysis> registry
    ) {
        List<ProcessMaterial> result = new ArrayList<>(outputs.size());
        for (ProcessMaterial output : outputs) {
            MaterialAnalysis component = registry.get(output.materialId());
            if (component != null
                    && component.source().backingMaterial() instanceof IndustrialMaterial material
                    && net.mads.industron.material.MaterialCategory.of(material)
                    == net.mads.industron.material.MaterialCategory.METAL
                    && material.has(MaterialPart.MOLTEN_FLUID)) {
                result.add(processMaterialMilliUnits(component, output.milliUnits(), MaterialPart.MOLTEN_FLUID));
            } else {
                result.add(output);
            }
        }
        return List.copyOf(result);
    }

    private static Optional<ProcessPlan> thermalReductionRoute(
            MaterialAnalysis analysis,
            Map<String, MaterialAnalysis> registry,
            ProcessPlan.Builder plan,
            ProcessMaterial dust,
            List<ProcessMaterial> outputs,
            int minimumTierIndex,
            List<ChemistryDiagnostic> diagnostics
    ) {
        Map<String, MaterialSnapshot> snapshots = new LinkedHashMap<>();
        for (MaterialAnalysis registered : registry.values()) {
            snapshots.put(registered.source().id(), registered.source());
        }
        Optional<ReactionPlan> solved = ReactionSolver.decompose(analysis.source(), snapshots);
        if (solved.isEmpty()) {
            diagnostics.add(problem(analysis, "THERMAL_REDUCTION_NOT_BALANCED",
                    "Thermal reduction was selected from calculated chemistry, but ReactionSolver could not produce a balanced direct decomposition."));
            return Optional.empty();
        }
        ReactionPlan reaction = solved.orElseThrow();
        List<ProcessMaterial> hotOutputs = thermalReductionOutputs(outputs, registry);
        int temperature = Math.max(1, reaction.requiredTemperature());
        for (ProcessMaterial output : hotOutputs) {
            if (output.part() != MaterialPart.MOLTEN_FLUID
                    || !(output.backingMaterial() instanceof IndustrialMaterial metal)) continue;
            int meltingTemperature = metal.properties().hasProperty("meltingPoint")
                    ? Math.max(1, metal.meltingPoint())
                    : Math.max(1, metal.castTemperature());
            temperature = Math.max(temperature, meltingTemperature);
        }
        plan.step(new ProcessStep(
                "materials/" + analysis.source().id() + "/smelt_dust",
                ProcessKind.SMELTING,
                List.of(dust),
                hotOutputs,
                List.of(),
                temperature,
                minimumTierIndex,
                "ReactionSolver-balanced thermal reduction at " + temperature
                        + " C. Metallic products leave the furnace molten, so casting produces HOT forms that must cool. The SMELTING RecipeType carries the heat requirement for Blast Furnace/EBF executors."
        ));
        return Optional.of(plan.build());
    }

    private static Optional<ProcessPlan> directRoute(
            ProcessPlan.Builder plan,
            IndustrialMaterial material,
            ProcessMaterial dust,
            List<ProcessMaterial> outputs,
            ProcessKind kind,
            List<ProcessRequirement> requirements,
            int minimumTierIndex,
            String explanation
    ) {
        plan.step(new ProcessStep(
                "materials/" + material.id() + "/separate_dust",
                kind,
                List.of(dust),
                outputs,
                requirements,
                0,
                minimumTierIndex,
                explanation
        ));
        return Optional.of(plan.build());
    }

    private static Optional<ProcessPlan> moltenRoute(
            MaterialAnalysis analysis,
            IndustrialMaterial material,
            ProcessMaterial dust,
            List<ProcessMaterial> outputs,
            long units,
            ProcessPlan.Builder plan,
            ProcessKind separationKind,
            List<ProcessRequirement> separationRequirements,
            int minimumTierIndex,
            String explanation,
            List<ChemistryDiagnostic> diagnostics
    ) {
        if (!material.has(MaterialPart.MOLTEN_FLUID)) {
            diagnostics.add(missingIntermediateProblem(
                    analysis,
                    "MISSING_MOLTEN_INTERMEDIATE",
                    "The calculated route requires a real molten form before " + separationKind + ".",
                    ChemistryPhase.MOLTEN
            ));
            return Optional.empty();
        }

        ProcessMaterial molten = processMaterial(analysis, units, MaterialPart.MOLTEN_FLUID);
        int temperature = Math.max(1, material.meltingPoint());
        plan.step(new ProcessStep(
                "materials/" + material.id() + "/melt_dust",
                ProcessKind.MELTING,
                List.of(dust),
                List.of(molten),
                List.of(),
                temperature,
                minimumTierIndex,
                "The registered solid is converted to its own molten form at its calculated melting temperature."
        ));
        plan.step(new ProcessStep(
                "materials/" + material.id() + "/separate_molten",
                separationKind,
                List.of(molten),
                outputs,
                separationRequirements,
                temperature,
                minimumTierIndex,
                explanation
        ));
        return Optional.of(plan.build());
    }

    private static MaterialAnalysis requiredIntermediate(
            MaterialAnalysis owner,
            IndustrialMaterial parent,
            AutomaticProcessIntermediate.Kind kind,
            Map<String, MaterialAnalysis> registry,
            List<ChemistryDiagnostic> diagnostics
    ) {
        MaterialAnalysis resolved = AutomaticProcessIntermediate.find(parent, kind, registry).orElse(null);
        if (resolved == null) {
            diagnostics.add(missingIntermediateProblem(
                    owner,
                    "MISSING_" + kind.name(),
                    "The compiled chain requires " + kind.name().toLowerCase(java.util.Locale.ROOT)
                            + ", but that registry-backed state was not generated. The whole plan is blocked.",
                    kind.phase()
            ));
        }
        return resolved;
    }

    static List<ProcessRequirement> leachingRequirements(ProcessChainProfile profile) {
        if (profile.leachEnvironment() == ProcessChainProfile.LeachEnvironment.NONE) return List.of();

        // Acid/base chemistry is represented by the machine's Chemical Balance (CB), never by
        // inserting an acidic/basic fluid as a fake recipe input. Positive CB is acidic and
        // negative CB is basic; the recipe runtime/CB hatch supplies and maintains this environment.
        return List.of(ProcessRequirement.chemicalBalance(
                profile.chemicalBalanceMinimum(),
                profile.chemicalBalanceMaximum()
        ));
    }

    static List<ProcessRequirement> activatorRequirements(
            String activatorId,
            Map<String, MaterialAnalysis> registry
    ) {
        return fixedAidRequirement(ProcessRequirement.Role.ACTIVATOR, activatorId, registry);
    }

    static List<ProcessRequirement> catalystRequirements(
            String catalystId,
            Map<String, MaterialAnalysis> registry
    ) {
        return fixedAidRequirement(ProcessRequirement.Role.CATALYST, catalystId, registry);
    }

    static List<ProcessRequirement> mergeRequirements(
            List<ProcessRequirement> first,
            List<ProcessRequirement> second
    ) {
        if (first.isEmpty()) return second;
        if (second.isEmpty()) return first;
        List<ProcessRequirement> merged = new ArrayList<>(first.size() + second.size());
        merged.addAll(first);
        for (ProcessRequirement requirement : second) {
            if (!merged.contains(requirement)) merged.add(requirement);
        }
        return List.copyOf(merged);
    }

    private static List<ProcessRequirement> fixedAidRequirement(
            ProcessRequirement.Role role,
            String materialId,
            Map<String, MaterialAnalysis> registry
    ) {
        if (materialId == null) return List.of();
        MaterialAnalysis material = registry.get(materialId);
        if (material == null) return List.of();
        ChemistryPhase phase = ChemistryPhase.SOLID;
        if (material.source().backingMaterial() instanceof IndustrialMaterial backing) {
            if (solidProcessingFeed(backing) != null) phase = ChemistryPhase.SOLID;
            else if (backing.has(MaterialPart.LIQUID)) phase = ChemistryPhase.LIQUID;
            else if (backing.has(MaterialPart.GAS)) phase = ChemistryPhase.GAS;
            else phase = material.phase();
        } else {
            phase = material.phase();
        }
        return List.of(ProcessRequirement.fixed(role, phase, materialId, 1000));
    }

    /**
     * Any real composite IndustrialMaterial that owns a DUST (or explicit biological feed) is a one-way automatic
     * decomposition target. Elements have no composition and therefore never enter this path;
     * clay keeps its dedicated processing chain. Process intermediates are excluded earlier by
     * {@link AutomaticProcessIntermediate#isAutomatic(net.mads.industron.material.chemistry.MaterialSnapshot)}.
     *
     * <p>This intentionally is not limited to ORE/MINERAL_DUST. Normal registered compounds such as
     * gem-bearing or other fictional composite dusts must follow the same deterministic chemistry
     * rules. Decomposition and synthesis are planned independently: natural ore/mineral definitions
     * remain input-only, while synthetic compounds may also receive a ReactionSolver-balanced
     * formation route. Safety validation still rejects unbalanced or unrelated cycles.</p>
     */
    /** Concrete feed form used by automatic decomposition for a registered backing definition. */
    public static MaterialPart automaticProcessingInputPart(Object backing) {
        if (backing instanceof IndustrialMaterial material && isAutomaticDustProcessingTarget(material)) {
            return solidProcessingFeed(material);
        }
        if (backing instanceof StructureMaterial structure && isAutomaticStructureProcessingTarget(structure)) {
            return structure.generatedForms().contains(MaterialPart.WOOD_PULP)
                    || structure.hasExistingPart(MaterialPart.WOOD_PULP)
                    ? MaterialPart.WOOD_PULP
                    : MaterialPart.DUST;
        }
        return null;
    }

    /**
     * Structure-material decomposition is opt-in by owned process form. Wood currently owns the
     * WOOD_PULP chemistry feed; stone keeps its dedicated processing system and is therefore not
     * silently pulled into this graph merely because it also has DUST.
     */
    public static boolean isAutomaticStructureProcessingTarget(StructureMaterial material) {
        return material instanceof WoodMaterial
                && !material.components().isEmpty()
                && (material.generatedForms().contains(MaterialPart.WOOD_PULP)
                || material.hasExistingPart(MaterialPart.WOOD_PULP));
    }

    public static boolean isAutomaticCompositeProcessingTarget(Object backing) {
        if (backing instanceof IndustrialMaterial material) return isAutomaticDustProcessingTarget(material);
        if (backing instanceof StructureMaterial structure) return isAutomaticStructureProcessingTarget(structure);
        return false;
    }

    public static boolean isAutomaticDustProcessingTarget(IndustrialMaterial material) {
        return material != null
                && material.atomicNumber() == 0
                && !material.isClayMaterial()
                && solidProcessingFeed(material) != null
                && !material.components().isEmpty();
    }

    /** Biological compounds may own their real item rather than a fictitious dust. */
    private static MaterialPart solidProcessingFeed(IndustrialMaterial material) {
        if (material.has(MaterialPart.DUST)) return MaterialPart.DUST;
        if (material.contentProfile() == net.mads.industron.material.MaterialContentProfile.BIOLOGICAL
                && material.has(MaterialPart.BIOLOGICAL_FEED)) return MaterialPart.BIOLOGICAL_FEED;
        return null;
    }

    private static String dustProcessingPlanId(IndustrialMaterial material) {
        if (solidProcessingFeed(material) == MaterialPart.BIOLOGICAL_FEED) return material.id() + "_biological_processing";
        if (material.isOreMaterial()) return material.id() + "_ore_dust_processing";
        if (material.isMineralDust()) return material.id() + "_mineral_dust_processing";
        return material.id() + "_composite_dust_processing";
    }

    /**
     * Exact conserved weights for the declared top-level .contains(...) components.
     *
     * <p>The declaration boundary is authoritative. If a material says
     * {@code .contains(A x14, B x3, C x2, D x1)}, separating that material yields exactly the
     * {@code 14:3:2:1} ratio. A child component's own nested .contains(...) is used only when that
     * child is processed in a later recipe; it must never rescale its parent's declared ratio.</p>
     */
    static List<Map.Entry<String, Long>> topLevelComponentWeights(
            MaterialAnalysis analysis,
            Map<String, MaterialAnalysis> registry
    ) {
        if (analysis == null || analysis.source().composition().isEmpty()) return List.of();
        Map<String, Long> weights = new LinkedHashMap<>();
        for (CompositionEntry component : analysis.source().composition()) {
            MaterialAnalysis child = registry.get(component.substanceId());
            if (child == null || !hasMaterialForm(child.source().backingMaterial())) return List.of();
            if (component.amount() <= 0) return List.of();
            weights.merge(component.substanceId(), (long) component.amount(), Math::addExact);
        }
        return weights.entrySet().stream().map(entry -> Map.entry(entry.getKey(), entry.getValue())).toList();
    }

    private static long greatestCommonDivisor(long left, long right) {
        left = Math.abs(left);
        right = Math.abs(right);
        while (right != 0L) {
            long next = left % right;
            left = right;
            right = next;
        }
        return left;
    }

    /** Recursively flattens nested composition to atomic/elemental leaves. */
    private static boolean flattenElements(
            String materialId,
            long multiplier,
            Map<String, MaterialAnalysis> registry,
            Map<String, Long> result,
            Set<String> stack
    ) {
        MaterialAnalysis analysis = registry.get(materialId);
        if (analysis == null) return false;
        if (!stack.add(materialId)) return false;
        try {
            IndustrialMaterial backing = analysis.source().backingMaterial() instanceof IndustrialMaterial value ? value : null;
            if (backing != null && backing.atomicNumber() > 0) {
                result.merge(materialId, multiplier, Math::addExact);
                return true;
            }
            // .contains(...) is universal across registered material definitions. A definition with
            // no deeper composition is therefore a conserved terminal substance, not an invalid
            // chemistry node. This mirrors ProcessSafetyValidator.flatten(...) so registry preview,
            // planning and mass validation all interpret the same composition graph.
            if (analysis.source().composition().isEmpty()) {
                result.merge(materialId, multiplier, Math::addExact);
                return true;
            }
            for (CompositionEntry component : analysis.source().composition()) {
                if (!flattenElements(
                        component.substanceId(),
                        Math.multiplyExact(multiplier, component.amount()),
                        registry,
                        result,
                        stack
                )) return false;
            }
            return true;
        } finally {
            stack.remove(materialId);
        }
    }

    private Optional<ProcessStep> physicalSeparation(
            MaterialAnalysis analysis,
            Map<String, MaterialAnalysis> registry,
            MaterialPart inputPart,
            List<ChemistryDiagnostic> diagnostics
    ) {
        MaterialSnapshot material = analysis.source();
        List<ProcessMaterial> outputs = new ArrayList<>();
        long total = 0;
        for (CompositionEntry component : material.composition()) {
            MaterialAnalysis child = registry.get(component.substanceId());
            if (child == null) {
                diagnostics.add(missingComponentProblem(analysis, component));
                return Optional.empty();
            }
            Object childBacking = child.source().backingMaterial();
            if (!hasMaterialForm(childBacking)) {
                diagnostics.add(problem(analysis, "SEPARATION_COMPONENT_NO_BACKING",
                        "Component " + component.substanceId()
                                + " has no registered material/structure backing form, so this separation recipe was skipped."));
                return Optional.empty();
            }
            MaterialPart outputPart = outputPart(child, childBacking);
            if (outputPart == null) {
                diagnostics.add(formProblem(analysis, child.source().id(), child.phase(), childBacking));
                return Optional.empty();
            }
            long declaredAmount = component.amount();
            if (declaredAmount <= 0L) {
                diagnostics.add(problem(analysis, "SEPARATION_COMPONENT_AMOUNT_INVALID",
                        "Component " + component.substanceId() + " has a non-positive .contains(...) amount."));
                return Optional.empty();
            }
            outputs.add(processMaterial(child, declaredAmount, outputPart));
            total = Math.addExact(total, declaredAmount);
        }

        MaterialPart actualInputPart = inputPart != null ? inputPart : defaultPart(analysis);
        if (!(material.backingMaterial() instanceof IndustrialMaterial inputMaterial)) {
            diagnostics.add(problem(analysis, "SEPARATION_INPUT_NO_BACKING",
                    "Material has no IndustrialMaterial backing, so the separation recipe was skipped."));
            return Optional.empty();
        }
        if (actualInputPart == null || !inputMaterial.has(actualInputPart)) {
            diagnostics.add(formProblem(analysis, material.id(), analysis.phase(), inputMaterial));
            return Optional.empty();
        }

        ProcessMaterial input = processMaterial(analysis, total, actualInputPart);
        if (!ProcessSemantics.supportsPhysicalSeparation(input, outputs)) {
            diagnostics.add(problem(analysis, "SEPARATION_PHASE_MISMATCH",
                    "The registered input phase cannot be physically separated into the calculated output phases. "
                            + "Add an explicit phase-conversion, solution or slurry intermediate."));
            return Optional.empty();
        }
        Optional<ProcessKind> kind = choosePhysicalSeparation(input, outputs, registry);
        if (kind.isEmpty()) {
            diagnostics.add(problem(analysis, "NO_VALID_PHYSICAL_SEPARATION",
                    "No registered physical process rule matches the mixture state/properties. "
                            + "Generation was rejected instead of falling back to centrifuging."));
            return Optional.empty();
        }
        return Optional.of(new ProcessStep(
                "materials/" + material.id() + "_separation",
                kind.orElseThrow(),
                List.of(input),
                outputs,
                List.of(),
                "Automatic physical separation from top-level composition."
        ));
    }

    private static Optional<ProcessKind> choosePhysicalSeparation(
            ProcessMaterial input,
            List<ProcessMaterial> outputs,
            Map<String, MaterialAnalysis> registry
    ) {
        if (!input.isPhysicalMixture() || outputs.size() < 2) return Optional.empty();

        if (input.phase() == ChemistryPhase.SOLID) {
            boolean anyMagnetic = outputs.stream().anyMatch(ProcessPlanner::isMagnetic);
            boolean anyNonMagnetic = outputs.stream().anyMatch(output -> !isMagnetic(output));
            if (anyMagnetic && anyNonMagnetic) return Optional.of(ProcessKind.MAGNETIC_SEPARATION);

            double densitySpread = rawPropertySpread(outputs, registry, "density");
            if (densitySpread >= 12.0D) return Optional.of(ProcessKind.CENTRIFUGING);
            return Optional.empty();
        }

        if (input.phase() == ChemistryPhase.GAS || input.phase() == ChemistryPhase.PLASMA) {
            return outputs.stream().allMatch(output -> output.phase() == ChemistryPhase.GAS
                    || output.phase() == ChemistryPhase.PLASMA)
                    ? Optional.of(ProcessKind.GAS_SEPARATION)
                    : Optional.empty();
        }

        if (input.phase() == ChemistryPhase.MIXED
                || input.substanceState().mixtureKind() == ProcessSubstanceState.MixtureKind.PHASE_SEPARATED) {
            long distinctPhases = outputs.stream().map(ProcessMaterial::phase).distinct().count();
            return distinctPhases > 1 ? Optional.of(ProcessKind.PHASE_SEPARATION) : Optional.empty();
        }

        if (input.phase() == ChemistryPhase.LIQUID || input.phase() == ChemistryPhase.MOLTEN) {
            long solids = outputs.stream().filter(output -> output.phase() == ChemistryPhase.SOLID).count();
            if (solids > 0 && solids < outputs.size()) return Optional.of(ProcessKind.FILTRATION);

            boolean allFluidOrGas = outputs.stream().allMatch(output -> output.phase().isFluidLike());
            if (!allFluidOrGas) return Optional.empty();

            boolean anyGas = outputs.stream().anyMatch(output -> output.phase() == ChemistryPhase.GAS
                    || output.phase() == ChemistryPhase.PLASMA);
            if (anyGas) return Optional.of(ProcessKind.DISTILLATION);

            List<Double> boilingPoints = rawPropertyValues(outputs, registry, "boilingpoint");
            if (boilingPoints.size() >= 2) {
                boilingPoints = boilingPoints.stream().sorted().toList();
                double spread = boilingPoints.getLast() - boilingPoints.getFirst();
                double minimumGap = Double.POSITIVE_INFINITY;
                for (int index = 1; index < boilingPoints.size(); index++) {
                    minimumGap = Math.min(minimumGap, boilingPoints.get(index) - boilingPoints.get(index - 1));
                }
                if (spread >= 5.0D && minimumGap <= 35.0D) return Optional.of(ProcessKind.FRACTIONATION);
                if (spread >= 15.0D) return Optional.of(ProcessKind.DISTILLATION);
            }

            double volatilitySpread = rawPropertySpread(outputs, registry, "volatility");
            if (volatilitySpread >= 30.0D) return Optional.of(ProcessKind.DISTILLATION);
            if (volatilitySpread >= 8.0D) return Optional.of(ProcessKind.FRACTIONATION);

            // A homogeneous liquid mixture is not silently reclassified as two phases from polarity
            // alone. PHASE_SEPARATION requires an explicit PHASE_SEPARATED/MIXED state supplied by
            // chemistry or a future runtime Foundry payload.
            return Optional.empty();
        }

        return Optional.empty();
    }

    private static double rawPropertySpread(
            List<ProcessMaterial> values,
            Map<String, MaterialAnalysis> registry,
            String property
    ) {
        List<Double> resolved = rawPropertyValues(values, registry, property);
        if (resolved.size() < 2) return 0.0D;
        double minimum = resolved.stream().mapToDouble(Double::doubleValue).min().orElse(0.0D);
        double maximum = resolved.stream().mapToDouble(Double::doubleValue).max().orElse(0.0D);
        return maximum - minimum;
    }

    private static List<Double> rawPropertyValues(
            List<ProcessMaterial> values,
            Map<String, MaterialAnalysis> registry,
            String property
    ) {
        List<Double> result = new ArrayList<>();
        for (ProcessMaterial value : values) {
            double propertyValue = value.processProperty(property);
            if (!Double.isFinite(propertyValue)) {
                MaterialAnalysis analysis = registry.get(value.materialId());
                if (analysis != null) propertyValue = analysis.properties().get(property);
            }
            if (Double.isFinite(propertyValue)) result.add(propertyValue);
        }
        return List.copyOf(result);
    }

    private static boolean isMagnetic(ProcessMaterial material) {
        double magneticStrength = material.processProperty("magneticstrength");
        if (Double.isFinite(magneticStrength)) return magneticStrength > 0.0D;
        return material.backingMaterial() instanceof IndustrialMaterial industrial
                && industrial.properties().magnetic()
                && industrial.properties().magneticStrength() > 0;
    }

    private Optional<ProcessStep> synthesisStep(
            MaterialAnalysis analysis,
            Map<String, MaterialAnalysis> registry,
            ProcessKind kind,
            List<ProcessRequirement> requirements,
            List<ChemistryDiagnostic> diagnostics
    ) {
        Map<String, MaterialSnapshot> snapshots = new LinkedHashMap<>();
        for (MaterialAnalysis registered : registry.values()) {
            snapshots.put(registered.source().id(), registered.source());
        }
        Optional<ReactionPlan> solved = ReactionSolver.synthesize(analysis.source(), snapshots);
        if (solved.isEmpty()) {
            diagnostics.add(problem(analysis, "SYNTHESIS_NOT_BALANCED",
                    "ReactionSolver could not produce an atom/conserved-composition balanced formation plan from .contains(...)."));
            return Optional.empty();
        }

        ReactionPlan reaction = solved.orElseThrow();
        List<ProcessMaterial> inputs = new ArrayList<>();
        for (ReactionParticipant participant : reaction.reactants()) {
            MaterialAnalysis child = registry.get(participant.materialId());
            if (child == null) {
                diagnostics.add(problem(analysis, "SYNTHESIS_COMPONENT_MISSING",
                        "ReactionSolver references missing component " + participant.materialId() + "."));
                return Optional.empty();
            }
            Object childBacking = child.source().backingMaterial();
            MaterialPart childPart = defaultPart(child);
            if (!hasMaterialForm(childBacking) || childPart == null || !hasPart(childBacking, childPart)) {
                diagnostics.add(formProblem(analysis, child.source().id(), child.phase(), childBacking));
                return Optional.empty();
            }
            inputs.add(processMaterialMilliUnits(child, participant.milliUnits(), childPart));
        }

        if (!(analysis.source().backingMaterial() instanceof IndustrialMaterial outputMaterial)) {
            diagnostics.add(problem(analysis, "SYNTHESIS_OUTPUT_NO_BACKING",
                    "Target material has no IndustrialMaterial backing, so this synthesis recipe was skipped."));
            return Optional.empty();
        }
        MaterialPart outputPart = defaultPart(analysis);
        if (outputPart == null || !outputMaterial.has(outputPart)) {
            diagnostics.add(formProblem(analysis, analysis.source().id(), analysis.phase(), outputMaterial));
            return Optional.empty();
        }
        ReactionParticipant product = reaction.products().stream()
                .filter(value -> value.materialId().equals(analysis.source().id()))
                .findFirst()
                .orElse(null);
        if (product == null) {
            diagnostics.add(problem(analysis, "SYNTHESIS_PRODUCT_MISSING",
                    "ReactionSolver formation plan does not contain the requested target product."));
            return Optional.empty();
        }

        ProcessMaterial output = processMaterialMilliUnits(analysis, product.milliUnits(), outputPart);
        return Optional.of(new ProcessStep(
                "materials/" + analysis.source().id() + "_synthesis",
                kind,
                inputs,
                List.of(output),
                requirements,
                reaction.requiredTemperature(),
                0,
                "ReactionSolver-balanced automatic formation from direct .contains(...) composition."
        ));
    }

    private Optional<List<ProcessRequirement>> environmentRequirements(
            MaterialAnalysis analysis,
            Map<String, MaterialAnalysis> registry,
            List<ChemistryDiagnostic> diagnostics
    ) {
        List<ProcessRequirement> result = new ArrayList<>();
        double acidity = analysis.properties().get("acidity");
        double reactivity = analysis.properties().get("reactivity");
        if (Math.abs(acidity) >= 35) {
            double min = acidity > 0 ? Math.max(1, acidity - 15) : Math.min(-1, acidity - 15);
            double max = acidity > 0 ? Math.min(100, acidity + 15) : Math.min(-1, acidity + 15);
            if (min > max) { double value = min; min = max; max = value; }
            result.add(ProcessRequirement.chemicalBalance(min, max));
        }
        if (reactivity < 30) {
            String catalystId = reagentResolver.resolveCatalyst(analysis, registry).orElse(null);
            if (catalystId == null) {
                diagnostics.add(problem(analysis, "MISSING_FICTIONAL_CATALYST",
                        "The calculated chemical-reaction route requires a fictional catalyst, but none is available at or below "
                                + analysis.calculatedTierName() + "."));
                return Optional.empty();
            }
            result.addAll(fixedAidRequirement(ProcessRequirement.Role.CATALYST, catalystId, registry));
        }
        return Optional.of(List.copyOf(result));
    }

    static MaterialPart outputPart(MaterialAnalysis analysis, Object material) {
        // Global .contains(...) representation precedence. Any definition type under material
        // definitions may participate: ordinary/elemental materials use DUST first, wood structure
        // materials use WOOD_PULP, biological compounds may own their existing feed item, then fluid/gas forms
        // apply. The default physical state must never hide a concrete registered solid form.
        if (hasPart(material, MaterialPart.DUST)) return MaterialPart.DUST;
        if (hasPart(material, MaterialPart.WOOD_PULP)) return MaterialPart.WOOD_PULP;
        if (hasPart(material, MaterialPart.BIOLOGICAL_FEED)) return MaterialPart.BIOLOGICAL_FEED;
        if (hasPart(material, MaterialPart.LIQUID)) return MaterialPart.LIQUID;
        if (hasPart(material, MaterialPart.GAS)) return MaterialPart.GAS;
        return null;
    }

    static MaterialPart defaultPart(MaterialAnalysis analysis) {
        Object backing = analysis.source().backingMaterial();
        if (!hasMaterialForm(backing)) return null;
        return outputPart(analysis, backing);
    }

    static ProcessMaterial processMaterial(MaterialAnalysis analysis, long amount, MaterialPart part) {
        return processMaterialMilliUnits(analysis, Math.multiplyExact(amount, 1000L), part);
    }

    static ProcessMaterial processMaterialMilliUnits(MaterialAnalysis analysis, long milliUnits, MaterialPart part) {
        Object backing = analysis.source().backingMaterial();
        if (!hasMaterialForm(backing)) {
            throw new IllegalStateException("Material " + analysis.source().id() + " has no supported backing definition");
        }
        if (part == null || !hasPart(backing, part)) {
            throw new IllegalStateException("Material " + analysis.source().id() + " has no form " + part);
        }
        if (milliUnits <= 0L) throw new IllegalArgumentException("Material amount must be positive");
        ChemistryPhase phase = part.isFluid() ? phaseForPart(part) : ChemistryPhase.SOLID;
        return new ProcessMaterial(
                analysis.source().id(),
                phase,
                milliUnits,
                analysis.calculatedTierIndex(),
                analysis.calculatedTierName(),
                true,
                backing,
                part,
                ProcessSubstanceState.fromAnalysis(analysis),
                analysis.properties().values()
        );
    }

    private static boolean hasMaterialForm(Object backing) {
        return backing instanceof IndustrialMaterial || backing instanceof StructureMaterial;
    }

    private static boolean hasPart(Object backing, MaterialPart part) {
        if (backing instanceof IndustrialMaterial material) return material.has(part);
        if (backing instanceof StructureMaterial material) {
            return material.hasExistingPart(part) || material.generatedForms().contains(part);
        }
        return false;
    }

    private static ChemistryPhase phaseForPart(MaterialPart part) {
        return switch (part) {
            case GAS -> ChemistryPhase.GAS;
            case LIQUID -> ChemistryPhase.LIQUID;
            case MOLTEN_FLUID -> ChemistryPhase.MOLTEN;
            default -> ChemistryPhase.SOLID;
        };
    }


    private static ChemistryDiagnostic missingComponentProblem(MaterialAnalysis analysis, CompositionEntry component) {
        return new ChemistryDiagnostic(
                ChemistryStatus.MISSING_MATERIAL,
                analysis.source().id(),
                "MISSING_COMPONENT_MATERIAL",
                "Component " + component.substanceId() + " is referenced by .contains(...) but no registered material with that id exists. The automatic recipe was skipped.",
                List.of(
                        "Define/register the missing fictional material before expecting this recipe to generate.",
                        "Keep the existing composition ratio; do not replace it with a different material just to satisfy recipe generation."
                ),
                "// " + analysis.source().id() + " already expects this component:\n"
                        + ".contains(component(" + component.substanceId().toUpperCase(java.util.Locale.ROOT) + ", " + component.amount() + "))"
        );
    }

    private static ChemistryDiagnostic formProblem(
            MaterialAnalysis owner,
            String materialId,
            ChemistryPhase phase,
            Object backing
    ) {
        String expected = phase == ChemistryPhase.GAS || phase == ChemistryPhase.PLASMA
                ? "GAS"
                : phase == ChemistryPhase.LIQUID
                ? "LIQUID"
                : phase == ChemistryPhase.MOLTEN
                ? "MOLTEN_FLUID"
                : backing instanceof StructureMaterial && hasPart(backing, MaterialPart.WOOD_PULP)
                ? "DUST or WOOD_PULP"
                : backing instanceof IndustrialMaterial material
                    && material.contentProfile() == net.mads.industron.material.MaterialContentProfile.BIOLOGICAL
                ? "DUST or BIOLOGICAL_FEED"
                : "DUST";
        return new ChemistryDiagnostic(
                ChemistryStatus.CHANGE_REQUIRED,
                owner.source().id(),
                "MISSING_PROCESS_FORM",
                "Material " + materialId + " needs a registered .contains(...) process form (" + expected
                        + ") for phase " + phase + ", but none is available. The automatic recipe was skipped.",
                List.of("Add/map a physically valid DUST/WOOD_PULP/BIOLOGICAL_FEED/LIQUID/GAS form on the referenced definition, or correct the composition so the automatic route is not selected."),
                "// Required registered composition form for " + materialId + ": " + expected
        );
    }

    private static ChemistryDiagnostic missingIntermediateProblem(
            MaterialAnalysis analysis,
            String code,
            String message,
            ChemistryPhase requiredPhase
    ) {
        return new ChemistryDiagnostic(
                ChemistryStatus.CHANGE_REQUIRED,
                analysis.source().id(),
                code,
                message + " No physically invalid direct recipe was emitted.",
                List.of(
                        "Ensure automatic process intermediates are registered before item/fluid registries and rerun runData.",
                        "Do not replace the missing liquid or molten stage with direct dust distillation/electrorefining."
                ),
                "// Expected automatic process intermediate for " + analysis.source().id()
                        + " in phase " + requiredPhase
        );
    }

    private static ChemistryDiagnostic problem(MaterialAnalysis analysis, String code, String message) {
        return new ChemistryDiagnostic(
                ChemistryStatus.OK_WITH_REQUIREMENTS,
                analysis.source().id(),
                code,
                message,
                List.of("Define the missing generic capability/form; chemistry topology and properties remain derived from .contains(...). No invalid shortcut recipe was emitted."),
                ""
        );
    }

    private static double normalized(double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(100.0D, value));
    }

}
