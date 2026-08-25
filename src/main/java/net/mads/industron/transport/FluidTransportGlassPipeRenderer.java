package net.mads.industron.transport;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.PipeConnection.Flow;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.fluid.FluidRenderer;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.data.Iterate;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.fluids.FluidStack;

public class FluidTransportGlassPipeRenderer extends SafeBlockEntityRenderer<FluidTransportGlassPipeBlockEntity> {
    public FluidTransportGlassPipeRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(
            FluidTransportGlassPipeBlockEntity blockEntity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int light,
            int overlay
    ) {
        FluidTransportBehaviour pipe = blockEntity.getBehaviour(FluidTransportBehaviour.TYPE);
        if (pipe == null) {
            return;
        }

        for (Direction side : Iterate.directions) {
            Flow flow = pipe.getFlow(side);
            if (flow == null) {
                continue;
            }

            FluidStack fluidStack = flow.fluid;
            if (fluidStack.isEmpty()) {
                continue;
            }

            LerpedFloat progress = flow.progress;
            if (progress == null) {
                continue;
            }

            float value = progress.getValue(partialTicks);
            boolean inbound = flow.inbound;
            if (value == 1.0F) {
                if (inbound) {
                    Flow opposite = pipe.getFlow(side.getOpposite());
                    if (opposite == null) {
                        value -= 1.0E-6F;
                    }
                } else {
                    FluidTransportBehaviour adjacent = BlockEntityBehaviour.get(
                            blockEntity.getLevel(),
                            blockEntity.getBlockPos().relative(side),
                            FluidTransportBehaviour.TYPE
                    );
                    if (adjacent == null) {
                        value -= 1.0E-6F;
                    } else {
                        Flow other = adjacent.getFlow(side.getOpposite());
                        if (other == null || !other.inbound && !other.complete) {
                            value -= 1.0E-6F;
                        }
                    }
                }
            }

            FluidRenderer.renderFluidStream(
                    fluidStack,
                    side,
                    3.0F / 16.0F,
                    value,
                    inbound,
                    buffer,
                    poseStack,
                    light
            );
        }
    }
}
