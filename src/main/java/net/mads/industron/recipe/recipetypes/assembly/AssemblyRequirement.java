package net.mads.industron.recipe.recipetypes.assembly;

import net.mads.industron.material.MaterialLookup;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.MaterialProperties;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.machine.MachineTierStats;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.math.BigDecimal;

public record AssemblyRequirement(
        AssemblyCapability capability,
        Kind kind,
        double first,
        double second,
        AssemblyProperty<?> property,
        Object expected
) {
    public enum Kind { AT_LEAST, AT_MOST, EXACT, RANGE, IS }

    /** Compatibility constructor for older code that instantiated numeric requirements directly. */
    public AssemblyRequirement(AssemblyCapability capability, Kind kind, double first, double second) {
        this(capability, kind, first, second, null, null);
    }

    static AssemblyRequirement atLeast(AssemblyCapability capability, double value) {
        return new AssemblyRequirement(capability, Kind.AT_LEAST, value, value, null, null);
    }

    static AssemblyRequirement atMost(AssemblyCapability capability, double value) {
        return new AssemblyRequirement(capability, Kind.AT_MOST, value, value, null, null);
    }

    static AssemblyRequirement exactly(AssemblyCapability capability, double value) {
        return new AssemblyRequirement(capability, Kind.EXACT, value, value, null, null);
    }

    static AssemblyRequirement range(AssemblyCapability capability, double min, double max) {
        if (max < min) throw new IllegalArgumentException("Range max cannot be lower than min");
        return new AssemblyRequirement(capability, Kind.RANGE, min, max, null, null);
    }

    static <T> AssemblyRequirement is(AssemblyProperty<T> property, T expected) {
        return new AssemblyRequirement(null, Kind.IS, 0.0D, 0.0D, property, expected);
    }

    public boolean matches(ItemStack stack) {
        // Tier requirements also apply directly to finished Assembly tools. This lets recipes use
        // .tool(...).tier(MachineTier.ULV) without exposing the numeric tier multiplier.
        if (kind != Kind.IS && capability == AssemblyCapability.TIER_MULTIPLIER) {
            ToolVariantDefinition tool = AssemblyTools.findAny(stack);
            if (tool != null) {
                double value = MachineTierStats.tierIndex(tool.tier().recipeTier()) + 1;
                return matchesScalar(value);
            }
        }

        MaterialLookup.MaterialTarget target = MaterialLookup.find(stack);
        if (target == null) {
            AssemblyMaterialCatalog.Target assemblyTarget = AssemblyMaterialCatalog.find(stack);
            if (assemblyTarget != null && assemblyTarget.material() instanceof IndustrialMaterial industrial) {
                target = new MaterialLookup.MaterialTarget(industrial, assemblyTarget.part());
            }
        }
        if (target == null) return false;
        MaterialProperties properties = target.material().properties();

        if (kind == Kind.IS) {
            return property != null && property.matches(properties, expected);
        }

        if (capability == null) return false;

        if (capability.isPracticalStat()) {
            var resolved = PracticalCapabilityResolver.resolveScalar(capability, target);
            if (resolved.isEmpty()) return false;
            double value = resolved.getAsDouble();
            return matchesScalar(value);
        }

        return matches(properties);
    }

    /** Matches a concrete material form without requiring an already-created ItemStack. */
    public boolean matches(IndustrialMaterial material, MaterialPart part) {
        if (material == null || part == null || !AssemblyMaterialCatalog.exposesPart(material, part)) return false;

        if (kind == Kind.IS) {
            return property != null && property.matches(material.properties(), expected);
        }
        if (capability == null) return false;

        if (capability.isPracticalStat()) {
            var resolved = PracticalCapabilityResolver.resolveScalar(
                    capability,
                    new MaterialLookup.MaterialTarget(material, part)
            );
            if (resolved.isEmpty()) return false;
            double value = resolved.getAsDouble();
            return matchesScalar(value);
        }

        return matches(material.properties());
    }

    /**
     * Matches a raw material directly. Part-specific practical capabilities are
     * intentionally rejected because they require a concrete MaterialPart.
     */
    public boolean matches(MaterialProperties properties) {
        if (properties == null) return false;

        if (kind == Kind.IS) {
            return property != null && property.matches(properties, expected);
        }

        if (capability == null || capability.isPracticalStat()) return false;

        return switch (kind) {
            case AT_LEAST -> capability.matchesAtLeast(properties, first);
            case AT_MOST -> capability.matchesAtMost(properties, first);
            case EXACT -> capability.matchesExactly(properties, first);
            case RANGE -> capability.matchesRange(properties, first, second);
            case IS -> false;
        };
    }

    private boolean matchesScalar(double value) {
        return switch (kind) {
            case AT_LEAST -> value >= first;
            case AT_MOST -> value <= first;
            case EXACT -> Double.compare(value, first) == 0;
            case RANGE -> value >= first && value <= second;
            case IS -> false;
        };
    }

    public Component tooltip() {
        if (kind == Kind.IS) {
            return Component.literal(property.displayName() + ": " + formatExpected(expected));
        }
        if (capability == AssemblyCapability.TIER_MULTIPLIER && kind == Kind.EXACT
                && first == Math.rint(first)) {
            int index = (int) first - 1;
            if (index >= 0 && index < MachineTier.ALL.size()) {
                return Component.literal("Tier: " + MachineTier.ALL.get(index).displayName());
            }
        }

        String firstText = format(first);
        String secondText = format(second);
        return Component.literal(switch (kind) {
            case AT_LEAST -> capability.displayName() + ": >= " + firstText;
            case AT_MOST -> capability.displayName() + ": <= " + firstText;
            case EXACT -> capability.displayName() + ": = " + firstText;
            case RANGE -> capability.displayName() + ": " + firstText + " to " + secondText;
            case IS -> throw new IllegalStateException("Handled above");
        });
    }

    private static String format(double value) {
        if (Double.isFinite(value) && value == Math.rint(value)) {
            return Long.toString((long) value);
        }
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private static String formatExpected(Object value) {
        if (value instanceof Enum<?> enumValue) {
            String[] words = enumValue.name().toLowerCase(java.util.Locale.ROOT).split("_");
            StringBuilder result = new StringBuilder();
            for (String word : words) {
                if (!result.isEmpty()) result.append(' ');
                result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
            return result.toString();
        }
        return String.valueOf(value);
    }
}
