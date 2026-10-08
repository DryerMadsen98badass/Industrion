package net.mads.industron.material.chemistry.process;

import net.mads.industron.material.chemistry.ChemistryPhase;

import java.util.List;
import java.util.Objects;

/**
 * A process condition or non-consumed process aid.
 *
 * <p>Any reagent that is truly consumed must be represented as an explicit ProcessMaterial input so
 * atom/mass conservation can validate it. Requirements are therefore reserved for environments,
 * catalysts, carriers and activators that are present but returned/unchanged by the recipe.</p>
 */
public record ProcessRequirement(
        Role role,
        ChemistryPhase requiredPhase,
        List<PropertyConstraint> constraints,
        boolean consumed,
        long milliUnits,
        String fixedMaterialId
) {
    public enum Role {
        REAGENT,
        CATALYST,
        SOLVENT,
        ACTIVATOR,
        CHEMICAL_BALANCE,
        OXIDIZER,
        REDUCER,
        ATMOSPHERE
    }

    /** Compatibility constructor for existing callers that use property-based selection. */
    public ProcessRequirement(
            Role role,
            ChemistryPhase requiredPhase,
            List<PropertyConstraint> constraints,
            boolean consumed,
            long milliUnits
    ) {
        this(role, requiredPhase, constraints, consumed, milliUnits, null);
    }

    public ProcessRequirement {
        role = Objects.requireNonNull(role, "role");
        requiredPhase = requiredPhase == null ? ChemistryPhase.UNKNOWN : requiredPhase;
        constraints = constraints == null ? List.of() : List.copyOf(constraints);
        if (milliUnits < 0) throw new IllegalArgumentException("milliUnits cannot be negative");
        if (!consumed && milliUnits == 0 && role != Role.CHEMICAL_BALANCE) milliUnits = 1000;
        if (fixedMaterialId != null) {
            fixedMaterialId = fixedMaterialId.trim().toLowerCase(java.util.Locale.ROOT);
            if (fixedMaterialId.isEmpty()) fixedMaterialId = null;
        }
        if (consumed) {
            throw new IllegalArgumentException(
                    "Consumed process aids must be explicit ProcessMaterial inputs/outputs so mass cannot disappear: " + role
            );
        }
    }


    /** Machine-environment Chemical Balance requirement; this is never a material/reagent input. */
    public static ProcessRequirement chemicalBalance(double minimum, double maximum) {
        if (!Double.isFinite(minimum) || !Double.isFinite(maximum)) {
            throw new IllegalArgumentException("Chemical Balance range must be finite");
        }
        if (minimum < -100.0D || maximum > 100.0D || minimum > maximum) {
            throw new IllegalArgumentException(
                    "Chemical Balance range must satisfy -100 <= min <= max <= 100, got "
                            + minimum + ".." + maximum
            );
        }
        if (minimum <= 0.0D && maximum >= 0.0D) {
            throw new IllegalArgumentException(
                    "Acid/basic processing CB range may not cross neutral: " + minimum + ".." + maximum
            );
        }
        return new ProcessRequirement(
                Role.CHEMICAL_BALANCE,
                ChemistryPhase.UNKNOWN,
                List.of(PropertyConstraint.between("chemicalbalance", minimum, maximum)),
                false,
                0
        );
    }

    public static ProcessRequirement fixed(
            Role role,
            ChemistryPhase requiredPhase,
            String materialId,
            long milliUnits
    ) {
        return new ProcessRequirement(role, requiredPhase, List.of(), false, milliUnits, materialId);
    }

    public boolean fixedMaterial() {
        return fixedMaterialId != null;
    }
}
