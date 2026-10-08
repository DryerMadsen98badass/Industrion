package net.mads.industron.machine.foundry;

/** Implemented by heat providers; fuel and energy consumption stay with the provider. */
public interface FoundryHeatSource {
    /** Called during link validation, never as a per-tick footprint scan. */
    boolean matchesFoundryFootprint(int minX, int minZ, int baseY, int outerSize);
    /** Zero whenever unformed, disabled, unloaded or unpowered. */
    double availableFoundryHeatPerTick();
    double maximumFoundryTemperature();
    /** Changes whenever formed geometry changes, allowing O(1) cached-link checks. */
    long foundryHeatRevision();
}
