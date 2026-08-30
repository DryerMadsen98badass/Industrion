package net.mads.industron.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.mads.industron.Industron;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Emits the datapack hooks for Industron geology and removes legacy random ore/stone deposits.
 *
 * <p>Natural material deposits are owned by OreMaterials + StoneMaterials. Existing vanilla/mod
 * block ids may still be reused as block forms, but their original placed features are removed so
 * they cannot bypass the geology planner.</p>
 */
public final class GeologyWorldgenDataProvider implements DataProvider {
    private static final List<String> OVERWORLD_REMOVED_FEATURES = List.of(
            // Vanilla stone blobs that are now placed only by Industron geology around deposits.
            "minecraft:ore_granite_upper",
            "minecraft:ore_granite_lower",
            "minecraft:ore_diorite_upper",
            "minecraft:ore_diorite_lower",
            "minecraft:ore_andesite_upper",
            "minecraft:ore_andesite_lower",
            "minecraft:ore_tuff",
            "minecraft:amethyst_geode",

            // Vanilla ores. Block mappings may remain, but normal ore generation does not.
            "minecraft:ore_coal_upper",
            "minecraft:ore_coal_lower",
            "minecraft:ore_iron_upper",
            "minecraft:ore_iron_middle",
            "minecraft:ore_iron_small",
            "minecraft:ore_gold_extra",
            "minecraft:ore_gold",
            "minecraft:ore_gold_lower",
            "minecraft:ore_redstone",
            "minecraft:ore_redstone_lower",
            "minecraft:ore_diamond",
            "minecraft:ore_diamond_medium",
            "minecraft:ore_diamond_large",
            "minecraft:ore_diamond_buried",
            "minecraft:ore_lapis",
            "minecraft:ore_lapis_buried",
            "minecraft:ore_infested",
            "minecraft:ore_emerald",
            "minecraft:ore_copper",
            "minecraft:ore_copper_large",

            // Create's layered stone deposits and zinc bypass the StoneMaterials/OreMaterials graph.
            "create:striated_ores_overworld",
            "create:zinc_ore"
    );

    private static final List<String> NETHER_REMOVED_FEATURES = List.of(
            "minecraft:ore_gold_deltas",
            "minecraft:ore_quartz_deltas",
            "minecraft:ore_gold_nether",
            "minecraft:ore_quartz_nether",
            "minecraft:ore_blackstone",
            "minecraft:ore_ancient_debris_large",
            "minecraft:ore_debris_small",
            "create:striated_ores_nether"
    );

    private final Path dataPackRoot;

    public GeologyWorldgenDataProvider(PackOutput output) {
        this.dataPackRoot = output.getOutputFolder(PackOutput.Target.DATA_PACK);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        futures.add(DataProvider.saveStable(output, configuredFeature(), path(
                Industron.MOD_ID, "worldgen/configured_feature/geology_deposits.json")));
        futures.add(DataProvider.saveStable(output, placedFeature(), path(
                Industron.MOD_ID, "worldgen/placed_feature/geology_deposits.json")));
        futures.add(DataProvider.saveStable(output, addBiomeModifier("#minecraft:is_overworld"), path(
                Industron.MOD_ID, "neoforge/biome_modifier/geology_overworld.json")));
        futures.add(DataProvider.saveStable(output, addBiomeModifier("#minecraft:is_nether"), path(
                Industron.MOD_ID, "neoforge/biome_modifier/geology_nether.json")));
        futures.add(DataProvider.saveStable(output, addBiomeModifier("#minecraft:is_end"), path(
                Industron.MOD_ID, "neoforge/biome_modifier/geology_end.json")));

        futures.add(DataProvider.saveStable(output, removeFeaturesBiomeModifier(
                "#minecraft:is_overworld", OVERWORLD_REMOVED_FEATURES), path(
                Industron.MOD_ID, "neoforge/biome_modifier/remove_legacy_overworld_geology.json")));
        futures.add(DataProvider.saveStable(output, removeFeaturesBiomeModifier(
                "#minecraft:is_nether", NETHER_REMOVED_FEATURES), path(
                Industron.MOD_ID, "neoforge/biome_modifier/remove_legacy_nether_geology.json")));

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private Path path(String namespace, String relative) {
        return dataPackRoot.resolve(namespace).resolve(relative);
    }

    private static JsonObject configuredFeature() {
        JsonObject json = new JsonObject();
        json.addProperty("type", Industron.MOD_ID + ":geology_deposit");
        json.add("config", new JsonObject());
        return json;
    }

    private static JsonObject placedFeature() {
        JsonObject json = new JsonObject();
        json.addProperty("feature", Industron.MOD_ID + ":geology_deposits");
        JsonArray placement = new JsonArray();

        JsonObject count = new JsonObject();
        count.addProperty("type", "minecraft:count");
        count.addProperty("count", 1);
        placement.add(count);

        JsonObject inSquare = new JsonObject();
        inSquare.addProperty("type", "minecraft:in_square");
        placement.add(inSquare);

        JsonObject biome = new JsonObject();
        biome.addProperty("type", "minecraft:biome");
        placement.add(biome);

        json.add("placement", placement);
        return json;
    }

    private static JsonObject addBiomeModifier(String biomes) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "neoforge:add_features");
        json.addProperty("biomes", biomes);
        json.addProperty("features", Industron.MOD_ID + ":geology_deposits");
        json.addProperty("step", "underground_ores");
        return json;
    }

    private static JsonObject removeFeaturesBiomeModifier(String biomes, List<String> features) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "neoforge:remove_features");
        json.addProperty("biomes", biomes);
        JsonArray values = new JsonArray();
        for (String feature : features) values.add(feature);
        json.add("features", values);
        // Deliberately omit `steps`: NeoForge then removes the listed features from every
        // decoration step, which also catches geodes/local-modification features.
        return json;
    }

    @Override
    public String getName() {
        return "Industron Geology Worldgen";
    }
}
