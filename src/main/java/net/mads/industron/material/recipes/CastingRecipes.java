package net.mads.industron.material.recipes;

import net.mads.industron.machine.foundry.FoundryMetallurgy;
import net.mads.industron.material.*;
import net.mads.industron.material.defenitions.*;
import net.mads.industron.recipe.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import java.util.*;

public final class CastingRecipes {
    private static List<RecipeHolder<CERecipe>> cooling;
    private CastingRecipes() {}
    public static List<RecipeHolder<CERecipe>> cooling() {
        if (cooling == null) {
            List<RecipeHolder<CERecipe>> result = new ArrayList<>();
            for (IndustrialMaterial material : IndustrialMaterials.ALL) {
                for (MaterialPart cold : MaterialPart.values()) {
                    if (cold.isHotForgePart() || !cold.isForgeableForm()) continue;
                    MaterialPart hot = cold.hotForgePart();
                    if (hot == null || !material.has(cold) || !material.has(hot)) continue;

                    int hotTemperature = MaterialPropertyCalculator.temperatureFor(material.properties(), hot);
                    String id = "foundry/cooling/" + hot.registryName(material);
                    var recipe = RecipeDefinition.recipe()
                            .recipeDefinition(RecipeDefinition.Option.id(id))
                            .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.COOLING))
                            .recipeDefinition(RecipeDefinition.Option.tier(MaterialProcessingRules.processingTier(material)))
                            .recipeDefinition(RecipeDefinition.Option.duration(FoundryMetallurgy.duration(
                                    material, cold.materialAmountMb(), hotTemperature)))
                            .recipeDefinition(RecipeDefinition.Option.inputItem(MaterialRecipeHelper.itemId(material, hot), 1))
                            .recipeDefinition(RecipeDefinition.Option.outputItem(MaterialRecipeHelper.itemId(material, cold), 1)).build();
                    result.add(new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath("industron", id), recipe));
                    if (canRackCool(material)) {
                        String rackId = "foundry/rack_cooling/" + hot.registryName(material);
                        var rackRecipe = RecipeDefinition.recipe()
                                .recipeDefinition(RecipeDefinition.Option.id(rackId))
                                .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.RACK_DRYING))
                                .recipeDefinition(RecipeDefinition.Option.duration((int) Math.min(Integer.MAX_VALUE,
                                        2L * recipe.duration().orElse(200))))
                                .recipeDefinition(RecipeDefinition.Option.inputItem(MaterialRecipeHelper.itemId(material, hot), 1))
                                .recipeDefinition(RecipeDefinition.Option.outputItem(MaterialRecipeHelper.itemId(material, cold), 1)).build();
                        result.add(new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath("industron", rackId), rackRecipe));
                    }
                }
            }
            cooling = List.copyOf(result);
        }
        return cooling;
    }
    public static void build(RecipeOutput output) {
        for (var recipe : cooling()) output.accept(recipe.id(), recipe.value(), null);
    }
    public static boolean isHot(ItemStack stack) {
        var target = MaterialLookup.find(stack);
        return target != null && target.part().isHotForgePart();
    }
    public static boolean canRackCool(IndustrialMaterial material) {
        return material != null && MaterialProcessingRules.allowsPrimitive(material.tier());
    }
    public static boolean canRackCool(ItemStack stack) {
        var target = MaterialLookup.find(stack);
        return target != null && isHot(stack) && canRackCool(target.material());
    }
}
