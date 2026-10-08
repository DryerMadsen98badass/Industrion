package net.mads.industron.material.organic;

import net.mads.industron.material.CompositionColor;
import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.material.MaterialComponent;
import net.mads.industron.material.MaterialFormulaFormatter;
import net.mads.industron.material.chemistry.ChemistryPhase;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Definition for an existing non-plant organic material that still participates in the same
 * deterministic composition-driven processing/fuel rules as plant materials.
 *
 * <p>This is intentionally not a second plant system. It covers materials such as charcoal whose
 * Minecraft item already exists, but which need an explicit Industron .contains(...) identity so
 * generated systems never fall back to item-name or hardcoded burn-time checks.</p>
 */
public final class OrganicMaterial implements IndustrialSubstance {
    private final String id;
    private final String displayName;
    private final ChemistryPhase phase;
    private final ResourceLocation existingForm;
    private final List<MaterialComponent> components;

    private OrganicMaterial(Builder builder) {
        this.id = requireText(builder.id, "id");
        this.displayName = requireText(builder.displayName, "displayName");
        this.phase = Objects.requireNonNull(builder.phase, "phase");
        this.existingForm = Objects.requireNonNull(builder.existingForm, "existingForm");
        this.components = List.copyOf(builder.components);
        if (components.isEmpty()) {
            throw new IllegalArgumentException("Organic material must declare .contains(...): " + id);
        }
    }

    public static Builder organic(String id, String displayName) {
        return new Builder(id, displayName);
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String displayName() {
        return displayName;
    }

    public ChemistryPhase phase() {
        return phase;
    }

    public ResourceLocation existingForm() {
        return existingForm;
    }

    public List<MaterialComponent> components() {
        return components;
    }

    @Override
    public int color() {
        return CompositionColor.blend(components);
    }

    @Override
    public String formula() {
        return formula(false);
    }

    @Override
    public String formula(boolean nested) {
        return MaterialFormulaFormatter.compound(components, nested);
    }

    @Override
    public int componentTemperature() {
        return 20;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Organic material " + name + " cannot be blank");
        }
        return value;
    }

    public static final class Builder {
        private final String id;
        private final String displayName;
        private ChemistryPhase phase = ChemistryPhase.SOLID;
        private ResourceLocation existingForm;
        private final List<MaterialComponent> components = new ArrayList<>();

        private Builder(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public Builder phase(ChemistryPhase phase) {
            this.phase = Objects.requireNonNull(phase, "phase");
            return this;
        }

        public Builder existing(String id) {
            return existing(ResourceLocation.parse(id));
        }

        public Builder existing(ResourceLocation id) {
            this.existingForm = Objects.requireNonNull(id, "id");
            return this;
        }

        public Builder contains(MaterialComponent... components) {
            this.components.clear();
            if (components != null) {
                for (MaterialComponent component : components) {
                    if (component == null || component.amount() <= 0) {
                        throw new IllegalArgumentException("Organic .contains(...) entries must be positive");
                    }
                    this.components.add(component);
                }
            }
            return this;
        }

        public OrganicMaterial build() {
            return new OrganicMaterial(this);
        }
    }
}
