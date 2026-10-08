package net.mads.industron.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.mads.industron.Industron;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.defenitions.StoneMaterials;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Models for worldgen-only loose Pebbles, using the same resolved STONE texture as generated stone shapes. */
public final class PebbleWorldgenModelProvider implements DataProvider {
    private final Path resourceRoot;
    private final StoneTextureResolver stoneTextureResolver;

    public PebbleWorldgenModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        this.resourceRoot = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve(Industron.MOD_ID);
        this.stoneTextureResolver = new StoneTextureResolver(existingFileHelper);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (var stone : StoneMaterials.ALL) {
            if (!stone.generatedForms().contains(MaterialPart.PEBBLE)) continue;
            String blockId = stone.id() + "_loose_pebble";
            ResourceLocation texture = stoneTextureResolver.baseSideTexture(stone);
            ResourceLocation modelId = ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, "block/loose_pebble/" + stone.id());
            futures.add(DataProvider.saveStable(output, model(texture), resourceRoot.resolve("models").resolve(modelId.getPath() + ".json")));
            futures.add(DataProvider.saveStable(output, blockState(modelId), resourceRoot.resolve("blockstates").resolve(blockId + ".json")));
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private static JsonObject blockState(ResourceLocation model) {
        JsonObject root = new JsonObject();
        JsonObject variants = new JsonObject();
        JsonArray choices = new JsonArray();
        for (int y : new int[]{0, 90, 180, 270}) {
            JsonObject choice = new JsonObject();
            choice.addProperty("model", model.toString());
            if (y != 0) choice.addProperty("y", y);
            choices.add(choice);
        }
        variants.add("", choices);
        root.add("variants", variants);
        return root;
    }

    private static JsonObject model(ResourceLocation texture) {
        JsonObject root = new JsonObject();
        root.addProperty("parent", "minecraft:block/block");
        root.addProperty("ambientocclusion", false);
        JsonObject textures = new JsonObject();
        textures.addProperty("particle", texture.toString());
        textures.addProperty("all", texture.toString());
        root.add("textures", textures);

        JsonObject element = new JsonObject();
        element.add("from", vec(4, 0, 4));
        element.add("to", vec(12, 3, 12));
        JsonObject faces = new JsonObject();
        faces.add("down", face(4, 4, 12, 12));
        faces.add("up", face(4, 4, 12, 12));
        faces.add("north", face(4, 13, 12, 16));
        faces.add("south", face(4, 13, 12, 16));
        faces.add("west", face(4, 13, 12, 16));
        faces.add("east", face(4, 13, 12, 16));
        element.add("faces", faces);
        JsonArray elements = new JsonArray();
        elements.add(element);
        root.add("elements", elements);
        return root;
    }

    private static JsonArray vec(int a, int b, int c) {
        JsonArray array = new JsonArray();
        array.add(a); array.add(b); array.add(c);
        return array;
    }

    private static JsonObject face(int u1, int v1, int u2, int v2) {
        JsonObject face = new JsonObject();
        JsonArray uv = new JsonArray();
        uv.add(u1); uv.add(v1); uv.add(u2); uv.add(v2);
        face.add("uv", uv);
        face.addProperty("texture", "#all");
        return face;
    }

    @Override
    public String getName() {
        return "Industron Pebble Worldgen Models";
    }
}
