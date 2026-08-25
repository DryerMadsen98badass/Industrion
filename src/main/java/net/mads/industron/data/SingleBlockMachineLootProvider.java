package net.mads.industron.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.mads.industron.Industron;
import net.mads.industron.machine.MachineDefinition;
import net.mads.industron.machine.SingleBlockMachineInstance;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class SingleBlockMachineLootProvider implements DataProvider {
    private final PackOutput.PathProvider lootTables;

    public SingleBlockMachineLootProvider(PackOutput output) {
        this.lootTables = output.createPathProvider(
                PackOutput.Target.DATA_PACK,
                "loot_table/blocks"
        );
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> futures = new ArrayList<>();

        for (SingleBlockMachineInstance instance
                : MachineDefinition.INSTANCES) {
            saveBlockLoot(
                    futures,
                    output,
                    instance.registryName()
            );
        }

        return CompletableFuture.allOf(
                futures.toArray(CompletableFuture[]::new)
        );
    }

    @Override
    public String getName() {
        return "Industron Singleblock Machine Loot Tables";
    }

    private void saveBlockLoot(
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
        root.addProperty("type", "minecraft:block");

        JsonArray pools = new JsonArray();
        JsonObject pool = new JsonObject();
        pool.addProperty("rolls", 1.0F);

        JsonArray entries = new JsonArray();
        JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", block.toString());
        entries.add(entry);
        pool.add("entries", entries);

        JsonArray conditions = new JsonArray();
        JsonObject survivesExplosion = new JsonObject();
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
}
