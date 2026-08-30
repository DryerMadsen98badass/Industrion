package net.mads.industron.material.structure;

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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;

/**
 * Resolves grayscale structure-set templates from source resources.
 * Wood and stone select one named family. Metal selects the complete role-based
 * tree because each block role has its own independent numbered variants.
 */
public final class StructureSetResolver {
    private static final String ROOT = "textures/block/structure_sets";
    private static final List<Path> RESOURCE_ROOTS = discoverResourceRoots();
    private static final Map<String, List<String>> TEXTURE_FILES_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Optional<ResolvedTemplate>> SOURCE_TEMPLATE_CACHE = new ConcurrentHashMap<>();

    private StructureSetResolver() {
    }

    public static List<String> textureFiles(StructureModel model) {
        return TEXTURE_FILES_CACHE.computeIfAbsent(modelKey(model), ignored -> discoverTextureFiles(model));
    }

    private static List<String> discoverTextureFiles(StructureModel model) {
        String relativeDirectory = relativeDirectory(model);
        boolean recursive = model instanceof MetalModel;

        for (Path assetRoot : RESOURCE_ROOTS) {
            Path directory = assetRoot.resolve(ROOT).resolve(relativeDirectory);
            if (!Files.isDirectory(directory)) {
                continue;
            }
            try (var stream = recursive ? Files.walk(directory) : Files.list(directory)) {
                List<String> names = stream
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".png"))
                        .map(path -> normalizeRelative(directory.relativize(path)))
                        .sorted()
                        .toList();
                if (!names.isEmpty()) {
                    return List.copyOf(names);
                }
            } catch (Exception ignored) {
                // Try the next source root.
            }
        }

        List<String> packaged = packagedTextureFiles(model);
        if (!packaged.isEmpty()) {
            return packaged;
        }

        if (model == StoneModel.STONE && sharedStoneTemplate("stone.png").isPresent()) {
            return List.of("stone.png");
        }

        return List.of();
    }

    public static boolean hasTexture(StructureModel model, String fileName) {
        return textureFiles(model).contains(normalizeRelative(Path.of(fileName)));
    }

    public static ResourceLocation sourceTexture(StructureModel model, String fileName) {
        String safeFile = safeRelative(fileName);
        return ResourceLocation.fromNamespaceAndPath(
                Industron.MOD_ID,
                "block/structure_sets/" + relativeDirectory(model) + "/" + stripPng(safeFile)
        );
    }

    public static ResourceLocation generatedTexture(StructureMaterial material, String fileName) {
        String safeFile = safeRelative(fileName);
        return ResourceLocation.fromNamespaceAndPath(
                Industron.MOD_ID,
                "block/structure_materials/" + material.id() + "/" + stripPng(safeFile)
        );
    }

    public static Optional<Path> sourcePath(StructureModel model, String fileName) {
        return sourceTemplate(model, fileName).map(ResolvedTemplate::path);
    }

    /**
     * Resolves the template and records which stone model donated a fallback sprite.
     * The donor is used by data generation to normalize fallback luminance before
     * applying the target material tint.
     */
    public static Optional<ResolvedTemplate> sourceTemplate(StructureModel model, String fileName) {
        String safeFile = safeRelative(fileName);
        String cacheKey = modelKey(model) + '\0' + safeFile;
        return SOURCE_TEMPLATE_CACHE.computeIfAbsent(cacheKey, ignored -> discoverSourceTemplate(model, safeFile));
    }

    private static Optional<ResolvedTemplate> discoverSourceTemplate(StructureModel model, String safeFile) {
        Optional<Path> direct = directSourcePath(model, safeFile);
        if (direct.isPresent()) {
            return Optional.of(new ResolvedTemplate(direct.get(), Optional.empty()));
        }

        if (model instanceof StoneModel stoneModel) {
            return stoneFallbackSourceTemplate(stoneModel, safeFile);
        }

        return Optional.empty();
    }

    private static Optional<Path> directSourcePath(StructureModel model, String safeFile) {
        String relativeDirectory = relativeDirectory(model);
        for (Path assetRoot : RESOURCE_ROOTS) {
            Path directory = assetRoot.resolve(ROOT).resolve(relativeDirectory).normalize();
            Path path = directory.resolve(safeFile).normalize();
            if (path.startsWith(directory) && Files.isRegularFile(path)) {
                return Optional.of(path);
            }
        }
        return Optional.empty();
    }

    /**
     * Standard stone roles are guaranteed even when the selected Minecraft/Create family
     * does not ship a matching sprite. Missing cobbled/polished templates therefore reuse
     * a neutral grayscale shape and are tinted with the target StoneMaterial color later.
     */
    private static Optional<ResolvedTemplate> stoneFallbackSourceTemplate(StoneModel model, String safeFile) {
        String fileName = Path.of(safeFile).getFileName().toString();

        if (model == StoneModel.STONE && fileName.equals("stone.png")) {
            return sharedStoneTemplate("stone.png");
        }

        if (fileName.equals("cobblestone.png")) {
            return sharedStoneTemplate("cobblestone.png");
        }

        if (fileName.startsWith("polished_")) {
            return donatedTemplate(StoneModel.DIORITE, "polished_diorite.png");
        }

        return Optional.empty();
    }

    private static Optional<ResolvedTemplate> sharedStoneTemplate(String fileName) {
        String safeFile = safeRelative(fileName);
        for (Path assetRoot : RESOURCE_ROOTS) {
            Path directory = assetRoot.resolve(ROOT).resolve("stone/_shared").normalize();
            Path path = directory.resolve(safeFile).normalize();
            if (path.startsWith(directory) && Files.isRegularFile(path)) {
                return Optional.of(new ResolvedTemplate(path, Optional.empty()));
            }
        }
        return Optional.empty();
    }

    private static Optional<ResolvedTemplate> donatedTemplate(StoneModel donor, String fileName) {
        return directSourcePath(donor, fileName)
                .map(path -> new ResolvedTemplate(path, Optional.of(donor)));
    }

    public static String stripPng(String fileName) {
        return fileName.endsWith(".png")
                ? fileName.substring(0, fileName.length() - 4)
                : fileName;
    }

    private static String modelKey(StructureModel model) {
        return model.getClass().getName() + ':' + model.category() + '/' + model.id();
    }

    private static String relativeDirectory(StructureModel model) {
        if (model instanceof MetalModel) {
            return model.category();
        }
        return model.category() + "/" + model.id();
    }

    private static List<String> packagedTextureFiles(StructureModel model) {
        try {
            URI location = StructureSetResolver.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            Path codePath = Path.of(location);
            if (!Files.isRegularFile(codePath) || !codePath.toString().endsWith(".jar")) {
                return List.of();
            }

            String prefix = "assets/" + Industron.MOD_ID + "/" + ROOT + "/"
                    + relativeDirectory(model) + "/";
            boolean recursive = model instanceof MetalModel;
            List<String> result = new ArrayList<>();
            try (JarFile jar = new JarFile(codePath.toFile())) {
                jar.stream().forEach(entry -> {
                    String name = entry.getName();
                    if (!entry.isDirectory() && name.startsWith(prefix) && name.endsWith(".png")) {
                        String relative = name.substring(prefix.length());
                        if (recursive || !relative.contains("/")) {
                            result.add(relative);
                        }
                    }
                });
            }
            result.sort(Comparator.naturalOrder());
            return List.copyOf(result);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private static String safeRelative(String fileName) {
        Path relative = Path.of(fileName).normalize();
        if (relative.isAbsolute() || relative.startsWith("..")) {
            throw new IllegalArgumentException("Invalid structure texture path: " + fileName);
        }
        return normalizeRelative(relative);
    }

    private static String normalizeRelative(Path path) {
        return path.toString().replace('\\', '/');
    }

    private static List<Path> discoverResourceRoots() {
        Set<Path> roots = new LinkedHashSet<>();
        addProjectTreeRoots(roots, Path.of("").toAbsolutePath().normalize());

        try {
            URI location = StructureSetResolver.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            Path codePath = Path.of(location).toAbsolutePath().normalize();
            if (Files.isDirectory(codePath)) {
                addProjectTreeRoots(roots, codePath);
            }
        } catch (Exception ignored) {
        }

        try {
            var resources = StructureSetResolver.class.getClassLoader()
                    .getResources("assets/" + Industron.MOD_ID);
            while (resources.hasMoreElements()) {
                var url = resources.nextElement();
                if ("file".equalsIgnoreCase(url.getProtocol())) {
                    addIfDirectory(roots, Path.of(url.toURI()));
                }
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
    public record ResolvedTemplate(Path path, Optional<StoneModel> fallbackDonor) {
        public ResolvedTemplate {
            if (path == null) {
                throw new IllegalArgumentException("Resolved structure template path cannot be null");
            }
            fallbackDonor = fallbackDonor == null ? Optional.empty() : fallbackDonor;
        }
    }

}
