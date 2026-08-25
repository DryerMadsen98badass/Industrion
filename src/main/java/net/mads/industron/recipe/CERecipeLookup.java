package net.mads.industron.recipe;

import net.mads.industron.registry.RecipeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class CERecipeLookup {
    private static final Map<RecipeManager, RecipeIndex> BY_MANAGER =
            new IdentityHashMap<>();

    private CERecipeLookup() {
    }

    public static void invalidate(RecipeManager manager) {
        BY_MANAGER.remove(manager);
    }

    public static List<RecipeHolder<CERecipe>> byType(RecipeManager manager, RecipeTypeDefinition type) {
        return byType(manager, type.id());
    }

    public static List<RecipeHolder<CERecipe>> byType(RecipeManager manager, ResourceLocation type) {
        return index(manager).allByType().getOrDefault(type, List.of());
    }

    public static List<RecipeHolder<CERecipe>> byTypes(RecipeManager manager, List<ResourceLocation> types) {
        return types.stream()
                .flatMap(type -> byType(manager, type).stream())
                .toList();
    }

    public static List<RecipeHolder<CERecipe>> candidatesByTypes(
            RecipeManager manager,
            List<ResourceLocation> types,
            CERecipeInput input
    ) {
        RecipeIndex index = index(manager);
        LinkedHashSet<RecipeHolder<CERecipe>> candidates = new LinkedHashSet<>();
        Set<LookupKey> inputKeys = itemKeys(input.items());
        for (ResourceLocation type : types) {
            TypeIndex typeIndex = index.byType().get(type);
            if (typeIndex == null) {
                continue;
            }
            candidates.addAll(typeIndex.fallback());
            for (LookupKey key : inputKeys) {
                candidates.addAll(typeIndex.byFirstIngredient().getOrDefault(key, List.of()));
            }
        }
        return List.copyOf(candidates);
    }

    public static Optional<RecipeHolder<CERecipe>> find(Level level, RecipeTypeDefinition type, CERecipeInput input) {
        return byType(level.getRecipeManager(), type).stream()
                .filter(recipe -> recipe.value().matches(input, level))
                .findFirst();
    }

    public static Optional<RecipeHolder<CERecipe>> byId(RecipeManager manager, ResourceLocation recipeId) {
        return manager.byKey(recipeId)
                .filter(holder -> holder.value() instanceof CERecipe)
                .map(holder -> (RecipeHolder<CERecipe>) (RecipeHolder<?>) holder);
    }

    public static Optional<RecipeHolder<CERecipe>> preferred(
            RecipeManager manager,
            ResourceLocation recipeId,
            Set<ResourceLocation> allowedTypes
    ) {
        return byId(manager, recipeId)
                .filter(holder -> allowedTypes.contains(holder.value().recipeType()));
    }

    private static RecipeIndex index(RecipeManager manager) {
        return BY_MANAGER.computeIfAbsent(manager, CERecipeLookup::buildIndex);
    }

    private static RecipeIndex buildIndex(RecipeManager manager) {
        Map<ResourceLocation, List<RecipeHolder<CERecipe>>> allByType = new java.util.HashMap<>();
        Map<ResourceLocation, MutableTypeIndex> mutableByType = new java.util.HashMap<>();
        for (RecipeHolder<CERecipe> holder : manager.getAllRecipesFor(RecipeRegistry.MACHINE_RECIPE_TYPE.get())) {
            ResourceLocation type = holder.value().recipeType();
            allByType.computeIfAbsent(type, ignored -> new ArrayList<>()).add(holder);
            mutableByType.computeIfAbsent(type, ignored -> new MutableTypeIndex()).add(holder);
        }

        Map<ResourceLocation, TypeIndex> byType = new java.util.HashMap<>();
        mutableByType.forEach((type, mutable) -> byType.put(type, mutable.freeze()));
        return new RecipeIndex(copyLists(allByType), Map.copyOf(byType));
    }

    private static Map<ResourceLocation, List<RecipeHolder<CERecipe>>> copyLists(Map<ResourceLocation, List<RecipeHolder<CERecipe>>> source) {
        Map<ResourceLocation, List<RecipeHolder<CERecipe>>> copy = new java.util.HashMap<>();
        source.forEach((type, recipes) -> copy.put(type, List.copyOf(recipes)));
        return Map.copyOf(copy);
    }

    private static Set<LookupKey> itemKeys(List<ItemStack> stacks) {
        LinkedHashSet<LookupKey> keys = new LinkedHashSet<>();
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                keys.add(LookupKey.item(BuiltInRegistries.ITEM.getKey(stack.getItem())));
            }
        }
        return keys;
    }

    private static Set<LookupKey> recipeItemKeys(CERecipe recipe) {
        LinkedHashSet<LookupKey> keys = new LinkedHashSet<>();
        recipe.itemInputs().forEach(input -> addIngredientKeys(keys, input.ingredient().getItems()));
        recipe.chancedItemInputs().forEach(input -> addIngredientKeys(keys, input.ingredient().ingredient().getItems()));
        recipe.notConsumableItems().forEach(input -> addIngredientKeys(keys, input.ingredient().getItems()));
        return keys;
    }

    private static void addIngredientKeys(Set<LookupKey> keys, ItemStack[] stacks) {
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                keys.add(LookupKey.item(BuiltInRegistries.ITEM.getKey(stack.getItem())));
            }
        }
    }

    private record RecipeIndex(
            Map<ResourceLocation, List<RecipeHolder<CERecipe>>> allByType,
            Map<ResourceLocation, TypeIndex> byType
    ) {
    }

    private record TypeIndex(
            Map<LookupKey, List<RecipeHolder<CERecipe>>> byFirstIngredient,
            List<RecipeHolder<CERecipe>> fallback
    ) {
    }

    private static final class MutableTypeIndex {
        private final Map<LookupKey, List<RecipeHolder<CERecipe>>> byFirstIngredient = new java.util.HashMap<>();
        private final List<RecipeHolder<CERecipe>> fallback = new ArrayList<>();

        private void add(RecipeHolder<CERecipe> holder) {
            Set<LookupKey> keys = recipeItemKeys(holder.value());
            if (keys.isEmpty()) {
                fallback.add(holder);
                return;
            }
            for (LookupKey key : keys) {
                byFirstIngredient.computeIfAbsent(key, ignored -> new ArrayList<>()).add(holder);
            }
        }

        private TypeIndex freeze() {
            Map<LookupKey, List<RecipeHolder<CERecipe>>> copied = new java.util.HashMap<>();
            byFirstIngredient.forEach((key, recipes) -> copied.put(key, List.copyOf(recipes)));
            return new TypeIndex(Map.copyOf(copied), List.copyOf(fallback));
        }
    }

    private record LookupKey(Kind kind, ResourceLocation id) {
        private static LookupKey item(ResourceLocation id) {
            return new LookupKey(Kind.ITEM, id);
        }
    }

    private enum Kind {
        ITEM
    }
}
