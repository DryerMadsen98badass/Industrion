package net.mads.industron.recipe.recipes.assembly;

import net.mads.industron.material.MaterialPart;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyTools;
import net.mads.industron.recipe.recipetypes.assembly.ToolDefinition;

import java.util.List;

import static net.mads.industron.recipe.recipetypes.assembly.ToolDefinition.tool;

/** Permanent material/part-based tool families. No vanilla test-tool variants live here. */
public final class ToolDefinitions {
    public static final ToolDefinition PICKAXE = tool("pickaxe", "Pickaxe")
            .forgeFormula("y * 2")
            .part("handle", MaterialPart.TOOL_HANDLE, 0)
            .part("head", MaterialPart.TOOL_HEAD_PICKAXE, 1)
            .basePart("handle")
            .strengthReference("head")
            .build();

    public static final ToolDefinition AXE = tool("axe", "Axe")
            .forgeFormula("y * 1.5")
            .part("handle", MaterialPart.TOOL_HANDLE, 0)
            .part("head", MaterialPart.TOOL_HEAD_AXE, 1)
            .basePart("handle")
            .strengthReference("head")
            .build();

    public static final ToolDefinition SWORD = tool("sword", "Sword")
            .part("handle", MaterialPart.TOOL_HANDLE, 0)
            .part("blade", MaterialPart.SWORD_BLADE, 1)
            .basePart("handle").strengthReference("blade").build();

    public static final ToolDefinition KNIFE = tool("knife", "Knife")
            .forgeFormula("y * 1.25")
            .part("handle", MaterialPart.TOOL_HANDLE, 0)
            .part("blade", MaterialPart.KNIFE_BLADE, 1)
            .basePart("handle")
            .strengthReference("blade")
            .build();

    public static final ToolDefinition SHOVEL = tool("shovel", "Shovel")
            .forgeFormula("y + 64")
            .part("handle", MaterialPart.TOOL_HANDLE, 0)
            .part("head", MaterialPart.TOOL_HEAD_SHOVEL, 1)
            .basePart("handle")
            .strengthReference("head")
            .build();

    public static final ToolDefinition HOE = tool("hoe", "Hoe")
            .forgeFormula("y - 64")
            .part("handle", MaterialPart.TOOL_HANDLE, 0)
            .part("head", MaterialPart.TOOL_HEAD_HOE, 1)
            .basePart("handle")
            .strengthReference("head")
            .build();

    public static final ToolDefinition HAMMER = tool("hammer", "Hammer")
            .forgeFormula("y / 2")
            .part("handle", MaterialPart.TOOL_HANDLE, 0)
            .part("head", MaterialPart.TOOL_HEAD_HAMMER, 1)
            .basePart("handle")
            .strengthReference("head")
            .build();


    public static final ToolDefinition SAW = tool("saw", "Saw")
            .forgeFormula("y * 0.5")
            .part("handle", MaterialPart.SAW_HANDLE, 0)
            .part("head", MaterialPart.SAW_BLADE, 1)
            .basePart("handle")
            .strengthReference("head")
            .build();

    public static final ToolDefinition FILE = tool("file", "File")
            .forgeFormula("y * 0.75")
            .part("handle", MaterialPart.TOOL_HANDLE, 0)
            .part("head", MaterialPart.TOOL_HEAD_FILE, 1)
            .basePart("handle")
            .strengthReference("head")
            .build();

    public static final ToolDefinition CHISEL = tool("chisel", "Chisel")
            .forgeFormula("y + 32")
            .part("handle", MaterialPart.TOOL_HANDLE, 0)
            .part("head", MaterialPart.TOOL_HEAD_CHISEL, 1)
            .basePart("handle")
            .strengthReference("head")
            .build();

    public static final ToolDefinition SCREWDRIVER = tool("screwdriver", "Screwdriver")
            .forgeFormula("y - 32")
            .part("handle", MaterialPart.TOOL_HANDLE, 0)
            .part("head", MaterialPart.TOOL_HEAD_SCREWDRIVER, 1)
            .basePart("handle")
            .strengthReference("head")
            .build();

    public static final ToolDefinition CROWBAR = tool("crowbar", "Crowbar")
            .forgeFormula("y + 128")
            .part("handle", MaterialPart.TOOL_HANDLE, 0)
            .part("head", MaterialPart.TOOL_HEAD_CROWBAR, 1)
            .basePart("handle")
            .strengthReference("head")
            .build();

    public static final ToolDefinition WIRE_CUTTER = tool("wire_cutter", "Wire Cutter")
            .forgeFormula("y - 16")
            .part("body", MaterialPart.WIRE_CUTTER_BODY, 0)
            .part("head", MaterialPart.TOOL_HEAD_WIRE_CUTTER, 1)
            .basePart("body")
            .strengthReference("head")
            .build();

    public static final ToolDefinition DRILL = tool("drill", "Drill")
            .forgeFormula("y + 16")
            .part("handle", MaterialPart.TOOL_HANDLE, 0)
            .part("head", MaterialPart.TOOL_HEAD_DRILL, 1)
            .basePart("handle")
            .strengthReference("head")
            // The head exists/casts now, but the completed powered drill is not obtainable yet.
            .finishedToolUnavailable()
            // The drill head template sits left/high on the shared 16x16 composition canvas.
            // Keep the source textures untouched and center only the finished composed item.
            .renderOffsetPixels(2.5F, -0.5F)
            .build();


    public static final ToolDefinition MALLET = tool("mallet", "Mallet")
            .forgeFormula("y / 1.5")
            .part("handle", MaterialPart.TOOL_HANDLE, 0)
            .part("head", MaterialPart.TOOL_HEAD_MALLET, 1)
            .basePart("handle")
            .strengthReference("head")
            .build();

    public static final ToolDefinition PESTLE = tool("pestle", "Pestle")
            .part("handle", MaterialPart.TOOL_HANDLE, 0)
            .part("head", MaterialPart.TOOL_HEAD_PESTLE, 1)
            .basePart("handle")
            .strengthReference("head")
            .build();

    public static final ToolDefinition SIFTER = tool("sifter", "Sifter")
            .part("frame", MaterialPart.SIFTER_FRAME, 0)
            .fixedPart("mesh", "industron:sifter_mesh", 1)
            .basePart("frame")
            .strengthReference("frame")
            .build();

    public static final ToolDefinition WRENCH = tool("wrench", "Wrench")
            .forgeFormula("y + 48")
            .part("head", MaterialPart.WRENCH, 0)
            .directPartTool()
            .build();

    /** Primitive striking tool: every stone pebble is always LV and takes exactly 1 second per use. */
    public static final ToolDefinition PEBBLE = tool("pebble", "Pebble")
            .part("pebble", MaterialPart.PEBBLE, 0)
            .directPartTool()
            .build();

    public static final ToolDefinition SHEARS = tool("shears", "Shears")
            .forgeFormula("y + 24")
            .part("handle", MaterialPart.SHEARS_HANDLE, 0)
            .part("head", MaterialPart.SHEARS_HEAD, 1)
            .basePart("handle").strengthReference("head").build();

    public static final ToolDefinition SNAP_RING_PLIERS = tool("snap_ring_pliers", "Snap Ring Pliers")
            .forgeFormula("y - 24")
            .part("handle", MaterialPart.SNAP_RING_PLIERS_HANDLE, 0)
            .part("head", MaterialPart.SNAP_RING_PLIERS_HEAD, 1)
            .basePart("handle").strengthReference("head").build();

    public static final ToolDefinition BEARING_PRESS = tool("bearing_press", "Bearing Press")
            .forgeFormula("y * 0.8")
            .part("handle", MaterialPart.BEARING_PRESS_HANDLE, 0)
            .part("head", MaterialPart.BEARING_PRESS_HEAD, 1)
            .basePart("handle").strengthReference("head").build();

    public static final ToolDefinition CLAMP = tool("clamp", "Clamp")
            .forgeFormula("y + 8")
            .part("handle", MaterialPart.CLAMP_HANDLE, 0)
            .part("head", MaterialPart.CLAMP_HEAD, 1)
            .basePart("handle").strengthReference("head").build();

    public static final ToolDefinition CRIMPING_TOOL = tool("crimping_tool", "Crimping Tool")
            .forgeFormula("y - 8")
            .part("handle", MaterialPart.CRIMPING_TOOL_HANDLE, 0)
            .part("head", MaterialPart.CRIMPING_TOOL_HEAD, 1)
            .basePart("handle").strengthReference("head").build();

    public static final ToolDefinition GEAR_CUTTER = tool("gear_cutter", "Gear Cutter")
            .forgeFormula("y / 1.25")
            .part("handle", MaterialPart.GEAR_CUTTER_HANDLE, 0)
            .part("head", MaterialPart.GEAR_CUTTER_HEAD, 1)
            .basePart("handle").strengthReference("head").build();

    public static final ToolDefinition BOW = tool("bow", "Bow")
            .part("body", MaterialPart.BOW_BODY, 0).fixedPart("string", "industron:bow_string", 1).basePart("body").strengthReference("body").build();
    public static final ToolDefinition CROSSBOW = tool("crossbow", "Crossbow")
            .part("stock", MaterialPart.CROSSBOW_STOCK, 0).part("limbs", MaterialPart.CROSSBOW_LIMBS, 1)
            .part("trigger", MaterialPart.CROSSBOW_TRIGGER, 2).fixedPart("string", "industron:crossbow_string", 3).basePart("stock").strengthReference("limbs").build();
    public static final ToolDefinition SHIELD = tool("shield", "Shield")
            .part("handle", MaterialPart.SHIELD_HANDLE, 0).part("body", MaterialPart.SHIELD_BODY, 1)
            .basePart("body").strengthReference("body").build();
    public static final ToolDefinition FISHING_ROD = tool("fishing_rod", "Fishing Rod")
            .part("body", MaterialPart.FISHING_ROD_BODY, 0).part("hook", MaterialPart.FISHING_HOOK, 1).fixedPart("line", "industron:fishing_line", 2)
            .basePart("body").strengthReference("body").build();
    public static final ToolDefinition HELMET = tool("helmet", "Helmet")
            .part("shell", MaterialPart.HELMET_SHELL, 0).basePart("shell").strengthReference("shell").build();
    public static final ToolDefinition CHESTPLATE = tool("chestplate", "Chestplate")
            .part("shell", MaterialPart.CHESTPLATE_SHELL, 0).basePart("shell").strengthReference("shell").build();
    public static final ToolDefinition LEGGINGS = tool("leggings", "Leggings")
            .part("shell", MaterialPart.LEGGINGS_SHELL, 0).basePart("shell").strengthReference("shell").build();
    public static final ToolDefinition BOOTS = tool("boots", "Boots")
            .part("shell", MaterialPart.BOOTS_SHELL, 0).basePart("shell").strengthReference("shell").build();

    public static final List<ToolDefinition> ALL = List.of(
            BOW, CROSSBOW, SHIELD, FISHING_ROD, HELMET, CHESTPLATE, LEGGINGS, BOOTS,
            SHEARS,
            SNAP_RING_PLIERS,
            BEARING_PRESS,
            CLAMP,
            CRIMPING_TOOL,
            GEAR_CUTTER,
            PICKAXE,
            AXE,
            KNIFE,
            SWORD,
            SHOVEL,
            HOE,
            HAMMER,
            SAW,
            FILE,
            CHISEL,
            SCREWDRIVER,
            CROWBAR,
            WIRE_CUTTER,
            DRILL,
            MALLET,
            PESTLE,
            SIFTER,
            WRENCH,
            PEBBLE
    );

    private static boolean registered;

    private ToolDefinitions() {
    }

    public static void bootstrap() {
        if (registered) return;
        registered = true;
        ALL.stream()
                .filter(ToolDefinition::isFinishedToolEnabled)
                .forEach(AssemblyTools::register);
    }
}
