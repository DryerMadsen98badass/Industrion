package net.mads.industron.material.recipes;

import net.mads.industron.material.ClayMaterialRules;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.defenitions.IndustrialMaterials;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyRecipeDefinition;
import net.mads.industron.recipe.recipes.assembly.Material;
import net.mads.industron.recipe.recipes.assembly.Tool;
import net.mads.industron.recipe.recipes.assembly.WorkbenchLevels;

import java.util.ArrayList;
import java.util.List;

/** Fired-clay brick construction generated for both generated and .existing clay forms. */
public final class MaterialBrickAssemblyRecipes {
    public static final List<AssemblyRecipeDefinition> ALL = buildAll();

    private MaterialBrickAssemblyRecipes() {
    }

    private static List<AssemblyRecipeDefinition> buildAll() {
        List<AssemblyRecipeDefinition> result = new ArrayList<>();
        for (IndustrialMaterial material : IndustrialMaterials.ALL) {
            if (!material.isClayMaterial() || !material.has(Material.BRICK)) continue;

            addBrickBlock(result, material);
            addBrickSlab(result, material);
            addBrickStairs(result, material);
            addBrickWall(result, material);
        }
        return List.copyOf(result);
    }

    private static void addBrickBlock(List<AssemblyRecipeDefinition> out, IndustrialMaterial material) {
        if (!material.has(Material.BRICKS)) return;
        out.add(recipe(material, "bricks")
                .baseItemInput(Material.BRICK, material)
                .input(Material.BRICK, material, ClayMaterialRules.BRICKS_PER_BLOCK - 1)
                .baseBlockOutput(Material.BRICKS, material)
                .build());
    }

    private static void addBrickSlab(List<AssemblyRecipeDefinition> out, IndustrialMaterial material) {
        if (!material.has(Material.BRICK_SLAB)) return;
        out.add(recipe(material, "brick_slab")
                .baseItemInput(Material.BRICK, material)
                .tool(Tool.SAW, 1)
                .input(Material.BRICK, material, ClayMaterialRules.BRICKS_PER_SLAB - 1)
                .baseBlockOutput(Material.BRICK_SLAB, material)
                .build());
    }

    private static void addBrickStairs(List<AssemblyRecipeDefinition> out, IndustrialMaterial material) {
        if (!material.has(Material.BRICK_STAIRS)) return;
        out.add(recipe(material, "brick_stairs")
                .baseItemInput(Material.BRICK, material)
                .tool(Tool.HAMMER, 1)
                .tool(Tool.SAW, 1)
                .input(Material.BRICK, material, ClayMaterialRules.BRICKS_PER_STAIRS - 1)
                .baseBlockOutput(Material.BRICK_STAIRS, material)
                .build());
    }

    private static void addBrickWall(List<AssemblyRecipeDefinition> out, IndustrialMaterial material) {
        if (!material.has(Material.BRICK_WALL)) return;
        out.add(recipe(material, "brick_wall")
                .baseItemInput(Material.BRICK, material)
                .tool(Tool.FILE, 1)
                .tool(Tool.SAW, 1)
                .input(Material.BRICK, material, ClayMaterialRules.BRICKS_PER_WALL - 1)
                .baseBlockOutput(Material.BRICK_WALL, material)
                .build());
    }

    private static AssemblyRecipeDefinition.Builder recipe(IndustrialMaterial material, String name) {
        return AssemblyRecipeDefinition.recipe("material/recipes/" + material.id() + "_" + name)
                .level(WorkbenchLevels.forTier(material.tier()));
    }
}
