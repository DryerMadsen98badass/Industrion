package net.mads.industron.recipe.recipes.chiseling;

import net.mads.industron.machine.foundry.FoundryPartType;

import java.util.List;

/**
 * Private, explicit Chiseling recipes for the four non-material-specific Foundry parts.
 *
 * <p>The output blocks are fixed. The data provider only expands these definitions over the
 * currently registered clay BRICKS inputs so every Foundry clay can make the same abilities.
 * Each route begins on a different grid cell, so the intended part is distinguished immediately.</p>
 */
public final class FoundryAbilityRecipes {
    public record Definition(String id, FoundryPartType output, List<Integer> pattern) {
        public Definition {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("Foundry ability recipe id cannot be blank");
            if (output == null) throw new IllegalArgumentException("Foundry ability output cannot be null");
            if (pattern == null || pattern.size() != 9) {
                throw new IllegalArgumentException("Foundry ability Chiseling pattern must contain exactly 9 hits");
            }
            for (int cell : pattern) {
                if (cell < 1 || cell > 9) {
                    throw new IllegalArgumentException("Foundry ability Chiseling cells must be 1..9");
                }
            }
            pattern = List.copyOf(pattern);
        }
    }

    public static final List<Definition> ALL = List.of(
            new Definition("controller", FoundryPartType.CONTROLLER,
                    List.of(3, 1, 7, 9, 5, 2, 4, 6, 8)),
            new Definition("item_input_bus", FoundryPartType.ITEM_INPUT_BUS,
                    List.of(4, 1, 2, 3, 6, 9, 8, 7, 5)),
            new Definition("fluid_input_hatch", FoundryPartType.FLUID_INPUT_HATCH,
                    List.of(5, 1, 9, 3, 7, 2, 8, 4, 6)),
            new Definition("fluid_output_hatch", FoundryPartType.FLUID_OUTPUT_HATCH,
                    List.of(6, 3, 2, 1, 4, 7, 8, 9, 5))
    );

    private FoundryAbilityRecipes() {
    }
}
