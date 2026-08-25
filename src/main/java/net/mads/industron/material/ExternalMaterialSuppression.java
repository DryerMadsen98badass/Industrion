package net.mads.industron.material;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.mads.industron.Industron;
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

    private static final Set<ResourceLocation> VISIBLE_EXTERNAL_EXCEPTIONS = Set.of(
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

    private static final Set<String> SUPPRESSED_SINGLE_TOKENS = Set.of(
            "iron",
            "gold",
            "golden",
            "copper",
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
        if (SUPPRESSED_EXTERNAL_TRANSPORT.contains(id)) {
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

    /**
     * Removes material progression recipes from every external namespace. Industron's
     * future custom recipes are deliberately exempt even when they use the same materials.
     */
    public static boolean isSuppressedExternalRecipe(ResourceLocation recipeId, JsonElement recipeJson) {
        if (recipeId == null || recipeJson == null
                || Industron.MOD_ID.equals(recipeId.getNamespace())) {
            return false;
        }
        return containsSuppressedRecipeReference(recipeJson, false)
                || containsSuppressedRecipeInput(recipeJson, false);
    }

    public static Map<ResourceLocation, JsonElement> filterExternalRecipes(
            Map<ResourceLocation, JsonElement> recipes
    ) {
        if (recipes == null || recipes.isEmpty()) {
            return recipes;
        }

        java.util.LinkedHashMap<ResourceLocation, JsonElement> filtered = new java.util.LinkedHashMap<>();
        recipes.forEach((id, json) -> {
            if (!isSuppressedExternalRecipe(id, json)) {
                filtered.put(id, json);
            }
        });
        return filtered;
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
        return id != null && SUPPRESSED_EXTERNAL_TRANSPORT.contains(id);
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
