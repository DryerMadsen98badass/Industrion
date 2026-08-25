package net.mads.industron.data;

import com.google.gson.JsonObject;
import net.mads.industron.Industron;
import net.mads.industron.registry.FluidRegistry;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class FluidBucketModelProvider implements DataProvider {
    private final PackOutput output;

    public FluidBucketModelProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        Path modelFolder = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK)
                .resolve("assets")
                .resolve(Industron.MOD_ID)
                .resolve("models")
                .resolve("item");

        for (FluidRegistry.RegisteredFluid fluid : FluidRegistry.allFluids()) {
            JsonObject json = new JsonObject();
            json.addProperty("parent", "neoforge:item/bucket_drip");
            json.addProperty("loader", "neoforge:fluid_container");
            json.addProperty("fluid", ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, fluid.definition().registryName()).toString());
            json.addProperty("flip_gas", fluid.definition().isGas());
            json.addProperty("apply_fluid_luminosity", fluid.definition().lightLevel() > 0);

            futures.add(DataProvider.saveStable(cache, json, modelFolder.resolve(fluid.definition().bucketName() + ".json")));
        }

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Industron Fluid Bucket Models";
    }
}
