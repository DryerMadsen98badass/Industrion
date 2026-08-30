package net.mads.industron.transport.color;

import net.mads.industron.control.ControlKeyState;
import net.minecraft.world.entity.player.Player;

/** Compatibility facade: pipe painting and assembly share the same physical Ctrl key state. */
public final class PipePaintControlState {
    private PipePaintControlState() {
    }

    public static void set(Player player, boolean held) {
        ControlKeyState.set(player, held);
    }

    public static boolean isHeld(Player player) {
        return ControlKeyState.isHeld(player);
    }

    public static void clear(Player player) {
        ControlKeyState.clear(player);
    }
}
