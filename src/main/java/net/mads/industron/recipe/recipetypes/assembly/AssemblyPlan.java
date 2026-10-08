package net.mads.industron.recipe.recipetypes.assembly;

import net.mads.industron.Industron;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.material.MaterialCategory;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.plant.PlantPart;
import net.mads.industron.material.structure.StructureMaterial;
import net.mads.industron.recipe.recipes.assembly.ComponentDefinitions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Compiles recursive Component trees into deterministic material/item/tool/wait steps. */
public final class AssemblyPlan {
    public enum Kind { MATERIAL, PLANT_PART, ITEM, TOOL, WAIT }

    public record Step(
            Kind kind,
            MaterialPart material,
            PlantPart plantPart,
            IndustrialMaterial fixedMaterial,
            MaterialCategory materialCategory,
            AssemblyMaterialSelector materialSelector,
            int bindingId,
            ResourceLocation itemId,
            AssemblyToolType tool,
            List<AssemblyRequirement> requirements,
            List<AssemblyCapturedRequirement> capturedRequirements,
            String captureRole,
            int waitTicks,
            int consumeChance,
            SoundEvent sound
    ) {
        public Step {
            requirements = List.copyOf(requirements);
            capturedRequirements = capturedRequirements == null ? List.of() : List.copyOf(capturedRequirements);
            if (kind == Kind.MATERIAL) {
                if (materialSelector == null) throw new IllegalArgumentException("Material step requires a selector");
                if (fixedMaterial == null && bindingId < 0) {
                    throw new IllegalArgumentException("Free material step requires its own runtime binding id");
                }
            }
            if (fixedMaterial != null && materialCategory != null) {
                throw new IllegalArgumentException("Fixed material step cannot also define a category");
            }
            if (captureRole != null && (kind != Kind.MATERIAL || captureRole.isBlank())) {
                throw new IllegalArgumentException("Only material steps may define a non-blank capture role");
            }
            if (consumeChance < 0 || consumeChance > AssemblyRecipeDefinition.MAX_CHANCE) {
                throw new IllegalArgumentException("Assembly consume chance out of range");
            }
            if ((kind == Kind.TOOL || kind == Kind.WAIT) && consumeChance != AssemblyRecipeDefinition.MAX_CHANCE) {
                throw new IllegalArgumentException("Tool/wait steps cannot have consume chance");
            }
        }

        public boolean usesDynamicBinding() {
            return kind == Kind.MATERIAL && fixedMaterial == null;
        }

        public boolean acceptsCategory(IndustrialMaterial candidate) {
            return materialCategory == null || materialCategory.matches(candidate);
        }

        public boolean acceptsMaterial(IndustrialSubstance candidate) {
            if (candidate == null || kind != Kind.MATERIAL) return false;
            if (fixedMaterial != null) {
                return candidate instanceof IndustrialMaterial industrial
                        && fixedMaterial.id().equals(industrial.id());
            }
            if (materialSelector.isToolSelector()) {
                return materialSelector.matchesTool(candidate, material);
            }
            return materialSelector.matchesSubstance(candidate);
        }
    }

    /**
     * A component branch is either fixed to one material or free. A free branch does not
     * share one ANY/category binding: each material leaf receives its own runtime binding id.
     */
    private record Binding(
            IndustrialSubstance fixedMaterial,
            MaterialCategory category,
            AssemblyMaterialSelector selector,
            int id,
            boolean free
    ) {
        static Binding fixed(IndustrialSubstance material) {
            return new Binding(material, null, AssemblyMaterialSelector.fixed(material), -1, false);
        }
        static Binding unbound() {
            return new Binding(null, null, AssemblyMaterialSelector.ANY, -1, true);
        }
        static Binding unbound(MaterialCategory category) {
            return new Binding(null, category, AssemblyMaterialSelector.category(category), -1, true);
        }
        static Binding unbound(AssemblyMaterialSelector selector) {
            return new Binding(null, selector.isCategory() ? selector.category() : null, selector, -1, true);
        }
        static Binding dynamicLeaf(int id, Binding parent) {
            return new Binding(null, parent.category(), parent.selector(), id, false);
        }
    }

    private record RelativeContext(
            AssemblyRelativeRequirement requirement,
            IndustrialMaterial parentMaterial
    ) {
    }

    private static final class CompileState {
        private int nextBindingId;
        int newBindingId() { return nextBindingId++; }
    }

    private AssemblyPlan() {
    }

    public static List<Step> compile(AssemblyRecipeDefinition recipe) {
        List<Step> result = new ArrayList<>();
        CompileState state = new CompileState();

        for (AssemblyRecipeDefinition.RootInput root : recipe.inputs()) {
            appendRoot(result, root, state);
        }
        return List.copyOf(result);
    }

    /** Compiles one top-level input independently for direct JEI presentation. */
    public static List<Step> compileInput(AssemblyRecipeDefinition.RootInput root) {
        List<Step> result = new ArrayList<>();
        appendRoot(result, root, new CompileState());
        return List.copyOf(result);
    }

    /** JEI/validation view of one standalone component with completely free material leaves. */
    public static List<Step> compileComponent(AssemblyComponent component) {
        return compileComponent(component, List.of());
    }

    /** Free standalone component with recipe-level requirements propagated to every material leaf. */
    public static List<Step> compileComponent(
            AssemblyComponent component,
            List<AssemblyRequirement> recipeRequirements
    ) {
        List<Step> result = new ArrayList<>();
        appendComponent(
                result,
                component,
                Binding.unbound(),
                List.copyOf(recipeRequirements),
                List.of(),
                AssemblyRecipeDefinition.MAX_CHANCE,
                null,
                new HashSet<>(),
                new CompileState()
        );
        return List.copyOf(result);
    }

    /** Compiles a standalone component under an explicit category/ANY/fixed selector. */
    public static List<Step> compileComponent(
            AssemblyComponent component,
            AssemblyMaterialSelector selector,
            List<AssemblyRequirement> recipeRequirements
    ) {
        List<Step> result = new ArrayList<>();
        appendComponent(
                result,
                component,
                binding(java.util.Objects.requireNonNull(selector, "selector")),
                List.copyOf(recipeRequirements),
                List.of(),
                AssemblyRecipeDefinition.MAX_CHANCE,
                null,
                new HashSet<>(),
                new CompileState()
        );
        return List.copyOf(result);
    }

    /** Compiles a standalone component with one fixed inherited material. */
    public static List<Step> compileComponent(AssemblyComponent component, IndustrialSubstance inheritedMaterial) {
        return compileComponent(component, inheritedMaterial, List.of());
    }

    /** Fixed standalone component with recipe-level requirements propagated to every material leaf. */
    public static List<Step> compileComponent(
            AssemblyComponent component,
            IndustrialSubstance inheritedMaterial,
            List<AssemblyRequirement> recipeRequirements
    ) {
        List<Step> result = new ArrayList<>();
        appendComponent(
                result,
                component,
                Binding.fixed(java.util.Objects.requireNonNull(inheritedMaterial, "inheritedMaterial")),
                List.copyOf(recipeRequirements),
                List.of(),
                AssemblyRecipeDefinition.MAX_CHANCE,
                null,
                new HashSet<>(),
                new CompileState()
        );
        return List.copyOf(result);
    }

    /**
     * Compiles a component as a deliberately free inputAny branch relative to a fixed
     * parent material. Used by JEI/validation to mirror runtime semantics.
     */
    public static List<Step> compileFreeComponent(
            AssemblyComponent component,
            IndustrialMaterial parentMaterial,
            List<AssemblyRelativeRequirement> relativeRequirements
    ) {
        java.util.Objects.requireNonNull(parentMaterial, "parentMaterial");
        List<RelativeContext> relative = relativeRequirements.stream()
                .map(requirement -> new RelativeContext(requirement, parentMaterial))
                .toList();
        List<Step> result = new ArrayList<>();
        appendComponent(
                result,
                component,
                Binding.unbound(),
                List.of(),
                relative,
                AssemblyRecipeDefinition.MAX_CHANCE,
                null,
                new HashSet<>(),
                new CompileState()
        );
        return List.copyOf(result);
    }

    private static void appendRoot(
            List<Step> result,
            AssemblyRecipeDefinition.RootInput root,
            CompileState state
    ) {
        Binding rootBinding = switch (root.kind()) {
            case MATERIAL, COMPONENT -> binding(root.materialSelector());
            case PLANT_PART, ITEM, TOOL, WAIT -> null;
        };

        for (int i = 0; i < root.count(); i++) {
            switch (root.kind()) {
                case MATERIAL -> appendMaterial(
                        result,
                        root.material(),
                        rootBinding,
                        root.requirements(),
                        root.capturedRequirements(),
                        root.captureRole(),
                        List.of(),
                        root.consumeChance(),
                        root.sound(),
                        state
                );
                case COMPONENT -> {
                    int firstStep = result.size();
                    appendComponent(
                            result,
                            root.component(),
                            rootBinding,
                            root.requirements(),
                            List.of(),
                            root.consumeChance(),
                            root.sound(),
                            new HashSet<>(),
                            state
                    );
                    // Preserve the parent recipe's material identity constraints through
                    // recursive expansion, including independent inputAny fasteners.
                    if (!root.capturedRequirements().isEmpty()) {
                        for (int index = firstStep; index < result.size(); index++) {
                            Step step = result.get(index);
                            if (step.kind() != Kind.MATERIAL) continue;
                            List<AssemblyCapturedRequirement> constraints =
                                    new ArrayList<>(step.capturedRequirements());
                            constraints.addAll(root.capturedRequirements());
                            result.set(index, new Step(
                                    step.kind(), step.material(), step.plantPart(), step.fixedMaterial(),
                                    step.materialCategory(), step.materialSelector(), step.bindingId(),
                                    step.itemId(), step.tool(), step.requirements(), constraints,
                                    step.captureRole(), step.waitTicks(), step.consumeChance(), step.sound()
                            ));
                        }
                    }
                }
                case PLANT_PART -> result.add(plantPart(root.plantPart(), root.consumeChance(), root.sound()));
                case ITEM -> result.add(item(root.itemId(), root.consumeChance(), root.sound()));
                case TOOL -> result.add(tool(root.tool(), root.requirements(), root.sound()));
                case WAIT -> result.add(waitStep(root.waitTicks(), root.sound()));
            }
        }
    }

    private static void appendComponent(
            List<Step> result,
            AssemblyComponent component,
            Binding inheritedBinding,
            List<AssemblyRequirement> recipeRequirements,
            List<RelativeContext> inheritedRelativeRequirements,
            int inheritedConsumeChance,
            SoundEvent inheritedSound,
            Set<AssemblyComponent> path,
            CompileState state
    ) {
        if (!path.add(component)) {
            throw new IllegalStateException(
                    "Assembly component cycle detected at Component."
                            + component.id().toUpperCase(java.util.Locale.ROOT)
            );
        }

        ComponentDefinition definition = ComponentDefinitions.find(component);
        if (definition == null) {
            throw new IllegalStateException(
                    "No ComponentDefinition registered for Component."
                            + component.id().toUpperCase(java.util.Locale.ROOT)
            );
        }

        for (ComponentDefinition.Step local : definition.steps()) {
            Binding localBinding = switch (local.kind()) {
                case MATERIAL, COMPONENT -> local.materialOverride() == null
                        ? inheritedBinding
                        : binding(local.materialOverride());
                case PLANT_PART, ITEM, TOOL, WAIT -> null;
            };
            SoundEvent sound = local.sound() != null ? local.sound() : inheritedSound;

            List<RelativeContext> localRelativeRequirements = inheritedRelativeRequirements;
            if (!local.relativeRequirements().isEmpty()) {
                if (local.materialOverride() == null || !local.materialOverride().isAny()) {
                    throw new IllegalStateException(
                            "Relative component stats require inputAny(...) / MaterialType.ANY at Component."
                                    + component.id().toUpperCase(java.util.Locale.ROOT)
                    );
                }
                if (inheritedBinding == null || !(inheritedBinding.fixedMaterial() instanceof IndustrialMaterial parentMaterial)) {
                    throw new IllegalStateException(
                            "inputAny(...).stat(...).atLeastParent()/atMostParent() requires a fixed IndustrialMaterial parent at Component."
                                    + component.id().toUpperCase(java.util.Locale.ROOT)
                    );
                }
                List<RelativeContext> next = new ArrayList<>(inheritedRelativeRequirements);
                for (AssemblyRelativeRequirement requirement : local.relativeRequirements()) {
                    next.add(new RelativeContext(requirement, parentMaterial));
                }
                localRelativeRequirements = List.copyOf(next);
            }

            for (int count = 0; count < local.count(); count++) {
                switch (local.kind()) {
                    case MATERIAL -> appendMaterial(
                            result,
                            local.material(),
                            localBinding,
                            recipeRequirements,
                            List.of(),
                            null,
                            localRelativeRequirements,
                            inheritedConsumeChance,
                            sound,
                            state
                    );
                    case COMPONENT -> appendComponent(
                            result,
                            local.component(),
                            localBinding,
                            recipeRequirements,
                            localRelativeRequirements,
                            inheritedConsumeChance,
                            sound,
                            new HashSet<>(path),
                            state
                    );
                    case PLANT_PART -> result.add(plantPart(local.plantPart(), inheritedConsumeChance, sound));
                    case ITEM -> result.add(item(local.itemId(), inheritedConsumeChance, sound));
                    case TOOL -> result.add(tool(local.tool(), List.of(), sound));
                    case WAIT -> result.add(waitStep(local.waitTicks(), sound));
                }
            }
        }
    }

    private static void appendMaterial(
            List<Step> result,
            MaterialPart material,
            Binding binding,
            List<AssemblyRequirement> recipeRequirements,
            List<AssemblyCapturedRequirement> capturedRequirements,
            String captureRole,
            List<RelativeContext> relativeRequirements,
            int consumeChance,
            SoundEvent sound,
            CompileState state
    ) {
        if (binding == null) {
            throw new IllegalStateException("Material." + material.name() + " has no inherited or explicit material selector");
        }

        if (!binding.free() && binding.fixedMaterial() instanceof StructureMaterial structureMaterial) {
            if (!recipeRequirements.isEmpty() || !capturedRequirements.isEmpty() || captureRole != null || !relativeRequirements.isEmpty()) {
                throw new IllegalStateException(
                        "Structure-material Assembly leaves do not support material stat/capture requirements: "
                                + structureMaterial.id() + " Material." + material.name()
                );
            }
            result.add(item(resolveStructurePart(structureMaterial, material), consumeChance, sound));
            return;
        }

        Binding materialBinding = binding.free()
                ? Binding.dynamicLeaf(state.newBindingId(), binding)
                : binding;

        List<AssemblyRequirement> requirements = new ArrayList<>(recipeRequirements);
        for (RelativeContext relative : relativeRequirements) {
            requirements.add(relative.requirement().resolveAgainst(relative.parentMaterial(), material));
        }

        result.add(new Step(
                Kind.MATERIAL,
                material,
                null,
                materialBinding.fixedMaterial() instanceof IndustrialMaterial industrial ? industrial : null,
                materialBinding.category(),
                materialBinding.selector(),
                materialBinding.id(),
                null,
                null,
                requirements,
                capturedRequirements,
                captureRole,
                0,
                consumeChance,
                sound
        ));
    }

    private static Binding binding(AssemblyMaterialSelector selector) {
        if (selector == null) {
            throw new IllegalStateException("Top-level Material/Component input requires a material selector");
        }
        if (selector.isFixed()) return Binding.fixed(selector.resolveSubstance());
        if (selector.isCategory()) return Binding.unbound(selector.category());
        return Binding.unbound(selector);
    }

    private static Step plantPart(PlantPart part, int consumeChance, SoundEvent sound) {
        return new Step(Kind.PLANT_PART, null, java.util.Objects.requireNonNull(part, "part"), null, null, null, -1, null, null, List.of(), List.of(), null, 0, consumeChance, sound);
    }

    private static ResourceLocation resolveStructurePart(StructureMaterial material, MaterialPart part) {
        if (part == null || part.isFluid()) {
            throw new IllegalArgumentException("Structure-material Assembly leaf requires a non-fluid MaterialPart");
        }
        if (material.hasExistingPart(part)) return material.existingPart(part);
        if (!material.generatedForms().contains(part)) {
            throw new IllegalStateException(
                    "Structure material " + material.id() + " does not expose Material." + part.name()
            );
        }
        return ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, part.registryName(material));
    }

    private static Step item(ResourceLocation itemId, int consumeChance, SoundEvent sound) {
        return new Step(Kind.ITEM, null, null, null, null, null, -1, itemId, null, List.of(), List.of(), null, 0, consumeChance, sound);
    }

    private static Step tool(AssemblyToolType tool, List<AssemblyRequirement> requirements, SoundEvent sound) {
        return new Step(Kind.TOOL, null, null, null, null, null, -1, null, tool,
                requirements == null ? List.of() : List.copyOf(requirements), List.of(), null, 0,
                AssemblyRecipeDefinition.MAX_CHANCE, sound);
    }

    private static Step waitStep(int ticks, SoundEvent sound) {
        return new Step(Kind.WAIT, null, null, null, null, null, -1, null, null, List.of(), List.of(), null, ticks, AssemblyRecipeDefinition.MAX_CHANCE, sound);
    }
}
