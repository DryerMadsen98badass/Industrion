package net.mads.industron.data;

import com.google.gson.*;
import net.mads.industron.material.defenitions.Organisms;
import net.mads.industron.material.organism.*;
import net.minecraft.data.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Imports the complete baseline, rewrites declared item entries, and appends explicit part drops.
 * Never removes equipment/interaction logic. Copied conditions, functions and subtable references survive.
 */
public final class OrganismLootProvider implements DataProvider {
    private final Path root;
    public OrganismLootProvider(PackOutput output,ExistingFileHelper files) {
        root=output.getOutputFolder(PackOutput.Target.DATA_PACK);
    }
    @Override public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> pending=new ArrayList<>();
        try (var vanilla=new VanillaPackResourcesBuilder().pushJarResources().exposeNamespace("minecraft")
                .build(new PackLocationInfo("vanilla",Component.literal("Vanilla baseline"),PackSource.BUILT_IN,Optional.empty()))) {
            for(var organism:Organisms.ALL) {
                if(organism.lootMode()==OrganismDefinition.LootMode.PRESERVE_EXISTING)continue;
                String entity=organism.existingEntity().orElse("industron:"+organism.id());
                ResourceLocation entityId=ResourceLocation.parse(entity);
                ResourceLocation table=ResourceLocation.fromNamespaceAndPath(entityId.getNamespace(),"entities/"+entityId.getPath());
                Map<String,String> replacements=new LinkedHashMap<>();
                for(var part:organism.parts().entrySet()) for(var replacement:part.getValue().replacements().entrySet())
                    replacements.put(replacement.getKey(),OrganismItemCatalog.form(organism,part.getKey(),replacement.getValue()).itemId());
                JsonObject json=organism.lootMode()==OrganismDefinition.LootMode.REWRITE_EXISTING ? read(vanilla,table) : empty();
                JsonArray kept=json.has("pools")?json.getAsJsonArray("pools"):new JsonArray();
                json.add("pools",kept);
                replace(json,replacements);
                for(var part:organism.parts().entrySet()) for(var loot:part.getValue().loot())
                    kept.add(pool(OrganismItemCatalog.form(organism,part.getKey(),loot.form()),loot));
                pending.add(save(output,table,json));
                // Sheep selects color-specific child tables. Preserve their conditions and references.
                if(organism.id().equals("sheep")) for(String color:List.of("white","orange","magenta","light_blue","yellow","lime","pink","gray","light_gray","cyan","purple","blue","brown","green","red","black")) {
                    var child=ResourceLocation.withDefaultNamespace("entities/sheep/"+color);
                    var childJson=read(vanilla,child);replace(childJson,replacements);
                    pending.add(save(output,child,childJson));
                }
            }
            return CompletableFuture.allOf(pending.toArray(CompletableFuture[]::new));
        }catch(Exception e){return CompletableFuture.failedFuture(e);}
    }
    private JsonObject read(VanillaPackResources vanilla, ResourceLocation id) throws IOException {
        var resource=vanilla.getResource(PackType.SERVER_DATA,
                ResourceLocation.fromNamespaceAndPath(id.getNamespace(),"loot_table/"+id.getPath()+".json"));
        if(resource==null)throw new FileNotFoundException("Missing vanilla baseline loot table: "+id);
        try(var stream=resource.get();
            var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
    private CompletableFuture<?> save(CachedOutput output,ResourceLocation id,JsonObject value) {
        return DataProvider.saveStable(output,value,root.resolve(id.getNamespace()+"/loot_table/"+id.getPath()+".json"));
    }
    private static JsonObject empty(){JsonObject j=new JsonObject();j.addProperty("type","minecraft:entity");j.add("pools",new JsonArray());return j;}
    private static void replace(JsonElement element,Map<String,String> map) {
        if(element.isJsonArray()){for(var child:element.getAsJsonArray())replace(child,map);return;}
        if(!element.isJsonObject())return;
        var obj=element.getAsJsonObject();
        if(obj.has("type")&&obj.get("type").isJsonPrimitive()&&obj.get("type").getAsString().equals("minecraft:item")&&obj.has("name")) {
            String target=map.get(obj.get("name").getAsString());if(target!=null)obj.addProperty("name",target);
        }
        for(var entry:obj.entrySet())if(entry.getValue().isJsonObject()||entry.getValue().isJsonArray())replace(entry.getValue(),map);
    }
    private static JsonObject pool(OrganismItemCatalog.Entry definition,OrganismLoot loot) {
        JsonObject pool=new JsonObject();pool.addProperty("rolls",1);
        JsonArray conditions=new JsonArray();
        if(loot.adultOnly()) {
            JsonObject condition=new JsonObject();condition.addProperty("condition","minecraft:entity_properties");condition.addProperty("entity","this");
            JsonObject predicate=new JsonObject(),flags=new JsonObject();flags.addProperty("is_baby",false);predicate.add("flags",flags);condition.add("predicate",predicate);conditions.add(condition);
        }
        if(loot.playerKillOnly()) {JsonObject c=new JsonObject();c.addProperty("condition","minecraft:killed_by_player");conditions.add(c);}
        if(loot.chance()!=1 || loot.lootingChanceBonus()!=0) {
            JsonObject c=new JsonObject();c.addProperty("condition","minecraft:random_chance_with_enchanted_bonus");
            c.addProperty("enchantment","minecraft:looting");c.addProperty("unenchanted_chance",loot.chance());
            JsonObject value=new JsonObject();value.addProperty("type","minecraft:linear");value.addProperty("base",loot.chance()+loot.lootingChanceBonus());value.addProperty("per_level_above_first",loot.lootingChanceBonus());c.add("enchanted_chance",value);conditions.add(c);
        }
        if(!conditions.isEmpty())pool.add("conditions",conditions);
        JsonObject entry=new JsonObject();entry.addProperty("type","minecraft:item");entry.addProperty("name",definition.itemId());
        JsonArray functions=new JsonArray();JsonObject count=new JsonObject();count.addProperty("function","minecraft:set_count");count.add("count",range(loot.minimum(),loot.maximum()));functions.add(count);
        if(loot.lootingMaximum()>0) {
            JsonObject f=new JsonObject();f.addProperty("function","minecraft:enchanted_count_increase");f.addProperty("enchantment","minecraft:looting");f.add("count",range(loot.lootingMinimum(),loot.lootingMaximum()));functions.add(f);
        }
        if(loot.cookWhenBurning()) {
            if(!definition.part().cookingFamily() || definition.form()!=OrganicForm.RAW)
                throw new IllegalArgumentException("cookWhenBurning requires a raw cooking part");
            var cooked=OrganismItemCatalog.form(definition.owner(),definition.part(),OrganicForm.COOKED);
            JsonObject f=new JsonObject();f.addProperty("function","minecraft:set_item");f.addProperty("item",cooked.itemId());
            JsonObject condition=new JsonObject();condition.addProperty("condition","minecraft:entity_properties");condition.addProperty("entity","this");
            JsonObject predicate=new JsonObject(),flags=new JsonObject();flags.addProperty("is_on_fire",true);
            predicate.add("flags",flags);condition.add("predicate",predicate);
            JsonArray when=new JsonArray();when.add(condition);f.add("conditions",when);functions.add(f);
        }
        entry.add("functions",functions);JsonArray entries=new JsonArray();entries.add(entry);pool.add("entries",entries);return pool;
    }
    private static JsonElement range(int min,int max){if(min==max)return new JsonPrimitive(min);JsonObject j=new JsonObject();j.addProperty("type","minecraft:uniform");j.addProperty("min",min);j.addProperty("max",max);return j;}
    @Override public String getName(){return "Industron organism replacement loot tables";}
}
