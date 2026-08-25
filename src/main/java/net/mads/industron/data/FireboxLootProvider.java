package net.mads.industron.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.mads.industron.Industron;
import net.mads.industron.block.ActiveBlockDefinition;
import net.mads.industron.block.SimpleBlocks;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class FireboxLootProvider implements DataProvider {
    private final PackOutput output;

    public FireboxLootProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        Path data = output.getOutputFolder(PackOutput.Target.DATA_PACK)
                .resolve(Industron.MOD_ID)
                .resolve("loot_table")
                .resolve("blocks");

        for (ActiveBlockDefinition definition : SimpleBlocks.ACTIVE) {
            futures.add(DataProvider.saveStable(cache, lootTable(definition), data.resolve(definition.id() + ".json")));
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Industron Active Block Loot Tables";
    }

    private static JsonObject lootTable(ActiveBlockDefinition definition) {
        JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", Industron.MOD_ID + ":" + definition.id());

        JsonArray entries = new JsonArray();
        entries.add(entry);

        JsonObject condition = new JsonObject();
        condition.addProperty("condition", "minecraft:survives_explosion");

        JsonArray conditions = new JsonArray();
        conditions.add(condition);

        JsonObject pool = new JsonObject();
        pool.addProperty("rolls", 1);
        pool.add("entries", entries);
        pool.add("conditions", conditions);

        JsonArray pools = new JsonArray();
        pools.add(pool);

        JsonObject json = new JsonObject();
        json.addProperty("type", "minecraft:block");
        json.add("pools", pools);
        return json;
    }
}
