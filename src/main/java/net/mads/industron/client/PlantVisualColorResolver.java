package net.mads.industron.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import net.mads.industron.material.CompositionColor;
import net.mads.industron.material.plant.PlantMaterial;
import net.mads.industron.material.plant.PlantProcessIntermediate;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Client-only visual color resolver for generated plant forms.
 *
 * <p>Priority is intentional: explicit {@code .color(...)} wins. Otherwise the color is sampled
 * from the main {@code .existing(...)} Minecraft item/model. Only when no existing visual can be
 * resolved do we fall back to the declared {@code .contains(...)} composition.</p>
 */
public final class PlantVisualColorResolver {
    private static final Map<String, Integer> PLANT_CACHE = new HashMap<>();
    private static final int MAX_MODEL_DEPTH = 16;

    private PlantVisualColorResolver() {
    }

    public static int colorFor(PlantMaterial material) {
        if (material.hasColor()) return material.optionalColor().orElse(0xFFFFFF);
        return PLANT_CACHE.computeIfAbsent(material.id(), ignored -> resolveExistingColor(material));
    }

    public static int colorFor(PlantProcessIntermediate intermediate) {
        // States that still contain the complete parent material should visually remain that plant.
        if (intermediate.components().equals(intermediate.parent().components())) {
            return colorFor(intermediate.parent());
        }
        // A separated fraction has its own chemistry and therefore its own composition color.
        return CompositionColor.blend(intermediate.components());
    }

    private static int resolveExistingColor(PlantMaterial material) {
        Optional<ResourceLocation> source = material.mainExistingVisual();
        if (source.isEmpty()) return CompositionColor.blend(material.components());

        ResourceLocation itemId = source.get();
        Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
        if (item == null) return CompositionColor.blend(material.components());

        Integer sampled = sampleItemModel(itemId);
        if (sampled == null) return CompositionColor.blend(material.components());

        // Preserve vanilla item tinting where the source itself uses it (for example foliage).
        int vanillaTint = Minecraft.getInstance().getItemColors().getColor(new ItemStack(item), 0);
        if (vanillaTint != -1 && vanillaTint != 0xFFFFFFFF) {
            sampled = multiplyRgb(sampled, vanillaTint);
        }
        return sampled;
    }

    private static Integer sampleItemModel(ResourceLocation itemId) {
        ResourceLocation model = ResourceLocation.fromNamespaceAndPath(
                itemId.getNamespace(), "item/" + itemId.getPath()
        );
        Map<String, String> textures = new LinkedHashMap<>();
        if (!collectModelTextures(model, textures, 0)) return null;

        String[] preferred = {"layer0", "all", "side", "particle", "top", "end", "texture"};
        for (String key : preferred) {
            String value = resolveTextureAlias(textures, textures.get(key));
            Integer color = sampleTexture(value);
            if (color != null) return color;
        }
        for (String raw : textures.values()) {
            String value = resolveTextureAlias(textures, raw);
            Integer color = sampleTexture(value);
            if (color != null) return color;
        }
        return null;
    }

    private static boolean collectModelTextures(
            ResourceLocation model,
            Map<String, String> textures,
            int depth
    ) {
        if (depth > MAX_MODEL_DEPTH) return false;
        ResourceLocation jsonId = ResourceLocation.fromNamespaceAndPath(
                model.getNamespace(), "models/" + model.getPath() + ".json"
        );
        var resource = Minecraft.getInstance().getResourceManager().getResource(jsonId).orElse(null);
        if (resource == null) return false;

        try (var reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            if (json.has("parent")) {
                ResourceLocation parent = ResourceLocation.tryParse(json.get("parent").getAsString());
                if (parent != null && !parent.equals(model)) {
                    collectModelTextures(parent, textures, depth + 1);
                }
            }
            if (json.has("textures") && json.get("textures").isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("textures").entrySet()) {
                    if (entry.getValue().isJsonPrimitive()) {
                        textures.put(entry.getKey(), entry.getValue().getAsString());
                    }
                }
            }
            return !textures.isEmpty();
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String resolveTextureAlias(Map<String, String> textures, String value) {
        String current = value;
        for (int i = 0; i < 16 && current != null && current.startsWith("#"); i++) {
            current = textures.get(current.substring(1));
        }
        return current;
    }

    private static Integer sampleTexture(String texture) {
        if (texture == null || texture.isBlank() || texture.startsWith("#")) return null;
        ResourceLocation id = ResourceLocation.tryParse(texture);
        if (id == null) return null;
        ResourceLocation png = ResourceLocation.fromNamespaceAndPath(
                id.getNamespace(), "textures/" + id.getPath() + ".png"
        );
        var resource = Minecraft.getInstance().getResourceManager().getResource(png).orElse(null);
        if (resource == null) return null;

        try (var input = resource.open(); NativeImage image = NativeImage.read(input)) {
            long red = 0L;
            long green = 0L;
            long blue = 0L;
            long weight = 0L;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int pixel = image.getPixelRGBA(x, y);
                    int alpha = FastColor.ABGR32.alpha(pixel);
                    if (alpha <= 8) continue;
                    red += (long) FastColor.ABGR32.red(pixel) * alpha;
                    green += (long) FastColor.ABGR32.green(pixel) * alpha;
                    blue += (long) FastColor.ABGR32.blue(pixel) * alpha;
                    weight += alpha;
                }
            }
            if (weight <= 0L) return null;
            int r = clamp((int) Math.round((double) red / weight));
            int g = clamp((int) Math.round((double) green / weight));
            int b = clamp((int) Math.round((double) blue / weight));
            return (r << 16) | (g << 8) | b;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static int multiplyRgb(int first, int second) {
        int r = (((first >> 16) & 0xFF) * ((second >> 16) & 0xFF)) / 255;
        int g = (((first >> 8) & 0xFF) * ((second >> 8) & 0xFF)) / 255;
        int b = ((first & 0xFF) * (second & 0xFF)) / 255;
        return (r << 16) | (g << 8) | b;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
