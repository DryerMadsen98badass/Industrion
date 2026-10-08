package net.mads.industron.integration.create.kinetic;

import net.mads.industron.mixin.CreateRecipeManagerAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import java.util.*;

/** Bounded save-time identity cache. Never duplicates the whole recipe registry or searches each tick. */
public final class NativeCreateRecipeIdentity {
    private NativeCreateRecipeIdentity() {}
    private static final Map<RecipeManager,Cache> CACHES=new WeakHashMap<>();
    private static final class Cache {
        final java.lang.ref.WeakReference<Map<ResourceLocation,RecipeHolder<?>>> source;
        final LinkedHashMap<Recipe<?>,String> ids=new LinkedHashMap<>(16,0.75F,true);
        Cache(Map<ResourceLocation,RecipeHolder<?>> source) {this.source=new java.lang.ref.WeakReference<>(source);}
    }
    public static synchronized String id(Level level,Recipe<?> recipe) {
        if(level==null || recipe==null)return "";
        RecipeManager manager=level.getRecipeManager();
        Map<ResourceLocation,RecipeHolder<?>> source=((CreateRecipeManagerAccess)(Object)manager).industron$recipesByName();
        Cache cache=CACHES.get(manager);
        if(cache==null || cache.source.get()!=source) {cache=new Cache(source);CACHES.put(manager,cache);}
        String known=cache.ids.get(recipe);if(known!=null)return known;
        for(var entry:source.entrySet())if(entry.getValue().value()==recipe) {
            String result=entry.getKey().toString();cache.ids.put(recipe,result);
            if(cache.ids.size()>512)cache.ids.remove(cache.ids.keySet().iterator().next());
            return result;
        }
        return "";
    }
    public static Recipe<?> restore(Level level,String id) {
        if(level==null || id==null || id.isBlank())return null;
        ResourceLocation key=ResourceLocation.tryParse(id);if(key==null)return null;
        return level.getRecipeManager().byKey(key).map(RecipeHolder::value).orElse(null);
    }
}
