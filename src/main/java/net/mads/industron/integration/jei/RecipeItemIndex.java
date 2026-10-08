package net.mads.industron.integration.jei;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Item buckets retain recipe order and exact component matching without copying stacks per query. */
final class RecipeItemIndex<R> {
    private final List<R> recipes;
    private final Map<Item, List<Entry<R>>> inputs = new IdentityHashMap<>();
    private final Map<Item, List<Entry<R>>> outputs = new IdentityHashMap<>();

    RecipeItemIndex(List<R> recipes, Function<R, List<ItemStack>> recipeInputs,
                    Function<R, List<ItemStack>> recipeOutputs) {
        this.recipes = List.copyOf(recipes);
        for (R recipe : recipes) {
            add(inputs, recipe, recipeInputs.apply(recipe));
            add(outputs, recipe, recipeOutputs.apply(recipe));
        }
    }

    List<R> all() { return recipes; }
    boolean hasInput(ItemStack stack) { return contains(inputs, stack); }
    boolean hasOutput(ItemStack stack) { return contains(outputs, stack); }
    List<R> inputs(ItemStack stack) { return matching(inputs, stack); }
    List<R> outputs(ItemStack stack) { return matching(outputs, stack); }

    private static <R> void add(Map<Item, List<Entry<R>>> index, R recipe, List<ItemStack> stacks) {
        Map<Item, List<ItemStack>> grouped = new IdentityHashMap<>();
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) grouped.computeIfAbsent(stack.getItem(), ignored -> new ArrayList<>()).add(stack.copy());
        }
        grouped.forEach((item, templates) -> index.computeIfAbsent(item, ignored -> new ArrayList<>())
                .add(new Entry<>(recipe, List.copyOf(templates))));
    }

    private static <R> boolean contains(Map<Item, List<Entry<R>>> index, ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (Entry<R> entry : index.getOrDefault(stack.getItem(), List.of())) {
            if (entry.matches(stack)) return true;
        }
        return false;
    }

    private static <R> List<R> matching(Map<Item, List<Entry<R>>> index, ItemStack stack) {
        if (stack.isEmpty()) return List.of();
        List<Entry<R>> candidates = index.get(stack.getItem());
        if (candidates == null) return List.of();
        List<R> result = new ArrayList<>();
        for (Entry<R> entry : candidates) if (entry.matches(stack)) result.add(entry.recipe);
        return List.copyOf(result);
    }

    private record Entry<R>(R recipe, List<ItemStack> templates) {
        private boolean matches(ItemStack stack) {
            for (ItemStack template : templates) {
                if (ItemStack.isSameItemSameComponents(template, stack)) return true;
            }
            return false;
        }
    }
}
