package net.mads.industron.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.mads.industron.recipe.recipetypes.assembly.workbench.AssemblyWorkbenchBlockEntity;
import net.mads.industron.tool.MaterialEquipment;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Renders the workpiece flat on the workbench with one generated-item pixel of depth. */
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

        ItemStack displayedStack = workbench.displayedStack();
        BakedModel model = itemRenderer.getModel(displayedStack, workbench.getLevel(), null, 0);

        /*
         * Normal generated item models already have the standard 1/16-model-depth extrusion.
         * Full 3D/block models must therefore be flattened to the same relative depth while they
         * lie on the workbench. A MaterialEquipment is a builtin/entity model externally, but its
         * BEWLR renders ordinary generated part models internally. Treating that outer model as a
         * full 3D model flattened the generated parts a second time, making finished tools much
         * thinner than one pixel on the bench.
         */
        boolean composedTool = displayedStack.getItem() instanceof MaterialEquipment;
        float depthScale = !composedTool && model.isGui3d() ? 1.0F / 16.0F : 1.0F;

        /*
         * A block-entity renderer receives light sampled at the block entity's own position.
         * Because the workbench occupies that position, the value can be much darker than the
         * exposed top surface. Sample the real world light one block above instead. This does not
         * make the item emissive or increase the world's light level; it only lights it from the
         * position where it is visually rendered.
         */
        int workSurfaceLight = packedLight;
        if (workbench.getLevel() != null) {
            workSurfaceLight = LevelRenderer.getLightColor(
                    workbench.getLevel(),
                    workbench.getBlockPos().above()
            );
        }

        poseStack.pushPose();
        poseStack.translate(0.5D, 1.03125D, 0.5D);
        // Face the generated item's front surface upward instead of into the workbench.
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.scale(0.75F, 0.75F, depthScale);
        itemRenderer.renderStatic(
                displayedStack,
                ItemDisplayContext.FIXED,
                workSurfaceLight,
                packedOverlay,
                poseStack,
                buffer,
                workbench.getLevel(),
                0
        );
        poseStack.popPose();
    }
}
