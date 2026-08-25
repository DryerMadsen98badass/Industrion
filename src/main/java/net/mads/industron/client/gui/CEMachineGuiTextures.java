package net.mads.industron.client.gui;

import net.mads.industron.Industron;
import net.mads.industron.gui.ProgressBar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

public final class CEMachineGuiTextures {
    private static final int PANEL_BACKGROUND = 0xFFC6C6C6;
    private static final int PANEL_LIGHT = 0xFFFFFFFF;
    private static final int PANEL_DARK = 0xFF555555;
    private static final int PANEL_INNER = 0xFF8B8B8B;
    public static final ResourceLocation ITEM_SLOT = ResourceLocation.fromNamespaceAndPath(
            Industron.MOD_ID,
            "gui/slot/item.png"
    );
    public static final ResourceLocation FLUID_SLOT = ResourceLocation.fromNamespaceAndPath(
            Industron.MOD_ID,
            "gui/slot/fluid_slot.png"
    );
    public static final ResourceLocation INPUT_SLOT_OVERLAY = ResourceLocation.fromNamespaceAndPath(
            Industron.MOD_ID,
            "gui/overlay/in_slot_overlay.png"
    );
    public static final ResourceLocation OUTPUT_SLOT_OVERLAY = ResourceLocation.fromNamespaceAndPath(
            Industron.MOD_ID,
            "gui/overlay/out_slot_overlay.png"
    );

    private CEMachineGuiTextures() {
    }

    public static void drawItemSlot(GuiGraphics graphics, int x, int y) {
        graphics.blit(ITEM_SLOT, x, y, 0, 0, 18, 18, 18, 18);
    }

    public static void drawFluidSlot(GuiGraphics graphics, int x, int y) {
        graphics.blit(FLUID_SLOT, x, y, 0, 0, 18, 18, 18, 18);
    }

    public static void drawInputOverlay(GuiGraphics graphics, int x, int y) {
        graphics.blit(INPUT_SLOT_OVERLAY, x, y, 0, 0, 18, 18, 18, 18);
    }

    public static void drawOutputOverlay(GuiGraphics graphics, int x, int y) {
        graphics.blit(OUTPUT_SLOT_OVERLAY, x, y, 0, 0, 18, 18, 18, 18);
    }

    public static void drawMachinePanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, PANEL_BACKGROUND);
        graphics.fill(x, y, x + width, y + 1, PANEL_LIGHT);
        graphics.fill(x, y, x + 1, y + height, PANEL_LIGHT);
        graphics.fill(x, y + height - 1, x + width, y + height, PANEL_DARK);
        graphics.fill(x + width - 1, y, x + width, y + height, PANEL_DARK);
        graphics.fill(x + 7, y + 16, x + width - 7, y + height - 7, PANEL_INNER);
    }

    public static void drawFluid(GuiGraphics graphics, FluidStack stack, int x, int y) {
        if (stack.isEmpty()) {
            return;
        }
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(stack.getFluid());
        ResourceLocation texture = extensions.getStillTexture(stack);
        if (texture == null) {
            return;
        }
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(texture);
        int tint = extensions.getTintColor(stack);
        float alpha = ((tint >> 24) & 0xFF) / 255.0F;
        float red = ((tint >> 16) & 0xFF) / 255.0F;
        float green = ((tint >> 8) & 0xFF) / 255.0F;
        float blue = (tint & 0xFF) / 255.0F;
        graphics.blit(x, y, 0, 16, 16, sprite, red, green, blue, alpha <= 0.0F ? 1.0F : alpha);
    }

    public static void drawProgressBar(
            GuiGraphics graphics,
            ProgressBar bar,
            int x,
            int y,
            float progress
    ) {
        int width = bar.width();
        int height = bar.height();
        graphics.blit(
                bar.texture(),
                x,
                y,
                0,
                0,
                width,
                height,
                bar.textureWidth(),
                bar.textureHeight()
        );

        float clamped = Mth.clamp(progress, 0.0F, 1.0F);
        if (clamped <= 0.0F) {
            return;
        }

        int filledWidth = Math.max(1, Math.round(width * clamped));
        int filledHeight = Math.max(1, Math.round(height * clamped));
        int sourceU = bar.filledU();
        int sourceV = bar.filledV();
        int drawX = x;
        int drawY = y;
        int drawWidth = width;
        int drawHeight = height;

        switch (bar.direction()) {
            case RIGHT -> drawWidth = filledWidth;
            case LEFT -> {
                drawX += width - filledWidth;
                sourceU += width - filledWidth;
                drawWidth = filledWidth;
            }
            case DOWN -> drawHeight = filledHeight;
            case UP -> {
                drawY += height - filledHeight;
                sourceV += height - filledHeight;
                drawHeight = filledHeight;
            }
        }

        graphics.blit(
                bar.texture(),
                drawX,
                drawY,
                sourceU,
                sourceV,
                drawWidth,
                drawHeight,
                bar.textureWidth(),
                bar.textureHeight()
        );
    }
}
