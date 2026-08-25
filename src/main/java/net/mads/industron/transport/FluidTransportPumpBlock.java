package net.mads.industron.transport;

import com.simibubi.create.content.fluids.pump.PumpBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class FluidTransportPumpBlock extends PumpBlock implements TieredFluidPump {
    private final FluidTransportTier tier;

    public FluidTransportPumpBlock(FluidTransportTier tier, BlockBehaviour.Properties properties) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public FluidTransportTier transportTier() {
        return tier;
    }

    @Override
    public BlockEntityType<? extends FluidTransportPumpBlockEntity> getBlockEntityType() {
        return FluidTransportRegistrations.blockEntities(tier).pump().get();
    }
}
