package net.mads.industron.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.registry.BlockRegistry;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Loot tables for generated industrial-material shafts. */
public final class MaterialShaftLootProvider implements DataProvider {
    private final PackOutput.PathProvider lootTables;

    public MaterialShaftLootProvider(PackOutput output) {
        this.lootTables = output.createPathProvider(PackOutput.Target.DATA_PACK, "loot_table/blocks");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> futures = new ArrayList<>();

        for (Map<MaterialPart, DeferredHolder<Block, ? extends Block>> blocks : BlockRegistry.MATERIAL_BLOCKS.values()) {
            DeferredHolder<Block, ? extends Block> shaft = blocks.get(MaterialPart.SHAFT);
            if (shaft == null) {
                continue;
            }

            ResourceLocation id = shaft.getId();
            futures.add(DataProvider.saveStable(output, selfDrop(id), lootTables.json(id)));
        }

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private static JsonObject selfDrop(ResourceLocation id) {
        JsonObject root = new JsonObject();
        root.addProperty("type", "minecraft:block");

        JsonObject pool = new JsonObject();
        pool.addProperty("rolls", 1.0F);

        JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", id.toString());

        JsonArray entries = new JsonArray();
        entries.add(entry);
        pool.add("entries", entries);

        JsonObject survivesExplosion = new JsonObject();
        survivesExplosion.addProperty("condition", "minecraft:survives_explosion");
        JsonArray conditions = new JsonArray();
        conditions.add(survivesExplosion);
        pool.add("conditions", conditions);

        JsonArray pools = new JsonArray();
        pools.add(pool);
        root.add("pools", pools);
        return root;
    }

    @Override
    public String getName() {
        return "Industron material shaft loot";
    }
}
