package net.mads.industron.material;

import java.util.Locale;

/**
 * Selects the grayscale base-block template used by generated gem blocks.
 *
 * <p>This only controls MaterialPart.BLOCK in material_sets/gem. Decorative
 * gem structures are a separate structure-set family and always use the
 * Quartz structure templates.</p>
 */
public enum GemBlockStyle {
    DIAMOND,
    LAPIS,
    REDSTONE,
    EMERALD,
    QUARTZ,
    NETHERITE;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Deterministic default for fictional/generated gems. */
    public static GemBlockStyle forAtomicNumber(int atomicNumber) {
        if (atomicNumber <= 0) {
            throw new IllegalArgumentException("Atomic number must be positive");
        }
        GemBlockStyle[] styles = values();
        return styles[Math.floorMod(atomicNumber - 1, styles.length)];
    }
}
