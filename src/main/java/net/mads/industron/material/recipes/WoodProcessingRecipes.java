package net.mads.industron.material.recipes;

import net.mads.industron.Industron;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.defenitions.WoodMaterials;
import net.mads.industron.material.structure.WoodMaterial;
import net.mads.industron.recipe.CERecipeTypes;
import net.mads.industron.recipe.RecipeDefinition;
import net.minecraft.data.recipes.RecipeOutput;

/** Machine counterpart for turning recovered bark into the wood's normal pulp form. */
public final class WoodProcessingRecipes {
    private static final int BARK_GRINDING_TICKS = 20 * 5;
    private static final int SHAFT_CUTTING_TICKS = 20 * 10;

    private WoodProcessingRecipes() {
    }

    public static void build(RecipeOutput output) {
        for (WoodMaterial wood : WoodMaterials.ALL) {
            buildShaftCutting(output, wood);

            // ULV/LV manual Mortaring emits the matching automated Grinding recipe.
            if (BasinMortaringRecipes.supportsTier(wood.tier())) continue;
            String bark = Industron.MOD_ID + ":" + MaterialPart.BARK.registryName(wood);
            String pulp = wood.hasExistingPart(MaterialPart.WOOD_PULP)
                    ? wood.existingPart(MaterialPart.WOOD_PULP).toString()
                    : Industron.MOD_ID + ":" + MaterialPart.WOOD_PULP.registryName(wood);

            RecipeDefinition.recipe()
                    .recipeDefinition(RecipeDefinition.Option.id("wood_processing/" + wood.id() + "/bark_to_pulp"))
                    .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.GRINDING))
                    .recipeDefinition(RecipeDefinition.Option.inputItem(bark, 1))
                    .recipeDefinition(RecipeDefinition.Option.outputItem(pulp, 1))
                    .recipeDefinition(RecipeDefinition.Option.duration(BARK_GRINDING_TICKS))
                    .recipeDefinition(RecipeDefinition.Option.tier(MaterialProcessingRules.processingTier(wood.tier())))
                    .save(output);
        }
    }

    /** Machine/Mechanical Saw counterpart to the precise manual Saw shaft route. */
    private static void buildShaftCutting(RecipeOutput output, WoodMaterial wood) {
        if (!WoodRecipeIds.has(wood, MaterialPart.PLANKS)
                || !WoodRecipeIds.has(wood, MaterialPart.SHAFT)
                || !WoodRecipeIds.has(wood, MaterialPart.WOOD_PULP)) {
            return;
        }

        RecipeDefinition.recipe()
                .recipeDefinition(RecipeDefinition.Option.id("wood_processing/" + wood.id() + "/planks_to_shaft"))
                .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.CUTTING))
                .recipeDefinition(RecipeDefinition.Option.inputItem(WoodRecipeIds.stringId(wood, MaterialPart.PLANKS), 1))
                .recipeDefinition(RecipeDefinition.Option.outputItem(WoodRecipeIds.stringId(wood, MaterialPart.SHAFT), 1))
                .recipeDefinition(RecipeDefinition.Option.outputItem(WoodRecipeIds.stringId(wood, MaterialPart.WOOD_PULP), 1))
                .recipeDefinition(RecipeDefinition.Option.duration(SHAFT_CUTTING_TICKS))
                .recipeDefinition(RecipeDefinition.Option.tier(MaterialProcessingRules.processingTier(wood.tier())))
                .save(output);
    }
}
