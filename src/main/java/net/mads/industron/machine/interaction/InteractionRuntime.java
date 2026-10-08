package net.mads.industron.machine.interaction;

import net.mads.industron.machine.tree.TreeExtractionSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Shared evaluator for planned machine and recipe interactions. */
public final class InteractionRuntime {

    private InteractionRuntime() {
    }

    public static boolean conditionsMatch(
            List<MachineCondition> conditions,
            InteractionContext context,
            InteractionPhase phase
    ) {
        return conditions.stream()
                .filter(condition ->
                        condition.check() == phase
                                || condition.check() == InteractionPhase.WHILE_PROCESSING
                                && phase == InteractionPhase.ON_START
                )
                .allMatch(condition ->
                        condition.matches(context)
                );
    }

    public static ConditionFailure failedConditionBehavior(
            List<MachineCondition> conditions,
            InteractionContext context,
            InteractionPhase phase
    ) {
        for (MachineCondition condition : conditions) {
            if (condition.check() == phase
                    && !condition.matches(context)) {
                return condition.onFailure();
            }
        }

        return null;
    }

    public static boolean interactionsMatch(
            List<BlockInteraction> interactions,
            InteractionContext context,
            InteractionPhase phase
    ) {
        return interactions.stream()
                .filter(interaction -> interaction.when() == phase)
                .allMatch(interaction ->
                        matchingRequirement(interaction, context).isPresent()
                );
    }

    public static boolean applyInteractions(
            List<BlockInteraction> interactions,
            InteractionContext context,
            InteractionPhase phase
    ) {
        for (BlockInteraction interaction : interactions) {
            if (interaction.when() == phase && !applyInteraction(interaction, context)) {
                return false;
            }
        }
        return true;
    }

    public static Optional<MachineModifier> firstMatchingModifier(
            List<MachineModifier> modifiers,
            InteractionContext context
    ) {
        return modifiers.stream()
                .filter(modifier -> modifierMatches(modifier, context))
                .findFirst();
    }

    public static int adjustedDuration(
            int duration,
            Optional<MachineModifier> machineModifier,
            Optional<MachineModifier> recipeModifier
    ) {
        float speedBonus =
                machineModifier.map(MachineModifier::speedBonus).orElse(0.0F)
                        + recipeModifier.map(MachineModifier::speedBonus).orElse(0.0F);

        return Math.max(
                1,
                Math.round(duration / Math.max(0.01F, 1.0F + speedBonus))
        );
    }

    public static int adjustedResource(
            int resourcePerTick,
            boolean steam,
            Optional<MachineModifier> machineModifier,
            Optional<MachineModifier> recipeModifier
    ) {
        if (resourcePerTick <= 0) {
            return resourcePerTick;
        }

        float bonus =
                (steam
                        ? machineModifier.map(MachineModifier::steamUsageBonus)
                        : machineModifier.map(MachineModifier::energyUsageBonus)
                ).orElse(0.0F)
                        + (steam
                        ? recipeModifier.map(MachineModifier::steamUsageBonus)
                        : recipeModifier.map(MachineModifier::energyUsageBonus)
                ).orElse(0.0F);

        return Math.max(
                0,
                Math.round(resourcePerTick * Math.max(0.0F, 1.0F + bonus))
        );
    }

    private static boolean modifierMatches(
            MachineModifier modifier,
            InteractionContext context
    ) {
        for (ModifierRequirement requirement : modifier.requirements()) {
            boolean matches = switch (requirement.type()) {
                case ITEM -> requirement.matchesItems(context.itemInputs());
                case FLUID -> requirement.matchesFluids(context.fluidInputs());

                case BLOCK -> requirement.blockInteraction()
                        .flatMap(interaction ->
                                matchingRequirement(interaction, context)
                        )
                        .isPresent();

                case CONDITION -> requirement.condition()
                        .map(condition ->
                                condition.matches(context)
                        )
                        .orElse(false);
            };

            if (!matches) {
                return false;
            }
        }

        return true;
    }

    private static Optional<BlockRequirement> matchingRequirement(
            BlockInteraction interaction,
            InteractionContext context
    ) {
        BlockRequirement first = null;
        int matches = 0;
        for (BlockPos pos : targetPositions(interaction, context)) {
            Optional<BlockRequirement> match = matchingRequirementAt(interaction, context.level(), pos);
            if (match.isEmpty()) continue;
            if (first == null) first = match.get();
            matches++;
            if (matches >= interaction.minimumMatches()) {
                return Optional.of(first);
            }
        }
        return Optional.empty();
    }

    private static Optional<BlockRequirement> matchingRequirementAt(
            BlockInteraction interaction,
            Level level,
            BlockPos pos
    ) {
        if (interaction.type() == BlockInteraction.Type.SPRINKLER) {
            return Optional.of(interaction.requirement());
        }
        if (interaction.type() == BlockInteraction.Type.TREE_EXTRACT) {
            return matchingTreeRequirement(interaction, level, pos);
        }
        return interaction.requirement().matchingLeaf(level, level.getBlockState(pos));
    }

    private static List<BlockPos> targetPositions(BlockInteraction interaction, InteractionContext context) {
        List<BlockPos> positions = interaction.area()
                .flatMap(name -> Optional.ofNullable(context.areas().get(name)))
                .map(MachineArea.Resolved::positions)
                .orElseGet(() -> List.of(interaction.pos().rotate(context.origin(), context.facing())));

        List<BlockPos> ordered = new ArrayList<>(positions);
        Comparator<BlockPos> distance = Comparator.comparingDouble(pos -> pos.distSqr(context.origin()));
        switch (interaction.selection()) {
            case NEAREST, FIRST -> ordered.sort(distance);
            case FARTHEST -> ordered.sort(distance.reversed());
            case RANDOM -> {
                for (int index = ordered.size() - 1; index > 0; index--) {
                    int swapIndex = context.level().random.nextInt(index + 1);
                    BlockPos current = ordered.get(index);
                    ordered.set(index, ordered.get(swapIndex));
                    ordered.set(swapIndex, current);
                }
            }
            case ALL -> { }
        }
        int limit = interaction.selection() == AreaSelection.ALL
                ? Math.min(interaction.limit(), ordered.size())
                : Math.min(Math.max(1, interaction.limit()), ordered.size());
        return ordered.subList(0, limit);
    }

    private static Optional<BlockRequirement> matchingTreeRequirement(
            BlockInteraction interaction,
            Level level,
            BlockPos pos
    ) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return Optional.empty();
        }

        Optional<TreeExtractionSavedData.TreeEntry> tree =
                TreeExtractionSavedData.get(serverLevel).getTree(pos);

        if (tree.isEmpty()) {
            return Optional.empty();
        }

        BlockState state = level.getBlockState(pos);
        ResourceLocation currentBlockId =
                BuiltInRegistries.BLOCK.getKey(state.getBlock());

        if (!currentBlockId.equals(tree.get().logId())) {
            return Optional.empty();
        }

        return interaction.requirement().matchingLeaf(
                level,
                state
        );
    }

    private static boolean applyInteraction(
            BlockInteraction interaction,
            InteractionContext context
    ) {
        Level level = context.level();
        boolean matchedAny = false;
        int matchedCount = 0;

        for (BlockPos pos : targetPositions(interaction, context)) {
            Optional<BlockRequirement> requirement = matchingRequirementAt(interaction, level, pos);
            if (requirement.isEmpty()) continue;

            matchedAny = true;
            matchedCount++;
            BlockRequirement matched = requirement.get();
            if (interaction.type() != BlockInteraction.Type.DAMAGE
                    && interaction.type() != BlockInteraction.Type.TREE_EXTRACT
                    && level.random.nextFloat() >= matched.actionChance()) continue;

            BlockState state = level.getBlockState(pos);
            switch (interaction.type()) {
                case REQUIRE -> { }
                case CONSUME -> removeWithLoot(level, pos, state, matched);
                case CONVERT -> {
                    removeDrops(level, pos, state, matched);
                    level.setBlock(pos, interaction.targetBlock().defaultBlockState(), Block.UPDATE_ALL);
                    context.wearStore().clear(pos);
                }
                case PLACE -> {
                    level.setBlock(pos, interaction.targetBlock().defaultBlockState(), Block.UPDATE_ALL);
                    context.wearStore().clear(pos);
                }
                case DAMAGE -> damage(level, pos, state, matched, context.wearStore());
                case TREE_EXTRACT -> {
                    if (!extractTreeResource(level, pos)) {
                        return false;
                    }
                }
                case SPRINKLER -> { } // Continuous sprinkler work is stateful and handled by the machine host.
            }
        }

        return interaction.type() == BlockInteraction.Type.REQUIRE
                ? matchedCount >= interaction.minimumMatches()
                : matchedAny;
    }

    private static boolean extractTreeResource(
            Level level,
            BlockPos rootPos
    ) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        return TreeExtractionSavedData.get(serverLevel)
                .consumeLog(rootPos);
    }

    private static void damage(
            Level level,
            BlockPos pos,
            BlockState state,
            BlockRequirement requirement,
            InteractionWearStore wearStore
    ) {
        if (requirement.damage() <= 0
                || requirement.breakAfter() <= 0
                || level.random.nextFloat() >= requirement.damageChance()) {
            return;
        }

        ResourceLocation blockId =
                BuiltInRegistries.BLOCK.getKey(state.getBlock());

        int wear = wearStore.addWear(
                pos,
                blockId,
                requirement.damage()
        );

        if (wear >= requirement.breakAfter()) {
            removeWithLoot(
                    level,
                    pos,
                    state,
                    requirement
            );

            wearStore.clear(pos);
        }
    }

    private static void removeWithLoot(
            Level level,
            BlockPos pos,
            BlockState state,
            BlockRequirement requirement
    ) {
        removeDrops(
                level,
                pos,
                state,
                requirement
        );

        level.setBlock(
                pos,
                Blocks.AIR.defaultBlockState(),
                Block.UPDATE_ALL
        );
    }

    private static void removeDrops(
            Level level,
            BlockPos pos,
            BlockState state,
            BlockRequirement requirement
    ) {
        for (ItemStack stack : requirement.lootTable()
                .roll(state, level.random)) {
            if (!stack.isEmpty()) {
                Block.popResource(
                        level,
                        pos,
                        stack
                );
            }
        }
    }
}