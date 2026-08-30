package net.mads.industron.material.chemistry.process;

import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.chemistry.ChemicalStructure;
import net.mads.industron.material.chemistry.ChemistryDiagnostic;
import net.mads.industron.material.chemistry.ChemistryPhase;
import net.mads.industron.material.chemistry.ChemistryStatus;
import net.mads.industron.material.chemistry.CompositionEntry;
import net.mads.industron.material.chemistry.MaterialAnalysis;
import net.mads.industron.material.chemistry.MaterialClassification;
import net.mads.industron.material.chemistry.MaterialSnapshot;
import net.mads.industron.material.chemistry.MaterialSourceType;
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
    public record Result(List<ProcessPlan> plans, List<ChemistryDiagnostic> diagnostics) {
        public Result {
            plans = List.copyOf(plans);
            diagnostics = List.copyOf(diagnostics);
        }
    }

    public Result plan(MaterialAnalysis analysis, Map<String, MaterialAnalysis> registry) {
        List<ProcessPlan> plans = new ArrayList<>();
        List<ChemistryDiagnostic> diagnostics = new ArrayList<>();
        MaterialSnapshot material = analysis.source();

        if (material.composition().isEmpty()) {
            pureElementFinishing(analysis).ifPresent(plans::add);
            return new Result(plans, diagnostics);
        }

        boolean mineralDustInput = material.backingMaterial() instanceof IndustrialMaterial backing
                && backing.isMineralDust();
        boolean oreMineral = GeologyMaterialRoles.isOreMineral(analysis);
        if (mineralDustInput || oreMineral) {
            // mineralDust(...) is input-only by definition, even before any StoneMaterial references
            // it. .contains(...) may describe processing, but must never become an automatic
            // components -> mineral-dust synthesis route.
            Optional<ProcessPlan> route = oreDustRoute(analysis, registry, diagnostics);
            route.ifPresent(plans::add);
            return new Result(plans, diagnostics);
        }

        // Preserve the existing generic chemistry behaviour for non-ore compounds.
        if (analysis.classifications().contains(MaterialClassification.PHYSICAL_MIXTURE)) {
            physicalSeparation(analysis, registry, null, diagnostics).ifPresent(step ->
                    plans.add(new ProcessPlan(material.id(), List.of(step))));
        } else if (analysis.classifications().contains(MaterialClassification.ALLOY) && !material.naturallyAcquired()) {
            synthesisStep(analysis, registry, ProcessKind.ALLOYING, List.of(), diagnostics).ifPresent(step ->
                    plans.add(new ProcessPlan(material.id(), List.of(step))));
        } else if (analysis.classifications().contains(MaterialClassification.POLYMER) && !material.naturallyAcquired()) {
            List<ProcessRequirement> requirements = new ArrayList<>();
            if (analysis.properties().get("chemicalstability") < 35) {
                requirements.add(new ProcessRequirement(
                        ProcessRequirement.Role.CATALYST,
                        ChemistryPhase.SOLID,
                        List.of(PropertyConstraint.between("catalyticactivity", 45, 78)),
                        false,
                        1000
                ));
            }
            synthesisStep(analysis, registry, ProcessKind.POLYMERIZATION, requirements, diagnostics).ifPresent(step ->
                    plans.add(new ProcessPlan(material.id(), List.of(step))));
        } else if (!material.naturallyAcquired()
                && !material.hasSource(MaterialSourceType.PROCESS_OUTPUT)
                && !material.hasSource(MaterialSourceType.PROCESS_BYPRODUCT)) {
            synthesisStep(
                    analysis,
                    registry,
                    ProcessKind.CHEMICAL_REACTION,
                    environmentRequirements(analysis),
                    diagnostics
            ).ifPresent(step -> plans.add(new ProcessPlan(material.id(), List.of(step))));
        }

        return new Result(plans, diagnostics);
    }

    /** Pure solid metallic element dust -> ingot. Temperature comes from the material, never machine tier. */
    private Optional<ProcessPlan> pureElementFinishing(MaterialAnalysis analysis) {
        if (!(analysis.source().backingMaterial() instanceof IndustrialMaterial material)) return Optional.empty();
        if (material.atomicNumber() <= 0 || analysis.phase() != ChemistryPhase.SOLID) return Optional.empty();
        if (!material.properties().metal() || !material.has(MaterialPart.DUST) || !material.has(MaterialPart.INGOT)) return Optional.empty();

        ProcessMaterial input = solid(analysis, 1, MaterialPart.DUST);
        ProcessMaterial output = solid(analysis, 1, MaterialPart.INGOT);
        ProcessStep step = new ProcessStep(
                "materials/" + material.id() + "/dust_to_ingot",
                ProcessKind.SMELTING,
                List.of(input),
                List.of(output),
                List.of(),
                Math.max(1, material.meltingPoint()),
                "Pure elemental dust is already chemically separated; smelting only changes its physical form."
        );
        return Optional.of(new ProcessPlan(material.id() + "_finishing", List.of(step)));
    }

    /**
     * Reviewed mineral DUST -> calculated treatment -> elemental outputs.
     * The input batch equals the sum of flattened top-level element units so every generated step is balanced.
     */
    private Optional<ProcessPlan> oreDustRoute(
            MaterialAnalysis analysis,
            Map<String, MaterialAnalysis> registry,
            List<ChemistryDiagnostic> diagnostics
    ) {
        if (!(analysis.source().backingMaterial() instanceof IndustrialMaterial mineral)) {
            diagnostics.add(problem(analysis, "ORE_NO_BACKING", "Ore mineral has no IndustrialMaterial backing object."));
            return Optional.empty();
        }
        if (!mineral.has(MaterialPart.DUST)) {
            diagnostics.add(problem(analysis, "ORE_NO_DUST", "Ore mineral has no DUST form, so post-dust processing cannot start."));
            return Optional.empty();
        }

        Map<String, Long> flattened = new LinkedHashMap<>();
        if (!flattenElements(analysis.source().id(), 1L, registry, flattened, new java.util.LinkedHashSet<>())) {
            diagnostics.add(problem(analysis, "ORE_COMPONENT_ROUTE_BLOCKED",
                    "Ore composition could not be flattened to registered elemental substances without a cycle or missing component."));
            return Optional.empty();
        }
        flattened.remove(analysis.source().id());
        if (flattened.isEmpty()) {
            diagnostics.add(problem(analysis, "ORE_NO_RECOVERABLE_ELEMENTS", "Ore mineral has no recoverable elemental leaves."));
            return Optional.empty();
        }

        List<ProcessMaterial> elementalOutputs = new ArrayList<>();
        long totalUnits = 0L;
        for (Map.Entry<String, Long> entry : flattened.entrySet()) {
            MaterialAnalysis child = registry.get(entry.getKey());
            if (child == null || !(child.source().backingMaterial() instanceof IndustrialMaterial childMaterial)) {
                diagnostics.add(problem(analysis, "ORE_MISSING_COMPONENT", "Missing registered component " + entry.getKey() + "."));
                return Optional.empty();
            }
            long amount = entry.getValue();
            if (amount <= 0 || amount > Integer.MAX_VALUE) {
                diagnostics.add(problem(analysis, "ORE_INVALID_STOICHIOMETRY", "Invalid component amount for " + entry.getKey() + ": " + amount));
                return Optional.empty();
            }
            MaterialPart outputPart = outputPart(child, childMaterial);
            if (outputPart == null) {
                diagnostics.add(problem(analysis, "ORE_COMPONENT_FORM_MISSING",
                        "No suitable elemental output form exists for " + entry.getKey() + "."));
                return Optional.empty();
            }
            elementalOutputs.add(processMaterial(child, amount, outputPart));
            totalUnits = Math.addExact(totalUnits, amount);
        }

        ProcessMaterial compoundDust = processMaterial(analysis, totalUnits, MaterialPart.DUST);
        String processingSuffix = mineral.isOreMaterial() ? "_ore_dust_processing" : "_mineral_dust_processing";
        ProcessPlan.Builder plan = ProcessPlan.builder(analysis.source().id() + processingSuffix);

        ChemicalStructure.Topology topology = inferredTopology(analysis);
        if (topology == ChemicalStructure.Topology.PHYSICAL_MIXTURE
                || analysis.classifications().contains(MaterialClassification.PHYSICAL_MIXTURE)) {
            ProcessKind kind = choosePhysicalSeparation(elementalOutputs, registry);
            plan.step(new ProcessStep(
                    "materials/" + mineral.id() + "/dust_separation",
                    kind,
                    List.of(compoundDust),
                    elementalOutputs,
                    List.of(),
                    0,
                    "The source is a physical mixture, so its already-distinct fractions may be separated without breaking chemical bonds."
            ));
            return Optional.of(plan.build());
        }

        if (topology == ChemicalStructure.Topology.METALLIC_LATTICE
                || analysis.classifications().contains(MaterialClassification.ALLOY)) {
            return metallicLatticeRoute(analysis, mineral, compoundDust, elementalOutputs, totalUnits, plan, diagnostics);
        }

        if (topology == ChemicalStructure.Topology.IONIC_LATTICE
                || analysis.classifications().contains(MaterialClassification.IONIC_COMPOUND)) {
            return ionicRoute(analysis, mineral, compoundDust, elementalOutputs, totalUnits, plan, diagnostics);
        }

        return boundCompoundRoute(analysis, mineral, compoundDust, elementalOutputs, totalUnits, plan, diagnostics);
    }

    private Optional<ProcessPlan> metallicLatticeRoute(
            MaterialAnalysis analysis,
            IndustrialMaterial mineral,
            ProcessMaterial dust,
            List<ProcessMaterial> outputs,
            long units,
            ProcessPlan.Builder plan,
            List<ChemistryDiagnostic> diagnostics
    ) {
        if (mineral.has(MaterialPart.MOLTEN_FLUID)) {
            ProcessMaterial molten = processMaterial(analysis, units, MaterialPart.MOLTEN_FLUID);
            int meltingPoint = Math.max(1, mineral.meltingPoint());
            plan.step(new ProcessStep(
                    "materials/" + mineral.id() + "/melt_dust",
                    ProcessKind.MELTING,
                    List.of(dust),
                    List.of(molten),
                    List.of(),
                    meltingPoint,
                    "Metallic lattice is melted before electrochemical separation."
            ));
            plan.step(new ProcessStep(
                    "materials/" + mineral.id() + "/electrorefine_melt",
                    ProcessKind.ELECTROREFINING,
                    List.of(molten),
                    outputs,
                    List.of(),
                    meltingPoint,
                    "Electrorefining separates the molten metallic component stream; centrifuging is not used to break the lattice."
            ));
            return Optional.of(plan.build());
        }

        plan.step(new ProcessStep(
                "materials/" + mineral.id() + "/electrorefine_dust",
                ProcessKind.ELECTROREFINING,
                List.of(dust),
                outputs,
                List.of(),
                0,
                "This mineral intentionally owns only a dust form, so calculated electrorefining consumes the dust directly instead of inventing a molten material form."
        ));
        return Optional.of(plan.build());
    }

    private Optional<ProcessPlan> ionicRoute(
            MaterialAnalysis analysis,
            IndustrialMaterial mineral,
            ProcessMaterial dust,
            List<ProcessMaterial> outputs,
            long units,
            ProcessPlan.Builder plan,
            List<ChemistryDiagnostic> diagnostics
    ) {
        if (mineral.has(MaterialPart.MOLTEN_FLUID)) {
            ProcessMaterial molten = processMaterial(analysis, units, MaterialPart.MOLTEN_FLUID);
            int meltingPoint = Math.max(1, mineral.meltingPoint());
            plan.step(new ProcessStep(
                    "materials/" + mineral.id() + "/melt_dust",
                    ProcessKind.MELTING,
                    List.of(dust),
                    List.of(molten),
                    List.of(),
                    meltingPoint,
                    "Bound ionic material is melted so charge carriers can move before electrolysis."
            ));
            plan.step(new ProcessStep(
                    "materials/" + mineral.id() + "/electrolyse_melt",
                    ProcessKind.ELECTROLYSIS,
                    List.of(molten),
                    outputs,
                    List.of(),
                    meltingPoint,
                    "Electrolysis is selected from ionic structure, not merely because the input is a fluid."
            ));
            return Optional.of(plan.build());
        }

        plan.step(new ProcessStep(
                "materials/" + mineral.id() + "/electrolyse_dust",
                ProcessKind.ELECTROLYSIS,
                List.of(dust),
                outputs,
                List.of(),
                0,
                "This mineral intentionally owns only a dust form. Electrolysis therefore consumes the registered dust directly instead of inventing a molten output/form."
        ));
        return Optional.of(plan.build());
    }

    private Optional<ProcessPlan> boundCompoundRoute(
            MaterialAnalysis analysis,
            IndustrialMaterial mineral,
            ProcessMaterial dust,
            List<ProcessMaterial> outputs,
            long units,
            ProcessPlan.Builder plan,
            List<ChemistryDiagnostic> diagnostics
    ) {
        double bond = analysis.properties().get("bondstrength");
        double stability = analysis.properties().get("chemicalstability");
        double reactivity = analysis.properties().get("reactivity");
        double polarity = analysis.properties().get("polarity");
        double crystal = analysis.properties().get("crystalstability");
        double brittleness = analysis.properties().get("brittleness");
        double volatility = analysis.properties().get("volatility");

        ProcessMaterial current = dust;
        boolean hasPurified = mineral.has(MaterialPart.PURIFIED_DUST);

        if (hasPurified) {
            ProcessKind treatment;
            int temperature = 0;
            String explanation;

            if (stability >= 68 || bond >= 68) {
                treatment = crystal + brittleness >= 105 ? ProcessKind.CALCINATION : ProcessKind.ROASTING;
                temperature = thermalTreatmentTemperature(mineral, bond, stability, crystal);
                explanation = "Strong/stable bound structure receives thermal pretreatment before component recovery.";
            } else if (polarity >= 48 || reactivity >= 55) {
                treatment = ProcessKind.LEACHING;
                explanation = "Reactive/polar bound structure is pretreated by leaching before the final recovery step.";
            } else {
                treatment = ProcessKind.DISSOLUTION;
                explanation = "Moderately bound structure receives a generic dissolution pretreatment before component recovery.";
            }

            ProcessMaterial purified = processMaterial(analysis, units, MaterialPart.PURIFIED_DUST);
            plan.step(new ProcessStep(
                    "materials/" + mineral.id() + "/dust_pretreatment",
                    treatment,
                    List.of(current),
                    List.of(purified),
                    environmentRequirementsForTreatment(analysis, treatment),
                    temperature,
                    explanation
            ));
            current = purified;
        }

        ProcessKind finalKind = finalRecoveryKind(analysis, outputs);
        int finalTemperature = switch (finalKind) {
            case ELECTROLYSIS, ELECTROWINNING, ELECTROREFINING -> electrochemicalTemperature(mineral, analysis);
            default -> 0;
        };

        plan.step(new ProcessStep(
                "materials/" + mineral.id() + "/recover_elements",
                finalKind,
                List.of(current),
                outputs,
                environmentRequirementsForRecovery(analysis, finalKind),
                finalTemperature,
                recoveryExplanation(finalKind, bond, stability, volatility)
        ));
        return Optional.of(plan.build());
    }

    private static ProcessKind finalRecoveryKind(MaterialAnalysis analysis, List<ProcessMaterial> outputs) {
        boolean hasMetalLike = outputs.stream().anyMatch(output -> {
            if (!(output.backingMaterial() instanceof IndustrialMaterial material)) return false;
            return material.properties().metal();
        });
        double polarity = analysis.properties().get("polarity");
        double conductivity = analysis.properties().get("electricalconductivity");
        double bond = analysis.properties().get("bondstrength");

        if (polarity >= 62 || analysis.classifications().contains(MaterialClassification.IONIC_COMPOUND)) {
            return ProcessKind.ELECTROLYSIS;
        }
        if (hasMetalLike && (conductivity >= 25 || polarity >= 35)) {
            return ProcessKind.ELECTROWINNING;
        }
        if (bond >= 72) return ProcessKind.CHEMICAL_REACTION;
        return ProcessKind.CHEMICAL_REACTION;
    }

    private static List<ProcessRequirement> environmentRequirementsForTreatment(MaterialAnalysis analysis, ProcessKind kind) {
        if (kind != ProcessKind.LEACHING && kind != ProcessKind.DISSOLUTION) return List.of();
        double polarity = analysis.properties().get("polarity");
        if (polarity < 20) return List.of();
        return List.of(new ProcessRequirement(
                ProcessRequirement.Role.SOLVENT,
                ChemistryPhase.LIQUID,
                List.of(PropertyConstraint.between("polarity", Math.max(0, polarity - 25), Math.min(100, polarity + 25))),
                false,
                1000
        ));
    }

    private static List<ProcessRequirement> environmentRequirementsForRecovery(MaterialAnalysis analysis, ProcessKind kind) {
        if (kind != ProcessKind.ELECTROWINNING && kind != ProcessKind.CHEMICAL_REACTION) return List.of();
        double reactivity = analysis.properties().get("reactivity");
        if (reactivity >= 35) return List.of();
        return List.of(new ProcessRequirement(
                ProcessRequirement.Role.CATALYST,
                ChemistryPhase.SOLID,
                List.of(PropertyConstraint.between("catalyticactivity", 45, 85)),
                false,
                1000
        ));
    }

    private static int thermalTreatmentTemperature(IndustrialMaterial material, double bond, double stability, double crystal) {
        int melting = Math.max(1, material.meltingPoint());
        int calculated = (int) Math.ceil(120 + bond * 6.0 + stability * 5.0 + crystal * 2.0);
        return Math.max(1, Math.min(Math.max(1, melting - 1), calculated));
    }

    private static int electrochemicalTemperature(IndustrialMaterial material, MaterialAnalysis analysis) {
        double stability = analysis.properties().get("chemicalstability");
        double bond = analysis.properties().get("bondstrength");
        int calculated = (int) Math.ceil(25 + stability * 1.5 + bond * 1.2);
        return Math.max(0, Math.min(Math.max(0, material.meltingPoint() - 1), calculated));
    }

    private static String recoveryExplanation(ProcessKind kind, double bond, double stability, double volatility) {
        return switch (kind) {
            case ELECTROLYSIS -> "Calculated charge/polarity behaviour supports electrochemical decomposition into the registered elemental streams.";
            case ELECTROWINNING -> "Calculated conductive/polar behaviour supports electrowinning of recoverable elemental material.";
            case ELECTROREFINING -> "Calculated metallic behaviour supports electrorefining of the component stream.";
            default -> "A chemical recovery step is required because the components remain chemically bound (bond="
                    + Math.round(bond) + ", stability=" + Math.round(stability) + ", volatility=" + Math.round(volatility) + ").";
        };
    }

    private static ChemicalStructure.Topology inferredTopology(MaterialAnalysis analysis) {
        if (analysis.source().structure().isPresent()) return analysis.source().structure().orElseThrow().topology();
        if (analysis.classifications().contains(MaterialClassification.PHYSICAL_MIXTURE)) return ChemicalStructure.Topology.PHYSICAL_MIXTURE;
        if (analysis.classifications().contains(MaterialClassification.ALLOY)) return ChemicalStructure.Topology.METALLIC_LATTICE;
        if (analysis.classifications().contains(MaterialClassification.IONIC_COMPOUND)) return ChemicalStructure.Topology.IONIC_LATTICE;
        if (analysis.classifications().contains(MaterialClassification.POLYMER)) return ChemicalStructure.Topology.POLYMER_NETWORK;
        return ChemicalStructure.Topology.NETWORK;
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
            if (analysis.source().composition().isEmpty()) return false;
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
            if (!(child.source().backingMaterial() instanceof IndustrialMaterial childMaterial)) {
                diagnostics.add(problem(analysis, "SEPARATION_COMPONENT_NO_BACKING",
                        "Component " + component.substanceId() + " has no IndustrialMaterial backing, so this separation recipe was skipped."));
                return Optional.empty();
            }
            MaterialPart outputPart = outputPart(child, childMaterial);
            if (outputPart == null) {
                diagnostics.add(formProblem(analysis, child.source().id(), child.phase(), childMaterial));
                return Optional.empty();
            }
            outputs.add(processMaterial(child, component.amount(), outputPart));
            total = Math.addExact(total, component.amount());
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
        ProcessKind kind = choosePhysicalSeparation(outputs, registry);
        return Optional.of(new ProcessStep(
                "materials/" + material.id() + "_separation",
                kind,
                List.of(input),
                outputs,
                List.of(),
                "Automatic physical separation from top-level composition."
        ));
    }

    private static ProcessKind choosePhysicalSeparation(List<ProcessMaterial> outputs, Map<String, MaterialAnalysis> registry) {
        boolean allGas = outputs.stream().allMatch(output -> output.phase() == ChemistryPhase.GAS);
        boolean allFluid = outputs.stream().allMatch(output -> output.phase().isFluidLike());
        if (allGas) return ProcessKind.GAS_SEPARATION;
        if (allFluid) {
            double min = outputs.stream().map(output -> registry.get(output.materialId())).filter(java.util.Objects::nonNull)
                    .mapToDouble(value -> value.properties().get("volatility")).min().orElse(0);
            double max = outputs.stream().map(output -> registry.get(output.materialId())).filter(java.util.Objects::nonNull)
                    .mapToDouble(value -> value.properties().get("volatility")).max().orElse(0);
            return max - min >= 12 ? ProcessKind.DISTILLATION : ProcessKind.PHASE_SEPARATION;
        }

        long solids = outputs.stream().filter(output -> output.phase() == ChemistryPhase.SOLID).count();
        if (solids > 0 && solids < outputs.size()) return ProcessKind.FILTRATION;

        if (solids == outputs.size() && solids > 1) {
            boolean anyMagnetic = outputs.stream().anyMatch(ProcessPlanner::isMagnetic);
            boolean anyNonMagnetic = outputs.stream().anyMatch(output -> !isMagnetic(output));
            if (anyMagnetic && anyNonMagnetic) return ProcessKind.MAGNETIC_SEPARATION;

            double minDensity = outputs.stream().map(output -> registry.get(output.materialId())).filter(java.util.Objects::nonNull)
                    .mapToDouble(value -> value.properties().get("density")).min().orElse(0);
            double maxDensity = outputs.stream().map(output -> registry.get(output.materialId())).filter(java.util.Objects::nonNull)
                    .mapToDouble(value -> value.properties().get("density")).max().orElse(0);
            if (maxDensity - minDensity >= 12) return ProcessKind.CENTRIFUGING;
        }
        return ProcessKind.PHASE_SEPARATION;
    }

    private static boolean isMagnetic(ProcessMaterial material) {
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
        List<ProcessMaterial> inputs = new ArrayList<>();
        long total = 0;
        for (CompositionEntry component : analysis.source().composition()) {
            MaterialAnalysis child = registry.get(component.substanceId());
            if (child == null) {
                diagnostics.add(missingComponentProblem(analysis, component));
                return Optional.empty();
            }
            if (!(child.source().backingMaterial() instanceof IndustrialMaterial childMaterial)) {
                diagnostics.add(problem(analysis, "SYNTHESIS_COMPONENT_NO_BACKING",
                        "Component " + component.substanceId() + " has no IndustrialMaterial backing, so this synthesis recipe was skipped."));
                return Optional.empty();
            }
            MaterialPart childPart = defaultPart(child);
            if (childPart == null || !childMaterial.has(childPart)) {
                diagnostics.add(formProblem(analysis, child.source().id(), child.phase(), childMaterial));
                return Optional.empty();
            }
            total = Math.addExact(total, component.amount());
            inputs.add(processMaterial(child, component.amount(), childPart));
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

        ProcessMaterial output = processMaterial(analysis, total, outputPart);
        return Optional.of(new ProcessStep(
                "materials/" + analysis.source().id() + "_synthesis",
                kind,
                inputs,
                List.of(output),
                requirements,
                "Automatic synthesis from top-level composition."
        ));
    }

    private List<ProcessRequirement> environmentRequirements(MaterialAnalysis analysis) {
        List<ProcessRequirement> result = new ArrayList<>();
        double acidity = analysis.properties().get("acidity");
        double reactivity = analysis.properties().get("reactivity");
        if (Math.abs(acidity) >= 35) {
            double min = acidity > 0 ? Math.max(1, acidity - 15) : Math.min(-1, acidity - 15);
            double max = acidity > 0 ? Math.min(100, acidity + 15) : Math.min(-1, acidity + 15);
            if (min > max) { double value = min; min = max; max = value; }
            result.add(new ProcessRequirement(
                    ProcessRequirement.Role.ACID_BASE_ENVIRONMENT,
                    ChemistryPhase.LIQUID,
                    List.of(PropertyConstraint.between("acidity", min, max)),
                    false,
                    0
            ));
        }
        if (reactivity < 30) {
            result.add(new ProcessRequirement(
                    ProcessRequirement.Role.CATALYST,
                    ChemistryPhase.SOLID,
                    List.of(PropertyConstraint.between("catalyticactivity", 50, 80)),
                    false,
                    1000
            ));
        }
        return result;
    }

    private static MaterialPart outputPart(MaterialAnalysis analysis, IndustrialMaterial material) {
        if (analysis.phase() == ChemistryPhase.GAS || analysis.phase() == ChemistryPhase.PLASMA) {
            return material.has(MaterialPart.GAS) ? MaterialPart.GAS : null;
        }
        if (analysis.phase() == ChemistryPhase.LIQUID) {
            return material.has(MaterialPart.LIQUID) ? MaterialPart.LIQUID : null;
        }
        if (analysis.phase() == ChemistryPhase.MOLTEN) {
            return material.has(MaterialPart.MOLTEN_FLUID) ? MaterialPart.MOLTEN_FLUID : null;
        }
        return material.has(MaterialPart.DUST) ? MaterialPart.DUST : null;
    }

    private static MaterialPart defaultPart(MaterialAnalysis analysis) {
        if (!(analysis.source().backingMaterial() instanceof IndustrialMaterial material)) return null;
        MaterialPart preferred = outputPart(analysis, material);
        if (preferred != null) return preferred;
        return material.has(MaterialPart.DUST) ? MaterialPart.DUST : null;
    }

    private static ProcessMaterial solid(MaterialAnalysis analysis, long amount, MaterialPart part) {
        return processMaterial(analysis, amount, part);
    }

    private static ProcessMaterial processMaterial(MaterialAnalysis analysis, long amount, MaterialPart part) {
        if (!(analysis.source().backingMaterial() instanceof IndustrialMaterial backing)) {
            throw new IllegalStateException("Material " + analysis.source().id() + " has no IndustrialMaterial backing");
        }
        if (part == null || !backing.has(part)) {
            throw new IllegalStateException("Material " + backing.id() + " has no form " + part);
        }
        long milliUnits = Math.multiplyExact(amount, 1000L);
        ChemistryPhase phase = part.isFluid() ? phaseForPart(part) : ChemistryPhase.SOLID;
        return new ProcessMaterial(
                analysis.source().id(),
                phase,
                milliUnits,
                analysis.calculatedTierIndex(),
                analysis.calculatedTierName(),
                true,
                backing,
                part
        );
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
            IndustrialMaterial material
    ) {
        String expected = switch (phase) {
            case GAS, PLASMA -> "GAS";
            case LIQUID -> "LIQUID";
            case MOLTEN -> "MOLTEN_FLUID";
            default -> "DUST";
        };
        return new ChemistryDiagnostic(
                ChemistryStatus.CHANGE_REQUIRED,
                owner.source().id(),
                "MISSING_PROCESS_FORM",
                "Material " + materialId + " needs process form " + expected + " for phase " + phase
                        + ", but that form is not registered. The automatic recipe was skipped.",
                List.of("Add/map the missing form if it is physically valid for this material, otherwise correct the composition or phase inputs so the automatic route is not selected."),
                "// Required form for " + material.id() + ": MaterialPart." + expected
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
}
