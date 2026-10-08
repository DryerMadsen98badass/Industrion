package net.mads.industron.client.kinetic;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.render.CachedBuffers;
import net.mads.industron.IndustronPartialModels;
import net.mads.industron.integration.create.kinetic.CEKineticRecipeHost;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;

/** Full fallback for mechanisms; actual workpiece items also render when Flywheel is enabled. */
public final class KineticMachineRenderer<T extends KineticBlockEntity & CEKineticRecipeHost> extends KineticBlockEntityRenderer<T> {
    public KineticMachineRenderer(BlockEntityRendererProvider.Context context) { super(context); }
    @Override protected void renderSafe(T be, float pt, PoseStack poses, MultiBufferSource buffers, int light, int overlay) {
        String id = KineticMachineMotion.id(be);
        var facing = KineticMachineMotion.facing(be);
        float[] delta = KineticMachineMotion.offset(be, pt);
        if (!VisualizationManager.supportsVisualization(be.getLevel())) {
            var consumer = buffers.getBuffer(RenderType.cutoutMipped());
            var rotor = CachedBuffers.partial(IndustronPartialModels.kineticPart(id, "rotor", facing), be.getBlockState());
            standardKineticRotationTransform(rotor, be, light).renderInto(poses, consumer);
            CachedBuffers.partial(IndustronPartialModels.kineticPart(id, "moving", facing), be.getBlockState())
                    .translate(delta[0], delta[1], delta[2]).light(light).renderInto(poses, consumer);
        }
        var item = be.ceProcessing().workpiece();
        if (item.isEmpty()) return;
        float p = be.ceProcessing().progress(pt);
        float angle = getAngleForBe(be, be.getBlockPos(), getRotationAxisOf(be));
        boolean spins = id.equals("lathe") || id.equals("winding_machine") || id.equals("mechanical_centrifuge");
        poses.pushPose();
        if (id.equals("mechanical_sifter")) poses.translate(delta[0], delta[1], delta[2]);
        poses.translate(.5, .5, .5);
        if (spins) poses.mulPose(switch (getRotationAxisOf(be)) {
            case X -> Axis.XP.rotation(angle); case Y -> Axis.YP.rotation(angle); case Z -> Axis.ZP.rotation(angle);
        });
        poses.mulPose(Axis.YP.rotationDegrees(-KineticMachineMotion.facingDegrees(facing)));
        float scale = .3F;
        switch (id) {
            case "lathe" -> scale = .5F;
            case "mechanical_centrifuge" -> { poses.translate(.18, .06, 0); scale = .18F; }
            case "mechanical_sifter" -> { poses.translate(0, .24, 0); poses.mulPose(Axis.XP.rotationDegrees(90)); scale = .4F; }
            case "pulverizer" -> { poses.translate(0, .38 - p * .18, 0); scale = .24F; }
            case "wire_drawing_machine" -> { poses.translate(-.08 + p * .12, .04, -.2); scale = .22F; }
            case "winding_machine" -> { poses.translate(0, 0, -.18); scale = .14F + .1F * p; }
            case "mechanical_bender" -> { poses.translate(0, .2, 0); poses.mulPose(Axis.XP.rotationDegrees(90)); scale = .3F; }
            case "magnetic_separator" -> { poses.translate(0, .38 - p * .25, -p * .25); scale = .23F; }
            default -> { }
        }
        poses.scale(scale, scale, scale);
        Minecraft.getInstance().getItemRenderer().renderStatic(item, ItemDisplayContext.FIXED, light, overlay, poses, buffers, be.getLevel(), be.getBlockPos().hashCode());
        poses.popPose();
    }
}
