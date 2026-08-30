package net.mads.industron.material.structure;

import net.mads.industron.material.MaterialPart;

import net.mads.industron.Industron;
import net.minecraft.resources.ResourceLocation;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Uses the existing material_sets item library for structure-material forms. */
public final class StructureMaterialVariantResolver {
    private static final Pattern VARIANT = Pattern.compile("variant_(\\d+)");

    private StructureMaterialVariantResolver() {
    }

    public static Optional<ItemTextureSet> itemTextures(StructureMaterial material, MaterialPart part) {
        TextureFamily family = textureFamily(part);
        if (family == null) {
            return Optional.empty();
        }

        List<VariantLocation> variants = discover(family.family(), family.size(), "base.png");
        if (variants.isEmpty()) {
            return Optional.empty();
        }

        int seed = 31 * material.id().hashCode() + 17 * part.ordinal();
        VariantLocation chosen = variants.get(Math.floorMod(seed, variants.size()));
        ResourceLocation base = itemLayer(chosen, "base");
        Optional<ResourceLocation> secondary = fileExists(chosen, "secondary.png")
                ? Optional.of(itemLayer(chosen, "secondary"))
                : Optional.empty();
        Optional<ResourceLocation> overlay = fileExists(chosen, "overlay.png")
                ? Optional.of(itemLayer(chosen, "overlay"))
                : Optional.empty();
        return Optional.of(new ItemTextureSet(base, secondary, overlay));
    }

    private static TextureFamily textureFamily(MaterialPart part) {
        return switch (part) {
            case TINY_DUST, TINY_WOOD_PULP -> new TextureFamily("dust", "tiny");
            case SMALL_DUST, SMALL_WOOD_PULP -> new TextureFamily("dust", "small");
            case DUST, WOOD_PULP -> new TextureFamily("dust", "normal");
            default -> null;
        };
    }

    private static ResourceLocation itemLayer(VariantLocation variant, String layer) {
        return ResourceLocation.fromNamespaceAndPath(
                Industron.MOD_ID,
                "item/material_sets/" + variant.relativePath() + "/" + layer
        );
    }

    private static List<VariantLocation> discover(String family, String size, String requiredFile) {
        String candidate = family + "/" + size;
        for (Path assetRoot : resourceRoots()) {
            Path base = assetRoot.resolve("textures/item/material_sets").resolve(candidate);
            if (!Files.isDirectory(base)) {
                continue;
            }
            try (var stream = Files.list(base)) {
                List<VariantLocation> result = new ArrayList<>();
                stream.filter(Files::isDirectory).forEach(path -> {
                    Matcher matcher = VARIANT.matcher(path.getFileName().toString());
                    if (matcher.matches() && Files.isRegularFile(path.resolve(requiredFile))) {
                        result.add(new VariantLocation(
                                Integer.parseInt(matcher.group(1)),
                                candidate + "/" + path.getFileName()
                        ));
                    }
                });
                result.sort(Comparator.comparingInt(VariantLocation::number));
                if (!result.isEmpty()) {
                    return List.copyOf(result);
                }
            } catch (Exception ignored) {
            }
        }
        return packagedVariants(candidate, requiredFile);
    }

    private static boolean fileExists(VariantLocation variant, String fileName) {
        String relative = "textures/item/material_sets/" + variant.relativePath() + "/" + fileName;
        for (Path assetRoot : resourceRoots()) {
            if (Files.isRegularFile(assetRoot.resolve(relative))) {
                return true;
            }
        }
        try {
            URI location = StructureMaterialVariantResolver.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            Path codePath = Path.of(location);
            if (Files.isRegularFile(codePath) && codePath.toString().endsWith(".jar")) {
                try (JarFile jar = new JarFile(codePath.toFile())) {
                    return jar.getEntry("assets/" + Industron.MOD_ID + "/" + relative) != null;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static List<VariantLocation> packagedVariants(String candidate, String requiredFile) {
        try {
            URI location = StructureMaterialVariantResolver.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            Path codePath = Path.of(location);
            if (!Files.isRegularFile(codePath) || !codePath.toString().endsWith(".jar")) {
                return List.of();
            }
            String prefix = "assets/" + Industron.MOD_ID + "/textures/item/material_sets/" + candidate + "/";
            List<VariantLocation> result = new ArrayList<>();
            try (JarFile jar = new JarFile(codePath.toFile())) {
                jar.stream().forEach(entry -> {
                    String name = entry.getName();
                    if (!name.startsWith(prefix) || !name.endsWith("/" + requiredFile)) {
                        return;
                    }
                    String remainder = name.substring(prefix.length());
                    int slash = remainder.indexOf('/');
                    if (slash <= 0) {
                        return;
                    }
                    String variantName = remainder.substring(0, slash);
                    Matcher matcher = VARIANT.matcher(variantName);
                    if (matcher.matches()) {
                        result.add(new VariantLocation(
                                Integer.parseInt(matcher.group(1)),
                                candidate + "/" + variantName
                        ));
                    }
                });
            }
            result.sort(Comparator.comparingInt(VariantLocation::number));
            return List.copyOf(result);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private static List<Path> resourceRoots() {
        Set<Path> roots = new LinkedHashSet<>();
        addProjectTreeRoots(roots, Path.of("").toAbsolutePath().normalize());
        try {
            URI location = StructureMaterialVariantResolver.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            Path codePath = Path.of(location).toAbsolutePath().normalize();
            if (Files.isDirectory(codePath)) {
                addProjectTreeRoots(roots, codePath);
            }
        } catch (Exception ignored) {
        }
        return List.copyOf(roots);
    }

    private static void addProjectTreeRoots(Set<Path> roots, Path start) {
        Path current = start;
        for (int depth = 0; depth < 10 && current != null; depth++, current = current.getParent()) {
            addIfDirectory(roots, current.resolve(Path.of("src/main/resources/assets", Industron.MOD_ID)));
            addIfDirectory(roots, current.resolve(Path.of("main/resources/assets", Industron.MOD_ID)));
            addIfDirectory(roots, current.resolve(Path.of("src/generated/resources/assets", Industron.MOD_ID)));
            addIfDirectory(roots, current.resolve(Path.of("build/resources/main/assets", Industron.MOD_ID)));
        }
    }

    private static void addIfDirectory(Set<Path> roots, Path path) {
        try {
            Path normalized = path.toAbsolutePath().normalize();
            if (Files.isDirectory(normalized)) {
                roots.add(normalized);
            }
        } catch (Exception ignored) {
        }
    }

    public record ItemTextureSet(
            ResourceLocation base,
            Optional<ResourceLocation> secondary,
            Optional<ResourceLocation> overlay
    ) {
    }

    private record TextureFamily(String family, String size) {
    }

    private record VariantLocation(int number, String relativePath) {
    }
}
