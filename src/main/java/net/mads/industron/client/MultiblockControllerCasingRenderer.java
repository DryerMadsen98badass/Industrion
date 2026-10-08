package net.mads.industron.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockControllerBlock;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockControllerBlockEntity;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockModelSource;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/** Draws the dominant formed casing over a controller's fallback casing model. */
public final class MultiblockControllerCasingRenderer implements BlockEntityRenderer<MultiblockControllerBlockEntity> {
    // Controller overlays are generated slightly outside the base cube. Keep the dynamic casing
    // above the fallback cube, but below those functional overlays.
    private static final float CASING_MODEL_OFFSET = 0.00025F;

    public MultiblockControllerCasingRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            MultiblockControllerBlockEntity controller,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        if (!controller.isFormed()) {
            return;
        }

        BlockState state = controller.getBlockState();
        if (!(state.getBlock() instanceof MultiblockControllerBlock controllerBlock)
                || controllerBlock.definition().formedModelSource() != MultiblockModelSource.CASING) {
            return;
        }

        ResourceLocation casingModel = controller.formedCasingModel();
        if (casingModel == null) {
            return;
        }

        CasingModelOverlayRenderer.render(
                controller,
                casingModel,
                null,
                CASING_MODEL_OFFSET,
                poseStack,
                buffer,
                packedLight
        );
    }
}
