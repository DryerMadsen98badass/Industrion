package net.mads.industron.data;

import net.mads.industron.material.recipes.MaterialRecipeHelper;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyPlan;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyRecipeDefinition;
import net.mads.industron.recipe.recipes.assembly.AssemblyRecipes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Resolves deterministic ~50% block salvage from the complete Assembly component tree. */
final class AssemblySalvageResolver {
    private AssemblySalvageResolver() {
    }

    static Map<ResourceLocation, Integer> salvageForBlock(ResourceLocation blockId) {
        List<AssemblyRecipeDefinition> recipes = AssemblyRecipes.ALL.stream()
                .filter(AssemblyRecipeDefinition::hasBlockBaseOutput)
                .filter(recipe -> blockId.equals(recipe.baseOutput().id()))
                .sorted(Comparator.comparing(AssemblyRecipeDefinition::id))
                .toList();

        if (recipes.isEmpty()) {
            return Map.of();
        }

        Map<ResourceLocation, Integer> recoverable = recoverableInputs(recipes.getFirst());
        for (int i = 1; i < recipes.size(); i++) {
            Map<ResourceLocation, Integer> alternate = recoverableInputs(recipes.get(i));
            if (!recoverable.equals(alternate)) {
                throw new IllegalStateException(
                        "Assembly-salvage block " + blockId
                                + " has multiple Assembly recipes with different recoverable inputs: "
                                + recipes.getFirst().id() + " and " + recipes.get(i).id()
                                + ". The broken block does not store which recipe built it."
                );
            }
        }

        return halfDeterministically(recoverable);
    }

    /**
     * Flattens the whole component tree and keeps only inputs whose concrete identity is known.
     *
     * <p>Ignored on purpose:
     * <ul>
     *     <li>tools and wait steps (not consumed material),</li>
     *     <li>ANY/category material leaves (the broken block does not remember what was used),</li>
     *     <li>ANY/category material-selected base blocks/items for the same reason.</li>
     * </ul>
     * Exact item ids and fixed material leaves are recoverable.</p>
     */
    static Map<ResourceLocation, Integer> recoverableInputs(AssemblyRecipeDefinition recipe) {
        Map<ResourceLocation, Integer> result = new LinkedHashMap<>();

        addBaseInput(result, recipe.baseInput());

        for (AssemblyPlan.Step step : AssemblyPlan.compile(recipe)) {
            switch (step.kind()) {
                case MATERIAL -> {
                    if (step.fixedMaterial() != null) {
                        add(result, ResourceLocation.parse(
                                MaterialRecipeHelper.itemId(step.fixedMaterial(), step.material())
                        ), 1);
                    }
                    // Dynamic ANY/category material bindings are deliberately not recoverable.
                }
                case ITEM -> add(result, step.itemId(), 1);
                case PLANT_PART -> {
                    // A role such as PlantPart.STRING can match several concrete items, and the
                    // placed block does not remember which one was consumed. Do not guess salvage.
                }
                case TOOL, WAIT -> {
                    // Work/time is not physical salvage.
                }
            }
        }

        return Map.copyOf(result);
    }

    private static void addBaseInput(
            Map<ResourceLocation, Integer> result,
            AssemblyRecipeDefinition.BaseValue base
    ) {
        if (base.isMaterialSelection()) {
            if (base.materialSelector().isFixed()) {
                add(result, ResourceLocation.parse(
                        MaterialRecipeHelper.itemId(base.materialSelector().resolve(), base.material())
                ), 1);
            }
            return;
        }
        if (base.isPlantPartSelection()) {
            // A plant-role base can be any matching physical item; the finished block does not
            // remember which concrete item was used, so deterministic salvage must not guess.
            return;
        }

        if (base.kind() == AssemblyRecipeDefinition.BaseKind.ITEM) {
            add(result, base.id(), 1);
            return;
        }
        // Entity bases are live world objects, not an item unit that can be deterministically
        // recovered by block-salvage generation.
        if (base.kind() == AssemblyRecipeDefinition.BaseKind.ENTITY) {
            return;
        }

        Block block = BuiltInRegistries.BLOCK.getOptional(base.id()).orElse(null);
        if (block == null) {
            throw new IllegalStateException("Unknown Assembly base block while generating salvage: " + base.id());
        }
        Item item = block.asItem();
        if (item == Items.AIR) {
            return;
        }
        add(result, BuiltInRegistries.ITEM.getKey(item), 1);
    }

    /**
     * Produces an exact, non-random salvage bill at or just below 50% of recoverable item units.
     * Odd remainders are distributed by stable item id so runData is deterministic.
     */
    private static Map<ResourceLocation, Integer> halfDeterministically(Map<ResourceLocation, Integer> full) {
        int total = full.values().stream().mapToInt(Integer::intValue).sum();
        int target = total / 2;
        if (target <= 0) {
            return Map.of();
        }

        List<Map.Entry<ResourceLocation, Integer>> entries = new ArrayList<>(full.entrySet());
        entries.sort(Comparator.comparing(entry -> entry.getKey().toString()));

        Map<ResourceLocation, Integer> result = new LinkedHashMap<>();
        int selected = 0;
        for (Map.Entry<ResourceLocation, Integer> entry : entries) {
            int half = entry.getValue() / 2;
            if (half > 0) {
                result.put(entry.getKey(), half);
                selected += half;
            }
        }

        int remaining = target - selected;
        if (remaining > 0) {
            for (Map.Entry<ResourceLocation, Integer> entry : entries) {
                if (remaining == 0) break;
                if ((entry.getValue() & 1) == 0) continue;
                result.merge(entry.getKey(), 1, Integer::sum);
                remaining--;
            }
        }

        return Map.copyOf(result);
    }

    private static void add(Map<ResourceLocation, Integer> result, ResourceLocation item, int count) {
        if (item == null || count <= 0) return;
        result.merge(item, count, Integer::sum);
    }
}
