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
        String title,
        String nextStep,
        int toolProgressTicks,
        int toolDurationTicks
) implements CustomPacketPayload {
    public static final Type<AssemblyNextStepPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, "assembly_next_step")
    );

    public static final StreamCodec<ByteBuf, AssemblyNextStepPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public AssemblyNextStepPayload decode(ByteBuf buffer) {
            return new AssemblyNextStepPayload(
                    ByteBufCodecs.BOOL.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    BlockPos.STREAM_CODEC.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer)
            );
        }

        @Override
        public void encode(ByteBuf buffer, AssemblyNextStepPayload payload) {
            ByteBufCodecs.BOOL.encode(buffer, payload.visible());
            ByteBufCodecs.STRING_UTF8.encode(buffer, payload.dimension());
            BlockPos.STREAM_CODEC.encode(buffer, payload.pos());
            ByteBufCodecs.STRING_UTF8.encode(buffer, payload.title());
            ByteBufCodecs.STRING_UTF8.encode(buffer, payload.nextStep());
            ByteBufCodecs.VAR_INT.encode(buffer, payload.toolProgressTicks());
            ByteBufCodecs.VAR_INT.encode(buffer, payload.toolDurationTicks());
        }
    };

    public static AssemblyNextStepPayload show(String dimension, BlockPos pos, String nextStep) {
        return show("Assembly", dimension, pos, nextStep);
    }

    public static AssemblyNextStepPayload show(String title, String dimension, BlockPos pos, String nextStep) {
        return new AssemblyNextStepPayload(true, dimension, pos.immutable(), title, nextStep, 0, 0);
    }

    public static AssemblyNextStepPayload showToolProgress(
            String dimension,
            BlockPos pos,
            String nextStep,
            int progressTicks,
            int durationTicks
    ) {
        return showToolProgress("Assembly", dimension, pos, nextStep, progressTicks, durationTicks);
    }

    public static AssemblyNextStepPayload showToolProgress(
            String title,
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
                title,
                nextStep,
                Math.max(0, progressTicks),
                Math.max(0, durationTicks)
        );
    }

    public static AssemblyNextStepPayload clear() {
        return new AssemblyNextStepPayload(false, "", BlockPos.ZERO, "", "", 0, 0);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AssemblyNextStepPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> net.mads.industron.client.AssemblyNextStepOverlay.accept(payload));
    }
}
