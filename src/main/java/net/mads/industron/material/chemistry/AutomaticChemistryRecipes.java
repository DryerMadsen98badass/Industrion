package net.mads.industron.material.chemistry;

import net.mads.industron.material.chemistry.integration.ReflectiveRecipeEmitter;
import net.mads.industron.material.chemistry.process.GeneratedProcessRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeOutput;

import java.util.ArrayList;

public final class AutomaticChemistryRecipes {
    private AutomaticChemistryRecipes() {
    }

    public static void build(RecipeOutput output, HolderLookup.Provider holderLookup) {
        ChemistryBootstrap.AnalysisResult analysis = ChemistryBootstrap.currentOrAnalyze();
        ReflectiveRecipeEmitter.Result emitted = new ReflectiveRecipeEmitter()
                .emit(output, analysis.plans(), analysis.analyses());

        var merged = new ArrayList<>(analysis.diagnostics());
        merged.addAll(emitted.diagnostics());
        var publishedPlans = analysis.plans().stream()
                .filter(plan -> emitted.emittedPlanIds().contains(plan.id()))
                .toList();
        GeneratedProcessRegistry.replace(publishedPlans, merged);

        // runData initially writes planner reports before recipe providers execute. Rewrite them
        // here with the exact plans that survived emission preflight so build/reports never claims
        // that a blocked/unsaved automatic chain exists.
        ChemistryBootstrap.writeReports(new ChemistryBootstrap.AnalysisResult(
                analysis.analyses(),
                publishedPlans,
                merged,
                analysis.geology(),
                analysis.oreSources()
        ));

        ChemistryBootstrap.writeBiologicalEmissionReport(publishedPlans,merged);

        System.out.println(
                "[Industron Chemistry] Emitted " + emitted.emitted() + " concrete recipes from "
                        + emitted.emittedPlanIds().size() + " complete plans; "
                        + emitted.diagnostics().size() + " emission diagnostics."
        );
    }
}
