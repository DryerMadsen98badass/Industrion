package net.mads.industron.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.mads.industron.Industron;
import net.mads.industron.machine.foundry.CastingGeometry;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.MaterialTextures;
import net.mads.industron.material.defenitions.IndustrialMaterials;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Shared geometry with per-clay brick textures; both placed and inventory models use it. */
public final class CastingModelProvider {
    private CastingModelProvider() {}

    public static void generate(CachedOutput cache, Path assets, List<CompletableFuture<?>> futures,
                                StructureMaterialBlockStateProvider resolver) {
        String[] names = {"caster", "faucet"};
        List<List<CastingGeometry.Cuboid>> shapes = List.of(CastingGeometry.CASTER,
                CastingGeometry.FAUCET);
        for (int i = 0; i < names.length; i++) {
            save(cache, assets, futures, "models/block/foundry/" + names[i] + "_template.json", template(shapes.get(i)));
        }
        for (var clay : IndustrialMaterials.ALL) {
            if (clay.supportsCeramicMolds()) {
                for (var form : net.mads.industron.machine.foundry.casting.CastingDefinitions.ALL) {
                    saveMoldModel(cache, assets, futures, clay, form.mold(),
                            net.mads.industron.machine.foundry.casting.CastingDefinitions.moldTexture(form));
                }
                for (var pending : net.mads.industron.machine.foundry.casting.CastingDefinitions.PENDING_MOLDS) {
                    saveMoldModel(cache, assets, futures, clay, pending.mold(),
                            net.mads.industron.machine.foundry.casting.CastingDefinitions.moldTexture(pending));
                }
                for (var definition : net.mads.industron.machine.foundry.casting.TerracottaMoldDefinitions.ALL) {
                    String texture = net.mads.industron.machine.foundry.casting.CastingDefinitions.moldTexture(definition.coldPart());
                    saveMoldItemModel(cache, assets, futures,
                            net.mads.industron.machine.foundry.CastingRegistry.unfiredMoldId(clay, definition.moldPart()), texture);
                    saveMoldItemModel(cache, assets, futures,
                            net.mads.industron.machine.foundry.CastingRegistry.driedUnfiredMoldId(clay, definition.moldPart()), texture);
                }
            }
            if (!clay.isClayMaterial()) continue;
            String side, top, bottom;
            if (clay.hasExistingPart(MaterialPart.BRICKS)) {
                var existing = resolver.existingBrickTextures(clay.existingPart(MaterialPart.BRICKS));
                side = existing.side().toString(); top = existing.top().toString(); bottom = existing.bottom().toString();
            } else {
                side = (clay.hasCustomPartTexture(MaterialPart.BRICKS) ? clay.customPartTexture(MaterialPart.BRICKS)
                        : MaterialTextures.blockTexture(clay, MaterialPart.BRICKS).orElseThrow(
                        () -> new IllegalStateException("Missing brick texture for " + clay.id()))).toString();
                top = side; bottom = side;
            }
            for (String name : names) {
                String id = clay.id() + "_" + name;
                String modelId = Industron.MOD_ID + ":block/foundry/" + id;
                JsonObject model = new JsonObject();
                model.addProperty("parent", Industron.MOD_ID + ":block/foundry/" + name + "_template");
                JsonObject textures = new JsonObject();
                textures.addProperty("brick", side);
                textures.addProperty("top", top);
                textures.addProperty("bottom", bottom);
                textures.addProperty("particle", side);
                model.add("textures", textures);
                save(cache, assets, futures, "models/block/foundry/" + id + ".json", model);
                JsonObject item = new JsonObject();
                item.addProperty("parent", modelId);
                save(cache, assets, futures, "models/item/" + id + ".json", item);
                JsonObject variants = new JsonObject();
                String[] directions = {"north", "east", "south", "west"};
                for (int d = 0; d < 4; d++) variants.add("facing=" + directions[d], variant(modelId, 0, d * 90));
                JsonObject state = new JsonObject();
                state.add("variants", variants);
                save(cache, assets, futures, "blockstates/" + id + ".json", state);
            }
        }
    }

    private static void saveMoldModel(
            CachedOutput cache,
            Path assets,
            List<CompletableFuture<?>> futures,
            net.mads.industron.material.IndustrialMaterial clay,
            MaterialPart moldPart,
            String moldTexture
    ) {
        saveMoldItemModel(cache, assets, futures,
                net.mads.industron.machine.foundry.CastingRegistry.moldId(clay, moldPart), moldTexture);
    }

    private static void saveMoldItemModel(
            CachedOutput cache,
            Path assets,
            List<CompletableFuture<?>> futures,
            String itemId,
            String moldTexture
    ) {
        if (moldTexture == null) {
            // Registered molds stay visible even before dedicated artwork exists.
            moldTexture = "industron:item/material_sets/empty_mold/variant_1/base";
        }
        JsonObject moldModel = new JsonObject();
        moldModel.addProperty("parent", "minecraft:item/generated");
        JsonObject layers = new JsonObject();
        layers.addProperty("layer0", moldTexture);
        moldModel.add("textures", layers);
        save(cache, assets, futures, "models/item/" + itemId + ".json", moldModel);
    }

    private static void save(CachedOutput cache, Path assets, List<CompletableFuture<?>> futures, String path, JsonObject json) {
        futures.add(DataProvider.saveStable(cache, json, assets.resolve(path)));
    }
    private static JsonObject variant(String model, int x, int y) {
        JsonObject result = new JsonObject();
        result.addProperty("model", model);
        result.addProperty("x", x);
        result.addProperty("y", y);
        return result;
    }
    public static JsonObject template(List<CastingGeometry.Cuboid> geometry) {
        JsonObject model = new JsonObject();
        model.addProperty("parent", "minecraft:block/block");
        model.addProperty("ambientocclusion", true);
        JsonArray elements = new JsonArray();
        for (var box : geometry) {
            JsonObject element = new JsonObject();
            element.add("from", vector(box.x0(), box.y0(), box.z0()));
            element.add("to", vector(box.x1(), box.y1(), box.z1()));
            JsonObject faces = new JsonObject();
            for (String side : List.of("down", "up", "north", "south", "west", "east")) {
                JsonObject face = new JsonObject();
                face.addProperty("texture", side.equals("up") ? "#top" : side.equals("down") ? "#bottom" : "#brick");
                face.addProperty("tintindex", 0);
                // Default element UVs retain pixel density instead of squeezing a full block onto each post.
                faces.add(side, face);
            }
            element.add("faces", faces);
            elements.add(element);
        }
        model.add("elements", elements);
        return model;
    }
    private static JsonArray vector(double x, double y, double z) {
        JsonArray result = new JsonArray();
        result.add(x); result.add(y); result.add(z);
        return result;
    }
}
