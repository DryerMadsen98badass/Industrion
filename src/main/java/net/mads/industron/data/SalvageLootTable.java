package net.mads.industron.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.mads.industron.machine.interaction.BlockLoot;
import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.Map;

/** JSON helpers for deterministic technical-block salvage. */
final class SalvageLootTable {
    private SalvageLootTable() {
    }

    static JsonObject empty() {
        JsonObject root = new JsonObject();
        root.addProperty("type", "minecraft:block");
        root.add("pools", new JsonArray());
        return root;
    }

    static JsonObject singleItem(ResourceLocation item) {
        return items(Map.of(item, 1));
    }

    /** Converts the shared declarative BlockLoot model into a generated vanilla block loot table. */
    static JsonObject from(BlockLoot loot, ResourceLocation selfBlockId) {
        if (loot == null || loot.mode() == BlockLoot.Mode.NONE) {
            return empty();
        }
        if (loot.mode() == BlockLoot.Mode.SELF) {
            return singleItem(selfBlockId);
        }

        JsonObject root = new JsonObject();
        root.addProperty("type", "minecraft:block");
        JsonArray pools = new JsonArray();

        if (loot.mode() == BlockLoot.Mode.ALL_OF) {
            for (BlockLoot.Entry entry : loot.entries()) {
                pools.add(chancedItemPool(entry.itemId(), entry.chance()));
            }
        } else if (loot.mode() == BlockLoot.Mode.ANY_OF && !loot.entries().isEmpty()) {
            JsonObject pool = new JsonObject();
            pool.addProperty("rolls", 1);
            pool.addProperty("bonus_rolls", 0);
            JsonArray entries = new JsonArray();
            for (BlockLoot.Entry candidate : loot.entries()) {
                JsonObject entry = new JsonObject();
                entry.addProperty("type", "minecraft:item");
                entry.addProperty("name", candidate.itemId().toString());
                entry.addProperty("weight", Math.max(1, Math.round(candidate.chance() * 10000.0F)));
                entries.add(entry);
            }
            pool.add("entries", entries);
            pools.add(pool);
        }

        root.add("pools", pools);
        return root;
    }

    /**
     * Guaranteed exact item/count drops. There are no chance conditions in this table;
     * the ~50% decision is already resolved deterministically by AssemblySalvageResolver.
     */
    static JsonObject items(Map<ResourceLocation, Integer> drops) {
        if (drops == null || drops.isEmpty()) {
            return empty();
        }

        JsonObject root = new JsonObject();
        root.addProperty("type", "minecraft:block");
        JsonArray pools = new JsonArray();

        drops.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null && entry.getValue() > 0)
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> pools.add(itemPool(entry.getKey(), entry.getValue())));

        root.add("pools", pools);
        return root;
    }

    private static JsonObject chancedItemPool(ResourceLocation item, float chance) {
        JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", item.toString());

        JsonObject pool = new JsonObject();
        pool.addProperty("rolls", 1);
        pool.addProperty("bonus_rolls", 0);
        JsonArray entries = new JsonArray();
        entries.add(entry);
        pool.add("entries", entries);

        if (chance < 1.0F) {
            JsonObject condition = new JsonObject();
            condition.addProperty("condition", "minecraft:random_chance");
            condition.addProperty("chance", Math.max(0.0F, chance));
            JsonArray conditions = new JsonArray();
            conditions.add(condition);
            pool.add("conditions", conditions);
        }
        return pool;
    }

    private static JsonObject itemPool(ResourceLocation item, int count) {
        JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", item.toString());

        if (count != 1) {
            JsonObject setCount = new JsonObject();
            setCount.addProperty("function", "minecraft:set_count");
            setCount.addProperty("count", count);
            JsonArray functions = new JsonArray();
            functions.add(setCount);
            entry.add("functions", functions);
        }

        JsonArray entries = new JsonArray();
        entries.add(entry);

        JsonObject pool = new JsonObject();
        pool.addProperty("rolls", 1);
        pool.addProperty("bonus_rolls", 0);
        pool.add("entries", entries);
        return pool;
    }
}
