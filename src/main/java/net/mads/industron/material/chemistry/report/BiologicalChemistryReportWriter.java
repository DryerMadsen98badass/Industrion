package net.mads.industron.material.chemistry.report;

import net.mads.industron.material.chemistry.ChemistryDiagnostic;
import net.mads.industron.material.chemistry.process.ProcessPlan;
import net.mads.industron.material.chemistry.process.ProcessMaterial;
import net.mads.industron.material.organism.BiologicalChemistryBridge;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.*;

/** Called only after emission preflight and save; never describes preview candidates as real recipes. */
public final class BiologicalChemistryReportWriter {
    public void write(Path directory,List<ProcessPlan> published,List<ChemistryDiagnostic> diagnostics) throws IOException {
        List<String> lines=new ArrayList<>();
        lines.add("BIOLOGICAL CHEMISTRY — POST-EMISSION REPORT");
        lines.add("Generated: "+java.time.Instant.now());
        lines.add("Only successfully emitted plans appear below. Missing routes are not replaced with guessed recipes.");
        lines.add("1 material unit = 1 solid item, 144 mB liquid/molten, or 576 mB gas. Quantities below are exact milli-units.");
        for(var entry:new TreeMap<>(BiologicalChemistryBridge.adapters()).entrySet()) {
            String id=entry.getKey();lines.add("");lines.add("== "+id+" ==");
            List<ProcessPlan> routes=published.stream().filter(p->belongsTo(p,id)).toList();
            lines.add("Published plans: "+routes.size());
            for(var route:routes)for(var step:route.steps()) {
                lines.add(step.id()+" | "+step.kind()+" | minimum tier "+step.recipeTierName()
                        +" | required temperature "+step.requiredTemperature());
                lines.add("  IN  "+quantities(step.inputs()));
                lines.add("  OUT "+quantities(step.outputs()));
                for(var requirement:step.requirements())lines.add("  REQUIRES "+requirement);
            }
            for(var diagnostic:diagnostics)if(diagnosticIds(id).contains(diagnostic.materialId())) {
                lines.add("  "+diagnostic.status()+" ["+diagnostic.code()+"] "+diagnostic.message());
            }
            if(routes.isEmpty())lines.add("  No emitted route. Inspect this section's diagnostics and the main chemistry reports.");
        }
        Files.createDirectories(directory);
        Files.write(directory.resolve("biological_emission.txt"),lines,StandardCharsets.UTF_8);
    }
    // Decomposition plans use a route ID as targetMaterialId (e.g. collagen_composite_dust_processing).
    // Read their actual first input rather than confusing that route ID with the material ID.
    static boolean belongsTo(ProcessPlan plan,String materialId) {
        return plan.targetMaterialId().equals(materialId) || (!plan.steps().isEmpty()
                && plan.steps().get(0).inputs().stream().anyMatch(input->input.materialId().equals(materialId)));
    }
    private static Set<String> diagnosticIds(String materialId) {
        return Set.of(materialId,materialId+"_composite_dust_processing",materialId+"_mineral_dust_processing",
                materialId+"_ore_dust_processing",materialId+"_biological_processing");
    }
    private static String quantities(List<ProcessMaterial> materials) {
        return materials.stream().map(m->m.materialId()+" ["+m.phase()+", "+m.part()+"] "+m.milliUnits()+"/1000 units")
                .reduce((a,b)->a+" + "+b).orElse("none");
    }
}
