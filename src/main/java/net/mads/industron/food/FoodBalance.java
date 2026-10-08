package net.mads.industron.food;

import net.mads.industron.Industron;
import net.minecraft.core.component.DataComponents;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

/** Global food rules: small stacks and a much slower survival hunger economy. */
@EventBusSubscriber(modid = Industron.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class FoodBalance {
    /** Every edible item, vanilla or modded, stacks to at most four. */
    public static final int FOOD_STACK_SIZE = 4;

    /** Vanilla activity still matters, but hunger builds far more slowly. */
    public static final float ACTIVITY_EXHAUSTION_MULTIPLIER = 0.05F;

    /**
     * Two food points (one visible hunger bar) cost eight exhaustion.
     * Adding eight exhaustion over one 24,000-tick Minecraft day therefore
     * gives roughly one hunger bar of passive daily food use.
     */
    public static final float PASSIVE_EXHAUSTION_PER_TICK = 8.0F / 24_000.0F;

    private FoodBalance() {
    }

    /**
     * FOOD is a default data component in 1.21.1, so this also catches vanilla
     * food and any future Industron food without maintaining an item list.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void modifyFoodStacks(ModifyDefaultComponentsEvent event) {
        event.modifyMatching(
                item -> item.components().has(DataComponents.FOOD),
                builder -> builder.set(DataComponents.MAX_STACK_SIZE, FOOD_STACK_SIZE)
        );
    }
}
