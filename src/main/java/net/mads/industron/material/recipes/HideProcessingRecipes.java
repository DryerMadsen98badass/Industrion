package net.mads.industron.material.recipes;

import net.mads.industron.material.organism.*;
import net.mads.industron.material.defenitions.AnimalMaterials;
import net.mads.industron.recipe.CERecipeTypes;
import net.mads.industron.recipe.RecipeDefinition;
import net.minecraft.data.recipes.RecipeOutput;

/** Oil dressing, washing and mechanical softening. No magical rotten-flesh-to-leather smelting. */
public final class HideProcessingRecipes {
    private HideProcessingRecipes() {}
    public static void build(RecipeOutput output) {
        for(var source:OrganismItemCatalog.ALL) {
            if(source.part()!=OrganismPart.HIDE || source.form()!=OrganicForm.RAW
                    || OrganismItemCatalog.byItem(source.itemId())!=source)continue;
            wash(output,source.itemId(),source.material(),"organisms/"+source.owner().id()+"/hide/washing");
        }
        wash(output,"industron:hide",AnimalMaterials.HIDE,"organics/hide/washing");
        var tier=BiologicalProcessingTier.of(AnimalMaterials.LEATHER);
        // Hand work is restricted at generation time because HAND_PROCESSING deliberately ignores tier.
        if(MaterialProcessingRules.allowsPrimitive(tier)) {
            RecipeDefinition.recipe()
                    .recipeDefinition(RecipeDefinition.Option.id("organics/hide/softening"))
                    .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.HAND_PROCESSING))
                    .recipeDefinition(RecipeDefinition.Option.inputItem("industron:oiled_hide",1))
                    .recipeDefinition(RecipeDefinition.Option.outputItem("minecraft:leather",1))
                    .recipeDefinition(RecipeDefinition.Option.uses(24)).save(output);
        }
        // Mechanical rolling/flexing can soften the same dressed hide at any supported machine tier.
        RecipeDefinition.recipe()
                .recipeDefinition(RecipeDefinition.Option.id("organics/hide/softening"))
                .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.ROLLING))
                .recipeDefinition(RecipeDefinition.Option.inputItem("industron:oiled_hide",1))
                .recipeDefinition(RecipeDefinition.Option.outputItem("minecraft:leather",1))
                .recipeDefinition(RecipeDefinition.Option.tier(tier))
                .recipeDefinition(RecipeDefinition.Option.duration(480)).save(output);
    }
    private static void wash(RecipeOutput output,String source,BiologicalMaterial material,String id) {
        var optional=HideProcessingBalance.tryOf(material);
        if(optional.isEmpty())return;
        var batch=optional.get();
        if(batch.hides()>64 || batch.oiledHides()>64 || batch.fiber()>64)
            throw new IllegalArgumentException("Hide batch exceeds machine stack capacity: "+id);
        var recipe=RecipeDefinition.recipe()
                .recipeDefinition(RecipeDefinition.Option.id(id))
                .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.WASHING))
                .recipeDefinition(RecipeDefinition.Option.inputItem(source,batch.hides()))
                .recipeDefinition(RecipeDefinition.Option.notConsumableFluid("minecraft:water",144))
                .recipeDefinition(RecipeDefinition.Option.outputItem("industron:oiled_hide",batch.oiledHides()))
                .recipeDefinition(RecipeDefinition.Option.duration(80*batch.hides()))
                .recipeDefinition(RecipeDefinition.Option.tier(BiologicalProcessingTier.of(material)));
        if(batch.fiber()>0)recipe.recipeDefinition(RecipeDefinition.Option.outputItem("industron:fur_fiber",batch.fiber()));
        if(batch.waterUnits()>0)recipe.recipeDefinition(RecipeDefinition.Option.outputFluid("minecraft:water",144*batch.waterUnits()));
        recipe.save(output);
    }
}
