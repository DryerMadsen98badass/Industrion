package net.mads.industron.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.mads.industron.Industron;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.defenitions.StoneMaterials;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Loot for the internal worldgen-only loose-pebble blocks. */
public final class PebbleWorldgenLootProvider implements DataProvider {
    private final PackOutput.PathProvider lootTables;

    public PebbleWorldgenLootProvider(PackOutput output) {
        this.lootTables = output.createPathProvider(PackOutput.Target.DATA_PACK, "loot_table/blocks");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (var stone : StoneMaterials.ALL) {
            if (!stone.generatedForms().contains(MaterialPart.PEBBLE)) continue;
            ResourceLocation blockId = ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, stone.id() + "_loose_pebble");
            ResourceLocation itemId = ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, MaterialPart.PEBBLE.registryName(stone));
            futures.add(DataProvider.saveStable(output, table(itemId), lootTables.json(blockId)));
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private static JsonObject table(ResourceLocation item) {
        JsonObject root = new JsonObject();
        root.addProperty("type", "minecraft:block");
        JsonArray pools = new JsonArray();
        JsonObject pool = new JsonObject();
        pool.addProperty("rolls", 1.0F);
        JsonArray entries = new JsonArray();
        JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", item.toString());
        entries.add(entry);
        pool.add("entries", entries);
        pools.add(pool);
        root.add("pools", pools);
        return root;
    }

    @Override
    public String getName() {
        return "Industron Pebble Worldgen Loot";
    }
}
