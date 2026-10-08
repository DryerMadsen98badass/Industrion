package net.mads.industron.machine.machines.without_energy.multiblock.machines;

import net.mads.industron.recipe.recipes.assembly.Tool;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.machine.interaction.AreaValue;
import net.mads.industron.machine.interaction.MachineArea;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockControllerDefinition;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockDefinition;
import net.mads.industron.recipe.CERecipeTypes;
import net.minecraft.world.level.block.Blocks;

import static net.mads.industron.machine.machines.electric.multiblock.MultiblockPattern.row;
import static net.mads.industron.machine.machines.electric.multiblock.MultiblockPredicates.air;
import static net.mads.industron.machine.machines.electric.multiblock.MultiblockPredicates.block;
import static net.mads.industron.machine.machines.electric.multiblock.MultiblockPredicates.tag;

/** Primitive 5x5x5 earth-covered charcoal pit with a 3x3x3 processing volume. */
public final class CharcoalPit {
    public static final String INSIDE_AREA = "inside";

    /**
     * The controller intentionally behaves like dirt in the world.  The custom Industron
     * breaking system still supplies SHOVEL as the preferred tool family and ULV as the tier.
     */
    public static final MultiblockControllerDefinition CONTROLLER = MultiblockControllerDefinition.machine()
            .machineDefinition(MultiblockControllerDefinition.Option.id("charcoal_pit"))
            .machineDefinition(MultiblockControllerDefinition.Option.displayName("Charcoal Pit"))
            .machineDefinition(MultiblockControllerDefinition.Option.model("minecraft:block/dirt"))
            .machineDefinition(MultiblockControllerDefinition.Option.strength(Blocks.DIRT))
            .machineDefinition(MultiblockControllerDefinition.Option.sound(Blocks.DIRT))
            .machineDefinition(MultiblockControllerDefinition.Option.loot(Blocks.DIRT))
            .machineDefinition(MultiblockControllerDefinition.Option.mineableWith(Tool.SHOVEL))
            .machineDefinition(MultiblockControllerDefinition.Option.breakingTier(MachineTier.ULV))
            .machineDefinition(MultiblockControllerDefinition.Option.wrenchable(false))
            .machineDefinition(MultiblockControllerDefinition.Option.openMenu(false))
            .build();

    public static final MultiblockDefinition DEFINITION = MultiblockDefinition.machine()
            .machineDefinition(MultiblockDefinition.Option.id("charcoal_pit"))
            .machineDefinition(MultiblockDefinition.Option.controller(CONTROLLER))
            .machineDefinition(MultiblockDefinition.Option.displayName("Charcoal Pit"))
            .machineDefinition(MultiblockDefinition.Option.recipeType(CERecipeTypes.PRIMITIVE_PYROLYSIS))
            .machineDefinition(MultiblockDefinition.Option.activationItem("minecraft:flint_and_steel"))
            .machineDefinition(MultiblockDefinition.Option.activationItem("industron:flint_and_pebble"))
            .machineDefinition(MultiblockDefinition.Option.tooltip(
                    "Fill the 3 x 3 x 3 inside with logs, then cover the pit with dirt.",
                    "Use Flint and Steel or Flint and Pebble on the dirt-like controller to ignite one batch."
            ))
            // Controller is centered in the roof.  The 3x3x3 work volume is two blocks below it.
            .machineDefinition(MultiblockDefinition.Option.area(
                    MachineArea.area(INSIDE_AREA)
                            .include(MachineArea.centeredBox(
                                    AreaValue.fixed(3),
                                    AreaValue.fixed(3),
                                    AreaValue.fixed(3)
                            ).offsetBottom(2))
            ))
            .machineDefinition(MultiblockDefinition.Option.variant("default", pattern -> pattern
                    .layer(
                            row('d', 'd', 'd', 'd', 'd'),
                            row('d', 'd', 'd', 'd', 'd'),
                            row('d', 'd', 'd', 'd', 'd'),
                            row('d', 'd', 'd', 'd', 'd'),
                            row('d', 'd', 'd', 'd', 'd')
                    )
                    .layer(
                            row('d', 'd', 'd', 'd', 'd'),
                            row('d', 'i', 'i', 'i', 'd'),
                            row('d', 'i', 'i', 'i', 'd'),
                            row('d', 'i', 'i', 'i', 'd'),
                            row('d', 'd', 'd', 'd', 'd')
                    )
                    .layer(
                            row('d', 'd', 'd', 'd', 'd'),
                            row('d', 'i', 'i', 'i', 'd'),
                            row('d', 'i', 'i', 'i', 'd'),
                            row('d', 'i', 'i', 'i', 'd'),
                            row('d', 'd', '@', 'd', 'd')
                    )
                    .layer(
                            row('d', 'd', 'd', 'd', 'd'),
                            row('d', 'i', 'i', 'i', 'd'),
                            row('d', 'i', 'i', 'i', 'd'),
                            row('d', 'i', 'i', 'i', 'd'),
                            row('d', 'd', 'd', 'd', 'd')
                    )
                    .layer(
                            row('d', 'd', 'd', 'd', 'd'),
                            row('d', 'd', 'd', 'd', 'd'),
                            row('d', 'd', 'd', 'd', 'd'),
                            row('d', 'd', 'd', 'd', 'd'),
                            row('d', 'd', 'd', 'd', 'd')
                    )
            ))
            .machineDefinition(MultiblockDefinition.Option.where('d', block("minecraft:dirt")))
            // Empty slots are allowed; logs are the recipe input; finished charcoal remains valid.
            .machineDefinition(MultiblockDefinition.Option.where('i',
                    air()
                            .or(tag("minecraft:logs"))
                            .or(block("industron:charcoal_block"))
            ))
            .build();

    private CharcoalPit() {
    }
}
