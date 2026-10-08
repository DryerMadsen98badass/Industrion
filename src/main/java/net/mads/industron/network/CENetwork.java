package net.mads.industron.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class CENetwork {
    private CENetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(ClimateStatePayload.TYPE,ClimateStatePayload.STREAM_CODEC,ClimateStatePayload::handle);
        registrar.playToServer(
                BindMultiblockSchedulePayload.TYPE,
                BindMultiblockSchedulePayload.STREAM_CODEC,
                BindMultiblockSchedulePayload::handle
        );
        registrar.playToServer(
                AssemblyUseControlPayload.TYPE,
                AssemblyUseControlPayload.STREAM_CODEC,
                AssemblyUseControlPayload::handle
        );
        registrar.playToClient(
                AssemblyNextStepPayload.TYPE,
                AssemblyNextStepPayload.STREAM_CODEC,
                AssemblyNextStepPayload::handle
        );
        registrar.playToServer(
                PipePaintControlPayload.TYPE,
                PipePaintControlPayload.STREAM_CODEC,
                PipePaintControlPayload::handle
        );
        registrar.playToServer(
                MultiblockDevToolActionPayload.TYPE,
                MultiblockDevToolActionPayload.STREAM_CODEC,
                MultiblockDevToolActionPayload::handle
        );
    }
}
