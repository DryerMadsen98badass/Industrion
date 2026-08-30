package net.mads.industron.recipe.recipes.assembly;

import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.defenitions.IndustrialMaterials;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyComponent;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyPlan;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyTools;
import net.mads.industron.recipe.recipes.assembly.Component;
import net.mads.industron.recipe.recipetypes.assembly.ComponentDefinition;
import net.mads.industron.recipe.recipes.assembly.Material;
import net.mads.industron.recipe.recipes.assembly.Tool;

import java.util.List;

import static net.mads.industron.recipe.recipetypes.assembly.ComponentDefinition.component;

/** Explicit semantic component trees. Recipe-level stats are supplied by the recipe that uses them. */
public final class ComponentDefinitions {
    public static final ComponentDefinition SCREW = component(Component.SCREW)
            .input(Material.SCREW)
            .input(Material.RING)
            .input(Tool.PICKAXE)
            .build();

    public static final ComponentDefinition PLATE = component(Component.PLATE)
            .input(Material.PLATE)
            .input(Component.SCREW, 4)
            .build();

    public static final ComponentDefinition VERY_LONG_ROD = component(Component.VERY_LONG_ROD)
            .input(Material.VERY_LONG_ROD)
            .input(Component.SCREW, 8)
            .build();

    public static final List<ComponentDefinition> ALL = List.of(
            SCREW,
            PLATE,
            VERY_LONG_ROD
    );

    private ComponentDefinitions() {
    }

    public static ComponentDefinition find(AssemblyComponent component) {
        return ALL.stream()
                .filter(definition -> definition.component().equals(component))
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns false when a fixed material-bound component tree cannot be completed.
     * inputAny branches are allowed to substitute any material that satisfies their
     * propagated absolute/relative stat requirements.
     *
     * <p>Structural definition errors (missing definitions/cycles/invalid relative
     * parent use) remain hard errors and are intentionally not converted to false.</p>
     */
    public static boolean canResolve(AssemblyComponent component, IndustrialMaterial material) {
        return canResolve(component, material, List.of());
    }

    public static boolean canResolve(
            AssemblyComponent component,
            IndustrialMaterial material,
            List<net.mads.industron.recipe.recipetypes.assembly.AssemblyRequirement> requirements
    ) {
        List<AssemblyPlan.Step> plan = AssemblyPlan.compileComponent(
                java.util.Objects.requireNonNull(component, "component"),
                java.util.Objects.requireNonNull(material, "material"),
                List.copyOf(requirements)
        );
        return planCanResolve(plan);
    }

    public static boolean canResolveFree(
            AssemblyComponent component,
            List<net.mads.industron.recipe.recipetypes.assembly.AssemblyRequirement> requirements
    ) {
        return planCanResolve(AssemblyPlan.compileComponent(
                java.util.Objects.requireNonNull(component, "component"),
                List.copyOf(requirements)
        ));
    }

    private static boolean planCanResolve(List<AssemblyPlan.Step> plan) {
        for (AssemblyPlan.Step step : plan) {
            switch (step.kind()) {
                case MATERIAL -> {
                    if (step.fixedMaterial() != null) {
                        if (!matches(step.fixedMaterial(), step)) return false;
                    } else if (IndustrialMaterials.ALL.stream().noneMatch(candidate -> matches(candidate, step))) {
                        return false;
                    }
                }
                case TOOL -> {
                    if (AssemblyTools.all().stream().noneMatch(tool -> tool.type().equals(step.tool()))) {
                        return false;
                    }
                }
                case ITEM, WAIT -> {
                    // Exact item ids are registry-backed; waits have no material dependency.
                }
            }
        }
        return true;
    }

    private static boolean matches(IndustrialMaterial material, AssemblyPlan.Step step) {
        if (!material.has(step.material())) return false;
        return step.requirements().stream().allMatch(requirement -> requirement.matches(material, step.material()));
    }
}
