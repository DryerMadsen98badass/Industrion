package net.mads.industron.recipe.recipes.assembly;

import net.mads.industron.recipe.recipetypes.assembly.AssemblyPlan;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyRecipeDefinition;

import java.util.List;
import java.util.Objects;

/**
 * Fail-fast validation for Assembly routes. Shared prefixes are valid; a route only becomes
 * invalid when one complete recipe is a prefix of another, two routes are identical, or the
 * first differing step still cannot be distinguished by the player's interaction.
 */
public final class AssemblyRecipeCollisionValidator {
    private AssemblyRecipeCollisionValidator() {
    }

    public static void validate(List<AssemblyRecipeDefinition> recipes) {
        for (int leftIndex = 0; leftIndex < recipes.size(); leftIndex++) {
            AssemblyRecipeDefinition left = recipes.get(leftIndex);
            for (int rightIndex = leftIndex + 1; rightIndex < recipes.size(); rightIndex++) {
                AssemblyRecipeDefinition right = recipes.get(rightIndex);
                if (!left.baseInput().equals(right.baseInput())) continue;

                List<AssemblyPlan.Step> leftPlan = AssemblyPlan.compile(left);
                List<AssemblyPlan.Step> rightPlan = AssemblyPlan.compile(right);
                if (leftPlan.isEmpty() || rightPlan.isEmpty()) continue;

                int common = commonPrefixLength(leftPlan, rightPlan);
                int shortest = Math.min(leftPlan.size(), rightPlan.size());
                if (common == shortest) {
                    if (leftPlan.size() == rightPlan.size()) {
                        throw new IllegalStateException(
                                "Duplicate Assembly route between '" + left.id() + "' and '" + right.id()
                                        + "': same base and complete interaction sequence."
                        );
                    }
                    throw new IllegalStateException(
                            "Assembly recipe prefix collision between '" + left.id() + "' and '" + right.id()
                                    + "': one recipe completes before the shared route can branch."
                    );
                }

                // A WAIT cannot be chosen by a player interaction. If one branch waits while the
                // other expects an input at the first differing step, there is no deterministic
                // way to decide which branch was intended.
                AssemblyPlan.Step leftBranch = leftPlan.get(common);
                AssemblyPlan.Step rightBranch = rightPlan.get(common);
                if (leftBranch.kind() == AssemblyPlan.Kind.WAIT || rightBranch.kind() == AssemblyPlan.Kind.WAIT) {
                    throw new IllegalStateException(
                            "Assembly automatic-step collision between '" + left.id() + "' and '" + right.id()
                                    + "' at step " + (common + 1) + ". Branch with an explicit interaction before WAIT."
                    );
                }

                if (sameInteraction(leftBranch, rightBranch)) {
                    throw new IllegalStateException(
                            "Ambiguous Assembly branch between '" + left.id() + "' and '" + right.id()
                                    + "' at step " + (common + 1)
                                    + ": the first differing definitions still accept the same interaction."
                    );
                }
            }
        }
    }

    private static int commonPrefixLength(List<AssemblyPlan.Step> left, List<AssemblyPlan.Step> right) {
        int limit = Math.min(left.size(), right.size());
        int index = 0;
        while (index < limit && sameStep(left.get(index), right.get(index))) index++;
        return index;
    }

    private static boolean sameInteraction(AssemblyPlan.Step a, AssemblyPlan.Step b) {
        if (a.kind() != b.kind()) return false;
        return switch (a.kind()) {
            case PLANT_PART -> a.plantPart() == b.plantPart();
            case ITEM -> Objects.equals(a.itemId(), b.itemId());
            case TOOL -> Objects.equals(a.tool(), b.tool())
                    && Objects.equals(a.requirements(), b.requirements());
            case WAIT -> true;
            case MATERIAL -> a.material() == b.material()
                    && Objects.equals(a.fixedMaterial(), b.fixedMaterial())
                    && Objects.equals(a.materialSelector(), b.materialSelector());
        };
    }

    private static boolean sameStep(AssemblyPlan.Step a, AssemblyPlan.Step b) {
        if (a.kind() != b.kind()) return false;
        return switch (a.kind()) {
            case PLANT_PART -> a.plantPart() == b.plantPart()
                    && a.consumeChance() == b.consumeChance();
            case ITEM -> Objects.equals(a.itemId(), b.itemId())
                    && a.consumeChance() == b.consumeChance();
            case TOOL -> Objects.equals(a.tool(), b.tool())
                    && Objects.equals(a.requirements(), b.requirements());
            case WAIT -> a.waitTicks() == b.waitTicks();
            case MATERIAL -> a.material() == b.material()
                    && Objects.equals(a.fixedMaterial(), b.fixedMaterial())
                    && Objects.equals(a.materialCategory(), b.materialCategory())
                    && Objects.equals(a.materialSelector(), b.materialSelector())
                    && Objects.equals(a.requirements(), b.requirements())
                    && Objects.equals(a.capturedRequirements(), b.capturedRequirements())
                    && Objects.equals(a.captureRole(), b.captureRole())
                    && a.consumeChance() == b.consumeChance();
        };
    }
}
