package net.mads.industron.client;

import net.mads.industron.Industron;
import net.mads.industron.network.AssemblyUseControlPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Industron.MOD_ID, value = Dist.CLIENT)
public final class AssemblyUseControlHandler {
    private static boolean lastSent;

    private AssemblyUseControlHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null) {
            lastSent = false;
            return;
        }

        boolean held = minecraft.screen == null && minecraft.options.keyUse.isDown();
        if (held == lastSent) return;

        lastSent = held;
        PacketDistributor.sendToServer(new AssemblyUseControlPayload(held));
    }
}
