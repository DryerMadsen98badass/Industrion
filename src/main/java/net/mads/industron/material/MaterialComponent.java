package net.mads.industron.material;

public record MaterialComponent(
        IndustrialSubstance substance,
        int amount
) {
    public MaterialComponent {
        if (substance == null) {
            throw new IllegalArgumentException("Material component substance cannot be null");
        }
        if (amount < 1) {
            throw new IllegalArgumentException("Material component amount must be 1 or higher");
        }
    }
}
