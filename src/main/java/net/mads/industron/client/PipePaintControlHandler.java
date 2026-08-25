package net.mads.industron.client;

import net.mads.industron.Industron;
import net.mads.industron.network.PipePaintControlPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Industron.MOD_ID, value = Dist.CLIENT)
public final class PipePaintControlHandler {
    private static boolean lastSent;

    private PipePaintControlHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        sendIfChanged(Screen.hasControlDown());
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        sendIfChanged(Screen.hasControlDown());
    }

    private static void sendIfChanged(boolean held) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null) {
            lastSent = false;
            return;
        }

        if (held == lastSent) {
            return;
        }

        lastSent = held;
        PacketDistributor.sendToServer(new PipePaintControlPayload(held));
    }
}
