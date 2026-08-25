package net.mads.industron.recipe.recipetypes;

import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.MaterialLookup;
import net.mads.industron.material.MaterialPart;
import net.minecraft.network.chat.Component;

import java.util.Objects;

/**
 * Relative numeric requirement used only by free/ANY steps inside a ComponentDefinition.
 * The candidate material form is compared with the same form made from the inherited
 * parent material.
 */
public record AssemblyRelativeRequirement(
        AssemblyCapability capability,
        Kind kind
) {
    public enum Kind { AT_LEAST_PARENT, AT_MOST_PARENT }

    public AssemblyRelativeRequirement {
        Objects.requireNonNull(capability, "capability");
        Objects.requireNonNull(kind, "kind");
        if (capability.isRangeStat()) {
            throw new IllegalArgumentException(
                    capability.displayName() + " is a range capability and cannot be compared to a parent material"
            );
        }
    }

    static AssemblyRelativeRequirement atLeastParent(AssemblyCapability capability) {
        return new AssemblyRelativeRequirement(capability, Kind.AT_LEAST_PARENT);
    }

    static AssemblyRelativeRequirement atMostParent(AssemblyCapability capability) {
        return new AssemblyRelativeRequirement(capability, Kind.AT_MOST_PARENT);
    }

    public Component tooltip() {
        return Component.literal(switch (kind) {
            case AT_LEAST_PARENT -> capability.displayName() + ": >= parent material";
            case AT_MOST_PARENT -> capability.displayName() + ": <= parent material";
        });
    }

    /** Resolves the relative comparison to the normal absolute requirement used by runtime/JEI. */
    public AssemblyRequirement resolveAgainst(IndustrialMaterial parentMaterial, MaterialPart part) {
        Objects.requireNonNull(parentMaterial, "parentMaterial");
        Objects.requireNonNull(part, "part");

        var value = PracticalCapabilityResolver.resolveScalar(
                capability,
                new MaterialLookup.MaterialTarget(parentMaterial, part)
        );
        if (value.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot resolve parent " + capability.displayName()
                            + " for " + parentMaterial.id()
                            + " Material." + part.name()
            );
        }

        return switch (kind) {
            case AT_LEAST_PARENT -> capability.atLeast(value.getAsDouble());
            case AT_MOST_PARENT -> capability.atMost(value.getAsDouble());
        };
    }
}
