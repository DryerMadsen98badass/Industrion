package net.mads.industron.machine.foundry;

import net.mads.industron.material.IndustrialMaterial;

/** A heat-rated, bufferless outlet that participates in the existing Foundry structure. */
public final class FoundryDrainBlock extends FoundryPartBlock {
    private final IndustrialMaterial clay;
    public FoundryDrainBlock(IndustrialMaterial clay) {
        super(FoundryPartType.FLUID_OUTPUT_HATCH);
        this.clay = clay;
    }
    public IndustrialMaterial clay() { return clay; }
}
