package net.mads.industron.machine.machines.electric.multiblock;

import net.mads.industron.machine.machines.without_energy.multiblock.machines.CharcoalPit;
import net.mads.industron.machine.machines.without_energy.multiblock.machines.BlastFurnace;
import net.mads.industron.machine.machines.electric.multiblock.machines.ElectricHeater;

import java.util.List;

public final class MultiblockDefinitions {
    public static final List<MultiblockDefinition> ALL = List.of(
            ElectricHeater.DEFINITION,
            CharcoalPit.DEFINITION,
            BlastFurnace.DEFINITION
    );

    private MultiblockDefinitions() {
    }

    public static void bootstrap() {
        ALL.forEach(MultiblockRegistry::register);
    }

    public static List<MultiblockControllerDefinition> controllers() {
        return ALL.stream().map(MultiblockDefinition::controller).toList();
    }
}
