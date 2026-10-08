package net.mads.industron.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.mads.industron.Industron;
import net.mads.industron.machine.MachinePortType;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.machine.StaticMachinePortType;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockControllerDefinition;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockDefinitions;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MachinePortModelProvider implements DataProvider {
    private static final String COLOR_OVERLAY_TEXTURE = Industron.MOD_ID + ":block/machines/ino/frame_coler_overlay";
    private static final String KINETIC_CASING_SINGLE_TEXTURE = Industron.MOD_ID + ":block/machines/ino/kinetic_casing_single";
    private static final String KINETIC_INSIDE_FRAME_TEXTURE = Industron.MOD_ID + ":block/machines/ino/kinetic_inside_frame";
    private static final String KINETIC_HOLE_TEXTURE = Industron.MOD_ID + ":block/machines/ino/kinetic_hole";

    private final PackOutput output;

    public MachinePortModelProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        Path assets = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK)
                .resolve(Industron.MOD_ID);
        Path blockstates = assets.resolve("blockstates");
        Path blockModels = assets.resolve("models").resolve("block");
        Path itemModels = assets.resolve("models").resolve("item");

        for (MultiblockControllerDefinition controller : MultiblockDefinitions.controllers()) {
            String idleModel = controllerModelName(controller, false, 0);
            futures.add(DataProvider.saveStable(cache, controllerModel(controller, false, 0), blockModels.resolve(idleModel + ".json")));
            for (int frame = 0; frame < 1; frame++) {
                String activeModel = controllerModelName(controller, true, frame);
                futures.add(DataProvider.saveStable(cache, controllerModel(controller, true, frame), blockModels.resolve(activeModel + ".json")));
            }
            futures.add(DataProvider.saveStable(cache, controllerBlockstate(controller), blockstates.resolve(controller.registryName() + ".json")));
            futures.add(DataProvider.saveStable(cache, itemModel(idleModel), itemModels.resolve(controller.registryName() + ".json")));
        }

        for (MachinePortType portType : MachinePortType.ALL) {
            for (MachineTier tier : MachineTier.ALL) {
                String registryName = portType.registryName(tier);
                String modelName = "machine_port/" + registryName;
                JsonObject model = portType.isKinetic()
                        ? kineticBoxModel(tier)
                        : machinePortModel(portType, tier);

                futures.add(DataProvider.saveStable(cache, model, blockModels.resolve(modelName + ".json")));
                futures.add(DataProvider.saveStable(cache, facingBlockstate(modelName), blockstates.resolve(registryName + ".json")));
                futures.add(DataProvider.saveStable(cache, itemModel(modelName), itemModels.resolve(registryName + ".json")));
            }
        }

        for (StaticMachinePortType portType : StaticMachinePortType.ALL) {
            String modelName = "machine_port/" + portType.id();
            futures.add(DataProvider.saveStable(cache, staticMachinePortModel(portType), blockModels.resolve(modelName + ".json")));
            futures.add(DataProvider.saveStable(cache, facingBlockstate(modelName), blockstates.resolve(portType.id() + ".json")));
            futures.add(DataProvider.saveStable(cache, itemModel(modelName), itemModels.resolve(portType.id() + ".json")));
        }

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Industron Machine Port Models";
    }

    private static JsonObject machinePortModel(MachinePortType portType, MachineTier tier) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", "minecraft:block/block");
        json.addProperty("render_type", "minecraft:cutout");

        String casingTexture = tier.singleBlockMachineCasingSideTexture();
        JsonObject textures = new JsonObject();
        textures.addProperty("casing", casingTexture);
        textures.addProperty("overlay", Industron.MOD_ID + ":" + portType.texturePath());
        textures.addProperty("color_overlay", COLOR_OVERLAY_TEXTURE);
        textures.addProperty("particle", casingTexture);
        json.add("textures", textures);

        JsonArray elements = new JsonArray();
        elements.add(casingElement());
        if (colorable(portType.abilities())) {
            elements.add(colorOverlayElement());
        }
        elements.add(overlayElement());
        json.add("elements", elements);
        return json;
    }

    private static JsonObject staticMachinePortModel(StaticMachinePortType portType) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", "minecraft:block/block");
        json.addProperty("render_type", "minecraft:cutout");

        JsonObject textures = new JsonObject();
        textures.addProperty("casing", textureReference(portType.casingTexture()));
        textures.addProperty("front", textureReference(portType.frontTexture()));
        textures.addProperty("particle", textureReference(portType.casingTexture()));
        json.add("textures", textures);

        JsonArray elements = new JsonArray();
        if (portType.modelKind() == StaticMachinePortType.ModelKind.KINETIC) {
            elements.add(staticKineticBodyElement(portType.tinted()));
            elements.add(staticKineticOuterTopElement(portType.tinted()));
            elements.add(staticKineticOuterBottomElement(portType.tinted()));
            elements.add(staticKineticOuterLeftElement(portType.tinted()));
            elements.add(staticKineticOuterRightElement(portType.tinted()));
            elements.add(staticKineticFrontTopRingElement());
            elements.add(staticKineticFrontBottomRingElement());
            elements.add(staticKineticFrontLeftRingElement());
            elements.add(staticKineticFrontRightRingElement());
            elements.add(staticKineticInsetElement());
        } else {
            elements.add(staticCasingElement(portType.tinted(), true));
            elements.add(staticFrontOverlayElement());
        }

        json.add("elements", elements);
        return json;
    }

    private static JsonObject controllerModel(MultiblockControllerDefinition controller, boolean active, int frame) {
        String customModel = controller.model();
        if (customModel != null && hasControllerOverlay(controller)) {
            return compositeControllerModel(controller, customModel, active, frame);
        }

        JsonObject json = new JsonObject();
        json.addProperty("parent", customModel == null ? "minecraft:block/block" : textureReference(customModel));
        json.addProperty("render_type", "minecraft:cutout");

        JsonObject textures = new JsonObject();
        addControllerBaseTextures(textures, controller);
        addControllerOverlayTextures(textures, controller, active, frame);
        textures.addProperty("particle", textureReference(controller.sideTexture(MultiblockControllerDefinition.Side.FRONT)));
        json.add("textures", textures);

        if (customModel == null) {
            JsonArray elements = new JsonArray();
            elements.add(controllerCasingElement(controller));
            addControllerSideOverlays(elements, controller);
            json.add("elements", elements);
        }
        return json;
    }

    private static JsonObject compositeControllerModel(
            MultiblockControllerDefinition controller,
            String customModel,
            boolean active,
            int frame
    ) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", "minecraft:block/block");
        json.addProperty("loader", "neoforge:composite");

        JsonObject textures = new JsonObject();
        textures.addProperty("particle", textureReference(controller.sideTexture(MultiblockControllerDefinition.Side.FRONT)));
        json.add("textures", textures);

        JsonObject children = new JsonObject();

        JsonObject base = new JsonObject();
        base.addProperty("parent", textureReference(customModel));
        children.add("base", base);

        JsonObject overlay = new JsonObject();
        overlay.addProperty("parent", "minecraft:block/block");
        overlay.addProperty("render_type", "minecraft:cutout");
        JsonObject overlayTextures = new JsonObject();
        addControllerOverlayTextures(overlayTextures, controller, active, frame);
        overlay.add("textures", overlayTextures);
        JsonArray overlayElements = new JsonArray();
        addControllerSideOverlays(overlayElements, controller);
        overlay.add("elements", overlayElements);
        children.add("overlay", overlay);

        json.add("children", children);

        JsonArray itemRenderOrder = new JsonArray();
        itemRenderOrder.add("base");
        itemRenderOrder.add("overlay");
        json.add("item_render_order", itemRenderOrder);
        return json;
    }

    private static boolean hasControllerOverlay(MultiblockControllerDefinition controller) {
        for (MultiblockControllerDefinition.Side side : MultiblockControllerDefinition.Side.values()) {
            if (controller.hasOverlay(side)) {
                return true;
            }
        }
        return false;
    }

    private static void addControllerBaseTextures(JsonObject textures, MultiblockControllerDefinition controller) {
        textures.addProperty("front_base", textureReference(controller.sideTexture(MultiblockControllerDefinition.Side.FRONT)));
        textures.addProperty("back_base", textureReference(controller.sideTexture(MultiblockControllerDefinition.Side.BACK)));
        textures.addProperty("left_base", textureReference(controller.sideTexture(MultiblockControllerDefinition.Side.LEFT)));
        textures.addProperty("right_base", textureReference(controller.sideTexture(MultiblockControllerDefinition.Side.RIGHT)));
        textures.addProperty("top_base", textureReference(controller.sideTexture(MultiblockControllerDefinition.Side.TOP)));
        textures.addProperty("bottom_base", textureReference(controller.sideTexture(MultiblockControllerDefinition.Side.BOTTOM)));
    }

    private static void addControllerOverlayTextures(
            JsonObject textures,
            MultiblockControllerDefinition controller,
            boolean active,
            int frame
    ) {
        for (MultiblockControllerDefinition.Side side : MultiblockControllerDefinition.Side.values()) {
            if (controller.hasOverlay(side)) {
                textures.addProperty(controllerOverlayTextureKey(side), textureReference(controllerOverlay(controller, side, active, frame)));
            }
        }
    }

    private static String controllerOverlay(
            MultiblockControllerDefinition controller,
            MultiblockControllerDefinition.Side side,
            boolean active,
            int frame
    ) {
        String idleOverlay = controller.idleOverlay(side);
        if (idleOverlay == null) {
            throw new IllegalStateException("Missing overlay for " + side + " on " + controller.registryName());
        }
        List<String> activeOverlays = controller.activeOverlays(side);
        if (!active || activeOverlays.isEmpty()) {
            return idleOverlay;
        }
        return activeOverlays.size() > 1 ? AnimatedMachineTextureProvider.texture(activeOverlays) : activeOverlays.get(0);
    }

    private static JsonObject controllerCasingElement(MultiblockControllerDefinition controller) {
        JsonObject element = element(0, 0, 0, 16, 16, 16);
        JsonObject faces = new JsonObject();
        addControllerFace(faces, "north", "#front_base", "north", controller, MultiblockControllerDefinition.Side.FRONT);
        addControllerFace(faces, "south", "#back_base", "south", controller, MultiblockControllerDefinition.Side.BACK);
        addControllerFace(faces, "west", "#left_base", "west", controller, MultiblockControllerDefinition.Side.LEFT);
        addControllerFace(faces, "east", "#right_base", "east", controller, MultiblockControllerDefinition.Side.RIGHT);
        addControllerFace(faces, "up", "#top_base", "up", controller, MultiblockControllerDefinition.Side.TOP);
        addControllerFace(faces, "down", "#bottom_base", "down", controller, MultiblockControllerDefinition.Side.BOTTOM);
        element.add("faces", faces);
        return element;
    }

    private static void addControllerFace(
            JsonObject faces,
            String direction,
            String texture,
            String cullface,
            MultiblockControllerDefinition controller,
            MultiblockControllerDefinition.Side side
    ) {
        JsonObject face = new JsonObject();
        face.addProperty("texture", texture);
        face.addProperty("cullface", cullface);
        if (controller.hasSideTextureColor(side)) {
            face.addProperty("tintindex", side.tintIndex());
        }
        faces.add(direction, face);
    }

    private static void addControllerSideOverlays(JsonArray elements, MultiblockControllerDefinition controller) {
        for (MultiblockControllerDefinition.Side side : MultiblockControllerDefinition.Side.values()) {
            if (controller.hasOverlay(side)) {
                elements.add(controllerSideOverlay(side));
            }
        }
    }

    private static JsonObject controllerSideOverlay(MultiblockControllerDefinition.Side side) {
        JsonObject element;
        JsonObject faces = new JsonObject();
        String texture = "#" + controllerOverlayTextureKey(side);

        switch (side) {
            case FRONT -> {
                element = element(0, 0, -0.01D, 16, 16, 0);
                addUntintedFace(faces, "north", texture);
            }
            case BACK -> {
                element = element(0, 0, 16, 16, 16, 16.01D);
                addUntintedFace(faces, "south", texture);
            }
            case LEFT -> {
                element = element(-0.01D, 0, 0, 0, 16, 16);
                addUntintedFace(faces, "west", texture);
            }
            case RIGHT -> {
                element = element(16, 0, 0, 16.01D, 16, 16);
                addUntintedFace(faces, "east", texture);
            }
            case TOP -> {
                element = element(0, 16, 0, 16, 16.01D, 16);
                addUntintedFace(faces, "up", texture);
            }
            case BOTTOM -> {
                element = element(0, -0.01D, 0, 16, 0, 16);
                addUntintedFace(faces, "down", texture);
            }
            default -> throw new IllegalStateException("Unknown controller side " + side);
        }

        element.add("faces", faces);
        return element;
    }

    private static String controllerOverlayTextureKey(MultiblockControllerDefinition.Side side) {
        return switch (side) {
            case FRONT -> "front_overlay";
            case BACK -> "back_overlay";
            case LEFT -> "left_overlay";
            case RIGHT -> "right_overlay";
            case TOP -> "top_overlay";
            case BOTTOM -> "bottom_overlay";
        };
    }

    private static JsonObject casingElement() {
        JsonObject element = element(0, 0, 0, 16, 16, 16);
        JsonObject faces = new JsonObject();
        addTintedFace(faces, "down", "#casing", "down");
        addTintedFace(faces, "up", "#casing", "up");
        addTintedFace(faces, "north", "#casing", "north");
        addTintedFace(faces, "south", "#casing", "south");
        addTintedFace(faces, "west", "#casing", "west");
        addTintedFace(faces, "east", "#casing", "east");
        element.add("faces", faces);
        return element;
    }

    private static JsonObject overlayElement() {
        JsonObject element = element(0, 0, -0.02, 16, 16, -0.01);
        JsonObject faces = new JsonObject();
        addUntintedFace(faces, "north", "#overlay");
        element.add("faces", faces);
        return element;
    }

    private static JsonObject colorOverlayElement() {
        JsonObject element = element(0, 0, -0.01, 16, 16, 0);
        JsonObject faces = new JsonObject();
        addTintedFace(faces, "north", "#color_overlay", 1);
        element.add("faces", faces);
        return element;
    }

    private static JsonObject staticCasingElement(boolean tinted, boolean includeFront) {
        JsonObject element = element(0, 0, 0, 16, 16, 16);
        JsonObject faces = new JsonObject();
        addFace(faces, "down", "#casing", "down", tinted);
        addFace(faces, "up", "#casing", "up", tinted);
        addFace(faces, "south", "#casing", "south", tinted);
        addFace(faces, "west", "#casing", "west", tinted);
        addFace(faces, "east", "#casing", "east", tinted);
        if (includeFront) {
            addFace(faces, "north", "#casing", "north", tinted);
        }
        element.add("faces", faces);
        return element;
    }

    private static JsonObject staticFrontElement() {
        JsonObject element = element(0, 0, -0.02, 16, 16, -0.01);
        JsonObject faces = new JsonObject();
        addUntintedFace(faces, "north", "#front");
        element.add("faces", faces);
        return element;
    }

    private static JsonObject staticFrontOverlayElement() {
        return staticFrontElement();
    }

    private static JsonObject staticKineticBodyElement(boolean tinted) {
        JsonObject element = element(0, 0, 1, 16, 16, 16);
        JsonObject faces = new JsonObject();
        addFace(faces, "east", "#casing", "east", tinted);
        addFace(faces, "west", "#casing", "west", tinted);
        addFace(faces, "south", "#casing", "south", tinted);
        addFace(faces, "up", "#casing", "up", tinted);
        addFace(faces, "down", "#casing", "down", tinted);
        element.add("faces", faces);
        return element;
    }

    private static JsonObject staticKineticOuterTopElement(boolean tinted) {
        JsonObject element = element(0, 15, 0, 16, 16, 1);
        JsonObject faces = new JsonObject();
        addFace(faces, "north", "#casing", "north", tinted);
        addFace(faces, "up", "#casing", "up", tinted);
        addFace(faces, "east", "#casing", "east", tinted);
        addFace(faces, "west", "#casing", "west", tinted);
        element.add("faces", faces);
        return element;
    }

    private static JsonObject staticKineticOuterBottomElement(boolean tinted) {
        JsonObject element = element(0, 0, 0, 16, 1, 1);
        JsonObject faces = new JsonObject();
        addFace(faces, "north", "#casing", "north", tinted);
        addFace(faces, "down", "#casing", "down", tinted);
        addFace(faces, "east", "#casing", "east", tinted);
        addFace(faces, "west", "#casing", "west", tinted);
        element.add("faces", faces);
        return element;
    }

    private static JsonObject staticKineticOuterLeftElement(boolean tinted) {
        JsonObject element = element(0, 1, 0, 1, 15, 1);
        JsonObject faces = new JsonObject();
        addFace(faces, "north", "#casing", "north", tinted);
        addFace(faces, "west", "#casing", "west", tinted);
        element.add("faces", faces);
        return element;
    }

    private static JsonObject staticKineticOuterRightElement(boolean tinted) {
        JsonObject element = element(15, 1, 0, 16, 15, 1);
        JsonObject faces = new JsonObject();
        addFace(faces, "north", "#casing", "north", tinted);
        addFace(faces, "east", "#casing", "east", tinted);
        element.add("faces", faces);
        return element;
    }

    private static JsonObject staticKineticFrontTopRingElement() {
        JsonObject element = element(1, 14, 0, 15, 15, 1);
        JsonObject faces = new JsonObject();
        addUntintedFace(faces, "north", "#front");
        addUntintedFace(faces, "down", "#front");
        addUntintedFace(faces, "east", "#front");
        addUntintedFace(faces, "west", "#front");
        element.add("faces", faces);
        return element;
    }

    private static JsonObject staticKineticFrontBottomRingElement() {
        JsonObject element = element(1, 1, 0, 15, 2, 1);
        JsonObject faces = new JsonObject();
        addUntintedFace(faces, "north", "#front");
        addUntintedFace(faces, "up", "#front");
        addUntintedFace(faces, "east", "#front");
        addUntintedFace(faces, "west", "#front");
        element.add("faces", faces);
        return element;
    }

    private static JsonObject staticKineticFrontLeftRingElement() {
        JsonObject element = element(1, 2, 0, 2, 14, 1);
        JsonObject faces = new JsonObject();
        addUntintedFace(faces, "north", "#front");
        addUntintedFace(faces, "east", "#front");
        element.add("faces", faces);
        return element;
    }

    private static JsonObject staticKineticFrontRightRingElement() {
        JsonObject element = element(14, 2, 0, 15, 14, 1);
        JsonObject faces = new JsonObject();
        addUntintedFace(faces, "north", "#front");
        addUntintedFace(faces, "west", "#front");
        element.add("faces", faces);
        return element;
    }

    private static JsonObject staticKineticInsetElement() {
        JsonObject element = element(2, 2, 0.5, 14, 14, 1);
        JsonObject faces = new JsonObject();
        addUntintedFace(faces, "north", "#front");
        element.add("faces", faces);
        return element;
    }

    private static JsonObject kineticBoxModel(MachineTier tier) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", "minecraft:block/block");
        json.addProperty("render_type", "minecraft:cutout");

        String casingTexture = tier.singleBlockMachineCasingSideTexture();
        JsonObject textures = new JsonObject();
        textures.addProperty("casing", casingTexture);
        textures.addProperty("kinetic_casing_single", KINETIC_CASING_SINGLE_TEXTURE);
        textures.addProperty("kinetic_inside_frame", KINETIC_INSIDE_FRAME_TEXTURE);
        textures.addProperty("kinetic_hole", KINETIC_HOLE_TEXTURE);
        textures.addProperty("particle", casingTexture);
        json.add("textures", textures);

        JsonArray elements = new JsonArray();
        elements.add(kineticBodyElement());
        elements.add(kineticOuterTopCasingElement());
        elements.add(kineticOuterBottomCasingElement());
        elements.add(kineticOuterLeftCasingElement());
        elements.add(kineticOuterRightCasingElement());
        elements.add(kineticFrontTopRingElement());
        elements.add(kineticFrontBottomRingElement());
        elements.add(kineticFrontLeftRingElement());
        elements.add(kineticFrontRightRingElement());
        elements.add(kineticInsideFrameElement());
        elements.add(kineticHoleElement());
        json.add("elements", elements);
        return json;
    }

    private static JsonObject kineticBodyElement() {
        JsonObject element = element(0, 0, 1, 16, 16, 16);
        JsonObject faces = new JsonObject();
        addTintedFace(faces, "east", "#casing", "east");
        addTintedFace(faces, "west", "#casing", "west");
        addTintedFace(faces, "south", "#casing", "south");
        addTintedFace(faces, "up", "#casing", "up");
        addTintedFace(faces, "down", "#casing", "down");
        element.add("faces", faces);
        return element;
    }

    private static JsonObject kineticOuterTopCasingElement() {
        JsonObject element = element(0, 15, 0, 16, 16, 1);
        JsonObject faces = new JsonObject();
        addTintedFace(faces, "north", "#casing", "north");
        addTintedFace(faces, "up", "#casing", "up");
        addUntintedFace(faces, "down", "#casing");
        addTintedFace(faces, "east", "#casing", "east");
        addTintedFace(faces, "west", "#casing", "west");
        element.add("faces", faces);
        return element;
    }

    private static JsonObject kineticOuterBottomCasingElement() {
        JsonObject element = element(0, 0, 0, 16, 1, 1);
        JsonObject faces = new JsonObject();
        addTintedFace(faces, "north", "#casing", "north");
        addUntintedFace(faces, "up", "#casing");
        addTintedFace(faces, "down", "#casing", "down");
        addTintedFace(faces, "east", "#casing", "east");
        addTintedFace(faces, "west", "#casing", "west");
        element.add("faces", faces);
        return element;
    }

    private static JsonObject kineticOuterLeftCasingElement() {
        JsonObject element = element(0, 1, 0, 1, 15, 1);
        JsonObject faces = new JsonObject();
        addTintedFace(faces, "north", "#casing", "north");
        addUntintedFace(faces, "east", "#casing");
        addTintedFace(faces, "west", "#casing", "west");
        element.add("faces", faces);
        return element;
    }

    private static JsonObject kineticOuterRightCasingElement() {
        JsonObject element = element(15, 1, 0, 16, 15, 1);
        JsonObject faces = new JsonObject();
        addTintedFace(faces, "north", "#casing", "north");
        addTintedFace(faces, "east", "#casing", "east");
        addUntintedFace(faces, "west", "#casing");
        element.add("faces", faces);
        return element;
    }

    private static JsonObject kineticFrontTopRingElement() {
        JsonObject element = element(1, 14, 0, 15, 15, 1);
        JsonObject faces = new JsonObject();
        addTintedFace(faces, "north", "#kinetic_casing_single", "north");
        addTintedFace(faces, "down", "#kinetic_casing_single");
        addTintedFace(faces, "east", "#kinetic_casing_single");
        addTintedFace(faces, "west", "#kinetic_casing_single");
        element.add("faces", faces);
        return element;
    }

    private static JsonObject kineticFrontBottomRingElement() {
        JsonObject element = element(1, 1, 0, 15, 2, 1);
        JsonObject faces = new JsonObject();
        addTintedFace(faces, "north", "#kinetic_casing_single", "north");
        addTintedFace(faces, "up", "#kinetic_casing_single");
        addTintedFace(faces, "east", "#kinetic_casing_single");
        addTintedFace(faces, "west", "#kinetic_casing_single");
        element.add("faces", faces);
        return element;
    }

    private static JsonObject kineticFrontLeftRingElement() {
        JsonObject element = element(1, 2, 0, 2, 14, 1);
        JsonObject faces = new JsonObject();
        addTintedFace(faces, "north", "#kinetic_casing_single", "north");
        addTintedFace(faces, "east", "#kinetic_casing_single");
        element.add("faces", faces);
        return element;
    }

    private static JsonObject kineticFrontRightRingElement() {
        JsonObject element = element(14, 2, 0, 15, 14, 1);
        JsonObject faces = new JsonObject();
        addTintedFace(faces, "north", "#kinetic_casing_single", "north");
        addTintedFace(faces, "west", "#kinetic_casing_single");
        element.add("faces", faces);
        return element;
    }

    private static JsonObject kineticInsideFrameElement() {
        JsonObject element = element(2, 2, 0.5, 14, 14, 1);
        JsonObject faces = new JsonObject();
        addTintedFace(faces, "north", "#kinetic_inside_frame", "north");
        element.add("faces", faces);
        return element;
    }

    private static JsonObject kineticHoleElement() {
        JsonObject element = element(3, 3, 0.49, 13, 13, 0.5);
        JsonObject faces = new JsonObject();
        addUntintedFace(faces, "north", "#kinetic_hole");
        element.add("faces", faces);
        return element;
    }

    private static JsonObject facingBlockstate(String modelName) {
        JsonObject variants = new JsonObject();
        variants.add("facing=down", variant(modelName, 90, 0));
        variants.add("facing=north", variant(modelName, 0));
        variants.add("facing=east", variant(modelName, 90));
        variants.add("facing=south", variant(modelName, 180));
        variants.add("facing=west", variant(modelName, 270));
        variants.add("facing=up", variant(modelName, 270, 0));

        JsonObject json = new JsonObject();
        json.add("variants", variants);
        return json;
    }

    private static JsonObject horizontalFacingBlockstate(String modelName) {
        JsonObject variants = new JsonObject();
        variants.add("facing=north", variant(modelName, 0));
        variants.add("facing=east", variant(modelName, 90));
        variants.add("facing=south", variant(modelName, 180));
        variants.add("facing=west", variant(modelName, 270));

        JsonObject json = new JsonObject();
        json.add("variants", variants);
        return json;
    }

    private static JsonObject controllerBlockstate(MultiblockControllerDefinition controller) {
        JsonObject variants = new JsonObject();
        for (boolean formed : new boolean[]{false, true}) {
            for (boolean active : new boolean[]{false, true}) {
                for (int frame = 0; frame < 1; frame++) {
                    addControllerVariants(variants, controller, formed, active, frame);
                }
            }
        }

        JsonObject json = new JsonObject();
        json.add("variants", variants);
        return json;
    }

    private static void addControllerVariants(
            JsonObject variants,
            MultiblockControllerDefinition controller,
            boolean formed,
            boolean active,
            int frame
    ) {
        addControllerVariant(variants, controller, formed, active, frame, "north", 0);
        addControllerVariant(variants, controller, formed, active, frame, "east", 90);
        addControllerVariant(variants, controller, formed, active, frame, "south", 180);
        addControllerVariant(variants, controller, formed, active, frame, "west", 270);
    }

    private static void addControllerVariant(
            JsonObject variants,
            MultiblockControllerDefinition controller,
            boolean formed,
            boolean active,
            int frame,
            String facing,
            int yRotation
    ) {
        boolean renderActive = formed && active;
        String key = "facing=" + facing + ",formed=" + formed + ",active=" + active;
        variants.add(key, variant(controllerModelName(controller, renderActive, frame), yRotation));
    }

    private static String controllerModelName(MultiblockControllerDefinition controller, boolean active, int frame) {
        if (!active) {
            return controller.registryName();
        }
        return frame == 0 ? controller.registryName() + "_formed" : controller.registryName() + "_formed_" + frame;
    }

    private static JsonObject variant(String modelName, int yRotation) {
        JsonObject variant = new JsonObject();
        variant.addProperty("model", Industron.MOD_ID + ":block/" + modelName);
        if (yRotation != 0) {
            variant.addProperty("y", yRotation);
        }
        return variant;
    }

    private static JsonObject variant(String modelName, int xRotation, int yRotation) {
        JsonObject variant = variant(modelName, yRotation);
        if (xRotation != 0) {
            variant.addProperty("x", xRotation);
        }
        return variant;
    }

    private static JsonObject itemModel(String modelName) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", Industron.MOD_ID + ":block/" + modelName);
        return json;
    }

    private static JsonObject element(double fromX, double fromY, double fromZ, double toX, double toY, double toZ) {
        JsonObject element = new JsonObject();
        element.add("from", vec(fromX, fromY, fromZ));
        element.add("to", vec(toX, toY, toZ));
        return element;
    }

    private static JsonArray vec(double x, double y, double z) {
        JsonArray array = new JsonArray();
        array.add(x);
        array.add(y);
        array.add(z);
        return array;
    }

    private static String textureReference(String texture) {
        if (texture.contains(":")) {
            return texture;
        }

        return Industron.MOD_ID + ":" + texture;
    }

    private static void addTintedFace(JsonObject faces, String direction, String texture, String cullface) {
        JsonObject face = new JsonObject();
        face.addProperty("texture", texture);
        face.addProperty("cullface", cullface);
        face.addProperty("tintindex", 0);
        faces.add(direction, face);
    }

    private static void addTintedFace(JsonObject faces, String direction, String texture) {
        addTintedFace(faces, direction, texture, 0);
    }

    private static void addTintedFace(JsonObject faces, String direction, String texture, int tintIndex) {
        JsonObject face = new JsonObject();
        face.addProperty("texture", texture);
        face.addProperty("tintindex", tintIndex);
        faces.add(direction, face);
    }

    private static void addUntintedFace(JsonObject faces, String direction, String texture) {
        JsonObject face = new JsonObject();
        face.addProperty("texture", texture);
        faces.add(direction, face);
    }

    private static void addFace(JsonObject faces, String direction, String texture, String cullface, boolean tinted) {
        JsonObject face = new JsonObject();
        face.addProperty("texture", texture);
        face.addProperty("cullface", cullface);
        if (tinted) {
            face.addProperty("tintindex", 0);
        }
        faces.add(direction, face);
    }

    private static boolean colorable(java.util.Set<net.mads.industron.machine.machines.electric.multiblock.MultiblockAbility> abilities) {
        return abilities.contains(net.mads.industron.machine.machines.electric.multiblock.MultiblockAbility.ITEM_INPUT)
                || abilities.contains(net.mads.industron.machine.machines.electric.multiblock.MultiblockAbility.ITEM_OUTPUT)
                || abilities.contains(net.mads.industron.machine.machines.electric.multiblock.MultiblockAbility.FLUID_INPUT)
                || abilities.contains(net.mads.industron.machine.machines.electric.multiblock.MultiblockAbility.FLUID_OUTPUT)
                || abilities.contains(net.mads.industron.machine.machines.electric.multiblock.MultiblockAbility.IO_INTERFACE);
    }
}
