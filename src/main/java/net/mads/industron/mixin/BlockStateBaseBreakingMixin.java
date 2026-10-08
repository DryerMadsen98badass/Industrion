package net.mads.industron.mixin;

import net.mads.industron.block.breaking.BlockBreakingCalculator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Routes only Industron-owned breaking cases through the custom calculator. */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseBreakingMixin {
    @Inject(method = "getDestroyProgress", at = @At("HEAD"), cancellable = true)
    private void industron$replaceDestroyProgress(
            Player player,
            BlockGetter level,
            BlockPos pos,
            CallbackInfoReturnable<Float> cir
    ) {
        BlockState state = (BlockState) (Object) this;
        if (!BlockBreakingCalculator.shouldOverride(player, state, level, pos)) {
            return;
        }
        cir.setReturnValue(BlockBreakingCalculator.destroyProgress(player, state, level, pos));
    }
}
