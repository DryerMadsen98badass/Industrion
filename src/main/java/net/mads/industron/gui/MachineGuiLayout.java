package net.mads.industron.gui;

/**
 * Shared GTCEu-style automatic layout used by machine screens and recipe viewers.
 */
public record MachineGuiLayout(
        int itemInputs,
        int itemOutputs,
        int fluidInputs,
        int fluidOutputs,
        ProgressBar progressBar,
        int width,
        int machineTop,
        int contentHeight,
        int playerInventoryY
) {
    public static final int SLOT_SIZE = 18;
    public static final int MAX_COLUMNS = 3;
    public static final int SIDE_MARGIN = 8;
    public static final int CAPABILITY_GAP = 4;
    public static final int MIN_WIDTH = 176;

    public static MachineGuiLayout automatic(
            int itemInputs,
            int itemOutputs,
            int fluidInputs,
            int fluidOutputs,
            ProgressBar progressBar
    ) {
        int safeItemInputs = Math.max(0, itemInputs);
        int safeItemOutputs = Math.max(0, itemOutputs);
        int safeFluidInputs = Math.max(0, fluidInputs);
        int safeFluidOutputs = Math.max(0, fluidOutputs);
        ProgressBar bar = progressBar == null ? ProgressBar.ARROW : progressBar;
        int inputWidth = Math.max(gridWidth(safeItemInputs), gridWidth(safeFluidInputs));
        int outputWidth = Math.max(gridWidth(safeItemOutputs), gridWidth(safeFluidOutputs));
        int width = Math.max(
                MIN_WIDTH,
                SIDE_MARGIN * 2 + inputWidth + outputWidth + bar.width() + 24
        );
        int inputHeight = stackHeight(safeItemInputs, safeFluidInputs);
        int outputHeight = stackHeight(safeItemOutputs, safeFluidOutputs);
        int contentHeight = Math.max(bar.height(), Math.max(inputHeight, outputHeight));
        int machineTop = 18;
        int playerInventoryY = Math.max(84, machineTop + contentHeight + 32);
        return new MachineGuiLayout(
                safeItemInputs,
                safeItemOutputs,
                safeFluidInputs,
                safeFluidOutputs,
                bar,
                width,
                machineTop,
                contentHeight,
                playerInventoryY
        );
    }

    public int inputItemX(int index) {
        return SIDE_MARGIN + inputColumn(index) * SLOT_SIZE;
    }

    public int outputItemX(int index) {
        return outputBaseX() + column(index) * SLOT_SIZE;
    }

    public int inputFluidX(int index) {
        return SIDE_MARGIN + inputColumn(index) * SLOT_SIZE;
    }

    public int outputFluidX(int index) {
        return outputBaseX() + column(index) * SLOT_SIZE;
    }

    public int inputItemY(int index) {
        return machineTop + row(index) * SLOT_SIZE;
    }

    public int outputItemY(int index) {
        return machineTop + row(index) * SLOT_SIZE;
    }

    public int inputFluidY(int index) {
        return fluidBaseY(machineTop, itemInputs, fluidInputs) + row(index) * SLOT_SIZE;
    }

    public int outputFluidY(int index) {
        return fluidBaseY(machineTop, itemOutputs, fluidOutputs) + row(index) * SLOT_SIZE;
    }

    public int progressX() {
        return (width - progressBar.width()) / 2;
    }

    public int progressY() {
        return machineTop + (contentHeight - progressBar.height()) / 2;
    }

    public int menuHeight() {
        return playerInventoryY + 82;
    }

    private int outputBaseX() {
        return width - SIDE_MARGIN - Math.max(gridWidth(itemOutputs), gridWidth(fluidOutputs));
    }

    private static int fluidBaseY(int top, int itemCount, int fluidCount) {
        if (fluidCount <= 0) {
            return top + gridHeight(itemCount);
        }
        return top + gridHeight(itemCount) + (itemCount > 0 ? CAPABILITY_GAP : 0);
    }

    private static int stackHeight(int itemCount, int fluidCount) {
        int height = gridHeight(itemCount);
        if (fluidCount > 0) {
            height += (itemCount > 0 ? CAPABILITY_GAP : 0) + gridHeight(fluidCount);
        }
        return height;
    }

    private static int gridWidth(int count) {
        return Math.min(MAX_COLUMNS, Math.max(0, count)) * SLOT_SIZE;
    }

    private static int gridHeight(int count) {
        return rows(count) * SLOT_SIZE;
    }

    private static int rows(int count) {
        return count <= 0 ? 0 : (count + MAX_COLUMNS - 1) / MAX_COLUMNS;
    }

    private static int column(int index) {
        return Math.max(0, index) % MAX_COLUMNS;
    }

    private static int inputColumn(int index) {
        return MAX_COLUMNS - 1 - column(index);
    }

    private static int row(int index) {
        return Math.max(0, index) / MAX_COLUMNS;
    }
}
