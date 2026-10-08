package net.mads.industron.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.catnip.theme.Color;
import net.mads.industron.IndustronPartialModels;
import net.mads.industron.kinetics.shaft.MaterialShaftBlockEntity;
import net.mads.industron.material.IndustrialMaterialShaftBlock;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** Vanilla/fallback renderer for material shafts when Flywheel visualization is unavailable. */
public final class MaterialShaftRenderer extends KineticBlockEntityRenderer<MaterialShaftBlockEntity> {
    public MaterialShaftRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(
            MaterialShaftBlockEntity be,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int light,
            int overlay
    ) {
        if (VisualizationManager.supportsVisualization(be.getLevel())) {
            return;
        }

        SuperByteBuffer shaft = CachedBuffers.partial(
                IndustronPartialModels.materialShaft(be.getBlockState().getBlock()),
                be.getBlockState()
        );
        standardKineticRotationTransform(shaft, be, light);

        Color base = be.getBlockState().getBlock() instanceof IndustrialMaterialShaftBlock materialShaft
                ? new Color(materialShaft.material().color(), false)
                : Color.WHITE;
        if (be.isOverStressed()) {
            base = base.copy().mixWith(Color.RED, 0.65f);
        }
        shaft.color(base);

        VertexConsumer consumer = buffer.getBuffer(RenderType.cutoutMipped());
        shaft.renderInto(poseStack, consumer);
    }
}
