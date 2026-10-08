package net.mads.industron.material.chemistry;

import net.mads.industron.machine.MachineTier;
import net.mads.industron.material.AutomaticProcessIntermediate;
import net.mads.industron.material.ElementDefinition;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.defenitions.StoneMaterials;
import net.mads.industron.material.defenitions.WoodMaterials;
import net.mads.industron.material.chemistry.process.ProcessChainProfile;
import net.mads.industron.material.chemistry.process.ProcessKind;
import net.mads.industron.material.chemistry.process.SeparationClassifier;
import net.mads.industron.material.chemistry.process.ProcessMaterial;
import net.mads.industron.material.chemistry.process.ProcessOperation;
import net.mads.industron.material.chemistry.process.ProcessSafetyValidator;
import net.mads.industron.material.chemistry.process.ProcessPlan;
import net.mads.industron.material.chemistry.process.ProcessPlanner;
import net.mads.industron.material.chemistry.process.ProcessRecipeNormalizer;
import net.mads.industron.material.chemistry.process.ProcessRecipeResolver;
import net.mads.industron.material.chemistry.process.ProcessRequirement;
import net.mads.industron.material.chemistry.process.ProcessRuleSet;
import net.mads.industron.material.chemistry.process.ProcessSemantics;
import net.mads.industron.material.chemistry.process.ProcessStep;
import net.mads.industron.material.chemistry.process.ProcessSubstanceState;
import net.mads.industron.material.chemistry.process.WoodProcessingPlanner;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ChemistrySelfTest {
    private ChemistrySelfTest() {}

    public static void run() {
        durationAndTierChecks();
        phase8IdentityChecks();
        phase10To13CoreChecks();
        chainProfileChecks();
        compoundInferenceChecks();
        definitionCompositionChecks();
        woodProcessingChecks();
        recipeReductionChecks();
        physicalStateChecks();
        boilingSeparationChecks();
        electrochemistryChecks();
        chemicalBalanceChecks();
        completeChainChecks();
        operationContractChecks();
    }

    private static void phase10To13CoreChecks() {
        ElementDefinition donor = new ElementDefinition("phase_core_donor", "Phase Core Donor", "Pd", 3, MachineTier.ULV);
        ElementDefinition acceptor = new ElementDefinition("phase_core_acceptor", "Phase Core Acceptor", "Pa", 9, MachineTier.ULV);
        MaterialSnapshot donorSnapshot = new MaterialSnapshot(
                donor.id(), donor.displayName(), donor.color(), ChemistryPhase.SOLID,
                List.of(), Optional.empty(), Map.of(), Set.of(MaterialClassification.ELEMENT),
                0, "ULV", Set.of(), donor
        );
        MaterialSnapshot acceptorSnapshot = new MaterialSnapshot(
                acceptor.id(), acceptor.displayName(), acceptor.color(), ChemistryPhase.SOLID,
                List.of(), Optional.empty(), Map.of(), Set.of(MaterialClassification.ELEMENT),
                0, "ULV", Set.of(), acceptor
        );

        ChemicalStructure molecule = ChemicalStructure.builder(ChemicalStructure.Topology.DISCRETE_MOLECULE)
                .atom("donor", donor.id())
                .atom("acceptor", acceptor.id())
                .bond("donor", "acceptor", BondType.COVALENT, BondOrder.SINGLE)
                .build();
        MaterialSnapshot molecularSnapshot = new MaterialSnapshot(
                "phase_core_molecule", "Phase Core Molecule", 0x808080, ChemistryPhase.SOLID,
                List.of(
                        new CompositionEntry(donor.id(), 1, ChemistryPhase.SOLID),
                        new CompositionEntry(acceptor.id(), 1, ChemistryPhase.SOLID)
                ),
                Optional.of(molecule), Map.of(), Set.of(), 0, "ULV", Set.of(), null
        );
        Map<String, MaterialSnapshot> registry = Map.of(
                donor.id(), donorSnapshot,
                acceptor.id(), acceptorSnapshot,
                molecularSnapshot.id(), molecularSnapshot
        );
        check(ChemicalStructureValidator.validate(molecularSnapshot, registry).valid(),
                "Phase 10 valid molecule graph must pass structure validation");
        check(ChemicalStructureGenerator.generate(molecularSnapshot, registry).isPresent(),
                "Phase 10 generator must return explicit structures unchanged");
        check(ChemicalFormula.fromStructure(molecule).signature().contains("charge=0"),
                "Phase 11 ChemicalFormula must preserve structure charge");
        check(new CompositionResolver(registry).atomic(molecularSnapshot)
                        .flatMap(vector -> ChemicalFormula.stoichiometricFromAtomicVector(vector, 0))
                        .map(ChemicalFormula::signature)
                        .orElse("")
                        .contains(donor.id() + "=1"),
                "Phase 11 ChemicalFormula must scale fractional atomic vectors into stoichiometric formulas");
        check(IonicCompoundSolver.best(donorSnapshot, acceptorSnapshot).isPresent(),
                "Phase 11 ionic solver must find a neutral candidate when opposite ion states exist");
        ChemicalSpecies dissolvedDonor = new ChemicalSpecies(
                "phase_core_donor_ion",
                donorSnapshot.substanceIdentity(Map.of(donor.id(), donorSnapshot)),
                new ChemicalFormula(Map.of(donor.id(), 1), 1),
                ChemistryPhase.LIQUID,
                ChemicalStructure.Topology.ATOMIC
        );
        ChemicalSpecies dissolvedAcceptor = new ChemicalSpecies(
                "phase_core_acceptor_ion",
                acceptorSnapshot.substanceIdentity(Map.of(acceptor.id(), acceptorSnapshot)),
                new ChemicalFormula(Map.of(acceptor.id(), 1), -1),
                ChemistryPhase.LIQUID,
                ChemicalStructure.Topology.ATOMIC
        );
        check(new SolutionPayload("phase_core_solvent", List.of(dissolvedDonor, dissolvedAcceptor)).chargeBalanced(),
                "Phase 11 solution payload must represent balanced dissolved species without registered solution items");

        ChemicalStructure polymer = ChemicalStructure.builder(ChemicalStructure.Topology.POLYMER_CHAIN)
                .atom("p1", donor.id())
                .atom("p2", acceptor.id())
                .bond("p1", "p2", BondType.COVALENT, BondOrder.DOUBLE)
                .repeatUnit("phase_core_unit", 8)
                .chainFlexibility(0.8)
                .crosslinkDensity(0.1)
                .build();
        PolymerDescriptor descriptor = PolymerDescriptor.from(polymer);
        check(descriptor.family() == PolymerDescriptor.PolymerFamily.ELASTOMER
                        && StructuralMotif.detect(polymer).contains(StructuralMotif.MULTIPLE_BOND),
                "Phase 12 polymer descriptor/motif detection must derive from structure");

        ReactionPlan reaction = new ReactionPlan(
                "phase_core/reaction_balance",
                List.of(
                        ReactionParticipant.units(donor.id(), 1, ChemistryPhase.SOLID),
                        ReactionParticipant.units(acceptor.id(), 1, ChemistryPhase.SOLID)
                ),
                List.of(ReactionParticipant.units(molecularSnapshot.id(), 2, ChemistryPhase.SOLID)),
                List.of("form donor-acceptor"),
                0,
                250,
                -20,
                20,
                "",
                "Phase 13 balance test"
        );
        check(ReactionBalancer.validate(reaction, registry).balanced(),
                "Phase 13 ReactionBalancer must conserve the canonical composition vector");
        check(ReactionSolver.synthesize(molecularSnapshot, registry).isPresent(),
                "Phase 13 ReactionSolver must synthesize a known target from direct .contains(...) components");
        check(ReactionSolver.decompose(molecularSnapshot, registry).isPresent(),
                "Phase 13 ReactionSolver must decompose a known target to direct .contains(...) components");
        ProcessStep reactionStep = ReactionProcessAdapter.toProcessStep(reaction, Map.of(
                donor.id(), analysisFor(donorSnapshot),
                acceptor.id(), analysisFor(acceptorSnapshot),
                molecularSnapshot.id(), analysisFor(molecularSnapshot)
        ));
        check(reactionStep.kind() == ProcessKind.CHEMICAL_REACTION && reactionStep.balanced(),
                "Phase 13 reaction plans must adapt into balanced CHEMICAL_REACTION process steps");
    }

    private static MaterialAnalysis analysisFor(MaterialSnapshot snapshot) {
        return new MaterialAnalysis(
                snapshot,
                new DerivedMaterialProperties(Map.of(), snapshot.color()),
                snapshot.classifications(),
                snapshot.phase(),
                snapshot.tierIndex(),
                snapshot.tierName(),
                List.of()
        );
    }

    private static void phase8IdentityChecks() {
        CompositionVector first = CompositionVector.direct(List.of(
                new CompositionEntry("phase8_a", 2, ChemistryPhase.SOLID),
                new CompositionEntry("phase8_b", 4, ChemistryPhase.SOLID)
        ));
        CompositionVector second = CompositionVector.direct(List.of(
                new CompositionEntry("phase8_b", 2, ChemistryPhase.SOLID),
                new CompositionEntry("phase8_a", 1, ChemistryPhase.SOLID)
        ));
        check(first.equals(second), "Phase 8 direct composition ratios must normalize deterministically");

        MaterialSnapshot atomA = new MaterialSnapshot(
                "phase8_a", "Phase 8 A", 0xFFFFFF, ChemistryPhase.SOLID,
                List.of(), Optional.empty(), Map.of(), Set.of(MaterialClassification.ELEMENT),
                0, "ULV", Set.of(), null
        );
        MaterialSnapshot atomB = new MaterialSnapshot(
                "phase8_b", "Phase 8 B", 0xAAAAAA, ChemistryPhase.SOLID,
                List.of(), Optional.empty(), Map.of(), Set.of(MaterialClassification.ELEMENT),
                0, "ULV", Set.of(), null
        );
        MaterialSnapshot compound = new MaterialSnapshot(
                "phase8_compound", "Phase 8 Compound", 0x808080, ChemistryPhase.SOLID,
                List.of(
                        new CompositionEntry(atomA.id(), 1, ChemistryPhase.SOLID),
                        new CompositionEntry(atomB.id(), 3, ChemistryPhase.SOLID)
                ),
                Optional.empty(), Map.of(), Set.of(), 0, "ULV", Set.of(), null
        );
        CompositionResolver resolver = new CompositionResolver(Map.of(
                atomA.id(), atomA,
                atomB.id(), atomB,
                compound.id(), compound
        ));
        check(resolver.conserved(compound).signature().equals("phase8_a=1/4;phase8_b=3/4"),
                "Phase 8 conserved vector must normalize each .contains(...) boundary");
        check(resolver.atomic(compound).orElseThrow().signature().equals("phase8_a=1/4;phase8_b=3/4"),
                "Phase 8 atomic vector must be available when all leaves are elements");

        ChemicalStructure left = ChemicalStructure.builder(ChemicalStructure.Topology.DISCRETE_MOLECULE)
                .atom("a1", atomA.id())
                .atom("b1", atomB.id())
                .bond("a1", "b1", BondType.COVALENT, BondOrder.SINGLE)
                .build();
        ChemicalStructure right = ChemicalStructure.builder(ChemicalStructure.Topology.DISCRETE_MOLECULE)
                .atom("random_b", atomB.id())
                .atom("random_a", atomA.id())
                .bond("random_b", "random_a", BondType.COVALENT, BondOrder.SINGLE)
                .build();
        check(left.canonicalSignature().equals(right.canonicalSignature()),
                "Phase 10 structure identity must not depend on authored atom ids or insertion order");
    }

    private static void durationAndTierChecks() {
        ProcessStep fractional = new ProcessStep(
                "self_test/fractional_fluid",
                ProcessKind.CENTRIFUGING,
                List.of(ProcessMaterial.fluid("input", ChemistryPhase.LIQUID, 144, 2, "MV", null)),
                List.of(ProcessMaterial.fluid("output", ChemistryPhase.LIQUID, 144, 3, "HV", null)),
                List.of(),
                "Duration rounding test"
        );
        check(fractional.balanced(), "144 mB balance");
        check(fractional.durationTicks() == 450, "144 mB (= 1 material unit) centrifuging must take 450 ticks, got " + fractional.durationTicks());
        check(fractional.recipeTierIndex() == 2, "HV output must create MV recipe");

        ProcessStep mixed = new ProcessStep(
                "self_test/mixed_output",
                ProcessKind.CENTRIFUGING,
                List.of(ProcessMaterial.item("feed", 2, 1, "LV", null), ProcessMaterial.fluid("wash", ChemistryPhase.LIQUID, 576, 1, "LV", null)),
                List.of(ProcessMaterial.item("solid", 2, 2, "MV", null), ProcessMaterial.fluid("liquid", ChemistryPhase.LIQUID, 576, 4, "EV", null)),
                List.of(),
                "Six unit duration and highest-output-tier test"
        );
        check(mixed.outputMilliUnits() == 6000, "mixed output units");
        check(mixed.durationTicks() == 2700, "six centrifuged units must take 2700 ticks");
        check(mixed.recipeTierIndex() == 3, "EV highest output must produce HV recipe");
    }

    private static void chainProfileChecks() {
        ProcessChainProfile ulv = ProcessChainProfile.forAnalysis(chainAnalysis(0, false));
        ProcessChainProfile lv = ProcessChainProfile.forAnalysis(chainAnalysis(1, true));
        ProcessChainProfile mvLow = ProcessChainProfile.forAnalysis(chainAnalysis(2, false));
        ProcessChainProfile mvHigh = ProcessChainProfile.forAnalysis(chainAnalysis(2, true));
        ProcessChainProfile hvLow = ProcessChainProfile.forAnalysis(chainAnalysis(3, false));
        ProcessChainProfile hvHigh = ProcessChainProfile.forAnalysis(chainAnalysis(3, true));
        ProcessChainProfile evLow = ProcessChainProfile.forAnalysis(chainAnalysis(4, false));
        ProcessChainProfile evHigh = ProcessChainProfile.forAnalysis(chainAnalysis(4, true));
        ProcessChainProfile ivLow = ProcessChainProfile.forAnalysis(chainAnalysis(5, false));
        ProcessChainProfile ivHigh = ProcessChainProfile.forAnalysis(chainAnalysis(5, true));
        ProcessChainProfile luvLow = ProcessChainProfile.forAnalysis(chainAnalysis(6, false));
        ProcessChainProfile luvHigh = ProcessChainProfile.forAnalysis(chainAnalysis(6, true));
        ProcessChainProfile zpm = ProcessChainProfile.forAnalysis(chainAnalysis(7, false));

        check(ulv.enabled() && ulv.targetSteps() == 1, "ULV automatic chemistry must be exactly 1 step");
        check(lv.enabled() && lv.targetSteps() == 1, "LV automatic chemistry must be exactly 1 step");
        check(mvLow.targetSteps() == 1 && mvHigh.targetSteps() == 2, "MV automatic chemistry must stay inside 1..2 steps");
        check(hvLow.targetSteps() == 1 && hvHigh.targetSteps() == 2, "HV automatic chemistry must stay inside 1..2 steps");
        check(evLow.targetSteps() == 2 && evHigh.targetSteps() == 3, "EV automatic chemistry must stay inside 2..3 steps");
        check(ivLow.targetSteps() == 3 && ivHigh.targetSteps() == 4, "IV automatic chemistry must stay inside 3..4 steps");
        check(luvLow.targetSteps() == 4 && luvHigh.targetSteps() == 5, "LuV automatic chemistry must stay inside 4..5 steps");
        check(!zpm.enabled() && zpm.targetSteps() == 0, "ZPM+ must not receive autogenerated compound-dust chemistry");

        ProcessChainProfile lvLeach = ProcessChainProfile.forAnalysis(
                chainAnalysis(1, true),
                SeparationClassifier.Route.LEACHING
        );
        check(lvLeach.enabled()
                        && lvLeach.targetSteps() == 2
                        && ProcessChainProfile.processingTierIndex(chainAnalysis(1, true), SeparationClassifier.Route.LEACHING) == 2,
                "A chemically required two-step LV leaching route must be promoted to MV processing instead of being discarded");

        ProcessChainProfile mvLeach = ProcessChainProfile.forAnalysis(
                chainAnalysis(2, true),
                SeparationClassifier.Route.LEACHING
        );
        check(mvLeach.enabled()
                        && mvLeach.targetSteps() == 2
                        && mvLeach.stages().equals(List.of(ProcessChainProfile.Stage.SOLUTION)),
                "MV two-step leaching must generate only the registry-backed solution intermediate");

        ProcessChainProfile ivLeach = ProcessChainProfile.forAnalysis(
                chainAnalysis(5, true),
                SeparationClassifier.Route.LEACHING
        );
        check(ivLeach.stages().equals(List.of(
                        ProcessChainProfile.Stage.ROASTED_DUST,
                        ProcessChainProfile.Stage.SLURRY,
                        ProcessChainProfile.Stage.SOLUTION
                )),
                "IV four-step leaching must be roasted dust -> slurry -> solution -> recovery");

        ProcessChainProfile luvLeach = ProcessChainProfile.forAnalysis(
                chainAnalysis(6, true),
                SeparationClassifier.Route.LEACHING
        );
        check(luvLeach.stages().equals(List.of(
                        ProcessChainProfile.Stage.ROASTED_DUST,
                        ProcessChainProfile.Stage.SLURRY,
                        ProcessChainProfile.Stage.SOLUTION,
                        ProcessChainProfile.Stage.REACTION_MIXTURE
                )),
                "LuV five-step leaching must include the reaction-mixture stage");

        ProcessChainProfile molten = ProcessChainProfile.forAnalysis(
                chainAnalysis(4, true),
                SeparationClassifier.Route.MOLTEN_ELECTROLYSIS
        );
        check(molten.enabled() && molten.targetSteps() == 2 && molten.stages().isEmpty(),
                "molten routes must be exactly melt + separation and must not invent slurry/solution intermediates");

        ProcessChainProfile lowTierMolten = ProcessChainProfile.forAnalysis(
                chainAnalysis(1, true),
                SeparationClassifier.Route.MOLTEN_ELECTROLYSIS
        );
        check(lowTierMolten.enabled()
                        && lowTierMolten.targetSteps() == 2
                        && ProcessChainProfile.processingTierIndex(chainAnalysis(1, true), SeparationClassifier.Route.MOLTEN_ELECTROLYSIS) == 2,
                "chemically required melt + electrolysis must promote processing to MV instead of deleting the route");
    }

    private static void compoundInferenceChecks() {
        MaterialSnapshot gasDonor = snapshot(
                "self_test_gas_donor",
                ChemistryPhase.GAS,
                Map.of(
                        "electrondonationtendency", 80.0D,
                        "electronacceptancetendency", 10.0D,
                        "bondstrength", 35.0D,
                        "crystalstability", 20.0D,
                        "volatility", 90.0D,
                        "metallicity", 10.0D
                )
        );
        MaterialSnapshot gasAcceptor = snapshot(
                "self_test_gas_acceptor",
                ChemistryPhase.GAS,
                Map.of(
                        "electrondonationtendency", 5.0D,
                        "electronacceptancetendency", 85.0D,
                        "bondstrength", 40.0D,
                        "crystalstability", 25.0D,
                        "volatility", 92.0D,
                        "metallicity", 5.0D
                )
        );
        MaterialSnapshot solidCompound = new MaterialSnapshot(
                "self_test_solid_from_gases",
                "Self Test Solid From Gases",
                0x808080,
                ChemistryPhase.SOLID,
                List.of(
                        new CompositionEntry(gasDonor.id(), 2, ChemistryPhase.GAS),
                        new CompositionEntry(gasAcceptor.id(), 1, ChemistryPhase.GAS)
                ),
                Optional.empty(),
                Map.of(),
                Set.of(),
                1,
                "LV",
                Set.of(),
                null
        );
        Map<String, MaterialSnapshot> registry = Map.of(
                gasDonor.id(), gasDonor,
                gasAcceptor.id(), gasAcceptor,
                solidCompound.id(), solidCompound
        );
        check(ChemicalTopologyResolver.resolve(solidCompound, registry) != ChemicalStructure.Topology.PHYSICAL_MIXTURE,
                "a registered solid compound must not become PHYSICAL_MIXTURE merely because its recovered components are gases");

        MaterialAnalysis analyzed = new ChemistryEngine().analyze(solidCompound, registry);
        check(analyzed.properties().get("polarity") > 0.0D,
                "compound polarity must be derived after blending donor/acceptor properties instead of being frozen at zero");
        check(ChemistryEngine.calculateTier(Map.of(), Set.of(), 1) >= 1,
                "compound tier must never fall below the strongest component tier floor");

        MaterialSnapshot viscousHighMelting = snapshot(
                "self_test_viscous_high_melting",
                ChemistryPhase.UNKNOWN,
                Map.of(
                        "meltingpoint", 1250.0D,
                        "boilingpoint", 2100.0D,
                        "viscosity", 80.0D,
                        "volatility", 10.0D
                )
        );
        MaterialAnalysis viscousHighMeltingAnalysis = new ChemistryEngine().analyze(
                viscousHighMelting,
                Map.of(viscousHighMelting.id(), viscousHighMelting)
        );
        check(viscousHighMeltingAnalysis.phase() == ChemistryPhase.SOLID,
                "high viscosity must not override a melting point above ambient temperature");

        ProcessStep promoted = new ProcessStep(
                "self_test/promoted_processing_tier",
                ProcessKind.ELECTROLYSIS,
                List.of(ProcessMaterial.item("feed", 1, 1, "LV", null)),
                List.of(ProcessMaterial.item("product", 1, 1, "LV", null)),
                List.of(),
                0,
                2,
                "A two-step route may impose MV processing even when material identity is LV"
        );
        check(promoted.recipeTierIndex() == 2,
                "route processing-tier promotion must reach the emitted recipe tier");
    }

    private static MaterialSnapshot snapshot(String id, ChemistryPhase phase, Map<String, Double> properties) {
        return new MaterialSnapshot(
                id,
                id,
                0x808080,
                phase,
                List.of(),
                Optional.empty(),
                properties,
                Set.of(),
                0,
                "ULV",
                Set.of(),
                null
        );
    }

    private static void definitionCompositionChecks() {
        check(WoodMaterials.OAK.generatedForms().contains(MaterialPart.WOOD_PULP),
                "wood definitions used by .contains(...) must expose WOOD_PULP as a concrete composition form");
        check(StoneMaterials.STONE.generatedForms().contains(MaterialPart.DUST),
                "stone definitions used by .contains(...) must expose DUST as a concrete composition form");
        check(!WoodMaterials.OAK.components().isEmpty(),
                "wood definitions participating in .contains(...) must remain chemically analyzable");
        check(!StoneMaterials.STONE.components().isEmpty(),
                "stone definitions participating in .contains(...) must remain chemically analyzable");
    }

    private static void woodProcessingChecks() {
        ChemistryBootstrap.AnalysisResult current = ChemistryBootstrap.currentOrAnalyze();
        MaterialAnalysis oak = current.analyses().get(WoodMaterials.OAK.id());
        check(oak != null, "Oak must be present in the chemistry analysis registry");

        WoodProcessingPlanner.Preview first = WoodProcessingPlanner.preview(
                WoodMaterials.OAK, oak, current.analyses());
        WoodProcessingPlanner.Preview second = WoodProcessingPlanner.preview(
                WoodMaterials.OAK, oak, current.analyses());
        check(first.pyrolysis().isPresent(),
                "ordinary wood composition must have a representable deterministic pyrolysis route");
        check(first.pyrolysis().map(WoodProcessingPlanner.PyrolysisPreview::direct)
                        .equals(second.pyrolysis().map(WoodProcessingPlanner.PyrolysisPreview::direct)),
                "wood route selection must be deterministic for identical composition/properties");
        check(first.pyrolysis().orElseThrow().direct(),
                "ordinary 4-component wood must decompose directly when Pyrolysis IO can represent every declared component");
        check(!first.requiredIntermediates().contains(AutomaticProcessIntermediate.Kind.PYROLYSATE),
                "a direct wood pyrolysis route must not register an unused pyrolysate intermediate");
        List<Integer> declaredWoodRatio = first.pyrolysis().orElseThrow().recoveryOutputs().stream()
                .map(ProcessMaterial::itemAmountExact)
                .toList();
        check(declaredWoodRatio.equals(List.of(14, 3, 2, 1)),
                "wood decomposition must preserve the declared top-level .contains(...) ratio 14:3:2:1, got "
                        + declaredWoodRatio);
        check(AutomaticProcessIntermediate.Kind.PYROLYSATE.materialId(WoodMaterials.OAK).equals("oak_pyrolysate"),
                "pyrolysate registry id must be derived from the source wood id");
        check(AutomaticProcessIntermediate.Kind.PYROLYSATE.displayName(WoodMaterials.OAK).equals("Oak Pyrolysate"),
                "pyrolysate display name must be source wood + shared process suffix");
        check(AutomaticProcessIntermediate.Kind.PYROLYSATE.sourceId(WoodMaterials.OAK)
                        .equals("automatic_processing:oak:pyrolysate"),
                "wood pyrolysate source id must use the general automatic-processing namespace when a route needs one");
        check(!current.analyses().containsKey(AutomaticProcessIntermediate.Kind.PYROLYSATE.materialId(WoodMaterials.OAK)),
                "Oak Pyrolysate must not exist when the selected Oak route does not consume it");
        check(AutomaticProcessIntermediate.Kind.SLURRY.sourceId(WoodMaterials.OAK)
                        .equals("automatic_processing:oak:slurry"),
                "wood slurry source id must use the general automatic-processing namespace when a route actually requires it");

        check(ProcessPlanner.automaticProcessingInputPart(WoodMaterials.OAK) == MaterialPart.WOOD_PULP,
                "wood automatic chemistry must enter through WOOD_PULP");
        check(ProcessPlanner.isAutomaticCompositeProcessingTarget(WoodMaterials.OAK),
                "composition-backed wood with WOOD_PULP must participate in complete-chain validation");

        for (var wood : WoodMaterials.ALL) {
            MaterialAnalysis woodAnalysis = current.analyses().get(wood.id());
            check(woodAnalysis != null, wood.displayName() + " must be present in the chemistry analysis registry");
            WoodProcessingPlanner.Preview selected = WoodProcessingPlanner.preview(wood, woodAnalysis, current.analyses());
            for (AutomaticProcessIntermediate.Kind kind : List.of(
                    AutomaticProcessIntermediate.Kind.ROASTED_DUST,
                    AutomaticProcessIntermediate.Kind.SLURRY,
                    AutomaticProcessIntermediate.Kind.SOLUTION,
                    AutomaticProcessIntermediate.Kind.REACTION_MIXTURE,
                    AutomaticProcessIntermediate.Kind.PYROLYSATE
            )) {
                boolean expected = selected.requiredIntermediates().contains(kind);
                boolean registered = current.analyses().containsKey(kind.materialId(wood));
                check(expected == registered,
                        "wood intermediate " + kind + " must exist iff the selected "
                                + wood.displayName() + " routes require it");
            }
        }

        boolean hasPublishedPyrolysisPlan = current.plans().stream()
                .filter(plan -> plan.targetMaterialId().equals("oak_wood_pyrolysis_processing"))
                .flatMap(plan -> plan.steps().stream())
                .anyMatch(step -> step.kind() == ProcessKind.PYROLYSIS);
        check(hasPublishedPyrolysisPlan,
                "Oak must publish the selected pyrolysis processing plan after safety validation");
    }

    private static void recipeReductionChecks() {
        ProcessMaterial moltenFourUnits = new ProcessMaterial(
                "molten_vernite", ChemistryPhase.MOLTEN, 4000, 1, "LV", true, null, MaterialPart.MOLTEN_FLUID
        );
        check(moltenFourUnits.milliBucketsExact() == 576, "4 molten material units must equal 576 mB");
        ProcessMaterial moltenTenUnits = new ProcessMaterial(
                "molten_vernite", ChemistryPhase.MOLTEN, 10000, 1, "LV", true, null, MaterialPart.MOLTEN_FLUID
        );
        check(moltenTenUnits.milliBucketsExact() == 1440, "10 molten material units must equal 1440 mB");

        ProcessMaterial moltenFromMb = ProcessMaterial.fluid(
                "molten_vernite", ChemistryPhase.MOLTEN, 144, 1, "LV", null, MaterialPart.MOLTEN_FLUID
        );
        check(moltenFromMb.milliUnits() == 1000, "144 mB molten must equal one material unit internally");

        ProcessMaterial gasOneUnit = new ProcessMaterial(
                "test_gas", ChemistryPhase.GAS, 1000, 1, "LV", true, null, MaterialPart.GAS
        );
        check(gasOneUnit.milliBucketsExact() == 576, "1 gas material unit must equal 576 mB");
        ProcessMaterial gasFromMb = ProcessMaterial.fluid(
                "test_gas", ChemistryPhase.GAS, 576, 1, "LV", null, MaterialPart.GAS
        );
        check(gasFromMb.milliUnits() == 1000, "576 mB gas must equal one material unit internally");

        ProcessStep reducible = new ProcessStep(
                "self_test/reducible_batch",
                ProcessKind.MELTING,
                List.of(ProcessMaterial.item("vernite_dust", 4, 1, "LV", null)),
                List.of(moltenFourUnits),
                List.of(),
                "Generated recipe amounts should be emitted in lowest terms"
        );
        ProcessStep reduced = ProcessRecipeNormalizer.reduceCommonBatch(reducible);
        check(ProcessRecipeNormalizer.commonDivisor(reducible) == 4, "4 dust -> 576 mB common divisor");
        check(reduced.inputs().getFirst().itemAmountExact() == 1, "4 dust must reduce to 1 dust");
        check(reduced.outputs().getFirst().milliBucketsExact() == 144, "576 mB must reduce to 144 mB");
        check(reduced.balanced(), "reduced process must remain balanced");
        check(reduced.durationTicks() * 4 == reducible.durationTicks(), "reduced batch duration must preserve throughput");
        check(reduced.id().equals(reducible.id()) && reduced.kind() == reducible.kind(),
                "recipe reduction must not change identity or process kind");

        ProcessStep irreducible = new ProcessStep(
                "self_test/irreducible_batch",
                ProcessKind.CENTRIFUGING,
                List.of(ProcessMaterial.item("feed", 3, 1, "LV", null)),
                List.of(
                        ProcessMaterial.fluid("fraction_a", ChemistryPhase.LIQUID, 144, 1, "LV", null),
                        ProcessMaterial.fluid("fraction_b", ChemistryPhase.LIQUID, 288, 1, "LV", null)
                ),
                List.of(),
                "A recipe with no shared serialized divisor must stay unchanged"
        );
        check(ProcessRecipeNormalizer.reduceCommonBatch(irreducible) == irreducible,
                "irreducible recipe must remain exactly unchanged");

        ProcessStep stoichiometric = new ProcessStep(
                "self_test/stoichiometric_3_4",
                ProcessKind.CHEMICAL_REACTION,
                List.of(ProcessMaterial.item("compound_dust", 14, 2, "MV", null)),
                List.of(
                        ProcessMaterial.item("component_a_dust", 6, 1, "LV", null),
                        ProcessMaterial.item("component_b_dust", 8, 1, "LV", null)
                ),
                List.of(),
                "The serializer must preserve a 3:4 .contains(...) ratio when reducing a larger balanced batch"
        );
        ProcessStep stoichiometricReduced = ProcessRecipeNormalizer.reduceCommonBatch(stoichiometric);
        check(stoichiometricReduced.inputs().getFirst().itemAmountExact() == 7,
                "a 3+4 compound formula must reduce to a 7-unit compound batch");
        check(stoichiometricReduced.outputs().get(0).itemAmountExact() == 3
                        && stoichiometricReduced.outputs().get(1).itemAmountExact() == 4,
                "a 3+4 compound formula must preserve exact 3:4 component output stoichiometry");
        check(stoichiometricReduced.balanced(), "reduced 3:4 compound processing must remain material-balanced");
    }

    private static void physicalStateChecks() {
        ProcessStep invalidDistillation = new ProcessStep(
                "self_test/dust_distillation",
                ProcessKind.DISTILLATION,
                List.of(ProcessMaterial.item("dust", 1, 1, "LV", null)),
                List.of(
                        ProcessMaterial.fluid("fraction_a", ChemistryPhase.LIQUID, 144, 1, "LV", null),
                        ProcessMaterial.fluid("fraction_b", ChemistryPhase.LIQUID, 144, 1, "LV", null)
                ),
                List.of(),
                "Semantic phase test"
        );
        check(ProcessSemantics.validate(invalidDistillation).isPresent(), "dust distillation must be rejected");

        ProcessMaterial bondedDust = material(
                "bonded_dust", ChemistryPhase.SOLID, 1000,
                ProcessSubstanceState.bonded(ChemicalStructure.Topology.NETWORK), Map.of()
        );
        ProcessStep invalidBondBreakingSeparator = new ProcessStep(
                "self_test/bonded_centrifuging",
                ProcessKind.CENTRIFUGING,
                List.of(bondedDust),
                List.of(
                        material("a", ChemistryPhase.SOLID, 500, ProcessSubstanceState.bonded(ChemicalStructure.Topology.ATOMIC), Map.of("density", 10.0D)),
                        material("b", ChemistryPhase.SOLID, 500, ProcessSubstanceState.bonded(ChemicalStructure.Topology.ATOMIC), Map.of("density", 80.0D))
                ),
                List.of(),
                "Bonded substances cannot be opened by a physical separator"
        );
        check(ProcessSemantics.validate(invalidBondBreakingSeparator).isPresent(),
                "physical separator must reject a bonded substance");

        ProcessMaterial physicalPowder = material(
                "powder_mix", ChemistryPhase.SOLID, 1000,
                ProcessSubstanceState.physicalMixture(ProcessSubstanceState.MixtureKind.POWDER_MIXTURE), Map.of()
        );
        ProcessStep validCentrifuging = new ProcessStep(
                "self_test/density_centrifuging",
                ProcessKind.CENTRIFUGING,
                List.of(physicalPowder),
                List.of(
                        material("dense", ChemistryPhase.SOLID, 500, ProcessSubstanceState.bonded(ChemicalStructure.Topology.ATOMIC), Map.of("density", 80.0D)),
                        material("light", ChemistryPhase.SOLID, 500, ProcessSubstanceState.bonded(ChemicalStructure.Topology.ATOMIC), Map.of("density", 20.0D))
                ),
                List.of(),
                "Density separation test"
        );
        check(ProcessSemantics.validate(validCentrifuging).isEmpty(),
                "centrifuging must accept an explicit powder mixture with density contrast");

        ProcessMaterial mixedOutput = material(
                "mixed", ChemistryPhase.SOLID, 2000,
                ProcessSubstanceState.physicalMixture(ProcessSubstanceState.MixtureKind.POWDER_MIXTURE), Map.of()
        );
        ProcessStep validMixing = new ProcessStep(
                "self_test/physical_mixing",
                ProcessKind.MIXING,
                List.of(ProcessMaterial.item("a", 1, 1, "LV", null), ProcessMaterial.item("b", 1, 1, "LV", null)),
                List.of(mixedOutput),
                List.of(),
                "Mixing creates a physical mixture without claiming bonds"
        );
        check(ProcessSemantics.validate(validMixing).isEmpty(), "mixing must accept an explicit physical-mixture output");

        ProcessMaterial genericLiquidMix = material(
                "liquid_mix", ChemistryPhase.LIQUID, 1000,
                ProcessSubstanceState.physicalMixture(ProcessSubstanceState.MixtureKind.LIQUID_MIXTURE), Map.of()
        );
        ProcessStep invalidFiltration = new ProcessStep(
                "self_test/not_a_suspension",
                ProcessKind.FILTRATION,
                List.of(genericLiquidMix),
                List.of(
                        material("solid", ChemistryPhase.SOLID, 500, ProcessSubstanceState.bonded(ChemicalStructure.Topology.ATOMIC), Map.of()),
                        material("liquid", ChemistryPhase.LIQUID, 500, ProcessSubstanceState.bonded(ChemicalStructure.Topology.DISCRETE_MOLECULE), Map.of())
                ),
                List.of(),
                "Filtration requires suspension state"
        );
        check(ProcessSemantics.validate(invalidFiltration).isPresent(),
                "filtration must not treat every physical liquid mixture as a suspension");

        ProcessStep validVaporization = new ProcessStep(
                "self_test/liquid_vaporization",
                ProcessKind.VAPORIZATION,
                List.of(ProcessMaterial.fluid("liquid", ChemistryPhase.LIQUID, 144, 1, "LV", null)),
                List.of(ProcessMaterial.fluid("gas", ChemistryPhase.GAS, 576, 1, "LV", null)),
                List.of(),
                "Vaporization enum and semantic phase test"
        );
        check(ProcessSemantics.validate(validVaporization).isEmpty(), "liquid vaporization must be accepted");

        ProcessMaterial crystallizationFeed = material(
                "self_test_pyrolysate", ChemistryPhase.LIQUID, 1000,
                ProcessSubstanceState.physicalMixture(ProcessSubstanceState.MixtureKind.LIQUID_MIXTURE), Map.of()
        );
        ProcessStep validCrystallization = new ProcessStep(
                "self_test/crystallization",
                ProcessKind.CRYSTALLIZATION,
                List.of(crystallizationFeed),
                List.of(material(
                        "crystal", ChemistryPhase.SOLID, 1000,
                        ProcessSubstanceState.bonded(ChemicalStructure.Topology.DISCRETE_MOLECULE), Map.of()
                )),
                List.of(),
                "A condensed physical mixture may crystallize solid products"
        );
        check(ProcessSemantics.validate(validCrystallization).isEmpty(),
                "crystallization must accept a condensed physical mixture and solid products");
    }

    private static void boilingSeparationChecks() {
        ProcessMaterial solution = material(
                "solution", ChemistryPhase.LIQUID, 1000,
                ProcessSubstanceState.physicalMixture(ProcessSubstanceState.MixtureKind.SOLUTION), Map.of()
        );
        ProcessMaterial closeA = material(
                "close_a", ChemistryPhase.LIQUID, 500,
                ProcessSubstanceState.bonded(ChemicalStructure.Topology.DISCRETE_MOLECULE), Map.of("boilingpoint", 100.0D, "volatility", 40.0D)
        );
        ProcessMaterial closeB = material(
                "close_b", ChemistryPhase.LIQUID, 500,
                ProcessSubstanceState.bonded(ChemicalStructure.Topology.DISCRETE_MOLECULE), Map.of("boilingpoint", 122.0D, "volatility", 52.0D)
        );
        ProcessStep fractionation = new ProcessStep(
                "self_test/fractionation",
                ProcessKind.FRACTIONATION,
                List.of(solution),
                List.of(closeA, closeB),
                List.of(),
                "Close boiling points require fractionation"
        );
        check(ProcessSemantics.validate(fractionation).isEmpty(),
                "fractionation must accept close distinguishable boiling points from process properties");

        ProcessMaterial farB = material(
                "far_b", ChemistryPhase.LIQUID, 500,
                ProcessSubstanceState.bonded(ChemicalStructure.Topology.DISCRETE_MOLECULE), Map.of("boilingpoint", 190.0D, "volatility", 82.0D)
        );
        ProcessStep distillation = new ProcessStep(
                "self_test/distillation",
                ProcessKind.DISTILLATION,
                List.of(solution),
                List.of(closeA, farB),
                List.of(),
                "Large boiling/volatility difference supports ordinary distillation"
        );
        check(ProcessSemantics.validate(distillation).isEmpty(),
                "distillation must accept a physical solution with a large boiling spread");
    }

    private static void electrochemistryChecks() {
        ProcessMaterial ionicSolution = material(
                "generated_ionic_solution", ChemistryPhase.LIQUID, 1000,
                ProcessSubstanceState.physicalMixture(ProcessSubstanceState.MixtureKind.SOLUTION),
                Map.of(
                        "electrochemicalpotential", 44.0D,
                        "electrondonationtendency", 60.0D,
                        "preferredioncharge", 2.0D
                )
        );
        ProcessMaterial metalOutput = material(
                "generated_metal", ChemistryPhase.SOLID, 1000,
                ProcessSubstanceState.bonded(ChemicalStructure.Topology.ATOMIC),
                Map.of(
                        "metalliccharacter", 90.0D,
                        "electrochemicalpotential", 30.0D,
                        "preferredioncharge", 2.0D
                )
        );

        ProcessStep electrowinning = new ProcessStep(
                "self_test/electrowinning_generated_properties",
                ProcessKind.ELECTROWINNING,
                List.of(ionicSolution),
                List.of(metalOutput),
                List.of(),
                "Generated process properties must be sufficient without hardcoded backing material"
        );
        check(ProcessSemantics.validate(electrowinning).isEmpty(),
                "electrowinning must read generated electrochemical/oxidation properties from ProcessMaterial");

        ProcessMaterial ionicDust = material(
                "generated_ionic_dust", ChemistryPhase.SOLID, 1000,
                ProcessSubstanceState.bonded(ChemicalStructure.Topology.IONIC_LATTICE),
                Map.of("electrochemicalpotential", 40.0D, "preferredioncharge", 1.0D)
        );
        ProcessStep directIonicElectrolysis = new ProcessStep(
                "self_test/direct_ionic_dust_electrolysis",
                ProcessKind.ELECTROLYSIS,
                List.of(ionicDust),
                List.of(metalOutput),
                List.of(),
                "An explicitly ionic solid lattice may use the classifier's direct-electrolysis family"
        );
        check(ProcessSemantics.validate(directIonicElectrolysis).isEmpty(),
                "direct electrolysis must accept an explicitly ionic solid lattice");

        ProcessMaterial moltenMetalMix = material(
                "generated_molten_metal_mix", ChemistryPhase.MOLTEN, 1000,
                ProcessSubstanceState.physicalMixture(ProcessSubstanceState.MixtureKind.MOLTEN_MIXTURE),
                Map.of("metalliccharacter", 90.0D, "electrochemicalpotential", 50.0D, "preferredioncharge", 2.0D)
        );
        ProcessStep validElectrorefining = new ProcessStep(
                "self_test/electrorefining_metal_outputs",
                ProcessKind.ELECTROREFINING,
                List.of(moltenMetalMix),
                List.of(metalOutput),
                List.of(),
                "Molten metallic feed may electrorefine only into solid metal products"
        );
        check(ProcessSemantics.validate(validElectrorefining).isEmpty(),
                "electrorefining must accept molten metallic feed with solid-metal recovered outputs");

        ProcessMaterial nonMetalOutput = material(
                "generated_nonmetal", ChemistryPhase.SOLID, 1000,
                ProcessSubstanceState.bonded(ChemicalStructure.Topology.ATOMIC),
                Map.of("metalliccharacter", 0.0D)
        );
        ProcessStep invalidElectrorefining = new ProcessStep(
                "self_test/electrorefining_nonmetal_output",
                ProcessKind.ELECTROREFINING,
                List.of(moltenMetalMix),
                List.of(nonMetalOutput),
                List.of(),
                "Electrorefining must not pretend that a metallic feed can recover arbitrary non-metal components"
        );
        check(ProcessSemantics.validate(invalidElectrorefining).isPresent(),
                "electrorefining must reject non-metal guaranteed outputs");
    }

    private static void chemicalBalanceChecks() {
        ProcessMaterial bondedDust = material(
                "cb_feed", ChemistryPhase.SOLID, 1000,
                ProcessSubstanceState.bonded(ChemicalStructure.Topology.NETWORK), Map.of()
        );
        ProcessMaterial solution = material(
                "cb_solution", ChemistryPhase.LIQUID, 1000,
                ProcessSubstanceState.physicalMixture(ProcessSubstanceState.MixtureKind.SOLUTION), Map.of()
        );

        ProcessStep withoutCb = new ProcessStep(
                "self_test/dissolution_without_cb",
                ProcessKind.DISSOLUTION,
                List.of(bondedDust),
                List.of(solution),
                List.of(),
                "Dissolution must not invent a direct acid/base material input"
        );
        check(ProcessSemantics.validate(withoutCb).isPresent(),
                "dissolution without a Chemical Balance environment must be rejected");

        ProcessStep withCb = new ProcessStep(
                "self_test/dissolution_with_cb",
                ProcessKind.DISSOLUTION,
                List.of(bondedDust),
                List.of(solution),
                List.of(ProcessRequirement.chemicalBalance(20.0D, 70.0D)),
                "Acidic/basic chemistry is represented by CB rather than a recipe fluid input"
        );
        check(ProcessSemantics.validate(withCb).isEmpty(),
                "dissolution with a Chemical Balance range must be accepted");
        check(withCb.inputMilliUnits() == 1000 && withCb.outputMilliUnits() == 1000,
                "Chemical Balance must not add material mass to the recipe");
    }

    private static void completeChainChecks() {
        MaterialSnapshot woodSnapshot = new MaterialSnapshot(
                WoodMaterials.OAK.id(),
                WoodMaterials.OAK.displayName(),
                WoodMaterials.OAK.color(),
                ChemistryPhase.SOLID,
                WoodMaterials.OAK.components().stream()
                        .map(component -> new CompositionEntry(component.substance().id(), component.amount(), ChemistryPhase.SOLID))
                        .toList(),
                Optional.empty(),
                Map.of(),
                Set.of(),
                0,
                "ULV",
                Set.of(),
                WoodMaterials.OAK
        );
        MaterialAnalysis woodAnalysis = new MaterialAnalysis(
                woodSnapshot,
                new DerivedMaterialProperties(Map.of(), WoodMaterials.OAK.color()),
                Set.of(),
                ChemistryPhase.SOLID,
                0,
                "ULV",
                List.of()
        );

        ProcessPlan woodTerminal = new ProcessPlan(
                "self_test_composite_dust_processing",
                List.of(new ProcessStep(
                        "self_test/wood_terminal",
                        ProcessKind.CENTRIFUGING,
                        List.of(new ProcessMaterial(
                                "self_test_feed", ChemistryPhase.SOLID, 1000, 0, "ULV", true, null, MaterialPart.DUST
                        )),
                        List.of(new ProcessMaterial(
                                WoodMaterials.OAK.id(), ChemistryPhase.SOLID, 1000, 0, "ULV", true,
                                WoodMaterials.OAK, MaterialPart.WOOD_PULP
                        )),
                        List.of(),
                        "StructureMaterial outputs are legal terminal .contains(...) components"
                ))
        );
        ProcessSafetyValidator.Result terminalResult = new ProcessSafetyValidator().validateCompleteChains(
                List.of(woodTerminal),
                Map.of(WoodMaterials.OAK.id(), woodAnalysis)
        );
        check(terminalResult.plans().isEmpty(),
                "wood with WOOD_PULP and .contains(...) must not be treated as a terminal component when its downstream route is missing");

        MaterialSnapshot deadSnapshot = new MaterialSnapshot(
                "self_test_dead_component",
                "Self Test Dead Component",
                0x808080,
                ChemistryPhase.SOLID,
                List.of(new CompositionEntry("missing_child", 1, ChemistryPhase.SOLID)),
                Optional.empty(),
                Map.of(),
                Set.of(),
                0,
                "ULV",
                Set.of(),
                null
        );
        MaterialAnalysis deadAnalysis = new MaterialAnalysis(
                deadSnapshot, new DerivedMaterialProperties(Map.of(), 0x808080), Set.of(),
                ChemistryPhase.SOLID, 0, "ULV", List.of()
        );
        ProcessPlan deadEnd = new ProcessPlan(
                "self_test_dead_composite_dust_processing",
                List.of(new ProcessStep(
                        "self_test/dead_end",
                        ProcessKind.CENTRIFUGING,
                        List.of(new ProcessMaterial(
                                "self_test_feed", ChemistryPhase.SOLID, 1000, 0, "ULV", true, null, MaterialPart.DUST
                        )),
                        List.of(new ProcessMaterial(
                                "self_test_dead_component", ChemistryPhase.SOLID, 1000, 0, "ULV", true, null, MaterialPart.DUST
                        )),
                        List.of(),
                        "Unknown composite dead ends must still be blocked"
                ))
        );
        ProcessSafetyValidator.Result deadResult = new ProcessSafetyValidator().validateCompleteChains(
                List.of(deadEnd),
                Map.of("self_test_dead_component", deadAnalysis)
        );
        check(deadResult.plans().isEmpty(),
                "non-terminal composite output without a downstream automatic DUST route must be blocked");
    }

    private static void operationContractChecks() {
        check(ProcessRuleSet.forKind(ProcessKind.CENTRIFUGING).supportsAll(
                        java.util.Set.of(ProcessOperation.SEPARATE_PHYSICAL_PHASES, ProcessOperation.SEPARATE_BY_DENSITY)),
                "centrifuging typed capabilities");
        check(!ProcessRuleSet.forKind(ProcessKind.CENTRIFUGING).supportsAll(
                        java.util.Set.of(ProcessOperation.BREAK_OR_FORM_BONDS)),
                "centrifuging must not expose bond-breaking capability");
        check(ProcessRuleSet.forKind(ProcessKind.ELECTROLYSIS).supportsAll(
                        java.util.Set.of(ProcessOperation.TRANSFER_ELECTRONS, ProcessOperation.CHANGE_OXIDATION_STATE)),
                "electrolysis typed electron-transfer capabilities");

        ProcessRecipeResolver resolver = new ProcessRecipeResolver();
        for (ProcessKind kind : List.of(
                ProcessKind.CENTRIFUGING,
                ProcessKind.MAGNETIC_SEPARATION,
                ProcessKind.ROASTING,
                ProcessKind.LEACHING,
                ProcessKind.DISSOLUTION,
                ProcessKind.CHEMICAL_REACTION,
                ProcessKind.MELTING,
                ProcessKind.ELECTROLYSIS,
                ProcessKind.ELECTROREFINING,
                ProcessKind.ELECTROWINNING,
                ProcessKind.PYROLYSIS,
                ProcessKind.CRYSTALLIZATION,
                ProcessKind.DISTILLATION,
                ProcessKind.FRACTIONATION
        )) {
            check(resolver.recipeTypeFor(kind).isPresent(),
                    "automatic chemistry process " + kind + " must map to an existing RecipeTypeDefinition");
        }
    }

    private static MaterialAnalysis chainAnalysis(int tierIndex, boolean difficult) {
        Map<String, Double> properties = difficult
                ? Map.of(
                "bondstrength", 100.0D,
                "chemicalstability", 100.0D,
                "crystalstability", 100.0D,
                "reactivity", 0.0D,
                "polarity", 0.0D,
                "acidity", 20.0D
        )
                : Map.of(
                "bondstrength", 0.0D,
                "chemicalstability", 0.0D,
                "crystalstability", 0.0D,
                "reactivity", 0.0D,
                "polarity", 100.0D,
                "acidity", -20.0D
        );
        MaterialSnapshot source = new MaterialSnapshot(
                "self_test_chain_" + tierIndex + "_" + difficult,
                "Self Test Chain",
                0x808080,
                ChemistryPhase.SOLID,
                List.of(
                        new CompositionEntry("component_a", 1, ChemistryPhase.SOLID),
                        new CompositionEntry("component_b", 1, ChemistryPhase.SOLID)
                ),
                Optional.of(ChemicalStructure.builder(ChemicalStructure.Topology.NETWORK).build()),
                Map.of(),
                Set.of(),
                tierIndex,
                "T" + tierIndex,
                Set.of(),
                null
        );
        return new MaterialAnalysis(
                source,
                new DerivedMaterialProperties(properties, 0x808080),
                Set.of(MaterialClassification.MOLECULAR_COMPOUND),
                ChemistryPhase.SOLID,
                tierIndex,
                "T" + tierIndex,
                List.of()
        );
    }

    private static ProcessMaterial material(
            String id,
            ChemistryPhase phase,
            long milliUnits,
            ProcessSubstanceState state,
            Map<String, Double> properties
    ) {
        return new ProcessMaterial(id, phase, milliUnits, 1, "LV", true, null, null, state, properties);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Chemistry self-test failed: " + message);
    }
}
