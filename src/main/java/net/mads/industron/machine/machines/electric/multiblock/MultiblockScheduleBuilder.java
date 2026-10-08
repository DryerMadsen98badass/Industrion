package net.mads.industron.machine.machines.electric.multiblock;

import net.mads.industron.machine.MachinePortBlock;
import net.mads.industron.machine.MachineTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MultiblockScheduleBuilder {
    private MultiblockScheduleBuilder() {
    }

    public static boolean build(
            ServerLevel level,
            ServerPlayer player,
            BlockPos controllerPos,
            Direction facing,
            String definitionId,
            String variantId,
            String tierId
    ) {
        MultiblockDefinition definition = MultiblockRegistry.all().stream()
                .filter(candidate -> candidate.id().equals(definitionId))
                .findFirst()
                .orElse(null);
        if (definition == null) {
            return false;
        }

        PatternVariant variant = definition.variants().stream()
                .filter(candidate -> candidate.id().equals(variantId))
                .findFirst()
                .orElse(null);
        if (variant == null) {
            return false;
        }

        MachineTier tier = definition.visualization().tiers().stream()
                .filter(candidate -> candidate.id().equals(tierId))
                .findFirst()
                .orElseGet(() -> definition.visualization().tiers().isEmpty()
                        ? MachineTier.LV
                        : definition.visualization().tiers().getFirst());

        boolean creative = player.getAbilities().instabuild;
        LocalPos controllerLocal = controllerLocalPos(definition, variant);
        List<Placement> placements = new ArrayList<>();
        int[] reservedInventory = new int[player.getInventory().getContainerSize()];
        boolean complete = true;

        for (int x = 0; x < variant.width(); x++) {
            for (int y = 0; y < variant.height(); y++) {
                for (int z = 0; z < variant.length(); z++) {
                    char symbol = variant.symbolAt(x, y, z);
                    if (symbol == MultiblockPattern.air || symbol == MultiblockPattern.ignore) {
                        continue;
                    }

                    BlockPos worldPos = rotate(
                            controllerPos,
                            facing,
                            x - controllerLocal.x(),
                            y - controllerLocal.y(),
                            z - controllerLocal.z()
                    );
                    BlockState existing = level.getBlockState(worldPos);

                    if (symbol == definition.controllerSymbol()) {
                        if (!worldPos.equals(controllerPos) || !existing.is(BuiltInRegistries.BLOCK.get(definition.controllerId()))) {
                            return false;
                        }
                        continue;
                    }

                    // Dynamic predicates (air, tags, OR-combinations, etc.) may already be satisfied
                    // even when they do not have one fixed visualization/build item. Never replace a
                    // valid world block just because the visualization candidate list is narrower.
                    MultiblockPredicate predicate = definition.predicate(symbol);
                    if (predicate != null && predicate.match(level, worldPos, existing).matches()) {
                        continue;
                    }

                    List<ItemStack> candidates = creative
                            ? definition.visualization().validStacks(symbol, tier, definition.controller(), definition.controllerSymbol())
                            : survivalCandidates(definition, symbol, tier);
                    if (candidates.isEmpty()) {
                        return false;
                    }

                    BlockState existingTargetState = matchingExistingState(existing, candidates, facing);
                    if (existingTargetState != null) {
                        if (!existing.equals(existingTargetState)) {
                            placements.add(new Placement(worldPos, existingTargetState));
                        }
                        continue;
                    }

                    if (!existing.canBeReplaced()) {
                        return false;
                    }

                    if (creative) {
                        BlockState state = stateFor(candidates.getFirst(), facing);
                        if (state == null) {
                            return false;
                        }
                        placements.add(new Placement(worldPos, state));
                        continue;
                    }

                    InventoryChoice choice = findInventoryChoice(player, candidates, reservedInventory);
                    if (choice == null) {
                        complete = false;
                        continue;
                    }

                    BlockState state = stateFor(choice.stack(), facing);
                    if (state == null) {
                        return false;
                    }
                    reservedInventory[choice.slot()]++;
                    placements.add(new Placement(worldPos, state));
                }
            }
        }

        if (!creative) {
            for (int slot = 0; slot < reservedInventory.length; slot++) {
                int amount = reservedInventory[slot];
                if (amount <= 0) {
                    continue;
                }
                ItemStack stack = player.getInventory().getItem(slot);
                if (stack.getCount() < amount) {
                    return false;
                }
            }
            for (int slot = 0; slot < reservedInventory.length; slot++) {
                int amount = reservedInventory[slot];
                if (amount > 0) {
                    player.getInventory().getItem(slot).shrink(amount);
                }
            }
            player.getInventory().setChanged();
        }

        for (Placement placement : placements) {
            level.setBlock(placement.pos(), placement.state(), 3);
        }

        if (level.getBlockEntity(controllerPos) instanceof MultiblockControllerBlockEntity controller) {
            controller.markStructureDirty();
        }
        return complete || !placements.isEmpty();
    }

    private static List<ItemStack> survivalCandidates(
            MultiblockDefinition definition,
            char symbol,
            MachineTier selectedTier
    ) {
        MultiblockVisualization.SymbolInfo info = definition.visualization().symbols().get(symbol);
        boolean abilitySymbol = info != null && (!info.requiredAbilities().isEmpty() || !info.anyAbilities().isEmpty());
        if (!abilitySymbol) {
            return definition.visualization().validStacks(symbol, selectedTier, definition.controller(), definition.controllerSymbol());
        }

        Map<Object, ItemStack> unique = new LinkedHashMap<>();
        for (MachineTier candidateTier : MachineTier.ALL) {
            for (ItemStack stack : definition.visualization().validStacks(symbol, candidateTier, definition.controller(), definition.controllerSymbol())) {
                if (!stack.isEmpty()) {
                    unique.putIfAbsent(stack.getItem(), stack);
                }
            }
        }

        MultiblockPredicate predicate = definition.predicate(symbol);
        return unique.values().stream()
                .filter(stack -> {
                    if (!(stack.getItem() instanceof BlockItem blockItem)) {
                        return false;
                    }
                    if (!(blockItem.getBlock() instanceof MachinePortBlock port)) {
                        return true;
                    }
                    return predicate == null || predicate.allowsBuildTier(port.effectiveTier());
                })
                .toList();
    }

    private static BlockState matchingExistingState(BlockState existing, List<ItemStack> candidates, Direction facing) {
        for (ItemStack candidate : candidates) {
            if (!(candidate.getItem() instanceof BlockItem blockItem) || !existing.is(blockItem.getBlock())) {
                continue;
            }
            return orient(existing, facing);
        }
        return null;
    }

    private static InventoryChoice findInventoryChoice(
            ServerPlayer player,
            List<ItemStack> candidates,
            int[] reservedInventory
    ) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack inventoryStack = player.getInventory().getItem(slot);
            if (inventoryStack.isEmpty() || inventoryStack.getCount() <= reservedInventory[slot]) {
                continue;
            }

            for (ItemStack candidate : candidates) {
                if (ItemStack.isSameItemSameComponents(inventoryStack, candidate)) {
                    return new InventoryChoice(slot, candidate);
                }
            }
        }
        return null;
    }

    private static BlockState stateFor(ItemStack stack, Direction facing) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return null;
        }
        return orient(blockItem.getBlock().defaultBlockState(), facing);
    }

    private static BlockState orient(BlockState state, Direction facing) {
        if (state.hasProperty(MultiblockControllerBlock.FACING)) {
            state = state.setValue(MultiblockControllerBlock.FACING, facing);
        }
        if (state.hasProperty(MachinePortBlock.FACING)) {
            state = state.setValue(MachinePortBlock.FACING, facing);
        }
        if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            state = state.setValue(HorizontalDirectionalBlock.FACING, facing);
        }
        return state;
    }

    private static LocalPos controllerLocalPos(MultiblockDefinition definition, PatternVariant variant) {
        for (int x = 0; x < variant.width(); x++) {
            for (int y = 0; y < variant.height(); y++) {
                for (int z = 0; z < variant.length(); z++) {
                    if (variant.symbolAt(x, y, z) == definition.controllerSymbol()) {
                        return new LocalPos(x, y, z);
                    }
                }
            }
        }
        return new LocalPos(0, 0, 0);
    }

    private static BlockPos rotate(BlockPos controllerPos, Direction facing, int localX, int localY, int localZ) {
        Direction right = facing.getClockWise();
        Direction forward = facing.getOpposite();
        return controllerPos.relative(right, localX).above(localY).relative(forward, localZ);
    }

    private record Placement(BlockPos pos, BlockState state) {
    }

    private record InventoryChoice(int slot, ItemStack stack) {
    }

    private record LocalPos(int x, int y, int z) {
    }
}
