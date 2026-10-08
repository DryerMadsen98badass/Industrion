package net.mads.industron.machine.machines.without_energy.singleblock.machines;

import net.mads.industron.machine.MachineTier;
import net.mads.industron.machine.SingleBlockDefinition;
import net.mads.industron.machine.interaction.BlockInteraction;
import net.mads.industron.machine.interaction.BlockRequirement;
import net.mads.industron.machine.interaction.ConditionFailure;
import net.mads.industron.machine.interaction.InteractionPhase;
import net.mads.industron.machine.interaction.MachineArea;
import net.mads.industron.machine.interaction.MachineCondition;
import net.mads.industron.recipe.CERecipeTypes;
import net.mads.industron.recipe.recipes.assembly.Tool;

/** Primitive river-powered washer for separating trace minerals from loose material. */
public final class RiverWasher {
    private static final String WATER_SIDES = "water_sides";

    public static final SingleBlockDefinition DEFINITION = SingleBlockDefinition.machine()
            .machineDefinition(SingleBlockDefinition.Option.id("river_washer"))
            .machineDefinition(SingleBlockDefinition.Option.displayName("River Washer"))
            .machineDefinition(SingleBlockDefinition.Option.onlyTier(MachineTier.NONE))
            .machineDefinition(SingleBlockDefinition.Option.recipeType(CERecipeTypes.RIVER_WASHER))
            .machineDefinition(SingleBlockDefinition.Option.slots(1, 16, 0, 0))
            .machineDefinition(SingleBlockDefinition.Option.waterloggable())
            .machineDefinition(SingleBlockDefinition.Option.model("block/machines/without_energy/river_washer_base"))
            .machineDefinition(oakFaces())
            .machineDefinition(SingleBlockDefinition.Option.area(
                    MachineArea.area(WATER_SIDES)
                            .include(MachineArea.box().offsetFront(1))
                            .include(MachineArea.box().offsetBack(1))
                            .include(MachineArea.box().offsetLeft(1))
                            .include(MachineArea.box().offsetRight(1))
            ))
            .machineDefinition(SingleBlockDefinition.Option.condition(
                    MachineCondition.biomeTag("minecraft:is_river")
                            .check(InteractionPhase.WHILE_PROCESSING)
                            .onFailure(ConditionFailure.PAUSE)
            ))
            .machineDefinition(SingleBlockDefinition.Option.condition(
                    MachineCondition.height().between(53, 63)
                            .check(InteractionPhase.WHILE_PROCESSING)
                            .onFailure(ConditionFailure.PAUSE)
            ))
            .machineDefinition(SingleBlockDefinition.Option.condition(
                    MachineCondition.isWaterlogged()
                            .check(InteractionPhase.WHILE_PROCESSING)
                            .onFailure(ConditionFailure.PAUSE)
            ))
            .machineDefinition(SingleBlockDefinition.Option.blockInteraction(
                    BlockInteraction.require()
                            .inArea(WATER_SIDES)
                            .minimumMatches(2)
                            .requires(BlockRequirement.fluid("minecraft:water"))
                            .when(InteractionPhase.ON_START)
            ))
            .machineDefinition(SingleBlockDefinition.Option.blockInteraction(
                    BlockInteraction.require()
                            .inArea(WATER_SIDES)
                            .minimumMatches(2)
                            .requires(BlockRequirement.fluid("minecraft:water"))
                            .when(InteractionPhase.ON_COMPLETE)
            ))
            .machineDefinition(SingleBlockDefinition.Option.blockInteraction(
                    BlockInteraction.require()
                            .top(1)
                            .requires(BlockRequirement.not(BlockRequirement.fluid("minecraft:water")))
                            .when(InteractionPhase.ON_START)
            ))
            .machineDefinition(SingleBlockDefinition.Option.blockInteraction(
                    BlockInteraction.require()
                            .top(1)
                            .requires(BlockRequirement.not(BlockRequirement.fluid("minecraft:water")))
                            .when(InteractionPhase.ON_COMPLETE)
            ))
            .machineDefinition(SingleBlockDefinition.Option.tooltip("Must be waterlogged in a river biome between Y 53 and Y 63."))
            .machineDefinition(SingleBlockDefinition.Option.tooltip("At least two horizontal sides must touch water; the block above must not contain water."))
            .machineDefinition(SingleBlockDefinition.Option.tooltip("Uses the river itself as process water; no fluid input is required."))
            .machineDefinition(SingleBlockDefinition.Option.mineableWith(Tool.AXE))
            .machineDefinition(SingleBlockDefinition.Option.breakingTier(MachineTier.ULV))
            .machineDefinition(SingleBlockDefinition.Option.strength(1.5F, 2.0F))
            .build();

    private static SingleBlockDefinition.Option oakFaces() {
        return builder -> {
            String texture = "minecraft:block/oak_planks";
            SingleBlockDefinition.Option.frontTexture(texture).apply(builder);
            SingleBlockDefinition.Option.backTexture(texture).apply(builder);
            SingleBlockDefinition.Option.leftTexture(texture).apply(builder);
            SingleBlockDefinition.Option.rightTexture(texture).apply(builder);
            SingleBlockDefinition.Option.topTexture(texture).apply(builder);
            SingleBlockDefinition.Option.bottomTexture(texture).apply(builder);
        };
    }

    private RiverWasher() {
    }
}
