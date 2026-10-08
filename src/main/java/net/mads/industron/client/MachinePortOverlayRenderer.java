package net.mads.industron.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mads.industron.machine.MachinePortBlock;
import net.mads.industron.machine.MachinePortBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public class MachinePortOverlayRenderer implements BlockEntityRenderer<MachinePortBlockEntity> {
    private static final float CASING_MODEL_OFFSET = 0.002F;

    public MachinePortOverlayRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            MachinePortBlockEntity port,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        BlockState portState = port.getBlockState();
        if (!(portState.getBlock() instanceof MachinePortBlock)) {
            return;
        }

        ResourceLocation overlayModel = port.assembledOverlayModel();
        if (overlayModel != null && port.controllerPos() != null) {
            Direction front = portState.getValue(MachinePortBlock.FACING);
            CasingModelOverlayRenderer.render(
                    port,
                    overlayModel,
                    front,
                    CASING_MODEL_OFFSET,
                    poseStack,
                    buffer,
                    packedLight
            );
        }

        MachineControlScheduleOverlayRenderer.render(
                port.machineControlScheduleSides(),
                poseStack,
                buffer,
                packedLight
        );
    }
}
