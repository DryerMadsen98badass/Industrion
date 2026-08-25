package net.mads.industron.transport.color;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.pipes.EncasedPipeBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;

public abstract class AbstractColoredEncasedFluidPipeBlock extends EncasedPipeBlock implements ColoredPipeBlock {
    private final PipeColorDefinitions.PipeFamily family;
    private final DyeColor color;

    protected AbstractColoredEncasedFluidPipeBlock(
            PipeColorDefinitions.PipeFamily family,
            DyeColor color,
            BlockBehaviour.Properties properties
    ) {
        super(properties, AllBlocks.COPPER_CASING::get);
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

    protected abstract FluidPipeBlock regularPipe();

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(state));

        FluidPipeBlock pipe = regularPipe();
        BlockState equivalentPipe = transferSixWayProperties(state, pipe.defaultBlockState());

        Direction firstFound = Direction.UP;
        for (Direction direction : Iterate.directions) {
            if (state.getValue(FACING_TO_PROPERTY_MAP.get(direction))) {
                firstFound = direction;
                break;
            }
        }

        FluidTransportBehaviour.cacheFlows(level, pos);
        level.setBlockAndUpdate(pos, pipe.updateBlockState(equivalentPipe, firstFound, null, level, pos));
        FluidTransportBehaviour.loadFlows(level, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        return new ItemStack(ColoredFluidPipeRegistrations.items(family, color).pipe().get());
    }

    @Override
    public ItemRequirement getRequiredItems(BlockState state, BlockEntity blockEntity) {
        return ItemRequirement.of(regularPipe().defaultBlockState(), blockEntity);
    }
}
