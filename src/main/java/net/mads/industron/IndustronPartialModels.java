package net.mads.industron;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.lang.Lang;
import net.mads.industron.energy.WireThickness;
import net.mads.industron.material.structure.StructureBlockDefinition;
import net.mads.industron.material.structure.StructureMaterial;
import net.mads.industron.material.structure.StructureMaterialGenerator;
import net.mads.industron.material.structure.StructureMaterials;
import net.mads.industron.transport.FluidTransportTier;
import net.mads.industron.transport.color.PipeColorDefinitions;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.registries.BuiltInRegistries;
import net.mads.industron.kinetics.shaft.AbstractMaterialShaftBlock;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class IndustronPartialModels {
    public static final PartialModel BRICK_MOLD_INSIDE = block("brick_mold_inside");
    private static final Map<String, Map<Direction, PartialModel>> ENERGY_WIRE_ARMS = new LinkedHashMap<>();
    private static final Map<String, PartialModel> FLUID_PIPE_CASINGS = new LinkedHashMap<>();
    private static final Map<Block, PartialModel> MATERIAL_SHAFTS = new LinkedHashMap<>();
    private static final Map<
            String,
            Map<FluidTransportBehaviour.AttachmentTypes.ComponentPartials, Map<Direction, PartialModel>>
            > PIPE_ATTACHMENTS = new LinkedHashMap<>();

    private static final Map<String, PartialModel> KINETIC_MACHINE_PARTS = new LinkedHashMap<>();

    static {
        registerKineticParts("lathe");
        registerKineticParts("mechanical_centrifuge");
        registerKineticParts("mechanical_sifter");
        registerKineticParts("pulverizer");
        registerKineticParts("wire_drawing_machine");
        registerKineticParts("winding_machine");
        registerKineticParts("mechanical_bender");
        registerKineticParts("magnetic_separator");

        for (WireThickness thickness : WireThickness.ALL) {
            registerEnergyWireModels(thickness, false);
            registerEnergyWireModels(thickness, true);
        }
        for (FluidTransportTier tier : FluidTransportTier.all()) {
            registerPipeModels(tier.pipeId());
        }
        for (DyeColor color : DyeColor.values()) {
            registerPipeModels(PipeColorDefinitions.sharedColoredModelId(color));
        }
    }

    private static void registerKineticParts(String id) {
        for (String part : new String[]{"rotor", "moving"}) {
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                String key = id + "/" + part + "_" + facing.getSerializedName();
                KINETIC_MACHINE_PARTS.put(key, block("machines/kinetic/" + key));
            }
        }
    }

    public static PartialModel kineticPart(String id, String part, Direction facing) {
        String key = id + "/" + part + "_" + facing.getSerializedName();
        PartialModel model = KINETIC_MACHINE_PARTS.get(key);
        if (model == null) throw new IllegalArgumentException("Missing kinetic partial " + key);
        return model;
    }

    private IndustronPartialModels() {
    }


    public static PartialModel energyWireArm(WireThickness thickness, boolean insulated, Direction direction) {
        String key = energyWireModelPrefix(thickness, insulated);
        Map<Direction, PartialModel> directions = ENERGY_WIRE_ARMS.get(key);
        if (directions == null || !directions.containsKey(direction)) {
            throw new IllegalArgumentException("Missing energy wire partial for " + key + " " + direction);
        }
        return directions.get(direction);
    }

    private static void registerEnergyWireModels(WireThickness thickness, boolean insulated) {
        String prefix = energyWireModelPrefix(thickness, insulated);
        Map<Direction, PartialModel> directions = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) {
            directions.put(direction, block("energy/wire/" + prefix + "_" + direction.getSerializedName()));
        }
        ENERGY_WIRE_ARMS.put(prefix, directions);
    }

    private static String energyWireModelPrefix(WireThickness thickness, boolean insulated) {
        return (insulated ? "insulated_wire_" : "wire_") + thickness.id();
    }

    public static PartialModel fluidPipeCasing(String modelId) {
        PartialModel model = FLUID_PIPE_CASINGS.get(modelId);
        if (model == null) {
            throw new IllegalArgumentException("Missing fluid pipe casing partial for " + modelId);
        }
        return model;
    }

    public static PartialModel pipeAttachment(
            String modelId,
            FluidTransportBehaviour.AttachmentTypes.ComponentPartials partial,
            Direction direction
    ) {
        Map<FluidTransportBehaviour.AttachmentTypes.ComponentPartials, Map<Direction, PartialModel>> partials =
                PIPE_ATTACHMENTS.get(modelId);
        if (partials == null || !partials.containsKey(partial) || !partials.get(partial).containsKey(direction)) {
            throw new IllegalArgumentException("Missing fluid pipe attachment partial for " + modelId);
        }
        return partials.get(partial).get(direction);
    }

    private static void registerPipeModels(String modelId) {
        FLUID_PIPE_CASINGS.put(modelId, block(modelId + "/casing"));

        Map<FluidTransportBehaviour.AttachmentTypes.ComponentPartials, Map<Direction, PartialModel>> partials =
                new EnumMap<>(FluidTransportBehaviour.AttachmentTypes.ComponentPartials.class);
        for (FluidTransportBehaviour.AttachmentTypes.ComponentPartials partial
                : FluidTransportBehaviour.AttachmentTypes.ComponentPartials.values()) {
            Map<Direction, PartialModel> directions = new EnumMap<>(Direction.class);
            for (Direction direction : Iterate.directions) {
                directions.put(
                        direction,
                        block(
                                modelId
                                        + "/"
                                        + Lang.asId(partial.name())
                                        + "/"
                                        + Lang.asId(direction.getSerializedName())
                        )
                );
            }
            partials.put(partial, directions);
        }
        PIPE_ATTACHMENTS.put(modelId, partials);
    }

    private static PartialModel block(String path) {
        return PartialModel.of(ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, "block/" + path));
    }

    public static void init() {
        // Loads all static, per-tier, and colored pipe partial models before renderers request them.
    }

    public static void initClient() {
        registerMaterialShafts();
        registerStructureFoldingDoors();
    }

    public static PartialModel materialShaft(Block block) {
        PartialModel model = MATERIAL_SHAFTS.get(block);
        if (model == null) {
            throw new IllegalArgumentException("Missing material shaft partial for " + BuiltInRegistries.BLOCK.getKey(block));
        }
        return model;
    }

    private static void registerMaterialShafts() {
        BuiltInRegistries.BLOCK.forEach(block -> {
            if (!(block instanceof AbstractMaterialShaftBlock)) {
                return;
            }
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            MATERIAL_SHAFTS.put(
                    block,
                    PartialModel.of(ResourceLocation.fromNamespaceAndPath(
                            id.getNamespace(),
                            "block/" + id.getPath() + "_rotating"
                    ))
            );
        });
    }

    private static void registerStructureFoldingDoors() {
        for (StructureMaterial material : StructureMaterials.ALL) {
            for (StructureBlockDefinition definition
                    : StructureMaterialGenerator.generatedBlockDefinitions(material)) {
                if (definition.modelKind() != StructureBlockDefinition.ModelKind.CREATE_DOOR) {
                    continue;
                }
                String sourceModel = definition.modelTemplate().orElse("");
                if (!sourceModel.equals("copper_door") && !sourceModel.equals("andesite_door")) {
                    continue;
                }

                ResourceLocation blockId = ResourceLocation.fromNamespaceAndPath(
                        Industron.MOD_ID,
                        definition.registryName()
                );
                AllPartialModels.FOLDING_DOORS.put(
                        blockId,
                        Couple.create(
                                block(definition.registryName() + "_fold_left"),
                                block(definition.registryName() + "_fold_right")
                        )
                );
            }
        }
    }
}
