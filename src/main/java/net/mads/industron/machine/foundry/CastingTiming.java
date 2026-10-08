package net.mads.industron.machine.foundry;

/** Shared caster timing in game ticks (20 ticks per second). */
public final class CastingTiming {
    public static final int FAUCET_MB_PER_SECOND = 24;
    private CastingTiming() {}
    public static int duration(int mb) {
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, ((long) mb * 200 + 143) / 144));
    }
    public static int transfer(int tick) {
        return ((tick % 20 + 1) * FAUCET_MB_PER_SECOND / 20)
                - ((tick % 20) * FAUCET_MB_PER_SECOND / 20);
    }
}
