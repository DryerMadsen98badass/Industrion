package net.mads.industron.material.chemistry;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ChemistryDefinition {
    private final String materialId;
    private final Optional<ChemicalStructure> structure;
    private final Optional<ChemistryPhase> phase;
    private final Set<MaterialClassification> classifications;
    private final Set<MaterialSource> sources;
    private final Map<String, Double> propertyOverrides;

    private ChemistryDefinition(Builder b) {
        materialId = b.materialId;
        structure = Optional.ofNullable(b.structure);
        phase = Optional.ofNullable(b.phase);
        classifications = Set.copyOf(b.classifications);
        sources = Set.copyOf(b.sources);
        propertyOverrides = Map.copyOf(b.propertyOverrides);
    }
    public String materialId(){return materialId;}
    public Optional<ChemicalStructure> structure(){return structure;}
    public Optional<ChemistryPhase> phase(){return phase;}
    public Set<MaterialClassification> classifications(){return classifications;}
    public Set<MaterialSource> sources(){return sources;}
    public Map<String,Double> propertyOverrides(){return propertyOverrides;}
    public static Builder material(String id){return new Builder(id);}
    public static final class Builder {
        private final String materialId;
        private ChemicalStructure structure;
        private ChemistryPhase phase;
        private final Set<MaterialClassification> classifications=EnumSet.noneOf(MaterialClassification.class);
        private final Set<MaterialSource> sources=new LinkedHashSet<>();
        private final Map<String,Double> propertyOverrides=new LinkedHashMap<>();
        private Builder(String id){materialId=id.trim().toLowerCase(java.util.Locale.ROOT);if(materialId.isEmpty())throw new IllegalArgumentException("blank material id");}
        public Builder structure(ChemicalStructure v){structure=v;return this;}
        public Builder phase(ChemistryPhase v){phase=v;return this;}
        public Builder classification(MaterialClassification... values){classifications.addAll(java.util.List.of(values));return this;}
        public Builder source(MaterialSourceType type,String id){sources.add(new MaterialSource(type,id));return this;}
        public Builder property(String id,double value){propertyOverrides.put(id.trim().toLowerCase(java.util.Locale.ROOT),value);return this;}
        public ChemistryDefinition build(){ChemistryDefinition d=new ChemistryDefinition(this);ChemistryDefinitions.register(d);return d;}
    }
}
