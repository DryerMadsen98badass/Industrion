package net.mads.industron.transport.color;

import net.minecraft.world.item.DyeColor;

public interface PipeColorHolder {
    DyeColor createExpansion$getPipeColor();

    void createExpansion$setPipeColor(DyeColor color);
}
