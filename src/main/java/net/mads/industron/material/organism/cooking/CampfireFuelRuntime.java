package net.mads.industron.material.organism.cooking;

import net.mads.industron.Industron;
import net.mads.industron.recipe.recipetypes.FuelRecipeLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Fuel runtime for vanilla campfires.
 *
 * <p>Campfires have four dedicated one-item fuel slots in addition to vanilla's four cooking
 * slots. Fuel validity/value comes exclusively from the canonical Industron FUEL recipe table.
 * One Fuel Unit is worth six burn ticks, so 600 FU burns for 3600 ticks = 3 minutes.</p>
 */
@EventBusSubscriber(modid = Industron.MOD_ID)
public final class CampfireFuelRuntime {
    public static final int MAX_FUEL_ITEMS = 4;
    public static final double TICKS_PER_FUEL_UNIT = 6.0D;

    private static final ResourceLocation FLINT_AND_PEBBLE =
            ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, "flint_and_pebble");

    private CampfireFuelRuntime() {
    }

    /** Called before a lit campfire performs one cooking tick. */
    public static boolean prepareBurn(Level level, BlockPos pos, BlockState state, CampfireBlockEntity fire) {
        if (level.isClientSide || !state.getValue(CampfireBlock.LIT)) return true;
        CampfireFuelAccess access = (CampfireFuelAccess) fire;
        if (access.industron$fuelTicks() > 0) return true;
        if (startNextFuel(level, fire, access)) return true;
        extinguish(level, pos, state, fire);
        return false;
    }

    /** Called after exactly one cooking tick has been allowed to happen. */
    public static void finishBurn(Level level, BlockPos pos, BlockState state, CampfireBlockEntity fire) {
        if (level.isClientSide) return;
        CampfireFuelAccess access = (CampfireFuelAccess) fire;
        int remaining = access.industron$fuelTicks();
        if (remaining <= 0) return;
        if (remaining == 1) {
            access.industron$setFuelTicks(0);
            access.industron$setActiveFuelSlot(-1);
            fire.setChanged();
            if (!hasQueuedUsableFuel(level, access.industron$fuelItems())) {
                extinguish(level, pos, state, fire);
            }
            return;
        }

        access.industron$setFuelTicks(remaining - 1);
        fire.setChanged();
    }

    private static boolean startNextFuel(Level level, CampfireBlockEntity fire, CampfireFuelAccess access) {
        NonNullList<ItemStack> fuels = access.industron$fuelItems();
        access.industron$setActiveFuelSlot(-1);
        for (int slot = 0; slot < fuels.size(); slot++) {
            ItemStack stored = fuels.get(slot);
            if (stored.isEmpty()) continue;

            double fuelUnits = FuelRecipeLookup.itemFuelUnits(level, stored);
            if (fuelUnits <= 0.0D) continue;

            int ticks = Math.max(1, (int) Math.ceil(fuelUnits * TICKS_PER_FUEL_UNIT));
            fuels.set(slot, ItemStack.EMPTY);
            access.industron$setActiveFuelSlot(slot);
            access.industron$setFuelTicks(ticks);
            fire.setChanged();
            return true;
        }
        return false;
    }

    public static boolean hasUsableFuel(Level level, CampfireBlockEntity fire) {
        CampfireFuelAccess access = (CampfireFuelAccess) fire;
        return access.industron$fuelTicks() > 0 || hasQueuedUsableFuel(level, access.industron$fuelItems());
    }

    private static boolean hasQueuedUsableFuel(Level level, NonNullList<ItemStack> fuels) {
        for (ItemStack stack : fuels) {
            if (!stack.isEmpty() && FuelRecipeLookup.itemFuelUnits(level, stack) > 0.0D) return true;
        }
        return false;
    }

    private static boolean insertOneFuel(Level level, CampfireBlockEntity fire, ItemStack held, Player player) {
        if (held.isEmpty() || FuelRecipeLookup.itemFuelUnits(level, held) <= 0.0D) return false;
        if (isCookable(level, held)) return false;

        CampfireFuelAccess access = (CampfireFuelAccess) fire;
        NonNullList<ItemStack> fuels = access.industron$fuelItems();
        for (int slot = 0; slot < fuels.size(); slot++) {
            if (slot == access.industron$activeFuelSlot() && access.industron$fuelTicks() > 0) continue;
            if (!fuels.get(slot).isEmpty()) continue;
            fuels.set(slot, held.copyWithCount(1));
            if (!player.getAbilities().instabuild) held.shrink(1);
            fire.setChanged();
            BlockState state = fire.getBlockState();
            level.sendBlockUpdated(fire.getBlockPos(), state, state, Block.UPDATE_ALL);
            level.gameEvent(player, GameEvent.BLOCK_CHANGE, fire.getBlockPos());
            return true;
        }
        return false;
    }

    private static boolean isCookable(Level level, ItemStack stack) {
        if (OrganicCampfireRuntime.definition(stack) != null) return true;
        return level.getRecipeManager()
                .getRecipeFor(RecipeType.CAMPFIRE_COOKING, new SingleRecipeInput(stack), level)
                .isPresent();
    }

    private static boolean isIgniter(ItemStack stack) {
        if (stack.is(Items.FLINT_AND_STEEL)) return true;
        return FLINT_AND_PEBBLE.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    private static boolean isForbiddenIgniter(ItemStack stack) {
        return stack.is(Items.FIRE_CHARGE);
    }

    private static void ignite(Level level, BlockPos pos, BlockState state, CampfireBlockEntity fire, Player player,
                               InteractionHand hand, ItemStack igniter) {
        if (!hasUsableFuel(level, fire)) return;

        BlockState lit = state.setValue(CampfireBlock.LIT, true);
        level.setBlock(pos, lit, Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F,
                level.getRandom().nextFloat() * 0.4F + 0.8F);
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);

        if (!player.getAbilities().instabuild && igniter.isDamageableItem()) {
            igniter.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND
                    ? EquipmentSlot.MAINHAND
                    : EquipmentSlot.OFFHAND);
        }
    }

    private static void extinguish(Level level, BlockPos pos, BlockState state, CampfireBlockEntity fire) {
        if (!state.getValue(CampfireBlock.LIT)) return;
        CampfireBlock.dowse(null, level, pos, state);
        level.setBlock(pos, state.setValue(CampfireBlock.LIT, false), Block.UPDATE_ALL);
        fire.setChanged();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel().getBlockEntity(event.getPos()) instanceof CampfireBlockEntity fire)) return;
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!(state.getBlock() instanceof CampfireBlock)) return;

        ItemStack held = event.getItemStack();

        // Campfires in Industron may only be lit by Flint and Steel or Flint and Pebble.
        if (!state.getValue(CampfireBlock.LIT) && isForbiddenIgniter(held)) {
            own(event, InteractionResult.FAIL);
            return;
        }
        if (!state.getValue(CampfireBlock.LIT) && isIgniter(held)) {
            if (event.getLevel().isClientSide) {
                own(event, InteractionResult.SUCCESS);
                return;
            }
            boolean hasFuel = hasUsableFuel(event.getLevel(), fire);
            if (hasFuel) ignite(event.getLevel(), event.getPos(), state, fire, event.getEntity(), event.getHand(), held);
            own(event, hasFuel ? InteractionResult.SUCCESS : InteractionResult.FAIL);
            return;
        }

        // Fuel is deliberately one item per interaction and never shares the cooking inventory.
        if (!held.isEmpty()
                && FuelRecipeLookup.itemFuelUnits(event.getLevel(), held) > 0.0D
                && !isCookable(event.getLevel(), held)) {
            if (!event.getLevel().isClientSide) {
                insertOneFuel(event.getLevel(), fire, held, event.getEntity());
            }
            own(event, InteractionResult.SUCCESS);
        }
    }

    /** Drops the four custom fuel slots when a player breaks the campfire. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(level.getBlockEntity(event.getPos()) instanceof CampfireBlockEntity fire)) return;
        CampfireFuelAccess access = (CampfireFuelAccess) fire;
        for (int i = 0; i < access.industron$fuelItems().size(); i++) {
            ItemStack stack = access.industron$fuelItems().get(i);
            if (stack.isEmpty()) continue;
            Containers.dropItemStack(level, event.getPos().getX() + 0.5D,
                    event.getPos().getY() + 0.5D, event.getPos().getZ() + 0.5D, stack.copy());
            access.industron$fuelItems().set(i, ItemStack.EMPTY);
        }
        fire.setChanged();
    }

    private static void own(PlayerInteractEvent.RightClickBlock event, InteractionResult result) {
        event.setCanceled(true);
        event.setCancellationResult(result);
    }
}
