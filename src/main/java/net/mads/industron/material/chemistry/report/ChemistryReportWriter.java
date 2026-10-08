package net.mads.industron.material.chemistry.report;

import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.material.chemistry.ChemicalTopologyResolver;
import net.mads.industron.material.chemistry.ChemistryDiagnostic;
import net.mads.industron.material.chemistry.ChemistryStatus;
import net.mads.industron.material.chemistry.CompositionVector;
import net.mads.industron.material.chemistry.ChemicalStructure;
import net.mads.industron.material.chemistry.ChemicalStructureGenerator;
import net.mads.industron.material.chemistry.MaterialAnalysis;
import net.mads.industron.material.chemistry.MaterialSnapshot;
import net.mads.industron.material.chemistry.PolymerDescriptor;
import net.mads.industron.material.chemistry.ReactionPlan;
import net.mads.industron.material.chemistry.ReactionSolver;
import net.mads.industron.material.chemistry.StructuralMotif;
import net.mads.industron.material.chemistry.SubstanceIdentity;
import net.mads.industron.material.chemistry.geology.DepositDefinition;
import net.mads.industron.material.chemistry.geology.OreMineral;
import net.mads.industron.material.chemistry.process.ProcessMaterial;
import net.mads.industron.material.chemistry.process.ProcessPlan;
import net.mads.industron.material.chemistry.process.ProcessRequirement;
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
            byTarget.computeIfAbsent(reportMaterialId(plan), ignored -> new ArrayList<>()).add(plan);
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
                    diagnostics.stream().filter(d -> diagnosticBelongsTo(d, analysis.source().id())).toList()
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

    private static String reportMaterialId(ProcessPlan plan) {
        return plan == null ? "" : normalizeGeneratedPlanTarget(plan.targetMaterialId());
    }

    private static boolean diagnosticBelongsTo(ChemistryDiagnostic diagnostic, String materialId) {
        return diagnostic != null
                && normalizeGeneratedPlanTarget(diagnostic.materialId()).equals(materialId);
    }

    private static String normalizeGeneratedPlanTarget(String id) {
        if (id == null) return "";
        for (String suffix : List.of(
                "_ore_dust_processing",
                "_mineral_dust_processing",
                "_composite_dust_processing"
        )) {
            if (id.endsWith(suffix)) return id.substring(0, id.length() - suffix.length());
        }
        return id;
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
        lines.add("STATUS: " + combinedStatus(analysis, plannerDiagnostics));
        lines.add("PHASE: " + analysis.phase());
        lines.add("TIER: " + analysis.calculatedTierName() + " [" + analysis.calculatedTierIndex() + "]");
        lines.add("CLASSIFICATIONS: " + analysis.classifications());
        lines.add(String.format("COLOR: #%06X", analysis.properties().color()));
        if (analysis.source().backingMaterial() instanceof IndustrialSubstance substance) {
            String formula = substance.formula();
            if (!formula.isBlank()) lines.add("FORMULA: " + formula);
        }
        try {
            SubstanceIdentity identity = analysis.source().substanceIdentity(snapshots);
            lines.add("");
            lines.add("SUBSTANCE IDENTITY:");
            lines.add("  direct=" + identity.directComposition().signature());
            lines.add("  conserved=" + identity.conservedComposition().signature());
            lines.add("  atomic=" + identity.atomicComposition().map(CompositionVector::signature).orElse("<not fully atomic>"));
            lines.add("  structure=" + identity.structureSignature().orElse("<none>"));
        } catch (RuntimeException error) {
            lines.add("");
            lines.add("SUBSTANCE IDENTITY:");
            lines.add("  <unresolved: " + error.getMessage() + ">");
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
        java.util.Optional<ChemicalStructure> generatedStructure = analysis.source().structure().isEmpty()
                ? ChemicalStructureGenerator.generate(analysis.source(), snapshots)
                : java.util.Optional.empty();
        if (analysis.source().structure().isEmpty() && generatedStructure.isEmpty()) {
            lines.add("  source=automatic topology from .contains(...) and component properties");
            lines.add("  topology=" + ChemicalTopologyResolver.resolve(analysis.source(), snapshots));
            lines.add("  exact atom graph=<not generated for this bulk/runtime substance>");
        } else {
            var structure = analysis.source().structure().orElseGet(generatedStructure::orElseThrow);
            lines.add("  source=" + (analysis.source().structure().isPresent() ? "explicit override" : "generated deterministic graph"));
            lines.add("  topology=" + structure.topology());
            lines.add("  atomFormula=" + structure.formula());
            lines.add("  netCharge=" + structure.netCharge());
            lines.add("  canonicalSignature=" + structure.canonicalSignature());
            lines.add("  motifs=" + StructuralMotif.detect(structure).stream().map(StructuralMotif::id).toList());
            PolymerDescriptor polymer = PolymerDescriptor.from(structure);
            if (polymer.family() != PolymerDescriptor.PolymerFamily.NOT_POLYMER) {
                lines.add("  polymerFamily=" + polymer.family());
                lines.add("  repeatUnit=" + polymer.repeatUnitId() + " x" + polymer.repeatCount());
                lines.add("  chainFlexibility=" + polymer.chainFlexibility());
                lines.add("  crosslinkDensity=" + polymer.crosslinkDensity());
            }
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
            lines.add("  PLAN " + plan.targetMaterialId());
            lines.add("    CHAIN " + chainSummary(plan));
            for (ProcessStep step : plan.steps()) {
                lines.add("    " + step.kind() + " " + step.id()
                        + " | tier=" + step.recipeTierName()
                        + " | duration=" + step.durationTicks() + " ticks"
                        + " | temperature=" + step.requiredTemperature() + " C"
                        + " | balanced=" + step.balanced());
                lines.add("      inputs=" + step.inputs());
                lines.add("      outputs=" + step.outputs());
                for (ProcessRequirement requirement : step.requirements()) {
                    lines.add("      requirement=" + formatRequirement(requirement));
                }
            }
        }

        lines.add("");
        lines.add("REACTION SOLVER:");
        java.util.Optional<ReactionPlan> synthesis = ReactionSolver.synthesize(analysis.source(), snapshots);
        java.util.Optional<ReactionPlan> decomposition = ReactionSolver.decompose(analysis.source(), snapshots);
        if (synthesis.isEmpty() && decomposition.isEmpty()) {
            lines.add("  <none>");
        } else {
            synthesis.ifPresent(plan -> lines.add("  SYNTHESIS " + reactionSummary(plan)));
            decomposition.ifPresent(plan -> lines.add("  DECOMPOSITION " + reactionSummary(plan)));
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

    private static ChemistryStatus combinedStatus(
            MaterialAnalysis analysis,
            List<ChemistryDiagnostic> plannerDiagnostics
    ) {
        ChemistryStatus status = analysis.status();
        for (ChemistryDiagnostic diagnostic : plannerDiagnostics) {
            if (diagnostic.status().ordinal() > status.ordinal()) status = diagnostic.status();
        }
        return status;
    }

    private static String chainSummary(ProcessPlan plan) {
        if (plan.steps().isEmpty()) return "<empty>";
        List<String> nodes = new ArrayList<>();
        ProcessStep first = plan.steps().getFirst();
        nodes.add(formatMaterials(first.inputs()));
        for (ProcessStep step : plan.steps()) {
            nodes.add(formatMaterials(step.outputs()));
        }
        return String.join(" -> ", nodes);
    }

    private static String reactionSummary(ReactionPlan plan) {
        return plan.reactants() + " -> " + plan.products()
                + " | temperature=" + plan.requiredTemperature() + " C"
                + " | cb=" + plan.minChemicalBalance() + ".." + plan.maxChemicalBalance()
                + (plan.catalystFamily().isBlank() ? "" : " | catalyst=" + plan.catalystFamily());
    }

    private static String formatMaterials(List<ProcessMaterial> materials) {
        if (materials.isEmpty()) return "<none>";
        return materials.stream()
                .map(material -> material.materialId()
                        + "@" + (material.part() == null ? material.phase() : material.part())
                        + "[" + material.milliUnits() + "mu]")
                .toList()
                .toString();
    }

    private static String formatRequirement(ProcessRequirement requirement) {
        if (requirement.role() == ProcessRequirement.Role.CHEMICAL_BALANCE) {
            return "CHEMICAL_BALANCE " + requirement.constraints();
        }
        if (requirement.fixedMaterial()) {
            return requirement.role() + " fixed=" + requirement.fixedMaterialId()
                    + " phase=" + requirement.requiredPhase()
                    + " nonConsumable=true";
        }
        return requirement.role() + " phase=" + requirement.requiredPhase()
                + " constraints=" + requirement.constraints()
                + " nonConsumable=true";
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
