package net.mads.industron.transport.color;

import com.simibubi.create.content.fluids.pipes.StraightPipeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class ColoredCreateGlassFluidPipeBlockEntity extends StraightPipeBlockEntity {
    public ColoredCreateGlassFluidPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ColoredFluidPipeRegistrations.createGlassPipeBlockEntity().get(), pos, state);
    }
}
