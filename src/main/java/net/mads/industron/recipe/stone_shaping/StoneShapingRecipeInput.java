package net.mads.industron.recipe.stone_shaping;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record StoneShapingRecipeInput(ItemStack mainHand, ItemStack offHand) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return switch (index) {
            case 0 -> mainHand;
            case 1 -> offHand;
            default -> throw new IndexOutOfBoundsException("Stone Shaping input index: " + index);
        };
    }

    @Override
    public int size() {
        return 2;
    }
}
