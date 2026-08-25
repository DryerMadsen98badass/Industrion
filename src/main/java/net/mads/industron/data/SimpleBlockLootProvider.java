package net.mads.industron.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.mads.industron.Industron;
import net.mads.industron.block.SimpleBlockDefinition;
import net.mads.industron.block.SimpleBlockVariant;
import net.mads.industron.block.SimpleBlocks;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class SimpleBlockLootProvider implements DataProvider {

    private final PackOutput.PathProvider lootTables;

    public SimpleBlockLootProvider(PackOutput output) {
        this.lootTables = output.createPathProvider(
                PackOutput.Target.DATA_PACK,
                "loot_table/blocks"
        );
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> futures =
                new ArrayList<>();

        for (SimpleBlockDefinition definition : SimpleBlocks.ALL) {
            saveNormalBlockLoot(
                    futures,
                    output,
                    definition.id()
            );

            for (SimpleBlockVariant variant
                    : definition.variants()) {

                String variantId =
                        definition.variantId(variant);

                if (variant == SimpleBlockVariant.SLAB) {
                    saveSlabLoot(
                            futures,
                            output,
                            variantId
                    );
                } else {
                    saveNormalBlockLoot(
                            futures,
                            output,
                            variantId
                    );
                }
            }
        }

        return CompletableFuture.allOf(
                futures.toArray(CompletableFuture[]::new)
        );
    }

    private void saveNormalBlockLoot(
            List<CompletableFuture<?>> futures,
            CachedOutput output,
            String blockId
    ) {
        ResourceLocation block =
                ResourceLocation.fromNamespaceAndPath(
                        Industron.MOD_ID,
                        blockId
                );

        JsonObject root = new JsonObject();
        root.addProperty(
                "type",
                "minecraft:block"
        );

        JsonArray pools = new JsonArray();
        JsonObject pool = new JsonObject();

        pool.addProperty("rolls", 1.0F);

        JsonArray entries = new JsonArray();
        JsonObject entry = new JsonObject();

        entry.addProperty(
                "type",
                "minecraft:item"
        );

        entry.addProperty(
                "name",
                block.toString()
        );

        entries.add(entry);
        pool.add("entries", entries);

        JsonArray conditions = new JsonArray();
        JsonObject survivesExplosion =
                new JsonObject();

        survivesExplosion.addProperty(
                "condition",
                "minecraft:survives_explosion"
        );

        conditions.add(survivesExplosion);
        pool.add("conditions", conditions);

        pools.add(pool);
        root.add("pools", pools);

        Path path = lootTables.json(block);

        futures.add(
                DataProvider.saveStable(
                        output,
                        root,
                        path
                )
        );
    }

    private void saveSlabLoot(
            List<CompletableFuture<?>> futures,
            CachedOutput output,
            String slabId
    ) {
        ResourceLocation slab =
                ResourceLocation.fromNamespaceAndPath(
                        Industron.MOD_ID,
                        slabId
                );

        JsonObject root = new JsonObject();
        root.addProperty(
                "type",
                "minecraft:block"
        );

        JsonArray pools = new JsonArray();
        JsonObject pool = new JsonObject();

        pool.addProperty("rolls", 1.0F);

        JsonArray entries = new JsonArray();
        JsonObject entry = new JsonObject();

        entry.addProperty(
                "type",
                "minecraft:item"
        );

        entry.addProperty(
                "name",
                slab.toString()
        );

        JsonArray functions = new JsonArray();

        JsonObject setCount = new JsonObject();
        setCount.addProperty(
                "function",
                "minecraft:set_count"
        );
        setCount.addProperty(
                "count",
                2.0F
        );

        JsonArray setCountConditions =
                new JsonArray();

        JsonObject doubleSlabCondition =
                new JsonObject();

        doubleSlabCondition.addProperty(
                "condition",
                "minecraft:block_state_property"
        );

        doubleSlabCondition.addProperty(
                "block",
                slab.toString()
        );

        JsonObject properties = new JsonObject();
        properties.addProperty(
                "type",
                "double"
        );

        doubleSlabCondition.add(
                "properties",
                properties
        );

        setCountConditions.add(
                doubleSlabCondition
        );

        setCount.add(
                "conditions",
                setCountConditions
        );

        functions.add(setCount);

        JsonObject explosionDecay =
                new JsonObject();

        explosionDecay.addProperty(
                "function",
                "minecraft:explosion_decay"
        );

        functions.add(explosionDecay);

        entry.add("functions", functions);
        entries.add(entry);
        pool.add("entries", entries);

        pools.add(pool);
        root.add("pools", pools);

        Path path = lootTables.json(slab);

        futures.add(
                DataProvider.saveStable(
                        output,
                        root,
                        path
                )
        );
    }

    @Override
    public String getName() {
        return "Industron Simple Block Loot Tables";
    }
}