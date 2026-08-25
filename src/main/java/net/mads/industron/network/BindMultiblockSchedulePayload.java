package net.mads.industron.network;

import io.netty.buffer.ByteBuf;
import net.mads.industron.Industron;
import net.mads.industron.machine.control.MachineControlScheduleItem;
import net.mads.industron.registry.ItemRegistry;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BindMultiblockSchedulePayload(
        String definitionId,
        String variantId,
        String tierId,
        int handIndex
) implements CustomPacketPayload {
    public static final Type<BindMultiblockSchedulePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, "bind_multiblock_schedule")
    );

    public static final StreamCodec<ByteBuf, BindMultiblockSchedulePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            BindMultiblockSchedulePayload::definitionId,
            ByteBufCodecs.STRING_UTF8,
            BindMultiblockSchedulePayload::variantId,
            ByteBufCodecs.STRING_UTF8,
            BindMultiblockSchedulePayload::tierId,
            ByteBufCodecs.VAR_INT,
            BindMultiblockSchedulePayload::handIndex,
            BindMultiblockSchedulePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BindMultiblockSchedulePayload payload, IPayloadContext context) {
        InteractionHand hand = payload.handIndex() == InteractionHand.OFF_HAND.ordinal()
                ? InteractionHand.OFF_HAND
                : InteractionHand.MAIN_HAND;
        ItemStack stack = context.player().getItemInHand(hand);
        if (!stack.is(ItemRegistry.MACHINE_CONTROL_SCHEDULE.get())) {
            return;
        }

        MachineControlScheduleItem.setMultiblockBuildTarget(
                stack,
                new MachineControlScheduleItem.MultiblockBuildTarget(
                        payload.definitionId(),
                        payload.variantId(),
                        payload.tierId()
                )
        );
    }
}
