package net.mads.industron.transport.color;

import net.mads.industron.transport.FluidTransportGlassPipeBlock;
import net.mads.industron.transport.FluidTransportPipeBlock;
import net.mads.industron.transport.FluidTransportTier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ColoredFluidTransportGlassPipeBlock extends FluidTransportGlassPipeBlock implements ColoredPipeBlock {
    private final PipeColorDefinitions.PipeFamily family;
    private final DyeColor color;

    public ColoredFluidTransportGlassPipeBlock(
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
    protected FluidTransportPipeBlock regularPipe() {
        return (FluidTransportPipeBlock) ColoredFluidPipeRegistrations.blocks(family, color).pipe().get();
    }
}
