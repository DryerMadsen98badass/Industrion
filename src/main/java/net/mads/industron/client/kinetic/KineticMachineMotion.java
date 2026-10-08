package net.mads.industron.client.kinetic;

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.mads.industron.integration.create.kinetic.CEKineticRecipeHost;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Direction;

/** The same pose calculation is used by Flywheel and the fallback renderer. */
public final class KineticMachineMotion {
    public static String id(KineticBlockEntity be) {
        return BuiltInRegistries.BLOCK.getKey(be.getBlockState().getBlock()).getPath();
    }
    public static Direction facing(KineticBlockEntity be) {
        return be.getBlockState().getValue(HorizontalKineticBlock.HORIZONTAL_FACING);
    }
    public static int facingDegrees(Direction facing) {
        return switch (facing) { case EAST -> 90; case SOUTH -> 180; case WEST -> 270; default -> 0; };
    }
    public static <T extends KineticBlockEntity & CEKineticRecipeHost> float[] offset(T be, float pt) {
        var ce = be.ceProcessing();
        float p = ce.progress(pt);
        float angle = KineticBlockEntityRenderer.getAngleForBe(be, be.getBlockPos(), KineticBlockEntityRenderer.getRotationAxisOf(be));
        float x = 0, y = 0, z = 0;
        boolean working = ce.logic().isActive();
        switch (id(be)) {
            case "lathe" -> x = p * 5F / 16F;
            case "mechanical_centrifuge" -> y = ce.logic().isProcessing() ? 0 : 1F / 16F;
            case "mechanical_sifter" -> z = (float) Math.sin(angle) * 0.045F;
            case "pulverizer" -> y = working ? (1F + (float) Math.sin(angle)) * 0.025F : 0;
            case "wire_drawing_machine" -> z = working ? (float) Math.sin(angle) * 0.012F : 0;
            case "winding_machine" -> x = (1F - (float) Math.cos(p * Math.PI * 4)) * 2.5F / 16F;
            case "mechanical_bender" -> z = working ? p * 2F / 16F : 0;
            case "magnetic_separator" -> x = working ? (float) Math.sin(angle) * 0.03F : 0;
            default -> { }
        }
        return switch (facing(be)) {
            case EAST -> new float[]{-z, y, x};
            case SOUTH -> new float[]{-x, y, -z};
            case WEST -> new float[]{z, y, -x};
            default -> new float[]{x, y, z};
        };
    }
    private KineticMachineMotion() {}
}
