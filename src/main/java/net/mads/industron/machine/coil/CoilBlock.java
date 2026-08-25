package net.mads.industron.machine.coil;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class CoilBlock extends Block {
    public static final MapCodec<CoilBlock> CODEC = simpleCodec(properties -> new CoilBlock(properties, CoilDefinitions.PLACEHOLDER));
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    private final CoilDefinition definition;

    public CoilBlock(CoilDefinition definition) {
        this(BlockBehaviour.Properties.of()
                .requiresCorrectToolForDrops()
                .strength(5.0F, 6.0F)
                .lightLevel(state -> state.getValue(ACTIVE) ? 10 : 0)
                .sound(SoundType.METAL), definition);
    }

    public CoilBlock(BlockBehaviour.Properties properties, CoilDefinition definition) {
        super(properties);
        this.definition = definition;
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    public CoilDefinition definition() {
        return definition;
    }
}
