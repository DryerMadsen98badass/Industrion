package net.mads.industron.material;

import net.mads.industron.machine.MachineTier;
import net.mads.industron.material.atomic.AtomicModel;
import net.mads.industron.material.atomic.AtomicState;

import java.util.Set;

public final class MaterialPropertyCalculator {
    public static final int AMBIENT_TEMPERATURE_C = 20;
    public static final int METAL_MELTING_TIER_BAND_C = 500;

    // Historical name kept for compatibility. This is now an overlapping tier-center step,
    // not a non-overlapping 100-point band. Intrinsic atomic specialization supplies more
    // of the final spread than tier does.
    public static final int DEFAULT_TIER_BAND_SIZE = 65;
    private static final double DEFAULT_INTRINSIC_SPREAD = 2.50D;
    private static final float[] COLOR_SATURATION_PROFILES = {0.90F, 0.82F, 0.48F, 0.56F};
    private static final float[] COLOR_BRIGHTNESS_PROFILES = {0.92F, 0.62F, 0.93F, 0.58F};

    private MaterialPropertyCalculator() {
    }

    /**
     * Center value of the default tier-banded material-property scale. The scale is deliberately
     * open-ended; adding later MachineTier entries does not require changing chemistry thresholds.
     */
    public static double defaultTierBandCenter(int tierIndex) {
        int safeTier = Math.max(0, tierIndex);
        double intrinsicCenter = 5.0D + 50.0D * DEFAULT_INTRINSIC_SPREAD;
        return safeTier * (double) DEFAULT_TIER_BAND_SIZE + intrinsicCenter;
    }

    /**
     * Converts an open-ended tier-banded property score back to the nearest tier index.
     * This method intentionally does not clamp to MachineTier.ALL so callers can decide how
     * to handle values beyond the currently registered highest tier.
     */
    public static int tierIndexForBandedValue(double value) {
        if (!Double.isFinite(value)) return 0;
        double rawIndex = (value - defaultTierBandCenter(0)) / DEFAULT_TIER_BAND_SIZE;
        long rounded = Math.round(rawIndex);
        if (rounded <= 0L) return 0;
        return rounded >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) rounded;
    }

    public static MaterialProperties calculate(ElementDefinition element) {
        int atomicNumber = element.atomicNumber();
        int tierIndex = tierIndex(element.tier());
        int tierMultiplier = tierIndex + 1;

        AtomicState atomic = AtomicModel.neutral(atomicNumber);
        MaterialProperties.ElectronicFamily family = atomic.family();

        int metallicityProfile = metallicityProfile(atomic);
        int reactivityProfile = reactivityProfile(atomic);
        int bandMobility = bandMobility(atomic);
        int carrierProfile = carrierProfile(atomic, bandMobility, metallicityProfile);
        int electronMobility = clampScore(weighted(
                carrierProfile, 0.50,
                bandMobility, 0.18,
                atomic.electronDonationTendency(), 0.14,
                100 - atomic.ionizationEnergy(), 0.10,
                atomic.atomicStability(), 0.08
        ));
        int compactness = compactness(atomic);
        int crystalPotential = crystalPotential(atomic, compactness);
        int cohesionProfile = cohesionProfile(atomic, metallicityProfile, crystalPotential);
        int volatility = volatility(atomic, cohesionProfile, crystalPotential, reactivityProfile);
        int polarity = Math.abs(atomic.electronAcceptanceTendency() - atomic.electronDonationTendency());

        int density = safePositiveInt(
                12.0D
                        + atomicNumber * 2.35D
                        + atomic.neutrons() * 0.75D
                        + compactness * 0.55D
        );

        int openShellStrengthProfile = openShellStrengthProfile(atomic);
        int latticeStrengthProfile = latticeStrengthProfile(
                atomic, compactness, crystalPotential, openShellStrengthProfile
        );
        int ductilityScore = clampScore(weighted(
                electronMobility, 0.30,
                metallicityProfile, 0.18,
                100 - latticeStrengthProfile, 0.24,
                atomic.atomicRadius(), 0.14,
                atomic.atomicStability(), 0.14
        ) + ductilityFamilyAdjustment(atomic.family()));
        int hardnessScore = clampScore(weighted(
                latticeStrengthProfile, 0.36,
                openShellStrengthProfile, 0.28,
                atomic.bondStrength(), 0.14,
                compactness, 0.10,
                crystalPotential, 0.06,
                atomic.directionalBonding(), 0.06
        ));
        int elasticityScore = clampScore(weighted(
                electronMobility, 0.24,
                ductilityScore, 0.26,
                atomic.bondStrength(), 0.18,
                atomic.atomicStability(), 0.18,
                100 - latticeStrengthProfile, 0.14
        ));
        int brittlenessScore = clampScore(weighted(
                hardnessScore, 0.28,
                crystalPotential, 0.32,
                atomic.directionalBonding(), 0.18,
                100 - ductilityScore, 0.22
        ));
        int tensileScore = clampScore(weighted(
                latticeStrengthProfile, 0.30,
                openShellStrengthProfile, 0.24,
                atomic.bondStrength(), 0.16,
                ductilityScore, 0.12,
                compactness, 0.08,
                atomic.atomicStability(), 0.10
        ));
        int yieldScore = clampScore(weighted(
                tensileScore, 0.50,
                ductilityScore, 0.30,
                hardnessScore, 0.20
        ));
        int fractureScore = clampScore(weighted(
                ductilityScore, 0.30,
                atomic.atomicStability(), 0.25,
                atomic.bondStrength(), 0.20,
                100 - brittlenessScore, 0.25
        ));
        int compressiveScore = clampScore(weighted(
                latticeStrengthProfile, 0.30,
                openShellStrengthProfile, 0.22,
                hardnessScore, 0.18,
                compactness, 0.18,
                atomic.bondStrength(), 0.12
        ));
        int wearScore = clampScore(weighted(
                hardnessScore, 0.48,
                atomic.atomicStability(), 0.22,
                crystalPotential, 0.30
        ));
        int fatigueScore = clampScore(weighted(
                fractureScore, 0.40,
                elasticityScore, 0.35,
                yieldScore, 0.25
        ));

        int hardness = tierBanded(hardnessScore, tierMultiplier);
        int elasticity = elasticityScore;
        int tensileStrength = tierBanded(tensileScore, tierMultiplier);
        int yieldStrength = tierBanded(yieldScore, tierMultiplier);
        int fractureToughness = tierBanded(fractureScore, tierMultiplier);
        int compressiveStrength = tierBanded(compressiveScore, tierMultiplier);
        int ductility = ductilityScore;
        int brittleness = brittlenessScore;
        int wearResistance = tierBanded(wearScore, tierMultiplier);
        int fatigueResistance = tierBanded(fatigueScore, tierMultiplier);

        MaterialProperties.PhysicalState targetState = intrinsicState(atomic);
        MaterialProperties.MetallicityClass metallicityClass = metallicityClass(metallicityProfile);
        boolean tierBandedMetalMelting = targetState == MaterialProperties.PhysicalState.SOLID
                && (metallicityClass == MaterialProperties.MetallicityClass.STRONGLY_METALLIC
                || metallicityClass == MaterialProperties.MetallicityClass.METALLIC);
        int meltingPoint = meltingPointC(
                atomic,
                compactness,
                crystalPotential,
                cohesionProfile,
                volatility,
                targetState,
                tierIndex,
                tierBandedMetalMelting
        );
        int boilingPoint = boilingPointC(
                meltingPoint,
                atomic,
                cohesionProfile,
                volatility,
                targetState
        );
        MaterialProperties.PhysicalState state = stateForTemperatures(meltingPoint, boilingPoint);
        int metallicity = metallicityProfile;
        boolean metal = state != MaterialProperties.PhysicalState.GAS
                && (metallicityClass == MaterialProperties.MetallicityClass.STRONGLY_METALLIC
                || metallicityClass == MaterialProperties.MetallicityClass.METALLIC);

        int thermalConductivityProfile = clampScore(weighted(
                carrierProfile, 0.38,
                electronMobility, 0.22,
                latticeStrengthProfile, 0.18,
                compactness, 0.12,
                100 - polarity, 0.10
        ));
        int thermalConductivity = tierBanded(thermalConductivityProfile, tierMultiplier);
        int specificHeatCapacity = Math.max(1, Math.round(
                18.0F
                        + volatility * 0.55F
                        + (100 - compactness) * 0.30F
                        + atomic.outerShell() * 3.0F
        ));
        int thermalExpansion = clampScore(weighted(
                volatility, 0.52,
                100 - atomic.bondStrength(), 0.28,
                100 - cohesionProfile, 0.20
        ));
        int maxOperatingTemperature = maxOperatingTemperatureC(
                state,
                meltingPoint,
                boilingPoint,
                thermalExpansion,
                atomic.atomicStability(),
                tierIndex
        );
        int thermalShockScore = clampScore(weighted(
                fractureScore, 0.38,
                100 - thermalExpansion, 0.34,
                atomic.atomicStability(), 0.28
        ));
        int thermalShockResistance = tierBanded(thermalShockScore, tierMultiplier);

        int electricalConductivityProfile = electricalConductivityProfile(
                atomic,
                electronMobility,
                metallicityProfile,
                polarity
        );
        int insulationProfile = clampScore(weighted(
                100 - electricalConductivityProfile, 0.48,
                atomic.atomicStability(), 0.22,
                atomic.directionalBonding(), 0.18,
                polarity, 0.12
        ));

        // Electrical behavior is a hard material-class rule:
        // metal -> conductor, non-metal -> insulator. The opposite rating is always exactly zero.
        MaterialProperties.ElectricalBehavior electricalBehavior = metal
                ? MaterialProperties.ElectricalBehavior.CONDUCTOR
                : MaterialProperties.ElectricalBehavior.INSULATOR;
        int electricalConductivity = metal
                ? tierBanded(electricalConductivityProfile, tierMultiplier)
                : 0;
        int insulationStrength = metal
                ? 0
                : tierBanded(insulationProfile, tierMultiplier);

        int electrochemicalPotential = electrochemicalPotential(
                atomic,
                reactivityProfile,
                metallicityProfile
        );
        int chargeStoragePotential = chargeStoragePotential(
                atomic,
                insulationStrength,
                crystalPotential,
                reactivityProfile
        );
        int batteryProfile = clampScore(Math.max(
                electrochemicalPotential,
                weighted(
                        chargeStoragePotential, 0.72,
                        electrochemicalPotential, 0.28
                )
        ));
        int batteryPotential = tierBanded(batteryProfile, tierMultiplier);

        int oxidationTendency = clampScore(weighted(
                reactivityProfile, 0.42,
                atomic.electronDonationTendency(), 0.33,
                100 - atomic.atomicStability(), 0.25
        ));
        int nobilityProfile = chemicalNobilityProfile(atomic, reactivityProfile);
        int corrosionScore = clampScore(weighted(
                nobilityProfile, 0.68,
                atomic.atomicStability(), 0.10,
                100 - reactivityProfile, 0.08,
                100 - oxidationTendency, 0.08,
                atomic.bondStrength(), 0.06
        ));
        int chemicalStabilityScore = clampScore(weighted(
                nobilityProfile, 0.54,
                atomic.atomicStability(), 0.20,
                atomic.bondStrength(), 0.12,
                100 - reactivityProfile, 0.08,
                cohesionProfile, 0.06
        ));
        int oxidationResistanceScore = 100 - oxidationTendency;
        int corrosionResistance = tierBanded(corrosionScore, tierMultiplier);
        int chemicalStability = tierBanded(chemicalStabilityScore, tierMultiplier);
        int reactivity = reactivityProfile;
        int oxidationResistance = tierBanded(oxidationResistanceScore, tierMultiplier);
        int acidity = clamp(
                Math.round((atomic.electronAcceptanceTendency() - atomic.electronDonationTendency()) / 2.0F),
                -50,
                50
        );

        int structuralScore = weakestWeightedAverage(
                0.40,
                tensileScore,
                yieldScore,
                compressiveScore,
                fractureScore
        );
        int pressureScore = weakestWeightedAverage(
                0.45,
                compressiveScore,
                fractureScore,
                tensileScore,
                compactness
        );
        int pressureResistance = tierBanded(pressureScore, tierMultiplier);
        int structuralStrength = tierBanded(structuralScore, tierMultiplier);
        int maxPressure = safePositiveInt(pressureResistance * 15.0D);

        int magneticTendency = magneticTendency(atomic, metallicityProfile);
        boolean magnetic = magneticTendency >= 55;
        int magneticStrength = magnetic ? tierBanded(magneticTendency, tierMultiplier) : 0;

        int machinability = clampScore(weighted(
                ductilityScore, 0.35,
                100 - hardnessScore, 0.25,
                atomic.atomicStability(), 0.25,
                metallicityProfile, 0.15
        ));
        int formability = clampScore(weighted(
                ductilityScore, 0.50,
                elasticityScore, 0.25,
                100 - brittlenessScore, 0.25
        ));
        int weldability = clampScore(weighted(
                metallicityProfile, 0.30,
                thermalConductivityProfile, 0.20,
                chemicalStabilityScore, 0.25,
                formability, 0.25
        ));
        int castability = clampScore(weighted(
                100 - thermalExpansion, 0.25,
                atomic.atomicStability(), 0.25,
                chemicalStabilityScore, 0.25,
                formability, 0.25
        ));

        MaterialProperties.CrystalStructure crystalStructure = crystalStructure(atomic, crystalPotential);
        int crystalStability = clampScore(weighted(
                crystalPotential, 0.44,
                atomic.atomicStability(), 0.24,
                atomic.bondStrength(), 0.18,
                atomic.directionalBonding(), 0.14
        ));
        int transparency = transparency(metallicityProfile, polarity, crystalStability, atomic);
        int refractiveIndex = 100 + crystalPotential + compactness / 2;
        int luster = clampScore(weighted(
                crystalPotential, 0.32,
                opticalCharacter(atomic), 0.28,
                electronMobility, 0.20,
                100 - metallicityProfile, 0.20
        ));
        int cleavage = clampScore(weighted(
                crystalPotential, 0.42,
                brittlenessScore, 0.38,
                atomic.directionalBonding(), 0.20
        ));
        int crystalHardnessScore = clampScore(weighted(
                hardnessScore, 0.62,
                crystalPotential, 0.24,
                atomic.directionalBonding(), 0.14
        ));
        int crystalHardness = tierBanded(crystalHardnessScore, tierMultiplier);
        MaterialProperties.FractureBehavior fractureBehavior = fractureBehavior(
                ductilityScore,
                brittlenessScore,
                cleavage
        );
        int impurityTolerance = clampScore(weighted(
                atomic.atomicStability(), 0.38,
                ductilityScore, 0.25,
                100 - crystalPotential, 0.20,
                100 - atomic.directionalBonding(), 0.17
        ));
        int opticalPurity = clampScore(weighted(
                transparency, 0.52,
                crystalStability, 0.36,
                opticalCharacter(atomic), 0.12
        ));
        int crystalGrowthDifficulty = clampScore(100 - weighted(
                crystalStability, 0.36,
                atomic.atomicStability(), 0.24,
                castability, 0.16,
                100 - reactivityProfile, 0.16,
                atomic.directionalBonding(), 0.08
        ));
        int crystalFormationTemperature = Math.max(
                -200,
                meltingPoint - Math.round(90 + castability * 1.6F)
        );
        int crystalFormationPressure = tierBanded(
                clampScore(weighted(
                        crystalPotential, 0.48,
                        compactness, 0.32,
                        atomic.directionalBonding(), 0.20
                )),
                tierMultiplier
        ) * 5;
        int gemAffinity = gemAffinity(atomic, state);
        int gemQuality = clampScore(weighted(
                crystalStability, 0.23,
                transparency, 0.16,
                opticalPurity, 0.18,
                luster, 0.10,
                crystalHardnessScore, 0.17,
                gemAffinity, 0.16
        ));

        int baseColor = element.colorOverride().orElseGet(() -> baseColor(
                atomicNumber,
                atomic.outerShellElectrons(),
                atomic.outerShell(),
                tierMultiplier,
                metallicityProfile,
                crystalPotential
        ));
        int highlightColor = blendColor(baseColor, 0xFFFFFF, 0.24F);
        int shadowColor = blendColor(baseColor, 0x000000, 0.34F);
        int brightness = brightness(baseColor);
        int emissiveStrength = clampScore(
                Math.max(0, reactivityProfile + atomic.unpairedElectrons() * 6 - 85)
        );

        boolean crystalline = state == MaterialProperties.PhysicalState.SOLID && crystalStability >= 55;
        boolean gemCandidate = !metal
                && state == MaterialProperties.PhysicalState.SOLID
                && gemAffinity >= 70
                && crystalline
                && gemQuality >= 60;
        boolean heatResistant = state == MaterialProperties.PhysicalState.SOLID
                && maxOperatingTemperature >= 600;
        boolean pressureResistant = pressureScore >= 65;

        int castTemperature = castTemperatureC(state, meltingPoint, boilingPoint, castability);
        int radioactivity = radioactivity(atomic);
        int furnaceFuelPotential = furnaceFuelPotential(
                atomic,
                reactivityProfile,
                oxidationTendency,
                electrochemicalPotential,
                chemicalStabilityScore,
                volatility,
                metal,
                metallicityProfile
        );
        boolean furnaceFuel = furnaceFuelPotential >= 50;
        int furnaceBurnTimeTicks = furnaceFuel
                ? safePositiveInt(200.0D + furnaceFuelPotential * 10.0D)
                : 0;

        int frictionScore = clampScore(weighted(
                hardnessScore, 0.25,
                100 - elasticityScore, 0.20,
                100 - wearScore, 0.30,
                100 - fatigueScore, 0.25
        ));
        double frictionCoefficient = roundToOneDecimal(0.1D + frictionScore * 0.014D);

        int pipeCapabilityScore = Math.max(1, weighted(
                pressureResistance, 0.45,
                structuralStrength, 0.25,
                fractureToughness, 0.15,
                fatigueResistance, 0.15
        ));
        int pipeThroughput = exponentialTransportValue(pipeCapabilityScore, 512.0D);

        int pumpFlowScore = Math.max(1, weighted(
                pressureResistance, 0.35,
                fatigueResistance, 0.25,
                wearResistance, 0.20,
                fractureToughness, 0.20
        ));
        double pumpFlowRate = FluidTransportLimits.tierCapacity(
                tierIndex, MachineTier.ALL.size(), pumpFlowScore,
                4.0D, FluidTransportLimits.MAX_PUMP_RATE);

        int normalizedFriction = clampScore((int) Math.round((frictionCoefficient - 0.1D) / 1.4D * 100.0D));
        int pumpStressScore = clampScore(weighted(
                normalizedFriction, 0.60,
                100 - wearScore, 0.15,
                100 - fatigueScore, 0.15,
                100 - elasticityScore, 0.10
        ));
        int pumpStressImpact = FluidTransportLimits.pumpStress(tierMultiplier, pumpFlowRate, pumpStressScore);

        int maxFluidTemperature = maxFluidTemperatureC(
                state,
                meltingPoint,
                maxOperatingTemperature
        );

        int chemicalTransportScore = Math.max(1, weighted(
                corrosionResistance, 0.50,
                chemicalStability, 0.30,
                oxidationResistance, 0.20
        ));
        double acidBias = clamp(acidity, -50, 50) / 200.0D;
        int maxChemicalRange = Math.max(0, safeInt(chemicalTransportScore * (0.85D + acidBias)));
        int minChemicalRange = -Math.max(0, safeInt(chemicalTransportScore * (0.85D - acidBias)));

        int tankCapabilityScore = Math.max(1, weighted(
                structuralStrength, 0.40,
                pressureResistance, 0.35,
                fractureToughness, 0.15,
                fatigueResistance, 0.10
        ));
        int tankCapacity = (int) Math.floor(FluidTransportLimits.tierCapacity(
                tierIndex, MachineTier.ALL.size(), tankCapabilityScore,
                5500.0D, FluidTransportLimits.MAX_TANK_CAPACITY));

        return new MaterialProperties(
                tierMultiplier,
                atomic.protons(),
                atomic.neutrons(),
                atomic.electrons(),
                atomic.shells(),
                atomic.outerShell(),
                atomic.outerShellCapacity(),
                atomic.outerShellElectrons(),
                atomic.stableValenceTarget(),
                atomic.electronsToStableShell(),
                atomic.electronsFromStableShell(),
                atomic.preferredIonCharge(),
                atomic.unpairedElectrons(),
                atomic.ionizationEnergy(),
                atomic.electronAffinity(),
                atomic.electronDonationTendency(),
                atomic.electronAcceptanceTendency(),
                atomic.bondStrength(),
                tierBanded(atomic.bondStrength(), tierMultiplier) * 10,
                atomic.atomicStability(),
                atomic.effectiveNuclearCharge(),
                atomic.atomicRadius(),
                atomic.valenceSElectrons(),
                atomic.valencePElectrons(),
                atomic.activeDElectrons(),
                atomic.activeFElectrons(),
                family,
                density,
                hardness,
                elasticity,
                tensileStrength,
                yieldStrength,
                fractureToughness,
                compressiveStrength,
                ductility,
                brittleness,
                wearResistance,
                fatigueResistance,
                meltingPoint,
                boilingPoint,
                thermalConductivity,
                specificHeatCapacity,
                thermalExpansion,
                maxOperatingTemperature,
                thermalShockResistance,
                electricalConductivity,
                insulationStrength,
                electricalBehavior,
                electrochemicalPotential,
                chargeStoragePotential,
                batteryPotential,
                corrosionResistance,
                chemicalStability,
                reactivity,
                oxidationResistance,
                acidity,
                pressureResistance,
                structuralStrength,
                maxPressure,
                magneticTendency,
                magneticStrength,
                machinability,
                formability,
                weldability,
                castability,
                crystalStructure,
                crystalStability,
                transparency,
                refractiveIndex,
                luster,
                cleavage,
                crystalHardness,
                fractureBehavior,
                impurityTolerance,
                opticalPurity,
                crystalGrowthDifficulty,
                crystalFormationTemperature,
                crystalFormationPressure,
                gemQuality,
                baseColor,
                highlightColor,
                shadowColor,
                brightness,
                emissiveStrength,
                state,
                metallicity,
                metallicityClass,
                metal,
                magnetic,
                crystalline,
                gemCandidate,
                heatResistant,
                pressureResistant,
                AMBIENT_TEMPERATURE_C,
                castTemperature,
                radioactivity,
                furnaceFuelPotential,
                furnaceFuel,
                furnaceBurnTimeTicks,
                frictionCoefficient,
                pipeCapabilityScore,
                pipeThroughput,
                pumpFlowScore,
                pumpFlowRate,
                pumpStressImpact,
                maxFluidTemperature,
                minChemicalRange,
                maxChemicalRange,
                tankCapabilityScore,
                tankCapacity,
                Set.of()
        );
    }

    /**
     * Converts generated electrical conductivity into the amp capacity of a bare 1x wire.
     *
     * Voltage tier and amp capacity are intentionally separate. The material tier sets the
     * overall amp scale, while the material's intrinsic conductivity decides where inside
     * that tier's range the wire lands. This prevents a highly conductive ULV material from
     * reaching high-tier amp values while still preserving meaningful material variation.
     *
     * Approximate bare 1x ranges are:
     * ULV 1-4 A, LV 2-8 A, MV 4-16 A, HV 8-32 A, EV 16-64 A, IV 32-128 A.
     */
    public static int wireBaseAmps(MaterialProperties properties) {
        if (!properties.electricallyConductive() || properties.electricalConductivity() <= 0) {
            return 0;
        }

        int tierIndex = Math.max(0, properties.tierMultiplier() - 1);
        int tierScale = 1 << Math.min(tierIndex, 20);

        double tierContribution = (double) tierIndex * DEFAULT_TIER_BAND_SIZE;
        double intrinsicConductivity = properties.electricalConductivity() - tierContribution;
        double intrinsicScore = (intrinsicConductivity - 5.0D) / DEFAULT_INTRINSIC_SPREAD;
        double normalizedConductivity = Math.max(0.0D, Math.min(1.0D, intrinsicScore / 100.0D));

        double materialMultiplier = 1.0D + 3.0D * normalizedConductivity * normalizedConductivity;
        return Math.max(1, safeInt(tierScale * materialMultiplier));
    }

    public static int temperatureFor(MaterialProperties properties, MaterialPart part) {
        // Manual-forging hot solids use exactly half the material melting point. Integer division
        // deliberately discards any .5 remainder, matching the anvil arithmetic rule.
        if (part != null && part.isHotForgePart()) return Math.max(0, properties.meltingPoint() / 2);
        if (part == MaterialPart.MOLTEN_FLUID) {
            return properties.castTemperature();
        }
        if (part.name().startsWith("CAST_") || part.name().startsWith("HOT_CAST_")) {
            return properties.castTemperature();
        }
        return properties.ambientTemperature();
    }

    public static int furnaceBurnTimeTicks(MaterialProperties properties, double itemMultiplier) {
        if (!properties.furnaceFuel() || itemMultiplier <= 0) {
            return 0;
        }
        return Math.max(1, safeInt(properties.furnaceBurnTimeTicks() * itemMultiplier));
    }

    private static int tierIndex(MachineTier tier) {
        int index = MachineTier.ALL.indexOf(tier);
        if (index < 0) {
            throw new IllegalArgumentException(
                    "Material tier must be present in MachineTier.ALL: " + tier.id()
            );
        }
        return index;
    }

    private static int metallicityProfile(AtomicState atomic) {
        int base = switch (atomic.family()) {
            case COINAGE_LIKE -> 98;
            case TRANSITION_MIDDLE -> 95;
            case TRANSITION_EARLY -> 92;
            case F_BLOCK -> 91;
            case ALKALI_LIKE -> 90;
            case TRANSITION_LATE -> 89;
            case CLOSED_D_SHELL -> 86;
            case ALKALINE_EARTH_LIKE -> 84;
            case EXTENDED_BLOCK -> 82;
            case NETWORK_CRYSTAL_P1 -> 38;
            case NETWORK_CRYSTAL_P2 -> 26;
            case PNICTOGEN_LIKE -> 20;
            case CHALCOGEN_LIKE -> 13;
            case HALOGEN_LIKE -> 8;
            case NOBLE_GAS_LIKE -> 3;
        };
        return clampScore(base + Math.max(0, atomic.outerShell() - 6));
    }

    private static int reactivityProfile(AtomicState atomic) {
        if (atomic.family() == MaterialProperties.ElectronicFamily.NOBLE_GAS_LIKE) {
            return 3;
        }
        int raw = Math.max(
                atomic.electronDonationTendency(),
                atomic.electronAcceptanceTendency()
        );
        int stabilityPenalty = atomic.atomicStability() / 5;
        int familyBonus = switch (atomic.family()) {
            case ALKALI_LIKE, HALOGEN_LIKE -> 18;
            case CHALCOGEN_LIKE -> 10;
            case ALKALINE_EARTH_LIKE, PNICTOGEN_LIKE -> 5;
            case COINAGE_LIKE, CLOSED_D_SHELL -> -12;
            default -> 0;
        };
        return clampScore(raw + familyBonus - stabilityPenalty);
    }

    private static int bandMobility(AtomicState atomic) {
        return switch (atomic.family()) {
            case COINAGE_LIKE -> 100;
            case TRANSITION_LATE -> 94;
            case TRANSITION_MIDDLE -> 90;
            case TRANSITION_EARLY -> 84;
            case ALKALI_LIKE -> 86;
            case CLOSED_D_SHELL -> 78;
            case F_BLOCK, EXTENDED_BLOCK -> 76;
            case ALKALINE_EARTH_LIKE -> 72;
            case NETWORK_CRYSTAL_P1 -> 48;
            case NETWORK_CRYSTAL_P2 -> 28;
            case PNICTOGEN_LIKE -> 20;
            case CHALCOGEN_LIKE -> 14;
            case HALOGEN_LIKE -> 8;
            case NOBLE_GAS_LIKE -> 2;
        };
    }

    private static int compactness(AtomicState atomic) {
        return clampScore(Math.round(
                22.0F
                        + atomic.effectiveNuclearCharge() * 0.50F
                        + (100 - atomic.atomicRadius()) * 0.30F
                        + atomic.atomicStability() * 0.12F
        ));
    }

    private static int crystalPotential(AtomicState atomic, int compactness) {
        return clampScore(weighted(
                atomic.bondStrength(), 0.30,
                atomic.atomicStability(), 0.22,
                compactness, 0.18,
                atomic.directionalBonding(), 0.30
        ));
    }

    private static int cohesionProfile(
            AtomicState atomic,
            int metallicity,
            int crystalPotential
    ) {
        int familyBase = switch (atomic.family()) {
            case NOBLE_GAS_LIKE -> 5;
            case HALOGEN_LIKE -> 10;
            case CHALCOGEN_LIKE -> 13;
            case PNICTOGEN_LIKE -> atomic.outerShell() <= 4 ? 15 : 33;
            case CLOSED_D_SHELL -> atomic.outerShell() <= 6 ? 34 : 52;
            case ALKALI_LIKE -> 62;
            case NETWORK_CRYSTAL_P1 -> 84;
            case NETWORK_CRYSTAL_P2 -> 94;
            case ALKALINE_EARTH_LIKE -> 68;
            case TRANSITION_EARLY -> 80;
            case TRANSITION_MIDDLE -> 88;
            case TRANSITION_LATE -> 78;
            case COINAGE_LIKE -> 70;
            case F_BLOCK -> 73;
            case EXTENDED_BLOCK -> 72;
        };
        int shellPolarizability = Math.max(0, atomic.outerShell() - 6) * 4;
        return clampScore(Math.round(
                familyBase
                        + metallicity * 0.06F
                        + crystalPotential * 0.08F
                        + shellPolarizability
        ));
    }

    private static int volatility(
            AtomicState atomic,
            int cohesion,
            int crystalPotential,
            int reactivity
    ) {
        return clampScore(Math.round(
                103.0F
                        - cohesion * 0.72F
                        - atomic.bondStrength() * 0.18F
                        - crystalPotential * 0.10F
                        + reactivity * 0.12F
        ));
    }

    private static MaterialProperties.PhysicalState intrinsicState(AtomicState atomic) {
        return switch (atomic.family()) {
            case NOBLE_GAS_LIKE -> MaterialProperties.PhysicalState.GAS;
            case ALKALI_LIKE -> MaterialProperties.PhysicalState.SOLID;
            case CHALCOGEN_LIKE, HALOGEN_LIKE -> atomic.outerShell() <= 6
                    ? MaterialProperties.PhysicalState.GAS
                    : MaterialProperties.PhysicalState.LIQUID;
            case PNICTOGEN_LIKE -> {
                if (atomic.outerShell() <= 4) {
                    yield MaterialProperties.PhysicalState.GAS;
                }
                if (atomic.outerShell() <= 6) {
                    yield MaterialProperties.PhysicalState.LIQUID;
                }
                yield MaterialProperties.PhysicalState.SOLID;
            }
            case CLOSED_D_SHELL -> atomic.outerShell() >= 4 && atomic.outerShell() <= 6
                    ? MaterialProperties.PhysicalState.LIQUID
                    : MaterialProperties.PhysicalState.SOLID;
            default -> MaterialProperties.PhysicalState.SOLID;
        };
    }

    private static int meltingPointC(
            AtomicState atomic,
            int compactness,
            int crystalPotential,
            int cohesion,
            int volatility,
            MaterialProperties.PhysicalState targetState,
            int tierIndex,
            boolean tierBandedMetal
    ) {
        int raw = safeInt(
                -210.0D
                        + cohesion * 18.0D
                        + atomic.bondStrength() * 5.0D
                        + compactness * 2.5D
                        + crystalPotential * 3.0D
                        - volatility * 5.0D
        );
        if (targetState == MaterialProperties.PhysicalState.SOLID && tierBandedMetal) {
            int chemistryScore = clamp(weighted(
                    cohesion, 0.35D,
                    atomic.bondStrength(), 0.20D,
                    compactness, 0.15D,
                    crystalPotential, 0.15D,
                    100 - volatility, 0.15D
            ), 0, 100);
            int base = 1 + Math.round(chemistryScore * 499.0F / 100.0F);
            return tierBandedMetalMeltingPoint(base, tierIndex);
        }
        return switch (targetState) {
            case GAS -> clamp(Math.min(raw, -35), -260, -35);
            case LIQUID -> clamp(Math.min(raw, 8), -180, 8);
            case SOLID -> clamp(Math.max(raw, 60), 60, 6000);
        };
    }

    public static int tierBandedMetalMeltingPoint(int baseMeltingPoint, int tierIndex) {
        int base = clamp(baseMeltingPoint, 1, METAL_MELTING_TIER_BAND_C);
        int safeTier = Math.max(0, tierIndex);
        return Math.addExact(base, Math.multiplyExact(safeTier, METAL_MELTING_TIER_BAND_C));
    }

    public static int normalizeMetalMeltingBase(double intrinsicMeltingPoint) {
        double positive = Math.max(0.0D, intrinsicMeltingPoint);
        double normalized = 1.0D - Math.exp(-positive / 900.0D);
        return clamp(1 + safeInt(normalized * 499.0D), 1, METAL_MELTING_TIER_BAND_C);
    }

    private static int boilingPointC(
            int meltingPoint,
            AtomicState atomic,
            int cohesion,
            int volatility,
            MaterialProperties.PhysicalState targetState
    ) {
        int gap = Math.max(20, Math.round(
                30.0F
                        + cohesion * 6.0F
                        + atomic.bondStrength() * 2.0F
                        + (100 - volatility) * 2.5F
        ));
        int raw = safeInt((double) meltingPoint + gap);
        return switch (targetState) {
            case GAS -> clamp(Math.min(raw, 15), meltingPoint + 1, 15);
            case LIQUID -> clamp(Math.max(raw, 45), 45, 4000);
            case SOLID -> clamp(Math.max(raw, meltingPoint + 80), meltingPoint + 80, 8000);
        };
    }

    private static int maxOperatingTemperatureC(
            MaterialProperties.PhysicalState state,
            int meltingPoint,
            int boilingPoint,
            int thermalExpansion,
            int atomicStability,
            int tierIndex
    ) {
        int safetyMargin = Math.max(
                10,
                45 + thermalExpansion + (100 - atomicStability) / 2 - tierIndex * 10
        );
        return switch (state) {
            case SOLID -> Math.max(AMBIENT_TEMPERATURE_C, meltingPoint - safetyMargin);
            case LIQUID -> Math.max(AMBIENT_TEMPERATURE_C, boilingPoint - safetyMargin);
            case GAS -> Math.max(AMBIENT_TEMPERATURE_C, boilingPoint + atomicStability + tierIndex * 20);
        };
    }

    private static int castTemperatureC(
            MaterialProperties.PhysicalState state,
            int meltingPoint,
            int boilingPoint,
            int castability
    ) {
        return switch (state) {
            case SOLID -> meltingPoint + 30 + Math.round((100 - castability) * 1.2F);
            case LIQUID -> Math.max(AMBIENT_TEMPERATURE_C, meltingPoint + 10);
            case GAS -> Math.max(AMBIENT_TEMPERATURE_C, boilingPoint + 10);
        };
    }

    /**
     * Material-specialization axes. These are deterministic gameplay physics derived from
     * electron configuration; they are intentionally not a lookup table of real elements.
     * Open/half-filled d/f shells favor lattice strength and magnetism, while near-filled d
     * shells favor mobile carriers and chemical nobility. This creates real trade-offs instead
     * of making every material in a tier converge on the same stats.
     */
    private static int carrierProfile(
            AtomicState atomic,
            int familyMobility,
            int metallicity
    ) {
        int dCarrier = dBandConductionProfile(atomic.activeDElectrons());
        int fLocalization = occupancyPercent(atomic.activeFElectrons(), 14);
        int sCarrier = switch (atomic.valenceSElectrons()) {
            case 1 -> 100;
            case 2 -> 68;
            default -> 24;
        };

        int profile = weighted(
                dCarrier, 0.34,
                sCarrier, 0.18,
                familyMobility, 0.16,
                100 - atomic.ionizationEnergy(), 0.12,
                atomic.electronDonationTendency(), 0.10,
                atomic.atomicRadius(), 0.06,
                metallicity, 0.04
        );
        profile -= Math.round(fLocalization * 0.16F);
        profile -= Math.min(24, atomic.unpairedElectrons() * 3);
        return clampScore(profile);
    }

    private static int openShellStrengthProfile(AtomicState atomic) {
        int dStrength = dBandStrengthProfile(atomic.activeDElectrons());
        int fStrength = halfFilledProfile(atomic.activeFElectrons(), 14);
        int directionalFallback = switch (atomic.family()) {
            case NETWORK_CRYSTAL_P1, NETWORK_CRYSTAL_P2 -> atomic.directionalBonding();
            default -> 0;
        };
        return Math.max(directionalFallback, Math.max(dStrength, Math.round(fStrength * 0.85F)));
    }

    private static int latticeStrengthProfile(
            AtomicState atomic,
            int compactness,
            int crystalPotential,
            int openShellStrength
    ) {
        int familyAdjustment = switch (atomic.family()) {
            case TRANSITION_MIDDLE -> 10;
            case TRANSITION_EARLY -> 6;
            case F_BLOCK -> 4;
            case ALKALI_LIKE -> -18;
            case COINAGE_LIKE -> -12;
            case CLOSED_D_SHELL -> -8;
            default -> 0;
        };
        return clampScore(weighted(
                openShellStrength, 0.52,
                atomic.bondStrength(), 0.18,
                compactness, 0.12,
                atomic.atomicStability(), 0.08,
                crystalPotential, 0.06,
                atomic.directionalBonding(), 0.04
        ) + familyAdjustment);
    }

    private static int chemicalNobilityProfile(AtomicState atomic, int reactivity) {
        int dClosure = dBandNobilityProfile(atomic.activeDElectrons());
        int fClosurePenalty = occupancyPercent(atomic.activeFElectrons(), 14);
        int familyAdjustment = switch (atomic.family()) {
            case COINAGE_LIKE -> 22;
            case CLOSED_D_SHELL -> 16;
            case TRANSITION_LATE -> 8;
            case ALKALI_LIKE -> -20;
            case ALKALINE_EARTH_LIKE -> -8;
            case F_BLOCK -> -6;
            default -> 0;
        };
        return clampScore(weighted(
                dClosure, 0.32,
                atomic.atomicStability(), 0.20,
                100 - reactivity, 0.16,
                atomic.ionizationEnergy(), 0.12,
                100 - atomic.electronDonationTendency(), 0.10,
                100 - fClosurePenalty, 0.10
        ) + familyAdjustment);
    }

    private static int ductilityFamilyAdjustment(MaterialProperties.ElectronicFamily family) {
        return switch (family) {
            case COINAGE_LIKE, CLOSED_D_SHELL -> 14;
            case TRANSITION_LATE -> 8;
            case ALKALI_LIKE -> 6;
            case TRANSITION_MIDDLE -> -5;
            case NETWORK_CRYSTAL_P1, NETWORK_CRYSTAL_P2 -> -14;
            default -> 0;
        };
    }

    private static int dBandConductionProfile(int dElectrons) {
        if (dElectrons <= 0) {
            return 58;
        }
        return switch (Math.min(10, dElectrons)) {
            case 1 -> 28;
            case 2 -> 34;
            case 3 -> 30;
            case 4 -> 22;
            case 5 -> 15;
            case 6 -> 46;
            case 7 -> 76;
            case 8 -> 90;
            case 9 -> 98;
            default -> 100;
        };
    }

    private static int dBandStrengthProfile(int dElectrons) {
        if (dElectrons <= 0) {
            return 0;
        }
        return switch (Math.min(10, dElectrons)) {
            case 1 -> 24;
            case 2 -> 42;
            case 3 -> 64;
            case 4 -> 84;
            case 5 -> 100;
            case 6 -> 72;
            case 7 -> 48;
            case 8 -> 30;
            case 9 -> 15;
            default -> 5;
        };
    }

    private static int dBandNobilityProfile(int dElectrons) {
        if (dElectrons <= 0) {
            return 30;
        }
        return switch (Math.min(10, dElectrons)) {
            case 1 -> 12;
            case 2 -> 16;
            case 3 -> 20;
            case 4 -> 22;
            case 5 -> 25;
            case 6 -> 42;
            case 7 -> 64;
            case 8 -> 82;
            case 9 -> 95;
            default -> 100;
        };
    }

    private static int halfFilledProfile(int occupancy, int capacity) {
        if (occupancy <= 0 || capacity <= 0) {
            return 0;
        }
        double half = capacity / 2.0D;
        double distance = Math.abs(occupancy - half) / half;
        return clampScore(safeInt(100.0D * (1.0D - distance)));
    }

    private static int occupancyPercent(int occupancy, int capacity) {
        if (occupancy <= 0 || capacity <= 0) {
            return 0;
        }
        return clampScore(safeInt(occupancy * 100.0D / capacity));
    }

    private static int electricalConductivityProfile(
            AtomicState atomic,
            int electronMobility,
            int metallicity,
            int polarity
    ) {
        int carrier = carrierProfile(atomic, bandMobility(atomic), metallicity);
        int openShellPenalty = Math.min(28, atomic.unpairedElectrons() * 3 + atomic.activeFElectrons());
        int familyAdjustment = switch (atomic.family()) {
            case COINAGE_LIKE -> 16;
            case CLOSED_D_SHELL -> 10;
            case TRANSITION_LATE -> 6;
            case TRANSITION_MIDDLE -> 0;
            case TRANSITION_EARLY -> -5;
            case F_BLOCK -> -8;
            case ALKALI_LIKE -> 5;
            case NETWORK_CRYSTAL_P2 -> -8;
            case NOBLE_GAS_LIKE, HALOGEN_LIKE -> -15;
            default -> 0;
        };
        return clampScore(weighted(
                carrier, 0.54,
                electronMobility, 0.18,
                100 - atomic.ionizationEnergy(), 0.10,
                atomic.electronDonationTendency(), 0.08,
                metallicity, 0.06,
                100 - polarity, 0.04
        ) + familyAdjustment - openShellPenalty);
    }

    private static int electrochemicalPotential(
            AtomicState atomic,
            int reactivity,
            int metallicity
    ) {
        int massEfficiency = clampScore((int) Math.round(
                108.0D - Math.log1p(atomic.protons()) * 12.0D
        ));
        int ionSimplicity = clampScore(100 - Math.min(100, Math.abs(atomic.preferredIonCharge()) - 1) * 22);
        int donorBias = atomic.electronDonationTendency();
        int raw = weighted(
                donorBias, 0.38,
                massEfficiency, 0.22,
                ionSimplicity, 0.16,
                reactivity, 0.12,
                metallicity, 0.12
        );
        if (atomic.family() == MaterialProperties.ElectronicFamily.ALKALI_LIKE && atomic.outerShell() > 1) {
            raw += 14;
        }
        if (atomic.family() == MaterialProperties.ElectronicFamily.COINAGE_LIKE) {
            raw -= 12;
        }
        return clampScore(raw);
    }

    private static int chargeStoragePotential(
            AtomicState atomic,
            int insulationStrength,
            int crystalPotential,
            int reactivity
    ) {
        int crystalStorageBonus = switch (atomic.family()) {
            case NETWORK_CRYSTAL_P1 -> 18;
            case NETWORK_CRYSTAL_P2 -> 24;
            case PNICTOGEN_LIKE -> 8;
            default -> 0;
        };
        return clampScore(weighted(
                insulationStrength, 0.38,
                crystalPotential, 0.25,
                atomic.atomicStability(), 0.20,
                100 - reactivity, 0.17
        ) + crystalStorageBonus);
    }

    private static int magneticTendency(AtomicState atomic, int metallicity) {
        int unpairedScore = Math.min(100, atomic.unpairedElectrons() * 14);
        int transitionBonus = switch (atomic.family()) {
            case TRANSITION_MIDDLE -> 15;
            case F_BLOCK -> 12;
            case TRANSITION_EARLY, TRANSITION_LATE -> 8;
            default -> 0;
        };
        return clampScore(weighted(
                unpairedScore, 0.58,
                metallicity, 0.25,
                100 - atomic.atomicStability(), 0.17
        ) + transitionBonus);
    }

    private static int transparency(
            int metallicity,
            int polarity,
            int crystalStability,
            AtomicState atomic
    ) {
        int familyBonus = switch (atomic.family()) {
            case NETWORK_CRYSTAL_P1, NETWORK_CRYSTAL_P2 -> 15;
            case NOBLE_GAS_LIKE -> 20;
            default -> 0;
        };
        return clampScore(weighted(
                100 - metallicity, 0.42,
                crystalStability, 0.34,
                100 - polarity, 0.24
        ) + familyBonus);
    }

    private static int opticalCharacter(AtomicState atomic) {
        return switch (atomic.family()) {
            case NETWORK_CRYSTAL_P2 -> 95;
            case NETWORK_CRYSTAL_P1 -> 88;
            case PNICTOGEN_LIKE -> 65;
            case CHALCOGEN_LIKE -> 58;
            case HALOGEN_LIKE -> 52;
            case NOBLE_GAS_LIKE -> 70;
            case COINAGE_LIKE -> 72;
            default -> 45;
        };
    }

    private static int gemAffinity(
            AtomicState atomic,
            MaterialProperties.PhysicalState state
    ) {
        if (state != MaterialProperties.PhysicalState.SOLID) {
            return 0;
        }
        return switch (atomic.family()) {
            case NETWORK_CRYSTAL_P1 -> 84;
            case NETWORK_CRYSTAL_P2 -> 96;
            case PNICTOGEN_LIKE -> 48;
            default -> 12;
        };
    }

    private static MaterialProperties.CrystalStructure crystalStructure(
            AtomicState atomic,
            int crystalPotential
    ) {
        if (crystalPotential < 35) {
            return MaterialProperties.CrystalStructure.AMORPHOUS;
        }
        return switch (atomic.family()) {
            case NETWORK_CRYSTAL_P1 -> MaterialProperties.CrystalStructure.HEXAGONAL;
            case NETWORK_CRYSTAL_P2 -> MaterialProperties.CrystalStructure.PRISMATIC;
            case F_BLOCK -> MaterialProperties.CrystalStructure.HEXAGONAL;
            case COINAGE_LIKE, CLOSED_D_SHELL -> MaterialProperties.CrystalStructure.CUBIC;
            case PNICTOGEN_LIKE, CHALCOGEN_LIKE -> MaterialProperties.CrystalStructure.LAYERED;
            default -> switch (Math.floorMod(
                    atomic.outerShellElectrons()
                            + atomic.outerShell()
                            + atomic.unpairedElectrons(),
                    5
            )) {
                case 0 -> MaterialProperties.CrystalStructure.CUBIC;
                case 1 -> MaterialProperties.CrystalStructure.HEXAGONAL;
                case 2 -> MaterialProperties.CrystalStructure.LAYERED;
                case 3 -> MaterialProperties.CrystalStructure.PRISMATIC;
                default -> MaterialProperties.CrystalStructure.IRREGULAR;
            };
        };
    }

    private static MaterialProperties.FractureBehavior fractureBehavior(
            int ductility,
            int brittleness,
            int cleavage
    ) {
        if (ductility > brittleness + 20) {
            return MaterialProperties.FractureBehavior.DUCTILE;
        }
        if (cleavage > 60) {
            return MaterialProperties.FractureBehavior.CLEAVED;
        }
        if (brittleness > 75) {
            return MaterialProperties.FractureBehavior.SHATTERING;
        }
        if (brittleness > ductility) {
            return MaterialProperties.FractureBehavior.CONCHOIDAL;
        }
        return MaterialProperties.FractureBehavior.GRANULAR;
    }

    private static int furnaceFuelPotential(
            AtomicState atomic,
            int reactivity,
            int oxidationTendency,
            int electrochemicalPotential,
            int chemicalStability,
            int volatility,
            boolean metal,
            int metallicity
    ) {
        if (atomic.family() == MaterialProperties.ElectronicFamily.NOBLE_GAS_LIKE || metal) {
            return 0;
        }
        int stabilityWindow = clampScore((int) Math.round(
                100.0D - Math.abs(chemicalStability - 55.0D) * 1.6D
        ));
        int nonMetal = clampScore(100 - metallicity);
        return clampScore(weighted(
                atomic.electronDonationTendency(), 0.22,
                electrochemicalPotential, 0.20,
                oxidationTendency, 0.18,
                reactivity, 0.12,
                volatility, 0.10,
                nonMetal, 0.12,
                stabilityWindow, 0.06
        ));
    }

    /**
     * A bounded nuclear-stability proxy. Heavy nuclei become harder to stabilize, but a
     * deterministic shell wave creates recurring stability islands instead of making every
     * element above one arbitrary Z monotonically worse.
     */
    private static int radioactivity(AtomicState atomic) {
        double heavyPressure = 100.0D * (1.0D - Math.exp(-Math.max(0, atomic.protons() - 55) / 150.0D));
        double shellWave = (Math.cos(Math.sqrt(atomic.protons()) * Math.PI) + 1.0D) * 0.5D;
        double islandBonus = shellWave * 38.0D;
        double electronicInstability = (100 - atomic.atomicStability()) * 0.22D;
        double oddPenalty = (atomic.protons() & 1) == 0 ? 0.0D : 5.0D;
        return clampScore((int) Math.round(
                heavyPressure + electronicInstability + oddPenalty - islandBonus
        ));
    }

    private static int tierBanded(int normalizedScore, int tierMultiplier) {
        return tierBanded(normalizedScore, tierMultiplier, DEFAULT_TIER_BAND_SIZE);
    }

    /**
     * Applies technological progression without erasing intrinsic material identity.
     * Adjacent tiers deliberately overlap: an exceptional lower-tier material can beat a
     * poor higher-tier material at one property, while higher tiers still shift the average up.
     * The historical DEFAULT_TIER_BAND_SIZE constant now acts as the tier-center step.
     */
    private static int tierBanded(int normalizedScore, int tierMultiplier, int bandSize) {
        int score = clamp(normalizedScore, 0, 100);
        int tierOffset = Math.max(0, tierMultiplier - 1);
        double intrinsic = 5.0D + score * DEFAULT_INTRINSIC_SPREAD;
        double value = (double) tierOffset * bandSize + intrinsic;
        return Math.max(1, safeInt(value));
    }

    private static int weakestWeightedAverage(double weakestWeight, int... values) {
        if (values.length == 0) {
            return 1;
        }
        int minimum = Integer.MAX_VALUE;
        int sum = 0;
        for (int value : values) {
            int score = clampScore(value);
            minimum = Math.min(minimum, score);
            sum += score;
        }
        double average = sum / (double) values.length;
        return clampScore((int) Math.round(
                average * (1.0 - weakestWeight) + minimum * weakestWeight
        ));
    }

    private static int exponentialTransportValue(int score, double valueAt50) {
        double value = exponentialTransportDouble(score, valueAt50);
        if (!Double.isFinite(value) || value >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return Math.max(1, (int) Math.round(value));
    }

    private static double exponentialTransportDouble(int score, double valueAt50) {
        double exponent = Math.min(30.0D, Math.max(-30.0D, (score - 50.0D) / 50.0D));
        return valueAt50 * Math.pow(2.0D, exponent);
    }

    private static double roundToOneDecimal(double value) {
        return Math.round(value * 10.0D) / 10.0D;
    }

    private static int maxFluidTemperatureC(
            MaterialProperties.PhysicalState state,
            int meltingPoint,
            int maxOperatingTemperature
    ) {
        if (state != MaterialProperties.PhysicalState.SOLID) {
            return maxOperatingTemperature;
        }
        int meltingSafetyLimit = (int) Math.floor(meltingPoint * 0.85D);
        return Math.min(maxOperatingTemperature, meltingSafetyLimit);
    }

    private static int weighted(Object... valueWeightPairs) {
        if (valueWeightPairs.length == 0 || valueWeightPairs.length % 2 != 0) {
            throw new IllegalArgumentException("weighted requires value/weight pairs");
        }
        double sum = 0.0;
        double weights = 0.0;
        for (int i = 0; i < valueWeightPairs.length; i += 2) {
            int value = (Integer) valueWeightPairs[i];
            double weight = (Double) valueWeightPairs[i + 1];
            sum += value * weight;
            weights += weight;
        }
        if (weights <= 0.0) {
            return 0;
        }
        return safeInt(sum / weights);
    }

    private static int baseColor(
            int atomicNumber,
            int outerShellElectrons,
            int outerShell,
            int tierMultiplier,
            int metallicity,
            int crystalPotential
    ) {
        // One hundred atomic numbers are distributed over 25 hue families and four
        // deliberately different tone profiles. The coprime multiplier permutes the
        // palette so neighbouring elements do not become minor variations of each other.
        int paletteIndex = (int) Math.floorMod(((long) atomicNumber - 1L) * 37L, 100L);
        int hueSlot = paletteIndex % 25;
        int toneProfile = paletteIndex / 25;

        long seed = mixColorSeed(
                (long) atomicNumber * 0x9E3779B97F4A7C15L
                        ^ (long) outerShellElectrons * 0xBF58476D1CE4E5B9L
                        ^ (long) outerShell * 0x94D049BB133111EBL
        );
        float hueJitter = (unitByte(seed) - 0.5F) / 75.0F;
        float hue = hueSlot / 25.0F + hueJitter;

        float metallicityAdjustment = (50 - metallicity) / 500.0F;
        float saturationJitter = (unitByte(seed >>> 8) - 0.5F) * 0.08F;
        float saturation = clamp(
                COLOR_SATURATION_PROFILES[toneProfile] + metallicityAdjustment + saturationJitter,
                0.34F,
                0.94F
        );

        float crystalAdjustment = (crystalPotential - 50) / 1000.0F;
        float tierAdjustment = (tierMultiplier - 1) * 0.008F;
        float brightnessJitter = (unitByte(seed >>> 16) - 0.5F) * 0.06F;
        float brightness = clamp(
                COLOR_BRIGHTNESS_PROFILES[toneProfile] + crystalAdjustment + tierAdjustment + brightnessJitter,
                0.48F,
                0.95F
        );
        return hsbToRgb(hue, saturation, brightness);
    }

    private static long mixColorSeed(long value) {
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        return value ^ value >>> 31;
    }

    private static float unitByte(long value) {
        return (value & 0xFFL) / 255.0F;
    }

    private static int hsbToRgb(float hue, float saturation, float brightness) {
        float h = (hue - (float) Math.floor(hue)) * 6.0F;
        int sector = (int) Math.floor(h);
        float fraction = h - sector;
        float p = brightness * (1.0F - saturation);
        float q = brightness * (1.0F - saturation * fraction);
        float t = brightness * (1.0F - saturation * (1.0F - fraction));

        float r;
        float g;
        float b;
        switch (sector) {
            case 0 -> { r = brightness; g = t; b = p; }
            case 1 -> { r = q; g = brightness; b = p; }
            case 2 -> { r = p; g = brightness; b = t; }
            case 3 -> { r = p; g = q; b = brightness; }
            case 4 -> { r = t; g = p; b = brightness; }
            default -> { r = brightness; g = p; b = q; }
        }

        int red = clamp(Math.round(r * 255.0F), 0, 255);
        int green = clamp(Math.round(g * 255.0F), 0, 255);
        int blue = clamp(Math.round(b * 255.0F), 0, 255);
        return red << 16 | green << 8 | blue;
    }

    private static int blendColor(int color, int target, float amount) {
        float blend = clamp(amount, 0.0F, 1.0F);
        int red = Math.round(((color >> 16) & 0xFF) * (1.0F - blend)
                + ((target >> 16) & 0xFF) * blend);
        int green = Math.round(((color >> 8) & 0xFF) * (1.0F - blend)
                + ((target >> 8) & 0xFF) * blend);
        int blue = Math.round((color & 0xFF) * (1.0F - blend)
                + (target & 0xFF) * blend);
        return red << 16 | green << 8 | blue;
    }

    private static int brightness(int color) {
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        return Math.round((red * 0.299F + green * 0.587F + blue * 0.114F) / 255.0F * 100.0F);
    }

    public static MaterialProperties.PhysicalState stateForTemperatures(int meltingPoint, int boilingPoint) {
        if (boilingPoint <= AMBIENT_TEMPERATURE_C) {
            return MaterialProperties.PhysicalState.GAS;
        }
        if (meltingPoint <= AMBIENT_TEMPERATURE_C) {
            return MaterialProperties.PhysicalState.LIQUID;
        }
        return MaterialProperties.PhysicalState.SOLID;
    }

    private static MaterialProperties.MetallicityClass metallicityClass(int metallicity) {
        if (metallicity >= 80) {
            return MaterialProperties.MetallicityClass.STRONGLY_METALLIC;
        }
        if (metallicity >= 55) {
            return MaterialProperties.MetallicityClass.METALLIC;
        }
        if (metallicity >= 35) {
            return MaterialProperties.MetallicityClass.SEMI_METALLIC;
        }
        return MaterialProperties.MetallicityClass.NON_METALLIC;
    }

    private static int clampScore(int value) {
        return clamp(value, 0, 100);
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static int safeInt(double value) {
        if (Double.isNaN(value)) {
            return 0;
        }
        if (value >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value <= Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) Math.round(value);
    }

    private static int safePositiveInt(double value) {
        return Math.max(1, safeInt(value));
    }

}
