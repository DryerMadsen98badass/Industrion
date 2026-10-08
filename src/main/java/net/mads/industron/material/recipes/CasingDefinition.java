package net.mads.industron.material.recipes;

import net.mads.industron.machine.MachineTier;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyCapability;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyComponent;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyMaterialSelector;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyNumericStatBuilder;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyProperty;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyPropertyStatBuilder;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyRequirement;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyToolType;
import net.mads.industron.recipe.recipetypes.assembly.ToolDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Code-first template for material-derived casing families. */
public final class CasingDefinition {
    public enum InputKind { MATERIAL, COMPONENT, ITEM, TOOL, WAIT }

    public record Input(
            InputKind kind,
            MaterialPart material,
            AssemblyMaterialSelector materialOverride,
            AssemblyComponent component,
            ResourceLocation itemId,
            AssemblyToolType tool,
            int count,
            List<AssemblyRequirement> requirements,
            int waitTicks,
            SoundEvent sound
    ) {
        public Input {
            Objects.requireNonNull(kind, "kind");
            if (count < 1) throw new IllegalArgumentException("Casing input count must be >= 1");
            requirements = List.copyOf(requirements);
        }

        Input withRequirement(AssemblyRequirement requirement) {
            List<AssemblyRequirement> next = new ArrayList<>(requirements);
            next.add(Objects.requireNonNull(requirement));
            return new Input(kind, material, materialOverride, component, itemId, tool, count, next, waitTicks, sound);
        }

        Input withSound(SoundEvent value) {
            return new Input(kind, material, materialOverride, component, itemId, tool, count, requirements, waitTicks, value);
        }
    }

    private final String id;
    private final String displayName;
    private final ResourceLocation texture;
    private final MachineTier startTier;
    private final List<AssemblyRequirement> materialRequirements;
    private final MaterialPart baseBlockInput;
    private final List<Input> inputs;

    private CasingDefinition(
            String id,
            String displayName,
            ResourceLocation texture,
            MachineTier startTier,
            List<AssemblyRequirement> materialRequirements,
            MaterialPart baseBlockInput,
            List<Input> inputs
    ) {
        this.id = id;
        this.displayName = displayName;
        this.texture = texture;
        this.startTier = startTier;
        this.materialRequirements = List.copyOf(materialRequirements);
        this.baseBlockInput = baseBlockInput;
        this.inputs = List.copyOf(inputs);
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public ResourceLocation texture() { return texture; }
    public MachineTier startTier() { return startTier; }
    public List<AssemblyRequirement> materialRequirements() { return materialRequirements; }
    public MaterialPart baseBlockInput() { return baseBlockInput; }
    public List<Input> inputs() { return inputs; }

    public static Builder casing(String id) { return new Builder(id); }

    public static final class Builder {
        private final String id;
        private String displayName;
        private ResourceLocation texture;
        private MachineTier startTier;
        private final List<AssemblyRequirement> materialRequirements = new ArrayList<>();
        private MaterialPart baseBlockInput;
        private final List<Input> inputs = new ArrayList<>();
        private int lastStatInput = -1;

        private Builder(String id) {
            this.id = Objects.requireNonNull(id, "id");
            if (id.isBlank()) throw new IllegalArgumentException("Casing id cannot be blank");
        }

        public Builder displayName(String value) { displayName = Objects.requireNonNull(value); return this; }
        public Builder texture(String value) { texture = ResourceLocation.parse(Objects.requireNonNull(value)); return this; }

        public Builder tier(MachineTier value) {
            Objects.requireNonNull(value);
            if (!MachineTier.ALL.contains(value)) {
                throw new IllegalArgumentException("Casing tier must be an electric MachineTier: " + value.id());
            }
            startTier = value;
            return this;
        }

        public StatBuilder<Builder> materialStats(AssemblyCapability capability) {
            Objects.requireNonNull(capability);
            if (capability.isPracticalStat()) {
                throw new IllegalArgumentException("materialStats requires a raw material stat: " + capability.displayName());
            }
            return new StatBuilder<>(this, capability, materialRequirements::add);
        }

        public Builder baseBlockInput(MaterialPart material) {
            Objects.requireNonNull(material);
            if (!material.isBlock()) {
                throw new IllegalArgumentException("Casing baseBlockInput must be a block Material: " + material);
            }
            baseBlockInput = material;
            return this;
        }

        /** Material leaf using the generated casing material. */
        public Builder input(MaterialPart material) { return input(material, 1); }
        public Builder input(MaterialPart material, int count) {
            addInput(new Input(InputKind.MATERIAL, requireItemMaterial(material), null, null, null, null, count, List.of(), 0, null));
            return this;
        }

        /** Material leaf with a fixed/category/ANY override instead of the casing material. */
        public Builder input(MaterialPart material, AssemblyMaterialSelector selector) { return input(material, selector, 1); }
        public Builder input(MaterialPart material, AssemblyMaterialSelector selector, int count) {
            addInput(new Input(InputKind.MATERIAL, requireItemMaterial(material), Objects.requireNonNull(selector), null, null, null, count, List.of(), 0, null));
            return this;
        }

        /** Material leaf fixed directly to a canonical IndustrialMaterials identity. */
        public Builder input(MaterialPart material, IndustrialSubstance substance) { return input(material, substance, 1); }
        public Builder input(MaterialPart material, IndustrialSubstance substance, int count) {
            return input(material, AssemblyMaterialSelector.fixed(Objects.requireNonNull(substance)), count);
        }

        /** Component tree using the generated casing material. */
        public Builder input(AssemblyComponent component) { return input(component, 1); }
        public Builder input(AssemblyComponent component, int count) {
            addInput(new Input(InputKind.COMPONENT, null, null, Objects.requireNonNull(component), null, null, count, List.of(), 0, null));
            return this;
        }

        /** Component tree with a fixed/category/ANY override instead of the casing material. */
        public Builder input(AssemblyComponent component, AssemblyMaterialSelector selector) { return input(component, selector, 1); }
        public Builder input(AssemblyComponent component, AssemblyMaterialSelector selector, int count) {
            addInput(new Input(InputKind.COMPONENT, null, Objects.requireNonNull(selector), Objects.requireNonNull(component), null, null, count, List.of(), 0, null));
            return this;
        }

        /** Component tree fixed directly to a canonical IndustrialMaterials identity. */
        public Builder input(AssemblyComponent component, IndustrialSubstance substance) { return input(component, substance, 1); }
        public Builder input(AssemblyComponent component, IndustrialSubstance substance, int count) {
            return input(component, AssemblyMaterialSelector.fixed(Objects.requireNonNull(substance)), count);
        }

        public Builder input(String itemId) { return input(itemId, 1); }
        public Builder input(String itemId, int count) {
            addInput(new Input(InputKind.ITEM, null, null, null, ResourceLocation.parse(itemId), null, count, List.of(), 0, null));
            return this;
        }

        public Builder input(AssemblyToolType tool) {
            return tool(tool, 1);
        }

        public Builder input(ToolDefinition tool) {
            return tool(tool, 1);
        }

        public Builder tool(ToolDefinition tool, int amount) {
            return tool(Objects.requireNonNull(tool).type(), amount);
        }

        public Builder tool(AssemblyToolType tool, int amount) {
            addInput(new Input(InputKind.TOOL, null, null, null, null, Objects.requireNonNull(tool), amount, List.of(), 0, null));
            return this;
        }

        public Builder waitTicks(int ticks) {
            if (ticks < 1) throw new IllegalArgumentException("Wait must be >= 1 tick");
            addInput(new Input(InputKind.WAIT, null, null, null, null, null, 1, List.of(), ticks, null));
            return this;
        }

        public Builder waitSeconds(double seconds) {
            if (!(seconds > 0.0D)) throw new IllegalArgumentException("Wait must be > 0 seconds");
            return waitTicks(Math.max(1, (int) Math.round(seconds * 20.0D)));
        }

        public AssemblyNumericStatBuilder<Builder> stat(AssemblyCapability capability) {
            ensureStatTarget();
            return new AssemblyNumericStatBuilder<>(this, capability, this::addRequirementToLastInput);
        }

        public <T> AssemblyPropertyStatBuilder<T, Builder> stat(AssemblyProperty<T> property) {
            ensureStatTarget();
            return new AssemblyPropertyStatBuilder<>(this, property, this::addRequirementToLastInput);
        }

        public Builder sound(SoundEvent sound) {
            if (inputs.isEmpty()) throw new IllegalStateException("sound() needs a previous input");
            int index = inputs.size() - 1;
            inputs.set(index, inputs.get(index).withSound(Objects.requireNonNull(sound)));
            return this;
        }

        public CasingDefinition build() {
            if (displayName == null || displayName.isBlank()) throw new IllegalStateException("Casing definition requires displayName");
            if (texture == null) throw new IllegalStateException("Casing definition requires texture");
            if (startTier == null) throw new IllegalStateException("Casing definition requires tier");
            if (materialRequirements.isEmpty()) throw new IllegalStateException("Casing definition requires at least one materialStats requirement");
            if (baseBlockInput == null) throw new IllegalStateException("Casing definition requires baseBlockInput");
            if (inputs.isEmpty()) throw new IllegalStateException("Casing definition requires at least one assembly input");
            return new CasingDefinition(id, displayName, texture, startTier, materialRequirements, baseBlockInput, inputs);
        }

        private void addInput(Input input) {
            inputs.add(input);
            lastStatInput = switch (input.kind()) {
                case MATERIAL, COMPONENT -> inputs.size() - 1;
                case ITEM, TOOL, WAIT -> -1;
            };
        }

        private void ensureStatTarget() {
            if (lastStatInput < 0 || lastStatInput >= inputs.size()) {
                throw new IllegalStateException("stat() needs a previous Material or Component input");
            }
        }

        private void addRequirementToLastInput(AssemblyRequirement requirement) {
            ensureStatTarget();
            Input current = inputs.get(lastStatInput);
            inputs.set(lastStatInput, current.withRequirement(requirement));
        }

        private static MaterialPart requireItemMaterial(MaterialPart material) {
            Objects.requireNonNull(material, "material");
            if (material.isFluid()) throw new IllegalArgumentException("Assembly material input cannot be fluid: " + material);
            return material;
        }
    }

    public static final class StatBuilder<P> {
        private final P parent;
        private final AssemblyCapability capability;
        private final java.util.function.Consumer<AssemblyRequirement> sink;

        private StatBuilder(P parent, AssemblyCapability capability, java.util.function.Consumer<AssemblyRequirement> sink) {
            this.parent = Objects.requireNonNull(parent);
            this.capability = Objects.requireNonNull(capability);
            this.sink = Objects.requireNonNull(sink);
        }

        public P atLeast(double value) { sink.accept(capability.atLeast(value)); return parent; }
        public P atMost(double value) { sink.accept(capability.atMost(value)); return parent; }
        public P exactly(double value) { sink.accept(capability.exactly(value)); return parent; }
        public P range(double min, double max) { sink.accept(capability.range(min, max)); return parent; }
        public P covers(double min, double max) { sink.accept(capability.covers(min, max)); return parent; }
    }
}
