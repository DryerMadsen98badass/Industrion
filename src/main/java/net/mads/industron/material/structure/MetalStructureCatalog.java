package net.mads.industron.material.structure;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Authoritative mapping between the grayscale metal structure library and the
 * block/model families generated from it.
 *
 * <p>The {@code metal_N} directory names are asset-library implementation
 * details only. They are never exposed through registry names or display names.
 * Every real visual family gets a stable semantic id and a readable in-game
 * name, while companion textures remain part of the same block definition.</p>
 */
public final class MetalStructureCatalog {
    private MetalStructureCatalog() {
    }

    public static List<StructureBlockDefinition> blocks(MetalMaterial material) {
        Set<String> available = new LinkedHashSet<>(StructureSetResolver.textureFiles(material.model()));
        List<StructureBlockDefinition> result = new ArrayList<>();

        addBars(result, material, available);
        addBracket(result, material, available);
        addBlocks(result, material, available);
        addBulb(result, material, available);
        addDoors(result, material, available);
        addGrate(result, material, available);
        addLadder(result, material, available);
        addScaffolds(result, material, available);
        addSurfaceFamily(result, material, available, "shingles", "Shingles", "shingle", "Shingle");
        addSurfaceFamily(result, material, available, "tiles", "Tiles", "tile", "Tile");
        addTrapdoors(result, material, available);
        addWindows(result, material, available);

        validateDefinitions(result, available);
        return List.copyOf(result);
    }

    private static void addBars(
            List<StructureBlockDefinition> result,
            MetalMaterial material,
            Set<String> available
    ) {
        addBarsVariant(result, material, available, 1,
                "framed_bars", "Framed Bars",
                "brass_bars.png", "brass_bars_edge.png",
                StructureBlockDefinition.ModelKind.CREATE_BARS);
        addBarsVariant(result, material, available, 2,
                "ornate_bars", "Ornate Bars",
                "copper_bars.png", "copper_bars_edge.png",
                StructureBlockDefinition.ModelKind.CREATE_BARS);
        addBarsVariant(result, material, available, 3,
                "slatted_bars", "Slatted Bars",
                "andesite_bars.png", "andesite_bars_edge.png",
                StructureBlockDefinition.ModelKind.CREATE_BARS);
        addBarsVariant(result, material, available, 4,
                "classic_bars", "Classic Bars",
                "iron_bars.png", null,
                StructureBlockDefinition.ModelKind.VANILLA_BARS);
    }

    private static void addBarsVariant(
            List<StructureBlockDefinition> result,
            MetalMaterial material,
            Set<String> available,
            int assetVariant,
            String suffix,
            String displaySuffix,
            String mainFile,
            String edgeFile,
            StructureBlockDefinition.ModelKind modelKind
    ) {
        String root = "bars/metal_" + assetVariant + "/";
        String main = require(available, root + mainFile);
        Map<String, String> textureMap = edgeFile == null
                ? textures("main", main)
                : textures("main", main, "edge", require(available, root + edgeFile));
        result.add(definition(
                material,
                suffix,
                displaySuffix,
                StructureBlockDefinition.Shape.BARS,
                main,
                null,
                null,
                null,
                null,
                modelKind,
                null,
                textureMap
        ));
    }

    private static void addBracket(
            List<StructureBlockDefinition> result,
            MetalMaterial material,
            Set<String> available
    ) {
        String root = "bracket/metal_1/";
        String bracket = require(available, root + "bracket_metal.png");
        String plate = require(available, root + "bracket_plate_metal.png");
        result.add(definition(
                material,
                "bracket",
                "Bracket",
                StructureBlockDefinition.Shape.BRACKET,
                bracket,
                null,
                null,
                null,
                null,
                StructureBlockDefinition.ModelKind.CREATE_BRACKET,
                null,
                textures("bracket", bracket, "plate", plate)
        ));
    }

    private static void addBlocks(
            List<StructureBlockDefinition> result,
            MetalMaterial material,
            Set<String> available
    ) {
        addSimpleBlock(result, material, available, 1,
                "framed_block", "Framed Block", "brass_block.png", null);
        addSimpleBlock(result, material, available, 2,
                "chiseled_block", "Chiseled Block", "chiseled_copper.png", null);
        addSimpleBlock(result, material, available, 3,
                "plated_block", "Plated Block", "copper_block.png", null);
        addSimpleBlock(result, material, available, 4,
                "cut_block", "Cut Block", "cut_copper.png", null);
        addSimpleBlock(result, material, available, 5,
                "dark_block", "Dark Block", "dark_metal_block.png", null);
        addSimpleBlock(result, material, available, 6,
                "industrial_block", "Industrial Block", "industrial_iron_block.png", "industrial_iron_block_top.png");
        addSimpleBlock(result, material, available, 7,
                "smooth_block", "Smooth Block", "iron_block.png", null);
        addSimpleBlock(result, material, available, 8,
                "weathered_block", "Weathered Block", "weathered_iron_block.png", "weathered_iron_block_top.png");
        addSimpleBlock(result, material, available, 9,
                "ribbed_block", "Ribbed Block", "zinc_block.png", null);

        String cut = require(available, "block/metal_4/cut_copper.png");
        String cutBaseId = id(material, "cut_block");
        Map<String, String> cutTextures = textures("side", cut, "top", cut, "bottom", cut);
        result.add(definition(
                material,
                "cut_slab",
                "Cut Slab",
                StructureBlockDefinition.Shape.SLAB,
                cut,
                cut,
                cut,
                null,
                cutBaseId,
                StructureBlockDefinition.ModelKind.DEFAULT,
                null,
                cutTextures
        ));
        result.add(definition(
                material,
                "cut_stairs",
                "Cut Stairs",
                StructureBlockDefinition.Shape.STAIRS,
                cut,
                cut,
                cut,
                null,
                cutBaseId,
                StructureBlockDefinition.ModelKind.DEFAULT,
                null,
                cutTextures
        ));
    }

    private static void addSimpleBlock(
            List<StructureBlockDefinition> result,
            MetalMaterial material,
            Set<String> available,
            int assetVariant,
            String suffix,
            String displaySuffix,
            String sideFile,
            String endFile
    ) {
        String root = "block/metal_" + assetVariant + "/";
        String side = require(available, root + sideFile);
        String end = endFile == null ? side : require(available, root + endFile);
        result.add(definition(
                material,
                suffix,
                displaySuffix,
                StructureBlockDefinition.Shape.CUBE,
                side,
                end,
                end,
                null,
                null,
                StructureBlockDefinition.ModelKind.DEFAULT,
                null,
                textures("side", side, "top", end, "bottom", end)
        ));
    }

    private static void addBulb(
            List<StructureBlockDefinition> result,
            MetalMaterial material,
            Set<String> available
    ) {
        String root = "bulb/metal_1/";
        String off = require(available, root + "copper_bulb.png");
        String lit = require(available, root + "copper_bulb_lit.png");
        String powered = require(available, root + "copper_bulb_powered.png");
        String litPowered = require(available, root + "copper_bulb_lit_powered.png");
        result.add(definition(
                material,
                "bulb",
                "Bulb",
                StructureBlockDefinition.Shape.BULB,
                off,
                null,
                null,
                null,
                null,
                StructureBlockDefinition.ModelKind.COPPER_BULB,
                null,
                textures(
                        "off", off,
                        "lit", lit,
                        "powered", powered,
                        "lit_powered", litPowered
                )
        ));
    }

    private static void addDoors(
            List<StructureBlockDefinition> result,
            MetalMaterial material,
            Set<String> available
    ) {
        addCreateDoor(result, material, available, 1,
                "ornate_door", "Ornate Door",
                "brass_door", "brass_door", "brass_casing.png");
        addCreateDoor(result, material, available, 2,
                "framed_door", "Framed Door",
                "copper_door", "copper_door", "copper_casing.png");
        addVanillaDoor(result, material, available, 3,
                "crossbar_door", "Crossbar Door", "copper_door");
        addCreateDoor(result, material, available, 4,
                "industrial_door", "Industrial Door",
                "andesite_door", "andesite_door", "andesite_casing.png");
        addCreateDoor(result, material, available, 5,
                "glass_door", "Glass Door",
                "glass_door", "framed_glass_door", "framed_glass.png");
        addCreateDoor(result, material, available, 6,
                "train_door", "Train Door",
                "train_door", "train_door", "train_trapdoor.png");
        addVanillaDoor(result, material, available, 7,
                "secure_door", "Secure Door", "iron_door");
    }

    private static void addCreateDoor(
            List<StructureBlockDefinition> result,
            MetalMaterial material,
            Set<String> available,
            int assetVariant,
            String suffix,
            String displaySuffix,
            String texturePrefix,
            String modelFolder,
            String particleFile
    ) {
        String root = "door/metal_" + assetVariant + "/";
        String item = require(available, root + texturePrefix + ".png");
        String bottom = require(available, root + texturePrefix + "_bottom.png");
        String side = require(available, root + texturePrefix + "_side.png");
        String top = require(available, root + texturePrefix + "_top.png");
        String particle = require(available, root + particleFile);
        result.add(definition(
                material,
                suffix,
                displaySuffix,
                StructureBlockDefinition.Shape.DOOR,
                bottom,
                top,
                null,
                item,
                null,
                StructureBlockDefinition.ModelKind.CREATE_DOOR,
                modelFolder,
                textures(
                        "item", item,
                        "bottom", bottom,
                        "side", side,
                        "top", top,
                        "particle", particle
                )
        ));
    }

    private static void addVanillaDoor(
            List<StructureBlockDefinition> result,
            MetalMaterial material,
            Set<String> available,
            int assetVariant,
            String suffix,
            String displaySuffix,
            String texturePrefix
    ) {
        String root = "door/metal_" + assetVariant + "/";
        String item = require(available, root + texturePrefix + ".png");
        String bottom = require(available, root + texturePrefix + "_bottom.png");
        String top = require(available, root + texturePrefix + "_top.png");
        result.add(definition(
                material,
                suffix,
                displaySuffix,
                StructureBlockDefinition.Shape.DOOR,
                bottom,
                top,
                null,
                item,
                null,
                StructureBlockDefinition.ModelKind.VANILLA_DOOR,
                null,
                textures("item", item, "bottom", bottom, "top", top)
        ));
    }

    private static void addGrate(
            List<StructureBlockDefinition> result,
            MetalMaterial material,
            Set<String> available
    ) {
        String main = require(available, "grate/metal_1/copper_grate.png");
        result.add(definition(
                material,
                "grate",
                "Grate",
                StructureBlockDefinition.Shape.CUBE,
                main,
                null,
                null,
                null,
                null,
                StructureBlockDefinition.ModelKind.CUTOUT_CUBE,
                null,
                textures("main", main)
        ));
    }

    private static void addLadder(
            List<StructureBlockDefinition> result,
            MetalMaterial material,
            Set<String> available
    ) {
        String root = "ladder/metal_1/";
        String main = require(available, root + "ladder_brass.png");
        String hoop = require(available, root + "ladder_brass_hoop.png");
        result.add(definition(
                material,
                "ladder",
                "Ladder",
                StructureBlockDefinition.Shape.LADDER,
                main,
                null,
                null,
                null,
                null,
                StructureBlockDefinition.ModelKind.CREATE_LADDER,
                null,
                textures("main", main, "hoop", hoop)
        ));
    }

    private static void addScaffolds(
            List<StructureBlockDefinition> result,
            MetalMaterial material,
            Set<String> available
    ) {
        addScaffold(result, material, available, 1,
                "ornate_scaffolding", "Ornate Scaffolding",
                "brass_scaffold.png", "brass_scaffold_connected.png",
                "brass_scaffold_inside.png", "brass_scaffold_inside_connected.png",
                "brass_casing.png", "brass_casing_connected.png", "brass_funnel_frame.png");
        addScaffold(result, material, available, 2,
                "framed_scaffolding", "Framed Scaffolding",
                "copper_scaffold.png", "copper_scaffold_connected.png",
                "copper_scaffold_inside.png", "copper_scaffold_inside_connected.png",
                "copper_casing.png", "copper_casing_connected.png", "copper_funnel_frame.png");
        addScaffold(result, material, available, 3,
                "industrial_scaffolding", "Industrial Scaffolding",
                "andesite_scaffold.png", "andesite_scaffold_connected.png",
                "andesite_scaffold_inside.png", "andesite_scaffold_inside_connected.png",
                "andesite_casing.png", "andesite_casing_connected.png", "andesite_funnel_frame.png");
    }

    private static void addScaffold(
            List<StructureBlockDefinition> result,
            MetalMaterial material,
            Set<String> available,
            int assetVariant,
            String suffix,
            String displaySuffix,
            String sideFile,
            String connectedFile,
            String insideFile,
            String insideConnectedFile,
            String casingFile,
            String casingConnectedFile,
            String topFile
    ) {
        String root = "scaffold/metal_" + assetVariant + "/";
        String side = require(available, root + sideFile);
        String connected = require(available, root + connectedFile);
        String inside = require(available, root + insideFile);
        String insideConnected = require(available, root + insideConnectedFile);
        String casing = require(available, root + casingFile);
        String casingConnected = require(available, root + casingConnectedFile);
        String top = require(available, root + topFile);
        result.add(definition(
                material,
                suffix,
                displaySuffix,
                StructureBlockDefinition.Shape.SCAFFOLD,
                side,
                null,
                null,
                null,
                null,
                StructureBlockDefinition.ModelKind.CREATE_SCAFFOLD,
                null,
                textures(
                        "side", side,
                        "connected", connected,
                        "inside", inside,
                        "inside_connected", insideConnected,
                        "casing", casing,
                        "casing_connected", casingConnected,
                        "top", top
                )
        ));
    }

    private static void addSurfaceFamily(
            List<StructureBlockDefinition> result,
            MetalMaterial material,
            Set<String> available,
            String category,
            String readableName,
            String singular,
            String singularReadableName
    ) {
        String root = category + "/metal_1/";
        String texturePrefix = category.equals("shingles") ? "copper_shingles" : "copper_tiles";
        String side = require(available, root + texturePrefix + ".png");
        String roofTop = require(available, root + "copper_roof_top.png");
        String connected = require(available, root + texturePrefix + "_top_connected.png");
        String baseId = id(material, category);
        Map<String, String> textureMap = textures(
                "side", side,
                "top", roofTop,
                "bottom", roofTop,
                "connected", connected
        );

        result.add(definition(
                material,
                category,
                readableName,
                StructureBlockDefinition.Shape.CUBE,
                side,
                roofTop,
                roofTop,
                null,
                null,
                StructureBlockDefinition.ModelKind.DEFAULT,
                null,
                textureMap
        ));
        result.add(definition(
                material,
                singular + "_slab",
                singularReadableName + " Slab",
                StructureBlockDefinition.Shape.SLAB,
                side,
                roofTop,
                roofTop,
                null,
                baseId,
                StructureBlockDefinition.ModelKind.DEFAULT,
                null,
                textureMap
        ));
        result.add(definition(
                material,
                singular + "_stairs",
                singularReadableName + " Stairs",
                StructureBlockDefinition.Shape.STAIRS,
                side,
                roofTop,
                roofTop,
                null,
                baseId,
                StructureBlockDefinition.ModelKind.DEFAULT,
                null,
                textureMap
        ));
    }

    private static void addTrapdoors(
            List<StructureBlockDefinition> result,
            MetalMaterial material,
            Set<String> available
    ) {
        addVanillaTrapdoor(result, material, available, 1,
                "crossbar_trapdoor", "Crossbar Trapdoor", "copper_trapdoor.png");
        addTrainTrapdoor(result, material, available);
        addVanillaTrapdoor(result, material, available, 3,
                "secure_trapdoor", "Secure Trapdoor", "iron_trapdoor.png");
    }

    private static void addVanillaTrapdoor(
            List<StructureBlockDefinition> result,
            MetalMaterial material,
            Set<String> available,
            int assetVariant,
            String suffix,
            String displaySuffix,
            String fileName
    ) {
        String main = require(available, "trapdoor/metal_" + assetVariant + "/" + fileName);
        result.add(definition(
                material,
                suffix,
                displaySuffix,
                StructureBlockDefinition.Shape.TRAPDOOR,
                main,
                null,
                null,
                null,
                null,
                StructureBlockDefinition.ModelKind.VANILLA_TRAPDOOR,
                null,
                textures("main", main)
        ));
    }

    private static void addTrainTrapdoor(
            List<StructureBlockDefinition> result,
            MetalMaterial material,
            Set<String> available
    ) {
        String root = "trapdoor/metal_2/";
        String main = require(available, root + "train_trapdoor.png");
        String side = require(available, root + "train_door_side.png");
        result.add(definition(
                material,
                "train_trapdoor",
                "Train Trapdoor",
                StructureBlockDefinition.Shape.TRAPDOOR,
                main,
                null,
                null,
                null,
                null,
                StructureBlockDefinition.ModelKind.CREATE_TRAIN_TRAPDOOR,
                null,
                textures("main", main, "side", side)
        ));
    }

    private static void addWindows(
            List<StructureBlockDefinition> result,
            MetalMaterial material,
            Set<String> available
    ) {
        addWindow(result, material, available, 1,
                "industrial_window", "Industrial Window",
                "industrial_iron_window.png",
                "industrial_iron_window_connected.png",
                "industrial_iron_window_end.png",
                "industrial_iron_window_pane_top.png",
                false);
        addWindow(result, material, available, 2,
                "ornate_window", "Ornate Window",
                "ornate_iron_window.png",
                "ornate_iron_window_connected.png",
                "ornate_iron_window_end.png",
                "ornate_iron_window_pane_top.png",
                false);
        addWindow(result, material, available, 3,
                "weathered_window", "Weathered Window",
                "weathered_iron_window.png",
                "weathered_iron_window_1_connected.png",
                "weathered_iron_window_1_end.png",
                "weathered_iron_window_pane_top.png",
                true);
    }

    private static void addWindow(
            List<StructureBlockDefinition> result,
            MetalMaterial material,
            Set<String> available,
            int assetVariant,
            String suffix,
            String displaySuffix,
            String sideFile,
            String connectedFile,
            String endFile,
            String paneTopFile,
            boolean randomEnds
    ) {
        String root = "window/metal_" + assetVariant + "/";
        String side = require(available, root + sideFile);
        String connected = require(available, root + connectedFile);
        String end = require(available, root + endFile);
        String paneTop = require(available, root + paneTopFile);
        Map<String, String> textureMap = new LinkedHashMap<>();
        textureMap.put("side", side);
        textureMap.put("connected", connected);
        textureMap.put("end", end);
        textureMap.put("pane_top", paneTop);

        if (randomEnds) {
            for (int index = 1; index <= 4; index++) {
                textureMap.put("connected_" + index, require(
                        available,
                        root + "weathered_iron_window_" + index + "_connected.png"
                ));
                textureMap.put("end_" + index, require(
                        available,
                        root + "weathered_iron_window_" + index + "_end.png"
                ));
            }
        }

        result.add(definition(
                material,
                suffix,
                displaySuffix,
                StructureBlockDefinition.Shape.WINDOW,
                side,
                end,
                end,
                null,
                null,
                StructureBlockDefinition.ModelKind.CREATE_WINDOW,
                null,
                textureMap
        ));
        result.add(definition(
                material,
                suffix + "_pane",
                displaySuffix + " Pane",
                StructureBlockDefinition.Shape.WINDOW_PANE,
                side,
                null,
                null,
                null,
                null,
                StructureBlockDefinition.ModelKind.CREATE_WINDOW_PANE,
                null,
                textureMap
        ));
    }

    private static StructureBlockDefinition definition(
            MetalMaterial material,
            String suffix,
            String displaySuffix,
            StructureBlockDefinition.Shape shape,
            String textureFile,
            String topTextureFile,
            String bottomTextureFile,
            String itemTextureFile,
            String baseRegistryName,
            StructureBlockDefinition.ModelKind modelKind,
            String modelTemplate,
            Map<String, String> textureFiles
    ) {
        return new StructureBlockDefinition(
                material,
                id(material, suffix),
                material.displayName() + " " + displaySuffix,
                shape,
                textureFile,
                Optional.ofNullable(topTextureFile),
                Optional.ofNullable(bottomTextureFile),
                Optional.ofNullable(itemTextureFile),
                Optional.ofNullable(baseRegistryName),
                Optional.empty(),
                Optional.empty(),
                modelKind,
                Optional.ofNullable(modelTemplate),
                textureFiles
        );
    }

    private static void validateDefinitions(
            List<StructureBlockDefinition> definitions,
            Set<String> available
    ) {
        Set<String> ids = new LinkedHashSet<>();
        Set<String> usedTextures = new LinkedHashSet<>();
        for (StructureBlockDefinition definition : definitions) {
            if (!ids.add(definition.registryName())) {
                throw new IllegalStateException("Duplicate metal structure block id: " + definition.registryName());
            }
            if (definition.registryName().contains("_variant_")
                    || definition.displayName().toLowerCase().contains("variant")) {
                throw new IllegalStateException(
                        "Asset variant leaked into a metal structure block name: " + definition.registryName()
                );
            }

            validateTexture(available, usedTextures, definition.registryName(), definition.textureFile());
            definition.topTextureFile().ifPresent(texture ->
                    validateTexture(available, usedTextures, definition.registryName(), texture));
            definition.bottomTextureFile().ifPresent(texture ->
                    validateTexture(available, usedTextures, definition.registryName(), texture));
            definition.itemTextureFile().ifPresent(texture ->
                    validateTexture(available, usedTextures, definition.registryName(), texture));
            for (String texture : definition.textureFiles().values()) {
                validateTexture(available, usedTextures, definition.registryName(), texture);
            }
        }

        Set<String> unusedTextures = new LinkedHashSet<>(available);
        unusedTextures.removeAll(usedTextures);
        if (!unusedTextures.isEmpty()) {
            throw new IllegalStateException(
                    "Unmapped metal structure textures: " + String.join(", ", unusedTextures)
            );
        }
    }

    private static void validateTexture(
            Set<String> available,
            Set<String> usedTextures,
            String registryName,
            String texture
    ) {
        if (!available.contains(texture)) {
            throw new IllegalStateException(
                    "Missing metal structure texture for " + registryName + ": " + texture
            );
        }
        usedTextures.add(texture);
    }

    private static String id(MetalMaterial material, String suffix) {
        return material.id() + "_" + suffix;
    }

    private static String require(Set<String> available, String path) {
        if (!available.contains(path)) {
            throw new IllegalStateException("Missing metal structure texture: " + path);
        }
        return path;
    }

    private static Map<String, String> textures(String... entries) {
        if (entries.length % 2 != 0) {
            throw new IllegalArgumentException("Texture map requires key/value pairs");
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            result.put(entries[index], entries[index + 1]);
        }
        return Map.copyOf(result);
    }
}
