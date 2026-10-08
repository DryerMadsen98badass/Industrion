package net.mads.industron.recipe.recipetypes;

import net.mads.industron.Industron;
import net.mads.industron.control.ControlKeyState;
import net.mads.industron.network.AssemblyNextStepPayload;
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
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Server-authoritative click-count runtime for Hand Processing.
 *
 * <p>Ctrl is required only for the first click, matching the intentional-start rule used by
 * Assembly. Once active, each later right click advances the same recipe while the required
 * main-hand input remains present. Switching/removing the input resets progress.</p>
 */
@EventBusSubscriber(modid = Industron.MOD_ID)
public final class HandProcessingRuntime {
    private static final Map<UUID, ActiveProcess> ACTIVE = new HashMap<>();

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
            clear(player);
            return;
        }

        ActiveProcess active = ACTIVE.get(player.getUUID());
        Optional<RecipeHolder<CERecipe>> holder = active == null
                ? Optional.empty()
                : activeRecipe(player, active, held);

        if (holder.isEmpty()) {
            if (active != null) clear(player);
            if (!ControlKeyState.isHeld(player)) return;

            holder = findRecipe(player, held);
            if (holder.isEmpty()) return;

            active = new ActiveProcess(holder.get().id());
            ACTIVE.put(player.getUUID(), active);
        }

        RecipeHolder<CERecipe> selected = holder.get();
        CERecipe recipe = selected.value();
        int requiredUses = recipe.manualUses().orElseThrow(() ->
                new IllegalStateException("Hand Processing recipe has no uses: " + selected.id())
        );

        active.completedUses++;
        sendProgress(player, recipe, active.completedUses, requiredUses);

        if (active.completedUses >= requiredUses) {
            complete(player, recipe);
            clear(player);
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    /** Clears progress immediately when the active input is no longer held/matching. */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        Iterator<Map.Entry<UUID, ActiveProcess>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ActiveProcess> entry = iterator.next();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }

            if (activeRecipe(player, entry.getValue(), player.getMainHandItem()).isEmpty()) {
                PacketDistributor.sendToPlayer(player, AssemblyNextStepPayload.clear());
                iterator.remove();
            }
        }
    }

    private static Optional<RecipeHolder<CERecipe>> findRecipe(ServerPlayer player, ItemStack held) {
        CERecipeInput input = CERecipeInput.of(List.of(held), List.of());
        return CERecipeLookup.byType(player.serverLevel().getRecipeManager(), CERecipeTypes.HAND_PROCESSING).stream()
                .filter(holder -> holder.value().matches(input, player.serverLevel()))
                .findFirst();
    }

    private static Optional<RecipeHolder<CERecipe>> activeRecipe(
            ServerPlayer player,
            ActiveProcess active,
            ItemStack held
    ) {
        if (held.isEmpty()) return Optional.empty();
        CERecipeInput input = CERecipeInput.of(List.of(held), List.of());
        return CERecipeLookup.byId(player.serverLevel().getRecipeManager(), active.recipeId)
                .filter(holder -> holder.value().recipeType().equals(CERecipeTypes.HAND_PROCESSING.id()))
                .filter(holder -> holder.value().matches(input, player.serverLevel()));
    }

    private static void complete(ServerPlayer player, CERecipe recipe) {
        ItemStack held = player.getMainHandItem();
        int consumed = recipe.itemInputs().getFirst().count();
        if (!player.isCreative()) {
            held.shrink(consumed);
        }

        for (CEChancedItemOutput output : recipe.itemOutputs()) {
            if (!output.guaranteed()
                    && player.serverLevel().random.nextInt(CEChancedItemOutput.MAX_CHANCE) >= output.chance()) {
                continue;
            }
            ItemStack result = output.stack().copy();
            if (!result.isEmpty() && !player.addItem(result)) {
                player.drop(result, false);
            }
        }
    }

    private static void sendProgress(
            ServerPlayer player,
            CERecipe recipe,
            int completedUses,
            int requiredUses
    ) {
        PacketDistributor.sendToPlayer(player, AssemblyNextStepPayload.showToolProgress(
                "Hand Processing",
                player.serverLevel().dimension().location().toString(),
                player.blockPosition(),
                "Hand use " + Math.min(completedUses, requiredUses) + "/" + requiredUses,
                Math.min(completedUses, requiredUses),
                requiredUses
        ));
    }

    private static void clear(ServerPlayer player) {
        ACTIVE.remove(player.getUUID());
        PacketDistributor.sendToPlayer(player, AssemblyNextStepPayload.clear());
    }

    private static final class ActiveProcess {
        private final ResourceLocation recipeId;
        private int completedUses;

        private ActiveProcess(ResourceLocation recipeId) {
            this.recipeId = recipeId;
        }
    }
}
