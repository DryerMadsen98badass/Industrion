package net.mads.industron.material.chemistry;

import net.mads.industron.material.chemistry.geology.GeologyPlanner;
import net.mads.industron.material.chemistry.geology.OreSourceAnalysis;
import net.mads.industron.material.chemistry.geology.OreSourceAnalyzer;
import net.mads.industron.material.chemistry.integration.IndustrialMaterialReflectionAdapter;
import net.mads.industron.material.chemistry.process.CompatibleMaterialResolver;
import net.mads.industron.material.chemistry.process.GeneratedProcessRegistry;
import net.mads.industron.material.chemistry.process.ProcessPlan;
import net.mads.industron.material.chemistry.process.ProcessPlanner;
import net.mads.industron.material.chemistry.process.ProcessRequirement;
import net.mads.industron.material.chemistry.process.ProcessSafetyValidator;
import net.mads.industron.material.chemistry.report.ChemistryReportWriter;
import net.mads.industron.material.chemistry.report.OreGenerationReportWriter;
import net.mads.industron.material.generation.MetalAliasSourceGenerator;
import net.mads.industron.material.structure.StructureMaterial;
import net.mads.industron.material.structure.WoodMaterial;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ChemistryBootstrap {
    private static final AtomicBoolean RUNNING = new AtomicBoolean();
    private static volatile AnalysisResult lastResult;

    private ChemistryBootstrap() {
    }

    public static AnalysisResult analyze() {
        IndustrialMaterialReflectionAdapter adapter = new IndustrialMaterialReflectionAdapter();
        Map<String, MaterialSnapshot> snapshots = adapter.loadAll();

        ChemistryEngine engine = new ChemistryEngine();
        Map<String, MaterialAnalysis> analyses = new LinkedHashMap<>();
        for (MaterialSnapshot snapshot : snapshots.values()) {
            analyses.put(snapshot.id(), engine.analyze(snapshot, snapshots));
        }

        List<ChemistryDiagnostic> diagnostics = new ArrayList<>();
        new CompositionResolver(snapshots).diagnostics().forEach((materialId, problem) -> diagnostics.add(
                new ChemistryDiagnostic(
                        ChemistryStatus.IMPOSSIBLE,
                        materialId,
                        "CANONICAL_COMPOSITION",
                        "Phase 8 canonical composition could not resolve this substance: " + problem + ".",
                        List.of("Fix the .contains(...) graph before generated recipes are emitted."),
                        ""
                )
        ));

        ProcessPlanner planner = new ProcessPlanner();
        List<ProcessPlan> plans = new ArrayList<>();
        for (MaterialAnalysis analysis : analyses.values()) {
            diagnostics.addAll(analysis.diagnostics());
            // Structure definitions remain composition dependencies. Wood is the exception: its
            // WOOD_PULP chemistry is now planned automatically from .contains(...), while normal
            // construction recipes still stay in the structure/wood recipe systems.
            if (analysis.source().backingMaterial() instanceof StructureMaterial
                    && !(analysis.source().backingMaterial() instanceof WoodMaterial)) continue;
            ProcessPlanner.Result result = planner.plan(analysis, analyses);
            plans.addAll(result.plans());
            diagnostics.addAll(result.diagnostics());
        }

        ProcessSafetyValidator.Result safety = new ProcessSafetyValidator().validate(plans, analyses);
        plans = new ArrayList<>(safety.plans());
        diagnostics.addAll(safety.diagnostics());

        CompatibleMaterialResolver resolver = new CompatibleMaterialResolver();
        List<ProcessPlan> requirementValid = new ArrayList<>();
        for (ProcessPlan plan : plans) {
            boolean valid = true;
            for (var step : plan.steps()) {
                for (ProcessRequirement requirement : step.requirements()) {
                    if (requirement.role() == ProcessRequirement.Role.CHEMICAL_BALANCE) {
                        // Chemical Balance requirements are fulfilled by the machine environment at runtime.
                        continue;
                    }
                    if (resolver.resolve(requirement, analyses).isEmpty()) {
                        valid = false;
                        diagnostics.add(new ChemistryDiagnostic(
                                ChemistryStatus.MISSING_MATERIAL,
                                plan.targetMaterialId(),
                                "UNSATISFIED_" + requirement.role(),
                                "No registered material satisfies "
                                        + (requirement.fixedMaterial()
                                        ? "fixed material " + requirement.fixedMaterialId()
                                        : requirement.constraints().toString())
                                        + " in phase " + requirement.requiredPhase() + ". The whole plan was blocked.",
                                List.of("Create a compatible fictional IndustrialMaterial or change the target material structure/composition."),
                                suggestRequirement(requirement)
                        ));
                    }
                }
            }
            if (valid) requirementValid.add(plan);
        }
        plans = requirementValid;

        // A locally valid split is still unusable when one of its composite fractions has no
        // surviving downstream automatic route. Propagate those dead ends upward so DUST and
        // WOOD_PULP decomposition graphs are published only when the full chain is reachable.
        ProcessSafetyValidator.Result completeChains = new ProcessSafetyValidator()
                .validateCompleteChains(plans, analyses);
        plans = new ArrayList<>(completeChains.plans());
        diagnostics.addAll(completeChains.diagnostics());

        GeologyPlanner.Result geology = new GeologyPlanner().plan(analyses);
        List<OreSourceAnalysis> oreSources = new OreSourceAnalyzer().analyze(analyses, geology.minerals());
        GeneratedProcessRegistry.replace(plans, diagnostics);
        AnalysisResult result = new AnalysisResult(analyses, plans, diagnostics, geology, oreSources);
        lastResult = result;
        return result;
    }

    public record RuntimeGeology(GeologyPlanner.Result geology,
            Map<String, net.mads.industron.material.IndustrialMaterial> materials) {
        public RuntimeGeology { materials = Map.copyOf(materials); }
    }

    public static RuntimeGeology runtimeGeology() {
        AnalysisResult cached = lastResult;
        Map<String, MaterialAnalysis> analyses;
        GeologyPlanner.Result geology;
        if (cached != null) {
            analyses = cached.analyses();
            geology = cached.geology();
        } else {
            Map<String, MaterialSnapshot> snapshots = new IndustrialMaterialReflectionAdapter().loadAll();
            ChemistryEngine engine = new ChemistryEngine();
            analyses = new LinkedHashMap<>();
            for (MaterialSnapshot snapshot : snapshots.values())
                analyses.put(snapshot.id(), engine.analyze(snapshot, snapshots));
            geology = new GeologyPlanner().plan(analyses);
        }
        Map<String, net.mads.industron.material.IndustrialMaterial> materials = new LinkedHashMap<>();
        for (MaterialAnalysis analysis : analyses.values())
            if (analysis.source().backingMaterial() instanceof net.mads.industron.material.IndustrialMaterial material)
                materials.put(material.id(), material);
        return new RuntimeGeology(geology, materials);
    }

    public static AnalysisResult currentOrAnalyze() {
        AnalysisResult cached = lastResult;
        return cached != null ? cached : analyze();
    }

    public static AnalysisResult runDataReports() {
        return runDataReports(defaultReportPath());
    }

    private static Path defaultReportPath() {
        Path workingDirectory = Path.of("").toAbsolutePath().normalize();
        Path projectRoot = workingDirectory;
        if (workingDirectory.getFileName() != null
                && workingDirectory.getFileName().toString().equalsIgnoreCase("run")
                && workingDirectory.getParent() != null) {
            projectRoot = workingDirectory.getParent();
        }
        return projectRoot.resolve("build").resolve("reports").resolve("industron").resolve("chemistry");
    }

    /** Writes the report set for an already-resolved analysis/publish state. */
    public static void writeReports(AnalysisResult result) {
        writeReports(defaultReportPath(), result);
    }

    public static void writeBiologicalEmissionReport(java.util.List<ProcessPlan> plans, java.util.List<ChemistryDiagnostic> diagnostics) {
        try {
            new net.mads.industron.material.chemistry.report.BiologicalChemistryReportWriter()
                    .write(defaultReportPath(),plans,diagnostics);
        } catch (java.io.IOException error) {
            throw new IllegalStateException("Could not write biological post-emission report",error);
        }
    }

    public static void writeReports(Path path, AnalysisResult result) {
        if (path == null || result == null) return;
        try {
            new ChemistryReportWriter().write(
                    path,
                    result.analyses(),
                    result.plans(),
                    result.diagnostics(),
                    result.geology().minerals(),
                    result.geology().deposits()
            );
            Path industronReportRoot = path.getParent() == null ? path : path.getParent();
            Path oreReportPath = industronReportRoot.resolve("ore_generation");
            new OreGenerationReportWriter().write(
                    oreReportPath,
                    result.geology().minerals(),
                    result.geology().deposits(),
                    result.oreSources(),
                    result.plans()
            );
        } catch (java.io.IOException error) {
            throw new IllegalStateException("Failed to write Industron chemistry reports to " + path, error);
        }
    }

    public static AnalysisResult runDataReports(Path path) {
        if (!RUNNING.compareAndSet(false, true)) return currentOrAnalyze();

        try {
            AnalysisResult result = analyze();
            // Self-tests touch real wood/stone definitions. Run them only after analyze() has
            // deterministically initialized the complete material universe, avoiding a possible
            // IndustrialMaterials -> StructureMaterials -> WoodMaterials static-init cycle.
            ChemistrySelfTest.run();
            MetalAliasSourceGenerator.sync();
            writeReports(path, result);
            Path industronReportRoot = path.getParent() == null ? path : path.getParent();
            Path oreReportPath = industronReportRoot.resolve("ore_generation");
            long blocking = result.diagnostics().stream().filter(ChemistryDiagnostic::blocksRecipeGeneration).count();
            long warnings = result.diagnostics().size() - blocking;
            System.out.println("[Industron Chemistry] " + result.analyses().size() + " materials, "
                    + result.plans().size() + " process plans, " + blocking + " blocking, " + warnings
                    + " warnings. Reports: " + path.toAbsolutePath());
            long missingOre = result.oreSources().stream().filter(OreSourceAnalysis::missing).count();
            System.out.println("[Industron Ore Generation] " + result.oreSources().size() + " elements analyzed, "
                    + result.geology().minerals().size() + " defined ore minerals, "
                    + result.geology().deposits().size() + " deposits, " + missingOre
                    + " missing/blocked sources. Reports: " + oreReportPath.toAbsolutePath());
            return result;
        } catch (Exception e) {
            System.err.println("[Industron Chemistry] Analysis failed: " + e);
            e.printStackTrace();
            return new AnalysisResult(
                    Map.of(),
                    List.of(),
                    List.of(new ChemistryDiagnostic(
                            ChemistryStatus.IMPOSSIBLE,
                            "global",
                            "BOOTSTRAP_FAILURE",
                            String.valueOf(e),
                            List.of("Inspect the stack trace and chemistry reports."),
                            ""
                    )),
                    new GeologyPlanner.Result(List.of(), List.of()),
                    List.of()
            );
        } finally {
            RUNNING.set(false);
        }
    }

    private static String suggestRequirement(ProcessRequirement requirement) {
        return "// Required " + requirement.role() + "\n"
                + "// phase: " + requirement.requiredPhase() + "\n"
                + "// constraints: " + requirement.constraints() + "\n"
                + "public static final IndustrialMaterial CHANGE_ME = material(\"change_me\", \"Change Me\", 0x808080)\n"
                + "        .contains(/* component(...) */)\n"
                + "        .build();";
    }

    public record AnalysisResult(
            Map<String, MaterialAnalysis> analyses,
            List<ProcessPlan> plans,
            List<ChemistryDiagnostic> diagnostics,
            GeologyPlanner.Result geology,
            List<OreSourceAnalysis> oreSources
    ) {
        public AnalysisResult {
            analyses = Map.copyOf(analyses);
            plans = List.copyOf(plans);
            diagnostics = List.copyOf(diagnostics);
            oreSources = List.copyOf(oreSources);
        }
    }
}
