package net.mads.industron.material.recipes;

import net.mads.industron.Industron;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.material.MaterialComponent;
import net.mads.industron.material.MaterialComponentWeights;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.MaterialTierResolver;
import net.mads.industron.material.defenitions.StoneMaterials;
import net.mads.industron.material.structure.StoneMaterial;
import net.mads.industron.material.structure.StructureMaterialGenerator;
import net.mads.industron.recipe.CEChancedItemOutput;
import net.mads.industron.recipe.CERecipeTypes;
import net.mads.industron.recipe.RecipeDefinition;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Generates the special trace-mineral centrifuge recipe for StoneMaterial dusts. */
public final class StoneCentrifugingRecipes {
    private static final int DURATION_TICKS = 20 * 20;

    private StoneCentrifugingRecipes() {
    }

    public static void build(RecipeOutput output) {
        for (StoneMaterial stone : StoneMaterials.ALL) {
            if (stone.components().isEmpty()) continue;

            String stoneDust = structureDustId(stone);
            if (stoneDust == null) continue;

            int maxOutputs = CERecipeTypes.CENTRIFUGING.maxItemOutputs();
            if (maxOutputs < 1) {
                throw new IllegalStateException("Centrifuging must support at least one item output");
            }

            List<MaterialComponent> components = stone.components();
            List<Integer> chances = MaterialComponentWeights.normalize(
                    components,
                    CEChancedItemOutput.MAX_CHANCE
            );
            int batches = (components.size() + maxOutputs - 1) / maxOutputs;
            for (int batch = 0; batch < batches; batch++) {
                int from = batch * maxOutputs;
                int to = Math.min(components.size(), from + maxOutputs);
                String id = "stone_trace/" + stone.id() + (batches == 1 ? "" : "/batch_" + (batch + 1));

                RecipeDefinition recipe = RecipeDefinition.recipe()
                        .recipeDefinition(RecipeDefinition.Option.id(id))
                        .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.CENTRIFUGING))
                        .recipeDefinition(RecipeDefinition.Option.inputItem(stoneDust, 1))
                        .recipeDefinition(RecipeDefinition.Option.duration(DURATION_TICKS))
                        .recipeDefinition(RecipeDefinition.Option.circuit(batch + 1))
                        .recipeDefinition(RecipeDefinition.Option.tier(MaterialTierResolver.oneTierBelowStrongest(stone)));

                for (int componentIndex = from; componentIndex < to; componentIndex++) {
                    MaterialComponent component = components.get(componentIndex);
                    String dust = materialDustId(component.substance());
                    if (dust == null) {
                        throw new IllegalStateException("Stone " + stone.id() + " contains " + component.substance().id()
                                + " but that component has no dust form");
                    }
                    recipe.recipeDefinition(RecipeDefinition.Option.chancedOutputItem(
                            dust,
                            1,
                            chances.get(componentIndex)
                    ));
                }
                recipe.save(output);
            }
        }
    }

    private static String materialDustId(IndustrialSubstance substance) {
        if (!(substance instanceof IndustrialMaterial material) || !material.has(MaterialPart.DUST)) return null;
        return MaterialRecipeHelper.itemId(material, MaterialPart.DUST);
    }

    private static String structureDustId(StoneMaterial material) {
        if (material.isWithout(MaterialPart.DUST)) return null;
        if (material.hasExistingPart(MaterialPart.DUST)) return material.existingPart(MaterialPart.DUST).toString();
        if (!StructureMaterialGenerator.generatedItemForms(material).contains(MaterialPart.DUST)) return null;
        return ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, MaterialPart.DUST.registryName(material)).toString();
    }
}
