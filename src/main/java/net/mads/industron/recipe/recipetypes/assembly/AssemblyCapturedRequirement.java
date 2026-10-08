package net.mads.industron.recipe.recipetypes.assembly;

import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.material.MaterialLookup;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.tool.ToolMaterialStatCalculator;
import net.mads.industron.tool.ToolMaterialResolver;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;

/**
 * Cross-input numeric requirement. The candidate input stat is compared with a stat from
 * an earlier captured material input. Tool assembly uses this for rivet load vs. head strength.
 */
public record AssemblyCapturedRequirement(
        AssemblyCapability candidateCapability,
        Kind kind,
        String captureRole,
        AssemblyCapability sourceCapability
) {
    public enum Kind { AT_LEAST_CAPTURE, AT_MOST_CAPTURE, SAME_MATERIAL }

    public AssemblyCapturedRequirement {
        Objects.requireNonNull(kind, "kind");
        if (captureRole == null || captureRole.isBlank()) {
            throw new IllegalArgumentException("Captured requirement role cannot be blank");
        }
        if (kind == Kind.SAME_MATERIAL) {
            if (candidateCapability != null || sourceCapability != null) {
                throw new IllegalArgumentException("Same-material capture must not define numeric capabilities");
            }
        } else {
            Objects.requireNonNull(candidateCapability, "candidateCapability");
            Objects.requireNonNull(sourceCapability, "sourceCapability");
            if (candidateCapability.isRangeStat() || sourceCapability.isRangeStat()) {
                throw new IllegalArgumentException("Captured comparisons require scalar stats");
            }
        }
    }

    public static AssemblyCapturedRequirement atLeast(
            AssemblyCapability candidate,
            String captureRole,
            AssemblyCapability source
    ) {
        return new AssemblyCapturedRequirement(candidate, Kind.AT_LEAST_CAPTURE, captureRole, source);
    }

    public static AssemblyCapturedRequirement atMost(
            AssemblyCapability candidate,
            String captureRole,
            AssemblyCapability source
    ) {
        return new AssemblyCapturedRequirement(candidate, Kind.AT_MOST_CAPTURE, captureRole, source);
    }

    public static AssemblyCapturedRequirement sameMaterial(String captureRole) {
        return new AssemblyCapturedRequirement(null, Kind.SAME_MATERIAL, captureRole, null);
    }

    public boolean matches(ItemStack candidateStack, Map<String, IndustrialSubstance> capturedMaterials) {
        IndustrialSubstance sourceMaterial = capturedMaterials.get(captureRole);
        if (sourceMaterial == null || candidateStack == null || candidateStack.isEmpty()) return false;

        MaterialLookup.MaterialTarget candidate = MaterialLookup.find(candidateStack);
        if (candidate == null) return false;

        if (kind == Kind.SAME_MATERIAL) {
            return ToolMaterialResolver.key(candidate.material()).equals(ToolMaterialResolver.key(sourceMaterial));
        }

        OptionalDouble candidateValue = resolveIndustrial(candidateCapability, candidate.material(), candidate.part());
        OptionalDouble sourceValue = resolveSource(sourceCapability, sourceMaterial);
        if (candidateValue.isEmpty() || sourceValue.isEmpty()) return false;

        return switch (kind) {
            case AT_LEAST_CAPTURE -> candidateValue.getAsDouble() >= sourceValue.getAsDouble();
            case AT_MOST_CAPTURE -> candidateValue.getAsDouble() <= sourceValue.getAsDouble();
            case SAME_MATERIAL -> throw new IllegalStateException("Handled above");
        };
    }

    private static OptionalDouble resolveIndustrial(
            AssemblyCapability capability,
            IndustrialMaterial material,
            MaterialPart part
    ) {
        if (capability.isPracticalStat()) {
            return PracticalCapabilityResolver.resolveScalar(
                    capability,
                    new MaterialLookup.MaterialTarget(material, part)
            );
        }
        if (!capability.isAvailable(material.properties())) return OptionalDouble.empty();
        return OptionalDouble.of(capability.rawScalar(material.properties()));
    }

    private static OptionalDouble resolveSource(AssemblyCapability capability, IndustrialSubstance material) {
        // A captured tool part is not itself a fastener, but for a FASTENER_LOAD comparison
        // it supplies the minimum load any individual rivet must survive. Use the same
        // structural/tensile/yield failure envelope as the rivet-side practical stat.
        if (capability == AssemblyCapability.FASTENER_LOAD) {
            return OptionalDouble.of(ToolMaterialStatCalculator.fastenerRequirement(material));
        }
        if (!(material instanceof IndustrialMaterial industrial)) return OptionalDouble.empty();
        if (capability.isPracticalStat() || !capability.isAvailable(industrial.properties())) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(capability.rawScalar(industrial.properties()));
    }

    public Component tooltip() {
        if (kind == Kind.SAME_MATERIAL) {
            return Component.literal("Same material as " + captureRole);
        }
        String operator = kind == Kind.AT_LEAST_CAPTURE ? ">=" : "<=";
        return Component.literal(candidateCapability.displayName() + " " + operator + " "
                + captureRole + " " + sourceCapability.displayName());
    }
}
