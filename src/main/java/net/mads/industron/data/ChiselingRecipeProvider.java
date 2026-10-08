package net.mads.industron.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.mads.industron.Industron;
import net.mads.industron.item.SimpleItemDefinition;
import net.mads.industron.item.SimpleItems;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.defenitions.IndustrialMaterials;
import net.mads.industron.material.defenitions.StoneMaterials;
import net.mads.industron.material.defenitions.WoodMaterials;
import net.mads.industron.material.recipes.StoneRecipeResolver;
import net.mads.industron.material.structure.StoneMaterial;
import net.mads.industron.material.structure.StructureMaterialGenerator;
import net.mads.industron.material.structure.WoodMaterial;
import net.mads.industron.recipe.recipes.chiseling.FoundryAbilityRecipes;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Generates the manual block-face pattern recipes used for real carved stone forms and terracotta casting molds. */
public final class ChiselingRecipeProvider implements DataProvider {
    private static final List<Integer> CUT = List.of(1, 2, 3, 6, 9, 8, 7, 4, 5);
    private static final List<Integer> BRICKS = List.of(1, 4, 7, 2, 5, 8, 3, 6, 9);
    private static final List<Integer> CHISELED = List.of(1, 5, 3, 9, 7, 6, 5, 1, 2);
    private static final List<Integer> TILES = List.of(1, 3, 7, 9, 2, 8, 4, 6, 5);
    private static final List<Integer> CHISELED_BRICKS = List.of(1, 3, 9, 7, 5, 2, 8, 4, 6);
    private static final List<Integer> CHISELED_POLISHED = List.of(1, 2, 3, 6, 9, 8, 7, 4, 5);
    private static final List<Integer> CUT_BRICKS = List.of(1, 3, 7, 9, 2, 8, 4, 6, 5);
    private static final List<Integer> SMALL_BRICKS = List.of(1, 2, 4, 5, 3, 6, 7, 8, 9);
    private static final List<Integer> LAYERED = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9);
    private static final List<Integer> PILLAR = List.of(2, 5, 8, 1, 4, 7, 3, 6, 9);
    /** Carve the rim around a cobbled slab, then hollow the centre. */
    private static final List<Integer> BASIN = List.of(1, 2, 3, 6, 9, 8, 7, 4, 5);
    /** Cut four mold corners first, then finish the brick cavity. */
    private static final List<Integer> BRICK_MOLD = List.of(1, 3, 7, 9, 2, 4, 6, 8, 5);
    /** Open the kiln face and chamber through a full cobbled-stone blank. */
    private static final List<Integer> KILN = List.of(1, 4, 7, 8, 9, 6, 3, 2, 5);

    /** Wood utility carving patterns. These are distinct when they share the same wood base. */
    private static final List<Integer> WOOD_CHISELED_BOOKSHELF = List.of(1, 5, 3, 9, 7, 6, 5, 1, 2);
    private static final List<Integer> WOOD_BOWL = List.of(1, 2, 3, 6, 9, 8, 7, 4, 5);
    private static final List<Integer> WOOD_MOSAIC = List.of(2, 5, 8, 1, 4, 7, 3, 6, 9);

    /** All clay Casters share this route. The first hit differs from Faucet and all Foundry abilities. */
    private static final List<Integer> CASTER = List.of(1, 3, 7, 9, 2, 4, 6, 8, 5);
    /** All clay Faucets share this route. The first hit differs from Caster and all Foundry abilities. */
    private static final List<Integer> FAUCET = List.of(2, 5, 8, 1, 4, 7, 3, 6, 9);

    /** Only dyed/coloured terracotta is a valid raw casting-mold blank. Plain terracotta is intentionally excluded. */
    private static final List<ResourceLocation> COLORED_TERRACOTTA = List.of(
            minecraft("white_terracotta"),
            minecraft("orange_terracotta"),
            minecraft("magenta_terracotta"),
            minecraft("light_blue_terracotta"),
            minecraft("yellow_terracotta"),
            minecraft("lime_terracotta"),
            minecraft("pink_terracotta"),
            minecraft("gray_terracotta"),
            minecraft("light_gray_terracotta"),
            minecraft("cyan_terracotta"),
            minecraft("purple_terracotta"),
            minecraft("blue_terracotta"),
            minecraft("brown_terracotta"),
            minecraft("green_terracotta"),
            minecraft("red_terracotta"),
            minecraft("black_terracotta")
    );

    private final PackOutput.PathProvider recipes;

    public ChiselingRecipeProvider(PackOutput output) {
        this.recipes = output.createPathProvider(PackOutput.Target.DATA_PACK, "recipe");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        Map<ResourceLocation, Set<List<Integer>>> patternsByBase = new HashMap<>();

        addStoneRecipes(futures, output, patternsByBase);
        addWoodRecipes(futures, output, patternsByBase);
        addTerracottaMoldRecipes(futures, output, patternsByBase);
        addClayCastingBlockRecipes(futures, output, patternsByBase);
        addFoundryAbilityRecipes(futures, output, patternsByBase);

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private void addStoneRecipes(
            List<CompletableFuture<?>> futures,
            CachedOutput output,
            Map<ResourceLocation, Set<List<Integer>>> patternsByBase
    ) {
        for (StoneMaterial stone : StoneMaterials.ALL) {
            Optional<ResourceLocation> dust = StoneRecipeResolver.item(stone, MaterialPart.DUST);
            if (dust.isEmpty()) continue;

            // Basin acquisition is also material-driven: the input is always this StoneMaterial's
            // COBBLED_SLAB MaterialPart, falling back to SLAB only when that stone has no cobbled slab. Basin itself is a generated primitive machine rather than
            // a MaterialPart, so its registry id follows the existing <stone>_basin machine id.
            addBlockRecipe(futures, output, patternsByBase, stone, "basin",
                    StoneRecipeResolver.block(stone, MaterialPart.COBBLED_SLAB, MaterialPart.SLAB),
                    Optional.of(ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, stone.id() + "_basin")),
                    StoneRecipeResolver.item(stone, MaterialPart.SMALL_DUST), BASIN);

            // Primitive machine acquisition stays material-derived too. Brick Mold uses the same
            // cobbled-slab family as Basin but has its own non-colliding face pattern.
            addBlockRecipe(futures, output, patternsByBase, stone, "brick_mold",
                    StoneRecipeResolver.block(stone, MaterialPart.COBBLED_SLAB, MaterialPart.SLAB),
                    Optional.of(ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, stone.id() + "_brick_mold")),
                    StoneRecipeResolver.item(stone, MaterialPart.SMALL_DUST), BRICK_MOLD);

            // Kiln is carved from the full cobbled-stone form, falling back to normal stone only
            // for a StoneMaterial that deliberately does not expose COBBLED_STONE.
            addBlockRecipe(futures, output, patternsByBase, stone, "kiln",
                    StoneRecipeResolver.block(stone, MaterialPart.COBBLED_STONE, MaterialPart.STONE),
                    Optional.of(ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, stone.id() + "_kiln")),
                    StoneRecipeResolver.item(stone, MaterialPart.SMALL_DUST), KILN);

            // Recipes are defined strictly as MaterialPart -> MaterialPart relationships.
            // Registry ids never change recipe semantics.
            addBlockRecipe(futures, output, patternsByBase, stone, "cut_stone",
                    StoneRecipeResolver.block(stone, MaterialPart.STONE),
                    StoneRecipeResolver.block(stone, MaterialPart.CUT_STONE), dust, CUT);

            // Pattern-carved full-block forms. These intentionally do not live in Assembly.
            addBlockRecipe(futures, output, patternsByBase, stone, "stone_bricks",
                    StoneRecipeResolver.block(stone, MaterialPart.STONE),
                    StoneRecipeResolver.block(stone, MaterialPart.STONE_BRICKS), dust, BRICKS);
            addBlockRecipe(futures, output, patternsByBase, stone, "chiseled_stone",
                    StoneRecipeResolver.block(stone, MaterialPart.STONE),
                    StoneRecipeResolver.block(stone, MaterialPart.CHISELED_STONE), dust, CHISELED);

            addBlockRecipe(futures, output, patternsByBase, stone, "polished_stone_bricks",
                    StoneRecipeResolver.block(stone, MaterialPart.POLISHED_STONE),
                    StoneRecipeResolver.block(stone, MaterialPart.POLISHED_STONE_BRICKS), dust, BRICKS);
            addBlockRecipe(futures, output, patternsByBase, stone, "chiseled_polished_stone",
                    StoneRecipeResolver.block(stone, MaterialPart.POLISHED_STONE),
                    StoneRecipeResolver.block(stone, MaterialPart.CHISELED_POLISHED_STONE), dust, CHISELED_POLISHED);

            addBlockRecipe(futures, output, patternsByBase, stone, "stone_tiles",
                    StoneRecipeResolver.block(stone, MaterialPart.STONE_BRICKS),
                    StoneRecipeResolver.block(stone, MaterialPart.STONE_TILES), dust, TILES);
            addBlockRecipe(futures, output, patternsByBase, stone, "chiseled_stone_bricks",
                    StoneRecipeResolver.block(stone, MaterialPart.STONE_BRICKS),
                    StoneRecipeResolver.block(stone, MaterialPart.CHISELED_STONE_BRICKS), dust, CHISELED_BRICKS);

            addBlockRecipe(futures, output, patternsByBase, stone, "cut_stone_bricks",
                    StoneRecipeResolver.block(stone, MaterialPart.CUT_STONE),
                    StoneRecipeResolver.block(stone, MaterialPart.CUT_STONE_BRICKS), dust, CUT_BRICKS);
            addBlockRecipe(futures, output, patternsByBase, stone, "layered_stone",
                    StoneRecipeResolver.block(stone, MaterialPart.CUT_STONE),
                    StoneRecipeResolver.block(stone, MaterialPart.LAYERED_STONE), dust, LAYERED);
            addBlockRecipe(futures, output, patternsByBase, stone, "pillar",
                    StoneRecipeResolver.block(stone, MaterialPart.CUT_STONE),
                    StoneRecipeResolver.block(stone, MaterialPart.PILLAR), dust, PILLAR);

            addBlockRecipe(futures, output, patternsByBase, stone, "small_stone_bricks",
                    StoneRecipeResolver.block(stone, MaterialPart.CUT_STONE_BRICKS),
                    StoneRecipeResolver.block(stone, MaterialPart.SMALL_STONE_BRICKS), dust, SMALL_BRICKS);
        }
    }

    private void addWoodRecipes(
            List<CompletableFuture<?>> futures,
            CachedOutput output,
            Map<ResourceLocation, Set<List<Integer>>> patternsByBase
    ) {
        for (WoodMaterial wood : WoodMaterials.ALL) {
            Optional<ResourceLocation> pulp = woodPart(wood, MaterialPart.WOOD_PULP);

            // Chiseled Bookshelf is carved from the normal plank block. It is a Chiseling
            // process, not an Assembly recipe: the compartments are removed from one wood blank.
            addWoodBlockRecipe(
                    futures, output, patternsByBase, wood, "chiseled_bookshelf",
                    woodPart(wood, MaterialPart.PLANKS),
                    woodPart(wood, MaterialPart.CHISELED_BOOKSHELF),
                    pulp, WOOD_CHISELED_BOOKSHELF
            );

            // One slab is hollowed into exactly one bowl. Wood pulp is the physical chiseling
            // residue and also serves as the wrong-pattern remainder for this wood process.
            addWoodItemRecipe(
                    futures, output, patternsByBase, wood, "bowl",
                    woodPart(wood, MaterialPart.SLAB),
                    woodPart(wood, MaterialPart.BOWL),
                    pulp, WOOD_BOWL
            );

            // Only wood materials that actually expose MOSAIC receive this route today (Bamboo).
            // Keeping it MaterialPart-driven means future woods can opt in without special code.
            addWoodBlockRecipe(
                    futures, output, patternsByBase, wood, "mosaic",
                    woodPart(wood, MaterialPart.PLANKS),
                    woodPart(wood, MaterialPart.MOSAIC),
                    pulp, WOOD_MOSAIC
            );
        }
    }

    private void addWoodBlockRecipe(
            List<CompletableFuture<?>> futures,
            CachedOutput output,
            Map<ResourceLocation, Set<List<Integer>>> patternsByBase,
            WoodMaterial wood,
            String name,
            Optional<ResourceLocation> base,
            Optional<ResourceLocation> result,
            Optional<ResourceLocation> byproduct,
            List<Integer> pattern
    ) {
        if (base.isEmpty() || result.isEmpty() || base.get().equals(result.get())) return;
        reservePattern(patternsByBase, List.of(base.get()), pattern, wood.id() + "/" + name);

        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", Industron.MOD_ID + ":chiseling");
        recipe.addProperty("base_block_input", base.get().toString());
        JsonArray patternJson = new JsonArray();
        pattern.forEach(patternJson::add);
        recipe.add("pattern", patternJson);
        recipe.addProperty("base_block_output", result.get().toString());
        byproduct.ifPresent(id -> recipe.addProperty("dust_output", id.toString()));
        recipe.addProperty("tier", wood.tier().id());

        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                Industron.MOD_ID, "chiseling/wood/" + wood.id() + "/" + name
        );
        futures.add(DataProvider.saveStable(output, recipe, recipes.json(id)));
    }

    private void addWoodItemRecipe(
            List<CompletableFuture<?>> futures,
            CachedOutput output,
            Map<ResourceLocation, Set<List<Integer>>> patternsByBase,
            WoodMaterial wood,
            String name,
            Optional<ResourceLocation> base,
            Optional<ResourceLocation> result,
            Optional<ResourceLocation> byproduct,
            List<Integer> pattern
    ) {
        if (base.isEmpty() || result.isEmpty()) return;
        reservePattern(patternsByBase, List.of(base.get()), pattern, wood.id() + "/" + name);

        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", Industron.MOD_ID + ":chiseling");
        recipe.addProperty("base_block_input", base.get().toString());
        JsonArray patternJson = new JsonArray();
        pattern.forEach(patternJson::add);
        recipe.add("pattern", patternJson);
        recipe.addProperty("item_output", result.get().toString());
        byproduct.ifPresent(id -> recipe.addProperty("dust_output", id.toString()));
        recipe.addProperty("tier", wood.tier().id());

        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                Industron.MOD_ID, "chiseling/wood/" + wood.id() + "/" + name
        );
        futures.add(DataProvider.saveStable(output, recipe, recipes.json(id)));
    }

    private void addTerracottaMoldRecipes(
            List<CompletableFuture<?>> futures,
            CachedOutput output,
            Map<ResourceLocation, Set<List<Integer>>> patternsByBase
    ) {
        int patternIndex = 0;
        for (SimpleItemDefinition definition : SimpleItems.ALL) {
            if (!definition.id().startsWith("terracotta_")) continue;

            List<Integer> pattern = terracottaPattern(patternIndex++);
            reservePattern(patternsByBase, COLORED_TERRACOTTA, pattern,
                    "terracotta mold " + definition.id());

            JsonObject recipe = new JsonObject();
            recipe.addProperty("type", Industron.MOD_ID + ":chiseling");

            JsonArray bases = new JsonArray();
            COLORED_TERRACOTTA.forEach(id -> bases.add(id.toString()));
            recipe.add("base_block_inputs", bases);

            JsonArray patternJson = new JsonArray();
            pattern.forEach(patternJson::add);
            recipe.add("pattern", patternJson);

            recipe.addProperty("item_output", ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, definition.id()).toString());
            recipe.addProperty("tier", MachineTier.ULV.id());

            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                    Industron.MOD_ID,
                    "chiseling/terracotta_molds/" + definition.id().substring("terracotta_".length())
            );
            futures.add(DataProvider.saveStable(output, recipe, recipes.json(id)));
        }
    }

    /**
     * Material-driven acquisition for Caster/Faucet. New clay definitions automatically gain both
     * recipes. Every Caster uses the same pattern and every Faucet uses the same pattern; only the
     * clay BRICKS input and matching clay machine output change.
     */
    private void addClayCastingBlockRecipes(
            List<CompletableFuture<?>> futures,
            CachedOutput output,
            Map<ResourceLocation, Set<List<Integer>>> patternsByBase
    ) {
        for (IndustrialMaterial clay : IndustrialMaterials.ALL) {
            if (!clay.isClayMaterial()) continue;

            Optional<ResourceLocation> bricks = materialPart(clay, MaterialPart.BRICKS);
            Optional<ResourceLocation> dust = materialPart(clay, MaterialPart.SMALL_DUST);
            if (bricks.isEmpty()) continue;

            addClayFoundryBlockRecipe(
                    futures, output, patternsByBase, clay,
                    "caster", bricks.get(),
                    ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, clay.id() + "_caster"),
                    dust, CASTER
            );
            addClayFoundryBlockRecipe(
                    futures, output, patternsByBase, clay,
                    "faucet", bricks.get(),
                    ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, clay.id() + "_faucet"),
                    dust, FAUCET
            );
        }
    }

    /**
     * Fixed/private recipes for the Foundry controller and ability blocks. Their interaction
     * patterns live in recipe/recipes; this provider only resolves each current clay BRICKS block
     * to the same fixed output.
     */
    private void addFoundryAbilityRecipes(
            List<CompletableFuture<?>> futures,
            CachedOutput output,
            Map<ResourceLocation, Set<List<Integer>>> patternsByBase
    ) {
        for (FoundryAbilityRecipes.Definition definition : FoundryAbilityRecipes.ALL) {
            for (IndustrialMaterial clay : IndustrialMaterials.ALL) {
                if (!clay.isClayMaterial()) continue;

                Optional<ResourceLocation> bricks = materialPart(clay, MaterialPart.BRICKS);
                Optional<ResourceLocation> dust = materialPart(clay, MaterialPart.SMALL_DUST);
                if (bricks.isEmpty()) continue;

                addClayFoundryBlockRecipe(
                        futures, output, patternsByBase, clay,
                        "abilities/" + definition.id(), bricks.get(),
                        ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, definition.output().id()),
                        dust, definition.pattern()
                );
            }
        }
    }

    private void addClayFoundryBlockRecipe(
            List<CompletableFuture<?>> futures,
            CachedOutput output,
            Map<ResourceLocation, Set<List<Integer>>> patternsByBase,
            IndustrialMaterial clay,
            String name,
            ResourceLocation base,
            ResourceLocation result,
            Optional<ResourceLocation> dust,
            List<Integer> pattern
    ) {
        if (base.equals(result)) return;
        reservePattern(patternsByBase, List.of(base), pattern, clay.id() + "/" + name);

        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", Industron.MOD_ID + ":chiseling");
        recipe.addProperty("base_block_input", base.toString());

        JsonArray patternJson = new JsonArray();
        pattern.forEach(patternJson::add);
        recipe.add("pattern", patternJson);

        recipe.addProperty("base_block_output", result.toString());
        dust.ifPresent(id -> recipe.addProperty("dust_output", id.toString()));
        recipe.addProperty("tier", clay.tier().id());

        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                Industron.MOD_ID,
                "chiseling/foundry/" + name + "/" + clay.id()
        );
        futures.add(DataProvider.saveStable(output, recipe, recipes.json(id)));
    }

    private static Optional<ResourceLocation> woodPart(WoodMaterial wood, MaterialPart part) {
        if (wood == null || part == null) return Optional.empty();
        if (wood.hasExistingPart(part)) return Optional.of(wood.existingPart(part));
        if (part.isItem() && wood.generatedForms().contains(part)) {
            return Optional.of(ResourceLocation.fromNamespaceAndPath(
                    Industron.MOD_ID, part.registryName(wood)
            ));
        }
        if (part.isBlock()) {
            boolean generated = StructureMaterialGenerator.blockDefinitions(wood).stream()
                    .anyMatch(definition -> definition.part().filter(candidate -> candidate == part).isPresent());
            if (generated) {
                return Optional.of(ResourceLocation.fromNamespaceAndPath(
                        Industron.MOD_ID, part.registryName(wood)
                ));
            }
        }
        return Optional.empty();
    }

    private static Optional<ResourceLocation> materialPart(IndustrialMaterial material, MaterialPart part) {
        if (material == null || part == null || !material.has(part)) return Optional.empty();
        if (material.hasExistingPart(part)) return Optional.of(material.existingPart(part));
        return Optional.of(ResourceLocation.fromNamespaceAndPath(
                Industron.MOD_ID,
                part.registryName(material)
        ));
    }

    private void addBlockRecipe(
            List<CompletableFuture<?>> futures,
            CachedOutput output,
            Map<ResourceLocation, Set<List<Integer>>> patternsByBase,
            StoneMaterial stone,
            String name,
            Optional<ResourceLocation> base,
            Optional<ResourceLocation> result,
            Optional<ResourceLocation> dust,
            List<Integer> pattern
    ) {
        if (base.isEmpty() || result.isEmpty() || dust.isEmpty() || base.get().equals(result.get())) return;
        if (pattern.size() != 9) {
            throw new IllegalStateException("Chiseling pattern '" + name + "' must contain exactly 9 hits");
        }
        reservePattern(patternsByBase, List.of(base.get()), pattern, stone.id() + "/" + name);

        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", Industron.MOD_ID + ":chiseling");
        recipe.addProperty("base_block_input", base.get().toString());

        JsonArray patternJson = new JsonArray();
        pattern.forEach(patternJson::add);
        recipe.add("pattern", patternJson);

        recipe.addProperty("base_block_output", result.get().toString());
        recipe.addProperty("dust_output", dust.get().toString());
        recipe.addProperty("tier", stone.tier().id());

        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                Industron.MOD_ID,
                "chiseling/" + stone.id() + "/" + name
        );
        futures.add(DataProvider.saveStable(output, recipe, recipes.json(id)));
    }

    private static void reservePattern(
            Map<ResourceLocation, Set<List<Integer>>> patternsByBase,
            List<ResourceLocation> bases,
            List<Integer> pattern,
            String name
    ) {
        if (pattern.size() != 9) {
            throw new IllegalStateException("Chiseling pattern '" + name + "' must contain exactly 9 hits");
        }
        List<Integer> immutablePattern = List.copyOf(pattern);
        for (ResourceLocation base : bases) {
            Set<List<Integer>> used = patternsByBase.computeIfAbsent(base, ignored -> new HashSet<>());
            if (!used.add(immutablePattern)) {
                throw new IllegalStateException(
                        "Duplicate Chiseling pattern for base " + base + " while generating " + name
                );
            }
        }
    }

    /**
     * Deterministically maps the mold index to a unique permutation of cells 1..9.
     * Every terracotta mold therefore has its own pattern while remaining stable across datagen runs.
     */
    private static List<Integer> terracottaPattern(int index) {
        if (index < 0) throw new IllegalArgumentException("Pattern index cannot be negative");
        List<Integer> available = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9));
        List<Integer> pattern = new ArrayList<>(9);
        int value = index;
        while (!available.isEmpty()) {
            int pick = value % available.size();
            value /= available.size();
            pattern.add(available.remove(pick));
        }
        if (value != 0) {
            throw new IllegalStateException("Ran out of unique 3x3 Chiseling permutations for terracotta molds");
        }
        return List.copyOf(pattern);
    }

    private static ResourceLocation minecraft(String path) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", path);
    }


    @Override
    public String getName() {
        return "Industron Chiseling Recipes";
    }
}
