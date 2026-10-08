package net.mads.industron.machine.foundry;

import java.util.ArrayList;
import java.util.List;

/** Pixel-space geometry shared by generated models and collision shapes. North is the outlet. */
public final class CastingGeometry {
    public record Cuboid(double x0, double y0, double z0, double x1, double y1, double z1) {}
    public static final List<Cuboid> CASTER = caster();
    public static final List<Cuboid> FAUCET = List.of(
            new Cuboid(4, 5, 14, 12, 12, 16),
            new Cuboid(5, 6, 7, 7, 7, 14),
            new Cuboid(9, 6, 7, 11, 7, 14),
            new Cuboid(7, 6, 9, 9, 7, 14),
            new Cuboid(5, 7, 6, 6, 10, 14),
            new Cuboid(10, 7, 6, 11, 10, 14),
            new Cuboid(6, 7, 6, 10, 9, 7),
            new Cuboid(6, 4, 7, 7, 7, 10),
            new Cuboid(9, 4, 7, 10, 7, 10),
            new Cuboid(7, 4, 9, 9, 7, 10));
    public static final List<Cuboid> DRAIN = List.of(
            new Cuboid(0, 0, 0, 16, 4, 16),
            new Cuboid(0, 12, 0, 16, 16, 16),
            new Cuboid(0, 4, 0, 4, 12, 16),
            new Cuboid(12, 4, 0, 16, 12, 16),
            new Cuboid(4, 4, 4, 12, 12, 16),
            new Cuboid(4, 4, 1, 12, 5, 4),
            new Cuboid(4, 11, 1, 12, 12, 4),
            new Cuboid(4, 5, 1, 5, 11, 4),
            new Cuboid(11, 5, 1, 12, 11, 4));

    private CastingGeometry() {}
    private static List<Cuboid> caster() {
        List<Cuboid> parts = new ArrayList<>();
        for (int x : new int[]{0, 13}) for (int z : new int[]{0, 13}) {
            parts.add(new Cuboid(x, 0, z, x + 3, 2, z + 3));
            parts.add(new Cuboid(x + .5, 2, z + .5, x + 2.5, 8, z + 2.5));
        }
        // Full block tabletop, with a 14 by 14 pixel unobstructed recess.
        parts.add(new Cuboid(0, 8, 0, 16, 10, 16));
        parts.add(new Cuboid(0, 10, 0, 1, 12, 16));
        parts.add(new Cuboid(15, 10, 0, 16, 12, 16));
        parts.add(new Cuboid(1, 10, 0, 15, 12, 1));
        parts.add(new Cuboid(1, 10, 15, 15, 12, 16));
        return List.copyOf(parts);
    }
}
