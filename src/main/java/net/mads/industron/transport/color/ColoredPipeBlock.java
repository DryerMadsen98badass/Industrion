package net.mads.industron.transport.color;

import net.minecraft.world.item.DyeColor;

public interface ColoredPipeBlock {
    DyeColor pipeColor();

    PipeColorDefinitions.PipeFamily pipeFamily();
}
