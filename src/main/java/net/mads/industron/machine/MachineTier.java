package net.mads.industron.machine;

import java.util.List;

public record MachineTier(String id, String displayName, int color) {
    private static final float TNT_EXPLOSION_POWER = 4.0F;

    public enum Family {
        NONE,
        ELECTRIC,
        STEAM
    }

    public static final MachineTier NONE = new MachineTier("none", "", 0xFFFFFF);

    public static final MachineTier ULV = new MachineTier("ulv", "ULV", 0x6D6D6D);
    public static final MachineTier LV = new MachineTier("lv", "LV", 0x4E8FDC);
    public static final MachineTier MV = new MachineTier("mv", "MV", 0xE0A83A);
    public static final MachineTier HV = new MachineTier("hv", "HV", 0xE85B5B);
    public static final MachineTier EV = new MachineTier("ev", "EV", 0x9B5DE5);
    public static final MachineTier IV = new MachineTier("iv", "IV", 0x5DD9C1);
    public static final MachineTier LUV = new MachineTier("luv", "LuV", 0xFF8C42);
    public static final MachineTier ZPM = new MachineTier("zpm", "ZPM", 0xD84FD6);
    public static final MachineTier UV = new MachineTier("uv", "UV", 0x5A4BFF);
    public static final MachineTier UHV = new MachineTier("uhv", "UHV", 0x37D6FF);
    public static final MachineTier UEV = new MachineTier("uev", "UEV", 0xF2F7FF);
    public static final MachineTier UIV = new MachineTier("uiv", "UIV", 0xB7FF4A);
    public static final MachineTier UXV = new MachineTier("uxv", "UXV", 0xFF3D8B);
    public static final MachineTier OPV = new MachineTier("opv", "OpV", 0x2B2B2B);
    public static final MachineTier MAX = new MachineTier("max", "MAX", 0xFFFFFF);

    public static final MachineTier STEAM_COPPER =
            new MachineTier("steam_copper", "Copper", 0xC87533);

    public static final MachineTier STEAM_BRONZE =
            new MachineTier("steam_bronze", "Bronze", 0xB08D57);

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

    public static final List<MachineTier> STEAM_SINGLEBLOCK_TIERS = List.of(
            STEAM_COPPER,
            STEAM_BRONZE
    );

    public String casingRegistryName() {
        return id + "_machine_casing";
    }

    public String casingDisplayName() {
        return displayName + " Machine Casing";
    }

    public String singleBlockMachineCasingSideTexture() {
        if (this == STEAM_COPPER) {
            return "industron:block/casings/casing/bricked_copper_casing_side";
        }

        if (this == STEAM_BRONZE) {
            return "industron:block/casings/casing/bricked_bronze_casing_side";
        }

        return "industron:block/casings/universal_textures/casing";
    }

    public String singleBlockMachineCasingBottomTexture() {
        if (this == STEAM_COPPER) {
            return "industron:block/casings/casing/bricked_copper_casing_bottom";
        }

        if (this == STEAM_BRONZE) {
            return "industron:block/casings/casing/bricked_bronze_casing_bottom";
        }

        return singleBlockMachineCasingSideTexture();
    }

    public String singleBlockMachineCasingTopTexture() {
        if (this == STEAM_COPPER) {
            return "industron:block/casings/casing/bricked_copper_casing_top";
        }

        if (this == STEAM_BRONZE) {
            return "industron:block/casings/casing/bricked_bronze_casing_top";
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
        if (this == STEAM_COPPER) {
            return ULV;
        }

        if (this == STEAM_BRONZE) {
            return LV;
        }

        return this;
    }

    public int steamDurationMultiplier() {
        if (this == STEAM_COPPER) {
            return 8;
        }

        if (this == STEAM_BRONZE) {
            return 4;
        }

        return 1;
    }

    public int steamUsageMultiplier() {
        if (this == STEAM_BRONZE) {
            return 2;
        }

        return 1;
    }

    public int steamCapacityMultiplier() {
        if (this == STEAM_BRONZE) {
            return 2;
        }

        return 1;
    }

    public float steamExplosionPower() {
        if (this == STEAM_BRONZE) {
            return TNT_EXPLOSION_POWER * 2.0F;
        }

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
