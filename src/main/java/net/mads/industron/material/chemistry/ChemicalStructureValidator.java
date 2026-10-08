package net.mads.industron.material.chemistry;

import net.mads.industron.material.ElementDefinition;
import net.mads.industron.material.IndustrialMaterial;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Phase 10 validation bridge between atom/bond graphs and material atomic data. */
public final class ChemicalStructureValidator {
    public record Result(List<ChemistryDiagnostic> diagnostics) {
        public Result {
            diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
        }

        public boolean valid() {
            return diagnostics.stream().noneMatch(ChemistryDiagnostic::blocksRecipeGeneration);
        }
    }

    private ChemicalStructureValidator() {
    }

    public static Result validate(MaterialSnapshot material, Map<String, MaterialSnapshot> registry) {
        if (material == null || material.structure().isEmpty()) return new Result(List.of());
        ChemicalStructure structure = material.structure().orElseThrow();
        List<ChemistryDiagnostic> diagnostics = new ArrayList<>();

        Map<String, Double> usedOrders = new LinkedHashMap<>();
        for (ChemicalBond bond : structure.bonds()) {
            usedOrders.merge(bond.firstAtom(), bond.order().value(), Double::sum);
            usedOrders.merge(bond.secondAtom(), bond.order().value(), Double::sum);
        }

        for (ChemicalAtom atom : structure.atoms()) {
            MaterialSnapshot element = registry.get(atom.elementId());
            if (element == null) {
                diagnostics.add(new ChemistryDiagnostic(
                        ChemistryStatus.MISSING_MATERIAL,
                        material.id(),
                        "UNKNOWN_STRUCTURE_ATOM",
                        "ChemicalStructure atom " + atom.id() + " references missing element " + atom.elementId() + ".",
                        List.of("Register the referenced element/material or correct the atom id."),
                        ""
                ));
                continue;
            }

            double capacity = bondingCapacity(element);
            double used = usedOrders.getOrDefault(atom.id(), 0.0D);
            if (used > capacity + 0.0001D) {
                diagnostics.add(new ChemistryDiagnostic(
                        ChemistryStatus.CHANGE_REQUIRED,
                        material.id(),
                        "BOND_CAPACITY_EXCEEDED",
                        "Atom " + atom.id() + " of " + atom.elementId() + " uses bond order " + used
                                + " but calculated capacity is " + capacity + ".",
                        List.of("Lower the bond order/count or change the element's atomic model/properties."),
                        ""
                ));
            }
        }

        return new Result(diagnostics);
    }

    private static double bondingCapacity(MaterialSnapshot element) {
        Object backing = element.backingMaterial();
        if (backing instanceof ElementDefinition definition) {
            return Math.max(1, definition.atomicState().unpairedElectrons());
        }
        if (backing instanceof IndustrialMaterial material && material.atomicNumber() > 0) {
            return Math.max(1, material.properties().unpairedElectrons());
        }
        double tendency = Math.max(
                element.property("electrondonationtendency"),
                element.property("electronacceptancetendency")
        );
        double bondStrength = element.property("bondstrength");
        return Math.max(1.0D, Math.round((tendency + bondStrength) / 45.0D));
    }
}
