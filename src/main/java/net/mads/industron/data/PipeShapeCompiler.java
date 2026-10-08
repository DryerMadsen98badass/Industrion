package net.mads.industron.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/** Compile the existing unrotated pipe multipart template during datagen.
 * Uses the original elements verbatim, including UVs, tint and cull faces.
 * Child elements replace (not append to) inherited elements, as in vanilla.
 */
final class PipeShapeCompiler {
    private static final String PREFIX = "industron:block/bronze_fluid_pipe/";
    private static final String[] DIRECTIONS = {"down", "up", "north", "south", "west", "east"};

    private PipeShapeCompiler() { }

    static JsonObject[] compile(JsonObject blockstate, Function<String, JsonObject> readModel) {
        JsonArray selectors = blockstate.getAsJsonArray("multipart");
        Map<String, JsonObject> resolved = new HashMap<>();
        JsonObject[] result = new JsonObject[64];
        for (int mask = 0; mask < result.length; mask++) {
            JsonArray elements = new JsonArray();
            JsonObject textures = new JsonObject();
            int part = 0;
            for (JsonElement entry : selectors) {
                JsonObject selector = entry.getAsJsonObject();
                JsonObject apply = selector.getAsJsonObject("apply");
                // Fail datagen if the template evolves beyond this lossless compiler.
                if (apply.size() != 1 || !apply.has("model")) {
                    throw new IllegalStateException("Pipe shape compiler requires unrotated single models");
                }
                String id = apply.get("model").getAsString();
                JsonObject source = resolve(id, readModel, resolved, 0);
                if (!textures.has("particle")) {
                    textures.addProperty("particle", texture(source.getAsJsonObject("textures"), "#particle"));
                }
                if (selector.has("when") && !matches(selector.getAsJsonObject("when"), mask)) continue;
                for (JsonElement original : source.getAsJsonArray("elements")) {
                    JsonObject element = original.getAsJsonObject().deepCopy();
                    for (Map.Entry<String, JsonElement> faceEntry : element.getAsJsonObject("faces").entrySet()) {
                        JsonObject face = faceEntry.getValue().getAsJsonObject();
                        String key = "part_" + part++;
                        textures.addProperty(key, texture(source.getAsJsonObject("textures"), face.get("texture").getAsString()));
                        face.addProperty("texture", "#" + key);
                    }
                    elements.add(element);
                }
            }
            JsonObject model = new JsonObject();
            model.add("textures", textures);
            model.add("elements", elements);
            result[mask] = model;
        }
        return result;
    }

    private static boolean matches(JsonObject condition, int mask) {
        for (Map.Entry<String, JsonElement> entry : condition.entrySet()) {
            int index = java.util.Arrays.asList(DIRECTIONS).indexOf(entry.getKey());
            if (index < 0) throw new IllegalStateException("Unsupported pipe condition: " + entry.getKey());
            String value = entry.getValue().getAsString();
            if (!value.equals("true") && !value.equals("false")) {
                throw new IllegalStateException("Unsupported pipe condition value: " + value);
            }
            if (((mask & (1 << index)) != 0) != Boolean.parseBoolean(value)) return false;
        }
        return true;
    }

    private static JsonObject resolve(String id, Function<String, JsonObject> reader,
                                      Map<String, JsonObject> cache, int depth) {
        if (depth > 32 || !id.startsWith(PREFIX)) throw new IllegalStateException("Unsupported pipe parent: " + id);
        if (cache.containsKey(id)) return cache.get(id);
        JsonObject child = reader.apply("models/fluid_pipe/" + id.substring(PREFIX.length()) + ".json");
        for (String key : child.keySet()) {
            if (!java.util.Set.of("parent", "textures", "elements", "credit").contains(key)) {
                throw new IllegalStateException("Unsupported pipe model setting: " + key);
            }
        }
        JsonObject model = child.has("parent")
                ? resolve(child.get("parent").getAsString(), reader, cache, depth + 1).deepCopy()
                : new JsonObject();
        JsonObject textures = model.has("textures") ? model.getAsJsonObject("textures") : new JsonObject();
        if (child.has("textures")) child.getAsJsonObject("textures").entrySet()
                .forEach(entry -> textures.add(entry.getKey(), entry.getValue().deepCopy()));
        model.add("textures", textures);
        if (child.has("elements")) model.add("elements", child.getAsJsonArray("elements").deepCopy());
        cache.put(id, model);
        return model;
    }

    private static String texture(JsonObject textures, String value) {
        for (int depth = 0; value.startsWith("#"); depth++) {
            if (depth > 32 || !textures.has(value.substring(1))) {
                throw new IllegalStateException("Unresolved pipe texture: " + value);
            }
            value = textures.get(value.substring(1)).getAsString();
        }
        return value;
    }
}
