package net.mads.industron.client.screen;

import net.mads.industron.client.gui.CEMachineGuiTextures;
import net.mads.industron.gui.ProgressBar;
import net.mads.industron.menu.SingleBlockMachineMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.ArrayList;
import java.util.List;

public class SingleBlockMachineScreen extends AbstractContainerScreen<SingleBlockMachineMenu> {
    private static final int TEXT = 0xFF404040;
    private static final int FLUID_SLOT_SIZE = 18;
    private static final int CIRCUIT_BUTTON_WIDTH = 38;
    private static final int CIRCUIT_BUTTON_HEIGHT = 16;
    private static final int POWER_BUTTON_WIDTH = 38;
    private static final int POWER_BUTTON_HEIGHT = 16;

    public SingleBlockMachineScreen(
            SingleBlockMachineMenu menu,
            Inventory playerInventory,
            Component title
    ) {
        super(menu, playerInventory, title);
        imageWidth = menu.menuImageWidth();
        imageHeight = menu.menuImageHeight();
        titleLabelX = 8;
        titleLabelY = 6;
        inventoryLabelX = (imageWidth - 9 * 18) / 2;
        inventoryLabelY = menu.playerInventoryY() - 11;
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderFluidTooltip(graphics, mouseX, mouseY);
        renderCircuitTooltip(graphics, mouseX, mouseY);
        renderPowerTooltip(graphics, mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(
            GuiGraphics graphics,
            float partialTick,
            int mouseX,
            int mouseY
    ) {
        int x = leftPos;
        int y = topPos;
        CEMachineGuiTextures.drawMachinePanel(graphics, x, y, imageWidth, imageHeight);

        for (int index = 0; index < menu.slots.size(); index++) {
            Slot slot = menu.slots.get(index);
            CEMachineGuiTextures.drawItemSlot(graphics, x + slot.x - 1, y + slot.y - 1);
            boolean kilnSlot = menu.blockEntity() != null
                    && menu.blockEntity().isKiln()
                    && index < menu.itemInputSlots();
            if (!kilnSlot && index < menu.itemInputSlots()) {
                CEMachineGuiTextures.drawInputOverlay(graphics, x + slot.x - 1, y + slot.y - 1);
            } else if (!kilnSlot && index < menu.itemInputSlots() + menu.itemOutputSlots()) {
                CEMachineGuiTextures.drawOutputOverlay(graphics, x + slot.x - 1, y + slot.y - 1);
            }
        }

        drawFluidSlots(graphics);
        drawHoveredFluidSlot(graphics, mouseX, mouseY);
        drawProgress(graphics, x, y);
        drawCircuitButton(graphics, mouseX, mouseY);
        drawPowerButton(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if ((button == 0 || button == 1) && minecraft != null && minecraft.gameMode != null) {
            if (inside((int) mouseX, (int) mouseY, powerButtonX(), powerButtonY(), POWER_BUTTON_WIDTH, POWER_BUTTON_HEIGHT)) {
                minecraft.gameMode.handleInventoryButtonClick(
                        menu.containerId,
                        SingleBlockMachineMenu.BUTTON_TOGGLE_MACHINE
                );
                return true;
            }

            if (inside((int) mouseX, (int) mouseY, circuitButtonX(), circuitButtonY(), CIRCUIT_BUTTON_WIDTH, CIRCUIT_BUTTON_HEIGHT)) {
                int buttonId = hasShiftDown()
                        ? SingleBlockMachineMenu.BUTTON_CIRCUIT_RESET
                        : button == 0
                        ? SingleBlockMachineMenu.BUTTON_CIRCUIT_INCREMENT
                        : SingleBlockMachineMenu.BUTTON_CIRCUIT_DECREMENT;
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
                return true;
            }

            HoveredFluid hovered = hoveredFluid((int) mouseX, (int) mouseY);
            if (hovered != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, hovered.buttonId());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
    }


    private void drawPowerButton(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = powerButtonX();
        int y = powerButtonY();
        boolean enabled = menu.machineEnabled();
        boolean hovered = inside(mouseX, mouseY, x, y, POWER_BUTTON_WIDTH, POWER_BUTTON_HEIGHT);
        int outer = hovered ? 0xFFFFFFFF : 0xFFC6C6C6;
        int inner = enabled ? 0xFF3F8A52 : 0xFF8A4141;

        graphics.fill(x, y, x + POWER_BUTTON_WIDTH, y + POWER_BUTTON_HEIGHT, outer);
        graphics.fill(x + 1, y + 1, x + POWER_BUTTON_WIDTH - 1, y + POWER_BUTTON_HEIGHT - 1, 0xFF373737);
        graphics.fill(x + 2, y + 2, x + POWER_BUTTON_WIDTH - 2, y + POWER_BUTTON_HEIGHT - 2, inner);
        graphics.drawCenteredString(
                font,
                enabled ? "ON" : "OFF",
                x + POWER_BUTTON_WIDTH / 2,
                y + 4,
                0xFFFFFFFF
        );
    }

    private void renderPowerTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!inside(mouseX, mouseY, powerButtonX(), powerButtonY(), POWER_BUTTON_WIDTH, POWER_BUTTON_HEIGHT)) {
            return;
        }

        graphics.renderComponentTooltip(
                font,
                List.of(
                        Component.literal(menu.machineEnabled() ? "Machine enabled" : "Machine disabled"),
                        Component.literal("Click to toggle").withStyle(ChatFormatting.GRAY)
                ),
                mouseX,
                mouseY
        );
    }

    private int powerButtonX() {
        return leftPos + imageWidth - POWER_BUTTON_WIDTH - 8;
    }

    private int powerButtonY() {
        return topPos + menu.playerInventoryY() - 28;
    }

    private void drawCircuitButton(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = circuitButtonX();
        int y = circuitButtonY();
        boolean hovered = inside(mouseX, mouseY, x, y, CIRCUIT_BUTTON_WIDTH, CIRCUIT_BUTTON_HEIGHT);
        int outer = hovered ? 0xFFFFFFFF : 0xFFC6C6C6;
        int inner = hovered ? 0xFF8F8F8F : 0xFF707070;

        graphics.fill(x, y, x + CIRCUIT_BUTTON_WIDTH, y + CIRCUIT_BUTTON_HEIGHT, outer);
        graphics.fill(x + 1, y + 1, x + CIRCUIT_BUTTON_WIDTH - 1, y + CIRCUIT_BUTTON_HEIGHT - 1, 0xFF373737);
        graphics.fill(x + 2, y + 2, x + CIRCUIT_BUTTON_WIDTH - 2, y + CIRCUIT_BUTTON_HEIGHT - 2, inner);

        String label = "C " + (menu.blockEntity() == null ? 0 : menu.blockEntity().circuit());
        graphics.drawCenteredString(font, label, x + CIRCUIT_BUTTON_WIDTH / 2, y + 4, 0xFFFFFFFF);
    }

    private void renderCircuitTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!inside(mouseX, mouseY, circuitButtonX(), circuitButtonY(), CIRCUIT_BUTTON_WIDTH, CIRCUIT_BUTTON_HEIGHT)) {
            return;
        }
        graphics.renderComponentTooltip(
                font,
                List.of(
                        Component.literal("Circuit " + (menu.blockEntity() == null ? 0 : menu.blockEntity().circuit())),
                        Component.literal("Left-click: next").withStyle(ChatFormatting.GRAY),
                        Component.literal("Right-click: previous").withStyle(ChatFormatting.GRAY),
                        Component.literal("Shift-click: reset").withStyle(ChatFormatting.GRAY)
                ),
                mouseX,
                mouseY
        );
    }

    private int circuitButtonX() {
        return leftPos + 8;
    }

    private int circuitButtonY() {
        return topPos + menu.playerInventoryY() - 28;
    }

    private void drawProgress(GuiGraphics graphics, int x, int y) {
        ProgressBar bar = menu.blockEntity() == null
                ? ProgressBar.ARROW
                : menu.blockEntity().progressBar();
        int done = Math.min(menu.progress(), menu.progressTotal());
        float progress = menu.progressTotal() <= 0 ? 0.0F : done / (float) menu.progressTotal();
        CEMachineGuiTextures.drawProgressBar(
                graphics,
                bar,
                x + menu.layout().progressX(),
                y + menu.layout().progressY(),
                progress
        );
    }

    private void drawFluidSlots(GuiGraphics graphics) {
        if (menu.blockEntity() == null) {
            return;
        }

        List<FluidTank> inputTanks = menu.blockEntity().inputFluidTanks();
        for (int i = 0; i < menu.fluidInputSlots(); i++) {
            int x = leftPos + menu.inputFluidSlotX(i) - 1;
            int y = topPos + menu.inputFluidSlotY(i) - 1;
            drawFluidSlot(graphics, x, y, i < inputTanks.size() ? inputTanks.get(i).getFluid() : FluidStack.EMPTY);
            CEMachineGuiTextures.drawInputOverlay(graphics, x, y);
        }

        List<FluidTank> outputTanks = menu.blockEntity().outputFluidTanks();
        for (int i = 0; i < menu.fluidOutputSlots(); i++) {
            int x = leftPos + menu.outputFluidSlotX(i) - 1;
            int y = topPos + menu.outputFluidSlotY(i) - 1;
            drawFluidSlot(graphics, x, y, i < outputTanks.size() ? outputTanks.get(i).getFluid() : FluidStack.EMPTY);
            CEMachineGuiTextures.drawOutputOverlay(graphics, x, y);
        }
    }

    private void drawFluidSlot(GuiGraphics graphics, int x, int y, FluidStack stack) {
        CEMachineGuiTextures.drawFluidSlot(graphics, x, y);
        if (!stack.isEmpty()) {
            drawFluidLayer(graphics, stack, x + 1, y + 1, 16, 16);
        }
    }

    private void renderFluidTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (menu.blockEntity() == null) {
            return;
        }

        HoveredFluid hovered = hoveredFluid(mouseX, mouseY);
        if (hovered == null) {
            return;
        }

        List<Component> tooltip = new ArrayList<>();
        if (hovered.stack().isEmpty()) {
            tooltip.add(Component.literal("Empty"));
        } else {
            tooltip.add(hovered.stack().getHoverName());
            tooltip.add(Component.literal(hovered.stack().getAmount() + " / " + hovered.capacity() + " mB")
                    .withStyle(ChatFormatting.GRAY));
        }
        graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
    }

    private void drawHoveredFluidSlot(GuiGraphics graphics, int mouseX, int mouseY) {
        HoveredFluid hovered = hoveredFluid(mouseX, mouseY);
        if (hovered == null) {
            return;
        }
        graphics.fill(hovered.x(), hovered.y(), hovered.x() + FLUID_SLOT_SIZE, hovered.y() + FLUID_SLOT_SIZE, 0x60FFFFFF);
    }

    private HoveredFluid hoveredFluid(int mouseX, int mouseY) {
        if (menu.blockEntity() == null) return null;
        List<FluidTank> inputTanks = menu.blockEntity().inputFluidTanks();
        for (int i = 0; i < menu.fluidInputSlots(); i++) {
            int x = leftPos + menu.inputFluidSlotX(i) - 1;
            int y = topPos + menu.inputFluidSlotY(i) - 1;
            if (inside(mouseX, mouseY, x, y, FLUID_SLOT_SIZE, FLUID_SLOT_SIZE)) {
                FluidTank tank = i < inputTanks.size() ? inputTanks.get(i) : null;
                int buttonId = SingleBlockMachineMenu.BUTTON_INPUT_FLUID_SLOT + i;
                return tank == null
                        ? new HoveredFluid(FluidStack.EMPTY, 0, x, y, buttonId)
                        : new HoveredFluid(tank.getFluid(), tank.getCapacity(), x, y, buttonId);
            }
        }

        List<FluidTank> outputTanks = menu.blockEntity().outputFluidTanks();
        for (int i = 0; i < menu.fluidOutputSlots(); i++) {
            int x = leftPos + menu.outputFluidSlotX(i) - 1;
            int y = topPos + menu.outputFluidSlotY(i) - 1;
            if (inside(mouseX, mouseY, x, y, FLUID_SLOT_SIZE, FLUID_SLOT_SIZE)) {
                FluidTank tank = i < outputTanks.size() ? outputTanks.get(i) : null;
                int buttonId = SingleBlockMachineMenu.BUTTON_OUTPUT_FLUID_SLOT + i;
                return tank == null
                        ? new HoveredFluid(FluidStack.EMPTY, 0, x, y, buttonId)
                        : new HoveredFluid(tank.getFluid(), tank.getCapacity(), x, y, buttonId);
            }
        }
        return null;
    }

    private static void drawFluidLayer(GuiGraphics graphics, FluidStack stack, int x, int y, int width, int height) {
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
        if (alpha <= 0.0F) {
            alpha = 1.0F;
        }

        graphics.blit(x, y, 0, width, height, sprite, red, green, blue, alpha);
    }

    private static boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }

    private record HoveredFluid(FluidStack stack, int capacity, int x, int y, int buttonId) {
    }
}
