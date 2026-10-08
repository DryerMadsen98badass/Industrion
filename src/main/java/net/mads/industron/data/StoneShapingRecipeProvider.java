package net.mads.industron.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.mads.industron.Industron;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.defenitions.StoneMaterials;
import net.mads.industron.material.structure.StoneMaterial;
import net.mads.industron.material.structure.StructureMaterialVariantResolver;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Generates Pebble -> stone-tool-part shaping recipes from the existing 16x16 tool-part sprites. */
public final class StoneShapingRecipeProvider implements DataProvider {
    private final PackOutput.PathProvider recipes;
    private final StoneTextureResolver stoneTextureResolver;

    public StoneShapingRecipeProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        this.recipes = output.createPathProvider(PackOutput.Target.DATA_PACK, "recipe");
        this.stoneTextureResolver = new StoneTextureResolver(existingFileHelper);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (StoneMaterial stone : StoneMaterials.ALL) {
            if (!stone.generatedForms().contains(MaterialPart.PEBBLE)) continue;
            ResourceLocation pebble = generatedItemId(stone, MaterialPart.PEBBLE);
            ResourceLocation texture = stoneTextureResolver.baseSideTexture(stone);

            for (MaterialPart part : stone.generatedForms()) {
                if (!StructureMaterialVariantResolver.isStoneShapingPart(part)) continue;
                ResourceLocation maskTexture = StructureMaterialVariantResolver.toolTemplateTextures(stone, part)
                        .orElseThrow(() -> new IllegalStateException(
                                "Stone form " + stone.id() + " exposes " + part
                                        + " as a shaping part but no tool template exists"
                        ))
                        .base();
                List<String> pattern = readPattern(maskTexture);
                ResourceLocation result = generatedItemId(stone, part);

                JsonObject recipe = new JsonObject();
                recipe.addProperty("type", Industron.MOD_ID + ":stone_shaping");
                recipe.add("main_hand", stack(pebble));
                recipe.add("off_hand", stack(pebble));
                recipe.addProperty("texture", texture.toString());
                JsonArray rows = new JsonArray();
                pattern.forEach(rows::add);
                recipe.add("pattern", rows);
                recipe.add("result", stack(result));

                ResourceLocation recipeId = ResourceLocation.fromNamespaceAndPath(
                        Industron.MOD_ID,
                        "stone_shaping/" + stone.id() + "/" + part.id()
                );
                futures.add(DataProvider.saveStable(output, recipe, recipes.json(recipeId)));
            }
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private static JsonObject stack(ResourceLocation item) {
        JsonObject json = new JsonObject();
        json.addProperty("id", item.toString());
        return json;
    }

    private static ResourceLocation generatedItemId(StoneMaterial stone, MaterialPart part) {
        if (stone.hasExistingPart(part)) return stone.existingPart(part);
        return ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, part.registryName(stone));
    }

    private static List<String> readPattern(ResourceLocation texture) {
        try (InputStream input = openTexture(texture)) {
            if (input == null) {
                throw new IllegalStateException("Could not open Stone Shaping mask texture " + texture);
            }
            BufferedImage image = ImageIO.read(input);
            if (image == null) throw new IllegalStateException("Could not decode Stone Shaping mask texture " + texture);
            if (image.getWidth() != 16 || image.getHeight() != 16) {
                throw new IllegalStateException("Stone Shaping mask must be exactly 16x16: " + texture
                        + " is " + image.getWidth() + "x" + image.getHeight());
            }
            List<String> rows = new ArrayList<>(16);
            for (int y = 0; y < 16; y++) {
                StringBuilder row = new StringBuilder(16);
                for (int x = 0; x < 16; x++) {
                    int alpha = (image.getRGB(x, y) >>> 24) & 0xFF;
                    row.append(alpha == 0 ? '.' : '#');
                }
                rows.add(row.toString());
            }
            return List.copyOf(rows);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read Stone Shaping mask " + texture, exception);
        }
    }

    private static InputStream openTexture(ResourceLocation texture) throws IOException {
        String relative = "assets/" + texture.getNamespace() + "/textures/" + texture.getPath() + ".png";
        InputStream classpath = StoneShapingRecipeProvider.class.getClassLoader().getResourceAsStream(relative);
        if (classpath != null) return classpath;

        Path current = Path.of("").toAbsolutePath().normalize();
        for (int depth = 0; depth < 10 && current != null; depth++, current = current.getParent()) {
            for (Path root : List.of(current.resolve("src/main/resources"), current.resolve("main/resources"))) {
                Path candidate = root.resolve(relative);
                if (Files.isRegularFile(candidate)) return Files.newInputStream(candidate);
            }
        }
        return null;
    }

    @Override
    public String getName() {
        return "Industron Stone Shaping Recipes";
    }
}
