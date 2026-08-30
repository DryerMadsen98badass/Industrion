package net.mads.industron.material.recipes;

import net.mads.industron.Industron;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.material.MaterialComponent;
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

            RecipeDefinition recipe = RecipeDefinition.recipe()
                    .recipeDefinition(RecipeDefinition.Option.id("stone_trace/" + stone.id()))
                    .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.CENTRIFUGING))
                    .recipeDefinition(RecipeDefinition.Option.inputItem(stoneDust, 1))
                    .recipeDefinition(RecipeDefinition.Option.duration(DURATION_TICKS))
                    .recipeDefinition(RecipeDefinition.Option.tier(MaterialTierResolver.oneTierBelowStrongest(stone)));

            int outputs = 0;
            for (MaterialComponent component : stone.components()) {
                String dust = materialDustId(component.substance());
                if (dust == null) {
                    throw new IllegalStateException("Stone " + stone.id() + " contains " + component.substance().id()
                            + " but that component has no dust form");
                }
                int chance = percentToChance(component.amount());
                recipe.recipeDefinition(RecipeDefinition.Option.chancedOutputItem(dust, 1, chance));
                outputs++;
            }

            if (outputs > CERecipeTypes.CENTRIFUGING.maxItemOutputs()) {
                throw new IllegalStateException("Stone " + stone.id() + " has " + outputs
                        + " trace outputs but centrifuging supports only " + CERecipeTypes.CENTRIFUGING.maxItemOutputs());
            }
            recipe.save(output);
        }
    }

    private static int percentToChance(int percentPoints) {
        if (percentPoints < 0 || percentPoints > 100) {
            throw new IllegalArgumentException("Stone trace amount must be a percentage point in 0..100: " + percentPoints);
        }
        return (int) Math.round((percentPoints / 100.0D) * CEChancedItemOutput.MAX_CHANCE);
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
