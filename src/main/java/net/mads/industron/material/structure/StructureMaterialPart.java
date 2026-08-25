package net.mads.industron.material.structure;

public enum StructureMaterialPart {
    BLOCK("", "", Kind.BLOCK),

    LOG("log", "Log", Kind.BLOCK),
    STRIPPED_LOG("stripped_log", "Stripped Log", Kind.BLOCK),
    WOOD("wood", "Wood", Kind.BLOCK),
    STRIPPED_WOOD("stripped_wood", "Stripped Wood", Kind.BLOCK),
    PLANKS("planks", "Planks", Kind.BLOCK),
    SLAB("slab", "Slab", Kind.BLOCK),
    STAIRS("stairs", "Stairs", Kind.BLOCK),
    FENCE("fence", "Fence", Kind.BLOCK),
    FENCE_GATE("fence_gate", "Fence Gate", Kind.BLOCK),
    BUTTON("button", "Button", Kind.BLOCK),
    PRESSURE_PLATE("pressure_plate", "Pressure Plate", Kind.BLOCK),
    DOOR("door", "Door", Kind.BLOCK),
    TRAPDOOR("trapdoor", "Trapdoor", Kind.BLOCK),
    LEAVES("leaves", "Leaves", Kind.BLOCK),
    SAPLING("sapling", "Sapling", Kind.BLOCK),
    WINDOW("window", "Window", Kind.BLOCK),

    TINY_DUST("tiny_dust", "Tiny Dust", Kind.ITEM),
    SMALL_DUST("small_dust", "Small Dust", Kind.ITEM),
    DUST("dust", "Dust", Kind.ITEM),

    TINY_WOOD_PULP("tiny_wood_pulp", "Tiny Wood Pulp", Kind.ITEM),
    SMALL_WOOD_PULP("small_wood_pulp", "Small Wood Pulp", Kind.ITEM),
    WOOD_PULP("wood_pulp", "Wood Pulp", Kind.ITEM),

    MOLTEN("molten", "Molten", Kind.FLUID),

    // Gem-only decorative roles are appended so existing enum ordinals stay stable.
    WALL("wall", "Wall", Kind.BLOCK),
    PILLAR("pillar", "Pillar", Kind.BLOCK),
    CHISELED_BLOCK("chiseled_block", "Chiseled Block", Kind.BLOCK),
    BRICKS("bricks", "Bricks", Kind.BLOCK),
    SMOOTH_BLOCK("smooth_block", "Smooth Block", Kind.BLOCK),
    SMOOTH_SLAB("smooth_slab", "Smooth Slab", Kind.BLOCK),
    SMOOTH_STAIRS("smooth_stairs", "Smooth Stairs", Kind.BLOCK);

    private final String id;
    private final String displayName;
    private final Kind kind;

    StructureMaterialPart(String id, String displayName, Kind kind) {
        this.id = id;
        this.displayName = displayName;
        this.kind = kind;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public Kind kind() {
        return kind;
    }

    public boolean isBlock() {
        return kind == Kind.BLOCK;
    }

    public boolean isItem() {
        return kind == Kind.ITEM;
    }

    public boolean isFluid() {
        return kind == Kind.FLUID;
    }

    public String registryName(StructureMaterial material) {
        return switch (this) {
            case BLOCK -> material.id();
            case CHISELED_BLOCK -> "chiseled_" + material.id() + "_block";
            case SMOOTH_BLOCK -> "smooth_" + material.id() + "_block";
            case SMOOTH_SLAB -> "smooth_" + material.id() + "_slab";
            case SMOOTH_STAIRS -> "smooth_" + material.id() + "_stairs";
            default -> material.id() + "_" + id;
        };
    }

    public String readableName(StructureMaterial material) {
        return switch (this) {
            case BLOCK -> material.displayName();
            case TINY_DUST -> "Tiny " + material.displayName() + " Dust";
            case SMALL_DUST -> "Small " + material.displayName() + " Dust";
            case DUST -> material.displayName() + " Dust";
            case TINY_WOOD_PULP -> "Tiny " + material.displayName() + " Wood Pulp";
            case SMALL_WOOD_PULP -> "Small " + material.displayName() + " Wood Pulp";
            case WOOD_PULP -> material.displayName() + " Wood Pulp";
            case MOLTEN -> "Molten " + material.displayName();
            case CHISELED_BLOCK -> "Chiseled " + material.displayName() + " Block";
            case SMOOTH_BLOCK -> "Smooth " + material.displayName() + " Block";
            case SMOOTH_SLAB -> "Smooth " + material.displayName() + " Slab";
            case SMOOTH_STAIRS -> "Smooth " + material.displayName() + " Stairs";
            default -> material.displayName() + " " + displayName;
        };
    }

    public enum Kind {
        ITEM,
        BLOCK,
        FLUID
    }
}
