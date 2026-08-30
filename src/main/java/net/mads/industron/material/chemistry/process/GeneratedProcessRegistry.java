package net.mads.industron.material.chemistry.process;

import net.mads.industron.material.chemistry.ChemistryDiagnostic;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GeneratedProcessRegistry {
    private static volatile List<ProcessPlan> plans=List.of();
    private static volatile List<ChemistryDiagnostic> diagnostics=List.of();
    private GeneratedProcessRegistry(){}
    public static synchronized void replace(List<ProcessPlan> next,List<ChemistryDiagnostic> nextDiagnostics){
        plans=List.copyOf(next);diagnostics=List.copyOf(nextDiagnostics);
    }
    public static List<ProcessPlan> plans(){return plans;}
    public static List<ChemistryDiagnostic> diagnostics(){return diagnostics;}
}
