package net.mads.industron.machine.machines.electric.singleblock;

import net.mads.industron.block.MiningTier;
import net.mads.industron.block.MiningTool;
import net.mads.industron.gui.ProgressBar;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.machine.SingleBlockDefinition;
import net.mads.industron.recipe.CERecipeTypes;

import java.util.List;

public final class ElectricSingleBlockMachines {
    public static final SingleBlockDefinition TEST_MACHINE =
            SingleBlockDefinition.machine()
                    .machineDefinition(SingleBlockDefinition.Option.id("test_machine"))
                    .machineDefinition(SingleBlockDefinition.Option.displayName("Test Machine"))
                    .machineDefinition(SingleBlockDefinition.Option.consumesEnergy())
                    .machineDefinition(SingleBlockDefinition.Option.tier(MachineTier.LV))
                    .machineDefinition(SingleBlockDefinition.Option.recipeType(CERecipeTypes.TEST_PROCESSING))
                    .machineDefinition(SingleBlockDefinition.Option.slots(1, 1, 1, 1))
                    .machineDefinition(SingleBlockDefinition.Option.energyUsage(4))
                    .machineDefinition(SingleBlockDefinition.Option.progressBar(ProgressBar.ARROW))
                    .machineDefinition(SingleBlockDefinition.Option.frontOverlay("block/machines/overlay/test_machine/overlay_front"))
                    .machineDefinition(SingleBlockDefinition.Option.tooltip("Minimal electric machine used to verify the Industron framework."))
                    .machineDefinition(SingleBlockDefinition.Option.mineableWith(MiningTool.PICKAXE))
                    .machineDefinition(SingleBlockDefinition.Option.miningTier(MiningTier.IRON))
                    .build();

    public static final List<SingleBlockDefinition> ALL = List.of(TEST_MACHINE);

    private ElectricSingleBlockMachines() {
    }
}