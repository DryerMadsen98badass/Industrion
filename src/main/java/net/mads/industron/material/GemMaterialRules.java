package net.mads.industron.material;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Shared gem-bearing identity, generated forms and exact gem material quantities. */
public final class GemMaterialRules {
    private static final EnumSet<MaterialPart> ORE_GEM_FORMS = EnumSet.of(
            MaterialPart.TINY_GEM,
            MaterialPart.SMALL_GEM,
            MaterialPart.GEM,
            MaterialPart.FLAWLESS_GEM,
            MaterialPart.EXQUISITE_GEM,
            MaterialPart.ROUGH_TINY_GEM,
            MaterialPart.ROUGH_SMALL_GEM,
            MaterialPart.ROUGH_GEM,
            MaterialPart.ROUGH_FLAWLESS_GEM,
            MaterialPart.ROUGH_EXQUISITE_GEM,
            MaterialPart.LENS
    );

    private GemMaterialRules() {
    }

    /**
     * Natural ore is gem-bearing when any recursively contained substance is itself a calculated
     * gem candidate. The parent ore does not become a generic gem material; it only receives the
     * physical gem forms needed by ore recovery.
     */
    public static boolean isGemBearing(IndustrialMaterial material) {
        if (material == null) return false;
        return containsGemCandidate(material.components(), new HashSet<>());
    }

    public static boolean isGemBearing(List<MaterialComponent> components) {
        return containsGemCandidate(components, new HashSet<>());
    }

    public static Set<MaterialPart> oreGemForms() {
        return Set.copyOf(ORE_GEM_FORMS);
    }

    public static boolean isGemForm(MaterialPart part) {
        return part != null && switch (part) {
            case TINY_GEM, SMALL_GEM, GEM, FLAWLESS_GEM, EXQUISITE_GEM,
                    ROUGH_TINY_GEM, ROUGH_SMALL_GEM, ROUGH_GEM,
                    ROUGH_FLAWLESS_GEM, ROUGH_EXQUISITE_GEM -> true;
            default -> false;
        };
    }

    /** Exact amount represented by each rough/polished gem grade, in mB-equivalent material units. */
    public static int millibuckets(MaterialPart part) {
        if (part == null) return 0;
        return switch (part) {
            case TINY_GEM, ROUGH_TINY_GEM -> 36;          // 1 Small Dust
            case SMALL_GEM, ROUGH_SMALL_GEM -> 72;       // 2 Small Dust
            case GEM, ROUGH_GEM -> 144;                  // 1 Dust
            case FLAWLESS_GEM, ROUGH_FLAWLESS_GEM -> 288; // 2 Dust
            case EXQUISITE_GEM, ROUGH_EXQUISITE_GEM -> 576; // 4 Dust
            default -> 0;
        };
    }

    private static boolean containsGemCandidate(
            List<MaterialComponent> components,
            Set<String> visiting
    ) {
        if (components == null || components.isEmpty()) return false;
        for (MaterialComponent component : components) {
            if (component != null && containsGemCandidate(component.substance(), visiting)) return true;
        }
        return false;
    }

    private static boolean containsGemCandidate(IndustrialSubstance substance, Set<String> visiting) {
        if (substance == null) return false;
        String key = substance.getClass().getName() + ":" + substance.id();
        if (!visiting.add(key)) return false;
        try {
            if (substance instanceof IndustrialMaterial material) {
                if (material.properties().gemCandidate()) return true;
                return containsGemCandidate(material.components(), visiting);
            }
            if (substance instanceof ElementDefinition element) {
                return MaterialPropertyCalculator.calculate(element).gemCandidate();
            }
            return false;
        } finally {
            visiting.remove(key);
        }
    }
}
