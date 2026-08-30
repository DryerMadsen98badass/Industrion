package net.mads.industron.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.mads.industron.Industron;
import net.mads.industron.energy.EnergyWireBlock;
import net.mads.industron.energy.WireThickness;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.defenitions.IndustrialMaterials;
import net.minecraft.core.Direction;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class EnergyWireModelProvider implements DataProvider {
    private static final Direction[] DIRECTIONS = {
            Direction.DOWN,
            Direction.UP,
            Direction.NORTH,
            Direction.SOUTH,
            Direction.WEST,
            Direction.EAST
    };

    private final PackOutput output;

    public EnergyWireModelProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        Path assets = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK)
                .resolve(Industron.MOD_ID);
        Path blockstates = assets.resolve("blockstates");
        Path blockModels = assets.resolve("models").resolve("block").resolve("energy").resolve("wire");
        Path itemModels = assets.resolve("models").resolve("item");

        for (WireThickness thickness : WireThickness.ALL) {
            for (boolean insulated : List.of(false, true)) {
                String prefix = modelPrefix(thickness, insulated);
                String texture = Industron.MOD_ID + ":block/energy/wire/" + texture(thickness, insulated);

                futures.add(DataProvider.saveStable(cache, model(thickness, texture, null, !insulated), blockModels.resolve(prefix + "_core.json")));
                for (Direction direction : DIRECTIONS) {
                    futures.add(DataProvider.saveStable(cache, model(thickness, texture, direction, !insulated), blockModels.resolve(prefix + "_" + direction.getName() + ".json")));
                }
                JsonObject itemBlockModel = insulated
                        ? insulatedItemBlockModel(thickness)
                        : wireItemBlockModel(thickness);
                futures.add(DataProvider.saveStable(cache, itemBlockModel, blockModels.resolve(prefix + "_item.json")));
            }
        }

        for (IndustrialMaterial material : IndustrialMaterials.ALL) {
            for (WireThickness thickness : WireThickness.ALL) {
                if (!material.has(thickness.materialPart())) {
                    continue;
                }
                for (boolean insulated : List.of(false, true)) {
                    String registryName = EnergyWireBlock.registryName(material, thickness, insulated);
                    String prefix = modelPrefix(thickness, insulated);
                    futures.add(DataProvider.saveStable(cache, blockstate(prefix), blockstates.resolve(registryName + ".json")));
                    futures.add(DataProvider.saveStable(cache, itemModel(thickness, prefix, insulated), itemModels.resolve(registryName + ".json")));
                }
            }
        }

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Industron Energy Wire Models";
    }

    private static JsonObject blockstate(String prefix) {
        JsonObject variants = new JsonObject();
        variants.add("", modelReference(prefix + "_core"));

        JsonObject json = new JsonObject();
        json.add("variants", variants);
        return json;
    }

    private static JsonObject modelReference(String modelName) {
        JsonObject model = new JsonObject();
        model.addProperty("model", Industron.MOD_ID + ":block/energy/wire/" + modelName);
        return model;
    }

    private static JsonObject model(WireThickness thickness, String texture, Direction armDirection, boolean tinted) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", "minecraft:block/block");
        json.addProperty("render_type", "minecraft:cutout");

        JsonObject textures = new JsonObject();
        textures.addProperty("wire", texture);
        textures.addProperty("particle", texture);
        json.add("textures", textures);

        JsonArray elements = new JsonArray();
        elements.add(element(thickness, armDirection, tinted));
        json.add("elements", elements);
        return json;
    }

    private static JsonObject element(WireThickness thickness, Direction armDirection, boolean tinted) {
        double min = (16 - thickness.pixels()) / 2.0D;
        double max = min + thickness.pixels();

        double fromX = min;
        double fromY = min;
        double fromZ = min;
        double toX = max;
        double toY = max;
        double toZ = max;

        if (armDirection != null) {
            switch (armDirection) {
                case DOWN -> {
                    fromY = 0;
                    toY = min;
                }
                case UP -> {
                    fromY = max;
                    toY = 16;
                }
                case NORTH -> {
                    fromZ = 0;
                    toZ = min;
                }
                case SOUTH -> {
                    fromZ = max;
                    toZ = 16;
                }
                case WEST -> {
                    fromX = 0;
                    toX = min;
                }
                case EAST -> {
                    fromX = max;
                    toX = 16;
                }
            }
        }

        JsonObject element = new JsonObject();
        element.add("from", vector(fromX, fromY, fromZ));
        element.add("to", vector(toX, toY, toZ));

        JsonObject faces = new JsonObject();
        addFace(faces, "north", fromX, fromY, toX, toY, tinted);
        addFace(faces, "south", fromX, fromY, toX, toY, tinted);
        addFace(faces, "east", fromZ, fromY, toZ, toY, tinted);
        addFace(faces, "west", fromZ, fromY, toZ, toY, tinted);
        addFace(faces, "up", fromX, fromZ, toX, toZ, tinted);
        addFace(faces, "down", fromX, fromZ, toX, toZ, tinted);
        element.add("faces", faces);
        return element;
    }

    private static JsonArray vector(double x, double y, double z) {
        JsonArray vector = new JsonArray();
        vector.add(x);
        vector.add(y);
        vector.add(z);
        return vector;
    }

    private static JsonArray vector(double x1, double y1, double x2, double y2) {
        JsonArray vector = new JsonArray();
        vector.add(x1);
        vector.add(y1);
        vector.add(x2);
        vector.add(y2);
        return vector;
    }

    private static void addFace(JsonObject faces, String direction, double u1, double v1, double u2, double v2, boolean tinted) {
        JsonObject face = new JsonObject();
        face.addProperty("texture", "#wire");
        if (tinted) {
            face.addProperty("tintindex", 0);
        }
        face.add("uv", vector(u1, v1, u2, v2));
        faces.add(direction, face);
    }

    private static JsonObject itemModel(WireThickness thickness, String prefix, boolean insulated) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", Industron.MOD_ID + ":block/energy/wire/" + prefix + "_item");
        return json;
    }

    private static String modelPrefix(WireThickness thickness, boolean insulated) {
        return textureName(thickness, insulated);
    }

    private static String textureName(WireThickness thickness, boolean insulated) {
        return (insulated ? "insulated_wire_" : "wire_") + thickness.id();
    }

    private static String texture(WireThickness thickness, boolean insulated) {
        if (insulated) {
            return "insulation_5";
        }
        return "wire_side";
    }

    private static int insulationIndex(WireThickness thickness) {
        return switch (thickness) {
            case X1 -> 0;
            case X2 -> 1;
            case X4 -> 2;
            case X8 -> 3;
            case X16 -> 4;
        };
    }


    private static JsonObject wireItemBlockModel(WireThickness thickness) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", "minecraft:block/block");
        json.addProperty("render_type", "minecraft:cutout");

        JsonObject textures = new JsonObject();
        textures.addProperty("wire", Industron.MOD_ID + ":block/energy/wire/wire_side");
        textures.addProperty("particle", Industron.MOD_ID + ":block/energy/wire/wire_side");
        json.add("textures", textures);

        double wireMin = (16 - thickness.pixels()) / 2.0D;
        double wireMax = wireMin + thickness.pixels();

        JsonArray elements = new JsonArray();
        elements.add(itemElement(wireMin, wireMin, 0, wireMax, wireMax, 16, "#wire", true));
        json.add("elements", elements);
        return json;
    }

    private static JsonObject insulatedItemBlockModel(WireThickness thickness) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", "minecraft:block/block");
        json.addProperty("render_type", "minecraft:cutout");

        JsonObject textures = new JsonObject();
        textures.addProperty("wire", Industron.MOD_ID + ":block/energy/wire/wire_side");
        textures.addProperty("insulation", Industron.MOD_ID + ":block/energy/wire/insulation_5");
        textures.addProperty("particle", Industron.MOD_ID + ":block/energy/wire/insulation_5");
        json.add("textures", textures);

        JsonArray elements = new JsonArray();
        double wireMin = (16 - thickness.pixels()) / 2.0D;
        double wireMax = wireMin + thickness.pixels();
        double outerMin = Math.max(0, wireMin - 1);
        double outerMax = Math.min(16, wireMax + 1);

        elements.add(itemElement(wireMin, wireMin, 0, wireMax, wireMax, 16, "#wire", true));
        elements.add(itemElement(outerMin, outerMin, 0, wireMin, outerMax, 16, "#insulation", false));
        elements.add(itemElement(wireMax, outerMin, 0, outerMax, outerMax, 16, "#insulation", false));
        elements.add(itemElement(wireMin, outerMin, 0, wireMax, wireMin, 16, "#insulation", false));
        elements.add(itemElement(wireMin, wireMax, 0, wireMax, outerMax, 16, "#insulation", false));

        json.add("elements", elements);
        return json;
    }

    private static JsonObject itemElement(double fromX, double fromY, double fromZ, double toX, double toY, double toZ, String texture, boolean tinted) {
        JsonObject element = new JsonObject();
        element.add("from", vector(fromX, fromY, fromZ));
        element.add("to", vector(toX, toY, toZ));

        JsonObject faces = new JsonObject();
        addItemFace(faces, "north", texture, tinted);
        addItemFace(faces, "south", texture, tinted);
        addItemFace(faces, "east", texture, tinted);
        addItemFace(faces, "west", texture, tinted);
        addItemFace(faces, "up", texture, tinted);
        addItemFace(faces, "down", texture, tinted);
        element.add("faces", faces);
        return element;
    }

    private static void addItemFace(JsonObject faces, String direction, String texture, boolean tinted) {
        JsonObject face = new JsonObject();
        face.addProperty("texture", texture);
        if (tinted) {
            face.addProperty("tintindex", 0);
        }
        faces.add(direction, face);
    }
}
