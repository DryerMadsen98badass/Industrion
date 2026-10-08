package net.mads.industron.machine.machines.kinetic;

import com.simibubi.create.api.stress.BlockStressValues;
import net.mads.industron.machine.MachineDefinition;
import net.mads.industron.machine.SingleBlockMachinePower;
import net.mads.industron.registry.BlockRegistry;
import net.mads.industron.transport.FluidTransportRegistrations;

public final class KineticMachineStress {
    private KineticMachineStress() {
    }

    public static void register() {
        KineticMachines.registerStress();
        MachineDefinition.INSTANCES.stream()
                .filter(instance -> instance.definition().power() == SingleBlockMachinePower.KINETIC)
                .forEach(instance -> {
                    if (instance.definition().usesKineticInput()) {
                        BlockStressValues.IMPACTS.register(
                                BlockRegistry.getSingleBlockMachine(instance.registryName()).get(),
                                instance::kineticSuPerRpm
                        );
                    } else if (instance.definition().usesKineticOutput()) {
                        BlockStressValues.CAPACITIES.register(
                                BlockRegistry.getSingleBlockMachine(instance.registryName()).get(),
                                instance::kineticSuPerRpm
                        );
                    }
                });
        FluidTransportRegistrations.allBlocks().forEach(registration ->
                BlockStressValues.IMPACTS.register(
                        registration.pump().get(),
                        registration.tier()::pumpStressImpact
                )
        );
    }
}