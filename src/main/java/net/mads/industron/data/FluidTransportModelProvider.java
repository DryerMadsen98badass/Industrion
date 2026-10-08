package net.mads.industron.data;

import com.google.common.hash.Hashing;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.mads.industron.Industron;
import net.mads.industron.transport.FluidTransportTier;
import net.mads.industron.transport.color.PipeColorDefinitions;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.DyeColor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public final class FluidTransportModelProvider implements DataProvider {
    private static final String TEMPLATE_ROOT = "/fluid_transport_templates/";
    private static final Map<String, String> TEXT_TEMPLATE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, BufferedImage> IMAGE_TEMPLATE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, BufferedImage> CREATE_IMAGE_CACHE = new ConcurrentHashMap<>();

    private static final List<TextureTemplate> TEXTURES = List.of(
            new TextureTemplate("pipes.png", "_pipes.png"),
            new TextureTemplate("pipes_connected.png", "_pipes_connected.png"),
            new TextureTemplate("pump.png", "_pump.png"),
            new TextureTemplate("fluid_tank.png", "_fluid_tank.png"),
            new TextureTemplate("fluid_tank_connected.png", "_fluid_tank_connected.png"),
            new TextureTemplate("fluid_tank_top.png", "_fluid_tank_top.png"),
            new TextureTemplate("fluid_tank_top_connected.png", "_fluid_tank_top_connected.png"),
            new TextureTemplate("fluid_tank_inner.png", "_fluid_tank_inner.png"),
            new TextureTemplate("fluid_tank_inner_connected.png", "_fluid_tank_inner_connected.png")
    );

    private final PackOutput output;

    public FluidTransportModelProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        Path assets = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK)
                .resolve(Industron.MOD_ID);
        Path blockstates = assets.resolve("blockstates");
        Path blockModels = assets.resolve("models/block");
        Path itemModels = assets.resolve("models/item");
        Path blockTextures = assets.resolve("textures/block");

        List<String> pipeModels = readLines("pipe_models.txt");
        List<String> pumpModels = readLines("pump_models.txt");
        List<String> tankModels = readLines("tank_models.txt");
        List<PipeColorDefinitions.PipeFamily> pipeFamilies = PipeColorDefinitions.allFamilies();
        Map<DyeColor, ColoredPipeModelSet> coloredModelSets = new EnumMap<>(DyeColor.class);
        JsonObject[] pipeShapes = PipeShapeCompiler.compile(
                JsonParser.parseString(readText("blockstates/fluid_pipe.json").replace("\uFEFF", "")).getAsJsonObject(),
                path -> JsonParser.parseString(readText(path).replace("\uFEFF", "")).getAsJsonObject());

        removeLegacySharedPipeOverrides(resourcePackRoot(), pipeModels);
        removeLegacyColoredPipeModelCopies(resourcePackRoot(), pipeFamilies);

        for (FluidTransportTier tier : FluidTransportTier.all()) {
            futures.add(DataProvider.saveStable(
                    cache,
                    sharedPipeBlockState(tier.pipeId()),
                    blockstates.resolve(tier.pipeId() + ".json")
            ));
            futures.add(DataProvider.saveStable(
                    cache,
                    glassPipeBlockState(tier),
                    blockstates.resolve(tier.glassPipeId() + ".json")
            ));
            futures.add(DataProvider.saveStable(
                    cache,
                    templateJson("blockstates/mechanical_pump.json", tier),
                    blockstates.resolve(tier.pumpId() + ".json")
            ));
            futures.add(DataProvider.saveStable(
                    cache,
                    templateJson("blockstates/fluid_tank.json", tier),
                    blockstates.resolve(tier.tankId() + ".json")
            ));

            addModels(futures, cache, tier, "fluid_pipe", tier.pipeId(), pipeModels, blockModels);
            for (int mask = 0; mask < 64; mask++) {
                futures.add(DataProvider.saveStable(cache,
                        JsonParser.parseString(pipeShapes[mask].toString()
                                .replace("bronze_pipes", tier.id() + "_pipes")).getAsJsonObject(),
                        blockModels.resolve(tier.pipeId()).resolve("shared/shape_" + mask + ".json")));
            }
            addModels(futures, cache, tier, "mechanical_pump", tier.pumpId(), pumpModels, blockModels);
            addModels(futures, cache, tier, "fluid_tank", tier.tankId(), tankModels, blockModels);

            futures.add(DataProvider.saveStable(
                    cache,
                    parentModel(Industron.MOD_ID + ":block/" + tier.pipeId() + "/item"),
                    itemModels.resolve(tier.pipeId() + ".json")
            ));
            futures.add(DataProvider.saveStable(
                    cache,
                    parentModel(Industron.MOD_ID + ":block/" + tier.pumpId() + "/item"),
                    itemModels.resolve(tier.pumpId() + ".json")
            ));
            futures.add(DataProvider.saveStable(
                    cache,
                    parentModel(Industron.MOD_ID + ":block/" + tier.tankId() + "/block_single_window"),
                    itemModels.resolve(tier.tankId() + ".json")
            ));

            writeTextures(cache, tier, blockTextures);
        }

        for (DyeColor color : DyeColor.values()) {
            writeColoredPipeTextures(cache, color, blockTextures.resolve("pipe_colors"));
            addSharedColoredPipeModels(futures, cache, color, pipeModels, blockModels, pipeShapes);
            coloredModelSets.put(color, createColoredPipeModelSet(color));
        }

        for (PipeColorDefinitions.PipeFamily family : pipeFamilies) {
            for (DyeColor color : DyeColor.values()) {
                addColoredPipe(
                        futures,
                        cache,
                        family,
                        color,
                        coloredModelSets.get(color),
                        blockstates,
                        itemModels
                );
            }
        }

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Industron Fluid Transport Models and Textures";
    }

    private Path resourcePackRoot() {
        return output.getOutputFolder(PackOutput.Target.RESOURCE_PACK);
    }

    private static void removeLegacySharedPipeOverrides(Path resourceRoot, List<String> pipeModels) {
        Path createAssets = resourceRoot.resolve("create");
        Path createExpansionAssets = resourceRoot.resolve(Industron.MOD_ID);

        try {
            Files.deleteIfExists(createAssets.resolve("blockstates/fluid_pipe.json"));
            Files.deleteIfExists(createAssets.resolve("blockstates/glass_fluid_pipe.json"));
            Files.deleteIfExists(createAssets.resolve("models/item/fluid_pipe.json"));

            Path oldPipeModels = createAssets.resolve("models/block/fluid_pipe");
            for (String modelFile : pipeModels) {
                Files.deleteIfExists(oldPipeModels.resolve(modelFile));
            }
            deleteEmptyDirectories(oldPipeModels, createAssets.resolve("models/block"));

            Files.deleteIfExists(createExpansionAssets.resolve("textures/block/colorable_fluid_pipe.png"));
            Files.deleteIfExists(createExpansionAssets.resolve("textures/block/colorable_fluid_pipe_connected.png"));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not remove legacy shared fluid pipe model overrides", exception);
        }
    }

    private static void deleteEmptyDirectories(Path directory, Path stopAt) throws IOException {
        Path current = directory;
        while (current != null && current.startsWith(stopAt) && !current.equals(stopAt)) {
            if (!Files.isDirectory(current)) {
                current = current.getParent();
                continue;
            }
            try (var entries = Files.list(current)) {
                if (entries.findAny().isPresent()) {
                    break;
                }
            }
            Files.deleteIfExists(current);
            current = current.getParent();
        }
    }

    private static void removeLegacyColoredPipeModelCopies(
            Path resourceRoot,
            List<PipeColorDefinitions.PipeFamily> families
    ) {
        Path blockModels = resourceRoot.resolve(Industron.MOD_ID).resolve("models/block");
        if (!Files.isDirectory(blockModels)) {
            return;
        }

        Set<String> legacyModelDirectories = new HashSet<>();
        for (PipeColorDefinitions.PipeFamily family : families) {
            for (DyeColor color : DyeColor.values()) {
                legacyModelDirectories.add(family.coloredPipeId(color));
                legacyModelDirectories.add(family.coloredEncasedPipeId(color));
            }
        }

        try (Stream<Path> children = Files.list(blockModels)) {
            for (Path directory : children
                    .filter(Files::isDirectory)
                    .filter(path -> legacyModelDirectories.contains(path.getFileName().toString()))
                    .toList()) {
                deleteGeneratedModelDirectory(directory);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not remove legacy per-pipe colored model copies", exception);
        }
    }

    private static void deleteGeneratedModelDirectory(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private static void addSharedColoredPipeModels(
            List<CompletableFuture<?>> futures,
            CachedOutput cache,
            DyeColor color,
            List<String> pipeModels,
            Path blockModels,
            JsonObject[] pipeShapes
    ) {
        String modelId = PipeColorDefinitions.sharedColoredModelId(color);
        Path modelRoot = blockModels.resolve(modelId);

        for (int mask = 0; mask < 64; mask++) {
            futures.add(DataProvider.saveStable(cache,
                    JsonParser.parseString(pipeShapes[mask].toString()
                            .replace("bronze_pipes", "pipe_colors/" + color.getName() + "_pipes")).getAsJsonObject(),
                    modelRoot.resolve("shared/shape_" + mask + ".json")));
        }

        futures.add(DataProvider.saveStable(
                cache,
                coloredEncasedPipeFlatModel(),
                modelRoot.resolve("encased/block_flat.json")
        ));
        futures.add(DataProvider.saveStable(
                cache,
                coloredEncasedPipeOpenModel(color),
                modelRoot.resolve("encased/block_open.json")
        ));

        for (String modelFile : pipeModels) {
            JsonObject model = modelFile.equals("window.json")
                    ? coloredGlassPipeModelJson("models/fluid_pipe/" + modelFile, modelId, color)
                    : coloredPipeTemplateJson("models/fluid_pipe/" + modelFile, modelId, color);
            futures.add(DataProvider.saveStable(
                    cache,
                    model,
                    modelRoot.resolve(modelFile)
            ));
        }
    }

    private static void addColoredPipe(
            List<CompletableFuture<?>> futures,
            CachedOutput cache,
            PipeColorDefinitions.PipeFamily family,
            DyeColor color,
            ColoredPipeModelSet models,
            Path blockstates,
            Path itemModels
    ) {
        String pipeId = family.coloredPipeId(color);
        String glassPipeId = family.coloredGlassPipeId(color);
        String encasedPipeId = family.coloredEncasedPipeId(color);

        futures.add(DataProvider.saveStable(
                cache,
                models.pipeBlockState(),
                blockstates.resolve(pipeId + ".json")
        ));
        futures.add(DataProvider.saveStable(
                cache,
                models.glassBlockState(),
                blockstates.resolve(glassPipeId + ".json")
        ));
        futures.add(DataProvider.saveStable(
                cache,
                models.encasedBlockState(),
                blockstates.resolve(encasedPipeId + ".json")
        ));

        futures.add(DataProvider.saveStable(
                cache,
                models.itemModel(),
                itemModels.resolve(pipeId + ".json")
        ));
    }

    private static ColoredPipeModelSet createColoredPipeModelSet(DyeColor color) {
        String modelId = PipeColorDefinitions.sharedColoredModelId(color);
        return new ColoredPipeModelSet(
                sharedPipeBlockState(modelId),
                coloredGlassPipeBlockState(modelId),
                coloredEncasedPipeBlockState(modelId),
                parentModel(Industron.MOD_ID + ":block/" + modelId + "/item")
        );
    }

    private static void addModels(
            List<CompletableFuture<?>> futures,
            CachedOutput cache,
            FluidTransportTier tier,
            String templateFolder,
            String outputFolder,
            List<String> modelFiles,
            Path blockModels
    ) {
        for (String modelFile : modelFiles) {
            futures.add(DataProvider.saveStable(
                    cache,
                    templateJson("models/" + templateFolder + "/" + modelFile, tier),
                    blockModels.resolve(outputFolder).resolve(modelFile)
            ));
        }
    }

    private static JsonObject templateJson(String path, FluidTransportTier tier) {
        String json = readText(path)
                .replace("\uFEFF", "")
                .replace("bronze_fluid_pipe", tier.pipeId())
                .replace("bronze_mechanical_pump", tier.pumpId())
                .replace("bronze_fluid_tank", tier.tankId())
                .replace("bronze_pipes", tier.id() + "_pipes")
                .replace("bronze_pump", tier.id() + "_pump");
        return JsonParser.parseString(json).getAsJsonObject();
    }

    private static JsonObject glassPipeBlockState(FluidTransportTier tier) {
        JsonObject variants = new JsonObject();
        String model = Industron.MOD_ID + ":block/" + tier.pipeId() + "/window";

        addGlassPipeVariant(variants, false, "x", model, 90, 90);
        addGlassPipeVariant(variants, false, "y", model, 0, 0);
        addGlassPipeVariant(variants, false, "z", model, 90, 0);
        addGlassPipeVariant(variants, true, "x", model, 90, 90);
        addGlassPipeVariant(variants, true, "y", model, 0, 0);
        addGlassPipeVariant(variants, true, "z", model, 90, 0);

        JsonObject root = new JsonObject();
        root.add("variants", variants);
        return root;
    }

    /** All block IDs use the same model locations per colour and connection mask.
     * Waterlogging remains a real state, but does not duplicate identical geometry.
     * These are ordinary vanilla models: no replacement renderer or state changes.
     */
    private static JsonObject sharedPipeBlockState(String modelId) {
        String[] directions = {"down", "up", "north", "south", "west", "east"};
        JsonObject variants = new JsonObject();
        for (int mask = 0; mask < 64; mask++) {
            StringBuilder state = new StringBuilder();
            for (int index = 0; index < directions.length; index++) {
                if (index != 0) state.append(',');
                state.append(directions[index]).append('=').append((mask & (1 << index)) != 0);
            }
            JsonObject model = new JsonObject();
            model.addProperty("model", Industron.MOD_ID + ":block/" + modelId + "/shared/shape_" + mask);
            variants.add(state.toString(), model);
        }
        JsonObject result = new JsonObject();
        result.add("variants", variants);
        return result;
    }

    private static JsonObject coloredPipeTemplateJson(String path, String modelId, DyeColor color) {
        String texture = "pipe_colors/" + color.getName() + "_pipes";
        String json = readText(path)
                .replace("\uFEFF", "")
                .replace("bronze_fluid_pipe", modelId)
                .replace("bronze_pipes_connected", texture + "_connected")
                .replace("bronze_pipes", texture);
        return JsonParser.parseString(json).getAsJsonObject();
    }

    private static JsonObject coloredGlassPipeModelJson(String path, String modelId, DyeColor color) {
        JsonObject model = coloredPipeTemplateJson(path, modelId, color);
        model.remove("render_type");
        if (model.has("textures") && model.get("textures").isJsonObject()) {
            model.getAsJsonObject("textures").addProperty(
                    "0",
                    Industron.MOD_ID + ":block/pipe_colors/" + color.getName() + "_glass_fluid_pipe"
            );
        }
        return model;
    }

    private static JsonObject coloredGlassPipeBlockState(String pipeModelId) {
        JsonObject variants = new JsonObject();
        String model = Industron.MOD_ID + ":block/" + pipeModelId + "/window";

        addGlassPipeVariant(variants, false, "x", model, 90, 90);
        addGlassPipeVariant(variants, false, "y", model, 0, 0);
        addGlassPipeVariant(variants, false, "z", model, 90, 0);
        addGlassPipeVariant(variants, true, "x", model, 90, 90);
        addGlassPipeVariant(variants, true, "y", model, 0, 0);
        addGlassPipeVariant(variants, true, "z", model, 90, 0);

        JsonObject root = new JsonObject();
        root.add("variants", variants);
        return root;
    }

    private static JsonObject coloredEncasedPipeBlockState(String pipeModelId) {
        JsonArray multipart = new JsonArray();
        for (boolean flat : new boolean[]{false, true}) {
            for (Direction direction : Direction.values()) {
                JsonObject when = new JsonObject();
                when.addProperty(direction.getSerializedName(), Boolean.toString(!flat));

                JsonObject apply = new JsonObject();
                apply.addProperty(
                        "model",
                        Industron.MOD_ID + ":block/" + pipeModelId + "/encased/block_" + (flat ? "flat" : "open")
                );

                int rotationX = direction == Direction.UP ? 90 : direction == Direction.DOWN ? 270 : 0;
                int rotationY = ((int) direction.toYRot() + (direction.getAxis().isVertical() ? 90 : 0)) % 360;
                if (rotationX != 0) {
                    apply.addProperty("x", rotationX);
                }
                if (rotationY != 0) {
                    apply.addProperty("y", rotationY);
                }

                JsonObject part = new JsonObject();
                part.add("when", when);
                part.add("apply", apply);
                multipart.add(part);
            }
        }

        JsonObject root = new JsonObject();
        root.add("multipart", multipart);
        return root;
    }

    private static JsonObject coloredEncasedPipeFlatModel() {
        return encasedFaceModel("create:block/copper_casing");
    }

    private static JsonObject coloredEncasedPipeOpenModel(DyeColor color) {
        return encasedFaceModel(
                Industron.MOD_ID + ":block/pipe_colors/" + color.getName() + "_encased_pipe"
        );
    }

    private static JsonObject encasedFaceModel(String texture) {
        JsonObject textures = new JsonObject();
        textures.addProperty("0", texture);
        textures.addProperty("particle", texture);

        JsonObject face = new JsonObject();
        JsonArray uv = new JsonArray();
        uv.add(0);
        uv.add(0);
        uv.add(16);
        uv.add(16);
        face.add("uv", uv);
        face.addProperty("texture", "#0");

        JsonObject faces = new JsonObject();
        faces.add("south", face);

        JsonObject element = new JsonObject();
        JsonArray from = new JsonArray();
        from.add(0);
        from.add(0);
        from.add(15);
        JsonArray to = new JsonArray();
        to.add(16);
        to.add(16);
        to.add(16);
        element.add("from", from);
        element.add("to", to);
        element.add("faces", faces);

        JsonArray elements = new JsonArray();
        elements.add(element);

        JsonObject root = new JsonObject();
        root.addProperty("credit", "Made with Blockbench");
        root.add("textures", textures);
        root.add("elements", elements);
        return root;
    }

    private static void addGlassPipeVariant(
            JsonObject variants,
            boolean alt,
            String axis,
            String model,
            int rotationX,
            int rotationY
    ) {
        JsonObject variant = new JsonObject();
        variant.addProperty("model", model);
        if (rotationX != 0) {
            variant.addProperty("x", rotationX);
        }
        if (rotationY != 0) {
            variant.addProperty("y", rotationY);
        }
        variants.add("alt=" + alt + ",axis=" + axis, variant);
    }

    private static JsonObject parentModel(String parent) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", parent);
        return json;
    }

    private static List<String> readLines(String path) {
        return readText(path).lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();
    }

    private static String readText(String path) {
        return TEXT_TEMPLATE_CACHE.computeIfAbsent(path, FluidTransportModelProvider::readTextUncached);
    }

    private static String readTextUncached(String path) {
        try (InputStream stream = open(path)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read fluid transport template " + path, exception);
        }
    }

    private static void writeTextures(CachedOutput cache, FluidTransportTier tier, Path outputFolder) {
        try {
            Files.createDirectories(outputFolder);
            for (TextureTemplate texture : TEXTURES) {
                BufferedImage source = readImage("textures/" + texture.templateName());
                BufferedImage recolored = recolor(source, tier.color());
                Path outputPath = outputFolder.resolve(tier.id() + texture.outputSuffix());
                writePng(cache, outputPath, recolored);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not generate fluid transport textures for " + tier.id(), exception);
        }
    }

    private static void writeColoredPipeTextures(CachedOutput cache, DyeColor color, Path outputFolder) {
        try {
            Files.createDirectories(outputFolder);
            int rgb = color.getTextureDiffuseColor() & 0xFFFFFF;
            writePng(
                    cache,
                    outputFolder.resolve(color.getName() + "_pipes.png"),
                    recolor(readImage("textures/pipes.png"), rgb)
            );
            writePng(
                    cache,
                    outputFolder.resolve(color.getName() + "_pipes_connected.png"),
                    recolor(readImage("textures/pipes_connected.png"), rgb)
            );
            writePng(
                    cache,
                    outputFolder.resolve(color.getName() + "_glass_fluid_pipe.png"),
                    recolor(readCreateImage("textures/block/glass_fluid_pipe.png"), rgb)
            );
            writePng(
                    cache,
                    outputFolder.resolve(color.getName() + "_encased_pipe.png"),
                    recolor(readCreateImage("textures/block/encased_pipe.png"), rgb)
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Could not generate colored fluid pipe textures for " + color, exception);
        }
    }

    private static BufferedImage readImage(String path) throws IOException {
        BufferedImage cached = IMAGE_TEMPLATE_CACHE.get(path);
        if (cached != null) {
            return cached;
        }
        try (InputStream stream = open(path)) {
            BufferedImage image = ImageIO.read(stream);
            if (image == null) {
                throw new IOException("Unsupported image template: " + path);
            }
            IMAGE_TEMPLATE_CACHE.put(path, image);
            return image;
        }
    }


    private static BufferedImage readCreateImage(String path) throws IOException {
        BufferedImage cached = CREATE_IMAGE_CACHE.get(path);
        if (cached != null) {
            return cached;
        }
        String resourcePath = "assets/create/" + path;
        try (InputStream stream = FluidTransportModelProvider.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IOException("Missing Create resource: " + resourcePath);
            }
            BufferedImage image = ImageIO.read(stream);
            if (image == null) {
                throw new IOException("Unsupported Create image resource: " + resourcePath);
            }
            CREATE_IMAGE_CACHE.put(path, image);
            return image;
        }
    }

    private static BufferedImage recolor(BufferedImage source, int color) {
        int targetRed = color >> 16 & 0xFF;
        int targetGreen = color >> 8 & 0xFF;
        int targetBlue = color & 0xFF;
        double referenceLuminance = referenceLuminance(source);

        BufferedImage output = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int argb = source.getRGB(x, y);
                int alpha = argb >>> 24;
                if (alpha == 0) {
                    output.setRGB(x, y, 0);
                    continue;
                }

                double shade = luminance(argb) / referenceLuminance;
                int red = clamp((int) Math.round(targetRed * shade));
                int green = clamp((int) Math.round(targetGreen * shade));
                int blue = clamp((int) Math.round(targetBlue * shade));
                output.setRGB(x, y, alpha << 24 | red << 16 | green << 8 | blue);
            }
        }
        return output;
    }

    private static double referenceLuminance(BufferedImage source) {
        int[] histogram = new int[256];
        int count = 0;
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int argb = source.getRGB(x, y);
                if ((argb >>> 24) == 0) {
                    continue;
                }
                histogram[clamp((int) Math.round(luminance(argb)))]++;
                count++;
            }
        }

        int middle = Math.max(1, count) / 2;
        int seen = 0;
        for (int value = 0; value < histogram.length; value++) {
            seen += histogram[value];
            if (seen >= middle) {
                return Math.max(1.0D, value);
            }
        }
        return 255.0D;
    }

    private static double luminance(int argb) {
        int red = argb >> 16 & 0xFF;
        int green = argb >> 8 & 0xFF;
        int blue = argb & 0xFF;
        return red * 0.2126D + green * 0.7152D + blue * 0.0722D;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    @SuppressWarnings("deprecation")
    private static void writePng(CachedOutput cache, Path outputPath, BufferedImage image) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "PNG", output)) {
            throw new IOException("No PNG writer is available");
        }

        byte[] png = output.toByteArray();
        cache.writeIfNeeded(outputPath, png, Hashing.sha1().hashBytes(png));
    }

    private static InputStream open(String path) {
        InputStream stream = FluidTransportModelProvider.class.getResourceAsStream(TEMPLATE_ROOT + path);
        if (stream == null) {
            throw new IllegalStateException("Missing fluid transport template: " + path);
        }
        return stream;
    }

    private record TextureTemplate(String templateName, String outputSuffix) {
    }

    private record ColoredPipeModelSet(
            JsonObject pipeBlockState,
            JsonObject glassBlockState,
            JsonObject encasedBlockState,
            JsonObject itemModel
    ) {
    }
}
