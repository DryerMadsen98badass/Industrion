package net.mads.industron.transport;

import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class FluidTransportPumpBlockEntity extends PumpBlockEntity {
    public FluidTransportPumpBlockEntity(BlockPos pos, BlockState state) {
        this(blockEntityType(state), pos, state);
    }

    public FluidTransportPumpBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public FluidTransportTier transportTier() {
        if (getBlockState().getBlock() instanceof TieredFluidPump pump) {
            return pump.transportTier();
        }
        throw new IllegalStateException("Fluid transport pump block entity is attached to an invalid block");
    }

    @Override
    public float calculateStressApplied() {
        float impact = (float) transportTier().pumpStressImpact();
        lastStressApplied = impact;
        return impact;
    }

    private static BlockEntityType<?> blockEntityType(BlockState state) {
        if (state.getBlock() instanceof FluidTransportPumpBlock pump) {
            return pump.getBlockEntityType();
        }
        throw new IllegalArgumentException("Fluid transport pump block entity received an invalid block state: " + state);
    }
}
