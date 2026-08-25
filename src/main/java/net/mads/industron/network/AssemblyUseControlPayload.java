package net.mads.industron.network;

import io.netty.buffer.ByteBuf;
import net.mads.industron.Industron;
import net.mads.industron.input.AssemblyUseState;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AssemblyUseControlPayload(boolean held) implements CustomPacketPayload {
    public static final Type<AssemblyUseControlPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, "assembly_use_control")
    );

    public static final StreamCodec<ByteBuf, AssemblyUseControlPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            AssemblyUseControlPayload::held,
            AssemblyUseControlPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AssemblyUseControlPayload payload, IPayloadContext context) {
        AssemblyUseState.set(context.player(), payload.held());
    }
}
