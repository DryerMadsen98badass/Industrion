package net.mads.industron.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.mads.industron.Industron;
import net.mads.industron.block.ActiveBlockDefinition;
import net.mads.industron.block.SimpleBlocks;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.MaterialTextures;
import net.mads.industron.material.defenitions.ClayMaterials;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Models for active blocks plus material-generated clay Fireboxes. */
public class FireboxModelProvider implements DataProvider {
    private static final String OFF_OVERLAY = "industron:block/casings/firebox/firebrick_firebox_off";
    private static final String ON_OVERLAY = "industron:block/casings/firebox/firebrick_firebox_on";

    private final PackOutput output;
    private final StoneTextureResolver textureResolver;

    public FireboxModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        this.output = output;
        this.textureResolver = new StoneTextureResolver(existingFileHelper);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        Path assets = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve(Industron.MOD_ID);
        Path blockstates = assets.resolve("blockstates");
        Path activeBlockModels = assets.resolve("models").resolve("block").resolve("casings").resolve("active");
        Path materialBlockModels = assets.resolve("models").resolve("block").resolve("material").resolve("firebox");
        Path itemModels = assets.resolve("models").resolve("item");

        for (ActiveBlockDefinition definition : SimpleBlocks.ACTIVE) {
            futures.add(DataProvider.saveStable(cache, activeBlockstate(definition), blockstates.resolve(definition.id() + ".json")));
            futures.add(DataProvider.saveStable(cache, simpleBlockModel(definition.idleTexture().toString()), activeBlockModels.resolve(definition.id() + "_idle.json")));
            for (int frame = 0; frame < 1; frame++) {
                futures.add(DataProvider.saveStable(cache, simpleBlockModel(definition.activeFrameCount() > 1
                        ? AnimatedMachineTextureProvider.texture(definition.activeTextures().stream().map(Object::toString).toList())
                        : definition.activeTexture(frame).toString()), activeBlockModels.resolve(definition.id() + "_active_" + (frame + 1) + ".json")));
            }
            futures.add(DataProvider.saveStable(cache, activeItemModel(definition), itemModels.resolve(definition.id() + ".json")));
        }

        for (IndustrialMaterial clay : ClayMaterials.ALL) {
            if (!clay.has(MaterialPart.FIREBOX)) continue;
            String id = MaterialPart.FIREBOX.registryName(clay);
            BaseTextures base = baseTextures(clay);
            String offModel = "block/material/firebox/" + id + "_off";
            String onModel = "block/material/firebox/" + id + "_on";
            boolean tintBase = !clay.hasExistingPart(MaterialPart.BRICKS);

            futures.add(DataProvider.saveStable(cache, materialFireboxBlockstate(offModel, onModel), blockstates.resolve(id + ".json")));
            futures.add(DataProvider.saveStable(cache, materialFireboxModel(base, OFF_OVERLAY, tintBase), materialBlockModels.resolve(id + "_off.json")));
            futures.add(DataProvider.saveStable(cache, materialFireboxModel(base, ON_OVERLAY, tintBase), materialBlockModels.resolve(id + "_on.json")));
            futures.add(DataProvider.saveStable(cache, parentModel(Industron.MOD_ID + ":" + offModel), itemModels.resolve(id + ".json")));
        }

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private BaseTextures baseTextures(IndustrialMaterial clay) {
        if (clay.hasExistingPart(MaterialPart.BRICKS)) {
            ResourceLocation blockId = clay.existingPart(MaterialPart.BRICKS);
            StoneTextureResolver.ExistingTextures textures = textureResolver.existingTextures(blockId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Could not resolve existing BRICKS textures for clay " + clay.id() + ": " + blockId
                    ));
            return new BaseTextures(textures.side().toString(), textures.top().toString(), textures.bottom().toString());
        }
        ResourceLocation texture = MaterialTextures.blockTexture(clay, MaterialPart.BRICKS)
                .orElseThrow(() -> new IllegalStateException("Missing generated BRICKS texture for clay " + clay.id()));
        return new BaseTextures(texture.toString(), texture.toString(), texture.toString());
    }

    @Override
    public String getName() {
        return "Industron Firebox Models";
    }

    private static JsonObject activeBlockstate(ActiveBlockDefinition definition) {
        JsonObject variants = new JsonObject();
        for (int frame = 0; frame < 1; frame++) {
            variants.add("active=false", variant(Industron.MOD_ID + ":" + definition.idleModelPath()));
            variants.add("active=true", variant(Industron.MOD_ID + ":" + definition.activeModelPath(frame)));
        }
        JsonObject json = new JsonObject();
        json.add("variants", variants);
        return json;
    }

    private static JsonObject materialFireboxBlockstate(String offModel, String onModel) {
        JsonObject variants = new JsonObject();
        addMaterialFireboxFacing(variants, offModel, onModel, "north", 0);
        addMaterialFireboxFacing(variants, offModel, onModel, "east", 90);
        addMaterialFireboxFacing(variants, offModel, onModel, "south", 180);
        addMaterialFireboxFacing(variants, offModel, onModel, "west", 270);
        JsonObject json = new JsonObject();
        json.add("variants", variants);
        return json;
    }

    private static void addMaterialFireboxFacing(
            JsonObject variants,
            String offModel,
            String onModel,
            String facing,
            int rotation
    ) {
        for (int frame = 0; frame < 1; frame++) {
            variants.add(
                    "facing=" + facing + ",active=false",
                    variant(Industron.MOD_ID + ":" + offModel, rotation)
            );
            variants.add(
                    "facing=" + facing + ",active=true",
                    variant(Industron.MOD_ID + ":" + onModel, rotation)
            );
        }
    }

    private static JsonObject variant(String modelPath) {
        return variant(modelPath, 0);
    }

    private static JsonObject variant(String modelPath, int yRotation) {
        JsonObject variant = new JsonObject();
        variant.addProperty("model", modelPath);
        if (yRotation != 0) variant.addProperty("y", yRotation);
        return variant;
    }

    private static JsonObject simpleBlockModel(String texture) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", "minecraft:block/cube_all");
        JsonObject textures = new JsonObject();
        textures.addProperty("all", texture);
        textures.addProperty("particle", texture);
        json.add("textures", textures);
        return json;
    }

    private static JsonObject materialFireboxModel(BaseTextures base, String overlay, boolean tintBase) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", "minecraft:block/block");
        json.addProperty("render_type", "minecraft:cutout");
        JsonObject textures = new JsonObject();
        textures.addProperty("particle", base.side());
        textures.addProperty("side", base.side());
        textures.addProperty("top", base.top());
        textures.addProperty("bottom", base.bottom());
        textures.addProperty("overlay", overlay);
        json.add("textures", textures);

        JsonArray elements = new JsonArray();
        JsonObject baseCube = element(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
        JsonObject baseFaces = new JsonObject();
        baseFaces.add("down", face("#bottom", "down", tintBase));
        baseFaces.add("up", face("#top", "up", tintBase));
        baseFaces.add("north", face("#side", "north", tintBase));
        baseFaces.add("south", face("#side", "south", tintBase));
        baseFaces.add("west", face("#side", "west", tintBase));
        baseFaces.add("east", face("#side", "east", tintBase));
        baseCube.add("faces", baseFaces);
        elements.add(baseCube);

        // Bricks remain the base texture on every face. The transparent firebox overlay
        // is rendered on all four horizontal sides, never on the top or bottom.
        JsonObject overlayCube = element(-0.01D, 0.0D, -0.01D, 16.01D, 16.0D, 16.01D);
        JsonObject overlayFaces = new JsonObject();
        overlayFaces.add("north", face("#overlay", "north", false));
        overlayFaces.add("south", face("#overlay", "south", false));
        overlayFaces.add("west", face("#overlay", "west", false));
        overlayFaces.add("east", face("#overlay", "east", false));
        overlayCube.add("faces", overlayFaces);
        elements.add(overlayCube);
        json.add("elements", elements);
        return json;
    }

    private static JsonObject element(double x1, double y1, double z1, double x2, double y2, double z2) {
        JsonObject element = new JsonObject();
        JsonArray from = new JsonArray();
        from.add(x1); from.add(y1); from.add(z1);
        JsonArray to = new JsonArray();
        to.add(x2); to.add(y2); to.add(z2);
        element.add("from", from);
        element.add("to", to);
        return element;
    }

    private static JsonObject face(String texture, String cullFace, boolean tinted) {
        JsonObject face = new JsonObject();
        face.addProperty("texture", texture);
        face.addProperty("cullface", cullFace);
        if (tinted) face.addProperty("tintindex", 0);
        return face;
    }

    private static JsonObject activeItemModel(ActiveBlockDefinition definition) {
        return parentModel(Industron.MOD_ID + ":" + definition.idleModelPath());
    }

    private static JsonObject parentModel(String parent) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", parent);
        return json;
    }

    private record BaseTextures(String side, String top, String bottom) { }
}
