package net.mads.industron.machine.machines.kinetic;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.machine.ProcessingProfile;
/** Shared definition for native Create-style Industron processors and their recipe bridge. */
public final class KineticProcessingDefinition {
    private KineticProcessingDefinition() {}
    public static final MachineTier TIER=MachineTier.LV;
    public static final ProcessingProfile PROCESSING=new ProcessingProfile(4,256);
    public static final int MAX_RPM=256;
}
