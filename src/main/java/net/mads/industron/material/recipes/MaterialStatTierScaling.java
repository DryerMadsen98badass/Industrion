package net.mads.industron.material.recipes;

import net.mads.industron.machine.MachineTier;
import net.mads.industron.material.MaterialPropertyCalculator;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyCapability;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyRequirement;

import java.util.Objects;

/**
 * Tier projection for material-stat requirements.
 *
 * <p>Each stat owns the same tier transform used by its MaterialProperties
 * calculation. Intrinsic/atomic stats intentionally use the identity transform.
 * This keeps a casing definition's starting requirement meaningful when it is
 * automatically expanded to later tiers.</p>
 */
public final class MaterialStatTierScaling {
    private static final double TIER_BAND = MaterialPropertyCalculator.DEFAULT_TIER_BAND_SIZE;

    private MaterialStatTierScaling() {
    }

    public static AssemblyRequirement scale(
            AssemblyRequirement requirement,
            MachineTier startTier,
            MachineTier targetTier
    ) {
        Objects.requireNonNull(requirement, "requirement");
        int delta = tierDelta(startTier, targetTier);
        if (delta == 0 || requirement.kind() == AssemblyRequirement.Kind.IS) {
            return requirement;
        }

        AssemblyCapability capability = requirement.capability();
        if (capability == null) {
            return requirement;
        }

        if (capability == AssemblyCapability.CHEMICAL && requirement.kind() == AssemblyRequirement.Kind.RANGE) {
            double spread = chemicalRangeStep() * delta;
            return new AssemblyRequirement(
                    capability,
                    requirement.kind(),
                    requirement.first() - spread,
                    requirement.second() + spread,
                    null,
                    null
            );
        }

        double first = scaleValue(capability, requirement.first(), startTier, targetTier, delta);
        double second = requirement.kind() == AssemblyRequirement.Kind.RANGE
                ? scaleValue(capability, requirement.second(), startTier, targetTier, delta)
                : first;

        return new AssemblyRequirement(
                capability,
                requirement.kind(),
                first,
                second,
                requirement.property(),
                requirement.expected()
        );
    }

    public static double scaleValue(
            AssemblyCapability capability,
            double value,
            MachineTier startTier,
            MachineTier targetTier
    ) {
        return scaleValue(capability, value, startTier, targetTier, tierDelta(startTier, targetTier));
    }

    private static double scaleValue(
            AssemblyCapability capability,
            double value,
            MachineTier startTier,
            MachineTier targetTier,
            int delta
    ) {
        return switch (capability) {
            // MaterialPropertyCalculator.tierBanded(...)
            case HARDNESS,
                    TENSILE_STRENGTH,
                    YIELD_STRENGTH,
                    FRACTURE_TOUGHNESS,
                    COMPRESSIVE_STRENGTH,
                    WEAR_RESISTANCE,
                    FATIGUE_RESISTANCE,
                    STRUCTURAL_STRENGTH,
                    THERMAL_CONDUCTIVITY,
                    THERMAL_SHOCK_RESISTANCE,
                    ELECTRICAL_CONDUCTIVITY,
                    INSULATION_STRENGTH,
                    BATTERY_POTENTIAL,
                    CORROSION_RESISTANCE,
                    CHEMICAL_STABILITY,
                    OXIDATION_RESISTANCE,
                    PRESSURE_RESISTANCE,
                    MAGNETIC_STRENGTH,
                    CRYSTAL_HARDNESS,
                    PIPE_CAPABILITY_SCORE,
                    PUMP_FLOW_SCORE,
                    TANK_CAPABILITY_SCORE -> value + TIER_BAND * delta;

            // tierBanded(...) followed by a fixed unit conversion.
            case BOND_ENERGY -> value + TIER_BAND * 10.0D * delta;
            case MAX_PRESSURE -> value + TIER_BAND * 15.0D * delta;
            case CRYSTAL_FORMATION_PRESSURE -> value + TIER_BAND * 5.0D * delta;

            // tier index is part of the temperature safety margin. Solid/liquid
            // material casings use the +10 C center shift per tier.
            case MAX_OPERATING_TEMPERATURE,
                    MAX_FLUID_TEMPERATURE -> value + 10.0D * delta;

            // Generated visual brightness has a +2 per-tier contribution.
            case BRIGHTNESS -> value + 2.0D * delta;

            // This stat is literally tier index + 1.
            case TIER_MULTIPLIER -> value + delta;

            // Fuel burn time multiplies by tierMultiplier.
            case FURNACE_BURN_TIME_TICKS -> scaleByTierMultiplier(value, startTier, targetTier);

            // Transport output is exponential in a capability score. The score
            // itself shifts by the normal tier band.
            case PIPE_THROUGHPUT -> scaleExponential(value, 512.0D, delta, true);
            case PUMP_FLOW_RATE -> scaleExponential(value, 4.0D, delta, false);
            case TANK_CAPACITY -> scaleExponential(value, 5500.0D, delta, true);

            // Chemical transport is derived from three +65 tier-banded stats.
            // 0.85 is the neutral-center multiplier used by the calculator.
            case MAX_CHEMICAL_RANGE -> value + chemicalRangeStep() * delta;
            case MIN_CHEMICAL_RANGE -> value - chemicalRangeStep() * delta;

            // Intrinsic/atomic, classification, processability, state, color,
            // and derived stats without a direct tier term intentionally remain
            // unchanged when a requirement is projected to a later tier.
            default -> value;
        };
    }

    private static int tierDelta(MachineTier startTier, MachineTier targetTier) {
        Objects.requireNonNull(startTier, "startTier");
        Objects.requireNonNull(targetTier, "targetTier");
        int start = MachineTier.ALL.indexOf(startTier);
        int target = MachineTier.ALL.indexOf(targetTier);
        if (start < 0 || target < 0) {
            throw new IllegalArgumentException("Casing stat scaling only supports electric material tiers");
        }
        if (target < start) {
            throw new IllegalArgumentException(
                    "Target tier " + targetTier.displayName() + " is below start tier " + startTier.displayName()
            );
        }
        return target - start;
    }

    private static double scaleByTierMultiplier(double value, MachineTier startTier, MachineTier targetTier) {
        double startMultiplier = MachineTier.ALL.indexOf(startTier) + 1.0D;
        double targetMultiplier = MachineTier.ALL.indexOf(targetTier) + 1.0D;
        return value * targetMultiplier / startMultiplier;
    }

    private static double scaleExponential(double value, double valueAt50, int delta, boolean integral) {
        if (value <= 0.0D) {
            return value;
        }
        double score = 50.0D + 50.0D * (Math.log(value / valueAt50) / Math.log(2.0D));
        double targetScore = score + TIER_BAND * delta;
        double exponent = Math.min(30.0D, Math.max(-30.0D, (targetScore - 50.0D) / 50.0D));
        double scaled = valueAt50 * Math.pow(2.0D, exponent);
        if (integral) {
            return Math.max(1.0D, Math.round(scaled));
        }
        return Math.round(scaled * 10.0D) / 10.0D;
    }

    private static double chemicalRangeStep() {
        return TIER_BAND * 0.85D;
    }
}
