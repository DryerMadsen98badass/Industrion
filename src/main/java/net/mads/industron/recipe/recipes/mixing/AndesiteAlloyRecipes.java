package net.mads.industron.recipe.recipes.mixing;

import net.mads.industron.machine.MachineTier;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.defenitions.CompoundMaterials;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.defenitions.StoneMaterials;
import net.mads.industron.recipe.CERecipeTypes;
import net.mads.industron.recipe.RecipeDefinition;
import net.mads.industron.recipe.recipes.assembly.Tool;
import net.minecraft.data.recipes.RecipeOutput;

/** Private Create composite route; not a generic alloy-chemistry rule. */
public final class AndesiteAlloyRecipes {
    private AndesiteAlloyRecipes() {}

    public static void build(RecipeOutput output) {
        String dust = stoneItem(MaterialPart.DUST);
        // Obtain full dust before powered grinding is available; eight pebbles contain one stone/dust unit.
        RecipeDefinition.recipe()
                .recipeDefinition(RecipeDefinition.Option.id("recipes/mixing/andesite_pebbles_to_dust"))
                .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.BASIN_MORTARING))
                .recipeDefinition(RecipeDefinition.Option.tier(MachineTier.ULV))
                .recipeDefinition(RecipeDefinition.Option.inputItem(stoneItem(MaterialPart.PEBBLE), 8))
                .recipeDefinition(RecipeDefinition.Option.outputItem(dust, 1))
                .recipeDefinition(RecipeDefinition.Option.duration(480))
                .tool(Tool.PESTLE, 24).save(output);

        for (IndustrialMaterial alloy : CompoundMaterials.andesiteAlloys()) {
            IndustrialMaterial metal = (IndustrialMaterial) alloy.components().get(1).substance();
            RecipeDefinition.recipe()
                    .recipeDefinition(RecipeDefinition.Option.id("recipes/mixing/andesite_alloy/" + metal.id()))
                    .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.BASIN_MIXING))
                    .recipeDefinition(RecipeDefinition.Option.tier(MachineTier.ULV))
                    .recipeDefinition(RecipeDefinition.Option.inputItem(dust, 1))
                    .recipeDefinition(RecipeDefinition.Option.inputItem(item(metal, MaterialPart.NUGGET), 8))
                    .recipeDefinition(RecipeDefinition.Option.outputItem(item(alloy, MaterialPart.DUST), 1))
                    .recipeDefinition(RecipeDefinition.Option.duration(100))
                    .tool(Tool.SHOVEL, 1).save(output);

            RecipeDefinition.recipe()
                    .recipeDefinition(RecipeDefinition.Option.id("recipes/blast_furnace/" + alloy.id()))
                    .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.BLAST_FURNACE))
                    .recipeDefinition(RecipeDefinition.Option.tier(MachineTier.LV))
                    .recipeDefinition(RecipeDefinition.Option.inputItem(item(alloy, MaterialPart.DUST), 1))
                    .recipeDefinition(RecipeDefinition.Option.outputItem(item(alloy, MaterialPart.INGOT), 1))
                    .recipeDefinition(RecipeDefinition.Option.duration(1200))
                    .recipeDefinition(RecipeDefinition.Option.temperature(900))
                    .save(output);
        }
    }

    private static String item(IndustrialMaterial material, MaterialPart part) {
        return material.hasExistingPart(part) ? material.existingPart(part).toString()
                : "industron:" + part.registryName(material);
    }

    private static String stoneItem(MaterialPart part) {
        return StoneMaterials.ANDESITE.hasExistingPart(part)
                ? StoneMaterials.ANDESITE.existingPart(part).toString()
                : "industron:" + part.registryName(StoneMaterials.ANDESITE);
    }
}
