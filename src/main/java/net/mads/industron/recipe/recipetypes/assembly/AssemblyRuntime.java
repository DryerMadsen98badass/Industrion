package net.mads.industron.recipe.recipetypes.assembly;

import net.mads.industron.recipe.recipetypes.assembly.workbench.AssemblyWorkbenchBlockEntity;
import net.mads.industron.recipe.recipes.assembly.AssemblyRecipes;
import net.mads.industron.material.defenitions.IndustrialMaterials;
import net.mads.industron.material.MaterialLookup;

import net.mads.industron.Industron;
import net.mads.industron.recipe.recipetypes.assembly.input.AssemblyUseState;
import net.mads.industron.control.ControlKeyState;
import net.mads.industron.network.AssemblyNextStepPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Server-authoritative right-click assembly runtime. */
@EventBusSubscriber(modid = Industron.MOD_ID)
public final class AssemblyRuntime {
    /** One fake-player/deployer activation contributes 0.1 s of tool work. */
    private static final int FAKE_PLAYER_TOOL_USE_TICKS_PER_INTERACTION = 2;
    private static final Map<Key, ActiveAssembly> ACTIVE = new HashMap<>();

    private AssemblyRuntime() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        Level level = event.getLevel();
        BlockPos pos = event.getPos();

        if (level.isClientSide) {
            return;
        }
        if (!(level instanceof ServerLevel serverLevel) || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        BlockPos workbenchPos = clickedWorkbench(level, pos, event.getFace());
        InteractionResult result;
        if (workbenchPos != null) {
            result = applyWorkbenchInteraction(
                    serverLevel,
                    workbenchPos,
                    player,
                    event.getItemStack()
            );
        } else {
            Key key = key(level, pos);
            ActiveAssembly active = ACTIVE.get(key);
            if (active == null) {
                // Starting an assembly on a world block must be intentional. Ctrl
                // distinguishes the first recipe input from the block's normal use.
                if (!ControlKeyState.isHeld(player)) return;

                AssemblyRecipeDefinition recipe = findWorldBlockStartRecipe(level, pos, event.getItemStack());
                if (recipe == null) return;

                try {
                    active = new ActiveAssembly(recipe, AssemblyPlan.compile(recipe), false);
                } catch (IllegalStateException exception) {
                    Industron.LOGGER.error("Cannot compile assembly recipe {}", recipe.id(), exception);
                    return;
                }
                ACTIVE.put(key, active);
            } else if (event.getItemStack().getItem() instanceof BlockItem
                    && !ControlKeyState.isHeld(player)) {
                // After the first step, block items still require Ctrl so ordinary
                // placement remains available next to the assembly.
                return;
            }
            result = applyCurrentStep(serverLevel, pos, player, event.getItemStack(), active);
        }

        if (result.consumesAction()) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos interactionPos = event.getPos();
        Key key = key(level, interactionPos);
        ActiveAssembly active = removeActive(level, interactionPos);
        if (active == null && level.getBlockEntity(interactionPos.below()) instanceof AssemblyWorkbenchBlockEntity workbench) {
            interactionPos = interactionPos.below();
            key = key(level, interactionPos);
            active = removeActive(level, interactionPos);
            if (active == null) active = loadWorkbenchAssembly(level, interactionPos, workbench);
            ACTIVE.remove(key);
        }
        if (active == null) return;

        clearExpectedForViewers(level, active);
        clearWorkbenchAssemblyData(level, interactionPos, active);
        Player player = event.getPlayer();
        for (ItemStack stack : active.refund) {
            ItemStack refund = stack.copy();
            if (!player.addItem(refund)) {
                player.drop(refund, false);
            }
        }
        player.displayClientMessage(Component.literal("Assembly cancelled - consumed parts returned.").withStyle(ChatFormatting.YELLOW), true);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        Iterator<Map.Entry<Key, ActiveAssembly>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Key, ActiveAssembly> entry = iterator.next();
            ActiveAssembly active = entry.getValue();
            ServerLevel level = findLevel(event.getServer(), entry.getKey().dimension);
            if (level == null) continue;

            if (!hasValidBase(level, entry.getKey().pos, active)) {
                clearExpectedForViewers(level, active);
                dropRefund(level, entry.getKey().pos, active);
                clearWorkbenchAssemblyData(level, entry.getKey().pos, active);
                iterator.remove();
                continue;
            }

            if (tickToolUse(level, entry.getKey().pos, active)) {
                if (active.finished()) {
                    finish(level, entry.getKey().pos, active);
                    iterator.remove();
                    continue;
                }
                persistWorkbenchAssembly(level, entry.getKey().pos, active);
            }

            if (active.waitUntilGameTime < 0 || level.getGameTime() < active.waitUntilGameTime) continue;

            active.waitUntilGameTime = -1;
            advanceAutomatic(level, entry.getKey().pos, active);
            persistWorkbenchAssembly(level, entry.getKey().pos, active);
            if (active.finished()) {
                finish(level, entry.getKey().pos, active);
                iterator.remove();
            } else if (active.waitUntilGameTime >= 0) {
                hideExpectedForViewers(level, active);
            } else {
                syncExpectedForViewers(level, entry.getKey().pos, active);
            }
        }
    }

    private static AssemblyRecipeDefinition findWorldBlockStartRecipe(Level level, BlockPos pos, ItemStack firstStep) {
        for (AssemblyRecipeDefinition recipe : AssemblyRecipes.ALL) {
            if (!recipe.usesWorldBlockRuntime() || !level.getBlockState(pos).is(recipe.baseBlock())) continue;

            try {
                List<AssemblyPlan.Step> plan = AssemblyPlan.compile(recipe);
                if (!plan.isEmpty() && matches(plan.getFirst(), firstStep, Map.of(), plan)) return recipe;
            } catch (IllegalStateException exception) {
                Industron.LOGGER.error("Cannot compile assembly recipe {}", recipe.id(), exception);
            }
        }
        return null;
    }

    private static InteractionResult applyWorkbenchInteraction(
            ServerLevel level,
            BlockPos workbenchPos,
            ServerPlayer player,
            ItemStack held
    ) {
        if (!(level.getBlockEntity(workbenchPos) instanceof AssemblyWorkbenchBlockEntity workbench)) {
            return InteractionResult.PASS;
        }

        Key key = key(level, workbenchPos);
        ActiveAssembly active = ACTIVE.get(key);
        if (active == null) {
            active = loadWorkbenchAssembly(level, workbenchPos, workbench);
            if (active != null) ACTIVE.put(key, active);
        }

        if (active != null) {
            if (held.getItem() instanceof BlockItem && !ControlKeyState.isHeld(player)) {
                return InteractionResult.PASS;
            }
            return applyCurrentStep(level, workbenchPos, player, held, active);
        }

        if (workbench.resultReady()) {
            if (!held.isEmpty()) {
                player.displayClientMessage(Component.literal("Use an empty hand to take the assembly result.").withStyle(ChatFormatting.GRAY), true);
                return InteractionResult.SUCCESS;
            }
            giveOrDrop(player, workbench.takeDisplayedStack());
            return InteractionResult.SUCCESS;
        }

        if (workbench.hasDisplayedStack()) {
            if (held.isEmpty()) {
                giveOrDrop(player, workbench.takeDisplayedStack());
                return InteractionResult.SUCCESS;
            }
            if (held.getItem() instanceof BlockItem && !ControlKeyState.isHeld(player)) {
                return InteractionResult.PASS;
            }

            AssemblyRecipeDefinition recipe = findWorkbenchStartRecipe(workbench, held);
            if (recipe == null) {
                player.displayClientMessage(Component.literal("No assembly starts with this input.").withStyle(ChatFormatting.GRAY), true);
                return InteractionResult.SUCCESS;
            }
            active = createActive(recipe, true);
            ACTIVE.put(key, active);
            persistWorkbenchAssembly(level, workbenchPos, active);
            return applyCurrentStep(level, workbenchPos, player, held, active);
        }

        if (level.getBlockState(workbenchPos.above()).isAir() && isItemBaseForAnyRecipe(held)) {
            ItemStack base = held.copyWithCount(1);
            if (!player.isCreative()) held.shrink(1);
            workbench.setDisplayedStack(base, false);
            return InteractionResult.SUCCESS;
        }

        if (held.getItem() instanceof BlockItem && !ControlKeyState.isHeld(player)) {
            return InteractionResult.PASS;
        }

        AssemblyRecipeDefinition recipe = findWorkbenchStartRecipe(workbench, held);
        if (recipe == null) return InteractionResult.PASS;

        active = createActive(recipe, true);
        ACTIVE.put(key, active);
        persistWorkbenchAssembly(level, workbenchPos, active);
        return applyCurrentStep(level, workbenchPos, player, held, active);
    }

    private static AssemblyRecipeDefinition findWorkbenchStartRecipe(
            AssemblyWorkbenchBlockEntity workbench,
            ItemStack firstStep
    ) {
        for (AssemblyRecipeDefinition recipe : AssemblyRecipes.ALL) {
            if (!recipe.hasItemBaseInput()) continue;

            boolean baseMatches;
            baseMatches = workbench.hasDisplayedStack()
                    && workbench.displayedStack().is(recipe.baseItem());
            if (!baseMatches) continue;

            try {
                List<AssemblyPlan.Step> plan = AssemblyPlan.compile(recipe);
                if (!plan.isEmpty() && matches(plan.getFirst(), firstStep, Map.of(), plan)) return recipe;
            } catch (IllegalStateException exception) {
                Industron.LOGGER.error("Cannot compile assembly recipe {}", recipe.id(), exception);
            }
        }
        return null;
    }

    private static boolean isItemBaseForAnyRecipe(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return AssemblyRecipes.ALL.stream()
                .filter(AssemblyRecipeDefinition::hasItemBaseInput)
                .anyMatch(recipe -> stack.is(recipe.baseItem()));
    }

    private static ActiveAssembly createActive(AssemblyRecipeDefinition recipe, boolean workbench) {
        List<AssemblyPlan.Step> plan = AssemblyPlan.compile(recipe);
        return new ActiveAssembly(recipe, plan, workbench);
    }

    private static InteractionResult applyCurrentStep(ServerLevel level, BlockPos pos, ServerPlayer player, ItemStack held, ActiveAssembly active) {
        if (!isFakePlayer(player)) active.viewers.add(player.getUUID());

        if (!hasValidBase(level, pos, active)) {
            clearExpectedForViewers(level, active);
            refund(player, active);
            clearWorkbenchAssemblyData(level, pos, active);
            ACTIVE.remove(key(level, pos));
            return InteractionResult.SUCCESS;
        }

        advanceAutomatic(level, pos, active);
        persistWorkbenchAssembly(level, pos, active);
        if (active.waitUntilGameTime >= 0) {
            clearExpected(player);
            long ticks = Math.max(0, active.waitUntilGameTime - level.getGameTime());
            if (!isFakePlayer(player)) {
                player.displayClientMessage(Component.literal("Assembly waiting: " + formatSeconds(ticks) + " s").withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResult.SUCCESS;
        }
        if (active.finished()) {
            finish(level, pos, active);
            ACTIVE.remove(key(level, pos));
            return InteractionResult.SUCCESS;
        }

        AssemblyPlan.Step step = active.current();
        if (!matches(step, held, active.materialBindings, active.plan)) {
            syncExpected(player, level, pos, active);
            return InteractionResult.SUCCESS;
        }

        bindMaterial(step, held, active.materialBindings);

        switch (step.kind()) {
            case MATERIAL, ITEM -> consumeOne(player, held, active, step.sound(), level, pos);
            case TOOL -> {
                boolean completedImmediately = beginToolUse(player, held, active, step, level, pos);
                if (completedImmediately && active.finished()) {
                    finish(level, pos, active);
                    ACTIVE.remove(key(level, pos));
                } else if (!completedImmediately) {
                    syncExpected(player, level, pos, active);
                }
                return InteractionResult.SUCCESS;
            }
            case WAIT -> throw new IllegalStateException("WAIT should be handled automatically");
        }

        active.index++;
        advanceAutomatic(level, pos, active);
        persistWorkbenchAssembly(level, pos, active);
        if (active.finished() && active.waitUntilGameTime < 0) {
            finish(level, pos, active);
            ACTIVE.remove(key(level, pos));
        } else if (active.waitUntilGameTime >= 0) {
            hideExpectedForViewers(level, active);
            long ticks = Math.max(0, active.waitUntilGameTime - level.getGameTime());
            if (!isFakePlayer(player)) {
                player.displayClientMessage(Component.literal("Assembly working: " + formatSeconds(ticks) + " s").withStyle(ChatFormatting.GRAY), true);
            }
        } else {
            syncExpectedForViewers(level, pos, active);
        }
        return InteractionResult.SUCCESS;
    }

    private static void consumeOne(ServerPlayer player, ItemStack held, ActiveAssembly active, SoundEvent sound, ServerLevel level, BlockPos pos) {
        if (!player.isCreative()) {
            active.refund.add(held.copyWithCount(1));
            held.shrink(1);
        }
        play(level, pos, sound);
    }

    private static boolean beginToolUse(ServerPlayer player, ItemStack held, ActiveAssembly active, AssemblyPlan.Step step, ServerLevel level, BlockPos pos) {
        ToolDefinition tool = AssemblyTools.find(step.tool(), held);
        if (tool == null) return false;

        if (tool.useTimeTicks() <= 0) {
            SoundEvent sound = step.sound() != null ? step.sound() : tool.sound();
            play(level, pos, sound);
            completeToolUse(player, held, active, level, pos);
            return true;
        }

        if (isFakePlayer(player)) {
            return advanceFakePlayerToolUse(player, held, active, step, tool, level, pos);
        }

        if (active.toolAutomated
                || !player.getUUID().equals(active.toolUser)
                || !tool.equals(active.activeTool)) {
            active.toolUser = player.getUUID();
            active.activeTool = tool;
            active.toolProgressTicks = 0;
            active.toolAutomated = false;

            SoundEvent sound = step.sound() != null ? step.sound() : tool.sound();
            play(level, pos, sound);
        }

        return false;
    }

    /**
     * Fake players cannot send the real player's continuous-use state. Each actual
     * automation interaction therefore contributes a fixed 2 ticks (0.1 s) of the
     * tool's normal use time. Progress survives between activations, and durability
     * is still charged only once when the complete tool step finishes.
     */
    private static boolean advanceFakePlayerToolUse(
            ServerPlayer player,
            ItemStack held,
            ActiveAssembly active,
            AssemblyPlan.Step step,
            ToolDefinition tool,
            ServerLevel level,
            BlockPos pos
    ) {
        if (!active.toolAutomated || !tool.equals(active.activeTool)) {
            active.toolUser = null;
            active.activeTool = tool;
            active.toolProgressTicks = 0;
            active.toolAutomated = true;
        }

        SoundEvent sound = step.sound() != null ? step.sound() : tool.sound();
        play(level, pos, sound);

        active.toolProgressTicks = Math.min(
                tool.useTimeTicks(),
                active.toolProgressTicks + FAKE_PLAYER_TOOL_USE_TICKS_PER_INTERACTION
        );
        persistWorkbenchAssembly(level, pos, active);

        if (active.toolProgressTicks < tool.useTimeTicks()) {
            syncExpectedForViewers(level, pos, active);
            return false;
        }

        completeToolUse(player, held, active, level, pos);
        return true;
    }

    /**
     * Advances a TOOL step only while the same player continuously holds right-click with
     * the same valid tool. Releasing right-click, changing tool, or walking away resets the
     * progress, so a timed tool can no longer be completed by a single click.
     */
    private static boolean tickToolUse(ServerLevel level, BlockPos pos, ActiveAssembly active) {
        if (active.toolAutomated) return false;
        if (active.finished() || active.current().kind() != AssemblyPlan.Kind.TOOL || active.toolUser == null || active.activeTool == null) {
            return false;
        }

        ServerPlayer player = level.getServer().getPlayerList().getPlayer(active.toolUser);
        if (player == null
                || player.level() != level
                || !AssemblyUseState.isHeld(player)
                || player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 36.0D) {
            interruptToolUse(level, pos, player, active);
            return false;
        }

        AssemblyPlan.Step step = active.current();
        ItemStack held = player.getMainHandItem();
        ToolDefinition tool = AssemblyTools.find(step.tool(), held);
        if (tool == null || !tool.equals(active.activeTool)) {
            interruptToolUse(level, pos, player, active);
            return false;
        }

        active.toolProgressTicks++;
        if (active.toolProgressTicks < tool.useTimeTicks()) {
            syncExpectedForViewers(level, pos, active);
            return false;
        }

        completeToolUse(player, held, active, level, pos);
        return true;
    }

    private static void completeToolUse(ServerPlayer player, ItemStack held, ActiveAssembly active, ServerLevel level, BlockPos pos) {
        if (!player.isCreative() && held.isDamageableItem()) {
            held.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        }

        clearToolUse(active);
        active.index++;
        advanceAutomatic(level, pos, active);
        persistWorkbenchAssembly(level, pos, active);

        if (active.finished()) {
            return;
        }
        if (active.waitUntilGameTime >= 0) {
            hideExpectedForViewers(level, active);
            long ticks = Math.max(0, active.waitUntilGameTime - level.getGameTime());
            if (!isFakePlayer(player)) {
                player.displayClientMessage(Component.literal("Assembly working: " + formatSeconds(ticks) + " s").withStyle(ChatFormatting.GRAY), true);
            }
        } else {
            syncExpectedForViewers(level, pos, active);
        }
    }

    private static void interruptToolUse(ServerLevel level, BlockPos pos, ServerPlayer player, ActiveAssembly active) {
        boolean hadProgress = active.toolProgressTicks > 0;
        clearToolUse(active);
        syncExpectedForViewers(level, pos, active);
        if (player != null && hadProgress) {
            player.displayClientMessage(Component.literal("Tool use interrupted - hold right-click continuously.").withStyle(ChatFormatting.YELLOW), true);
        }
    }

    private static void clearToolUse(ActiveAssembly active) {
        active.toolUser = null;
        active.activeTool = null;
        active.toolProgressTicks = 0;
        active.toolAutomated = false;
    }

    private static void advanceAutomatic(ServerLevel level, BlockPos pos, ActiveAssembly active) {
        while (!active.finished() && active.waitUntilGameTime < 0 && active.current().kind() == AssemblyPlan.Kind.WAIT) {
            AssemblyPlan.Step wait = active.current();
            active.index++;
            play(level, pos, wait.sound());
            active.waitUntilGameTime = level.getGameTime() + wait.waitTicks();
        }
    }

    private static boolean matches(
            AssemblyPlan.Step step,
            ItemStack stack,
            Map<Integer, net.mads.industron.material.IndustrialMaterial> bindings,
            List<AssemblyPlan.Step> plan
    ) {
        if (stack.isEmpty()) return false;

        MaterialLookup.MaterialTarget target = MaterialLookup.find(stack);
        boolean base = switch (step.kind()) {
            case MATERIAL -> target != null
                    && target.part() == step.material()
                    && matchesMaterialBinding(step, target.material(), bindings, plan);
            case ITEM -> BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(step.itemId());
            case TOOL -> AssemblyTools.find(step.tool(), stack) != null;
            case WAIT -> false;
        };
        if (!base) return false;

        return step.kind() != AssemblyPlan.Kind.MATERIAL
                || step.requirements().stream().allMatch(requirement -> requirement.matches(stack));
    }

    private static boolean matchesMaterialBinding(
            AssemblyPlan.Step step,
            net.mads.industron.material.IndustrialMaterial candidate,
            Map<Integer, net.mads.industron.material.IndustrialMaterial> bindings,
            List<AssemblyPlan.Step> plan
    ) {
        if (step.fixedMaterial() != null) return step.fixedMaterial().id().equals(candidate.id());
        var selected = bindings.get(step.bindingId());
        if (selected != null) return selected.id().equals(candidate.id());

        // Dynamic ids are now leaf-local for free/Metal.ANY trees. Keep this grouped
        // feasibility check so explicitly shared dynamic ids remain safe if added later.
        return plan.stream()
                .filter(candidateStep -> candidateStep.kind() == AssemblyPlan.Kind.MATERIAL)
                .filter(candidateStep -> candidateStep.usesDynamicBinding())
                .filter(candidateStep -> candidateStep.bindingId() == step.bindingId())
                .allMatch(candidateStep -> candidate.has(candidateStep.material())
                        && candidateStep.requirements().stream()
                        .allMatch(requirement -> requirement.matches(candidate, candidateStep.material())));
    }

    private static void bindMaterial(
            AssemblyPlan.Step step,
            ItemStack stack,
            Map<Integer, net.mads.industron.material.IndustrialMaterial> bindings
    ) {
        if (!step.usesDynamicBinding() || bindings.containsKey(step.bindingId())) return;
        MaterialLookup.MaterialTarget target = MaterialLookup.find(stack);
        if (target == null) throw new IllegalStateException("Matched material step has no material target");
        bindings.put(step.bindingId(), target.material());
    }

    private static void syncExpected(ServerPlayer player, ServerLevel level, BlockPos pos, ActiveAssembly active) {
        if (isFakePlayer(player)) return;
        active.viewers.add(player.getUUID());
        if (active.finished() || active.waitUntilGameTime >= 0) {
            clearExpected(player);
            return;
        }

        String dimension = level.dimension().location().toString();
        String nextStep = expectedName(active.current(), active.materialBindings);
        AssemblyNextStepPayload payload = active.current().kind() == AssemblyPlan.Kind.TOOL
                && active.activeTool != null
                ? AssemblyNextStepPayload.showToolProgress(
                        dimension,
                        pos,
                        nextStep,
                        active.toolProgressTicks,
                        active.activeTool.useTimeTicks()
                )
                : AssemblyNextStepPayload.show(dimension, pos, nextStep);
        PacketDistributor.sendToPlayer(player, payload);
    }

    private static void syncExpectedForViewers(ServerLevel level, BlockPos pos, ActiveAssembly active) {
        for (UUID viewerId : Set.copyOf(active.viewers)) {
            ServerPlayer viewer = level.getServer().getPlayerList().getPlayer(viewerId);
            if (viewer == null) continue;
            if (viewer.level() != level) {
                clearExpected(viewer);
                continue;
            }
            syncExpected(viewer, level, pos, active);
        }
    }

    private static void hideExpectedForViewers(ServerLevel level, ActiveAssembly active) {
        for (UUID viewerId : Set.copyOf(active.viewers)) {
            ServerPlayer viewer = level.getServer().getPlayerList().getPlayer(viewerId);
            if (viewer != null) clearExpected(viewer);
        }
    }

    private static void clearExpectedForViewers(ServerLevel level, ActiveAssembly active) {
        hideExpectedForViewers(level, active);
        active.viewers.clear();
    }

    private static void clearExpected(ServerPlayer player) {
        if (isFakePlayer(player)) return;
        PacketDistributor.sendToPlayer(player, AssemblyNextStepPayload.clear());
    }

    private static boolean isFakePlayer(ServerPlayer player) {
        return player instanceof FakePlayer;
    }

    private static String expectedName(
            AssemblyPlan.Step step,
            Map<Integer, net.mads.industron.material.IndustrialMaterial> bindings
    ) {
        return switch (step.kind()) {
            case MATERIAL -> {
                var material = step.fixedMaterial() != null
                        ? step.fixedMaterial()
                        : bindings.get(step.bindingId());
                yield material == null
                        ? "Any " + step.material().displayName()
                        : material.displayName() + " " + step.material().displayName();
            }
            case ITEM -> BuiltInRegistries.ITEM.getOptional(step.itemId())
                    .map(item -> item.getDescription().getString())
                    .orElse(step.itemId().toString());
            case TOOL -> step.tool().displayName() + " (Tool)";
            case WAIT -> "Waiting...";
        };
    }

    private static void finish(ServerLevel level, BlockPos pos, ActiveAssembly active) {
        clearExpectedForViewers(level, active);
        active.refund.clear();

        if (!active.workbench) {
            if (!level.getBlockState(pos).is(active.recipe.baseBlock())) return;

            if (active.recipe.hasBlockBaseOutput()) {
                level.setBlock(pos, active.recipe.outputBlock().defaultBlockState(), 3);
                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.8F, 1.0F);
            } else {
                level.removeBlock(pos, false);
                net.minecraft.world.Containers.dropItemStack(
                        level,
                        pos.getX() + 0.5D,
                        pos.getY() + 0.5D,
                        pos.getZ() + 0.5D,
                        new ItemStack(active.recipe.outputItem())
                );
                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8F, 1.0F);
            }
            return;
        }

        if (!(level.getBlockEntity(pos) instanceof AssemblyWorkbenchBlockEntity workbench)) return;
        BlockPos outputPos = pos.above();
        workbench.clearAssemblyData();

        if (active.recipe.hasBlockBaseOutput()) {
            if (active.recipe.hasItemBaseInput()) workbench.clearDisplayedStack();
            level.setBlock(outputPos, active.recipe.outputBlock().defaultBlockState(), 3);
            level.playSound(null, outputPos, net.minecraft.sounds.SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.8F, 1.0F);
        } else {
            if (active.recipe.hasBlockBaseInput()) level.removeBlock(outputPos, false);
            workbench.setDisplayedStack(new ItemStack(active.recipe.outputItem()), true);
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8F, 1.0F);
        }
    }

    private static void refund(Player player, ActiveAssembly active) {
        for (ItemStack stack : active.refund) {
            ItemStack refund = stack.copy();
            if (!player.addItem(refund)) player.drop(refund, false);
        }
        active.refund.clear();
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemStack remaining = stack.copy();
        if (!player.addItem(remaining)) player.drop(remaining, false);
    }


    private static void dropRefund(ServerLevel level, BlockPos pos, ActiveAssembly active) {
        for (ItemStack stack : active.refund) {
            net.minecraft.world.Containers.dropItemStack(
                    level,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.5D,
                    pos.getZ() + 0.5D,
                    stack.copy()
            );
        }
        active.refund.clear();
    }

    /** Called by the workbench block for explosions, pistons and non-player removal. */
    public static void dropAndClearWorkbenchAssembly(
            ServerLevel level,
            BlockPos pos,
            AssemblyWorkbenchBlockEntity workbench
    ) {
        ActiveAssembly active = ACTIVE.remove(key(level, pos));
        if (active == null) active = loadWorkbenchAssembly(level, pos, workbench);
        if (active != null) {
            clearExpectedForViewers(level, active);
            dropRefund(level, pos, active);
        } else if (workbench.hasAssemblyData()) {
            dropStoredRefunds(level, pos, workbench.assemblyData());
        }
        workbench.clearAssemblyData();
    }

    private static boolean hasValidBase(ServerLevel level, BlockPos pos, ActiveAssembly active) {
        if (!active.workbench) {
            return active.recipe.hasBlockBaseInput() && level.getBlockState(pos).is(active.recipe.baseBlock());
        }
        if (!(level.getBlockEntity(pos) instanceof AssemblyWorkbenchBlockEntity workbench)) return false;
        if (active.recipe.hasBlockBaseInput()) return false;
        return !workbench.resultReady()
                && workbench.hasDisplayedStack()
                && workbench.displayedStack().is(active.recipe.baseItem())
                && level.getBlockState(pos.above()).isAir();
    }

    private static BlockPos clickedWorkbench(Level level, BlockPos clickedPos, Direction face) {
        if (face != Direction.UP) return null;
        return level.getBlockEntity(clickedPos) instanceof AssemblyWorkbenchBlockEntity
                ? clickedPos.immutable()
                : null;
    }

    private static Key key(Level level, BlockPos pos) {
        return new Key(level.dimension().location().toString(), pos.immutable());
    }

    private static ActiveAssembly removeActive(ServerLevel level, BlockPos pos) {
        Key key = key(level, pos);
        ActiveAssembly active = ACTIVE.remove(key);
        if (active == null && level.getBlockEntity(pos) instanceof AssemblyWorkbenchBlockEntity workbench) {
            active = loadWorkbenchAssembly(level, pos, workbench);
        }
        return active;
    }

    private static void persistWorkbenchAssembly(ServerLevel level, BlockPos pos, ActiveAssembly active) {
        if (!active.workbench) return;
        if (level.getBlockEntity(pos) instanceof AssemblyWorkbenchBlockEntity workbench) {
            workbench.setAssemblyData(active.save(level.registryAccess()));
        }
    }

    private static void clearWorkbenchAssemblyData(ServerLevel level, BlockPos pos, ActiveAssembly active) {
        if (!active.workbench) return;
        if (level.getBlockEntity(pos) instanceof AssemblyWorkbenchBlockEntity workbench) {
            workbench.clearAssemblyData();
        }
    }

    private static ActiveAssembly loadWorkbenchAssembly(
            ServerLevel level,
            BlockPos pos,
            AssemblyWorkbenchBlockEntity workbench
    ) {
        if (!workbench.hasAssemblyData()) return null;
        CompoundTag tag = workbench.assemblyData();
        AssemblyRecipeDefinition recipe = AssemblyRecipes.find(tag.getString("Recipe"));
        if (recipe == null) {
            dropStoredRefunds(level, pos, tag);
            workbench.clearAssemblyData();
            return null;
        }
        try {
            return ActiveAssembly.load(tag, recipe, level.registryAccess());
        } catch (RuntimeException exception) {
            Industron.LOGGER.error("Cannot restore assembly recipe {} at {}", recipe.id(), pos, exception);
            dropStoredRefunds(level, pos, tag);
            workbench.clearAssemblyData();
            return null;
        }
    }

    private static void dropStoredRefunds(ServerLevel level, BlockPos pos, CompoundTag tag) {
        ListTag refunds = tag.getList("Refund", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int index = 0; index < refunds.size(); index++) {
            ItemStack stack = ItemStack.parseOptional(level.registryAccess(), refunds.getCompound(index));
            if (stack.isEmpty()) continue;
            net.minecraft.world.Containers.dropItemStack(
                    level,
                    pos.getX() + 0.5D,
                    pos.getY() + 1.05D,
                    pos.getZ() + 0.5D,
                    stack
            );
        }
    }

    private static void play(ServerLevel level, BlockPos pos, SoundEvent sound) {
        if (sound != null) level.playSound(null, pos, sound, SoundSource.BLOCKS, 0.8F, 1.0F);
    }

    private static String formatSeconds(long ticks) {
        return String.format(java.util.Locale.ROOT, "%.1f", ticks / 20.0F);
    }

    private static ServerLevel findLevel(net.minecraft.server.MinecraftServer server, String dimension) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().location().toString().equals(dimension)) return level;
        }
        return null;
    }

    private record Key(String dimension, BlockPos pos) {
    }

    private static final class ActiveAssembly {
        private final AssemblyRecipeDefinition recipe;
        private final List<AssemblyPlan.Step> plan;
        private final boolean workbench;
        private final List<ItemStack> refund = new ArrayList<>();
        private final Set<UUID> viewers = new HashSet<>();
        private final Map<Integer, net.mads.industron.material.IndustrialMaterial> materialBindings = new HashMap<>();
        private int index;
        private long waitUntilGameTime = -1;
        private UUID toolUser;
        private ToolDefinition activeTool;
        private int toolProgressTicks;
        private boolean toolAutomated;

        private ActiveAssembly(
                AssemblyRecipeDefinition recipe,
                List<AssemblyPlan.Step> plan,
                boolean workbench
        ) {
            this.recipe = recipe;
            this.plan = plan;
            this.workbench = workbench;
        }

        private AssemblyPlan.Step current() { return plan.get(index); }
        private boolean finished() { return index >= plan.size(); }

        private CompoundTag save(HolderLookup.Provider registries) {
            CompoundTag tag = new CompoundTag();
            tag.putString("Recipe", recipe.id());
            tag.putInt("Step", index);
            tag.putLong("WaitUntil", waitUntilGameTime);

            ListTag refunds = new ListTag();
            refund.forEach(stack -> refunds.add(stack.saveOptional(registries)));
            tag.put("Refund", refunds);

            CompoundTag bindings = new CompoundTag();
            materialBindings.forEach((bindingId, material) ->
                    bindings.putString(Integer.toString(bindingId), material.id()));
            tag.put("Bindings", bindings);
            return tag;
        }

        private static ActiveAssembly load(
                CompoundTag tag,
                AssemblyRecipeDefinition recipe,
                HolderLookup.Provider registries
        ) {
            List<AssemblyPlan.Step> plan = AssemblyPlan.compile(recipe);
            ActiveAssembly active = new ActiveAssembly(recipe, plan, true);
            active.index = Math.max(0, Math.min(tag.getInt("Step"), plan.size()));
            active.waitUntilGameTime = tag.contains("WaitUntil") ? tag.getLong("WaitUntil") : -1L;

            ListTag refunds = tag.getList("Refund", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int index = 0; index < refunds.size(); index++) {
                ItemStack stack = ItemStack.parseOptional(registries, refunds.getCompound(index));
                if (!stack.isEmpty()) active.refund.add(stack);
            }

            CompoundTag bindings = tag.getCompound("Bindings");
            for (String key : bindings.getAllKeys()) {
                int bindingId;
                try {
                    bindingId = Integer.parseInt(key);
                } catch (NumberFormatException ignored) {
                    continue;
                }
                String materialId = bindings.getString(key);
                IndustrialMaterials.ALL.stream()
                        .filter(material -> material.id().equals(materialId))
                        .findFirst()
                        .ifPresent(material -> active.materialBindings.put(bindingId, material));
            }
            return active;
        }
    }
}
