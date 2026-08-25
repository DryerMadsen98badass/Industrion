package net.mads.industron.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.mads.industron.Industron;
import net.mads.industron.material.structure.StructureBlockDefinition;
import net.mads.industron.material.structure.StructureMaterial;
import net.mads.industron.material.structure.StructureMaterialGenerator;
import net.mads.industron.material.structure.StructureMaterials;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Loot tables for generated wood/stone structure blocks. */
public final class StructureMaterialLootProvider implements DataProvider {
    private final PackOutput.PathProvider lootTables;

    public StructureMaterialLootProvider(PackOutput output) {
        this.lootTables = output.createPathProvider(PackOutput.Target.DATA_PACK, "loot_table/blocks");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (StructureMaterial material : StructureMaterials.ALL) {
            for (StructureBlockDefinition definition : StructureMaterialGenerator.generatedBlockDefinitions(material)) {
                JsonObject table = switch (definition.shape()) {
                    case SLAB -> slabLoot(definition.registryName());
                    case DOOR -> doorLoot(definition.registryName());
                    default -> normalLoot(definition.registryName());
                };
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, definition.registryName());
                Path path = lootTables.json(id);
                futures.add(DataProvider.saveStable(output, table, path));
            }
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private static JsonObject normalLoot(String id) {
        JsonObject root = root();
        JsonObject pool = pool();
        JsonObject entry = itemEntry(id);
        JsonArray entries = new JsonArray();
        entries.add(entry);
        pool.add("entries", entries);
        addSurvivesExplosion(pool);
        root.getAsJsonArray("pools").add(pool);
        return root;
    }

    private static JsonObject slabLoot(String id) {
        JsonObject root = root();
        JsonObject pool = pool();
        JsonObject entry = itemEntry(id);

        JsonArray functions = new JsonArray();
        JsonObject setCount = new JsonObject();
        setCount.addProperty("function", "minecraft:set_count");
        setCount.addProperty("count", 2.0F);

        JsonObject condition = new JsonObject();
        condition.addProperty("condition", "minecraft:block_state_property");
        condition.addProperty("block", rl(id));
        JsonObject properties = new JsonObject();
        properties.addProperty("type", "double");
        condition.add("properties", properties);
        JsonArray conditions = new JsonArray();
        conditions.add(condition);
        setCount.add("conditions", conditions);
        functions.add(setCount);

        JsonObject decay = new JsonObject();
        decay.addProperty("function", "minecraft:explosion_decay");
        functions.add(decay);
        entry.add("functions", functions);

        JsonArray entries = new JsonArray();
        entries.add(entry);
        pool.add("entries", entries);
        root.getAsJsonArray("pools").add(pool);
        return root;
    }

    private static JsonObject doorLoot(String id) {
        JsonObject root = root();
        JsonObject pool = pool();
        JsonArray entries = new JsonArray();
        entries.add(itemEntry(id));
        pool.add("entries", entries);

        JsonArray conditions = new JsonArray();
        JsonObject lowerHalf = new JsonObject();
        lowerHalf.addProperty("condition", "minecraft:block_state_property");
        lowerHalf.addProperty("block", rl(id));
        JsonObject properties = new JsonObject();
        properties.addProperty("half", "lower");
        lowerHalf.add("properties", properties);
        conditions.add(lowerHalf);
        JsonObject survives = new JsonObject();
        survives.addProperty("condition", "minecraft:survives_explosion");
        conditions.add(survives);
        pool.add("conditions", conditions);

        root.getAsJsonArray("pools").add(pool);
        return root;
    }

    private static JsonObject root() {
        JsonObject root = new JsonObject();
        root.addProperty("type", "minecraft:block");
        root.add("pools", new JsonArray());
        return root;
    }

    private static JsonObject pool() {
        JsonObject pool = new JsonObject();
        pool.addProperty("rolls", 1.0F);
        return pool;
    }

    private static JsonObject itemEntry(String id) {
        JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", rl(id));
        return entry;
    }

    private static void addSurvivesExplosion(JsonObject pool) {
        JsonArray conditions = new JsonArray();
        JsonObject survives = new JsonObject();
        survives.addProperty("condition", "minecraft:survives_explosion");
        conditions.add(survives);
        pool.add("conditions", conditions);
    }

    private static String rl(String id) {
        return Industron.MOD_ID + ":" + id;
    }

    @Override
    public String getName() {
        return "Industron structure material loot";
    }
}
