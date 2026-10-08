package net.mads.industron.data;

import net.mads.industron.Industron;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.recipes.MaterialCasingGenerator;
import net.mads.industron.material.recipes.MaterialRecipeHelper;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Salvage loot for generated material casings. The finished casing never drops itself. */
public final class MaterialCasingLootProvider implements DataProvider {
    private final PackOutput.PathProvider lootTables;

    public MaterialCasingLootProvider(PackOutput output) {
        lootTables = output.createPathProvider(PackOutput.Target.DATA_PACK, "loot_table/blocks");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (MaterialCasingGenerator.GeneratedCasing generated : MaterialCasingGenerator.ALL) {
            ResourceLocation block = ResourceLocation.fromNamespaceAndPath(
                    Industron.MOD_ID,
                    generated.registryName()
            );
            ResourceLocation recoveredPlate = ResourceLocation.parse(
                    MaterialRecipeHelper.itemId(generated.material(), MaterialPart.PLATE)
            );
            futures.add(DataProvider.saveStable(
                    output,
                    SalvageLootTable.singleItem(recoveredPlate),
                    lootTables.json(block)
            ));
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Industron Material Casing Salvage Loot Tables";
    }
}
