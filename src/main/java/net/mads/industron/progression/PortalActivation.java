package net.mads.industron.progression;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.PortalShape;

/** Vanilla fire cannot open a dimensional gateway. Scope is local to this activation call. */
public final class PortalActivation {
    private static final ThreadLocal<Boolean> ACTIVATING=ThreadLocal.withInitial(()->false);
    private PortalActivation() {}

    public static boolean allowed() { return ACTIVATING.get(); }

    public static boolean activate(Level level,BlockPos pos) {
        if(level.isClientSide || (!level.dimension().equals(Level.OVERWORLD)&&!level.dimension().equals(Level.NETHER)))return false;
        var shape=PortalShape.findEmptyPortalShape(level,pos,Direction.Axis.X);
        if(shape.isEmpty())return false;
        boolean previous=ACTIVATING.get();
        ACTIVATING.set(true);
        try { shape.get().createPortalBlocks(); return true; }
        finally { if(previous)ACTIVATING.set(true);else ACTIVATING.remove(); }
    }
}
