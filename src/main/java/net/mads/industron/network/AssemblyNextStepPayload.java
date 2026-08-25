package net.mads.industron.network;

import io.netty.buffer.ByteBuf;
import net.mads.industron.Industron;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server-to-client state for the Create-style assembly hover tooltip. */
public record AssemblyNextStepPayload(
        boolean visible,
        String dimension,
        BlockPos pos,
        String nextStep,
        int toolProgressTicks,
        int toolDurationTicks
) implements CustomPacketPayload {
    public static final Type<AssemblyNextStepPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, "assembly_next_step")
    );

    public static final StreamCodec<ByteBuf, AssemblyNextStepPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            AssemblyNextStepPayload::visible,
            ByteBufCodecs.STRING_UTF8,
            AssemblyNextStepPayload::dimension,
            BlockPos.STREAM_CODEC,
            AssemblyNextStepPayload::pos,
            ByteBufCodecs.STRING_UTF8,
            AssemblyNextStepPayload::nextStep,
            ByteBufCodecs.VAR_INT,
            AssemblyNextStepPayload::toolProgressTicks,
            ByteBufCodecs.VAR_INT,
            AssemblyNextStepPayload::toolDurationTicks,
            AssemblyNextStepPayload::new
    );

    public static AssemblyNextStepPayload show(String dimension, BlockPos pos, String nextStep) {
        return new AssemblyNextStepPayload(true, dimension, pos.immutable(), nextStep, 0, 0);
    }

    public static AssemblyNextStepPayload showToolProgress(
            String dimension,
            BlockPos pos,
            String nextStep,
            int progressTicks,
            int durationTicks
    ) {
        return new AssemblyNextStepPayload(
                true,
                dimension,
                pos.immutable(),
                nextStep,
                Math.max(0, progressTicks),
                Math.max(0, durationTicks)
        );
    }

    public static AssemblyNextStepPayload clear() {
        return new AssemblyNextStepPayload(false, "", BlockPos.ZERO, "", 0, 0);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AssemblyNextStepPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> net.mads.industron.client.AssemblyNextStepOverlay.accept(payload));
    }
}
