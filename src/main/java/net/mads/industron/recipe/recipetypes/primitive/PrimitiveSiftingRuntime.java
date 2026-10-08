package net.mads.industron.recipe.recipetypes.primitive;

import net.mads.industron.Industron;
import net.mads.industron.network.AssemblyNextStepPayload;
import net.mads.industron.recipe.CEChancedItemOutput;
import net.mads.industron.recipe.CERecipe;
import net.mads.industron.recipe.CERecipeInput;
import net.mads.industron.recipe.CERecipeLookup;
import net.mads.industron.recipe.CERecipeTypes;
import net.mads.industron.recipe.CEToolRequirement;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyTools;
import net.mads.industron.recipe.recipetypes.assembly.ToolVariantDefinition;
import net.mads.industron.recipe.recipetypes.assembly.input.AssemblyUseState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
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

/** Server-authoritative two-hand runtime for the primitive sieve recipe type. */
@EventBusSubscriber(modid = Industron.MOD_ID)
public final class PrimitiveSiftingRuntime {
    private static final Map<UUID, ActiveSifting> ACTIVE = new HashMap<>();

    private PrimitiveSiftingRuntime() {
    }

    /** The sifter is held in the main hand; the recipe input dust is held in the offhand. */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || event.getLevel().isClientSide
                || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack toolStack = player.getMainHandItem();
        ItemStack inputStack = player.getOffhandItem();
        Optional<RecipeHolder<CERecipe>> recipe = findRecipe(player.serverLevel(), inputStack, toolStack);
        if (recipe.isEmpty()) return;

        CEToolRequirement requirement = recipe.get().value().tools().getFirst();
        ToolVariantDefinition tool = AssemblyTools.find(requirement.toolId(), toolStack);
        if (tool == null) return;

        ActiveSifting active = ACTIVE.get(player.getUUID());
        if (active == null || !active.recipeId.equals(recipe.get().id())) {
            active = new ActiveSifting(recipe.get().id());
            ACTIVE.put(player.getUUID(), active);
        }
        sendProgress(player, requirement, active.progressTicks, Math.max(1, tool.useTimeTicks()));
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        Iterator<Map.Entry<UUID, ActiveSifting>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ActiveSifting> entry = iterator.next();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }

            ActiveSifting active = entry.getValue();
            Optional<RecipeHolder<CERecipe>> recipeHolder = CERecipeLookup.byId(
                    player.serverLevel().getRecipeManager(),
                    active.recipeId
            ).filter(holder -> holder.value().recipeType().equals(CERecipeTypes.PRIMITIVE_SIFTING.id()));
            if (recipeHolder.isEmpty() || !tick(player, recipeHolder.get(), active)) {
                clear(player);
                iterator.remove();
            }
        }
    }

    /** Returns true while the current operation should remain active. */
    private static boolean tick(ServerPlayer player, RecipeHolder<CERecipe> holder, ActiveSifting active) {
        CERecipe recipe = holder.value();
        if (recipe.tools().isEmpty() || !AssemblyUseState.isHeld(player)) return false;

        CEToolRequirement requirement = recipe.tools().getFirst();
        ItemStack toolStack = player.getMainHandItem();
        ItemStack inputStack = player.getOffhandItem();
        ToolVariantDefinition tool = AssemblyTools.find(requirement.toolId(), toolStack);
        CERecipeInput input = CERecipeInput.of(List.of(inputStack), List.of());
        if (tool == null || !recipe.matches(input, player.serverLevel())) return false;

        int duration = Math.max(1, tool.useTimeTicks());
        active.progressTicks = Math.min(duration, active.progressTicks + 1);
        sendProgress(player, requirement, active.progressTicks, duration);
        if (active.progressTicks < duration) return true;

        ItemStack result = rollResult(recipe.itemOutputs(), player.serverLevel());
        if (!player.isCreative()) {
            inputStack.shrink(1);
            if (toolStack.isDamageableItem()) {
                toolStack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            }
        }
        if (!result.isEmpty() && !player.addItem(result)) player.drop(result, false);
        player.serverLevel().playSound(
                null,
                player.blockPosition(),
                tool.sound(),
                SoundSource.PLAYERS,
                0.8F,
                1.0F
        );
        return false;
    }

    private static Optional<RecipeHolder<CERecipe>> findRecipe(
            ServerLevel level,
            ItemStack inputStack,
            ItemStack toolStack
    ) {
        if (inputStack.isEmpty() || toolStack.isEmpty()) return Optional.empty();
        CERecipeInput input = CERecipeInput.of(List.of(inputStack), List.of());
        return CERecipeLookup.byType(level.getRecipeManager(), CERecipeTypes.PRIMITIVE_SIFTING).stream()
                .filter(holder -> holder.value().matches(input, level))
                .filter(holder -> !holder.value().tools().isEmpty())
                .filter(holder -> holder.value().tools().getFirst().matches(toolStack))
                .findFirst();
    }

    /**
     * The sum is the overall find chance. On a successful find, the individual values become
     * mutually exclusive weights, so the operation can produce zero or exactly one dust.
     */
    private static ItemStack rollResult(List<CEChancedItemOutput> outputs, ServerLevel level) {
        int total = outputs.stream().mapToInt(CEChancedItemOutput::chance).sum();
        if (total < 1) return ItemStack.EMPTY;
        if (level.random.nextInt(CEChancedItemOutput.MAX_CHANCE) >= total) return ItemStack.EMPTY;

        int selected = level.random.nextInt(total);
        int cumulative = 0;
        for (CEChancedItemOutput output : outputs) {
            cumulative += output.chance();
            if (selected < cumulative) return output.stack().copy();
        }
        return outputs.getLast().stack().copy();
    }

    private static void sendProgress(
            ServerPlayer player,
            CEToolRequirement requirement,
            int progress,
            int duration
    ) {
        BlockPos anchor = player.blockPosition();
        PacketDistributor.sendToPlayer(player, AssemblyNextStepPayload.showToolProgress(
                PrimitiveSiftingRules.DISPLAY_NAME,
                player.serverLevel().dimension().location().toString(),
                anchor,
                requirement.displayName() + " 1/1",
                progress,
                duration
        ));
    }

    private static void clear(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, AssemblyNextStepPayload.clear());
    }

    private static final class ActiveSifting {
        private final ResourceLocation recipeId;
        private int progressTicks;

        private ActiveSifting(ResourceLocation recipeId) {
            this.recipeId = recipeId;
        }
    }
}
