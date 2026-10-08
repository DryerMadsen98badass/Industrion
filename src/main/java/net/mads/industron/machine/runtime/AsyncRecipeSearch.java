package net.mads.industron.machine.runtime;

import net.mads.industron.recipe.*;
import net.mads.industron.runtime.IndustronWorkers;
import net.mads.industron.runtime.BoundedIdentityCache;
import net.mads.industron.runtime.RecipeMatchSnapshot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Server-thread adapter. Component predicates run here; workers perform quantity matching only. */
public final class AsyncRecipeSearch {
    private static final int ASYNC_THRESHOLD = 32;
    private CompletableFuture<RecipeMatchSnapshot.Result> pending;
    private CERecipeInput previous;
    private RecipeManager manager;
    private long epoch, revision, hostRevision, invalidation, submittedInvalidation;
    public void invalidate() { invalidation++; }
    private static long ingredientEpoch = Long.MIN_VALUE;
    private static final BoundedIdentityCache<net.minecraft.world.item.crafting.Ingredient, Set<Integer>> ITEM_KINDS = new BoundedIdentityCache<>(4096);
    private static final BoundedIdentityCache<net.neoforged.neoforge.fluids.crafting.FluidIngredient, Set<Integer>> FLUID_KINDS = new BoundedIdentityCache<>(4096);
    private List<RecipeHolder<CERecipe>> submitted = List.of();

    public static void clearCaches() { ITEM_KINDS.clear(); FLUID_KINDS.clear(); }

    public void discardFinished() {
        if (pending != null && pending.isDone()) {
            IndustronWorkers.discard(); pending = null; previous = null; submitted = List.of();
        }
    }
    public boolean pending() { return pending != null; }

    public List<RecipeHolder<CERecipe>> candidates(RecipeManager currentManager,
            List<RecipeHolder<CERecipe>> candidates, CERecipeInput input, long currentHostRevision) {
        long currentEpoch = CERecipeLookup.revision();
        if (pending != null) {
            boolean valid = manager == currentManager && epoch == currentEpoch && hostRevision == currentHostRevision && submittedInvalidation == invalidation && same(previous, input)
                    && submitted.equals(candidates);
            if (!pending.isDone()) return List.of(); // One job per host, including obsolete jobs.
            boolean failed = pending.isCompletedExceptionally() || pending.isCancelled();
            List<String> ids = List.of();
            if (valid && !pending.isCompletedExceptionally() && !pending.isCancelled()) {
                RecipeMatchSnapshot.Result result = pending.join();
                if (result.revision() == revision) ids = result.ids();
            } else IndustronWorkers.discard();
            pending = null;
            previous = null;
            submitted = List.of();
            if (valid) {
                if (failed) return candidates;
                Set<String> accepted = Set.copyOf(ids);
                return candidates.stream().filter(holder -> accepted.contains(holder.id().toString())).toList();
            }
        }
        if (candidates.size() < ASYNC_THRESHOLD || !IndustronWorkers.available()) return candidates;
        if (ingredientEpoch != currentEpoch) { ITEM_KINDS.clear(); FLUID_KINDS.clear(); ingredientEpoch = currentEpoch; }
        // Predicates may inspect registry tags/data components. They MUST remain on this thread.
        // No ItemStack, FluidStack, Ingredient, Level or machine is captured by the supplier.
        List<RecipeMatchSnapshot.Candidate> snapshots = new ArrayList<>();
        for (var holder : candidates) {
            CERecipe recipe = holder.value();
            List<RecipeMatchSnapshot.Requirement> items = new ArrayList<>(), fluids = new ArrayList<>();
            recipe.itemInputs().forEach(r -> items.add(itemRequirement(r.ingredient(), r.count(), input.items())));
            recipe.chancedItemInputs().forEach(r -> items.add(itemRequirement(r.ingredient().ingredient(), r.ingredient().count(), input.items())));
            recipe.notConsumableItems().forEach(r -> items.add(itemRequirement(r.ingredient(), r.count(), input.items())));
            recipe.fluidInputs().forEach(r -> fluids.add(fluidRequirement(r.ingredient(), r.amount(), input.fluids())));
            recipe.chancedFluidInputs().forEach(r -> fluids.add(fluidRequirement(r.ingredient().ingredient(), r.ingredient().amount(), input.fluids())));
            recipe.notConsumableFluids().forEach(r -> fluids.add(fluidRequirement(r.ingredient(), r.amount(), input.fluids())));
            if (items.stream().anyMatch(requirement -> !requirement.byKind())) items.clear();
            if (fluids.stream().anyMatch(requirement -> !requirement.byKind())) fluids.clear();
            snapshots.add(new RecipeMatchSnapshot.Candidate(holder.id().toString(), items, fluids));
        }
        RecipeMatchSnapshot snapshot = new RecipeMatchSnapshot(++revision,
                input.items().stream().map(ItemStack::getCount).toList(),
                input.fluids().stream().map(FluidStack::getAmount).toList(),
                input.items().stream().map(s -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getId(s.getItem())).toList(),
                input.fluids().stream().map(s -> net.minecraft.core.registries.BuiltInRegistries.FLUID.getId(s.getFluid())).toList(), snapshots);
        pending = IndustronWorkers.submit("recipe-match", snapshot::match);
        if (pending == null) return candidates; // Bounded queue fallback; never delays gameplay indefinitely.
        previous = new CERecipeInput(input.items().stream().map(ItemStack::copy).toList(),
                input.fluids().stream().map(FluidStack::copy).toList(), input.circuit(), input.availableLogic(),
                input.machineTier(), input.kineticTier(), input.energyTier(), input.drive(), input.rpm(), input.coilHeat());
        manager = currentManager; epoch = currentEpoch; hostRevision = currentHostRevision; submittedInvalidation = invalidation; submitted = List.copyOf(candidates);
        return List.of();
    }
    private static RecipeMatchSnapshot.Requirement itemRequirement(net.minecraft.world.item.crafting.Ingredient ingredient,
            int amount, List<ItemStack> stacks) {
        if (ingredient.getCustomIngredient() == null || ingredient.getCustomIngredient().isSimple()) {
            Set<Integer> kinds = ITEM_KINDS.computeIfAbsent(ingredient, key -> {
                Set<Integer> accepted = new HashSet<>();
                for (ItemStack stack : key.getItems()) if (!stack.isEmpty()) accepted.add(net.minecraft.core.registries.BuiltInRegistries.ITEM.getId(stack.getItem()));
                return Set.copyOf(accepted);
            });
            return new RecipeMatchSnapshot.Requirement(amount, kinds, true);
        }
        return new RecipeMatchSnapshot.Requirement(amount, Set.of(), false);
    }
    private static RecipeMatchSnapshot.Requirement fluidRequirement(net.neoforged.neoforge.fluids.crafting.FluidIngredient ingredient,
            int amount, List<FluidStack> stacks) {
        if (ingredient.isSimple()) {
            Set<Integer> kinds = FLUID_KINDS.computeIfAbsent(ingredient, key -> {
                Set<Integer> accepted = new HashSet<>();
                for (FluidStack stack : key.getStacks()) if (!stack.isEmpty()) accepted.add(net.minecraft.core.registries.BuiltInRegistries.FLUID.getId(stack.getFluid()));
                return Set.copyOf(accepted);
            });
            return new RecipeMatchSnapshot.Requirement(amount, kinds, true);
        }
        return new RecipeMatchSnapshot.Requirement(amount, Set.of(), false);
    }
    private static boolean same(CERecipeInput a, CERecipeInput b) {
        if (a == null || !a.circuit().equals(b.circuit()) || !a.availableLogic().equals(b.availableLogic())
                || !a.machineTier().equals(b.machineTier()) || !a.kineticTier().equals(b.kineticTier())
                || !a.energyTier().equals(b.energyTier()) || a.drive() != b.drive() || a.rpm() != b.rpm()
                || a.coilHeat() != b.coilHeat() || a.items().size() != b.items().size() || a.fluids().size() != b.fluids().size()) return false;
        for (int i = 0; i < a.items().size(); i++) if (a.items().get(i).getCount() != b.items().get(i).getCount()
                || !ItemStack.isSameItemSameComponents(a.items().get(i), b.items().get(i))) return false;
        for (int i = 0; i < a.fluids().size(); i++) if (a.fluids().get(i).getAmount() != b.fluids().get(i).getAmount()
                || !FluidStack.isSameFluidSameComponents(a.fluids().get(i), b.fluids().get(i))) return false;
        return true;
    }
}
