package net.mads.industron.machine.foundry;

/** Full metal yield. Existing slag is an additional byproduct, never deducted from metal. */
public final class FoundryOutputRules {
    private FoundryOutputRules() {}
    public record Output(int metalMb,int slagMb) {}
    public static Output amounts(int inputMb,int previousRecoveredPer144) {
        int amount=Math.max(0,inputMb),factor=Math.max(0,Math.min(144,previousRecoveredPer144));
        int previousMetal=(int)((long)amount*factor/144);
        return new Output(amount,amount-previousMetal);
    }
}
