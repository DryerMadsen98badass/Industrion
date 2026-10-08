package net.mads.industron.material.organism;

import net.mads.industron.material.*;
import net.mads.industron.material.chemistry.*;
import net.mads.industron.material.chemistry.integration.IndustrialMaterialReflectionAdapter;
import net.mads.industron.material.defenitions.*;
import net.mads.industron.material.organic.OrganicMaterial;
import net.mads.industron.material.plant.*;
import net.mads.industron.material.structure.StructureMaterial;
import net.mads.industron.fluid.IndustrialFluid;
import java.util.*;

/** Uses the existing exact chemistry resolver; this graph does not invent reaction capabilities. */
public final class OrganicCompositionGraph {
    private OrganicCompositionGraph() {}
    public static Map<String,MaterialSnapshot> snapshots() {
        var adapter=new IndustrialMaterialReflectionAdapter();
        Map<String,MaterialSnapshot> result=new LinkedHashMap<>(adapter.loadAll());
        Set<IndustrialSubstance> visited=Collections.newSetFromMap(new IdentityHashMap<>());
        for(var material:OrganicMaterials.BIOLOGICAL)add(material,adapter,result,visited);
        for(var material:AnimalMaterials.ALL)add(material,adapter,result,visited);
        for(var organism:Organisms.ALL)for(var part:organism.parts().values())add(part.material(),adapter,result,visited);
        for(var state:OrganicFractionPlanner.PLAN.intermediates())add(state.material(),adapter,result,visited);
        return Collections.unmodifiableMap(result);
    }
    private static void add(IndustrialSubstance material,IndustrialMaterialReflectionAdapter adapter,
                            Map<String,MaterialSnapshot> target,Set<IndustrialSubstance> visited) {
        if(!visited.add(material))return;
        // Adapt before traversing children so graph cycles are left for CompositionResolver to reject.
        target.putIfAbsent(material.id(),adapter.adapt(material));
        for(var child:components(material))add(child.substance(),adapter,target,visited);
    }
    public static List<MaterialComponent> components(IndustrialSubstance material) {
        if(material instanceof BiologicalMaterial m)return m.components();
        if(material instanceof IndustrialMaterial m)return m.components();
        if(material instanceof IndustrialFluid m)return m.components();
        if(material instanceof OrganicMaterial m)return m.components();
        if(material instanceof PlantMaterial m)return m.components();
        if(material instanceof PlantDerivedSubstance m)return m.components();
        if(material instanceof PlantProcessIntermediate m)return m.components();
        if(material instanceof StructureMaterial m)return m.components();
        if(material instanceof ElementDefinition)return List.of();
        throw new IllegalArgumentException("Unsupported .contains identity: "+material.getClass().getName());
    }
}
