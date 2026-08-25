package net.mads.industron.mixin;

import com.simibubi.create.content.fluids.pipes.GlassFluidPipeBlock;
import net.mads.industron.transport.color.PipeColorManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GlassFluidPipeBlock.class)
public abstract class GlassFluidPipeBlockMixin {
    @Unique
    private static final ThreadLocal<DyeColor> createExpansion$wrenchColor = new ThreadLocal<>();

    @Inject(method = "onWrenched", at = @At("HEAD"))
    private void createExpansion$captureGlassPipeColorBeforeWrench(
            BlockState state,
            UseOnContext context,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            createExpansion$wrenchColor.remove();
            return;
        }

        DyeColor color = PipeColorManager.getColor(level, context.getClickedPos());
        if (color == null) {
            createExpansion$wrenchColor.remove();
        } else {
            createExpansion$wrenchColor.set(color);
        }
    }

    @Inject(method = "onWrenched", at = @At("RETURN"))
    private void createExpansion$restoreGlassPipeColorAfterWrench(
            BlockState state,
            UseOnContext context,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        DyeColor color = createExpansion$wrenchColor.get();
        createExpansion$wrenchColor.remove();
        if (color == null || context.getLevel().isClientSide) {
            return;
        }

        BlockPos pos = context.getClickedPos();
        if (PipeColorManager.isColorablePipe(context.getLevel().getBlockState(pos))) {
            PipeColorManager.setColor(context.getLevel(), pos, color);
        }
    }
}
