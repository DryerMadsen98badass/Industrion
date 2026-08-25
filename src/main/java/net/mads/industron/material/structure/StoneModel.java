package net.mads.industron.material.structure;

public enum StoneModel implements StructureModel {
    ANDESITE("andesite"),
    ASURINE("asurine"),
    BASALT("basalt"),
    BLACKSTONE("blackstone"),
    CALCITE("calcite"),
    CRIMSITE("crimsite"),
    DEEPSLATE("deepslate"),
    DIORITE("diorite"),
    DRIPSTONE("dripstone"),
    END_STONE("end_stone"),
    GRANITE("granite"),
    LIMESTONE("limestone"),
    NETHERRACK("netherrack"),
    OCHRUM("ochrum"),
    RED_SANDSTONE("red_sandstone"),
    SANDSTONE("sandstone"),
    SCORCHIA("scorchia"),
    SCORIA("scoria"),
    TUFF("tuff"),
    VERIDIUM("veridium");

    private final String id;

    StoneModel(String id) {
        this.id = id;
    }

    @Override
    public String category() {
        return "stone";
    }

    @Override
    public String id() {
        return id;
    }
}
