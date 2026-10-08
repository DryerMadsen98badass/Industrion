package net.mads.industron.material;

import net.mads.industron.Industron;
import net.mads.industron.fluid.IndustrialFluid;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URI;
import java.util.jar.JarFile;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Dynamic resource variant discovery. Variant counts are never hardcoded. */
public final class MaterialVariantResolver {
    private static final Pattern VARIANT = Pattern.compile("variant_(\\d+)");
    private static final Map<DiscoveryKey, List<VariantLocation>> DISCOVERY_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> RESOURCE_EXISTS_CACHE = new ConcurrentHashMap<>();
    private static volatile List<Path> RESOURCE_ROOTS_CACHE;

    private MaterialVariantResolver() {
    }

    public static Optional<ResourceLocation> model(IndustrialMaterial material, MaterialPart part) {
        if (part.textureFamily() == null || part.isFluid()) return Optional.empty();
        String domain = part.isBlock() ? "block" : "item";
        List<VariantLocation> variants = discoverModelVariants(domain, part);
        if (variants.isEmpty()) return Optional.empty();
        VariantLocation chosen = choose(variants, material, part);
        return Optional.of(ResourceLocation.fromNamespaceAndPath(
                Industron.MOD_ID,
                domain + "/" + resourceCollection(part) + "/" + chosen.relativePath() + "/model"
        ));
    }

    public static Optional<ResourceLocation> texture(IndustrialMaterial material, MaterialPart part, String layer) {
        if (part.textureFamily() == null) return Optional.empty();
        String domain = part.isBlock() || part.isFluid() ? "block" : "item";
        List<VariantLocation> variants = discoverTextureVariants(domain, part, layer);
        if (variants.isEmpty()) return Optional.empty();
        VariantLocation chosen = choose(variants, material, part);
        String resolvedLayer = resolvedTextureLayer(domain, part, layer);
        return Optional.of(ResourceLocation.fromNamespaceAndPath(
                Industron.MOD_ID,
                domain + "/" + resourceCollection(part) + "/" + chosen.relativePath() + "/" + resolvedLayer
        ));
    }


    public static Optional<BlockTextureSet> blockTextures(IndustrialMaterial material, MaterialPart part) {
        if (!part.isBlock() || part.textureFamily() == null) return Optional.empty();

        // Gem base blocks are not metal/block variants. They use a dedicated
        // named grayscale template selected by GemBlockStyle. Existing vanilla
        // gem blocks never reach this path because their MaterialPart.BLOCK is
        // mapped with .existing(...).
        if (part == MaterialPart.BLOCK && material.properties().gemCandidate()) {
            String relative = "textures/block/material_sets/gem/"
                    + material.blockMaterialSet() + "/base.png";
            if (!resourceFileExists(relative)) {
                return Optional.empty();
            }
            ResourceLocation base = ResourceLocation.fromNamespaceAndPath(
                    Industron.MOD_ID,
                    "block/material_sets/gem/" + material.blockMaterialSet() + "/base"
            );
            return Optional.of(new BlockTextureSet(base, Optional.empty(), Optional.empty(), Optional.empty()));
        }

        List<VariantLocation> variants = discoverTextureVariants("block", part, "base");
        if (variants.isEmpty()) return Optional.empty();
        VariantLocation chosen = choose(variants, material, part);
        ResourceLocation base = blockLayer(chosen, "base");
        Optional<ResourceLocation> secondary = textureFileExists("block", part, chosen, "secondary.png")
                ? Optional.of(blockLayer(chosen, "secondary")) : Optional.empty();
        Optional<ResourceLocation> layer2 = textureFileExists("block", part, chosen, "layer2.png")
                ? Optional.of(blockLayer(chosen, "layer2")) : Optional.empty();
        Optional<ResourceLocation> overlay = textureFileExists("block", part, chosen, "overlay.png")
                ? Optional.of(blockLayer(chosen, "overlay")) : Optional.empty();
        return Optional.of(new BlockTextureSet(base, secondary, layer2, overlay));
    }

    private static ResourceLocation blockLayer(VariantLocation variant, String layer) {
        return ResourceLocation.fromNamespaceAndPath(
                Industron.MOD_ID,
                "block/material_sets/" + variant.relativePath() + "/" + layer
        );
    }

    public static Optional<ItemTextureSet> itemTextures(IndustrialMaterial material, MaterialPart part) {
        if (!part.isItem() || part.textureFamily() == null) return Optional.empty();
        List<VariantLocation> variants = discoverTextureVariants("item", part, "base");
        if (variants.isEmpty()) return Optional.empty();
        VariantLocation chosen = choose(variants, material, part);
        ResourceLocation base = itemLayer(part, chosen, resolvedTextureLayer("item", part, "base"));
        Optional<ResourceLocation> secondary = textureFileExists("item", part, chosen, "secondary.png")
                ? Optional.of(itemLayer(part, chosen, "secondary")) : Optional.empty();
        Optional<ResourceLocation> overlay = textureFileExists("item", part, chosen, "overlay.png")
                ? Optional.of(itemLayer(part, chosen, "overlay")) : Optional.empty();
        return Optional.of(new ItemTextureSet(base, secondary, overlay));
    }

    private static ResourceLocation itemLayer(MaterialPart part, VariantLocation variant, String layer) {
        return ResourceLocation.fromNamespaceAndPath(
                Industron.MOD_ID,
                "item/" + resourceCollection(part) + "/" + variant.relativePath() + "/" + layer
        );
    }

    public static MaterialPart coldTexturePart(MaterialPart part) {
        if (part == MaterialPart.HOT_DRILL) return MaterialPart.DRILL;
        if (part == MaterialPart.HOT_SAW_HANDLE) return MaterialPart.SAW_HANDLE;
        if (part == MaterialPart.HOT_WIRE_CUTTER_BODY) return MaterialPart.WIRE_CUTTER_BODY;
        var form = net.mads.industron.machine.foundry.casting.CastingDefinitions.cold(part);
        return form != null && form.hot() == part ? form.cold() : part;
    }

    public static Optional<ResourceLocation> hotOverlayTexture(IndustrialMaterial material, MaterialPart part) {
        MaterialPart cold = coldTexturePart(part);
        if (cold == part) return Optional.empty();
        // Select from base variants so the overlay always matches the item's silhouette.
        List<VariantLocation> variants = discoverTextureVariants("item", cold, "base");
        if (variants.isEmpty()) return Optional.empty();
        VariantLocation chosen = choose(variants, material, cold);
        return textureFileExists("item", cold, chosen, "hot_overlay.png")
                ? Optional.of(itemLayer(cold, chosen, "hot_overlay")) : Optional.empty();
    }

    public static Optional<ResourceLocation> hotIngotOverlayTexture(IndustrialMaterial material) {
        List<VariantLocation> variants = discover(
                "textures/item/material_sets",
                List.of("ingots/ingot_hot", "ingot_hot"),
                "ingot_hot_overlay.png"
        );
        if (variants.isEmpty()) return Optional.empty();
        VariantLocation chosen = choose(variants, material, MaterialPart.HOT_INGOT);
        return Optional.of(ResourceLocation.fromNamespaceAndPath(
                Industron.MOD_ID,
                "item/material_sets/" + chosen.relativePath() + "/ingot_hot_overlay"
        ));
    }

    public static Optional<ResourceLocation> magneticOverlayTexture(IndustrialMaterial material, MaterialPart part) {
        List<VariantLocation> variants = discover(
                "textures/item/material_sets",
                List.of("magnetic_overlay"),
                "overlay.png"
        );
        String layer = "overlay";
        if (variants.isEmpty()) {
            variants = discover("textures/item/material_sets", List.of("magnetic_overlay"), "base.png");
            layer = "base";
        }
        if (variants.isEmpty()) return Optional.empty();
        VariantLocation chosen = choose(variants, material, part);
        return Optional.of(ResourceLocation.fromNamespaceAndPath(
                Industron.MOD_ID,
                "item/material_sets/" + chosen.relativePath() + "/" + layer
        ));
    }

    public static Optional<ResourceLocation> fluidTexture(IndustrialFluid fluid, String layer) {
        List<String> paths = List.of(fluid.textureName());
        List<VariantLocation> variants = discover("textures/block/material_sets", paths, layer + ".png");
        if (variants.isEmpty()) return Optional.empty();
        int seed = 31 * fluid.id().hashCode() + fluid.kind().ordinal();
        VariantLocation chosen = variants.get(Math.floorMod(seed, variants.size()));
        return Optional.of(ResourceLocation.fromNamespaceAndPath(
                Industron.MOD_ID,
                "block/material_sets/" + chosen.relativePath() + "/" + layer
        ));
    }

    public static int variantCount(String domain, MaterialPart part) {
        return discoverTextureVariants(domain, part, "base").size();
    }

    private static VariantLocation choose(List<VariantLocation> variants, IndustrialMaterial material, MaterialPart part) {
        int seed = 31 * material.atomicNumber()
                + 17 * material.properties().tierMultiplier()
                + part.id().hashCode();
        return variants.get(Math.floorMod(seed, variants.size()));
    }

    private static List<VariantLocation> discoverModelVariants(String domain, MaterialPart part) {
        List<String> paths = new ArrayList<>();
        addFamilyPaths(paths, part.textureFamily(), part.textureSize());
        if (part.legacyTextureFamily() != null) paths.add(part.legacyTextureFamily());
        return discover("models/" + domain + "/" + resourceCollection(part), paths, "model.json");
    }

    private static List<VariantLocation> discoverTextureVariants(String domain, MaterialPart part, String layer) {
        List<String> paths = new ArrayList<>();
        if (isNormalOre(part)) {
            paths.add("ores/ore_normal");
            paths.add("ore/normal");
            paths.add("ore");
        } else if (part.isSmallOre()) {
            paths.add("ores/ore_small");
            paths.add("ore/small");
            paths.add("ore_small");
        } else {
            addFamilyPaths(paths, part.textureFamily(), part.textureSize());
            // Prefer the current family before old aliases. Otherwise stale generated resources
            // such as frame_gt can shadow the real frame/variant_n texture directories.
            if (part.legacyTextureFamily() != null) paths.add(part.legacyTextureFamily());
        }
        String resolvedLayer = resolvedTextureLayer(domain, part, layer);
        return discover("textures/" + domain + "/" + resourceCollection(part), paths, resolvedLayer + ".png");
    }

    /** Explicit modern paths replace suffix searches, which mixed block with clay/block. */
    private static void addFamilyPaths(List<String> paths, String family, String size) {
        if (family == null) return;
        String modern = switch (family) {
            case "ingot" -> "double".equals(size) ? "ingots/ingot_double" : "ingots/ingot";
            case "nugget" -> "nuggets/nugget";
            case "plate" -> "plates/plate";
            case "plate_double" -> "plates/double_plate";
            case "plate_dense" -> "plates/dense_plate";
            case "plate_reinforced" -> "plates/plate_reinforced";
            case "plate_heat_exchanger" -> "plates/plate_heat_exchanger";
            case "gem" -> "gems/gem";
            case "gem_rough" -> "gems/rough";
            default -> family;
        };
        if (size != null) paths.add(modern + "/" + size);
        paths.add(modern);
        // Keep exact legacy paths available; never search unrelated parent directories.
        if (!modern.equals(family)) {
            if (size != null) paths.add(family + "/" + size);
            paths.add(family);
        }
    }

    private static String resourceCollection(MaterialPart part) {
        MaterialPart cold = coldTexturePart(part);
        return switch (cold) {
            case SHEARS_HEAD, SHEARS_HANDLE, CROSSBOW_LIMBS, CROSSBOW_TRIGGER, FISHING_HOOK, SHIELD_BODY, SHIELD_HANDLE, HELMET_SHELL, CHESTPLATE_SHELL, LEGGINGS_SHELL, BOOTS_SHELL, SNAP_RING_PLIERS_HEAD, SNAP_RING_PLIERS_HANDLE, BEARING_PRESS_HEAD, BEARING_PRESS_HANDLE, CLAMP_HEAD, CLAMP_HANDLE, CRIMPING_TOOL_HEAD, CRIMPING_TOOL_HANDLE, GEAR_CUTTER_HEAD, GEAR_CUTTER_HANDLE, BOW_BODY, CROSSBOW_STOCK, FISHING_ROD_BODY -> "tool";
            case TOOL_HEAD_AXE, TOOL_HEAD_CHAINSAW, TOOL_HEAD_CHISEL, TOOL_HEAD_CROWBAR,
                    TOOL_HEAD_DRILL, TOOL_HEAD_FILE, TOOL_HEAD_HAMMER, TOOL_HEAD_HOE,
                    TOOL_HEAD_PICKAXE, TOOL_HEAD_SCREWDRIVER, TOOL_HEAD_SHOVEL,
                    TOOL_HEAD_WIRE_CUTTER, WRENCH, KNIFE_BLADE, SWORD_BLADE, SAW_BLADE, TOOL_HANDLE,
                    SAW_HANDLE, WIRE_CUTTER_BODY, DRILL_BODY, CHAINSAW_BODY -> "tool";
            default -> "material_sets";
        };
    }

    /**
     * Some tool component sprites intentionally use semantic filenames (head.png, handle.png,
     * saw.png, wrench.png) instead of base.png. Keep that file naming while still treating the
     * sprite as the generated material item's primary layer.
     */
    private static String resolvedTextureLayer(String domain, MaterialPart part, String requestedLayer) {
        if (!"item".equals(domain) || !"base".equals(requestedLayer)) return requestedLayer;
        MaterialPart cold = coldTexturePart(part);
        return switch (cold) {
            case TOOL_HEAD_AXE, TOOL_HEAD_CHISEL, TOOL_HEAD_CROWBAR, TOOL_HEAD_FILE, TOOL_HEAD_HAMMER,
                    TOOL_HEAD_HOE, TOOL_HEAD_PICKAXE, TOOL_HEAD_SHOVEL, KNIFE_BLADE, SWORD_BLADE -> "head";
            case TOOL_HEAD_DRILL -> "drill";
            case SAW_BLADE -> "saw";
            case TOOL_HANDLE, SAW_HANDLE -> "handle";
            case WRENCH -> "wrench";
            case WIRE_CUTTER_BODY -> "wire_cutter_base";
            case DRILL_BODY -> "body";
            case CHAINSAW_BODY -> "chainsaw_body";
            default -> "base";
        };
    }

    private static boolean isNormalOre(MaterialPart part) {
        return part.isOre() && !part.isSmallOre();
    }

    private static List<VariantLocation> discover(String relativeRoot, List<String> candidatePaths, String requiredFile) {
        DiscoveryKey key = new DiscoveryKey(relativeRoot, List.copyOf(candidatePaths), requiredFile);
        return DISCOVERY_CACHE.computeIfAbsent(key, MaterialVariantResolver::discoverUncached);
    }

    private static List<VariantLocation> discoverUncached(DiscoveryKey key) {
        // Resolve one candidate family at a time (new path before legacy aliases), but merge that
        // family across every resource root. Returning from the first root allowed stale processed
        // resources with one variant to hide newer source resources containing more variants.
        for (String candidate : key.candidatePaths()) {
            List<VariantLocation> found = new ArrayList<>();
            for (Path assetRoot : resourceRoots()) {
                Path materialSetRoot = assetRoot.resolve(key.relativeRoot());
                if (!Files.isDirectory(materialSetRoot)) continue;

                try (var walk = Files.walk(materialSetRoot, 6)) {
                    List<Path> matchingBases = walk
                            .filter(Files::isDirectory)
                            .filter(path -> matchesCandidate(normalizedRelative(materialSetRoot, path), candidate))
                            .sorted()
                            .toList();

                    for (Path base : matchingBases) {
                        try (var stream = Files.list(base)) {
                            stream.filter(Files::isDirectory).forEach(path -> {
                                Matcher matcher = VARIANT.matcher(path.getFileName().toString());
                                if (matcher.matches() && Files.exists(path.resolve(key.requiredFile()))) {
                                    String relativeBase = normalizedRelative(materialSetRoot, base);
                                    addUnique(found, new VariantLocation(
                                            Integer.parseInt(matcher.group(1)),
                                            relativeBase + "/" + path.getFileName()
                                    ));
                                }
                            });
                        }
                    }
                } catch (Exception ignored) {
                    // Try the next source for the same candidate family.
                }
            }

            // In production NeoForge may expose resources through a non-standard/union classpath
            // rather than a normal filesystem or code-source JarFile. Exact classloader probing is
            // reliable there and naturally discovers any contiguous variant_1..variant_n set.
            for (VariantLocation location : discoverClasspath(
                    key.relativeRoot(), candidate, key.requiredFile())) {
                addUnique(found, location);
            }

            // Jar scanning additionally preserves support for deliberately non-contiguous variant
            // numbers, which the fast classpath probe cannot infer after the first gap.
            for (VariantLocation location : discoverPackaged(
                    key.relativeRoot(), List.of(candidate), key.requiredFile())) {
                addUnique(found, location);
            }

            found.sort(Comparator.comparingInt(VariantLocation::number)
                    .thenComparing(VariantLocation::relativePath));
            if (!found.isEmpty()) return List.copyOf(found);
        }
        return List.of();
    }

    private static List<VariantLocation> discoverClasspath(
            String relativeRoot,
            String candidate,
            String requiredFile
    ) {
        List<VariantLocation> found = new ArrayList<>();
        ClassLoader loader = MaterialVariantResolver.class.getClassLoader();
        for (int number = 1; ; number++) {
            String relativePath = candidate + "/variant_" + number;
            String resource = "assets/" + Industron.MOD_ID + "/" + relativeRoot
                    + "/" + relativePath + "/" + requiredFile;
            if (loader.getResource(resource) == null) break;
            found.add(new VariantLocation(number, relativePath));
        }
        return found;
    }

    private static void addUnique(List<VariantLocation> variants, VariantLocation candidate) {
        if (!variants.contains(candidate)) variants.add(candidate);
    }

    private static String normalizedRelative(Path root, Path path) {
        return root.relativize(path).toString().replace('\\', '/');
    }

    private static boolean matchesCandidate(String path, String candidate) {
        return path.equals(candidate);
    }

    private static boolean resourceFileExists(String relative) {
        return RESOURCE_EXISTS_CACHE.computeIfAbsent(relative, MaterialVariantResolver::resourceFileExistsUncached);
    }

    private static boolean resourceFileExistsUncached(String relative) {
        for (Path root : resourceRoots()) {
            if (Files.exists(root.resolve(relative))) {
                return true;
            }
        }
        if (MaterialVariantResolver.class.getClassLoader()
                .getResource("assets/" + Industron.MOD_ID + "/" + relative) != null) {
            return true;
        }
        try {
            URI location = MaterialVariantResolver.class.getProtectionDomain().getCodeSource().getLocation().toURI();
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

    private static boolean textureFileExists(String domain, MaterialPart part, VariantLocation variant, String fileName) {
        String relative = "textures/" + domain + "/" + resourceCollection(part) + "/" + variant.relativePath() + "/" + fileName;
        return resourceFileExists(relative);
    }

    private static List<VariantLocation> discoverPackaged(
            String relativeRoot,
            List<String> candidatePaths,
            String requiredFile
    ) {
        try {
            URI location = MaterialVariantResolver.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            Path codePath = Path.of(location);
            if (!Files.isRegularFile(codePath) || !codePath.toString().endsWith(".jar")) return List.of();

            String rootPrefix = "assets/" + Industron.MOD_ID + "/" + relativeRoot + "/";
            try (JarFile jar = new JarFile(codePath.toFile())) {
                for (String candidate : candidatePaths) {
                    List<VariantLocation> found = new ArrayList<>();
                    jar.stream().forEach(entry -> {
                        String name = entry.getName();
                        if (!name.startsWith(rootPrefix) || !name.endsWith("/" + requiredFile)) return;
                        String relative = name.substring(rootPrefix.length());
                        String marker = "/" + requiredFile;
                        relative = relative.substring(0, relative.length() - marker.length());
                        int slash = relative.lastIndexOf('/');
                        if (slash <= 0) return;
                        String variantName = relative.substring(slash + 1);
                        String basePath = relative.substring(0, slash);
                        if (!matchesCandidate(basePath, candidate)) return;
                        Matcher matcher = VARIANT.matcher(variantName);
                        if (!matcher.matches()) return;
                        VariantLocation locationEntry = new VariantLocation(
                                Integer.parseInt(matcher.group(1)),
                                basePath + "/" + variantName
                        );
                        if (!found.contains(locationEntry)) found.add(locationEntry);
                    });
                    found.sort(Comparator.comparingInt(VariantLocation::number));
                    if (!found.isEmpty()) return found;
                }
            }
        } catch (Exception ignored) {
            // No packaged variants available.
        }
        return List.of();
    }

    private static List<Path> resourceRoots() {
        List<Path> cached = RESOURCE_ROOTS_CACHE;
        if (cached != null) {
            return cached;
        }

        synchronized (MaterialVariantResolver.class) {
            cached = RESOURCE_ROOTS_CACHE;
            if (cached != null) {
                return cached;
            }

            Set<Path> roots = new LinkedHashSet<>();

            // Search from the working directory and its parents. runData is commonly
            // launched from a run/ or IDE child directory, not necessarily project root.
            addProjectTreeRoots(roots, Path.of("").toAbsolutePath().normalize());

            // Also search upward from the compiled class location in dev.
            try {
                URI location = MaterialVariantResolver.class.getProtectionDomain().getCodeSource().getLocation().toURI();
                Path codePath = Path.of(location).toAbsolutePath().normalize();
                if (Files.isDirectory(codePath)) {
                    addProjectTreeRoots(roots, codePath);
                }
            } catch (Exception ignored) {
            }

            // Processed resources may already be directly on the classpath.
            try {
                var resources = MaterialVariantResolver.class.getClassLoader()
                        .getResources("assets/" + Industron.MOD_ID);
                while (resources.hasMoreElements()) {
                    var url = resources.nextElement();
                    if (!"file".equalsIgnoreCase(url.getProtocol())) continue;
                    addIfDirectory(roots, Path.of(url.toURI()));
                }
            } catch (Exception ignored) {
            }

            RESOURCE_ROOTS_CACHE = List.copyOf(roots);
            return RESOURCE_ROOTS_CACHE;
        }
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
            if (Files.isDirectory(normalized)) roots.add(normalized);
        } catch (Exception ignored) {
        }
    }

    public record BlockTextureSet(
            ResourceLocation base,
            Optional<ResourceLocation> secondary,
            Optional<ResourceLocation> layer2,
            Optional<ResourceLocation> overlay
    ) {
    }

    public record ItemTextureSet(
            ResourceLocation base,
            Optional<ResourceLocation> secondary,
            Optional<ResourceLocation> overlay
    ) {
    }

    private record DiscoveryKey(String relativeRoot, List<String> candidatePaths, String requiredFile) {
    }

    private record VariantLocation(int number, String relativePath) {
    }
}
