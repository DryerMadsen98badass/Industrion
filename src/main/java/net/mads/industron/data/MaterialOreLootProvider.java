package net.mads.industron.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.mads.industron.Industron;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.IndustrialMaterials;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.recipes.MaterialRecipeHelper;
import net.mads.industron.registry.BlockRegistry;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class MaterialOreLootProvider implements DataProvider {

    private static final float CRUSHED_ORE_CHANCE = 0.75F;
    private static final float IMPURE_DUST_CHANCE = 0.50F;
    private static final float TINY_DUST_CHANCE = 0.25F;

    private static final Set<MaterialPart> ORE_PARTS = EnumSet.of(
            MaterialPart.ORE,
            MaterialPart.SMALL_ORE,
            MaterialPart.DEEPSLATE_ORE,
            MaterialPart.SMALL_DEEPSLATE_ORE,
            MaterialPart.DIORITE_ORE,
            MaterialPart.SMALL_DIORITE_ORE,
            MaterialPart.ANDESITE_ORE,
            MaterialPart.SMALL_ANDESITE_ORE,
            MaterialPart.GRANITE_ORE,
            MaterialPart.SMALL_GRANITE_ORE,
            MaterialPart.TUFF_ORE,
            MaterialPart.SMALL_TUFF_ORE,
            MaterialPart.NETHERRACK_ORE,
            MaterialPart.SMALL_NETHERRACK_ORE,
            MaterialPart.BLACKSTONE_ORE,
            MaterialPart.SMALL_BLACKSTONE_ORE,
            MaterialPart.END_STONE_ORE,
            MaterialPart.SMALL_END_STONE_ORE
    );

    private final Path dataPackRoot;

    public MaterialOreLootProvider(PackOutput output) {
        this.dataPackRoot = output
                .getOutputFolder(PackOutput.Target.DATA_PACK);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> futures = new ArrayList<>();

        for (IndustrialMaterial material : IndustrialMaterials.ALL) {
            Map<MaterialPart, DeferredHolder<
                    net.minecraft.world.level.block.Block,
                    ? extends net.minecraft.world.level.block.Block
                    >> materialBlocks =
                    BlockRegistry.MATERIAL_BLOCKS.get(material.id());

            if (materialBlocks == null || materialBlocks.isEmpty()) {
                continue;
            }

            if (!MaterialRecipeHelper.hasItems(
                    material,
                    MaterialPart.RAW_ORE
            )) {
                continue;
            }

            addExistingOreLootTables(
                    futures,
                    output,
                    material
            );

            for (Map.Entry<MaterialPart, DeferredHolder<
                    net.minecraft.world.level.block.Block,
                    ? extends net.minecraft.world.level.block.Block
                    >> entry : materialBlocks.entrySet()) {

                MaterialPart part = entry.getKey();

                if (!ORE_PARTS.contains(part)) {
                    continue;
                }

                DeferredHolder<
                        net.minecraft.world.level.block.Block,
                        ? extends net.minecraft.world.level.block.Block
                        > blockHolder = entry.getValue();

                String blockId = blockHolder.getId().toString();

                futures.add(
                        DataProvider.saveStable(
                                output,
                                createOreLootTable(
                                        material,
                                        blockId
                                ),
                                lootTablePath(blockHolder.getId())
                        )
                );
            }
        }

        return CompletableFuture.allOf(
                futures.toArray(CompletableFuture[]::new)
        );
    }

    private void addExistingOreLootTables(
            List<CompletableFuture<?>> futures,
            CachedOutput output,
            IndustrialMaterial material
    ) {
        for (MaterialPart part : ORE_PARTS) {
            if (!material.hasExistingPart(part)) {
                continue;
            }

            ResourceLocation blockId =
                    material.existingPart(part);

            futures.add(
                    DataProvider.saveStable(
                            output,
                            createOreLootTable(
                                    material,
                                    blockId.toString()
                            ),
                            lootTablePath(blockId)
                    )
            );
        }
    }

    private Path lootTablePath(
            ResourceLocation blockId
    ) {
        return dataPackRoot
                .resolve(blockId.getNamespace())
                .resolve("loot_table")
                .resolve("blocks")
                .resolve(blockId.getPath() + ".json");
    }

    private static JsonObject createOreLootTable(
            IndustrialMaterial material,
            String blockId
    ) {
        JsonObject table = new JsonObject();
        table.addProperty("type", "minecraft:block");

        JsonArray pools = new JsonArray();

        /*
         * Silk Touch:
         * Dropper selve ore-blokken.
         *
         * Uten Silk Touch:
         * Dropper raw ore, påvirket av Fortune.
         */
        pools.add(createMainDropPool(
                material,
                blockId
        ));

        /*
         * 75 % sjanse for crushed ore.
         */
        if (MaterialRecipeHelper.hasItems(
                material,
                MaterialPart.CRUSHED_ORE
        )) {
            pools.add(createBonusDropPool(
                    MaterialRecipeHelper.itemId(
                            material,
                            MaterialPart.CRUSHED_ORE
                    ),
                    CRUSHED_ORE_CHANCE
            ));
        }

        /*
         * 50 % sjanse for impure dust.
         */
        if (MaterialRecipeHelper.hasItems(
                material,
                MaterialPart.IMPURE_DUST
        )) {
            pools.add(createBonusDropPool(
                    MaterialRecipeHelper.itemId(
                            material,
                            MaterialPart.IMPURE_DUST
                    ),
                    IMPURE_DUST_CHANCE
            ));
        }

        /*
         * 25 % sjanse for tiny dust.
         */
        if (MaterialRecipeHelper.hasItems(
                material,
                MaterialPart.TINY_DUST
        )) {
            pools.add(createBonusDropPool(
                    MaterialRecipeHelper.itemId(
                            material,
                            MaterialPart.TINY_DUST
                    ),
                    TINY_DUST_CHANCE
            ));
        }

        table.add("pools", pools);

        return table;
    }

    private static JsonObject createMainDropPool(
            IndustrialMaterial material,
            String blockId
    ) {
        JsonObject pool = new JsonObject();
        pool.addProperty("rolls", 1);

        JsonArray entries = new JsonArray();

        JsonObject alternatives = new JsonObject();
        alternatives.addProperty(
                "type",
                "minecraft:alternatives"
        );

        JsonArray children = new JsonArray();

        /*
         * Første alternativ:
         * Silk Touch gir selve ore-blokken.
         */
        JsonObject silkTouchEntry = new JsonObject();
        silkTouchEntry.addProperty(
                "type",
                "minecraft:item"
        );
        silkTouchEntry.addProperty(
                "name",
                blockId
        );

        JsonArray silkTouchConditions = new JsonArray();
        silkTouchConditions.add(silkTouchCondition());

        silkTouchEntry.add(
                "conditions",
                silkTouchConditions
        );

        children.add(silkTouchEntry);

        /*
         * Andre alternativ:
         * Raw ore når verktøyet ikke har Silk Touch.
         */
        JsonObject rawOreEntry = new JsonObject();
        rawOreEntry.addProperty(
                "type",
                "minecraft:item"
        );
        rawOreEntry.addProperty(
                "name",
                MaterialRecipeHelper.itemId(
                        material,
                        MaterialPart.RAW_ORE
                )
        );

        rawOreEntry.add(
                "functions",
                fortuneFunctions()
        );

        children.add(rawOreEntry);

        alternatives.add("children", children);
        entries.add(alternatives);

        pool.add("entries", entries);

        return pool;
    }

    private static JsonObject createBonusDropPool(
            String itemId,
            float chance
    ) {
        JsonObject pool = new JsonObject();
        pool.addProperty("rolls", 1);

        JsonArray conditions = new JsonArray();

        /*
         * Bonusdrops skal ikke forekomme med Silk Touch.
         */
        conditions.add(noSilkTouchCondition());
        conditions.add(randomChanceCondition(chance));

        pool.add("conditions", conditions);

        JsonArray entries = new JsonArray();

        JsonObject entry = new JsonObject();
        entry.addProperty(
                "type",
                "minecraft:item"
        );
        entry.addProperty(
                "name",
                itemId
        );

        /*
         * Fortune øker bonusdropens mengde.
         */
        entry.add(
                "functions",
                fortuneFunctions()
        );

        entries.add(entry);
        pool.add("entries", entries);

        return pool;
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

        silkTouch.add("levels", levels);
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

    private static JsonObject noSilkTouchCondition() {
        JsonObject condition = new JsonObject();
        condition.addProperty(
                "condition",
                "minecraft:inverted"
        );

        condition.add(
                "term",
                silkTouchCondition()
        );

        return condition;
    }

    private static JsonObject randomChanceCondition(float chance) {
        JsonObject condition = new JsonObject();
        condition.addProperty(
                "condition",
                "minecraft:random_chance"
        );
        condition.addProperty(
                "chance",
                chance
        );

        return condition;
    }

    private static JsonArray fortuneFunctions() {
        JsonArray functions = new JsonArray();

        JsonObject fortune = new JsonObject();
        fortune.addProperty(
                "function",
                "minecraft:apply_bonus"
        );
        fortune.addProperty(
                "enchantment",
                "minecraft:fortune"
        );
        fortune.addProperty(
                "formula",
                "minecraft:ore_drops"
        );

        functions.add(fortune);

        JsonObject explosionDecay = new JsonObject();
        explosionDecay.addProperty(
                "function",
                "minecraft:explosion_decay"
        );

        functions.add(explosionDecay);

        return functions;
    }

    @Override
    public String getName() {
        return "Industron Material Ore Loot Tables";
    }
}
