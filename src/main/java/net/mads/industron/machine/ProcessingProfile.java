package net.mads.industron.machine;

/** One source for execution and presentation. 1x is implicit. */
public record ProcessingProfile(double durationMultiplier, int referenceRpm) {
    public static final ProcessingProfile STANDARD = new ProcessingProfile(1, 0);
    public static final ProcessingProfile STEAM = new ProcessingProfile(2, 0);
    public ProcessingProfile {
        if (!Double.isFinite(durationMultiplier) || durationMultiplier <= 0 || referenceRpm < 0)
            throw new IllegalArgumentException("Invalid processing profile");
    }
    public int duration(int ticks) {
        return (int)Math.max(1, Math.min(Integer.MAX_VALUE, Math.ceil(Math.max(1, ticks) * durationMultiplier)));
    }
    public double speed(double rpm) {
        if (!Double.isFinite(rpm) || rpm == 0) return 0;
        return referenceRpm == 0 ? 1 : Math.sqrt(Math.min(referenceRpm, Math.abs(rpm)) / referenceRpm);
    }
}
