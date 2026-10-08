package net.mads.industron.material;

import java.util.List;

/** Shared deterministic RGB blend for a substance defined by .contains(...). */
public final class CompositionColor {
    private CompositionColor() {
    }

    public static int blend(List<MaterialComponent> components) {
        if (components == null || components.isEmpty()) return 0x808080;
        long totalWeight = 0L;
        long red = 0L;
        long green = 0L;
        long blue = 0L;
        for (MaterialComponent component : components) {
            int weight = Math.max(1, component.amount());
            int color = component.substance().color() & 0x00FFFFFF;
            red += (long) ((color >> 16) & 0xFF) * weight;
            green += (long) ((color >> 8) & 0xFF) * weight;
            blue += (long) (color & 0xFF) * weight;
            totalWeight += weight;
        }
        if (totalWeight <= 0L) return 0x808080;
        int r = clamp((int) Math.round((double) red / totalWeight));
        int g = clamp((int) Math.round((double) green / totalWeight));
        int b = clamp((int) Math.round((double) blue / totalWeight));
        return (r << 16) | (g << 8) | b;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
