package net.mads.industron.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.gui.RemovedGuiUtils;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.infrastructure.config.CClient;
import net.createmod.catnip.gui.element.BoxElement;
import net.createmod.catnip.theme.Color;
import net.mads.industron.network.AssemblyNextStepPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Create-style hover tooltip for the next assembly step.
 *
 * The server owns assembly progress. This class only renders the last synced next step,
 * and only while the player is looking at the exact block position being assembled.
 */
public final class AssemblyNextStepOverlay {
    private static boolean visible;
    private static String dimension = "";
    private static net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.ZERO;
    private static String nextStep = "";
    private static int toolProgressTicks;
    private static int toolDurationTicks;
    private static int hoverTicks;

    private AssemblyNextStepOverlay() {
    }

    public static void accept(AssemblyNextStepPayload payload) {
        if (!payload.visible()) {
            clear();
            return;
        }

        boolean changed = !visible
                || !dimension.equals(payload.dimension())
                || !pos.equals(payload.pos())
                || !nextStep.equals(payload.nextStep());

        visible = true;
        dimension = payload.dimension();
        pos = payload.pos().immutable();
        nextStep = payload.nextStep();
        toolProgressTicks = payload.toolProgressTicks();
        toolDurationTicks = payload.toolDurationTicks();
        if (changed) hoverTicks = 0;
    }

    public static void renderOverlay(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!visible
                || minecraft.options.hideGui
                || minecraft.level == null
                || minecraft.player == null
                || minecraft.screen != null
                || !minecraft.level.dimension().location().toString().equals(dimension)
                || !(minecraft.hitResult instanceof BlockHitResult hit)
                || !hit.getBlockPos().equals(pos)) {
            hoverTicks = 0;
            return;
        }

        hoverTicks++;

        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.literal("Assembly").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Next: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(nextStep).withStyle(ChatFormatting.AQUA)));
        if (toolDurationTicks > 0) {
            tooltip.add(Component.literal("Timer: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(
                            formatSeconds(toolProgressTicks) + " / " + formatSeconds(toolDurationTicks) + " s"
                    ).withStyle(ChatFormatting.YELLOW)));
        }

        int tooltipTextWidth = 0;
        for (FormattedText line : tooltip) {
            tooltipTextWidth = Math.max(tooltipTextWidth, minecraft.font.width(line));
        }

        int tooltipHeight = 8;
        if (tooltip.size() > 1) {
            tooltipHeight += 2;
            tooltipHeight += (tooltip.size() - 1) * 10;
        }

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        CClient cfg = AllConfigs.client();
        int posX = width / 2 + cfg.overlayOffsetX.get();
        int posY = height / 2 + cfg.overlayOffsetY.get();
        posX = Math.min(posX, width - tooltipTextWidth - 20);
        posY = Math.min(posY, height - tooltipHeight - 20);

        float fade = Mth.clamp((hoverTicks + deltaTracker.getGameTimeDeltaPartialTick(false)) / 24.0F, 0.0F, 1.0F);
        boolean useCustom = cfg.overlayCustomColor.get();
        Color background = useCustom
                ? new Color(cfg.overlayBackgroundColor.get())
                : BoxElement.COLOR_VANILLA_BACKGROUND.scaleAlpha(0.75F);
        Color borderTop = useCustom
                ? new Color(cfg.overlayBorderColorTop.get())
                : BoxElement.COLOR_VANILLA_BORDER.getFirst().copy();
        Color borderBottom = useCustom
                ? new Color(cfg.overlayBorderColorBot.get())
                : BoxElement.COLOR_VANILLA_BORDER.getSecond().copy();

        PoseStack pose = graphics.pose();
        pose.pushPose();
        if (fade < 1.0F) {
            pose.translate(Math.pow(1.0F - fade, 3.0F) * Math.signum(cfg.overlayOffsetX.get() + 0.5F) * 8.0F, 0.0F, 0.0F);
            background.scaleAlpha(fade);
            borderTop.scaleAlpha(fade);
            borderBottom.scaleAlpha(fade);
        }

        RemovedGuiUtils.drawHoveringText(
                graphics,
                tooltip,
                posX,
                posY,
                width,
                height,
                -1,
                background.getRGB(),
                borderTop.getRGB(),
                borderBottom.getRGB(),
                minecraft.font
        );
        pose.popPose();
    }

    private static void clear() {
        visible = false;
        dimension = "";
        pos = net.minecraft.core.BlockPos.ZERO;
        nextStep = "";
        toolProgressTicks = 0;
        toolDurationTicks = 0;
        hoverTicks = 0;
    }

    private static String formatSeconds(int ticks) {
        return String.format(Locale.ROOT, "%.1f", ticks / 20.0F);
    }
}
