package net.mads.industron.recipe.chiseling;

import net.mads.industron.Industron;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.network.AssemblyNextStepPayload;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyTools;
import net.mads.industron.recipe.recipetypes.assembly.ToolVariantDefinition;
import net.mads.industron.recipe.recipetypes.assembly.input.AssemblyUseState;
import net.mads.industron.recipe.recipes.assembly.Tool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server-authoritative block-face pattern runtime for Chiseling.
 *
 * Each pattern cell is a real timed tool action: the player must continuously hold
 * right-click for the Chisel's full use time. Releasing early resets only that pending
 * cell. A cell is committed only after its timer reaches 100%, and the player must
 * release right-click before the next cell can begin.
 */
@EventBusSubscriber(modid = Industron.MOD_ID)
public final class ChiselingRuntime {
    private static final long IDLE_TIMEOUT_TICKS = 20L * 30L;
    private static final Map<Key, ActiveChiseling> ACTIVE = new HashMap<>();
    private static final Map<UUID, Key> FINISHED_USE = new HashMap<>();

    private ChiselingRuntime() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        // A held use key sends repeated interactions after the output replaces the base.
        // Consume those on both hands until release, before considering output recipes/tools.
        if (!event.getLevel().isClientSide()) {
            Key finished = FINISHED_USE.get(event.getEntity().getUUID());
            if (finished != null && finished.dimension.equals(event.getLevel().dimension())
                    && finished.pos.equals(event.getPos())) {
                event.setCancellationResult(InteractionResult.CONSUME);
                event.setCanceled(true);
                return;
            }
        }
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        Level level = event.getLevel();
        ItemStack mainHand = event.getItemStack();
        ItemStack offHand = event.getEntity().getOffhandItem();
        ToolVariantDefinition hammer = AssemblyTools.find(Tool.HAMMER.type(), mainHand);
        ToolVariantDefinition chisel = AssemblyTools.find(Tool.CHISEL.type(), offHand);
        if (hammer == null || chisel == null) return;

        List<ChiselingRecipe> recipes = ChiselingRecipes.forBlock(level, level.getBlockState(event.getPos())).stream()
                .map(holder -> holder.value())
                .filter(recipe -> meetsTier(hammer.tier(), chisel.tier(), recipe.tier()))
                .toList();
        if (recipes.isEmpty()) return;

        // Chiseling owns this interaction while the correct two tools are held.
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
        event.setCanceled(true);
        if (level.isClientSide()) return;
        if (!(level instanceof ServerLevel serverLevel) || !(event.getEntity() instanceof ServerPlayer player)) return;

        int cell = ChiselingGrid.cell(event.getHitVec());
        if (cell < 1 || cell > 9) return;

        Key key = new Key(serverLevel.dimension(), event.getPos().immutable());
        ActiveChiseling active = ACTIVE.get(key);
        if (active == null || !active.owner.equals(player.getUUID())) {
            if (active != null) clearOverlay(serverLevel, active.owner);
            active = new ActiveChiseling(
                    player.getUUID(),
                    recipes,
                    BuiltInRegistries.BLOCK.getKey(serverLevel.getBlockState(event.getPos()).getBlock()),
                    serverLevel.getGameTime()
            );
            ACTIVE.put(key, active);
        } else {
            // Datapack reloads or tool-tier changes may remove legal candidates.
            active.candidates.removeIf(candidate -> recipes.stream().noneMatch(recipe -> sameRecipe(recipe, candidate)));
            List<Integer> completedPattern = active.completed;
            active.candidates.removeIf(candidate -> !candidate.matchesPrefix(completedPattern));
            if (active.candidates.isEmpty()) {
                active = new ActiveChiseling(
                        player.getUUID(),
                        recipes,
                        BuiltInRegistries.BLOCK.getKey(serverLevel.getBlockState(event.getPos()).getBlock()),
                        serverLevel.getGameTime()
                );
                ACTIVE.put(key, active);
            }
        }

        active.lastActivity = serverLevel.getGameTime();

        // After a completed cell the use key must be released before another cell starts.
        if (active.awaitingRelease) {
            syncOverlay(player, serverLevel, event.getPos(), active);
            return;
        }

        // Repeated RightClickBlock events while the key is held must never reset or replace
        // the already-running timed hit.
        if (active.pendingCell != 0) {
            syncOverlay(player, serverLevel, event.getPos(), active);
            return;
        }

        active.beginHit(cell, hammer, chisel);
        serverLevel.playSound(null, event.getPos(), SoundEvents.STONE_HIT, SoundSource.BLOCKS, 0.75F, 1.0F);
        syncOverlay(player, serverLevel, event.getPos(), active);
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        ActiveChiseling removed = ACTIVE.remove(new Key(level.dimension(), event.getPos()));
        if (removed != null) clearOverlay(level, removed.owner);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        FINISHED_USE.entrySet().removeIf(entry -> {
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            return player == null || !AssemblyUseState.isHeld(player)
                    || !player.level().dimension().equals(entry.getValue().dimension);
        });
        Iterator<Map.Entry<Key, ActiveChiseling>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Key, ActiveChiseling> entry = iterator.next();
            ServerLevel level = event.getServer().getLevel(entry.getKey().dimension);
            if (level == null) continue;

            ActiveChiseling active = entry.getValue();
            BlockPos pos = entry.getKey().pos;
            if (level.getGameTime() - active.lastActivity > IDLE_TIMEOUT_TICKS
                    || active.candidates.stream().noneMatch(recipe -> recipe.matchesBase(level.getBlockState(pos)))) {
                clearOverlay(level, active.owner);
                iterator.remove();
                continue;
            }

            ServerPlayer player = event.getServer().getPlayerList().getPlayer(active.owner);
            if (player == null || player.level() != level) {
                clearOverlay(level, active.owner);
                continue;
            }

            if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 36.0D) {
                active.interruptPendingHit();
                clearOverlay(player);
                continue;
            }

            // A finished cell is already committed. Nothing else may begin until the use
            // key has actually been released, which prevents one long hold from chaining cells.
            if (active.awaitingRelease) {
                if (!AssemblyUseState.isHeld(player)) {
                    active.awaitingRelease = false;
                    active.lastActivity = level.getGameTime();
                }
                syncOverlay(player, level, pos, active);
                continue;
            }

            if (active.pendingCell == 0) {
                syncOverlay(player, level, pos, active);
                continue;
            }

            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();
            ToolVariantDefinition hammer = AssemblyTools.find(Tool.HAMMER.type(), mainHand);
            ToolVariantDefinition chisel = AssemblyTools.find(Tool.CHISEL.type(), offHand);

            // Exactly like Assembly tool steps: releasing right-click, swapping either tool,
            // losing tier validity, or walking away resets only this unfinished hit.
            if (!AssemblyUseState.isHeld(player)
                    || hammer == null
                    || chisel == null
                    || !hammer.equals(active.hammer)
                    || !chisel.equals(active.chisel)
                    || active.candidates.stream().noneMatch(recipe -> meetsTier(hammer.tier(), chisel.tier(), recipe.tier()))) {
                active.interruptPendingHit();
                syncOverlay(player, level, pos, active);
                continue;
            }

            active.progressTicks++;
            active.lastActivity = level.getGameTime();

            int durationTicks = chisel.useTimeTicks();
            if (active.progressTicks < durationTicks) {
                syncOverlay(player, level, pos, active);
                continue;
            }

            // Nothing affecting the block, pattern, or durability happens before this point.
            // The full Chisel duration has now elapsed, so this one cell is committed atomically.
            if (!player.isCreative()) {
                if (mainHand.isDamageableItem()) mainHand.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                if (offHand.isDamageableItem()) offHand.hurtAndBreak(1, player, EquipmentSlot.OFFHAND);
            }

            int completedCell = active.pendingCell;
            active.clearPendingHit();
            active.completed.add(completedCell);
            active.candidates.removeIf(recipe -> !recipe.matchesPrefix(active.completed));

            if (active.candidates.isEmpty()) {
                // A wrong cell destroys the block only after that wrong cell's full timer completed.
                fail(level, pos, active);
                clearOverlay(player);
                iterator.remove();
                continue;
            }

            List<ChiselingRecipe> completedRecipes = active.candidates.stream()
                    .filter(recipe -> recipe.complete(active.completed))
                    .toList();
            if (!completedRecipes.isEmpty()) {
                ChiselingRecipe recipe = completedRecipes.getFirst();
                if (completedRecipes.size() > 1) {
                    Industron.LOGGER.error("Ambiguous completed Chiseling pattern at {}: {} recipes match", pos, completedRecipes.size());
                }
                // The output block is placed only after the final cell's complete Chisel timer.
                FINISHED_USE.put(player.getUUID(), entry.getKey());
                finish(level, pos, recipe);
                clearOverlay(player);
                iterator.remove();
                continue;
            }

            active.awaitingRelease = true;
            syncOverlay(player, level, pos, active);
        }
    }

    private static void finish(ServerLevel level, BlockPos pos, ChiselingRecipe recipe) {
        if (!recipe.matchesBase(level.getBlockState(pos))) return;

        if (recipe.hasBlockOutput()) {
            level.setBlock(pos, recipe.baseBlockOutput().defaultBlockState(), 3);
        } else {
            level.removeBlock(pos, false);
            ItemStack result = recipe.resultStack();
            if (!result.isEmpty()) {
                Containers.dropItemStack(
                        level,
                        pos.getX() + 0.5D,
                        pos.getY() + 0.5D,
                        pos.getZ() + 0.5D,
                        result.copy()
                );
            }
        }

        dropOptionalItem(level, pos, recipe.dustOutputId());
        level.playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.8F, 1.0F);
    }

    private static void fail(ServerLevel level, BlockPos pos, ActiveChiseling active) {
        if (!active.initialBaseStillPresent(level, pos)) return;
        level.removeBlock(pos, false);
        dropOptionalItem(level, pos, active.dustOutputId);
        level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.9F, 0.85F);
    }

    private static void dropOptionalItem(
            ServerLevel level,
            BlockPos pos,
            java.util.Optional<net.minecraft.resources.ResourceLocation> itemId
    ) {
        if (itemId == null || itemId.isEmpty()) return;
        BuiltInRegistries.ITEM.getOptional(itemId.get()).ifPresent(item -> Containers.dropItemStack(
                level,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                new ItemStack(item)
        ));
    }

    private static void syncOverlay(ServerPlayer player, ServerLevel level, BlockPos pos, ActiveChiseling active) {
        if (player == null) return;

        String dimension = level.dimension().location().toString();
        String nextStep;
        AssemblyNextStepPayload payload;

        if (active.pendingCell != 0 && active.chisel != null) {
            nextStep = "Cell " + active.pendingCell + " (" + (active.completed.size() + 1) + "/" + active.patternLength() + ")";
            payload = AssemblyNextStepPayload.showToolProgress(
                    "Chiseling",
                    dimension,
                    pos,
                    nextStep,
                    active.progressTicks,
                    active.chisel.useTimeTicks()
            );
        } else if (active.awaitingRelease) {
            nextStep = "Release right-click";
            payload = AssemblyNextStepPayload.show("Chiseling", dimension, pos, nextStep);
        } else {
            int expectedCell = active.commonNextCell();
            int step = Math.min(active.completed.size() + 1, active.patternLength());
            nextStep = expectedCell > 0
                    ? "Cell " + expectedCell + " (" + step + "/" + active.patternLength() + ")"
                    : "Choose pattern cell (" + step + "/" + active.patternLength() + ")";
            payload = AssemblyNextStepPayload.show("Chiseling", dimension, pos, nextStep);
        }

        PacketDistributor.sendToPlayer(player, payload);
    }

    private static void clearOverlay(ServerLevel level, UUID playerId) {
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
        if (player != null) clearOverlay(player);
    }

    private static void clearOverlay(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, AssemblyNextStepPayload.clear());
    }

    private static boolean meetsTier(MachineTier hammer, MachineTier chisel, MachineTier required) {
        int hammerRank = rank(hammer);
        int chiselRank = rank(chisel);
        int requiredRank = rank(required);
        return Math.min(hammerRank, chiselRank) >= requiredRank;
    }

    private static int rank(MachineTier tier) {
        if (tier == null || tier == MachineTier.NONE) return -1;
        int rank = MachineTier.ELECTRIC_TIERS.indexOf(tier.recipeTier());
        return rank < 0 ? -1 : rank;
    }

    private static boolean sameRecipe(ChiselingRecipe left, ChiselingRecipe right) {
        return left.baseBlockInputIds().equals(right.baseBlockInputIds())
                && left.baseBlockOutputId().equals(right.baseBlockOutputId())
                && left.itemOutputId().equals(right.itemOutputId())
                && left.pattern().equals(right.pattern());
    }

    private record Key(ResourceKey<Level> dimension, BlockPos pos) {
    }

    private static final class ActiveChiseling {
        private final UUID owner;
        private final List<ChiselingRecipe> candidates;
        private final List<Integer> completed = new ArrayList<>();
        private final net.minecraft.resources.ResourceLocation initialBaseBlockId;
        private final java.util.Optional<net.minecraft.resources.ResourceLocation> dustOutputId;
        private long lastActivity;
        private int pendingCell;
        private int progressTicks;
        private ToolVariantDefinition hammer;
        private ToolVariantDefinition chisel;
        private boolean awaitingRelease;

        private ActiveChiseling(
                UUID owner,
                List<ChiselingRecipe> candidates,
                net.minecraft.resources.ResourceLocation initialBaseBlockId,
                long gameTime
        ) {
            if (candidates == null || candidates.isEmpty()) throw new IllegalArgumentException("Chiseling needs candidates");
            if (initialBaseBlockId == null) throw new IllegalArgumentException("Chiseling needs the actual starting block");
            this.owner = owner;
            this.candidates = new ArrayList<>(candidates);
            this.initialBaseBlockId = initialBaseBlockId;
            this.dustOutputId = candidates.getFirst().dustOutputId();
            this.lastActivity = gameTime;
        }

        private void beginHit(int cell, ToolVariantDefinition currentHammer, ToolVariantDefinition currentChisel) {
            pendingCell = cell;
            progressTicks = 0;
            hammer = currentHammer;
            chisel = currentChisel;
        }

        private void interruptPendingHit() {
            clearPendingHit();
        }

        private void clearPendingHit() {
            pendingCell = 0;
            progressTicks = 0;
            hammer = null;
            chisel = null;
        }

        private int patternLength() {
            return candidates.stream().mapToInt(recipe -> recipe.pattern().size()).max().orElse(9);
        }

        private int commonNextCell() {
            int index = completed.size();
            Integer common = null;
            for (ChiselingRecipe recipe : candidates) {
                if (index >= recipe.pattern().size()) continue;
                int cell = recipe.pattern().get(index);
                if (common == null) common = cell;
                else if (common != cell) return -1;
            }
            return common == null ? -1 : common;
        }

        private boolean initialBaseStillPresent(ServerLevel level, BlockPos pos) {
            return BuiltInRegistries.BLOCK.getOptional(initialBaseBlockId)
                    .map(block -> level.getBlockState(pos).is(block))
                    .orElse(false);
        }
    }
}
