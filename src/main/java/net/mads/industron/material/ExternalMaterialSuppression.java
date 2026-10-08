package net.mads.industron.material;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.mads.industron.Industron;
import net.mads.industron.material.defenitions.WoodMaterials;
import net.mads.industron.material.defenitions.StoneMaterials;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Set;

/**
 * Central policy for external Minecraft/Create material families that Industron replaces.
 *
 * <p>The registry entries themselves remain untouched so commands, loot tables, structures
 * and old worlds can still reference them. Presentation and external recipes are suppressed
 * separately; this lets functional blocks remain available while Industron owns material
 * progression and later supplies its own recipes.</p>
 */
public final class ExternalMaterialSuppression {
    private static final Set<String> EXTERNAL_NAMESPACES = Set.of("minecraft", "create");

    /** Create processing families whose recipes are now generated from canonical Industron recipes. */
    private static final Set<String> REPLACED_CREATE_RECIPE_TYPES = Set.of(
            "create:milling",
            "create:crushing",
            "create:mixing",
            "create:splashing",
            "create:cutting",
            "create:sandpaper_polishing",
            "create:compacting",
            "create:pressing"
    );

    /** Create-owned recipe families intentionally left alone until Industron has an agreed mapping. */
    private static final Set<String> PRESERVED_CREATE_RECIPE_TYPES = Set.of(
            "create:haunting",
            "create:sequenced_assembly",
            "create:mechanical_crafting",
            "create:filling",
            "create:emptying",
            "minecraft:crafting_shaped",
            "minecraft:crafting_shapeless"
    );

    /** External wood outputs now owned by the generated Industron Assembly progression. */
    private static final Set<MaterialPart> REPLACED_WOOD_PARTS = java.util.EnumSet.of(
            MaterialPart.STRIPPED_LOG,
            MaterialPart.WOOD,
            MaterialPart.STRIPPED_WOOD,
            MaterialPart.PLANKS,
            MaterialPart.SLAB,
            MaterialPart.STAIRS,
            MaterialPart.FENCE,
            MaterialPart.FENCE_GATE,
            MaterialPart.BUTTON,
            MaterialPart.PRESSURE_PLATE,
            MaterialPart.DOOR,
            MaterialPart.TRAPDOOR,
            MaterialPart.SIGN,
            MaterialPart.HANGING_SIGN,
            MaterialPart.WINDOW,
            MaterialPart.WINDOW_PANE,
            MaterialPart.STICK,
            MaterialPart.CHEST,
            MaterialPart.BARREL,
            MaterialPart.BOOKSHELF,
            MaterialPart.CHISELED_BOOKSHELF,
            MaterialPart.LADDER,
            MaterialPart.BOWL,
            MaterialPart.BOAT,
            MaterialPart.CHEST_BOAT,
            MaterialPart.MOSAIC,
            MaterialPart.MOSAIC_SLAB,
            MaterialPart.MOSAIC_STAIRS
    );

    private static final Set<ResourceLocation> REPLACED_EXTERNAL_WOOD_OUTPUTS = replacedExternalWoodOutputs();

    /** Stone forms whose vanilla/Create crafting/stonecutting/processing routes are replaced by Industron. */
    private static final Set<MaterialPart> REPLACED_STONE_PARTS = java.util.EnumSet.of(
            MaterialPart.STONE,
            MaterialPart.COBBLED_STONE,
            MaterialPart.GRAVEL,
            MaterialPart.SLAB,
            MaterialPart.STAIRS,
            MaterialPart.WALL,
            MaterialPart.COBBLED_SLAB,
            MaterialPart.COBBLED_STAIRS,
            MaterialPart.COBBLED_WALL,
            MaterialPart.POLISHED_STONE,
            MaterialPart.POLISHED_SLAB,
            MaterialPart.POLISHED_STAIRS,
            MaterialPart.POLISHED_WALL,
            MaterialPart.STONE_BRICKS,
            MaterialPart.STONE_BRICK_SLAB,
            MaterialPart.STONE_BRICK_STAIRS,
            MaterialPart.STONE_BRICK_WALL,
            MaterialPart.STONE_TILES,
            MaterialPart.STONE_TILE_SLAB,
            MaterialPart.STONE_TILE_STAIRS,
            MaterialPart.STONE_TILE_WALL,
            MaterialPart.CRACKED_STONE_BRICKS,
            MaterialPart.CRACKED_STONE_TILES,
            MaterialPart.CHISELED_STONE,
            MaterialPart.CHISELED_STONE_BRICKS,
            MaterialPart.SMOOTH_STONE,
            MaterialPart.SMOOTH_STONE_SLAB,
            MaterialPart.SMOOTH_STONE_STAIRS,
            MaterialPart.CUT_STONE,
            MaterialPart.CUT_STONE_SLAB,
            MaterialPart.CUT_STONE_STAIRS,
            MaterialPart.CUT_STONE_WALL,
            MaterialPart.POLISHED_CUT_STONE,
            MaterialPart.POLISHED_CUT_STONE_SLAB,
            MaterialPart.POLISHED_CUT_STONE_STAIRS,
            MaterialPart.POLISHED_CUT_STONE_WALL,
            MaterialPart.CUT_STONE_BRICKS,
            MaterialPart.CUT_STONE_BRICK_SLAB,
            MaterialPart.CUT_STONE_BRICK_STAIRS,
            MaterialPart.CUT_STONE_BRICK_WALL,
            MaterialPart.SMALL_STONE_BRICKS,
            MaterialPart.SMALL_STONE_BRICK_SLAB,
            MaterialPart.SMALL_STONE_BRICK_STAIRS,
            MaterialPart.SMALL_STONE_BRICK_WALL,
            MaterialPart.LAYERED_STONE,
            MaterialPart.PILLAR,
            MaterialPart.POLISHED_STONE_BRICKS,
            MaterialPart.POLISHED_STONE_BRICK_SLAB,
            MaterialPart.POLISHED_STONE_BRICK_STAIRS,
            MaterialPart.POLISHED_STONE_BRICK_WALL,
            MaterialPart.CRACKED_POLISHED_STONE_BRICKS,
            MaterialPart.CHISELED_POLISHED_STONE,
            MaterialPart.BUTTON,
            MaterialPart.PRESSURE_PLATE
    );

    private static final Set<ResourceLocation> REPLACED_EXTERNAL_STONE_OUTPUTS = replacedExternalStoneOutputs();

    private static final Set<ResourceLocation> VISIBLE_EXTERNAL_EXCEPTIONS = Set.of(
            ResourceLocation.withDefaultNamespace("diamond_helmet"),
            ResourceLocation.withDefaultNamespace("diamond_chestplate"),
            ResourceLocation.withDefaultNamespace("diamond_leggings"),
            ResourceLocation.withDefaultNamespace("diamond_boots"),
            ResourceLocation.withDefaultNamespace("netherite_helmet"),
            ResourceLocation.withDefaultNamespace("netherite_chestplate"),
            ResourceLocation.withDefaultNamespace("netherite_leggings"),
            ResourceLocation.withDefaultNamespace("netherite_boots"),

            ResourceLocation.fromNamespaceAndPath("create", "brass_funnel"),
            ResourceLocation.fromNamespaceAndPath("create", "brass_tunnel"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "diamond"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "diamond_block"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "diamond_ore"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "deepslate_diamond_ore"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "emerald"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "emerald_block"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "emerald_ore"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "deepslate_emerald_ore")
    );

    private static final Set<ResourceLocation> SUPPRESSED_EXTERNAL_TRANSPORT = Set.of(
            ResourceLocation.fromNamespaceAndPath("create", "fluid_pipe"),
            ResourceLocation.fromNamespaceAndPath("create", "glass_fluid_pipe"),
            ResourceLocation.fromNamespaceAndPath("create", "encased_fluid_pipe"),
            ResourceLocation.fromNamespaceAndPath("create", "mechanical_pump"),
            ResourceLocation.fromNamespaceAndPath("create", "fluid_tank"),
            ResourceLocation.fromNamespaceAndPath("create", "creative_fluid_tank")
    );


    /** Vanilla items that existed only as temporary Assembly/block-breaking test tools. */
    private static final Set<ResourceLocation> SUPPRESSED_EXTERNAL_TEST_TOOLS = Set.of(
            ResourceLocation.withDefaultNamespace("wooden_sword"),
            ResourceLocation.withDefaultNamespace("stone_sword"),
            ResourceLocation.withDefaultNamespace("iron_sword"),
            ResourceLocation.withDefaultNamespace("golden_sword"),
            ResourceLocation.withDefaultNamespace("diamond_sword"),
            ResourceLocation.withDefaultNamespace("netherite_sword"),
            ResourceLocation.withDefaultNamespace("shears"),
            ResourceLocation.withDefaultNamespace("bow"),
            ResourceLocation.withDefaultNamespace("crossbow"),
            ResourceLocation.withDefaultNamespace("shield"),
            ResourceLocation.withDefaultNamespace("fishing_rod"),
            ResourceLocation.withDefaultNamespace("leather_helmet"),
            ResourceLocation.withDefaultNamespace("leather_chestplate"),
            ResourceLocation.withDefaultNamespace("leather_leggings"),
            ResourceLocation.withDefaultNamespace("leather_boots"),
            ResourceLocation.withDefaultNamespace("chainmail_helmet"),
            ResourceLocation.withDefaultNamespace("chainmail_chestplate"),
            ResourceLocation.withDefaultNamespace("chainmail_leggings"),
            ResourceLocation.withDefaultNamespace("chainmail_boots"),
            ResourceLocation.withDefaultNamespace("iron_helmet"),
            ResourceLocation.withDefaultNamespace("iron_chestplate"),
            ResourceLocation.withDefaultNamespace("iron_leggings"),
            ResourceLocation.withDefaultNamespace("iron_boots"),
            ResourceLocation.withDefaultNamespace("golden_helmet"),
            ResourceLocation.withDefaultNamespace("golden_chestplate"),
            ResourceLocation.withDefaultNamespace("golden_leggings"),
            ResourceLocation.withDefaultNamespace("golden_boots"),

            ResourceLocation.fromNamespaceAndPath("minecraft", "wooden_pickaxe"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "stone_pickaxe"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "netherite_pickaxe"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "wooden_axe"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "stone_axe"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "netherite_axe"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "wooden_shovel"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "stone_shovel"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "netherite_shovel"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "wooden_hoe"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "stone_hoe"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "netherite_hoe")
    );

    /** Vanilla processing blocks intentionally removed from progression/presentation. */
    private static final Set<ResourceLocation> SUPPRESSED_EXTERNAL_PROCESSING_BLOCKS = Set.of(
            ResourceLocation.fromNamespaceAndPath("minecraft", "furnace"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "smoker")
    );

    /** Exact vanilla block outputs now produced only through private Assembly recipes. */
    private static final Set<ResourceLocation> REPLACED_PRIVATE_ASSEMBLY_OUTPUTS = Set.of(
            ResourceLocation.fromNamespaceAndPath("minecraft", "nether_bricks"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "cracked_nether_bricks")
    );

    /** Create blocks whose stock crafting recipes are replaced by pre-metal Assembly routes. */
    private static final Set<ResourceLocation> REPLACED_CREATE_EARLY_KINETIC_OUTPUTS = Set.of(
            ResourceLocation.fromNamespaceAndPath("create", "shaft"),
            ResourceLocation.fromNamespaceAndPath("create", "mechanical_mixer"),
            ResourceLocation.fromNamespaceAndPath("create", "encased_fan"),
            ResourceLocation.fromNamespaceAndPath("create", "mechanical_press"),
            ResourceLocation.fromNamespaceAndPath("create", "crushing_wheel"),
            ResourceLocation.fromNamespaceAndPath("create", "mechanical_saw"),
            ResourceLocation.fromNamespaceAndPath("create", "mechanical_drill"),
            ResourceLocation.fromNamespaceAndPath("create", "mechanical_pump"),
            ResourceLocation.fromNamespaceAndPath("create", "deployer"),
            ResourceLocation.fromNamespaceAndPath("create", "andesite_alloy"),
            ResourceLocation.fromNamespaceAndPath("create", "andesite_casing"),
            ResourceLocation.fromNamespaceAndPath("create", "cogwheel"),
            ResourceLocation.fromNamespaceAndPath("create", "large_cogwheel"),
            ResourceLocation.fromNamespaceAndPath("create", "millstone"),
            ResourceLocation.fromNamespaceAndPath("create", "hand_crank"),
            ResourceLocation.fromNamespaceAndPath("create", "gearbox"),
            ResourceLocation.fromNamespaceAndPath("create", "water_wheel"),
            ResourceLocation.fromNamespaceAndPath("create", "large_water_wheel"),
            ResourceLocation.fromNamespaceAndPath("create", "windmill_bearing"),
            ResourceLocation.fromNamespaceAndPath("create", "sail_frame"),
            ResourceLocation.fromNamespaceAndPath("create", "white_sail")
    );

    /** Vanilla outputs whose Crafting Table recipes are replaced by Industron early-game routes. */
    private static final Set<ResourceLocation> REPLACED_EARLY_GAME_OUTPUTS = Set.of(
            ResourceLocation.withDefaultNamespace("diamond_helmet"),
            ResourceLocation.withDefaultNamespace("diamond_chestplate"),
            ResourceLocation.withDefaultNamespace("diamond_leggings"),
            ResourceLocation.withDefaultNamespace("diamond_boots"),
            ResourceLocation.withDefaultNamespace("netherite_helmet"),
            ResourceLocation.withDefaultNamespace("netherite_chestplate"),
            ResourceLocation.withDefaultNamespace("netherite_leggings"),
            ResourceLocation.withDefaultNamespace("netherite_boots"),

            ResourceLocation.fromNamespaceAndPath("minecraft", "hopper"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "torch"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "white_bed"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "orange_bed"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "magenta_bed"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "light_blue_bed"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "yellow_bed"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "lime_bed"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "pink_bed"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "gray_bed"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "light_gray_bed"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "cyan_bed"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "purple_bed"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "blue_bed"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "brown_bed"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "green_bed"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "red_bed"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "black_bed"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "fishing_rod"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "bow"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "arrow"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "lever"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "item_frame"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "composter"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "scaffolding"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "paper"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "book"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "lead"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "armor_stand"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "loom"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "cartography_table"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "lectern"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "flint_and_steel"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "chain"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "bucket"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "shears"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "tripwire_hook")
    );

    private static final Set<String> SUPPRESSED_SINGLE_TOKENS = Set.of(
            "iron",
            "gold",
            "golden",
            "copper",
            "coal",
            "zinc",
            "brass",
            "diamond",
            "emerald"
    );

    private static final Set<String> SUPPRESSED_COMPOUND_TOKENS = Set.of(
            "rose_quartz"
    );

    /**
     * Exact external items that invalidate a recipe only when they are used as inputs.
     * Keeping this separate from {@link #RECIPE_MATERIAL_TOKENS} is important: an
     * external recipe that merely produces one of these items must not be removed.
     */
    private static final Set<ResourceLocation> SUPPRESSED_RECIPE_INPUT_ITEMS = Set.of(
            ResourceLocation.fromNamespaceAndPath("create", "andesite_alloy")
    );

    /**
     * Materials whose vanilla/Create recipe progression is intentionally removed.
     * Diamond, Emerald, Lapis, Redstone and Quartz stay visible because their existing
     * registry forms are reused by Industron, but their old recipes are replaced later.
     */
    private static final Set<String> RECIPE_MATERIAL_TOKENS = Set.of(
            "iron",
            "gold",
            "golden",
            "copper",
            "coal",
            "coals",
            "zinc",
            "brass",
            "diamond",
            "emerald",
            "netherite",
            "redstone",
            "lapis",
            "lazuli",
            "quartz",
            "rose_quartz"
    );

    private ExternalMaterialSuppression() {
    }

    /** Used by JEI/external presentation suppression. */
    public static boolean isSuppressedExternalMaterial(ResourceLocation id) {
        if (id == null || !EXTERNAL_NAMESPACES.contains(id.getNamespace())) {
            return false;
        }
        if (VISIBLE_EXTERNAL_EXCEPTIONS.contains(id)) {
            return false;
        }
        if (SUPPRESSED_EXTERNAL_TRANSPORT.contains(id)
                || SUPPRESSED_EXTERNAL_TEST_TOOLS.contains(id)
                || SUPPRESSED_EXTERNAL_PROCESSING_BLOCKS.contains(id)) {
            return true;
        }

        String path = id.getPath();

        for (String compound : SUPPRESSED_COMPOUND_TOKENS) {
            if (containsPathToken(path, compound)) {
                return true;
            }
        }

        for (String token : SUPPRESSED_SINGLE_TOKENS) {
            if (containsPathToken(path, token)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isSuppressedExternalTransport(ResourceLocation id) {
        return id != null && SUPPRESSED_EXTERNAL_TRANSPORT.contains(id);
    }

    public static Set<ResourceLocation> suppressedExternalTransportIds() {
        return SUPPRESSED_EXTERNAL_TRANSPORT;
    }

    public static Set<ResourceLocation> suppressedExternalTestToolIds() {
        return SUPPRESSED_EXTERNAL_TEST_TOOLS;
    }

    public static Set<ResourceLocation> suppressedExternalProcessingBlockIds() {
        return SUPPRESSED_EXTERNAL_PROCESSING_BLOCKS;
    }

    /**
     * Removes material progression recipes from every external namespace. Industron's
     * future custom recipes are deliberately exempt even when they use the same materials.
     */
    public static boolean isSuppressedExternalRecipe(ResourceLocation recipeId, JsonElement recipeJson) {
        if (recipeId == null || recipeJson == null) {
            return false;
        }

        if (net.mads.industron.recipe.recipes.food.FarmingFoodRecipes.suppress(recipeId, recipeJson)) return true;

        // runData does not guarantee removal of stale files from providers or recipe ids that changed.
        // These are old Industron-generated ids only; they are cleanup, not material-family logic.
        if (Industron.MOD_ID.equals(recipeId.getNamespace())) {
            String path = recipeId.getPath();
            return path.equals("recipes/assembly/climate_instrument")
                    || path.equals("recipes/assembly/andesite_alloy_caskel")
                    || path.equals("recipes/assembly/andesite_alloy_uskara")
                    || path.startsWith("stone_processing/milling/")
                    || path.startsWith("plant_processing/milling/")
                    || path.startsWith("plant_processing/cutting/")
                    || (path.startsWith("chiseling/") && path.endsWith("/cut_stone_alias"))
                    || (path.startsWith("stone_processing/") && path.endsWith("/raw_to_smooth_stone_alias"))
                    || (path.startsWith("stone_processing/") && path.endsWith("/raw_to_gravel"));
        }
        if (recipeId.getNamespace().equals("create")) {
            // Preserved crafting must not reintroduce legacy metal inputs/outputs through
            // ingots, sheets or tags. The material routes belong to Industron's progression.
            if (containsLegacyMetal(recipeJson)) return true;
            // These specific blocks are now acquired through Industron Assembly. Check them
            // before the general "preserve Create crafting" rule so the stock shaped recipes
            // cannot bypass the pre-metal progression.
            if (containsReplacedCreateEarlyKineticOutput(recipeJson, false)) return true;
            if (isPreservedCreateRecipeType(recipeJson)) return false;
            if (isReplacedCreateRecipeType(recipeJson)) return true;
        }
        if (EXTERNAL_NAMESPACES.contains(recipeId.getNamespace())
                && isSuppressedVanillaCookingRecipe(recipeJson)) {
            return true;
        }

        return containsSuppressedRecipeReference(recipeJson, false)
                || containsSuppressedRecipeInput(recipeJson, false)
                || (EXTERNAL_NAMESPACES.contains(recipeId.getNamespace())
                && (containsReplacedWoodOutput(recipeJson, false)
                || containsReplacedStoneOutput(recipeJson, false)
                || containsReplacedPrivateAssemblyOutput(recipeJson, false)
                || containsReplacedEarlyGameOutput(recipeJson, false)));
    }

    private static boolean isPreservedCreateRecipeType(JsonElement recipeJson) {
        String type = recipeType(recipeJson);
        return type != null && PRESERVED_CREATE_RECIPE_TYPES.contains(type);
    }

    private static boolean containsLegacyMetal(JsonElement json) {
        if (json == null) return false;
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
            ResourceLocation id = ResourceLocation.tryParse(json.getAsString());
            if (id == null) return false;
            if (net.mads.industron.progression.ProgressionMaterials.replacement(id, true) != null) return true;
            // Tag ingredients can use c:ingots/iron, forge:ingots/iron, or minecraft:gold_ores.
            String path = id.getPath();
            return (id.getNamespace().equals("c") || id.getNamespace().equals("forge")
                    || (id.getNamespace().equals("minecraft") && path.endsWith("_ores")))
                    && java.util.Arrays.stream(path.split("[/_]"))
                    .anyMatch(token -> Set.of("iron", "gold", "golden", "copper", "zinc", "brass").contains(token));
        }
        if (json.isJsonArray()) {
            for (JsonElement child : json.getAsJsonArray()) if (containsLegacyMetal(child)) return true;
        } else if (json.isJsonObject()) {
            for (var entry : json.getAsJsonObject().entrySet()) if (containsLegacyMetal(entry.getValue())) return true;
        }
        return false;
    }

    private static boolean isReplacedCreateRecipeType(JsonElement recipeJson) {
        String type = recipeType(recipeJson);
        return type != null && REPLACED_CREATE_RECIPE_TYPES.contains(type);
    }

    private static String recipeType(JsonElement recipeJson) {
        if (!recipeJson.isJsonObject()) return null;
        JsonElement type = recipeJson.getAsJsonObject().get("type");
        return type != null && type.isJsonPrimitive() && type.getAsJsonPrimitive().isString()
                ? type.getAsString()
                : null;
    }

    public static Map<ResourceLocation, JsonElement> filterExternalRecipes(
            Map<ResourceLocation, JsonElement> recipes
    ) {
        if (recipes == null || recipes.isEmpty()) {
            return recipes;
        }

        java.util.LinkedHashMap<ResourceLocation, JsonElement> filtered = new java.util.LinkedHashMap<>();
        recipes.forEach((id, json) -> {
            JsonElement effective = json;
            if (id.getNamespace().equals("create") && isPreservedCreateRecipeType(json)) {
                effective = net.mads.industron.progression.ProgressionRecipes.rewriteCreate(json);
            }
            if (effective != null && !isSuppressedExternalRecipe(id, effective)) {
                filtered.put(id, effective);
            }
        });
        return filtered;
    }

    private static Set<ResourceLocation> replacedExternalStoneOutputs() {
        java.util.LinkedHashSet<ResourceLocation> result = new java.util.LinkedHashSet<>();
        for (var stone : StoneMaterials.ALL) {
            for (MaterialPart part : REPLACED_STONE_PARTS) {
                if (!stone.hasExistingPart(part) || stone.isWithout(part)) continue;
                ResourceLocation id = stone.existingPart(part);
                if (id != null && EXTERNAL_NAMESPACES.contains(id.getNamespace())) result.add(id);
            }

        }
        return Set.copyOf(result);
    }

    /** Output-only scan for old vanilla/Create stone routes replaced by Industron. */
    private static boolean containsReplacedStoneOutput(JsonElement element, boolean outputContext) {
        if (element == null || element.isJsonNull()) return false;
        if (element.isJsonPrimitive()) {
            if (!outputContext || !element.getAsJsonPrimitive().isString()) return false;
            String value = element.getAsString();
            ResourceLocation id = ResourceLocation.tryParse(value.startsWith("#") ? value.substring(1) : value);
            return id != null && REPLACED_EXTERNAL_STONE_OUTPUTS.contains(id);
        }
        if (element.isJsonArray()) {
            for (JsonElement value : element.getAsJsonArray()) {
                if (containsReplacedStoneOutput(value, outputContext)) return true;
            }
            return false;
        }
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            String key = entry.getKey().toLowerCase(java.util.Locale.ROOT);
            boolean childOutput = outputContext || switch (key) {
                case "result", "results", "output", "outputs" -> true;
                default -> false;
            };
            if (containsReplacedStoneOutput(entry.getValue(), childOutput)) return true;
        }
        return false;
    }

    /** Removes old vanilla crafting/firing routes for exact blocks now owned by private Assembly. */
    private static boolean containsReplacedPrivateAssemblyOutput(JsonElement element, boolean outputContext) {
        if (element == null || element.isJsonNull()) return false;
        if (element.isJsonPrimitive()) {
            if (!outputContext || !element.getAsJsonPrimitive().isString()) return false;
            String value = element.getAsString();
            ResourceLocation id = ResourceLocation.tryParse(value.startsWith("#") ? value.substring(1) : value);
            return id != null && REPLACED_PRIVATE_ASSEMBLY_OUTPUTS.contains(id);
        }
        if (element.isJsonArray()) {
            for (JsonElement value : element.getAsJsonArray()) {
                if (containsReplacedPrivateAssemblyOutput(value, outputContext)) return true;
            }
            return false;
        }
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            String key = entry.getKey().toLowerCase(java.util.Locale.ROOT);
            boolean childOutput = outputContext || switch (key) {
                case "result", "results", "output", "outputs" -> true;
                default -> false;
            };
            if (containsReplacedPrivateAssemblyOutput(entry.getValue(), childOutput)) return true;
        }
        return false;
    }

    /** Output-only scan for the exact Create kinetic blocks now owned by early Assembly. */
    private static boolean containsReplacedCreateEarlyKineticOutput(JsonElement element, boolean outputContext) {
        if (element == null || element.isJsonNull()) return false;
        if (element.isJsonPrimitive()) {
            if (!outputContext || !element.getAsJsonPrimitive().isString()) return false;
            String value = element.getAsString();
            ResourceLocation id = ResourceLocation.tryParse(value.startsWith("#") ? value.substring(1) : value);
            return id != null && REPLACED_CREATE_EARLY_KINETIC_OUTPUTS.contains(id);
        }
        if (element.isJsonArray()) {
            for (JsonElement value : element.getAsJsonArray()) {
                if (containsReplacedCreateEarlyKineticOutput(value, outputContext)) return true;
            }
            return false;
        }
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            String key = entry.getKey().toLowerCase(java.util.Locale.ROOT);
            boolean childOutput = outputContext || switch (key) {
                case "result", "results", "output", "outputs" -> true;
                default -> false;
            };
            if (containsReplacedCreateEarlyKineticOutput(entry.getValue(), childOutput)) return true;
        }
        return false;
    }

    /** Output-only scan for vanilla Crafting Table routes now replaced by early-game Industron recipes. */
    private static boolean containsReplacedEarlyGameOutput(JsonElement element, boolean outputContext) {
        if (element == null || element.isJsonNull()) return false;
        if (element.isJsonPrimitive()) {
            if (!outputContext || !element.getAsJsonPrimitive().isString()) return false;
            String value = element.getAsString();
            ResourceLocation id = ResourceLocation.tryParse(value.startsWith("#") ? value.substring(1) : value);
            return id != null && REPLACED_EARLY_GAME_OUTPUTS.contains(id);
        }
        if (element.isJsonArray()) {
            for (JsonElement value : element.getAsJsonArray()) {
                if (containsReplacedEarlyGameOutput(value, outputContext)) return true;
            }
            return false;
        }
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            String key = entry.getKey().toLowerCase(java.util.Locale.ROOT);
            boolean childOutput = outputContext || switch (key) {
                case "result", "results", "output", "outputs" -> true;
                default -> false;
            };
            if (containsReplacedEarlyGameOutput(entry.getValue(), childOutput)) return true;
        }
        return false;
    }

    /** Furnace and Smoker recipe families are removed; Campfire Cooking deliberately remains. */
    private static boolean isSuppressedVanillaCookingRecipe(JsonElement recipeJson) {
        if (recipeJson == null || !recipeJson.isJsonObject()) return false;
        JsonElement type = recipeJson.getAsJsonObject().get("type");
        if (type == null || !type.isJsonPrimitive() || !type.getAsJsonPrimitive().isString()) return false;
        return switch (type.getAsString()) {
            case "minecraft:smelting", "minecraft:smoking" -> true;
            default -> false;
        };
    }

    private static Set<ResourceLocation> replacedExternalWoodOutputs() {
        java.util.LinkedHashSet<ResourceLocation> result = new java.util.LinkedHashSet<>();
        for (var wood : WoodMaterials.ALL) {
            for (MaterialPart part : REPLACED_WOOD_PARTS) {
                if (!wood.hasExistingPart(part)) continue;
                ResourceLocation id = wood.existingPart(part);
                if (id != null && EXTERNAL_NAMESPACES.contains(id.getNamespace())) result.add(id);
            }
        }
        // WoodMaterial intentionally generates species-specific sticks (for example spruce_stick),
        // so there is no existing-part mapping to vanilla's generic stick. Its old plank recipes
        // are still a direct bypass of the new 16-stick Assembly route and must be suppressed.
        result.add(ResourceLocation.fromNamespaceAndPath("minecraft", "stick"));
        // Campfire is now acquired through the bark + log + stick Assembly progression.
        result.add(ResourceLocation.fromNamespaceAndPath("minecraft", "campfire"));
        return Set.copyOf(result);
    }

    /** Output-only scan: consuming wood remains legal; only old ways of producing replaced forms are removed. */
    private static boolean containsReplacedWoodOutput(JsonElement element, boolean outputContext) {
        if (element == null || element.isJsonNull()) return false;
        if (element.isJsonPrimitive()) {
            if (!outputContext || !element.getAsJsonPrimitive().isString()) return false;
            String value = element.getAsString();
            ResourceLocation id = ResourceLocation.tryParse(value.startsWith("#") ? value.substring(1) : value);
            return id != null && REPLACED_EXTERNAL_WOOD_OUTPUTS.contains(id);
        }
        if (element.isJsonArray()) {
            for (JsonElement value : element.getAsJsonArray()) {
                if (containsReplacedWoodOutput(value, outputContext)) return true;
            }
            return false;
        }
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            String key = entry.getKey().toLowerCase(java.util.Locale.ROOT);
            boolean childOutput = outputContext || switch (key) {
                case "result", "results", "output", "outputs" -> true;
                default -> false;
            };
            if (containsReplacedWoodOutput(entry.getValue(), childOutput)) return true;
        }
        return false;
    }

    /**
     * Scans only recipe ingredient/result references. Metadata such as
     * {@code category: "redstone"} must not remove an unrelated recipe.
     */
    private static boolean containsSuppressedRecipeReference(JsonElement element, boolean referenceContext) {
        if (element == null || element.isJsonNull()) {
            return false;
        }
        if (element.isJsonPrimitive()) {
            if (!referenceContext || !element.getAsJsonPrimitive().isString()) {
                return false;
            }
            String value = element.getAsString();
            return isSuppressedTransportReference(value) || containsRecipeMaterialToken(value);
        }
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement value : array) {
                if (containsSuppressedRecipeReference(value, referenceContext)) {
                    return true;
                }
            }
            return false;
        }

        JsonObject object = element.getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String key = entry.getKey().toLowerCase(java.util.Locale.ROOT);
            if (isRecipeMetadataField(key)) {
                continue;
            }
            boolean childReferenceContext = referenceContext || isRecipeReferenceField(key);
            if (containsSuppressedRecipeReference(entry.getValue(), childReferenceContext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Input-only scan for exact item ids such as Create's Andesite Alloy.
     * The input context begins only at ingredient-like fields, so a matching id
     * nested below result/output does not suppress the recipe.
     */
    private static boolean containsSuppressedRecipeInput(JsonElement element, boolean inputContext) {
        if (element == null || element.isJsonNull()) {
            return false;
        }
        if (element.isJsonPrimitive()) {
            if (!inputContext || !element.getAsJsonPrimitive().isString()) {
                return false;
            }
            String value = element.getAsString();
            String normalized = value.startsWith("#") ? value.substring(1) : value;
            ResourceLocation id = ResourceLocation.tryParse(normalized);
            return id != null && SUPPRESSED_RECIPE_INPUT_ITEMS.contains(id);
        }
        if (element.isJsonArray()) {
            for (JsonElement value : element.getAsJsonArray()) {
                if (containsSuppressedRecipeInput(value, inputContext)) {
                    return true;
                }
            }
            return false;
        }

        JsonObject object = element.getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String key = entry.getKey().toLowerCase(java.util.Locale.ROOT);
            boolean childInputContext = inputContext || isRecipeInputField(key);
            if (containsSuppressedRecipeInput(entry.getValue(), childInputContext)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRecipeInputField(String key) {
        return switch (key) {
            case "ingredient", "ingredients", "input", "inputs",
                    "key", "base", "addition", "template" -> true;
            default -> false;
        };
    }

    private static boolean isRecipeReferenceField(String key) {
        return switch (key) {
            case "item", "items", "tag", "tags", "fluid", "fluids",
                    "ingredient", "ingredients", "input", "inputs",
                    "result", "results", "output", "outputs",
                    "key", "base", "addition", "template", "transitionalitem" -> true;
            default -> false;
        };
    }

    private static boolean isRecipeMetadataField(String key) {
        return key.equals("type")
                || key.equals("category")
                || key.equals("group")
                || key.equals("pattern");
    }

    private static boolean isSuppressedTransportReference(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.startsWith("#") ? value.substring(1) : value;
        ResourceLocation id = ResourceLocation.tryParse(normalized);
        return id != null && (SUPPRESSED_EXTERNAL_TRANSPORT.contains(id)
                || SUPPRESSED_EXTERNAL_TEST_TOOLS.contains(id)
                || SUPPRESSED_EXTERNAL_PROCESSING_BLOCKS.contains(id));
    }

    private static boolean containsRecipeMaterialToken(String value) {
        String normalized = value.toLowerCase(java.util.Locale.ROOT);
        for (String token : RECIPE_MATERIAL_TOKENS) {
            int searchFrom = 0;
            while (searchFrom <= normalized.length() - token.length()) {
                int index = normalized.indexOf(token, searchFrom);
                if (index < 0) {
                    break;
                }
                int end = index + token.length();
                boolean leftBoundary = index == 0
                        || !Character.isLetterOrDigit(normalized.charAt(index - 1));
                boolean rightBoundary = end == normalized.length()
                        || !Character.isLetterOrDigit(normalized.charAt(end));
                if (leftBoundary && rightBoundary) {
                    return true;
                }
                searchFrom = index + 1;
            }
        }
        return false;
    }

    private static boolean containsPathToken(String path, String token) {
        int searchFrom = 0;
        while (searchFrom <= path.length() - token.length()) {
            int index = path.indexOf(token, searchFrom);
            if (index < 0) {
                return false;
            }

            int end = index + token.length();
            boolean leftBoundary = index == 0 || path.charAt(index - 1) == '_';
            boolean rightBoundary = end == path.length() || path.charAt(end) == '_';
            if (leftBoundary && rightBoundary) {
                return true;
            }

            searchFrom = index + 1;
        }

        return false;
    }
}
