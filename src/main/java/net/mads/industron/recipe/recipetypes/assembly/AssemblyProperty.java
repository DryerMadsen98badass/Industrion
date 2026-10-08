package net.mads.industron.recipe.recipetypes.assembly;

import net.mads.industron.material.MaterialProperties;

import java.util.Objects;
import java.util.function.Function;

/**
 * Typed non-numeric material property that can be required by assembly recipes
 * and component definitions.
 *
 * <pre>
 * Stats.STATE.is(MaterialProperties.PhysicalState.SOLID)
 * Stats.METAL.is(true)
 * Stats.ELECTRONIC_FAMILY.is(MaterialProperties.ElectronicFamily.TRANSITION_MIDDLE)
 * </pre>
 */
public final class AssemblyProperty<T> {
    private final String displayName;
    private final String propertyId;
    private final Function<MaterialProperties, T> getter;

    public AssemblyProperty(String displayName, Function<MaterialProperties, T> getter) {
        this(displayName, null, getter);
    }

    public AssemblyProperty(String displayName, String propertyId, Function<MaterialProperties, T> getter) {
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.propertyId = propertyId;
        this.getter = Objects.requireNonNull(getter, "getter");
    }

    public String displayName() {
        return displayName;
    }

    public AssemblyRequirement is(T expected) {
        return AssemblyRequirement.is(this, Objects.requireNonNull(expected, "expected"));
    }

    boolean matches(MaterialProperties properties, Object expected) {
        return properties != null
                && (propertyId == null || properties.hasProperty(propertyId))
                && Objects.equals(getter.apply(properties), expected);
    }
}
