package net.mads.industron.data;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.util.concurrent.CompletableFuture;

/**
 * Retained as an empty provider so datagen can remove the old clay-brick stonecutting outputs.
 * Brick slab/stairs/wall construction now lives in MaterialBrickAssemblyRecipes.
 */
public final class ClayBrickShapeRecipeProvider implements DataProvider {

    public ClayBrickShapeRecipeProvider(PackOutput output) {
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public String getName() {
        return "Industron Clay Brick Shape Recipes";
    }
}
