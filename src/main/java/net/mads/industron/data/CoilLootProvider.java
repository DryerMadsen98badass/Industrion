package net.mads.industron.data;

import net.mads.industron.Industron;
import net.mads.industron.block.coils.CoilDefinition;
import net.mads.industron.block.coils.CoilDefinitions;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Coils have no material recipe yet, so breaking them currently yields no recovered item. */
public class CoilLootProvider implements DataProvider {
    private final PackOutput.PathProvider lootTables;

    public CoilLootProvider(PackOutput output) {
        this.lootTables = output.createPathProvider(PackOutput.Target.DATA_PACK, "loot_table/blocks");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (CoilDefinition coil : CoilDefinitions.ALL) {
            ResourceLocation block = ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, coil.blockId());
            futures.add(DataProvider.saveStable(cache, SalvageLootTable.empty(), lootTables.json(block)));
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Industron Coil Salvage Loot Tables";
    }
}
