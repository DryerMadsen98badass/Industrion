package net.mads.industron.client.screen;

import net.mads.industron.machine.control.MachineControlSchedule;
import net.mads.industron.recipe.ChemicalBalanceRange;
import net.mads.industron.menu.MachineControlScheduleMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class MachineControlScheduleScreen extends AbstractContainerScreen<MachineControlScheduleMenu> {
    private static final int SIDEBAR_WIDTH = 108;
    private static final int SETTINGS_WIDTH = 154;
    private static final int HEADER_HEIGHT = 20;
    private static final int PALETTE_ROW_HEIGHT = 17;
    private static final int PORT_SPACING = 11;
    private static final int BOOLEAN_INPUT = 0xFF43C66D;
    private static final int BOOLEAN_OUTPUT = 0xFFE65151;
    private static final int NUMBER_INPUT = 0xFF55D7E8;
    private static final int NUMBER_OUTPUT = 0xFFFFA943;

    private MachineControlSchedule.Category category = MachineControlSchedule.Category.CONTROL;
    private int paletteScroll;
    private int settingsScroll;
    private double cameraX;
    private double cameraY;
    private double zoom = 1.0;
    private int draggingNode = -1;
    private int dragOffsetX;
    private int dragOffsetY;
    private boolean panning;
    private double lastMouseX;
    private double lastMouseY;
    private int wireSource = -1;
    private MachineControlSchedule.PortKind wireKind;
    private PaletteEntry draggingPalette;
    private int selectedNodeId = -1;
    private int selectedVariableId = -1;
    private EditBox editor;
    private EditMode editMode = EditMode.NONE;
    private int editingId = -1;

    public MachineControlScheduleScreen(MachineControlScheduleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 440;
        imageHeight = 270;
        inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        imageWidth = Math.max(1, Math.min(480, width - 12));
        imageHeight = Math.max(1, Math.min(300, height - 12));
        super.init();
        editor = new EditBox(font, leftPos + SIDEBAR_WIDTH + 10, topPos + 27, 150, 17, Component.empty());
        editor.setVisible(false);
        editor.setMaxLength(256);
        addRenderableWidget(editor);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        suppressExternalWidgets();
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, width, height, 0xE8080B0F);
        super.render(graphics, mouseX, mouseY, partialTick);
        enableWorkspaceScissor(graphics);
        if (draggingPalette != null && workspaceContains(mouseX, mouseY)) drawGhostNode(graphics, mouseX, mouseY, draggingPalette);
        if (wireSource >= 0) {
            MachineControlSchedule.Node source = menu.clientSchedule().node(wireSource);
            if (source != null) drawWire(graphics, outputX(source, wireKind), outputY(source, wireKind), mouseX, mouseY, wireKind);
        }
        graphics.disableScissor();
        renderTooltip(graphics, mouseX, mouseY);
    }

    private void suppressExternalWidgets() {
        if (editor == null) return;
        boolean focused = editor.isFocused();
        clearWidgets();
        addRenderableWidget(editor);
        if (focused) {
            editor.setFocused(true);
            setFocused(editor);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF14181E);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + HEADER_HEIGHT, 0xFF252B33);
        graphics.fill(leftPos, topPos + HEADER_HEIGHT, leftPos + SIDEBAR_WIDTH, topPos + imageHeight, 0xFF1D2229);
        graphics.fill(leftPos + SIDEBAR_WIDTH, topPos + HEADER_HEIGHT, leftPos + imageWidth, topPos + imageHeight, 0xFF101419);
        drawGrid(graphics);
        enableWorkspaceScissor(graphics);
        drawConnections(graphics);
        for (MachineControlSchedule.Node node : menu.clientSchedule().nodes()) drawNode(graphics, node, mouseX, mouseY);
        graphics.disableScissor();

        // Sidebars and header-adjacent UI are deliberately rendered after the workspace.
        // This keeps every node, port and wire visually behind the outer GUI panels.
        drawSidebar(graphics, mouseX, mouseY);
        if (settingsOpen()) drawSettingsPanel(graphics, mouseX, mouseY);
        drawStatus(graphics);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 7, 6, 0xFFF2F5F7, false);
        String status = menu.itemMode() ? "Item editor" : "Input " + menu.redstoneInput() + "  Output " + menu.redstoneOutput();
        graphics.drawString(font, status, imageWidth - font.width(status) - 7, 6, 0xFFB8C1CA, false);
    }

    private void drawGrid(GuiGraphics graphics) {
        int left = workspaceLeft();
        int right = settingsOpen() ? settingsLeft() : leftPos + imageWidth;
        int top = topPos + HEADER_HEIGHT;
        int spacing = Math.max(8, (int) Math.round(16 * zoom));
        int offsetX = Math.floorMod((int) Math.round(-cameraX * zoom), spacing);
        int offsetY = Math.floorMod((int) Math.round(-cameraY * zoom), spacing);
        for (int x = left + offsetX; x < right; x += spacing) graphics.fill(x, top, x + 1, topPos + imageHeight, 0x221E2A32);
        for (int y = top + offsetY; y < topPos + imageHeight; y += spacing) graphics.fill(left, y, right, y + 1, 0x221E2A32);
    }

    private void drawSidebar(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = leftPos + 4;
        int y = topPos + HEADER_HEIGHT + 4;
        int tabWidth = 24;
        MachineControlSchedule.Category[] categories = MachineControlSchedule.Category.values();
        for (int i = 0; i < categories.length; i++) {
            int tx = x + (i % 4) * (tabWidth + 1);
            int ty = y + (i / 4) * 15;
            graphics.fill(tx, ty, tx + tabWidth, ty + 13, categories[i] == category ? 0xFF596978 : 0xFF333B44);
            graphics.drawCenteredString(font, categoryShort(categories[i]), tx + tabWidth / 2, ty + 3, 0xFFFFFFFF);
        }

        int listTop = y + 33;
        List<PaletteEntry> entries = paletteEntries();
        int bottomReserve = category == MachineControlSchedule.Category.VARIABLES ? 22 : 4;
        int visible = Math.max(1, (imageHeight - HEADER_HEIGHT - 39 - bottomReserve) / PALETTE_ROW_HEIGHT);
        paletteScroll = Math.max(0, Math.min(paletteScroll, Math.max(0, entries.size() - visible)));
        for (int i = 0; i < visible && i + paletteScroll < entries.size(); i++) {
            PaletteEntry entry = entries.get(i + paletteScroll);
            int rowY = listTop + i * PALETTE_ROW_HEIGHT;
            int color = entry.variableId >= 0 ? 0xFFB36B20 : nodeColor(entry.type);
            boolean hovered = inside(mouseX, mouseY, x, rowY, SIDEBAR_WIDTH - 8, 14);
            graphics.fill(x, rowY, x + SIDEBAR_WIDTH - 8, rowY + 14, hovered ? lighten(color) : color);
            String value = entry.variableId >= 0 && menu.clientSchedule().variable(entry.variableId) != null
                    ? " = " + MachineControlSchedule.formatNumber(menu.variableValue(entry.variableId)) : "";
            graphics.drawString(font, trim(entry.label + value, SIDEBAR_WIDTH - 16), x + 4, rowY + 3, 0xFFFFFFFF, false);
        }

        if (category == MachineControlSchedule.Category.VARIABLES) {
            int plusY = topPos + imageHeight - 19;
            graphics.fill(x, plusY, x + SIDEBAR_WIDTH - 8, plusY + 15, 0xFFB36B20);
            graphics.drawString(font, "+ variable", x + 4, plusY + 4, 0xFFFFFFFF, false);
        }
    }

    private List<PaletteEntry> paletteEntries() {
        List<PaletteEntry> result = new ArrayList<>();
        for (MachineControlSchedule.NodeType type : MachineControlSchedule.NodeType.values()) {
            if (type.category() == category && type.paletteVisible() && type != MachineControlSchedule.NodeType.VARIABLE_REPORTER) {
                result.add(new PaletteEntry(type, -1, label(type)));
            }
        }
        if (category == MachineControlSchedule.Category.VARIABLES) {
            for (MachineControlSchedule.Variable variable : menu.clientSchedule().variables()) {
                result.add(new PaletteEntry(MachineControlSchedule.NodeType.VARIABLE_REPORTER, variable.id(), variable.name()));
            }
        }
        return result;
    }

    private void drawNode(GuiGraphics graphics, MachineControlSchedule.Node node, int mouseX, int mouseY) {
        int screenX = nodeScreenX(node);
        int screenY = nodeScreenY(node);
        int baseWidth = baseNodeWidth(node);
        int baseHeight = baseNodeHeight(node);
        boolean hovered = inside(mouseX, mouseY, screenX, screenY, scaled(baseWidth), scaled(baseHeight));

        graphics.pose().pushPose();
        graphics.pose().translate(screenX, screenY, 0);
        graphics.pose().scale((float) zoom, (float) zoom, 1);

        graphics.fill(0, 0, baseWidth, baseHeight, hovered || node.id() == selectedNodeId ? 0xFFFFFFFF : 0xFF9DA8B2);
        graphics.fill(1, 1, baseWidth - 1, baseHeight - 1, nodeColor(node.type()));
        graphics.drawString(font, trim(nodeTitle(node), Math.max(18, baseWidth - 10)), 5, 4, 0xFFFFFFFF, false);
        drawNodeLiveValue(graphics, node, baseWidth, baseHeight);

        for (MachineControlSchedule.PortKind kind : MachineControlSchedule.PortKind.values()) {
            for (int slot = 0; slot < node.visibleInputSlots(kind); slot++) {
                int px = -2;
                int py = inputLocalY(node, kind, slot);
                boolean connected = node.connectionAt(kind, slot) != null;
                drawPort(graphics, px, py, kind, true, connected);
                graphics.drawString(font, Integer.toString(slot + 1), px + 6, py - 4, 0xFFCDD4DB, false);
            }
        }
        if (node.provides(MachineControlSchedule.PortKind.BOOLEAN)) {
            drawPort(graphics, baseWidth, outputLocalY(node, MachineControlSchedule.PortKind.BOOLEAN), MachineControlSchedule.PortKind.BOOLEAN, false, false);
        }
        if (node.provides(MachineControlSchedule.PortKind.NUMBER)) {
            drawPort(graphics, baseWidth, outputLocalY(node, MachineControlSchedule.PortKind.NUMBER), MachineControlSchedule.PortKind.NUMBER, false, false);
        }
        graphics.pose().popPose();
    }

    private void drawNodeLiveValue(GuiGraphics graphics, MachineControlSchedule.Node node, int width, int height) {
        String value = liveValue(node);
        if (value.isEmpty()) return;
        int badgeWidth = Math.min(width - 8, font.width(value) + 6);
        int bx = width - badgeWidth - 4;
        int by = height - 11;
        graphics.fill(bx, by, bx + badgeWidth, by + 9, 0xAA11161C);
        graphics.drawCenteredString(font, trim(value, badgeWidth - 4), bx + badgeWidth / 2, by + 1, 0xFFFFFFFF);
    }

    private String liveValue(MachineControlSchedule.Node node) {
        String bool = "";
        String number = "";
        if (node.provides(MachineControlSchedule.PortKind.BOOLEAN)) {
            if (menu.hasBooleanValue(node.id())) bool = menu.booleanValue(node.id()) ? "true" : "false";
            else if (menu.itemMode()) bool = localBooleanValue(node);
            else bool = "?";
        }
        if (node.provides(MachineControlSchedule.PortKind.NUMBER)) {
            if (node.type() == MachineControlSchedule.NodeType.NUMBER) number = MachineControlSchedule.formatNumber(node.value());
            else if (node.type() == MachineControlSchedule.NodeType.VARIABLE_REPORTER) {
                MachineControlSchedule.Variable variable = menu.clientSchedule().variable(node.variableId());
                number = variable == null ? "?" : MachineControlSchedule.formatNumber(menu.variableValue(variable.id()));
            } else if (menu.hasNumberValue(node.id())) {
                int value = menu.numberValue(node.id());
                number = MachineControlSchedule.formatNumber(value);
            } else if (menu.itemMode()) number = localNumberValue(node);
            else number = "?";
        }
        if (!bool.isEmpty() && !number.isEmpty()) return bool + " | " + number;
        return !bool.isEmpty() ? bool : number;
    }

    private static boolean isCbNode(MachineControlSchedule.NodeType type) {
        return type == MachineControlSchedule.NodeType.RECIPE_MIN_CB
                || type == MachineControlSchedule.NodeType.RECIPE_MAX_CB
                || type == MachineControlSchedule.NodeType.MACHINE_CB;
    }

    private String localBooleanValue(MachineControlSchedule.Node node) {
        if (node.type() == MachineControlSchedule.NodeType.VARIABLE_REPORTER) {
            MachineControlSchedule.Variable variable = menu.clientSchedule().variable(node.variableId());
            return variable != null && variable.value() != 0 ? "true" : "false";
        }
        return switch (node.type()) {
            case MACHINE_ENABLED -> node.inputs().isEmpty() ? "true" : "false";
            case REDSTONE_INPUT, INPUT_ENERGY, INPUT_STEAM, INPUT_ITEMS, INPUT_FLUIDS,
                 MACHINE_RUNNING, HAS_ACTIVE_RECIPE, MISSING_ENERGY, OUTPUT_BLOCKED, MISSING_INPUT,
                 IS_RAINING, IS_THUNDERING, HAS_DIRECT_SKY, IS_DAY -> "false";
            default -> "?";
        };
    }

    private String localNumberValue(MachineControlSchedule.Node node) {
        if (isCbNode(node.type())) return MachineControlSchedule.formatNumber(ChemicalBalanceRange.NEUTRAL_HUNDREDTHS);
        if (node.type() == MachineControlSchedule.NodeType.REDSTONE_OUTPUT) return MachineControlSchedule.formatNumber(node.value());
        if (node.type() == MachineControlSchedule.NodeType.SET_VARIABLE && node.variableId() >= 0) {
            MachineControlSchedule.Variable variable = menu.clientSchedule().variable(node.variableId());
            return variable == null ? "?" : MachineControlSchedule.formatNumber(variable.value());
        }
        return "0";
    }

    private void drawConnections(GuiGraphics graphics) {
        for (MachineControlSchedule.Node target : menu.clientSchedule().nodes()) {
            for (MachineControlSchedule.Connection connection : target.inputs()) {
                if (connection.sourceNodeId() < 0) continue;
                MachineControlSchedule.Node source = menu.clientSchedule().node(connection.sourceNodeId());
                if (source == null) continue;
                drawWire(graphics, outputX(source, connection.kind()), outputY(source, connection.kind()),
                        inputX(target, connection.kind(), connection.targetSlot()), inputY(target, connection.kind(), connection.targetSlot()), connection.kind());
            }
        }
    }

    private void drawWire(GuiGraphics graphics, int x1, int y1, int x2, int y2, MachineControlSchedule.PortKind kind) {
        int color = kind == MachineControlSchedule.PortKind.BOOLEAN ? BOOLEAN_OUTPUT : NUMBER_OUTPUT;
        int middle = x1 + Math.max(8, (x2 - x1) / 2);
        horizontal(graphics, x1, middle, y1, color);
        vertical(graphics, middle, y1, y2, color);
        horizontal(graphics, middle, x2, y2, color);
    }

    private void drawSettingsPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = settingsLeft();
        int top = topPos + HEADER_HEIGHT;
        graphics.fill(x, top, leftPos + imageWidth, topPos + imageHeight, 0xFA181E25);
        graphics.fill(x, top, leftPos + imageWidth, top + 18, 0xFF303944);
        String title = selectedNodeId >= 0 ? nodeTitle(menu.clientSchedule().node(selectedNodeId)) : variableTitle();
        graphics.drawString(font, trim(title, SETTINGS_WIDTH - 28), x + 6, top + 5, 0xFFFFFFFF, false);
        graphics.drawString(font, "x", leftPos + imageWidth - 12, top + 5, 0xFFFF8A8A, false);

        List<SettingsEntry> entries = settingsEntries();
        int listTop = top + 21;
        int visible = Math.max(1, (imageHeight - HEADER_HEIGHT - 25) / 14);
        settingsScroll = Math.max(0, Math.min(settingsScroll, Math.max(0, entries.size() - visible)));
        for (int i = 0; i < visible && i + settingsScroll < entries.size(); i++) {
            SettingsEntry entry = entries.get(i + settingsScroll);
            int rowY = listTop + i * 14;
            boolean hovered = inside(mouseX, mouseY, x + 3, rowY, SETTINGS_WIDTH - 6, 13);
            graphics.fill(x + 3, rowY, leftPos + imageWidth - 3, rowY + 13, hovered && entry.action != null ? 0xFF3B4652 : 0xFF232A32);
            graphics.drawString(font, trim(entry.label, SETTINGS_WIDTH - 14), x + 6, rowY + 3, entry.color, false);
        }
    }

    private List<SettingsEntry> settingsEntries() {
        if (selectedVariableId >= 0) return variableSettingsEntries();
        MachineControlSchedule.Node node = menu.clientSchedule().node(selectedNodeId);
        List<SettingsEntry> entries = new ArrayList<>();
        if (node == null) return entries;

        entries.add(info("Live: " + liveValue(node)));
        if (node.type() == MachineControlSchedule.NodeType.NUMBER || node.type() == MachineControlSchedule.NodeType.REDSTONE_OUTPUT) {
            entries.add(action("Set value: " + MachineControlSchedule.formatNumber(node.value()), () -> beginEdit(EditMode.NODE_VALUE, node.id(), MachineControlSchedule.formatNumber(node.value()))));
        }
        if (node.type() == MachineControlSchedule.NodeType.REDSTONE_INPUT) {
            entries.add(info("Input behavior"));
            entries.add(option("Any signal", node.mode() == MachineControlSchedule.NodeMode.ANY,
                    () -> sendAction(MachineControlScheduleMenu.setModeAction(node.id(), MachineControlSchedule.NodeMode.ANY))));
            entries.add(option("Exact signal", node.mode() == MachineControlSchedule.NodeMode.EXACT,
                    () -> sendAction(MachineControlScheduleMenu.setModeAction(node.id(), MachineControlSchedule.NodeMode.EXACT))));
            if (node.mode() == MachineControlSchedule.NodeMode.EXACT) {
                entries.add(action("Exact value: " + MachineControlSchedule.formatNumber(node.value()), () -> beginEdit(EditMode.NODE_VALUE, node.id(), MachineControlSchedule.formatNumber(node.value()))));
            }
        }
        if (node.type() == MachineControlSchedule.NodeType.INPUT_ENERGY || node.type() == MachineControlSchedule.NodeType.INPUT_STEAM) {
            entries.add(info("Number output"));
            entries.add(option("Amount", node.mode() == MachineControlSchedule.NodeMode.AMOUNT,
                    () -> sendAction(MachineControlScheduleMenu.setModeAction(node.id(), MachineControlSchedule.NodeMode.AMOUNT))));
            entries.add(option("Percent 0-255", node.mode() == MachineControlSchedule.NodeMode.PERCENT,
                    () -> sendAction(MachineControlScheduleMenu.setModeAction(node.id(), MachineControlSchedule.NodeMode.PERCENT))));
        }
        if (node.type() == MachineControlSchedule.NodeType.INPUT_ITEMS || node.type() == MachineControlSchedule.NodeType.INPUT_FLUIDS) {
            entries.add(info("IDs and tags use OR"));
            entries.add(info("Example: minecraft:stone, #c:ores"));
            entries.add(action("Edit filters...", () -> beginEdit(EditMode.NODE_TEXT, node.id(), node.textValue())));
            if (!node.textValue().isEmpty()) entries.add(action("Clear filters", () -> setNodeText(node.id(), "")));
        }
        if (node.type() == MachineControlSchedule.NodeType.COMPARE) {
            entries.add(info("Comparison"));
            for (MachineControlSchedule.Operation operation : comparisonOperations()) {
                entries.add(option(operationLabel(operation), node.operation() == operation,
                        () -> sendAction(MachineControlScheduleMenu.setOperationAction(node.id(), operation))));
            }
        }
        if (node.type() == MachineControlSchedule.NodeType.MATH) {
            entries.add(info("Math operation"));
            for (MachineControlSchedule.Operation operation : mathOperations()) {
                entries.add(option(operationLabel(operation), node.operation() == operation,
                        () -> sendAction(MachineControlScheduleMenu.setOperationAction(node.id(), operation))));
            }
        }
        if (node.type() == MachineControlSchedule.NodeType.SET_VARIABLE) {
            entries.add(info("Variable"));
            for (MachineControlSchedule.Variable variable : menu.clientSchedule().variables()) {
                entries.add(option(variable.name(), node.variableId() == variable.id(),
                        () -> sendAction(MachineControlScheduleMenu.setNodeVariableAction(node.id(), variable.id()))));
            }
        }

        entries.add(info("Connections"));
        for (MachineControlSchedule.Connection connection : node.inputs()) {
            String source = connection.sourceNodeId() >= 0 ? nodeTitle(menu.clientSchedule().node(connection.sourceNodeId())) : "variable";
            String shape = connection.kind() == MachineControlSchedule.PortKind.BOOLEAN ? "circle" : "square";
            int slot = connection.targetSlot();
            entries.add(action("Remove " + shape + " " + (slot + 1) + " <- " + source,
                    () -> sendAction(MachineControlScheduleMenu.disconnectInputAction(node.id(), connection.kind(), slot))));
        }
        for (MachineControlSchedule.OutgoingConnection outgoing : menu.clientSchedule().outgoingConnections(node.id())) {
            MachineControlSchedule.Node target = menu.clientSchedule().node(outgoing.targetNodeId());
            entries.add(action("Remove -> " + nodeTitle(target) + " input " + (outgoing.targetSlot() + 1),
                    () -> sendAction(MachineControlScheduleMenu.disconnectInputAction(outgoing.targetNodeId(), outgoing.kind(), outgoing.targetSlot()))));
        }
        entries.add(action("Disconnect all", () -> sendAction(MachineControlScheduleMenu.disconnectAllAction(node.id()))));
        entries.add(danger("Delete block", () -> {
            sendAction(MachineControlScheduleMenu.removeNodeAction(node.id()));
            closeSettings();
        }));
        return entries;
    }

    private List<SettingsEntry> variableSettingsEntries() {
        List<SettingsEntry> entries = new ArrayList<>();
        MachineControlSchedule.Variable variable = menu.clientSchedule().variable(selectedVariableId);
        if (variable == null) return entries;
        int currentValue = menu.variableValue(variable.id());
        entries.add(info("Current value: " + currentValue));
        entries.add(action("Rename...", () -> beginEdit(EditMode.RENAME_VARIABLE, variable.id(), variable.name())));
        entries.add(action("Set value...", () -> beginEdit(EditMode.VARIABLE_VALUE, variable.id(), Integer.toString(currentValue))));
        long uses = menu.clientSchedule().nodes().stream().filter(node -> node.variableId() == variable.id()).count();
        entries.add(info("Used by " + uses + " blocks"));
        entries.add(danger("Delete variable", () -> {
            sendAction(MachineControlScheduleMenu.deleteVariableAction(variable.id()));
            closeSettings();
        }));
        return entries;
    }

    private boolean handleSettingsClick(double mouseX, double mouseY, int button) {
        if (!settingsOpen() || mouseX < settingsLeft()) return false;
        if (button != 0) return true;
        if (inside(mouseX, mouseY, leftPos + imageWidth - 18, topPos + HEADER_HEIGHT, 18, 18)) {
            closeSettings();
            return true;
        }
        List<SettingsEntry> entries = settingsEntries();
        int index = ((int) mouseY - (topPos + HEADER_HEIGHT + 21)) / 14 + settingsScroll;
        if (index >= 0 && index < entries.size() && entries.get(index).action != null) entries.get(index).action.run();
        return true;
    }

    private void drawStatus(GuiGraphics graphics) {
        int y = topPos + imageHeight - 12;
        graphics.drawString(font, "Zoom " + (int) Math.round(zoom * 100) + "%", workspaceLeft() + 5, y, 0xFF9EABB7, false);
        if (!menu.itemMode()) graphics.drawString(font, menu.result() ? "Machine ON" : "Machine OFF", leftPos + imageWidth - 70, y,
                menu.result() ? 0xFF62D87A : 0xFFE05D5D, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (editor.isVisible()) return super.mouseClicked(mouseX, mouseY, button);
        if (handleSettingsClick(mouseX, mouseY, button)) return true;

        if (mouseY >= topPos + HEADER_HEIGHT + 4 && mouseY < topPos + HEADER_HEIGHT + 34 && mouseX < leftPos + SIDEBAR_WIDTH) {
            int column = ((int) mouseX - leftPos - 4) / 25;
            int row = ((int) mouseY - topPos - HEADER_HEIGHT - 4) / 15;
            int index = row * 4 + column;
            if (index >= 0 && index < MachineControlSchedule.Category.values().length) {
                category = MachineControlSchedule.Category.values()[index];
                paletteScroll = 0;
                return true;
            }
        }

        if (category == MachineControlSchedule.Category.VARIABLES
                && inside(mouseX, mouseY, leftPos + 4, topPos + imageHeight - 19, SIDEBAR_WIDTH - 8, 15)) {
            if (button == 0) beginEdit(EditMode.NEW_VARIABLE, -1, "");
            return true;
        }

        PaletteEntry palette = paletteAt(mouseX, mouseY);
        if (palette != null) {
            if (button == 1 && palette.variableId >= 0) {
                selectedVariableId = palette.variableId;
                selectedNodeId = -1;
                settingsScroll = 0;
                return true;
            }
            if (button == 0) {
                draggingPalette = palette;
                return true;
            }
        }

        MachineControlSchedule.Node node = nodeAt(mouseX, mouseY);
        if (node != null) {
            PortHit input = inputPortAt(node, mouseX, mouseY);
            if (input != null && button == 1 && node.connectionAt(input.kind, input.slot) != null) {
                sendAction(MachineControlScheduleMenu.disconnectInputAction(node.id(), input.kind, input.slot));
                return true;
            }
            MachineControlSchedule.PortKind output = outputPortAt(node, mouseX, mouseY);
            if (output != null && button == 0) {
                wireSource = node.id();
                wireKind = output;
                return true;
            }
            if (button == 1) {
                selectedNodeId = node.id();
                selectedVariableId = -1;
                settingsScroll = 0;
                return true;
            }
            if (button == 0) {
                draggingNode = node.id();
                dragOffsetX = (int) Math.round(screenToWorldX(mouseX)) - node.x();
                dragOffsetY = (int) Math.round(screenToWorldY(mouseY)) - node.y();
                return true;
            }
        }

        if (button == 1 && workspaceContains(mouseX, mouseY)) {
            closeSettings();
            return true;
        }
        if (button == 0 && workspaceContains(mouseX, mouseY)) {
            panning = true;
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingNode >= 0) {
            MachineControlSchedule.Node node = menu.clientSchedule().node(draggingNode);
            if (node != null) menu.clientSchedule().moveNode(node.id(),
                    (int) Math.round(screenToWorldX(mouseX)) - dragOffsetX,
                    (int) Math.round(screenToWorldY(mouseY)) - dragOffsetY);
            return true;
        }
        if (panning) {
            cameraX -= (mouseX - lastMouseX) / zoom;
            cameraY -= (mouseY - lastMouseY) / zoom;
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingPalette != null) {
            if (workspaceContains(mouseX, mouseY)) {
                int wx = (int) Math.round(screenToWorldX(mouseX));
                int wy = (int) Math.round(screenToWorldY(mouseY));
                int action = draggingPalette.variableId >= 0
                        ? MachineControlScheduleMenu.addVariableReporterAction(draggingPalette.variableId, wx, wy)
                        : MachineControlScheduleMenu.addNodeAction(draggingPalette.type, wx, wy);
                sendAction(action);
            }
            draggingPalette = null;
            return true;
        }
        if (draggingNode >= 0) {
            MachineControlSchedule.Node node = menu.clientSchedule().node(draggingNode);
            if (node != null) sendServerOnly(MachineControlScheduleMenu.moveNodeAction(node.id(), node.x(), node.y()));
            draggingNode = -1;
            return true;
        }
        if (wireSource >= 0) {
            MachineControlSchedule.Node target = nodeAt(mouseX, mouseY);
            if (target != null) {
                PortHit input = inputPortAt(target, mouseX, mouseY);
                if (input != null && input.kind == wireKind) {
                    sendAction(MachineControlScheduleMenu.connectAction(target.id(), wireSource, wireKind, input.slot));
                }
            }
            wireSource = -1;
            wireKind = null;
            return true;
        }
        panning = false;
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (settingsOpen() && mouseX >= settingsLeft()) {
            settingsScroll = Math.max(0, settingsScroll - (int) Math.signum(scrollY));
            return true;
        }
        if (mouseX < leftPos + SIDEBAR_WIDTH) {
            paletteScroll = Math.max(0, paletteScroll - (int) Math.signum(scrollY));
            return true;
        }
        if (workspaceContains(mouseX, mouseY)) {
            double beforeX = screenToWorldX(mouseX);
            double beforeY = screenToWorldY(mouseY);
            zoom = Math.max(.35, Math.min(2.25, zoom * (scrollY > 0 ? 1.12 : .89)));
            cameraX += beforeX - screenToWorldX(mouseX);
            cameraY += beforeY - screenToWorldY(mouseY);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editor.isVisible()) {
            if (keyCode == 257 || keyCode == 335) { finishEdit(); return true; }
            if (keyCode == 256) { cancelEdit(); return true; }
            return editor.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void beginEdit(EditMode mode, int id, String value) {
        editMode = mode;
        editingId = id;
        editor.setMaxLength(mode == EditMode.NODE_TEXT ? 256 : 12);
        editor.setValue(value);
        editor.setVisible(true);
        editor.setFocused(true);
        setFocused(editor);
    }

    private void finishEdit() {
        String value = editor.getValue();
        if (editMode == EditMode.NEW_VARIABLE) {
            sendTextToVariableBuilder(MachineControlScheduleMenu.beginVariableAction(), value, MachineControlScheduleMenu.finishVariableAction());
            menu.clientSchedule().addVariable(value);
        } else if (editMode == EditMode.RENAME_VARIABLE) {
            sendTextToVariableBuilder(MachineControlScheduleMenu.beginRenameVariableAction(editingId), value, MachineControlScheduleMenu.finishRenameVariableAction());
            menu.clientSchedule().renameVariable(editingId, value);
        } else if (editMode == EditMode.NODE_VALUE) {
            try {
                int number = MachineControlSchedule.parseScaledNumber(value);
                menu.clientSchedule().setNodeValue(editingId, number);
                sendNumberToServer(MachineControlScheduleMenu.beginNodeValueAction(editingId), number);
            } catch (NumberFormatException ignored) { }
        } else if (editMode == EditMode.VARIABLE_VALUE) {
            try {
                int number = MachineControlSchedule.parseScaledNumber(value);
                menu.clientSchedule().setVariableValue(editingId, number);
                sendNumberToServer(MachineControlScheduleMenu.beginVariableValueAction(editingId), number);
            } catch (NumberFormatException ignored) { }
        } else if (editMode == EditMode.NODE_TEXT) {
            setNodeText(editingId, value);
        }
        cancelEdit();
    }

    private void sendNumberToServer(int beginAction, int value) {
        sendServerOnly(beginAction);
        for (char c : MachineControlSchedule.formatNumber(value).toCharArray()) sendServerOnly(MachineControlScheduleMenu.appendNumberCharAction(c));
        sendServerOnly(MachineControlScheduleMenu.finishNumberValueAction());
    }

    private void sendTextToVariableBuilder(int beginAction, String value, int finishAction) {
        sendServerOnly(beginAction);
        for (char c : value.toCharArray()) sendServerOnly(MachineControlScheduleMenu.appendVariableCharAction(c));
        sendServerOnly(finishAction);
    }

    private void setNodeText(int nodeId, String value) {
        sendServerOnly(MachineControlScheduleMenu.beginNodeTextAction(nodeId));
        for (char c : value.toCharArray()) sendServerOnly(MachineControlScheduleMenu.appendNodeTextCharAction(c));
        sendServerOnly(MachineControlScheduleMenu.finishNodeTextAction());
        menu.clientSchedule().setNodeText(nodeId, value);
    }

    private void cancelEdit() {
        editor.setVisible(false);
        editor.setFocused(false);
        editMode = EditMode.NONE;
        editingId = -1;
    }

    private PaletteEntry paletteAt(double mouseX, double mouseY) {
        if (mouseX < leftPos + 4 || mouseX >= leftPos + SIDEBAR_WIDTH - 4) return null;
        int listTop = topPos + HEADER_HEIGHT + 37;
        if (mouseY < listTop || mouseY >= topPos + imageHeight - (category == MachineControlSchedule.Category.VARIABLES ? 22 : 2)) return null;
        int index = ((int) mouseY - listTop) / PALETTE_ROW_HEIGHT + paletteScroll;
        List<PaletteEntry> entries = paletteEntries();
        return index >= 0 && index < entries.size() ? entries.get(index) : null;
    }

    private MachineControlSchedule.Node nodeAt(double mouseX, double mouseY) {
        List<MachineControlSchedule.Node> nodes = menu.clientSchedule().nodes();
        for (int i = nodes.size() - 1; i >= 0; i--) {
            MachineControlSchedule.Node node = nodes.get(i);
            if (inside(mouseX, mouseY, nodeScreenX(node) - 6, nodeScreenY(node), nodeWidth(node) + 12, nodeHeight(node))) return node;
        }
        return null;
    }

    private MachineControlSchedule.PortKind outputPortAt(MachineControlSchedule.Node node, double mouseX, double mouseY) {
        for (MachineControlSchedule.PortKind kind : MachineControlSchedule.PortKind.values()) {
            int radius = Math.max(5, scaled(5));
            if (node.provides(kind) && inside(mouseX, mouseY, outputX(node, kind) - radius, outputY(node, kind) - radius, radius * 2, radius * 2)) return kind;
        }
        return null;
    }

    private PortHit inputPortAt(MachineControlSchedule.Node node, double mouseX, double mouseY) {
        for (MachineControlSchedule.PortKind kind : MachineControlSchedule.PortKind.values()) {
            for (int slot = 0; slot < node.visibleInputSlots(kind); slot++) {
                int radius = Math.max(5, scaled(5));
                if (inside(mouseX, mouseY, inputX(node, kind, slot) - radius, inputY(node, kind, slot) - radius, radius * 2, radius * 2)) return new PortHit(kind, slot);
            }
        }
        return null;
    }

    private int nodeScreenX(MachineControlSchedule.Node node) { return workspaceLeft() + (int) Math.round((node.x() - cameraX) * zoom); }
    private int nodeScreenY(MachineControlSchedule.Node node) { return topPos + HEADER_HEIGHT + (int) Math.round((node.y() - cameraY) * zoom); }
    private int baseNodeWidth(MachineControlSchedule.Node node) { return Math.max(42, Math.min(104, font.width(nodeTitle(node)) + 18)); }
    private int baseNodeHeight(MachineControlSchedule.Node node) {
        int inputs = node.visibleInputSlots(MachineControlSchedule.PortKind.BOOLEAN) + node.visibleInputSlots(MachineControlSchedule.PortKind.NUMBER);
        int outputs = (node.provides(MachineControlSchedule.PortKind.BOOLEAN) ? 1 : 0) + (node.provides(MachineControlSchedule.PortKind.NUMBER) ? 1 : 0);
        return Math.max(25, 10 + Math.max(inputs, outputs) * PORT_SPACING);
    }
    private int nodeWidth(MachineControlSchedule.Node node) { return scaled(baseNodeWidth(node)); }
    private int nodeHeight(MachineControlSchedule.Node node) { return scaled(baseNodeHeight(node)); }
    private int scaled(int value) { return Math.max(2, (int) Math.round(value * zoom)); }
    private double screenToWorldX(double x) { return cameraX + (x - workspaceLeft()) / zoom; }
    private double screenToWorldY(double y) { return cameraY + (y - topPos - HEADER_HEIGHT) / zoom; }
    private void enableWorkspaceScissor(GuiGraphics graphics) {
        int right = settingsOpen() ? settingsLeft() : leftPos + imageWidth;
        graphics.enableScissor(workspaceLeft(), topPos + HEADER_HEIGHT, right, topPos + imageHeight);
    }

    private boolean workspaceContains(double x, double y) {
        int right = settingsOpen() ? settingsLeft() : leftPos + imageWidth;
        return x >= workspaceLeft() && x < right && y >= topPos + HEADER_HEIGHT && y < topPos + imageHeight;
    }
    private int workspaceLeft() { return leftPos + SIDEBAR_WIDTH; }
    private int settingsLeft() { return leftPos + imageWidth - SETTINGS_WIDTH; }
    private boolean settingsOpen() { return selectedNodeId >= 0 || selectedVariableId >= 0; }
    private void closeSettings() { selectedNodeId = -1; selectedVariableId = -1; settingsScroll = 0; }

    private int inputX(MachineControlSchedule.Node node, MachineControlSchedule.PortKind kind, int slot) { return nodeScreenX(node) - scaled(2); }
    private int inputY(MachineControlSchedule.Node node, MachineControlSchedule.PortKind kind, int slot) {
        return nodeScreenY(node) + scaled(inputLocalY(node, kind, slot));
    }
    private int inputLocalY(MachineControlSchedule.Node node, MachineControlSchedule.PortKind kind, int slot) {
        int preceding = kind == MachineControlSchedule.PortKind.NUMBER ? node.visibleInputSlots(MachineControlSchedule.PortKind.BOOLEAN) : 0;
        return 8 + (preceding + slot) * PORT_SPACING;
    }
    private int outputX(MachineControlSchedule.Node node, MachineControlSchedule.PortKind kind) { return node == null ? 0 : nodeScreenX(node) + nodeWidth(node); }
    private int outputY(MachineControlSchedule.Node node, MachineControlSchedule.PortKind kind) {
        return node == null ? 0 : nodeScreenY(node) + scaled(outputLocalY(node, kind));
    }
    private int outputLocalY(MachineControlSchedule.Node node, MachineControlSchedule.PortKind kind) {
        int row = kind == MachineControlSchedule.PortKind.NUMBER && node.provides(MachineControlSchedule.PortKind.BOOLEAN) ? 1 : 0;
        return 8 + row * PORT_SPACING;
    }

    private String nodeTitle(MachineControlSchedule.Node node) {
        if (node == null) return "unknown";
        if (node.type() == MachineControlSchedule.NodeType.VARIABLE_REPORTER) {
            MachineControlSchedule.Variable variable = menu.clientSchedule().variable(node.variableId());
            return variable == null ? "variable" : variable.name();
        }
        if (node.type() == MachineControlSchedule.NodeType.SET_VARIABLE) {
            MachineControlSchedule.Variable variable = menu.clientSchedule().variable(node.variableId());
            return variable == null ? "set variable" : "set " + variable.name();
        }
        return switch (node.type()) {
            case NUMBER -> "number";
            case REDSTONE_INPUT -> "redstone input";
            case INPUT_ENERGY -> "energy input";
            case INPUT_STEAM -> "steam input";
            case INPUT_ITEMS -> "item input";
            case INPUT_FLUIDS -> "fluid input";
            case REDSTONE_OUTPUT -> "redstone output";
            case RECIPE_MIN_CB -> "recipe min CB";
            case RECIPE_MAX_CB -> "recipe max CB";
            case MACHINE_CB -> "machine CB";
            case RECIPE_MIN_RPM -> "recipe min RPM";
            case RECIPE_MAX_RPM -> "recipe max RPM";
            case MACHINE_RPM -> "machine RPM";
            case COMPARE -> "compare " + operationLabel(node.operation());
            case MATH -> "math " + operationLabel(node.operation());
            default -> label(node.type());
        };
    }

    private String variableTitle() {
        MachineControlSchedule.Variable variable = menu.clientSchedule().variable(selectedVariableId);
        return variable == null ? "Variable" : variable.name();
    }

    private static List<MachineControlSchedule.Operation> comparisonOperations() {
        return List.of(MachineControlSchedule.Operation.EQUALS, MachineControlSchedule.Operation.NOT_EQUALS,
                MachineControlSchedule.Operation.LESS, MachineControlSchedule.Operation.LESS_OR_EQUAL,
                MachineControlSchedule.Operation.GREATER, MachineControlSchedule.Operation.GREATER_OR_EQUAL);
    }

    private static List<MachineControlSchedule.Operation> mathOperations() {
        return List.of(MachineControlSchedule.Operation.ADD, MachineControlSchedule.Operation.SUBTRACT,
                MachineControlSchedule.Operation.MULTIPLY, MachineControlSchedule.Operation.DIVIDE,
                MachineControlSchedule.Operation.MIN, MachineControlSchedule.Operation.MAX);
    }

    private static String operationLabel(MachineControlSchedule.Operation operation) {
        return switch (operation) {
            case EQUALS -> "=";
            case NOT_EQUALS -> "!=";
            case LESS -> "<";
            case LESS_OR_EQUAL -> "<=";
            case GREATER -> ">";
            case GREATER_OR_EQUAL -> ">=";
            case ADD -> "+";
            case SUBTRACT -> "-";
            case MULTIPLY -> "*";
            case DIVIDE -> "/";
            case MIN -> "min";
            case MAX -> "max";
        };
    }

    private static String label(MachineControlSchedule.NodeType type) {
        return switch (type) {
            case RECIPE_MIN_CB -> "recipe min CB";
            case RECIPE_MAX_CB -> "recipe max CB";
            case MACHINE_CB -> "machine CB";
            case RECIPE_MIN_RPM -> "recipe min RPM";
            case RECIPE_MAX_RPM -> "recipe max RPM";
            case MACHINE_RPM -> "machine RPM";
            default -> type.name().toLowerCase().replace('_', ' ');
        };
    }
    private static String categoryShort(MachineControlSchedule.Category category) {
        return switch (category) {
            case CONTROL -> "Ctl";
            case LOGIC -> "Log";
            case MATH -> "Math";
            case MACHINE -> "Mach";
            case INPUTS -> "In";
            case WORLD -> "Wld";
            case REDSTONE -> "Out";
            case VARIABLES -> "Var";
        };
    }
    private static int nodeColor(MachineControlSchedule.NodeType type) {
        return switch (type.category()) {
            case CONTROL -> 0xFFD18A32;
            case LOGIC -> 0xFF5D56B8;
            case MATH -> 0xFF4E9D62;
            case MACHINE -> 0xFF3D80B8;
            case INPUTS -> 0xFF3F84A8;
            case WORLD -> 0xFF4A8B7B;
            case REDSTONE -> 0xFFB34A3C;
            case VARIABLES -> 0xFFB36B20;
        };
    }
    private static int lighten(int color) {
        int r = Math.min(255, (color >> 16 & 255) + 24);
        int g = Math.min(255, (color >> 8 & 255) + 24);
        int b = Math.min(255, (color & 255) + 24);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
    private String trim(String text, int width) { return font.width(text) <= width ? text : font.plainSubstrByWidth(text, Math.max(4, width - 6)) + "..."; }

    private void drawGhostNode(GuiGraphics graphics, int mouseX, int mouseY, PaletteEntry entry) {
        graphics.fill(mouseX - 25, mouseY - 8, mouseX + 25, mouseY + 8, 0xCCFFFFFF);
        graphics.fill(mouseX - 24, mouseY - 7, mouseX + 24, mouseY + 7, nodeColor(entry.type));
        graphics.drawCenteredString(font, trim(entry.label, 43), mouseX, mouseY - 4, 0xFFFFFFFF);
    }
    private static void drawPort(GuiGraphics graphics, int x, int y, MachineControlSchedule.PortKind kind, boolean input, boolean connected) {
        int color = kind == MachineControlSchedule.PortKind.BOOLEAN
                ? input ? BOOLEAN_INPUT : BOOLEAN_OUTPUT
                : input ? NUMBER_INPUT : NUMBER_OUTPUT;
        if (kind == MachineControlSchedule.PortKind.BOOLEAN) drawCircle(graphics, x, y, color, connected);
        else drawSquare(graphics, x, y, color, connected);
    }
    private static void drawCircle(GuiGraphics graphics, int x, int y, int color, boolean connected) {
        int border = connected ? 0xFFFFFFFF : 0xFF15191E;
        graphics.fill(x - 4, y - 3, x + 5, y + 4, border);
        graphics.fill(x - 3, y - 4, x + 4, y + 5, border);
        graphics.fill(x - 3, y - 2, x + 4, y + 3, color);
        graphics.fill(x - 2, y - 3, x + 3, y + 4, color);
    }
    private static void drawSquare(GuiGraphics graphics, int x, int y, int color, boolean connected) {
        graphics.fill(x - 5, y - 5, x + 5, y + 5, connected ? 0xFFFFFFFF : 0xFF15191E);
        graphics.fill(x - 3, y - 3, x + 3, y + 3, color);
    }
    private static void horizontal(GuiGraphics graphics, int x1, int x2, int y, int color) { graphics.fill(Math.min(x1, x2), y, Math.max(x1, x2) + 1, y + 2, color); }
    private static void vertical(GuiGraphics graphics, int x, int y1, int y2, int color) { graphics.fill(x, Math.min(y1, y2), x + 2, Math.max(y1, y2) + 1, color); }
    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) { return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height; }

    private SettingsEntry info(String label) { return new SettingsEntry(label, null, 0xFFABB5BF); }
    private SettingsEntry action(String label, Runnable action) { return new SettingsEntry(label, action, 0xFFFFFFFF); }
    private SettingsEntry option(String label, boolean selected, Runnable action) { return new SettingsEntry((selected ? "[x] " : "[ ] ") + label, action, selected ? 0xFF73E28A : 0xFFFFFFFF); }
    private SettingsEntry danger(String label, Runnable action) { return new SettingsEntry(label, action, 0xFFFF7777); }

    private void sendAction(int action) { menu.applyClientAction(action); sendServerOnly(action); }
    private void sendServerOnly(int action) { if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action); }

    private record PaletteEntry(MachineControlSchedule.NodeType type, int variableId, String label) { }
    private record SettingsEntry(String label, Runnable action, int color) { }
    private record PortHit(MachineControlSchedule.PortKind kind, int slot) { }
    private enum EditMode { NONE, NEW_VARIABLE, RENAME_VARIABLE, NODE_VALUE, VARIABLE_VALUE, NODE_TEXT }
}
