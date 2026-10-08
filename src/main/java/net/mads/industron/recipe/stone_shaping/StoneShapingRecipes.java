package net.mads.industron.recipe.stone_shaping;

import net.mads.industron.registry.RecipeRegistry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

import java.util.List;

public final class StoneShapingRecipes {
    private StoneShapingRecipes() {
    }

    public static List<RecipeHolder<StoneShapingRecipe>> forHands(Level level, ItemStack mainHand, ItemStack offHand) {
        if (level == null) return List.of();
        StoneShapingRecipeInput input = new StoneShapingRecipeInput(mainHand, offHand);
        return level.getRecipeManager().getAllRecipesFor(RecipeRegistry.STONE_SHAPING_RECIPE_TYPE.get()).stream()
                .filter(holder -> holder.value().matches(input, level))
                .toList();
    }

    /** Recipe lookup retained after the opening Pebbles have already been consumed. */
    public static List<RecipeHolder<StoneShapingRecipe>> forItems(Level level, Item mainHand, Item offHand) {
        if (level == null || mainHand == null || offHand == null) return List.of();
        return forHands(level, new ItemStack(mainHand), new ItemStack(offHand));
    }
}
