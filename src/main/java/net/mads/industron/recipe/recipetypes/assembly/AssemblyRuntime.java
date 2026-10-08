package net.mads.industron.recipe.recipetypes.assembly;

import net.mads.industron.recipe.recipetypes.assembly.workbench.AssemblyWorkbenchBlockEntity;
import net.mads.industron.recipe.recipes.assembly.AssemblyRecipes;
import net.mads.industron.recipe.recipes.assembly.WorkbenchLevels;
import net.mads.industron.material.MaterialCatalog;
import net.mads.industron.material.MaterialLookup;
import net.mads.industron.material.plant.PlantPartItemCatalog;
import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.tool.ToolMaterialLookup;
import net.mads.industron.tool.ToolMaterialResolver;
import net.mads.industron.tool.ToolStackFactory;

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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
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
    private static final Map<EntityKey, EntityAssemblyState> ENTITY_ACTIVE = new HashMap<>();

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

                List<AssemblyRecipeDefinition> recipes = findWorldBlockStartRecipes(level, pos, event.getItemStack());
                if (recipes.isEmpty()) return;

                try {
                    active = createActive(recipes, false, ItemStack.EMPTY);
                    for (AssemblyRecipeDefinition candidate : recipes)
                        captureWorldBaseMaterial(candidate, level.getBlockState(pos), active.capturedMaterials);
                } catch (IllegalStateException exception) {
                    Industron.LOGGER.error("Cannot compile assembly route candidates {}", recipeIds(recipes), exception);
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

    /**
     * Entity-base Assembly intentionally lives beside the block/workbench runtime instead of
     * inventing a Boat-specific RecipeType. Ctrl starts/continues the Assembly interaction;
     * without Ctrl, vanilla entity interaction (for example mounting a boat) remains untouched.
     */
    @SubscribeEvent
    public static void onRightClickEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Entity target = event.getTarget();
        EntityKey entityKey = entityKey(level, target);
        EntityAssemblyState state = ENTITY_ACTIVE.get(entityKey);

        // A normal right-click must always keep the entity's vanilla interaction (for example
        // mounting a Boat). Ctrl is the explicit signal for both starting and continuing an
        // entity-based Assembly route.
        if (!ControlKeyState.isHeld(player)) return;

        if (state == null) {
            List<AssemblyRecipeDefinition> recipes = findWorldEntityStartRecipes(target, player.getMainHandItem());
            if (recipes.isEmpty()) return;

            try {
                state = new EntityAssemblyState(createActive(recipes, false, ItemStack.EMPTY), target.blockPosition());
            } catch (IllegalStateException exception) {
                Industron.LOGGER.error("Cannot compile entity assembly route candidates {}", recipeIds(recipes), exception);
                return;
            }
            ENTITY_ACTIVE.put(entityKey, state);
        }

        InteractionResult result = applyCurrentEntityStep(level, target, player, player.getMainHandItem(), state);
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


        Iterator<Map.Entry<EntityKey, EntityAssemblyState>> entityIterator = ENTITY_ACTIVE.entrySet().iterator();
        while (entityIterator.hasNext()) {
            Map.Entry<EntityKey, EntityAssemblyState> entry = entityIterator.next();
            EntityAssemblyState state = entry.getValue();
            ActiveAssembly active = state.active;
            ServerLevel level = findLevel(event.getServer(), entry.getKey().dimension);
            if (level == null) continue;

            Entity target = level.getEntity(entry.getKey().entityId);
            if (target == null || !target.isAlive() || !active.recipe.matchesBaseEntity(target)) {
                clearExpectedForViewers(level, active);
                dropRefund(level, state.lastPos, active);
                entityIterator.remove();
                continue;
            }

            BlockPos pos = target.blockPosition();
            state.lastPos = pos.immutable();

            if (tickToolUse(level, pos, active)) {
                if (active.finished()) {
                    finishEntity(level, target, active);
                    entityIterator.remove();
                    continue;
                }
            }

            if (active.waitUntilGameTime < 0 || level.getGameTime() < active.waitUntilGameTime) continue;

            active.waitUntilGameTime = -1;
            advanceAutomatic(level, pos, active);
            if (active.finished()) {
                finishEntity(level, target, active);
                entityIterator.remove();
            } else if (active.waitUntilGameTime >= 0) {
                hideExpectedForViewers(level, active);
            } else {
                syncExpectedForViewers(level, pos, active);
            }
        }
    }

    private static List<AssemblyRecipeDefinition> findWorldBlockStartRecipes(Level level, BlockPos pos, ItemStack firstStep) {
        List<AssemblyRecipeDefinition> matches = new ArrayList<>();
        for (AssemblyRecipeDefinition recipe : AssemblyRecipes.ALL) {
            if (!recipe.usesWorldBlockRuntime() || !recipe.matchesBaseBlock(level.getBlockState(pos))) continue;

            try {
                List<AssemblyPlan.Step> plan = AssemblyPlan.compile(recipe);
                Map<String, IndustrialSubstance> baseCaptures = new HashMap<>();
                captureWorldBaseMaterial(recipe, level.getBlockState(pos), baseCaptures);
                if (plan.isEmpty() || !matches(plan.getFirst(), firstStep, Map.of(), baseCaptures, plan)) continue;
                matches.add(recipe);
            } catch (IllegalStateException exception) {
                Industron.LOGGER.error("Cannot compile assembly recipe {}", recipe.id(), exception);
            }
        }
        return List.copyOf(matches);
    }

    private static List<AssemblyRecipeDefinition> findWorldEntityStartRecipes(Entity entity, ItemStack firstStep) {
        List<AssemblyRecipeDefinition> matches = new ArrayList<>();
        for (AssemblyRecipeDefinition recipe : AssemblyRecipes.ALL) {
            if (!recipe.usesWorldEntityRuntime() || !recipe.matchesBaseEntity(entity)) continue;

            try {
                List<AssemblyPlan.Step> plan = AssemblyPlan.compile(recipe);
                if (plan.isEmpty() || !matches(plan.getFirst(), firstStep, Map.of(), Map.of(), plan)) continue;
                matches.add(recipe);
            } catch (IllegalStateException exception) {
                Industron.LOGGER.error("Cannot compile entity assembly recipe {}", recipe.id(), exception);
            }
        }
        return List.copyOf(matches);
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
        int workbenchLevel = WorkbenchLevels.levelOf(workbench.getBlockState());
        if (workbenchLevel < 1) return InteractionResult.PASS;

        Key key = key(level, workbenchPos);
        ActiveAssembly active = ACTIVE.get(key);
        if (active == null) {
            active = loadWorkbenchAssembly(level, workbenchPos, workbench);
            if (active != null) ACTIVE.put(key, active);
        }

        if (active != null) {
            // A workbench owns its top-face interaction while an assembly is active.
            // BlockItems are valid recipe inputs too, so they must be matched by Assembly
            // before vanilla gets a chance to place the block above the bench.
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
            List<AssemblyRecipeDefinition> recipes = findWorkbenchStartRecipes(workbench, held, workbenchLevel);
            if (recipes.isEmpty()) {
                int requiredLevel = minimumWorkbenchLevelForStart(workbench, held);
                if (requiredLevel > workbenchLevel) {
                    player.displayClientMessage(Component.literal(
                            "Requires Assembly Workbench Level " + requiredLevel + "."
                    ).withStyle(ChatFormatting.YELLOW), true);
                    return InteractionResult.SUCCESS;
                }
                // If a block item is not a valid first assembly step, preserve normal
                // Minecraft placement behaviour. Valid block-item steps are consumed above.
                if (held.getItem() instanceof BlockItem && !ControlKeyState.isHeld(player)) {
                    return InteractionResult.PASS;
                }
                player.displayClientMessage(Component.literal("No assembly starts with this input.").withStyle(ChatFormatting.GRAY), true);
                return InteractionResult.SUCCESS;
            }
            active = createActive(recipes, true, workbench.displayedStack());
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

        List<AssemblyRecipeDefinition> recipes = findWorkbenchStartRecipes(workbench, held, workbenchLevel);
        if (recipes.isEmpty()) {
            int requiredLevel = minimumWorkbenchLevelForStart(workbench, held);
            if (requiredLevel > workbenchLevel) {
                player.displayClientMessage(Component.literal(
                        "Requires Assembly Workbench Level " + requiredLevel + "."
                ).withStyle(ChatFormatting.YELLOW), true);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        active = createActive(recipes, true, workbench.displayedStack());
        ACTIVE.put(key, active);
        persistWorkbenchAssembly(level, workbenchPos, active);
        return applyCurrentStep(level, workbenchPos, player, held, active);
    }

    private static List<AssemblyRecipeDefinition> findWorkbenchStartRecipes(
            AssemblyWorkbenchBlockEntity workbench,
            ItemStack firstStep,
            int workbenchLevel
    ) {
        List<AssemblyRecipeDefinition> matches = new ArrayList<>();
        for (AssemblyRecipeDefinition recipe : AssemblyRecipes.ALL) {
            if (!recipe.hasItemBaseInput()) continue;
            if (recipe.level() > workbenchLevel) continue;

            boolean baseMatches = workbench.hasDisplayedStack()
                    && recipe.matchesBaseItem(workbench.displayedStack());
            if (!baseMatches) continue;
            if (WorkbenchLevels.forStack(workbench.displayedStack()) > workbenchLevel) continue;

            try {
                List<AssemblyPlan.Step> plan = AssemblyPlan.compile(recipe);
                if (plan.isEmpty() || !matches(plan.getFirst(), firstStep, Map.of(), Map.of(), plan)) continue;
                if (!stepAllowedOnWorkbench(plan.getFirst(), firstStep, workbenchLevel)) continue;
                matches.add(recipe);
            } catch (IllegalStateException exception) {
                Industron.LOGGER.error("Cannot compile assembly recipe {}", recipe.id(), exception);
            }
        }
        return List.copyOf(matches);
    }

    private static int minimumWorkbenchLevelForStart(
            AssemblyWorkbenchBlockEntity workbench,
            ItemStack firstStep
    ) {
        if (!workbench.hasDisplayedStack()) return 0;
        int minimum = Integer.MAX_VALUE;
        for (AssemblyRecipeDefinition recipe : AssemblyRecipes.ALL) {
            if (!recipe.hasItemBaseInput() || !recipe.matchesBaseItem(workbench.displayedStack())) continue;
            try {
                List<AssemblyPlan.Step> plan = AssemblyPlan.compile(recipe);
                if (plan.isEmpty() || !matches(plan.getFirst(), firstStep, Map.of(), Map.of(), plan)) continue;
                int required = Math.max(recipe.level(), WorkbenchLevels.forStack(workbench.displayedStack()));
                if (plan.getFirst().kind() == AssemblyPlan.Kind.MATERIAL) {
                    ToolMaterialLookup.Target target = materialTarget(plan.getFirst(), firstStep);
                    if (target != null) required = Math.max(required, WorkbenchLevels.forMaterial(target.material()));
                }
                minimum = Math.min(minimum, required);
            } catch (IllegalStateException exception) {
                Industron.LOGGER.error("Cannot compile assembly recipe {}", recipe.id(), exception);
            }
        }
        return minimum == Integer.MAX_VALUE ? 0 : minimum;
    }

    private static boolean isItemBaseForAnyRecipe(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return AssemblyRecipes.ALL.stream()
                .filter(AssemblyRecipeDefinition::hasItemBaseInput)
                .anyMatch(recipe -> recipe.matchesBaseItem(stack));
    }

    private static ActiveAssembly createActive(
            List<AssemblyRecipeDefinition> recipes,
            boolean workbench,
            ItemStack baseStack
    ) {
        if (recipes.isEmpty()) throw new IllegalArgumentException("Assembly candidate list cannot be empty");
        ActiveAssembly active = new ActiveAssembly(recipes, workbench);
        for (AssemblyRecipeDefinition candidate : recipes)
            captureBaseMaterial(candidate, baseStack, active.capturedMaterials);
        return active;
    }

    private static String recipeIds(List<AssemblyRecipeDefinition> recipes) {
        return recipes.stream().map(AssemblyRecipeDefinition::id).toList().toString();
    }

    private static void captureBaseMaterial(
            AssemblyRecipeDefinition recipe,
            ItemStack baseStack,
            Map<String, IndustrialSubstance> captures
    ) {
        String role = recipe.baseInput().captureRole();
        if (role == null || baseStack == null || baseStack.isEmpty()) return;
        ToolMaterialLookup.Target target = ToolMaterialLookup.find(baseStack);
        if (target == null || target.part() != recipe.baseInput().material()) {
            throw new IllegalStateException("Assembly base capture has no matching material target: " + recipe.id());
        }
        captures.put(role, target.material());
    }

    private static void captureWorldBaseMaterial(
            AssemblyRecipeDefinition recipe,
            BlockState baseState,
            Map<String, IndustrialSubstance> captures
    ) {
        String role = recipe.baseInput().captureRole();
        if (role == null) return;
        AssemblyMaterialCatalog.Target target = AssemblyMaterialCatalog.find(baseState.getBlock());
        if (target == null || target.part() != recipe.baseInput().material()) {
            throw new IllegalStateException("Assembly world-base capture has no matching material target: " + recipe.id());
        }
        captures.put(role, target.material());
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

        int workbenchLevel = active.workbench
                ? WorkbenchLevels.levelOf(level.getBlockState(pos))
                : Integer.MAX_VALUE;
        int heldMaterialLevel = matchingHeldMaterialLevel(active, held);
        if (active.workbench && heldMaterialLevel > workbenchLevel) {
            player.displayClientMessage(Component.literal(
                    "Requires Assembly Workbench Level " + heldMaterialLevel + "."
            ).withStyle(ChatFormatting.YELLOW), true);
            syncExpected(player, level, pos, active);
            return InteractionResult.SUCCESS;
        }

        if (!selectCandidatesForInteraction(active, held, workbenchLevel)) {
            syncExpected(player, level, pos, active);
            return InteractionResult.SUCCESS;
        }

        AssemblyPlan.Step step = active.current();
        bindCandidateMaterials(active, held);

        switch (step.kind()) {
            case MATERIAL, PLANT_PART, ITEM -> consumeOne(player, held, active, step.consumeChance(), step.sound(), level, pos);
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

    private static InteractionResult applyCurrentEntityStep(
            ServerLevel level,
            Entity target,
            ServerPlayer player,
            ItemStack held,
            EntityAssemblyState state
    ) {
        ActiveAssembly active = state.active;
        BlockPos pos = target.blockPosition();
        state.lastPos = pos.immutable();
        if (!isFakePlayer(player)) active.viewers.add(player.getUUID());

        if (!target.isAlive() || !active.recipe.matchesBaseEntity(target)) {
            clearExpectedForViewers(level, active);
            refund(player, active);
            ENTITY_ACTIVE.remove(entityKey(level, target));
            return InteractionResult.SUCCESS;
        }

        advanceAutomatic(level, pos, active);
        if (active.waitUntilGameTime >= 0) {
            clearExpected(player);
            long ticks = Math.max(0, active.waitUntilGameTime - level.getGameTime());
            if (!isFakePlayer(player)) {
                player.displayClientMessage(Component.literal("Assembly waiting: " + formatSeconds(ticks) + " s").withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResult.SUCCESS;
        }
        if (active.finished()) {
            finishEntity(level, target, active);
            ENTITY_ACTIVE.remove(entityKey(level, target));
            return InteractionResult.SUCCESS;
        }

        if (!selectCandidatesForInteraction(active, held, Integer.MAX_VALUE)) {
            syncExpected(player, level, pos, active);
            return InteractionResult.SUCCESS;
        }

        AssemblyPlan.Step step = active.current();
        bindCandidateMaterials(active, held);

        switch (step.kind()) {
            case MATERIAL, PLANT_PART, ITEM -> consumeOne(player, held, active, step.consumeChance(), step.sound(), level, pos);
            case TOOL -> {
                boolean completedImmediately = beginToolUse(player, held, active, step, level, pos);
                if (completedImmediately && active.finished()) {
                    EntityKey key = entityKey(level, target);
                    finishEntity(level, target, active);
                    ENTITY_ACTIVE.remove(key);
                } else if (!completedImmediately) {
                    syncExpected(player, level, pos, active);
                }
                return InteractionResult.SUCCESS;
            }
            case WAIT -> throw new IllegalStateException("WAIT should be handled automatically");
        }

        active.index++;
        advanceAutomatic(level, pos, active);
        if (active.finished() && active.waitUntilGameTime < 0) {
            EntityKey key = entityKey(level, target);
            finishEntity(level, target, active);
            ENTITY_ACTIVE.remove(key);
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

    private static void consumeOne(
            ServerPlayer player,
            ItemStack held,
            ActiveAssembly active,
            int consumeChance,
            SoundEvent sound,
            ServerLevel level,
            BlockPos pos
    ) {
        if (!player.isCreative()) {
            ItemStack consumed = held.copyWithCount(1);
            active.refund.add(consumed.copy());
            if (consumeChance < AssemblyRecipeDefinition.MAX_CHANCE) {
                active.conditionalConsumptions.add(new ConditionalConsumption(consumed.copy(), consumeChance));
            }
            held.shrink(1);
        }
        play(level, pos, sound);
    }

    private static boolean beginToolUse(ServerPlayer player, ItemStack held, ActiveAssembly active, AssemblyPlan.Step step, ServerLevel level, BlockPos pos) {
        ToolVariantDefinition tool = AssemblyTools.find(step.tool(), held);
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
            ToolVariantDefinition tool,
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
        ToolVariantDefinition tool = AssemblyTools.find(step.tool(), held);
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
        while (!active.finished() && active.waitUntilGameTime < 0) {
            AssemblyPlan.Step wait = commonCurrentStep(active);
            if (wait == null || wait.kind() != AssemblyPlan.Kind.WAIT) return;
            active.index++;
            play(level, pos, wait.sound());
            active.waitUntilGameTime = level.getGameTime() + wait.waitTicks();
        }
    }

    /**
     * Keeps every recipe that still matches the interaction history. Shared prefixes are
     * consumed only once; the concrete recipe is selected as soon as the player performs
     * the first interaction that distinguishes the branches.
     */
    private static boolean selectCandidatesForInteraction(ActiveAssembly active, ItemStack held, int workbenchLevel) {
        if (active.candidates.size() <= 1) {
            return matches(active.current(), held, active.materialBindings, active.capturedMaterials, active.plan)
                    && stepAllowedOnWorkbench(active.current(), held, workbenchLevel);
        }

        List<AssemblyRecipeDefinition> matchingRecipes = new ArrayList<>();
        List<List<AssemblyPlan.Step>> matchingPlans = new ArrayList<>();
        for (AssemblyRecipeDefinition candidate : active.candidates) {
            List<AssemblyPlan.Step> candidatePlan = AssemblyPlan.compile(candidate);
            if (active.index >= candidatePlan.size()) continue;
            if (!matches(
                    candidatePlan.get(active.index),
                    held,
                    active.bindingsFor(candidate),
                    active.capturedMaterials,
                    candidatePlan
            )) continue;
            if (!stepAllowedOnWorkbench(candidatePlan.get(active.index), held, workbenchLevel)) continue;
            matchingRecipes.add(candidate);
            matchingPlans.add(candidatePlan);
        }

        if (matchingRecipes.isEmpty()) return false;
        AssemblyPlan.Step selectedStep = matchingPlans.getFirst().get(active.index);
        for (int index = 1; index < matchingPlans.size(); index++) {
            AssemblyPlan.Step other = matchingPlans.get(index).get(active.index);
            if (!sameBranchStep(selectedStep, other)
                    && !sameConsumedInteraction(selectedStep, other)) {
                Industron.LOGGER.error(
                        "Assembly interaction remains ambiguous between {} at step {}",
                        recipeIds(matchingRecipes),
                        active.index + 1
                );
                return false;
            }
        }

        active.setCandidates(matchingRecipes, matchingPlans.getFirst());
        return true;
    }

    private static boolean stepAllowedOnWorkbench(AssemblyPlan.Step step, ItemStack stack, int workbenchLevel) {
        if (workbenchLevel == Integer.MAX_VALUE || step.kind() != AssemblyPlan.Kind.MATERIAL) return true;
        ToolMaterialLookup.Target target = materialTarget(step, stack);
        return target == null || WorkbenchLevels.forMaterial(target.material()) <= workbenchLevel;
    }

    private static int matchingHeldMaterialLevel(ActiveAssembly active, ItemStack held) {
        int required = 1;
        for (AssemblyRecipeDefinition candidate : active.candidates) {
            List<AssemblyPlan.Step> candidatePlan = AssemblyPlan.compile(candidate);
            if (active.index >= candidatePlan.size()) continue;
            AssemblyPlan.Step step = candidatePlan.get(active.index);
            if (step.kind() != AssemblyPlan.Kind.MATERIAL) continue;
            if (!matches(step, held, active.bindingsFor(candidate), active.capturedMaterials, candidatePlan)) continue;
            ToolMaterialLookup.Target target = materialTarget(step, held);
            if (target != null) required = Math.max(required, WorkbenchLevels.forMaterial(target.material()));
        }
        return required;
    }

    private static AssemblyPlan.Step commonCurrentStep(ActiveAssembly active) {
        if (active.finished()) return null;
        if (active.candidates.size() <= 1) return active.current();

        AssemblyPlan.Step common = null;
        for (AssemblyRecipeDefinition candidate : active.candidates) {
            List<AssemblyPlan.Step> plan = AssemblyPlan.compile(candidate);
            if (active.index >= plan.size()) return null;
            AssemblyPlan.Step step = plan.get(active.index);
            if (common == null) common = step;
            else if (!sameBranchStep(common, step)) return null;
        }
        return common;
    }

    /**
     * Two already-matched consumed inputs may describe the same physical click at different
     * abstraction levels (for example an exact Oak Stick versus Material.STICK/ANY). Keeping
     * both candidates is safe as long as neither step carries future material-capture state.
     */
    private static boolean sameConsumedInteraction(AssemblyPlan.Step a, AssemblyPlan.Step b) {
        if (!isConsumedInput(a) || !isConsumedInput(b)) return false;
        if (a.consumeChance() != b.consumeChance()) return false;
        if (!java.util.Objects.equals(a.sound(), b.sound())) return false;
        if (a.captureRole() != null || b.captureRole() != null) return false;
        if (!a.capturedRequirements().isEmpty() || !b.capturedRequirements().isEmpty()) return false;
        return true;
    }

    private static boolean isConsumedInput(AssemblyPlan.Step step) {
        return step.kind() == AssemblyPlan.Kind.MATERIAL
                || step.kind() == AssemblyPlan.Kind.PLANT_PART
                || step.kind() == AssemblyPlan.Kind.ITEM;
    }

    private static boolean sameBranchStep(AssemblyPlan.Step a, AssemblyPlan.Step b) {
        if (a.kind() != b.kind()) return false;
        return switch (a.kind()) {
            case PLANT_PART -> a.plantPart() == b.plantPart()
                    && a.consumeChance() == b.consumeChance();
            case ITEM -> java.util.Objects.equals(a.itemId(), b.itemId())
                    && a.consumeChance() == b.consumeChance();
            case TOOL -> java.util.Objects.equals(a.tool(), b.tool())
                    && java.util.Objects.equals(a.requirements(), b.requirements());
            case WAIT -> a.waitTicks() == b.waitTicks();
            case MATERIAL -> a.material() == b.material()
                    && java.util.Objects.equals(a.fixedMaterial(), b.fixedMaterial())
                    && java.util.Objects.equals(a.materialCategory(), b.materialCategory())
                    && java.util.Objects.equals(a.materialSelector(), b.materialSelector())
                    && java.util.Objects.equals(a.requirements(), b.requirements())
                    && java.util.Objects.equals(a.capturedRequirements(), b.capturedRequirements())
                    && java.util.Objects.equals(a.captureRole(), b.captureRole())
                    && a.consumeChance() == b.consumeChance();
        };
    }

    private static boolean matches(
            AssemblyPlan.Step step,
            ItemStack stack,
            Map<Integer, IndustrialSubstance> bindings,
            Map<String, IndustrialSubstance> captures,
            List<AssemblyPlan.Step> plan
    ) {
        if (stack.isEmpty()) return false;

        ToolMaterialLookup.Target target = step.kind() == AssemblyPlan.Kind.MATERIAL
                ? materialTarget(step, stack)
                : null;
        boolean base = switch (step.kind()) {
            case MATERIAL -> target != null
                    && target.part() == step.material()
                    && matchesMaterialBinding(step, target.material(), bindings, plan);
            case PLANT_PART -> PlantPartItemCatalog.contains(
                    step.plantPart(),
                    BuiltInRegistries.ITEM.getKey(stack.getItem())
            );
            case ITEM -> BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(step.itemId());
            case TOOL -> AssemblyTools.find(step.tool(), stack) != null;
            case WAIT -> false;
        };
        if (!base) return false;

        if (step.kind() == AssemblyPlan.Kind.TOOL) {
            return step.requirements().stream().allMatch(requirement -> requirement.matches(stack));
        }
        if (step.kind() != AssemblyPlan.Kind.MATERIAL) return true;
        if (!step.requirements().isEmpty()
                && !step.requirements().stream().allMatch(requirement -> requirement.matches(stack))) {
            return false;
        }
        return step.capturedRequirements().stream()
                .allMatch(requirement -> requirement.matches(stack, captures));
    }

    private static ToolMaterialLookup.Target materialTarget(AssemblyPlan.Step step, ItemStack stack) {
        if (step.materialSelector() != null && step.materialSelector().isToolSelector()) {
            return ToolMaterialLookup.find(stack);
        }
        AssemblyMaterialCatalog.Target target = AssemblyMaterialCatalog.find(stack);
        return target == null ? null : target.asToolTarget();
    }

    private static boolean matchesMaterialBinding(
            AssemblyPlan.Step step,
            IndustrialSubstance candidate,
            Map<Integer, IndustrialSubstance> bindings,
            List<AssemblyPlan.Step> plan
    ) {
        if (!step.acceptsMaterial(candidate)) return false;
        if (step.fixedMaterial() != null) return step.fixedMaterial().id().equals(candidate.id());

        IndustrialSubstance selected = bindings.get(step.bindingId());
        if (selected != null) return sameMaterial(selected, candidate);

        // Dynamic ids are leaf-local for normal free/category selectors. Keep this grouped
        // feasibility check so explicitly shared ids remain deterministic if added later.
        return plan.stream()
                .filter(candidateStep -> candidateStep.kind() == AssemblyPlan.Kind.MATERIAL)
                .filter(AssemblyPlan.Step::usesDynamicBinding)
                .filter(candidateStep -> candidateStep.bindingId() == step.bindingId())
                .allMatch(candidateStep -> candidateStep.acceptsMaterial(candidate)
                        && exposesPart(candidateStep, candidate)
                        && requirementsMatch(candidateStep, candidate));
    }

    private static boolean exposesPart(AssemblyPlan.Step step, IndustrialSubstance material) {
        if (step.materialSelector().isToolSelector()) {
            return step.materialSelector().matchesTool(material, step.material());
        }
        return AssemblyMaterialCatalog.exposesPart(material, step.material());
    }

    private static boolean requirementsMatch(AssemblyPlan.Step step, IndustrialSubstance material) {
        if (step.requirements().isEmpty()) return true;
        if (!(material instanceof IndustrialMaterial industrial)) return false;
        return step.requirements().stream()
                .allMatch(requirement -> requirement.matches(industrial, step.material()));
    }

    private static boolean sameMaterial(IndustrialSubstance a, IndustrialSubstance b) {
        return ToolMaterialResolver.key(a).equals(ToolMaterialResolver.key(b));
    }

    private static void bindCandidateMaterials(ActiveAssembly active, ItemStack held) {
        for (AssemblyRecipeDefinition candidate : active.candidates) {
            List<AssemblyPlan.Step> candidatePlan = AssemblyPlan.compile(candidate);
            bindMaterial(candidatePlan.get(active.index), held, active.bindingsFor(candidate), active.capturedMaterials);
        }
        active.materialBindings.clear();
        active.materialBindings.putAll(active.bindingsFor(active.recipe));
    }

    private static void bindMaterial(
            AssemblyPlan.Step step,
            ItemStack stack,
            Map<Integer, IndustrialSubstance> bindings,
            Map<String, IndustrialSubstance> captures
    ) {
        if (step.kind() != AssemblyPlan.Kind.MATERIAL) return;
        ToolMaterialLookup.Target target = materialTarget(step, stack);
        if (target == null) throw new IllegalStateException("Matched material step has no material target");

        if (step.usesDynamicBinding() && !bindings.containsKey(step.bindingId())) {
            bindings.put(step.bindingId(), target.material());
        }
        if (step.captureRole() != null) {
            IndustrialSubstance previous = captures.putIfAbsent(step.captureRole(), target.material());
            if (previous != null && !sameMaterial(previous, target.material())) {
                throw new IllegalStateException("Assembly capture role changed material: " + step.captureRole());
            }
        }
    }

    private static void syncExpected(ServerPlayer player, ServerLevel level, BlockPos pos, ActiveAssembly active) {
        if (isFakePlayer(player)) return;
        active.viewers.add(player.getUUID());
        if (active.finished() || active.waitUntilGameTime >= 0) {
            clearExpected(player);
            return;
        }

        String dimension = level.dimension().location().toString();
        AssemblyPlan.Step commonStep = commonCurrentStep(active);
        String nextStep = commonStep == null
                ? "Choose next valid assembly step"
                : expectedName(commonStep, active.materialBindings);
        AssemblyNextStepPayload payload = commonStep != null
                && commonStep.kind() == AssemblyPlan.Kind.TOOL
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
            Map<Integer, IndustrialSubstance> bindings
    ) {
        return switch (step.kind()) {
            case MATERIAL -> {
                var material = step.fixedMaterial() != null
                        ? step.fixedMaterial()
                        : bindings.get(step.bindingId());
                yield material == null
                        ? step.materialSelector().displayName() + " " + step.material().displayName()
                        : material.displayName() + " " + step.material().displayName();
            }
            case PLANT_PART -> "Any " + prettyPlantPart(step.plantPart().name());
            case ITEM -> BuiltInRegistries.ITEM.getOptional(step.itemId())
                    .map(item -> item.getDescription().getString())
                    .orElse(step.itemId().toString());
            case TOOL -> step.tool().displayName() + " (Tool)";
            case WAIT -> "Waiting...";
        };
    }

    private static String prettyPlantPart(String name) {
        String lower = name.toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static String categoryName(net.mads.industron.material.MaterialCategory category) {
        String lower = category.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static void finish(ServerLevel level, BlockPos pos, ActiveAssembly active) {
        clearExpectedForViewers(level, active);

        // Do not resolve chances or consume held inputs if the physical base disappeared/changed
        // before the final step completed. In that edge case all consumed inputs are refunded.
        if (!hasValidBase(level, pos, active)) {
            dropRefund(level, pos, active);
            clearWorkbenchAssemblyData(level, pos, active);
            return;
        }

        // Chanced inputs were held by Assembly while the sequence ran. Resolve them exactly once
        // at successful completion; failed consume rolls are returned as physical items.
        for (ConditionalConsumption conditional : active.conditionalConsumptions) {
            if (!roll(level, conditional.consumeChance())) {
                dropStack(level, pos, conditional.stack());
            }
        }
        active.conditionalConsumptions.clear();
        active.refund.clear();

        boolean produceMain = roll(level, active.recipe.baseOutputChance());
        boolean producedBlock = false;

        if (!active.workbench) {
            if (!active.recipe.matchesBaseBlock(level.getBlockState(pos))) return;

            if (active.recipe.hasBlockBaseOutput()) {
                if (produceMain) {
                    placeBlockOutput(level, pos, active.recipe);
                    producedBlock = true;
                } else {
                    level.removeBlock(pos, false);
                }
            } else if (active.recipe.hasEntityBaseOutput()) {
                level.removeBlock(pos, false);
                if (produceMain) {
                    active.recipe.outputEntityDefinition().spawn(
                            level,
                            new Vec3(pos.getX() + 0.5D, pos.getY() + 0.1D, pos.getZ() + 0.5D),
                            0.0F
                    );
                }
            } else {
                level.removeBlock(pos, false);
                if (produceMain) {
                    ItemStack result = active.recipe.hasDynamicToolOutput()
                            ? ToolStackFactory.create(active.recipe.toolOutput(), active.capturedMaterials)
                            : new ItemStack(active.recipe.outputItem(), active.recipe.baseOutputCount());
                    dropStack(level, pos, result);
                }
            }
        } else {
            if (!(level.getBlockEntity(pos) instanceof AssemblyWorkbenchBlockEntity workbench)) return;
            BlockPos outputPos = pos.above();
            workbench.clearAssemblyData();

            if (active.recipe.hasBlockBaseOutput()) {
                if (active.recipe.hasItemBaseInput()) workbench.clearDisplayedStack();
                if (produceMain) {
                    placeBlockOutput(level, outputPos, active.recipe);
                    producedBlock = true;
                } else if (active.recipe.hasBlockBaseInput()) {
                    level.removeBlock(outputPos, false);
                }
            } else if (active.recipe.hasEntityBaseOutput()) {
                if (active.recipe.hasItemBaseInput()) workbench.clearDisplayedStack();
                if (active.recipe.hasBlockBaseInput()) level.removeBlock(outputPos, false);
                if (produceMain) {
                    active.recipe.outputEntityDefinition().spawn(
                            level,
                            new Vec3(outputPos.getX() + 0.5D, outputPos.getY() + 0.1D, outputPos.getZ() + 0.5D),
                            0.0F
                    );
                }
            } else {
                if (active.recipe.hasBlockBaseInput()) level.removeBlock(outputPos, false);
                if (produceMain) {
                    ItemStack result = active.recipe.hasDynamicToolOutput()
                            ? ToolStackFactory.create(active.recipe.toolOutput(), active.capturedMaterials)
                            : new ItemStack(active.recipe.outputItem(), active.recipe.baseOutputCount());
                    workbench.setDisplayedStack(result, true);
                    level.playSound(null, pos, net.minecraft.sounds.SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8F, 1.0F);
                } else {
                    workbench.clearDisplayedStack();
                }
            }
        }

        for (AssemblyRecipeDefinition.Byproduct byproduct : active.recipe.byproducts()) {
            if (!roll(level, byproduct.chance())) continue;
            BuiltInRegistries.ITEM.getOptional(byproduct.itemId()).ifPresent(item ->
                    dropStack(level, pos, new ItemStack(item, byproduct.count()))
            );
        }

        if (producedBlock) {
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.8F, 1.0F);
        }
    }

    private static void finishEntity(ServerLevel level, Entity target, ActiveAssembly active) {
        BlockPos pos = target.blockPosition();
        clearExpectedForViewers(level, active);

        if (!target.isAlive() || !active.recipe.matchesBaseEntity(target)) {
            dropRefund(level, pos, active);
            return;
        }

        for (ConditionalConsumption conditional : active.conditionalConsumptions) {
            if (!roll(level, conditional.consumeChance())) {
                dropStack(level, pos, conditional.stack());
            }
        }
        active.conditionalConsumptions.clear();
        active.refund.clear();

        boolean produceMain = roll(level, active.recipe.baseOutputChance());
        Vec3 position = target.position();
        Vec3 velocity = target.getDeltaMovement();
        float yRot = target.getYRot();

        // The entity is the physical Assembly base and is therefore consumed exactly like a
        // placed block base. Discard before spawning the result so Boat -> Chest Boat does not
        // briefly collide with two entities occupying the same space.
        target.ejectPassengers();
        target.discard();

        if (produceMain) {
            if (active.recipe.hasEntityBaseOutput()) {
                Entity result = active.recipe.outputEntityDefinition().spawn(level, position, yRot);
                result.setDeltaMovement(velocity);
            } else if (active.recipe.hasItemBaseOutput()) {
                ItemStack result = active.recipe.hasDynamicToolOutput()
                        ? ToolStackFactory.create(active.recipe.toolOutput(), active.capturedMaterials)
                        : new ItemStack(active.recipe.outputItem(), active.recipe.baseOutputCount());
                dropStack(level, pos, result);
            } else if (active.recipe.hasBlockBaseOutput()) {
                placeBlockOutput(level, pos, active.recipe);
            }
        }

        for (AssemblyRecipeDefinition.Byproduct byproduct : active.recipe.byproducts()) {
            if (!roll(level, byproduct.chance())) continue;
            BuiltInRegistries.ITEM.getOptional(byproduct.itemId()).ifPresent(item ->
                    dropStack(level, pos, new ItemStack(item, byproduct.count()))
            );
        }

        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.8F, 1.0F);
    }

    private static boolean roll(ServerLevel level, int chance) {
        if (chance <= 0) return false;
        if (chance >= AssemblyRecipeDefinition.MAX_CHANCE) return true;
        return level.random.nextInt(AssemblyRecipeDefinition.MAX_CHANCE) < chance;
    }

    private static void placeBlockOutput(ServerLevel level, BlockPos pos, AssemblyRecipeDefinition recipe) {
        var block = recipe.outputBlock();
        int count = recipe.baseOutputCount();
        if (count == 1) {
            level.setBlock(pos, block.defaultBlockState(), 3);
            return;
        }
        if (count == 2 && block instanceof SlabBlock) {
            level.setBlock(pos, block.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE), 3);
            return;
        }
        throw new IllegalStateException(
                "Assembly block output count " + count + " is unsupported for " + BuiltInRegistries.BLOCK.getKey(block)
                        + " in recipe " + recipe.id()
        );
    }

    private static void dropStack(ServerLevel level, BlockPos pos, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        net.minecraft.world.Containers.dropItemStack(
                level,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                stack.copy()
        );
    }

    private static void refund(Player player, ActiveAssembly active) {
        for (ItemStack stack : active.refund) {
            ItemStack refund = stack.copy();
            if (!player.addItem(refund)) player.drop(refund, false);
        }
        active.refund.clear();
        active.conditionalConsumptions.clear();
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
        active.conditionalConsumptions.clear();
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
            return active.recipe.hasBlockBaseInput() && active.recipe.matchesBaseBlock(level.getBlockState(pos));
        }
        if (!(level.getBlockEntity(pos) instanceof AssemblyWorkbenchBlockEntity workbench)) return false;
        if (active.recipe.hasBlockBaseInput()) return false;
        return !workbench.resultReady()
                && workbench.hasDisplayedStack()
                && active.recipe.matchesBaseItem(workbench.displayedStack())
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

    private static EntityKey entityKey(Level level, Entity entity) {
        return new EntityKey(level.dimension().location().toString(), entity.getUUID());
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
        List<AssemblyRecipeDefinition> candidates = new ArrayList<>();
        ListTag savedCandidates = tag.getList("CandidateRecipes", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int index = 0; index < savedCandidates.size(); index++) {
            AssemblyRecipeDefinition candidate = AssemblyRecipes.find(savedCandidates.getCompound(index).getString("Id"));
            if (candidate != null) candidates.add(candidate);
        }
        if (candidates.isEmpty()) {
            AssemblyRecipeDefinition recipe = AssemblyRecipes.find(tag.getString("Recipe"));
            if (recipe != null) candidates.add(recipe);
        }
        if (candidates.isEmpty()) {
            dropStoredRefunds(level, pos, tag);
            workbench.clearAssemblyData();
            return null;
        }
        try {
            return ActiveAssembly.load(tag, candidates, level.registryAccess());
        } catch (RuntimeException exception) {
            Industron.LOGGER.error("Cannot restore assembly recipes {} at {}", recipeIds(candidates), pos, exception);
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

    private record EntityKey(String dimension, UUID entityId) {
    }

    private static final class EntityAssemblyState {
        private final ActiveAssembly active;
        private BlockPos lastPos;

        private EntityAssemblyState(ActiveAssembly active, BlockPos lastPos) {
            this.active = active;
            this.lastPos = lastPos.immutable();
        }
    }

    private record ConditionalConsumption(ItemStack stack, int consumeChance) {
        private ConditionalConsumption {
            stack = stack.copyWithCount(1);
            if (consumeChance < 0 || consumeChance > AssemblyRecipeDefinition.MAX_CHANCE) {
                throw new IllegalArgumentException("Conditional Assembly consume chance out of range");
            }
        }
    }

    private static final class ActiveAssembly {
        private AssemblyRecipeDefinition recipe;
        private List<AssemblyPlan.Step> plan;
        private final List<AssemblyRecipeDefinition> candidates = new ArrayList<>();
        private final boolean workbench;
        private final List<ItemStack> refund = new ArrayList<>();
        private final List<ConditionalConsumption> conditionalConsumptions = new ArrayList<>();
        private final Set<UUID> viewers = new HashSet<>();
        private final Map<Integer, IndustrialSubstance> materialBindings = new HashMap<>();
        private final Map<String, Map<Integer, IndustrialSubstance>> candidateBindings = new HashMap<>();
        private final Map<String, IndustrialSubstance> capturedMaterials = new HashMap<>();
        private int index;
        private long waitUntilGameTime = -1;
        private UUID toolUser;
        private ToolVariantDefinition activeTool;
        private int toolProgressTicks;
        private boolean toolAutomated;

        private ActiveAssembly(
                List<AssemblyRecipeDefinition> recipes,
                boolean workbench
        ) {
            if (recipes.isEmpty()) throw new IllegalArgumentException("Assembly candidate list cannot be empty");
            this.recipe = recipes.getFirst();
            this.plan = AssemblyPlan.compile(this.recipe);
            this.candidates.addAll(recipes);
            this.workbench = workbench;
        }

        private void setCandidates(
                List<AssemblyRecipeDefinition> recipes,
                List<AssemblyPlan.Step> representativePlan
        ) {
            if (recipes.isEmpty()) throw new IllegalArgumentException("Assembly candidate list cannot be empty");
            candidates.clear();
            candidates.addAll(recipes);
            recipe = recipes.getFirst();
            plan = representativePlan;
            Map<Integer, IndustrialSubstance> selectedBindings = bindingsFor(recipe);
            materialBindings.clear();
            materialBindings.putAll(selectedBindings);
            candidateBindings.keySet().removeIf(id -> recipes.stream().noneMatch(candidate -> candidate.id().equals(id)));
        }

        private Map<Integer, IndustrialSubstance> bindingsFor(AssemblyRecipeDefinition candidate) {
            return candidateBindings.computeIfAbsent(candidate.id(), ignored -> new HashMap<>(materialBindings));
        }

        private AssemblyPlan.Step current() { return plan.get(index); }
        private boolean finished() { return index >= plan.size(); }

        private CompoundTag save(HolderLookup.Provider registries) {
            CompoundTag tag = new CompoundTag();
            tag.putString("Recipe", recipe.id());
            ListTag candidateTags = new ListTag();
            for (AssemblyRecipeDefinition candidate : candidates) {
                CompoundTag entry = new CompoundTag();
                entry.putString("Id", candidate.id());
                CompoundTag branchBindings = new CompoundTag();
                bindingsFor(candidate).forEach((id, material) -> branchBindings.putString(Integer.toString(id), ToolMaterialResolver.key(material)));
                entry.put("Bindings", branchBindings);
                candidateTags.add(entry);
            }
            tag.put("CandidateRecipes", candidateTags);
            tag.putInt("Step", index);
            tag.putLong("WaitUntil", waitUntilGameTime);

            ListTag refunds = new ListTag();
            refund.forEach(stack -> refunds.add(stack.saveOptional(registries)));
            tag.put("Refund", refunds);

            ListTag conditional = new ListTag();
            for (ConditionalConsumption value : conditionalConsumptions) {
                CompoundTag entry = new CompoundTag();
                entry.put("Stack", value.stack().saveOptional(registries));
                entry.putInt("Chance", value.consumeChance());
                conditional.add(entry);
            }
            tag.put("ConditionalConsume", conditional);

            CompoundTag bindings = new CompoundTag();
            materialBindings.forEach((bindingId, material) ->
                    bindings.putString(Integer.toString(bindingId), ToolMaterialResolver.key(material)));
            tag.put("Bindings", bindings);

            CompoundTag captures = new CompoundTag();
            capturedMaterials.forEach((role, material) -> captures.putString(role, ToolMaterialResolver.key(material)));
            tag.put("Captures", captures);
            return tag;
        }

        private static ActiveAssembly load(
                CompoundTag tag,
                List<AssemblyRecipeDefinition> recipes,
                HolderLookup.Provider registries
        ) {
            ActiveAssembly active = new ActiveAssembly(recipes, true);
            active.index = Math.max(0, Math.min(tag.getInt("Step"), active.plan.size()));
            active.waitUntilGameTime = tag.contains("WaitUntil") ? tag.getLong("WaitUntil") : -1L;

            ListTag refunds = tag.getList("Refund", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int index = 0; index < refunds.size(); index++) {
                ItemStack stack = ItemStack.parseOptional(registries, refunds.getCompound(index));
                if (!stack.isEmpty()) active.refund.add(stack);
            }

            ListTag conditional = tag.getList("ConditionalConsume", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int index = 0; index < conditional.size(); index++) {
                CompoundTag entry = conditional.getCompound(index);
                ItemStack stack = ItemStack.parseOptional(registries, entry.getCompound("Stack"));
                if (!stack.isEmpty()) {
                    active.conditionalConsumptions.add(new ConditionalConsumption(
                            stack,
                            entry.contains("Chance") ? entry.getInt("Chance") : AssemblyRecipeDefinition.MAX_CHANCE
                    ));
                }
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
                IndustrialSubstance material = ToolMaterialResolver.resolve(materialId);
                // Backward compatibility with assembly saves from before typed tool-material keys.
                if (material == null) {
                    material = MaterialCatalog.all().stream()
                            .filter(candidate -> candidate.id().equals(materialId))
                            .findFirst()
                            .orElse(null);
                }
                if (material != null) active.materialBindings.put(bindingId, material);
            }

            CompoundTag captures = tag.getCompound("Captures");
            for (String role : captures.getAllKeys()) {
                IndustrialSubstance material = ToolMaterialResolver.resolve(captures.getString(role));
                if (material != null) active.capturedMaterials.put(role, material);
            }
            ListTag savedCandidates = tag.getList("CandidateRecipes", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int index = 0; index < savedCandidates.size(); index++) {
                CompoundTag entry = savedCandidates.getCompound(index);
                if (!entry.contains("Bindings")) continue;
                Map<Integer, IndustrialSubstance> values = new HashMap<>();
                CompoundTag branch = entry.getCompound("Bindings");
                for (String key : branch.getAllKeys()) {
                    try {
                        IndustrialSubstance material = ToolMaterialResolver.resolve(branch.getString(key));
                        if (material != null) values.put(Integer.parseInt(key), material);
                    } catch (NumberFormatException ignored) { }
                }
                active.candidateBindings.put(entry.getString("Id"), values);
            }
            return active;
        }
    }
}
