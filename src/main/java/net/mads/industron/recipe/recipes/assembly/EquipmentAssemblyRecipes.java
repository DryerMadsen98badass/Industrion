package net.mads.industron.recipe.recipes.assembly;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.plant.PlantPart;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyRecipeDefinition;
import net.mads.industron.recipe.recipetypes.assembly.ToolDefinition;
import java.util.ArrayList;
import java.util.List;

/** Explicit equipment bills of materials. No tier-based recipe generation. */
public final class EquipmentAssemblyRecipes {
    public static final List<AssemblyRecipeDefinition> ALL=build();
    private EquipmentAssemblyRecipes(){}
    private static List<AssemblyRecipeDefinition> build(){
        List<AssemblyRecipeDefinition> out=new ArrayList<>();
        out.add(AssemblyRecipeDefinition.recipe("equipment/components/bow_string").level(1)
            .baseItemInput(PlantPart.STRING).input(PlantPart.STRING,2).tool(Tool.KNIFE,1)
            .baseItemOutput("industron:bow_string").build());
        out.add(AssemblyRecipeDefinition.recipe("equipment/components/crossbow_string").level(1)
            .baseItemInput(PlantPart.STRING).input(PlantPart.STRING,2).tool(Tool.FILE,1)
            .baseItemOutput("industron:crossbow_string").build());
        out.add(AssemblyRecipeDefinition.recipe("equipment/components/fishing_line").level(1)
            .baseItemInput(PlantPart.STRING).input(PlantPart.STRING,2).tool(Tool.SHEARS,1)
            .baseItemOutput("industron:fishing_line").build());
        out.add(AssemblyRecipeDefinition.recipe("equipment/bow").level(1)
            .baseItemInput(MaterialPart.BOW_BODY,MaterialType.TOOL_WOOD).capture("body")
            .input("industron:bow_string").toolOutput(ToolDefinitions.BOW).build());
        out.add(AssemblyRecipeDefinition.recipe("equipment/crossbow").level(1)
            .baseItemInput(MaterialPart.CROSSBOW_STOCK,MaterialType.TOOL_WOOD).capture("stock")
            .input(MaterialPart.CROSSBOW_LIMBS,MaterialType.TOOL_MATERIAL).capture("limbs")
            .input("industron:crossbow_string")
            .input(MaterialPart.CROSSBOW_TRIGGER,MaterialType.METAL).capture("trigger")
            .input(MaterialPart.SHORT_ROD,MaterialType.METAL,2).input(Component.SMALL_SCREW,2)
            // Each mounted screw already contributes its ring and screwdriver operation.
            .toolOutput(ToolDefinitions.CROSSBOW).build());
        out.add(AssemblyRecipeDefinition.recipe("equipment/fishing_rod").level(1)
            .baseItemInput(MaterialPart.FISHING_ROD_BODY,MaterialType.TOOL_WOOD).capture("body")
            .input("industron:fishing_line")
            .input(MaterialPart.FISHING_HOOK,MaterialType.METAL).capture("hook")
            .toolOutput(ToolDefinitions.FISHING_ROD).build());
        out.add(AssemblyRecipeDefinition.recipe("equipment/shield").level(1)
            .baseItemInput(MaterialPart.SHIELD_BODY,MaterialType.TOOL_MATERIAL).capture("body")
            .input(MaterialPart.SHIELD_HANDLE,MaterialType.TOOL_MATERIAL).capture("handle")
            .input(MaterialPart.RIVET,MaterialType.METAL,4).stat(Stats.FASTENER_LOAD).atLeastInput("body",Stats.FASTENER_LOAD)
            .input("minecraft:leather",2).tool(Tool.HAMMER,4).toolOutput(ToolDefinitions.SHIELD).build());
        armour(out,ToolDefinitions.HELMET,2,2,4);
        armour(out,ToolDefinitions.CHESTPLATE,4,4,8);
        armour(out,ToolDefinitions.LEGGINGS,3,4,6);
        armour(out,ToolDefinitions.BOOTS,2,2,4);
        return List.copyOf(out);
    }
    private static void armour(List<AssemblyRecipeDefinition> out,ToolDefinition def,int leather,int string,int rivets){
        out.add(AssemblyRecipeDefinition.recipe("equipment/"+def.id()).level(1)
            .baseItemInput(def.basePart().part(),MaterialType.TOOL_MATERIAL).capture("shell")
            .input("minecraft:leather",leather).input(PlantPart.STRING,string)
            .input(MaterialPart.RIVET,MaterialType.METAL,rivets)
            .stat(Stats.FASTENER_LOAD).atLeastInput("shell",Stats.FASTENER_LOAD)
            .tool(Tool.HAMMER,rivets).toolOutput(def).build());
    }
}
