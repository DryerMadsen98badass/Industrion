package net.mads.industron.material.chemistry.process;

import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.chemistry.ChemicalStructure;
import net.mads.industron.material.chemistry.ChemistryPhase;

import java.util.List;
import java.util.Optional;

/**
 * Process-level physical/state rules shared by planning, validation and recipe emission.
 *
 * <p>Recipe IO limits describe storage only. These rules decide whether the transformation itself
 * is physically represented by the selected process.</p>
 */
public final class ProcessSemantics {
    private ProcessSemantics() {
    }

    public static Optional<String> validate(ProcessStep step) {
        return validate(step.kind(), step.inputs(), step.outputs(), step.requirements());
    }

    public static Optional<String> validate(
            ProcessKind kind,
            List<ProcessMaterial> inputs,
            List<ProcessMaterial> outputs,
            List<ProcessRequirement> requirements
    ) {
        Optional<String> basic = switch (kind) {
            case MIXING -> require(
                    inputs.size() >= 2
                            && outputs.size() == 1
                            && outputs.getFirst().isPhysicalMixture(),
                    "mixing requires at least two feeds and must produce an explicit physical-mixture state"
            );
            case ALLOYING -> require(
                    !inputs.isEmpty()
                            && outputs.size() == 1
                            && outputs.getFirst().substanceState().topology() == ChemicalStructure.Topology.METALLIC_LATTICE,
                    "alloying must produce an explicit metallic/alloy phase, not a generic physical mixture"
            );
            case CENTRIFUGING -> require(
                    inputs.size() == 1
                            && inputs.getFirst().isPhysicalMixture()
                            && supportsPhysicalSeparation(inputs.getFirst(), outputs)
                            && propertySpread(outputs, "density") >= 12.0D,
                    "centrifuging requires a physical mixture with phase-compatible fractions and sufficient density contrast"
            );
            case MAGNETIC_SEPARATION -> require(
                    inputs.size() == 1
                            && inputs.getFirst().isPhysicalMixture()
                            && all(inputs, ProcessSemantics::isSolid)
                            && all(outputs, ProcessSemantics::isSolid)
                            && any(outputs, ProcessSemantics::isMagnetic)
                            && any(outputs, value -> !isMagnetic(value)),
                    "magnetic separation requires a physical solid mixture with distinct magnetic/non-magnetic fractions"
            );
            case DISTILLATION -> require(
                    inputs.size() == 1
                            && inputs.getFirst().isPhysicalMixture()
                            && isCondensedFluid(inputs.getFirst())
                            && outputs.size() >= 2
                            && all(outputs, ProcessSemantics::isFluidOrGas)
                            && supportsDistillation(outputs),
                    "distillation requires a physical liquid/molten mixture with separable boiling/volatility behaviour"
            );
            case FRACTIONATION -> require(
                    inputs.size() == 1
                            && inputs.getFirst().isPhysicalMixture()
                            && isCondensedFluid(inputs.getFirst())
                            && outputs.size() >= 2
                            && all(outputs, ProcessSemantics::isFluidOrGas)
                            && supportsFractionation(outputs),
                    "fractionation requires a physical liquid/molten mixture with close but distinguishable boiling/volatility behaviour"
            );
            case GAS_SEPARATION -> require(
                    inputs.size() == 1
                            && inputs.getFirst().isPhysicalMixture()
                            && all(inputs, ProcessSemantics::isGas)
                            && outputs.size() >= 2
                            && all(outputs, ProcessSemantics::isGas),
                    "gas separation requires a physical gas mixture and gas fractions"
            );
            case CRYSTALLIZATION -> require(
                    inputs.size() == 1
                            && inputs.getFirst().isPhysicalMixture()
                            && isCondensedFluid(inputs.getFirst())
                            && !outputs.isEmpty()
                            && all(outputs, ProcessSemantics::isSolid),
                    "crystallization requires one condensed physical-mixture feed and solid crystalline products"
            );
            case PHASE_SEPARATION -> require(
                    inputs.size() == 1
                            && inputs.getFirst().isPhysicalMixture()
                            && (inputs.getFirst().phase() == ChemistryPhase.MIXED
                            || inputs.getFirst().substanceState().mixtureKind() == ProcessSubstanceState.MixtureKind.PHASE_SEPARATED)
                            && outputs.size() >= 2
                            && outputs.stream().allMatch(value -> value.phase() != ChemistryPhase.UNKNOWN),
                    "phase separation requires an explicitly phase-separated physical mixture and multiple defined fractions"
            );
            case SOLVENT_EXTRACTION -> require(
                    inputs.size() == 1
                            && inputs.getFirst().isPhysicalMixture()
                            && isCondensedFluid(inputs.getFirst())
                            && outputs.size() >= 2
                            && outputs.stream().allMatch(value -> value.phase() != ChemistryPhase.UNKNOWN),
                    "solvent extraction requires an explicit condensed physical-mixture carrier and multiple defined fractions"
            );
            case FILTRATION -> require(
                    inputs.size() == 1
                            && inputs.getFirst().isPhysicalMixture()
                            && inputs.getFirst().substanceState().mixtureKind() == ProcessSubstanceState.MixtureKind.SUSPENSION
                            && any(outputs, ProcessSemantics::isSolid)
                            && any(outputs, ProcessSemantics::isCondensedFluid),
                    "filtration requires an explicit physical suspension and both solid and condensed-fluid products"
            );
            case DISSOLUTION, LEACHING -> require(
                    all(inputs, ProcessSemantics::isSolid)
                            && all(outputs, value -> isCondensedFluid(value) && value.isPhysicalMixture())
                            && requirements.stream().anyMatch(requirement ->
                            requirement.role() == ProcessRequirement.Role.CHEMICAL_BALANCE
                                    && requirement.constraints().stream().anyMatch(constraint ->
                                    constraint.property().equals("chemicalbalance"))),
                    "dissolution/leaching requires solid feed, a Chemical Balance (CB) environment and a physical liquid intermediate"
            );
            case ELECTROLYSIS, ELECTROREFINING, ELECTROWINNING -> Optional.empty();
            case MELTING -> require(
                    all(inputs, ProcessSemantics::isSolid)
                            && all(outputs, value -> value.phase() == ChemistryPhase.MOLTEN),
                    "melting requires solid input and molten output"
            );
            case CONDENSATION, LIQUEFACTION -> require(
                    all(inputs, ProcessSemantics::isGas)
                            && all(outputs, ProcessSemantics::isCondensedFluid),
                    "condensation/liquefaction requires gas input and condensed-fluid output"
            );
            case VAPORIZATION -> require(
                    all(inputs, ProcessSemantics::isCondensedFluid)
                            && all(outputs, ProcessSemantics::isGas),
                    "vaporization requires condensed-fluid input and gas output"
            );
            case FREEZING -> require(
                    all(inputs, ProcessSemantics::isCondensedFluid)
                            && all(outputs, ProcessSemantics::isSolid),
                    "freezing requires condensed-fluid input and solid output"
            );
            case ROASTING, CALCINATION, PYROLYSIS -> require(
                    all(inputs, ProcessSemantics::isSolid),
                    "thermal solid treatment requires solid input"
            );
            case SMELTING -> require(
                    all(inputs, ProcessSemantics::isSolid),
                    "smelting requires solid input"
            );
            case CHEMICAL_REACTION -> require(
                    !inputs.isEmpty() && !outputs.isEmpty(),
                    "chemical reaction executes an already planned transformation and requires explicit reactants/products"
            );
            default -> Optional.empty();
        };
        if (basic.isPresent()) return basic;

        if (kind == ProcessKind.ELECTROLYSIS
                || kind == ProcessKind.ELECTROREFINING
                || kind == ProcessKind.ELECTROWINNING) {
            return ElectrochemistrySemantics.validate(kind, inputs, outputs);
        }
        return Optional.empty();
    }

    /** Physical separators may expose existing fractions but may never open a bonded topology. */
    public static boolean supportsPhysicalSeparation(ProcessMaterial input, List<ProcessMaterial> outputs) {
        if (!input.isPhysicalMixture() || input.isBondedSubstance() || outputs.size() < 2) return false;
        if (input.phase() == ChemistryPhase.SOLID) {
            return all(outputs, ProcessSemantics::isSolid);
        }
        if (isGas(input)) {
            return all(outputs, ProcessSemantics::isGas);
        }
        if (input.phase() == ChemistryPhase.MIXED) {
            return outputs.stream().allMatch(value -> value.phase() != ChemistryPhase.UNKNOWN);
        }
        if (isCondensedFluid(input)) {
            return all(outputs, ProcessSemantics::isFluidOrGas)
                    || (any(outputs, ProcessSemantics::isSolid)
                    && any(outputs, ProcessSemantics::isCondensedFluid));
        }
        return false;
    }


    private static boolean supportsDistillation(List<ProcessMaterial> outputs) {
        if (outputs.stream().anyMatch(ProcessSemantics::isGas)) return true;
        double boilingSpread = propertySpread(outputs, "boilingpoint");
        if (boilingSpread >= 15.0D) return true;
        return propertySpread(outputs, "volatility") >= 30.0D;
    }

    private static boolean supportsFractionation(List<ProcessMaterial> outputs) {
        List<Double> boiling = propertyValues(outputs, "boilingpoint").stream().sorted().toList();
        if (boiling.size() >= 2) {
            double spread = boiling.getLast() - boiling.getFirst();
            double minimumGap = Double.POSITIVE_INFINITY;
            for (int index = 1; index < boiling.size(); index++) {
                minimumGap = Math.min(minimumGap, boiling.get(index) - boiling.get(index - 1));
            }
            if (spread >= 5.0D && minimumGap <= 35.0D) return true;
        }
        double volatility = propertySpread(outputs, "volatility");
        return volatility >= 8.0D && volatility < 30.0D;
    }

    private static double propertySpread(List<ProcessMaterial> values, String property) {
        List<Double> resolved = propertyValues(values, property);
        if (resolved.size() < 2) return 0.0D;
        double minimum = resolved.stream().mapToDouble(Double::doubleValue).min().orElse(0.0D);
        double maximum = resolved.stream().mapToDouble(Double::doubleValue).max().orElse(0.0D);
        return maximum - minimum;
    }

    private static List<Double> propertyValues(List<ProcessMaterial> values, String property) {
        java.util.ArrayList<Double> result = new java.util.ArrayList<>();
        for (ProcessMaterial value : values) {
            double resolved = value.processProperty(property);
            if (!Double.isFinite(resolved) && value.backingMaterial() instanceof IndustrialMaterial material) {
                resolved = switch (property) {
                    case "density" -> material.properties().hasProperty("density")
                            ? material.properties().density() : Double.NaN;
                    case "boilingpoint" -> material.properties().boilingPoint();
                    default -> Double.NaN;
                };
            }
            if (Double.isFinite(resolved)) result.add(resolved);
        }
        return List.copyOf(result);
    }

    private static boolean isMagnetic(ProcessMaterial value) {
        double magneticStrength = value.processProperty("magneticstrength");
        if (Double.isFinite(magneticStrength)) return magneticStrength > 0.0D;
        return value.backingMaterial() instanceof IndustrialMaterial material
                && material.properties().magnetic()
                && material.properties().magneticStrength() > 0;
    }

    private static Optional<String> require(boolean valid, String message) {
        return valid ? Optional.empty() : Optional.of(message);
    }

    private static boolean all(List<ProcessMaterial> values, java.util.function.Predicate<ProcessMaterial> predicate) {
        return !values.isEmpty() && values.stream().allMatch(predicate);
    }

    private static boolean any(List<ProcessMaterial> values, java.util.function.Predicate<ProcessMaterial> predicate) {
        return values.stream().anyMatch(predicate);
    }

    private static boolean isSolid(ProcessMaterial value) {
        return value.phase() == ChemistryPhase.SOLID;
    }

    private static boolean isGas(ProcessMaterial value) {
        return value.phase() == ChemistryPhase.GAS || value.phase() == ChemistryPhase.PLASMA;
    }

    private static boolean isCondensedFluid(ProcessMaterial value) {
        return value.phase() == ChemistryPhase.LIQUID || value.phase() == ChemistryPhase.MOLTEN;
    }

    private static boolean isFluidOrGas(ProcessMaterial value) {
        return isCondensedFluid(value) || isGas(value);
    }

    private static boolean isMixtureCarrier(ProcessMaterial value) {
        return value.phase() == ChemistryPhase.MIXED || isCondensedFluid(value) || isGas(value);
    }
}
