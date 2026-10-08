package net.mads.industron.material.chemistry.process;

import net.mads.industron.material.AutomaticProcessIntermediate;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.material.MaterialComponent;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.chemistry.ChemicalStructure;
import net.mads.industron.material.defenitions.IndustrialMaterials;
import net.mads.industron.material.chemistry.ChemistryEngine;
import net.mads.industron.material.chemistry.MaterialAnalysis;
import net.mads.industron.material.chemistry.MaterialClassification;
import net.mads.industron.material.chemistry.MaterialSnapshot;
import net.mads.industron.material.chemistry.MaterialSourceType;
import net.mads.industron.material.chemistry.integration.IndustrialMaterialReflectionAdapter;
import net.mads.industron.material.structure.StructureMaterials;
import net.mads.industron.material.structure.WoodMaterial;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Compiles only registry-backed intermediates that a deterministic processing route actually uses.
 * Nothing is generated from a material merely because a possible suffix exists: slurry, solution,
 * pyrolysate, and later rich/dirty/fraction states are on-demand graph nodes.
 */
public final class ProcessIntermediateMaterials {
    private ProcessIntermediateMaterials() {
    }

    public static void generate(List<IndustrialMaterial> registeredMaterials) {
        List<IndustrialMaterial> parents = List.copyOf(registeredMaterials);
        IndustrialMaterialReflectionAdapter adapter = new IndustrialMaterialReflectionAdapter();
        Map<String, MaterialSnapshot> snapshots = new LinkedHashMap<>();
        for (IndustrialMaterial element : IndustrialMaterials.ELEMENTS) {
            MaterialSnapshot snapshot = adapter.adapt(element);
            snapshots.put(snapshot.id(), snapshot);
        }
        for (IndustrialMaterial parent : parents) {
            MaterialSnapshot snapshot = adapter.adapt(parent);
            snapshots.put(snapshot.id(), snapshot);
        }
        // Keep registry-time route preview on the same composition universe as ChemistryBootstrap.
        // .contains(...) may legally reference any structure definition, including wood and stone.
        for (var structure : StructureMaterials.ALL) {
            if (snapshots.containsKey(structure.id())) continue;
            MaterialSnapshot snapshot = adapter.adapt(structure);
            snapshots.put(snapshot.id(), snapshot);
        }

        ChemistryEngine engine = new ChemistryEngine();
        Map<String, MaterialAnalysis> analyses = new LinkedHashMap<>();
        for (MaterialSnapshot snapshot : snapshots.values()) {
            analyses.put(snapshot.id(), engine.analyze(snapshot, snapshots));
        }

        FictionalReagentResolver reagentResolver = new FictionalReagentResolver();
        Set<String> generatedIds = new LinkedHashSet<>(snapshots.keySet());

        // Existing generic compound-DUST intermediates remain route-selected and on-demand.
        for (IndustrialMaterial parent : parents) {
            if (!ProcessPlanner.isAutomaticDustProcessingTarget(parent)
                    || ProcessPlanner.automaticProcessingInputPart(parent) == null
                    || parent.components().isEmpty()) continue;
            MaterialAnalysis analysis = analyses.get(parent.id());
            if (analysis == null || AutomaticProcessIntermediate.isAutomatic(analysis.source())) continue;

            ProcessPlanner.CompositeRoutePreview preview = ProcessPlanner
                    .previewCompositeDustRoute(analysis, analyses)
                    .orElse(null);
            if (preview == null) continue;
            ProcessChainProfile profile = preview.profile();
            if (!profile.enabled()) continue;
            int processingTier = ProcessChainProfile.processingTierIndex(analysis, preview.decision().route());

            String catalystId = null;
            if (ProcessPlanner.compositeRouteRequiresCatalyst(analysis, preview)) {
                catalystId = reagentResolver.resolveCatalyst(analysis, analyses, processingTier).orElse(null);
                if (catalystId == null) continue;
            }

            String activatorId = null;
            if (profile.requiresActivator()) {
                activatorId = reagentResolver.resolveActivator(
                        analysis,
                        analyses,
                        catalystId == null ? Set.of() : Set.of(catalystId),
                        processingTier
                ).orElse(null);
                if (activatorId == null) continue;
            }

            if (ProcessPlanner.compositeRouteRepresentabilityProblem(
                    preview, catalystId, activatorId, analyses).isPresent()) {
                continue;
            }

            for (ProcessChainProfile.Stage stage : profile.stages()) {
                register(parent, WoodProcessingPlanner.kindForStage(stage), generatedIds);
            }
        }

        // Wood uses WOOD_PULP as its chemistry feed. The same preview used later for recipe planning
        // decides exactly which graph nodes are reachable. For example, Oak Slurry does not exist
        // unless the calculated Oak route actually contains a slurry stage.
        for (var structure : StructureMaterials.ALL) {
            if (!(structure instanceof WoodMaterial wood)
                    || wood.components().isEmpty()
                    || !wood.generatedForms().contains(MaterialPart.WOOD_PULP)) continue;
            MaterialAnalysis analysis = analyses.get(wood.id());
            if (analysis == null) continue;

            WoodProcessingPlanner.Preview preview = WoodProcessingPlanner.preview(wood, analysis, analyses);
            for (AutomaticProcessIntermediate.Kind kind : preview.requiredIntermediates()) {
                register(wood, kind, generatedIds);
            }
        }
    }

    private static void register(
            IndustrialSubstance parent,
            AutomaticProcessIntermediate.Kind kind,
            Set<String> generatedIds
    ) {
        String id = kind.materialId(parent);
        if (!generatedIds.add(id)) return;

        MaterialComponent[] composition = components(parent);
        if (composition.length == 0) return;

        var builder = IndustrialMaterials.material(id, kind.displayName(parent), parent.color())
                .contains(composition)
                .phase(kind.phase())
                .source(MaterialSourceType.PROCESS_OUTPUT, kind.sourceId(parent))
                .parts(kind.part());

        if (kind.physicalMixture()) {
            builder.structure(ChemicalStructure.physicalMixture())
                    .classification(MaterialClassification.PHYSICAL_MIXTURE);
        }
        builder.build();
    }

    private static MaterialComponent[] components(IndustrialSubstance parent) {
        if (parent instanceof IndustrialMaterial material) {
            return material.components().toArray(MaterialComponent[]::new);
        }
        if (parent instanceof WoodMaterial wood) {
            return wood.components().toArray(MaterialComponent[]::new);
        }
        return new MaterialComponent[0];
    }
}
