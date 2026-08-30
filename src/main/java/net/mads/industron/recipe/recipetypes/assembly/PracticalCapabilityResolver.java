package net.mads.industron.recipe.recipetypes.assembly;

import net.mads.industron.energy.WireThickness;
import net.mads.industron.material.MaterialLookup;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.MaterialProperties;
import net.mads.industron.material.MaterialPropertyCalculator;

import java.util.OptionalDouble;

/**
 * Resolves form-specific assembly capabilities without mutating a material's
 * intrinsic properties. Intrinsic material values remain the source of truth;
 * the concrete MaterialPart contributes only a geometry/application factor.
 */
public final class PracticalCapabilityResolver {
    private PracticalCapabilityResolver() {
    }

    public static OptionalDouble resolveScalar(
            AssemblyCapability capability,
            MaterialLookup.MaterialTarget target
    ) {
        MaterialProperties properties = target.material().properties();
        MaterialPart part = target.part();

        return switch (capability.practicalKind()) {
            case NONE -> capability.isRangeStat()
                    ? OptionalDouble.empty()
                    : OptionalDouble.of(capability.rawScalar(properties));
            case ELECTRICAL_CAPACITY -> electricalCapacity(properties, part);
            case INSULATION_CAPACITY -> insulationCapacity(properties, part);
            case STRUCTURAL_LOAD -> structuralLoad(properties, part);
            case PRESSURE_CAPACITY -> pressureCapacity(properties, part);
            case SHAFT_LOAD -> shaftLoad(properties, part);
            case FASTENER_LOAD -> fastenerLoad(properties, part);
        };
    }

    private static OptionalDouble electricalCapacity(MaterialProperties properties, MaterialPart part) {
        int baseAmps = MaterialPropertyCalculator.wireBaseAmps(properties);
        if (baseAmps <= 0) return OptionalDouble.empty();

        for (WireThickness thickness : WireThickness.ALL) {
            if (thickness.materialPart() == part) {
                return OptionalDouble.of((double) baseAmps * thickness.ampMultiplier());
            }
        }

        if (part == MaterialPart.WIRE) {
            return OptionalDouble.of(baseAmps);
        }
        if (part == MaterialPart.FINE_WIRE) {
            return OptionalDouble.of(baseAmps * 0.5D);
        }

        return OptionalDouble.empty();
    }

    private static OptionalDouble insulationCapacity(MaterialProperties properties, MaterialPart part) {
        if (!properties.electricallyInsulating() || properties.insulationStrength() <= 0) {
            return OptionalDouble.empty();
        }

        double geometryFactor = familySizeFactor(part, "ring");
        if (geometryFactor <= 0.0D) return OptionalDouble.empty();

        return OptionalDouble.of(properties.insulationStrength() * geometryFactor);
    }

    private static OptionalDouble structuralLoad(MaterialProperties properties, MaterialPart part) {
        double geometryFactor = plateStructuralFactor(part);
        if (geometryFactor <= 0.0D) return OptionalDouble.empty();

        return OptionalDouble.of(properties.structuralStrength() * geometryFactor);
    }

    private static OptionalDouble pressureCapacity(MaterialProperties properties, MaterialPart part) {
        double geometryFactor = familySizeFactor(part, "pipe");
        if (geometryFactor <= 0.0D) return OptionalDouble.empty();

        return OptionalDouble.of(properties.maxPressure() * geometryFactor);
    }

    private static OptionalDouble shaftLoad(MaterialProperties properties, MaterialPart part) {
        if (!"rod".equals(part.textureFamily())) return OptionalDouble.empty();

        String size = part.textureSize();
        double lengthFactor = size == null ? 1.00D : switch (size) {
            case "very_short" -> 1.50D;
            case "short" -> 1.25D;
            case "normal" -> 1.00D;
            case "long" -> 0.80D;
            case "very_long" -> 0.60D;
            default -> 1.00D;
        };

        double intrinsicShaftStrength = Math.min(
                properties.structuralStrength(),
                Math.min(properties.tensileStrength(), properties.yieldStrength())
        );
        return OptionalDouble.of(intrinsicShaftStrength * lengthFactor);
    }

    private static OptionalDouble fastenerLoad(MaterialProperties properties, MaterialPart part) {
        String family = part.textureFamily();
        if (!"screw".equals(family) && !"bolt".equals(family) && !"rivet".equals(family)) {
            return OptionalDouble.empty();
        }

        double geometryFactor = sizeFactor(part.textureSize());
        double intrinsicFastenerStrength = Math.min(
                properties.structuralStrength(),
                Math.min(properties.tensileStrength(), properties.yieldStrength())
        );
        return OptionalDouble.of(intrinsicFastenerStrength * geometryFactor);
    }

    private static double plateStructuralFactor(MaterialPart part) {
        String family = part.textureFamily();
        if (family == null) return 0.0D;

        double base = switch (family) {
            case "plate" -> 1.0D;
            case "plate_double" -> 2.0D;
            case "plate_dense" -> 4.0D;
            case "plate_reinforced" -> 2.0D;
            case "plate_heat_exchanger" -> 1.0D;
            default -> 0.0D;
        };
        if (base <= 0.0D) return 0.0D;

        return base * sizeFactor(part.textureSize());
    }

    private static double familySizeFactor(MaterialPart part, String family) {
        if (!family.equals(part.textureFamily())) return 0.0D;
        return sizeFactor(part.textureSize());
    }

    private static double sizeFactor(String size) {
        if (size == null) return 1.0D;
        return switch (size) {
            case "tiny" -> 0.25D;
            case "small" -> 0.50D;
            case "normal" -> 1.00D;
            case "large" -> 2.00D;
            case "huge" -> 4.00D;
            default -> 1.00D;
        };
    }
}
