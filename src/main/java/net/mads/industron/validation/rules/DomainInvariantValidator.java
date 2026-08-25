package net.mads.industron.validation.rules;

import net.mads.industron.machine.MachineTier;
import net.mads.industron.material.ElementDefinition;
import net.mads.industron.material.MaterialProperties;
import net.mads.industron.material.MaterialPropertyCalculator;
import net.mads.industron.validation.ValidationCode;
import net.mads.industron.validation.ValidationCollector;
import net.mads.industron.validation.ValidationContext;
import net.mads.industron.validation.ValidationGraph;
import net.mads.industron.validation.ValidationReport;
import net.mads.industron.validation.ValidationRule;
import net.mads.industron.validation.ValidationSubsystem;

/**
 * Small automated domain tests that run without a world/server. They deliberately
 * test invariants instead of freezing balance numbers that are expected to evolve.
 */
public final class DomainInvariantValidator implements ValidationRule {
    private static final int[] REPRESENTATIVE_ATOMIC_NUMBERS = {
            1, 2, 11, 29, 32, 118, 1_024, 1_000_000, Integer.MAX_VALUE
    };

    @Override
    public void validate(ValidationContext context, ValidationCollector diagnostics) {
        for (int atomicNumber : REPRESENTATIVE_ATOMIC_NUMBERS) {
            ElementDefinition element = new ElementDefinition(
                    "validation_z" + atomicNumber,
                    "Validation Z" + atomicNumber,
                    "V" + atomicNumber,
                    atomicNumber,
                    MachineTier.ULV
            );
            MaterialProperties properties = MaterialPropertyCalculator.calculate(element);
            String subject = "atomic_number:" + atomicNumber;

            require(properties.protons() == atomicNumber, subject,
                    "proton count must equal atomic number", diagnostics);
            require(properties.electrons() == atomicNumber, subject,
                    "neutral electron count must equal atomic number", diagnostics);
            require(sum(properties.electronShells()) == atomicNumber, subject,
                    "electron shell sum must equal atomic number", diagnostics);
            require(properties.outerShellElectrons() >= 0
                            && properties.outerShellElectrons() <= properties.outerShellCapacity(),
                    subject, "outer-shell occupancy must fit its stable-shell capacity", diagnostics);
            require(properties.boilingPoint() > properties.meltingPoint(), subject,
                    "boiling point must be greater than melting point", diagnostics);
            require(properties.minChemicalRange() <= properties.maxChemicalRange(), subject,
                    "chemical compatibility minimum cannot exceed maximum", diagnostics);
            require(!(properties.metal() && properties.gemCandidate()), subject,
                    "metal and gem classifications must be mutually exclusive", diagnostics);
            require(properties.metal() == properties.electricallyConductive(), subject,
                    "metal must map exactly to electrical conductor", diagnostics);
            if (properties.metal()) {
                require(properties.electricalConductivity() > 0, subject,
                        "metal conductor must have positive electrical conductivity", diagnostics);
                require(properties.insulationStrength() == 0, subject,
                        "metal conductor cannot have insulation strength", diagnostics);
            } else {
                require(properties.electricalConductivity() == 0, subject,
                        "non-metal insulator cannot conduct electricity", diagnostics);
                require(properties.insulationStrength() > 0, subject,
                        "non-metal insulator must have positive insulation strength", diagnostics);
                require(MaterialPropertyCalculator.wireBaseAmps(properties) == 0, subject,
                        "non-metal insulator must have zero bare-wire amp capacity", diagnostics);
            }
            require((properties.baseColor() & 0xFF000000) == 0, subject,
                    "baseColor must remain a 24-bit RGB value", diagnostics);
        }

        ElementDefinition colorOverride = new ElementDefinition(
                "validation_color", "Validation Color", "Vc", 17, MachineTier.ULV
        ).color(0x123456);
        MaterialProperties overridden = MaterialPropertyCalculator.calculate(colorOverride);
        MaterialProperties natural = MaterialPropertyCalculator.calculate(new ElementDefinition(
                "validation_color_natural", "Validation Color Natural", "Vcn", 17, MachineTier.ULV
        ));
        require(
                overridden.baseColor() == 0x123456,
                "color_override",
                "ElementDefinition.color(...) must be the final baseColor",
                diagnostics
        );
        require(
                samePhysics(natural, overridden),
                "color_override",
                "ElementDefinition.color(...) must never change material physics",
                diagnostics
        );

        require(MaterialPropertyCalculator.stateForTemperatures(21, 100) == MaterialProperties.PhysicalState.SOLID,
                "ambient_state_boundary", "meltingPoint 21 C must be solid at 20 C", diagnostics);
        require(MaterialPropertyCalculator.stateForTemperatures(20, 100) == MaterialProperties.PhysicalState.LIQUID,
                "ambient_state_boundary", "meltingPoint 20 C must be liquid at 20 C", diagnostics);
        require(MaterialPropertyCalculator.stateForTemperatures(-10, 20) == MaterialProperties.PhysicalState.GAS,
                "ambient_state_boundary", "boilingPoint 20 C must be gas at 20 C", diagnostics);

        validateCycleDetector(diagnostics);
    }

    private static boolean samePhysics(MaterialProperties a, MaterialProperties b) {
        return a.tierMultiplier() == b.tierMultiplier()
                && a.protons() == b.protons()
                && a.neutrons() == b.neutrons()
                && a.electrons() == b.electrons()
                && a.electronShells().equals(b.electronShells())
                && a.density() == b.density()
                && a.hardness() == b.hardness()
                && a.tensileStrength() == b.tensileStrength()
                && a.meltingPoint() == b.meltingPoint()
                && a.boilingPoint() == b.boilingPoint()
                && a.thermalConductivity() == b.thermalConductivity()
                && a.electricalConductivity() == b.electricalConductivity()
                && a.insulationStrength() == b.insulationStrength()
                && a.electricalBehavior() == b.electricalBehavior()
                && a.corrosionResistance() == b.corrosionResistance()
                && a.chemicalStability() == b.chemicalStability()
                && a.structuralStrength() == b.structuralStrength()
                && a.state() == b.state()
                && a.metal() == b.metal()
                && a.gemCandidate() == b.gemCandidate();
    }

    private static long sum(Iterable<Integer> values) {
        long total = 0;
        for (int value : values) total += value;
        return total;
    }


    private static void validateCycleDetector(ValidationCollector diagnostics) {
        GraphNode a = new GraphNode("a");
        GraphNode b = new GraphNode("b");
        GraphNode c = new GraphNode("c");
        a.dependencies = java.util.List.of(b);
        b.dependencies = java.util.List.of(c);
        c.dependencies = java.util.List.of(a);

        ValidationCollector cycleDiagnostics = new ValidationCollector();
        ValidationGraph.detectCycles(
                java.util.List.of(a),
                GraphNode::id,
                node -> node.dependencies,
                ValidationSubsystem.FOUNDATION,
                ValidationCode.COMPONENT_CYCLE,
                cycleDiagnostics
        );
        ValidationReport cycleReport = cycleDiagnostics.build();
        require(
                cycleReport.errorCount() == 1,
                "validation_graph",
                "Cycle detector must report one diagnostic for a simple three-node cycle",
                diagnostics
        );

        c.dependencies = java.util.List.of();
        ValidationCollector acyclicDiagnostics = new ValidationCollector();
        ValidationGraph.detectCycles(
                java.util.List.of(a),
                GraphNode::id,
                node -> node.dependencies,
                ValidationSubsystem.FOUNDATION,
                ValidationCode.COMPONENT_CYCLE,
                acyclicDiagnostics
        );
        require(
                !acyclicDiagnostics.build().hasErrors(),
                "validation_graph",
                "Cycle detector must not report an acyclic graph",
                diagnostics
        );
    }

    private static final class GraphNode {
        private final String id;
        private java.util.List<GraphNode> dependencies = java.util.List.of();

        private GraphNode(String id) {
            this.id = id;
        }

        private String id() {
            return id;
        }
    }

    private static void require(
            boolean condition,
            String subject,
            String message,
            ValidationCollector diagnostics
    ) {
        if (!condition) {
            diagnostics.error(
                    ValidationSubsystem.FOUNDATION,
                    ValidationCode.DOMAIN_INVARIANT_FAILED,
                    subject,
                    message
            );
        }
    }
}
