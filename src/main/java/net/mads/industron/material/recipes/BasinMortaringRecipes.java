package net.mads.industron.material.recipes;

import net.mads.industron.Industron;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.defenitions.ClayMaterials;
import net.mads.industron.material.defenitions.StoneMaterials;
import net.mads.industron.material.defenitions.WoodMaterials;
import net.mads.industron.material.structure.StoneMaterial;
import net.mads.industron.material.structure.WoodMaterial;
import net.mads.industron.recipe.CERecipeTypes;
import net.mads.industron.recipe.RecipeDefinition;
import net.mads.industron.recipe.recipes.assembly.Tool;
import net.minecraft.data.recipes.RecipeOutput;

/**
 * Primitive Pestle processing performed in the Basin.
 *
 * <p>Only ULV/LV materials receive manual Mortaring recipes. The recipe tier is
 * also the minimum accepted Pestle tier at runtime. Every Mortaring recipe is
 * mirrored automatically into normal machine Grinding by the recipe type.</p>
 */
public final class BasinMortaringRecipes {
    private static final int BARK_TO_PULP_USES = 6;
    private static final int BARK_GRINDING_TICKS = 20 * 5;
    private static final int PEBBLE_TO_SMALL_DUST_USES = 6;
    private static final int PEBBLE_GRINDING_TICKS = 20 * 6;
    private static final int BRICK_TO_DUST_USES = 12;
    private static final int BRICK_GRINDING_TICKS = 20 * 45;
    private static final int CRACKED_BRICK_TO_SMALL_DUST_USES = 8;
    private static final int CRACKED_BRICK_GRINDING_TICKS = 20 * 35;

    private BasinMortaringRecipes() {
    }

    public static void build(RecipeOutput output) {
        buildWood(output);
        buildStone(output);
        buildClay(output);
    }

    static boolean supportsTier(MachineTier tier) {
        return MaterialProcessingRules.allowsPrimitive(tier);
    }

    private static void buildWood(RecipeOutput output) {
        for (WoodMaterial wood : WoodMaterials.ALL) {
            if (!supportsTier(wood.tier())) continue;

            String bark = structureItemId(wood, MaterialPart.BARK);
            String pulp = structureItemId(wood, MaterialPart.WOOD_PULP);
            RecipeDefinition.recipe()
                    .recipeDefinition(RecipeDefinition.Option.id(
                            "wood_processing/" + wood.id() + "/bark_to_pulp"
                    ))
                    .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.BASIN_MORTARING))
                    .recipeDefinition(RecipeDefinition.Option.inputItem(bark, 1))
                    .recipeDefinition(RecipeDefinition.Option.outputItem(pulp, 1))
                    .recipeDefinition(RecipeDefinition.Option.tier(MaterialProcessingRules.processingTier(wood.tier())))
                    .recipeDefinition(RecipeDefinition.Option.duration(BARK_GRINDING_TICKS))
                    .tool(Tool.PESTLE, BARK_TO_PULP_USES)
                    .save(output);
        }
    }

    private static void buildStone(RecipeOutput output) {
        for (StoneMaterial stone : StoneMaterials.ALL) {
            if (!supportsTier(stone.tier())) continue;
            if (stone.isWithout(MaterialPart.PEBBLE)
                    || stone.isWithout(MaterialPart.SMALL_DUST)
                    || !stone.generatedForms().contains(MaterialPart.PEBBLE)
                    || !stone.generatedForms().contains(MaterialPart.SMALL_DUST)) {
                continue;
            }

            String pebble = structureItemId(stone, MaterialPart.PEBBLE);
            String smallDust = structureItemId(stone, MaterialPart.SMALL_DUST);

            // 1 Stone = 8 Pebbles and 1 Dust = 4 Small Dust:
            // 2 Pebbles = 1/4 Stone = 1 Small Dust.
            RecipeDefinition.recipe()
                    .recipeDefinition(RecipeDefinition.Option.id(
                            "stone_processing/" + stone.id() + "/pebble_to_small_dust"
                    ))
                    .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.BASIN_MORTARING))
                    .recipeDefinition(RecipeDefinition.Option.inputItem(pebble, 2))
                    .recipeDefinition(RecipeDefinition.Option.outputItem(smallDust, 1))
                    .recipeDefinition(RecipeDefinition.Option.tier(MaterialProcessingRules.processingTier(stone.tier())))
                    .recipeDefinition(RecipeDefinition.Option.duration(PEBBLE_GRINDING_TICKS))
                    .tool(Tool.PESTLE, PEBBLE_TO_SMALL_DUST_USES)
                    .save(output);
        }
    }

    private static void buildClay(RecipeOutput output) {
        for (IndustrialMaterial clay : ClayMaterials.ALL) {
            if (!clay.isClayMaterial() || !supportsTier(clay.tier())) continue;

            String brick = industrialItemId(clay, MaterialPart.BRICK);
            String crackedBrick = industrialItemId(clay, MaterialPart.CRACKED_BRICK);
            String dust = industrialItemId(clay, MaterialPart.DUST);
            String smallDust = industrialItemId(clay, MaterialPart.SMALL_DUST);

            RecipeDefinition.recipe()
                    .recipeDefinition(RecipeDefinition.Option.id(clay.id() + "/brick_to_dust"))
                    .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.BASIN_MORTARING))
                    .recipeDefinition(RecipeDefinition.Option.inputItem(brick, 1))
                    .recipeDefinition(RecipeDefinition.Option.outputItem(dust, 1))
                    .recipeDefinition(RecipeDefinition.Option.tier(MaterialProcessingRules.processingTier(clay)))
                    .recipeDefinition(RecipeDefinition.Option.duration(BRICK_GRINDING_TICKS))
                    .tool(Tool.PESTLE, BRICK_TO_DUST_USES)
                    .save(output);

            RecipeDefinition.recipe()
                    .recipeDefinition(RecipeDefinition.Option.id(clay.id() + "/cracked_brick_to_small_dust"))
                    .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.BASIN_MORTARING))
                    .recipeDefinition(RecipeDefinition.Option.inputItem(crackedBrick, 1))
                    .recipeDefinition(RecipeDefinition.Option.outputItem(smallDust, 2))
                    .recipeDefinition(RecipeDefinition.Option.tier(MaterialProcessingRules.processingTier(clay)))
                    .recipeDefinition(RecipeDefinition.Option.duration(CRACKED_BRICK_GRINDING_TICKS))
                    .tool(Tool.PESTLE, CRACKED_BRICK_TO_SMALL_DUST_USES)
                    .save(output);
        }
    }

    private static String structureItemId(net.mads.industron.material.structure.StructureMaterial material, MaterialPart part) {
        if (material.hasExistingPart(part)) {
            return material.existingPart(part).toString();
        }
        return Industron.MOD_ID + ":" + part.registryName(material);
    }

    private static String industrialItemId(IndustrialMaterial material, MaterialPart part) {
        if (material.hasExistingPart(part)) {
            return material.existingPart(part).toString();
        }
        return Industron.MOD_ID + ":" + part.registryName(material);
    }
}
