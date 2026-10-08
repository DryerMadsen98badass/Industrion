package net.mads.industron.data;

import com.google.common.hash.Hashing;
import com.google.gson.JsonObject;
import net.mads.industron.Industron;
import net.mads.industron.block.SimpleBlocks;
import net.mads.industron.machine.MachineDefinition;
import net.mads.industron.machine.SingleBlockDefinition;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockDefinitions;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockControllerDefinition;
import net.minecraft.data.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Converts definition frame lists to atlas animations, without server updates/chunk rebuilds. */
public final class AnimatedMachineTextureProvider implements DataProvider {
    private final Path assets;
    private final ExistingFileHelper files;
    public AnimatedMachineTextureProvider(PackOutput output, ExistingFileHelper files) {
        assets = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve(Industron.MOD_ID);
        this.files = files;
    }
    public static String texture(List<String> frames) {
        String key = String.join("\n", frames.stream().map(f -> ResourceLocation.parse(f.contains(":") ? f : "industron:" + f).toString()).toList());
        return "industron:block/machines/animated/" + Hashing.sha256().hashString(key, StandardCharsets.UTF_8);
    }
    @Override public CompletableFuture<?> run(CachedOutput cache) {
        Set<List<String>> animations = new LinkedHashSet<>();
        for (var instance : MachineDefinition.INSTANCES)
            for (var side : SingleBlockDefinition.MachineSide.values()) add(animations, instance.definition().activeOverlays(side));
        for (var controller : MultiblockDefinitions.controllers())
            for (var side : MultiblockControllerDefinition.Side.values()) add(animations, controller.activeOverlays(side));
        for (var block : SimpleBlocks.ACTIVE) add(animations, block.activeTextures().stream().map(Object::toString).toList());
        List<CompletableFuture<?>> futures = new ArrayList<>();
        try {
            for (List<String> frames : animations) {
                List<BufferedImage> images = new ArrayList<>();
                for (String frame : frames) {
                    ResourceLocation id = ResourceLocation.parse(frame.contains(":") ? frame : "industron:" + frame);
                    String path = "assets/" + id.getNamespace() + "/textures/" + id.getPath() + ".png";
                    InputStream stream = getClass().getClassLoader().getResourceAsStream(path);
                    if (stream == null) stream = files.getResource(id, PackType.CLIENT_RESOURCES, ".png", "textures").open();
                    try (InputStream input = stream) {
                        BufferedImage image = ImageIO.read(input);
                        if (image == null) throw new IOException("Invalid animation frame: " + id);
                        images.add(image);
                    }
                }
                int width = images.get(0).getWidth(), height = images.get(0).getHeight();
                BufferedImage strip = new BufferedImage(width, height * images.size(), BufferedImage.TYPE_INT_ARGB);
                for (int i = 0; i < images.size(); i++) {
                    BufferedImage frame = images.get(i);
                    if (frame.getWidth() != width || frame.getHeight() != height) throw new IOException("Mismatched machine animation frames: " + frames);
                    strip.setRGB(0, i * height, width, height, frame.getRGB(0, 0, width, height, null, 0, width), 0, width);
                }
                ResourceLocation id = ResourceLocation.parse(texture(frames));
                Path png = assets.resolve("textures/" + id.getPath() + ".png");
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                ImageIO.write(strip, "PNG", bytes);
                byte[] encoded = bytes.toByteArray();
                cache.writeIfNeeded(png, encoded, Hashing.sha1().hashBytes(encoded));
                JsonObject animation = new JsonObject();
                animation.addProperty("frametime", 5);
                animation.addProperty("width", width); animation.addProperty("height", height);
                JsonObject metadata = new JsonObject(); metadata.add("animation", animation);
                futures.add(DataProvider.saveStable(cache, metadata, Path.of(png.toString() + ".mcmeta")));
            }
        } catch (IOException error) { return CompletableFuture.failedFuture(error); }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }
    private static void add(Set<List<String>> animations, List<String> frames) {
        if (frames.size() > 1) animations.add(List.copyOf(frames));
    }
    @Override public String getName() { return "Industron client machine animations"; }
}
