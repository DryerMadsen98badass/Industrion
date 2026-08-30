package net.mads.industron.material.chemistry.process;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ProcessPlan {
    private final String targetMaterialId;
    private final List<ProcessStep> steps;
    private final Map<String,List<String>> dependencies;

    public ProcessPlan(String targetMaterialId,List<ProcessStep> steps){
        this.targetMaterialId=Objects.requireNonNull(targetMaterialId);
        this.steps=List.copyOf(steps);
        Map<String,List<String>> deps=new LinkedHashMap<>();
        for(ProcessStep step:this.steps){
            step.requireBalanced();
            deps.put(step.id(),step.inputs().stream().map(ProcessMaterial::materialId).distinct().toList());
        }
        this.dependencies=Collections.unmodifiableMap(deps);
    }
    public String targetMaterialId(){return targetMaterialId;}
    public List<ProcessStep> steps(){return steps;}
    public Map<String,List<String>> dependencies(){return dependencies;}
    public static Builder builder(String target){return new Builder(target);}
    public static final class Builder{
        private final String target;private final List<ProcessStep> steps=new ArrayList<>();
        private Builder(String target){this.target=target;}
        public Builder step(ProcessStep step){steps.add(step);return this;}
        public ProcessPlan build(){return new ProcessPlan(target,steps);}
    }
}
