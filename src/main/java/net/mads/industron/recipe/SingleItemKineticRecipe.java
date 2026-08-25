package net.mads.industron.recipe;

import net.minecraft.world.item.ItemStack;

public interface SingleItemKineticRecipe {
    boolean matchesItem(ItemStack stack);

    boolean canProcessAtRpm(float rpm);

    ItemStack result();

    int processingDuration();

    default int inputCount() {
        return 1;
    }
}
