package net.mads.industron.recipe.fired_bucket;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.mads.industron.Industron;
import net.mads.industron.registry.FluidRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class FiredBucketRecipeInjector {
    private FiredBucketRecipeInjector() {
    }

    public static List<RecipeHolder<?>> withFiredBucketRecipes(
            Collection<RecipeHolder<?>> loadedRecipes,
            HolderLookup.Provider registries
    ) {
        FluidRegistry.buildFiredBucketMaps();

        List<RecipeHolder<?>> recipes =
                new ArrayList<>(loadedRecipes);

        int added = 0;
        int failed = 0;

        for (RecipeHolder<?> holder : loadedRecipes) {
            if (isGeneratedFiredRecipe(holder.id())) {
                continue;
            }

            Recipe<?> recipe = holder.value();

            if (!(recipe instanceof ShapedRecipe)
                    && !(recipe instanceof ShapelessRecipe)) {
                continue;
            }

            if (!(recipe instanceof CraftingRecipe craftingRecipe)) {
                continue;
            }

            if (!usesNormalBucket(craftingRecipe)) {
                continue;
            }

            Recipe<?> firedRecipe =
                    createFiredRecipeCopy(
                            recipe,
                            registries,
                            holder.id()
                    );

            if (firedRecipe == null) {
                failed++;
                continue;
            }

            recipes.add(
                    new RecipeHolder<>(
                            firedRecipeId(holder.id()),
                            firedRecipe
                    )
            );

            added++;
        }

        Industron.LOGGER.info(
                "Industron fired bucket recipe injection: added {} recipe copies, {} failed",
                added,
                failed
        );

        return recipes;
    }

    private static boolean usesNormalBucket(
            CraftingRecipe recipe
    ) {
        for (var ingredient : recipe.getIngredients()) {
            for (var itemStack : ingredient.getItems()) {
                if (FluidRegistry.FIRED_BUCKET_BY_NORMAL_BUCKET
                        .containsKey(itemStack.getItem())) {
                    return true;
                }
            }
        }

        return false;
    }

    @SuppressWarnings({
            "rawtypes",
            "unchecked"
    })
    private static Recipe<?> createFiredRecipeCopy(
            Recipe<?> originalRecipe,
            HolderLookup.Provider registries,
            ResourceLocation originalId
    ) {
        try {
            RecipeSerializer serializer =
                    originalRecipe.getSerializer();

            DynamicOps<JsonElement> ops =
                    registries.createSerializationContext(
                            JsonOps.INSTANCE
                    );

            JsonElement originalJson =
                    (JsonElement) serializer
                            .codec()
                            .codec()
                            .encodeStart(
                                    ops,
                                    originalRecipe
                            )
                            .getOrThrow();

            ReplacementResult replacement =
                    replaceBucketIds(originalJson);

            if (!replacement.changed()) {
                Industron.LOGGER.warn(
                        "Could not create fired bucket recipe copy for {} because no direct bucket item id was found in its serialized recipe",
                        originalId
                );

                return null;
            }

            return (Recipe<?>) serializer
                    .codec()
                    .codec()
                    .parse(
                            ops,
                            replacement.element()
                    )
                    .getOrThrow();
        } catch (Exception exception) {
            Industron.LOGGER.error(
                    "Failed to create fired bucket recipe copy for {}",
                    originalId,
                    exception
            );

            return null;
        }
    }

    private static ReplacementResult replaceBucketIds(
            JsonElement element
    ) {
        if (element == null || element.isJsonNull()) {
            return new ReplacementResult(
                    element,
                    false
            );
        }

        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive =
                    element.getAsJsonPrimitive();

            if (!primitive.isString()) {
                return new ReplacementResult(
                        element.deepCopy(),
                        false
                );
            }

            String value = primitive.getAsString();

            for (Map.Entry<Item, Item> entry
                    : FluidRegistry.FIRED_BUCKET_BY_NORMAL_BUCKET.entrySet()) {

                String normalId =
                        BuiltInRegistries.ITEM
                                .getKey(entry.getKey())
                                .toString();

                if (!value.equals(normalId)) {
                    continue;
                }

                String firedId =
                        BuiltInRegistries.ITEM
                                .getKey(entry.getValue())
                                .toString();

                return new ReplacementResult(
                        new JsonPrimitive(firedId),
                        true
                );
            }

            return new ReplacementResult(
                    element.deepCopy(),
                    false
            );
        }

        if (element.isJsonArray()) {
            JsonArray source =
                    element.getAsJsonArray();

            JsonArray result =
                    new JsonArray();

            boolean changed = false;

            for (JsonElement child : source) {
                ReplacementResult childResult =
                        replaceBucketIds(child);

                result.add(childResult.element());
                changed |= childResult.changed();
            }

            return new ReplacementResult(
                    result,
                    changed
            );
        }

        JsonObject source =
                element.getAsJsonObject();

        JsonObject result =
                new JsonObject();

        boolean changed = false;

        for (Map.Entry<String, JsonElement> entry
                : source.entrySet()) {

            ReplacementResult childResult =
                    replaceBucketIds(entry.getValue());

            result.add(
                    entry.getKey(),
                    childResult.element()
            );

            changed |= childResult.changed();
        }

        return new ReplacementResult(
                result,
                changed
        );
    }

    private static boolean isGeneratedFiredRecipe(
            ResourceLocation id
    ) {
        return id.getNamespace()
                .equals(Industron.MOD_ID)
                && id.getPath()
                .startsWith("fired_bucket/");
    }

    private static ResourceLocation firedRecipeId(
            ResourceLocation originalId
    ) {
        return ResourceLocation.fromNamespaceAndPath(
                Industron.MOD_ID,
                "fired_bucket/"
                        + originalId.getNamespace()
                        + "/"
                        + originalId.getPath()
        );
    }

    private record ReplacementResult(
            JsonElement element,
            boolean changed
    ) {
    }
}