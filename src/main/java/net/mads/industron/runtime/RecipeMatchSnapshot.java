package net.mads.industron.runtime;

import java.util.ArrayList;
import java.util.List;

/** Plain immutable numbers/strings only. Requirements retain CERecipe's greedy slot order. */
public record RecipeMatchSnapshot(long revision, List<Integer> items, List<Integer> fluids, List<Integer> itemKinds, List<Integer> fluidKinds, List<Candidate> candidates) {
    public RecipeMatchSnapshot { items = List.copyOf(items); fluids = List.copyOf(fluids); candidates = List.copyOf(candidates); itemKinds = List.copyOf(itemKinds); fluidKinds = List.copyOf(fluidKinds); }
    public record Requirement(int amount, java.util.Set<Integer> selectors, boolean byKind) {
        public Requirement { selectors = java.util.Set.copyOf(selectors); }
    }
    public record Candidate(String id, List<Requirement> items, List<Requirement> fluids) {
        public Candidate { items = List.copyOf(items); fluids = List.copyOf(fluids); }
    }
    public record Result(long revision, List<String> ids) { public Result { ids = List.copyOf(ids); } }
    public Result match() {
        List<String> ids = new ArrayList<>();
        for (Candidate candidate : candidates) {
            if (Thread.currentThread().isInterrupted()) break;
            if (matches(items, itemKinds, candidate.items) && matches(fluids, fluidKinds, candidate.fluids)) ids.add(candidate.id);
        }
        return new Result(revision, ids);
    }
    private static boolean matches(List<Integer> amounts, List<Integer> kinds, List<Requirement> requirements) {
        int[] remaining = amounts.stream().mapToInt(Integer::intValue).toArray();
        for (Requirement requirement : requirements) {
            int needed = requirement.amount;
            for (int slot = 0; slot < remaining.length; slot++) {
                if (!requirement.selectors.contains(requirement.byKind ? kinds.get(slot) : slot)) continue;
                int taken = Math.min(needed, remaining[slot]);
                remaining[slot] -= taken;
                needed -= taken;
                if (needed == 0) break;
            }
            if (needed > 0) return false;
        }
        return true;
    }
}
