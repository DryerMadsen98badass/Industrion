package net.mads.industron.material.recipes;

import net.mads.industron.material.organism.*;
import net.mads.industron.recipe.CERecipeTypes;
import net.mads.industron.recipe.RecipeDefinition;
import net.mads.industron.recipe.recipes.assembly.Tool;
import net.minecraft.data.recipes.RecipeOutput;

/** Identity-preserving form changes. Grinding bone never directly yields isolated collagen. */
public final class OrganismFormRecipes {
    private OrganismFormRecipes() {}
    public static void build(RecipeOutput output) {
        for (var source : OrganismItemCatalog.ALL) {
            if (source.form()!=OrganicForm.RAW || !source.part().forms().contains(OrganicForm.DUST)) continue;
            // Several mobs can reference the exact same existing item. Emit one canonical route.
            if (OrganismItemCatalog.byItem(source.itemId())!=source) continue;
            var dust=OrganismItemCatalog.form(source.owner(),source.part(),OrganicForm.DUST);
            var tier=BiologicalProcessingTier.of(source.material());
            String path="organisms/"+source.owner().id()+"/"+source.part().suffix();
            var grinding=RecipeDefinition.recipe()
                    .recipeDefinition(RecipeDefinition.Option.id(path+"/grinding"))
                    .recipeDefinition(RecipeDefinition.Option.inputItem(source.itemId(),1))
                    .recipeDefinition(RecipeDefinition.Option.outputItem(dust.itemId(),1))
                    .recipeDefinition(RecipeDefinition.Option.duration(120))
                    .recipeDefinition(RecipeDefinition.Option.tier(tier));
            if (MaterialProcessingRules.allowsPrimitive(tier)) {
                grinding.recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.BASIN_MORTARING))
                        .tool(Tool.PESTLE,6).save(output);
            } else {
                grinding.recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.GRINDING)).save(output);
            }
            for (var size : new OrganicForm[]{OrganicForm.SMALL_DUST,OrganicForm.TINY_DUST}) {
                if (!source.part().forms().contains(size)) continue;
                var target=OrganismItemCatalog.form(source.owner(),source.part(),size);
                int count=size==OrganicForm.SMALL_DUST?4:9;
                String sizePath=path+"/"+size.name().toLowerCase(java.util.Locale.ROOT);
                RecipeDefinition.recipe()
                        .recipeDefinition(RecipeDefinition.Option.id(sizePath+"/split"))
                        .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.DECOMPACTING))
                        .recipeDefinition(RecipeDefinition.Option.inputItem(dust.itemId(),1))
                        .recipeDefinition(RecipeDefinition.Option.outputItem(target.itemId(),count))
                        .recipeDefinition(RecipeDefinition.Option.circuit(count==4?1:2))
                        .recipeDefinition(RecipeDefinition.Option.duration(40))
                        .recipeDefinition(RecipeDefinition.Option.tier(tier)).save(output);
                RecipeDefinition.recipe()
                        .recipeDefinition(RecipeDefinition.Option.id(sizePath+"/combine"))
                        .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.COMPACTING))
                        .recipeDefinition(RecipeDefinition.Option.inputItem(target.itemId(),count))
                        .recipeDefinition(RecipeDefinition.Option.outputItem(dust.itemId(),1))
                        .recipeDefinition(RecipeDefinition.Option.duration(40))
                        .recipeDefinition(RecipeDefinition.Option.tier(tier)).save(output);
            }
        }
    }
}
