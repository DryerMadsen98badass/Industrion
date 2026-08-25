package net.mads.industron.transport.color;

import net.mads.industron.Industron;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = Industron.MOD_ID)
public final class PipeColorEvents {
    private PipeColorEvents() {
    }

    @SubscribeEvent
    public static void onRightClickPipe(PlayerInteractEvent.RightClickBlock event) {
        ItemStack held = event.getItemStack();
        if (!(held.getItem() instanceof DyeItem dyeItem)) {
            return;
        }

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        if (!PipeColorManager.isColorablePipe(level.getBlockState(pos))
                || PipeColorManager.getFamily(level.getBlockState(pos)) == null) {
            return;
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
        if (level.isClientSide) {
            return;
        }

        Player player = event.getEntity();
        DyeColor color = dyeItem.getDyeColor();
        if (PipePaintControlState.isHeld(player)) {
            paintLine(level, pos, player, held, color);
        } else {
            paintOne(level, pos, player, held, color);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        PipePaintControlState.clear(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        PipePaintControlState.clear(event.getEntity());
    }

    private static void paintLine(Level level, BlockPos start, Player player, ItemStack held, DyeColor color) {
        Direction direction = player.getDirection();
        BlockPos.MutableBlockPos cursor = start.mutable();

        while (level.isLoaded(cursor)) {
            if (!PipeColorManager.isColorablePipe(level.getBlockState(cursor))
                    || PipeColorManager.getFamily(level.getBlockState(cursor)) == null) {
                break;
            }

            DyeColor current = PipeColorManager.getColor(level, cursor);
            if (current != color) {
                if (!player.isCreative() && held.isEmpty()) {
                    break;
                }
                if (PipeColorManager.setColor(level, cursor, color) && !player.isCreative()) {
                    held.shrink(1);
                }
            }

            cursor.move(direction);
        }
    }

    private static void paintOne(Level level, BlockPos pos, Player player, ItemStack held, DyeColor color) {
        if (PipeColorManager.getColor(level, pos) == color) {
            return;
        }
        if (PipeColorManager.setColor(level, pos, color) && !player.isCreative()) {
            held.shrink(1);
        }
    }
}
