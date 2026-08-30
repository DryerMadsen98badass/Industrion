package net.mads.industron.material.chemistry;

public enum BondOrder {
    SINGLE(1.0),
    DOUBLE(2.0),
    TRIPLE(3.0),
    AROMATIC(1.5),
    METALLIC(0.5),
    IONIC(1.0),
    UNSPECIFIED(0.0);

    private final double value;

    BondOrder(double value) {
        this.value = value;
    }

    public double value() {
        return value;
    }
}
