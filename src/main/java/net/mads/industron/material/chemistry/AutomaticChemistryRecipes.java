package net.mads.industron.material.chemistry;

import net.mads.industron.material.chemistry.integration.ReflectiveRecipeEmitter;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeOutput;

public final class AutomaticChemistryRecipes {
    private AutomaticChemistryRecipes(){}
    public static void build(RecipeOutput output,HolderLookup.Provider holderLookup){
        ChemistryBootstrap.AnalysisResult analysis=ChemistryBootstrap.currentOrAnalyze();
        ReflectiveRecipeEmitter.Result emitted=new ReflectiveRecipeEmitter().emit(output,analysis.plans(),analysis.analyses());
        if(!emitted.diagnostics().isEmpty()){
            var merged=new java.util.ArrayList<>(analysis.diagnostics());merged.addAll(emitted.diagnostics());
            net.mads.industron.material.chemistry.process.GeneratedProcessRegistry.replace(analysis.plans(),merged);
        }
        System.out.println("[Industron Chemistry] Emitted "+emitted.emitted()+" concrete recipes; "+emitted.diagnostics().size()+" plans require adapter/material attention.");
    }
}
