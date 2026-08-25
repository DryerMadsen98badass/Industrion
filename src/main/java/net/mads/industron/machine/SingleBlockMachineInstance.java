package net.mads.industron.machine;

public record SingleBlockMachineInstance(
        SingleBlockDefinition definition,
        MachineTier tier
) {
    public String registryName() {
        return tier == MachineTier.NONE
                ? definition.id()
                : tier.id() + "_" + definition.id();
    }

    public String displayName() {
        return tier == MachineTier.NONE
                ? definition.displayName()
                : tier.displayName() + " " + definition.displayName();
    }

    public int steamCapacity() {
        return definition.steamCapacity() * tier.steamCapacityMultiplier();
    }

    public int steamUsage() {
        return definition.steamUsage() * tier.steamUsageMultiplier();
    }

    public int energyUsage() {
        return MachineTierStats.machineEnergyUsage(definition.energyUsage(), tier.recipeTier());
    }

    public double kineticSuPerRpm() {
        return MachineTierStats.machineKineticSuPerRpm(
                definition.startSu(),
                definition.startTier(),
                tier
        );
    }
}
