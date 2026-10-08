package net.mads.industron.material.recipes;
import net.mads.industron.material.MaterialPart;
/** Cleaning saves thermal work, never the promised metal output. */
public final class OreWorkRules {
    private OreWorkRules(){}
    public static int meltingDuration(int baseTicks,MaterialPart part) {
        double factor=durationMultiplier(part);
        return (int)Math.min(Integer.MAX_VALUE,Math.ceil(Math.max(1,baseTicks)*factor));
    }
    public static double durationMultiplier(MaterialPart part) {
        return switch(part) {
            case RAW_ORE, RAW_BLOCK -> 2.0;
            case CRUSHED_ORE, IMPURE_DUST -> 1.5;
            case WASHED_CRUSHED_ORE -> 1.15;
            default -> 1.0;
        };
    }
}
