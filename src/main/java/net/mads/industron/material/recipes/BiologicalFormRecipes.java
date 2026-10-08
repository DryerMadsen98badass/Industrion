package net.mads.industron.material.recipes;

import net.mads.industron.material.defenitions.OrganicMaterials;
import net.mads.industron.material.organism.*;
import net.mads.industron.recipe.CERecipeTypes;
import net.mads.industron.recipe.RecipeDefinition;
import net.mads.industron.recipe.recipes.assembly.Tool;
import net.minecraft.data.recipes.RecipeOutput;

/** Physical form conversion only; fiber dust is NOT isolated protein. */
public final class BiologicalFormRecipes {
    private BiologicalFormRecipes() {}
    public static void build(RecipeOutput output) {
        for(var source:OrganicMaterials.ALL) {
            var adapter=BiologicalChemistryBridge.adapters().get(source.id());
            if(adapter==null || !adapter.has(net.mads.industron.material.MaterialPart.DUST))continue;
            String dust=MaterialRecipeHelper.itemId(adapter,net.mads.industron.material.MaterialPart.DUST);
            if(dust.equals(source.existingForm().toString()))continue;
            grind(output,"organics/"+source.id()+"/grinding",source.existingForm().toString(),dust,
                    BiologicalProcessingTier.of(source));
        }
        for (var material : OrganicMaterials.BIOLOGICAL) {
            if (!material.roles().contains(OrganicRole.FIBER)) continue;
            grind(output,"organics/"+material.id()+"/grinding","industron:"+material.id(),
                    "industron:"+material.id()+"_dust",BiologicalProcessingTier.of(material));
        }
    }
    private static void grind(RecipeOutput output,String id,String source,String target,net.mads.industron.machine.MachineTier tier) {
        var recipe=RecipeDefinition.recipe()
                .recipeDefinition(RecipeDefinition.Option.id(id))
                .recipeDefinition(RecipeDefinition.Option.inputItem(source,1))
                .recipeDefinition(RecipeDefinition.Option.outputItem(target,1))
                .recipeDefinition(RecipeDefinition.Option.tier(tier))
                .recipeDefinition(RecipeDefinition.Option.duration(120));
        if(MaterialProcessingRules.allowsPrimitive(tier)) {
            recipe.recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.BASIN_MORTARING))
                    .tool(Tool.PESTLE,6).save(output);
        } else {
            recipe.recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.GRINDING)).save(output);
        }
    }
}
