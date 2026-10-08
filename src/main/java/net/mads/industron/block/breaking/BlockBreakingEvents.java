package net.mads.industron.block.breaking;

import net.mads.industron.Industron;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/** Server-side drop policy for the tiered breaking model. */
@EventBusSubscriber(modid = Industron.MOD_ID)
public final class BlockBreakingEvents {
    private BlockBreakingEvents() {
    }

    @SubscribeEvent
    public static void onHarvestCheck(PlayerEvent.HarvestCheck event) {
        Player player = event.getEntity();
        if (player.getAbilities().instabuild) {
            return;
        }

        if (!BlockBreakingCalculator.shouldOverride(
                player,
                event.getTargetBlock(),
                event.getLevel(),
                event.getPos()
        )) {
            return;
        }

        // In the Industron model tool family controls speed, not harvesting. Tier alone decides loot.
        event.setCanHarvest(BlockBreakingCalculator.hasSufficientTier(
                player.getMainHandItem(),
                event.getTargetBlock(),
                event.getLevel(),
                event.getPos()
        ));
    }

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof Player player)) {
            return;
        }
        if (player.getAbilities().instabuild) {
            return;
        }

        BlockBreakingProfile profile = BlockBreakingResolver.resolve(
                event.getState(),
                event.getLevel(),
                event.getPos()
        );
        boolean industronOwned = profile.explicitIndustronProfile()
                || BlockBreakingCalculator.isRegisteredTool(event.getTool());
        if (!industronOwned) {
            return;
        }

        if (!BlockBreakingCalculator.hasSufficientTier(event.getTool(), profile)) {
            // Tier is the only gate for block loot. Tool family only changes breaking speed.
            // This clears self-drops, Silk Touch/Fortune results and Assembly salvage alike.
            event.getDrops().clear();
            event.setDroppedExperience(0);
        }
    }
}
