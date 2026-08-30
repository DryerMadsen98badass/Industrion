package net.mads.industron.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.mads.industron.Industron;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.defenitions.StoneMaterials;
import net.mads.industron.material.structure.StoneMaterial;
import net.mads.industron.material.structure.StructureBlockDefinition;
import net.mads.industron.material.structure.StructureMaterialGenerator;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Owns the loot tables for the base STONE and COBBLED_STONE blocks of every
 * {@link StoneMaterial}.
 *
 * Rules:
 * - Silk Touch always drops the broken block itself.
 * - Without Silk Touch, both STONE and COBBLED_STONE can drop the material dust.
 * - Dust chance is exactly the vanilla gravel -> flint Fortune curve:
 *   10 %, 1/7, 25 %, 100 % for Fortune 0/I/II/III+.
 * - If dust does not drop, STONE drops the material's COBBLED_STONE when one exists.
 * - COBBLED_STONE drops itself when dust does not drop.
 *
 * Existing Minecraft/Create blocks are intentionally written to their own namespace
 * under the generated data pack. That replaces their normal block loot table while
 * Industron is loaded, so no manual per-stone loot override is required.
 */
public final class MaterialStoneLootProvider implements DataProvider {
    private final Path dataPackRoot;

    public MaterialStoneLootProvider(PackOutput output) {
        this.dataPackRoot = output.getOutputFolder(PackOutput.Target.DATA_PACK);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> futures = new ArrayList<>();

        for (StoneMaterial material : StoneMaterials.ALL) {
            Optional<ResourceLocation> dustItem = resolveDust(material);
            if (dustItem.isEmpty()) {
                continue;
            }

            Optional<ResourceLocation> stoneBlock = resolveBlock(material, MaterialPart.STONE);
            Optional<ResourceLocation> cobbledBlock = resolveBlock(material, MaterialPart.COBBLED_STONE);

            if (stoneBlock.isPresent()) {
                ResourceLocation fallback = cobbledBlock.orElse(stoneBlock.get());
                addLootTable(futures, output, stoneBlock.get(), dustItem.get(), fallback);
            }

            if (cobbledBlock.isPresent()) {
                addLootTable(futures, output, cobbledBlock.get(), dustItem.get(), cobbledBlock.get());
            }
        }

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private void addLootTable(
            List<CompletableFuture<?>> futures,
            CachedOutput output,
            ResourceLocation blockId,
            ResourceLocation dustItem,
            ResourceLocation normalDrop
    ) {
        Path lootTablePath = dataPackRoot
                .resolve(blockId.getNamespace())
                .resolve("loot_table")
                .resolve("blocks")
                .resolve(blockId.getPath() + ".json");

        futures.add(DataProvider.saveStable(
                output,
                lootTable(blockId, dustItem, normalDrop),
                lootTablePath
        ));
    }

    private static Optional<ResourceLocation> resolveDust(StoneMaterial material) {
        if (material.isWithout(MaterialPart.DUST)) {
            return Optional.empty();
        }
        if (material.hasExistingPart(MaterialPart.DUST)) {
            return Optional.of(material.existingPart(MaterialPart.DUST));
        }
        if (StructureMaterialGenerator.generatedItemForms(material).contains(MaterialPart.DUST)) {
            return Optional.of(ResourceLocation.fromNamespaceAndPath(
                    Industron.MOD_ID,
                    MaterialPart.DUST.registryName(material)
            ));
        }
        return Optional.empty();
    }

    private static Optional<ResourceLocation> resolveBlock(StoneMaterial material, MaterialPart part) {
        if (material.isWithout(part)) {
            return Optional.empty();
        }
        if (material.hasExistingPart(part)) {
            return Optional.of(material.existingPart(part));
        }

        return StructureMaterialGenerator.blockDefinitions(material).stream()
                .filter(definition -> definition.part().orElse(null) == part)
                .map(StructureBlockDefinition::registryName)
                .map(id -> ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, id))
                .findFirst();
    }

    private static JsonObject lootTable(
            ResourceLocation blockId,
            ResourceLocation dustItem,
            ResourceLocation normalDrop
    ) {
        JsonObject table = new JsonObject();
        table.addProperty("type", "minecraft:block");
        table.addProperty("random_sequence", blockId.getNamespace() + ":blocks/" + blockId.getPath());

        JsonObject pool = new JsonObject();
        pool.addProperty("rolls", 1.0F);
        pool.addProperty("bonus_rolls", 0.0F);

        JsonObject outerAlternatives = new JsonObject();
        outerAlternatives.addProperty("type", "minecraft:alternatives");
        JsonArray outerChildren = new JsonArray();

        // Silk Touch: preserve the exact block that was mined.
        JsonObject silkTouchEntry = itemEntry(blockId);
        JsonArray silkTouchConditions = new JsonArray();
        silkTouchConditions.add(silkTouchCondition());
        silkTouchEntry.add("conditions", silkTouchConditions);
        outerChildren.add(silkTouchEntry);

        // Normal mining: dust chance first, then stone/cobbled fallback.
        JsonObject normalAlternatives = new JsonObject();
        normalAlternatives.addProperty("type", "minecraft:alternatives");
        JsonArray normalConditions = new JsonArray();
        normalConditions.add(survivesExplosionCondition());
        normalAlternatives.add("conditions", normalConditions);

        JsonArray normalChildren = new JsonArray();
        JsonObject dustEntry = itemEntry(dustItem);
        JsonArray dustConditions = new JsonArray();
        dustConditions.add(dustChanceCondition());
        dustEntry.add("conditions", dustConditions);
        normalChildren.add(dustEntry);
        normalChildren.add(itemEntry(normalDrop));
        normalAlternatives.add("children", normalChildren);

        outerChildren.add(normalAlternatives);
        outerAlternatives.add("children", outerChildren);

        JsonArray entries = new JsonArray();
        entries.add(outerAlternatives);
        pool.add("entries", entries);

        JsonArray pools = new JsonArray();
        pools.add(pool);
        table.add("pools", pools);
        return table;
    }

    private static JsonObject itemEntry(ResourceLocation itemId) {
        JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", itemId.toString());
        return entry;
    }

    private static JsonObject silkTouchCondition() {
        JsonObject condition = new JsonObject();
        condition.addProperty("condition", "minecraft:match_tool");

        JsonObject predicate = new JsonObject();
        JsonObject predicates = new JsonObject();
        JsonArray enchantments = new JsonArray();
        JsonObject silkTouch = new JsonObject();
        silkTouch.addProperty("enchantments", "minecraft:silk_touch");

        JsonObject levels = new JsonObject();
        levels.addProperty("min", 1);
        silkTouch.add("levels", levels);
        enchantments.add(silkTouch);
        predicates.add("minecraft:enchantments", enchantments);
        predicate.add("predicates", predicates);
        condition.add("predicate", predicate);
        return condition;
    }

    private static JsonObject dustChanceCondition() {
        JsonObject condition = new JsonObject();
        condition.addProperty("condition", "minecraft:table_bonus");
        condition.addProperty("enchantment", "minecraft:fortune");

        JsonArray chances = new JsonArray();
        chances.add(0.1F);        // Fortune 0: 10 %
        chances.add(1.0F / 7.0F); // Fortune I: vanilla gravel -> flint
        chances.add(0.25F);       // Fortune II: 25 %
        chances.add(1.0F);        // Fortune III+: 100 %
        condition.add("chances", chances);
        return condition;
    }

    private static JsonObject survivesExplosionCondition() {
        JsonObject condition = new JsonObject();
        condition.addProperty("condition", "minecraft:survives_explosion");
        return condition;
    }

    @Override
    public String getName() {
        return "Industron Material Stone Loot Tables";
    }
}
