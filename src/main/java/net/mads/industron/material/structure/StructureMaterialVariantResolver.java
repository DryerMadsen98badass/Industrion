package net.mads.industron.material.structure;

import net.mads.industron.material.MaterialPart;
import net.mads.industron.tool.ToolMaterialRules;

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
    private static final List<Path> RESOURCE_ROOTS = resourceRoots();
    private static final java.util.Map<String, List<VariantLocation>> DISCOVERED = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<String, List<ToolVariantLocation>> DISCOVERED_TOOL_TEMPLATES = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<String, Boolean> EXISTS = new java.util.concurrent.ConcurrentHashMap<>();

    private StructureMaterialVariantResolver() {
    }

    public static Optional<ItemTextureSet> itemTextures(StructureMaterial material, MaterialPart part) {
        if (material instanceof WoodMaterial wood && part == MaterialPart.STICK && !wood.hasExistingPart(MaterialPart.STICK)) {
            return Optional.of(new ItemTextureSet(
                    generatedWoodStickTexture(wood),
                    Optional.empty(),
                    Optional.empty()
            ));
        }
        if (material instanceof WoodMaterial wood && part == MaterialPart.BARK) {
            ResourceLocation barkTexture = wood.hasExistingPart(MaterialPart.LOG)
                    ? ResourceLocation.fromNamespaceAndPath(
                            wood.existingPart(MaterialPart.LOG).getNamespace(),
                            "block/" + wood.existingPart(MaterialPart.LOG).getPath()
                    )
                    : StructureSetResolver.generatedTexture(wood, wood.model().id() + "_planks.png");
            return Optional.of(new ItemTextureSet(barkTexture, Optional.empty(), Optional.empty()));
        }
        if (material instanceof WoodMaterial wood) {
            Optional<String> utilityTexture = woodUtilityTextureFile(part);
            if (utilityTexture.isPresent()) {
                return Optional.of(new ItemTextureSet(
                        StructureSetResolver.generatedTexture(wood, utilityTexture.get()),
                        Optional.empty(),
                        Optional.empty()
                ));
            }
        }
        if (material instanceof StoneMaterial stone) {
            if (isStoneShapingPart(part)) {
                return Optional.of(new ItemTextureSet(
                        generatedStoneToolTexture(stone, part),
                        Optional.empty(),
                        Optional.empty()
                ));
            }
        }

        Optional<ItemTextureSet> toolTextures = toolTemplateTextures(material, part);
        if (toolTextures.isPresent()) {
            return toolTextures;
        }
        return materialSetTemplateTextures(material, part);
    }

    /** Grayscale material-set sprite used only as a shape/alpha template during datagen. */
    public static Optional<ItemTextureSet> materialSetTemplateTextures(StructureMaterial material, MaterialPart part) {
        TextureFamily family = textureFamily(part);
        if (family == null) return Optional.empty();

        List<VariantLocation> variants = discover(
                "textures/item/material_sets",
                family.family(),
                family.size(),
                "base.png"
        );
        if (variants.isEmpty()) return Optional.empty();

        int seed = 31 * material.id().hashCode() + 17 * part.ordinal();
        VariantLocation chosen = variants.get(Math.floorMod(seed, variants.size()));
        ResourceLocation base = itemLayer("material_sets", chosen, "base");
        Optional<ResourceLocation> secondary = fileExists("textures/item/material_sets", chosen, "secondary.png")
                ? Optional.of(itemLayer("material_sets", chosen, "secondary"))
                : Optional.empty();
        Optional<ResourceLocation> overlay = fileExists("textures/item/material_sets", chosen, "overlay.png")
                ? Optional.of(itemLayer("material_sets", chosen, "overlay"))
                : Optional.empty();
        return Optional.of(new ItemTextureSet(base, secondary, overlay));
    }

    /**
     * Generic tool-part sprite resolved directly from MaterialPart texture metadata.
     * No MaterialPart switch lives here: adding a tool part to a structure material is enough
     * as long as its MaterialPart texture family points at a real tool texture directory.
     */
    public static Optional<ItemTextureSet> toolTemplateTextures(StructureMaterial material, MaterialPart part) {
        if (material == null || part == null || part.textureFamily() == null) return Optional.empty();

        List<ToolVariantLocation> variants = discoverToolTemplates(part.textureFamily(), part.textureSize());
        if (variants.isEmpty()) return Optional.empty();

        int seed = 31 * material.id().hashCode() + 17 * part.ordinal();
        ToolVariantLocation chosen = variants.get(Math.floorMod(seed, variants.size()));
        VariantLocation variant = new VariantLocation(chosen.number(), chosen.relativePath());
        ResourceLocation base = itemLayer("tool", variant, stripPng(chosen.baseFile()));
        Optional<ResourceLocation> secondary = fileExists("textures/item/tool", variant, "secondary.png")
                ? Optional.of(itemLayer("tool", variant, "secondary"))
                : Optional.empty();
        Optional<ResourceLocation> overlay = fileExists("textures/item/tool", variant, "overlay.png")
                ? Optional.of(itemLayer("tool", variant, "overlay"))
                : Optional.empty();
        return Optional.of(new ItemTextureSet(base, secondary, overlay));
    }

    /**
     * Stone Shaping eligibility is data-driven: the part must be used by a real tool definition
     * and declare a tool texture family. StoneMaterial.generatedForms() decides whether a
     * particular stone actually exposes that part; datagen fails loudly if its template is missing.
     */
    public static boolean isStoneShapingPart(MaterialPart part) {
        return part != null
                && ToolMaterialRules.isAssemblyToolPart(part)
                && part.textureFamily() != null;
    }

    /**
     * Item-only wood forms use one shared shape template from structure_sets/wood/common.
     * The same file is recolored per WoodMaterial by StructureMaterialTextureProvider.
     */
    public static Optional<String> woodUtilityTextureFile(MaterialPart part) {
        if (part == null) return Optional.empty();
        return switch (part) {
            case BOWL -> Optional.of("bowl/bowl.png");
            case BOAT -> Optional.of("boat/boat.png");
            case CHEST_BOAT -> Optional.of("boat/chest_boat.png");
            default -> Optional.empty();
        };
    }

    /** Pre-colored stick texture generated from the shared grayscale vanilla-stick template. */
    public static ResourceLocation generatedWoodStickTexture(WoodMaterial wood) {
        return ResourceLocation.fromNamespaceAndPath(
                Industron.MOD_ID,
                "item/structure_materials/" + wood.id() + "/stick"
        );
    }

    public static ResourceLocation generatedStoneToolTexture(StoneMaterial stone, MaterialPart part) {
        if (!isStoneShapingPart(part)) {
            throw new IllegalArgumentException("Not a Stone Shaping part: " + part);
        }
        return ResourceLocation.fromNamespaceAndPath(
                Industron.MOD_ID,
                "item/structure_materials/" + stone.id() + "/" + part.id()
        );
    }

    private static TextureFamily textureFamily(MaterialPart part) {
        return switch (part) {
            case TINY_DUST, TINY_WOOD_PULP -> new TextureFamily("dust", "tiny");
            case SMALL_DUST, SMALL_WOOD_PULP -> new TextureFamily("dust", "small");
            case DUST, WOOD_PULP -> new TextureFamily("dust", "normal");
            case STICK -> new TextureFamily("stick", "normal");
            case PLATE -> new TextureFamily("plates/plate", "normal");
            case SMALL_GEAR -> new TextureFamily("gear", "small");
            case GEAR -> new TextureFamily("gear", "normal");
            case RING -> new TextureFamily("ring", "normal");
            case WOOD_PEG -> new TextureFamily("peg", "normal");
            default -> null;
        };
    }

    private static String stripPng(String fileName) {
        return fileName.endsWith(".png") ? fileName.substring(0, fileName.length() - 4) : fileName;
    }

    private static ResourceLocation itemLayer(String collection, VariantLocation variant, String layer) {
        return ResourceLocation.fromNamespaceAndPath(
                Industron.MOD_ID,
                "item/" + collection + "/" + variant.relativePath() + "/" + layer
        );
    }

    private static List<ToolVariantLocation> discoverToolTemplates(String family, String size) {
        if (family == null || family.isBlank()) return List.of();
        String candidate = size == null || size.isBlank() ? family : family + "/" + size;
        return DISCOVERED_TOOL_TEMPLATES.computeIfAbsent(candidate, StructureMaterialVariantResolver::discoverToolTemplatesUncached);
    }

    private static List<ToolVariantLocation> discoverToolTemplatesUncached(String candidate) {
        for (Path assetRoot : RESOURCE_ROOTS) {
            Path base = assetRoot.resolve("textures/item/tool").resolve(candidate);
            if (!Files.isDirectory(base)) continue;
            try (var stream = Files.list(base)) {
                List<ToolVariantLocation> result = new ArrayList<>();
                stream.filter(Files::isDirectory).forEach(path -> {
                    Matcher matcher = VARIANT.matcher(path.getFileName().toString());
                    if (!matcher.matches()) return;
                    String baseFile = primaryToolFile(path);
                    if (baseFile == null) return;
                    result.add(new ToolVariantLocation(
                            Integer.parseInt(matcher.group(1)),
                            candidate + "/" + path.getFileName(),
                            baseFile
                    ));
                });
                result.sort(Comparator.comparingInt(ToolVariantLocation::number));
                if (!result.isEmpty()) return List.copyOf(result);
            } catch (Exception ignored) {
            }
        }
        return packagedToolTemplates(candidate);
    }

    private static String primaryToolFile(Path variantDirectory) {
        try (var stream = Files.list(variantDirectory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(StructureMaterialVariantResolver::isPrimaryToolFile)
                    .sorted()
                    .findFirst()
                    .orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean isPrimaryToolFile(String fileName) {
        if (fileName == null || !fileName.endsWith(".png")) return false;
        return !fileName.equals("overlay.png")
                && !fileName.equals("secondary.png")
                && !fileName.equals("hot_overlay.png")
                && !fileName.equals("mold.png")
                && !fileName.equals("terracotta.png");
    }

    private static List<ToolVariantLocation> packagedToolTemplates(String candidate) {
        try {
            URI location = StructureMaterialVariantResolver.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            Path codePath = Path.of(location);
            if (!Files.isRegularFile(codePath) || !codePath.toString().endsWith(".jar")) return List.of();

            String prefix = "assets/" + Industron.MOD_ID + "/textures/item/tool/" + candidate + "/";
            java.util.Map<Integer, ToolVariantLocation> chosen = new java.util.TreeMap<>();
            try (JarFile jar = new JarFile(codePath.toFile())) {
                jar.stream().forEach(entry -> {
                    String name = entry.getName();
                    if (!name.startsWith(prefix) || entry.isDirectory()) return;
                    String remainder = name.substring(prefix.length());
                    int slash = remainder.indexOf('/');
                    if (slash <= 0) return;
                    String variantName = remainder.substring(0, slash);
                    Matcher matcher = VARIANT.matcher(variantName);
                    if (!matcher.matches()) return;
                    String fileName = remainder.substring(slash + 1);
                    if (fileName.contains("/") || !isPrimaryToolFile(fileName)) return;
                    int number = Integer.parseInt(matcher.group(1));
                    ToolVariantLocation candidateLocation = new ToolVariantLocation(
                            number, candidate + "/" + variantName, fileName
                    );
                    chosen.merge(number, candidateLocation, (left, right) ->
                            left.baseFile().compareTo(right.baseFile()) <= 0 ? left : right
                    );
                });
            }
            return List.copyOf(chosen.values());
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private static List<VariantLocation> discover(
            String relativeRoot,
            String family,
            String size,
            String requiredFile
    ) {
        String candidate = size == null || size.isBlank() ? family : family + "/" + size;
        String key = relativeRoot + "|" + candidate + "|" + requiredFile;
        return DISCOVERED.computeIfAbsent(key, ignored -> discoverUncached(relativeRoot, candidate, requiredFile));
    }

    private static List<VariantLocation> discoverUncached(String relativeRoot, String candidate, String requiredFile) {
        for (Path assetRoot : RESOURCE_ROOTS) {
            Path base = assetRoot.resolve(relativeRoot).resolve(candidate);
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
        return packagedVariants(relativeRoot, candidate, requiredFile);
    }

    private static boolean fileExists(String relativeRoot, VariantLocation variant, String fileName) {
        String relative = relativeRoot + "/" + variant.relativePath() + "/" + fileName;
        return EXISTS.computeIfAbsent(relative, StructureMaterialVariantResolver::fileExistsUncached);
    }

    private static boolean fileExistsUncached(String relative) {
        for (Path assetRoot : RESOURCE_ROOTS) {
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

    private static List<VariantLocation> packagedVariants(String relativeRoot, String candidate, String requiredFile) {
        try {
            URI location = StructureMaterialVariantResolver.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            Path codePath = Path.of(location);
            if (!Files.isRegularFile(codePath) || !codePath.toString().endsWith(".jar")) {
                return List.of();
            }
            String prefix = "assets/" + Industron.MOD_ID + "/" + relativeRoot + "/" + candidate + "/";
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

    private record ToolVariantLocation(int number, String relativePath, String baseFile) {
    }

    private record VariantLocation(int number, String relativePath) {
    }
}
