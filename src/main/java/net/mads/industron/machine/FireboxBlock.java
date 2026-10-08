package net.mads.industron.machine;

import com.mojang.serialization.MapCodec;
import net.mads.industron.block.ActiveBlockDefinition;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.Nullable;

public class FireboxBlock extends Block {

    public static final MapCodec<FireboxBlock> CODEC = simpleCodec(FireboxBlock::new);
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private final ActiveBlockDefinition definition;

    public FireboxBlock() {
        this(BlockBehaviour.Properties.of().requiresCorrectToolForDrops().strength(5.0F, 6.0F).lightLevel(state -> state.getValue(ACTIVE) ? 12 : 0).sound(SoundType.STONE), null);
    }

    public FireboxBlock(BlockBehaviour.Properties properties) {
        this(properties, null);
    }

    public FireboxBlock(BlockBehaviour.Properties properties, @Nullable ActiveBlockDefinition definition) {
        super(properties);
        this.definition = definition;
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ACTIVE, false));
    }

    @Nullable
    public ActiveBlockDefinition definition() { return definition; }

    public int activeFrameCount() { return definition == null ? 1 : definition.activeFrameCount(); }

    @Override
    protected MapCodec<? extends Block> codec() { return CODEC; }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE);
    }
}
