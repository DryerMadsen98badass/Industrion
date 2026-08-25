package net.mads.industron.client.screen;

import net.mads.industron.client.gui.CEMachineGuiTextures;
import net.mads.industron.menu.MachinePortMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.ArrayList;
import java.util.List;

public class MachinePortScreen extends AbstractContainerScreen<MachinePortMenu> {
    private static final int BACKGROUND = 0xF0111418;
    private static final int PANEL = 0xFF1A2027;
    private static final int PANEL_EDGE = 0xFF3E4A55;
    private static final int SLOT = 0xFF20262D;
    private static final int SLOT_DARK = 0xFF15191F;
    private static final int SLOT_EDGE = 0xFF59636F;
    private static final int TEXT = 0xFFE6EDF3;
    private static final int MUTED = 0xFF9CA8B3;
    private static final int BUTTON_WIDTH = 52;
    private static final int BUTTON_HEIGHT = 16;
    private static final int BUTTON_GAP = 5;

    public MachinePortScreen(MachinePortMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = menu.menuImageWidth();
        imageHeight = menu.menuImageHeight();
        titleLabelX = 10;
        titleLabelY = 8;
        inventoryLabelX = menu.playerInventoryX();
        inventoryLabelY = menu.playerInventoryY() - 10;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderConfigTooltip(graphics, mouseX, mouseY);
        renderFluidTooltip(graphics, mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        drawPanel(graphics, x, y, imageWidth, imageHeight);
        drawConfigButtons(graphics, x, y, mouseX, mouseY);

        if (menu.portSlotCount() > 0) {
            graphics.drawString(font, Component.literal("Items").withStyle(ChatFormatting.GRAY), x + menu.contentX(), y + menu.itemSlotY() - 10, MUTED, false);
        }
        if (menu.fluidSlotCount() > 0) {
            graphics.drawString(font, Component.literal("Fluids").withStyle(ChatFormatting.GRAY), x + menu.contentX(), y + menu.fluidSlotY() - 10, MUTED, false);
            drawFluidSlots(graphics, mouseX, mouseY);
        }

        for (Slot slot : menu.slots) {
            CEMachineGuiTextures.drawItemSlot(graphics, x + slot.x, y + slot.y);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, MUTED, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if ((button == 0 || button == 1) && minecraft != null && minecraft.gameMode != null) {
            int action = clickedConfigAction((int) mouseX, (int) mouseY, button);
            if (action >= 0) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action);
                return true;
            }
        }
        if ((button == 0 || button == 1) && minecraft != null && minecraft.gameMode != null) {
            int fluidSlot = hoveredFluidSlot((int) mouseX, (int) mouseY);
            if (fluidSlot >= 0) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, MachinePortMenu.BUTTON_FLUID_SLOT + fluidSlot);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, MachinePortMenu.BUTTON_SYNC);
        }
        super.onClose();
    }

    private void drawConfigButtons(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        if (menu.blockEntity() == null) {
            return;
        }

        int index = 0;
        if (menu.supportsIoColor()) {
            DyeColor color = menu.blockEntity().ioColor();
            int buttonX = configButtonX(x);
            int buttonY = configButtonY(y, index++);
            drawSmallButton(graphics, buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT, shortColorName(color), isHoveringArea(mouseX, mouseY, buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT), false);
            graphics.fill(buttonX + 4, buttonY + 4, buttonX + 11, buttonY + 11, dyeColor(color));
        }
        if (menu.supportsCircuit()) {
            int buttonX = configButtonX(x);
            int buttonY = configButtonY(y, index++);
            drawSmallButton(graphics, buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT, "C " + menu.blockEntity().circuit(), isHoveringArea(mouseX, mouseY, buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT), false);
        }
        if (menu.supportsAutoOutput()) {
            int buttonX = configButtonX(x);
            int buttonY = configButtonY(y, index);
            boolean on = menu.blockEntity().autoOutput();
            drawSmallButton(graphics, buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT, on ? "On" : "Off", isHoveringArea(mouseX, mouseY, buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT), on);
        }
    }

    private int clickedConfigAction(int mouseX, int mouseY, int mouseButton) {
        int index = 0;
        if (menu.supportsIoColor()) {
            if (isHoveringArea(mouseX, mouseY, configButtonX(leftPos), configButtonY(topPos, index), BUTTON_WIDTH, BUTTON_HEIGHT)) {
                return mouseButton == 0 && hasShiftDown()
                        ? MachinePortMenu.BUTTON_COLOR_RESET
                        : mouseButton == 0 ? MachinePortMenu.BUTTON_COLOR_INCREMENT : MachinePortMenu.BUTTON_COLOR_DECREMENT;
            }
            index++;
        }
        if (menu.supportsCircuit()) {
            if (isHoveringArea(mouseX, mouseY, configButtonX(leftPos), configButtonY(topPos, index), BUTTON_WIDTH, BUTTON_HEIGHT)) {
                return mouseButton == 0 && hasShiftDown()
                        ? MachinePortMenu.BUTTON_CIRCUIT_RESET
                        : mouseButton == 0 ? MachinePortMenu.BUTTON_CIRCUIT_INCREMENT : MachinePortMenu.BUTTON_CIRCUIT_DECREMENT;
            }
            index++;
        }
        if (menu.supportsAutoOutput() && isHoveringArea(mouseX, mouseY, configButtonX(leftPos), configButtonY(topPos, index), BUTTON_WIDTH, BUTTON_HEIGHT)) {
            return mouseButton == 0 && hasShiftDown() ? MachinePortMenu.BUTTON_AUTO_RESET : MachinePortMenu.BUTTON_AUTO_TOGGLE;
        }
        return -1;
    }

    private void renderConfigTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int index = 0;
        if (menu.supportsIoColor()) {
            if (isHoveringArea(mouseX, mouseY, configButtonX(leftPos), configButtonY(topPos, index), BUTTON_WIDTH, BUTTON_HEIGHT)) {
                DyeColor color = menu.blockEntity().ioColor();
                graphics.renderComponentTooltip(font, List.of(
                        Component.literal("Color: " + color.getName()),
                        Component.literal("Left: next").withStyle(ChatFormatting.GRAY),
                        Component.literal("Right: previous").withStyle(ChatFormatting.GRAY),
                        Component.literal("Shift Left: gray").withStyle(ChatFormatting.GRAY)
                ), mouseX, mouseY);
                return;
            }
            index++;
        }
        if (menu.supportsCircuit()) {
            if (isHoveringArea(mouseX, mouseY, configButtonX(leftPos), configButtonY(topPos, index), BUTTON_WIDTH, BUTTON_HEIGHT)) {
                graphics.renderComponentTooltip(font, List.of(
                        Component.literal("Circuit: " + menu.blockEntity().circuit()),
                        Component.literal("Left: up").withStyle(ChatFormatting.GRAY),
                        Component.literal("Right: down").withStyle(ChatFormatting.GRAY),
                        Component.literal("Shift Left: default").withStyle(ChatFormatting.GRAY)
                ), mouseX, mouseY);
                return;
            }
            index++;
        }
        if (menu.supportsAutoOutput() && isHoveringArea(mouseX, mouseY, configButtonX(leftPos), configButtonY(topPos, index), BUTTON_WIDTH, BUTTON_HEIGHT)) {
            graphics.renderComponentTooltip(font, List.of(
                    Component.literal("Auto Output: " + (menu.blockEntity().autoOutput() ? "On" : "Off")),
                    Component.literal("Left/Right: toggle").withStyle(ChatFormatting.GRAY),
                    Component.literal("Shift Left: off").withStyle(ChatFormatting.GRAY)
            ), mouseX, mouseY);
        }
    }

    private void drawFluidSlots(GuiGraphics graphics, int mouseX, int mouseY) {
        List<FluidTank> tanks = menu.fluidTanks();
        for (int i = 0; i < menu.fluidSlotCount(); i++) {
            int x = leftPos + menu.fluidSlotX(i);
            int y = topPos + menu.fluidSlotY();
            CEMachineGuiTextures.drawFluidSlot(graphics, x, y);
            if (isHoveringArea(mouseX, mouseY, x, y, 18, 18)) {
                graphics.fill(x, y, x + 18, y + 18, 0x60FFFFFF);
            }

            if (i >= tanks.size()) {
                continue;
            }

            FluidStack stack = tanks.get(i).getFluid();
            if (!stack.isEmpty()) {
                CEMachineGuiTextures.drawFluid(graphics, stack, x + 1, y + 1);
                String amount = formatAmount(stack.getAmount());
                int amountX = x + 9 - font.width(amount) / 2;
                graphics.drawString(font, amount, amountX, y + 20, MUTED, false);
            }
        }
    }

    private void renderFluidTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        List<FluidTank> tanks = menu.fluidTanks();
        for (int i = 0; i < Math.min(menu.fluidSlotCount(), tanks.size()); i++) {
            int x = leftPos + menu.fluidSlotX(i);
            int y = topPos + menu.fluidSlotY();
            if (!isHoveringArea(mouseX, mouseY, x, y, 18, 18)) {
                continue;
            }

            FluidStack stack = tanks.get(i).getFluid();
            List<Component> tooltip = new ArrayList<>();
            if (stack.isEmpty()) {
                tooltip.add(Component.literal("Empty"));
            } else {
                tooltip.add(stack.getHoverName());
                tooltip.add(Component.literal(stack.getAmount() + " / " + tanks.get(i).getCapacity() + " mB").withStyle(ChatFormatting.GRAY));
            }
            graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
            return;
        }
    }

    private int hoveredFluidSlot(int mouseX, int mouseY) {
        for (int i = 0; i < menu.fluidSlotCount(); i++) {
            int x = leftPos + menu.fluidSlotX(i);
            int y = topPos + menu.fluidSlotY();
            if (isHoveringArea(mouseX, mouseY, x, y, 18, 18)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isHoveringArea(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }

    private static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, BACKGROUND);
        graphics.fill(x, y, x + width, y + 1, PANEL_EDGE);
        graphics.fill(x, y + height - 1, x + width, y + height, PANEL_EDGE);
        graphics.fill(x, y, x + 1, y + height, PANEL_EDGE);
        graphics.fill(x + width - 1, y, x + width, y + height, PANEL_EDGE);
        graphics.fill(x + 5, y + 15, x + width - 5, y + height - 5, PANEL);
    }

    private static int configButtonX(int panelX) {
        return panelX + 10;
    }

    private static int configButtonY(int panelY, int index) {
        return panelY + 24 + index * (BUTTON_HEIGHT + BUTTON_GAP);
    }

    private void drawSmallButton(GuiGraphics graphics, int x, int y, int width, int height, String label, boolean hovered, boolean active) {
        int edge = hovered ? 0xFF7E8B98 : 0xFF56616D;
        int fill = active ? 0xFF263B2D : 0xFF202832;
        graphics.fill(x, y, x + width, y + height, edge);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, fill);
        int textX = x + Math.max(3, (width - font.width(label)) / 2);
        graphics.drawString(font, label, textX, y + 4, TEXT, false);
    }

    private static String shortColorName(DyeColor color) {
        return switch (color) {
            case LIGHT_BLUE -> "Lt Blue";
            case LIGHT_GRAY -> "Lt Gray";
            default -> color.getName();
        };
    }

    private static String formatAmount(int amount) {
        if (amount >= 1_000_000) {
            return (amount / 1_000_000) + "M";
        }
        if (amount >= 1000) {
            return (amount / 1000) + "k";
        }
        return Integer.toString(amount);
    }

    private static int dyeColor(DyeColor color) {
        return switch (color) {
            case WHITE -> 0xFFF9FFFE;
            case ORANGE -> 0xFFF9801D;
            case MAGENTA -> 0xFFC74EBD;
            case LIGHT_BLUE -> 0xFF3AB3DA;
            case YELLOW -> 0xFFFED83D;
            case LIME -> 0xFF80C71F;
            case PINK -> 0xFFF38BAA;
            case GRAY -> 0xFF474F52;
            case LIGHT_GRAY -> 0xFF9D9D97;
            case CYAN -> 0xFF169C9C;
            case PURPLE -> 0xFF8932B8;
            case BLUE -> 0xFF3C44AA;
            case BROWN -> 0xFF835432;
            case GREEN -> 0xFF5E7C16;
            case RED -> 0xFFB02E26;
            case BLACK -> 0xFF1D1D21;
        };
    }
}
