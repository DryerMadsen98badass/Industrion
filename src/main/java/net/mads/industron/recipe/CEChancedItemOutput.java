package net.mads.industron.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.machine.MachineTierStats;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public record CEChancedItemOutput(ItemStack stack, int chance, int tierBonus) {
    public static final int MAX_CHANCE = 10_000;

    public static final Codec<CEChancedItemOutput> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.CODEC.fieldOf("stack").forGetter(CEChancedItemOutput::stack),
            ExtraCodecs.intRange(0, MAX_CHANCE).optionalFieldOf("chance", MAX_CHANCE).forGetter(CEChancedItemOutput::chance),
            ExtraCodecs.intRange(-MAX_CHANCE, MAX_CHANCE).optionalFieldOf("tier_bonus", 0).forGetter(CEChancedItemOutput::tierBonus)
    ).apply(instance, CEChancedItemOutput::new));

    public CEChancedItemOutput(ItemStack stack, int chance) {
        this(stack, chance, 0);
    }

    public CEChancedItemOutput {
        if (chance < 0 || chance > MAX_CHANCE) {
            throw new IllegalArgumentException("Chanced output chance must be between 0 and " + MAX_CHANCE);
        }
        if (tierBonus < -MAX_CHANCE || tierBonus > MAX_CHANCE) {
            throw new IllegalArgumentException("Chanced output tier bonus must be between -" + MAX_CHANCE + " and " + MAX_CHANCE);
        }
    }

    public boolean guaranteed() {
        return chance >= MAX_CHANCE && tierBonus >= 0;
    }

    public int effectiveChance(Optional<MachineTier> runtimeTier, Optional<MachineTier> baselineTier) {
        int tierDifference = 0;
        if (runtimeTier.isPresent() && baselineTier.isPresent()) {
            tierDifference = Math.max(
                    0,
                    MachineTierStats.tierIndex(runtimeTier.get()) - MachineTierStats.tierIndex(baselineTier.get())
            );
        }

        long effective = (long) chance + (long) tierBonus * tierDifference;
        return (int) Math.max(0L, Math.min(MAX_CHANCE, effective));
    }
}
