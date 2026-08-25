package net.mads.industron.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.mads.industron.Industron;
import net.mads.industron.machine.coil.CoilDefinition;
import net.mads.industron.machine.coil.CoilDefinitions;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CoilModelProvider implements DataProvider {
    private final PackOutput output;

    public CoilModelProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        Path assets = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK)
                .resolve(Industron.MOD_ID);
        Path blockstates = assets.resolve("blockstates");
        Path blockModels = assets.resolve("models").resolve("block").resolve("casings").resolve("coils");
        Path itemModels = assets.resolve("models").resolve("item");

        for (CoilDefinition coil : CoilDefinitions.ALL) {
            futures.add(DataProvider.saveStable(cache, blockstate(coil), blockstates.resolve(coil.blockId() + ".json")));
            futures.add(DataProvider.saveStable(cache, blockModel(coil, false), blockModels.resolve(coil.blockId() + "_off.json")));
            futures.add(DataProvider.saveStable(cache, blockModel(coil, true), blockModels.resolve(coil.blockId() + "_on.json")));
            futures.add(DataProvider.saveStable(cache, itemModel(coil), itemModels.resolve(coil.itemId() + ".json")));
        }

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Industron Coil Models";
    }

    private static JsonObject blockstate(CoilDefinition coil) {
        JsonObject variants = new JsonObject();
        variants.add("active=false", variant(coil, false));
        variants.add("active=true", variant(coil, true));

        JsonObject json = new JsonObject();
        json.add("variants", variants);
        return json;
    }

    private static JsonObject variant(CoilDefinition coil, boolean active) {
        JsonObject variant = new JsonObject();
        variant.addProperty("model", Industron.MOD_ID + ":block/casings/coils/" + coil.blockId() + (active ? "_on" : "_off"));
        return variant;
    }

    private static JsonObject blockModel(CoilDefinition coil, boolean active) {
        String baseTexture = (active ? coil.onTexture() : coil.offTexture()).toString();
        JsonObject json = new JsonObject();
        json.addProperty("parent", "minecraft:block/block");
        json.addProperty("render_type", "minecraft:cutout");

        JsonObject textures = new JsonObject();
        textures.addProperty("base", baseTexture);
        textures.addProperty("frame", coil.frameTexture().toString());
        textures.addProperty("particle", baseTexture);
        json.add("textures", textures);

        JsonArray elements = new JsonArray();
        elements.add(cubeElement(0, 0, 0, 16, 16, 16, "#base"));
        elements.add(cubeElement(-0.01, -0.01, -0.01, 16.01, 16.01, 16.01, "#frame"));
        json.add("elements", elements);
        return json;
    }

    private static JsonObject itemModel(CoilDefinition coil) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", Industron.MOD_ID + ":block/casings/coils/" + coil.blockId() + "_off");
        return json;
    }

    private static JsonObject cubeElement(double fromX, double fromY, double fromZ, double toX, double toY, double toZ, String texture) {
        JsonObject element = new JsonObject();
        element.add("from", vec(fromX, fromY, fromZ));
        element.add("to", vec(toX, toY, toZ));

        JsonObject faces = new JsonObject();
        addFace(faces, "down", texture);
        addFace(faces, "up", texture);
        addFace(faces, "north", texture);
        addFace(faces, "south", texture);
        addFace(faces, "west", texture);
        addFace(faces, "east", texture);
        element.add("faces", faces);
        return element;
    }

    private static void addFace(JsonObject faces, String direction, String texture) {
        JsonObject face = new JsonObject();
        face.addProperty("texture", texture);
        face.addProperty("cullface", direction);
        faces.add(direction, face);
    }

    private static JsonArray vec(double x, double y, double z) {
        JsonArray array = new JsonArray();
        array.add(x);
        array.add(y);
        array.add(z);
        return array;
    }
}
