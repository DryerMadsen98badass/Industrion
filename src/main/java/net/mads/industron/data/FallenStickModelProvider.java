package net.mads.industron.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.mads.industron.Industron;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.defenitions.WoodMaterials;
import net.mads.industron.material.structure.StructureMaterialVariantResolver;
import net.mads.industron.material.structure.WoodMaterial;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Tiny extruded ground models for fallen WoodMaterial sticks. */
public final class FallenStickModelProvider implements DataProvider {
    private static final double SCALE = 0.25D;
    private static final double THICKNESS = 0.25D;

    private final Path resourceRoot;

    public FallenStickModelProvider(PackOutput output) {
        this.resourceRoot = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve(Industron.MOD_ID);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        boolean[][] stickMask = loadStickMask();
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (WoodMaterial wood : WoodMaterials.ALL) {
            ResourceLocation texture = wood.hasExistingPart(MaterialPart.STICK)
                    ? existingItemTexture(wood.existingPart(MaterialPart.STICK))
                    : StructureMaterialVariantResolver.generatedWoodStickTexture(wood);
            String blockId = wood.id() + "_fallen_stick";

            ResourceLocation[] models = new ResourceLocation[16];
            for (int position = 0; position < 16; position++) {
                ResourceLocation modelId = ResourceLocation.fromNamespaceAndPath(
                        Industron.MOD_ID, "block/fallen_stick/" + wood.id() + "/position_" + position
                );
                models[position] = modelId;
                futures.add(DataProvider.saveStable(output, model(texture, position, stickMask),
                        resourceRoot.resolve("models").resolve(modelId.getPath() + ".json")));
            }
            futures.add(DataProvider.saveStable(output, blockState(models),
                    resourceRoot.resolve("blockstates").resolve(blockId + ".json")));
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private static ResourceLocation existingItemTexture(ResourceLocation itemId) {
        return ResourceLocation.fromNamespaceAndPath(itemId.getNamespace(), "item/" + itemId.getPath());
    }

    private static JsonObject blockState(ResourceLocation[] models) {
        JsonObject root = new JsonObject();
        JsonObject variants = new JsonObject();
        String[] facings = {"north", "east", "south", "west"};
        int[] rotations = {0, 90, 180, 270};
        for (int position = 0; position < 16; position++) {
            for (int i = 0; i < facings.length; i++) {
                JsonObject variant = new JsonObject();
                variant.addProperty("model", models[position].toString());
                if (rotations[i] != 0) variant.addProperty("y", rotations[i]);
                variants.add("facing=" + facings[i] + ",position=" + position, variant);
            }
        }
        root.add("variants", variants);
        return root;
    }

    /**
     * Extrudes the actual opaque pixels of the 16x16 stick sprite.
     * The sprite is scaled to 25%, so each source pixel becomes a 0.25 x 0.25 model-pixel voxel,
     * and the branch is extruded by the same 0.25 model-pixel vertically.
     */
    private static JsonObject model(ResourceLocation texture, int position, boolean[][] mask) {
        JsonObject root = new JsonObject();
        root.addProperty("parent", "minecraft:block/block");
        root.addProperty("ambientocclusion", false);
        JsonObject textures = new JsonObject();
        textures.addProperty("particle", texture.toString());
        textures.addProperty("stick", texture.toString());
        root.add("textures", textures);

        double baseX = (position & 3) * 4.0D;
        double baseZ = (position >> 2) * 4.0D;
        JsonArray elements = new JsonArray();

        for (int py = 0; py < 16; py++) {
            for (int px = 0; px < 16; px++) {
                if (!mask[py][px]) continue;

                double x0 = baseX + px * SCALE;
                double z0 = baseZ + py * SCALE;
                JsonObject element = new JsonObject();
                element.add("from", vec(x0, 0.0D, z0));
                element.add("to", vec(x0 + SCALE, THICKNESS, z0 + SCALE));

                JsonObject faces = new JsonObject();
                addFace(faces, "up", px, py);
                addFace(faces, "down", px, py);
                if (!opaque(mask, px, py - 1)) addFace(faces, "north", px, py);
                if (!opaque(mask, px, py + 1)) addFace(faces, "south", px, py);
                if (!opaque(mask, px - 1, py)) addFace(faces, "west", px, py);
                if (!opaque(mask, px + 1, py)) addFace(faces, "east", px, py);
                element.add("faces", faces);
                elements.add(element);
            }
        }

        root.add("elements", elements);
        return root;
    }

    private static void addFace(JsonObject faces, String direction, int px, int py) {
        JsonObject face = new JsonObject();
        face.add("uv", uv(px, py, px + 1, py + 1));
        face.addProperty("texture", "#stick");
        faces.add(direction, face);
    }

    private static boolean opaque(boolean[][] mask, int x, int y) {
        return x >= 0 && x < 16 && y >= 0 && y < 16 && mask[y][x];
    }

    private static boolean[][] loadStickMask() {
        ResourceLocation texture = StructureMaterialVariantResolver
                .materialSetTemplateTextures(WoodMaterials.BIRCH, MaterialPart.STICK)
                .orElseThrow(() -> new IllegalStateException("Missing grayscale stick template"))
                .base();
        String path = "assets/" + texture.getNamespace() + "/textures/" + texture.getPath() + ".png";

        try (InputStream input = FallenStickModelProvider.class.getClassLoader().getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException("Could not open stick shape texture at " + path);
            }
            BufferedImage image = ImageIO.read(input);
            if (image == null) {
                throw new IllegalStateException("Could not decode stick shape texture " + texture);
            }
            if (image.getWidth() != 16 || image.getHeight() != 16) {
                throw new IllegalStateException(
                        "Fallen stick source texture must be 16x16: " + texture
                                + " is " + image.getWidth() + "x" + image.getHeight()
                );
            }

            boolean[][] mask = new boolean[16][16];
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    mask[y][x] = ((image.getRGB(x, y) >>> 24) & 0xFF) != 0;
                }
            }
            return mask;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load fallen stick shape texture " + texture, exception);
        }
    }

    private static JsonArray vec(double a, double b, double c) {
        JsonArray array = new JsonArray();
        array.add(a); array.add(b); array.add(c);
        return array;
    }

    private static JsonArray uv(double a, double b, double c, double d) {
        JsonArray array = new JsonArray();
        array.add(a); array.add(b); array.add(c); array.add(d);
        return array;
    }

    @Override
    public String getName() {
        return "Industron Fallen Stick Models";
    }
}
