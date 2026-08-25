package net.mads.industron.network;

import io.netty.buffer.ByteBuf;
import net.mads.industron.Industron;
import net.mads.industron.transport.color.PipePaintControlState;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PipePaintControlPayload(boolean held) implements CustomPacketPayload {
    public static final Type<PipePaintControlPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, "pipe_paint_control")
    );

    public static final StreamCodec<ByteBuf, PipePaintControlPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            PipePaintControlPayload::held,
            PipePaintControlPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PipePaintControlPayload payload, IPayloadContext context) {
        PipePaintControlState.set(context.player(), payload.held());
    }
}
