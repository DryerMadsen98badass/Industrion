package net.mads.industron.recipe.chiseling;

import net.mads.industron.registry.RecipeRegistry;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public final class ChiselingRecipes {
    private ChiselingRecipes() {
    }

    public static List<RecipeHolder<ChiselingRecipe>> forBlock(Level level, BlockState state) {
        if (level == null || state == null) return List.of();
        return level.getRecipeManager()
                .getAllRecipesFor(RecipeRegistry.CHISELING_RECIPE_TYPE.get())
                .stream()
                .filter(holder -> holder.value().matchesBase(state))
                .toList();
    }
}
