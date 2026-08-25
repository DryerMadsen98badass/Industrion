package net.mads.industron.validation.rules;

import net.mads.industron.machine.MachineTier;
import net.mads.industron.material.ElementDefinition;
import net.mads.industron.material.MaterialProperties;
import net.mads.industron.material.MaterialPropertyCalculator;
import net.mads.industron.material.atomic.AtomicModel;
import net.mads.industron.material.atomic.AtomicState;
import net.mads.industron.material.atomic.IonState;
import net.mads.industron.validation.ValidationCode;
import net.mads.industron.validation.ValidationCollector;
import net.mads.industron.validation.ValidationContext;
import net.mads.industron.validation.ValidationRule;
import net.mads.industron.validation.ValidationSubsystem;

import java.util.List;
import java.util.Optional;

/** Phase-01 validation for neutral atomic identity and charged chemical states. */
public final class AtomicModelValidator implements ValidationRule {
    private static final int[] REPRESENTATIVE_ION_ATOMIC_NUMBERS = {
            1, 2, 6, 8, 11, 17, 29, 32, 118, 1_024, 1_000_000
    };

    @Override
    public void validate(ValidationContext context, ValidationCollector diagnostics) {
        for (ElementDefinition element : context.elements()) {
            validateElement(element, diagnostics);
        }

        for (int atomicNumber : REPRESENTATIVE_ION_ATOMIC_NUMBERS) {
            validateIonSet(atomicNumber, diagnostics);
        }

        validateTierIndependence(diagnostics);
        validateHighAtomicNumberChargeArithmetic(diagnostics);
    }

    private static void validateElement(ElementDefinition element, ValidationCollector diagnostics) {
        AtomicState atomic = element.atomicState();
        MaterialProperties material = MaterialPropertyCalculator.calculate(element);
        String subject = "element:" + element.id();

        require(atomic.protons() == material.protons(), subject,
                "Material properties must use the shared neutral atomic model (protons)", diagnostics);
        require(atomic.neutrons() == material.neutrons(), subject,
                "Material properties must use the shared neutral atomic model (neutrons)", diagnostics);
        require(atomic.electrons() == material.electrons(), subject,
                "Material properties must use the shared neutral atomic model (electrons)", diagnostics);
        require(atomic.shells().equals(material.electronShells()), subject,
                "Material properties must use the shared neutral electron-shell model", diagnostics);
        require(atomic.preferredIonCharge() == material.preferredIonCharge(), subject,
                "Material preferred ion charge must come from the shared atomic model", diagnostics);

        validateIonSet(element.atomicNumber(), diagnostics);
    }

    private static void validateIonSet(int atomicNumber, ValidationCollector diagnostics) {
        String subject = "atomic_number:" + atomicNumber;
        AtomicState neutral = AtomicModel.neutral(atomicNumber);
        List<IonState> first = AtomicModel.allowedIonStates(atomicNumber);
        List<IonState> second = AtomicModel.allowedIonStates(atomicNumber);

        require(first.equals(second), subject,
                "Allowed ion states must be deterministic and stably ordered", diagnostics);

        for (IonState ion : first) {
            require(ion.charge() != 0, subject,
                    "Allowed ion list cannot contain neutral charge", diagnostics);
            require(ion.chemicallyAllowed(), subject,
                    "Allowed ion list contained a state marked chemically disallowed", diagnostics);
            require(ion.atomicNumber() == atomicNumber, subject,
                    "Ion must preserve proton/element identity", diagnostics);
            require(ion.neutrons() == neutral.neutrons(), subject,
                    "Ionization must not change neutron count", diagnostics);
            require(ion.electronCount() == (long) atomicNumber - ion.charge(), subject,
                    "Ion electron count must equal Z - charge", diagnostics);
            require(sum(ion.electronShells()) == ion.electronCount(), subject,
                    "Ion electron shell sum must equal ion electron count", diagnostics);
            require(Math.abs((long) ion.charge()) <= AtomicModel.MAX_AUTOMATIC_ION_MAGNITUDE, subject,
                    "Automatic ion charge exceeded configured chemistry bound", diagnostics);
        }

        Optional<IonState> preferred = AtomicModel.preferredIonState(atomicNumber);
        if (neutral.preferredIonCharge() == 0) {
            require(preferred.isEmpty(), subject,
                    "Neutral-preferring atom must not expose a preferred charged state", diagnostics);
        } else {
            require(preferred.isPresent(), subject,
                    "Non-zero preferredIonCharge must resolve to a preferred IonState", diagnostics);
            preferred.ifPresent(ion -> {
                require(ion.charge() == neutral.preferredIonCharge(), subject,
                        "Preferred IonState charge disagrees with neutral preferredIonCharge", diagnostics);
                require(ion.chemicallyAllowed(), subject,
                        "Preferred IonState must be chemically allowed", diagnostics);
                require(first.contains(ion), subject,
                        "Preferred IonState must be part of allowedIonStates", diagnostics);
            });
        }
    }

    private static void validateTierIndependence(ValidationCollector diagnostics) {
        int atomicNumber = 29;
        ElementDefinition lowTier = new ElementDefinition(
                "tier_test_low", "Tier Test Low", "Ttl", atomicNumber, MachineTier.ULV
        );
        ElementDefinition highTier = new ElementDefinition(
                "tier_test_high", "Tier Test High", "Tth", atomicNumber, MachineTier.IV
        );

        require(lowTier.atomicState().equals(highTier.atomicState()), "tier_independence:z29",
                "Tier must not alter neutral atomic identity", diagnostics);
        require(lowTier.allowedIonStates().equals(highTier.allowedIonStates()), "tier_independence:z29",
                "Tier must not alter allowed ion states", diagnostics);

        MaterialProperties lowProperties = MaterialPropertyCalculator.calculate(lowTier);
        MaterialProperties highProperties = MaterialPropertyCalculator.calculate(highTier);
        require(lowProperties.electronShells().equals(highProperties.electronShells()), "tier_independence:z29",
                "Tier must not alter material electron shells", diagnostics);
        require(lowProperties.preferredIonCharge() == highProperties.preferredIonCharge(), "tier_independence:z29",
                "Tier must not alter preferred ion charge", diagnostics);
        require(lowProperties.electronicFamily() == highProperties.electronicFamily(), "tier_independence:z29",
                "Tier must not alter electronic family", diagnostics);
    }

    private static void validateHighAtomicNumberChargeArithmetic(ValidationCollector diagnostics) {
        int atomicNumber = Integer.MAX_VALUE;
        IonState cation = AtomicModel.ion(atomicNumber, 1);
        IonState anion = AtomicModel.ion(atomicNumber, -1);
        String subject = "atomic_number:" + atomicNumber;

        require(cation.electronCount() == 2_147_483_646L, subject,
                "High-Z cation electron arithmetic overflowed", diagnostics);
        require(anion.electronCount() == 2_147_483_648L, subject,
                "High-Z anion electron arithmetic must exceed signed int safely", diagnostics);
        require(sum(anion.electronShells()) == anion.electronCount(), subject,
                "High-Z anion shell accounting must remain exact", diagnostics);
    }

    private static long sum(Iterable<Integer> values) {
        long total = 0L;
        for (int value : values) {
            total += value;
        }
        return total;
    }

    private static void require(
            boolean condition,
            String subject,
            String message,
            ValidationCollector diagnostics
    ) {
        if (!condition) {
            diagnostics.error(
                    ValidationSubsystem.ATOMIC,
                    ValidationCode.INVALID_ION_STATE,
                    subject,
                    message
            );
        }
    }
}
