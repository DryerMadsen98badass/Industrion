package net.mads.industron.transport.color;

import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlockEntity;
import com.simibubi.create.content.fluids.pipes.GlassFluidPipeBlock;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public class ColoredCreateFluidPipeBlock extends FluidPipeBlock implements ColoredPipeBlock {
    private final PipeColorDefinitions.PipeFamily family;
    private final DyeColor color;

    public ColoredCreateFluidPipeBlock(
            PipeColorDefinitions.PipeFamily family,
            DyeColor color,
            BlockBehaviour.Properties properties
    ) {
        super(properties);
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
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        if (tryRemoveBracket(context)) {
            return InteractionResult.SUCCESS;
        }

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();
        Axis axis = FluidPropagator.getStraightPipeAxis(state);

        if (axis == null) {
            Vec3 clickLocation = context.getClickLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
            double closest = Double.MAX_VALUE;
            Direction closestDirection = Direction.UP;

            for (Direction direction : Iterate.directions) {
                if (clickedFace.getAxis() == direction.getAxis()) {
                    continue;
                }

                double distance = Vec3.atCenterOf(direction.getNormal()).distanceToSqr(clickLocation);
                if (distance < closest) {
                    closest = distance;
                    closestDirection = direction;
                }
            }
            axis = closestDirection.getAxis();
        }

        if (clickedFace.getAxis() == axis) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, pipe -> pipe.getBehaviour(FluidTransportBehaviour.TYPE).interfaces.values()
                    .stream()
                    .filter(connection -> connection != null && connection.hasFlow())
                    .findAny()
                    .ifPresent(ignored -> AllAdvancements.GLASS_PIPE.awardTo(context.getPlayer())));

            FluidTransportBehaviour.cacheFlows(level, pos);
            ColoredCreateGlassFluidPipeBlock glassPipe = (ColoredCreateGlassFluidPipeBlock) ColoredFluidPipeRegistrations
                    .blocks(family, color)
                    .glassPipe()
                    .get();
            level.setBlockAndUpdate(
                    pos,
                    glassPipe.defaultBlockState()
                            .setValue(GlassFluidPipeBlock.AXIS, axis)
                            .setValue(
                                    BlockStateProperties.WATERLOGGED,
                                    state.getValue(BlockStateProperties.WATERLOGGED)
                            )
            );
            FluidTransportBehaviour.loadFlows(level, pos);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntityType<? extends FluidPipeBlockEntity> getBlockEntityType() {
        return ColoredFluidPipeRegistrations.createPipeBlockEntity().get();
    }
}
