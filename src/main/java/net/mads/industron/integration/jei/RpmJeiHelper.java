package net.mads.industron.integration.jei;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Optional;

final class RpmJeiHelper {
    private RpmJeiHelper() {
    }

    static void draw(GuiGraphics graphics, int x, int y, int minRpm, Optional<Integer> maxRpm) {
        String text = text(minRpm, maxRpm);
        if (!text.isEmpty()) {
            graphics.drawString(Minecraft.getInstance().font, text, x, y, 0xFF404040, false);
        }
    }

    private static String text(int minRpm, Optional<Integer> maxRpm) {
        boolean hasMin = minRpm > 0;
        boolean hasMax = maxRpm.isPresent();
        if (hasMin && hasMax) {
            return "RPM: " + minRpm + "-" + maxRpm.get();
        }
        if (hasMin) {
            return "Min RPM: " + minRpm;
        }
        if (hasMax) {
            return "Max RPM: " + maxRpm.get();
        }
        return "";
    }
}
