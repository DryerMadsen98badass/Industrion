package net.mads.industron.block;

public enum SimpleBlockVariant {

    SLAB(
            "_slab",
            "Slab"
    ),

    STAIR(
            "_stairs",
            "Stairs"
    ),

    WALL(
            "_wall",
            "Wall"
    ),

    FENCE(
            "_fence",
            "Fence"
    ),

    FENCE_GATE(
            "_fence_gate",
            "Fence Gate"
    ),

    BUTTON(
            "_button",
            "Button"
    ),

    PRESSURE_PLATE(
            "_pressure_plate",
            "Pressure Plate"
    );

    private final String suffix;
    private final String displaySuffix;

    SimpleBlockVariant(
            String suffix,
            String displaySuffix
    ) {
        this.suffix = suffix;
        this.displaySuffix = displaySuffix;
    }

    public String suffix() {
        return suffix;
    }

    public String displaySuffix() {
        return displaySuffix;
    }
}