package net.mads.industron.data;

import com.google.gson.JsonObject;
import net.mads.industron.machine.machines.kinetic.KineticMachines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Native machine models live under block/machines/kinetic; registry-facing assets are generated. */
public final class KineticMachineAssetProvider implements DataProvider {
    private final PackOutput output;
    private final boolean client;
    private final boolean server;
    public KineticMachineAssetProvider(PackOutput output, boolean client, boolean server) {
        this.output = output; this.client = client; this.server = server;
    }
    @Override public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> saves = new ArrayList<>();
        var assets = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve("industron");
        var data = output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("industron");
        for (var holder : KineticMachines.blocks()) {
            String id = BuiltInRegistries.BLOCK.getKey(holder.get()).getPath();
            if (client) {
                JsonObject variants = new JsonObject();
                String[] faces = {"north", "east", "south", "west"};
                for (int i = 0; i < faces.length; i++) {
                    JsonObject variant = new JsonObject();
                    variant.addProperty("model", "industron:block/machines/kinetic/" + id + "/body");
                    if (i > 0) variant.addProperty("y", i * 90);
                    variants.add("facing=" + faces[i], variant);
                }
                JsonObject state = new JsonObject(); state.add("variants", variants);
                JsonObject item = new JsonObject(); item.addProperty("parent", "industron:block/machines/kinetic/" + id + "/item");
                saves.add(DataProvider.saveStable(cache, state, assets.resolve("blockstates/" + id + ".json")));
                saves.add(DataProvider.saveStable(cache, item, assets.resolve("models/item/" + id + ".json")));
            }
            if (server) {
                saves.add(DataProvider.saveStable(cache, SalvageLootTable.singleItem(BuiltInRegistries.BLOCK.getKey(holder.get())), data.resolve("loot_table/blocks/" + id + ".json")));
            }
        }
        return CompletableFuture.allOf(saves.toArray(CompletableFuture[]::new));
    }
    @Override public String getName() { return "Industron Native Kinetic Machine Assets"; }
}
