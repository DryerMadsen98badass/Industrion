package net.mads.industron.machine.machines.without_energy.multiblock.machines;

import net.mads.industron.machine.MachineTier;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockControllerDefinition;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockDefinition;
import net.mads.industron.recipe.CERecipeTypes;
import net.mads.industron.recipe.recipes.assembly.Tool;
import net.minecraft.world.level.block.Blocks;

import static net.mads.industron.machine.machines.electric.multiblock.MultiblockPattern.row;
import static net.mads.industron.machine.machines.electric.multiblock.MultiblockPredicates.*;

/** LV, brick-built, fuel-fired Blast Furnace. Fuel runtime is supplied by the shared FUEL system. */
public final class BlastFurnace {
    public static final MultiblockControllerDefinition CONTROLLER = MultiblockControllerDefinition.machine()
            .machineDefinition(MultiblockControllerDefinition.Option.id("blast_furnace"))
            .machineDefinition(MultiblockControllerDefinition.Option.displayName("Blast Furnace"))
            .machineDefinition(MultiblockControllerDefinition.Option.tier(MachineTier.LV))
            .machineDefinition(MultiblockControllerDefinition.Option.model("minecraft:block/bricks"))
            .machineDefinition(MultiblockControllerDefinition.Option.frontTexture("minecraft:block/bricks"))
            .machineDefinition(MultiblockControllerDefinition.Option.backTexture("minecraft:block/bricks"))
            .machineDefinition(MultiblockControllerDefinition.Option.leftTexture("minecraft:block/bricks"))
            .machineDefinition(MultiblockControllerDefinition.Option.rightTexture("minecraft:block/bricks"))
            .machineDefinition(MultiblockControllerDefinition.Option.topTexture("minecraft:block/bricks"))
            .machineDefinition(MultiblockControllerDefinition.Option.bottomTexture("minecraft:block/bricks"))
            .machineDefinition(MultiblockControllerDefinition.Option.frontOverlay(
                    "block/machines/overlay/blast_furnace/blast_furnace_off",
                    "block/machines/overlay/blast_furnace/blast_furnace_on"
            ))
            .machineDefinition(MultiblockControllerDefinition.Option.strength(Blocks.BRICKS))
            .machineDefinition(MultiblockControllerDefinition.Option.sound(Blocks.BRICKS))
            .machineDefinition(MultiblockControllerDefinition.Option.mineableWith(Tool.PICKAXE))
            .machineDefinition(MultiblockControllerDefinition.Option.breakingTier(MachineTier.LV))
            .build();

    public static final MultiblockDefinition DEFINITION = MultiblockDefinition.machine()
            .machineDefinition(MultiblockDefinition.Option.id("blast_furnace"))
            .machineDefinition(MultiblockDefinition.Option.controller(CONTROLLER))
            .machineDefinition(MultiblockDefinition.Option.displayName("Blast Furnace"))
            .machineDefinition(MultiblockDefinition.Option.recipeType(CERecipeTypes.BLAST_FURNACE))
            .machineDefinition(MultiblockDefinition.Option.parallel(4))
            .machineDefinition(MultiblockDefinition.Option.tooltip(
                    "Supply solid fuel through an Item Input Bus.",
                    "One Fuel Unit supplies 40 processing ticks per parallel.",
                    "Fuel demand scales with the number of active parallels."))
            // Pattern coordinates follow the runtime directly:
            // - layer(...) = left -> right
            // - rows inside each layer = bottom -> top
            // - symbols inside each row = front -> back
            // The 3x3 firebox floor is therefore the first row of every layer.
            // The controller sits centered on the front wall, one block above the fireboxes.
            .machineDefinition(MultiblockDefinition.Option.variant("default", pattern -> pattern
                    .layer(
                            row('f', 'f', 'f'),
                            row('b', 'b', 'b'),
                            row('b', 'b', 'b'),
                            row('b', 'b', 'b')
                    )
                    .layer(
                            row('f', 'f', 'f'),
                            row('@', 'a', 'b'),
                            row('b', 'a', 'b'),
                            row('b', 'b', 'b')
                    )
                    .layer(
                            row('f', 'f', 'f'),
                            row('b', 'b', 'b'),
                            row('b', 'b', 'b'),
                            row('b', 'b', 'b')
                    )
            ))
            .machineDefinition(MultiblockDefinition.Option.where('b',
                    block("minecraft:bricks").min(20).or(ability(needed))
            ))
            .machineDefinition(MultiblockDefinition.Option.where('f', block("industron:clay_firebox")))
            .machineDefinition(MultiblockDefinition.Option.where('a', air()))
            .build();

    private BlastFurnace() {
    }
}
