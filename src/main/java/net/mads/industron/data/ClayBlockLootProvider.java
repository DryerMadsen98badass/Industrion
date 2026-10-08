package net.mads.industron.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.defenitions.IndustrialMaterials;
import net.mads.industron.material.recipes.MaterialRecipeHelper;
import net.mads.industron.registry.BlockRegistry;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Clay blocks drop four matching clay balls; fired construction forms keep their existing drops. */
public final class ClayBlockLootProvider implements DataProvider {
    private final Path dataPackRoot;

    public ClayBlockLootProvider(PackOutput output) {
        dataPackRoot = output.getOutputFolder(PackOutput.Target.DATA_PACK);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (IndustrialMaterial material : IndustrialMaterials.ALL) {
            if (!material.isClayMaterial()) continue;
            for (MaterialPart part : List.of(
                    MaterialPart.CLAY_BLOCK,
                    MaterialPart.BRICKS,
                    MaterialPart.FIREBOX,
                    MaterialPart.BRICK_SLAB,
                    MaterialPart.BRICK_STAIRS,
                    MaterialPart.BRICK_WALL
            )) {
                var blocks = BlockRegistry.MATERIAL_BLOCKS.get(material.id());
                var holder = blocks == null ? null : blocks.get(part);
                ResourceLocation id;
                if (part == MaterialPart.CLAY_BLOCK && material.hasExistingPart(part)) {
                    id = material.existingPart(part);
                } else {
                    if (holder == null) continue;
                    id = holder.getId();
                }
                Path path = dataPackRoot.resolve(id.getNamespace()).resolve("loot_table/blocks")
                        .resolve(id.getPath() + ".json");
                futures.add(DataProvider.saveStable(
                        output,
                        part == MaterialPart.CLAY_BLOCK
                                ? clayBallDrop(MaterialRecipeHelper.itemId(material, MaterialPart.CLAY))
                                : part == MaterialPart.BRICK_SLAB ? slabDrop(id) : selfDrop(id),
                        path
                ));
            }
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private static JsonObject clayBallDrop(String itemId) {
        JsonObject table = new JsonObject();
        table.addProperty("type", "minecraft:block");
        JsonObject pool = new JsonObject();
        pool.addProperty("rolls", 1);
        pool.addProperty("bonus_rolls", 0);
        JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", itemId);
        JsonArray functions = new JsonArray();
        JsonObject count = new JsonObject();
        count.addProperty("function", "minecraft:set_count");
        count.addProperty("count", 4);
        functions.add(count);
        JsonObject decay = new JsonObject();
        decay.addProperty("function", "minecraft:explosion_decay");
        functions.add(decay);
        entry.add("functions", functions);
        JsonArray entries = new JsonArray();
        entries.add(entry);
        pool.add("entries", entries);
        JsonArray pools = new JsonArray();
        pools.add(pool);
        table.add("pools", pools);
        return table;
    }

    private static JsonObject slabDrop(ResourceLocation id) {
        JsonObject table = new JsonObject();
        table.addProperty("type", "minecraft:block");
        JsonObject pool = new JsonObject();
        pool.addProperty("rolls", 1);
        pool.addProperty("bonus_rolls", 0);

        JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", id.toString());

        JsonArray functions = new JsonArray();
        JsonObject setCount = new JsonObject();
        setCount.addProperty("function", "minecraft:set_count");
        setCount.addProperty("count", 2.0F);
        JsonArray setCountConditions = new JsonArray();
        JsonObject doubleSlab = new JsonObject();
        doubleSlab.addProperty("condition", "minecraft:block_state_property");
        doubleSlab.addProperty("block", id.toString());
        JsonObject properties = new JsonObject();
        properties.addProperty("type", "double");
        doubleSlab.add("properties", properties);
        setCountConditions.add(doubleSlab);
        setCount.add("conditions", setCountConditions);
        functions.add(setCount);

        JsonObject explosionDecay = new JsonObject();
        explosionDecay.addProperty("function", "minecraft:explosion_decay");
        functions.add(explosionDecay);
        entry.add("functions", functions);

        JsonArray entries = new JsonArray();
        entries.add(entry);
        pool.add("entries", entries);
        JsonArray pools = new JsonArray();
        pools.add(pool);
        table.add("pools", pools);
        return table;
    }

    private static JsonObject selfDrop(ResourceLocation id) {
        JsonObject table = new JsonObject();
        table.addProperty("type", "minecraft:block");
        JsonObject pool = new JsonObject();
        pool.addProperty("rolls", 1);
        pool.addProperty("bonus_rolls", 0);
        JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", id.toString());
        JsonArray conditions = new JsonArray();
        JsonObject survives = new JsonObject();
        survives.addProperty("condition", "minecraft:survives_explosion");
        conditions.add(survives);
        entry.add("conditions", conditions);
        JsonArray entries = new JsonArray();
        entries.add(entry);
        pool.add("entries", entries);
        JsonArray pools = new JsonArray();
        pools.add(pool);
        table.add("pools", pools);
        return table;
    }

    @Override
    public String getName() {
        return "Industron clay block loot";
    }
}
