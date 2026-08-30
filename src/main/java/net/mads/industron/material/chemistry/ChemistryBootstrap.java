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
import net.mads.industron.material.chemistry.report.ChemistryReportWriter;
import net.mads.industron.material.chemistry.report.OreGenerationReportWriter;

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

        ProcessPlanner planner = new ProcessPlanner();
        List<ProcessPlan> plans = new ArrayList<>();
        List<ChemistryDiagnostic> diagnostics = new ArrayList<>();
        for (MaterialAnalysis analysis : analyses.values()) {
            diagnostics.addAll(analysis.diagnostics());
            ProcessPlanner.Result result = planner.plan(analysis, analyses);
            plans.addAll(result.plans());
            diagnostics.addAll(result.diagnostics());
        }

        CompatibleMaterialResolver resolver = new CompatibleMaterialResolver();
        for (ProcessPlan plan : plans) {
            for (var step : plan.steps()) {
                for (ProcessRequirement requirement : step.requirements()) {
                    if (requirement.role() == ProcessRequirement.Role.ACID_BASE_ENVIRONMENT) {
                        // Chemical Balance requirements are fulfilled by the machine environment at runtime.
                        continue;
                    }
                    if (resolver.resolve(requirement, analyses).isEmpty()) {
                        diagnostics.add(new ChemistryDiagnostic(
                                ChemistryStatus.MISSING_MATERIAL,
                                plan.targetMaterialId(),
                                "UNSATISFIED_" + requirement.role(),
                                "No registered material satisfies " + requirement.constraints()
                                        + " in phase " + requirement.requiredPhase() + ".",
                                List.of("Create a compatible fictional IndustrialMaterial or change the target material structure/composition."),
                                suggestRequirement(requirement)
                        ));
                    }
                }
            }
        }

        GeologyPlanner.Result geology = new GeologyPlanner().plan(analyses);
        List<OreSourceAnalysis> oreSources = new OreSourceAnalyzer().analyze(analyses, geology.minerals());
        GeneratedProcessRegistry.replace(plans, diagnostics);
        AnalysisResult result = new AnalysisResult(analyses, plans, diagnostics, geology, oreSources);
        lastResult = result;
        return result;
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

    public static AnalysisResult runDataReports(Path path) {
        ChemistrySelfTest.run();
        if (!RUNNING.compareAndSet(false, true)) return currentOrAnalyze();

        try {
            AnalysisResult result = analyze();
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
