package net.mads.industron.machine.foundry.casting;

import net.mads.industron.material.MaterialPart;
import java.util.List;
import java.util.Set;
import java.util.EnumSet;
import static net.mads.industron.material.MaterialPart.*;

/** Checklist Input: Har Mold=Ja and Nei are eligible; Nei means missing artwork only.
 * Volumes are shared by casting, cooling and remelting to prevent duplication loops. */
public final class CastingDefinitions {
    public record Form(MaterialPart cold, MaterialPart hot, MaterialPart mold) {
        public Form {
            if (cold == null || hot == null || mold == null) throw new IllegalArgumentException("Casting form cannot contain null parts");
            if (cold.materialAmountMb() <= 0) throw new IllegalArgumentException("Casting form needs MaterialPart mass: " + cold);
            if (hot.coldForgePart() != cold) throw new IllegalArgumentException("Casting hot form must inherit from cold form: " + hot);
        }

        public int millibuckets() { return cold.materialAmountMb(); }
    }

    /**
     * Mold artwork/items that already exist, but deliberately have no casting volume/recipe yet.
     * Moving one of these into ALL later assigns gameplay casting data without changing its mold id.
     */
    public record PendingMold(MaterialPart cold, MaterialPart hot, MaterialPart mold) {}
    public static final List<Form> ALL = List.of(
            new Form(SHEARS_HEAD, HOT_SHEARS_HEAD, CAST_SHEARS_HEAD_MOLD),
            new Form(SHEARS_HANDLE, HOT_SHEARS_HANDLE, CAST_SHEARS_HANDLE_MOLD),
            new Form(CROSSBOW_LIMBS, HOT_CROSSBOW_LIMBS, CAST_CROSSBOW_LIMBS_MOLD),
            new Form(CROSSBOW_TRIGGER, HOT_CROSSBOW_TRIGGER, CAST_CROSSBOW_TRIGGER_MOLD),
            new Form(FISHING_HOOK, HOT_FISHING_HOOK, CAST_FISHING_HOOK_MOLD),
            new Form(SHIELD_BODY, HOT_SHIELD_BODY, CAST_SHIELD_BODY_MOLD),
            new Form(SHIELD_HANDLE, HOT_SHIELD_HANDLE, CAST_SHIELD_HANDLE_MOLD),
            new Form(HELMET_SHELL, HOT_HELMET_SHELL, CAST_HELMET_SHELL_MOLD),
            new Form(CHESTPLATE_SHELL, HOT_CHESTPLATE_SHELL, CAST_CHESTPLATE_SHELL_MOLD),
            new Form(LEGGINGS_SHELL, HOT_LEGGINGS_SHELL, CAST_LEGGINGS_SHELL_MOLD),
            new Form(BOOTS_SHELL, HOT_BOOTS_SHELL, CAST_BOOTS_SHELL_MOLD),
            new Form(SNAP_RING_PLIERS_HEAD, HOT_SNAP_RING_PLIERS_HEAD, CAST_SNAP_RING_PLIERS_HEAD_MOLD),
            new Form(SNAP_RING_PLIERS_HANDLE, HOT_SNAP_RING_PLIERS_HANDLE, CAST_SNAP_RING_PLIERS_HANDLE_MOLD),
            new Form(BEARING_PRESS_HEAD, HOT_BEARING_PRESS_HEAD, CAST_BEARING_PRESS_HEAD_MOLD),
            new Form(BEARING_PRESS_HANDLE, HOT_BEARING_PRESS_HANDLE, CAST_BEARING_PRESS_HANDLE_MOLD),
            new Form(CLAMP_HEAD, HOT_CLAMP_HEAD, CAST_CLAMP_HEAD_MOLD),
            new Form(CLAMP_HANDLE, HOT_CLAMP_HANDLE, CAST_CLAMP_HANDLE_MOLD),
            new Form(CRIMPING_TOOL_HEAD, HOT_CRIMPING_TOOL_HEAD, CAST_CRIMPING_TOOL_HEAD_MOLD),
            new Form(CRIMPING_TOOL_HANDLE, HOT_CRIMPING_TOOL_HANDLE, CAST_CRIMPING_TOOL_HANDLE_MOLD),
            new Form(GEAR_CUTTER_HEAD, HOT_GEAR_CUTTER_HEAD, CAST_GEAR_CUTTER_HEAD_MOLD),
            new Form(GEAR_CUTTER_HANDLE, HOT_GEAR_CUTTER_HANDLE, CAST_GEAR_CUTTER_HANDLE_MOLD),

            new Form(TINY_BALL, HOT_TINY_BALL, CAST_TINY_BALL_MOLD),
            new Form(SMALL_BALL, HOT_SMALL_BALL, CAST_SMALL_BALL_MOLD),
            new Form(BALL, HOT_BALL, CAST_BEARING_BALL_MOLD),
            new Form(LARGE_BALL, HOT_LARGE_BALL, CAST_LARGE_BALL_MOLD),
            new Form(HUGE_BALL, HOT_HUGE_BALL, CAST_HUGE_BALL_MOLD),
            new Form(TINY_GEAR, HOT_TINY_GEAR, CAST_TINY_GEAR_MOLD),
            new Form(SMALL_GEAR, HOT_SMALL_GEAR, CAST_SMALL_GEAR_MOLD),
            new Form(GEAR, HOT_GEAR, CAST_GEAR_MOLD),
            new Form(LARGE_GEAR, HOT_LARGE_GEAR, CAST_LARGE_GEAR_MOLD),
            new Form(HUGE_GEAR, HOT_HUGE_GEAR, CAST_HUGE_GEAR_MOLD),
            new Form(TINY_RING, HOT_TINY_RING, CAST_TINY_RING_MOLD),
            new Form(SMALL_RING, HOT_SMALL_RING, CAST_SMALL_RING_MOLD),
            new Form(RING, HOT_RING, CAST_RING_MOLD),
            new Form(LARGE_RING, HOT_LARGE_RING, CAST_LARGE_RING_MOLD),
            new Form(HUGE_RING, HOT_HUGE_RING, CAST_HUGE_RING_MOLD),
            new Form(TINY_RIVET, HOT_TINY_RIVET, CAST_TINY_RIVET_MOLD),
            new Form(SMALL_RIVET, HOT_SMALL_RIVET, CAST_SMALL_RIVET_MOLD),
            new Form(RIVET, HOT_RIVET, CAST_RIVET_MOLD),
            new Form(LARGE_RIVET, HOT_LARGE_RIVET, CAST_LARGE_RIVET_MOLD),
            new Form(HUGE_RIVET, HOT_HUGE_RIVET, CAST_HUGE_RIVET_MOLD),
            new Form(TINY_ROTOR, HOT_TINY_ROTOR, CAST_TINY_ROTOR_MOLD),
            new Form(SMALL_ROTOR, HOT_SMALL_ROTOR, CAST_SMALL_ROTOR_MOLD),
            new Form(ROTOR, HOT_ROTOR, CAST_ROTOR_MOLD),
            new Form(LARGE_ROTOR, HOT_LARGE_ROTOR, CAST_LARGE_ROTOR_MOLD),
            new Form(HUGE_ROTOR, HOT_HUGE_ROTOR, CAST_HUGE_ROTOR_MOLD),
            new Form(INGOT, HOT_INGOT, CAST_INGOT_MOLD),
            new Form(NUGGET, HOT_NUGGET, CAST_NUGGET_MOLD),
            new Form(PLATE, HOT_PLATE, CAST_PLATE_MOLD),
            new Form(LARGE_PLATE, HOT_LARGE_PLATE, CAST_LARGE_PLATE_MOLD),
            new Form(VERY_SHORT_ROD, HOT_VERY_SHORT_ROD, CAST_VERY_SHORT_ROD_MOLD),
            new Form(SHORT_ROD, HOT_SHORT_ROD, CAST_SHORT_ROD_MOLD),
            new Form(ROD, HOT_ROD, CAST_ROD_MOLD),
            new Form(LONG_ROD, HOT_LONG_ROD, CAST_LONG_ROD_MOLD),
            new Form(VERY_LONG_ROD, HOT_VERY_LONG_ROD, CAST_VERY_LONG_ROD_MOLD),
            new Form(TOOL_HEAD_AXE, HOT_TOOL_HEAD_AXE, CAST_TOOL_HEAD_AXE_MOLD),
            new Form(BUZZ_SAW, HOT_BUZZ_SAW, CAST_BUZZ_SAW_MOLD),
            new Form(TOOL_HEAD_CHAINSAW, HOT_TOOL_HEAD_CHAINSAW, CAST_TOOL_HEAD_CHAINSAW_MOLD),
            new Form(TOOL_HEAD_CHISEL, HOT_TOOL_HEAD_CHISEL, CAST_TOOL_HEAD_CHISEL_MOLD),
            new Form(TOOL_HEAD_CROWBAR, HOT_TOOL_HEAD_CROWBAR, CAST_TOOL_HEAD_CROWBAR_MOLD),
            new Form(TOOL_HEAD_DRILL, HOT_TOOL_HEAD_DRILL, CAST_TOOL_HEAD_DRILL_MOLD),
            new Form(TOOL_HEAD_FILE, HOT_TOOL_HEAD_FILE, CAST_TOOL_HEAD_FILE_MOLD),
            new Form(TOOL_HEAD_HAMMER, HOT_TOOL_HEAD_HAMMER, CAST_TOOL_HEAD_HAMMER_MOLD),
            new Form(TOOL_HEAD_HOE, HOT_TOOL_HEAD_HOE, CAST_TOOL_HEAD_HOE_MOLD),
            new Form(TOOL_HEAD_PICKAXE, HOT_TOOL_HEAD_PICKAXE, CAST_TOOL_HEAD_PICKAXE_MOLD),
            new Form(TOOL_HEAD_SCREWDRIVER, HOT_TOOL_HEAD_SCREWDRIVER, CAST_TOOL_HEAD_SCREWDRIVER_MOLD),
            new Form(TOOL_HEAD_SHOVEL, HOT_TOOL_HEAD_SHOVEL, CAST_TOOL_HEAD_SHOVEL_MOLD),
            new Form(TOOL_HEAD_WIRE_CUTTER, HOT_TOOL_HEAD_WIRE_CUTTER, CAST_TOOL_HEAD_WIRE_CUTTER_MOLD),
            new Form(WRENCH, HOT_WRENCH, CAST_WRENCH_MOLD),
            new Form(SWORD_BLADE, HOT_SWORD_BLADE, CAST_SWORD_BLADE_MOLD),
            new Form(KNIFE_BLADE, HOT_KNIFE_BLADE, CAST_KNIFE_BLADE_MOLD),
            new Form(SAW_BLADE, HOT_SAW_BLADE, CAST_SAW_BLADE_MOLD),
            new Form(SAW_HANDLE, HOT_SAW_HANDLE, CAST_SAW_HANDLE_MOLD),
            new Form(WIRE_CUTTER_BODY, HOT_WIRE_CUTTER_BODY, CAST_WIRE_CUTTER_BODY_MOLD),
            new Form(TOOL_HANDLE, HOT_TOOL_HANDLE, CAST_TOOL_HANDLE_MOLD),
            new Form(MIXING_BLADE, HOT_MIXING_BLADE, CAST_MIXING_BLADE_MOLD),
            new Form(IMPELLER, HOT_IMPELLER, CAST_IMPELLER_MOLD),
            new Form(CRANK, HOT_CRANK, CAST_CRANK_MOLD),
            new Form(CONNECTING_ROD, HOT_CONNECTING_ROD, CAST_CONNECTING_ROD_MOLD),
            new Form(PRESS_HEAD, HOT_PRESS_HEAD, CAST_PRESS_HEAD_MOLD),
            new Form(GUIDE_RAIL, HOT_GUIDE_RAIL, CAST_GUIDE_RAIL_MOLD),
            new Form(CHUCK_JAW, HOT_CHUCK_JAW, CAST_CHUCK_JAW_MOLD),
            new Form(CUTTING_INSERT, HOT_CUTTING_INSERT, CAST_CUTTING_INSERT_MOLD),
            new Form(CRUSHING_SEGMENT, HOT_CRUSHING_SEGMENT, CAST_CRUSHING_SEGMENT_MOLD),
            new Form(CYLINDER, HOT_CYLINDER, CAST_CYLINDER_MOLD),
            new Form(PISTON, HOT_PISTON, CAST_PISTON_MOLD),
            new Form(PISTON_RING, HOT_PISTON_RING, CAST_PISTON_RING_MOLD),
            new Form(VALVE_BODY, HOT_VALVE_BODY, CAST_VALVE_BODY_MOLD),
            new Form(VALVE_STEM, HOT_VALVE_STEM, CAST_VALVE_STEM_MOLD),
            new Form(VALVE_SEAT, HOT_VALVE_SEAT, CAST_VALVE_SEAT_MOLD),
            new Form(SPRING, HOT_SPRING, CAST_SPRING_MOLD),
            new Form(FLANGE, HOT_FLANGE, CAST_FLANGE_MOLD),
            new Form(EXTRUSION_DIE, HOT_EXTRUSION_DIE, CAST_EXTRUSION_DIE_MOLD)
    );

    // Kept as an explicit extension point for future molds that have artwork but no gameplay volume yet.
    public static final List<PendingMold> PENDING_MOLDS = List.of();
    private CastingDefinitions() {}
    /** Dedicated cast-mold artwork paths. Forms without artwork use the empty-mold model fallback. */
    public static String moldTexture(Form form) {
        return moldTexture(form.cold());
    }

    public static String moldTexture(PendingMold mold) {
        return moldTexture(mold.cold());
    }

    public static String moldTexture(MaterialPart cold) {
        String path = switch (cold) {
            case MIXING_BLADE -> "machine_parts/mixing_blade/mold";
            case IMPELLER -> "machine_parts/impeller/mold";
            case CRANK -> "machine_parts/crank/mold";
            case CONNECTING_ROD -> "machine_parts/connecting_rod/mold";
            case PRESS_HEAD -> "machine_parts/press_head/mold";
            case GUIDE_RAIL -> "machine_parts/guide_rail/mold";
            case CHUCK_JAW -> "machine_parts/chuck_jaw/mold";
            case CUTTING_INSERT -> "machine_parts/cutting_insert/mold";
            case CRUSHING_SEGMENT -> "machine_parts/crushing_segment/mold";
            case CYLINDER -> "machine_parts/cylinder/mold";
            case PISTON -> "machine_parts/piston/mold";
            case PISTON_RING -> "machine_parts/piston_ring/mold";
            case VALVE_BODY -> "machine_parts/valve_body/mold";
            case VALVE_STEM -> "machine_parts/valve_stem/mold";
            case VALVE_SEAT -> "machine_parts/valve_seat/mold";
            case SPRING -> "spring/normal/mold";
            case FLANGE -> "machine_parts/flange/mold";
            case EXTRUSION_DIE -> "machine_parts/extrusion_die/mold";
            case SHEARS_HEAD -> "shears/head/mold";
            case SHEARS_HANDLE -> "shears/handle/mold";
            case CROSSBOW_LIMBS -> "crossbow/limbs/mold";
            case CROSSBOW_TRIGGER -> "crossbow/trigger/mold";
            case FISHING_HOOK -> "fishing_rod/hook/mold";
            case SHIELD_BODY -> "shield/body/mold";
            case SHIELD_HANDLE -> "shield/handle/mold";
            case HELMET_SHELL -> "armour/helmet/mold";
            case CHESTPLATE_SHELL -> "armour/chestplate/mold";
            case LEGGINGS_SHELL -> "armour/leggings/mold";
            case BOOTS_SHELL -> "armour/boots/mold";
            case SNAP_RING_PLIERS_HEAD -> "snap_ring_pliers/head/mold";
            case SNAP_RING_PLIERS_HANDLE -> "snap_ring_pliers/handle/mold";
            case BEARING_PRESS_HEAD -> "bearing_press/head/mold";
            case BEARING_PRESS_HANDLE -> "bearing_press/handle/mold";
            case CLAMP_HEAD -> "clamp/head/mold";
            case CLAMP_HANDLE -> "clamp/handle/mold";
            case CRIMPING_TOOL_HEAD -> "crimping_tool/head/mold";
            case CRIMPING_TOOL_HANDLE -> "crimping_tool/handle/mold";
            case GEAR_CUTTER_HEAD -> "gear_cutter/head/mold";
            case GEAR_CUTTER_HANDLE -> "gear_cutter/handle/mold";

            case LARGE_BALL -> "ball/large/mold";
            case HUGE_BALL -> "ball/huge/mold";
            case TINY_GEAR -> "gear/tiny/mold";
            case HUGE_GEAR -> "gear/huge/mold";
            case TINY_RING -> "ring/tiny/mold";
            case HUGE_RING -> "ring/huge/mold";
            case TINY_RIVET -> "rivet/tiny/mold";
            case SMALL_RIVET -> "rivet/small/mold";
            case LARGE_RIVET -> "rivet/large/mold";
            case HUGE_RIVET -> "rivet/huge/mold";
            case TINY_ROTOR -> "rotor/tiny/mold";
            case SMALL_ROTOR -> "rotor/small/mold";
            case ROTOR -> "rotor/normal/mold";
            case HUGE_ROTOR -> "rotor/huge/mold";
            case LARGE_PLATE -> "plates/plate/large/mold";
            case VERY_SHORT_ROD -> "rod/very_short/mold";
            case SHORT_ROD -> "rod/short/mold";
            case ROD -> "rod/normal/mold";
            case TINY_BALL -> "ball/tiny/mold";
            case SMALL_BALL -> "ball/small/mold";
            case BALL -> "ball/normal/mold";
            case SMALL_GEAR -> "gear/small/mold";
            case GEAR -> "gear/normal/mold";
            case LARGE_GEAR -> "gear/large/mold";
            case RIVET -> "rivet/normal/mold";
            case INGOT -> "ingots/ingot/mold";
            case NUGGET -> "nuggets/nugget/mold";
            case PLATE -> "plates/plate/normal/mold";
            case SMALL_RING -> "ring/small/mold";
            case RING -> "ring/normal/mold";
            case LARGE_RING -> "ring/large/mold";
            case LONG_ROD -> "rod/long/mold";
            case VERY_LONG_ROD -> "rod/very_long/mold";
            case LARGE_ROTOR -> "rotor/large/mold";
            case TOOL_HEAD_AXE -> "axe/head/mold";
            case BUZZ_SAW -> "buzz_saw/mold";
            case TOOL_HEAD_CHAINSAW -> "chainsaw/head/mold";
            case TOOL_HEAD_CHISEL -> "chisel/head/mold";
            case TOOL_HEAD_CROWBAR -> "crowbar/head/mold";
            case TOOL_HEAD_DRILL -> "drill/head/mold";
            case TOOL_HEAD_FILE -> "file/head/mold";
            case TOOL_HEAD_HAMMER -> "hammer/head/mold";
            case TOOL_HEAD_HOE -> "hoe/head/mold";
            case TOOL_HEAD_PICKAXE -> "pickaxe/head/mold";
            case TOOL_HEAD_SCREWDRIVER -> "screwdriver/head/mold";
            case TOOL_HEAD_SHOVEL -> "shovel/head/mold";
            case TOOL_HEAD_WIRE_CUTTER -> "wirecutter/head/mold";
            case WRENCH -> "wrench/mold";
            case SWORD_BLADE -> "sword/head/mold";
            case KNIFE_BLADE -> "knife/head/mold";
            case SAW_BLADE -> "saw/head/mold";
            case SAW_HANDLE -> "saw/handle/mold";
            case WIRE_CUTTER_BODY -> "wirecutter/body/mold";
            case TOOL_HANDLE -> "tool_handle/mold";
            default -> null;
        };
        if (path == null) return null;
        boolean tool = switch (cold) {
            case SHEARS_HEAD, SHEARS_HANDLE, CROSSBOW_LIMBS, CROSSBOW_TRIGGER, FISHING_HOOK, SHIELD_BODY, SHIELD_HANDLE, HELMET_SHELL, CHESTPLATE_SHELL, LEGGINGS_SHELL, BOOTS_SHELL, SNAP_RING_PLIERS_HEAD, SNAP_RING_PLIERS_HANDLE, BEARING_PRESS_HEAD, BEARING_PRESS_HANDLE, CLAMP_HEAD, CLAMP_HANDLE, CRIMPING_TOOL_HEAD, CRIMPING_TOOL_HANDLE, GEAR_CUTTER_HEAD, GEAR_CUTTER_HANDLE -> true;
            case TOOL_HEAD_AXE, TOOL_HEAD_CHAINSAW, TOOL_HEAD_CHISEL, TOOL_HEAD_CROWBAR,
                    TOOL_HEAD_DRILL, TOOL_HEAD_FILE, TOOL_HEAD_HAMMER, TOOL_HEAD_HOE,
                    TOOL_HEAD_PICKAXE, TOOL_HEAD_SCREWDRIVER, TOOL_HEAD_SHOVEL,
                    TOOL_HEAD_WIRE_CUTTER, WRENCH, KNIFE_BLADE, SAW_BLADE, TOOL_HANDLE,
                    SAW_HANDLE, WIRE_CUTTER_BODY -> true;
            default -> false;
        };
        return "industron:item/" + (tool ? "tool/" : "material_sets/") + path;
    }

    public static Form cold(MaterialPart part) {
        return ALL.stream().filter(form -> form.cold() == part || form.hot() == part).findFirst().orElse(null);
    }
    public static Form mold(MaterialPart part) {
        return ALL.stream().filter(form -> form.mold() == part).findFirst().orElse(null);
    }
    public static Set<MaterialPart> molds() {
        EnumSet<MaterialPart> result = EnumSet.noneOf(MaterialPart.class);
        ALL.forEach(form -> result.add(form.mold()));
        PENDING_MOLDS.forEach(mold -> result.add(mold.mold()));
        return result;
    }
    public static void addMetalForms(Set<MaterialPart> parts) {
        ALL.forEach(form -> { parts.add(form.cold()); parts.add(form.hot()); });
    }
}
