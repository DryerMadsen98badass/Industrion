package net.mads.industron.machine.machines.electric.multiblock;

import java.util.ArrayList;
import java.util.List;

public final class MultiblockPattern {
    public static final char air = '#';
    public static final char ignore = ' ';
    public static final char controller = '@';

    private MultiblockPattern() {
    }

    public static Row row(char... symbols) {
        return new Row(symbols);
    }

    public record Row(char[] symbols) {
        public int length() {
            return symbols.length;
        }
    }

    public static final class VariantBuilder {
        private final List<Row[]> layers = new ArrayList<>();

        public VariantBuilder layer(Row... rows) {
            layers.add(rows);
            return this;
        }

        PatternVariant build(String id) {
            return new PatternVariant(id, List.copyOf(layers));
        }
    }
}
