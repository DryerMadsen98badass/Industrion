package net.mads.industron.transport;

import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class FluidTransportTankBlock extends FluidTankBlock implements TieredFluidTank {
    private final FluidTransportTier tier;

    public FluidTransportTankBlock(FluidTransportTier tier, BlockBehaviour.Properties properties) {
        super(properties, false);
        this.tier = tier;
    }

    @Override
    public FluidTransportTier transportTier() {
        return tier;
    }

    @Override
    public BlockEntityType<? extends FluidTransportTankBlockEntity> getBlockEntityType() {
        return FluidTransportRegistrations.blockEntities(tier).tank().get();
    }
}
