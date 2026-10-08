package net.mads.industron.material.recipes;

import net.mads.industron.material.MaterialComponent;
import net.mads.industron.material.MaterialComponentWeights;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.defenitions.StoneMaterials;
import net.mads.industron.material.structure.StoneMaterial;
import net.mads.industron.recipe.CERecipeTypes;
import net.mads.industron.recipe.RecipeDefinition;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Primitive river-washing recipes. Loose stone material forms expose their declared
 * StoneMaterial .contains(...) trace minerals; soil uses ordinary Stone as its mineral profile.
 */
public final class RiverWasherRecipes {
    private static final int LOOSE_STONE_DURATION = 20 * 12;
    private static final int SOIL_DURATION = 20 * 16;
    private static final int LOOSE_STONE_TOTAL_RECOVERY = 2_500; // 25% expected trace dust per input.
    private static final int SOIL_TOTAL_RECOVERY = 1_250;        // 12.5% for less concentrated soil.

    private RiverWasherRecipes() {
    }

    public static void build(RecipeOutput output) {
        Set<ResourceLocation> emittedInputs = new HashSet<>();

        // MaterialPart.GRAVEL is the loose-material role for StoneMaterial. It includes normal
        // gravel, sand, red sand and every generated stone-specific gravel form.
        for (StoneMaterial stone : StoneMaterials.ALL) {
            if (stone.components().isEmpty()) continue;
            Optional<ResourceLocation> loose = StoneProcessingRecipes.resolveBlock(stone, MaterialPart.GRAVEL);
            if (loose.isEmpty() || !emittedInputs.add(loose.get())) continue;

            saveFromComponents(
                    output,
                    "stone/" + stone.id() + "/" + loose.get().getPath(),
                    loose.get().toString(),
                    stone.components(),
                    LOOSE_STONE_TOTAL_RECOVERY,
                    LOOSE_STONE_DURATION
            );
        }

        // Dirt does not own a StoneMaterial definition. Use the base Overworld Stone trace
        // profile: ordinary soil is weathered mixed rock, but is less concentrated than gravel/sand.
        saveSoil(output, "dirt", "minecraft:dirt");
        saveSoil(output, "coarse_dirt", "minecraft:coarse_dirt");
        saveSoil(output, "rooted_dirt", "minecraft:rooted_dirt");
        saveSoil(output, "mud", "minecraft:mud");
    }

    private static void saveSoil(RecipeOutput output, String id, String input) {
        saveFromComponents(
                output,
                "soil/" + id,
                input,
                StoneMaterials.STONE.components(),
                SOIL_TOTAL_RECOVERY,
                SOIL_DURATION
        );
    }

    private static void saveFromComponents(
            RecipeOutput output,
            String id,
            String input,
            List<MaterialComponent> components,
            int totalRecoveryChance,
            int duration
    ) {
        if (components.isEmpty()) return;
        if (components.size() > CERecipeTypes.RIVER_WASHER.maxItemOutputs()) {
            throw new IllegalStateException(
                    "River Washing input " + input + " has " + components.size()
                            + " trace outputs, but River Washing supports only "
                            + CERecipeTypes.RIVER_WASHER.maxItemOutputs()
            );
        }

        List<Integer> chances = MaterialComponentWeights.normalize(components, totalRecoveryChance);
        RecipeDefinition recipe = RecipeDefinition.recipe()
                .recipeDefinition(RecipeDefinition.Option.id(id))
                .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.RIVER_WASHER))
                .recipeDefinition(RecipeDefinition.Option.inputItem(input, 1))
                .recipeDefinition(RecipeDefinition.Option.duration(duration));

        for (int index = 0; index < components.size(); index++) {
            MaterialComponent component = components.get(index);
            MaterialRecipeHelper.ComponentIngredient ingredient =
                    MaterialRecipeHelper.componentIngredient(component.substance());
            if (!(ingredient instanceof MaterialRecipeHelper.ItemIngredient item)) {
                throw new IllegalStateException(
                        "Stone trace component " + component.substance().id()
                                + " is not a solid item and cannot be recovered by River Washing"
                );
            }
            recipe.recipeDefinition(RecipeDefinition.Option.chancedOutputItem(
                    item.item(),
                    1,
                    chances.get(index)
            ));
        }

        recipe.save(output);
    }
}
