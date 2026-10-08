package net.mads.industron.material.chemistry;

import java.util.List;

/** Phase 13 reaction-level plan, before Phase 14 maps it to a RecipeType. */
public record ReactionPlan(
        String id,
        List<ReactionParticipant> reactants,
        List<ReactionParticipant> products,
        List<String> bondChanges,
        int electronTransfer,
        int requiredTemperature,
        double minChemicalBalance,
        double maxChemicalBalance,
        String catalystFamily,
        String explanation
) {
    public ReactionPlan {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("reaction id cannot be blank");
        id = id.trim().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_/.-]+", "_");
        reactants = reactants == null ? List.of() : List.copyOf(reactants);
        products = products == null ? List.of() : List.copyOf(products);
        bondChanges = bondChanges == null ? List.of() : List.copyOf(bondChanges);
        requiredTemperature = Math.max(0, requiredTemperature);
        if (minChemicalBalance > maxChemicalBalance) {
            double swap = minChemicalBalance;
            minChemicalBalance = maxChemicalBalance;
            maxChemicalBalance = swap;
        }
        catalystFamily = catalystFamily == null ? "" : catalystFamily.trim().toLowerCase(java.util.Locale.ROOT);
        explanation = explanation == null ? "" : explanation;
        if (reactants.isEmpty() || products.isEmpty()) throw new IllegalArgumentException("reaction needs reactants and products");
    }
}
