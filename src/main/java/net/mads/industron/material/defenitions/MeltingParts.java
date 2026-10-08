package net.mads.industron.material.defenitions;

import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.melting.MeltablePart;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Explicit whitelist of manufactured forms that can be remelted.
 *
 * <p>The physical mB amount is canonical on MaterialPart; this class only declares which of those
 * manufactured forms participate in generic remelting.</p>
 */
public final class MeltingParts {
    // Bearings: 2 rings + 8 balls.
    public static final MeltablePart TINY_BEARING = new MeltablePart(MaterialPart.TINY_BEARING);
    public static final MeltablePart SMALL_BEARING = new MeltablePart(MaterialPart.SMALL_BEARING);
    public static final MeltablePart BEARING = new MeltablePart(MaterialPart.BEARING);
    public static final MeltablePart LARGE_BEARING = new MeltablePart(MaterialPart.LARGE_BEARING);
    public static final MeltablePart HUGE_BEARING = new MeltablePart(MaterialPart.HUGE_BEARING);

    // Bolts.
    public static final MeltablePart TINY_BOLT = new MeltablePart(MaterialPart.TINY_BOLT);
    public static final MeltablePart SMALL_BOLT = new MeltablePart(MaterialPart.SMALL_BOLT);
    public static final MeltablePart BOLT = new MeltablePart(MaterialPart.BOLT);
    public static final MeltablePart LARGE_BOLT = new MeltablePart(MaterialPart.LARGE_BOLT);
    public static final MeltablePart HUGE_BOLT = new MeltablePart(MaterialPart.HUGE_BOLT);

    // Screws keep the same material amount as the bolt they are turned from.
    public static final MeltablePart TINY_SCREW = new MeltablePart(MaterialPart.TINY_SCREW);
    public static final MeltablePart SMALL_SCREW = new MeltablePart(MaterialPart.SMALL_SCREW);
    public static final MeltablePart SCREW = new MeltablePart(MaterialPart.SCREW);
    public static final MeltablePart LARGE_SCREW = new MeltablePart(MaterialPart.LARGE_SCREW);
    public static final MeltablePart HUGE_SCREW = new MeltablePart(MaterialPart.HUGE_SCREW);

    // Plate-derived forms.
    public static final MeltablePart DOUBLE_PLATE = new MeltablePart(MaterialPart.DOUBLE_PLATE);
    public static final MeltablePart LARGE_DOUBLE_PLATE = new MeltablePart(MaterialPart.LARGE_DOUBLE_PLATE);
    public static final MeltablePart DENSE_PLATE = new MeltablePart(MaterialPart.DENSE_PLATE);
    public static final MeltablePart LARGE_DENSE_PLATE = new MeltablePart(MaterialPart.LARGE_DENSE_PLATE);
    public static final MeltablePart FOIL = new MeltablePart(MaterialPart.FOIL);

    // Wire. MaterialPart.WIRE is intentionally NOT defined; only fine wire and sized wires exist.
    public static final MeltablePart FINE_WIRE = new MeltablePart(MaterialPart.FINE_WIRE);
    public static final MeltablePart WIRE_1X = new MeltablePart(MaterialPart.WIRE_1X);
    public static final MeltablePart WIRE_2X = new MeltablePart(MaterialPart.WIRE_2X);
    public static final MeltablePart WIRE_4X = new MeltablePart(MaterialPart.WIRE_4X);
    public static final MeltablePart WIRE_8X = new MeltablePart(MaterialPart.WIRE_8X);
    public static final MeltablePart WIRE_16X = new MeltablePart(MaterialPart.WIRE_16X);

    // Springs and coils.
    public static final MeltablePart TINY_SPRING = new MeltablePart(MaterialPart.TINY_SPRING);
    public static final MeltablePart SMALL_SPRING = new MeltablePart(MaterialPart.SMALL_SPRING);
    public static final MeltablePart SPRING = new MeltablePart(MaterialPart.SPRING);
    public static final MeltablePart LARGE_SPRING = new MeltablePart(MaterialPart.LARGE_SPRING);
    public static final MeltablePart HUGE_SPRING = new MeltablePart(MaterialPart.HUGE_SPRING);
    public static final MeltablePart COIL = new MeltablePart(MaterialPart.COIL);

    public static final List<MeltablePart> ALL = List.of(
            TINY_BEARING, SMALL_BEARING, BEARING, LARGE_BEARING, HUGE_BEARING,
            TINY_BOLT, SMALL_BOLT, BOLT, LARGE_BOLT, HUGE_BOLT,
            TINY_SCREW, SMALL_SCREW, SCREW, LARGE_SCREW, HUGE_SCREW,
            DOUBLE_PLATE, LARGE_DOUBLE_PLATE, DENSE_PLATE, LARGE_DENSE_PLATE, FOIL,
            FINE_WIRE, WIRE_1X, WIRE_2X, WIRE_4X, WIRE_8X, WIRE_16X,
            TINY_SPRING, SMALL_SPRING, SPRING, LARGE_SPRING, HUGE_SPRING, COIL
    );

    private static final Map<MaterialPart, MeltablePart> BY_PART = buildLookup();

    private MeltingParts() {
    }

    public static MeltablePart get(MaterialPart part) {
        return part == null ? null : BY_PART.get(part);
    }

    public static int millibuckets(MaterialPart part) {
        MeltablePart definition = get(part);
        return definition == null ? 0 : definition.millibuckets();
    }

    private static Map<MaterialPart, MeltablePart> buildLookup() {
        EnumMap<MaterialPart, MeltablePart> definitions = new EnumMap<>(MaterialPart.class);
        for (MeltablePart definition : ALL) {
            MeltablePart previous = definitions.put(definition.part(), definition);
            if (previous != null) {
                throw new IllegalStateException("Duplicate melting definition for " + definition.part());
            }
        }
        return Map.copyOf(definitions);
    }
}
