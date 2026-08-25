package net.mads.industron.machine.interaction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Controller-relative position. X is right, Y is vertical, Z is forward. */
public record RelativePos(int x, int y, int z) {
    public static final Codec<RelativePos> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("x").forGetter(RelativePos::x),
            Codec.INT.fieldOf("y").forGetter(RelativePos::y),
            Codec.INT.fieldOf("z").forGetter(RelativePos::z)
    ).apply(instance, RelativePos::new));

    public BlockPos rotate(BlockPos origin, Direction facing) {
        Direction right = facing.getClockWise();
        Direction forward = facing.getOpposite();
        return origin.relative(right, x).above(y).relative(forward, z);
    }
}
