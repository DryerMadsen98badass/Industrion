package net.mads.industron.material.recipes;

import net.mads.industron.Industron;
import net.mads.industron.material.organism.OrganicForm;
import net.mads.industron.material.organism.OrganismItemCatalog;
import net.mads.industron.material.organism.OrganismPart;
import net.mads.industron.registry.ItemRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.HashSet;
import java.util.Set;

/** Campfire recipes for JEI/datagen visibility. Runtime still controls overcooking to burnt forms. */
public final class OrganismCampfireRecipes {
    private OrganismCampfireRecipes() {}

    public static void build(RecipeOutput output) {
        Set<String> emitted = new HashSet<>();
        for (var raw : OrganismItemCatalog.ALL) {
            if (raw.form() != OrganicForm.RAW || !raw.part().cookingFamily() || java.util.Set.of("minecraft:cod","minecraft:salmon","minecraft:tropical_fish","minecraft:pufferfish").contains(raw.itemId())) continue;
            if (OrganismItemCatalog.byItem(raw.itemId()) != raw) continue;
            save(output, emitted, raw, OrganicForm.COOKED, 0.35F, net.mads.industron.material.organism.cooking.OrganicCookingRules.cookedAt(raw));
        }
        for (var source : OrganismItemCatalog.ALL) {
            if (!source.part().cookingFamily()) continue;
            if (source.form() != OrganicForm.COOKED && source.form() != OrganicForm.ROTTEN) continue;
            if (OrganismItemCatalog.byItem(source.itemId()) != source) continue;
            save(output, emitted, source, OrganicForm.BURNT, 0.0F, 600);
        }
    }

    private static void save(RecipeOutput output, Set<String> emitted, OrganismItemCatalog.Entry source,
                             OrganicForm targetForm, float experience, int cookingTime) {
        var target = OrganismItemCatalog.form(source.owner(), source.part(), targetForm);
        String path = "organisms/" + source.owner().id() + "/" + source.part().suffix() + "/campfire_"
                + source.form().name().toLowerCase(java.util.Locale.ROOT) + "_to_"
                + targetForm.name().toLowerCase(java.util.Locale.ROOT);
        if (!emitted.add(source.itemId() + "->" + target.itemId())) return;
        SimpleCookingRecipeBuilder.campfireCooking(Ingredient.of(item(source.itemId())),
                        RecipeCategory.FOOD, item(target.itemId()), experience, cookingTime)
                .unlockedBy("has_input", net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance
                        .hasItems(item(source.itemId())))
                .save(output, ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, path));
    }

    private static Item item(String id) {
        var generated = ItemRegistry.ORGANISM_ITEMS.get(id);
        if (generated != null) return generated.get();
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
    }
}
