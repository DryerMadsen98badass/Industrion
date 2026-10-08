package net.mads.industron.recipe.recipes.assembly;

import net.mads.industron.recipe.recipetypes.assembly.AssemblyComponent;

/** Public recursive component-tree names used by assembly recipes. */
public final class Component {
    /** One physical stick tied with one valid plant-derived string/cord. */
    public static final AssemblyComponent STICK = component("stick", "Stick");
    /** One bamboo stem tied with two valid plant-derived strings/cords. */
    public static final AssemblyComponent BAMBOO = component("bamboo", "Bamboo");
    /** One wooden peg driven into an assembly with one Mallet use. */
    public static final AssemblyComponent WOOD_PEG = component("wood_peg", "Wood Peg");
    /** One wooden plate secured with four installed wooden pegs. */
    public static final AssemblyComponent WOOD_PLATE = component("wood_plate", "Wood Plate");
    /** One wooden shaft secured with four installed wooden pegs. */
    public static final AssemblyComponent WOOD_SHAFT = component("wood_shaft", "Wood Shaft");
    /** One small wooden gear secured with four installed wooden pegs. */
    public static final AssemblyComponent SMALL_WOOD_GEAR = component("small_wood_gear", "Small Wood Gear");
    /** One wooden gear secured with four installed wooden pegs. */
    public static final AssemblyComponent WOOD_GEAR = component("wood_gear", "Wood Gear");

    public static final AssemblyComponent TINY_SCREW = component("tiny_screw", "Tiny Screw");
    public static final AssemblyComponent SMALL_SCREW = component("small_screw", "Small Screw");
    public static final AssemblyComponent SCREW = component("screw", "Screw");
    public static final AssemblyComponent LARGE_SCREW = component("large_screw", "Large Screw");
    public static final AssemblyComponent HUGE_SCREW = component("huge_screw", "Huge Screw");

    public static final AssemblyComponent TINY_RING = component("tiny_ring", "Tiny Ring");
    public static final AssemblyComponent SMALL_RING = component("small_ring", "Small Ring");
    public static final AssemblyComponent RING = component("ring", "Ring");
    public static final AssemblyComponent LARGE_RING = component("large_ring", "Large Ring");
    public static final AssemblyComponent HUGE_RING = component("huge_ring", "Huge Ring");

    public static final AssemblyComponent VERY_SHORT_ROD = component("very_short_rod", "Very Short Rod");
    public static final AssemblyComponent SHORT_ROD = component("short_rod", "Short Rod");
    public static final AssemblyComponent ROD = component("rod", "Rod");
    public static final AssemblyComponent LONG_ROD = component("long_rod", "Long Rod");
    public static final AssemblyComponent VERY_LONG_ROD = component("very_long_rod", "Very Long Rod");

    public static final AssemblyComponent PLATE = component("plate", "Plate");
    public static final AssemblyComponent LARGE_PLATE = component("large_plate", "Large Plate");
    public static final AssemblyComponent DOUBLE_PLATE = component("double_plate", "Double Plate");
    public static final AssemblyComponent LARGE_DOUBLE_PLATE = component("large_double_plate", "Large Double Plate");
    public static final AssemblyComponent DENSE_PLATE = component("dense_plate", "Dense Plate");
    public static final AssemblyComponent LARGE_DENSE_PLATE = component("large_dense_plate", "Large Dense Plate");
    public static final AssemblyComponent REINFORCED_PLATE = component("reinforced_plate", "Reinforced Plate");
    public static final AssemblyComponent HEAT_EXCHANGER_PLATE = component("heat_exchanger_plate", "Heat Exchanger Plate");

    public static final AssemblyComponent TINY_GEAR = component("tiny_gear", "Tiny Gear");
    public static final AssemblyComponent SMALL_GEAR = component("small_gear", "Small Gear");
    public static final AssemblyComponent GEAR = component("gear", "Gear");
    public static final AssemblyComponent LARGE_GEAR = component("large_gear", "Large Gear");
    public static final AssemblyComponent HUGE_GEAR = component("huge_gear", "Huge Gear");

    public static final AssemblyComponent TINY_BEARING = component("tiny_bearing", "Tiny Bearing");
    public static final AssemblyComponent SMALL_BEARING = component("small_bearing", "Small Bearing");
    public static final AssemblyComponent BEARING = component("bearing", "Bearing");
    public static final AssemblyComponent LARGE_BEARING = component("large_bearing", "Large Bearing");
    public static final AssemblyComponent HUGE_BEARING = component("huge_bearing", "Huge Bearing");

    public static final AssemblyComponent TINY_ROTOR = component("tiny_rotor", "Tiny Rotor");
    public static final AssemblyComponent SMALL_ROTOR = component("small_rotor", "Small Rotor");
    public static final AssemblyComponent ROTOR = component("rotor", "Rotor");
    public static final AssemblyComponent LARGE_ROTOR = component("large_rotor", "Large Rotor");
    public static final AssemblyComponent HUGE_ROTOR = component("huge_rotor", "Huge Rotor");

    public static final AssemblyComponent TURBINE_BLADE = component("turbine_blade", "Turbine Blade");

    public static final AssemblyComponent MIXING_HEAD = component("mixing_head", "Mixing Head");
    public static final AssemblyComponent IMPELLER = component("impeller", "Impeller");
    public static final AssemblyComponent CRANK_LINKAGE = component("crank_linkage", "Crank Linkage");
    public static final AssemblyComponent PRESS_SLIDE = component("press_slide", "Press Slide");
    public static final AssemblyComponent CHUCK = component("chuck", "Chuck");
    public static final AssemblyComponent CUTTER = component("cutter", "Cutter");
    public static final AssemblyComponent CRUSHING_DRUM = component("crushing_drum", "Crushing Drum");
    public static final AssemblyComponent CYLINDER = component("cylinder", "Cylinder");
    public static final AssemblyComponent SAFETY_VALVE = component("safety_valve", "Safety Valve");
    public static final AssemblyComponent PRESSURE_VESSEL = component("pressure_vessel", "Pressure Vessel");
    public static final AssemblyComponent FAN_ASSEMBLY = component("fan_assembly", "Fan Assembly");
    public static final AssemblyComponent EXTRUSION_HEAD = component("extrusion_head", "Extrusion Head");

    private Component() {
    }

    public static AssemblyComponent component(String id, String displayName) {
        return new AssemblyComponent(id, displayName);
    }
}
