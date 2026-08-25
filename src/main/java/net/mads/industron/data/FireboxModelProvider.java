package net.mads.industron.data;

import com.google.gson.JsonObject;
import net.mads.industron.Industron;
import net.mads.industron.block.ActiveBlockDefinition;
import net.mads.industron.block.SimpleBlocks;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class FireboxModelProvider implements DataProvider {
    private final PackOutput output;

    public FireboxModelProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        Path assets = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve(Industron.MOD_ID);
        Path blockstates = assets.resolve("blockstates");
        Path blockModels = assets.resolve("models").resolve("block").resolve("casings").resolve("active");
        Path itemModels = assets.resolve("models").resolve("item");

        for (ActiveBlockDefinition definition : SimpleBlocks.ACTIVE) {
            futures.add(DataProvider.saveStable(cache, blockstate(definition), blockstates.resolve(definition.id() + ".json")));
            futures.add(DataProvider.saveStable(cache, blockModel(definition.idleTexture().toString()), blockModels.resolve(definition.id() + "_idle.json")));
            for (int frame = 0; frame < definition.activeFrameCount(); frame++) {
                futures.add(DataProvider.saveStable(cache, blockModel(definition.activeTexture(frame).toString()), blockModels.resolve(definition.id() + "_active_" + (frame + 1) + ".json")));
            }
            futures.add(DataProvider.saveStable(cache, itemModel(definition), itemModels.resolve(definition.id() + ".json")));
        }

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Industron Active Block Models";
    }

    private static JsonObject blockstate(ActiveBlockDefinition definition) {
        JsonObject variants = new JsonObject();
        for (int frame = 0; frame < ActiveBlockDefinition.MAX_ACTIVE_FRAMES; frame++) {
            variants.add("active=false,overlay_frame=" + frame, variant(definition.idleModelPath()));
            variants.add("active=true,overlay_frame=" + frame, variant(definition.activeModelPath(frame)));
        }
        JsonObject json = new JsonObject();
        json.add("variants", variants);
        return json;
    }

    private static JsonObject variant(String modelPath) {
        JsonObject variant = new JsonObject();
        variant.addProperty("model", Industron.MOD_ID + ":" + modelPath);
        return variant;
    }

    private static JsonObject blockModel(String texture) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", "minecraft:block/cube_all");
        JsonObject textures = new JsonObject();
        textures.addProperty("all", texture);
        textures.addProperty("particle", texture);
        json.add("textures", textures);
        return json;
    }

    private static JsonObject itemModel(ActiveBlockDefinition definition) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", Industron.MOD_ID + ":" + definition.idleModelPath());
        return json;
    }
}
