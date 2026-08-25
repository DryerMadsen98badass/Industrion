package net.mads.industron.block;

public enum MiningTier {

    WOOD(
            2.67F,
            4.0F
    ),

    STONE(
            5.33F,
            8.0F
    ),

    IRON(
            8.0F,
            12.0F
    ),

    DIAMOND(
            10.67F,
            16.0F
    ),

    NETHERITE(
            12.0F,
            20.0F
    );

    private final float hardness;
    private final float resistance;

    MiningTier(
            float hardness,
            float resistance
    ) {
        this.hardness = hardness;
        this.resistance = resistance;
    }

    public float hardness() {
        return hardness;
    }

    public float resistance() {
        return resistance;
    }
}