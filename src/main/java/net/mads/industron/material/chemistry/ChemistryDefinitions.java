package net.mads.industron.material.chemistry;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class ChemistryDefinitions {
    private static final Map<String,ChemistryDefinition> DEFINITIONS=new LinkedHashMap<>();
    private ChemistryDefinitions(){}
    public static synchronized void register(ChemistryDefinition definition){
        ChemistryDefinition previous=DEFINITIONS.putIfAbsent(definition.materialId(),definition);
        if(previous!=null)throw new IllegalStateException("Duplicate chemistry definition for "+definition.materialId());
    }
    public static synchronized Optional<ChemistryDefinition> find(String materialId){return Optional.ofNullable(DEFINITIONS.get(materialId));}
    public static synchronized Collection<ChemistryDefinition> all(){return java.util.List.copyOf(DEFINITIONS.values());}
    public static MaterialSnapshot apply(MaterialSnapshot source){
        ChemistryDefinition d=DEFINITIONS.get(source.id());if(d==null)return source;
        java.util.Map<String,Double> properties=new java.util.LinkedHashMap<>(source.properties());properties.putAll(d.propertyOverrides());
        java.util.Set<MaterialClassification> classes=new java.util.LinkedHashSet<>(source.classifications());classes.addAll(d.classifications());
        java.util.Set<MaterialSource> sources=new java.util.LinkedHashSet<>(source.sources());sources.addAll(d.sources());
        return new MaterialSnapshot(source.id(),source.displayName(),source.color(),d.phase().orElse(source.phase()),source.composition(),d.structure().isPresent()?d.structure():source.structure(),properties,classes,source.tierIndex(),source.tierName(),sources,source.backingMaterial());
    }
}
