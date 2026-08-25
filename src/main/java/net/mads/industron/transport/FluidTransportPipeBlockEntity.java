package net.mads.industron.transport;

import com.simibubi.create.content.fluids.pipes.FluidPipeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class FluidTransportPipeBlockEntity extends FluidPipeBlockEntity {
    public FluidTransportPipeBlockEntity(BlockPos pos, BlockState state) {
        this(blockEntityType(state), pos, state);
    }

    public FluidTransportPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    private static BlockEntityType<?> blockEntityType(BlockState state) {
        if (state.getBlock() instanceof TieredFluidPipe pipe) {
            return FluidTransportRegistrations.blockEntities(pipe.transportTier()).pipe().get();
        }
        throw new IllegalArgumentException("Fluid transport pipe block entity received an invalid block state: " + state);
    }
}
