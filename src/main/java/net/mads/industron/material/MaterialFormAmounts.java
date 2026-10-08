package net.mads.industron.material;

import net.mads.industron.machine.foundry.casting.CastingDefinitions;
import net.mads.industron.material.defenitions.MeltingParts;
import net.mads.industron.material.melting.MeltablePart;

/** Exact material amounts for forms whose quantity is defined by existing gameplay rules. */
public final class MaterialFormAmounts {
    private MaterialFormAmounts() {
    }

    public static int millibuckets(MaterialPart part) {
        if (part == null || part.isFluid()) return 0;
        // Composite machine components have no defined constituent bill yet.
        // Do not infer one material unit or create generic crushing/melting recipes.
        if (part.isMachineComponent()) return 0;


        // Canonical manufactured/cast form amounts live on MaterialPart so every subsystem,
        // including manual anvil forging, reads the same physical quantity.
        if (part.materialAmountMb() > 0) return part.materialAmountMb();

        MeltablePart manufactured = MeltingParts.get(part);
        if (manufactured != null) return manufactured.millibuckets();

        CastingDefinitions.Form cast = CastingDefinitions.cold(part);
        if (cast != null) return cast.millibuckets();

        int gemAmount = GemMaterialRules.millibuckets(part);
        if (gemAmount > 0) return gemAmount;

        return switch (part) {
            case TINY_DUST -> 16;
            case SMALL_DUST -> 36;
            case DUST, PURIFIED_DUST, IMPURE_DUST,
                    RAW_ORE, CRUSHED_ORE, WASHED_CRUSHED_ORE, REFINED_ORE -> 144;
            // Existing clay processing defines a cracked brick as exactly two Small Dust.
            case CRACKED_BRICK -> 2 * 36;
            case DOUBLE_INGOT -> 2 * 144;
            case CLAY_BLOCK -> ClayMaterialRules.CLAY_PER_BLOCK * 144;
            case BRICKS -> ClayMaterialRules.BRICKS_PER_BLOCK * 144;
            case FIREBOX -> 0;
            case BLOCK, RAW_BLOCK -> 9 * 144;
            // Industron's default solid quantity is one material unit per item/block.
            // Only explicit gameplay relationships above (or casting/melting definitions)
            // are allowed to override this; shape/name alone never implies a fraction.
            default -> (part.isItem() || part.isBlock()) ? 144 : 0;
        };
    }

    public static boolean isHot(MaterialPart part) {
        // Every generated hot material form uses the canonical hot_ id prefix, including forms
        // that are pending/not yet present in CastingDefinitions.ALL. Hot material must cool before
        // generic crushing or remelting, so this must not depend on caster artwork/volume support.
        return part != null && part.id().startsWith("hot_");
    }

    /**
     * Forms handled by ore preprocessing/storage are deliberately excluded from generic crushing.
     * Hot forms must cool before ordinary mechanical processing.
     */
    public static boolean isGenericCrushable(MaterialPart part) {
        if (part == null || part.isFluid() || isHot(part)) return false;
        return switch (part) {
            case BIOLOGICAL_FEED, DUST, SMALL_DUST, TINY_DUST,
                    RAW_ORE, RAW_BLOCK, CRUSHED_ORE, WASHED_CRUSHED_ORE, REFINED_ORE,
                    IMPURE_DUST, PURIFIED_DUST,
                    ORE, SMALL_ORE, DEEPSLATE_ORE, SMALL_DEEPSLATE_ORE,
                    NETHERRACK_ORE, SMALL_NETHERRACK_ORE, BLACKSTONE_ORE, SMALL_BLACKSTONE_ORE,
                    BASALT_ORE, SMALL_BASALT_ORE, END_STONE_ORE, SMALL_END_STONE_ORE -> false;
            default -> millibuckets(part) > 0;
        };
    }

    /** Pure material forms that may be phase-changed without invoking ore beneficiation chemistry. */
    public static boolean isGenericMeltable(MaterialPart part) {
        if (part == null || part.isFluid() || isHot(part)) return false;
        return switch (part) {
            case BIOLOGICAL_FEED, RAW_ORE, RAW_BLOCK, CRUSHED_ORE, WASHED_CRUSHED_ORE, REFINED_ORE,
                    IMPURE_DUST, PURIFIED_DUST,
                    ORE, SMALL_ORE, DEEPSLATE_ORE, SMALL_DEEPSLATE_ORE,
                    NETHERRACK_ORE, SMALL_NETHERRACK_ORE, BLACKSTONE_ORE, SMALL_BLACKSTONE_ORE,
                    BASALT_ORE, SMALL_BASALT_ORE, END_STONE_ORE, SMALL_END_STONE_ORE -> false;
            default -> millibuckets(part) > 0;
        };
    }
}
