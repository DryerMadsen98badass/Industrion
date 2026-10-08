package net.mads.industron.integration.create.kinetic;

/** LV overclocking is applied by CERecipe first. Four times that duration at 256 RPM. */
public final class KineticProcessingRules {
    public static final net.mads.industron.machine.ProcessingProfile PROFILE = net.mads.industron.machine.machines.kinetic.KineticProcessingDefinition.PROCESSING;
    public static final net.mads.industron.machine.MachineTier TIER=net.mads.industron.machine.machines.kinetic.KineticProcessingDefinition.TIER;
    public static final int MAX_RPM=net.mads.industron.machine.machines.kinetic.KineticProcessingDefinition.MAX_RPM;
    public static final int REFERENCE_RPM=PROFILE.referenceRpm(), DURATION_MULTIPLIER=(int)PROFILE.durationMultiplier();
    private KineticProcessingRules() {}
    public static boolean supportsTier(net.mads.industron.machine.MachineTier tier) {
        int required=net.mads.industron.machine.MachineTier.ELECTRIC_TIERS.indexOf(tier);
        return required>=0 && required<=net.mads.industron.machine.MachineTier.ELECTRIC_TIERS.indexOf(TIER);
    }
    public static int referenceDuration(int lvDuration) {
        return PROFILE.duration(lvDuration);
    }
    public static double speed(double rpm) {
        if(!Double.isFinite(rpm) || rpm==0)return 0;
        return PROFILE.speed(rpm);
    }
    public static final class WorkCredit {
        private double credit;
        public int advance(double rpm) {credit+=speed(rpm);int ticks=(int)credit;credit-=ticks;return ticks;}
        public void reset() {credit=0;}
        public double value() {return credit;}
        public void restore(double value) {credit=Double.isFinite(value)?Math.max(0,Math.min(Math.nextDown(1.0),value)):0;}
    }
}
