package net.mads.industron.material.chemistry.process;

import net.mads.industron.material.chemistry.ChemistryPhase;
import java.util.List;
import java.util.Objects;

public record ProcessRequirement(
        Role role,
        ChemistryPhase requiredPhase,
        List<PropertyConstraint> constraints,
        boolean consumed,
        long milliUnits
) {
    public enum Role { REAGENT, CATALYST, SOLVENT, ACID_BASE_ENVIRONMENT, OXIDIZER, REDUCER, ATMOSPHERE }

    public ProcessRequirement {
        role=Objects.requireNonNull(role,"role");
        requiredPhase=requiredPhase==null?ChemistryPhase.UNKNOWN:requiredPhase;
        constraints=constraints==null?List.of():List.copyOf(constraints);
        if (milliUnits<0) throw new IllegalArgumentException("milliUnits cannot be negative");
        if (!consumed && milliUnits==0 && role!=Role.ACID_BASE_ENVIRONMENT) milliUnits=1000;
    }
}
