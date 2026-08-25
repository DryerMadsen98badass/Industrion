package net.mads.industron.machine.interaction;

import net.mads.industron.machine.MachineTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Named controller-relative area made from included boxes minus excluded boxes.
 * Definitions use directions instead of X/Y/Z:
 * left/right, bottom/top and front/back. The complete area rotates with the
 * controller or singleblock facing.
 *
 * <pre>{@code
 * Option.area(MachineArea.area("work_area")
 *         .include(MachineArea.centeredSquare(
 *                 AreaValue.fixed(5).plusPerTier(2)
 *         ).offsetFront(3))
 *         .exclude(MachineArea.box()
 *                 .left(1).right(1).front(2).back(0)))
 * }</pre>
 *
 * <p>The first generated tier gets 5 x 5, the next gets 7 x 7. Excluded boxes
 * are removed after all included boxes have been combined.</p>
 */
public final class MachineArea {
    private final String name;
    private final List<Box> includes;
    private final List<Box> excludes;

    private MachineArea(Builder builder) {
        this.name = builder.name;
        this.includes = List.copyOf(builder.includes);
        this.excludes = List.copyOf(builder.excludes);
        if (name == null || name.isBlank()) throw new IllegalStateException("Machine area needs a name");
        if (includes.isEmpty()) throw new IllegalStateException("Machine area '" + name + "' needs at least one included box");
    }

    public static Builder area(String name) {
        return new Builder(name);
    }

    public static Box.Builder box() {
        return new Box.Builder();
    }

    /** A flat centered square. Size 5 means two left, two right and the center. */
    public static Box.Builder centeredSquare(AreaValue size) {
        return Box.Builder.centered(size, AreaValue.fixed(1), size);
    }

    /** A centered three-dimensional box. */
    public static Box.Builder centeredBox(AreaValue width, AreaValue height, AreaValue depth) {
        return Box.Builder.centered(width, height, depth);
    }

    public String name() {
        return name;
    }

    public Resolved resolve(BlockPos origin, Direction facing, MachineTier actualTier, MachineTier startTier) {
        Set<BlockPos> positions = new LinkedHashSet<>();
        includes.forEach(box -> positions.addAll(box.positions(origin, facing, actualTier, startTier)));
        excludes.forEach(box -> positions.removeAll(box.positions(origin, facing, actualTier, startTier)));
        return new Resolved(name, List.copyOf(positions), dimensions(actualTier, startTier), excludes.size());
    }

    public Dimensions dimensions(MachineTier actualTier, MachineTier startTier) {
        int left = 0, right = 0, bottom = 0, top = 0, front = 0, back = 0;
        for (Box box : includes) {
            ResolvedBox resolved = box.resolve(actualTier, startTier);
            left = Math.max(left, resolved.left());
            right = Math.max(right, resolved.right());
            bottom = Math.max(bottom, resolved.bottom());
            top = Math.max(top, resolved.top());
            front = Math.max(front, resolved.front());
            back = Math.max(back, resolved.back());
        }
        return new Dimensions(left + right + 1, bottom + top + 1, front + back + 1);
    }

    public record Dimensions(int width, int height, int depth) {
        public String tooltipText() {
            return height == 1 ? width + " x " + depth : width + " x " + height + " x " + depth;
        }
    }

    public record Resolved(String name, List<BlockPos> positions, Dimensions dimensions, int excludedAreas) {}

    public static final class Builder {
        private final String name;
        private final List<Box> includes = new ArrayList<>();
        private final List<Box> excludes = new ArrayList<>();

        private Builder(String name) {
            this.name = Objects.requireNonNull(name);
        }

        public Builder include(Box.Builder box) {
            return include(box.build());
        }

        public Builder include(Box box) {
            includes.add(Objects.requireNonNull(box));
            return this;
        }

        public Builder exclude(Box.Builder box) {
            return exclude(box.build());
        }

        public Builder exclude(Box box) {
            excludes.add(Objects.requireNonNull(box));
            return this;
        }

        public MachineArea build() {
            return new MachineArea(this);
        }
    }

    /** One directional cuboid. All distances are inclusive and non-negative. */
    public record Box(
            AreaValue left, AreaValue right,
            AreaValue bottom, AreaValue top,
            AreaValue front, AreaValue back,
            AreaValue offsetLeft, AreaValue offsetRight,
            AreaValue offsetBottom, AreaValue offsetTop,
            AreaValue offsetFront, AreaValue offsetBack
    ) {
        private ResolvedBox resolve(MachineTier actual, MachineTier start) {
            return new ResolvedBox(
                    nonNegative(left.resolve(actual, start)), nonNegative(right.resolve(actual, start)),
                    nonNegative(bottom.resolve(actual, start)), nonNegative(top.resolve(actual, start)),
                    nonNegative(front.resolve(actual, start)), nonNegative(back.resolve(actual, start)),
                    offsetRight.resolve(actual, start) - offsetLeft.resolve(actual, start),
                    offsetTop.resolve(actual, start) - offsetBottom.resolve(actual, start),
                    offsetFront.resolve(actual, start) - offsetBack.resolve(actual, start)
            );
        }

        private List<BlockPos> positions(BlockPos origin, Direction facing, MachineTier actual, MachineTier start) {
            ResolvedBox box = resolve(actual, start);
            List<BlockPos> result = new ArrayList<>();
            Direction rightDirection = facing.getClockWise();
            Direction frontDirection = facing.getOpposite();
            for (int right = -box.left(); right <= box.right(); right++) {
                for (int up = -box.bottom(); up <= box.top(); up++) {
                    for (int front = -box.back(); front <= box.front(); front++) {
                        result.add(origin.relative(rightDirection, right + box.offsetRight())
                                .above(up + box.offsetUp())
                                .relative(frontDirection, front + box.offsetFront()));
                    }
                }
            }
            return result;
        }

        private static int nonNegative(int value) {
            if (value < 0) throw new IllegalArgumentException("Area distances cannot be negative");
            return value;
        }

        public static final class Builder {
            private AreaValue left = AreaValue.fixed(0), right = AreaValue.fixed(0);
            private AreaValue bottom = AreaValue.fixed(0), top = AreaValue.fixed(0);
            private AreaValue front = AreaValue.fixed(0), back = AreaValue.fixed(0);
            private AreaValue offsetLeft = AreaValue.fixed(0), offsetRight = AreaValue.fixed(0);
            private AreaValue offsetBottom = AreaValue.fixed(0), offsetTop = AreaValue.fixed(0);
            private AreaValue offsetFront = AreaValue.fixed(0), offsetBack = AreaValue.fixed(0);

            private static Builder centered(AreaValue width, AreaValue height, AreaValue depth) {
                Builder builder = new Builder();
                builder.left = halfLow(width);
                builder.right = halfHigh(width);
                builder.bottom = halfLow(height);
                builder.top = halfHigh(height);
                builder.back = halfLow(depth);
                builder.front = halfHigh(depth);
                return builder;
            }

            private static AreaValue halfLow(AreaValue size) { return new AreaValue(Math.max(0, (size.base() - 1) / 2), size.perTier() / 2); }
            private static AreaValue halfHigh(AreaValue size) { return new AreaValue(Math.max(0, size.base() / 2), size.perTier() - size.perTier() / 2); }

            public Builder left(int value) { return left(AreaValue.fixed(value)); }
            public Builder left(AreaValue value) { left = value; return this; }
            public Builder right(int value) { return right(AreaValue.fixed(value)); }
            public Builder right(AreaValue value) { right = value; return this; }
            public Builder bottom(int value) { return bottom(AreaValue.fixed(value)); }
            public Builder bottom(AreaValue value) { bottom = value; return this; }
            public Builder top(int value) { return top(AreaValue.fixed(value)); }
            public Builder top(AreaValue value) { top = value; return this; }
            public Builder front(int value) { return front(AreaValue.fixed(value)); }
            public Builder front(AreaValue value) { front = value; return this; }
            public Builder back(int value) { return back(AreaValue.fixed(value)); }
            public Builder back(AreaValue value) { back = value; return this; }

            public Builder offsetLeft(int value) { offsetLeft = AreaValue.fixed(value); return this; }
            public Builder offsetRight(int value) { offsetRight = AreaValue.fixed(value); return this; }
            public Builder offsetBottom(int value) { offsetBottom = AreaValue.fixed(value); return this; }
            public Builder offsetTop(int value) { offsetTop = AreaValue.fixed(value); return this; }
            public Builder offsetFront(int value) { offsetFront = AreaValue.fixed(value); return this; }
            public Builder offsetBack(int value) { offsetBack = AreaValue.fixed(value); return this; }

            public Box build() {
                return new Box(left, right, bottom, top, front, back,
                        offsetLeft, offsetRight, offsetBottom, offsetTop, offsetFront, offsetBack);
            }
        }
    }

    private record ResolvedBox(int left, int right, int bottom, int top, int front, int back,
                               int offsetRight, int offsetUp, int offsetFront) {}
}
