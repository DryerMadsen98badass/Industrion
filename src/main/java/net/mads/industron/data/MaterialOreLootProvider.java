package net.mads.industron.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.mads.industron.Industron;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.defenitions.IndustrialMaterials;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.MaterialOreHost;
import net.mads.industron.material.recipes.MaterialRecipeHelper;
import net.mads.industron.material.structure.StoneMaterial;
import net.mads.industron.material.structure.StructureMaterialGenerator;
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
import java.util.Optional;
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
            MaterialPart.NETHERRACK_ORE,
            MaterialPart.SMALL_NETHERRACK_ORE,
            MaterialPart.BLACKSTONE_ORE,
            MaterialPart.SMALL_BLACKSTONE_ORE,
            MaterialPart.BASALT_ORE,
            MaterialPart.SMALL_BASALT_ORE,
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

            if (materialBlocks == null) {
                materialBlocks = Map.of();
            }

            if (!MaterialRecipeHelper.hasItems(
                    material,
                    MaterialPart.RAW_ORE
            )) {
                continue;
            }

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
                                        blockId,
                                        part,
                                        hostForLegacyPart(part).orElse(null)
                                ),
                                lootTablePath(blockHolder.getId())
                        )
                );
            }

            if (MaterialOreHost.hasNaturalOre(material)) {
                Map<String, DeferredHolder<net.minecraft.world.level.block.Block, ? extends net.minecraft.world.level.block.Block>> hostBlocks =
                        BlockRegistry.MATERIAL_ORE_HOST_BLOCKS.get(material.id());
                if (hostBlocks != null) {
                    for (MaterialOreHost host : MaterialOreHost.compatibleHosts(material)) {
                        for (boolean small : new boolean[]{false, true}) {
                            DeferredHolder<net.minecraft.world.level.block.Block, ? extends net.minecraft.world.level.block.Block> blockHolder =
                                    hostBlocks.get(host.key(small));
                            if (blockHolder == null) continue;
                            String blockId = blockHolder.getId().toString();
                            futures.add(DataProvider.saveStable(
                                    output,
                                    createOreLootTable(material, blockId, small, host),
                                    lootTablePath(blockHolder.getId())
                            ));
                        }
                    }
                }
            }
        }

        return CompletableFuture.allOf(
                futures.toArray(CompletableFuture[]::new)
        );
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
            String blockId,
            MaterialPart orePart,
            MaterialOreHost host
    ) {
        return createOreLootTable(material, blockId, orePart.isSmallOre(), host);
    }

    private static JsonObject createOreLootTable(
            IndustrialMaterial material,
            String blockId,
            boolean smallOre,
            MaterialOreHost host
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
        float yieldScale = smallOre ? 0.50F : 1.00F;

        pools.add(createMainDropPool(
                material,
                blockId,
                yieldScale
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
                    CRUSHED_ORE_CHANCE * yieldScale
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
                    IMPURE_DUST_CHANCE * yieldScale
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
                    TINY_DUST_CHANCE * yieldScale
            ));
        }

        // Mining an ore can also recover one dust from the actual host rock. The chance is
        // exactly the vanilla gravel -> flint Fortune curve and is independent of the ore
        // material's own bonus drops. Silk Touch suppresses this pool.
        if (host != null) {
            resolveStoneDust(host.stone()).ifPresent(dust -> pools.add(createHostDustPool(dust)));
        }

        table.add("pools", pools);

        return table;
    }


    private static Optional<MaterialOreHost> hostForLegacyPart(MaterialPart part) {
        boolean small = part.isSmallOre();
        return MaterialOreHost.all().stream()
                .filter(host -> host.legacyPart(small).orElse(null) == part)
                .findFirst();
    }

    private static Optional<ResourceLocation> resolveStoneDust(StoneMaterial stone) {
        if (stone.isWithout(MaterialPart.DUST)) return Optional.empty();
        if (stone.hasExistingPart(MaterialPart.DUST)) return Optional.of(stone.existingPart(MaterialPart.DUST));
        if (StructureMaterialGenerator.generatedItemForms(stone).contains(MaterialPart.DUST)) {
            return Optional.of(ResourceLocation.fromNamespaceAndPath(
                    Industron.MOD_ID,
                    MaterialPart.DUST.registryName(stone)
            ));
        }
        return Optional.empty();
    }

    private static JsonObject createHostDustPool(ResourceLocation dustItem) {
        JsonObject pool = new JsonObject();
        pool.addProperty("rolls", 1);

        JsonArray conditions = new JsonArray();
        conditions.add(noSilkTouchCondition());
        conditions.add(hostDustChanceCondition());
        pool.add("conditions", conditions);

        JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", dustItem.toString());

        JsonArray functions = new JsonArray();
        JsonObject explosionDecay = new JsonObject();
        explosionDecay.addProperty("function", "minecraft:explosion_decay");
        functions.add(explosionDecay);
        entry.add("functions", functions);

        JsonArray entries = new JsonArray();
        entries.add(entry);
        pool.add("entries", entries);
        return pool;
    }

    private static JsonObject hostDustChanceCondition() {
        JsonObject condition = new JsonObject();
        condition.addProperty("condition", "minecraft:table_bonus");
        condition.addProperty("enchantment", "minecraft:fortune");

        JsonArray chances = new JsonArray();
        chances.add(0.1F);
        chances.add(1.0F / 7.0F);
        chances.add(0.25F);
        chances.add(1.0F);
        condition.add("chances", chances);
        return condition;
    }

    private static JsonObject createMainDropPool(
            IndustrialMaterial material,
            String blockId,
            float yieldScale
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

        if (yieldScale < 1.0F) {
            JsonArray rawOreConditions = new JsonArray();
            rawOreConditions.add(randomChanceCondition(yieldScale));
            rawOreEntry.add("conditions", rawOreConditions);
        }

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
