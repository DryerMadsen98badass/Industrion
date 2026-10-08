package net.mads.industron.tool;

/** Exact fractional durability credit; no rounding loss between small repairs. */
public final class ToolRepairRules {
    private ToolRepairRules() {}
    public record Result(int repaired, long remainder) {}
    public static Result repair(int damage, int maximum, int headMb, int suppliedMb, long carried) {
        if(damage<=0 || maximum<=0 || headMb<=0 || suppliedMb<=0)return new Result(0,Math.max(0,carried));
        long credit=(long)suppliedMb*maximum+Math.max(0,Math.min(headMb-1L,carried));
        if(credit>=(long)damage*headMb)return new Result(damage,0); // Oversized parts are consumed whole.
        return new Result((int)(credit/headMb),credit%headMb);
    }
    public static double neededMb(int damage,int maximum,int headMb,long carried) {
        if(maximum<=0 || headMb<=0)return 0;
        return Math.max(0,((double)damage*headMb-Math.max(0,Math.min(headMb-1L,carried)))/maximum);
    }
}
