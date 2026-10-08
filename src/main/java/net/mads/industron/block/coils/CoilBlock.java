package net.mads.industron.block.coils;

import net.mads.industron.block.loot.AssemblySalvageBlock;

import net.mads.industron.machine.MachineTierStats;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import java.util.List;

public class CoilBlock extends Block implements AssemblySalvageBlock {
    public static final MapCodec<CoilBlock> CODEC = simpleCodec(properties -> new CoilBlock(properties, CoilDefinitions.PLACEHOLDER));
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    private final CoilDefinition definition;

    public CoilBlock(CoilDefinition definition) {
        this(BlockBehaviour.Properties.of()
                .requiresCorrectToolForDrops()
                .strength(MachineTierStats.blockHardness(definition.tier()), MachineTierStats.blockResistance(definition.tier()))
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

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.literal("Maximum Temperature: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(definition.heat() + " °C").withStyle(ChatFormatting.GOLD)));
    }

    public CoilDefinition definition() {
        return definition;
    }
}
