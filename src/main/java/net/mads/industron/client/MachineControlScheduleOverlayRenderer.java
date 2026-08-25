package net.mads.industron.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mads.industron.Industron;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

public final class MachineControlScheduleOverlayRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Industron.MOD_ID,
            "textures/block/machines/overlay/redstone/machine_controll_schedule_overlay.png"
    );
    private static final float OFFSET = 0.004F;

    private MachineControlScheduleOverlayRenderer() {
    }

    public static void render(
            Set<Direction> sides,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        if (sides.isEmpty()) {
            return;
        }

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        for (Direction side : sides) {
            renderFace(pose, consumer, side, packedLight);
        }
    }

    private static void renderFace(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            Direction side,
            int packedLight
    ) {
        switch (side) {
            case DOWN -> {
                float y = -OFFSET;
                vertex(pose, consumer, 0, y, 1, 0, 1, 0, -1, 0, packedLight);
                vertex(pose, consumer, 1, y, 1, 1, 1, 0, -1, 0, packedLight);
                vertex(pose, consumer, 1, y, 0, 1, 0, 0, -1, 0, packedLight);
                vertex(pose, consumer, 0, y, 0, 0, 0, 0, -1, 0, packedLight);
            }
            case UP -> {
                float y = 1 + OFFSET;
                vertex(pose, consumer, 0, y, 0, 0, 1, 0, 1, 0, packedLight);
                vertex(pose, consumer, 1, y, 0, 1, 1, 0, 1, 0, packedLight);
                vertex(pose, consumer, 1, y, 1, 1, 0, 0, 1, 0, packedLight);
                vertex(pose, consumer, 0, y, 1, 0, 0, 0, 1, 0, packedLight);
            }
            case NORTH -> {
                float z = -OFFSET;
                vertex(pose, consumer, 1, 0, z, 0, 1, 0, 0, -1, packedLight);
                vertex(pose, consumer, 0, 0, z, 1, 1, 0, 0, -1, packedLight);
                vertex(pose, consumer, 0, 1, z, 1, 0, 0, 0, -1, packedLight);
                vertex(pose, consumer, 1, 1, z, 0, 0, 0, 0, -1, packedLight);
            }
            case SOUTH -> {
                float z = 1 + OFFSET;
                vertex(pose, consumer, 0, 0, z, 0, 1, 0, 0, 1, packedLight);
                vertex(pose, consumer, 1, 0, z, 1, 1, 0, 0, 1, packedLight);
                vertex(pose, consumer, 1, 1, z, 1, 0, 0, 0, 1, packedLight);
                vertex(pose, consumer, 0, 1, z, 0, 0, 0, 0, 1, packedLight);
            }
            case WEST -> {
                float x = -OFFSET;
                vertex(pose, consumer, x, 0, 0, 0, 1, -1, 0, 0, packedLight);
                vertex(pose, consumer, x, 0, 1, 1, 1, -1, 0, 0, packedLight);
                vertex(pose, consumer, x, 1, 1, 1, 0, -1, 0, 0, packedLight);
                vertex(pose, consumer, x, 1, 0, 0, 0, -1, 0, 0, packedLight);
            }
            case EAST -> {
                float x = 1 + OFFSET;
                vertex(pose, consumer, x, 0, 1, 0, 1, 1, 0, 0, packedLight);
                vertex(pose, consumer, x, 0, 0, 1, 1, 1, 0, 0, packedLight);
                vertex(pose, consumer, x, 1, 0, 1, 0, 1, 0, 0, packedLight);
                vertex(pose, consumer, x, 1, 1, 0, 0, 1, 0, 0, packedLight);
            }
        }
    }

    private static void vertex(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float x,
            float y,
            float z,
            float u,
            float v,
            float normalX,
            float normalY,
            float normalZ,
            int packedLight
    ) {
        consumer.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(pose, normalX, normalY, normalZ);
    }
}
