package net.mads.industron.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mads.industron.machine.foundry.FoundryBlockEntity;
import net.mads.industron.machine.foundry.FoundryPartBlock;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Draws the dominant real Foundry brick over the Aerum fallback casing when formed. */
public final class FoundryCasingRenderer implements BlockEntityRenderer<FoundryBlockEntity> {
    // The baked I/O/controller overlay is farther out than this casing offset, so it stays visible.
    private static final float CASING_MODEL_OFFSET = 0.00025F;

    public FoundryCasingRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            FoundryBlockEntity foundry,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        if (!foundry.isFormed() || !(foundry.getBlockState().getBlock() instanceof FoundryPartBlock)
                || foundry.getBlockState().getBlock() instanceof net.mads.industron.machine.foundry.FoundryDrainBlock) {
            return;
        }

        ResourceLocation casingModel = foundry.formedCasingModel();
        if (casingModel == null) {
            return;
        }

        // Full cube: unlike ordinary machine ports, the Foundry's entire casing (including the
        // front under transparent overlay pixels) must visually become the dominant brick.
        CasingModelOverlayRenderer.render(
                foundry,
                casingModel,
                null,
                CASING_MODEL_OFFSET,
                poseStack,
                buffer,
                packedLight
        );
    }
}
