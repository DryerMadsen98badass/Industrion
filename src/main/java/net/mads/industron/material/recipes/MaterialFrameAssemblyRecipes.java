package net.mads.industron.material.recipes;

import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.IndustrialMaterials;
import net.mads.industron.recipe.recipetypes.AssemblyRecipeDefinition;
import net.mads.industron.recipe.recipetypes.Component;
import net.mads.industron.recipe.recipetypes.Material;
import net.mads.industron.recipe.recipes.assembly.ComponentDefinitions;

import java.util.ArrayList;
import java.util.List;

/** Generates one material-bound frame assembly recipe for every compatible material. */
public final class MaterialFrameAssemblyRecipes {
    public static final List<AssemblyRecipeDefinition> ALL = buildAll();

    private MaterialFrameAssemblyRecipes() {
    }

    private static List<AssemblyRecipeDefinition> buildAll() {
        List<AssemblyRecipeDefinition> result = new ArrayList<>();
        for (IndustrialMaterial material : IndustrialMaterials.ALL) {
            if (!qualifies(material)) continue;
            result.add(build(material));
        }
        return List.copyOf(result);
    }

    private static AssemblyRecipeDefinition build(IndustrialMaterial material) {
        return AssemblyRecipeDefinition.recipe("material/recipes/" + material.id() + "_frame")
                .baseItemInput(Material.VERY_LONG_ROD, material)
                .input(Component.VERY_LONG_ROD, material, 11)
                .baseBlockOutput(Material.FRAME, material)
                .build();
    }

    private static boolean qualifies(IndustrialMaterial material) {
        return material.has(Material.FRAME)
                && material.has(Material.VERY_LONG_ROD)
                && ComponentDefinitions.canResolve(Component.VERY_LONG_ROD, material);
    }
}
