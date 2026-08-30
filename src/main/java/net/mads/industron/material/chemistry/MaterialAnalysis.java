package net.mads.industron.material.chemistry;

import java.util.List;
import java.util.Set;

public record MaterialAnalysis(
        MaterialSnapshot source,
        DerivedMaterialProperties properties,
        Set<MaterialClassification> classifications,
        ChemistryPhase phase,
        int calculatedTierIndex,
        String calculatedTierName,
        List<ChemistryDiagnostic> diagnostics
) {
    public MaterialAnalysis {
        classifications = Set.copyOf(classifications);
        diagnostics = List.copyOf(diagnostics);
    }

    public ChemistryStatus status() {
        ChemistryStatus result = ChemistryStatus.OK;
        for (ChemistryDiagnostic diagnostic : diagnostics) {
            if (diagnostic.status().ordinal() > result.ordinal()) result = diagnostic.status();
        }
        return result;
    }
}
