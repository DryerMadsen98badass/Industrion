package net.mads.industron.recipe.chiseling;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/** Minimal RecipeInput bridge; block interaction is handled by ChiselingRuntime. */
public record ChiselingRecipeInput(ItemStack baseBlock, ItemStack mainHand, ItemStack offHand) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return switch (index) {
            case 0 -> baseBlock;
            case 1 -> mainHand;
            case 2 -> offHand;
            default -> throw new IndexOutOfBoundsException("Chiseling input index: " + index);
        };
    }

    @Override
    public int size() {
        return 3;
    }
}
