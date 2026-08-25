package net.mads.industron.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.mads.industron.Industron;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.IndustrialMaterials;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.recipes.MaterialRecipeHelper;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MaterialStoneLootProvider implements DataProvider {

    private final Path dataPackRoot;

    public MaterialStoneLootProvider(PackOutput output) {
        this.dataPackRoot = output.getOutputFolder(
                PackOutput.Target.DATA_PACK
        );
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> futures = new ArrayList<>();

        for (IndustrialMaterial material : IndustrialMaterials.ALL) {
            /*
             * Materialet må ha et dust-item for at denne
             * loot-tabellen skal kunne genereres.
             */
            if (!MaterialRecipeHelper.hasItems(
                    material,
                    MaterialPart.DUST
            )) {
                continue;
            }

            String dustItem = MaterialRecipeHelper.itemId(
                    material,
                    MaterialPart.DUST
            );

            for (var stoneSource : material.stoneSources()) {
                ResourceLocation blockId;

                /*
                 * Eksisterende stone, for eksempel:
                 *
                 * create:calcite
                 * minecraft:stone
                 *
                 * Da bruker vi ID-en til den eksisterende blokken.
                 */
                if (stoneSource.isExisting()) {
                    if (stoneSource.existingBlock().isEmpty()) {
                        continue;
                    }

                    blockId = stoneSource.existingBlock().get();
                } else {
                    /*
                     * Stone-blokk registrert av Industron.
                     */
                    blockId = ResourceLocation.fromNamespaceAndPath(
                            Industron.MOD_ID,
                            stoneSource.registryName(material)
                    );
                }

                Path lootTablePath = dataPackRoot
                        .resolve(blockId.getNamespace())
                        .resolve("loot_table")
                        .resolve("blocks")
                        .resolve(blockId.getPath() + ".json");

                futures.add(
                        DataProvider.saveStable(
                                output,
                                lootTable(blockId, dustItem),
                                lootTablePath
                        )
                );
            }
        }

        return CompletableFuture.allOf(
                futures.toArray(CompletableFuture[]::new)
        );
    }

    private static JsonObject lootTable(
            ResourceLocation blockId,
            String dustItem
    ) {
        JsonObject table = new JsonObject();
        table.addProperty(
                "type",
                "minecraft:block"
        );

        /*
         * Samme random_sequence-format som vanillas
         * block-loot-tabeller.
         */
        table.addProperty(
                "random_sequence",
                blockId.getNamespace()
                        + ":blocks/"
                        + blockId.getPath()
        );

        JsonArray pools = new JsonArray();
        JsonObject pool = new JsonObject();

        pool.addProperty("rolls", 1.0F);
        pool.addProperty("bonus_rolls", 0.0F);

        JsonArray entries = new JsonArray();

        /*
         * Ytre alternatives:
         *
     * 1. Silk Touch -> blokken
     * 2. Uten Silk Touch -> dust eller normal drop
     */
        JsonObject outerAlternatives = new JsonObject();
        outerAlternatives.addProperty(
                "type",
                "minecraft:alternatives"
        );

        JsonArray outerChildren = new JsonArray();

        /*
         * Alternativ 1:
         *
         * Silk Touch gir alltid selve blokken.
         */
        JsonObject silkTouchBlockEntry = itemEntry(
                blockId.toString()
        );

        JsonArray silkTouchConditions = new JsonArray();
        silkTouchConditions.add(silkTouchCondition());

        silkTouchBlockEntry.add(
                "conditions",
                silkTouchConditions
        );

        outerChildren.add(silkTouchBlockEntry);

        /*
         * Alternativ 2:
         *
         * Uten Silk Touch velges enten dust eller normal drop.
         */
        JsonObject normalAlternatives = new JsonObject();
        normalAlternatives.addProperty(
                "type",
                "minecraft:alternatives"
        );

        /*
         * Samme eksplosjonskontroll som gravel-tabellen.
         */
        JsonArray normalConditions = new JsonArray();
        normalConditions.add(survivesExplosionCondition());

        normalAlternatives.add(
                "conditions",
                normalConditions
        );

        JsonArray normalChildren = new JsonArray();

        /*
         * Dust-resultatet.
         *
         * Sjansene følger gravel -> flint:
         *
         * Ingen Fortune: 10 %
         * Fortune I:     14,285715 %
         * Fortune II:    25 %
         * Fortune III:   100 %
         */
        JsonObject dustEntry = itemEntry(dustItem);

        JsonArray dustConditions = new JsonArray();
        dustConditions.add(dustChanceCondition());

        dustEntry.add(
                "conditions",
                dustConditions
        );

        normalChildren.add(dustEntry);

        /*
         * Fallback:
         *
         * Dersom dust-sjansen feiler, dropper blokken sin normale drop.
         * Minecraft stone/deepslate er unntak her: de skal droppe sine cobbled-varianter.
         */
        normalChildren.add(
                itemEntry(normalDrop(blockId))
        );

        normalAlternatives.add(
                "children",
                normalChildren
        );

        outerChildren.add(normalAlternatives);

        outerAlternatives.add(
                "children",
                outerChildren
        );

        entries.add(outerAlternatives);

        pool.add("entries", entries);
        pools.add(pool);
        table.add("pools", pools);

        return table;
    }

    private static String normalDrop(ResourceLocation blockId) {
        if (blockId.equals(ResourceLocation.withDefaultNamespace("stone"))) {
            return "minecraft:cobblestone";
        }

        if (blockId.equals(ResourceLocation.withDefaultNamespace("deepslate"))) {
            return "minecraft:cobbled_deepslate";
        }

        return blockId.toString();
    }

    private static JsonObject itemEntry(String itemId) {
        JsonObject entry = new JsonObject();

        entry.addProperty(
                "type",
                "minecraft:item"
        );

        entry.addProperty(
                "name",
                itemId
        );

        return entry;
    }

    private static JsonObject silkTouchCondition() {
        JsonObject condition = new JsonObject();

        condition.addProperty(
                "condition",
                "minecraft:match_tool"
        );

        JsonObject predicate = new JsonObject();
        JsonObject predicates = new JsonObject();
        JsonArray enchantments = new JsonArray();

        JsonObject silkTouch = new JsonObject();

        silkTouch.addProperty(
                "enchantments",
                "minecraft:silk_touch"
        );

        JsonObject levels = new JsonObject();
        levels.addProperty("min", 1);

        silkTouch.add(
                "levels",
                levels
        );

        enchantments.add(silkTouch);

        predicates.add(
                "minecraft:enchantments",
                enchantments
        );

        predicate.add(
                "predicates",
                predicates
        );

        condition.add(
                "predicate",
                predicate
        );

        return condition;
    }

    private static JsonObject dustChanceCondition() {
        JsonObject condition = new JsonObject();

        condition.addProperty(
                "condition",
                "minecraft:table_bonus"
        );

        condition.addProperty(
                "enchantment",
                "minecraft:fortune"
        );

        JsonArray chances = new JsonArray();

        /*
         * Fortune 0
         */
        chances.add(0.1F);

        /*
         * Fortune I
         */
        chances.add(0.14285715F);

        /*
         * Fortune II
         */
        chances.add(0.25F);

        /*
         * Fortune III og høyere
         */
        chances.add(1.0F);

        condition.add(
                "chances",
                chances
        );

        return condition;
    }

    private static JsonObject survivesExplosionCondition() {
        JsonObject condition = new JsonObject();

        condition.addProperty(
                "condition",
                "minecraft:survives_explosion"
        );

        return condition;
    }

    @Override
    public String getName() {
        return "Industron Material Stone Loot Tables";
    }
}
