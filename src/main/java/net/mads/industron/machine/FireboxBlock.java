package net.mads.industron.machine;

import com.mojang.serialization.MapCodec;
import net.mads.industron.block.ActiveBlockDefinition;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.Nullable;

public class FireboxBlock extends Block {

    public static final MapCodec<FireboxBlock> CODEC = simpleCodec(FireboxBlock::new);
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final IntegerProperty OVERLAY_FRAME = IntegerProperty.create("overlay_frame", 0, 9);

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
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false).setValue(OVERLAY_FRAME, 0));
    }

    @Nullable
    public ActiveBlockDefinition definition() { return definition; }

    public int activeFrameCount() { return definition == null ? 1 : definition.activeFrameCount(); }

    @Override
    protected MapCodec<? extends Block> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(ACTIVE, OVERLAY_FRAME); }
}
