package net.mads.industron.transport.color;

import com.simibubi.create.content.fluids.pipes.FluidPipeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class ColoredCreateFluidPipeBlockEntity extends FluidPipeBlockEntity {
    public ColoredCreateFluidPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ColoredFluidPipeRegistrations.createPipeBlockEntity().get(), pos, state);
    }
}
