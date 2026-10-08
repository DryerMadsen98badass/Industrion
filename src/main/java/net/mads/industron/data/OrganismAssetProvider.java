package net.mads.industron.data;

import com.google.common.hash.Hashing;
import com.google.gson.JsonObject;
import net.mads.industron.material.organism.*;
import net.minecraft.data.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Grayscale variants generated from the installed Minecraft assets; no network or per-species PNGs. */
public final class OrganismAssetProvider implements DataProvider {
    private final Path root;
    private final ExistingFileHelper files;
    public OrganismAssetProvider(PackOutput output,ExistingFileHelper files) {
        root=output.getOutputFolder(PackOutput.Target.RESOURCE_PACK);this.files=files;

    }
    @Override public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> pending=new ArrayList<>();
        Set<String> textures=new HashSet<>();
        try {
            for(var entry:OrganismItemCatalog.generated()) {
                var model=new JsonObject();model.addProperty("parent","minecraft:item/generated");
                var layers=new JsonObject();layers.addProperty("layer0",entry.texture());model.add("textures",layers);
                pending.add(DataProvider.saveStable(output,model,root.resolve("industron/models/item/"+entry.itemId().substring(10)+".json")));

            }
            return CompletableFuture.allOf(pending.toArray(CompletableFuture[]::new));
        } catch(Exception e) { return CompletableFuture.failedFuture(e); }
    }
    private void generate(CachedOutput output,OrganismItemCatalog.Entry entry) throws IOException {
        String source=source(entry);
        double shade=entry.form()==OrganicForm.BURNT?0.23:entry.form()==OrganicForm.ROTTEN?0.64:1.0;
        if(entry.form()==OrganicForm.COOKED && !source.contains("cooked_"))shade=0.72;
        generate(output,source,entry.texture(),shade);
    }
    private void generate(CachedOutput output,String source,String texture,double shade) throws IOException {
        var id=ResourceLocation.parse(source);
        BufferedImage image;
        try(var stream=files.getResource(id,PackType.CLIENT_RESOURCES,".png","textures").open()) {
            image=ImageIO.read(stream);
        }
        if(image==null) throw new IOException("Unreadable organic texture source: "+id);
        BufferedImage gray=OrganicTextureGrayscale.convert(image,shade);
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();ImageIO.write(gray,"PNG",bytes);
        byte[] data=bytes.toByteArray();
        var target=ResourceLocation.parse(texture);
        output.writeIfNeeded(root.resolve(target.getNamespace()+"/textures/"+target.getPath()+".png"),data,Hashing.sha1().hashBytes(data));
    }
    private static String source(OrganismItemCatalog.Entry e) {
        if(e.form()==OrganicForm.DUST) return "industron:item/material_sets/dust/normal/variant_1/base";
        if(e.form()==OrganicForm.SMALL_DUST) return "industron:item/material_sets/dust/small/variant_1/base";
        if(e.form()==OrganicForm.TINY_DUST) return "industron:item/material_sets/dust/tiny/variant_1/base";
        if(e.part().cookingFamily()) {
            int index=Integer.parseInt(OrganismItemCatalog.variant(e.owner(),e.part()).substring(8))-1;
            String[] meat={"beef","porkchop","chicken","mutton","rabbit","rotten_flesh"};
            String[] fish={"cod","salmon","tropical_fish","pufferfish"};
            String name=(e.part()==OrganismPart.MEAT?meat:fish)[index];
            if(e.form()==OrganicForm.COOKED && (e.part()==OrganismPart.MEAT?index<5:index<2)) name="cooked_"+name;
            return "minecraft:item/"+name;
        }
        return switch(e.part()) {
            case BONE -> "minecraft:item/bone";
            case HIDE -> "minecraft:item/rabbit_hide";
            case FEATHER -> "minecraft:item/feather";
            case WOOL -> "minecraft:block/white_wool";
            case SILK -> "minecraft:item/string";
            case EYE -> "minecraft:item/spider_eye";
            case INK_SAC -> "minecraft:item/ink_sac";
            case SLIME -> "minecraft:item/slime_ball";
            case MEMBRANE -> "minecraft:item/phantom_membrane";
            case FOOT -> "minecraft:item/rabbit_foot";
            case HORN -> "minecraft:item/goat_horn";
            case SCUTE -> "minecraft:item/armadillo_scute";
            case EGG -> "minecraft:item/egg";
            default -> throw new IllegalArgumentException("No approved texture template for "+e.part());
        };
    }
    @Override public String getName() {return "Industron organism grayscale textures and models";}
}
