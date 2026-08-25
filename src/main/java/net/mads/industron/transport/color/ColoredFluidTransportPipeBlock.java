package net.mads.industron.transport.color;

import net.mads.industron.transport.FluidTransportGlassPipeBlock;
import net.mads.industron.transport.FluidTransportPipeBlock;
import net.mads.industron.transport.FluidTransportTier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ColoredFluidTransportPipeBlock extends FluidTransportPipeBlock implements ColoredPipeBlock {
    private final PipeColorDefinitions.PipeFamily family;
    private final DyeColor color;

    public ColoredFluidTransportPipeBlock(
            PipeColorDefinitions.PipeFamily family,
            FluidTransportTier tier,
            DyeColor color,
            BlockBehaviour.Properties properties
    ) {
        super(tier, properties);
        this.family = family;
        this.color = color;
    }

    @Override
    public DyeColor pipeColor() {
        return color;
    }

    @Override
    public PipeColorDefinitions.PipeFamily pipeFamily() {
        return family;
    }

    @Override
    protected FluidTransportGlassPipeBlock glassPipe() {
        return (FluidTransportGlassPipeBlock) ColoredFluidPipeRegistrations.blocks(family, color).glassPipe().get();
    }
}
