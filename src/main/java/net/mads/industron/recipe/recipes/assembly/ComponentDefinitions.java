package net.mads.industron.recipe.recipes.assembly;

import net.mads.industron.material.recipes.WoodAssemblyComponents;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.material.MaterialCatalog;
import net.mads.industron.material.plant.PlantPart;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyComponent;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyMaterialCatalog;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyPlan;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyTools;
import net.mads.industron.recipe.recipetypes.assembly.ComponentDefinition;

import java.util.List;

import static net.mads.industron.recipe.recipetypes.assembly.ComponentDefinition.component;

/** Explicit semantic component trees. Recipe-level stats are supplied by the recipe that uses them. */
public final class ComponentDefinitions {
    public static final ComponentDefinition STICK = component(Component.STICK)
            .input(Material.STICK)
            .input(PlantPart.STRING)
            .build();

    public static final ComponentDefinition BAMBOO = component(Component.BAMBOO)
            .input("minecraft:bamboo")
            .input(PlantPart.STRING, 2)
            .build();

    /** One wooden peg installed with exactly one Mallet use. */
    public static final ComponentDefinition WOOD_PEG = component(Component.WOOD_PEG)
            .input(net.mads.industron.material.MaterialPart.WOOD_PEG)
            .tool(Tool.MALLET, 1)
            .build();

    /** One wooden plate mounted with four fully-installed peg components. */
    public static final ComponentDefinition WOOD_PLATE = component(Component.WOOD_PLATE)
            .input(net.mads.industron.material.MaterialPart.PLATE)
            .input(Component.WOOD_PEG, 4)
            .build();

    /** One wooden shaft mounted with four fully-installed peg components. */
    public static final ComponentDefinition WOOD_SHAFT = component(Component.WOOD_SHAFT)
            .input(net.mads.industron.material.MaterialPart.SHAFT)
            .input(Component.WOOD_PEG, 4)
            .build();

    /** One small wooden gear mounted with four fully-installed peg components. */
    public static final ComponentDefinition SMALL_WOOD_GEAR = component(Component.SMALL_WOOD_GEAR)
            .input(net.mads.industron.material.MaterialPart.SMALL_GEAR)
            .input(Component.WOOD_PEG, 4)
            .build();

    /** One wooden gear mounted with four fully-installed peg components. */
    public static final ComponentDefinition WOOD_GEAR = component(Component.WOOD_GEAR)
            .input(net.mads.industron.material.MaterialPart.GEAR)
            .input(Component.WOOD_PEG, 4)
            .build();

    // Screws are terminal component branches: they consume a physical ring, never Component.RING.
    // That is intentional and keeps RING <-> SCREW from becoming a recursive cycle.
    public static final ComponentDefinition TINY_SCREW = component(Component.TINY_SCREW)
            .input(Material.TINY_SCREW)
            .input(Material.TINY_RING)
            // Screw installation uses the finished dynamic screwdriver family.
            .tool(Tool.SCREWDRIVER, 1)
            .build();

    public static final ComponentDefinition SMALL_SCREW = component(Component.SMALL_SCREW)
            .input(Material.SMALL_SCREW)
            .input(Material.SMALL_RING)
            // Screw installation uses the finished dynamic screwdriver family.
            .tool(Tool.SCREWDRIVER, 1)
            .build();

    public static final ComponentDefinition SCREW = component(Component.SCREW)
            .input(Material.SCREW)
            .input(Material.RING)
            // Screw installation uses the finished dynamic screwdriver family.
            .tool(Tool.SCREWDRIVER, 1)
            .build();

    public static final ComponentDefinition LARGE_SCREW = component(Component.LARGE_SCREW)
            .input(Material.LARGE_SCREW)
            .input(Material.LARGE_RING)
            // Screw installation uses the finished dynamic screwdriver family.
            .tool(Tool.SCREWDRIVER, 1)
            .build();

    public static final ComponentDefinition HUGE_SCREW = component(Component.HUGE_SCREW)
            .input(Material.HUGE_SCREW)
            .input(Material.HUGE_RING)
            // Screw installation uses the finished dynamic screwdriver family.
            .tool(Tool.SCREWDRIVER, 1)
            .build();

    public static final ComponentDefinition TINY_RING = component(Component.TINY_RING)
            .input(Material.TINY_RING)
                        // Never add Component.SCREW here: screws already consume rings.
            .tool(Tool.SNAP_RING_PLIERS, 1)
            .build();

    public static final ComponentDefinition SMALL_RING = component(Component.SMALL_RING)
            .input(Material.SMALL_RING)
                        // Never add Component.SCREW here: screws already consume rings.
            .tool(Tool.SNAP_RING_PLIERS, 1)
            .build();

    public static final ComponentDefinition RING = component(Component.RING)
            .input(Material.RING)
                        // Never add Component.SCREW here: screws already consume rings.
            .tool(Tool.SNAP_RING_PLIERS, 1)
            .build();

    public static final ComponentDefinition LARGE_RING = component(Component.LARGE_RING)
            .input(Material.LARGE_RING)
                        // Never add Component.SCREW here: screws already consume rings.
            .tool(Tool.SNAP_RING_PLIERS, 1)
            .build();

    public static final ComponentDefinition HUGE_RING = component(Component.HUGE_RING)
            .input(Material.HUGE_RING)
                        // Never add Component.SCREW here: screws already consume rings.
            .tool(Tool.SNAP_RING_PLIERS, 1)
            .build();

    // Rods carry one screw at each end. Mounting fasteners are inputAny on purpose:
    // a fixed parent material (for example Madsium) must not force the screw material.
    // Free fasteners are also intentionally excluded from Assembly salvage.
    public static final ComponentDefinition VERY_SHORT_ROD = component(Component.VERY_SHORT_ROD)
            .input(Material.VERY_SHORT_ROD)
            .inputAny(Component.TINY_SCREW, 2)
            .build();

    public static final ComponentDefinition SHORT_ROD = component(Component.SHORT_ROD)
            .input(Material.SHORT_ROD)
            .inputAny(Component.SMALL_SCREW, 2)
            .build();

    public static final ComponentDefinition ROD = component(Component.ROD)
            .input(Material.ROD)
            .inputAny(Component.SCREW, 2)
            .build();

    public static final ComponentDefinition LONG_ROD = component(Component.LONG_ROD)
            .input(Material.LONG_ROD)
            .inputAny(Component.LARGE_SCREW, 2)
            .build();

    public static final ComponentDefinition VERY_LONG_ROD = component(Component.VERY_LONG_ROD)
            .input(Material.VERY_LONG_ROD)
            .inputAny(Component.HUGE_SCREW, 2)
            .build();

    // Plates are mounting components: every plate brings its own fasteners when used in a larger assembly.
    public static final ComponentDefinition PLATE = component(Component.PLATE)
            .input(Material.PLATE)
            .inputAny(Component.SCREW, 4)
            .build();

    public static final ComponentDefinition LARGE_PLATE = component(Component.LARGE_PLATE)
            .input(Material.LARGE_PLATE)
            .inputAny(Component.LARGE_SCREW, 8)
            .build();

    public static final ComponentDefinition DOUBLE_PLATE = component(Component.DOUBLE_PLATE)
            .input(Material.DOUBLE_PLATE)
            .inputAny(Component.SCREW, 8)
            .build();

    public static final ComponentDefinition LARGE_DOUBLE_PLATE = component(Component.LARGE_DOUBLE_PLATE)
            .input(Material.LARGE_DOUBLE_PLATE)
            .inputAny(Component.LARGE_SCREW, 16)
            .build();

    public static final ComponentDefinition DENSE_PLATE = component(Component.DENSE_PLATE)
            .input(Material.DENSE_PLATE)
            .inputAny(Component.SCREW, 12)
            .build();

    public static final ComponentDefinition LARGE_DENSE_PLATE = component(Component.LARGE_DENSE_PLATE)
            .input(Material.LARGE_DENSE_PLATE)
            .inputAny(Component.LARGE_SCREW, 24)
            .build();

    /** General reinforced panel mounted as an assembly component. */
    public static final ComponentDefinition REINFORCED_PLATE = component(Component.REINFORCED_PLATE)
            .input(Material.REINFORCED_PLATE)
            .inputAny(Component.SCREW, 8)
            .build();

    /** General heat-exchanger panel mounted as an assembly component. */
    public static final ComponentDefinition HEAT_EXCHANGER_PLATE = component(Component.HEAT_EXCHANGER_PLATE)
            .input(Material.HEAT_EXCHANGER_PLATE)
            .inputAny(Component.SCREW, 8)
            .build();

    // A gear mounted in a larger assembly carries its shaft and retaining rings.
    public static final ComponentDefinition TINY_GEAR = component(Component.TINY_GEAR)
            .input(Material.TINY_GEAR)
            .input(Component.VERY_SHORT_ROD)
            .input(Component.TINY_RING, 2)
            // Mounted mechanical components use the finished dynamic wrench family.
            .tool(Tool.WRENCH, 1)
            .build();

    public static final ComponentDefinition SMALL_GEAR = component(Component.SMALL_GEAR)
            .input(Material.SMALL_GEAR)
            .input(Component.SHORT_ROD)
            .input(Component.SMALL_RING, 2)
            // Mounted mechanical components use the finished dynamic wrench family.
            .tool(Tool.WRENCH, 1)
            .build();

    public static final ComponentDefinition GEAR = component(Component.GEAR)
            .input(Material.GEAR)
            .input(Component.ROD)
            .input(Component.RING, 2)
            // Mounted mechanical components use the finished dynamic wrench family.
            .tool(Tool.WRENCH, 1)
            .build();

    public static final ComponentDefinition LARGE_GEAR = component(Component.LARGE_GEAR)
            .input(Material.LARGE_GEAR)
            .input(Component.LONG_ROD)
            .input(Component.LARGE_RING, 2)
            // Mounted mechanical components use the finished dynamic wrench family.
            .tool(Tool.WRENCH, 1)
            .build();

    public static final ComponentDefinition HUGE_GEAR = component(Component.HUGE_GEAR)
            .input(Material.HUGE_GEAR)
            .input(Component.VERY_LONG_ROD)
            .input(Component.HUGE_RING, 2)
            // Mounted mechanical components use the finished dynamic wrench family.
            .tool(Tool.WRENCH, 1)
            .build();

    // Bearings use retaining rings and their own fastening. Ring components do not depend on screws, so this stays acyclic.
    public static final ComponentDefinition TINY_BEARING = component(Component.TINY_BEARING)
            .input(Material.TINY_BEARING)
            .input(Component.TINY_RING, 2)
            .inputAny(Component.TINY_SCREW, 4)
                        .tool(Tool.BEARING_PRESS, 1)
            .build();

    public static final ComponentDefinition SMALL_BEARING = component(Component.SMALL_BEARING)
            .input(Material.SMALL_BEARING)
            .input(Component.SMALL_RING, 2)
            .inputAny(Component.SMALL_SCREW, 4)
                        .tool(Tool.BEARING_PRESS, 1)
            .build();

    public static final ComponentDefinition BEARING = component(Component.BEARING)
            .input(Material.BEARING)
            .input(Component.RING, 2)
            .inputAny(Component.SCREW, 4)
                        .tool(Tool.BEARING_PRESS, 1)
            .build();

    public static final ComponentDefinition LARGE_BEARING = component(Component.LARGE_BEARING)
            .input(Material.LARGE_BEARING)
            .input(Component.LARGE_RING, 2)
            .inputAny(Component.LARGE_SCREW, 4)
                        .tool(Tool.BEARING_PRESS, 1)
            .build();

    public static final ComponentDefinition HUGE_BEARING = component(Component.HUGE_BEARING)
            .input(Material.HUGE_BEARING)
            .input(Component.HUGE_RING, 2)
            .inputAny(Component.HUGE_SCREW, 4)
                        .tool(Tool.BEARING_PRESS, 1)
            .build();

    // A rotor mounted in a larger machine carries a shaft, two bearings and two retaining rings.
    public static final ComponentDefinition TINY_ROTOR = component(Component.TINY_ROTOR)
            .input(Material.TINY_ROTOR)
            .input(Component.VERY_SHORT_ROD)
            .input(Component.TINY_BEARING, 2)
            .input(Component.TINY_RING, 2)
            // Mounted mechanical components use the finished dynamic wrench family.
            .tool(Tool.WRENCH, 1)
            .build();

    public static final ComponentDefinition SMALL_ROTOR = component(Component.SMALL_ROTOR)
            .input(Material.SMALL_ROTOR)
            .input(Component.SHORT_ROD)
            .input(Component.SMALL_BEARING, 2)
            .input(Component.SMALL_RING, 2)
            // Mounted mechanical components use the finished dynamic wrench family.
            .tool(Tool.WRENCH, 1)
            .build();

    public static final ComponentDefinition ROTOR = component(Component.ROTOR)
            .input(Material.ROTOR)
            .input(Component.ROD)
            .input(Component.BEARING, 2)
            .input(Component.RING, 2)
            // Mounted mechanical components use the finished dynamic wrench family.
            .tool(Tool.WRENCH, 1)
            .build();

    public static final ComponentDefinition LARGE_ROTOR = component(Component.LARGE_ROTOR)
            .input(Material.LARGE_ROTOR)
            .input(Component.LONG_ROD)
            .input(Component.LARGE_BEARING, 2)
            .input(Component.LARGE_RING, 2)
            // Mounted mechanical components use the finished dynamic wrench family.
            .tool(Tool.WRENCH, 1)
            .build();

    public static final ComponentDefinition HUGE_ROTOR = component(Component.HUGE_ROTOR)
            .input(Material.HUGE_ROTOR)
            .input(Component.VERY_LONG_ROD)
            .input(Component.HUGE_BEARING, 2)
            .input(Component.HUGE_RING, 2)
            // Mounted mechanical components use the finished dynamic wrench family.
            .tool(Tool.WRENCH, 1)
            .build();

    public static final ComponentDefinition TURBINE_BLADE = component(Component.TURBINE_BLADE)
            .input(Material.TURBINE_BLADE)
            .inputAny(Component.SCREW, 2)
            // The finished blade is mounted to a turbine rotor with the dynamic wrench family.
            .tool(Tool.WRENCH, 1)
            .build();

    public static final ComponentDefinition MIXING_HEAD = component(Component.MIXING_HEAD)
            .input(net.mads.industron.material.MaterialPart.MIXING_BLADE, 4)
            .input(Component.ROD)
            .input(Component.RING, 2)
            .tool(Tool.WRENCH, 2)
            .build();

    public static final ComponentDefinition IMPELLER = component(Component.IMPELLER)
            .input(net.mads.industron.material.MaterialPart.IMPELLER)
            .input(Component.ROD)
            .input(Component.RING, 2)
            .tool(Tool.WRENCH, 2)
            .build();

    public static final ComponentDefinition CRANK_LINKAGE = component(Component.CRANK_LINKAGE)
            .input(net.mads.industron.material.MaterialPart.CRANK)
            .input(net.mads.industron.material.MaterialPart.CONNECTING_ROD)
            .input(Component.BEARING, 2)
            .inputAny(Component.SCREW, 4)
            .tool(Tool.WRENCH, 2)
            .build();

    public static final ComponentDefinition PRESS_SLIDE = component(Component.PRESS_SLIDE)
            .input(net.mads.industron.material.MaterialPart.PRESS_HEAD)
            .input(net.mads.industron.material.MaterialPart.GUIDE_RAIL, 2)
            .inputAny(Component.SCREW, 8)
            .tool(Tool.FILE, 4)
            .tool(Tool.WRENCH, 2)
            .build();

    public static final ComponentDefinition CHUCK = component(Component.CHUCK)
            .input(net.mads.industron.material.MaterialPart.CHUCK_JAW, 3)
            .input(Component.SMALL_GEAR)
            .inputAny(Component.SCREW, 6)
            .tool(Tool.FILE, 3)
            .build();

    public static final ComponentDefinition CUTTER = component(Component.CUTTER)
            .input(net.mads.industron.material.MaterialPart.CUTTING_INSERT)
            .input(Component.SHORT_ROD)
            .inputAny(Component.SCREW, 2)
            .tool(Tool.FILE, 2)
            .build();

    public static final ComponentDefinition CRUSHING_DRUM = component(Component.CRUSHING_DRUM)
            .input(net.mads.industron.material.MaterialPart.CRUSHING_SEGMENT, 6)
            .input(Component.ROD)
            .input(Component.BEARING, 2)
            .inputAny(Component.SCREW, 12)
            .tool(Tool.WRENCH, 4)
            .build();

    public static final ComponentDefinition CYLINDER = component(Component.CYLINDER)
            .input(net.mads.industron.material.MaterialPart.CYLINDER)
            .input(net.mads.industron.material.MaterialPart.PISTON)
            .input(net.mads.industron.material.MaterialPart.PISTON_RING, 2)
            .input(net.mads.industron.material.MaterialPart.CONNECTING_ROD)
            .input(net.mads.industron.material.MaterialPart.FLANGE, 2)
            .inputAny(Component.SCREW, 8)
            .input("industron:fiber_gasket", 2)
            .tool(Tool.FILE, 6)
            .tool(Tool.WRENCH, 4)
            .build();

    public static final ComponentDefinition SAFETY_VALVE = component(Component.SAFETY_VALVE)
            .input(net.mads.industron.material.MaterialPart.VALVE_BODY)
            .input(net.mads.industron.material.MaterialPart.VALVE_STEM)
            .input(net.mads.industron.material.MaterialPart.VALVE_SEAT)
            .input(net.mads.industron.material.MaterialPart.SPRING)
            .input(net.mads.industron.material.MaterialPart.FLANGE)
            .inputAny(Component.SCREW, 4)
            .input("industron:fiber_gasket", 1)
            .tool(Tool.FILE, 2)
            .tool(Tool.WRENCH, 2)
            .build();

    public static final ComponentDefinition PRESSURE_VESSEL = component(Component.PRESSURE_VESSEL)
            .input(Component.REINFORCED_PLATE, 6)
            .input(net.mads.industron.material.MaterialPart.FLANGE, 4)
            .inputAny(Component.SCREW, 8)
            .input("industron:fiber_gasket", 4)
            .tool(Tool.HAMMER, 8)
            .tool(Tool.WRENCH, 4)
            .build();

    // Expose the physical rotor directly for JEI; preserve the mounted rotor's
    // shaft, bearings, retaining rings and tool work when expanding its subtree.
    public static final ComponentDefinition FAN_ASSEMBLY = component(Component.FAN_ASSEMBLY)
            .input(Material.ROTOR)
            .input(Component.ROD)
            .input(Component.BEARING, 4)
            .input(Component.RING, 2)
            .inputAny(Component.SCREW, 4)
            .tool(Tool.WRENCH, 3)
            .build();

    public static final ComponentDefinition EXTRUSION_HEAD = component(Component.EXTRUSION_HEAD)
            .input(net.mads.industron.material.MaterialPart.EXTRUSION_DIE)
            .input(net.mads.industron.material.MaterialPart.FLANGE, 2)
            .inputAny(Component.SCREW, 4)
            .tool(Tool.FILE, 4)
            .tool(Tool.WRENCH, 2)
            .build();

    public static final List<ComponentDefinition> ALL = buildAll();

    private static List<ComponentDefinition> buildAll() {
        java.util.ArrayList<ComponentDefinition> result = new java.util.ArrayList<>(List.of(
                STICK, BAMBOO, WOOD_PEG, WOOD_PLATE, WOOD_SHAFT, SMALL_WOOD_GEAR, WOOD_GEAR,
                TINY_SCREW, SMALL_SCREW, SCREW, LARGE_SCREW, HUGE_SCREW,
                TINY_RING, SMALL_RING, RING, LARGE_RING, HUGE_RING,
                VERY_SHORT_ROD, SHORT_ROD, ROD, LONG_ROD, VERY_LONG_ROD,
                PLATE, LARGE_PLATE, DOUBLE_PLATE, LARGE_DOUBLE_PLATE, DENSE_PLATE, LARGE_DENSE_PLATE,
                REINFORCED_PLATE, HEAT_EXCHANGER_PLATE,
                TINY_GEAR, SMALL_GEAR, GEAR, LARGE_GEAR, HUGE_GEAR,
                TINY_BEARING, SMALL_BEARING, BEARING, LARGE_BEARING, HUGE_BEARING,
                TINY_ROTOR, SMALL_ROTOR, ROTOR, LARGE_ROTOR, HUGE_ROTOR,
                TURBINE_BLADE,
                MIXING_HEAD, IMPELLER, CRANK_LINKAGE, PRESS_SLIDE, CHUCK, CUTTER, CRUSHING_DRUM, CYLINDER, SAFETY_VALVE, PRESSURE_VESSEL, FAN_ASSEMBLY, EXTRUSION_HEAD
        ));
        result.addAll(WoodAssemblyComponents.DEFINITIONS);
        return List.copyOf(result);
    }

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
    public static boolean canResolve(AssemblyComponent component, IndustrialSubstance material) {
        return canResolve(component, material, List.of());
    }

    public static boolean canResolve(
            AssemblyComponent component,
            IndustrialSubstance material,
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

    public static boolean canResolve(
            AssemblyComponent component,
            net.mads.industron.recipe.recipetypes.assembly.AssemblyMaterialSelector selector,
            List<net.mads.industron.recipe.recipetypes.assembly.AssemblyRequirement> requirements
    ) {
        return planCanResolve(AssemblyPlan.compileComponent(
                java.util.Objects.requireNonNull(component, "component"),
                java.util.Objects.requireNonNull(selector, "selector"),
                List.copyOf(requirements)
        ));
    }

    private static boolean planCanResolve(List<AssemblyPlan.Step> plan) {
        for (AssemblyPlan.Step step : plan) {
            switch (step.kind()) {
                case MATERIAL -> {
                    if (step.fixedMaterial() != null) {
                        if (!matches(step.fixedMaterial(), step)) return false;
                    } else if (AssemblyMaterialCatalog.candidates(step.materialSelector(), step.material()).stream()
                            .noneMatch(candidate -> matches(candidate, step))) {
                        return false;
                    }
                }
                case TOOL -> {
                    if (!AssemblyTools.hasType(step.tool())) {
                        return false;
                    }
                }
                case PLANT_PART, ITEM, WAIT -> {
                    // Plant-part roles and exact item ids are registry-backed; waits have no material dependency.
                }
            }
        }
        return true;
    }

    private static boolean matches(IndustrialSubstance material, AssemblyPlan.Step step) {
        if (!step.acceptsMaterial(material)) return false;
        if (!AssemblyMaterialCatalog.exposesPart(material, step.material())) return false;
        if (step.requirements().isEmpty()) return true;
        if (!(material instanceof IndustrialMaterial industrial)) return false;
        return step.requirements().stream().allMatch(requirement -> requirement.matches(industrial, step.material()));
    }
}
