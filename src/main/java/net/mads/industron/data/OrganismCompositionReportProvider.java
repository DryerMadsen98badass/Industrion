package net.mads.industron.data;

import com.google.gson.JsonObject;
import net.mads.industron.material.chemistry.CompositionResolver;
import net.mads.industron.material.organism.*;
import net.minecraft.data.*;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/** Makes every generated/existing part's full conserved composition reviewable after runData. */
public final class OrganismCompositionReportProvider implements DataProvider {
    private final Path target;
    public OrganismCompositionReportProvider(PackOutput output) {
        target=output.getOutputFolder().resolve("reports/industron/organism_compositions.json");
    }
    @Override public CompletableFuture<?> run(CachedOutput output) {
        try {
            var graph=OrganicCompositionGraph.snapshots();
            var resolver=new CompositionResolver(graph);
            JsonObject report=new JsonObject();
            for(var entry:OrganismItemCatalog.ALL) {
                if(report.has(entry.itemId()))continue;
                JsonObject item=new JsonObject(),direct=new JsonObject(),elements=new JsonObject();
                entry.material().components().forEach(c->direct.addProperty(c.substance().id(),c.amount()));
                var atoms=resolver.atomic(entry.material().id()).orElseThrow(()->
                        new IllegalStateException("Unresolved element composition: "+entry.itemId()));
                atoms.entries().forEach((id,amount)->elements.addProperty(id,amount.signature()));
                item.add("contains",direct);item.add("conserved_element_fractions",elements);
                item.addProperty("form",entry.form().name());report.add(entry.itemId(),item);
            }
            return DataProvider.saveStable(output,report,target);
        } catch(Exception e) {return CompletableFuture.failedFuture(e);}
    }
    @Override public String getName(){return "Industron organic composition validation and report";}
}
