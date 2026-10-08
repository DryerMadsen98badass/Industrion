package net.mads.industron.material.chemistry;

import net.mads.industron.material.chemistry.process.ProcessKind;
import net.mads.industron.material.chemistry.process.ProcessMaterial;
import net.mads.industron.material.chemistry.process.ProcessRequirement;
import net.mads.industron.material.chemistry.process.ProcessStep;

import java.util.List;
import java.util.Map;

/** Bridge from Phase 13 reaction plans into Phase 14 process/recipe planning. */
public final class ReactionProcessAdapter {
    private ReactionProcessAdapter() {
    }

    public static ProcessStep toProcessStep(ReactionPlan plan, Map<String, MaterialAnalysis> analyses) {
        List<ProcessMaterial> inputs = plan.reactants().stream()
                .map(participant -> material(participant, analyses))
                .toList();
        List<ProcessMaterial> outputs = plan.products().stream()
                .map(participant -> material(participant, analyses))
                .toList();
        java.util.ArrayList<ProcessRequirement> requirements = new java.util.ArrayList<>();
        double minimumCb = plan.minChemicalBalance();
        double maximumCb = plan.maxChemicalBalance();
        if (!Double.isFinite(minimumCb) || !Double.isFinite(maximumCb)
                || minimumCb < -100.0D || maximumCb > 100.0D) {
            // Keep validation centralized in ProcessRequirement even when the range would otherwise
            // be treated as neutral/unconstrained.
            ProcessRequirement.chemicalBalance(minimumCb, maximumCb);
        }
        // Crossing/touching neutral means the reaction does not require an acidic or basic
        // environment. Only strictly one-sided ranges become a machine Chemical Balance requirement.
        if (maximumCb < 0.0D || minimumCb > 0.0D) {
            requirements.add(ProcessRequirement.chemicalBalance(minimumCb, maximumCb));
        }
        return new ProcessStep(
                plan.id(),
                ProcessKind.CHEMICAL_REACTION,
                inputs,
                outputs,
                requirements,
                plan.requiredTemperature(),
                0,
                plan.explanation()
        );
    }

    private static ProcessMaterial material(ReactionParticipant participant, Map<String, MaterialAnalysis> analyses) {
        MaterialAnalysis analysis = analyses.get(participant.materialId());
        int tier = analysis == null ? 0 : analysis.calculatedTierIndex();
        String tierName = analysis == null ? "ULV" : analysis.calculatedTierName();
        Object backing = analysis == null ? null : analysis.source().backingMaterial();
        return new ProcessMaterial(
                participant.materialId(),
                participant.phase(),
                participant.milliUnits(),
                tier,
                tierName,
                true,
                backing
        );
    }
}
