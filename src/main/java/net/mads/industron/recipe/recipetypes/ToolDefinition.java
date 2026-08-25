package net.mads.industron.recipe.recipetypes;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record ToolDefinition(Item item, AssemblyToolType type, int useTimeTicks, SoundEvent sound) {
    public ToolDefinition {
        if (useTimeTicks < 0) throw new IllegalArgumentException("Tool use time cannot be negative");
    }

    public boolean matches(ItemStack stack) {
        return stack.is(item);
    }
}
