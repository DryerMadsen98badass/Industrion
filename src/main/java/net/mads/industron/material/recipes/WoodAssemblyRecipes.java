package net.mads.industron.material.recipes;

import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.defenitions.WoodMaterials;
import net.mads.industron.material.structure.WoodMaterial;
import net.mads.industron.material.plant.PlantPart;
import net.mads.industron.recipe.recipes.assembly.Component;
import net.mads.industron.recipe.recipes.assembly.Tool;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyRecipeDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * Manual wood-working progression generated from WoodMaterial forms.
 *
 * <p>Every recipe is emitted only when the wood exposes every required form. Recipes sharing
 * one physical base deliberately have distinct first interactions so the server can select the
 * intended route immediately without guessing.</p>
 */
public final class WoodAssemblyRecipes {
    public static final List<AssemblyRecipeDefinition> ALL = buildAll();

    private WoodAssemblyRecipes() {
    }

    private static List<AssemblyRecipeDefinition> buildAll() {
        List<AssemblyRecipeDefinition> result = new ArrayList<>();
        for (WoodMaterial wood : WoodMaterials.ALL) {
            addWood(result, wood);
        }
        return List.copyOf(result);
    }

    private static void addEquipmentWoodParts(List<AssemblyRecipeDefinition> out, WoodMaterial wood) {
        MaterialPart[] parts={MaterialPart.SHEARS_HANDLE, MaterialPart.CROSSBOW_LIMBS, MaterialPart.SHIELD_BODY, MaterialPart.SHIELD_HANDLE, MaterialPart.SNAP_RING_PLIERS_HANDLE, MaterialPart.BEARING_PRESS_HANDLE, MaterialPart.CLAMP_HANDLE, MaterialPart.CRIMPING_TOOL_HANDLE, MaterialPart.GEAR_CUTTER_HANDLE, MaterialPart.BOW_BODY, MaterialPart.CROSSBOW_STOCK, MaterialPart.FISHING_ROD_BODY};
        for (int i=0;i<parts.length;i++) {
            MaterialPart part=parts[i];
            if (!has(wood,MaterialPart.PLANKS,part)) continue;
            var recipe=AssemblyRecipeDefinition.recipe(path(wood,part.id())).level(1)
                .baseBlockInput(id(wood,MaterialPart.PLANKS));
            // Larger pieces require extra wood before shaping.
            if (part==MaterialPart.SHIELD_BODY) recipe.input(id(wood,MaterialPart.PLANKS),3);
            recipe.tool(Tool.CHISEL,1+i).tool(Tool.SAW,2)
                .baseItemOutput(id(wood,part));
            out.add(recipe.build());
        }
    }

    private static void addWood(List<AssemblyRecipeDefinition> out, WoodMaterial wood) {
        addEquipmentWoodParts(out, wood);
        String bark = id(wood, MaterialPart.BARK);
        String pulp = id(wood, MaterialPart.WOOD_PULP);
        String stick = id(wood, MaterialPart.STICK);
        String handle = id(wood, MaterialPart.TOOL_HANDLE);
        String malletHead = id(wood, MaterialPart.TOOL_HEAD_MALLET);
        String sawHandle = id(wood, MaterialPart.SAW_HANDLE);
        String sifterFrame = id(wood, MaterialPart.SIFTER_FRAME);

        // 1: bark removal: four knife operations, one per barked side.
        if (has(wood, MaterialPart.LOG, MaterialPart.STRIPPED_LOG, MaterialPart.BARK)) {
            out.add(AssemblyRecipeDefinition.recipe(path(wood, "strip_log")).level(1)
                    .baseBlockInput(id(wood, MaterialPart.LOG))
                    .tool(Tool.KNIFE, 4)
                    .baseBlockOutput(id(wood, MaterialPart.STRIPPED_LOG))
                    .byproduct(bark, 4)
                    .build());
        }

        // 2: convert a four-sided log to six-sided wood by fastening bark over both end faces.
        if (has(wood, MaterialPart.LOG, MaterialPart.WOOD, MaterialPart.BARK)) {
            out.add(AssemblyRecipeDefinition.recipe(path(wood, "log_to_wood")).level(1)
                    .baseBlockInput(id(wood, MaterialPart.LOG))
                    .input(WoodAssemblyComponents.barkFastening(wood), 2)
                    .baseBlockOutput(id(wood, MaterialPart.WOOD))
                    .build());
        }

        // 3: six barked faces -> six knife operations -> 6 bark pieces.
        if (has(wood, MaterialPart.WOOD, MaterialPart.STRIPPED_WOOD, MaterialPart.BARK)) {
            out.add(AssemblyRecipeDefinition.recipe(path(wood, "strip_wood")).level(1)
                    .baseBlockInput(id(wood, MaterialPart.WOOD))
                    .tool(Tool.KNIFE, 6)
                    .baseBlockOutput(id(wood, MaterialPart.STRIPPED_WOOD))
                    .byproduct(bark, 6)
                    .build());
        }

        // 4: crude axe splitting may fail to recover a usable plank; the removed wood still becomes pulp.
        if (has(wood, MaterialPart.STRIPPED_LOG, MaterialPart.PLANKS, MaterialPart.WOOD_PULP)) {
            out.add(AssemblyRecipeDefinition.recipe(path(wood, "split_plank_axe")).level(1)
                    .baseBlockInput(id(wood, MaterialPart.STRIPPED_LOG))
                    .tool(Tool.AXE, 1)
                    .baseBlockOutput(id(wood, MaterialPart.PLANKS))
                    .chance(80.0D)
                    .byproduct(pulp, 1)
                    .build());
        }

        // 5: precise log sawing always recovers one plank.
        if (has(wood, MaterialPart.STRIPPED_LOG, MaterialPart.PLANKS, MaterialPart.WOOD_PULP)) {
            out.add(AssemblyRecipeDefinition.recipe(path(wood, "saw_plank_log")).level(1)
                    .baseBlockInput(id(wood, MaterialPart.STRIPPED_LOG))
                    .tool(Tool.SAW, 1)
                    .baseBlockOutput(id(wood, MaterialPart.PLANKS))
                    .byproduct(pulp, 1)
                    .build());
        }

        // 6: stripped six-sided wood is also cut down to one plank per operation.
        if (has(wood, MaterialPart.STRIPPED_WOOD, MaterialPart.PLANKS, MaterialPart.WOOD_PULP)) {
            out.add(AssemblyRecipeDefinition.recipe(path(wood, "saw_plank_wood")).level(1)
                    .baseBlockInput(id(wood, MaterialPart.STRIPPED_WOOD))
                    .tool(Tool.SAW, 1)
                    .baseBlockOutput(id(wood, MaterialPart.PLANKS))
                    .byproduct(pulp, 1)
                    .build());
        }

        // 7-8: all wood families use the same physical cut logic for slab/stair forms.
        addCutShapes(out, wood, MaterialPart.PLANKS, MaterialPart.SLAB, MaterialPart.STAIRS, "", pulp);
        // Bamboo Mosaic exposes its own slab/stair family. It deliberately reuses the exact same
        // generic wood-cut process instead of introducing Bamboo-specific recipe semantics.
        addCutShapes(out, wood, MaterialPart.MOSAIC, MaterialPart.MOSAIC_SLAB, MaterialPart.MOSAIC_STAIRS, "mosaic_", pulp);

        // 9: a rough shaft can be hewn by hand. Both shaft routes intentionally share the
        // four Knife uses; Assembly keeps both candidates alive until Axe vs Saw selects the branch.
        // Axe work is less precise, so only the shaft output has an 80% recovery chance.
        if (has(wood, MaterialPart.PLANKS, MaterialPart.SHAFT, MaterialPart.WOOD_PULP)) {
            out.add(AssemblyRecipeDefinition.recipe(path(wood, "shaft_axe")).level(1)
                    .baseBlockInput(id(wood, MaterialPart.PLANKS))
                    .tool(Tool.KNIFE, 4)
                    .tool(Tool.AXE, 4)
                    .baseBlockOutput(id(wood, MaterialPart.SHAFT))
                    .chance(80.0D)
                    .byproduct(pulp, 1)
                    .build());

            // Saw finishing is the precise manual route and always recovers the shaft.
            // Keep the agreed total work at 4 Knife + 4 Saw uses, but split the Knife work
            // so this route diverges before the existing KNIFE x4 -> SAW x2 stairs recipe completes.
            out.add(AssemblyRecipeDefinition.recipe(path(wood, "shaft_saw")).level(1)
                    .baseBlockInput(id(wood, MaterialPart.PLANKS))
                    .tool(Tool.KNIFE, 3)
                    .tool(Tool.SAW, 4)
                    .tool(Tool.KNIFE, 1)
                    .baseBlockOutput(id(wood, MaterialPart.SHAFT))
                    .byproduct(pulp, 1)
                    .build());
        }

        // 10: knife-first branches this route away from the one-saw slab recipe before either can complete.
        // The total work is unchanged: one Knife use + two Saw uses -> sixteen sticks + pulp.
        if (has(wood, MaterialPart.PLANKS, MaterialPart.STICK, MaterialPart.WOOD_PULP)) {
            out.add(AssemblyRecipeDefinition.recipe(path(wood, "sticks")).level(1)
                    .baseBlockInput(id(wood, MaterialPart.PLANKS))
                    .tool(Tool.KNIFE, 1)
                    .tool(Tool.SAW, 2)
                    .baseItemOutput(stick, 16)
                    .byproduct(pulp, 1)
                    .build());
        }

        // 10: slab base avoids colliding with the plank + knife stair route.
        if (has(wood, MaterialPart.SLAB, MaterialPart.BUTTON, MaterialPart.WOOD_PULP)) {
            out.add(AssemblyRecipeDefinition.recipe(path(wood, "button")).level(1)
                    .baseBlockInput(id(wood, MaterialPart.SLAB))
                    .tool(Tool.KNIFE, 1)
                    .baseBlockOutput(id(wood, MaterialPart.BUTTON))
                    .byproduct(pulp, 1)
                    .build());
        }

        // 11
        if (has(wood, MaterialPart.SLAB, MaterialPart.PRESSURE_PLATE)) {
            out.add(AssemblyRecipeDefinition.recipe(path(wood, "pressure_plate")).level(1)
                    .baseBlockInput(id(wood, MaterialPart.SLAB))
                    .tool(Tool.MALLET, 1)
                    .baseBlockOutput(id(wood, MaterialPart.PRESSURE_PLATE))
                    .build());
        }

        // 12: mallet-first is intentional so this does not ambiguously start like the sign route.
        if (has(wood, MaterialPart.PLANKS, MaterialPart.STICK, MaterialPart.FENCE)) {
            out.add(AssemblyRecipeDefinition.recipe(path(wood, "fence")).level(1)
                    .baseBlockInput(id(wood, MaterialPart.PLANKS))
                    .tool(Tool.MALLET, 1)
                    .input(stick, 2)
                    .baseBlockOutput(id(wood, MaterialPart.FENCE))
                    .build());
        }

        // 13
        if (has(wood, MaterialPart.SLAB, MaterialPart.STICK, MaterialPart.PLANKS, MaterialPart.FENCE_GATE)) {
            out.add(AssemblyRecipeDefinition.recipe(path(wood, "fence_gate")).level(1)
                    .baseBlockInput(id(wood, MaterialPart.SLAB))
                    .input(stick, 4)
                    .input(id(wood, MaterialPart.PLANKS), 2)
                    .tool(Tool.MALLET, 1)
                    .baseBlockOutput(id(wood, MaterialPart.FENCE_GATE))
                    .build());
        }

        // 14: door is returned as an item; placing a raw lower-half DoorBlock would create an invalid world state.
        if (has(wood, MaterialPart.STRIPPED_LOG, MaterialPart.PLANKS, MaterialPart.DOOR, MaterialPart.WOOD_PULP)) {
            out.add(AssemblyRecipeDefinition.recipe(path(wood, "door")).level(1)
                    .baseBlockInput(id(wood, MaterialPart.STRIPPED_LOG))
                    .input(id(wood, MaterialPart.PLANKS), 2)
                    .tool(Tool.SAW, 1)
                    .tool(Tool.MALLET, 1)
                    .baseItemOutput(id(wood, MaterialPart.DOOR))
                    .byproduct(pulp, 1)
                    .build());
        }

        // 15
        if (has(wood, MaterialPart.SLAB, MaterialPart.PLANKS, MaterialPart.TRAPDOOR, MaterialPart.WOOD_PULP)) {
            out.add(AssemblyRecipeDefinition.recipe(path(wood, "trapdoor")).level(1)
                    .baseBlockInput(id(wood, MaterialPart.SLAB))
                    .input(id(wood, MaterialPart.PLANKS))
                    .tool(Tool.SAW, 1)
                    .tool(Tool.MALLET, 1)
                    .baseBlockOutput(id(wood, MaterialPart.TRAPDOOR))
                    .byproduct(pulp, 1)
                    .build());
        }

        // 16: sign is returned as an item. Stick-first distinguishes it from other plank routes.
        if (has(wood, MaterialPart.PLANKS, MaterialPart.STICK, MaterialPart.SIGN, MaterialPart.WOOD_PULP)) {
            out.add(AssemblyRecipeDefinition.recipe(path(wood, "sign")).level(1)
                    .baseBlockInput(id(wood, MaterialPart.PLANKS))
                    .input(stick)
                    .tool(Tool.KNIFE, 1)
                    .baseItemOutput(id(wood, MaterialPart.SIGN))
                    .byproduct(pulp, 1)
                    .build());
        }

        // 17
        if (has(wood, MaterialPart.STRIPPED_WOOD, MaterialPart.HANGING_SIGN, MaterialPart.WOOD_PULP)) {
            out.add(AssemblyRecipeDefinition.recipe(path(wood, "hanging_sign")).level(1)
                    .baseBlockInput(id(wood, MaterialPart.STRIPPED_WOOD))
                    .input("minecraft:chain", 2)
                    .tool(Tool.SAW, 1)
                    .tool(Tool.MALLET, 1)
                    .baseItemOutput(id(wood, MaterialPart.HANGING_SIGN))
                    .byproduct(pulp, 1)
                    .build());
        }

        // PlantPart.STRING is one dynamic ingredient role: runtime accepts every registered
        // plant-derived string while JEI cycles those alternatives in one slot.
        if (has(wood, MaterialPart.PLANKS, MaterialPart.CHEST)) {
            out.add(AssemblyRecipeDefinition.recipe(path(wood, "chest")).level(1)
                    .baseBlockInput(id(wood, MaterialPart.PLANKS))
                    .input(PlantPart.STRING)
                    .tool(Tool.MALLET, 1)
                    .baseBlockOutput(id(wood, MaterialPart.CHEST))
                    .build());
        }

        if (has(wood, MaterialPart.PLANKS, MaterialPart.BARREL)) {
            out.add(AssemblyRecipeDefinition.recipe(path(wood, "barrel")).level(1)
                    .baseBlockInput(id(wood, MaterialPart.PLANKS))
                    .input(PlantPart.STRING, 2)
                    .tool(Tool.MALLET, 2)
                    .baseBlockOutput(id(wood, MaterialPart.BARREL))
                    .build());
        }

        if (has(wood, MaterialPart.PLANKS, MaterialPart.BOOKSHELF)) {
            out.add(AssemblyRecipeDefinition.recipe(path(wood, "bookshelf")).level(1)
                    .baseBlockInput(id(wood, MaterialPart.PLANKS))
                    .input("minecraft:book", 3)
                    .input(PlantPart.STRING)
                    .tool(Tool.MALLET, 1)
                    .baseBlockOutput(id(wood, MaterialPart.BOOKSHELF))
                    .build());
        }

        // Assembly needs one physical workbench base item. The base stick gets one free
        // PlantPart.STRING input, while the remaining five inputs use Component.STICK.
        // Net cost stays exactly six matching sticks + six valid plant-derived strings.
        if (has(wood, MaterialPart.STICK, MaterialPart.LADDER)) {
            out.add(AssemblyRecipeDefinition.recipe(path(wood, "ladder")).level(1)
                    .baseItemInput(stick)
                    .input(PlantPart.STRING)
                    .input(Component.STICK, wood, 5)
                    .baseItemOutput(id(wood, MaterialPart.LADDER))
                    .build());
        }

        // Boat is assembled in-world and spawns the species-correct entity.
        if (has(wood, MaterialPart.PLANKS, MaterialPart.BOAT)) {
            out.add(AssemblyRecipeDefinition.recipe(path(wood, "boat")).level(1)
                    .baseBlockInput(id(wood, MaterialPart.PLANKS))
                    .input(PlantPart.STRING)
                    .tool(Tool.SAW, 1)
                    .input(PlantPart.STRING)
                    .tool(Tool.MALLET, 2)
                    .baseEntityOutput(WoodAssemblyEntities.boat(wood))
                    .build());
        }

        // Chest Boat continues directly from the physical Boat entity.
        if (has(wood, MaterialPart.BOAT, MaterialPart.CHEST, MaterialPart.CHEST_BOAT)) {
            out.add(AssemblyRecipeDefinition.recipe(path(wood, "chest_boat")).level(1)
                    .baseEntityInput(WoodAssemblyEntities.boat(wood))
                    .input(id(wood, MaterialPart.CHEST))
                    .input(PlantPart.STRING)
                    .tool(Tool.MALLET, 1)
                    .baseEntityOutput(WoodAssemblyEntities.chestBoat(wood))
                    .build());
        }

        // Drying Rack acquisition: one stick is the workbench base; each of the remaining
        // thirteen structural sticks is a Component.STICK = one matching wood stick + one string.
        if (has(wood, MaterialPart.STICK)) {
            out.add(AssemblyRecipeDefinition.recipe(path(wood, "drying_rack")).level(1)
                    .baseItemInput(stick)
                    .input(Component.STICK, wood, 13)
                    .baseBlockOutput("industron:" + wood.id() + "_drying_rack")
                    .build());
        }

        // 18
        if (has(wood, MaterialPart.STICK, MaterialPart.TOOL_HANDLE, MaterialPart.WOOD_PULP)) {
            out.add(AssemblyRecipeDefinition.recipe(path(wood, "tool_handle_knife")).level(1)
                    .baseItemInput(stick)
                    .tool(Tool.KNIFE, 1)
                    .baseItemOutput(handle)
                    .byproduct(pulp, 1)
                    .build());
        }

        // Bootstrap alternate: a handle must be obtainable before the first assembled Knife exists.
        if (has(wood, MaterialPart.STICK, MaterialPart.TOOL_HANDLE)) {
            out.add(AssemblyRecipeDefinition.recipe(path(wood, "tool_handle_pebble")).level(1)
                    .baseItemInput(stick)
                    .tool(Tool.PEBBLE, 1)
                    .baseItemOutput(handle)
                    .build());
        }

        // 19: pebble-first keeps this route distinct from fence/sign while bootstrapping Mallet production.
        if (has(wood, MaterialPart.PLANKS, MaterialPart.STICK, MaterialPart.TOOL_HEAD_MALLET)) {
            out.add(AssemblyRecipeDefinition.recipe(path(wood, "mallet_head")).level(1)
                    .baseBlockInput(id(wood, MaterialPart.PLANKS))
                    .tool(Tool.PEBBLE, 1)
                    .input(stick)
                    .baseItemOutput(malletHead)
                    .build());
        }

        // 20
        if (has(wood, MaterialPart.STICK, MaterialPart.BARK, MaterialPart.SAW_HANDLE)) {
            out.add(AssemblyRecipeDefinition.recipe(path(wood, "saw_handle")).level(1)
                    .baseItemInput(stick)
                    .input(bark)
                    .tool(Tool.KNIFE, 1)
                    .baseItemOutput(sawHandle)
                    .build());
        }

        // 21
        if (has(wood, MaterialPart.STRIPPED_WOOD, MaterialPart.STICK, MaterialPart.SIFTER_FRAME)) {
            out.add(AssemblyRecipeDefinition.recipe(path(wood, "sifter_frame")).level(1)
                    .baseBlockInput(id(wood, MaterialPart.STRIPPED_WOOD))
                    .input(stick, 4)
                    .tool(Tool.MALLET, 1)
                    .baseItemOutput(sifterFrame)
                    .build());
        }

        // 22
        if (has(wood, MaterialPart.PLANKS, MaterialPart.WINDOW)) {
            out.add(AssemblyRecipeDefinition.recipe(path(wood, "window")).level(1)
                    .baseBlockInput(id(wood, MaterialPart.PLANKS))
                    .input("minecraft:glass")
                    .tool(Tool.MALLET, 1)
                    .baseBlockOutput(id(wood, MaterialPart.WINDOW))
                    .build());
        }

        // 23
        if (has(wood, MaterialPart.WINDOW, MaterialPart.WINDOW_PANE, MaterialPart.WOOD_PULP)) {
            out.add(AssemblyRecipeDefinition.recipe(path(wood, "window_pane")).level(1)
                    .baseBlockInput(id(wood, MaterialPart.WINDOW))
                    .tool(Tool.SAW, 1)
                    .baseBlockOutput(id(wood, MaterialPart.WINDOW_PANE))
                    .byproduct(pulp, 1)
                    .build());
        }
    }

    private static void addCutShapes(
            List<AssemblyRecipeDefinition> out,
            WoodMaterial wood,
            MaterialPart base,
            MaterialPart slab,
            MaterialPart stairs,
            String idPrefix,
            String pulp
    ) {
        if (has(wood, base, slab, MaterialPart.WOOD_PULP)) {
            out.add(AssemblyRecipeDefinition.recipe(path(wood, idPrefix + "slabs")).level(1)
                    .baseBlockInput(id(wood, base))
                    .tool(Tool.SAW, 1)
                    .baseBlockOutput(id(wood, slab), 2)
                    .byproduct(pulp, 1)
                    .build());
        }
        if (has(wood, base, stairs, MaterialPart.WOOD_PULP)) {
            out.add(AssemblyRecipeDefinition.recipe(path(wood, idPrefix + "stairs")).level(1)
                    .baseBlockInput(id(wood, base))
                    .tool(Tool.KNIFE, 4)
                    .tool(Tool.SAW, 2)
                    .baseBlockOutput(id(wood, stairs))
                    .byproduct(pulp, 1)
                    .build());
        }
    }

    private static boolean has(WoodMaterial wood, MaterialPart... parts) {
        for (MaterialPart part : parts) {
            if (!WoodRecipeIds.has(wood, part)) return false;
        }
        return true;
    }

    private static String id(WoodMaterial wood, MaterialPart part) {
        return WoodRecipeIds.stringId(wood, part);
    }

    private static String path(WoodMaterial wood, String recipe) {
        return "wood/" + wood.id() + "/" + recipe;
    }
}
