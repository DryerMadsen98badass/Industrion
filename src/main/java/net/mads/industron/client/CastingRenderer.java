package net.mads.industron.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.mads.industron.machine.foundry.CastingBlockEntity;
import net.mads.industron.machine.foundry.CastingBlock;
import net.mads.industron.material.ClayMaterialRules;
import net.mads.industron.material.MaterialLookup;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.registry.BlockRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/** Only moving contents are rendered here; the ceramic body is a baked block/item model. */
public final class CastingRenderer implements BlockEntityRenderer<CastingBlockEntity> {
    private static final float PIXEL = 1F / 16F;
    private static final float MOLD_BOTTOM = 10 * PIXEL;
    private static final float MOLD_TOP = 11 * PIXEL;
    private static final float CLAY_BOTTOM = 10 * PIXEL;
    private static final float CLAY_MAX_HEIGHT = 2 * PIXEL;
    private static final float CLAY_INSET = 1.01F * PIXEL;
    private static final float CLAY_WIDTH = 13.98F * PIXEL;
    // Keep a representable gap to avoid coplanar flickering at full capacity.
    private static final float FLUID_TOP = MOLD_TOP - .00001F;

    public CastingRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public boolean shouldRenderOffScreen(CastingBlockEntity caster) { return caster.block().faucet(); }
    @Override public void render(CastingBlockEntity caster, float partialTick, PoseStack poses,
                                 MultiBufferSource buffers, int light, int overlay) {
        if (caster.block().faucet()) {
            poses.pushPose();
            poses.translate(.5, 0, .5);
            poses.mulPose(Axis.YP.rotationDegrees(180 - caster.getBlockState().getValue(CastingBlock.FACING).toYRot()));
            poses.translate(-.5, 0, -.5);
            if (!caster.pouring().isEmpty()) stream(caster.pouring(), poses, buffers);
            poses.popPose();
            return;
        }
        renderItem(caster, caster.mold(), poses, buffers, light, overlay, MOLD_BOTTOM + PIXEL / 2);
        if (!caster.formingClay().isEmpty()) clayFill(caster.formingClay(), poses, buffers, light, overlay);
        if (!caster.fluid().isEmpty()) surface(caster.fluid(), poses, buffers, MOLD_BOTTOM + (FLUID_TOP - MOLD_BOTTOM) * Math.max(0F, Math.min(1F, caster.fillFraction())));
        renderItem(caster, caster.output(), poses, buffers, LightTexture.FULL_BRIGHT, overlay, MOLD_TOP + .0001F);
    }
    private static void renderItem(CastingBlockEntity caster, ItemStack stack, PoseStack poses, MultiBufferSource buffers,
                                   int light, int overlay, float height) {
        if (stack.isEmpty()) return;
        poses.pushPose();
        poses.translate(.5, height, .5);
        poses.mulPose(Axis.XP.rotationDegrees(-90));
        // NONE avoids the inherited FIXED scale. Generated item geometry is
        // one pixel thick, so leave its depth at 1 while fitting the 14px bed.
        poses.scale(14 * PIXEL, 14 * PIXEL, 1F);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.NONE,
                light, overlay, poses, buffers, caster.getLevel(), 0);
        poses.popPose();
    }

    private static void clayFill(ItemStack clay, PoseStack poses, MultiBufferSource buffers, int light, int overlay) {
        var ceramic = net.mads.industron.machine.foundry.CastingRegistry.moldMaterial(clay);
        if (ceramic == null) return;

        Block clayBlock;
        if (clay.is(net.minecraft.world.item.Items.NETHERRACK)) {
            clayBlock = Blocks.NETHERRACK;
        } else if (ceramic.hasExistingPart(MaterialPart.CLAY_BLOCK)) {
            clayBlock = BuiltInRegistries.BLOCK.getOptional(ceramic.existingPart(MaterialPart.CLAY_BLOCK)).orElse(null);
        } else {
            var holder = BlockRegistry.getMaterialBlock(ceramic, MaterialPart.CLAY_BLOCK);
            clayBlock = holder == null ? null : holder.get();
        }
        if (clayBlock == null) return;

        float fraction = Math.max(0F, Math.min(1F,
                (float) clay.getCount() / Math.max(1, ClayMaterialRules.MOLD_CLAY_COUNT)));
        if (fraction <= 0F) return;

        BlockState state = clayBlock.defaultBlockState();
        poses.pushPose();
        poses.translate(CLAY_INSET, CLAY_BOTTOM, CLAY_INSET);
        poses.scale(CLAY_WIDTH, CLAY_MAX_HEIGHT * fraction, CLAY_WIDTH);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, poses, buffers, light, overlay);
        poses.popPose();
    }

    private static TextureAtlasSprite sprite(FluidStack fluid) {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(IClientFluidTypeExtensions.of(fluid.getFluid()).getStillTexture(fluid));
    }
    private static void surface(FluidStack fluid, PoseStack poses, MultiBufferSource buffers, float y) {
        TextureAtlasSprite sprite = sprite(fluid);
        int color = IClientFluidTypeExtensions.of(fluid.getFluid()).getTintColor(fluid) | 0xFF000000;
        VertexConsumer vertices = buffers.getBuffer(RenderType.translucent());
        vertex(vertices, poses.last(), (2 * PIXEL), y, (2 * PIXEL), sprite.getU0(), sprite.getV0(), color);
        vertex(vertices, poses.last(), (2 * PIXEL), y, (14 * PIXEL), sprite.getU0(), sprite.getV1(), color);
        vertex(vertices, poses.last(), (14 * PIXEL), y, (14 * PIXEL), sprite.getU1(), sprite.getV1(), color);
        vertex(vertices, poses.last(), (14 * PIXEL), y, (2 * PIXEL), sprite.getU1(), sprite.getV0(), color);
    }
    private static void stream(FluidStack fluid, PoseStack poses, MultiBufferSource buffers) {
        TextureAtlasSprite sprite = sprite(fluid);
        int color = IClientFluidTypeExtensions.of(fluid.getFluid()).getTintColor(fluid) | 0xFF000000;
        VertexConsumer vertices = buffers.getBuffer(RenderType.translucent());
        // North-facing model: the open channel runs from the backing at z=14
        // to the two-pixel outlet at z=7..9. Rotate together with the block.
        fluidBox(vertices, poses.last(), sprite, color,
                7 * PIXEL, 7.01F * PIXEL, 8 * PIXEL,
                9 * PIXEL, 8 * PIXEL, 14.01F * PIXEL);
        fluidBox(vertices, poses.last(), sprite, color,
                7 * PIXEL, MOLD_BOTTOM - 1, 7 * PIXEL,
                9 * PIXEL, 8 * PIXEL, 9 * PIXEL);
    }
    private static void fluidBox(VertexConsumer out, PoseStack.Pose pose, TextureAtlasSprite sprite,
                                 int color, float x0, float y0, float z0, float x1, float y1, float z1) {
        float[][][] faces = {
                {{x0,y1,z0},{x0,y1,z1},{x1,y1,z1},{x1,y1,z0}},
                {{x0,y0,z0},{x1,y0,z0},{x1,y0,z1},{x0,y0,z1}},
                {{x0,y0,z0},{x0,y1,z0},{x1,y1,z0},{x1,y0,z0}},
                {{x1,y0,z1},{x1,y1,z1},{x0,y1,z1},{x0,y0,z1}},
                {{x0,y0,z1},{x0,y1,z1},{x0,y1,z0},{x0,y0,z0}},
                {{x1,y0,z0},{x1,y1,z0},{x1,y1,z1},{x1,y0,z1}}
        };
        for (float[][] face : faces) {
            for (int i = 0; i < 4; i++) {
                float[] p = face[i];
                vertex(out, pose, p[0], p[1], p[2],
                        i < 2 ? sprite.getU0() : sprite.getU1(),
                        i == 0 || i == 3 ? sprite.getV1() : sprite.getV0(), color);
            }
        }
    }
    private static void vertex(VertexConsumer out, PoseStack.Pose pose, float x, float y, float z, float u, float v, int color) {
        out.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0, 1, 0);
    }
}
