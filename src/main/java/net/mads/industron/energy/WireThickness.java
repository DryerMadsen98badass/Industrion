package net.mads.industron.energy;

import net.mads.industron.material.MaterialPart;

import java.util.List;

public enum WireThickness {
    X1("1x", "1x", 4, 1, MaterialPart.WIRE_1X),
    X2("2x", "2x", 6, 2, MaterialPart.WIRE_2X),
    X4("4x", "4x", 8, 4, MaterialPart.WIRE_4X),
    X8("8x", "8x", 10, 8, MaterialPart.WIRE_8X),
    X16("16x", "16x", 12, 16, MaterialPart.WIRE_16X);

    public static final List<WireThickness> ALL = List.of(values());

    private final String id;
    private final String displayName;
    private final int pixels;
    private final int ampMultiplier;
    private final MaterialPart materialPart;

    WireThickness(String id, String displayName, int pixels, int ampMultiplier, MaterialPart materialPart) {
        this.id = id;
        this.displayName = displayName;
        this.pixels = pixels;
        this.ampMultiplier = ampMultiplier;
        this.materialPart = materialPart;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public int pixels() {
        return pixels;
    }

    public int ampMultiplier() {
        return ampMultiplier;
    }

    public MaterialPart materialPart() {
        return materialPart;
    }
}
