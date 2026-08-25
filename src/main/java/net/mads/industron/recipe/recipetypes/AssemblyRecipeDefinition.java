package net.mads.industron.recipe.recipetypes;

import net.mads.industron.Industron;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.MaterialPart;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Code-first assembly recipe executed on a placed base block or an assembly workbench. */
public final class AssemblyRecipeDefinition {
    public enum BaseKind { BLOCK, ITEM }
    public enum InputKind { MATERIAL, COMPONENT, ITEM, TOOL, WAIT }

    public record BaseValue(BaseKind kind, ResourceLocation id) {
        public BaseValue {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(id, "id");
        }
    }

    public record RootInput(
            InputKind kind,
            MaterialPart material,
            AssemblyMetal metal,
            AssemblyComponent component,
            ResourceLocation itemId,
            AssemblyToolType tool,
            int count,
            List<AssemblyRequirement> requirements,
            int waitTicks,
            SoundEvent sound
    ) {
        public RootInput {
            Objects.requireNonNull(kind, "kind");
            if (count < 1) throw new IllegalArgumentException("Input count must be >= 1");
            requirements = List.copyOf(requirements);
        }

        RootInput withSound(SoundEvent value) {
            return new RootInput(kind, material, metal, component, itemId, tool, count, requirements, waitTicks, value);
        }

        RootInput withRequirement(AssemblyRequirement requirement) {
            List<AssemblyRequirement> next = new ArrayList<>(requirements);
            next.add(Objects.requireNonNull(requirement));
            return new RootInput(kind, material, metal, component, itemId, tool, count, next, waitTicks, sound);
        }
    }

    private final String id;
    private final BaseValue baseInput;
    private final BaseValue baseOutput;
    private final List<RootInput> inputs;

    private AssemblyRecipeDefinition(String id, BaseValue baseInput, BaseValue baseOutput, List<RootInput> inputs) {
        this.id = id;
        this.baseInput = baseInput;
        this.baseOutput = baseOutput;
        this.inputs = List.copyOf(inputs);
    }

    public String id() { return id; }
    public BaseValue baseInput() { return baseInput; }
    public BaseValue baseOutput() { return baseOutput; }
    public List<RootInput> inputs() { return inputs; }

    /** Every block-base recipe is assembled directly on the placed base block. */
    public boolean usesWorldBlockRuntime() {
        return baseInput.kind() == BaseKind.BLOCK;
    }

    public boolean hasBlockBaseInput() { return baseInput.kind() == BaseKind.BLOCK; }
    public boolean hasItemBaseInput() { return baseInput.kind() == BaseKind.ITEM; }
    public boolean hasBlockBaseOutput() { return baseOutput.kind() == BaseKind.BLOCK; }
    public boolean hasItemBaseOutput() { return baseOutput.kind() == BaseKind.ITEM; }

    public Block baseBlock() {
        if (baseInput.kind() != BaseKind.BLOCK) {
            throw new IllegalStateException("Assembly recipe " + id + " does not use a block base input");
        }
        return BuiltInRegistries.BLOCK.getOptional(baseInput.id())
                .orElseThrow(() -> new IllegalStateException("Unknown base block: " + baseInput.id()));
    }

    public Block outputBlock() {
        if (baseOutput.kind() != BaseKind.BLOCK) {
            throw new IllegalStateException("Assembly recipe " + id + " does not use a block base output");
        }
        return BuiltInRegistries.BLOCK.getOptional(baseOutput.id())
                .orElseThrow(() -> new IllegalStateException("Unknown output block: " + baseOutput.id()));
    }

    public Item baseItem() {
        if (baseInput.kind() != BaseKind.ITEM) {
            throw new IllegalStateException("Assembly recipe " + id + " does not use an item base input");
        }
        return BuiltInRegistries.ITEM.getOptional(baseInput.id())
                .orElseThrow(() -> new IllegalStateException("Unknown base item: " + baseInput.id()));
    }

    public Item outputItem() {
        if (baseOutput.kind() != BaseKind.ITEM) {
            throw new IllegalStateException("Assembly recipe " + id + " does not use an item base output");
        }
        return BuiltInRegistries.ITEM.getOptional(baseOutput.id())
                .orElseThrow(() -> new IllegalStateException("Unknown output item: " + baseOutput.id()));
    }

    public static Builder recipe(String id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final String id;
        private BaseValue baseInput;
        private BaseValue baseOutput;
        private final List<RootInput> inputs = new ArrayList<>();
        private int lastStatInput = -1;

        private Builder(String id) {
            this.id = Objects.requireNonNull(id, "id");
            if (id.isBlank()) throw new IllegalArgumentException("Assembly recipe id cannot be blank");
        }

        public Builder baseBlockInput(String blockId) {
            baseInput = setBase(baseInput, BaseKind.BLOCK, ResourceLocation.parse(blockId), "input");
            return this;
        }

        public Builder baseBlockInput(Block block) {
            baseInput = setBase(baseInput, BaseKind.BLOCK, BuiltInRegistries.BLOCK.getKey(Objects.requireNonNull(block)), "input");
            return this;
        }

        /** Physical material block used as the placed assembly base. */
        public Builder baseBlockInput(MaterialPart material, IndustrialMaterial boundMaterial) {
            baseInput = setBase(
                    baseInput,
                    BaseKind.BLOCK,
                    resolveMaterialPartId(material, boundMaterial, BaseKind.BLOCK),
                    "input"
            );
            return this;
        }

        public Builder baseItemInput(String itemId) {
            baseInput = setBase(baseInput, BaseKind.ITEM, ResourceLocation.parse(itemId), "input");
            return this;
        }

        public Builder baseItemInput(Item item) {
            baseInput = setBase(baseInput, BaseKind.ITEM, BuiltInRegistries.ITEM.getKey(Objects.requireNonNull(item)), "input");
            return this;
        }

        /** Physical material item used as the non-expanding workbench base. */
        public Builder baseItemInput(MaterialPart material, IndustrialMaterial boundMaterial) {
            baseInput = setBase(
                    baseInput,
                    BaseKind.ITEM,
                    resolveMaterialPartId(material, boundMaterial, BaseKind.ITEM),
                    "input"
            );
            return this;
        }

        public Builder baseBlockOutput(String blockId) {
            baseOutput = setBase(baseOutput, BaseKind.BLOCK, ResourceLocation.parse(blockId), "output");
            return this;
        }

        public Builder baseBlockOutput(Block block) {
            baseOutput = setBase(baseOutput, BaseKind.BLOCK, BuiltInRegistries.BLOCK.getKey(Objects.requireNonNull(block)), "output");
            return this;
        }

        /** Physical material block used as the assembly result. */
        public Builder baseBlockOutput(MaterialPart material, IndustrialMaterial boundMaterial) {
            baseOutput = setBase(
                    baseOutput,
                    BaseKind.BLOCK,
                    resolveMaterialPartId(material, boundMaterial, BaseKind.BLOCK),
                    "output"
            );
            return this;
        }

        public Builder baseItemOutput(String itemId) {
            baseOutput = setBase(baseOutput, BaseKind.ITEM, ResourceLocation.parse(itemId), "output");
            return this;
        }

        public Builder baseItemOutput(Item item) {
            baseOutput = setBase(baseOutput, BaseKind.ITEM, BuiltInRegistries.ITEM.getKey(Objects.requireNonNull(item)), "output");
            return this;
        }

        /** Physical material item/block-item used as the assembly result. */
        public Builder baseItemOutput(MaterialPart material, IndustrialMaterial boundMaterial) {
            baseOutput = setBase(
                    baseOutput,
                    BaseKind.ITEM,
                    resolveMaterialPartId(material, boundMaterial, BaseKind.ITEM),
                    "output"
            );
            return this;
        }

        /** Material leaf with implicit free Metal.ANY selection in a normal recipe. */
        public Builder input(MaterialPart material) { return input(material, Metal.ANY, 1); }
        public Builder input(MaterialPart material, int count) { return input(material, Metal.ANY, count); }
        public Builder input(MaterialPart material, AssemblyMetal metal) { return input(material, metal, 1); }
        public Builder input(MaterialPart material, AssemblyMetal metal, int count) {
            addInput(new RootInput(InputKind.MATERIAL, requireItemMaterial(material), Objects.requireNonNull(metal), null, null, null, count, List.of(), 0, null));
            return this;
        }

        /** Internal casing overload: bind the input to the generated casing material. */
        public Builder input(MaterialPart material, IndustrialMaterial boundMaterial, int count) {
            return input(material, new AssemblyMetal(Objects.requireNonNull(boundMaterial).id()), count);
        }

        /** Component tree with implicit free Metal.ANY selection; each material leaf may differ. */
        public Builder input(AssemblyComponent component) { return input(component, Metal.ANY, 1); }
        public Builder input(AssemblyComponent component, int count) { return input(component, Metal.ANY, count); }
        public Builder input(AssemblyComponent component, AssemblyMetal metal) { return input(component, metal, 1); }
        public Builder input(AssemblyComponent component, AssemblyMetal metal, int count) {
            addInput(new RootInput(InputKind.COMPONENT, null, Objects.requireNonNull(metal), Objects.requireNonNull(component), null, null, count, List.of(), 0, null));
            return this;
        }

        /** Internal casing overload: bind the complete tree to the generated casing material. */
        public Builder input(AssemblyComponent component, IndustrialMaterial boundMaterial, int count) {
            return input(component, new AssemblyMetal(Objects.requireNonNull(boundMaterial).id()), count);
        }

        public Builder input(String itemId) { return input(itemId, 1); }
        public Builder input(String itemId, int count) {
            addInput(new RootInput(InputKind.ITEM, null, null, null, ResourceLocation.parse(itemId), null, count, List.of(), 0, null));
            return this;
        }

        public Builder input(AssemblyToolType tool) {
            addInput(new RootInput(InputKind.TOOL, null, null, null, null, Objects.requireNonNull(tool), 1, List.of(), 0, null));
            return this;
        }

        public Builder waitTicks(int ticks) {
            if (ticks < 1) throw new IllegalArgumentException("Wait must be >= 1 tick");
            addInput(new RootInput(InputKind.WAIT, null, null, null, null, null, 1, List.of(), ticks, null));
            return this;
        }

        public Builder waitSeconds(double seconds) {
            if (!(seconds > 0.0D)) throw new IllegalArgumentException("Wait must be > 0 seconds");
            return waitTicks(Math.max(1, (int) Math.round(seconds * 20.0D)));
        }

        /** Adds a numeric material requirement to the previous Material/Component input. */
        public AssemblyNumericStatBuilder<Builder> stat(AssemblyCapability capability) {
            ensureStatTarget();
            return new AssemblyNumericStatBuilder<>(this, capability, this::addRequirementToLastInput);
        }

        /** Adds a typed material requirement to the previous Material/Component input. */
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

        public AssemblyRecipeDefinition build() {
            if (baseInput == null || baseOutput == null || inputs.isEmpty()) {
                throw new IllegalStateException("Assembly recipe requires one base input, at least one input step, and one base output");
            }
            return new AssemblyRecipeDefinition(id, baseInput, baseOutput, inputs);
        }

        private static BaseValue setBase(BaseValue current, BaseKind kind, ResourceLocation id, String side) {
            if (current != null) throw new IllegalStateException("Assembly recipe already has a base " + side + ": " + current.id());
            return new BaseValue(kind, Objects.requireNonNull(id));
        }

        private void addInput(RootInput input) {
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
            RootInput current = inputs.get(lastStatInput);
            inputs.set(lastStatInput, current.withRequirement(requirement));
        }

        private static MaterialPart requireItemMaterial(MaterialPart material) {
            Objects.requireNonNull(material, "material");
            if (material.isFluid()) throw new IllegalArgumentException("Assembly material input cannot be fluid: " + material);
            return material;
        }

        private static ResourceLocation resolveMaterialPartId(
                MaterialPart part,
                IndustrialMaterial material,
                BaseKind kind
        ) {
            Objects.requireNonNull(part, "part");
            Objects.requireNonNull(material, "material");
            Objects.requireNonNull(kind, "kind");

            if (!material.has(part)) {
                throw new IllegalArgumentException(
                        "Material " + material.id() + " does not expose Material." + part.name()
                );
            }
            if (part.isFluid()) {
                throw new IllegalArgumentException("Assembly base cannot be a fluid material part: " + part);
            }
            if (kind == BaseKind.BLOCK && !part.isBlock()) {
                throw new IllegalArgumentException(
                        "Assembly block base/output requires a block material part, got Material." + part.name()
                );
            }

            if (material.hasExistingPart(part)) {
                return material.existingPart(part);
            }
            return ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, part.registryName(material));
        }
    }
}
