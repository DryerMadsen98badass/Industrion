package net.mads.industron.material.chemistry;

import java.util.LinkedHashMap;
import java.util.Map;

public record DerivedMaterialProperties(Map<String, Double> values, int color) {
    public DerivedMaterialProperties {
        values = Map.copyOf(values == null ? Map.of() : values);
        color &= 0xFFFFFF;
    }

    public double get(String id) {
        return values.getOrDefault(normalize(id), 0.0);
    }

    public DerivedMaterialProperties with(String id, double value) {
        Map<String, Double> copy = new LinkedHashMap<>(values);
        copy.put(normalize(id), finite(value));
        return new DerivedMaterialProperties(copy, color);
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static double finite(double value) {
        return Double.isFinite(value) ? value : 0.0;
    }
}
