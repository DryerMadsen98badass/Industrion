package net.mads.industron.material.chemistry.process;

/**
 * Typed physical/chemical capabilities used by process selection.
 *
 * <p>These are intentionally independent from machine names. A chemistry route asks for operations;
 * a resolver may then select a registered process/RecipeType whose rule set covers every operation.</p>
 */
public enum ProcessOperation {
    MIX_PHASES,
    SEPARATE_PHYSICAL_PHASES,
    SEPARATE_BY_DENSITY,
    SEPARATE_BY_MAGNETISM,
    FILTER_SUSPENSION,
    SEPARATE_IMMISCIBLE_PHASES,
    SEPARATE_GASES,
    SEPARATE_BY_BOILING_POINT,
    FRACTIONATE_BY_BOILING_POINT,
    BREAK_OR_FORM_BONDS,
    TRANSFER_ELECTRONS,
    CHANGE_OXIDATION_STATE,
    FORM_IONIC_STRUCTURE,
    FORM_METALLIC_PHASE,
    CRYSTALLIZE,
    DISSOLVE,
    LEACH_SOLUBLE_COMPONENTS,
    PRECIPITATE,
    NEUTRALIZE_ACID_BASE,
    EXTRACT_BY_SOLVENT,
    THERMAL_REDUCTION,
    THERMAL_OXIDATION,
    THERMAL_DECOMPOSITION,
    POLYMERIZE,
    CRACK_MOLECULES,
    REFORM_MOLECULES,
    ABSORB,
    ADSORB,
    REMOVE_SOLVENT,
    EVAPORATE_SOLVENT,
    VAPORIZE,
    CONDENSE,
    LIQUEFY,
    FREEZE,
    MELT
}
