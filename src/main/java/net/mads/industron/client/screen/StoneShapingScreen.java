package net.mads.industron.client.screen;

import net.mads.industron.menu.StoneShapingMenu;
import net.mads.industron.recipe.stone_shaping.StoneShapingPattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;

public final class StoneShapingScreen extends AbstractContainerScreen<StoneShapingMenu> {
    private static final int CELL = 10;
    private static final int GRID_SIZE = CELL * StoneShapingPattern.SIZE;
    private static final int GRID_X = 12;
    private static final int GRID_Y = 28;
    private static final int DONE_X = 184;
    private static final int DONE_Y = 30;

    public StoneShapingScreen(StoneShapingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 250;
        imageHeight = 208;
        titleLabelX = 12;
        titleLabelY = 8;
        inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("Done"), button -> {
                    if (minecraft != null && minecraft.gameMode != null) {
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, StoneShapingMenu.COMPLETE_BUTTON);
                    }
                })
                .bounds(leftPos + DONE_X, topPos + DONE_Y, 54, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x0 = leftPos;
        int y0 = topPos;
        graphics.fill(x0, y0, x0 + imageWidth, y0 + imageHeight, 0xFFC6C6C6);
        graphics.fill(x0, y0, x0 + imageWidth, y0 + 1, 0xFFFFFFFF);
        graphics.fill(x0, y0, x0 + 1, y0 + imageHeight, 0xFFFFFFFF);
        graphics.fill(x0, y0 + imageHeight - 1, x0 + imageWidth, y0 + imageHeight, 0xFF555555);
        graphics.fill(x0 + imageWidth - 1, y0, x0 + imageWidth, y0 + imageHeight, 0xFF555555);

        int gridX = leftPos + GRID_X;
        int gridY = topPos + GRID_Y;
        graphics.fill(gridX - 2, gridY - 2, gridX + GRID_SIZE + 2, gridY + GRID_SIZE + 2, 0xFF555555);
        graphics.fill(gridX, gridY, gridX + GRID_SIZE, gridY + GRID_SIZE, 0xFF202020);

        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(menu.texture());
        for (int y = 0; y < StoneShapingPattern.SIZE; y++) {
            for (int x = 0; x < StoneShapingPattern.SIZE; x++) {
                if (!menu.hasPixel(x, y)) continue;
                int cellX = gridX + x * CELL;
                int cellY = gridY + y * CELL;
                graphics.enableScissor(cellX, cellY, cellX + CELL, cellY + CELL);
                graphics.blit(gridX, gridY, 0, GRID_SIZE, GRID_SIZE, sprite, 1.0F, 1.0F, 1.0F, 1.0F);
                graphics.disableScissor();
            }
        }

        for (int i = 0; i <= StoneShapingPattern.SIZE; i++) {
            int gx = gridX + i * CELL;
            int gy = gridY + i * CELL;
            graphics.fill(gx, gridY, gx + 1, gridY + GRID_SIZE, 0x30303030);
            graphics.fill(gridX, gy, gridX + GRID_SIZE, gy + 1, 0x30303030);
        }

    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Stone Shaping is intentionally visual-only. The Done button is the only text control.
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int gridX = leftPos + GRID_X;
        int gridY = topPos + GRID_Y;
        if (button == 0 && mouseX >= gridX && mouseX < gridX + GRID_SIZE
                && mouseY >= gridY && mouseY < gridY + GRID_SIZE) {
            int x = (int) ((mouseX - gridX) / CELL);
            int y = (int) ((mouseY - gridY) / CELL);
            if (menu.hasPixel(x, y)) {
                menu.removePixelLocal(x, y);
                if (minecraft != null && minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(
                            menu.containerId,
                            StoneShapingMenu.PIXEL_BUTTON_BASE + StoneShapingPattern.index(x, y)
                    );
                }
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
