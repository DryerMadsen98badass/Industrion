package net.mads.industron.material.chemistry.report;

import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.material.chemistry.ChemicalTopologyResolver;
import net.mads.industron.material.chemistry.ChemistryDiagnostic;
import net.mads.industron.material.chemistry.ChemistryStatus;
import net.mads.industron.material.chemistry.MaterialAnalysis;
import net.mads.industron.material.chemistry.MaterialSnapshot;
import net.mads.industron.material.chemistry.geology.DepositDefinition;
import net.mads.industron.material.chemistry.geology.OreMineral;
import net.mads.industron.material.chemistry.process.ProcessPlan;
import net.mads.industron.material.chemistry.process.ProcessStep;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public final class ChemistryReportWriter {
    public void write(
            Path root,
            Map<String, MaterialAnalysis> analyses,
            List<ProcessPlan> plans,
            List<ChemistryDiagnostic> diagnostics,
            List<OreMineral> minerals,
            List<DepositDefinition> deposits
    ) throws IOException {
        Files.createDirectories(root.resolve("materials"));

        Map<String, List<ProcessPlan>> byTarget = new LinkedHashMap<>();
        for (ProcessPlan plan : plans) {
            byTarget.computeIfAbsent(plan.targetMaterialId(), ignored -> new ArrayList<>()).add(plan);
        }
        Map<String, MaterialSnapshot> snapshots = new LinkedHashMap<>();
        analyses.forEach((id, analysis) -> snapshots.put(id, analysis.source()));

        for (MaterialAnalysis analysis : analyses.values().stream()
                .sorted(Comparator.comparing(value -> value.source().id()))
                .toList()) {
            writeMaterial(
                    root.resolve("materials").resolve(analysis.source().id() + ".txt"),
                    analysis,
                    snapshots,
                    byTarget.getOrDefault(analysis.source().id(), List.of()),
                    diagnostics.stream().filter(d -> d.materialId().equals(analysis.source().id())).toList()
            );
        }

        writeSummary(root.resolve("summary.txt"), analyses, plans, diagnostics, minerals, deposits);
        writeDiagnostics(root.resolve("missing-materials.txt"), diagnostics, d -> d.status() == ChemistryStatus.MISSING_MATERIAL);
        writeDiagnostics(root.resolve("required-changes.txt"), diagnostics, d -> d.status() == ChemistryStatus.CHANGE_REQUIRED);
        writeDiagnostics(root.resolve("ambiguous-structures.txt"), diagnostics, d -> d.status() == ChemistryStatus.AMBIGUOUS);
        writeDiagnostics(root.resolve("impossible-materials.txt"), diagnostics, d -> d.status() == ChemistryStatus.IMPOSSIBLE);
        writeGeology(root.resolve("geology.txt"), minerals, deposits);
        writeProcesses(root.resolve("process-plans.txt"), plans);
    }

    private void writeSummary(
            Path path,
            Map<String, MaterialAnalysis> analyses,
            List<ProcessPlan> plans,
            List<ChemistryDiagnostic> diagnostics,
            List<OreMineral> minerals,
            List<DepositDefinition> deposits
    ) throws IOException {
        Map<String, Long> statuses = new LinkedHashMap<>();
        for (MaterialAnalysis analysis : analyses.values()) {
            statuses.merge(analysis.status().name(), 1L, Long::sum);
        }

        List<String> lines = new ArrayList<>();
        lines.add("INDUSTRON AUTOMATIC MATERIAL ANALYSIS");
        lines.add("");
        lines.add("Materials: " + analyses.size());
        lines.add("Process plans: " + plans.size());
        lines.add("Diagnostics: " + diagnostics.size());
        lines.add("Ore minerals: " + minerals.size());
        lines.add("Deposits: " + deposits.size());
        lines.add("");
        statuses.forEach((status, count) -> lines.add(status + ": " + count));
        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    private void writeMaterial(
            Path path,
            MaterialAnalysis analysis,
            Map<String, MaterialSnapshot> snapshots,
            List<ProcessPlan> plans,
            List<ChemistryDiagnostic> plannerDiagnostics
    ) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("MATERIAL: " + analysis.source().displayName() + " (" + analysis.source().id() + ")");
        lines.add("STATUS: " + analysis.status());
        lines.add("PHASE: " + analysis.phase());
        lines.add("TIER: " + analysis.calculatedTierName() + " [" + analysis.calculatedTierIndex() + "]");
        lines.add("CLASSIFICATIONS: " + analysis.classifications());
        lines.add(String.format("COLOR: #%06X", analysis.properties().color()));
        if (analysis.source().backingMaterial() instanceof IndustrialSubstance substance) {
            String formula = substance.formula();
            if (!formula.isBlank()) lines.add("FORMULA: " + formula);
        }
        lines.add("");
        lines.add("COMPOSITION:");
        if (analysis.source().composition().isEmpty()) {
            lines.add("  <element / no top-level composition>");
        } else {
            analysis.source().composition().forEach(component -> lines.add(
                    "  " + component.amount() + " x " + component.substanceId() + " [" + component.phase() + "]"
            ));
        }

        lines.add("");
        lines.add("STRUCTURE:");
        if (analysis.source().structure().isEmpty()) {
            lines.add("  source=automatic from .contains(...) and component properties");
            lines.add("  topology=" + ChemicalTopologyResolver.resolve(analysis.source(), snapshots));
            lines.add("  exact atom graph=<not required for bulk inference>");
        } else {
            var structure = analysis.source().structure().orElseThrow();
            lines.add("  source=explicit override");
            lines.add("  topology=" + structure.topology());
            lines.add("  atomFormula=" + structure.formula());
            lines.add("  netCharge=" + structure.netCharge());
            lines.add("  atoms:");
            if (structure.atoms().isEmpty()) lines.add("    <none / bulk mixture topology>");
            structure.atoms().forEach(atom -> lines.add(
                    "    " + atom.id() + " = " + atom.elementId() + " | formalCharge=" + atom.formalCharge()
            ));
            lines.add("  bonds:");
            if (structure.bonds().isEmpty()) lines.add("    <none>");
            structure.bonds().forEach(bond -> lines.add(
                    "    " + bond.firstAtom() + " -- " + bond.type() + "/" + bond.order() + " -- " + bond.secondAtom()
            ));
        }

        lines.add("");
        lines.add("PROPERTIES:");
        analysis.properties().values().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> lines.add("  " + entry.getKey() + " = " + entry.getValue()));

        lines.add("");
        lines.add("PROCESS PLANS:");
        if (plans.isEmpty()) lines.add("  <none>");
        for (ProcessPlan plan : plans) {
            for (ProcessStep step : plan.steps()) {
                lines.add("  " + step.kind() + " " + step.id()
                        + " | tier=" + step.recipeTierName()
                        + " | duration=" + step.durationTicks() + " ticks"
                        + " | temperature=" + step.requiredTemperature() + " C"
                        + " | balanced=" + step.balanced());
                lines.add("    inputs=" + step.inputs());
                lines.add("    outputs=" + step.outputs());
                if (!step.requirements().isEmpty()) lines.add("    requirements=" + step.requirements());
            }
        }

        lines.add("");
        lines.add("DIAGNOSTICS:");
        List<ChemistryDiagnostic> materialDiagnostics = new ArrayList<>(analysis.diagnostics());
        for (ChemistryDiagnostic diagnostic : plannerDiagnostics) {
            if (!materialDiagnostics.contains(diagnostic)) materialDiagnostics.add(diagnostic);
        }
        if (materialDiagnostics.isEmpty()) lines.add("  <none>");
        for (ChemistryDiagnostic diagnostic : materialDiagnostics) {
            lines.add("  [" + diagnostic.status() + "] " + diagnostic.code() + ": " + diagnostic.message());
            for (String action : diagnostic.actions()) lines.add("    ACTION: " + action);
            if (!diagnostic.suggestedJava().isBlank()) {
                lines.add("    SUGGESTED JAVA:");
                for (String line : diagnostic.suggestedJava().split("\\R")) {
                    lines.add("      " + line);
                }
            }
        }

        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    private void writeDiagnostics(
            Path path,
            List<ChemistryDiagnostic> diagnostics,
            Predicate<ChemistryDiagnostic> filter
    ) throws IOException {
        List<String> lines = new ArrayList<>();
        for (ChemistryDiagnostic diagnostic : diagnostics) {
            if (!filter.test(diagnostic)) continue;
            lines.add("[" + diagnostic.status() + "] " + diagnostic.materialId() + " / " + diagnostic.code());
            lines.add(diagnostic.message());
            for (String action : diagnostic.actions()) lines.add("ACTION: " + action);
            if (!diagnostic.suggestedJava().isBlank()) {
                lines.add("SUGGESTED JAVA:");
                lines.add(diagnostic.suggestedJava());
            }
            lines.add("");
        }
        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    private void writeGeology(
            Path path,
            List<OreMineral> minerals,
            List<DepositDefinition> deposits
    ) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("ORE MINERALS");
        for (OreMineral mineral : minerals) {
            lines.add(mineral.id() + " (" + mineral.displayName() + ") -> " + mineral.recoverableMaterials());
        }
        lines.add("");
        lines.add("DEPOSITS");
        for (DepositDefinition deposit : deposits) {
            lines.add(deposit.id()
                    + " | " + deposit.geometry()
                    + " | size=" + deposit.minimumSize() + ".." + deposit.maximumSize()
                    + " | grade=" + deposit.grade()
                    + " | rarity=" + deposit.rarity()
                    + " | y=" + deposit.conditions().minimumY() + ".." + deposit.conditions().maximumY());
        }
        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    private void writeProcesses(Path path, List<ProcessPlan> plans) throws IOException {
        List<String> lines = new ArrayList<>();
        for (ProcessPlan plan : plans) {
            lines.add("TARGET " + plan.targetMaterialId());
            for (ProcessStep step : plan.steps()) {
                lines.add("  " + step.id() + " [" + step.kind() + "]");
                lines.add("    INPUT " + step.inputMilliUnits() + " milli-units");
                lines.add("    OUTPUT " + step.outputMilliUnits() + " milli-units");
                lines.add("    BALANCED " + step.balanced());
                lines.add("    TIER " + step.recipeTierName());
                lines.add("    DURATION " + step.durationTicks() + " ticks");
                lines.add("    TEMPERATURE " + step.requiredTemperature() + " C");
                lines.add("    REQUIREMENTS " + step.requirements());
                lines.add("    NOTE " + step.explanation());
            }
            lines.add("");
        }
        Files.write(path, lines, StandardCharsets.UTF_8);
    }
}
