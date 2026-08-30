package net.mads.industron.recipe.recipetypes.assembly.workbench;

import com.mojang.serialization.MapCodec;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Smithing-textured temporary host for manual item/block assembly. */
public final class AssemblyWorkbenchBlock extends BaseEntityBlock {
    public AssemblyWorkbenchBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(AssemblyWorkbenchBlock::new);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AssemblyWorkbenchBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston
    ) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof AssemblyWorkbenchBlockEntity workbench) {
            if (level instanceof ServerLevel serverLevel) {
                AssemblyRuntime.dropAndClearWorkbenchAssembly(serverLevel, pos, workbench);
                ItemStackDrop.drop(serverLevel, pos, workbench.takeDisplayedStack());
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private static final class ItemStackDrop {
        private static void drop(ServerLevel level, BlockPos pos, net.minecraft.world.item.ItemStack stack) {
            if (stack.isEmpty()) return;
            Containers.dropItemStack(
                    level,
                    pos.getX() + 0.5D,
                    pos.getY() + 1.05D,
                    pos.getZ() + 0.5D,
                    stack
            );
        }
    }
}
