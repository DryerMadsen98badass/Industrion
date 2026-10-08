package net.mads.industron.material.recipes;

import net.mads.industron.machine.MachineTier;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.defenitions.StoneMaterials;
import net.mads.industron.material.structure.StoneMaterial;
import net.mads.industron.recipe.CERecipeTypes;
import net.mads.industron.recipe.RecipeDefinition;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/** Physical stone processing. Chemistry/trace separation remains in StoneCentrifugingRecipes. */
public final class StoneProcessingRecipes {
    private static final int STONE_CRUSHING_TICKS = 20 * 8;
    private static final int COBBLED_CRUSHING_TICKS = 20 * 6;
    private static final int GRAVEL_PULVERIZING_TICKS = 20 * 6;
    private static final int SMOOTH_HEATING_TICKS = 20 * 10;
    private static final int CRACK_HEATING_TICKS = 20 * 12;

    private StoneProcessingRecipes() {
    }

    public static void build(RecipeOutput output) {
        for (StoneMaterial stone : StoneMaterials.ALL) {
            Optional<ResourceLocation> stoneBlock = resolveBlock(stone, MaterialPart.STONE);
            Optional<ResourceLocation> cobbledBlock = resolveBlock(stone, MaterialPart.COBBLED_STONE);
            Optional<ResourceLocation> gravelBlock = resolveBlock(stone, MaterialPart.GRAVEL);
            Optional<ResourceLocation> dustItem = resolveDust(stone);

            // 53: Stone -> Cobbled Stone through Crushing whenever both physical forms exist.
            if (stoneBlock.isPresent() && cobbledBlock.isPresent()) {
                saveMachine(output, stone, "stone_to_cobbled", CERecipeTypes.CRUSHING,
                        stoneBlock.get(), cobbledBlock.get(), STONE_CRUSHING_TICKS);
            }

            // 54: Cobbled Stone -> Gravel. This is deliberately independent of whether STONE exists.
            if (cobbledBlock.isPresent() && gravelBlock.isPresent()) {
                saveMachine(output, stone, "cobbled_to_gravel", CERecipeTypes.CRUSHING,
                        cobbledBlock.get(), gravelBlock.get(), COBBLED_CRUSHING_TICKS);
            } else if (cobbledBlock.isEmpty() && stoneBlock.isPresent() && gravelBlock.isPresent()
                    && !stoneBlock.get().equals(gravelBlock.get())) {
                // No COBBLED_STONE part: use the fixed MaterialPart.STONE -> MaterialPart.GRAVEL rule.
                saveMachine(output, stone, "stone_to_gravel", CERecipeTypes.CRUSHING,
                        stoneBlock.get(), gravelBlock.get(), STONE_CRUSHING_TICKS);
            }

            // 55: Gravel -> Dust.
            if (gravelBlock.isPresent() && dustItem.isPresent()) {
                saveMachine(output, stone, "gravel_to_dust", CERecipeTypes.PULVERIZING,
                        gravelBlock.get(), dustItem.get(), GRAVEL_PULVERIZING_TICKS);
            }

            addHeatingRecipes(output, stone);
        }
    }

    private static void addHeatingRecipes(RecipeOutput output, StoneMaterial stone) {
        Optional<ResourceLocation> stoneBlock = resolveBlock(stone, MaterialPart.STONE);
        Optional<ResourceLocation> smoothStone = resolveBlock(stone, MaterialPart.SMOOTH_STONE);

        // Fixed MaterialPart.STONE -> MaterialPart.SMOOTH_STONE rule.
        if (stoneBlock.isPresent() && smoothStone.isPresent()) {
            saveMachine(output, stone, "stone_to_smooth", CERecipeTypes.HEATING,
                    stoneBlock.get(), smoothStone.get(), SMOOTH_HEATING_TICKS);
        }


        addCrackedHeating(output, stone,
                MaterialPart.STONE_BRICKS,
                MaterialPart.CRACKED_STONE_BRICKS,
                "stone_bricks_to_cracked");
        addCrackedHeating(output, stone,
                MaterialPart.STONE_TILES,
                MaterialPart.CRACKED_STONE_TILES,
                "stone_tiles_to_cracked");
        addCrackedHeating(output, stone,
                MaterialPart.POLISHED_STONE_BRICKS,
                MaterialPart.CRACKED_POLISHED_STONE_BRICKS,
                "polished_stone_bricks_to_cracked");
    }

    private static void addCrackedHeating(
            RecipeOutput output,
            StoneMaterial stone,
            MaterialPart inputPart,
            MaterialPart outputPart,
            String id
    ) {
        Optional<ResourceLocation> input = resolveBlock(stone, inputPart);
        Optional<ResourceLocation> result = resolveBlock(stone, outputPart);
        if (input.isEmpty() || result.isEmpty()) return;
        saveMachine(output, stone, id, CERecipeTypes.HEATING, input.get(), result.get(), CRACK_HEATING_TICKS);
    }

    private static void saveMachine(
            RecipeOutput output,
            StoneMaterial stone,
            String name,
            net.mads.industron.recipe.RecipeTypeDefinition type,
            ResourceLocation input,
            ResourceLocation result,
            int durationTicks
    ) {
        RecipeDefinition.recipe()
                .recipeDefinition(RecipeDefinition.Option.id("stone_processing/" + stone.id() + "/" + name))
                .recipeDefinition(RecipeDefinition.Option.recipeType(type))
                .recipeDefinition(RecipeDefinition.Option.inputItem(input.toString(), 1))
                .recipeDefinition(RecipeDefinition.Option.outputItem(result.toString(), 1))
                .recipeDefinition(RecipeDefinition.Option.duration(durationTicks))
                .recipeDefinition(RecipeDefinition.Option.tier(stone.tier() == null
                        ? MachineTier.ULV
                        : MaterialProcessingRules.processingTier(stone.tier())))
                .save(output);
    }

    public static Optional<ResourceLocation> resolveBlock(StoneMaterial material, MaterialPart part) {
        return StoneRecipeResolver.block(material, part);
    }

    public static Optional<ResourceLocation> resolveDust(StoneMaterial material) {
        return StoneRecipeResolver.item(material, MaterialPart.DUST);
    }
}
