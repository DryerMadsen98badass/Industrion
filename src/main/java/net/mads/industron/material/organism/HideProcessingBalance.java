package net.mads.industron.material.organism;

import net.mads.industron.material.MaterialComponent;
import net.mads.industron.material.defenitions.AnimalMaterials;
import java.util.*;

/** Scraping/washing removes hair and moisture; retained collagen and fat form oil-tanned hide. */
public final class HideProcessingBalance {
    private HideProcessingBalance() {}
    public record Batch(int hides, int oiledHides, int fiber, int waterUnits) {}
    public static Optional<Batch> tryOf(BiologicalMaterial source) {
        try {
            return Optional.of(of(source));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
    public static Batch of(BiologicalMaterial source) {
        var components=source.components();
        // A single-component tissue wrapper preserves the common tissue's unit identity.
        Set<BiologicalMaterial> seen=Collections.newSetFromMap(new IdentityHashMap<>());
        seen.add(source);
        while(components.size()==1 && components.get(0).substance() instanceof BiologicalMaterial child) {
            if(!seen.add(child))throw new IllegalArgumentException("Cyclic hide composition");
            components=child.components();
        }
        var partition=OrganicFractionBalance.partition(components,c->
                c.substance()==AnimalMaterials.WATER || (c.substance() instanceof BiologicalMaterial m && m.roles().contains(OrganicRole.FIBER)));
        if(!signature(partition.residue().composition()).equals(signature(AnimalMaterials.LEATHER.components())))
            throw new IllegalArgumentException("Hide residue does not match leather composition: "+source.id());
        // Selected composition is normalized independently; recover counts from original amounts.
        int sum=components.stream().mapToInt(MaterialComponent::amount).sum();
        int divisor=sum/partition.inputUnits();
        int fiber=0,water=0;
        for(var component:components) {
            if(component.substance()==AnimalMaterials.WATER)water+=component.amount()/divisor;
            else if(component.substance() instanceof BiologicalMaterial m && m.roles().contains(OrganicRole.FIBER)) {
                if(!m.id().equals("fur_fiber"))throw new IllegalArgumentException("Unsupported hide fiber identity: "+m.id());
                fiber+=component.amount()/divisor;
            }
        }
        return new Batch(partition.inputUnits(),partition.residue().units(),fiber,water);
    }
    private static Map<String,Integer> signature(List<MaterialComponent> composition) {
        int gcd=0;
        for(var c:composition) {int b=c.amount();while(b!=0){int r=gcd%b;gcd=b;b=r;}}
        Map<String,Integer> values=new TreeMap<>();
        for(var c:composition)values.put(c.substance().id(),c.amount()/gcd);
        return values;
    }
}
