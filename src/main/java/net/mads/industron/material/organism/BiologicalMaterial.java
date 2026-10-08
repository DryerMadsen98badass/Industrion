package net.mads.industron.material.organism;

import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.material.MaterialComponent;
import net.mads.industron.material.MaterialFormulaFormatter;
import java.util.*;

/** Immutable composition source shared by organisms and processing. No new chemistry universe. */
public final class BiologicalMaterial implements IndustrialSubstance {
    private final String id, name;
    private final int color;
    private final List<MaterialComponent> components;
    private final Set<OrganicRole> roles;

    public BiologicalMaterial(String id, String name, int color, Set<OrganicRole> roles,
                              List<MaterialComponent> components) {
        if (id == null || !id.matches("[a-z0-9_]+")) throw new IllegalArgumentException("Invalid material id: " + id);
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Missing material name");
        if ((color & 0xFF000000) != 0) throw new IllegalArgumentException("Color must be RGB");
        if (components == null || components.isEmpty()) throw new IllegalArgumentException(id + " needs .contains(...)");
        this.id = id; this.name = name; this.color = color;
        this.roles = Set.copyOf(roles);
        this.components = List.copyOf(components);
        Set<String> seen = new HashSet<>();
        long sum = 0;
        for (MaterialComponent c : this.components) {
            if (c.substance().id().equals(id)) throw new IllegalArgumentException("Self composition: " + id);
            if (!seen.add(c.substance().id())) throw new IllegalArgumentException("Repeated component: " + c.substance().id());
            sum += c.amount();
        }
        if (sum > Integer.MAX_VALUE) throw new IllegalArgumentException("Composition too large: " + id);
    }
    public List<MaterialComponent> components() { return components; }
    public Set<OrganicRole> roles() { return roles; }
    @Override public String id() { return id; }
    @Override public String displayName() { return name; }
    @Override public int color() { return color; }
    @Override public String formula() { return formula(false); }
    @Override public String formula(boolean nested) { return MaterialFormulaFormatter.compound(components, nested); }
    @Override public int componentTemperature() { return 20; }

    public static Builder biological(String id, String name) {
        return new Builder(id, name);
    }

    public static final class Builder {
        private final String id, name;
        private Integer color;
        private Set<OrganicRole> roles = Set.of();
        private List<MaterialComponent> components = List.of();

        private Builder(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public Builder color(int rgb) {
            this.color = rgb;
            return this;
        }

        public Builder roles(OrganicRole... roles) {
            this.roles = roles.length == 0 ? Set.of() : EnumSet.copyOf(List.of(roles));
            return this;
        }

        public Builder contains(MaterialComponent... values) {
            this.components = List.of(values);
            return this;
        }

        public BiologicalMaterial build() {
            if (color == null) throw new IllegalArgumentException(id + " needs .color(...)");
            return new BiologicalMaterial(id, name, color, roles, components);
        }
    }
}
