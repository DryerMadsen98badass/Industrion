package net.mads.industron.compat.create;

import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.DoorMovingInteraction;
import com.simibubi.create.content.contraptions.behaviour.TrapdoorMovingInteraction;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorMovementBehaviour;
import net.mads.industron.material.structure.StructureBlockDefinition;
import net.mads.industron.material.structure.StructureMaterial;
import net.mads.industron.material.structure.StructureMaterialGenerator;
import net.mads.industron.material.structure.StructureMaterials;
import net.mads.industron.registry.BlockRegistry;
import net.minecraft.world.level.block.Block;

/** Registers the Create contraption behaviours used by generated structure blocks. */
public final class StructureCreateBehaviours {
    private static boolean registered;

    private StructureCreateBehaviours() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        for (StructureMaterial material : StructureMaterials.ALL) {
            for (StructureBlockDefinition definition
                    : StructureMaterialGenerator.generatedBlockDefinitions(material)) {
                Block block = BlockRegistry.getStructureMaterialBlock(definition.registryName()).get();

                switch (definition.modelKind()) {
                    case CREATE_DOOR -> {
                        MovingInteractionBehaviour.REGISTRY.register(block, new DoorMovingInteraction());
                        MovementBehaviour.REGISTRY.register(block, new SlidingDoorMovementBehaviour());
                    }
                    case CREATE_TRAIN_TRAPDOOR ->
                            MovingInteractionBehaviour.REGISTRY.register(block, new TrapdoorMovingInteraction());
                    default -> {
                    }
                }
            }
        }

        registered = true;
    }
}
