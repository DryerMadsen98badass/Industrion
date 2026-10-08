package net.mads.industron.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.structure.StoneMaterial;
import net.mads.industron.material.structure.StructureBlockDefinition;
import net.mads.industron.material.structure.StructureMaterialGenerator;
import net.mads.industron.material.structure.StructureSetResolver;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Shared resolver for a StoneMaterial's visible stone surface.
 *
 * <p>If the material declares {@code .existing(MaterialPart.STONE, ...)}, this follows the
 * existing blockstate/model chain and returns the actual texture used by that block. Otherwise
 * it returns the normal generated StructureMaterial stone texture. The same rule is intentionally
 * shared by generated slabs/stairs/walls, Pebbles, Stone Shaping and stone tool-part textures.</p>
 */
public final class StoneTextureResolver {
    private final ExistingFileHelper existingFileHelper;

    public StoneTextureResolver(ExistingFileHelper existingFileHelper) {
        this.existingFileHelper = existingFileHelper;
    }

    public ResourceLocation baseSideTexture(StoneMaterial stone) {
        if (stone.hasExistingPart(MaterialPart.STONE)) {
            return existingTextures(stone.existingPart(MaterialPart.STONE))
                    .orElseThrow(() -> new IllegalStateException(
                            "Could not resolve texture from existing STONE block "
                                    + stone.existingPart(MaterialPart.STONE) + " for " + stone.id()
                    ))
                    .side();
        }
        return StructureSetResolver.generatedTexture(stone, stone.model().baseSideTexture());
    }


    /**
     * Resolves the visible textures for a specific StoneMaterial MaterialPart.
     * Existing parts follow the referenced blockstate/model chain; generated parts use the
     * generated structure-material textures declared by that MaterialPart.
     */
    public Optional<ExistingTextures> partTextures(StoneMaterial stone, MaterialPart part) {
        if (stone == null || part == null || !part.isBlock() || stone.isWithout(part)) {
            return Optional.empty();
        }
        if (stone.hasExistingPart(part)) {
            return existingTextures(stone.existingPart(part));
        }
        return StructureMaterialGenerator.blockDefinitions(stone).stream()
                .filter(definition -> definition.part().orElse(null) == part)
                .findFirst()
                .map(definition -> generatedTextures(stone, definition));
    }

    /** Resolves a preferred MaterialPart and falls back only when that part does not exist. */
    public Optional<ExistingTextures> partTextures(
            StoneMaterial stone,
            MaterialPart preferred,
            MaterialPart... fallbacks
    ) {
        Optional<ExistingTextures> resolved = partTextures(stone, preferred);
        if (resolved.isPresent()) return resolved;
        if (fallbacks == null) return Optional.empty();
        for (MaterialPart fallback : fallbacks) {
            resolved = partTextures(stone, fallback);
            if (resolved.isPresent()) return resolved;
        }
        return Optional.empty();
    }

    private static ExistingTextures generatedTextures(
            StoneMaterial stone,
            StructureBlockDefinition definition
    ) {
        ResourceLocation side = StructureSetResolver.generatedTexture(stone, definition.textureFile());
        ResourceLocation top = StructureSetResolver.generatedTexture(
                stone,
                definition.topTextureFile().orElse(definition.textureFile())
        );
        ResourceLocation bottom = StructureSetResolver.generatedTexture(
                stone,
                definition.bottomTextureFile().orElse(definition.topTextureFile().orElse(definition.textureFile()))
        );
        return new ExistingTextures(side, top, bottom);
    }

    public Optional<ExistingTextures> existingTextures(StoneMaterial stone, MaterialPart part) {
        if (stone == null || part == null || !stone.hasExistingPart(part)) {
            return Optional.empty();
        }
        return existingTextures(stone.existingPart(part));
    }

    public Optional<ExistingTextures> existingTextures(ResourceLocation blockId) {
        try {
            ResourceLocation modelId = blockModel(blockId);
            if (modelId == null) return Optional.empty();

            Map<String, String> textures = new HashMap<>();
            collectModelTextures(modelId, textures, new HashSet<>());

            ResourceLocation all = resolvedTexture(textures, "all").orElse(null);
            ResourceLocation side = resolvedTexture(textures, "side")
                    .or(() -> resolvedTexture(textures, "wall"))
                    .or(() -> resolvedTexture(textures, "texture"))
                    .or(() -> Optional.ofNullable(all))
                    .or(() -> resolvedTexture(textures, "particle"))
                    .orElse(null);
            if (side == null) return Optional.empty();

            ResourceLocation top = resolvedTexture(textures, "top")
                    .or(() -> resolvedTexture(textures, "end"))
                    .orElse(side);
            ResourceLocation bottom = resolvedTexture(textures, "bottom").orElse(top);
            return Optional.of(new ExistingTextures(side, top, bottom));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    public BufferedImage readTexture(ResourceLocation texture) throws IOException {
        try (InputStream input = openClientResource(texture, "textures", ".png")) {
            if (input == null) {
                throw new IllegalStateException("Could not open stone texture " + texture);
            }
            BufferedImage image = ImageIO.read(input);
            if (image == null) {
                throw new IllegalStateException("Could not decode stone texture " + texture);
            }
            return image;
        }
    }

    private ResourceLocation blockModel(ResourceLocation blockId) throws Exception {
        try (InputStream input = openClientResource(blockId, "blockstates", ".json")) {
            if (input == null) return null;
            JsonObject root = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonElement model = null;

            JsonObject variants = root.has("variants") && root.get("variants").isJsonObject()
                    ? root.getAsJsonObject("variants")
                    : null;
            if (variants != null) {
                if (variants.has("")) {
                    model = variants.get("");
                } else if (!variants.entrySet().isEmpty()) {
                    model = variants.entrySet().iterator().next().getValue();
                }
            }

            if (model == null && root.has("multipart") && root.get("multipart").isJsonArray()) {
                for (JsonElement part : root.getAsJsonArray("multipart")) {
                    if (part.isJsonObject() && part.getAsJsonObject().has("apply")) {
                        model = part.getAsJsonObject().get("apply");
                        break;
                    }
                }
            }

            if (model == null) return null;
            if (model.isJsonArray()) {
                if (model.getAsJsonArray().isEmpty()) return null;
                model = model.getAsJsonArray().get(0);
            }
            if (model.isJsonObject() && model.getAsJsonObject().has("model")) {
                return ResourceLocation.tryParse(model.getAsJsonObject().get("model").getAsString());
            }
            return null;
        }
    }

    private void collectModelTextures(ResourceLocation modelId, Map<String, String> textures, Set<ResourceLocation> visited)
            throws Exception {
        if (!visited.add(modelId)) return;
        try (InputStream input = openClientResource(modelId, "models", ".json")) {
            if (input == null) {
                throw new IllegalStateException("Missing existing model resource " + modelId);
            }
            JsonObject model = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            if (model.has("parent")) {
                ResourceLocation parent = ResourceLocation.tryParse(model.get("parent").getAsString());
                if (parent != null) {
                    try {
                        collectModelTextures(parent, textures, visited);
                    } catch (Exception ignored) {
                        // Geometry-only parents do not need to contribute texture values.
                    }
                }
            }
            if (model.has("textures") && model.get("textures").isJsonObject()) {
                for (var entry : model.getAsJsonObject("textures").entrySet()) {
                    if (entry.getValue().isJsonPrimitive()) {
                        textures.put(entry.getKey(), entry.getValue().getAsString());
                    }
                }
            }
        }
    }

    private InputStream openClientResource(ResourceLocation id, String folder, String suffix) {
        String path = "assets/" + id.getNamespace() + "/" + folder + "/" + id.getPath() + suffix;
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        InputStream stream = contextLoader == null ? null : contextLoader.getResourceAsStream(path);
        if (stream == null) {
            stream = StoneTextureResolver.class.getClassLoader().getResourceAsStream(path);
        }
        if (stream != null) return stream;

        try {
            Resource resource = existingFileHelper.getResource(id, PackType.CLIENT_RESOURCES, suffix, folder);
            return resource.open();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Optional<ResourceLocation> resolvedTexture(Map<String, String> textures, String key) {
        String value = textures.get(key);
        Set<String> visited = new HashSet<>();
        while (value != null && value.startsWith("#")) {
            String next = value.substring(1);
            if (!visited.add(next)) return Optional.empty();
            value = textures.get(next);
        }
        return Optional.ofNullable(value).map(ResourceLocation::tryParse);
    }

    public record ExistingTextures(ResourceLocation side, ResourceLocation top, ResourceLocation bottom) {}
}
