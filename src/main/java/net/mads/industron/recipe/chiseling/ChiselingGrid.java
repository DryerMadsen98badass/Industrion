package net.mads.industron.recipe.chiseling;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Shared 3x3 block-face coordinate system used by runtime, renderer and JEI. */
public final class ChiselingGrid {
    public static final int SIZE = 3;

    private ChiselingGrid() {
    }

    /** Returns row-major cell 1..9 for the visible face. */
    public static int cell(BlockHitResult hit) {
        if (hit == null) return -1;
        BlockPos pos = hit.getBlockPos();
        Vec3 center = Vec3.atCenterOf(pos).add(
                hit.getDirection().getStepX() * 0.5D,
                hit.getDirection().getStepY() * 0.5D,
                hit.getDirection().getStepZ() * 0.5D
        );
        Vec3 delta = hit.getLocation().subtract(center);
        Basis basis = basis(hit.getDirection());
        double u = clamp(delta.dot(basis.right()) + 0.5D);
        double v = clamp(delta.dot(basis.down()) + 0.5D);
        int column = Math.min(SIZE - 1, (int) Math.floor(u * SIZE));
        int row = Math.min(SIZE - 1, (int) Math.floor(v * SIZE));
        return row * SIZE + column + 1;
    }

    public static Vec3 point(BlockPos pos, Direction face, double u, double v, double outwardOffset) {
        Basis basis = basis(face);
        Vec3 normal = Vec3.atLowerCornerOf(face.getNormal());
        return Vec3.atCenterOf(pos)
                .add(normal.scale(0.5D + outwardOffset))
                .add(basis.right().scale(u - 0.5D))
                .add(basis.down().scale(v - 0.5D));
    }

    public static Basis basis(Direction face) {
        return switch (face) {
            case NORTH -> new Basis(new Vec3(-1, 0, 0), new Vec3(0, -1, 0));
            case SOUTH -> new Basis(new Vec3(1, 0, 0), new Vec3(0, -1, 0));
            case WEST -> new Basis(new Vec3(0, 0, 1), new Vec3(0, -1, 0));
            case EAST -> new Basis(new Vec3(0, 0, -1), new Vec3(0, -1, 0));
            case UP -> new Basis(new Vec3(1, 0, 0), new Vec3(0, 0, 1));
            case DOWN -> new Basis(new Vec3(1, 0, 0), new Vec3(0, 0, -1));
        };
    }

    private static double clamp(double value) {
        return Math.max(0.0D, Math.min(0.999999D, value));
    }

    public record Basis(Vec3 right, Vec3 down) {
    }
}
