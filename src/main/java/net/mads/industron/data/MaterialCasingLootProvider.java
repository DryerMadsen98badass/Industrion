package net.mads.industron.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.mads.industron.Industron;
import net.mads.industron.material.recipes.MaterialCasingGenerator;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Drop-self loot tables for automatically generated material casings. */
public final class MaterialCasingLootProvider implements DataProvider {
    private final PackOutput.PathProvider lootTables;

    public MaterialCasingLootProvider(PackOutput output) {
        lootTables = output.createPathProvider(PackOutput.Target.DATA_PACK, "loot_table/blocks");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (MaterialCasingGenerator.GeneratedCasing generated : MaterialCasingGenerator.ALL) {
            ResourceLocation block = ResourceLocation.fromNamespaceAndPath(
                    Industron.MOD_ID,
                    generated.registryName()
            );

            JsonObject root = new JsonObject();
            root.addProperty("type", "minecraft:block");

            JsonObject pool = new JsonObject();
            pool.addProperty("rolls", 1.0F);

            JsonObject entry = new JsonObject();
            entry.addProperty("type", "minecraft:item");
            entry.addProperty("name", block.toString());
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

            futures.add(DataProvider.saveStable(output, root, lootTables.json(block)));
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Industron Material Casing Loot Tables";
    }
}
