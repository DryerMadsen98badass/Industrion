package net.mads.industron.material.recipes;

import net.mads.industron.material.MaterialComponent;
import net.mads.industron.material.MaterialComponentWeights;
import net.mads.industron.material.defenitions.StoneMaterials;
import net.mads.industron.material.structure.StoneMaterial;
import net.mads.industron.recipe.CERecipeTypes;
import net.mads.industron.recipe.RecipeDefinition;
import net.mads.industron.recipe.recipes.assembly.Tool;
import net.mads.industron.recipe.recipetypes.primitive.PrimitiveSiftingRules;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/** Generates one-stone-dust, exactly-one-dust recipes for the hand-held primitive sieve. */
public final class PrimitiveStoneSiftingRecipes {
    private PrimitiveStoneSiftingRecipes() {
    }

    public static void build(RecipeOutput output) {
        for (StoneMaterial stone : StoneMaterials.ALL) {
            if (!MaterialProcessingRules.allowsPrimitive(stone.tier())) continue;
            Optional<ResourceLocation> stoneDust = StoneProcessingRecipes.resolveDust(stone);
            if (stoneDust.isEmpty() || stone.components().isEmpty()) continue;

            List<Integer> chances = MaterialComponentWeights.normalize(
                    stone.components(),
                    PrimitiveSiftingRules.FIND_CHANCE
            );
            RecipeDefinition recipe = RecipeDefinition.recipe()
                    .recipeDefinition(RecipeDefinition.Option.id("stone/" + stone.id()))
                    .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.PRIMITIVE_SIFTING))
                    .recipeDefinition(RecipeDefinition.Option.inputItem(stoneDust.get().toString(), 1))
                    .recipeDefinition(RecipeDefinition.Option.tier(MaterialProcessingRules.processingTier(stone.tier())))
                    .tool(Tool.SIFTER, 1);

            for (int index = 0; index < stone.components().size(); index++) {
                MaterialComponent component = stone.components().get(index);
                MaterialRecipeHelper.ComponentIngredient ingredient =
                        MaterialRecipeHelper.componentIngredient(component.substance());
                if (!(ingredient instanceof MaterialRecipeHelper.ItemIngredient dust)) {
                    throw new IllegalStateException("Stone " + stone.id() + " contains "
                            + component.substance().id() + " but primitive sifting requires a dust output");
                }
                recipe.recipeDefinition(RecipeDefinition.Option.chancedOutputItem(
                        dust.item(),
                        1,
                        chances.get(index)
                ));
            }
            recipe.save(output);
        }
    }
}
