package net.mads.industron.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.mads.industron.Industron;
import net.mads.industron.machine.foundry.FoundryPartType;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Basic self-drop loot for the visual Foundry blocks while their real component recipes are pending. */
public final class FoundryLootProvider implements DataProvider {
    private final PackOutput.PathProvider lootTables;

    public FoundryLootProvider(PackOutput output) {
        this.lootTables = output.createPathProvider(PackOutput.Target.DATA_PACK, "loot_table/blocks");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (FoundryPartType type : FoundryPartType.ALL) {
            ResourceLocation blockId = ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, type.id());
            futures.add(DataProvider.saveStable(output, selfDrop(blockId), lootTables.json(blockId)));
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private static JsonObject selfDrop(ResourceLocation blockId) {
        JsonObject root = new JsonObject();
        root.addProperty("type", "minecraft:block");

        JsonObject pool = new JsonObject();
        pool.addProperty("rolls", 1.0F);

        JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", blockId.toString());
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
        return "Industron Foundry Loot Tables";
    }
}
