package net.mads.industron.material.defenitions;

/**
 * Reviewed ores that should own ore blocks, geology deposits, loot and world generation.
 *
 * <p>Define an ore here with {@code ore("id", "Display Name").contains(...).build()}.
 * Color, formula, chemistry, properties, strongest-component tier, dimension, Y range, biome,
 * compatible StoneMaterial hosts, deposit geometry and generated host-specific ore blocks are
 * derived automatically. Anonymous suggestions in build/reports/industron are deliberately not
 * registered until a named definition is added here.</p>
 */
public final class OreMaterials {
    // Intentionally empty for now. The existing Sorynite/Soryxite/Raskorite/Nerynite definitions
    // are trace mineral dusts and live in MineralDustMaterials, not here. Add a named ore as a
    // static field with ore("id", "Display Name").contains(...).build(); no companion list is needed.

    private OreMaterials() {
    }

    public static void init() {
        // Touching this class initializes future static ore definitions.
    }
}
