package net.mads.industron.transport.color;

import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlockEntity;
import net.mads.industron.transport.FluidTransportRegistrations;
import net.mads.industron.transport.FluidTransportTier;
import net.mads.industron.transport.TieredFluidPipe;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ColoredFluidTransportEncasedPipeBlock extends AbstractColoredEncasedFluidPipeBlock implements TieredFluidPipe {
    private final FluidTransportTier tier;

    public ColoredFluidTransportEncasedPipeBlock(
            PipeColorDefinitions.PipeFamily family,
            FluidTransportTier tier,
            DyeColor color,
            BlockBehaviour.Properties properties
    ) {
        super(family, color, properties);
        this.tier = tier;
    }

    @Override
    public FluidTransportTier transportTier() {
        return tier;
    }

    @Override
    protected FluidPipeBlock regularPipe() {
        return (FluidPipeBlock) ColoredFluidPipeRegistrations.blocks(pipeFamily(), pipeColor()).pipe().get();
    }

    @Override
    public BlockEntityType<? extends FluidPipeBlockEntity> getBlockEntityType() {
        return FluidTransportRegistrations.blockEntities(tier).pipe().get();
    }
}
