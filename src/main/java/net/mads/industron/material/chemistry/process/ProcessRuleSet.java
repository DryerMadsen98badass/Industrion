package net.mads.industron.material.chemistry.process;

import java.util.EnumSet;
import java.util.Set;

/** Typed capability set for one process identity. */
public record ProcessRuleSet(Set<ProcessOperation> operations) {
    public ProcessRuleSet {
        operations = operations == null || operations.isEmpty()
                ? Set.of()
                : Set.copyOf(operations);
    }

    public boolean supportsAll(Set<ProcessOperation> required) {
        return operations.containsAll(required);
    }

    public static ProcessRuleSet forKind(ProcessKind kind) {
        return new ProcessRuleSet(switch (kind) {
            case MIXING -> ops(ProcessOperation.MIX_PHASES);
            case ALLOYING -> ops(ProcessOperation.MIX_PHASES, ProcessOperation.FORM_METALLIC_PHASE);
            case SMELTING -> ops(ProcessOperation.THERMAL_REDUCTION);
            case ROASTING -> ops(ProcessOperation.THERMAL_OXIDATION);
            case CALCINATION -> ops(ProcessOperation.THERMAL_DECOMPOSITION);
            case CHEMICAL_REACTION -> ops(ProcessOperation.BREAK_OR_FORM_BONDS,
                    ProcessOperation.FORM_IONIC_STRUCTURE);
            case DISSOLUTION -> ops(ProcessOperation.DISSOLVE, ProcessOperation.MIX_PHASES);
            case NEUTRALIZATION -> ops(ProcessOperation.NEUTRALIZE_ACID_BASE, ProcessOperation.BREAK_OR_FORM_BONDS);
            case PRECIPITATION -> ops(ProcessOperation.PRECIPITATE);
            case CRYSTALLIZATION -> ops(ProcessOperation.CRYSTALLIZE);
            case LEACHING -> ops(ProcessOperation.DISSOLVE, ProcessOperation.MIX_PHASES, ProcessOperation.LEACH_SOLUBLE_COMPONENTS);
            case SOLVENT_EXTRACTION -> ops(ProcessOperation.EXTRACT_BY_SOLVENT, ProcessOperation.SEPARATE_PHYSICAL_PHASES);
            case ELECTROLYSIS -> ops(ProcessOperation.TRANSFER_ELECTRONS,
                    ProcessOperation.CHANGE_OXIDATION_STATE, ProcessOperation.BREAK_OR_FORM_BONDS);
            case ELECTROREFINING -> ops(ProcessOperation.TRANSFER_ELECTRONS,
                    ProcessOperation.CHANGE_OXIDATION_STATE, ProcessOperation.FORM_METALLIC_PHASE);
            case ELECTROWINNING -> ops(ProcessOperation.TRANSFER_ELECTRONS,
                    ProcessOperation.CHANGE_OXIDATION_STATE, ProcessOperation.PRECIPITATE);
            case CENTRIFUGING -> ops(ProcessOperation.SEPARATE_PHYSICAL_PHASES, ProcessOperation.SEPARATE_BY_DENSITY);
            case MAGNETIC_SEPARATION -> ops(ProcessOperation.SEPARATE_PHYSICAL_PHASES, ProcessOperation.SEPARATE_BY_MAGNETISM);
            case FILTRATION -> ops(ProcessOperation.SEPARATE_PHYSICAL_PHASES, ProcessOperation.FILTER_SUSPENSION);
            case PHASE_SEPARATION -> ops(ProcessOperation.SEPARATE_PHYSICAL_PHASES, ProcessOperation.SEPARATE_IMMISCIBLE_PHASES);
            case GAS_SEPARATION -> ops(ProcessOperation.SEPARATE_PHYSICAL_PHASES, ProcessOperation.SEPARATE_GASES);
            case DISTILLATION -> ops(ProcessOperation.SEPARATE_PHYSICAL_PHASES, ProcessOperation.SEPARATE_BY_BOILING_POINT);
            case FRACTIONATION -> ops(ProcessOperation.SEPARATE_PHYSICAL_PHASES,
                    ProcessOperation.SEPARATE_BY_BOILING_POINT, ProcessOperation.FRACTIONATE_BY_BOILING_POINT);
            case CRACKING -> ops(ProcessOperation.BREAK_OR_FORM_BONDS, ProcessOperation.CRACK_MOLECULES);
            case REFORMING -> ops(ProcessOperation.BREAK_OR_FORM_BONDS, ProcessOperation.REFORM_MOLECULES);
            case POLYMERIZATION -> ops(ProcessOperation.BREAK_OR_FORM_BONDS, ProcessOperation.POLYMERIZE);
            case PYROLYSIS -> ops(ProcessOperation.THERMAL_DECOMPOSITION, ProcessOperation.BREAK_OR_FORM_BONDS);
            case ABSORPTION -> ops(ProcessOperation.ABSORB);
            case ADSORPTION -> ops(ProcessOperation.ADSORB);
            case DRYING -> ops(ProcessOperation.REMOVE_SOLVENT);
            case EVAPORATION -> ops(ProcessOperation.EVAPORATE_SOLVENT);
            case VAPORIZATION -> ops(ProcessOperation.VAPORIZE);
            case CONDENSATION -> ops(ProcessOperation.CONDENSE);
            case LIQUEFACTION -> ops(ProcessOperation.LIQUEFY);
            case FREEZING -> ops(ProcessOperation.FREEZE);
            case MELTING -> ops(ProcessOperation.MELT);
        });
    }

    private static Set<ProcessOperation> ops(ProcessOperation first, ProcessOperation... rest) {
        EnumSet<ProcessOperation> result = EnumSet.of(first, rest);
        return Set.copyOf(result);
    }
}
