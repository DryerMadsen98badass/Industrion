package net.mads.industron.recipe.recipes.food;

import net.mads.industron.machine.MachineTier;
import net.mads.industron.recipe.*;
import net.mads.industron.recipe.recipes.assembly.Tool;
import net.mads.industron.registry.ItemRegistry;
import net.mads.industron.material.organism.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;

/** Deliberately fixed private food recipes, never automatically inherited by another tier. */
public final class FarmingFoodRecipes {
    private FarmingFoodRecipes() {}
    public static void build(RecipeOutput output) {
        RecipeDefinition.recipe()
            .recipeDefinition(RecipeDefinition.Option.id("recipes/food/grain_to_flour"))
            .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.BASIN_MORTARING))
            .recipeDefinition(RecipeDefinition.Option.inputItem("industron:wheat_grain",1))
            .recipeDefinition(RecipeDefinition.Option.outputItem("industron:wheat_flour",1))
            .recipeDefinition(RecipeDefinition.Option.tool(Tool.PESTLE,12))
            .recipeDefinition(RecipeDefinition.Option.tier(MachineTier.ULV))
            .recipeDefinition(RecipeDefinition.Option.duration(400)).save(output);
        RecipeDefinition.recipe()
            .recipeDefinition(RecipeDefinition.Option.id("recipes/food/flour_to_dough"))
            .recipeDefinition(RecipeDefinition.Option.recipeType(CERecipeTypes.BASIN_MIXING))
            .recipeDefinition(RecipeDefinition.Option.inputItem("industron:wheat_flour",3))
            .recipeDefinition(RecipeDefinition.Option.inputFluid("minecraft:water",250))
            .recipeDefinition(RecipeDefinition.Option.outputItem("industron:bread_dough",1))
            .recipeDefinition(RecipeDefinition.Option.tool(Tool.SHOVEL,4))
            .recipeDefinition(RecipeDefinition.Option.tier(MachineTier.ULV))
            .recipeDefinition(RecipeDefinition.Option.duration(200)).save(output);
        bake(output,"bread_dough","minecraft:bread",1200);
        bake(output,"minecraft:bread","industron:burnt_bread",600);
        bake(output,"minecraft:potato","minecraft:baked_potato",1200);
        bake(output,"minecraft:baked_potato","industron:burnt_potato",600);
        for(String fish:new String[]{"cod","salmon","tropical_fish"}) {
            var raw=OrganismItemCatalog.byItem("minecraft:"+fish);
            if(raw==null)throw new IllegalStateException("Missing fish catalog entry: "+fish);
            bake(output,"cleaned_"+fish,OrganismItemCatalog.form(raw.owner(),raw.part(),OrganicForm.COOKED).itemId(),800);
        }
    }
    private static Item item(String id) {
        if(!id.contains(":"))return ItemRegistry.SIMPLE_ITEMS.get(id).get();
        var simple=ItemRegistry.SIMPLE_ITEMS.get(id.replace("industron:",""));
        if(id.startsWith("industron:") && simple!=null)return simple.get();
        var organism=ItemRegistry.ORGANISM_ITEMS.get(id);if(organism!=null)return organism.get();
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
    }
    private static void bake(RecipeOutput output,String source,String target,int ticks) {
        SimpleCookingRecipeBuilder.campfireCooking(Ingredient.of(item(source)),RecipeCategory.FOOD,item(target),0.1F,ticks)
            .unlockedBy("has_input",net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance.hasItems(item(source)))
            .save(output,ResourceLocation.fromNamespaceAndPath("industron","recipes/food/bake_"+source.replace(":","_")));
    }
    public static boolean suppress(ResourceLocation id,com.google.gson.JsonElement json) {
        if(id.getNamespace().equals("industron") && id.getPath().startsWith("recipes/food/"))return false;
        if(!json.isJsonObject())return false;
        var o=json.getAsJsonObject();String type=o.has("type")?o.get("type").getAsString():"";
        if(type.contains("cooking") && o.has("ingredient") && rawFish(o.get("ingredient")))return true;
        if(type.equals("industron:assembly"))return false;
        // Only recipe outputs are checked: ingredient references must remain available.
        for(String key:new String[]{"result","results"})if(o.has(key) && foodOutput(o.get(key)))return true;
        return false;
    }
    private static boolean rawFish(com.google.gson.JsonElement e) {
        if(e.isJsonArray()) {for(var v:e.getAsJsonArray())if(rawFish(v))return true;return false;}
        if(e.isJsonObject()) {var o=e.getAsJsonObject();return o.has("item") && rawFish(o.get("item"));}
        return e.isJsonPrimitive() && java.util.Set.of("minecraft:cod","minecraft:salmon","minecraft:tropical_fish","minecraft:pufferfish").contains(e.getAsString());
    }
    private static boolean foodOutput(com.google.gson.JsonElement e) {
        if(e.isJsonArray()) {for(var v:e.getAsJsonArray())if(foodOutput(v))return true;return false;}
        if(e.isJsonObject()) {var o=e.getAsJsonObject();return (o.has("id") && foodOutput(o.get("id"))) || (o.has("item") && foodOutput(o.get("item")));}
        if(!e.isJsonPrimitive())return false;
        String id=e.getAsString();return java.util.Set.of("minecraft:bread","minecraft:baked_potato","minecraft:cooked_cod","minecraft:cooked_salmon","create:wheat_flour","create:dough").contains(id);
    }
}
