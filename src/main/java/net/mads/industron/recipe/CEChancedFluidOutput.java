package net.mads.industron.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.machine.MachineTierStats;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Optional;

public record CEChancedFluidOutput(FluidStack stack, int chance, int tierBonus) {
    public static final int MAX_CHANCE = 10_000;

    public static final Codec<CEChancedFluidOutput> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FluidStack.CODEC.fieldOf("stack").forGetter(CEChancedFluidOutput::stack),
            ExtraCodecs.intRange(0, MAX_CHANCE).fieldOf("chance").forGetter(CEChancedFluidOutput::chance),
            ExtraCodecs.intRange(-MAX_CHANCE, MAX_CHANCE).optionalFieldOf("tier_bonus", 0).forGetter(CEChancedFluidOutput::tierBonus)
    ).apply(instance, CEChancedFluidOutput::new));

    public CEChancedFluidOutput {
        if (chance < 0 || chance > MAX_CHANCE) {
            throw new IllegalArgumentException("Chanced fluid output chance must be between 0 and " + MAX_CHANCE);
        }
        if (tierBonus < -MAX_CHANCE || tierBonus > MAX_CHANCE) {
            throw new IllegalArgumentException("Chanced fluid output tier bonus must be between -" + MAX_CHANCE + " and " + MAX_CHANCE);
        }
    }

    public int effectiveChance(Optional<MachineTier> runtimeTier, Optional<MachineTier> baselineTier) {
        int tierDifference = 0;
        if (runtimeTier.isPresent() && baselineTier.isPresent()) {
            tierDifference = Math.max(0, MachineTierStats.tierIndex(runtimeTier.get()) - MachineTierStats.tierIndex(baselineTier.get()));
        }
        long effective = (long) chance + (long) tierBonus * tierDifference;
        return (int) Math.max(0L, Math.min(MAX_CHANCE, effective));
    }
}
