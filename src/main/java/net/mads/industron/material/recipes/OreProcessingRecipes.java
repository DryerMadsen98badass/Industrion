package net.mads.industron.material.recipes;

import net.mads.industron.Industron;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.GemMaterialRules;
import net.mads.industron.material.MaterialOreHost;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.defenitions.IndustrialMaterials;
import net.mads.industron.material.structure.StoneMaterial;
import net.mads.industron.material.structure.StructureMaterialGenerator;
import net.mads.industron.recipe.CEChancedItemOutput;
import net.mads.industron.recipe.CERecipeTypes;
import net.mads.industron.recipe.RecipeDefinition;
import net.mads.industron.recipe.RecipeTypeDefinition;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;


/**
 * Material-bound ore preprocessing recipes.
 *
 * <p>This is intentionally separate from chemistry. These recipes only transform the ore material's
 * own preprocessing forms (raw/crushed/washed/refined/dust) and never inspect composition to emit
 * component materials. Gem-bearing ores are only classified so their physical recovery profile can
 * be tuned without hard-coding Diamond, Emerald, or any future gem element.</p>
 */
public final class OreProcessingRecipes {
    private static final int WASHING_WATER_MB = 250;

    private OreProcessingRecipes() {
    }

    public static void build(RecipeOutput output) {
        for (IndustrialMaterial material : IndustrialMaterials.ALL) {
            if (!material.isOreMaterial()) continue;

            OreFamily family = GemMaterialRules.isGemBearing(material) ? OreFamily.GEM : OreFamily.ORE;
            String hostDust = bestHostStoneDust(material);

            buildOreBlockCrushing(output, material, family);
            buildRawCrushing(output, material, family, hostDust);
            buildCrushedWashing(output, material, family, hostDust);
            buildCrushedPulverizing(output, material, family, hostDust);
            buildWashedSifting(output, material, family);
            buildWashedPulverizing(output, material, family);
            buildRefinedPulverizing(output, material, family);
            buildImpureCentrifuging(output, material, family, hostDust);
            buildPurifiedCentrifuging(output, material, family);
        }
    }

    /** Silk-touched host-specific ore blocks enter the same universal chain as mined raw ore. */
    private static void buildOreBlockCrushing(
            RecipeOutput output,
            IndustrialMaterial material,
            OreFamily family
    ) {
        if (!has(material, MaterialPart.CRUSHED_ORE, MaterialPart.TINY_DUST)) return;
        for (MaterialOreHost host : MaterialOreHost.compatibleHosts(material)) {
            buildOreBlockCrushing(output, material, family, host, false);
            buildOreBlockCrushing(output, material, family, host, true);
        }
    }

    private static void buildOreBlockCrushing(
            RecipeOutput output,
            IndustrialMaterial material,
            OreFamily family,
            MaterialOreHost host,
            boolean small
    ) {
        String inputId = host.existingOreBlock(material, small)
                .map(ResourceLocation::toString)
                .orElseGet(() -> ResourceLocation.fromNamespaceAndPath(
                        Industron.MOD_ID, host.registryName(material, small)
                ).toString());
        int amountMb = small ? 72 : 144;
        int crushedCount = small ? 1 : 2;
        int tinyChance = family == OreFamily.GEM ? (small ? 10 : 20) : (small ? 8 : 15);

        RecipeDefinition recipe = recipe(
                material,
                family,
                CERecipeTypes.CRUSHING,
                host.id() + (small ? "_small_ore_block_to_crushed_ore" : "_ore_block_to_crushed_ore"),
                MaterialProcessingRules.crushingDuration(material, amountMb)
        )
                .recipeDefinition(RecipeDefinition.Option.inputItem(inputId, 1))
                .recipeDefinition(RecipeDefinition.Option.outputItem(
                        itemId(material, MaterialPart.CRUSHED_ORE), crushedCount
                ))
                .recipeDefinition(RecipeDefinition.Option.chancedOutputItem(
                        itemId(material, MaterialPart.TINY_DUST), 1, chance(tinyChance)
                ));

        addChancedHostDust(recipe, stoneDustId(host.stone()), small ? 15 : (family == OreFamily.GEM ? 35 : 30));
        recipe.save(output);
    }

    private static void buildRawCrushing(
            RecipeOutput output,
            IndustrialMaterial material,
            OreFamily family,
            String hostDust
    ) {
        if (!has(material, MaterialPart.RAW_ORE, MaterialPart.CRUSHED_ORE, MaterialPart.TINY_DUST)) return;

        RecipeDefinition recipe = recipe(
                material,
                family,
                CERecipeTypes.CRUSHING,
                "raw_ore_to_crushed_ore",
                MaterialProcessingRules.crushingDuration(material, 144)
        )
                .recipeDefinition(RecipeDefinition.Option.inputItem(itemId(material, MaterialPart.RAW_ORE), 1))
                .recipeDefinition(RecipeDefinition.Option.outputItem(itemId(material, MaterialPart.CRUSHED_ORE), 2))
                .recipeDefinition(RecipeDefinition.Option.chancedOutputItem(
                        itemId(material, MaterialPart.TINY_DUST),
                        1,
                        chance(family == OreFamily.GEM ? 20 : 15)
                ));

        addChancedHostDust(recipe, hostDust, family == OreFamily.GEM ? 35 : 30);
        recipe.save(output);
    }

    private static void buildCrushedWashing(
            RecipeOutput output,
            IndustrialMaterial material,
            OreFamily family,
            String hostDust
    ) {
        if (!has(material, MaterialPart.CRUSHED_ORE, MaterialPart.WASHED_CRUSHED_ORE, MaterialPart.TINY_DUST)) return;

        RecipeDefinition recipe = recipe(
                material,
                family,
                CERecipeTypes.WASHING,
                "crushed_ore_to_washed_crushed_ore",
                MaterialProcessingRules.washingDuration(material, 144)
        )
                .recipeDefinition(RecipeDefinition.Option.inputItem(itemId(material, MaterialPart.CRUSHED_ORE), 1))
                .recipeDefinition(RecipeDefinition.Option.inputFluid("minecraft:water", WASHING_WATER_MB))
                .recipeDefinition(RecipeDefinition.Option.outputItem(itemId(material, MaterialPart.WASHED_CRUSHED_ORE), 1))
                .recipeDefinition(RecipeDefinition.Option.chancedOutputItem(
                        itemId(material, MaterialPart.TINY_DUST),
                        1,
                        chance(family == OreFamily.GEM ? 30 : 25)
                ));

        addChancedHostDust(recipe, hostDust, family == OreFamily.GEM ? 55 : 50);
        recipe.save(output);
    }

    private static void buildCrushedPulverizing(
            RecipeOutput output,
            IndustrialMaterial material,
            OreFamily family,
            String hostDust
    ) {
        if (!has(material, MaterialPart.CRUSHED_ORE, MaterialPart.IMPURE_DUST, MaterialPart.TINY_DUST)) return;

        RecipeDefinition recipe = recipe(
                material,
                family,
                CERecipeTypes.PULVERIZING,
                "crushed_ore_to_impure_dust",
                MaterialProcessingRules.pulverizingDuration(material, 144)
        )
                .recipeDefinition(RecipeDefinition.Option.inputItem(itemId(material, MaterialPart.CRUSHED_ORE), 1))
                .recipeDefinition(RecipeDefinition.Option.outputItem(itemId(material, MaterialPart.IMPURE_DUST), 1))
                .recipeDefinition(RecipeDefinition.Option.chancedOutputItem(
                        itemId(material, MaterialPart.TINY_DUST),
                        1,
                        chance(family == OreFamily.GEM ? 25 : 20)
                ));

        addChancedHostDust(recipe, hostDust, 20);
        recipe.save(output);
    }

    private static void buildWashedSifting(
            RecipeOutput output,
            IndustrialMaterial material,
            OreFamily family
    ) {
        // Sifting is the gem-recovery branch. Non-gem ores continue through washed pulverizing.
        if (family != OreFamily.GEM) return;
        if (!has(
                material,
                MaterialPart.WASHED_CRUSHED_ORE,
                MaterialPart.REFINED_ORE,
                MaterialPart.PURIFIED_DUST,
                MaterialPart.ROUGH_TINY_GEM,
                MaterialPart.ROUGH_SMALL_GEM,
                MaterialPart.ROUGH_GEM,
                MaterialPart.ROUGH_FLAWLESS_GEM,
                MaterialPart.ROUGH_EXQUISITE_GEM
        )) return;

        recipe(
                material,
                family,
                CERecipeTypes.SIFTING,
                "washed_crushed_ore_to_refined_ore_and_gems",
                MaterialProcessingRules.siftingDuration(material, 144)
        )
                .recipeDefinition(RecipeDefinition.Option.inputItem(
                        itemId(material, MaterialPart.WASHED_CRUSHED_ORE), 1
                ))
                .recipeDefinition(RecipeDefinition.Option.outputItem(
                        itemId(material, MaterialPart.REFINED_ORE), 1
                ))
                .recipeDefinition(RecipeDefinition.Option.chancedOutputItem(
                        itemId(material, MaterialPart.PURIFIED_DUST), 1, chance(18.0D)
                ))
                .recipeDefinition(RecipeDefinition.Option.chancedOutputItem(
                        itemId(material, MaterialPart.ROUGH_TINY_GEM), 1, chance(30.0D)
                ))
                .recipeDefinition(RecipeDefinition.Option.chancedOutputItem(
                        itemId(material, MaterialPart.ROUGH_SMALL_GEM), 1, chance(18.0D)
                ))
                .recipeDefinition(RecipeDefinition.Option.chancedOutputItem(
                        itemId(material, MaterialPart.ROUGH_GEM), 1, chance(8.0D)
                ))
                .recipeDefinition(RecipeDefinition.Option.chancedOutputItem(
                        itemId(material, MaterialPart.ROUGH_FLAWLESS_GEM), 1, chance(2.0D)
                ))
                .recipeDefinition(RecipeDefinition.Option.chancedOutputItem(
                        itemId(material, MaterialPart.ROUGH_EXQUISITE_GEM), 1, chance(0.5D)
                ))
                .save(output);
    }

    private static void buildWashedPulverizing(
            RecipeOutput output,
            IndustrialMaterial material,
            OreFamily family
    ) {
        if (!has(material, MaterialPart.WASHED_CRUSHED_ORE, MaterialPart.PURIFIED_DUST, MaterialPart.TINY_DUST)) return;

        recipe(
                material,
                family,
                CERecipeTypes.PULVERIZING,
                "washed_crushed_ore_to_purified_dust",
                MaterialProcessingRules.pulverizingDuration(material, 144)
        )
                .recipeDefinition(RecipeDefinition.Option.inputItem(itemId(material, MaterialPart.WASHED_CRUSHED_ORE), 1))
                .recipeDefinition(RecipeDefinition.Option.outputItem(itemId(material, MaterialPart.PURIFIED_DUST), 1))
                .recipeDefinition(RecipeDefinition.Option.chancedOutputItem(
                        itemId(material, MaterialPart.TINY_DUST),
                        1,
                        chance(family == OreFamily.GEM ? 30 : 25)
                ))
                .save(output);
    }

    private static void buildRefinedPulverizing(
            RecipeOutput output,
            IndustrialMaterial material,
            OreFamily family
    ) {
        if (!has(material, MaterialPart.REFINED_ORE, MaterialPart.PURIFIED_DUST, MaterialPart.SMALL_DUST)) return;

        recipe(
                material,
                family,
                CERecipeTypes.PULVERIZING,
                "refined_ore_to_purified_dust",
                MaterialProcessingRules.pulverizingDuration(material, 144)
        )
                .recipeDefinition(RecipeDefinition.Option.inputItem(itemId(material, MaterialPart.REFINED_ORE), 1))
                .recipeDefinition(RecipeDefinition.Option.outputItem(itemId(material, MaterialPart.PURIFIED_DUST), 1))
                .recipeDefinition(RecipeDefinition.Option.chancedOutputItem(
                        itemId(material, MaterialPart.SMALL_DUST),
                        1,
                        chance(family == OreFamily.GEM ? 28 : 24)
                ))
                .save(output);
    }

    private static void buildImpureCentrifuging(
            RecipeOutput output,
            IndustrialMaterial material,
            OreFamily family,
            String hostDust
    ) {
        if (!has(material, MaterialPart.IMPURE_DUST, MaterialPart.DUST)) return;

        RecipeDefinition recipe = recipe(
                material,
                family,
                CERecipeTypes.CENTRIFUGING,
                "impure_dust_to_dust",
                MaterialProcessingRules.centrifugingDuration(material, 144, false)
                )
                .recipeDefinition(RecipeDefinition.Option.inputItem(itemId(material, MaterialPart.IMPURE_DUST), 1))
                .recipeDefinition(RecipeDefinition.Option.outputItem(itemId(material, MaterialPart.DUST), 1));

        addChancedHostDust(recipe, hostDust, 10);
        recipe.save(output);
    }

    private static void buildPurifiedCentrifuging(
            RecipeOutput output,
            IndustrialMaterial material,
            OreFamily family
    ) {
        if (!has(material, MaterialPart.PURIFIED_DUST, MaterialPart.DUST)) return;

        recipe(
                material,
                family,
                CERecipeTypes.CENTRIFUGING,
                "purified_dust_to_dust",
                MaterialProcessingRules.centrifugingDuration(material, 144, true)
                )
                .recipeDefinition(RecipeDefinition.Option.inputItem(itemId(material, MaterialPart.PURIFIED_DUST), 1))
                .recipeDefinition(RecipeDefinition.Option.outputItem(itemId(material, MaterialPart.DUST), 1))
                .save(output);
    }

    private static RecipeDefinition recipe(
            IndustrialMaterial material,
            OreFamily family,
            RecipeTypeDefinition type,
            String path,
            int duration
    ) {
        return RecipeDefinition.recipe()
                .recipeDefinition(RecipeDefinition.Option.id(
                        material.id() + "/ore_processing/" + family.id + "/" + path
                ))
                .recipeDefinition(RecipeDefinition.Option.recipeType(type))
                .recipeDefinition(RecipeDefinition.Option.duration(duration))
                .recipeDefinition(RecipeDefinition.Option.tier(MaterialProcessingRules.processingTier(material)));
    }

    private static void addChancedHostDust(RecipeDefinition recipe, String hostDust, int percent) {
        if (hostDust == null || percent <= 0) return;
        recipe.recipeDefinition(RecipeDefinition.Option.chancedOutputItem(hostDust, 1, chance(percent)));
    }

    /** Uses the first compatible host because MaterialOreHost already returns hosts best-fit first. */
    private static String bestHostStoneDust(IndustrialMaterial material) {
        return MaterialOreHost.compatibleHosts(material).stream()
                .findFirst()
                .map(MaterialOreHost::stone)
                .map(OreProcessingRecipes::stoneDustId)
                .orElse(null);
    }

    private static String stoneDustId(StoneMaterial stone) {
        if (stone.isWithout(MaterialPart.DUST)) return null;
        if (stone.hasExistingPart(MaterialPart.DUST)) return stone.existingPart(MaterialPart.DUST).toString();
        if (!StructureMaterialGenerator.generatedItemForms(stone).contains(MaterialPart.DUST)) return null;
        return ResourceLocation.fromNamespaceAndPath(
                Industron.MOD_ID,
                MaterialPart.DUST.registryName(stone)
        ).toString();
    }

    private static boolean has(IndustrialMaterial material, MaterialPart... parts) {
        for (MaterialPart part : parts) {
            if (!material.has(part)) return false;
        }
        return true;
    }

    private static String itemId(IndustrialMaterial material, MaterialPart part) {
        return MaterialRecipeHelper.itemId(material, part);
    }

    private static int chance(double percent) {
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("Ore processing chance must be 0..100: " + percent);
        }
        return (int) Math.round(percent / 100.0D * CEChancedItemOutput.MAX_CHANCE);
    }


    private enum OreFamily {
        ORE("ore"),
        GEM("gem");

        private final String id;

        OreFamily(String id) {
            this.id = id;
        }
    }
}
