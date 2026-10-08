package net.mads.industron.material.chemistry.process;

import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.chemistry.ChemicalStructure;
import net.mads.industron.material.chemistry.ChemistryPhase;

import java.util.List;
import java.util.Optional;

/** Ion/electron-aware validation shared by generated and runtime electrochemical routes. */
final class ElectrochemistrySemantics {
    private ElectrochemistrySemantics() {
    }

    static Optional<String> validate(ProcessKind kind, List<ProcessMaterial> inputs, List<ProcessMaterial> outputs) {
        boolean electrochemicalSignal = hasElectrochemicalSignal(inputs) || hasElectrochemicalSignal(outputs);
        boolean oxidationStateSignal = hasOxidationStateSignal(inputs) || hasOxidationStateSignal(outputs);

        return switch (kind) {
            case ELECTROLYSIS -> {
                boolean compatibleFeed = !inputs.isEmpty() && inputs.stream().allMatch(value ->
                        value.phase() == ChemistryPhase.LIQUID
                                || value.phase() == ChemistryPhase.MOLTEN
                                || (value.phase() == ChemistryPhase.SOLID
                                && value.substanceState().topology() == ChemicalStructure.Topology.IONIC_LATTICE));
                boolean ionicOrSolution = inputs.stream().anyMatch(value ->
                        value.substanceState().topology() == ChemicalStructure.Topology.IONIC_LATTICE
                                || value.substanceState().mixtureKind() == ProcessSubstanceState.MixtureKind.SOLUTION
                                || value.substanceState().mixtureKind() == ProcessSubstanceState.MixtureKind.REACTION_MIXTURE
                                || value.substanceState().mixtureKind() == ProcessSubstanceState.MixtureKind.MOLTEN_MIXTURE);
                if (!compatibleFeed) yield Optional.of("electrolysis requires liquid/molten feed or an explicitly ionic solid lattice");
                if (!ionicOrSolution) yield Optional.of("electrolysis requires ionic chemistry or an explicit solution/molten-mixture state");
                if (!electrochemicalSignal || !oxidationStateSignal) {
                    yield Optional.of("electrolysis requires ion/electrochemical data so electron transfer and oxidation-state change are defined");
                }
                yield Optional.empty();
            }
            case ELECTROREFINING -> {
                boolean molten = !inputs.isEmpty() && inputs.stream().allMatch(value -> value.phase() == ChemistryPhase.MOLTEN);
                boolean metallic = inputs.stream().anyMatch(value ->
                        value.substanceState().topology() == ChemicalStructure.Topology.METALLIC_LATTICE
                                || value.substanceState().mixtureKind() == ProcessSubstanceState.MixtureKind.MOLTEN_MIXTURE);
                boolean metallicOutputs = !outputs.isEmpty() && outputs.stream()
                        .filter(ProcessMaterial::guaranteed)
                        .allMatch(ElectrochemistrySemantics::isSolidMetal);
                if (!molten) yield Optional.of("electrorefining requires molten feed");
                if (!metallic) yield Optional.of("electrorefining requires a metallic lattice or molten physical metal mixture");
                if (!metallicOutputs) yield Optional.of("electrorefining guaranteed products must be solid metals");
                if (!electrochemicalSignal || !oxidationStateSignal) yield Optional.of("electrorefining requires electrochemical and oxidation-state data");
                yield Optional.empty();
            }
            case ELECTROWINNING -> {
                boolean solution = !inputs.isEmpty() && inputs.stream().allMatch(value ->
                        value.phase() == ChemistryPhase.LIQUID
                                && value.isPhysicalMixture()
                                && (value.substanceState().mixtureKind() == ProcessSubstanceState.MixtureKind.SOLUTION
                                || value.substanceState().mixtureKind() == ProcessSubstanceState.MixtureKind.REACTION_MIXTURE
                                || value.substanceState().mixtureKind() == ProcessSubstanceState.MixtureKind.LIQUID_MIXTURE));
                boolean metallicOutputs = !outputs.isEmpty() && outputs.stream()
                        .filter(ProcessMaterial::guaranteed)
                        .allMatch(ElectrochemistrySemantics::isSolidMetal);
                if (!solution) yield Optional.of("electrowinning requires an explicit metal-bearing liquid mixture/solution");
                if (!metallicOutputs) yield Optional.of("electrowinning guaranteed products must be solid metals");
                if (!electrochemicalSignal || !oxidationStateSignal) {
                    yield Optional.of("electrowinning requires ion/electrochemical data so reduction is defined");
                }
                yield Optional.empty();
            }
            default -> Optional.empty();
        };
    }

    private static boolean isSolidMetal(ProcessMaterial value) {
        if (value.phase() != ChemistryPhase.SOLID) return false;
        double metallicCharacter = value.processProperty("metalliccharacter");
        if (Double.isFinite(metallicCharacter) && metallicCharacter >= 50.0D) return true;
        return value.backingMaterial() instanceof IndustrialMaterial material && material.properties().metal();
    }

    private static boolean hasElectrochemicalSignal(List<ProcessMaterial> values) {
        for (ProcessMaterial value : values) {
            if (value.substanceState().topology() == ChemicalStructure.Topology.IONIC_LATTICE) return true;
            if (nonZero(value, "electrochemicalpotential")
                    || positive(value, "electrondonationtendency")
                    || positive(value, "electronacceptancetendency")) {
                return true;
            }
            if (value.backingMaterial() instanceof IndustrialMaterial material) {
                var properties = material.properties();
                if (properties.electrochemicalPotential() != 0
                        || properties.electronDonationTendency() > 0
                        || properties.electronAcceptanceTendency() > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasOxidationStateSignal(List<ProcessMaterial> values) {
        for (ProcessMaterial value : values) {
            if (value.substanceState().topology() == ChemicalStructure.Topology.IONIC_LATTICE) return true;
            if (nonZero(value, "preferredioncharge")) return true;
            if (value.backingMaterial() instanceof IndustrialMaterial material
                    && material.properties().preferredIonCharge() != 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean nonZero(ProcessMaterial value, String key) {
        double resolved = value.processProperty(key);
        return Double.isFinite(resolved) && Math.abs(resolved) > 1.0E-9D;
    }

    private static boolean positive(ProcessMaterial value, String key) {
        double resolved = value.processProperty(key);
        return Double.isFinite(resolved) && resolved > 0.0D;
    }
}
