package net.mads.industron.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.gui.RemovedGuiUtils;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.infrastructure.config.CClient;
import net.createmod.catnip.gui.element.BoxElement;
import net.createmod.catnip.theme.Color;
import net.mads.industron.item.CreativeGogglesItem;
import net.mads.industron.material.forging.AnvilForgeDebugData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;

/** Creative-goggle-only view of the otherwise hidden current anvil forge number. */
public final class CreativeGogglesAnvilOverlay {
    private CreativeGogglesAnvilOverlay() {
    }

    public static void renderOverlay(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.level == null || minecraft.player == null
                || minecraft.screen != null || !CreativeGogglesItem.isWearing(minecraft.player)
                || !(minecraft.hitResult instanceof BlockHitResult hit)
                || !(minecraft.level.getBlockState(hit.getBlockPos()).getBlock() instanceof AnvilBlock)) {
            return;
        }

        String dimension = minecraft.level.dimension().location().toString();
        // During an active tool use, AssemblyNextStepOverlay owns the Anvil tooltip and also
        // adds Creative-goggle forge state. Keep this idle overlay from drawing on top of it.
        if (AssemblyNextStepOverlay.isShowingAnvilAt(dimension, hit.getBlockPos())) return;

        Integer forgeValue = findForgeValue(minecraft, hit);
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.literal("Anvil Forging").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Forge value: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(forgeValue == null ? "Empty" : Integer.toString(forgeValue))
                        .withStyle(ChatFormatting.WHITE)));

        int tooltipTextWidth = 0;
        for (FormattedText line : tooltip) {
            tooltipTextWidth = Math.max(tooltipTextWidth, minecraft.font.width(line));
        }
        int tooltipHeight = 20;
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        CClient cfg = AllConfigs.client();
        int posX = Math.min(width / 2 + cfg.overlayOffsetX.get(), width - tooltipTextWidth - 20);
        int posY = Math.min(height / 2 + cfg.overlayOffsetY.get(), height - tooltipHeight - 20);

        boolean custom = cfg.overlayCustomColor.get();
        Color background = custom ? new Color(cfg.overlayBackgroundColor.get())
                : BoxElement.COLOR_VANILLA_BACKGROUND.scaleAlpha(0.75F);
        Color borderTop = custom ? new Color(cfg.overlayBorderColorTop.get())
                : BoxElement.COLOR_VANILLA_BORDER.getFirst().copy();
        Color borderBottom = custom ? new Color(cfg.overlayBorderColorBot.get())
                : BoxElement.COLOR_VANILLA_BORDER.getSecond().copy();

        PoseStack pose = graphics.pose();
        pose.pushPose();
        RemovedGuiUtils.drawHoveringText(
                graphics, tooltip, posX, posY, width, height, -1,
                background.getRGB(), borderTop.getRGB(), borderBottom.getRGB(), minecraft.font
        );
        pose.popPose();
    }

    private static Integer findForgeValue(Minecraft minecraft, BlockHitResult hit) {
        AABB box = new AABB(hit.getBlockPos()).inflate(1.25D).move(0.0D, 0.75D, 0.0D);
        for (ItemEntity entity : minecraft.level.getEntitiesOfClass(ItemEntity.class, box)) {
            Integer value = AnvilForgeDebugData.readForgeValue(entity.getItem(), hit.getBlockPos());
            if (value != null) return value;
        }
        return null;
    }
}
