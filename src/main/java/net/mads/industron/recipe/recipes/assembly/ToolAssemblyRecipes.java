package net.mads.industron.recipe.recipes.assembly;

import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.plant.PlantPart;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyMaterialSelector;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyRecipeDefinition;
import net.mads.industron.recipe.recipetypes.assembly.ToolDefinition;
import net.mads.industron.tool.ToolMaterialRules;

import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic Assembly recipes for the material/part based tool system.
 *
 * <p>These recipes capture the material identity of each permanent tool part. Wood bases use
 * string, while metal bases use rivets. Rivets are ordinary Assembly material inputs and each
 * rivet must individually meet the working part's fastener-load requirement.</p>
 */
public final class ToolAssemblyRecipes {
    public static final List<AssemblyRecipeDefinition> ALL = buildAll();

    private ToolAssemblyRecipes() {
    }

    private static List<AssemblyRecipeDefinition> buildAll() {
        List<AssemblyRecipeDefinition> result = new ArrayList<>();

        result.addAll(EquipmentAssemblyRecipes.ALL.stream()
                .filter(AssemblyRecipeDefinition::hasDynamicToolOutput).toList());
        addRoutes(result, ToolDefinitions.SHEARS, 2, 2);
        addRoutes(result, ToolDefinitions.SNAP_RING_PLIERS, 2, 2);
        addRoutes(result, ToolDefinitions.BEARING_PRESS, 2, 4);
        addRoutes(result, ToolDefinitions.CLAMP, 1, 2);
        addRoutes(result, ToolDefinitions.CRIMPING_TOOL, 2, 2);
        addRoutes(result, ToolDefinitions.GEAR_CUTTER, 2, 2);
        addRoutes(result, ToolDefinitions.PICKAXE, 2, 2);
        addRoutes(result, ToolDefinitions.AXE, 2, 2);
        // Knife supports the same wood/string and metal/rivet handle routes as other composed tools.
        addRoutes(result, ToolDefinitions.KNIFE, 1, 1);
        addRoutes(result, ToolDefinitions.SWORD, 2, 2);
        addRoutes(result, ToolDefinitions.SHOVEL, 1, 1);
        addRoutes(result, ToolDefinitions.HOE, 2, 2);
        addRoutes(result, ToolDefinitions.HAMMER, 3, 3);
        addRoutes(result, ToolDefinitions.SAW, 2, 2);
        addRoutes(result, ToolDefinitions.FILE, 1, 1);
        addRoutes(result, ToolDefinitions.CHISEL, 1, 1);
        addRoutes(result, ToolDefinitions.SCREWDRIVER, 1, 1);
        addRoutes(result, ToolDefinitions.CROWBAR, 2, 2);
        addRoutes(result, ToolDefinitions.WIRE_CUTTER, 0, 2);

        // Drill stays unavailable as a finished tool for now. Its 576 mB drill head can be cast,
        // and the complete powered drill will get its final assembly route later.

        // Primitive early-game tools: no metal/rivet route yet.
        // Mallet uses a wooden head; Pestle uses a stone head.
        addRoutes(result, ToolDefinitions.MALLET, 2, 0);
        addRoutes(result, ToolDefinitions.PESTLE, 2, 0);

        // Wood Sifter Frames come from the material wood recipes. The finished Sifter is a
        // dynamic tool assembly and therefore belongs here. The fixed string mesh is a normal
        // private component recipe and lives in AssemblyRecipes.
        result.add(buildSifter());

        // WRENCH is intentionally absent: its one cast WRENCH part is already the completed tool.
        return List.copyOf(result);
    }

    private static AssemblyRecipeDefinition buildSifter() {
        ToolDefinition definition = ToolDefinitions.SIFTER;
        ToolDefinition.PartSlot frame = definition.basePart();
        return AssemblyRecipeDefinition
                .recipe("tools/sifter/assembly")
                .level(1)
                .baseItemInput(frame.part(), MaterialType.TOOL_WOOD)
                .capture(frame.role())
                .input("industron:sifter_mesh")
                .toolOutput(definition)
                .build();
    }

    /**
     * Adds every valid base-material route for one tool family.
     * A count of zero means that route is intentionally disabled for that tool.
     */
    private static void addRoutes(
            List<AssemblyRecipeDefinition> out,
            ToolDefinition definition,
            int stringCount,
            int rivetCount
    ) {
        ToolDefinition.PartSlot base = definition.basePart();
        if (base == null || !definition.isAssembledTool()) return;

        if (stringCount > 0 && ToolMaterialRules.hasCandidate(base.part(), ToolMaterialRules.Kind.WOOD)) {
            out.add(buildRoute(definition, MaterialType.TOOL_WOOD, "string", stringCount, false));
        }
        if (rivetCount > 0 && ToolMaterialRules.hasCandidate(base.part(), ToolMaterialRules.Kind.METAL)) {
            out.add(buildRoute(definition, MaterialType.METAL, "rivet", rivetCount, true));
        }
    }

    private static AssemblyRecipeDefinition buildRoute(
            ToolDefinition definition,
            AssemblyMaterialSelector baseSelector,
            String routeName,
            int fastenerCount,
            boolean riveted
    ) {
        ToolDefinition.PartSlot base = definition.basePart();
        AssemblyRecipeDefinition.Builder recipe = AssemblyRecipeDefinition
                .recipe("tools/" + definition.id() + "/" + routeName)
                .level(1)
                .baseItemInput(base.part(), baseSelector)
                .capture(base.role());

        for (ToolDefinition.PartSlot slot : definition.parts()) {
            if (slot.role().equals(base.role())) continue;
            recipe.input(slot.part(), MaterialType.TOOL_MATERIAL)
                    .capture(slot.role());
        }

        if (riveted) {
            recipe.input(MaterialPart.RIVET, MaterialType.METAL, fastenerCount)
                    .stat(Stats.FASTENER_LOAD)
                    .atLeastInput(definition.strengthReferenceRole(), Stats.FASTENER_LOAD);
        } else {
            recipe.input(PlantPart.STRING, fastenerCount);
        }

        return recipe.toolOutput(definition).build();
    }
}
