package net.mads.industron.data;

import net.mads.industron.Industron;
import net.mads.industron.machine.MachineDefinition;
import net.mads.industron.machine.SingleBlockMachineInstance;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Singleblock machines never drop the assembled machine itself.
 *
 * <p>The project does not yet expose a reliable material/component bill-of-materials for every
 * machine, so this provider intentionally emits empty salvage until that composition is known.
 * This avoids inventing a wrong material drop from the machine tier alone.</p>
 */
public final class SingleBlockMachineLootProvider implements DataProvider {
    private final PackOutput.PathProvider lootTables;

    public SingleBlockMachineLootProvider(PackOutput output) {
        this.lootTables = output.createPathProvider(PackOutput.Target.DATA_PACK, "loot_table/blocks");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (SingleBlockMachineInstance instance : MachineDefinition.INSTANCES) {
            ResourceLocation block = ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, instance.registryName());
            futures.add(DataProvider.saveStable(output, SalvageLootTable.empty(), lootTables.json(block)));
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Industron Singleblock Machine Salvage Loot Tables";
    }
}
