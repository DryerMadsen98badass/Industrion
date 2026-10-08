package net.mads.industron.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.mads.industron.Industron;
import net.mads.industron.machine.foundry.FoundryPartType;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Generates Foundry controller/bus/hatch models from existing textures only; no PNGs are created. */
public final class FoundryModelProvider implements DataProvider {
    private static final String FALLBACK_BRICK_TEXTURE =
            Industron.MOD_ID + ":block/material_sets/bricks/normal/variant_1/base";

    private final PackOutput output;
    private final StructureMaterialBlockStateProvider textureResolver;

    public FoundryModelProvider(PackOutput output, net.neoforged.neoforge.common.data.ExistingFileHelper helper) {
        this.output = output;
        this.textureResolver = new StructureMaterialBlockStateProvider(output, helper);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        Path assets = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve(Industron.MOD_ID);
        Path blockstates = assets.resolve("blockstates");
        Path blockModels = assets.resolve("models/block");
        Path itemModels = assets.resolve("models/item");

        for (FoundryPartType type : FoundryPartType.ALL) {
            String modelName = "foundry/" + type.id();
            futures.add(DataProvider.saveStable(
                    cache,
                    model(type),
                    blockModels.resolve(modelName + ".json")
            ));
            futures.add(DataProvider.saveStable(
                    cache,
                    facingBlockstate(modelName),
                    blockstates.resolve(type.id() + ".json")
            ));
            futures.add(DataProvider.saveStable(
                    cache,
                    itemModel(modelName),
                    itemModels.resolve(type.id() + ".json")
            ));
        }

        CastingModelProvider.generate(cache, assets, futures, textureResolver);
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Industron Foundry Models";
    }

    private static JsonObject model(FoundryPartType type) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", "minecraft:block/block");
        json.addProperty("render_type", "minecraft:cutout");

        JsonObject textures = new JsonObject();
        textures.addProperty("casing", FALLBACK_BRICK_TEXTURE);
        textures.addProperty("overlay", Industron.MOD_ID + ":" + type.overlayTexture());
        textures.addProperty("particle", FALLBACK_BRICK_TEXTURE);
        json.add("textures", textures);

        JsonArray elements = new JsonArray();
        elements.add(casingElement());
        elements.add(overlayElement());
        json.add("elements", elements);
        return json;
    }

    private static JsonObject casingElement() {
        JsonObject element = element(0, 0, 0, 16, 16, 16);
        JsonObject faces = new JsonObject();
        addTintedFace(faces, "down", "#casing", "down");
        addTintedFace(faces, "up", "#casing", "up");
        addTintedFace(faces, "north", "#casing", "north");
        addTintedFace(faces, "south", "#casing", "south");
        addTintedFace(faces, "west", "#casing", "west");
        addTintedFace(faces, "east", "#casing", "east");
        element.add("faces", faces);
        return element;
    }

    /** Overlay sits farther out than the dynamic formed casing renderer. */
    private static JsonObject overlayElement() {
        JsonObject element = element(0, 0, -0.03, 16, 16, -0.02);
        JsonObject faces = new JsonObject();
        JsonObject face = new JsonObject();
        face.addProperty("texture", "#overlay");
        faces.add("north", face);
        element.add("faces", faces);
        return element;
    }

    private static JsonObject facingBlockstate(String modelName) {
        JsonObject variants = new JsonObject();
        variants.add("facing=down", variant(modelName, 90, 0));
        variants.add("facing=north", variant(modelName, 0, 0));
        variants.add("facing=east", variant(modelName, 0, 90));
        variants.add("facing=south", variant(modelName, 0, 180));
        variants.add("facing=west", variant(modelName, 0, 270));
        variants.add("facing=up", variant(modelName, 270, 0));

        JsonObject json = new JsonObject();
        json.add("variants", variants);
        return json;
    }

    private static JsonObject variant(String modelName, int xRotation, int yRotation) {
        JsonObject variant = new JsonObject();
        variant.addProperty("model", Industron.MOD_ID + ":block/" + modelName);
        if (xRotation != 0) {
            variant.addProperty("x", xRotation);
        }
        if (yRotation != 0) {
            variant.addProperty("y", yRotation);
        }
        return variant;
    }

    private static JsonObject itemModel(String modelName) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", Industron.MOD_ID + ":block/" + modelName);
        return json;
    }

    private static JsonObject element(double fromX, double fromY, double fromZ, double toX, double toY, double toZ) {
        JsonObject element = new JsonObject();
        JsonArray from = new JsonArray();
        from.add(fromX);
        from.add(fromY);
        from.add(fromZ);
        JsonArray to = new JsonArray();
        to.add(toX);
        to.add(toY);
        to.add(toZ);
        element.add("from", from);
        element.add("to", to);
        return element;
    }

    private static void addTintedFace(JsonObject faces, String side, String texture, String cullFace) {
        JsonObject face = new JsonObject();
        face.addProperty("texture", texture);
        face.addProperty("cullface", cullFace);
        face.addProperty("tintindex", 0);
        faces.add(side, face);
    }
}
