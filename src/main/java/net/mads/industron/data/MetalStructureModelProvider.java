package net.mads.industron.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.mads.industron.Industron;
import net.mads.industron.material.structure.MetalMaterial;
import net.mads.industron.material.structure.MetalMaterials;
import net.mads.industron.material.structure.StructureBlockDefinition;
import net.mads.industron.material.structure.StructureMaterialGenerator;
import net.mads.industron.material.structure.StructureSetResolver;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Writes the model families that cannot be expressed by the standard NeoForge
 * cube/slab/stairs/door helpers. Parent models and texture-slot names mirror
 * Minecraft 1.21.1 and Create 6.0.10.
 */
public final class MetalStructureModelProvider implements DataProvider {
    private static final String CUTOUT = "minecraft:cutout";
    private static final String TRANSLUCENT = "minecraft:translucent";

    private final PackOutput.PathProvider blockStates;
    private final PackOutput.PathProvider blockModels;
    private final PackOutput.PathProvider itemModels;

    public MetalStructureModelProvider(PackOutput output) {
        blockStates = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "blockstates");
        blockModels = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models/block");
        itemModels = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models/item");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (MetalMaterial material : MetalMaterials.ALL) {
            for (StructureBlockDefinition definition : StructureMaterialGenerator.generatedBlockDefinitions(material)) {
                if (isCustom(definition)) {
                    generate(output, futures, definition);
                }
            }
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private static boolean isCustom(StructureBlockDefinition definition) {
        return switch (definition.modelKind()) {
            case DEFAULT, CUTOUT_CUBE, VANILLA_DOOR, VANILLA_TRAPDOOR -> false;
            default -> true;
        };
    }

    private void generate(
            CachedOutput output,
            List<CompletableFuture<?>> futures,
            StructureBlockDefinition definition
    ) {
        switch (definition.modelKind()) {
            case CREATE_BARS -> generateCreateBars(output, futures, definition);
            case VANILLA_BARS -> generateVanillaBars(output, futures, definition);
            case CREATE_BRACKET -> generateBracket(output, futures, definition);
            case COPPER_BULB -> generateBulb(output, futures, definition);
            case CREATE_DOOR -> generateCreateDoor(output, futures, definition);
            case CREATE_LADDER -> generateLadder(output, futures, definition);
            case CREATE_SCAFFOLD -> generateScaffold(output, futures, definition);
            case CREATE_TRAIN_TRAPDOOR -> generateTrainTrapdoor(output, futures, definition);
            case CREATE_WINDOW -> generateWindow(output, futures, definition);
            case CREATE_WINDOW_PANE -> generateWindowPane(output, futures, definition);
            default -> throw new IllegalStateException(
                    "Unsupported custom metal model kind " + definition.modelKind()
                            + " for " + definition.registryName()
            );
        }
    }

    private void generateCreateBars(
            CachedOutput output,
            List<CompletableFuture<?>> futures,
            StructureBlockDefinition definition
    ) {
        String id = definition.registryName();
        String main = texture(definition, "main");
        String edge = texture(definition, "edge");
        Map<String, String> textures = map("bars", main, "edge", edge, "particle", main);

        saveBlockModel(output, futures, id + "_post_ends", model("create:block/bars/post_ends", textures, CUTOUT));
        saveBlockModel(output, futures, id + "_post", model("create:block/bars/post", textures, CUTOUT));
        saveBlockModel(output, futures, id + "_cap", model("create:block/bars/cap", textures, CUTOUT));
        saveBlockModel(output, futures, id + "_cap_alt", model("create:block/bars/cap_alt", textures, CUTOUT));
        saveBlockModel(output, futures, id + "_side", model("create:block/bars/side", textures, CUTOUT));
        saveBlockModel(output, futures, id + "_side_alt", model("create:block/bars/side_alt", textures, CUTOUT));
        saveBlockState(output, futures, id, barsBlockState(id));
        saveItemModel(output, futures, id, generatedItem(main));
    }

    private void generateVanillaBars(
            CachedOutput output,
            List<CompletableFuture<?>> futures,
            StructureBlockDefinition definition
    ) {
        String id = definition.registryName();
        String main = texture(definition, "main");
        Map<String, String> textures = map("bars", main, "edge", main, "particle", main);

        saveBlockModel(output, futures, id + "_post_ends", model("minecraft:block/iron_bars_post_ends", textures, CUTOUT));
        saveBlockModel(output, futures, id + "_post", model("minecraft:block/iron_bars_post", textures, CUTOUT));
        saveBlockModel(output, futures, id + "_cap", model("minecraft:block/iron_bars_cap", textures, CUTOUT));
        saveBlockModel(output, futures, id + "_cap_alt", model("minecraft:block/iron_bars_cap_alt", textures, CUTOUT));
        saveBlockModel(output, futures, id + "_side", model("minecraft:block/iron_bars_side", textures, CUTOUT));
        saveBlockModel(output, futures, id + "_side_alt", model("minecraft:block/iron_bars_side_alt", textures, CUTOUT));
        saveBlockState(output, futures, id, barsBlockState(id));
        saveItemModel(output, futures, id, generatedItem(main));
    }

    private void generateBracket(
            CachedOutput output,
            List<CompletableFuture<?>> futures,
            StructureBlockDefinition definition
    ) {
        String id = definition.registryName();
        Map<String, String> textures = map(
                "bracket", texture(definition, "bracket"),
                "plate", texture(definition, "plate")
        );
        for (String type : new String[]{"cog", "pipe", "shaft"}) {
            saveBlockModel(output, futures, id + "_" + type + "_ground", model(
                    "create:block/bracket/" + type + "/ground", textures, null
            ));
            saveBlockModel(output, futures, id + "_" + type + "_wall", model(
                    "create:block/bracket/" + type + "/wall", textures, null
            ));
        }
        saveBlockState(output, futures, id, bracketBlockState(id));
        saveItemModel(output, futures, id, model("create:block/bracket/item", textures, null));
    }

    private void generateBulb(
            CachedOutput output,
            List<CompletableFuture<?>> futures,
            StructureBlockDefinition definition
    ) {
        String id = definition.registryName();
        saveBlockModel(output, futures, id, cubeAll(texture(definition, "off")));
        saveBlockModel(output, futures, id + "_lit", cubeAll(texture(definition, "lit")));
        saveBlockModel(output, futures, id + "_powered", cubeAll(texture(definition, "powered")));
        saveBlockModel(output, futures, id + "_lit_powered", cubeAll(texture(definition, "lit_powered")));

        JsonObject state = variants();
        addVariant(state, "lit=false,powered=false", blockModel(id), 0, 0, false);
        addVariant(state, "lit=false,powered=true", blockModel(id + "_powered"), 0, 0, false);
        addVariant(state, "lit=true,powered=false", blockModel(id + "_lit"), 0, 0, false);
        addVariant(state, "lit=true,powered=true", blockModel(id + "_lit_powered"), 0, 0, false);
        saveBlockState(output, futures, id, state);
        saveItemModel(output, futures, id, parent(blockModel(id)));
    }

    private void generateCreateDoor(
            CachedOutput output,
            List<CompletableFuture<?>> futures,
            StructureBlockDefinition definition
    ) {
        String id = definition.registryName();
        String sourceModel = definition.modelTemplate().orElseThrow(() ->
                new IllegalStateException("Create door model template missing for " + id)
        );
        String bottom = texture(definition, "bottom");
        String top = texture(definition, "top");
        String side = texture(definition, "side");
        String particle = texture(definition, "particle");

        saveBlockModel(output, futures, id + "_bottom", model(
                "create:block/" + sourceModel + "/block_bottom",
                map("0", side, "2", bottom, "particle", particle),
                CUTOUT
        ));
        saveBlockModel(output, futures, id + "_top", model(
                "create:block/" + sourceModel + "/block_top",
                map("0", side, "2", top, "particle", particle),
                CUTOUT
        ));

        if (isFoldingDoor(sourceModel)) {
            Map<String, String> foldTextures = map(
                    "0", side,
                    "2", top,
                    "3", bottom,
                    "particle", particle
            );
            saveBlockModel(output, futures, id + "_fold_left", model(
                    "create:block/" + sourceModel + "/fold_left",
                    foldTextures,
                    CUTOUT
            ));
            saveBlockModel(output, futures, id + "_fold_right", model(
                    "create:block/" + sourceModel + "/fold_right",
                    foldTextures,
                    CUTOUT
            ));
        }

        saveBlockState(output, futures, id, createDoorBlockState(id));
        saveItemModel(output, futures, id, generatedItem(texture(definition, "item")));
    }

    private void generateLadder(
            CachedOutput output,
            List<CompletableFuture<?>> futures,
            StructureBlockDefinition definition
    ) {
        String id = definition.registryName();
        String main = texture(definition, "main");
        String hoop = texture(definition, "hoop");
        saveBlockModel(output, futures, id, model(
                "create:block/ladder",
                map("0", hoop, "1", main, "particle", main),
                CUTOUT
        ));

        JsonObject state = variants();
        addVariant(state, "facing=north", blockModel(id), 0, 0, false);
        addVariant(state, "facing=east", blockModel(id), 0, 90, false);
        addVariant(state, "facing=south", blockModel(id), 0, 180, false);
        addVariant(state, "facing=west", blockModel(id), 0, 270, false);
        saveBlockState(output, futures, id, state);
        saveItemModel(output, futures, id, parent(blockModel(id)));
    }

    private void generateScaffold(
            CachedOutput output,
            List<CompletableFuture<?>> futures,
            StructureBlockDefinition definition
    ) {
        String id = definition.registryName();
        String side = texture(definition, "side");
        String inside = texture(definition, "inside");
        String casing = texture(definition, "casing");
        String top = texture(definition, "top");
        Map<String, String> textures = map(
                "casing", casing,
                "inside", inside,
                "particle", side,
                "side", side,
                "top", top
        );
        saveBlockModel(output, futures, id, model("create:block/scaffold/block", textures, CUTOUT));
        saveBlockModel(output, futures, id + "_horizontal", model(
                "create:block/scaffold/block_horizontal",
                textures,
                CUTOUT
        ));

        JsonObject state = variants();
        addVariant(state, "bottom=false", blockModel(id), 0, 0, false);
        addVariant(state, "bottom=true", blockModel(id + "_horizontal"), 0, 0, false);
        saveBlockState(output, futures, id, state);
        saveItemModel(output, futures, id, parent(blockModel(id)));
    }

    private void generateTrainTrapdoor(
            CachedOutput output,
            List<CompletableFuture<?>> futures,
            StructureBlockDefinition definition
    ) {
        String id = definition.registryName();
        String main = texture(definition, "main");
        String side = texture(definition, "side");
        Map<String, String> textures = map("0", side, "1", main, "particle", main);
        saveBlockModel(output, futures, id + "_bottom", model(
                "create:block/train_trapdoor/block_bottom", textures, CUTOUT
        ));
        saveBlockModel(output, futures, id + "_top", model(
                "create:block/train_trapdoor/block_top", textures, CUTOUT
        ));
        saveBlockModel(output, futures, id + "_open", model(
                "create:block/train_trapdoor/block_open", textures, CUTOUT
        ));
        saveBlockState(output, futures, id, trainTrapdoorBlockState(id));
        saveItemModel(output, futures, id, parent(blockModel(id + "_bottom")));
    }

    private void generateWindow(
            CachedOutput output,
            List<CompletableFuture<?>> futures,
            StructureBlockDefinition definition
    ) {
        String id = definition.registryName();
        String side = texture(definition, "side");
        boolean randomWeathered = definition.texture("end_1").isPresent();
        String renderType = randomWeathered ? TRANSLUCENT : CUTOUT;

        if (randomWeathered) {
            JsonArray models = new JsonArray();
            for (int index = 1; index <= 4; index++) {
                String modelId = id + "_" + index;
                saveBlockModel(output, futures, modelId, model(
                        "minecraft:block/cube_column",
                        map("side", side, "end", texture(definition, "end_" + index), "particle", side),
                        renderType
                ));
                models.add(configuredModel(blockModel(modelId), 0, 0, false));
            }
            JsonObject state = variants();
            state.getAsJsonObject("variants").add("", models);
            saveBlockState(output, futures, id, state);
            saveItemModel(output, futures, id, parent(blockModel(id + "_1")));
            return;
        }

        String end = texture(definition, "end");
        saveBlockModel(output, futures, id, model(
                "minecraft:block/cube_column",
                map("side", side, "end", end, "particle", side),
                renderType
        ));
        JsonObject state = variants();
        addVariant(state, "", blockModel(id), 0, 0, false);
        saveBlockState(output, futures, id, state);
        saveItemModel(output, futures, id, parent(blockModel(id)));
    }

    private void generateWindowPane(
            CachedOutput output,
            List<CompletableFuture<?>> futures,
            StructureBlockDefinition definition
    ) {
        String id = definition.registryName();
        String pane = texture(definition, "side");
        String edge = texture(definition, "pane_top");
        String renderType = definition.texture("end_1").isPresent() ? TRANSLUCENT : CUTOUT;
        Map<String, String> textures = map("edge", edge, "pane", pane, "particle", pane);

        saveBlockModel(output, futures, id + "_post", model(
                "create:block/connected_glass_pane/post", textures, renderType
        ));
        saveBlockModel(output, futures, id + "_side", model(
                "create:block/connected_glass_pane/side", textures, renderType
        ));
        saveBlockModel(output, futures, id + "_noside", model(
                "create:block/connected_glass_pane/noside", textures, renderType
        ));
        saveBlockModel(output, futures, id + "_side_alt", model(
                "create:block/connected_glass_pane/side_alt", textures, renderType
        ));
        saveBlockModel(output, futures, id + "_noside_alt", model(
                "create:block/connected_glass_pane/noside_alt", textures, renderType
        ));
        saveBlockState(output, futures, id, windowPaneBlockState(id));
        saveItemModel(output, futures, id, generatedItem(pane));
    }

    private static JsonObject barsBlockState(String id) {
        JsonObject root = new JsonObject();
        JsonArray multipart = new JsonArray();
        multipart.add(apply(blockModel(id + "_post_ends"), 0));
        multipart.add(applyWhen(blockModel(id + "_post"), 0,
                map("east", "false", "north", "false", "south", "false", "west", "false")));
        multipart.add(applyWhen(blockModel(id + "_cap"), 0,
                map("east", "false", "north", "true", "south", "false", "west", "false")));
        multipart.add(applyWhen(blockModel(id + "_cap"), 90,
                map("east", "true", "north", "false", "south", "false", "west", "false")));
        multipart.add(applyWhen(blockModel(id + "_cap_alt"), 0,
                map("east", "false", "north", "false", "south", "true", "west", "false")));
        multipart.add(applyWhen(blockModel(id + "_cap_alt"), 90,
                map("east", "false", "north", "false", "south", "false", "west", "true")));
        multipart.add(applyWhen(blockModel(id + "_side"), 0, map("north", "true")));
        multipart.add(applyWhen(blockModel(id + "_side"), 90, map("east", "true")));
        multipart.add(applyWhen(blockModel(id + "_side_alt"), 0, map("south", "true")));
        multipart.add(applyWhen(blockModel(id + "_side_alt"), 90, map("west", "true")));
        root.add("multipart", multipart);
        return root;
    }

    private static JsonObject bracketBlockState(String id) {
        JsonObject root = variants();
        for (boolean alongFirst : new boolean[]{false, true}) {
            for (String type : new String[]{"cog", "pipe", "shaft"}) {
                addBracketVariant(root, id, alongFirst, "down", type);
                addBracketVariant(root, id, alongFirst, "up", type);
                addBracketVariant(root, id, alongFirst, "east", type);
                addBracketVariant(root, id, alongFirst, "west", type);
                addBracketVariant(root, id, alongFirst, "north", type);
                addBracketVariant(root, id, alongFirst, "south", type);
            }
        }
        return root;
    }

    private static void addBracketVariant(
            JsonObject root,
            String id,
            boolean alongFirst,
            String facing,
            String type
    ) {
        boolean ground = facing.equals("up") || facing.equals("down");
        int x = 0;
        int y = 0;
        if (!alongFirst) {
            switch (facing) {
                case "down" -> x = 180;
                case "west" -> y = 180;
                case "north" -> { x = 90; y = 270; }
                case "south" -> { x = 90; y = 90; }
                default -> { }
            }
        } else {
            switch (facing) {
                case "down" -> { x = 180; y = 90; }
                case "up" -> y = 90;
                case "east" -> x = 90;
                case "west" -> { x = 90; y = 180; }
                case "north" -> y = 270;
                case "south" -> y = 90;
                default -> { }
            }
        }
        String key = "axis_along_first=" + alongFirst + ",facing=" + facing + ",type=" + type;
        addVariant(root, key, blockModel(id + "_" + type + (ground ? "_ground" : "_wall")), x, y, false);
    }

    private static JsonObject createDoorBlockState(String id) {
        JsonObject root = variants();
        String[] facings = {"east", "north", "south", "west"};
        String[] halves = {"lower", "upper"};
        String[] hinges = {"left", "right"};
        for (String facing : facings) {
            for (String half : halves) {
                for (String hinge : hinges) {
                    for (boolean open : new boolean[]{false, true}) {
                        for (boolean visible : new boolean[]{false, true}) {
                            int rotation = doorRotation(facing, hinge, open);
                            String key = "facing=" + facing
                                    + ",half=" + half
                                    + ",hinge=" + hinge
                                    + ",open=" + open
                                    + ",visible=" + visible;
                            addVariant(
                                    root,
                                    key,
                                    blockModel(id + (half.equals("lower") ? "_bottom" : "_top")),
                                    0,
                                    rotation,
                                    false
                            );
                        }
                    }
                }
            }
        }
        return root;
    }

    private static boolean isFoldingDoor(String sourceModel) {
        return sourceModel.equals("copper_door") || sourceModel.equals("andesite_door");
    }

    private static int doorRotation(String facing, String hinge, boolean open) {
        int closed = switch (facing) {
            case "east" -> 0;
            case "south" -> 90;
            case "west" -> 180;
            case "north" -> 270;
            default -> throw new IllegalArgumentException("Unknown facing " + facing);
        };
        if (!open) {
            return closed;
        }
        int delta = hinge.equals("left") ? 90 : -90;
        return Math.floorMod(closed + delta, 360);
    }

    private static JsonObject trainTrapdoorBlockState(String id) {
        JsonObject root = variants();
        addTrapdoorFacing(root, id, "north", 0);
        addTrapdoorFacing(root, id, "east", 90);
        addTrapdoorFacing(root, id, "south", 180);
        addTrapdoorFacing(root, id, "west", 270);
        return root;
    }

    private static void addTrapdoorFacing(JsonObject root, String id, String facing, int rotation) {
        addVariant(root, "facing=" + facing + ",half=bottom,open=false",
                blockModel(id + "_bottom"), 0, rotation, false);
        addVariant(root, "facing=" + facing + ",half=bottom,open=true",
                blockModel(id + "_open"), 0, rotation, false);
        addVariant(root, "facing=" + facing + ",half=top,open=false",
                blockModel(id + "_top"), 0, rotation, false);
        int openTopY = Math.floorMod(rotation + 180, 360);
        addVariant(root, "facing=" + facing + ",half=top,open=true",
                blockModel(id + "_open"), 180, openTopY, false);
    }

    private static JsonObject windowPaneBlockState(String id) {
        JsonObject root = new JsonObject();
        JsonArray multipart = new JsonArray();
        multipart.add(apply(blockModel(id + "_post"), 0));
        multipart.add(applyWhen(blockModel(id + "_side"), 0, map("north", "true")));
        multipart.add(applyWhen(blockModel(id + "_noside"), 0, map("north", "false")));
        multipart.add(applyWhen(blockModel(id + "_side_alt"), 0, map("south", "true")));
        multipart.add(applyWhen(blockModel(id + "_noside_alt"), 90, map("south", "false")));
        multipart.add(applyWhen(blockModel(id + "_side_alt"), 90, map("west", "true")));
        multipart.add(applyWhen(blockModel(id + "_noside"), 270, map("west", "false")));
        multipart.add(applyWhen(blockModel(id + "_side"), 90, map("east", "true")));
        multipart.add(applyWhen(blockModel(id + "_noside_alt"), 0, map("east", "false")));
        root.add("multipart", multipart);
        return root;
    }

    private static JsonObject apply(String model, int y) {
        JsonObject entry = new JsonObject();
        entry.add("apply", configuredModel(model, 0, y, false));
        return entry;
    }

    private static JsonObject applyWhen(String model, int y, Map<String, String> conditions) {
        JsonObject entry = apply(model, y);
        JsonObject when = new JsonObject();
        conditions.forEach(when::addProperty);
        entry.add("when", when);
        return entry;
    }

    private static JsonObject variants() {
        JsonObject root = new JsonObject();
        root.add("variants", new JsonObject());
        return root;
    }

    private static void addVariant(
            JsonObject root,
            String key,
            String model,
            int x,
            int y,
            boolean uvLock
    ) {
        root.getAsJsonObject("variants").add(key, configuredModel(model, x, y, uvLock));
    }

    private static void addVariantArray(JsonObject root, String key, List<String> models) {
        JsonArray array = new JsonArray();
        for (String model : models) {
            array.add(configuredModel(model, 0, 0, false));
        }
        root.getAsJsonObject("variants").add(key, array);
    }

    private static JsonObject configuredModel(String model, int x, int y, boolean uvLock) {
        JsonObject configured = new JsonObject();
        configured.addProperty("model", model);
        if (x != 0) {
            configured.addProperty("x", x);
        }
        if (y != 0) {
            configured.addProperty("y", y);
        }
        if (uvLock) {
            configured.addProperty("uvlock", true);
        }
        return configured;
    }

    private static JsonObject cubeAll(String texture) {
        return model("minecraft:block/cube_all", map("all", texture, "particle", texture), null);
    }

    private static JsonObject generatedItem(String texture) {
        return model("minecraft:item/generated", map("layer0", texture), null);
    }

    private static JsonObject parent(String parent) {
        JsonObject model = new JsonObject();
        model.addProperty("parent", parent);
        return model;
    }

    private static JsonObject model(String parent, Map<String, String> textures, String renderType) {
        JsonObject model = new JsonObject();
        model.addProperty("parent", parent);
        if (!textures.isEmpty()) {
            JsonObject textureObject = new JsonObject();
            textures.forEach(textureObject::addProperty);
            model.add("textures", textureObject);
        }
        if (renderType != null && !renderType.isBlank()) {
            model.addProperty("render_type", renderType);
        }
        return model;
    }

    private static String texture(StructureBlockDefinition definition, String slot) {
        String fileName = definition.requiredTexture(slot);
        return StructureSetResolver.generatedTexture(definition.material(), fileName).toString();
    }

    private static String blockModel(String id) {
        return Industron.MOD_ID + ":block/" + id;
    }

    private void saveBlockState(
            CachedOutput output,
            List<CompletableFuture<?>> futures,
            String id,
            JsonObject json
    ) {
        save(output, futures, blockStates, id, json);
    }

    private void saveBlockModel(
            CachedOutput output,
            List<CompletableFuture<?>> futures,
            String id,
            JsonObject json
    ) {
        save(output, futures, blockModels, id, json);
    }

    private void saveItemModel(
            CachedOutput output,
            List<CompletableFuture<?>> futures,
            String id,
            JsonObject json
    ) {
        save(output, futures, itemModels, id, json);
    }

    private static void save(
            CachedOutput output,
            List<CompletableFuture<?>> futures,
            PackOutput.PathProvider provider,
            String id,
            JsonObject json
    ) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, id);
        Path path = provider.json(location);
        futures.add(DataProvider.saveStable(output, json, path));
    }

    private static Map<String, String> map(String... values) {
        if (values.length % 2 != 0) {
            throw new IllegalArgumentException("Expected key/value pairs");
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            result.put(values[index], values[index + 1]);
        }
        return result;
    }

    @Override
    public String getName() {
        return "Industron custom metal structure models";
    }
}
