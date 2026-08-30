package net.mads.industron.material;

import java.util.List;

/**
 * Formats material formulas directly from .contains(...).
 *
 * <p>Normal compounds use component amounts as stoichiometric subscripts. Stone trace components
 * are chance weights, so StoneMaterial asks this formatter to omit the outer amounts while each
 * contained compound keeps its own internal formula.</p>
 */
public final class MaterialFormulaFormatter {
    private static final char[] SUBSCRIPT_DIGITS = {'\u2080', '\u2081', '\u2082', '\u2083', '\u2084', '\u2085', '\u2086', '\u2087', '\u2088', '\u2089'};

    private MaterialFormulaFormatter() {
    }

    public static String compound(List<MaterialComponent> components, boolean nested) {
        return compound(components, nested, true);
    }

    public static String compound(List<MaterialComponent> components, boolean nested, boolean includeAmounts) {
        if (components == null || components.isEmpty()) return "";

        StringBuilder formula = new StringBuilder();
        for (MaterialComponent component : components) {
            IndustrialSubstance substance = component.substance();
            String componentFormula = substance.formula(true);
            if (componentFormula.isBlank()) componentFormula = substance.displayName();
            formula.append(componentFormula);
            if (includeAmounts && component.amount() > 1) {
                formula.append(subscript(component.amount()));
            }
        }

        String result = formula.toString();
        return nested ? "(" + result + ")" : result;
    }

    public static String subscript(int value) {
        if (value < 0) throw new IllegalArgumentException("Formula subscript cannot be negative: " + value);
        if (value < 10) return String.valueOf(SUBSCRIPT_DIGITS[value]);

        String digits = Integer.toString(value);
        StringBuilder result = new StringBuilder(digits.length());
        for (int i = 0; i < digits.length(); i++) {
            result.append(SUBSCRIPT_DIGITS[digits.charAt(i) - '0']);
        }
        return result.toString();
    }
}
