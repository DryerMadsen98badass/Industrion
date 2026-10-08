package net.mads.industron.material;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Shared interpretation of .contains(...) amounts when they represent selection weights. */
public final class MaterialComponentWeights {
    private MaterialComponentWeights() {
    }

    public static int total(List<MaterialComponent> components) {
        int total = 0;
        for (MaterialComponent component : components) {
            total = Math.addExact(total, component.amount());
        }
        return total;
    }

    public static double percentage(MaterialComponent component, List<MaterialComponent> components) {
        int total = total(components);
        return total == 0 ? 0.0D : component.amount() * 100.0D / total;
    }

    /**
     * Converts positive weights to integer chances whose sum is exactly {@code scale}.
     * Largest-remainder rounding keeps the result deterministic and avoids losing chance points.
     */
    public static List<Integer> normalize(List<MaterialComponent> components, int scale) {
        if (components.isEmpty()) return List.of();
        if (scale < 1) throw new IllegalArgumentException("Chance scale must be positive");

        int total = total(components);
        List<Integer> chances = new ArrayList<>(components.size());
        List<Remainder> remainders = new ArrayList<>(components.size());
        int assigned = 0;
        for (int index = 0; index < components.size(); index++) {
            long scaled = (long) components.get(index).amount() * scale;
            int chance = (int) (scaled / total);
            chances.add(chance);
            assigned += chance;
            remainders.add(new Remainder(index, scaled % total));
        }

        remainders.sort(Comparator.comparingLong(Remainder::value).reversed()
                .thenComparingInt(Remainder::index));
        for (int index = 0; index < scale - assigned; index++) {
            int componentIndex = remainders.get(index).index();
            chances.set(componentIndex, chances.get(componentIndex) + 1);
        }
        return List.copyOf(chances);
    }

    private record Remainder(int index, long value) {
    }
}
