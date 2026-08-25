package net.mads.industron.machine.interaction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Percent-based effect applied by the first matching modifier. */
public record ModifierEffect(Type type, float amount) {
    public static final Codec<ModifierEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(Type::valueOf, Type::name).fieldOf("type").forGetter(ModifierEffect::type),
            Codec.FLOAT.fieldOf("amount").forGetter(ModifierEffect::amount)
    ).apply(instance, ModifierEffect::new));

    /** Positive values make recipes faster, for example 0.10 is 10% faster. */
    public static ModifierEffect recipeSpeed(float amount) {
        return new ModifierEffect(Type.RECIPE_SPEED, amount);
    }

    /** Negative values reduce CE usage, for example -0.05 is 5% less CE. */
    public static ModifierEffect energyUsage(float amount) {
        return new ModifierEffect(Type.ENERGY_USAGE, amount);
    }

    /** Negative values reduce steam usage, for example -0.05 is 5% less steam. */
    public static ModifierEffect steamUsage(float amount) {
        return new ModifierEffect(Type.STEAM_USAGE, amount);
    }

    public enum Type {
        RECIPE_SPEED,
        ENERGY_USAGE,
        STEAM_USAGE
    }
}
