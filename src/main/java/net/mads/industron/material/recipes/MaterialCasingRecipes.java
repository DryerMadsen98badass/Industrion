package net.mads.industron.material.recipes;

import net.mads.industron.machine.MachineTier;
import net.mads.industron.recipe.recipes.assembly.Component;
import net.mads.industron.recipe.recipes.assembly.Material;
import net.mads.industron.recipe.recipes.assembly.Stats;

import java.util.List;

import static net.mads.industron.material.recipes.CasingDefinition.casing;

/** Test casing family for the material-derived casing generator. */
public final class MaterialCasingRecipes {
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
