package net.mads.industron.recipe.recipes.assembly;
import net.mads.industron.material.MaterialCategory;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.defenitions.IndustrialMaterials;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyRecipeDefinition;
import java.util.ArrayList;
import java.util.List;

/** Mineral inserts are set into a leather backing. They are never treated as molten metal. */
public final class MineralArmourRecipes {
    public static final List<AssemblyRecipeDefinition> ALL=build();
    private MineralArmourRecipes(){}
    private static List<AssemblyRecipeDefinition> build(){
        List<AssemblyRecipeDefinition> out=new ArrayList<>();
        MaterialPart[] shells={MaterialPart.HELMET_SHELL,MaterialPart.CHESTPLATE_SHELL,MaterialPart.LEGGINGS_SHELL,MaterialPart.BOOTS_SHELL};
        int[] amounts={4,8,7,4};
        for(var m:IndustrialMaterials.ALL){
            if(!MaterialCategory.GEM.matches(m))continue;
            for(int i=0;i<shells.length;i++){
                MaterialPart part=shells[i];if(!m.has(part)||!m.has(MaterialPart.GEM))continue;
                // Mineral base uniquely selects material; chisel count distinguishes helmet from boots.
                out.add(AssemblyRecipeDefinition.recipe("equipment/mineral/"+m.id()+"/"+part.id()).level(1)
                    .baseItemInput(MaterialPart.GEM,m).input(MaterialPart.GEM,m,amounts[i]-1)
                    .tool(Tool.CHISEL,i+1).input("minecraft:leather")
                    .baseItemOutput("industron:"+m.id()+"_"+part.id()).build());
            }
        }
        return List.copyOf(out);
    }
}
