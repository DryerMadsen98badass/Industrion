package net.mads.industron.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.createmod.catnip.render.CachedBuffers;
import net.mads.industron.IndustronPartialModels;
import net.mads.industron.machine.SingleBlockMachineBlock;
import net.mads.industron.machine.SingleBlockMachineBlockEntity;
import net.mads.industron.material.MaterialItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.ArrayList;
import java.util.List;

/** Renders primitive-machine contents: rack items, mold fill, and Basin items and fluids. */
public final class PrimitiveSingleBlockRenderer implements BlockEntityRenderer<SingleBlockMachineBlockEntity> {
    private final ItemRenderer itemRenderer;

    public PrimitiveSingleBlockRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(SingleBlockMachineBlockEntity machine, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (!(machine.getBlockState().getBlock() instanceof SingleBlockMachineBlock block) || block.instance() == null) return;
        String id = block.instance().definition().id();
        if (id.endsWith("_drying_rack")) renderRack(machine, poseStack, buffer, packedLight, packedOverlay);
        if (id.endsWith("_brick_mold")) renderMold(machine, poseStack, buffer, packedLight, packedOverlay);
        if (id.endsWith("_basin")) renderBasin(machine, poseStack, buffer, packedLight, packedOverlay);
    }

    private void renderRack(SingleBlockMachineBlockEntity machine, PoseStack poses, MultiBufferSource buffer, int light, int overlay) {
        double[][] positions = {{.29, .29}, {.71, .29}, {.29, .71}, {.71, .71}};
        for (int slot = 0; slot < 4; slot++) {
            ItemStack stack = machine.inputItems().getStackInSlot(slot);
            if (stack.isEmpty()) stack = machine.outputItems().getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            renderFlat(stack, machine, poses, buffer, light, overlay, positions[slot][0], 14.0 / 16.0 + 1.0 / 32.0, positions[slot][1], .36F);
        }
    }

    private void renderBasin(SingleBlockMachineBlockEntity machine, PoseStack poses,
                             MultiBufferSource buffer, int light, int overlay) {
        double[][] positions = {
                {.28, .28}, {.50, .28}, {.72, .28},
                {.28, .50}, {.50, .50}, {.72, .50},
                {.28, .72}, {.50, .72}, {.72, .72}
        };
        double itemY = renderBasinFluids(machine, poses, buffer, light);
        boolean renderedOutput = false;
        for (int slot = 0; slot < machine.itemOutputSlotCount(); slot++) {
            ItemStack output = machine.outputItems().getStackInSlot(slot);
            if (output.isEmpty()) continue;
            renderFlat(output, machine, poses, buffer, light, overlay, .5, itemY, .5, .42F);
            renderedOutput = true;
            break;
        }
        if (renderedOutput) return;
        for (int slot = 0; slot < Math.min(positions.length, machine.itemInputSlotCount()); slot++) {
            ItemStack input = machine.inputItems().getStackInSlot(slot);
            if (input.isEmpty()) continue;
            renderFlat(input, machine, poses, buffer, light, overlay,
                    positions[slot][0], itemY, positions[slot][1], .20F);
        }
    }

    /** Renders up to six independent tank surfaces and returns the safe item-render height. */
    private double renderBasinFluids(SingleBlockMachineBlockEntity machine, PoseStack poses,
                                     MultiBufferSource buffer, int light) {
        List<FluidTank> visible = new ArrayList<>();
        for (FluidTank tank : machine.inputFluidTanks()) if (!tank.isEmpty()) visible.add(tank);
        for (FluidTank tank : machine.outputFluidTanks()) if (!tank.isEmpty()) visible.add(tank);

        double highestSurface = 3.0D / 16.0D;
        int rendered = Math.min(6, visible.size());
        for (int index = 0; index < rendered; index++) {
            FluidTank tank = visible.get(index);
            double fullness = Math.min(1.0D, tank.getFluidAmount() / (double) Math.max(1, tank.getCapacity()));
            double y = (2.0D + fullness * 7.0D) / 16.0D;
            double minX = 3.0D / 16.0D + index * (10.0D / 16.0D / rendered);
            double maxX = 3.0D / 16.0D + (index + 1) * (10.0D / 16.0D / rendered);
            double minZ = 3.0D / 16.0D;
            double maxZ = 13.0D / 16.0D;
            renderFluidSurface(tank.getFluid(), poses, buffer, light, minX, maxX, y, minZ, maxZ);
            highestSurface = Math.max(highestSurface, y + 1.0D / 32.0D);
        }
        return highestSurface;
    }

    private static void renderFluidSurface(FluidStack fluid, PoseStack poses, MultiBufferSource buffer,
                                           int light, double minX, double maxX, double y,
                                           double minZ, double maxZ) {
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
        ResourceLocation texture = extensions.getStillTexture(fluid);
        if (texture == null) return;
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(texture);
        int tint = extensions.getTintColor(fluid);
        int alpha = tint >>> 24 & 0xFF;
        if (alpha == 0) alpha = 255;
        int red = tint >> 16 & 0xFF;
        int green = tint >> 8 & 0xFF;
        int blue = tint & 0xFF;
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
        PoseStack.Pose pose = poses.last();
        fluidVertex(consumer, pose, minX, y, maxZ, sprite.getU0(), sprite.getV1(), red, green, blue, alpha, light);
        fluidVertex(consumer, pose, maxX, y, maxZ, sprite.getU1(), sprite.getV1(), red, green, blue, alpha, light);
        fluidVertex(consumer, pose, maxX, y, minZ, sprite.getU1(), sprite.getV0(), red, green, blue, alpha, light);
        fluidVertex(consumer, pose, minX, y, minZ, sprite.getU0(), sprite.getV0(), red, green, blue, alpha, light);
    }

    private static void fluidVertex(VertexConsumer consumer, PoseStack.Pose pose,
                                    double x, double y, double z, float u, float v,
                                    int red, int green, int blue, int alpha, int light) {
        consumer.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0, 1, 0);
    }

    private void renderMold(SingleBlockMachineBlockEntity machine, PoseStack poses, MultiBufferSource buffer, int light, int overlay) {
        ItemStack output = machine.outputItems().getStackInSlot(0);
        ItemStack stack = output.isEmpty() ? machine.inputItems().getStackInSlot(0) : output;
        if (!(stack.getItem() instanceof MaterialItem materialItem)) return;
        int count = output.isEmpty() ? Math.min(4, stack.getCount()) : 4;
        if (count <= 0) return;
        // The insert occupies the cavity from Y=2 to Y=7 and rises by one quarter per clay item.
        float fill = count / 4.0F;
        Direction facing = machine.getBlockState().getValue(SingleBlockMachineBlock.FACING);
        float rotation = switch (facing) {
            case NORTH -> 0;
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> 0;
        };

        poses.pushPose();
        poses.translate(.5, 0, .5);
        poses.mulPose(Axis.YP.rotationDegrees(rotation));
        poses.translate(-.5, 0, -.5);
        // Scale around the cavity floor. Doing this on the pose keeps the model's bottom fixed.
        poses.translate(0, 2.0F / 16.0F, 0);
        poses.scale(1, fill, 1);
        poses.translate(0, -2.0F / 16.0F, 0);
        CachedBuffers.partial(IndustronPartialModels.BRICK_MOLD_INSIDE, machine.getBlockState())
                .color(materialItem.material().color())
                .light(light)
                .renderInto(poses, buffer.getBuffer(RenderType.solid()));
        poses.popPose();
    }

    private void renderFlat(ItemStack stack, SingleBlockMachineBlockEntity machine, PoseStack poses,
                            MultiBufferSource buffer, int light, int overlay,
                            double x, double y, double z, float scale) {
        BakedModel model = itemRenderer.getModel(stack, machine.getLevel(), null, 0);
        float onePixelDepth = model.isGui3d() ? 1.0F / 16.0F : 1.0F;
        poses.pushPose();
        poses.translate(x, y, z);
        poses.mulPose(Axis.XP.rotationDegrees(90));
        poses.scale(scale, scale, onePixelDepth);
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, poses, buffer, machine.getLevel(), 0);
        poses.popPose();
    }
}
