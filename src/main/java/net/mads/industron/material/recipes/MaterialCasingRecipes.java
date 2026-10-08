package net.mads.industron.material.recipes;

import net.mads.industron.machine.MachineTier;
import net.mads.industron.recipe.recipes.assembly.Component;
import net.mads.industron.recipe.recipes.assembly.Material;
import net.mads.industron.recipe.recipes.assembly.Stats;

import java.util.List;

import static net.mads.industron.material.recipes.CasingDefinition.casing;

/** Permanent Phase-04 material-derived casing catalogue. */
public final class MaterialCasingRecipes {
    /**
     * General structural casing available at every electric tier when the material and its
     * generated forms satisfy the projected structural requirement. Specialized thermal,
     * chemical and Foundry casings are added by their owning gameplay phases, not guessed here.
     */
    public static final CasingDefinition MACHINE_CASING = casing("machine_casing")
            .displayName("Machine Casing")
            .texture("industron:block/structure_sets/casing/casings/variant_6")
            .tier(MachineTier.ULV)
            .materialStats(Stats.STRUCTURAL_STRENGTH).atLeast(78)
            .baseBlockInput(Material.FRAME)
            .input(Component.PLATE, 6)
            .build();

    public static final List<CasingDefinition> ALL = List.of(MACHINE_CASING);

    private MaterialCasingRecipes() {
    }
}
