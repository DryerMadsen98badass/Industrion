package net.mads.industron.material.recipes;

import net.mads.industron.Industron;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.MaterialCatalog;
import net.mads.industron.material.MaterialCategory;
import net.mads.industron.material.MaterialComponent;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.defenitions.IndustrialMaterials;
import net.mads.industron.recipe.CERecipeTypes;
import net.mads.industron.recipe.RecipeDefinition;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generic Blast Furnace recipes derived directly from top-level .contains(...).
 *
 * <p>Only compounds with exactly one distinct metal and otherwise volatile FLUID/GAS components
 * qualify. Volatile components leave the furnace and are deliberately not collected. Counts are
 * exact declaration counts: sum(contains) Dust enters; only the metal's contains count exits.</p>
 */
public final class BlastFurnaceRecipes {
    public static final int TICKS_PER_INPUT_DUST = 1200;

    private BlastFurnaceRecipes() { }

    public static void build(RecipeOutput output) {
        for (IndustrialMaterial source : IndustrialMaterials.ALL) {
            RecipePlan plan = plan(source);
            if (plan == null) continue;

            RecipeDefinition.recipe()
                    .recipeDefinition(RecipeDefinition.Option.id("material/blast_furnace/" + source.id()))
                    .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.BLAST_FURNACE))
                    .recipeDefinition(RecipeDefinition.Option.tier(source.tier()))
                    .recipeDefinition(RecipeDefinition.Option.inputItem(itemId(source, MaterialPart.DUST), plan.inputCount()))
                    .recipeDefinition(RecipeDefinition.Option.outputItem(itemId(plan.metal(), MaterialPart.DUST), plan.metalCount()))
                    .recipeDefinition(RecipeDefinition.Option.duration(Math.multiplyExact(TICKS_PER_INPUT_DUST, plan.inputCount())))
                    .recipeDefinition(RecipeDefinition.Option.temperature(plan.metal().meltingPoint()))
                    .save(output);
        }
    }

    private static RecipePlan plan(IndustrialMaterial source) {
        if (source == null || !source.has(MaterialPart.DUST) || source.components().isEmpty()) return null;

        Map<String, MetalAmount> metals = new LinkedHashMap<>();
        int total = 0;
        boolean hasVolatile = false;

        for (MaterialComponent component : source.components()) {
            IndustrialMaterial resolved = MaterialCatalog.find(component.substance().id());
            if (resolved == null) return null;
            int amount = component.amount();
            total = Math.addExact(total, amount);

            MaterialCategory category = MaterialCategory.of(resolved);
            if (category == MaterialCategory.METAL) {
                metals.merge(resolved.id(), new MetalAmount(resolved, amount),
                        (left, right) -> new MetalAmount(left.material(), Math.addExact(left.amount(), right.amount())));
            } else if (category == MaterialCategory.GAS || category == MaterialCategory.FLUID) {
                hasVolatile = true;
            } else {
                return null;
            }
        }

        if (!hasVolatile || metals.size() != 1 || total <= 0) return null;
        MetalAmount metal = metals.values().iterator().next();
        if (!metal.material().has(MaterialPart.DUST) || metal.amount() <= 0) return null;
        return new RecipePlan(metal.material(), total, metal.amount());
    }

    private static String itemId(IndustrialMaterial material, MaterialPart part) {
        ResourceLocation id = material.hasExistingPart(part)
                ? material.existingPart(part)
                : ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, part.registryName(material));
        return id.toString();
    }

    private record MetalAmount(IndustrialMaterial material, int amount) { }
    private record RecipePlan(IndustrialMaterial metal, int inputCount, int metalCount) { }
}
