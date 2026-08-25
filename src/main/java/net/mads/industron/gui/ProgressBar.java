package net.mads.industron.gui;

import net.mads.industron.Industron;
import net.minecraft.resources.ResourceLocation;

/**
 * Registered progress bar textures available to Industron recipe types and machines.
 *
 * <p>Most textures contain an empty frame followed by a filled frame vertically.
 * The renderer clips the filled frame according to {@link #direction()}.</p>
 */
public enum ProgressBar {
    ARROW("arrow", 20, 20);

    private final ResourceLocation texture;
    private final int width;
    private final int height;
    private final Direction direction;
    private final FrameLayout frameLayout;

    ProgressBar(String name, int width, int height) {
        this(name, width, height, Direction.RIGHT);
    }

    ProgressBar(String name, int width, int height, Direction direction) {
        this(name, width, height, direction, FrameLayout.VERTICAL);
    }

    ProgressBar(String name, int width, int height, Direction direction, FrameLayout frameLayout) {
        this.texture = ResourceLocation.fromNamespaceAndPath(
                Industron.MOD_ID,
                "gui/progress_bar/progress_bar_" + name + ".png"
        );
        this.width = width;
        this.height = height;
        this.direction = direction;
        this.frameLayout = frameLayout;
    }

    public ResourceLocation texture() {
        return texture;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public Direction direction() {
        return direction;
    }

    public int filledU() {
        return frameLayout == FrameLayout.HORIZONTAL ? width : 0;
    }

    public int filledV() {
        return frameLayout == FrameLayout.VERTICAL ? height : 0;
    }

    public int textureWidth() {
        return frameLayout == FrameLayout.HORIZONTAL ? width * 2 : width;
    }

    public int textureHeight() {
        return frameLayout == FrameLayout.VERTICAL ? height * 2 : height;
    }

    public enum Direction {
        RIGHT,
        LEFT,
        UP,
        DOWN
    }

    private enum FrameLayout {
        VERTICAL,
        HORIZONTAL
    }
}
