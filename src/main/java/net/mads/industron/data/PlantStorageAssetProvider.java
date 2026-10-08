package net.mads.industron.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.mads.industron.Industron;
import net.mads.industron.material.plant.PlantStorage;
import net.mads.industron.material.plant.PlantStorageBlock;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Geometry reuses the actual declared source item's texture, including inherited models. */
public final class PlantStorageAssetProvider implements DataProvider {
    private final PackOutput.PathProvider states,models;
    private final ExistingFileHelper files;
    public PlantStorageAssetProvider(PackOutput output,ExistingFileHelper files) {
        states=output.createPathProvider(PackOutput.Target.RESOURCE_PACK,"blockstates");
        models=output.createPathProvider(PackOutput.Target.RESOURCE_PACK,"models/block");this.files=files;
    }
    @Override public CompletableFuture<?> run(CachedOutput output) {
        var futures=new ArrayList<CompletableFuture<?>>();
        for(var material:PlantStorage.materials()) {
            String name=material.storageBlockId();JsonObject variants=new JsonObject();
            for(int count=1;count<=PlantStorage.CAPACITY;count++) {
                JsonObject variant=new JsonObject();variant.addProperty("model","industron:block/"+name+"_"+((count+7)/8));
                variants.add("count="+count,variant);
            }
            JsonObject state=new JsonObject();state.add("variants",variants);
            futures.add(DataProvider.saveStable(output,state,states.json(id(name))));
            ResourceLocation source=material.storageItem().orElseThrow();
            ResourceLocation texture=texture(ResourceLocation.fromNamespaceAndPath(source.getNamespace(),"item/"+source.getPath()));
            for(int stage=1;stage<=8;stage++)futures.add(DataProvider.saveStable(output,model(texture,stage),models.json(id(name+"_"+stage))));
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }
    private ResourceLocation texture(ResourceLocation model) {
        Map<String,String> textures=new HashMap<>();ResourceLocation current=model;
        for(int depth=0;depth<16;depth++) {
            try(var reader=new InputStreamReader(files.getResource(current,PackType.CLIENT_RESOURCES,".json","models").open(),StandardCharsets.UTF_8)) {
                JsonObject root=JsonParser.parseReader(reader).getAsJsonObject();
                if(root.has("textures"))root.getAsJsonObject("textures").entrySet().forEach(e -> textures.putIfAbsent(e.getKey(),e.getValue().getAsString()));
                if(!root.has("parent"))break;
                current=ResourceLocation.parse(root.get("parent").getAsString());
                if(current.getPath().startsWith("builtin/"))break;
            }catch(Exception ignored){break;}
        }
        for(String key:new String[]{"layer0","all","side","particle","top","texture","cross","plant"}) {
            String value=textures.get(key);
            for(int depth=0;value!=null&&value.startsWith("#")&&depth<16;depth++)value=textures.get(value.substring(1));
            if(value!=null&&!value.startsWith("#"))return ResourceLocation.parse(value);
        }
        // Failing datagen is preferable to quietly assigning an unrelated plant texture.
        throw new IllegalStateException("Cannot resolve plant storage source model texture: "+model);
    }
    private static ResourceLocation id(String name){return ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID,name);}
    private static JsonArray array(int... values){JsonArray a=new JsonArray();for(int value:values)a.add(value);return a;}
    private static JsonObject model(ResourceLocation texture,int stage) {
        JsonObject root=new JsonObject();root.addProperty("render_type","minecraft:cutout");
        JsonObject textures=new JsonObject();textures.addProperty("plant",texture.toString());textures.addProperty("particle",texture.toString());root.add("textures",textures);
        JsonArray elements=new JsonArray();
        // Eight produce bundles per layer, eight layers form the complete 64-item block.
        for(int layer=0;layer<stage;layer++)for(int z=0;z<2;z++)for(int x=0;x<4;x++) {
            JsonObject element=new JsonObject();element.add("from",array(x*4,layer*2,z*8));element.add("to",array(x*4+4,layer*2+2,z*8+8));
            JsonObject faces=new JsonObject();for(String direction:new String[]{"up","down","north","south","east","west"}) {
                if ((direction.equals("up") && layer < stage-1) || (direction.equals("down") && layer > 0)
                    || (direction.equals("west") && x > 0) || (direction.equals("east") && x < 3)
                    || (direction.equals("north") && z > 0) || (direction.equals("south") && z < 1)) continue;
                JsonObject face=new JsonObject();face.addProperty("texture","#plant");face.addProperty("tintindex",0);face.add("uv",array(0,0,16,16));faces.add(direction,face);
            }
            element.add("faces",faces);elements.add(element);
        }
        root.add("elements",elements);return root;
    }
    @Override public String getName(){return "Industron Plant Storage Models";}
}
