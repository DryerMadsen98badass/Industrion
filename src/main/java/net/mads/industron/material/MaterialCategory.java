package net.mads.industron.material;

/**
 * Broad semantic material categories derived from the material data itself.
 *
 * <p>These are not registration lists. {@link MaterialCatalog} derives membership from
 * {@link MaterialProperties} for every entry in IndustrialMaterials, so new registered materials and
 * defined alloys automatically participate without being copied into recipe APIs.</p>
 *
 * <p>An alloy is a {@link #METAL}: compound chemistry marks a true metallic lattice as
 * {@code properties().metal() == true}. Molten form does not change the substance category;
 * a metal remains METAL even when represented by a molten fluid.</p>
 */
public enum MaterialCategory {
    METAL,
    GEM,
    FLUID,
    GAS,
    OTHER_SOLID;

    public static MaterialCategory of(IndustrialMaterial material) {
        if (material == null) throw new IllegalArgumentException("Material cannot be null");
        MaterialProperties properties = material.properties();

        // Clay is a closed gameplay family. Calculated bulk chemistry must never leak it into
        // generic metal/gem structure, wire or transport libraries.
        if (material.isClayMaterial() || material.isCeramicBrickMaterial()) return OTHER_SOLID;

        // Substance identity wins over ambient physical state. This keeps alloys in METAL.
        if (properties.metal()) return METAL;
        if (properties.gemCandidate()) return GEM;

        return switch (properties.state()) {
            case LIQUID -> FLUID;
            case GAS -> GAS;
            case SOLID -> OTHER_SOLID;
        };
    }

    public boolean matches(IndustrialMaterial material) {
        return material != null && of(material) == this;
    }
}
