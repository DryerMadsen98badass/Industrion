package net.mads.industron.transport;

import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class FluidTransportTankBlock extends FluidTankBlock implements TieredFluidTank {
    private final FluidTransportTier tier;

    public FluidTransportTankBlock(FluidTransportTier tier, BlockBehaviour.Properties properties) {
        super(properties, false);
        this.tier = tier;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())
                && !level.isClientSide()
                && level.getBlockEntity(pos) instanceof FluidTransportTankBlockEntity tank) {
            tank.discardStoredFluid();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
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
