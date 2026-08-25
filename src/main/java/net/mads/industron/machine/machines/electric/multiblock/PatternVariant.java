package net.mads.industron.machine.machines.electric.multiblock;

import java.util.List;

public record PatternVariant(String id, List<MultiblockPattern.Row[]> layers) {
    public int variantLevel() {
        try {
            return Integer.parseInt(id);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    public int width() {
        return layers.size();
    }

    public int height() {
        return layers.stream()
                .mapToInt(layer -> layer.length)
                .max()
                .orElse(0);
    }

    public int length() {
        return layers.stream()
                .flatMap(layer -> List.of(layer).stream())
                .mapToInt(MultiblockPattern.Row::length)
                .max()
                .orElse(0);
    }

    public char symbolAt(int x, int y, int z) {
        if (x < 0 || x >= layers.size()) {
            return MultiblockPattern.air;
        }

        MultiblockPattern.Row[] rows = layers.get(x);
        if (y < 0 || y >= rows.length) {
            return MultiblockPattern.air;
        }

        char[] symbols = rows[y].symbols();
        if (z < 0 || z >= symbols.length) {
            return MultiblockPattern.air;
        }

        return symbols[z];
    }
}
