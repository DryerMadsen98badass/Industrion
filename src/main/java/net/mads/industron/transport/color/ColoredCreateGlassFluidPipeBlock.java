package net.mads.industron.transport.color;

import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.GlassFluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.StraightPipeBlockEntity;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;

public class ColoredCreateGlassFluidPipeBlock extends GlassFluidPipeBlock implements ColoredPipeBlock {
    private final PipeColorDefinitions.PipeFamily family;
    private final DyeColor color;

    public ColoredCreateGlassFluidPipeBlock(
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
    public BlockState toRegularPipe(LevelAccessor level, BlockPos pos, BlockState state) {
        ColoredCreateFluidPipeBlock pipe = (ColoredCreateFluidPipeBlock) ColoredFluidPipeRegistrations
                .blocks(family, color)
                .pipe()
                .get();
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
        return ItemRequirement.of(
                ColoredFluidPipeRegistrations.blocks(family, color).pipe().get().defaultBlockState(),
                blockEntity
        );
    }

    @Override
    public ItemStack getCloneItemStack(
            BlockState state,
            HitResult target,
            LevelReader level,
            BlockPos pos,
            Player player
    ) {
        return new ItemStack(ColoredFluidPipeRegistrations.items(family, color).pipe().get());
    }

    @Override
    public BlockEntityType<? extends StraightPipeBlockEntity> getBlockEntityType() {
        return ColoredFluidPipeRegistrations.createGlassPipeBlockEntity().get();
    }
}
