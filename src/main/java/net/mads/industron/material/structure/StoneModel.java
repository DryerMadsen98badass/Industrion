package net.mads.industron.material.structure;

public enum StoneModel implements StructureModel {
    STONE("stone", "stone.png"),
    ANDESITE("andesite", "andesite.png"),
    ASURINE("asurine", "asurine_0.png"),
    BASALT("basalt", "basalt_side.png", "basalt_top.png", "basalt_top.png"),
    BLACKSTONE("blackstone", "blackstone.png", "blackstone_top.png", "blackstone_top.png"),
    CALCITE("calcite", "calcite.png"),
    CRIMSITE("crimsite", "crimsite_0.png"),
    DEEPSLATE("deepslate", "deepslate.png", "deepslate_top.png", "deepslate_top.png"),
    DIORITE("diorite", "diorite.png"),
    DRIPSTONE("dripstone", "dripstone_block.png"),
    END_STONE("end_stone", "end_stone.png"),
    GRANITE("granite", "granite.png"),
    LIMESTONE("limestone", "limestone.png"),
    NETHERRACK("netherrack", "netherrack.png"),
    OCHRUM("ochrum", "ochrum_0.png"),
    RED_SANDSTONE("red_sandstone", "red_sandstone.png", "red_sandstone_top.png", "red_sandstone_bottom.png"),
    SANDSTONE("sandstone", "sandstone.png", "sandstone_top.png", "sandstone_bottom.png"),
    SCORCHIA("scorchia", "scorchia.png"),
    SCORIA("scoria", "scoria.png"),
    TUFF("tuff", "tuff.png"),
    VERIDIUM("veridium", "veridium_0.png");

    private final String id;
    private final String baseSideTexture;
    private final String baseTopTexture;
    private final String baseBottomTexture;

    StoneModel(String id, String baseTexture) {
        this(id, baseTexture, baseTexture, baseTexture);
    }

    StoneModel(String id, String baseSideTexture, String baseTopTexture, String baseBottomTexture) {
        this.id = id;
        this.baseSideTexture = baseSideTexture;
        this.baseTopTexture = baseTopTexture;
        this.baseBottomTexture = baseBottomTexture;
    }

    @Override
    public String category() {
        return "stone";
    }

    @Override
    public String id() {
        return id;
    }

    public String baseSideTexture() {
        return baseSideTexture;
    }

    public String baseTopTexture() {
        return baseTopTexture;
    }

    public String baseBottomTexture() {
        return baseBottomTexture;
    }
}
