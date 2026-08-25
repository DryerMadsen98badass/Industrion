package net.mads.industron.network;

import io.netty.buffer.ByteBuf;
import net.mads.industron.Industron;
import net.mads.industron.item.MultiblockDevToolItem;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockControllerBlock;
import net.mads.industron.registry.ItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MultiblockDevToolActionPayload(int action, BlockPos first, BlockPos second) implements CustomPacketPayload {
    public static final int SET_POS_1 = 0;
    public static final int SET_POS_2 = 1;
    public static final int SET_CONTROLLER = 2;
    public static final int SET_BOUNDS = 3;
    public static final int EXPORT = 4;
    public static final int CLEAR = 5;

    public static final Type<MultiblockDevToolActionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, "multiblock_dev_tool_action")
    );

    public static final StreamCodec<ByteBuf, MultiblockDevToolActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            MultiblockDevToolActionPayload::action,
            BlockPos.STREAM_CODEC,
            MultiblockDevToolActionPayload::first,
            BlockPos.STREAM_CODEC,
            MultiblockDevToolActionPayload::second,
            MultiblockDevToolActionPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MultiblockDevToolActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        ItemStack stack = player.getMainHandItem();
        if (!stack.is(ItemRegistry.MULTIBLOCK_DEV_TOOL.get())) {
            return;
        }

        switch (payload.action()) {
            case SET_POS_1 -> {
                MultiblockDevToolItem.setPos1(stack, payload.first());
                player.displayClientMessage(Component.literal("Multiblock Dev Tool: first position selected").withStyle(ChatFormatting.YELLOW), true);
            }
            case SET_POS_2 -> {
                MultiblockDevToolItem.setPos2(stack, payload.first());
                player.displayClientMessage(Component.literal("Multiblock Dev Tool: second position selected").withStyle(ChatFormatting.YELLOW), true);
            }
            case SET_CONTROLLER -> {
                BlockPos pos = payload.first();
                if (level.getBlockState(pos).isAir()) {
                    player.displayClientMessage(Component.literal("Multiblock Dev Tool: controller must be a block.").withStyle(ChatFormatting.RED), true);
                    return;
                }
                BlockState state = level.getBlockState(pos);
                Direction facing = state.getBlock() instanceof MultiblockControllerBlock
                        ? state.getValue(MultiblockControllerBlock.FACING)
                        : player.getDirection().getOpposite();
                MultiblockDevToolItem.setController(stack, pos, facing);
                player.displayClientMessage(Component.literal("Multiblock Dev Tool: controller selected").withStyle(ChatFormatting.GOLD), true);
            }
            case SET_BOUNDS -> {
                MultiblockDevToolItem.setPos1(stack, payload.first());
                MultiblockDevToolItem.setPos2(stack, payload.second());
            }
            case EXPORT -> {
                if (MultiblockDevToolItem.sendExportIfReady(level, player, stack)) {
                    MultiblockDevToolItem.clearSelection(stack);
                }
            }
            case CLEAR -> {
                MultiblockDevToolItem.clearSelection(stack);
                player.displayClientMessage(Component.literal("Multiblock selection cleared").withStyle(ChatFormatting.YELLOW), true);
            }
            default -> {
            }
        }
    }
}
