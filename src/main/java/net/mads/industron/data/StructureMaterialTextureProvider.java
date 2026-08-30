package net.mads.industron.data;

import com.google.common.hash.Hashing;
import net.mads.industron.Industron;
import net.mads.industron.material.structure.GemMaterial;
import net.mads.industron.material.structure.StructureMaterial;
import net.mads.industron.material.structure.StoneModel;
import net.mads.industron.material.structure.StructureBlockDefinition;
import net.mads.industron.material.structure.StructureMaterialGenerator;
import net.mads.industron.material.structure.StructureMaterials;
import net.mads.industron.material.structure.StructureSetResolver;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Generates colored structure textures from the grayscale structure_sets templates. */
public final class StructureMaterialTextureProvider implements DataProvider {
    private final PackOutput.PathProvider textures;

    public StructureMaterialTextureProvider(PackOutput output) {
        this.textures = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "textures");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        long started = System.nanoTime();
        Map<Path, TemplatePixels> templateCache = new HashMap<>();
        Map<Path, byte[]> metadataCache = new HashMap<>();
        int generated = 0;

        try {
            for (StructureMaterial material : StructureMaterials.ALL) {
                generated += generateMaterial(output, material, templateCache, metadataCache);
            }
            long millis = (System.nanoTime() - started) / 1_000_000L;
            Industron.LOGGER.info(
                    "Generated {} structure textures for {} materials from {} decoded templates in {} ms",
                    generated,
                    StructureMaterials.ALL.size(),
                    templateCache.size(),
                    millis
            );
            return CompletableFuture.completedFuture(null);
        } catch (Exception exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }

    private int generateMaterial(
            CachedOutput output,
            StructureMaterial material,
            Map<Path, TemplatePixels> templateCache,
            Map<Path, byte[]> metadataCache
    ) throws IOException {
        Set<String> files = new LinkedHashSet<>();
        for (StructureBlockDefinition definition : StructureMaterialGenerator.generatedBlockDefinitions(material)) {
            files.add(definition.textureFile());
            definition.topTextureFile().ifPresent(files::add);
            definition.bottomTextureFile().ifPresent(files::add);
            definition.itemTextureFile().ifPresent(files::add);
            files.addAll(definition.textureFiles().values());
        }
        if (files.isEmpty()) {
            return 0;
        }

        int generated = 0;
        for (String fileName : files) {
            StructureSetResolver.ResolvedTemplate resolved = StructureSetResolver.sourceTemplate(material.model(), fileName)
                    .orElseThrow(() -> new IllegalStateException(
                            "Could not resolve source structure texture " + material.model().id() + "/" + fileName
                    ));
            Path source = resolved.path();
            TemplatePixels template = templatePixels(source, templateCache);
            double grayScale = fallbackGrayScale(material, resolved, templateCache);

            BufferedImage tinted = template.render(material.color(), material instanceof GemMaterial, grayScale);
            byte[] data = encodePng(tinted, source);
            ResourceLocation destination = StructureSetResolver.generatedTexture(material, fileName);
            Path path = texturePath(destination, "png");
            output.writeIfNeeded(path, data, Hashing.sha1().hashBytes(data));

            Path metadataSource = source.resolveSibling(source.getFileName() + ".mcmeta");
            if (Files.isRegularFile(metadataSource)) {
                byte[] metadata = metadataCache.get(metadataSource);
                if (metadata == null) {
                    metadata = Files.readAllBytes(metadataSource);
                    metadataCache.put(metadataSource, metadata);
                }
                Path metadataPath = texturePath(destination, "png.mcmeta");
                output.writeIfNeeded(metadataPath, metadata, Hashing.sha1().hashBytes(metadata));
            }
            generated++;
        }
        return generated;
    }

    private static TemplatePixels templatePixels(
            Path source,
            Map<Path, TemplatePixels> templateCache
    ) throws IOException {
        TemplatePixels template = templateCache.get(source);
        if (template == null) {
            template = decodeTemplate(source);
            templateCache.put(source, template);
        }
        return template;
    }

    /**
     * A fallback sprite comes from another stone family and may have a very different
     * grayscale baseline. Normalize the donor sprite against donor/target base textures
     * before tinting so, for example, polished netherrack keeps netherrack luminance
     * instead of inheriting polished diorite's brightness.
     */
    private static double fallbackGrayScale(
            StructureMaterial material,
            StructureSetResolver.ResolvedTemplate resolved,
            Map<Path, TemplatePixels> templateCache
    ) throws IOException {
        if (!(material.model() instanceof StoneModel targetModel)) {
            return 1.0D;
        }

        StoneModel donorModel = resolved.fallbackDonor().orElse(null);
        if (donorModel == null) {
            return 1.0D;
        }

        Path targetBase = StructureSetResolver.sourceTemplate(targetModel, targetModel.baseSideTexture())
                .orElseThrow(() -> new IllegalStateException(
                        "Could not resolve base stone template for " + targetModel.id()
                ))
                .path();
        Path donorBase = StructureSetResolver.sourceTemplate(donorModel, donorModel.baseSideTexture())
                .orElseThrow(() -> new IllegalStateException(
                        "Could not resolve donor base stone template for " + donorModel.id()
                ))
                .path();

        double targetAverage = templatePixels(targetBase, templateCache).averageGray();
        double donorAverage = templatePixels(donorBase, templateCache).averageGray();
        if (donorAverage <= 0.0D) {
            return 1.0D;
        }
        return targetAverage / donorAverage;
    }

    private static TemplatePixels decodeTemplate(Path source) throws IOException {
        BufferedImage image = ImageIO.read(source.toFile());
        if (image == null) {
            throw new IllegalStateException("Could not decode structure texture: " + source);
        }

        int width = image.getWidth();
        int height = image.getHeight();
        int[] alpha = new int[width * height];
        int[] gray = new int[width * height];
        int index = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);
                alpha[index] = (argb >>> 24) & 0xFF;
                int r = (argb >>> 16) & 0xFF;
                int g = (argb >>> 8) & 0xFF;
                int b = argb & 0xFF;
                gray[index] = (299 * r + 587 * g + 114 * b) / 1000;
                index++;
            }
        }

        return new TemplatePixels(width, height, alpha, gray);
    }

    private static byte[] encodePng(BufferedImage image, Path source) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(1024);
        if (!ImageIO.write(image, "png", bytes)) {
            throw new IllegalStateException("No PNG writer available for " + source);
        }
        return bytes.toByteArray();
    }

    private Path texturePath(ResourceLocation destination, String extension) {
        if (extension.startsWith(".")) {
            throw new IllegalArgumentException("Texture extension must not start with a dot: " + extension);
        }

        Path path = textures.file(destination, extension);
        String fileName = path.getFileName().toString();
        String expectedSuffix = "." + extension;
        if (!fileName.endsWith(expectedSuffix) || fileName.endsWith(".." + extension)) {
            throw new IllegalStateException(
                    "Invalid generated structure texture path " + path
                            + " for " + destination + " (extension " + extension + ")"
            );
        }
        return path;
    }

    private static int gemShade(int target, int gray) {
        if (gray <= 128) {
            return clamp((int) Math.round(target * (gray / 128.0D)));
        }
        double highlight = (gray - 128) / 127.0D;
        return clamp((int) Math.round(target + (255 - target) * highlight));
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static final class TemplatePixels {
        private final int[] alpha;
        private final int[] gray;
        private final BufferedImage target;
        private final int[] targetPixels;

        private TemplatePixels(int width, int height, int[] alpha, int[] gray) {
            this.alpha = alpha;
            this.gray = gray;
            this.target = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            this.targetPixels = ((DataBufferInt) target.getRaster().getDataBuffer()).getData();
        }

        private BufferedImage render(int rgb, boolean gem, double grayScale) {
            int targetR = (rgb >> 16) & 0xFF;
            int targetG = (rgb >> 8) & 0xFF;
            int targetB = rgb & 0xFF;

            for (int index = 0; index < gray.length; index++) {
                int shade = clamp((int) Math.round(gray[index] * grayScale));
                int outR;
                int outG;
                int outB;

                if (gem) {
                    outR = gemShade(targetR, shade);
                    outG = gemShade(targetG, shade);
                    outB = gemShade(targetB, shade);
                } else {
                    double factor = shade / 128.0D;
                    outR = clamp((int) Math.round(targetR * factor));
                    outG = clamp((int) Math.round(targetG * factor));
                    outB = clamp((int) Math.round(targetB * factor));
                }

                targetPixels[index] = (alpha[index] << 24) | (outR << 16) | (outG << 8) | outB;
            }
            return target;
        }

        private double averageGray() {
            long total = 0L;
            int count = 0;
            for (int index = 0; index < gray.length; index++) {
                if (alpha[index] == 0) {
                    continue;
                }
                total += gray[index];
                count++;
            }
            return count == 0 ? 0.0D : total / (double) count;
        }
    }

    @Override
    public String getName() {
        return "Industron structure material textures";
    }
}
