package net.mads.industron.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mads.industron.Industron;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.recipe.chiseling.ChiselingGrid;
import net.mads.industron.recipe.chiseling.ChiselingRecipes;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyTools;
import net.mads.industron.recipe.recipetypes.assembly.ToolVariantDefinition;
import net.mads.industron.recipe.recipes.assembly.Tool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Renders a numbered 3x3 Chiseling target grid directly over the block face under the crosshair. */
@EventBusSubscriber(modid = Industron.MOD_ID, value = Dist.CLIENT)
public final class ChiselingGridRenderer {
    private static final double FACE_OFFSET = 0.0025D;
    private static final double DIGIT_HALF_WIDTH = 0.055D;
    private static final double DIGIT_HALF_HEIGHT = 0.095D;
    private static final double DIGIT_STROKE_SPACING = 0.0045D;
    private static final int DIGIT_STROKE_RADIUS = 2;

    private ChiselingGridRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.hitResult == null
                || minecraft.hitResult.getType() != HitResult.Type.BLOCK
                || !(minecraft.hitResult instanceof BlockHitResult hit)) {
            return;
        }

        ToolVariantDefinition hammer = AssemblyTools.find(Tool.HAMMER.type(), minecraft.player.getMainHandItem());
        ToolVariantDefinition chisel = AssemblyTools.find(Tool.CHISEL.type(), minecraft.player.getOffhandItem());
        if (hammer == null || chisel == null) return;

        var recipes = ChiselingRecipes.forBlock(minecraft.level, minecraft.level.getBlockState(hit.getBlockPos()));
        if (recipes.stream().noneMatch(holder -> meetsTier(hammer.tier(), chisel.tier(), holder.value().tier()))) return;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        Vec3 camera = event.getCamera().getPosition();
        Direction face = hit.getDirection();
        BlockPos pos = hit.getBlockPos();

        poseStack.pushPose();
        PoseStack.Pose pose = poseStack.last();

        for (int i = 0; i <= ChiselingGrid.SIZE; i++) {
            double t = i / (double) ChiselingGrid.SIZE;
            line(lines, pose,
                    relative(ChiselingGrid.point(pos, face, t, 0.0D, FACE_OFFSET), camera),
                    relative(ChiselingGrid.point(pos, face, t, 1.0D, FACE_OFFSET), camera),
                    face, 255, 255, 255, 235);
            line(lines, pose,
                    relative(ChiselingGrid.point(pos, face, 0.0D, t, FACE_OFFSET), camera),
                    relative(ChiselingGrid.point(pos, face, 1.0D, t, FACE_OFFSET), camera),
                    face, 255, 255, 255, 235);
        }

        int hovered = ChiselingGrid.cell(hit);
        if (hovered >= 1) {
            int index = hovered - 1;
            int row = index / ChiselingGrid.SIZE;
            int column = index % ChiselingGrid.SIZE;
            double u0 = column / (double) ChiselingGrid.SIZE;
            double u1 = (column + 1) / (double) ChiselingGrid.SIZE;
            double v0 = row / (double) ChiselingGrid.SIZE;
            double v1 = (row + 1) / (double) ChiselingGrid.SIZE;
            double inset = 0.025D;
            outlineCell(lines, pose, pos, face, camera,
                    u0 + inset, v0 + inset, u1 - inset, v1 - inset,
                    255, 220, 90, 255);
        }

        // Draw 1..9 in exactly the same face coordinate system as the grid. These are
        // vector digits instead of billboarded font glyphs, so they remain attached to
        // NORTH/SOUTH/EAST/WEST/UP/DOWN faces and cannot rotate away from the surface.
        for (int cell = 1; cell <= 9; cell++) {
            drawCellNumber(lines, pose, pos, face, camera, cell, cell == hovered);
        }

        poseStack.popPose();
        buffers.endBatch();
    }

    private static void drawCellNumber(
            VertexConsumer lines,
            PoseStack.Pose pose,
            BlockPos pos,
            Direction face,
            Vec3 camera,
            int digit,
            boolean hovered
    ) {
        int index = digit - 1;
        int row = index / ChiselingGrid.SIZE;
        int column = index % ChiselingGrid.SIZE;
        double centerU = (column + 0.5D) / ChiselingGrid.SIZE;
        double centerV = (row + 0.5D) / ChiselingGrid.SIZE;
        int red = hovered ? 255 : 110;
        int green = hovered ? 220 : 235;
        int blue = hovered ? 90 : 255;
        int alpha = 255;

        boolean[] segments = segments(digit);
        // A, B, C, D, E, F, G seven-segment layout.
        if (segments[0]) digitLine(lines, pose, pos, face, camera, centerU - DIGIT_HALF_WIDTH, centerV - DIGIT_HALF_HEIGHT, centerU + DIGIT_HALF_WIDTH, centerV - DIGIT_HALF_HEIGHT, red, green, blue, alpha);
        if (segments[1]) digitLine(lines, pose, pos, face, camera, centerU + DIGIT_HALF_WIDTH, centerV - DIGIT_HALF_HEIGHT, centerU + DIGIT_HALF_WIDTH, centerV, red, green, blue, alpha);
        if (segments[2]) digitLine(lines, pose, pos, face, camera, centerU + DIGIT_HALF_WIDTH, centerV, centerU + DIGIT_HALF_WIDTH, centerV + DIGIT_HALF_HEIGHT, red, green, blue, alpha);
        if (segments[3]) digitLine(lines, pose, pos, face, camera, centerU - DIGIT_HALF_WIDTH, centerV + DIGIT_HALF_HEIGHT, centerU + DIGIT_HALF_WIDTH, centerV + DIGIT_HALF_HEIGHT, red, green, blue, alpha);
        if (segments[4]) digitLine(lines, pose, pos, face, camera, centerU - DIGIT_HALF_WIDTH, centerV, centerU - DIGIT_HALF_WIDTH, centerV + DIGIT_HALF_HEIGHT, red, green, blue, alpha);
        if (segments[5]) digitLine(lines, pose, pos, face, camera, centerU - DIGIT_HALF_WIDTH, centerV - DIGIT_HALF_HEIGHT, centerU - DIGIT_HALF_WIDTH, centerV, red, green, blue, alpha);
        if (segments[6]) digitLine(lines, pose, pos, face, camera, centerU - DIGIT_HALF_WIDTH, centerV, centerU + DIGIT_HALF_WIDTH, centerV, red, green, blue, alpha);
    }

    private static boolean[] segments(int digit) {
        return switch (digit) {
            case 1 -> new boolean[]{false, true, true, false, false, false, false};
            case 2 -> new boolean[]{true, true, false, true, true, false, true};
            case 3 -> new boolean[]{true, true, true, true, false, false, true};
            case 4 -> new boolean[]{false, true, true, false, false, true, true};
            case 5 -> new boolean[]{true, false, true, true, false, true, true};
            case 6 -> new boolean[]{true, false, true, true, true, true, true};
            case 7 -> new boolean[]{true, true, true, false, false, false, false};
            case 8 -> new boolean[]{true, true, true, true, true, true, true};
            case 9 -> new boolean[]{true, true, true, true, false, true, true};
            default -> new boolean[7];
        };
    }

    private static void digitLine(
            VertexConsumer lines,
            PoseStack.Pose pose,
            BlockPos pos,
            Direction face,
            Vec3 camera,
            double u0,
            double v0,
            double u1,
            double v1,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        // Render each digit segment as five tightly packed parallel lines. RenderType.lines()
        // is otherwise only a one-pixel stroke, which made the numbers look thin and broken.
        // Offsetting in the 2D face plane keeps every stroke attached to the same numbered cell.
        double du = u1 - u0;
        double dv = v1 - v0;
        double length = Math.sqrt(du * du + dv * dv);
        double perpendicularU = length <= 1.0E-9D ? 0.0D : -dv / length;
        double perpendicularV = length <= 1.0E-9D ? 0.0D : du / length;

        for (int stroke = -DIGIT_STROKE_RADIUS; stroke <= DIGIT_STROKE_RADIUS; stroke++) {
            double offset = stroke * DIGIT_STROKE_SPACING;
            double su = perpendicularU * offset;
            double sv = perpendicularV * offset;
            Vec3 from = relative(ChiselingGrid.point(pos, face, u0 + su, v0 + sv, FACE_OFFSET * 3.0D), camera);
            Vec3 to = relative(ChiselingGrid.point(pos, face, u1 + su, v1 + sv, FACE_OFFSET * 3.0D), camera);
            line(lines, pose, from, to, face, red, green, blue, alpha);
        }
    }

    private static void outlineCell(
            VertexConsumer lines,
            PoseStack.Pose pose,
            BlockPos pos,
            Direction face,
            Vec3 camera,
            double u0,
            double v0,
            double u1,
            double v1,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        Vec3 a = relative(ChiselingGrid.point(pos, face, u0, v0, FACE_OFFSET * 2.0D), camera);
        Vec3 b = relative(ChiselingGrid.point(pos, face, u1, v0, FACE_OFFSET * 2.0D), camera);
        Vec3 c = relative(ChiselingGrid.point(pos, face, u1, v1, FACE_OFFSET * 2.0D), camera);
        Vec3 d = relative(ChiselingGrid.point(pos, face, u0, v1, FACE_OFFSET * 2.0D), camera);
        line(lines, pose, a, b, face, red, green, blue, alpha);
        line(lines, pose, b, c, face, red, green, blue, alpha);
        line(lines, pose, c, d, face, red, green, blue, alpha);
        line(lines, pose, d, a, face, red, green, blue, alpha);
    }

    private static void line(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            Vec3 from,
            Vec3 to,
            Direction normal,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        consumer.addVertex(pose, (float) from.x, (float) from.y, (float) from.z)
                .setColor(red, green, blue, alpha)
                .setNormal(pose, normal.getStepX(), normal.getStepY(), normal.getStepZ());
        consumer.addVertex(pose, (float) to.x, (float) to.y, (float) to.z)
                .setColor(red, green, blue, alpha)
                .setNormal(pose, normal.getStepX(), normal.getStepY(), normal.getStepZ());
    }

    private static Vec3 relative(Vec3 world, Vec3 camera) {
        return world.subtract(camera);
    }

    private static boolean meetsTier(MachineTier hammer, MachineTier chisel, MachineTier required) {
        return Math.min(rank(hammer), rank(chisel)) >= rank(required);
    }

    private static int rank(MachineTier tier) {
        if (tier == null || tier == MachineTier.NONE) return -1;
        int rank = MachineTier.ELECTRIC_TIERS.indexOf(tier.recipeTier());
        return rank < 0 ? -1 : rank;
    }
}
