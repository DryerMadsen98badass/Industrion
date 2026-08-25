package net.mads.industron.machine.machines.electric.multiblock;

import net.mads.industron.block.MiningTier;
import net.mads.industron.block.MiningTool;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.recipe.CERecipeTypes;

import java.util.List;

import static net.mads.industron.machine.machines.electric.multiblock.MultiblockAbility.ENERGY_INPUT;
import static net.mads.industron.machine.machines.electric.multiblock.MultiblockAbility.ITEM_INPUT;
import static net.mads.industron.machine.machines.electric.multiblock.MultiblockAbility.ITEM_OUTPUT;
import static net.mads.industron.machine.machines.electric.multiblock.MultiblockPattern.row;

public final class MultiblockDefinitions {
    public static final MultiblockControllerDefinition TEST_MULTIBLOCK_CONTROLLER =
            MultiblockControllerDefinition.controller()
                    .controllerDefinition(MultiblockControllerDefinition.Option.id("test_multiblock"))
                    .controllerDefinition(MultiblockControllerDefinition.Option.displayName("Test Multiblock"))
                    .controllerDefinition(MultiblockControllerDefinition.Option.model("industron:block/lv_machine_casing"))
                    .controllerDefinition(MultiblockControllerDefinition.Option.frontOverlay("block/machines/overlay/test_machine/overlay_front"))
                    .controllerDefinition(MultiblockControllerDefinition.Option.mineableWith(MiningTool.PICKAXE))
                    .controllerDefinition(MultiblockControllerDefinition.Option.miningTier(MiningTier.IRON))
                    .build();

    public static final MultiblockDefinition TEST_MULTIBLOCK =
            MultiblockDefinition.machine()
                    .machineDefinition(MultiblockDefinition.Option.id("test_multiblock"))
                    .machineDefinition(MultiblockDefinition.Option.displayName("Test Multiblock"))
                    .machineDefinition(MultiblockDefinition.Option.controller(TEST_MULTIBLOCK_CONTROLLER))
                    .machineDefinition(MultiblockDefinition.Option.recipeType(CERecipeTypes.TEST_PROCESSING))
                    .machineDefinition(MultiblockDefinition.Option.tier(MachineTier.LV))
                    .machineDefinition(MultiblockDefinition.Option.energyUsage(4))
                    .machineDefinition(MultiblockDefinition.Option.tooltip("Minimal multiblock used to verify the Industron framework."))
                    .machineDefinition(MultiblockDefinition.Option.variant("default", pattern -> pattern
                            .layer(row('C', 'E', 'A'), row('I', 'O', 'A'))))
                    .machineDefinition(MultiblockDefinition.Option.where('A', "industron:lv_machine_casing"))
                    .machineDefinition(MultiblockDefinition.Option.where('I', ITEM_INPUT))
                    .machineDefinition(MultiblockDefinition.Option.where('O', ITEM_OUTPUT))
                    .machineDefinition(MultiblockDefinition.Option.where('E', ENERGY_INPUT))
                    .build();

    public static final List<MultiblockDefinition> ALL = List.of(TEST_MULTIBLOCK);

    private MultiblockDefinitions() {
    }

    public static void bootstrap() {
        ALL.forEach(MultiblockRegistry::register);
    }

    public static List<MultiblockControllerDefinition> controllers() {
        return ALL.stream().map(MultiblockDefinition::controller).toList();
    }
}
