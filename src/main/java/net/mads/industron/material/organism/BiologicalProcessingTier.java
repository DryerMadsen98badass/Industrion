package net.mads.industron.material.organism;

import net.mads.industron.machine.MachineTier;
import net.mads.industron.material.*;
import net.mads.industron.material.organic.OrganicMaterial;
import net.mads.industron.material.plant.PlantMaterial;
import net.mads.industron.material.structure.StructureMaterial;
import java.util.*;

/** Conservative mechanical gate: never lower than any explicitly declared component tier. */
public final class BiologicalProcessingTier {
    private BiologicalProcessingTier() {}
    public static MachineTier of(IndustrialSubstance material) {
        return resolve(material,Collections.newSetFromMap(new IdentityHashMap<>()));
    }
    private static MachineTier resolve(IndustrialSubstance material, Set<IndustrialSubstance> active) {
        if (!active.add(material)) throw new IllegalArgumentException("Cyclic organic composition: " + material.id());
        try {
            if (material instanceof ElementDefinition element) return element.tier();
            if (material instanceof IndustrialMaterial industrial) return industrial.tier();
            if (material instanceof StructureMaterial structure) return structure.tier();
            List<MaterialComponent> components;
            if (material instanceof BiologicalMaterial biological) components=biological.components();
            else if (material instanceof OrganicMaterial organic) components=organic.components();
            else if (material instanceof net.mads.industron.fluid.IndustrialFluid fluid) components=fluid.components();
            else if (material instanceof PlantMaterial plant) components=plant.components();
            else if (material instanceof net.mads.industron.material.plant.PlantDerivedSubstance plant) components=plant.components();
            else if (material instanceof net.mads.industron.material.plant.PlantProcessIntermediate plant) components=plant.components();
            else throw new IllegalArgumentException("No organic processing tier adapter for " + material.getClass().getName());
            if (components.isEmpty()) throw new IllegalArgumentException("Empty composition: " + material.id());
            int maximum=0;
            for (MaterialComponent component : components) {
                int index=MachineTier.ALL.indexOf(resolve(component.substance(),active));
                if (index<0) throw new IllegalArgumentException("Non-progression component tier");
                maximum=Math.max(maximum,index);
            }
            return MachineTier.ALL.get(maximum);
        } finally { active.remove(material); }
    }
}
