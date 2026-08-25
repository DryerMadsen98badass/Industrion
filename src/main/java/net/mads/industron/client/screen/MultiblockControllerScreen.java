package net.mads.industron.client.screen;

import net.mads.industron.client.gui.CEMachineGuiTextures;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockControllerBlock;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockControllerBlockEntity;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockDefinition;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockRegistry;
import net.mads.industron.menu.MultiblockControllerMenu;
import net.mads.industron.recipe.ChemicalBalanceRange;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidStack;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class MultiblockControllerScreen extends AbstractContainerScreen<MultiblockControllerMenu> {
    private static final int SPACE_BACKGROUND = 0xFF05070B;
    private static final int SPACE_EDGE = 0xFF3B4149;
    private static final int TEXT = 0xFFE8EDF2;
    private static final int MUTED = 0xFF9CA8B3;
    private static final int GREEN = 0xFF63D87C;
    private static final int RED = 0xFFF06A6A;
    private static final int BLUE = 0xFF6CA8EE;
    private static final int GOLD = 0xFFF0C56A;
    private static final int POWER_BUTTON_WIDTH = 42;
    private static final int POWER_BUTTON_HEIGHT = 14;
    private static final int INFO_X = 8;
    private static final int INFO_Y = 23;
    private static final int INFO_WIDTH = 244;
    private static final int INFO_HEIGHT = 126;
    private static final int CONTENT_PADDING = 7;
    private static final int LINE_HEIGHT = 12;
    private static final int RESOURCE_LINE_HEIGHT = 19;

    private int scrollOffset;
    private int contentHeight;

    public MultiblockControllerScreen(MultiblockControllerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 260;
        imageHeight = 250;
        titleLabelX = 8;
        titleLabelY = 6;
        inventoryLabelX = 49;
        inventoryLabelY = 154;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderPowerTooltip(graphics, mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        CEMachineGuiTextures.drawMachinePanel(graphics, x, y, imageWidth, imageHeight);
        drawSpacePanel(graphics, x + INFO_X, y + INFO_Y, INFO_WIDTH, INFO_HEIGHT);
        drawInformation(graphics, x, y, mouseX, mouseY);
        drawPowerButton(graphics, mouseX, mouseY);

        for (Slot slot : menu.slots) {
            CEMachineGuiTextures.drawItemSlot(graphics, x + slot.x - 1, y + slot.y - 1);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0
                && minecraft != null
                && minecraft.gameMode != null
                && inside(mouseX, mouseY, powerButtonX(), powerButtonY(), POWER_BUTTON_WIDTH, POWER_BUTTON_HEIGHT)) {
            minecraft.gameMode.handleInventoryButtonClick(
                    menu.containerId,
                    MultiblockControllerMenu.BUTTON_TOGGLE_MACHINE
            );
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!inside(mouseX, mouseY, leftPos + INFO_X, topPos + INFO_Y, INFO_WIDTH, INFO_HEIGHT)) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        int maximum = maximumScroll();
        if (maximum <= 0) {
            scrollOffset = 0;
            return true;
        }

        scrollOffset = Math.max(0, Math.min(maximum, scrollOffset - (int) Math.signum(scrollY) * RESOURCE_LINE_HEIGHT));
        return true;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    private void drawInformation(GuiGraphics graphics, int screenX, int screenY, int mouseX, int mouseY) {
        List<InfoLine> lines = informationLines();
        int calculatedHeight = CONTENT_PADDING;
        for (InfoLine line : lines) {
            calculatedHeight += line.height();
        }
        contentHeight = calculatedHeight + CONTENT_PADDING;
        scrollOffset = Math.min(scrollOffset, maximumScroll());

        int boxX = screenX + INFO_X;
        int boxY = screenY + INFO_Y;
        graphics.enableScissor(boxX + 1, boxY + 1, boxX + INFO_WIDTH - 1, boxY + INFO_HEIGHT - 1);

        int lineY = boxY + CONTENT_PADDING - scrollOffset;
        for (InfoLine line : lines) {
            if (line.resource() == null) {
                graphics.drawString(font, line.text(), boxX + CONTENT_PADDING, lineY, line.color(), false);
            } else {
                ItemStack icon = line.resource();
                graphics.renderItem(icon, boxX + CONTENT_PADDING, lineY - 3);
                graphics.drawString(font, line.text(), boxX + CONTENT_PADDING + 20, lineY + 1, line.color(), false);
            }
            lineY += line.height();
        }

        graphics.disableScissor();
        drawScrollBar(graphics, boxX, boxY);
    }

    private List<InfoLine> informationLines() {
        List<InfoLine> lines = new ArrayList<>();
        MultiblockControllerBlockEntity controller = menu.blockEntity();
        MultiblockDefinition definition = definition(controller);

        lines.add(InfoLine.text(title.getString(), GOLD));
        lines.add(InfoLine.spacer());
        lines.add(InfoLine.text(
                "Status: " + (menu.formed() ? "Formed" : "Unformed"),
                menu.formed() ? GREEN : RED
        ));
        lines.add(InfoLine.text(
                "Machine: " + (menu.machineEnabled() ? "Enabled" : "Disabled"),
                menu.machineEnabled() ? GREEN : RED
        ));
        if (controller != null && controller.activeRecipeId() != null) {
            String operation = controller.recipeLogic().status().name();
            lines.add(InfoLine.text("Operation: " + formatOperation(operation),
                    "WORKING".equals(operation) ? GREEN : GOLD));
        }

        if (menu.duration() > 0) {
            lines.add(InfoLine.text("Duration: " + formatSeconds(menu.duration()), TEXT));
            lines.add(InfoLine.text(
                    "Progress: " + formatSeconds(Math.min(menu.progress(), menu.duration()))
                            + " / " + formatSeconds(menu.duration()),
                    MUTED
            ));
        }

        if (definition != null) {
            if (menu.duration() <= 0 && definition.inputOnlyDisplay() != null) {
                lines.add(InfoLine.text(
                        "Duration: " + formatSeconds(definition.inputOnlyDisplay().durationTicks()),
                        TEXT
                ));
            }
            appendDriveInformation(lines, definition, controller);
        }

        if (menu.hasDurability()) {
            lines.add(InfoLine.text(
                    "Durability: " + formatHundredths(menu.durabilityHundredths())
                            + " / " + menu.maxDurability(),
                    TEXT
            ));
            if (menu.corrosionPerTickHundredths() > 0) {
                lines.add(InfoLine.text(
                        "Corrosion: -" + formatHundredths(menu.corrosionPerTickHundredths()) + "/tick",
                        RED
                ));
            }
        }

        if (menu.hasCbHatch()) {
            lines.add(InfoLine.text("CB: " + ChemicalBalanceRange.formatHundredths(menu.machineChemicalBalanceHundredths()), BLUE));
        }
        if (menu.hasSafeChemicalBalanceRange()) {
            lines.add(InfoLine.text(
                    "Safe CB: " + ChemicalBalanceRange.formatHundredths(menu.safeChemicalBalanceMinimumHundredths())
                            + " - " + ChemicalBalanceRange.formatHundredths(menu.safeChemicalBalanceMaximumHundredths()),
                    BLUE
            ));
        }
        if (controller != null && controller.formedCoilHeat() > 0) {
            lines.add(InfoLine.text("Temperature: " + controller.formedCoilHeat() + " C", GOLD));
        }
        if (menu.parallel() > 1) {
            lines.add(InfoLine.text("Parallel: x" + menu.parallel(), BLUE));
        }

        appendRecipeInformation(lines, controller, definition);
        return lines;
    }

    private void appendDriveInformation(
            List<InfoLine> lines,
            MultiblockDefinition definition,
            MultiblockControllerBlockEntity controller
    ) {
        int resource = Math.abs(menu.resourcePerTick());
        switch (definition.drive()) {
            case ELECTRIC -> lines.add(InfoLine.text(
                    "Energy Usage: " + (resource > 0 ? resource : definition.energyUsage()) + " CE/t",
                    TEXT
            ));
            case STEAM -> lines.add(InfoLine.text(
                    "Steam Usage: " + (resource > 0 ? resource : definition.steamUsage()) + " mB/t",
                    TEXT
            ));
            case KINETIC -> {
                if (controller != null) {
                    lines.add(InfoLine.text("RPM: " + controller.kineticInputRpm(), TEXT));
                }
            }
            case KINETIC_OUTPUT -> {
                if (controller != null) {
                    lines.add(InfoLine.text("Output RPM: " + controller.kineticOutputRpm(), TEXT));
                }
            }
            case NONE -> {
            }
        }
    }

    private void appendRecipeInformation(
            List<InfoLine> lines,
            MultiblockControllerBlockEntity controller,
            MultiblockDefinition definition
    ) {
        List<ItemStack> itemInputs = controller == null ? List.of() : controller.activeItemInputs();
        List<FluidStack> fluidInputs = controller == null ? List.of() : controller.activeFluidInputs();
        List<ItemStack> itemOutputs = controller == null ? List.of() : controller.activeItemOutputs();
        List<FluidStack> fluidOutputs = controller == null ? List.of() : controller.activeFluidOutputs();
        int duration = menu.duration();
        int parallel = menu.parallel();

        if (itemInputs.isEmpty() && fluidInputs.isEmpty() && itemOutputs.isEmpty() && fluidOutputs.isEmpty()
                && definition != null && definition.inputOnlyDisplay() != null) {
            MultiblockDefinition.InputOnlyDisplay display = definition.inputOnlyDisplay();
            duration = display.durationTicks();
            itemInputs = display.itemInputs().stream()
                    .map(input -> new ItemStack(BuiltInRegistries.ITEM.get(input.itemId()), input.amount()))
                    .filter(stack -> !stack.isEmpty() && !stack.is(Items.AIR))
                    .toList();
            fluidInputs = display.fluidInputs().stream()
                    .map(input -> new FluidStack(BuiltInRegistries.FLUID.get(input.fluidId()), input.amount()))
                    .filter(stack -> !stack.isEmpty())
                    .toList();
            parallel = 1;
        }

        if (!itemInputs.isEmpty() || !fluidInputs.isEmpty()) {
            lines.add(InfoLine.spacer());
            lines.add(InfoLine.text("Inputs", BLUE));
            for (ItemStack stack : itemInputs) {
                int amount = multiplyClamped(stack.getCount(), parallel);
                lines.add(itemLine(stack, amount, duration));
            }
            for (FluidStack stack : fluidInputs) {
                int amount = multiplyClamped(stack.getAmount(), parallel);
                lines.add(fluidLine(stack, amount, duration));
            }
        }

        if (!itemOutputs.isEmpty() || !fluidOutputs.isEmpty()) {
            lines.add(InfoLine.spacer());
            lines.add(InfoLine.text("Outputs", GOLD));
            for (ItemStack stack : itemOutputs) {
                lines.add(itemLine(stack, stack.getCount(), duration));
            }
            for (FluidStack stack : fluidOutputs) {
                lines.add(fluidLine(stack, stack.getAmount(), duration));
            }
        }
    }

    private InfoLine itemLine(ItemStack stack, int amount, int duration) {
        ItemStack icon = stack.copyWithCount(1);
        String rate = duration > 0 ? " " + formatRate(amount, duration) + "/sec" : "";
        return InfoLine.resource(icon, "x" + amount + " [" + stack.getHoverName().getString() + "]" + rate, TEXT);
    }

    private InfoLine fluidLine(FluidStack stack, int amount, int duration) {
        ItemStack icon = new ItemStack(stack.getFluid().getBucket());
        if (icon.isEmpty() || icon.is(Items.AIR)) {
            icon = new ItemStack(Items.BUCKET);
        }
        String rate = duration > 0 ? " " + formatRate(amount, duration) + " mB/sec" : "";
        return InfoLine.resource(icon, "x" + amount + " [" + stack.getHoverName().getString() + "]" + rate, TEXT);
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
        graphics.drawCenteredString(font, enabled ? "ON" : "OFF", x + POWER_BUTTON_WIDTH / 2, y + 3, 0xFFFFFFFF);
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

    private void drawSpacePanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, SPACE_EDGE);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, SPACE_BACKGROUND);
        for (int index = 0; index < 48; index++) {
            int dotX = x + 3 + Math.floorMod(index * 37 + 11, width - 6);
            int dotY = y + 3 + Math.floorMod(index * index * 13 + 7, height - 6);
            int color = index % 5 == 0 ? 0x99FFFFFF : 0x55FFFFFF;
            graphics.fill(dotX, dotY, dotX + 1, dotY + 1, color);
        }
    }

    private void drawScrollBar(GuiGraphics graphics, int boxX, int boxY) {
        int maximum = maximumScroll();
        if (maximum <= 0) {
            return;
        }

        int trackX = boxX + INFO_WIDTH - 4;
        int trackY = boxY + 3;
        int trackHeight = INFO_HEIGHT - 6;
        int thumbHeight = Math.max(12, trackHeight * INFO_HEIGHT / Math.max(INFO_HEIGHT, contentHeight));
        int thumbY = trackY + (trackHeight - thumbHeight) * scrollOffset / maximum;
        graphics.fill(trackX, trackY, trackX + 2, trackY + trackHeight, 0xFF30343A);
        graphics.fill(trackX, thumbY, trackX + 2, thumbY + thumbHeight, 0xFFB8C0C8);
    }

    private int maximumScroll() {
        return Math.max(0, contentHeight - INFO_HEIGHT + 2);
    }

    private int powerButtonX() {
        return leftPos + imageWidth - POWER_BUTTON_WIDTH - 8;
    }

    private int powerButtonY() {
        return topPos + 3;
    }

    private static MultiblockDefinition definition(MultiblockControllerBlockEntity controller) {
        if (controller == null || !(controller.getBlockState().getBlock() instanceof MultiblockControllerBlock block)) {
            return null;
        }
        return MultiblockRegistry.byController(block.controllerId()).orElse(null);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }

    private static int multiplyClamped(int value, int multiplier) {
        return (int) Math.min(Integer.MAX_VALUE, (long) Math.max(0, value) * Math.max(1, multiplier));
    }

    private static String formatSeconds(int ticks) {
        return BigDecimal.valueOf(ticks, 1)
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP)
                .toPlainString() + " sec";
    }

    private static String formatRate(int amount, int durationTicks) {
        return formatNumber(amount * 20.0D / Math.max(1, durationTicks));
    }

    private static String formatHundredths(long value) {
        return BigDecimal.valueOf(value, 2)
                .stripTrailingZeros()
                .toPlainString();
    }

    private static String formatNumber(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    private static String formatOperation(String operation) {
        return switch (operation) {
            case "WORKING" -> "Running";
            case "WAITING_FOR_CB" -> "Waiting for correct CB";
            case "WAITING_FOR_RPM" -> "Waiting for correct RPM";
            case "WAITING_FOR_RESOURCE" -> "Waiting for power or steam";
            case "WAITING_FOR_OUTPUT" -> "Output full";
            case "PAUSED" -> "Paused";
            default -> operation;
        };
    }

    private record InfoLine(Component text, int color, ItemStack resource, int height) {
        private static InfoLine text(String text, int color) {
            return new InfoLine(Component.literal(text), color, null, LINE_HEIGHT);
        }

        private static InfoLine resource(ItemStack icon, String text, int color) {
            return new InfoLine(Component.literal(text), color, icon, RESOURCE_LINE_HEIGHT);
        }

        private static InfoLine spacer() {
            return new InfoLine(Component.empty(), MUTED, null, 5);
        }
    }
}
