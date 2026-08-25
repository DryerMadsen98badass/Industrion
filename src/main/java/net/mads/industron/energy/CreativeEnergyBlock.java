package net.mads.industron.energy;

import com.mojang.serialization.MapCodec;
import net.mads.industron.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class CreativeEnergyBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 16, 16);

    private final boolean provider;

    public CreativeEnergyBlock(boolean provider) {
        super(BlockBehaviour.Properties.of()
                .strength(3.0F, 6.0F)
                .sound(SoundType.METAL));
        this.provider = provider;
    }

    public boolean provider() {
        return provider;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(properties -> new CreativeEnergyBlock(provider));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof CreativeEnergyBlockEntity energy) {
            if (!level.isClientSide()) {
                if (player.isShiftKeyDown()) {
                    energy.cycleTier();
                } else {
                    energy.cycleAmps();
                }
                player.displayClientMessage(Component.literal(energy.statusText()), true);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CreativeEnergyBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide() ? null : createTickerHelper(blockEntityType, BlockEntityRegistry.CREATIVE_ENERGY.get(), CreativeEnergyBlockEntity::tick);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
