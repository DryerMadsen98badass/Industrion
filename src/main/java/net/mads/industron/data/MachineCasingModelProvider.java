package net.mads.industron.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.mads.industron.Industron;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.material.recipes.MaterialCasingGenerator;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MachineCasingModelProvider implements DataProvider {
    private final PackOutput output;

    public MachineCasingModelProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        Path assets = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK)
                .resolve(Industron.MOD_ID);
        Path blockstates = assets.resolve("blockstates");
        Path blockModels = assets.resolve("models").resolve("block");
        Path itemModels = assets.resolve("models").resolve("item");

        for (MachineTier tier : MachineTier.ALL) {
            String casingName = tier.casingRegistryName();
            futures.add(DataProvider.saveStable(cache, blockstate(casingName), blockstates.resolve(casingName + ".json")));
            futures.add(DataProvider.saveStable(cache, blockModel(tier.singleBlockMachineCasingSideTexture(), true), blockModels.resolve(casingName + ".json")));
            futures.add(DataProvider.saveStable(cache, itemModel(casingName), itemModels.resolve(casingName + ".json")));
        }

        for (MaterialCasingGenerator.GeneratedCasing generated : MaterialCasingGenerator.ALL) {
            String casingName = generated.registryName();
            futures.add(DataProvider.saveStable(cache, blockstate(casingName), blockstates.resolve(casingName + ".json")));
            futures.add(DataProvider.saveStable(
                    cache,
                    blockModel(generated.definition().texture().toString(), true),
                    blockModels.resolve(casingName + ".json")
            ));
            futures.add(DataProvider.saveStable(cache, itemModel(casingName), itemModels.resolve(casingName + ".json")));
        }

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Industron Machine Casing Models";
    }

    private static JsonObject blockstate(String casingName) {
        JsonObject model = new JsonObject();
        model.addProperty("model", Industron.MOD_ID + ":block/" + casingName);

        JsonObject variants = new JsonObject();
        variants.add("", model);

        JsonObject json = new JsonObject();
        json.add("variants", variants);
        return json;
    }

    private static JsonObject blockModel(String texture, boolean tinted) {
        JsonObject json = new JsonObject();
        if (tinted) {
            json.addProperty("parent", "minecraft:block/block");
        }

        JsonObject textures = new JsonObject();
        textures.addProperty("all", texture);
        textures.addProperty("particle", texture);
        json.add("textures", textures);

        if (tinted) {
            json.add("elements", fullCubeElements());
        } else {
            json.addProperty("parent", "minecraft:block/cube_all");
        }
        return json;
    }

    private static JsonArray fullCubeElements() {
        JsonObject element = new JsonObject();
        JsonArray from = new JsonArray();
        from.add(0);
        from.add(0);
        from.add(0);
        JsonArray to = new JsonArray();
        to.add(16);
        to.add(16);
        to.add(16);
        element.add("from", from);
        element.add("to", to);

        JsonObject faces = new JsonObject();
        addFace(faces, "north");
        addFace(faces, "south");
        addFace(faces, "east");
        addFace(faces, "west");
        addFace(faces, "up");
        addFace(faces, "down");
        element.add("faces", faces);

        JsonArray elements = new JsonArray();
        elements.add(element);
        return elements;
    }

    private static void addFace(JsonObject faces, String direction) {
        JsonObject face = new JsonObject();
        face.addProperty("texture", "#all");
        face.addProperty("cullface", direction);
        face.addProperty("tintindex", 0);
        faces.add(direction, face);
    }

    private static JsonObject itemModel(String casingName) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", Industron.MOD_ID + ":block/" + casingName);
        return json;
    }
}
