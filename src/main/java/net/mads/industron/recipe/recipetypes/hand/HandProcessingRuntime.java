package net.mads.industron.recipe.recipetypes.hand;

import net.mads.industron.Industron;
import net.mads.industron.control.ControlKeyState;
import net.mads.industron.recipe.CEChancedItemOutput;
import net.mads.industron.recipe.CERecipe;
import net.mads.industron.recipe.CERecipeInput;
import net.mads.industron.recipe.CERecipeLookup;
import net.mads.industron.recipe.CERecipeTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Server-authoritative right-click runtime for recipes performed directly in the player's hand. */
@EventBusSubscriber(modid = Industron.MOD_ID)
public final class HandProcessingRuntime {
    private static final Map<UUID, ActiveHandProcessing> ACTIVE = new HashMap<>();

    private HandProcessingRuntime() {
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || event.getLevel().isClientSide
                || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            ACTIVE.remove(player.getUUID());
            return;
        }

        ActiveHandProcessing active = ACTIVE.get(player.getUUID());
        Optional<RecipeHolder<CERecipe>> holder = active == null
                ? Optional.empty()
                : activeRecipe(player, active, held);

        if (holder.isEmpty()) {
            ACTIVE.remove(player.getUUID());
            // Ctrl is required only to intentionally start a Hand Processing recipe.
            if (!ControlKeyState.isHeld(player)) return;
            holder = findRecipe(player, held);
            if (holder.isEmpty()) return;
            active = new ActiveHandProcessing(holder.get().id());
            ACTIVE.put(player.getUUID(), active);
        }

        CERecipe recipe = holder.get().value();
        int requiredUses = recipe.manualUses().orElse(0);
        if (requiredUses < 1) {
            ACTIVE.remove(player.getUUID());
            return;
        }

        active.completedUses++;
        if (active.completedUses >= requiredUses) {
            complete(player, recipe, held);
            ACTIVE.remove(player.getUUID());
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static Optional<RecipeHolder<CERecipe>> findRecipe(ServerPlayer player, ItemStack held) {
        CERecipeInput input = CERecipeInput.of(List.of(held), List.of());
        return CERecipeLookup.byType(player.serverLevel().getRecipeManager(), CERecipeTypes.HAND_PROCESSING).stream()
                .filter(holder -> validHandRecipe(holder.value()))
                .filter(holder -> holder.value().matches(input, player.serverLevel()))
                .findFirst();
    }

    private static Optional<RecipeHolder<CERecipe>> activeRecipe(
            ServerPlayer player,
            ActiveHandProcessing active,
            ItemStack held
    ) {
        CERecipeInput input = CERecipeInput.of(List.of(held), List.of());
        return CERecipeLookup.byId(player.serverLevel().getRecipeManager(), active.recipeId)
                .filter(holder -> holder.value().recipeType().equals(CERecipeTypes.HAND_PROCESSING.id()))
                .filter(holder -> validHandRecipe(holder.value()))
                .filter(holder -> holder.value().matches(input, player.serverLevel()));
    }

    private static boolean validHandRecipe(CERecipe recipe) {
        return recipe.manualUses().isPresent()
                && recipe.itemInputs().size() == 1
                && recipe.chancedItemInputs().isEmpty()
                && recipe.notConsumableItems().isEmpty()
                && recipe.fluidInputs().isEmpty()
                && recipe.chancedFluidInputs().isEmpty()
                && recipe.notConsumableFluids().isEmpty()
                && recipe.tools().isEmpty()
                && !recipe.itemOutputs().isEmpty()
                && recipe.fluidOutputs().isEmpty()
                && recipe.chancedFluidOutputs().isEmpty();
    }

    private static void complete(ServerPlayer player, CERecipe recipe, ItemStack held) {
        int consumed = recipe.itemInputs().getFirst().count();
        if (!player.isCreative()) {
            held.shrink(consumed);
        }

        for (CEChancedItemOutput output : recipe.itemOutputs()) {
            int chance = output.effectiveChance(Optional.empty(), recipe.requiredTier());
            if (chance <= 0 || (chance < CEChancedItemOutput.MAX_CHANCE
                    && player.serverLevel().random.nextInt(CEChancedItemOutput.MAX_CHANCE) >= chance)) {
                continue;
            }
            ItemStack result = output.stack().copy();
            if (!result.isEmpty() && !player.addItem(result)) {
                player.drop(result, false);
            }
        }
    }

    private static final class ActiveHandProcessing {
        private final ResourceLocation recipeId;
        private int completedUses;

        private ActiveHandProcessing(ResourceLocation recipeId) {
            this.recipeId = recipeId;
        }
    }
}
