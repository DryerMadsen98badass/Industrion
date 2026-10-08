package net.mads.industron.item;

import java.util.List;

/** Standalone items that do not depend on an IndustrialMaterial definition. */
public final class SimpleItems {
    public static final List<SimpleItemDefinition> ALL = withMachineComponents(List.of(
            new SimpleItemDefinition("fiber_gasket", "Fiber Gasket", "industron:item/components/fiber_gasket", null, 0, 0),
            materialTerracotta("mixing_blade", "Mixing Blade", "machine_parts/mixing_blade/terracotta"),
            materialTerracotta("impeller", "Impeller", "machine_parts/impeller/terracotta"),
            materialTerracotta("crank", "Crank", "machine_parts/crank/terracotta"),
            materialTerracotta("connecting_rod", "Connecting Rod", "machine_parts/connecting_rod/terracotta"),
            materialTerracotta("press_head", "Press Head", "machine_parts/press_head/terracotta"),
            materialTerracotta("guide_rail", "Guide Rail", "machine_parts/guide_rail/terracotta"),
            materialTerracotta("chuck_jaw", "Chuck Jaw", "machine_parts/chuck_jaw/terracotta"),
            materialTerracotta("cutting_insert", "Cutting Insert", "machine_parts/cutting_insert/terracotta"),
            materialTerracotta("crushing_segment", "Crushing Segment", "machine_parts/crushing_segment/terracotta"),
            materialTerracotta("cylinder", "Cylinder", "machine_parts/cylinder/terracotta"),
            materialTerracotta("piston", "Piston", "machine_parts/piston/terracotta"),
            materialTerracotta("piston_ring", "Piston Ring", "machine_parts/piston_ring/terracotta"),
            materialTerracotta("valve_body", "Valve Body", "machine_parts/valve_body/terracotta"),
            materialTerracotta("valve_stem", "Valve Stem", "machine_parts/valve_stem/terracotta"),
            materialTerracotta("valve_seat", "Valve Seat", "machine_parts/valve_seat/terracotta"),
            materialTerracotta("spring", "Spring", "spring/normal/terracotta"),
            materialTerracotta("flange", "Flange", "machine_parts/flange/terracotta"),
            materialTerracotta("extrusion_die", "Extrusion Die", "machine_parts/extrusion_die/terracotta"),
            new SimpleItemDefinition("wheat_grain", "Cleaned Wheat Grain", "minecraft:item/wheat_seeds", null, 0, 0),
            new SimpleItemDefinition("wheat_flour", "Wheat Flour", "minecraft:item/sugar", null, 0, 0),
            new SimpleItemDefinition("bread_dough", "Bread Dough", "minecraft:item/clay_ball", 0xdecba0, 0, 0),
            new SimpleItemDefinition("burnt_bread", "Burnt Bread", "minecraft:item/bread", 0x40302a, 0, 0),
            new SimpleItemDefinition("burnt_potato", "Burnt Potato", "minecraft:item/baked_potato", 0x40302a, 0, 0),
            new SimpleItemDefinition("cleaned_cod", "Cleaned Cod", "minecraft:item/cod", null, 0, 0),
            new SimpleItemDefinition("cleaned_salmon", "Cleaned Salmon", "minecraft:item/salmon", null, 0, 0),
            new SimpleItemDefinition("cleaned_tropical_fish", "Cleaned Tropical Fish", "minecraft:item/tropical_fish", null, 0, 0),
            new SimpleItemDefinition("portal_activator", "Portal Activator", "minecraft:item/ender_eye", null, 0, 0),
            new SimpleItemDefinition("portal_core", "Portal Core", "minecraft:item/ender_pearl", null, 0, 0),
            new SimpleItemDefinition("bow_string", "Bow String", "item/tool/bow/string/variant_1/base", null, 0, 0),
            new SimpleItemDefinition("crossbow_string", "Crossbow String", "item/tool/crossbow/string/variant_1/base", null, 0, 0),
            new SimpleItemDefinition("fishing_line", "Fishing Line", "item/tool/fishing_rod/line/variant_1/base", null, 0, 0),
            new SimpleItemDefinition(
                    "sifter_mesh",
                    "Sifter Mesh",
                    "item/tool/sifter/mesh/variant_1/mesh",
                    null,
                    0,
                    0
            ),
            new SimpleItemDefinition(
                    "wet_paper",
                    "Wet Paper",
                    "minecraft:item/paper",
                    0x808080
            ),
            new SimpleItemDefinition(
                    "flint_and_pebble",
                    "Flint and Pebble",
                    "minecraft:item/flint",
                    null,
                    0,
                    0
            ).durability(64),
            materialTerracotta("tiny_ball", "Tiny Ball", "ball/tiny/terracotta"),
            materialTerracotta("large_ball", "Large Ball", "ball/large/terracotta"),
            materialTerracotta("huge_ball", "Huge Ball", "ball/huge/terracotta"),
            materialTerracotta("tiny_gear", "Tiny Gear", "gear/tiny/terracotta"),
            materialTerracotta("huge_gear", "Huge Gear", "gear/huge/terracotta"),
            materialTerracotta("tiny_ring", "Tiny Ring", "ring/tiny/terracotta"),
            materialTerracotta("huge_ring", "Huge Ring", "ring/huge/terracotta"),
            materialTerracotta("tiny_rivet", "Tiny Rivet", "rivet/tiny/terracotta"),
            materialTerracotta("small_rivet", "Small Rivet", "rivet/small/terracotta"),
            materialTerracotta("large_rivet", "Large Rivet", "rivet/large/terracotta"),
            materialTerracotta("huge_rivet", "Huge Rivet", "rivet/huge/terracotta"),
            materialTerracotta("tiny_rotor", "Tiny Rotor", "rotor/tiny/terracotta"),
            materialTerracotta("small_rotor", "Small Rotor", "rotor/small/terracotta"),
            materialTerracotta("rotor", "Rotor", "rotor/normal/terracotta"),
            materialTerracotta("huge_rotor", "Huge Rotor", "rotor/huge/terracotta"),
            materialTerracotta("large_plate", "Large Plate", "plates/plate/large/terracotta"),
            materialTerracotta("very_short_rod", "Very Short Rod", "rod/very_short/terracotta"),
            materialTerracotta("short_rod", "Short Rod", "rod/short/terracotta"),
            materialTerracotta("rod", "Rod", "rod/normal/terracotta"),
            materialTerracotta("small_ball", "Small Ball", "ball/small/terracotta"),
            materialTerracotta("ball", "Ball", "ball/normal/terracotta"),
            materialTerracotta("small_gear", "Small Gear", "gear/small/terracotta"),
            materialTerracotta("gear", "Gear", "gear/normal/terracotta"),
            materialTerracotta("large_gear", "Large Gear", "gear/large/terracotta"),
            materialTerracotta("small_ring", "Small Ring", "ring/small/terracotta"),
            materialTerracotta("ring", "Ring", "ring/normal/terracotta"),
            materialTerracotta("large_ring", "Large Ring", "ring/large/terracotta"),
            materialTerracotta("rivet", "Rivet", "rivet/normal/terracotta"),
            materialTerracotta("large_rotor", "Large Rotor", "rotor/large/terracotta"),
            materialTerracotta("ingot", "Ingot", "ingots/ingot/terracotta"),
            materialTerracotta("nugget", "Nugget", "nuggets/nugget/terracotta"),
            materialTerracotta("plate", "Plate", "plates/plate/normal/terracotta"),
            materialTerracotta("long_rod", "Long Rod", "rod/long/terracotta"),
            materialTerracotta("very_long_rod", "Very Long Rod", "rod/very_long/terracotta"),
            toolTerracotta("axe_head", "Axe Head", "axe/head/terracotta"),
            materialTerracotta("buzz_saw", "Buzz Saw", "buzz_saw/terracotta"),
            toolTerracotta("chainsaw_head", "Chainsaw Head", "chainsaw/head/terracotta"),
            toolTerracotta("chisel_head", "Chisel Head", "chisel/head/terracotta"),
            toolTerracotta("crowbar_head", "Crowbar Head", "crowbar/head/terracotta"),
            toolTerracotta("drill_head", "Drill Head", "drill/head/terracotta"),
            toolTerracotta("file_head", "File Head", "file/head/terracotta"),
            toolTerracotta("hammer_head", "Hammer Head", "hammer/head/terracotta"),
            toolTerracotta("hoe_head", "Hoe Head", "hoe/head/terracotta"),
            toolTerracotta("pickaxe_head", "Pickaxe Head", "pickaxe/head/terracotta"),
            toolTerracotta("screwdriver_head", "Screwdriver Head", "screwdriver/head/terracotta"),
            toolTerracotta("shovel_head", "Shovel Head", "shovel/head/terracotta"),
            toolTerracotta("wire_cutter_head", "Wire Cutter Head", "wirecutter/head/terracotta"),
            toolTerracotta("wrench", "Wrench", "wrench/terracotta"),
            toolTerracotta("sword_blade", "Sword Blade", "sword/head/terracotta"),
            toolTerracotta("knife_blade", "Knife Blade", "knife/head/terracotta"),
            toolTerracotta("saw_blade", "Saw Blade", "saw/head/terracotta"),
            toolTerracotta("saw_handle", "Saw Handle", "saw/handle/terracotta"),
            toolTerracotta("wire_cutter_body", "Wire Cutter Body", "wirecutter/body/terracotta"),
            toolTerracotta("shears_head", "Shears Head", "shears/head/terracotta"),
            toolTerracotta("shears_handle", "Shears Handle", "shears/handle/terracotta"),
            toolTerracotta("crossbow_limbs", "Crossbow Limbs", "crossbow/limbs/terracotta"),
            toolTerracotta("crossbow_trigger", "Crossbow Trigger", "crossbow/trigger/terracotta"),
            toolTerracotta("fishing_hook", "Fishing Hook", "fishing_rod/hook/terracotta"),
            toolTerracotta("shield_body", "Shield Body", "shield/body/terracotta"),
            toolTerracotta("shield_handle", "Shield Handle", "shield/handle/terracotta"),
            toolTerracotta("helmet_shell", "Helmet Shell", "armour/helmet/terracotta"),
            toolTerracotta("chestplate_shell", "Chestplate Shell", "armour/chestplate/terracotta"),
            toolTerracotta("leggings_shell", "Leggings Shell", "armour/leggings/terracotta"),
            toolTerracotta("boots_shell", "Boots Shell", "armour/boots/terracotta"),
            toolTerracotta("snap_ring_pliers_head", "Snap Ring Pliers Head", "snap_ring_pliers/head/terracotta"),
            toolTerracotta("snap_ring_pliers_handle", "Snap Ring Pliers Handle", "snap_ring_pliers/handle/terracotta"),
            toolTerracotta("bearing_press_head", "Bearing Press Head", "bearing_press/head/terracotta"),
            toolTerracotta("bearing_press_handle", "Bearing Press Handle", "bearing_press/handle/terracotta"),
            toolTerracotta("clamp_head", "Clamp Head", "clamp/head/terracotta"),
            toolTerracotta("clamp_handle", "Clamp Handle", "clamp/handle/terracotta"),
            toolTerracotta("crimping_tool_head", "Crimping Tool Head", "crimping_tool/head/terracotta"),
            toolTerracotta("crimping_tool_handle", "Crimping Tool Handle", "crimping_tool/handle/terracotta"),
            toolTerracotta("gear_cutter_head", "Gear Cutter Head", "gear_cutter/head/terracotta"),
            toolTerracotta("gear_cutter_handle", "Gear Cutter Handle", "gear_cutter/handle/terracotta"),
            toolTerracotta("tool_handle", "Tool Handle", "tool_handle/terracotta")
    ));

    private static List<SimpleItemDefinition> withMachineComponents(List<SimpleItemDefinition> existing) {
        java.util.ArrayList<SimpleItemDefinition> items = new java.util.ArrayList<>(existing);
        items.addAll(MachineComponentItems.ALL);
        return List.copyOf(items);
    }

    private SimpleItems() {
    }

    /**
     * Raw terracotta casting form. The PNG already contains its final brown colour;
     * no item tint, material colour, recipe or gameplay behaviour is attached here.
     */
    private static SimpleItemDefinition materialTerracotta(String id, String displayName, String texturePath) {
        return terracotta(id, displayName, "item/material_sets/" + texturePath);
    }

    private static SimpleItemDefinition toolTerracotta(String id, String displayName, String texturePath) {
        return terracotta(id, displayName, "item/tool/" + texturePath);
    }

    private static SimpleItemDefinition terracotta(String id, String displayName, String texturePath) {
        return new SimpleItemDefinition(
                "terracotta_" + id,
                "Terracotta " + displayName,
                texturePath,
                null,
                0,
                0
        );
    }
}
