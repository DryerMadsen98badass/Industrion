package net.mads.industron.transport;

import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.GlassFluidPipeBlock;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;

public class FluidTransportGlassPipeBlock extends GlassFluidPipeBlock implements TieredFluidPipe {
    private final FluidTransportTier tier;

    public FluidTransportGlassPipeBlock(FluidTransportTier tier, BlockBehaviour.Properties properties) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public FluidTransportTier transportTier() {
        return tier;
    }

    @Override
    public BlockState toRegularPipe(LevelAccessor level, BlockPos pos, BlockState state) {
        FluidTransportPipeBlock pipe = regularPipe();
        Direction side = Direction.get(AxisDirection.POSITIVE, state.getValue(AXIS));
        return pipe.updateBlockState(
                pipe.defaultBlockState()
                        .setValue(FluidPipeBlock.PROPERTY_BY_DIRECTION.get(side), true)
                        .setValue(FluidPipeBlock.PROPERTY_BY_DIRECTION.get(side.getOpposite()), true),
                side,
                null,
                level,
                pos
        );
    }

    @Override
    public ItemRequirement getRequiredItems(BlockState state, BlockEntity blockEntity) {
        return ItemRequirement.of(regularPipe().defaultBlockState(), blockEntity);
    }

    @Override
    public ItemStack getCloneItemStack(
            BlockState state,
            HitResult target,
            LevelReader level,
            BlockPos pos,
            Player player
    ) {
        return new ItemStack(regularPipe());
    }

    protected FluidTransportPipeBlock regularPipe() {
        return FluidTransportRegistrations.blocks(tier).pipe().get();
    }

    @Override
    public BlockEntityType<? extends FluidTransportGlassPipeBlockEntity> getBlockEntityType() {
        return FluidTransportRegistrations.blockEntities(tier).glassPipe().get();
    }
}
