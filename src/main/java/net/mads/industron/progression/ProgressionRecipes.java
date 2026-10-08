package net.mads.industron.progression;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

/** Adapt retained Create crafting recipes; legacy metal outputs cannot become recycling shortcuts. */
public final class ProgressionRecipes {
    private ProgressionRecipes() {}

    public static JsonElement rewriteCreate(JsonElement original) {
        JsonElement copy=original.deepCopy();
        return rewrite(copy,false)?copy:null;
    }

    private static boolean rewrite(JsonElement json,boolean output) {
        if(output&&json.isJsonPrimitive()&&json.getAsJsonPrimitive().isString()) {
            var id=ResourceLocation.tryParse(json.getAsString());
            if(id!=null&&ProgressionMaterials.replacement(id,true)!=null)return false;
        }
        if(json.isJsonArray()) {
            for(var child:json.getAsJsonArray())if(!rewrite(child,output))return false;
        } else if(json.isJsonObject()) {
            JsonObject object=json.getAsJsonObject();
            for(String field:new String[]{"item","id"}) {
                var value=object.get(field);
                if(value!=null&&value.isJsonPrimitive()&&value.getAsJsonPrimitive().isString()) {
                    var id=ResourceLocation.tryParse(value.getAsString());
                    var replacement=id==null?null:ProgressionMaterials.replacement(id,true);
                    if(replacement!=null) {
                        if(output||replacement.count()!=1)return false;
                        object.addProperty(field,"industron:"+replacement.material()+"_"+replacement.part().id());
                    }
                }
            }
            var tag=object.get("tag");
            if(tag!=null&&tag.isJsonPrimitive()&&tag.getAsJsonPrimitive().isString()) {
                String item=tagItem(tag.getAsString());
                if(item!=null) { object.remove("tag");object.addProperty("item",item); }
            }
            // Entries are not structurally changed during traversal.
            for(var entry:object.entrySet()) {
                boolean childOutput=output||entry.getKey().equals("result")||entry.getKey().equals("results");
                if(!rewrite(entry.getValue(),childOutput))return false;
            }
        }
        return true;
    }

    private static String tagItem(String tag) {
        ResourceLocation id=ResourceLocation.tryParse(tag);
        if(id==null||(!id.getNamespace().equals("c")&&!id.getNamespace().equals("forge")))return null;
        String[] parts=id.getPath().split("/");
        if(parts.length!=2)return null;
        String material=switch(parts[1]) {
            case "iron"->ProgressionMaterials.IRON;case "copper"->ProgressionMaterials.COPPER;
            case "gold"->ProgressionMaterials.GOLD;case "zinc"->ProgressionMaterials.ZINC;
            case "brass"->ProgressionMaterials.BRASS;default->null;
        };
        String form=switch(parts[0]) {
            case "ingots"->"ingot";case "nuggets"->"nugget";case "storage_blocks"->"block";
            case "plates"->"plate";case "raw_materials"->"raw_ore";default->null;
        };
        return material==null||form==null?null:"industron:"+material+"_"+form;
    }
}
