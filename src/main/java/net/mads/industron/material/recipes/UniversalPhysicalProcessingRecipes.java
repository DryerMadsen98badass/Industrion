package net.mads.industron.material.recipes;

import net.mads.industron.fluid.IndustrialFluidLookup;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.MaterialCategory;
import net.mads.industron.material.MaterialFormAmounts;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.defenitions.IndustrialMaterials;
import net.mads.industron.recipe.CERecipeTypes;
import net.mads.industron.recipe.RecipeDefinition;
import net.minecraft.data.recipes.RecipeOutput;

import java.util.ArrayList;
import java.util.List;

/** Universal, chemistry-independent material-form processing derived only from registered forms. */
public final class UniversalPhysicalProcessingRecipes {
    private UniversalPhysicalProcessingRecipes() {
    }

    public static void build(RecipeOutput output) {
        for (IndustrialMaterial material : IndustrialMaterials.ALL) {
            buildRawStorage(output, material);
            buildMaterialBlockStorage(output, material);
            buildDustSizeConversions(output, material);

            if (material.has(MaterialPart.DUST)) {
                for (MaterialPart source : material.parts()) {
                    if (!MaterialFormAmounts.isGenericCrushable(source)) continue;
                    buildCrushing(output, material, source, MaterialFormAmounts.millibuckets(source));
                }
            }

            if (supportsUniversalMelting(material)) {
                for (MaterialPart source : material.parts()) {
                    if (!MaterialFormAmounts.isGenericMeltable(source)) continue;
                    buildMelting(output, material, source, MaterialFormAmounts.millibuckets(source));
                }
            }
        }
    }


    /**
     * Identity-preserving melting is universal for elemental materials and true metallic lattices.
     * Composite ore/mineral/clay families keep melting behind their chemistry route; otherwise a
     * generated MELTING recipe would bypass the post-DUST separation/reduction planner.
     */
    private static boolean supportsUniversalMelting(IndustrialMaterial material) {
        if (!material.has(MaterialPart.MOLTEN_FLUID)) return false;
        if (material.isOreMaterial() || material.isMineralDust()
                || material.isClayMaterial() || material.isCeramicBrickMaterial()) {
            return false;
        }
        return material.components().isEmpty() || MaterialCategory.METAL.matches(material);
    }

    /** Exact physical size conversion for the three canonical dust units. */
    private static void buildDustSizeConversions(RecipeOutput output, IndustrialMaterial material) {
        if (!material.has(MaterialPart.DUST)) return;
        int batchMillibuckets = 144;

        if (material.has(MaterialPart.SMALL_DUST)) {
            RecipeDefinition.recipe()
                    .recipeDefinition(RecipeDefinition.Option.id(material.id() + "/dust_size/dust_to_small_dust"))
                    .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.DECOMPACTING))
                    .recipeDefinition(RecipeDefinition.Option.inputItem(MaterialRecipeHelper.itemId(material, MaterialPart.DUST), 1))
                    .recipeDefinition(RecipeDefinition.Option.outputItem(MaterialRecipeHelper.itemId(material, MaterialPart.SMALL_DUST), 4))
                    .recipeDefinition(RecipeDefinition.Option.duration(MaterialProcessingRules.decompactingDuration(material, batchMillibuckets)))
                    .recipeDefinition(RecipeDefinition.Option.circuit(1))
                    .recipeDefinition(RecipeDefinition.Option.tier(MaterialProcessingRules.processingTier(material)))
                    .save(output);

            RecipeDefinition.recipe()
                    .recipeDefinition(RecipeDefinition.Option.id(material.id() + "/dust_size/small_dust_to_dust"))
                    .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.COMPACTING))
                    .recipeDefinition(RecipeDefinition.Option.inputItem(MaterialRecipeHelper.itemId(material, MaterialPart.SMALL_DUST), 4))
                    .recipeDefinition(RecipeDefinition.Option.outputItem(MaterialRecipeHelper.itemId(material, MaterialPart.DUST), 1))
                    .recipeDefinition(RecipeDefinition.Option.duration(MaterialProcessingRules.compactingDuration(material, batchMillibuckets)))
                    .recipeDefinition(RecipeDefinition.Option.tier(MaterialProcessingRules.processingTier(material)))
                    .save(output);
        }

        if (material.has(MaterialPart.TINY_DUST)) {
            RecipeDefinition.recipe()
                    .recipeDefinition(RecipeDefinition.Option.id(material.id() + "/dust_size/dust_to_tiny_dust"))
                    .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.DECOMPACTING))
                    .recipeDefinition(RecipeDefinition.Option.inputItem(MaterialRecipeHelper.itemId(material, MaterialPart.DUST), 1))
                    .recipeDefinition(RecipeDefinition.Option.outputItem(MaterialRecipeHelper.itemId(material, MaterialPart.TINY_DUST), 9))
                    .recipeDefinition(RecipeDefinition.Option.duration(MaterialProcessingRules.decompactingDuration(material, batchMillibuckets)))
                    .recipeDefinition(RecipeDefinition.Option.circuit(2))
                    .recipeDefinition(RecipeDefinition.Option.tier(MaterialProcessingRules.processingTier(material)))
                    .save(output);

            RecipeDefinition.recipe()
                    .recipeDefinition(RecipeDefinition.Option.id(material.id() + "/dust_size/tiny_dust_to_dust"))
                    .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.COMPACTING))
                    .recipeDefinition(RecipeDefinition.Option.inputItem(MaterialRecipeHelper.itemId(material, MaterialPart.TINY_DUST), 9))
                    .recipeDefinition(RecipeDefinition.Option.outputItem(MaterialRecipeHelper.itemId(material, MaterialPart.DUST), 1))
                    .recipeDefinition(RecipeDefinition.Option.duration(MaterialProcessingRules.compactingDuration(material, batchMillibuckets)))
                    .recipeDefinition(RecipeDefinition.Option.tier(MaterialProcessingRules.processingTier(material)))
                    .save(output);
        }
    }

    /** RAW_BLOCK is storage only: exact 9:1 packing, with no ore-processing/crushing role. */
    private static void buildRawStorage(RecipeOutput output, IndustrialMaterial material) {
        if (!material.has(MaterialPart.RAW_ORE) || !material.has(MaterialPart.RAW_BLOCK)) return;
        buildStoragePair(output, material, MaterialPart.RAW_ORE, MaterialPart.RAW_BLOCK, 9);
    }

    /**
     * Canonical material storage blocks are identity-preserving packing, not crafting chemistry.
     * Metals pack INGOT <-> BLOCK; true gem materials pack GEM <-> BLOCK. A dust-storage block
     * gets its own explicit form later rather than treating every generic BLOCK as compressed dust.
     */
    private static void buildMaterialBlockStorage(RecipeOutput output, IndustrialMaterial material) {
        if (!material.has(MaterialPart.BLOCK)) return;

        if (MaterialCategory.GEM.matches(material) && material.has(MaterialPart.GEM)) {
            buildStoragePair(output, material, MaterialPart.GEM, MaterialPart.BLOCK, 9);
            return;
        }
        if (MaterialCategory.METAL.matches(material) && material.has(MaterialPart.INGOT)) {
            buildStoragePair(output, material, MaterialPart.INGOT, MaterialPart.BLOCK, 9);
        }
    }

    private static void buildStoragePair(
            RecipeOutput output,
            IndustrialMaterial material,
            MaterialPart unitPart,
            MaterialPart storagePart,
            int unitsPerStorage
    ) {
        if (unitsPerStorage < 2) {
            throw new IllegalArgumentException("Storage packing must contain at least two units");
        }
        int unitAmount = MaterialFormAmounts.millibuckets(unitPart);
        int storageAmount = MaterialFormAmounts.millibuckets(storagePart);
        if (unitAmount <= 0 || storageAmount != Math.multiplyExact(unitAmount, unitsPerStorage)) {
            throw new IllegalStateException("Storage amount mismatch for " + material.id() + ": "
                    + unitsPerStorage + "x " + unitPart.id() + " does not equal " + storagePart.id());
        }

        int totalAmount = storageAmount;
        RecipeDefinition.recipe()
                .recipeDefinition(RecipeDefinition.Option.id(
                        material.id() + "/storage/" + unitPart.id() + "_to_" + storagePart.id()
                ))
                .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.COMPACTING))
                .recipeDefinition(RecipeDefinition.Option.inputItem(
                        MaterialRecipeHelper.itemId(material, unitPart), unitsPerStorage
                ))
                .recipeDefinition(RecipeDefinition.Option.outputItem(
                        MaterialRecipeHelper.itemId(material, storagePart), 1
                ))
                .recipeDefinition(RecipeDefinition.Option.duration(
                        MaterialProcessingRules.compactingDuration(material, totalAmount)
                ))
                .recipeDefinition(RecipeDefinition.Option.tier(MaterialProcessingRules.processingTier(material)))
                .save(output);

        RecipeDefinition.recipe()
                .recipeDefinition(RecipeDefinition.Option.id(
                        material.id() + "/storage/" + storagePart.id() + "_to_" + unitPart.id()
                ))
                .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.DECOMPACTING))
                .recipeDefinition(RecipeDefinition.Option.inputItem(
                        MaterialRecipeHelper.itemId(material, storagePart), 1
                ))
                .recipeDefinition(RecipeDefinition.Option.outputItem(
                        MaterialRecipeHelper.itemId(material, unitPart), unitsPerStorage
                ))
                .recipeDefinition(RecipeDefinition.Option.duration(
                        MaterialProcessingRules.decompactingDuration(material, totalAmount)
                ))
                .recipeDefinition(RecipeDefinition.Option.tier(MaterialProcessingRules.processingTier(material)))
                .save(output);
    }

    /** Phase change only: identity and exact material amount are preserved. */
    private static void buildMelting(
            RecipeOutput output,
            IndustrialMaterial material,
            MaterialPart source,
            int sourceMillibuckets
    ) {
        if (sourceMillibuckets <= 0) return;
        int temperature = material.properties().hasProperty("meltingPoint")
                ? Math.max(1, material.meltingPoint())
                : Math.max(1, material.castTemperature());

        RecipeDefinition.recipe()
                .recipeDefinition(RecipeDefinition.Option.id(
                        material.id() + "/universal_melting/" + source.id() + "_to_molten"
                ))
                .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.MELTING))
                .recipeDefinition(RecipeDefinition.Option.inputItem(MaterialRecipeHelper.itemId(material, source), 1))
                .recipeDefinition(RecipeDefinition.Option.outputFluid(
                        IndustrialFluidLookup.fluidId(material, MaterialPart.MOLTEN_FLUID).toString(), sourceMillibuckets
                ))
                .recipeDefinition(RecipeDefinition.Option.duration(
                        MaterialProcessingRules.meltingDuration(material, sourceMillibuckets)
                ))
                .recipeDefinition(RecipeDefinition.Option.coilTemperature(temperature))
                .recipeDefinition(RecipeDefinition.Option.tier(
                        MaterialProcessingRules.processingTier(material)
                ))
                .save(output);
    }

    private static void buildCrushing(
            RecipeOutput output,
            IndustrialMaterial material,
            MaterialPart source,
            int sourceMillibuckets
    ) {
        DustBatch batch = smallestExactDustBatch(material, sourceMillibuckets);
        if (batch == null) return;

        RecipeDefinition recipe = RecipeDefinition.recipe()
                .recipeDefinition(RecipeDefinition.Option.id(
                        material.id() + "/universal_crushing/" + source.id() + "_to_dust"
                ))
                .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.CRUSHING))
                .recipeDefinition(RecipeDefinition.Option.inputItem(MaterialRecipeHelper.itemId(material, source), batch.inputCount()))
                .recipeDefinition(RecipeDefinition.Option.duration(MaterialProcessingRules.crushingDuration(
                        material, Math.multiplyExact(sourceMillibuckets, batch.inputCount())
                )))
                .recipeDefinition(RecipeDefinition.Option.tier(MaterialProcessingRules.processingTier(material)));

        for (DustOutput dust : batch.outputs()) {
            recipe.recipeDefinition(RecipeDefinition.Option.outputItem(
                    MaterialRecipeHelper.itemId(material, dust.part()), dust.count()
            ));
        }
        recipe.save(output);
    }

    private static DustBatch smallestExactDustBatch(IndustrialMaterial material, int sourceMillibuckets) {
        if (sourceMillibuckets <= 0) return null;
        for (int inputCount = 1; inputCount <= 64; inputCount++) {
            int total = Math.multiplyExact(sourceMillibuckets, inputCount);
            List<DustOutput> outputs = exactDustOutputs(material, total);
            if (!outputs.isEmpty() && outputs.size() <= CERecipeTypes.CRUSHING.maxItemOutputs()) {
                return new DustBatch(inputCount, outputs);
            }
        }
        return null;
    }

    private static List<DustOutput> exactDustOutputs(IndustrialMaterial material, int millibuckets) {
        MaterialPart[] parts = {MaterialPart.DUST, MaterialPart.SMALL_DUST, MaterialPart.TINY_DUST};
        int[] amounts = {144, 36, 16};

        DustSolution best = null;
        int maxDust = material.has(parts[0]) ? Math.min(64, millibuckets / amounts[0]) : 0;
        for (int dust = maxDust; dust >= 0; dust--) {
            int afterDust = millibuckets - dust * amounts[0];
            int maxSmall = material.has(parts[1]) ? Math.min(64, afterDust / amounts[1]) : 0;
            for (int small = maxSmall; small >= 0; small--) {
                int remainder = afterDust - small * amounts[1];
                if (remainder < 0) continue;
                int tiny = 0;
                if (remainder > 0) {
                    if (!material.has(parts[2]) || remainder % amounts[2] != 0) continue;
                    tiny = remainder / amounts[2];
                    if (tiny > 64) continue;
                }
                int stacks = (dust > 0 ? 1 : 0) + (small > 0 ? 1 : 0) + (tiny > 0 ? 1 : 0);
                if (stacks == 0) continue;
                int items = dust + small + tiny;
                DustSolution candidate = new DustSolution(dust, small, tiny, stacks, items);
                if (best == null || candidate.betterThan(best)) best = candidate;
            }
        }
        if (best == null) return List.of();

        List<DustOutput> result = new ArrayList<>(3);
        if (best.dust() > 0) result.add(new DustOutput(MaterialPart.DUST, best.dust()));
        if (best.small() > 0) result.add(new DustOutput(MaterialPart.SMALL_DUST, best.small()));
        if (best.tiny() > 0) result.add(new DustOutput(MaterialPart.TINY_DUST, best.tiny()));
        return List.copyOf(result);
    }

    private record DustBatch(int inputCount, List<DustOutput> outputs) {
        private DustBatch {
            outputs = List.copyOf(outputs);
        }
    }

    private record DustOutput(MaterialPart part, int count) {
    }

    private record DustSolution(int dust, int small, int tiny, int stacks, int items) {
        private boolean betterThan(DustSolution other) {
            // Prefer the least fragmented exact result first. Only then optimize slot count.
            // This prevents a large form from turning into dozens of Tiny Dust merely because
            // that occupies one output slot instead of two sensible larger dust stacks.
            if (items != other.items) return items < other.items;
            if (stacks != other.stacks) return stacks < other.stacks;
            if (dust != other.dust) return dust > other.dust;
            return small > other.small;
        }
    }
}
