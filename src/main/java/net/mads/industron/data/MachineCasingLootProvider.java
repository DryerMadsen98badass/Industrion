package net.mads.industron.data;

import net.mads.industron.Industron;
import net.mads.industron.machine.MachineTier;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Standard tier machine casings are assembled technical blocks and never drop themselves. */
public final class MachineCasingLootProvider implements DataProvider {
    private final PackOutput.PathProvider lootTables;

    public MachineCasingLootProvider(PackOutput output) {
        this.lootTables = output.createPathProvider(PackOutput.Target.DATA_PACK, "loot_table/blocks");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (MachineTier tier : MachineTier.ALL) {
            ResourceLocation block = ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, tier.casingRegistryName());
            futures.add(DataProvider.saveStable(output, SalvageLootTable.empty(), lootTables.json(block)));
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Industron Machine Casing Salvage Loot Tables";
    }
}
