package net.mads.industron.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.mads.industron.Industron;
import net.mads.industron.block.SimpleBlockDefinition;
import net.mads.industron.block.SimpleBlockVariant;
import net.mads.industron.block.SimpleBlocks;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class SimpleBlockRecipeProvider implements DataProvider {

    private final PackOutput.PathProvider recipes;

    public SimpleBlockRecipeProvider(PackOutput output) {
        this.recipes = output.createPathProvider(
                PackOutput.Target.DATA_PACK,
                "recipe"
        );
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> futures =
                new ArrayList<>();

        for (SimpleBlockDefinition definition : SimpleBlocks.ALL) {
            for (SimpleBlockVariant variant
                    : definition.variants()) {

                saveCraftingRecipe(
                        futures,
                        output,
                        definition,
                        variant
                );

                saveStonecuttingRecipe(
                        futures,
                        output,
                        definition,
                        variant
                );
            }
        }

        return CompletableFuture.allOf(
                futures.toArray(CompletableFuture[]::new)
        );
    }

    private void saveCraftingRecipe(
            List<CompletableFuture<?>> futures,
            CachedOutput output,
            SimpleBlockDefinition definition,
            SimpleBlockVariant variant
    ) {
        ResourceLocation input =
                modLocation(definition.id());

        ResourceLocation result =
                modLocation(
                        definition.variantId(variant)
                );

        JsonObject recipe = switch (variant) {
            case SLAB -> shapedRecipe(
                    input,
                    result,
                    6,
                    List.of("XXX")
            );

            case STAIR -> shapedRecipe(
                    input,
                    result,
                    4,
                    List.of(
                            "X  ",
                            "XX ",
                            "XXX"
                    )
            );

            case WALL -> shapedRecipe(
                    input,
                    result,
                    6,
                    List.of(
                            "XXX",
                            "XXX"
                    )
            );

            case FENCE -> fenceRecipe(
                    input,
                    result
            );

            case FENCE_GATE -> fenceGateRecipe(
                    input,
                    result
            );

            case BUTTON -> shapedRecipe(
                    input,
                    result,
                    1,
                    List.of("X")
            );

            case PRESSURE_PLATE -> shapedRecipe(
                    input,
                    result,
                    1,
                    List.of("XX")
            );
        };

        ResourceLocation recipeId =
                modLocation(
                        "crafting/"
                                + definition.variantId(variant)
                );

        save(
                futures,
                output,
                recipeId,
                recipe
        );
    }

    private void saveStonecuttingRecipe(
            List<CompletableFuture<?>> futures,
            CachedOutput output,
            SimpleBlockDefinition definition,
            SimpleBlockVariant variant
    ) {
        ResourceLocation input =
                modLocation(definition.id());

        ResourceLocation result =
                modLocation(
                        definition.variantId(variant)
                );

        int count = switch (variant) {
            case SLAB -> 2;
            case STAIR -> 1;
            case WALL -> 1;
            case FENCE -> 1;
            case FENCE_GATE -> 1;
            case BUTTON -> 1;
            case PRESSURE_PLATE -> 1;
        };

        JsonObject recipe = new JsonObject();

        recipe.addProperty(
                "type",
                "minecraft:stonecutting"
        );

        recipe.add(
                "ingredient",
                itemIngredient(input)
        );

        recipe.add(
                "result",
                resultObject(
                        result,
                        count
                )
        );

        ResourceLocation recipeId =
                modLocation(
                        "stonecutting/"
                                + definition.variantId(variant)
                                + "_from_"
                                + definition.id()
                );

        save(
                futures,
                output,
                recipeId,
                recipe
        );
    }

    private JsonObject shapedRecipe(
            ResourceLocation input,
            ResourceLocation result,
            int resultCount,
            List<String> patternRows
    ) {
        JsonObject recipe = new JsonObject();

        recipe.addProperty(
                "type",
                "minecraft:crafting_shaped"
        );

        recipe.addProperty(
                "category",
                "building"
        );

        JsonArray pattern = new JsonArray();

        for (String row : patternRows) {
            pattern.add(row);
        }

        recipe.add("pattern", pattern);

        JsonObject key = new JsonObject();
        key.add(
                "X",
                itemIngredient(input)
        );

        recipe.add("key", key);

        recipe.add(
                "result",
                resultObject(
                        result,
                        resultCount
                )
        );

        return recipe;
    }

    private JsonObject fenceRecipe(
            ResourceLocation input,
            ResourceLocation result
    ) {
        JsonObject recipe = new JsonObject();

        recipe.addProperty(
                "type",
                "minecraft:crafting_shaped"
        );

        recipe.addProperty(
                "category",
                "building"
        );

        JsonArray pattern = new JsonArray();
        pattern.add("X#X");
        pattern.add("X#X");

        recipe.add("pattern", pattern);

        JsonObject key = new JsonObject();

        key.add(
                "X",
                itemIngredient(input)
        );

        key.add(
                "#",
                itemIngredient(
                        ResourceLocation.withDefaultNamespace(
                                "stick"
                        )
                )
        );

        recipe.add("key", key);

        recipe.add(
                "result",
                resultObject(
                        result,
                        3
                )
        );

        return recipe;
    }

    private JsonObject fenceGateRecipe(
            ResourceLocation input,
            ResourceLocation result
    ) {
        JsonObject recipe = new JsonObject();

        recipe.addProperty(
                "type",
                "minecraft:crafting_shaped"
        );

        recipe.addProperty(
                "category",
                "building"
        );

        JsonArray pattern = new JsonArray();
        pattern.add("#X#");
        pattern.add("#X#");

        recipe.add("pattern", pattern);

        JsonObject key = new JsonObject();

        key.add(
                "X",
                itemIngredient(input)
        );

        key.add(
                "#",
                itemIngredient(
                        ResourceLocation.withDefaultNamespace(
                                "stick"
                        )
                )
        );

        recipe.add("key", key);

        recipe.add(
                "result",
                resultObject(
                        result,
                        1
                )
        );

        return recipe;
    }

    private JsonObject itemIngredient(
            ResourceLocation item
    ) {
        JsonObject ingredient =
                new JsonObject();

        ingredient.addProperty(
                "item",
                item.toString()
        );

        return ingredient;
    }

    private JsonObject resultObject(
            ResourceLocation item,
            int count
    ) {
        JsonObject result =
                new JsonObject();

        result.addProperty(
                "id",
                item.toString()
        );

        if (count != 1) {
            result.addProperty(
                    "count",
                    count
            );
        }

        return result;
    }

    private void save(
            List<CompletableFuture<?>> futures,
            CachedOutput output,
            ResourceLocation recipeId,
            JsonObject recipe
    ) {
        Path path = recipes.json(recipeId);

        futures.add(
                DataProvider.saveStable(
                        output,
                        recipe,
                        path
                )
        );
    }

    private static ResourceLocation modLocation(
            String path
    ) {
        return ResourceLocation.fromNamespaceAndPath(
                Industron.MOD_ID,
                path
        );
    }

    @Override
    public String getName() {
        return "Industron Simple Block Recipes";
    }
}