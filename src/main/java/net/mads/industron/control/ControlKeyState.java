package net.mads.industron.control;

import net.minecraft.world.entity.player.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-side mirror of whether a player currently holds Ctrl. */
public final class ControlKeyState {
    private static final Set<UUID> HELD = ConcurrentHashMap.newKeySet();

    private ControlKeyState() {
    }

    public static void set(Player player, boolean held) {
        if (player == null) return;
        if (held) HELD.add(player.getUUID());
        else HELD.remove(player.getUUID());
    }

    public static boolean isHeld(Player player) {
        return player != null && HELD.contains(player.getUUID());
    }

    public static void clear(Player player) {
        if (player != null) HELD.remove(player.getUUID());
    }
}
