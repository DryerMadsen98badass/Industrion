package net.mads.industron.material.chemistry.process;

import java.util.List;

public enum ProcessKind {
    MIXING("mixing"),
    ALLOYING("alloying"),
    SMELTING("smelting"),
    ROASTING("roasting"),
    CALCINATION("calcination"),
    CHEMICAL_REACTION("chemical_reaction"),
    DISSOLUTION("dissolution"),
    NEUTRALIZATION("neutralization"),
    PRECIPITATION("precipitation"),
    CRYSTALLIZATION("crystallization"),
    LEACHING("leaching"),
    SOLVENT_EXTRACTION("solvent_extraction"),
    ELECTROLYSIS("electrolysis"),
    ELECTROREFINING("electrorefining"),
    ELECTROWINNING("electrowinning"),
    CENTRIFUGING("centrifuging", "centrifuge"),
    MAGNETIC_SEPARATION("magnetic_separation"),
    FILTRATION("filtration"),
    PHASE_SEPARATION("phase_separation"),
    GAS_SEPARATION("gas_separation"),
    DISTILLATION("distillation"),
    FRACTIONATION("fractionation"),
    CRACKING("cracking"),
    REFORMING("reforming"),
    POLYMERIZATION("polymerization"),
    PYROLYSIS("pyrolysis"),
    ABSORPTION("absorption"),
    ADSORPTION("adsorption"),
    DRYING("drying"),
    EVAPORATION("evaporation"),
    CONDENSATION("condensation"),
    LIQUEFACTION("liquefaction"),
    FREEZING("freezing"),
    MELTING("melting");

    private final List<String> recipeTypeIds;

    ProcessKind(String... recipeTypeIds) { this.recipeTypeIds=List.of(recipeTypeIds); }
    public List<String> recipeTypeIds() { return recipeTypeIds; }
    public String primaryRecipeTypeId() { return recipeTypeIds.get(0); }
}
