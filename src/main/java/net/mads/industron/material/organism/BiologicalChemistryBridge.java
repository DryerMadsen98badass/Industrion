package net.mads.industron.material.organism;

import net.mads.industron.material.*;
import net.mads.industron.material.defenitions.*;
import net.mads.industron.material.organic.OrganicMaterial;
import net.mads.industron.fluid.IndustrialFluid;
import net.mads.industron.fluid.IndustrialFluids;
import net.mads.industron.material.plant.*;
import net.mads.industron.material.structure.StructureMaterial;
import net.mads.industron.material.structure.StructureMaterials;
import net.mads.industron.material.chemistry.*;
import net.mads.industron.material.chemistry.process.ProcessIntermediateMaterials;
import java.util.*;

/** Registers process-feed adapters, not duplicate items or a second chemical reaction engine. */
public final class BiologicalChemistryBridge {
    private static boolean initialized;
    private static final Map<String,IndustrialMaterial> ADAPTERS=new LinkedHashMap<>();
    private record Feed(String item, MaterialPart part) {}
    private BiologicalChemistryBridge() {}
    public static synchronized void initialize() {
        if(initialized)return;
        // Force the original static registry to finish before touching biological definitions.
        Map<String,IndustrialMaterial> known=new LinkedHashMap<>();
        for(var material:IndustrialMaterials.ALL)known.put(material.id(),material);
        Map<String,Feed> feeds=new LinkedHashMap<>();
        Map<String,IndustrialSubstance> roots=new LinkedHashMap<>();
        for(var item:BiologicalItemCatalog.ALL) {
            MaterialPart part;
            if(item.form()==BiologicalItemCatalog.Form.POWDER)part=MaterialPart.DUST;
            else if(item.form()==BiologicalItemCatalog.Form.COMPOUND
                    && (OrganicMaterials.BIOLOGICAL.contains(item.material())
                        || item.material().roles().contains(OrganicRole.FAT)))part=MaterialPart.BIOLOGICAL_FEED;
            else continue; // Wet tissue/pulp must follow physical preparation first.
            feeds.putIfAbsent(item.material().id(),new Feed("industron:"+item.id(),part));
            roots.putIfAbsent(item.material().id(),item.material());
        }
        for(var item:OrganismItemCatalog.ALL) {
            if(item.form()==OrganicForm.DUST || (item.form()==OrganicForm.RAW && item.part()==OrganismPart.POWDER)) {
                feeds.putIfAbsent(item.material().id(),new Feed(item.itemId(),MaterialPart.DUST));
                roots.putIfAbsent(item.material().id(),item.material());
            }
        }
        // Shared physical aliases have one canonical chemistry identity, too.
        Map<String,String> identities=new HashMap<>();
        roots.entrySet().removeIf(entry->{
            String feed=feeds.get(entry.getKey()).item();
            return identities.putIfAbsent(feed,entry.getKey())!=null;
        });
        Set<String> active=new HashSet<>();
        for(var root:roots.values())register(root,feeds,known,active);
        // Same registry-time preview, catalysts and representability checks as industrial compounds.
        // Existing automatic intermediates are ignored by the preview; it is safe to extend once.
        ProcessIntermediateMaterials.generate(List.copyOf(IndustrialMaterials.MATERIALS));
        initialized=true;
    }
    private static List<MaterialComponent> composition(IndustrialSubstance source) {
        if(source instanceof BiologicalMaterial value)return value.components();
        if(source instanceof OrganicMaterial value)return value.components();
        if(source instanceof IndustrialMaterial value)return value.components();
        if(source instanceof IndustrialFluid value)return value.components();
        if(source instanceof PlantMaterial value)return value.components();
        if(source instanceof PlantDerivedSubstance value)return value.components();
        if(source instanceof PlantProcessIntermediate value)return value.components();
        if(source instanceof StructureMaterial value)return value.components();
        throw new IllegalArgumentException("Missing chemistry definition: "+source.id());
    }
    private static void requirePlantIntermediate(PlantProcessIntermediate state) {
        if(!PlantMaterials.ALL.contains(state.parent())
                || !PlantProcessingPlanner.requiredIntermediates(state.parent()).contains(state))
            throw new IllegalArgumentException("Biological composition references an unneeded plant intermediate: "+state.id());
    }
    private static Feed plantFeed(IndustrialSubstance source) {
        if(source instanceof PlantProcessIntermediate state) {
            requirePlantIntermediate(state);
            return new Feed(state.registryId().toString(),MaterialPart.BIOLOGICAL_FEED);
        }
        if(source instanceof PlantDerivedSubstance derived) {
            var parent=derived.parent();
            if(!PlantMaterials.ALL.contains(parent))throw new IllegalArgumentException("Unregistered plant: "+parent.id());
            if(!signature(derived.components()).equals(signature(PlantMaterialGenerator.componentsFor(parent,derived.part()))))
                throw new IllegalArgumentException("Plant form composition disagrees with its owning definition: "+derived.id());
            String item;
            if(parent.hasExistingPart(derived.part()))item=parent.existingPart(derived.part()).toString();
            else if(PlantMaterialGenerator.generates(parent,derived.part()))item="industron:"+derived.part().registryName(parent);
            else throw new IllegalArgumentException("Plant does not own form: "+derived.id());
            return new Feed(item,MaterialPart.BIOLOGICAL_FEED);
        }
        if(source instanceof PlantMaterial plant) {
            if(!PlantMaterials.ALL.contains(plant))throw new IllegalArgumentException("Unregistered plant: "+plant.id());
            // Stable enum precedence, and only one-unit raw forms: never alias a compressed bale as one unit.
            for(var part:PlantPart.values())if(part.biomassSource() && part!=PlantPart.COMPRESSED_BLOCK
                    && part!=PlantPart.BALE && plant.hasExistingPart(part))
                return new Feed(plant.existingPart(part).toString(),MaterialPart.BIOLOGICAL_FEED);
            throw new IllegalArgumentException("Plant has no owned one-unit process feed: "+plant.id());
        }
        return null;
    }
    private static String signature(List<MaterialComponent> composition) {
        if(composition.isEmpty())return "";
        int gcd=0;
        for(var c:composition){int n=c.amount();while(n!=0){int r=gcd%n;gcd=n;n=r;}}
        final int divisor=gcd;
        return composition.stream().map(c->c.substance().id()+"="+c.amount()/divisor).sorted().reduce("",(a,b)->a+";"+b);
    }
    public static Map<String,IndustrialMaterial> adapters() {return Collections.unmodifiableMap(ADAPTERS);}
    private static IndustrialSubstance register(IndustrialSubstance source,Map<String,Feed> feeds,
                                               Map<String,IndustrialMaterial> known,Set<String> active) {
        var existing=known.get(source.id());
        if(existing!=null) {
            List<MaterialComponent> declared=source instanceof ElementDefinition ? null : composition(source);
            // Gem/metal structure wrappers can intentionally expose their backing material under its ID.
            if(source instanceof StructureMaterial && declared.size()==1
                    && declared.get(0).substance()==existing)return existing;
            if(declared!=null && !signature(declared).equals(signature(existing.components())))
                throw new IllegalArgumentException("Conflicting biological chemistry identity: "+source.id());
            return existing;
        }
        if(!active.add(source.id()))throw new IllegalArgumentException("Cyclic biological chemistry: "+source.id());
        try {
            List<MaterialComponent> composition=composition(source);
            if(composition.isEmpty())throw new IllegalArgumentException("Referenced substance needs .contains: "+source.id());
            if(source instanceof StructureMaterial && StructureMaterials.ALL.stream().noneMatch(m->m==source))
                throw new IllegalArgumentException("Unregistered structure in biological composition: "+source.id());
            List<MaterialComponent> adapted=new ArrayList<>();
            for(var child:composition) {
                var material=register(child.substance(),feeds,known,active);
                adapted.add(new MaterialComponent(material,child.amount()));
            }
            // Structure families already own their registry and physical forms; keep that backing identity.
            if(source instanceof StructureMaterial)return source;
            var builder=new IndustrialMaterialBuilder(source.id(),source.displayName(),source.color(),MaterialContentProfile.BIOLOGICAL)
                    .contains(adapted.toArray(MaterialComponent[]::new))
                    .source(MaterialSourceType.BIOLOGICAL_EXTRACTION,"biological_"+source.id());
            if(source instanceof PlantProcessIntermediate state && !state.isSolid()) {
                requirePlantIntermediate(state);
                if(state.phase()!=ChemistryPhase.LIQUID && state.phase()!=ChemistryPhase.GAS)
                    throw new IllegalArgumentException("Plant fluid has no matching owned phase: "+state.id());
                MaterialPart part=state.isGas()?MaterialPart.GAS:MaterialPart.LIQUID;
                builder.phase(state.phase()).parts(part).existing(part,state.registryId());
            } else if(source instanceof IndustrialFluid fluid) {
                MaterialPart part=fluid.isGas()?MaterialPart.GAS:fluid.isMolten()?MaterialPart.MOLTEN_FLUID:MaterialPart.LIQUID;
                builder.phase(fluid.isGas()?ChemistryPhase.GAS:fluid.isMolten()?ChemistryPhase.MOLTEN:ChemistryPhase.LIQUID).parts(part);
                if(fluid.hasExistingFluid())builder.existing(part,fluid.existingFluidId());
                else if(IndustrialFluids.ALL.contains(fluid))builder.existing(part,"industron:"+fluid.registryName());
            } else {
                Feed feed=feeds.get(source.id());
                if(feed==null)feed=plantFeed(source);
                MaterialPart part=feed==null?MaterialPart.DUST:feed.part();
                builder.phase(ChemistryPhase.SOLID).parts(part);
                if(feed!=null)builder.existing(part,feed.item());
                boolean physicalFraction=OrganicFractionPlanner.PLAN.intermediates().stream()
                        .anyMatch(state->state.material()==source);
                if(physicalFraction)builder.structure(ChemicalStructure.physicalMixture())
                        .classification(MaterialClassification.PHYSICAL_MIXTURE);
                if(!physicalFraction && source instanceof BiologicalMaterial biological && biological.roles().contains(OrganicRole.PROTEIN)
                        && composition.stream().noneMatch(c->c.substance() instanceof BiologicalMaterial))
                    builder.classification(MaterialClassification.POLYMER);
            }
            var material=builder.build();known.put(material.id(),material);ADAPTERS.put(material.id(),material);
            return material;
        } finally {active.remove(source.id());}
    }
}
