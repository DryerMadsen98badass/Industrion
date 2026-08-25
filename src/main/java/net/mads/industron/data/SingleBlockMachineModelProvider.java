package net.mads.industron.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.mads.industron.Industron;
import net.mads.industron.machine.MachineDefinition;
import net.mads.industron.machine.SingleBlockDefinition;
import net.mads.industron.machine.SingleBlockMachineInstance;
import net.mads.industron.machine.SingleBlockMachinePower;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class SingleBlockMachineModelProvider implements DataProvider {
    private static final String MUFFLER_TEXTURE = Industron.MOD_ID + ":block/machines/ino/muffler";

    private final PackOutput output;

    public SingleBlockMachineModelProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        Path assets = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve(Industron.MOD_ID);
        Path blockstates = assets.resolve("blockstates");
        Path blockModels = assets.resolve("models").resolve("block");
        Path itemModels = assets.resolve("models").resolve("item");

        for (SingleBlockMachineInstance instance : MachineDefinition.INSTANCES) {
            String name = instance.registryName();
            futures.add(DataProvider.saveStable(cache, blockstate(name), blockstates.resolve(name + ".json")));
            futures.add(DataProvider.saveStable(cache, blockModel(instance, false, 0), blockModels.resolve(name + ".json")));
            futures.add(DataProvider.saveStable(cache, blockModel(instance, true, 0), blockModels.resolve(name + "_active.json")));

            for (int frame = 0; frame <= 9; frame++) {
                futures.add(DataProvider.saveStable(
                        cache,
                        blockModel(instance, true, frame),
                        blockModels.resolve(name + "_active_" + frame + ".json")
                ));
            }

            futures.add(DataProvider.saveStable(cache, itemModel(name), itemModels.resolve(name + ".json")));
        }

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Industron Singleblock Machine Models";
    }

    private static JsonObject blockstate(String name) {
        JsonObject variants = new JsonObject();
        addFacingVariants(variants, name, "north", 0);
        addFacingVariants(variants, name, "east", 90);
        addFacingVariants(variants, name, "south", 180);
        addFacingVariants(variants, name, "west", 270);

        JsonObject root = new JsonObject();
        root.add("variants", variants);
        return root;
    }

    private static void addFacingVariants(JsonObject variants, String name, String facing, int rotation) {
        for (int frame = 0; frame <= 9; frame++) {
            addVariant(variants, "facing=" + facing + ",active=false,overlay_frame=" + frame, name, false, frame, rotation);
            addVariant(variants, "facing=" + facing + ",active=true,overlay_frame=" + frame, name, true, frame, rotation);
        }
    }

    private static void addVariant(
            JsonObject variants,
            String key,
            String name,
            boolean active,
            int frame,
            int rotation
    ) {
        JsonObject variant = new JsonObject();
        variant.addProperty("model", Industron.MOD_ID + ":block/" + name + activeModelSuffix(active, frame));

        if (rotation != 0) {
            variant.addProperty("y", rotation);
        }

        variants.add(key, variant);
    }

    private static String activeModelSuffix(boolean active, int frame) {
        if (!active) {
            return "";
        }

        return frame == 0 ? "_active" : "_active_" + frame;
    }

    private static JsonObject blockModel(SingleBlockMachineInstance instance, boolean active, int frame) {
        String customModel = instance.definition().model();
        if (customModel != null && hasAdditionalGeometry(instance)) {
            return compositeBlockModel(instance, customModel, active, frame);
        }

        JsonObject root = new JsonObject();
        root.addProperty("parent", customModel == null ? "minecraft:block/block" : namespaced(customModel));
        root.addProperty("render_type", "minecraft:cutout");

        JsonObject textures = new JsonObject();
        addBaseTextures(textures, instance);
        addOverlayTextures(textures, instance, active, frame);
        textures.addProperty("muffler", MUFFLER_TEXTURE);
        textures.addProperty("particle", baseTexture(instance, SingleBlockDefinition.MachineSide.FRONT));
        root.add("textures", textures);

        if (customModel == null) {
            JsonArray elements = new JsonArray();
            addBaseGeometry(elements, instance);
            addSideOverlays(elements, instance);

            if (instance.definition().power() == SingleBlockMachinePower.STEAM) {
                elements.add(topMufflerOverlay());
            }

            root.add("elements", elements);
        }
        return root;
    }

    private static JsonObject compositeBlockModel(
            SingleBlockMachineInstance instance,
            String customModel,
            boolean active,
            int frame
    ) {
        JsonObject root = new JsonObject();
        root.addProperty("parent", "minecraft:block/block");
        root.addProperty("loader", "neoforge:composite");

        JsonObject textures = new JsonObject();
        textures.addProperty("particle", baseTexture(instance, SingleBlockDefinition.MachineSide.FRONT));
        root.add("textures", textures);

        JsonObject children = new JsonObject();

        JsonObject base = new JsonObject();
        base.addProperty("parent", namespaced(customModel));
        children.add("base", base);

        JsonObject overlay = new JsonObject();
        overlay.addProperty("parent", "minecraft:block/block");
        overlay.addProperty("render_type", "minecraft:cutout");
        JsonObject overlayTextures = new JsonObject();
        addOverlayTextures(overlayTextures, instance, active, frame);
        overlayTextures.addProperty("muffler", MUFFLER_TEXTURE);
        overlay.add("textures", overlayTextures);

        JsonArray overlayElements = new JsonArray();
        addSideOverlays(overlayElements, instance);
        if (instance.definition().power() == SingleBlockMachinePower.STEAM) {
            overlayElements.add(topMufflerOverlay());
        }
        overlay.add("elements", overlayElements);
        children.add("overlay", overlay);

        root.add("children", children);

        JsonArray itemRenderOrder = new JsonArray();
        itemRenderOrder.add("base");
        itemRenderOrder.add("overlay");
        root.add("item_render_order", itemRenderOrder);
        return root;
    }

    private static boolean hasAdditionalGeometry(SingleBlockMachineInstance instance) {
        if (instance.definition().power() == SingleBlockMachinePower.STEAM) {
            return true;
        }
        for (SingleBlockDefinition.MachineSide side : SingleBlockDefinition.MachineSide.values()) {
            if (instance.definition().hasOverlay(side)) {
                return true;
            }
        }
        return false;
    }

    private static void addBaseTextures(JsonObject textures, SingleBlockMachineInstance instance) {
        textures.addProperty("front_base", baseTexture(instance, SingleBlockDefinition.MachineSide.FRONT));
        textures.addProperty("back_base", baseTexture(instance, SingleBlockDefinition.MachineSide.BACK));
        textures.addProperty("left_base", baseTexture(instance, SingleBlockDefinition.MachineSide.LEFT));
        textures.addProperty("right_base", baseTexture(instance, SingleBlockDefinition.MachineSide.RIGHT));
        textures.addProperty("top_base", baseTexture(instance, SingleBlockDefinition.MachineSide.TOP));
        textures.addProperty("bottom_base", baseTexture(instance, SingleBlockDefinition.MachineSide.BOTTOM));
    }

    private static String baseTexture(
            SingleBlockMachineInstance instance,
            SingleBlockDefinition.MachineSide side
    ) {
        String customTexture = instance.definition().sideTexture(side);

        if (customTexture != null) {
            return namespaced(customTexture);
        }

        return switch (side) {
            case TOP -> instance.tier().singleBlockMachineCasingTopTexture();
            case BOTTOM -> instance.tier().singleBlockMachineCasingBottomTexture();
            case FRONT, BACK, LEFT, RIGHT -> instance.tier().singleBlockMachineCasingSideTexture();
        };
    }

    private static void addOverlayTextures(
            JsonObject textures,
            SingleBlockMachineInstance instance,
            boolean active,
            int frame
    ) {
        for (SingleBlockDefinition.MachineSide side : SingleBlockDefinition.MachineSide.values()) {
            if (instance.definition().hasOverlay(side)) {
                textures.addProperty(overlayTextureKey(side), namespaced(overlay(instance, side, active, frame)));
            }
        }
    }

    private static String overlay(
            SingleBlockMachineInstance instance,
            SingleBlockDefinition.MachineSide side,
            boolean active,
            int frame
    ) {
        String idleOverlay = instance.definition().idleOverlay(side);

        if (idleOverlay == null) {
            throw new IllegalStateException("Missing overlay for " + side + " on " + instance.registryName());
        }

        List<String> activeOverlays = instance.definition().activeOverlays(side);

        if (!active || activeOverlays.isEmpty()) {
            return idleOverlay;
        }

        return activeOverlays.get(Math.floorMod(frame, activeOverlays.size()));
    }

    private static void addBaseGeometry(
            JsonArray elements,
            SingleBlockMachineInstance instance
    ) {
        SingleBlockDefinition.MachineSide kineticSide =
                instance.definition().kineticSide();

        if (kineticSide == null) {
            elements.add(fullCube(instance));
            return;
        }

        addRecessedBody(elements, instance, kineticSide);
        addKineticFrame(elements, instance, kineticSide);
    }

    private static void addRecessedBody(
            JsonArray elements,
            SingleBlockMachineInstance instance,
            SingleBlockDefinition.MachineSide side
    ) {
        addKineticBackCore(elements, instance, side);
        addRecessedPanel(elements, instance, side);
    }

    /**
     * The shaft model ends exactly two pixels inside the block. The tiny
     * additional inset keeps the back wall from sharing the shaft's end plane.
     */
    private static void addKineticBackCore(
            JsonArray elements,
            SingleBlockMachineInstance instance,
            SingleBlockDefinition.MachineSide side
    ) {
        float positiveStart = 2.01F;
        float negativeEnd = 13.99F;

        switch (side) {
            case FRONT -> elements.add(cuboid(instance, 0, 0, positiveStart, 16, 16, 16, null));
            case BACK -> elements.add(cuboid(instance, 0, 0, 0, 16, 16, negativeEnd, null));
            case LEFT -> elements.add(cuboid(instance, positiveStart, 0, 0, 16, 16, 16, null));
            case RIGHT -> elements.add(cuboid(instance, 0, 0, 0, negativeEnd, 16, 16, null));
            case TOP -> elements.add(cuboid(instance, 0, 0, 0, 16, negativeEnd, 16, null));
            case BOTTOM -> elements.add(cuboid(instance, 0, positiveStart, 0, 16, 16, 16, null));
        }
    }

    /**
     * One-pixel-deep recessed panel with a centered 4x4 shaft opening.
     */
    private static void addRecessedPanel(
            JsonArray elements,
            SingleBlockMachineInstance instance,
            SingleBlockDefinition.MachineSide side
    ) {
        switch (side) {
            case FRONT -> {
                elements.add(cuboid(instance, 0, 0, 1, 16, 6, 2, "south"));
                elements.add(cuboid(instance, 0, 10, 1, 16, 16, 2, "south"));
                elements.add(cuboid(instance, 0, 6, 1, 6, 10, 2, "south"));
                elements.add(cuboid(instance, 10, 6, 1, 16, 10, 2, "south"));
            }
            case BACK -> {
                elements.add(cuboid(instance, 0, 0, 14, 16, 6, 15, "north"));
                elements.add(cuboid(instance, 0, 10, 14, 16, 16, 15, "north"));
                elements.add(cuboid(instance, 0, 6, 14, 6, 10, 15, "north"));
                elements.add(cuboid(instance, 10, 6, 14, 16, 10, 15, "north"));
            }
            case LEFT -> {
                elements.add(cuboid(instance, 1, 0, 0, 2, 6, 16, "east"));
                elements.add(cuboid(instance, 1, 10, 0, 2, 16, 16, "east"));
                elements.add(cuboid(instance, 1, 6, 0, 2, 10, 6, "east"));
                elements.add(cuboid(instance, 1, 6, 10, 2, 10, 16, "east"));
            }
            case RIGHT -> {
                elements.add(cuboid(instance, 14, 0, 0, 15, 6, 16, "west"));
                elements.add(cuboid(instance, 14, 10, 0, 15, 16, 16, "west"));
                elements.add(cuboid(instance, 14, 6, 0, 15, 10, 6, "west"));
                elements.add(cuboid(instance, 14, 6, 10, 15, 10, 16, "west"));
            }
            case TOP -> {
                elements.add(cuboid(instance, 0, 14, 0, 16, 15, 6, "down"));
                elements.add(cuboid(instance, 0, 14, 10, 16, 15, 16, "down"));
                elements.add(cuboid(instance, 0, 14, 6, 6, 15, 10, "down"));
                elements.add(cuboid(instance, 10, 14, 6, 16, 15, 10, "down"));
            }
            case BOTTOM -> {
                elements.add(cuboid(instance, 0, 1, 0, 16, 2, 6, "up"));
                elements.add(cuboid(instance, 0, 1, 10, 16, 2, 16, "up"));
                elements.add(cuboid(instance, 0, 1, 6, 6, 2, 10, "up"));
                elements.add(cuboid(instance, 10, 1, 6, 16, 2, 10, "up"));
            }
        }
    }

    private static void addKineticFrame(
            JsonArray elements,
            SingleBlockMachineInstance instance,
            SingleBlockDefinition.MachineSide side
    ) {
        switch (side) {
            case FRONT -> {
                elements.add(cuboid(instance, 0, 0, 0, 16, 2, 1, "south"));
                elements.add(cuboid(instance, 0, 14, 0, 16, 16, 1, "south"));
                elements.add(cuboid(instance, 0, 2, 0, 2, 14, 1, "south"));
                elements.add(cuboid(instance, 14, 2, 0, 16, 14, 1, "south"));
            }
            case BACK -> {
                elements.add(cuboid(instance, 0, 0, 15, 16, 2, 16, "north"));
                elements.add(cuboid(instance, 0, 14, 15, 16, 16, 16, "north"));
                elements.add(cuboid(instance, 0, 2, 15, 2, 14, 16, "north"));
                elements.add(cuboid(instance, 14, 2, 15, 16, 14, 16, "north"));
            }
            case LEFT -> {
                elements.add(cuboid(instance, 0, 0, 0, 1, 2, 16, "east"));
                elements.add(cuboid(instance, 0, 14, 0, 1, 16, 16, "east"));
                elements.add(cuboid(instance, 0, 2, 0, 1, 14, 2, "east"));
                elements.add(cuboid(instance, 0, 2, 14, 1, 14, 16, "east"));
            }
            case RIGHT -> {
                elements.add(cuboid(instance, 15, 0, 0, 16, 2, 16, "west"));
                elements.add(cuboid(instance, 15, 14, 0, 16, 16, 16, "west"));
                elements.add(cuboid(instance, 15, 2, 0, 16, 14, 2, "west"));
                elements.add(cuboid(instance, 15, 2, 14, 16, 14, 16, "west"));
            }
            case TOP -> {
                elements.add(cuboid(instance, 0, 15, 0, 16, 16, 2, "down"));
                elements.add(cuboid(instance, 0, 15, 14, 16, 16, 16, "down"));
                elements.add(cuboid(instance, 0, 15, 2, 2, 16, 14, "down"));
                elements.add(cuboid(instance, 14, 15, 2, 16, 16, 14, "down"));
            }
            case BOTTOM -> {
                elements.add(cuboid(instance, 0, 0, 0, 16, 1, 2, "up"));
                elements.add(cuboid(instance, 0, 0, 14, 16, 1, 16, "up"));
                elements.add(cuboid(instance, 0, 0, 2, 2, 1, 14, "up"));
                elements.add(cuboid(instance, 14, 0, 2, 16, 1, 14, "up"));
            }
        }
    }

    private static JsonObject cuboid(
            SingleBlockMachineInstance instance,
            Number fromX,
            Number fromY,
            Number fromZ,
            Number toX,
            Number toY,
            Number toZ,
            String omittedFace
    ) {
        JsonObject element = new JsonObject();
        element.add("from", vector(fromX, fromY, fromZ));
        element.add("to", vector(toX, toY, toZ));

        JsonObject faces = new JsonObject();
        addCuboidFace(faces, "north", "#front_base", baseTint(instance, SingleBlockDefinition.MachineSide.FRONT), omittedFace, fromZ.doubleValue() == 0.0D);
        addCuboidFace(faces, "south", "#back_base", baseTint(instance, SingleBlockDefinition.MachineSide.BACK), omittedFace, toZ.doubleValue() == 16.0D);
        addCuboidFace(faces, "west", "#left_base", baseTint(instance, SingleBlockDefinition.MachineSide.LEFT), omittedFace, fromX.doubleValue() == 0.0D);
        addCuboidFace(faces, "east", "#right_base", baseTint(instance, SingleBlockDefinition.MachineSide.RIGHT), omittedFace, toX.doubleValue() == 16.0D);
        addCuboidFace(faces, "up", "#top_base", baseTint(instance, SingleBlockDefinition.MachineSide.TOP), omittedFace, toY.doubleValue() == 16.0D);
        addCuboidFace(faces, "down", "#bottom_base", baseTint(instance, SingleBlockDefinition.MachineSide.BOTTOM), omittedFace, fromY.doubleValue() == 0.0D);
        element.add("faces", faces);
        return element;
    }

    private static void addCuboidFace(
            JsonObject faces,
            String direction,
            String texture,
            int tintIndex,
            String omittedFace,
            boolean cull
    ) {
        if (direction.equals(omittedFace)) {
            return;
        }

        JsonObject face = new JsonObject();
        face.addProperty("texture", texture);

        if (cull) {
            face.addProperty("cullface", direction);
        }

        if (tintIndex >= 0) {
            face.addProperty("tintindex", tintIndex);
        }

        faces.add(direction, face);
    }

    private static JsonObject fullCube(SingleBlockMachineInstance instance) {
        JsonObject element = new JsonObject();
        element.add("from", vector(0, 0, 0));
        element.add("to", vector(16, 16, 16));

        JsonObject faces = new JsonObject();
        addFace(faces, "north", "#front_base", baseTint(instance, SingleBlockDefinition.MachineSide.FRONT));
        addFace(faces, "south", "#back_base", baseTint(instance, SingleBlockDefinition.MachineSide.BACK));
        addFace(faces, "west", "#left_base", baseTint(instance, SingleBlockDefinition.MachineSide.LEFT));
        addFace(faces, "east", "#right_base", baseTint(instance, SingleBlockDefinition.MachineSide.RIGHT));
        addFace(faces, "up", "#top_base", baseTint(instance, SingleBlockDefinition.MachineSide.TOP));
        addFace(faces, "down", "#bottom_base", baseTint(instance, SingleBlockDefinition.MachineSide.BOTTOM));
        element.add("faces", faces);
        return element;
    }

    private static int baseTint(
            SingleBlockMachineInstance instance,
            SingleBlockDefinition.MachineSide side
    ) {
        if (instance.definition().sideTexture(side) != null) {
            return instance.definition().hasSideTextureColor(side) ? side.tintIndex() : -1;
        }

        return instance.tier().isElectric() ? side.tintIndex() : -1;
    }

    private static void addSideOverlays(JsonArray elements, SingleBlockMachineInstance instance) {
        for (SingleBlockDefinition.MachineSide side : SingleBlockDefinition.MachineSide.values()) {
            if (instance.definition().hasOverlay(side)) {
                elements.add(sideOverlay(side));
            }
        }
    }

    private static JsonObject sideOverlay(SingleBlockDefinition.MachineSide side) {
        JsonObject element = new JsonObject();
        JsonObject faces = new JsonObject();
        String texture = "#" + overlayTextureKey(side);

        switch (side) {
            case FRONT -> {
                element.add("from", vector(0, 0, -0.01F));
                element.add("to", vector(16, 16, 0));
                addFace(faces, "north", texture, -1);
            }
            case BACK -> {
                element.add("from", vector(0, 0, 16));
                element.add("to", vector(16, 16, 16.01F));
                addFace(faces, "south", texture, -1);
            }
            case LEFT -> {
                element.add("from", vector(-0.01F, 0, 0));
                element.add("to", vector(0, 16, 16));
                addFace(faces, "west", texture, -1);
            }
            case RIGHT -> {
                element.add("from", vector(16, 0, 0));
                element.add("to", vector(16.01F, 16, 16));
                addFace(faces, "east", texture, -1);
            }
            case TOP -> {
                element.add("from", vector(0, 16, 0));
                element.add("to", vector(16, 16.01F, 16));
                addFace(faces, "up", texture, -1);
            }
            case BOTTOM -> {
                element.add("from", vector(0, -0.01F, 0));
                element.add("to", vector(16, 0, 16));
                addFace(faces, "down", texture, -1);
            }
        }

        element.add("faces", faces);
        return element;
    }

    private static String overlayTextureKey(SingleBlockDefinition.MachineSide side) {
        return switch (side) {
            case FRONT -> "front_overlay";
            case BACK -> "back_overlay";
            case LEFT -> "left_overlay";
            case RIGHT -> "right_overlay";
            case TOP -> "top_overlay";
            case BOTTOM -> "bottom_overlay";
        };
    }

    private static JsonObject topMufflerOverlay() {
        JsonObject element = new JsonObject();
        element.add("from", vector(0, 16.02F, 0));
        element.add("to", vector(16, 16.03F, 16));

        JsonObject faces = new JsonObject();
        addFace(faces, "up", "#muffler", -1);
        element.add("faces", faces);
        return element;
    }

    private static JsonArray vector(Number x, Number y, Number z) {
        JsonArray vector = new JsonArray();
        vector.add(x);
        vector.add(y);
        vector.add(z);
        return vector;
    }

    private static void addFace(JsonObject faces, String direction, String texture, int tintIndex) {
        JsonObject face = new JsonObject();
        face.addProperty("texture", texture);
        face.addProperty("cullface", direction);

        if (tintIndex >= 0) {
            face.addProperty("tintindex", tintIndex);
        }

        faces.add(direction, face);
    }

    private static String namespaced(String texture) {
        return texture.contains(":") ? texture : Industron.MOD_ID + ":" + texture;
    }

    private static JsonObject itemModel(String name) {
        JsonObject root = new JsonObject();
        root.addProperty("parent", Industron.MOD_ID + ":block/" + name);
        return root;
    }
}
