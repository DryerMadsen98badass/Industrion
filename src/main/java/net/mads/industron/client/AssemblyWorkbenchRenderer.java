package net.mads.industron.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.mads.industron.recipe.recipetypes.assembly.workbench.AssemblyWorkbenchBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;

/** Renders the workpiece horizontally with the normal generated-item one-pixel depth. */
public final class AssemblyWorkbenchRenderer implements BlockEntityRenderer<AssemblyWorkbenchBlockEntity> {
    private final ItemRenderer itemRenderer;

    public AssemblyWorkbenchRenderer(BlockEntityRendererProvider.Context context) {
        itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(
            AssemblyWorkbenchBlockEntity workbench,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        if (!workbench.hasDisplayedStack()) return;

        BakedModel model = itemRenderer.getModel(workbench.displayedStack(), workbench.getLevel(), null, 0);
        float verticalScale = model.isGui3d() ? 1.0F / 16.0F : 1.0F;

        poseStack.pushPose();
        poseStack.translate(0.5D, 1.03125D, 0.5D);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.scale(0.75F, 0.75F, verticalScale);
        itemRenderer.renderStatic(
                workbench.displayedStack(),
                ItemDisplayContext.FIXED,
                packedLight,
                packedOverlay,
                poseStack,
                buffer,
                workbench.getLevel(),
                0
        );
        poseStack.popPose();
    }
}
