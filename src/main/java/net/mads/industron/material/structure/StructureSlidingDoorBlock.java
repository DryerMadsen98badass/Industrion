package net.mads.industron.material.structure;

import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlock;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlockEntity;
import net.mads.industron.registry.BlockEntityRegistry;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;

/**
 * Industron-owned sliding door that uses Create's door behaviour and animation,
 * but points at Industron's block-entity type so generated doors are valid
 * block-entity hosts instead of being limited to Create's built-in doors.
 */
public final class StructureSlidingDoorBlock extends SlidingDoorBlock {
    private StructureSlidingDoorBlock(
            BlockBehaviour.Properties properties,
            BlockSetType blockSetType,
            boolean folds
    ) {
        super(properties, blockSetType, folds);
    }

    public static StructureSlidingDoorBlock fromCreateModel(
            BlockBehaviour.Properties properties,
            String sourceModel
    ) {
        return switch (sourceModel) {
            case "brass_door" -> new StructureSlidingDoorBlock(
                    properties,
                    SlidingDoorBlock.STONE_SET_TYPE.get(),
                    false
            );
            case "copper_door", "andesite_door" -> new StructureSlidingDoorBlock(
                    properties,
                    SlidingDoorBlock.STONE_SET_TYPE.get(),
                    true
            );
            case "framed_glass_door" -> new StructureSlidingDoorBlock(
                    properties,
                    SlidingDoorBlock.GLASS_SET_TYPE.get(),
                    false
            );
            case "train_door" -> new StructureSlidingDoorBlock(
                    properties,
                    SlidingDoorBlock.TRAIN_SET_TYPE.get(),
                    false
            );
            default -> throw new IllegalArgumentException(
                    "Unsupported Create sliding-door model family: " + sourceModel
            );
        };
    }

    @Override
    public BlockEntityType<? extends SlidingDoorBlockEntity> getBlockEntityType() {
        return BlockEntityRegistry.STRUCTURE_SLIDING_DOOR.get();
    }
}
