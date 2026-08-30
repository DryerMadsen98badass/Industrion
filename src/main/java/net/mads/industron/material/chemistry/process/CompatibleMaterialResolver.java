package net.mads.industron.material.chemistry.process;

import net.mads.industron.material.chemistry.ChemistryPhase;
import net.mads.industron.material.chemistry.MaterialAnalysis;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class CompatibleMaterialResolver {
    public List<MaterialAnalysis> resolve(ProcessRequirement requirement,Map<String,MaterialAnalysis> materials){
        return materials.values().stream()
                .filter(m->requirement.requiredPhase()==ChemistryPhase.UNKNOWN||m.phase()==requirement.requiredPhase())
                .filter(m->requirement.constraints().stream().allMatch(c->c.matches(m.properties().get(c.property()))))
                .sorted(Comparator.comparing(a->a.source().id()))
                .toList();
    }
}
