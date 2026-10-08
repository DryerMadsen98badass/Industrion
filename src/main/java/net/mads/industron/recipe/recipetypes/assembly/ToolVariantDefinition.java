package net.mads.industron.recipe.recipetypes.assembly;

import net.mads.industron.machine.MachineTier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** One concrete item capable of satisfying a public tool family. */
public record ToolVariantDefinition(
        Item item,
        AssemblyToolType type,
        double efficiency,
        MachineTier tier,
        SoundEvent sound
) {
    public ToolVariantDefinition {
        if (item == null) throw new IllegalArgumentException("Tool item cannot be null");
        if (type == null) throw new IllegalArgumentException("Tool type cannot be null");
        if (!Double.isFinite(efficiency) || efficiency <= 0.0D) {
            throw new IllegalArgumentException("Tool efficiency must be a positive number of seconds");
        }
        if (tier == null || tier == MachineTier.NONE) {
            throw new IllegalArgumentException("Tool tier must be a real machine tier");
        }
        if (sound == null) sound = SoundEvents.STONE_HIT;
    }

    public int useTimeTicks() {
        return Math.max(1, (int) Math.round(efficiency * 20.0D));
    }

    public boolean matches(ItemStack stack) {
        return stack.is(item);
    }
}
