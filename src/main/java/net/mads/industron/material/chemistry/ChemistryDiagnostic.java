package net.mads.industron.material.chemistry;

import java.util.List;
import java.util.Objects;

public record ChemistryDiagnostic(
        ChemistryStatus status,
        String materialId,
        String code,
        String message,
        List<String> actions,
        String suggestedJava
) {
    public ChemistryDiagnostic {
        status = Objects.requireNonNull(status, "status");
        materialId = materialId == null ? "global" : materialId;
        code = code == null ? "UNSPECIFIED" : code;
        message = message == null ? "" : message;
        actions = actions == null ? List.of() : List.copyOf(actions);
        suggestedJava = suggestedJava == null ? "" : suggestedJava;
    }

    public boolean blocksRecipeGeneration() {
        return status == ChemistryStatus.MISSING_MATERIAL
                || status == ChemistryStatus.CHANGE_REQUIRED
                || status == ChemistryStatus.AMBIGUOUS
                || status == ChemistryStatus.IMPOSSIBLE;
    }
}
