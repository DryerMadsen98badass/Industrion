package net.mads.industron.machine.foundry;
/** One Fuel Unit buys 40 ticks of one parallel's thermal work. */
public final class BlastFuelRules {
    public static final int TICKS_PER_FUEL_UNIT=40;
    private BlastFuelRules(){}
    public static double units(int workTicks){return Math.max(1,workTicks)/(double)TICKS_PER_FUEL_UNIT;}
    public static double restore(double credit){return Double.isFinite(credit)?Math.max(0,Math.min(credit,1_000_000)):0;}
}
