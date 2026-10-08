package net.mads.industron.material.organism;

import net.mads.industron.material.MaterialComponent;
import net.mads.industron.material.defenitions.AnimalMaterials;
import java.util.*;

/** Physical tissue fractionation only. Bound proteins/minerals are never split into atoms here. */
public final class OrganicFractionPlanner {
    public enum Process { CUTTING, CENTRIFUGING, DRYING }
    public record Product(String itemOrFluid, int units, boolean fluid) {}
    public record Step(String id, Process process, String input, int inputUnits,
                       BiologicalMaterial material, List<Product> outputs) {
        public Step {
            outputs=List.copyOf(outputs);
            if(inputUnits<1 || outputs.stream().anyMatch(p->p.units()<1)
                    || outputs.stream().mapToInt(Product::units).sum()!=inputUnits)
                throw new IllegalArgumentException("Non-conserving organic step: "+id);
        }
    }
    public record Intermediate(String id,String name,BiologicalMaterial material, boolean powdered) {}
    public record Plan(List<Intermediate> intermediates,List<Step> steps) {
        public Plan {
            intermediates=List.copyOf(intermediates);steps=List.copyOf(steps);
            Set<String> ids=new HashSet<>();
            for(var intermediate:intermediates)if(!ids.add(intermediate.id()))
                throw new IllegalArgumentException("Duplicate biological intermediate: "+intermediate.id());
            ids.clear();
            for(var step:steps)if(!ids.add(step.id()))
                throw new IllegalArgumentException("Duplicate biological recipe: "+step.id());
        }
    }
    public static final Plan PLAN=plan(feeds());
    private OrganicFractionPlanner() {}

    /** A physical item entering tissue preparation. Useful for additional definition families. */
    public record Feed(String itemId, BiologicalMaterial material) {
        public Feed {
            if(itemId==null || !itemId.matches("[a-z0-9_.-]+:[a-z0-9/._-]+"))
                throw new IllegalArgumentException("Invalid biological feed item: "+itemId);
            Objects.requireNonNull(material);
        }
    }

    private static List<Feed> feeds() {
        List<Feed> feeds=new ArrayList<>();
        for(var entry:OrganismItemCatalog.ALL) {
            if(entry.part()==OrganismPart.HIDE)continue;
            if(entry.form()==OrganicForm.RAW || entry.form()==OrganicForm.ROTTEN || entry.form()==OrganicForm.COOKED)
                feeds.add(new Feed(entry.itemId(),entry.material()));
        }
        // Shared flesh is a real registered tissue, too. Append it so existing recipe/item IDs
        // remain stable and identical tissue can reuse a previously planned pulp chain.
        feeds.add(new Feed("industron:flesh",AnimalMaterials.FLESH));
        return List.copyOf(feeds);
    }

    public static Plan plan(List<Feed> feeds) {
        feeds=List.copyOf(feeds);
        List<Intermediate> intermediates=new ArrayList<>();
        List<Step> steps=new ArrayList<>();
        Map<String,String> preparations=new HashMap<>();
        Map<String,String> inputs=new HashMap<>();
        for(var entry:feeds) {
            var composition=unwrap(entry.material());
            String declared=signature(composition);
            String previous=inputs.putIfAbsent(entry.itemId(),declared);
            if(previous!=null) {
                if(!previous.equals(declared))throw new IllegalArgumentException("Conflicting feed composition: "+entry.itemId());
                continue;
            }
            boolean fat=composition.stream().anyMatch(OrganicFractionPlanner::fat);
            boolean water=composition.stream().anyMatch(c->c.substance()==AnimalMaterials.WATER);
            if((!fat && !water) || composition.size()<2)continue;
            String signature=signature(composition);
            String pulp=preparations.get(signature);
            if(pulp==null) {
                String stem="organic_"+entry.material().id();
                // Flesh is a usable, non-food material, not a creative-only duplicate of identical pulp.
                // Rotten/cooked inputs can recover this material but no route turns it into edible meat.
                boolean sharedFlesh=signature.equals(signature(AnimalMaterials.FLESH.components()));
                var prepared=sharedFlesh?AnimalMaterials.FLESH:new BiologicalMaterial(stem+"_pulp",
                        entry.material().displayName()+" Pulp",entry.material().color(),Set.of(),composition);
                pulp="industron:"+prepared.id();
                preparations.put(signature,pulp);
                if(!sharedFlesh)intermediates.add(new Intermediate(prepared.id(),prepared.displayName(),prepared,false));
                String current=pulp;
                var material=prepared;
                if(fat && composition.stream().anyMatch(c->!fat(c))) {
                    var partition=OrganicFractionBalance.partition(composition,OrganicFractionPlanner::fat);
                    Product selected=product(stem+"_fat_fraction",entry.material().displayName()+" Fat Fraction",
                            prepared.color(),partition.selected(),intermediates,false);
                    boolean wet=partition.residue().composition().stream()
                            .anyMatch(c->c.substance()==AnimalMaterials.WATER);
                    Product residue=product(stem+(wet?"_defatted_pulp":"_dry_fraction"),
                            entry.material().displayName()+(wet?" Defatted Pulp":" Dry Fraction"),
                            prepared.color(),partition.residue(),intermediates,!wet);
                    steps.add(new Step(stem+"/separate_fat",Process.CENTRIFUGING,current,partition.inputUnits(),material,List.of(selected,residue)));
                    current=residue.itemOrFluid();
                    composition=partition.residue().composition();
                    material=new BiologicalMaterial(stem+"_defatted",entry.material().displayName()+" Defatted Fraction",
                            prepared.color(),Set.of(),composition);
                }
                if(composition.size()>1 && composition.stream().anyMatch(c->c.substance()==AnimalMaterials.WATER)) {
                    var partition=OrganicFractionBalance.partition(composition,c->c.substance()==AnimalMaterials.WATER);
                    boolean protein=partition.residue().composition().stream().allMatch(c->
                            c.substance() instanceof BiologicalMaterial b && b.roles().contains(OrganicRole.PROTEIN));
                    Product dry=product(stem+(protein?"_protein_powder":"_dried_fraction"),
                            entry.material().displayName()+(protein?" Protein Powder":" Dried Fraction"),
                            prepared.color(),partition.residue(),intermediates,true);
                    steps.add(new Step(stem+"/recover_water",Process.DRYING,current,partition.inputUnits(),material,
                            List.of(dry,new Product("minecraft:water",partition.selected().units(),true))));
                }
            }
            if(entry.itemId().equals(pulp))continue; // Already prepared; never create flesh -> flesh.
            steps.add(new Step(entry.itemId().replace(':','/')+"/prepare",Process.CUTTING,
                    entry.itemId(),1,entry.material(),List.of(new Product(pulp,1,false))));
        }
        return new Plan(intermediates,steps);
    }
    private static Product product(String id,String name,int color,OrganicFractionBalance.Fraction fraction,List<Intermediate> intermediates,boolean powdered) {
        if(fraction.composition().size()==1) {
            var substance=fraction.composition().get(0).substance();
            if(substance==AnimalMaterials.WATER)return new Product("minecraft:water",fraction.units(),true);
            if(substance instanceof BiologicalMaterial material) {
                String known=constituentItem(material);
                if(known!=null)return new Product(known,fraction.units(),false);
            }
        }
        Set<OrganicRole> common=EnumSet.allOf(OrganicRole.class);
        for(var c:fraction.composition()) {
            if(c.substance() instanceof BiologicalMaterial b)common.retainAll(b.roles());
            else common.clear();
        }
        var material=new BiologicalMaterial(id,name,color,common,fraction.composition());
        intermediates.add(new Intermediate(id,name,material,powdered));
        return new Product("industron:"+id,fraction.units(),false);
    }
    public static String constituentItem(BiologicalMaterial material) {
        if(!net.mads.industron.material.defenitions.OrganicMaterials.BIOLOGICAL.contains(material))return null;
        var roles=material.roles();
        boolean dust=roles.contains(OrganicRole.PROTEIN)||roles.contains(OrganicRole.MINERAL)
                ||roles.contains(OrganicRole.PIGMENT)||roles.contains(OrganicRole.TOXIN);
        return "industron:"+material.id()+(dust?"_dust":"");
    }
    private static boolean fat(MaterialComponent c) {
        return c.substance() instanceof BiologicalMaterial material && material.roles().contains(OrganicRole.FAT);
    }
    private static List<MaterialComponent> unwrap(BiologicalMaterial material) {
        var composition=material.components();
        Set<BiologicalMaterial> seen=Collections.newSetFromMap(new IdentityHashMap<>());
        seen.add(material);
        while(composition.size()==1 && composition.get(0).substance() instanceof BiologicalMaterial child) {
            if(!seen.add(child))throw new IllegalArgumentException("Cyclic tissue: "+material.id());
            composition=child.components();
        }
        return composition;
    }
    private static String signature(List<MaterialComponent> composition) {
        int gcd=0;
        for(var c:composition){int n=c.amount();while(n!=0){int r=gcd%n;gcd=n;n=r;}}
        final int divisor=gcd;
        return composition.stream().map(c->c.substance().id()+"="+c.amount()/divisor).sorted().reduce("",(a,b)->a+";"+b);
    }
}
