package net.mads.industron.recipe.recipetypes.assembly;

import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.plant.PlantPart;
import net.mads.industron.recipe.recipes.assembly.MaterialType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Recursive semantic component structure.
 *
 * <p>Definitions contain structure. A normal material/component step inherits the
 * current material mode: a fixed IndustrialMaterial stays fixed through the complete subtree,
 * while a free/ANY caller stays free and lets each material leaf resolve independently.</p>
 *
 * <p>{@code inputAny(...)} explicitly breaks an inherited fixed material binding for
 * that branch. Relative requirements added after inputAny are compared against the
 * inherited parent material and propagate to every material leaf below the free branch.</p>
 *
 * <p>The first direct Material or exact-item step is the physical component being
 * worked on. JEI presents that step as the component output/link and shows the
 * remaining direct steps as its requirements. Runtime compilation still consumes
 * every step, including that physical component.</p>
 */
public final class ComponentDefinition {
    public enum StepKind { MATERIAL, PLANT_PART, COMPONENT, ITEM, TOOL, WAIT }

    public record Step(
            StepKind kind,
            MaterialPart material,
            PlantPart plantPart,
            AssemblyMaterialSelector materialOverride,
            AssemblyComponent component,
            ResourceLocation itemId,
            AssemblyToolType tool,
            int count,
            List<AssemblyRelativeRequirement> relativeRequirements,
            int waitTicks,
            SoundEvent sound
    ) {
        public Step {
            Objects.requireNonNull(kind, "kind");
            if (count < 1) throw new IllegalArgumentException("Component input count must be >= 1");
            relativeRequirements = List.copyOf(relativeRequirements);
        }

        Step withSound(SoundEvent value) {
            return new Step(kind, material, plantPart, materialOverride, component, itemId, tool, count, relativeRequirements, waitTicks, value);
        }

        Step withRelativeRequirement(AssemblyRelativeRequirement requirement) {
            List<AssemblyRelativeRequirement> next = new ArrayList<>(relativeRequirements);
            next.add(Objects.requireNonNull(requirement));
            return new Step(kind, material, plantPart, materialOverride, component, itemId, tool, count, next, waitTicks, sound);
        }
    }

    private final AssemblyComponent component;
    private final List<Step> steps;

    private ComponentDefinition(AssemblyComponent component, List<Step> steps) {
        this.component = Objects.requireNonNull(component, "component");
        this.steps = List.copyOf(steps);
    }

    public AssemblyComponent component() { return component; }
    public List<Step> steps() { return steps; }

    public static Builder component(AssemblyComponent component) {
        return new Builder(component);
    }

    public static final class Builder {
        private final AssemblyComponent component;
        private final List<Step> steps = new ArrayList<>();
        private int lastRelativeStatStep = -1;

        private Builder(AssemblyComponent component) {
            this.component = Objects.requireNonNull(component, "component");
        }

        /** Physical material leaf that inherits the current fixed/free material mode. */
        public Builder input(MaterialPart material) { return input(material, 1); }
        public Builder input(MaterialPart material, int count) {
            addStep(new Step(StepKind.MATERIAL, requireItemMaterial(material), null, null, null, null, null, count, List.of(), 0, null), false);
            return this;
        }

        /** Physical material leaf with an explicit fixed/category/free override. */
        public Builder input(MaterialPart material, AssemblyMaterialSelector selector) { return input(material, selector, 1); }
        public Builder input(MaterialPart material, AssemblyMaterialSelector selector, int count) {
            AssemblyMaterialSelector requiredSelector = Objects.requireNonNull(selector, "selector");
            addStep(new Step(StepKind.MATERIAL, requireItemMaterial(material), null, requiredSelector, null, null, null, count, List.of(), 0, null), requiredSelector.isAny());
            return this;
        }

        /** Explicitly fixes this leaf to one canonical IndustrialMaterial. */
        public Builder input(MaterialPart material, IndustrialSubstance fixedMaterial) { return input(material, fixedMaterial, 1); }
        public Builder input(MaterialPart material, IndustrialSubstance fixedMaterial, int count) {
            return input(material, AssemblyMaterialSelector.fixed(Objects.requireNonNull(fixedMaterial)), count);
        }

        /** Explicitly frees this material leaf from an inherited fixed IndustrialMaterial binding. */
        public Builder inputAny(MaterialPart material) { return inputAny(material, 1); }
        public Builder inputAny(MaterialPart material, int count) {
            addStep(new Step(StepKind.MATERIAL, requireItemMaterial(material), null, MaterialType.ANY, null, null, null, count, List.of(), 0, null), true);
            return this;
        }

        /** Plant-material role. Each use accepts any physical item registered for that PlantPart. */
        public Builder input(PlantPart part) { return input(part, 1); }
        public Builder input(PlantPart part, int count) {
            addStep(new Step(StepKind.PLANT_PART, null, Objects.requireNonNull(part, "part"), null, null, null, null, count, List.of(), 0, null), false);
            return this;
        }

        /** Nested component that inherits the current fixed/free material mode. */
        public Builder input(AssemblyComponent component) { return input(component, 1); }
        public Builder input(AssemblyComponent component, int count) {
            addStep(new Step(StepKind.COMPONENT, null, null, null, Objects.requireNonNull(component), null, null, count, List.of(), 0, null), false);
            return this;
        }

        /** Nested component with an explicit fixed/category/free override. */
        public Builder input(AssemblyComponent component, AssemblyMaterialSelector selector) { return input(component, selector, 1); }
        public Builder input(AssemblyComponent component, AssemblyMaterialSelector selector, int count) {
            AssemblyMaterialSelector requiredSelector = Objects.requireNonNull(selector, "selector");
            addStep(new Step(StepKind.COMPONENT, null, null, requiredSelector, Objects.requireNonNull(component), null, null, count, List.of(), 0, null), requiredSelector.isAny());
            return this;
        }

        /** Explicitly fixes this complete nested branch to one canonical IndustrialMaterial. */
        public Builder input(AssemblyComponent component, IndustrialSubstance fixedMaterial) { return input(component, fixedMaterial, 1); }
        public Builder input(AssemblyComponent component, IndustrialSubstance fixedMaterial, int count) {
            return input(component, AssemblyMaterialSelector.fixed(Objects.requireNonNull(fixedMaterial)), count);
        }

        /**
         * Explicitly breaks the inherited material binding for this complete nested branch.
         * Every material leaf below may choose independently. Relative stat requirements can
         * be added immediately afterwards with .stat(Stats.X).atLeastParent()/atMostParent().
         */
        public Builder inputAny(AssemblyComponent component) { return inputAny(component, 1); }
        public Builder inputAny(AssemblyComponent component, int count) {
            addStep(new Step(StepKind.COMPONENT, null, null, MaterialType.ANY, Objects.requireNonNull(component), null, null, count, List.of(), 0, null), true);
            return this;
        }

        /** Exact item-id leaf. */
        public Builder input(String itemId) { return input(itemId, 1); }
        public Builder input(String itemId, int count) {
            addStep(new Step(StepKind.ITEM, null, null, null, null, ResourceLocation.parse(itemId), null, count, List.of(), 0, null), false);
            return this;
        }

        /** Tool action. The tool is not consumed as a material input. */
        public Builder input(AssemblyToolType tool) {
            return tool(tool, 1);
        }

        public Builder input(ToolDefinition tool) {
            return tool(tool, 1);
        }

        /** Public recipe syntax: .tool(Tool.PICKAXE, amount). */
        public Builder tool(ToolDefinition tool, int amount) {
            return tool(Objects.requireNonNull(tool).type(), amount);
        }

        /** Repeats the same non-consumed tool action the requested number of times. */
        public Builder tool(AssemblyToolType tool, int amount) {
            addStep(new Step(StepKind.TOOL, null, null, null, null, null, Objects.requireNonNull(tool), amount, List.of(), 0, null), false);
            return this;
        }

        public Builder waitTicks(int ticks) {
            if (ticks < 1) throw new IllegalArgumentException("Wait must be >= 1 tick");
            addStep(new Step(StepKind.WAIT, null, null, null, null, null, null, 1, List.of(), ticks, null), false);
            return this;
        }

        public Builder waitSeconds(double seconds) {
            if (!(seconds > 0.0D)) throw new IllegalArgumentException("Wait must be > 0 seconds");
            return waitTicks(Math.max(1, (int) Math.round(seconds * 20.0D)));
        }

        /** Relative numeric stat for the immediately preceding inputAny(...) step. */
        public AssemblyRelativeNumericStatBuilder<Builder> stat(AssemblyCapability capability) {
            ensureRelativeStatTarget();
            return new AssemblyRelativeNumericStatBuilder<>(this, capability, this::addRelativeRequirementToLastStep);
        }

        public Builder sound(SoundEvent sound) {
            if (steps.isEmpty()) throw new IllegalStateException("sound() needs a previous step");
            int index = steps.size() - 1;
            steps.set(index, steps.get(index).withSound(Objects.requireNonNull(sound)));
            return this;
        }

        public ComponentDefinition build() {
            if (steps.isEmpty()) {
                throw new IllegalStateException("Component definition requires at least one input/tool/wait step");
            }
            return new ComponentDefinition(component, steps);
        }

        private void addStep(Step step, boolean relativeStatAllowed) {
            steps.add(step);
            lastRelativeStatStep = relativeStatAllowed ? steps.size() - 1 : -1;
        }

        private void ensureRelativeStatTarget() {
            if (lastRelativeStatStep < 0 || lastRelativeStatStep >= steps.size()) {
                throw new IllegalStateException("ComponentDefinition stat() is only valid immediately after inputAny(...)");
            }
        }

        private void addRelativeRequirementToLastStep(AssemblyRelativeRequirement requirement) {
            ensureRelativeStatTarget();
            Step current = steps.get(lastRelativeStatStep);
            steps.set(lastRelativeStatStep, current.withRelativeRequirement(requirement));
        }

        private static MaterialPart requireItemMaterial(MaterialPart material) {
            Objects.requireNonNull(material, "material");
            if (material.isFluid()) {
                throw new IllegalArgumentException("Assembly material input cannot be fluid: " + material);
            }
            return material;
        }
    }
}
