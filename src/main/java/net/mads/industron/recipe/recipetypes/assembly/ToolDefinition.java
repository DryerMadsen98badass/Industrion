package net.mads.industron.recipe.recipetypes.assembly;

import net.mads.industron.material.MaterialPart;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * One public tool family plus the permanent parts that compose it.
 *
 * <p>The definition is material-agnostic. Material identity lives on the finished ItemStack,
 * so one registered item can represent every valid material combination.</p>
 */
public final class ToolDefinition {
    public record PartSlot(String role, MaterialPart part, int layer) {
        public PartSlot {
            if (role == null || role.isBlank()) throw new IllegalArgumentException("Tool part role cannot be blank");
            if (part == null) throw new IllegalArgumentException("Tool part cannot be null");
            if (layer < 0) throw new IllegalArgumentException("Tool texture layer cannot be negative");
        }
    }

    /** Fixed non-material component that is rendered as part of the completed tool. */
    public record FixedPartSlot(String role, ResourceLocation itemId, int layer) {
        public FixedPartSlot {
            if (role == null || role.isBlank()) throw new IllegalArgumentException("Fixed tool part role cannot be blank");
            if (itemId == null) throw new IllegalArgumentException("Fixed tool part item cannot be null");
            if (layer < 0) throw new IllegalArgumentException("Tool texture layer cannot be negative");
        }
    }

    private final AssemblyToolType type;
    private final List<PartSlot> parts;
    private final List<FixedPartSlot> fixedParts;
    private final String baseRole;
    private final String strengthReferenceRole;
    private final boolean directPartTool;
    private final boolean finishedToolEnabled;
    private final float renderOffsetXPixels;
    private final float renderOffsetYPixels;
    private final ForgeFormula forgeFormula;

    private ToolDefinition(
            String id,
            String displayName,
            List<PartSlot> parts,
            List<FixedPartSlot> fixedParts,
            String baseRole,
            String strengthReferenceRole,
            boolean directPartTool,
            boolean finishedToolEnabled,
            float renderOffsetXPixels,
            float renderOffsetYPixels,
            ForgeFormula forgeFormula
    ) {
        this.type = new AssemblyToolType(id, displayName);
        this.parts = parts.stream().sorted(Comparator.comparingInt(PartSlot::layer)).toList();
        this.fixedParts = fixedParts.stream().sorted(Comparator.comparingInt(FixedPartSlot::layer)).toList();
        this.baseRole = baseRole;
        this.strengthReferenceRole = strengthReferenceRole;
        this.directPartTool = directPartTool;
        this.finishedToolEnabled = finishedToolEnabled;
        this.renderOffsetXPixels = renderOffsetXPixels;
        this.renderOffsetYPixels = renderOffsetYPixels;
        this.forgeFormula = forgeFormula;
    }

    public static Builder tool(String id, String displayName) {
        return new Builder(id, displayName);
    }

    public String id() { return type.id(); }
    public String displayName() { return type.displayName(); }
    public AssemblyToolType type() { return type; }
    public List<PartSlot> parts() { return parts; }
    public List<FixedPartSlot> fixedParts() { return fixedParts; }
    public String baseRole() { return baseRole; }
    public String strengthReferenceRole() { return strengthReferenceRole; }
    public boolean isDirectPartTool() { return directPartTool; }
    public boolean isAssembledTool() { return !directPartTool; }
    /** True when the completed tool item is currently part of gameplay/registration. */
    public boolean isFinishedToolEnabled() { return finishedToolEnabled; }
    public float renderOffsetXPixels() { return renderOffsetXPixels; }
    public float renderOffsetYPixels() { return renderOffsetYPixels; }
    /** Hidden arithmetic operation used by manual anvil forging. Never expose this in tooltips/JEI. */
    public String forgeFormula() { return forgeFormula == null ? null : forgeFormula.source(); }
    public boolean canForgeOnAnvil() { return forgeFormula != null; }
    public java.util.OptionalInt applyForgeFormula(int current) {
        return forgeFormula == null ? java.util.OptionalInt.empty() : forgeFormula.apply(current);
    }

    public PartSlot part(String role) {
        if (role == null) return null;
        return parts.stream().filter(slot -> slot.role().equals(role)).findFirst().orElse(null);
    }

    public PartSlot basePart() { return part(baseRole); }
    public PartSlot strengthReferencePart() { return part(strengthReferenceRole); }

    public static final class Builder {
        private final String id;
        private final String displayName;
        private final List<PartSlot> parts = new ArrayList<>();
        private final List<FixedPartSlot> fixedParts = new ArrayList<>();
        private String baseRole;
        private String strengthReferenceRole;
        private boolean directPartTool;
        private boolean finishedToolEnabled = true;
        private float renderOffsetXPixels;
        private float renderOffsetYPixels;
        private ForgeFormula forgeFormula;

        private Builder(String id, String displayName) {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("Tool id cannot be blank");
            if (displayName == null || displayName.isBlank()) {
                throw new IllegalArgumentException("Tool display name cannot be blank");
            }
            this.id = id;
            this.displayName = displayName;
        }

        public Builder part(String role, MaterialPart part, int layer) {
            parts.add(new PartSlot(role, part, layer));
            return this;
        }

        /** Fixed item component rendered with the completed tool but not used for material-derived stats. */
        public Builder fixedPart(String role, String itemId, int layer) {
            ResourceLocation parsed = ResourceLocation.tryParse(itemId);
            if (parsed == null) throw new IllegalArgumentException("Invalid fixed tool part item: " + itemId);
            fixedParts.add(new FixedPartSlot(role, parsed, layer));
            return this;
        }

        /** Part placed on the assembly workbench before the remaining inputs are added. */
        public Builder basePart(String role) {
            this.baseRole = role;
            return this;
        }

        /** Working part whose material strength sets the minimum rivet FASTENER_LOAD. */
        public Builder strengthReference(String role) {
            this.strengthReferenceRole = role;
            return this;
        }

        /**
         * Keeps the material parts active, but does not register the completed tool yet.
         * Use this for planned tools whose parts already belong to material/casting progression.
         */
        public Builder finishedToolUnavailable() {
            this.finishedToolEnabled = false;
            return this;
        }

        /**
         * Hidden manual-forging expression. The variable y is the current shape state; decimals are
         * kept during evaluation and discarded only after the complete expression.
         */
        public Builder forgeFormula(String expression) {
            this.forgeFormula = ForgeFormula.compile(expression);
            return this;
        }

        /** Optional visual centering correction for the finished composed item, in texture pixels. */
        public Builder renderOffsetPixels(float x, float y) {
            this.renderOffsetXPixels = x;
            this.renderOffsetYPixels = y;
            return this;
        }

        /** One cast/material part is itself the completed tool; no ToolAssemblyRecipe is generated. */
        public Builder directPartTool() {
            this.directPartTool = true;
            return this;
        }

        public ToolDefinition build() {
            if (parts.isEmpty()) throw new IllegalStateException("Tool family " + id + " has no permanent parts");

            Set<String> roles = new HashSet<>();
            Set<String> materialRoles = new HashSet<>();
            Set<Integer> layers = new HashSet<>();
            for (PartSlot part : parts) {
                if (!roles.add(part.role())) throw new IllegalStateException("Duplicate tool role in " + id + ": " + part.role());
                materialRoles.add(part.role());
                if (!layers.add(part.layer())) throw new IllegalStateException("Duplicate tool texture layer in " + id + ": " + part.layer());
            }
            for (FixedPartSlot part : fixedParts) {
                if (!roles.add(part.role())) throw new IllegalStateException("Duplicate tool role in " + id + ": " + part.role());
                if (!layers.add(part.layer())) throw new IllegalStateException("Duplicate tool texture layer in " + id + ": " + part.layer());
            }

            if (directPartTool) {
                if (!fixedParts.isEmpty()) throw new IllegalStateException("Direct-part tool " + id + " cannot contain fixed extra parts");
                if (parts.size() != 1) throw new IllegalStateException("Direct-part tool " + id + " must contain exactly one part");
                if (baseRole == null) baseRole = parts.get(0).role();
                if (strengthReferenceRole == null) strengthReferenceRole = parts.get(0).role();
            } else {
                if (baseRole == null || !materialRoles.contains(baseRole)) {
                    throw new IllegalStateException("Assembled tool " + id + " needs a valid material base part role");
                }
                if (strengthReferenceRole == null || !materialRoles.contains(strengthReferenceRole)) {
                    throw new IllegalStateException("Assembled tool " + id + " needs a valid strength-reference role");
                }
            }

            return new ToolDefinition(
                    id,
                    displayName,
                    List.copyOf(parts),
                    List.copyOf(fixedParts),
                    baseRole,
                    strengthReferenceRole,
                    directPartTool,
                    finishedToolEnabled,
                    renderOffsetXPixels,
                    renderOffsetYPixels,
                    forgeFormula
            );
        }
    }
}
