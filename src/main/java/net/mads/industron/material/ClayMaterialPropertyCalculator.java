package net.mads.industron.material;

import net.mads.industron.machine.MachineTier;
import net.mads.industron.material.chemistry.ChemistryPhase;
import net.mads.industron.material.chemistry.MaterialClassification;

import java.util.List;
import java.util.Set;

/**
 * Dedicated gameplay properties for clay/ceramic materials.
 *
 * <p>Clay composition is still kept on {@link IndustrialMaterial} for geology, primitive recipes
 * and formula display, but it must not run through {@link CompoundMaterialPropertyCalculator}.
 * Ceramic thermal limits are progression values owned by the clay tier instead of emergent compound
 * chemistry values.</p>
 */
public final class ClayMaterialPropertyCalculator {
    public record Result(
            MaterialProperties properties,
            MachineTier tier,
            ChemistryPhase phase,
            Set<MaterialClassification> classifications
    ) {
        public Result {
            classifications = Set.copyOf(classifications);
        }
    }

    private ClayMaterialPropertyCalculator() {
    }

    public static Result calculate(
            int explicitColor,
            List<MaterialComponent> components,
            MachineTier tier
    ) {
        if (components == null || components.isEmpty()) {
            throw new IllegalArgumentException("Clay material requires at least one .contains(...) component");
        }
        if (tier == null || tier == MachineTier.NONE) {
            throw new IllegalArgumentException("Clay material requires an explicit gameplay tier");
        }

        int tierIndex = MachineTier.ALL.indexOf(tier);
        if (tierIndex < 0) {
            throw new IllegalArgumentException("Clay material uses an unknown machine tier: " + tier.id());
        }

        int maxOperatingTemperature = maxOperatingTemperature(tierIndex);
        int boilingPoint = maxOperatingTemperature + 1250;
        int baseColor = explicitColor >= 0 ? explicitColor & 0xFFFFFF : blendColor(components);
        int highlightColor = adjustColor(baseColor, 1.16D);
        int shadowColor = adjustColor(baseColor, 0.72D);
        int tierMultiplier = tierIndex + 1;

        // These are ceramic gameplay defaults, not chemistry-derived compound values. Only the
        // thermal progression and visual color need to vary between clay definitions today.
        int brickHardness = Math.min(100, 48 + tierIndex * 7);
        int compressiveStrength = Math.min(100, 62 + tierIndex * 7);
        int wearResistance = Math.min(100, 58 + tierIndex * 6);
        int thermalShockResistance = Math.min(100, 50 + tierIndex * 8);
        int insulationStrength = Math.min(100, 72 + tierIndex * 6);
        int chemicalStability = Math.min(100, 78 + tierIndex * 4);
        int oxidationResistance = Math.min(100, 82 + tierIndex * 4);
        int pressureResistance = Math.min(100, 58 + tierIndex * 7);
        int structuralStrength = Math.min(100, 55 + tierIndex * 7);
        int maxPressure = 100 + tierIndex * 100;

        MaterialProperties properties = new MaterialProperties(
                tierMultiplier,
                0,
                0,
                0,
                List.of(),
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                70,
                700,
                85,
                0,
                0,
                0,
                0,
                0,
                0,
                MaterialProperties.ElectronicFamily.NETWORK_CRYSTAL_P2,
                Integer.MIN_VALUE,
                Integer.MIN_VALUE,
                10,
                30 + tierIndex * 4,
                20 + tierIndex * 4,
                35 + tierIndex * 4,
                compressiveStrength,
                5,
                85,
                wearResistance,
                40 + tierIndex * 4,
                Integer.MIN_VALUE,
                boilingPoint,
                15,
                80,
                20,
                maxOperatingTemperature,
                thermalShockResistance,
                Integer.MIN_VALUE,
                insulationStrength,
                null,
                0,
                0,
                0,
                Integer.MIN_VALUE,
                chemicalStability,
                Integer.MIN_VALUE,
                oxidationResistance,
                0,
                pressureResistance,
                structuralStrength,
                maxPressure,
                0,
                0,
                20,
                30,
                0,
                20,
                MaterialProperties.CrystalStructure.IRREGULAR,
                75,
                0,
                100,
                20,
                30,
                brickHardness,
                MaterialProperties.FractureBehavior.SHATTERING,
                55,
                20,
                65,
                Math.max(100, maxOperatingTemperature / 2),
                50 + tierIndex * 25,
                0,
                baseColor,
                highlightColor,
                shadowColor,
                brightness(baseColor),
                0,
                MaterialProperties.PhysicalState.SOLID,
                0,
                MaterialProperties.MetallicityClass.NON_METALLIC,
                false,
                false,
                true,
                false,
                maxOperatingTemperature >= 700,
                true,
                20,
                maxOperatingTemperature,
                0,
                0,
                false,
                0,
                0.65D,
                1,
                0,
                1,
                0.0D,
                0,
                maxOperatingTemperature,
                -chemicalStability,
                chemicalStability,
                1,
                0,
                Set.of(
                        "density",
                        "hardness",
                        "meltingPoint",
                        "electricalConductivity",
                        "electricalBehavior",
                        "corrosionResistance",
                        "reactivity"
                )
        );

        return new Result(
                properties,
                tier,
                ChemistryPhase.SOLID,
                Set.of(MaterialClassification.CERAMIC)
        );
    }

    /**
     * Clay foundry progression. ULV preserves the existing 578 C limit; later tiers advance in
     * 500 C steps (LV=1000, MV=1500, HV=2000, EV=2500, ...).
     */
    private static int maxOperatingTemperature(int tierIndex) {
        return tierIndex == 0 ? 578 : (tierIndex + 1) * 500;
    }

    private static int blendColor(List<MaterialComponent> components) {
        long totalWeight = 0L;
        long red = 0L;
        long green = 0L;
        long blue = 0L;
        for (MaterialComponent component : components) {
            int weight = Math.max(1, component.amount());
            int color = component.substance().color() & 0xFFFFFF;
            red += (long) ((color >> 16) & 0xFF) * weight;
            green += (long) ((color >> 8) & 0xFF) * weight;
            blue += (long) (color & 0xFF) * weight;
            totalWeight += weight;
        }
        if (totalWeight <= 0L) return 0x808080;
        int r = (int) Math.round((double) red / totalWeight);
        int g = (int) Math.round((double) green / totalWeight);
        int b = (int) Math.round((double) blue / totalWeight);
        return (clampColor(r) << 16) | (clampColor(g) << 8) | clampColor(b);
    }

    private static int adjustColor(int rgb, double multiplier) {
        int r = clampColor((int) Math.round(((rgb >> 16) & 0xFF) * multiplier));
        int g = clampColor((int) Math.round(((rgb >> 8) & 0xFF) * multiplier));
        int b = clampColor((int) Math.round((rgb & 0xFF) * multiplier));
        return (r << 16) | (g << 8) | b;
    }

    private static int brightness(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (int) Math.round((r * 0.2126D + g * 0.7152D + b * 0.0722D) / 255.0D * 100.0D);
    }

    private static int clampColor(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
