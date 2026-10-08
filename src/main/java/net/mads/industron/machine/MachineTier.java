package net.mads.industron.machine;

import java.util.List;

public record MachineTier(String id, String displayName, int color, String casingTexture) {
    private static final float TNT_EXPLOSION_POWER = 4.0F;

    public enum Family {
        NONE,
        ELECTRIC,
        STEAM
    }

    private static final String CASING_VARIANT_2 = "industron:block/structure_sets/casing/casings/variant_2";
    private static final String CASING_VARIANT_9 = "industron:block/structure_sets/casing/casings/variant_9";
    private static final String CASING_VARIANT_21 = "industron:block/structure_sets/casing/casings/variant_21";
    private static final String CASING_VARIANT_35 = "industron:block/structure_sets/casing/casings/variant_35";
    private static final String CASING_VARIANT_41 = "industron:block/structure_sets/casing/casings/variant_41";

    public static final MachineTier NONE = new MachineTier("none", "", 0xFFFFFF, null);

    public static final MachineTier ULV = new MachineTier("ulv", "ULV", 0x6D6D6D, CASING_VARIANT_2);
    public static final MachineTier LV = new MachineTier("lv", "LV", 0x4E8FDC, CASING_VARIANT_2);
    public static final MachineTier MV = new MachineTier("mv", "MV", 0xE0A83A, CASING_VARIANT_2);
    public static final MachineTier HV = new MachineTier("hv", "HV", 0xE85B5B, CASING_VARIANT_9);
    public static final MachineTier EV = new MachineTier("ev", "EV", 0x9B5DE5, CASING_VARIANT_9);
    public static final MachineTier IV = new MachineTier("iv", "IV", 0x5DD9C1, CASING_VARIANT_9);
    public static final MachineTier LUV = new MachineTier("luv", "LuV", 0xFF8C42, CASING_VARIANT_21);
    public static final MachineTier ZPM = new MachineTier("zpm", "ZPM", 0xD84FD6, CASING_VARIANT_21);
    public static final MachineTier UV = new MachineTier("uv", "UV", 0x5A4BFF, CASING_VARIANT_21);
    public static final MachineTier UHV = new MachineTier("uhv", "UHV", 0x37D6FF, CASING_VARIANT_35);
    public static final MachineTier UEV = new MachineTier("uev", "UEV", 0xF2F7FF, CASING_VARIANT_35);
    public static final MachineTier UIV = new MachineTier("uiv", "UIV", 0xB7FF4A, CASING_VARIANT_35);
    public static final MachineTier UXV = new MachineTier("uxv", "UXV", 0xFF3D8B, CASING_VARIANT_41);
    public static final MachineTier OPV = new MachineTier("opv", "OpV", 0x2B2B2B, CASING_VARIANT_41);
    public static final MachineTier MAX = new MachineTier("max", "MAX", 0xFFFFFF, CASING_VARIANT_41);


    public MachineTier(String id, String displayName, int color) {
        this(id, displayName, color, null);
    }

    /** Steam pressure bands map to explicit recipe progression tiers. */
    public static final MachineTier STEAM =
            new MachineTier("steam", "Low Pressure Steam", 0xB08D57);
    public static final MachineTier STEAM_LV =
            new MachineTier("steam_lv", "High Pressure Steam", 0x8FA8BE, CASING_VARIANT_2);

    public static final List<MachineTier> ALL = List.of(
            ULV,
            LV,
            MV,
            HV,
            EV,
            IV,
            LUV,
            ZPM,
            UV,
            UHV,
            UEV,
            UIV,
            UXV,
            OPV,
            MAX
    );

    public static final List<MachineTier> ELECTRIC_TIERS = ALL;

    public static final List<MachineTier> STEAM_SINGLEBLOCK_TIERS = List.of(STEAM, STEAM_LV);

    public String casingRegistryName() {
        return id + "_machine_casing";
    }

    public String casingDisplayName() {
        return displayName + " Machine Casing";
    }

    public String singleBlockMachineCasingSideTexture() {
        if (this == STEAM) {
            return "industron:block/casings/casing/steam_casing_side";
        }
        if (casingTexture != null) return casingTexture;
        return "industron:block/casings/universal_textures/casing";
    }

    public String singleBlockMachineCasingBottomTexture() {
        if (this == STEAM) {
            return "industron:block/casings/casing/steam_casing_bottom";
        }
        return singleBlockMachineCasingSideTexture();
    }

    public String singleBlockMachineCasingTopTexture() {
        if (this == STEAM) {
            return "industron:block/casings/casing/steam_casing_top";
        }
        return singleBlockMachineCasingSideTexture();
    }

    public Family family() {
        if (this == NONE) {
            return Family.NONE;
        }

        return isSteam()
                ? Family.STEAM
                : Family.ELECTRIC;
    }

    public boolean isSteam() {
        return STEAM_SINGLEBLOCK_TIERS.contains(this);
    }

    public boolean isElectric() {
        return ELECTRIC_TIERS.contains(this);
    }

    public MachineTier recipeTier() {
        return this == STEAM ? ULV : this == STEAM_LV ? LV : this;
    }

    public int steamDurationMultiplier() {
        return (int)(isSteam() ? ProcessingProfile.STEAM.durationMultiplier() : 1);
    }

    public int steamUsageMultiplier() {
        return this == STEAM_LV ? 2 : 1;
    }

    public int steamCapacityMultiplier() {
        return this == STEAM_LV ? 2 : 1;
    }

    public float steamExplosionPower() {
        return TNT_EXPLOSION_POWER;
    }

    public static List<MachineTier> expandSingleBlockTiers(
            MachineTier startTier
    ) {
        if (startTier == NONE) {
            return List.of(NONE);
        }

        List<MachineTier> family = startTier.isSteam()
                ? STEAM_SINGLEBLOCK_TIERS
                : ELECTRIC_TIERS;

        int startIndex = family.indexOf(startTier);
        if (startIndex < 0) {
            throw new IllegalArgumentException(
                    "Unknown singleblock machine tier: "
                            + startTier
            );
        }

        return family.subList(startIndex, family.size());
    }
}
