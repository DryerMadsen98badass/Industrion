package net.mads.industron.transport;

import com.simibubi.create.content.fluids.pipes.StraightPipeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class FluidTransportGlassPipeBlockEntity extends StraightPipeBlockEntity {
    public FluidTransportGlassPipeBlockEntity(BlockPos pos, BlockState state) {
        this(blockEntityType(state), pos, state);
    }

    public FluidTransportGlassPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private static BlockEntityType<?> blockEntityType(BlockState state) {
        if (state.getBlock() instanceof FluidTransportGlassPipeBlock glassPipe) {
            return glassPipe.getBlockEntityType();
        }
        throw new IllegalArgumentException(
                "Fluid transport glass pipe block entity received an invalid block state: " + state
        );
    }
}
