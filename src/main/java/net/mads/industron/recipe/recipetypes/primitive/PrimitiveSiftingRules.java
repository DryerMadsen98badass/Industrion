package net.mads.industron.recipe.recipetypes.primitive;

import net.mads.industron.recipe.CEChancedItemOutput;

/** Shared balance rules for hand-held primitive sifting. */
public final class PrimitiveSiftingRules {
    public static final String DISPLAY_NAME = "Primitive Sifting";

    /** Ten percent chance that one completed operation finds any dust at all. */
    public static final int FIND_CHANCE = CEChancedItemOutput.MAX_CHANCE / 10;

    private PrimitiveSiftingRules() {
    }
}
