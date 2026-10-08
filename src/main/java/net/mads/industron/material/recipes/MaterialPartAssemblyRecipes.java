package net.mads.industron.material.recipes;

import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.defenitions.IndustrialMaterials;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyComponent;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyRecipeDefinition;
import net.mads.industron.recipe.recipes.assembly.Component;
import net.mads.industron.recipe.recipes.assembly.ComponentDefinitions;
import net.mads.industron.recipe.recipes.assembly.Material;
import net.mads.industron.recipe.recipes.assembly.Tool;
import net.mads.industron.recipe.recipes.assembly.WorkbenchLevels;

import java.util.ArrayList;
import java.util.List;

/**
 * Assembly recipes for built-up mechanical parts. Metal forms with casting molds are
 * shaped through casting/anvil work; their machine-processing routes remain available.
 * Component trees describe mounting and fastening of already-produced parts.
 */
public final class MaterialPartAssemblyRecipes {
    public static final List<AssemblyRecipeDefinition> ALL = buildAll();

    private MaterialPartAssemblyRecipes() {
    }

    private static List<AssemblyRecipeDefinition> buildAll() {
        List<AssemblyRecipeDefinition> result = new ArrayList<>();
        for (IndustrialMaterial material : IndustrialMaterials.ALL) {
            buildRods(result, material);
            buildPlates(result, material);
            buildGears(result, material);
            buildBearings(result, material);
            buildRotors(result, material);
            buildSpecialParts(result, material);
        }
        return List.copyOf(result);
    }

    private static void buildRods(List<AssemblyRecipeDefinition> result, IndustrialMaterial material) {
        // Longer rods are assembled progressively from the previous rod size.
        // The physical base cannot itself be a Component, so its normal component fasteners
        // are mirrored explicitly; the second rod is included through the recursive component.
        if (canBuild(material, Material.ROD, List.of(Material.SHORT_ROD), Component.SHORT_ROD, Component.SMALL_SCREW)) {
            result.add(recipe(material, Material.ROD)
                    .baseItemInput(Material.SHORT_ROD, material)
                    .input(Component.SMALL_SCREW, material, 2)
                    .input(Component.SHORT_ROD, material, 1)
                    // The joined short-rod sections are tightened/aligned with the existing wrench family.
                    .tool(Tool.WRENCH, 2)
                    .baseItemOutput(Material.ROD, material)
                    .build());
        }

        if (canBuild(material, Material.LONG_ROD, List.of(Material.ROD), Component.ROD, Component.SCREW)) {
            result.add(recipe(material, Material.LONG_ROD)
                    .baseItemInput(Material.ROD, material)
                    .input(Component.SCREW, material, 2)
                    .input(Component.ROD, material, 1)
                    // The joined rod sections are tightened/aligned with the existing wrench family.
                    .tool(Tool.WRENCH, 3)
                    .baseItemOutput(Material.LONG_ROD, material)
                    .build());
        }

        if (canBuild(material, Material.VERY_LONG_ROD, List.of(Material.LONG_ROD), Component.LONG_ROD, Component.LARGE_SCREW)) {
            result.add(recipe(material, Material.VERY_LONG_ROD)
                    .baseItemInput(Material.LONG_ROD, material)
                    .input(Component.LARGE_SCREW, material, 2)
                    .input(Component.LONG_ROD, material, 1)
                    // The joined long-rod sections are tightened/aligned with the existing wrench family.
                    .tool(Tool.WRENCH, 4)
                    .baseItemOutput(Material.VERY_LONG_ROD, material)
                    .build());
        }
    }

    private static void buildPlates(List<AssemblyRecipeDefinition> result, IndustrialMaterial material) {
        // Four mounted plates become one large plate. The base plate gets the same four
        // screw branches as Component.PLATE; the other three are normal recursive components.
        if (canBuild(material, Material.LARGE_PLATE, List.of(Material.PLATE), Component.PLATE, Component.SCREW)) {
            result.add(recipe(material, Material.LARGE_PLATE)
                    .baseItemInput(Material.PLATE, material)
                    .input(Component.SCREW, material, 4)
                    .input(Component.PLATE, material, 3)
                                .tool(Tool.CLAMP, 4)
                    .baseItemOutput(Material.LARGE_PLATE, material)
                    .build());
        }

        if (canBuild(material, Material.DOUBLE_PLATE, List.of(Material.PLATE), Component.PLATE, Component.SCREW)) {
            result.add(recipe(material, Material.DOUBLE_PLATE)
                    .baseItemInput(Material.PLATE, material)
                    .input(Component.SCREW, material, 4)
                    .input(Component.PLATE, material, 1)
                                .tool(Tool.CLAMP, 3)
                    .baseItemOutput(Material.DOUBLE_PLATE, material)
                    .build());
        }

        if (canBuild(material, Material.LARGE_DOUBLE_PLATE, List.of(Material.LARGE_PLATE), Component.LARGE_PLATE, Component.LARGE_SCREW)) {
            result.add(recipe(material, Material.LARGE_DOUBLE_PLATE)
                    .baseItemInput(Material.LARGE_PLATE, material)
                    .input(Component.LARGE_SCREW, material, 8)
                    .input(Component.LARGE_PLATE, material, 1)
                                .tool(Tool.CLAMP, 6)
                    .baseItemOutput(Material.LARGE_DOUBLE_PLATE, material)
                    .build());
        }

        if (canBuild(material, Material.DENSE_PLATE, List.of(Material.PLATE), Component.PLATE, Component.SCREW)) {
            result.add(recipe(material, Material.DENSE_PLATE)
                    .baseItemInput(Material.PLATE, material)
                    .input(Component.SCREW, material, 4)
                    .input(Component.PLATE, material, 8)
                                .tool(Tool.CLAMP, 8)
                    .baseItemOutput(Material.DENSE_PLATE, material)
                    .build());
        }

        if (canBuild(material, Material.LARGE_DENSE_PLATE, List.of(Material.DENSE_PLATE), Component.DENSE_PLATE, Component.SCREW)) {
            result.add(recipe(material, Material.LARGE_DENSE_PLATE)
                    .baseItemInput(Material.DENSE_PLATE, material)
                    // Base item is treated like Component.DENSE_PLATE: 12 screws.
                    .input(Component.SCREW, material, 12)
                    .input(Component.DENSE_PLATE, material, 3)
                                .tool(Tool.CLAMP, 12)
                    .baseItemOutput(Material.LARGE_DENSE_PLATE, material)
                    .build());
        }

        if (canBuild(material, Material.REINFORCED_PLATE, List.of(Material.PLATE), Component.PLATE, Component.SCREW)) {
            result.add(recipe(material, Material.REINFORCED_PLATE)
                    .baseItemInput(Material.PLATE, material)
                    .input(Component.SCREW, material, 4)
                    .input(Component.PLATE, material, 1)
                    .input(Component.SCREW, material, 4)
                                .tool(Tool.CLAMP, 4)
                    // Final tightening is a real wrench step and no longer hidden inside the placeholder.
                    .tool(Tool.WRENCH, 1)
                    .baseItemOutput(Material.REINFORCED_PLATE, material)
                    .build());
        }

        if (canBuild(material, Material.HEAT_EXCHANGER_PLATE,
                List.of(Material.PLATE, Material.FINE_WIRE), Component.PLATE, Component.SCREW)) {
            result.add(recipe(material, Material.HEAT_EXCHANGER_PLATE)
                    .baseItemInput(Material.PLATE, material)
                    .input(Component.SCREW, material, 4)
                    .input(Component.PLATE, material, 1)
                    .input(Material.FINE_WIRE, material, 32)
                                .tool(Tool.CRIMPING_TOOL, 8)
                    .input(Component.SCREW, material, 16)
                    // Final panel tightening uses the existing wrench family.
                    .tool(Tool.WRENCH, 1)
                    .baseItemOutput(Material.HEAT_EXCHANGER_PLATE, material)
                    .build());
        }
    }

    private static void buildGears(List<AssemblyRecipeDefinition> result, IndustrialMaterial material) {
        addGear(result, material, Material.TINY_GEAR, Component.VERY_SHORT_ROD, 1, 6);
        addGear(result, material, Material.SMALL_GEAR, Component.SHORT_ROD, 2, 8);
        addGear(result, material, Material.GEAR, Component.ROD, 2, 10);
        addGear(result, material, Material.LARGE_GEAR, Component.LONG_ROD, 4, 14);
        addGear(result, material, Material.HUGE_GEAR, Component.VERY_LONG_ROD, 8, 20);
    }

    private static void addGear(
            List<AssemblyRecipeDefinition> result,
            IndustrialMaterial material,
            MaterialPart output,
            AssemblyComponent rodComponent,
            int plateCount,
            int shapingActions
    ) {
        if (!canBuild(material, output, List.of(Material.PLATE), Component.PLATE, Component.SCREW, rodComponent)) return;

        AssemblyRecipeDefinition.Builder builder = recipe(material, output)
                .baseItemInput(Material.PLATE, material)
                // The non-expanding base plate receives Component.PLATE's four screw branches manually.
                .input(Component.SCREW, material, 4);

        if (plateCount > 1) {
            builder.input(Component.PLATE, material, plateCount - 1);
        }
        builder.input(rodComponent, material, 4);

        result.add(builder
                            .tool(Tool.GEAR_CUTTER, shapingActions)
                // Filing is a separate real finishing step.
                .tool(Tool.FILE, 1)
                .baseItemOutput(output, material)
                .build());
    }

    private static void buildBearings(List<AssemblyRecipeDefinition> result, IndustrialMaterial material) {
        addBearing(result, material, Material.TINY_BEARING, Material.TINY_RING, Material.TINY_BALL, Component.TINY_RING, 1);
        addBearing(result, material, Material.SMALL_BEARING, Material.SMALL_RING, Material.SMALL_BALL, Component.SMALL_RING, 1);
        addBearing(result, material, Material.BEARING, Material.RING, Material.BALL, Component.RING, 2);
        addBearing(result, material, Material.LARGE_BEARING, Material.LARGE_RING, Material.LARGE_BALL, Component.LARGE_RING, 2);
        addBearing(result, material, Material.HUGE_BEARING, Material.HUGE_RING, Material.HUGE_BALL, Component.HUGE_RING, 3);
    }

    private static void addBearing(
            List<AssemblyRecipeDefinition> result,
            IndustrialMaterial material,
            MaterialPart output,
            MaterialPart ring,
            MaterialPart ball,
            AssemblyComponent ringComponent,
            int pressActions
    ) {
        if (!canBuild(material, output, List.of(ring, ball), ringComponent)) return;

        result.add(recipe(material, output)
                .baseItemInput(ring, material)
                // Base ring mirrors Component.RING's mounting action without recursively consuming another ring.
                            .tool(Tool.SNAP_RING_PLIERS, 1)
                .input(ringComponent, material, 1)
                .input(ball, material, 8)
                            .tool(Tool.BEARING_PRESS, pressActions)
                .baseItemOutput(output, material)
                .build());
    }

    private static void buildRotors(List<AssemblyRecipeDefinition> result, IndustrialMaterial material) {
        addRotor(result, material, Material.TINY_ROTOR, Material.VERY_SHORT_ROD, Component.TINY_SCREW, Component.TINY_GEAR, Component.TINY_RING, 2);
        addRotor(result, material, Material.SMALL_ROTOR, Material.SHORT_ROD, Component.SMALL_SCREW, Component.SMALL_GEAR, Component.SMALL_RING, 2);
        addRotor(result, material, Material.ROTOR, Material.ROD, Component.SCREW, Component.GEAR, Component.RING, 3);
        addRotor(result, material, Material.LARGE_ROTOR, Material.LONG_ROD, Component.LARGE_SCREW, Component.LARGE_GEAR, Component.LARGE_RING, 4);
        addRotor(result, material, Material.HUGE_ROTOR, Material.VERY_LONG_ROD, Component.HUGE_SCREW, Component.HUGE_GEAR, Component.HUGE_RING, 6);
    }

    private static void addRotor(
            List<AssemblyRecipeDefinition> result,
            IndustrialMaterial material,
            MaterialPart output,
            MaterialPart rod,
            AssemblyComponent rodScrew,
            AssemblyComponent gear,
            AssemblyComponent ring,
            int assemblyActions
    ) {
        if (!canBuild(material, output, List.of(rod), rodScrew, gear, ring)) return;

        result.add(recipe(material, output)
                .baseItemInput(rod, material)
                // Every rod has one screw at each end; the screwdriver actions are inside the screw components.
                .input(rodScrew, material, 2)
                .input(gear, material, 1)
                .input(ring, material, 1)
                // Final rotor alignment/fastening uses the existing wrench family.
                .tool(Tool.WRENCH, assemblyActions)
                .baseItemOutput(output, material)
                .build());
    }

    private static void buildSpecialParts(List<AssemblyRecipeDefinition> result, IndustrialMaterial material) {
        if (canBuild(material, Material.TURBINE_BLADE,
                List.of(Material.ROD), Component.SCREW, Component.LONG_ROD, Component.DENSE_PLATE)) {
            result.add(recipe(material, Material.TURBINE_BLADE)
                    .baseItemInput(Material.ROD, material)
                    // Base rod mirrors Component.ROD: one screw at each end.
                    .input(Component.SCREW, material, 2)
                    .input(Component.LONG_ROD, material, 2)
                    .input(Component.DENSE_PLATE, material, 2)
                                .tool(Tool.SHEARS, 12)
                    // Final edge/profile finishing uses the real file family.
                    .tool(Tool.FILE, 1)
                    .baseItemOutput(Material.TURBINE_BLADE, material)
                    .build());
        }
    }

    private static AssemblyRecipeDefinition.Builder recipe(IndustrialMaterial material, MaterialPart output) {
        return AssemblyRecipeDefinition.recipe("material/recipes/" + material.id() + "_" + output.id())
                .level(WorkbenchLevels.forTier(material.tier()));
    }

    private static boolean canBuild(
            IndustrialMaterial material,
            MaterialPart output,
            List<MaterialPart> directInputs,
            AssemblyComponent... components
    ) {
        if (net.mads.industron.machine.foundry.casting.CastingDefinitions.cold(output) != null) return false;
        if (!material.has(output) || material.hasExistingRecipe(output)) return false;
        for (MaterialPart part : directInputs) {
            if (!material.has(part)) return false;
        }
        for (AssemblyComponent component : components) {
            if (!ComponentDefinitions.canResolve(component, material)) return false;
        }
        return true;
    }
}
