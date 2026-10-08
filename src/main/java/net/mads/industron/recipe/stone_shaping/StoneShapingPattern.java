package net.mads.industron.recipe.stone_shaping;

import java.util.BitSet;
import java.util.List;

public final class StoneShapingPattern {
    public static final int SIZE = 16;
    public static final int PIXELS = SIZE * SIZE;

    private StoneShapingPattern() {
    }

    public static BitSet fullMask() {
        BitSet mask = new BitSet(PIXELS);
        mask.set(0, PIXELS);
        return mask;
    }

    public static BitSet fromRows(List<String> rows) {
        validate(rows);
        BitSet mask = new BitSet(PIXELS);
        for (int y = 0; y < SIZE; y++) {
            String row = rows.get(y);
            for (int x = 0; x < SIZE; x++) {
                if (row.charAt(x) == '#') {
                    mask.set(index(x, y));
                }
            }
        }
        return mask;
    }

    public static boolean matchesTranslated(BitSet target, BitSet actual) {
        Bounds targetBounds = bounds(target);
        Bounds actualBounds = bounds(actual);
        if (targetBounds == null || actualBounds == null) {
            return targetBounds == actualBounds;
        }
        if (targetBounds.width() != actualBounds.width() || targetBounds.height() != actualBounds.height()) {
            return false;
        }
        for (int y = 0; y < targetBounds.height(); y++) {
            for (int x = 0; x < targetBounds.width(); x++) {
                boolean targetPixel = target.get(index(targetBounds.minX + x, targetBounds.minY + y));
                boolean actualPixel = actual.get(index(actualBounds.minX + x, actualBounds.minY + y));
                if (targetPixel != actualPixel) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void validate(List<String> rows) {
        if (rows == null || rows.size() != SIZE) {
            throw new IllegalArgumentException("Stone Shaping pattern must contain exactly 16 rows");
        }
        boolean any = false;
        for (int y = 0; y < SIZE; y++) {
            String row = rows.get(y);
            if (row == null || row.length() != SIZE) {
                throw new IllegalArgumentException("Stone Shaping row " + y + " must contain exactly 16 characters");
            }
            for (int x = 0; x < SIZE; x++) {
                char value = row.charAt(x);
                if (value != '#' && value != '.') {
                    throw new IllegalArgumentException("Stone Shaping patterns may only contain '#' and '.'");
                }
                any |= value == '#';
            }
        }
        if (!any) {
            throw new IllegalArgumentException("Stone Shaping pattern cannot be empty");
        }
    }

    public static int index(int x, int y) {
        return y * SIZE + x;
    }

    private static Bounds bounds(BitSet mask) {
        int first = mask.nextSetBit(0);
        if (first < 0) return null;
        int minX = SIZE;
        int minY = SIZE;
        int maxX = -1;
        int maxY = -1;
        for (int bit = first; bit >= 0 && bit < PIXELS; bit = mask.nextSetBit(bit + 1)) {
            int x = bit % SIZE;
            int y = bit / SIZE;
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }
        return new Bounds(minX, minY, maxX, maxY);
    }

    private record Bounds(int minX, int minY, int maxX, int maxY) {
        int width() { return maxX - minX + 1; }
        int height() { return maxY - minY + 1; }
    }
}
