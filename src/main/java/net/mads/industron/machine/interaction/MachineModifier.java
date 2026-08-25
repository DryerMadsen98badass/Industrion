package net.mads.industron.machine.interaction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;

/** Ordered modifier. The first matching modifier wins; effects inside it stack. */
public record MachineModifier(List<ModifierRequirement> requirements, List<ModifierEffect> effects) {
    public static final Codec<MachineModifier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ModifierRequirement.CODEC.listOf().optionalFieldOf("requirements", List.of()).forGetter(MachineModifier::requirements),
            ModifierEffect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(MachineModifier::effects)
    ).apply(instance, MachineModifier::new));

    public MachineModifier {
        requirements = List.copyOf(requirements);
        effects = List.copyOf(effects);
    }

    public static Builder modifier() {
        return new Builder();
    }

    public float speedBonus() {
        return sum(ModifierEffect.Type.RECIPE_SPEED);
    }

    public float energyUsageBonus() {
        return sum(ModifierEffect.Type.ENERGY_USAGE);
    }

    public float steamUsageBonus() {
        return sum(ModifierEffect.Type.STEAM_USAGE);
    }

    private float sum(ModifierEffect.Type type) {
        float total = 0.0F;
        for (ModifierEffect effect : effects) {
            if (effect.type() == type) {
                total += effect.amount();
            }
        }
        return total;
    }

    public static final class Builder {
        private final List<ModifierRequirement> requirements = new ArrayList<>();
        private final List<ModifierEffect> effects = new ArrayList<>();

        /** Adds one requirement to this modifier. */
        public Builder requires(ModifierRequirement requirement) {
            requirements.add(requirement);
            return this;
        }

        /** Adds one effect to this modifier. */
        public Builder effect(ModifierEffect effect) {
            effects.add(effect);
            return this;
        }

        public MachineModifier build() {
            return new MachineModifier(requirements, effects);
        }
    }
}
