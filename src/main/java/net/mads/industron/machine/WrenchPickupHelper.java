package net.mads.industron.machine;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;

/**
 * Shared wrench handling for technical Industron blocks.
 *
 * <p>Technical blocks are intentionally not portable through Create's sneak-wrench pickup path:
 * they must be broken through Industron's tiered breaking system so their salvage loot table is
 * authoritative. Returning SUCCESS consumes the wrench interaction without destroying the block.</p>
 */
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
        return InteractionResult.SUCCESS;
    }
}
