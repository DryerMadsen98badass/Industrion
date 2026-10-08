package net.mads.industron.data;

import com.google.common.hash.Hashing;
import net.mads.industron.Industron;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.structure.GemMaterial;
import net.mads.industron.material.structure.StructureMaterial;
import net.mads.industron.material.structure.StoneModel;
import net.mads.industron.material.structure.StoneMaterial;
import net.mads.industron.material.structure.WoodMaterial;
import net.mads.industron.material.structure.StructureBlockDefinition;
import net.mads.industron.material.structure.StructureMaterialGenerator;
import net.mads.industron.material.structure.StructureMaterialVariantResolver;
import net.mads.industron.material.structure.StructureMaterials;
import net.mads.industron.material.structure.StructureSetResolver;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
    private final StoneTextureResolver stoneTextureResolver;

    public StructureMaterialTextureProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        this.textures = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "textures");
        this.stoneTextureResolver = new StoneTextureResolver(existingFileHelper);
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
        if (material instanceof WoodMaterial wood) {
            files.add(wood.model().id() + "_planks.png");
            for (MaterialPart part : StructureMaterialGenerator.generatedItemForms(wood)) {
                StructureMaterialVariantResolver.woodUtilityTextureFile(part).ifPresent(files::add);
            }
        } else if (material instanceof StoneMaterial stone) {
            files.add(stone.model().baseSideTexture());
            files.add(stone.model().baseTopTexture());
            files.add(stone.model().baseBottomTexture());
        }
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
            BufferedImage finalImage = applyUntintedOverlay(material, fileName, tinted);
            byte[] data = encodePng(finalImage, source);
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
        if (material instanceof StoneMaterial stone) {
            generated += generateStoneItemTextures(output, stone, templateCache);
        }
        if (material instanceof WoodMaterial wood) {
            if (!wood.hasExistingPart(MaterialPart.STICK)) {
                generated += generateWoodStickTexture(output, wood);
            }
            generated += generateWoodRuntimeTextures(output, wood, templateCache);
        }
        return generated;
    }

    /** Entity-rendered wood forms still use the same common grayscale sources, but their generated
     * textures live in Minecraft's entity texture namespaces instead of block/structure_materials. */
    private int generateWoodRuntimeTextures(
            CachedOutput output,
            WoodMaterial wood,
            Map<Path, TemplatePixels> templateCache
    ) throws IOException {
        int generated = 0;
        if (!wood.hasExistingPart(MaterialPart.CHEST)) {
            generated += generateWoodRuntimeTexture(output, wood, templateCache,
                    "chest/normal.png",
                    ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID,
                            "entity/chest/structure_materials/" + wood.id() + "/normal"));
            generated += generateWoodRuntimeTexture(output, wood, templateCache,
                    "chest/normal_left.png",
                    ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID,
                            "entity/chest/structure_materials/" + wood.id() + "/normal_left"));
            generated += generateWoodRuntimeTexture(output, wood, templateCache,
                    "chest/normal_right.png",
                    ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID,
                            "entity/chest/structure_materials/" + wood.id() + "/normal_right"));
        }
        if (!wood.hasExistingPart(MaterialPart.BOAT)) {
            generated += generateWoodRuntimeTexture(output, wood, templateCache,
                    "boat/boat_entity.png",
                    ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID,
                            "entity/boat/structure_materials/" + wood.id()));
        }
        if (!wood.hasExistingPart(MaterialPart.CHEST_BOAT)) {
            generated += generateWoodRuntimeTexture(output, wood, templateCache,
                    "boat/chest_boat_entity.png",
                    ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID,
                            "entity/chest_boat/structure_materials/" + wood.id()));
        }
        return generated;
    }

    private int generateWoodRuntimeTexture(
            CachedOutput output,
            WoodMaterial wood,
            Map<Path, TemplatePixels> templateCache,
            String templateFile,
            ResourceLocation destination
    ) throws IOException {
        StructureSetResolver.ResolvedTemplate resolved = StructureSetResolver.sourceTemplate(wood.model(), templateFile)
                .orElseThrow(() -> new IllegalStateException(
                        "Could not resolve common wood runtime texture " + templateFile + " for " + wood.id()
                ));
        Path source = resolved.path();
        BufferedImage tinted = templatePixels(source, templateCache).render(wood.color(), false, 1.0D);
        byte[] data = encodePng(tinted, source);
        output.writeIfNeeded(texturePath(destination, "png"), data, Hashing.sha1().hashBytes(data));
        return 1;
    }

    /**
     * A common wood template may provide an optional sibling <name>_overlay.png. The base is
     * grayscale/tinted, while the overlay is copied verbatim. This is used by Bookshelf so the
     * wood changes with the material but the book colors never do.
     */
    private static BufferedImage applyUntintedOverlay(
            StructureMaterial material,
            String fileName,
            BufferedImage tinted
    ) throws IOException {
        if (!(material instanceof WoodMaterial)) return tinted;
        int dot = fileName.lastIndexOf('.');
        String overlayFile = dot >= 0
                ? fileName.substring(0, dot) + "_overlay" + fileName.substring(dot)
                : fileName + "_overlay.png";
        var overlayTemplate = StructureSetResolver.sourceTemplate(material.model(), overlayFile);
        if (overlayTemplate.isEmpty()) return tinted;

        BufferedImage overlay = ImageIO.read(overlayTemplate.get().path().toFile());
        if (overlay == null) {
            throw new IllegalStateException("Could not decode wood texture overlay: " + overlayTemplate.get().path());
        }
        if (overlay.getWidth() != tinted.getWidth() || overlay.getHeight() != tinted.getHeight()) {
            throw new IllegalStateException(
                    "Wood texture overlay dimensions differ for " + fileName + ": base="
                            + tinted.getWidth() + "x" + tinted.getHeight() + ", overlay="
                            + overlay.getWidth() + "x" + overlay.getHeight()
            );
        }

        BufferedImage result = new BufferedImage(tinted.getWidth(), tinted.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < tinted.getHeight(); y++) {
            for (int x = 0; x < tinted.getWidth(); x++) {
                int base = tinted.getRGB(x, y);
                int over = overlay.getRGB(x, y);
                int oa = (over >>> 24) & 0xFF;
                if (oa == 0) {
                    result.setRGB(x, y, base);
                    continue;
                }
                if (oa == 255) {
                    result.setRGB(x, y, over);
                    continue;
                }
                int ba = (base >>> 24) & 0xFF;
                int outA = oa + ba * (255 - oa) / 255;
                if (outA == 0) continue;
                int or = (over >>> 16) & 0xFF, og = (over >>> 8) & 0xFF, ob = over & 0xFF;
                int br = (base >>> 16) & 0xFF, bg = (base >>> 8) & 0xFF, bb = base & 0xFF;
                int outR = (or * oa + br * ba * (255 - oa) / 255) / outA;
                int outG = (og * oa + bg * ba * (255 - oa) / 255) / outA;
                int outB = (ob * oa + bb * ba * (255 - oa) / 255) / outA;
                result.setRGB(x, y, (outA << 24) | (outR << 16) | (outG << 8) | outB);
            }
        }
        return result;
    }

    private int generateWoodStickTexture(CachedOutput output, WoodMaterial wood) throws IOException {
        ResourceLocation templateTexture = StructureMaterialVariantResolver
                .materialSetTemplateTextures(wood, MaterialPart.STICK)
                .orElseThrow(() -> new IllegalStateException("Missing grayscale stick template for " + wood.id()))
                .base();
        BufferedImage template = readResourceTexture(templateTexture);
        BufferedImage colored = tintGrayscale(template, wood.color());
        ResourceLocation destination = StructureMaterialVariantResolver.generatedWoodStickTexture(wood);
        byte[] data = encodePng(colored, Path.of(templateTexture.toString().replace(':', '_') + ".png"));
        output.writeIfNeeded(texturePath(destination, "png"), data, Hashing.sha1().hashBytes(data));
        return 1;
    }

    private static BufferedImage tintGrayscale(BufferedImage source, int rgb) {
        int targetR = (rgb >> 16) & 0xFF;
        int targetG = (rgb >> 8) & 0xFF;
        int targetB = rgb & 0xFF;
        BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int argb = source.getRGB(x, y);
                int alpha = (argb >>> 24) & 0xFF;
                if (alpha == 0) continue;
                int r = (argb >>> 16) & 0xFF;
                int g = (argb >>> 8) & 0xFF;
                int b = argb & 0xFF;
                int gray = (299 * r + 587 * g + 114 * b) / 1000;
                double factor = gray / 128.0D;
                int outR = clamp((int) Math.round(targetR * factor));
                int outG = clamp((int) Math.round(targetG * factor));
                int outB = clamp((int) Math.round(targetB * factor));
                result.setRGB(x, y, (alpha << 24) | (outR << 16) | (outG << 8) | outB);
            }
        }
        return result;
    }

    private int generateStoneItemTextures(
            CachedOutput output,
            StoneMaterial stone,
            Map<Path, TemplatePixels> templateCache
    ) throws IOException {
        BufferedImage stoneTexture;
        Path sourceLabel;

        if (stone.hasExistingPart(MaterialPart.STONE)) {
            ResourceLocation existingTexture = stoneTextureResolver.baseSideTexture(stone);
            stoneTexture = stoneTextureResolver.readTexture(existingTexture);
            sourceLabel = Path.of(existingTexture.getNamespace() + "_" + existingTexture.getPath().replace('/', '_') + ".png");
        } else {
            StructureSetResolver.ResolvedTemplate resolved = StructureSetResolver
                    .sourceTemplate(stone.model(), stone.model().baseSideTexture())
                    .orElseThrow(() -> new IllegalStateException(
                            "Could not resolve base stone texture for " + stone.id()
                    ));
            Path source = resolved.path();
            TemplatePixels template = templatePixels(source, templateCache);
            double grayScale = fallbackGrayScale(stone, resolved, templateCache);
            stoneTexture = template.render(stone.color(), false, grayScale);
            sourceLabel = source;
        }

        int generated = 0;

        for (MaterialPart part : stone.generatedForms()) {
            if (!StructureMaterialVariantResolver.isStoneShapingPart(part)) continue;
            ResourceLocation maskTexture = StructureMaterialVariantResolver.toolTemplateTextures(stone, part)
                    .orElseThrow(() -> new IllegalStateException(
                            "Missing tool-part shape texture for " + stone.id() + " " + part.id()
                    ))
                    .base();
            BufferedImage mask = readResourceTexture(maskTexture);
            BufferedImage result = applyStoneTextureMask(stoneTexture, mask, maskTexture, "Stone tool-part");

            ResourceLocation destination = StructureMaterialVariantResolver.generatedStoneToolTexture(stone, part);
            byte[] data = encodePng(result, sourceLabel);
            output.writeIfNeeded(texturePath(destination, "png"), data, Hashing.sha1().hashBytes(data));
            generated++;
        }
        return generated;
    }

    private static BufferedImage applyStoneTextureMask(
            BufferedImage stoneTexture,
            BufferedImage mask,
            ResourceLocation maskTexture,
            String description
    ) {
        if (mask.getWidth() != 16 || mask.getHeight() != 16) {
            throw new IllegalStateException(
                    description + " shape texture must be 16x16: " + maskTexture
                            + " is " + mask.getWidth() + "x" + mask.getHeight()
            );
        }

        BufferedImage result = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        int sourceFrameHeight = Math.min(stoneTexture.getWidth(), stoneTexture.getHeight());
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int maskAlpha = (mask.getRGB(x, y) >>> 24) & 0xFF;
                if (maskAlpha == 0) continue;
                int sx = Math.min(stoneTexture.getWidth() - 1, x * stoneTexture.getWidth() / 16);
                int sy = Math.min(sourceFrameHeight - 1, y * sourceFrameHeight / 16);
                int stoneArgb = stoneTexture.getRGB(sx, sy);
                int stoneAlpha = (stoneArgb >>> 24) & 0xFF;
                int alpha = maskAlpha * stoneAlpha / 255;
                result.setRGB(x, y, (alpha << 24) | (stoneArgb & 0x00FFFFFF));
            }
        }
        return result;
    }

    private static BufferedImage readResourceTexture(ResourceLocation texture) throws IOException {
        String path = "assets/" + texture.getNamespace() + "/textures/" + texture.getPath() + ".png";
        try (InputStream input = StructureMaterialTextureProvider.class.getClassLoader().getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException("Could not open item texture " + texture + " at " + path);
            }
            BufferedImage image = ImageIO.read(input);
            if (image == null) throw new IllegalStateException("Could not decode item texture " + texture);
            return image;
        }
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
