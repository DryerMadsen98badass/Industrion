package net.mads.industron.material;

import net.mads.industron.machine.MachineTier;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Shared ore dimension policy. Ore hosts themselves are discovered from StoneMaterials. */
public final class MaterialOrePolicy {
    public enum DimensionBand {
        OVERWORLD,
        NETHER,
        END
    }

    private MaterialOrePolicy() {
    }

    public static DimensionBand dimensionForTier(MachineTier tier) {
        int index = MachineTier.ALL.indexOf(tier);
        if (index < 0) {
            throw new IllegalArgumentException("Unknown material tier: " + tier);
        }
        if (index <= MachineTier.ALL.indexOf(MachineTier.HV)) {
            return DimensionBand.OVERWORLD;
        }
        if (index <= MachineTier.ALL.indexOf(MachineTier.LUV)) {
            return DimensionBand.NETHER;
        }
        return DimensionBand.END;
    }

    /** Dynamic host list. Adding a StoneMaterial automatically adds an ore host. */
    public static List<MaterialOreHost> oreHosts(DimensionBand dimension) {
        return MaterialOreHost.forDimension(dimension);
    }

    public static List<MaterialOreHost> allOreHosts() {
        return MaterialOreHost.all();
    }

    /**
     * Legacy MaterialPart compatibility only. These are not the source of truth for available hosts.
     */
    public static Set<MaterialPart> oreParts(DimensionBand dimension) {
        EnumSet<MaterialPart> result = EnumSet.noneOf(MaterialPart.class);
        for (MaterialOreHost host : oreHosts(dimension)) {
            host.legacyPart(false).ifPresent(result::add);
            host.legacyPart(true).ifPresent(result::add);
        }
        return Set.copyOf(result);
    }

    public static Set<MaterialPart> allOreParts() {
        Set<MaterialPart> result = new LinkedHashSet<>();
        for (MaterialOreHost host : allOreHosts()) {
            host.legacyPart(false).ifPresent(result::add);
            host.legacyPart(true).ifPresent(result::add);
        }
        return Set.copyOf(result);
    }
}
