package net.mads.industron.mixin;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.mads.industron.transport.color.PipeColorHolder;
import net.mads.industron.transport.color.PipeColorManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SmartBlockEntity.class)
public abstract class SmartBlockEntityPipeColorMixin implements PipeColorHolder {
    @Unique private int createExpansion$pipeColor = -1;

    @Inject(method = "write", at = @At("TAIL"))
    private void createExpansion$writePipeColor(
            CompoundTag tag,
            HolderLookup.Provider registries,
            boolean clientPacket,
            CallbackInfo ci
    ) {
        if (createExpansion$pipeColor >= 0) {
            tag.putInt(PipeColorManager.COLOR_DATA_KEY, createExpansion$pipeColor);
        }
    }

    @Inject(method = "read", at = @At("TAIL"))
    private void createExpansion$readPipeColor(
            CompoundTag tag,
            HolderLookup.Provider registries,
            boolean clientPacket,
            CallbackInfo ci
    ) {
        createExpansion$pipeColor = tag.contains(PipeColorManager.COLOR_DATA_KEY)
                ? DyeColor.byId(tag.getInt(PipeColorManager.COLOR_DATA_KEY)).getId()
                : -1;
    }

    @Override
    public DyeColor createExpansion$getPipeColor() {
        return createExpansion$pipeColor < 0 ? null : DyeColor.byId(createExpansion$pipeColor);
    }

    @Override
    public void createExpansion$setPipeColor(DyeColor color) {
        createExpansion$pipeColor = color == null ? -1 : color.getId();
    }
}
