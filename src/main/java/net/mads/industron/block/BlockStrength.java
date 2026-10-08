package net.mads.industron.block;

/** Immutable hardness/resistance pair used by declarative block definitions. */
public record BlockStrength(float hardness, float resistance) {
    public BlockStrength {
        if (!Float.isFinite(hardness)) {
            throw new IllegalArgumentException("Block hardness must be finite");
        }
        if (!Float.isFinite(resistance)) {
            throw new IllegalArgumentException("Block resistance must be finite");
        }
    }

    public static BlockStrength of(float hardness) {
        return new BlockStrength(hardness, hardness);
    }

    public static BlockStrength of(float hardness, float resistance) {
        return new BlockStrength(hardness, resistance);
    }
}
