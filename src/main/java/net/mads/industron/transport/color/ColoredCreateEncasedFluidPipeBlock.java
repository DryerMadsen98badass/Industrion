package net.mads.industron.transport.color;

import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlockEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ColoredCreateEncasedFluidPipeBlock extends AbstractColoredEncasedFluidPipeBlock {
    public ColoredCreateEncasedFluidPipeBlock(
            PipeColorDefinitions.PipeFamily family,
            DyeColor color,
            BlockBehaviour.Properties properties
    ) {
        super(family, color, properties);
    }

    @Override
    protected FluidPipeBlock regularPipe() {
        return (FluidPipeBlock) ColoredFluidPipeRegistrations.blocks(pipeFamily(), pipeColor()).pipe().get();
    }

    @Override
    public BlockEntityType<? extends FluidPipeBlockEntity> getBlockEntityType() {
        return ColoredFluidPipeRegistrations.createPipeBlockEntity().get();
    }
}
