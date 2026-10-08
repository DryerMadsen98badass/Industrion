package net.mads.industron.client.screen;

import net.mads.industron.client.gui.CEMachineGuiTextures;
import net.mads.industron.machine.foundry.FoundryFluidStorage;
import net.mads.industron.menu.FoundryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Horizontal inventory rows, vertical scrolling, layered fluids and a heat gauge. */
public final class FoundryScreen extends AbstractContainerScreen<FoundryMenu> {
    private static final int TEXT = 0xFFE8EDF2, MUTED = 0xFF9CA8B3;
    private static final int GREEN = 0xFF63D87C, RED = 0xFFF06A6A, GOLD = 0xFFF0C56A;
    private static final int SCROLL_X = 177, CONTENT_Y = FoundryMenu.GRID_Y - 1;
    private static final int CONTENT_HEIGHT = FoundryMenu.VISIBLE_ROWS * 18, THUMB_HEIGHT = 14;
    private static final int TANK_X = 200, TANK_WIDTH = 48, GAUGE_X = 261, GAUGE_WIDTH = 10;
    private boolean draggingScrollbar;
    private int lastTrackStep = -1;
    private double wheelRemainder;

    public FoundryScreen(FoundryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 380;
        imageHeight = 222;
        titleLabelX = 10;
        titleLabelY = 6;
        inventoryLabelX = FoundryMenu.GRID_X;
        inventoryLabelY = 126;
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        drawMeltingProgress(graphics);
        if (renderMeltingTooltip(graphics, mouseX, mouseY)) return;
        renderTooltip(graphics, mouseX, mouseY);
        if (inside(mouseX, mouseY, TANK_X - 2, CONTENT_Y - 2, TANK_WIDTH + 4, CONTENT_HEIGHT + 4)) {
            renderTankTooltip(graphics, mouseX, mouseY);
        } else if (inside(mouseX, mouseY, GAUGE_X - 4, CONTENT_Y - 2, GAUGE_WIDTH + 8, CONTENT_HEIGHT + 4)) {
            graphics.renderComponentTooltip(font, List.of(
                    text("temperature", "Temperature: %s °C", temperature()),
                    text("safe", "Weakest brick: %s °C", menu.formed() ? menu.safeTemperature() : "—"),
                    text("heat_hint", "The white mark shows the weakest brick's limit.")), mouseX, mouseY);
        } else if (inside(mouseX, mouseY, 11, 110, 174, 11)) {
            graphics.renderTooltip(font, text("storage_hint", "One slot per inside floor block. Scroll to see more."), mouseX, mouseY);
        }
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        CEMachineGuiTextures.drawMachinePanel(graphics, leftPos, topPos, imageWidth, imageHeight);
        label(graphics, "items", "Items", 11, 22, 0xFF303030);
        label(graphics, "fluids", "Fluids", TANK_X, 22, 0xFF303030);
        label(graphics, "heat", "Heat", GAUGE_X - 5, 22, 0xFF303030);
        recess(graphics, 9, CONTENT_Y - 2, 165, CONTENT_HEIGHT + 4);
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (!slot.isActive()) continue;
            CEMachineGuiTextures.drawItemSlot(graphics, leftPos + slot.x - 1, topPos + slot.y - 1);
            if (i < FoundryMenu.PAGE_SIZE && menu.firstVisibleSlot() + i >= menu.capacity())
                graphics.fill(leftPos + slot.x, topPos + slot.y, leftPos + slot.x + 16, topPos + slot.y + 16, 0x704E3430);
        }
        drawScrollbar(graphics);
        drawTank(graphics);
        drawHeatGauge(graphics);
        label(graphics, "storage", "Storage: %s / %s slots", 11, 111, 0xFF303030, menu.occupiedSlots(), menu.capacity());
        label(graphics, "tank_total", "%s / %s mB", TANK_X, 111, 0xFF303030, number(menu.fluidAmount()), number(menu.fluidCapacity()));
        recess(graphics, 283, 32, 87, 75);
        label(graphics, menu.formed() ? "formed" : "unformed", menu.formed() ? "Formed" : "Unformed",
                290, 39, menu.formed() ? GREEN : RED);
        label(graphics, "inside_label", "Inside", 290, 57, MUTED);
        label(graphics, "inside_size", "%s × %s × %s", 290, 70, TEXT, menu.insideWidth(), menu.insideHeight(), menu.insideWidth());
        label(graphics, "row", "Row %s", 290, 91, MUTED, menu.scrollRow() + 1);
        recess(graphics, 198, 134, 172, 79);
        label(graphics, "temperature", "Temperature: %s °C", 206, 141, menu.overheating() ? RED : GOLD, temperature());
        label(graphics, "safe", "Weakest brick: %s °C", 206, 155, MUTED, menu.formed() ? menu.safeTemperature() : "—");
        label(graphics, "damage", "Damaged bricks: %s", 206, 169, menu.damagedBricks() > 0 ? RED : MUTED, menu.damagedBricks());
        label(graphics, "worst", "Most damaged: %s%%", 206, 183, MUTED, menu.mostDamagedPercent());
        String state = menu.overheating() ? "Overheating" : menu.receivingHeat() ? "Heating"
                : menu.temperature() > 20.1 ? "Cooling" : "At ambient temperature";
        label(graphics, "state_" + state.replace(' ', '_').toLowerCase(Locale.ROOT), state, 206, 199,
                menu.overheating() ? RED : menu.receivingHeat() ? GOLD : GREEN);
    }

    private void drawMeltingProgress(GuiGraphics graphics) {
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 300);
        for (int i = 0; i < FoundryMenu.PAGE_SIZE; i++) {
            Slot slot = menu.slots.get(i);
            int status = menu.meltStatus(i);
            if (!slot.isActive() || !slot.hasItem() || status == 0) continue;
            int x = leftPos + slot.x + 16, y = topPos + slot.y;
            int height = (int) Math.min(16, 16L * menu.meltTicks(i) / Math.max(1, menu.meltDuration(i)));
            graphics.fill(x, y, x + 2, y + 16, 0xFF333333);
            int color = status == 3 ? RED : status == 2 ? 0xFFFFAB42 : 0xFF6AA9CC;
            if (height > 0) graphics.fill(x, y + 16 - height, x + 2, y + 16, color);
            else graphics.fill(x, y + 15, x + 2, y + 16, color);
        }
        graphics.pose().popPose();
    }

    private boolean renderMeltingTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        for (int i = 0; i < FoundryMenu.PAGE_SIZE; i++) {
            Slot slot = menu.slots.get(i);
            int status = menu.meltStatus(i);
            if (!slot.isActive() || !slot.hasItem() || status == 0
                    || !inside(mouseX, mouseY, slot.x + 15, slot.y, 3, 16)) continue;
            int duration = Math.max(1, menu.meltDuration(i));
            List<Component> lines = new ArrayList<>();
            lines.add(text("melting_progress", "Melting: %s%%", Math.min(100, 100L * menu.meltTicks(i) / duration)));
            if (status == 1) lines.add(text("melting_heat", "Waiting for %s °C", menu.meltTemperature(i)));
            else if (status == 3) lines.add(text("melting_blocked", "Waiting for room in the fluid tank"));
            else if (status == 4) lines.add(text("melting_input", "Waiting for more input items"));
            else lines.add(text("melting_remaining", "%s s remaining", String.format(Locale.ROOT, "%.1f",
                    Math.max(0, duration - menu.meltTicks(i)) / 20.0)));
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
            return true;
        }
        return false;
    }

    private void drawScrollbar(GuiGraphics graphics) {
        recess(graphics, SCROLL_X, CONTENT_Y, 12, CONTENT_HEIGHT);
        int offset = menu.maximumScrollRow() == 0 ? 0 : (int) Math.round((double) menu.scrollRow()
                / menu.maximumScrollRow() * (CONTENT_HEIGHT - THUMB_HEIGHT - 2));
        int x = leftPos + SCROLL_X + 2, y = topPos + CONTENT_Y + 1 + offset;
        graphics.fill(x, y, x + 8, y + THUMB_HEIGHT, menu.maximumScrollRow() > 0 ? 0xFFC6C6C6 : 0xFF60666C);
        graphics.fill(x, y, x + 8, y + 1, TEXT);
        graphics.fill(x + 2, y + 5, x + 6, y + 6, 0xFF555B63);
        graphics.fill(x + 2, y + 8, x + 6, y + 9, 0xFF555B63);
    }

    private void drawTank(GuiGraphics graphics) {
        recess(graphics, TANK_X - 2, CONTENT_Y - 2, TANK_WIDTH + 4, CONTENT_HEIGHT + 4);
        for (FluidBand band : fluidBands()) {
            int x = leftPos + TANK_X, y = topPos + band.top();
            graphics.enableScissor(x, y, x + TANK_WIDTH, y + band.height());
            for (int dy = 0; dy < band.height(); dy += 16)
                for (int dx = 0; dx < TANK_WIDTH; dx += 16)
                    CEMachineGuiTextures.drawFluid(graphics, band.fluid(), x + dx, y + dy);
            graphics.disableScissor();
        }
        for (int tick = 1; tick < 4; tick++) {
            int y = topPos + CONTENT_Y + tick * CONTENT_HEIGHT / 4;
            graphics.fill(leftPos + TANK_X + TANK_WIDTH - 5, y, leftPos + TANK_X + TANK_WIDTH, y + 1, 0xA0FFFFFF);
        }
    }

    private List<FluidBand> fluidBands() {
        List<FluidStack> fluids = menu.fluids();
        List<FluidBand> bands = new ArrayList<>();
        long denominator = Math.max(1, Math.max(menu.fluidCapacity(), menu.fluidAmount())), accumulated = 0;
        int bottom = CONTENT_Y + CONTENT_HEIGHT;
        for (int i = 0; i < fluids.size(); i++) {
            accumulated += fluids.get(i).getAmount();
            int top = CONTENT_Y + CONTENT_HEIGHT - (int) (accumulated * CONTENT_HEIGHT / denominator);
            if (top < bottom) bands.add(new FluidBand(i, fluids.get(i), top, bottom - top));
            bottom = top;
        }
        return bands;
    }

    private void drawHeatGauge(GuiGraphics graphics) {
        recess(graphics, GAUGE_X - 2, CONTENT_Y - 2, GAUGE_WIDTH + 4, CONTENT_HEIGHT + 4);
        double scale = Math.max(500.0, Math.ceil(Math.max(menu.temperature(), menu.safeTemperature()) / 500.0) * 500.0);
        int height = Math.max(0, Math.min(CONTENT_HEIGHT, (int) Math.round(menu.temperature() / scale * CONTENT_HEIGHT)));
        graphics.fillGradient(leftPos + GAUGE_X, topPos + CONTENT_Y + CONTENT_HEIGHT - height,
                leftPos + GAUGE_X + GAUGE_WIDTH, topPos + CONTENT_Y + CONTENT_HEIGHT,
                menu.overheating() ? 0xFFFF4433 : 0xFFFFCA66, 0xFFB83C24);
        if (menu.formed()) {
            int marker = CONTENT_Y + CONTENT_HEIGHT - (int) Math.round(menu.safeTemperature() / scale * (CONTENT_HEIGHT - 1));
            graphics.fill(leftPos + GAUGE_X - 1, topPos + marker, leftPos + GAUGE_X + GAUGE_WIDTH + 1, topPos + marker + 1, TEXT);
        }
    }

    private void renderTankTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        List<Component> lines = new ArrayList<>();
        FluidBand hovered = inside(mouseX, mouseY, TANK_X, CONTENT_Y, TANK_WIDTH, CONTENT_HEIGHT)
                ? fluidBands().stream().filter(b -> mouseY >= topPos + b.top()
                    && mouseY < topPos + b.top() + b.height()).findFirst().orElse(null) : null;
        if (hovered != null) {
            lines.add(hovered.fluid().getHoverName());
            var ratio = hovered.fluid().get(net.mads.industron.machine.foundry.FoundryComponents.COMPOSITION.get());
            if (ratio != null) {
                lines.add(Component.literal("Unidentified mixture - cannot be cast"));
                ratio.weights().forEach((id, weight) -> {
                    var material = net.mads.industron.material.MaterialCatalog.find(id);
                    double percentage = ratio.share(id) * 100.0;
                    lines.add(Component.literal((material == null ? id : material.displayName())
                            + String.format(java.util.Locale.ROOT, ": %.2f%%", percentage)));
                });
            }
            lines.add(text("fluid_amount", "%s mB", number(hovered.fluid().getAmount())));
        } else {
            List<FluidStack> fluids = menu.fluids();
            lines.add(text("tank", "Foundry tank"));
            if (fluids.isEmpty()) lines.add(text("empty", "Empty"));
            for (FluidStack fluid : fluids)
                lines.add(fluid.getHoverName().copy().append(" — " + number(fluid.getAmount()) + " mB"));
        }
        lines.add(text("tank_total", "%s / %s mB", number(menu.fluidAmount()), number(menu.fluidCapacity())));
        lines.add(text("tank_hint", "Click with a fluid container to fill or drain."));
        graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!inside(mouseX, mouseY, 9, CONTENT_Y - 2, 180, CONTENT_HEIGHT + 4))
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        if (!menu.getCarried().isEmpty() || menu.maximumScrollRow() == 0) return true;
        wheelRemainder += scrollY;
        int steps = (int) wheelRemainder;
        wheelRemainder -= steps;
        for (int i = 0; i < Math.min(20, Math.abs(steps)); i++) sendButton(steps > 0 ? FoundryMenu.SCROLL_UP : FoundryMenu.SCROLL_DOWN);
        return true;
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && inside(mouseX, mouseY, SCROLL_X, CONTENT_Y, 12, CONTENT_HEIGHT)) {
            if (menu.maximumScrollRow() > 0 && menu.getCarried().isEmpty()) {
                draggingScrollbar = true;
                lastTrackStep = -1;
                moveScrollbar(mouseY);
            }
            return true;
        }
        if (button == 0 && inside(mouseX, mouseY, TANK_X, CONTENT_Y, TANK_WIDTH, CONTENT_HEIGHT)) {
            int layer = fluidBands().stream().filter(b -> mouseY >= topPos + b.top()
                    && mouseY < topPos + b.top() + b.height()).mapToInt(FluidBand::index).findFirst()
                    .orElse(FoundryFluidStorage.MAX_FLUID_TYPES);
            sendButton(FoundryMenu.FLUID_CLICK_BASE + layer);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScrollbar && button == 0) { moveScrollbar(mouseY); return true; }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingScrollbar && button == 0) { draggingScrollbar = false; return true; }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void moveScrollbar(double mouseY) {
        double fraction = (mouseY - topPos - CONTENT_Y - 1 - THUMB_HEIGHT / 2.0) / (CONTENT_HEIGHT - THUMB_HEIGHT - 2.0);
        int step = (int) Math.round(Math.max(0.0, Math.min(1.0, fraction)) * FoundryMenu.SCROLL_TRACK_STEPS);
        if (step != lastTrackStep) { sendButton(FoundryMenu.SCROLL_TRACK_BASE + step); lastTrackStep = step; }
    }
    private void sendButton(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }
    private void recess(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(leftPos + x, topPos + y, leftPos + x + width, topPos + y + height, 0xFF3B4149);
        graphics.fill(leftPos + x + 1, topPos + y + 1, leftPos + x + width - 1, topPos + y + height - 1, 0xFF10161C);
    }
    private static Component text(String key, String fallback, Object... args) {
        return Component.translatableWithFallback("gui.industron.foundry." + key, fallback, args);
    }
    private void label(GuiGraphics graphics, String key, String fallback, int x, int y, int color, Object... args) {
        graphics.drawString(font, text(key, fallback, args), leftPos + x, topPos + y, color, false);
    }
    private static String number(int amount) { return String.format(Locale.ROOT, "%,d", amount); }
    private String temperature() { return String.format(Locale.ROOT, "%.1f", menu.temperature()); }
    private boolean inside(double x, double y, int rx, int ry, int width, int height) {
        return x >= leftPos + rx && x < leftPos + rx + width && y >= topPos + ry && y < topPos + ry + height;
    }
    private record FluidBand(int index, FluidStack fluid, int top, int height) {}
}
