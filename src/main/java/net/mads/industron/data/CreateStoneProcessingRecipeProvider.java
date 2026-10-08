package net.mads.industron.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.mads.industron.Industron;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.defenitions.StoneMaterials;
import net.mads.industron.material.recipes.StoneProcessingRecipes;
import net.mads.industron.material.structure.StoneMaterial;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Create Millstone integration for StoneMaterial progression.
 *
 * Existing-input recipes are deliberately written to the Create namespace using the same
 * milling recipe id as Create's generated recipe (for example create:milling/cobblestone).
 * This replaces conflicting Create recipes instead of leaving two recipes for one input.
 */
public final class CreateStoneProcessingRecipeProvider implements DataProvider {
    private static final int COBBLED_TO_GRAVEL_PROCESSING_TIME = 250;
    private static final int GRAVEL_TO_DUST_PROCESSING_TIME = 200;
    private static final float BYPRODUCT_CHANCE = 0.10F;

    private final Path dataPackRoot;

    public CreateStoneProcessingRecipeProvider(PackOutput output) {
        this.dataPackRoot = output.getOutputFolder(PackOutput.Target.DATA_PACK);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        Set<ResourceLocation> writtenRecipeIds = new HashSet<>();

        for (StoneMaterial stone : StoneMaterials.ALL) {
            Optional<ResourceLocation> stoneBlock = StoneProcessingRecipes.resolveBlock(stone, MaterialPart.STONE);
            Optional<ResourceLocation> cobbledBlock = StoneProcessingRecipes.resolveBlock(stone, MaterialPart.COBBLED_STONE);
            Optional<ResourceLocation> gravelBlock = StoneProcessingRecipes.resolveBlock(stone, MaterialPart.GRAVEL);
            Optional<ResourceLocation> dustItem = StoneProcessingRecipes.resolveDust(stone);

            if (stoneBlock.isEmpty() || gravelBlock.isEmpty() || dustItem.isEmpty()) {
                continue;
            }

            // Normal stones: cobbled -> gravel. Sandstone families have no cobbled form,
            // so sandstone -> sand / red sandstone -> red sand is the equivalent step.
            ResourceLocation millingInput = cobbledBlock.orElse(stoneBlock.get());
            ResourceLocation chanceOutput = gravelChanceOutput(gravelBlock.get(), dustItem.get());
            ResourceLocation gravelRecipeId = replacementRecipeId(millingInput, stone, "to_gravel");

            saveMilling(
                    futures,
                    output,
                    writtenRecipeIds,
                    gravelRecipeId,
                    millingInput,
                    gravelBlock.get(),
                    Optional.of(chanceOutput),
                    COBBLED_TO_GRAVEL_PROCESSING_TIME
            );

            ResourceLocation dustRecipeId = replacementRecipeId(gravelBlock.get(), stone, "gravel_to_dust");
            saveMilling(
                    futures,
                    output,
                    writtenRecipeIds,
                    dustRecipeId,
                    gravelBlock.get(),
                    dustItem.get(),
                    Optional.empty(),
                    GRAVEL_TO_DUST_PROCESSING_TIME
            );
        }

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private static ResourceLocation gravelChanceOutput(ResourceLocation gravel, ResourceLocation stoneDust) {
        return gravel.equals(ResourceLocation.withDefaultNamespace("gravel"))
                ? ResourceLocation.withDefaultNamespace("flint")
                : stoneDust;
    }

    /**
     * If the input comes from Minecraft/Create, use Create's normal milling id convention so
     * an existing Create recipe with that input path is replaced. Generated Industron inputs
     * get an Industron-owned id and therefore cannot collide with another stone material.
     */
    private static ResourceLocation replacementRecipeId(
            ResourceLocation input,
            StoneMaterial stone,
            String fallbackSuffix
    ) {
        if (input.getNamespace().equals("minecraft") || input.getNamespace().equals("create")) {
            return ResourceLocation.fromNamespaceAndPath("create", "milling/" + input.getPath());
        }
        return ResourceLocation.fromNamespaceAndPath(
                Industron.MOD_ID,
                "stone_processing/milling/" + stone.id() + "/" + fallbackSuffix
        );
    }

    private void saveMilling(
            List<CompletableFuture<?>> futures,
            CachedOutput output,
            Set<ResourceLocation> writtenRecipeIds,
            ResourceLocation recipeId,
            ResourceLocation inputItem,
            ResourceLocation mainOutput,
            Optional<ResourceLocation> chanceOutput,
            int processingTime
    ) {
        if (!writtenRecipeIds.add(recipeId)) {
            throw new IllegalStateException("Duplicate generated Create stone recipe id: " + recipeId);
        }

        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "create:milling");

        JsonArray ingredients = new JsonArray();
        JsonObject ingredient = new JsonObject();
        ingredient.addProperty("item", inputItem.toString());
        ingredients.add(ingredient);
        recipe.add("ingredients", ingredients);
        recipe.addProperty("processing_time", processingTime);

        JsonArray results = new JsonArray();
        results.add(result(mainOutput, null));
        chanceOutput.ifPresent(item -> results.add(result(item, BYPRODUCT_CHANCE)));
        recipe.add("results", results);

        Path path = dataPackRoot
                .resolve(recipeId.getNamespace())
                .resolve("recipe")
                .resolve(recipeId.getPath() + ".json");
        futures.add(DataProvider.saveStable(output, recipe, path));
    }

    private static JsonObject result(ResourceLocation item, Float chance) {
        JsonObject result = new JsonObject();
        result.addProperty("id", item.toString());
        if (chance != null) {
            result.addProperty("chance", chance);
        }
        return result;
    }

    @Override
    public String getName() {
        return "Create Stone Processing Recipes: " + Industron.MOD_ID;
    }
}
