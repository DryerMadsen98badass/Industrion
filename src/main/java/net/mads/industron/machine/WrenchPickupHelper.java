package net.mads.industron.machine;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.ArrayList;
import java.util.List;

public final class WrenchPickupHelper {
    private WrenchPickupHelper() {
    }

    public static boolean isHoldingWrench(Player player) {
        return isWrench(player.getMainHandItem()) || isWrench(player.getOffhandItem());
    }

    private static boolean isWrench(ItemStack stack) {
        return !stack.isEmpty() && stack.is(Tags.Items.TOOLS_WRENCH);
    }

    public static InteractionResult pickup(Block block, BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();

        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, pos, state, player);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            return InteractionResult.SUCCESS;
        }

        if (player != null && !player.isCreative()) {
            List<ItemStack> drops = new ArrayList<>(Block.getDrops(state, serverLevel, pos, level.getBlockEntity(pos), player, context.getItemInHand()));
            if (drops.stream().noneMatch(stack -> stack.is(block.asItem()))) {
                drops.add(0, new ItemStack(block));
            }
            drops.forEach(stack -> giveOrDrop(player, stack));
        }

        state.spawnAfterBreak(serverLevel, pos, ItemStack.EMPTY, true);
        level.destroyBlock(pos, false);
        IWrenchable.playRemoveSound(level, pos);
        return InteractionResult.SUCCESS;
    }

    private static void giveOrDrop(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        ItemStack remaining = stack.copy();
        if (!player.addItem(remaining) && !remaining.isEmpty()) {
            player.drop(remaining, false);
        }
    }
}
