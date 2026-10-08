package net.mads.industron.client.model;
import net.mads.industron.Industron;
import net.mads.industron.registry.ItemRegistry;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.event.ModelEvent;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.Consumer;
public final class ItemGeometrySharing {
    private ItemGeometrySharing(){}
    public static void apply(ModelEvent.ModifyBakingResult event) {
        VertexSharingCache cache=new VertexSharingCache();
        Set<BakedModel> visited=Collections.newSetFromMap(new IdentityHashMap<>());
        Set<BakedQuad> quads=Collections.newSetFromMap(new IdentityHashMap<>());
        RandomSource random=RandomSource.create(0);
        Consumer<Item> share=item->{
            if(item instanceof BlockItem)return;
            BakedModel model=event.getModels().get(ModelResourceLocation.inventory(BuiltInRegistries.ITEM.getKey(item)));
            if(model==null||model.getClass()!=SimpleBakedModel.class||!visited.add(model))return;
            shareQuads(model.getQuads(null,null,random),quads,cache);
            for(Direction direction:Direction.values())shareQuads(model.getQuads(null,direction,random),quads,cache);
        };
        ItemRegistry.getAllMaterialItems().forEach(holder->share.accept(holder.get()));
        ItemRegistry.getAllStructureMaterialFormItems().forEach(holder->share.accept(holder.get()));
        ItemRegistry.getAllPlantMaterialItems().forEach(holder->share.accept(holder.get()));
        ItemRegistry.getAllPlantProcessIntermediateItems().forEach(holder->share.accept(holder.get()));
        Industron.LOGGER.info("Industron item geometry: {} static models, {} bytes of duplicate vertex data shared",visited.size(),cache.savedBytes());
    }
    private static void shareQuads(java.util.List<BakedQuad> values,Set<BakedQuad> visited,VertexSharingCache cache) {
        for(BakedQuad quad:values)if(visited.add(quad)&&quad instanceof SharedQuadVertices sharing)sharing.industron$shareVertices(cache);
    }
}
