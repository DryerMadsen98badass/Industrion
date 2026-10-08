package net.mads.industron.machine.foundry.casting;

import net.mads.industron.Industron;
import net.mads.industron.material.MaterialPart;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static net.mads.industron.material.MaterialPart.*;

/**
 * Raw terracotta positive forms that can be packed with clay in a Caster.
 * This list intentionally mirrors the terracotta mold items exposed by SimpleItems.
 */
public final class TerracottaMoldDefinitions {
    public record Definition(String terracottaItemId, MaterialPart coldPart, MaterialPart moldPart) {}

    public static final List<Definition> ALL = List.of(
            def("shears_head", SHEARS_HEAD, CAST_SHEARS_HEAD_MOLD),
            def("shears_handle", SHEARS_HANDLE, CAST_SHEARS_HANDLE_MOLD),
            def("crossbow_limbs", CROSSBOW_LIMBS, CAST_CROSSBOW_LIMBS_MOLD),
            def("crossbow_trigger", CROSSBOW_TRIGGER, CAST_CROSSBOW_TRIGGER_MOLD),
            def("fishing_hook", FISHING_HOOK, CAST_FISHING_HOOK_MOLD),
            def("shield_body", SHIELD_BODY, CAST_SHIELD_BODY_MOLD),
            def("shield_handle", SHIELD_HANDLE, CAST_SHIELD_HANDLE_MOLD),
            def("helmet_shell", HELMET_SHELL, CAST_HELMET_SHELL_MOLD),
            def("chestplate_shell", CHESTPLATE_SHELL, CAST_CHESTPLATE_SHELL_MOLD),
            def("leggings_shell", LEGGINGS_SHELL, CAST_LEGGINGS_SHELL_MOLD),
            def("boots_shell", BOOTS_SHELL, CAST_BOOTS_SHELL_MOLD),
            def("snap_ring_pliers_head", SNAP_RING_PLIERS_HEAD, CAST_SNAP_RING_PLIERS_HEAD_MOLD),
            def("snap_ring_pliers_handle", SNAP_RING_PLIERS_HANDLE, CAST_SNAP_RING_PLIERS_HANDLE_MOLD),
            def("bearing_press_head", BEARING_PRESS_HEAD, CAST_BEARING_PRESS_HEAD_MOLD),
            def("bearing_press_handle", BEARING_PRESS_HANDLE, CAST_BEARING_PRESS_HANDLE_MOLD),
            def("clamp_head", CLAMP_HEAD, CAST_CLAMP_HEAD_MOLD),
            def("clamp_handle", CLAMP_HANDLE, CAST_CLAMP_HANDLE_MOLD),
            def("crimping_tool_head", CRIMPING_TOOL_HEAD, CAST_CRIMPING_TOOL_HEAD_MOLD),
            def("crimping_tool_handle", CRIMPING_TOOL_HANDLE, CAST_CRIMPING_TOOL_HANDLE_MOLD),
            def("gear_cutter_head", GEAR_CUTTER_HEAD, CAST_GEAR_CUTTER_HEAD_MOLD),
            def("gear_cutter_handle", GEAR_CUTTER_HANDLE, CAST_GEAR_CUTTER_HANDLE_MOLD),

            def("tiny_ball", TINY_BALL, CAST_TINY_BALL_MOLD),
            def("large_ball", LARGE_BALL, CAST_LARGE_BALL_MOLD),
            def("huge_ball", HUGE_BALL, CAST_HUGE_BALL_MOLD),
            def("tiny_gear", TINY_GEAR, CAST_TINY_GEAR_MOLD),
            def("huge_gear", HUGE_GEAR, CAST_HUGE_GEAR_MOLD),
            def("tiny_ring", TINY_RING, CAST_TINY_RING_MOLD),
            def("huge_ring", HUGE_RING, CAST_HUGE_RING_MOLD),
            def("tiny_rivet", TINY_RIVET, CAST_TINY_RIVET_MOLD),
            def("small_rivet", SMALL_RIVET, CAST_SMALL_RIVET_MOLD),
            def("large_rivet", LARGE_RIVET, CAST_LARGE_RIVET_MOLD),
            def("huge_rivet", HUGE_RIVET, CAST_HUGE_RIVET_MOLD),
            def("tiny_rotor", TINY_ROTOR, CAST_TINY_ROTOR_MOLD),
            def("small_rotor", SMALL_ROTOR, CAST_SMALL_ROTOR_MOLD),
            def("rotor", ROTOR, CAST_ROTOR_MOLD),
            def("huge_rotor", HUGE_ROTOR, CAST_HUGE_ROTOR_MOLD),
            def("large_plate", LARGE_PLATE, CAST_LARGE_PLATE_MOLD),
            def("very_short_rod", VERY_SHORT_ROD, CAST_VERY_SHORT_ROD_MOLD),
            def("short_rod", SHORT_ROD, CAST_SHORT_ROD_MOLD),
            def("rod", ROD, CAST_ROD_MOLD),
            def("small_ball", SMALL_BALL, CAST_SMALL_BALL_MOLD),
            def("ball", BALL, CAST_BEARING_BALL_MOLD),
            def("small_gear", SMALL_GEAR, CAST_SMALL_GEAR_MOLD),
            def("gear", GEAR, CAST_GEAR_MOLD),
            def("large_gear", LARGE_GEAR, CAST_LARGE_GEAR_MOLD),
            def("small_ring", SMALL_RING, CAST_SMALL_RING_MOLD),
            def("ring", RING, CAST_RING_MOLD),
            def("large_ring", LARGE_RING, CAST_LARGE_RING_MOLD),
            def("rivet", RIVET, CAST_RIVET_MOLD),
            def("large_rotor", LARGE_ROTOR, CAST_LARGE_ROTOR_MOLD),
            def("ingot", INGOT, CAST_INGOT_MOLD),
            def("nugget", NUGGET, CAST_NUGGET_MOLD),
            def("plate", PLATE, CAST_PLATE_MOLD),
            def("long_rod", LONG_ROD, CAST_LONG_ROD_MOLD),
            def("very_long_rod", VERY_LONG_ROD, CAST_VERY_LONG_ROD_MOLD),
            def("axe_head", TOOL_HEAD_AXE, CAST_TOOL_HEAD_AXE_MOLD),
            def("buzz_saw", BUZZ_SAW, CAST_BUZZ_SAW_MOLD),
            def("chainsaw_head", TOOL_HEAD_CHAINSAW, CAST_TOOL_HEAD_CHAINSAW_MOLD),
            def("chisel_head", TOOL_HEAD_CHISEL, CAST_TOOL_HEAD_CHISEL_MOLD),
            def("crowbar_head", TOOL_HEAD_CROWBAR, CAST_TOOL_HEAD_CROWBAR_MOLD),
            def("drill_head", TOOL_HEAD_DRILL, CAST_TOOL_HEAD_DRILL_MOLD),
            def("file_head", TOOL_HEAD_FILE, CAST_TOOL_HEAD_FILE_MOLD),
            def("hammer_head", TOOL_HEAD_HAMMER, CAST_TOOL_HEAD_HAMMER_MOLD),
            def("hoe_head", TOOL_HEAD_HOE, CAST_TOOL_HEAD_HOE_MOLD),
            def("pickaxe_head", TOOL_HEAD_PICKAXE, CAST_TOOL_HEAD_PICKAXE_MOLD),
            def("screwdriver_head", TOOL_HEAD_SCREWDRIVER, CAST_TOOL_HEAD_SCREWDRIVER_MOLD),
            def("shovel_head", TOOL_HEAD_SHOVEL, CAST_TOOL_HEAD_SHOVEL_MOLD),
            def("wire_cutter_head", TOOL_HEAD_WIRE_CUTTER, CAST_TOOL_HEAD_WIRE_CUTTER_MOLD),
            def("wrench", WRENCH, CAST_WRENCH_MOLD),
            def("sword_blade", SWORD_BLADE, CAST_SWORD_BLADE_MOLD),
            def("knife_blade", KNIFE_BLADE, CAST_KNIFE_BLADE_MOLD),
            def("saw_blade", SAW_BLADE, CAST_SAW_BLADE_MOLD),
            def("saw_handle", SAW_HANDLE, CAST_SAW_HANDLE_MOLD),
            def("wire_cutter_body", WIRE_CUTTER_BODY, CAST_WIRE_CUTTER_BODY_MOLD),
            def("tool_handle", TOOL_HANDLE, CAST_TOOL_HANDLE_MOLD),
            def("mixing_blade", MIXING_BLADE, CAST_MIXING_BLADE_MOLD),
            def("impeller", IMPELLER, CAST_IMPELLER_MOLD),
            def("crank", CRANK, CAST_CRANK_MOLD),
            def("connecting_rod", CONNECTING_ROD, CAST_CONNECTING_ROD_MOLD),
            def("press_head", PRESS_HEAD, CAST_PRESS_HEAD_MOLD),
            def("guide_rail", GUIDE_RAIL, CAST_GUIDE_RAIL_MOLD),
            def("chuck_jaw", CHUCK_JAW, CAST_CHUCK_JAW_MOLD),
            def("cutting_insert", CUTTING_INSERT, CAST_CUTTING_INSERT_MOLD),
            def("crushing_segment", CRUSHING_SEGMENT, CAST_CRUSHING_SEGMENT_MOLD),
            def("cylinder", CYLINDER, CAST_CYLINDER_MOLD),
            def("piston", PISTON, CAST_PISTON_MOLD),
            def("piston_ring", PISTON_RING, CAST_PISTON_RING_MOLD),
            def("valve_body", VALVE_BODY, CAST_VALVE_BODY_MOLD),
            def("valve_stem", VALVE_STEM, CAST_VALVE_STEM_MOLD),
            def("valve_seat", VALVE_SEAT, CAST_VALVE_SEAT_MOLD),
            def("spring", SPRING, CAST_SPRING_MOLD),
            def("flange", FLANGE, CAST_FLANGE_MOLD),
            def("extrusion_die", EXTRUSION_DIE, CAST_EXTRUSION_DIE_MOLD)
    );

    private TerracottaMoldDefinitions() {}

    public static Definition find(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!Industron.MOD_ID.equals(id.getNamespace()) || !id.getPath().startsWith("terracotta_")) return null;
        String rawId = id.getPath().substring("terracotta_".length());
        return ALL.stream().filter(definition -> definition.terracottaItemId().equals(rawId)).findFirst().orElse(null);
    }

    public static boolean supports(MaterialPart moldPart) {
        return ALL.stream().anyMatch(definition -> definition.moldPart() == moldPart);
    }

    private static Definition def(String terracottaItemId, MaterialPart coldPart, MaterialPart moldPart) {
        return new Definition(terracottaItemId, coldPart, moldPart);
    }
}
