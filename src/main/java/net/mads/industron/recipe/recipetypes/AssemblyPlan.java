package net.mads.industron.recipe.recipetypes;

import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.recipe.recipes.assembly.ComponentDefinitions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Compiles recursive Component trees into deterministic material/item/tool/wait steps. */
public final class AssemblyPlan {
    public enum Kind { MATERIAL, ITEM, TOOL, WAIT }

    public record Step(
            Kind kind,
            MaterialPart material,
            IndustrialMaterial fixedMaterial,
            int bindingId,
            ResourceLocation itemId,
            AssemblyToolType tool,
            List<AssemblyRequirement> requirements,
            int waitTicks,
            SoundEvent sound
    ) {
        public Step {
            requirements = List.copyOf(requirements);
            if (kind == Kind.MATERIAL && fixedMaterial == null && bindingId < 0) {
                throw new IllegalArgumentException("Free material step requires its own runtime binding id");
            }
        }

        public boolean usesDynamicBinding() {
            return kind == Kind.MATERIAL && fixedMaterial == null;
        }
    }

    /**
     * A component branch is either fixed to one material or free. A free branch does not
     * share one ANY binding: each material leaf receives its own runtime binding id.
     */
    private record Binding(IndustrialMaterial fixedMaterial, int id, boolean free) {
        static Binding fixed(IndustrialMaterial material) { return new Binding(material, -1, false); }
        static Binding unbound() { return new Binding(null, -1, true); }
        static Binding dynamicLeaf(int id) { return new Binding(null, id, false); }
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
                null,
                new HashSet<>(),
                new CompileState()
        );
        return List.copyOf(result);
    }

    /** Compiles a standalone component with one fixed inherited material. */
    public static List<Step> compileComponent(AssemblyComponent component, IndustrialMaterial inheritedMaterial) {
        return compileComponent(component, inheritedMaterial, List.of());
    }

    /** Fixed standalone component with recipe-level requirements propagated to every material leaf. */
    public static List<Step> compileComponent(
            AssemblyComponent component,
            IndustrialMaterial inheritedMaterial,
            List<AssemblyRequirement> recipeRequirements
    ) {
        List<Step> result = new ArrayList<>();
        appendComponent(
                result,
                component,
                Binding.fixed(java.util.Objects.requireNonNull(inheritedMaterial, "inheritedMaterial")),
                List.copyOf(recipeRequirements),
                List.of(),
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
            case MATERIAL, COMPONENT -> binding(root.metal());
            case ITEM, TOOL, WAIT -> null;
        };

        for (int i = 0; i < root.count(); i++) {
            switch (root.kind()) {
                case MATERIAL -> appendMaterial(
                        result,
                        root.material(),
                        rootBinding,
                        root.requirements(),
                        List.of(),
                        root.sound(),
                        state
                );
                case COMPONENT -> appendComponent(
                        result,
                        root.component(),
                        rootBinding,
                        root.requirements(),
                        List.of(),
                        root.sound(),
                        new HashSet<>(),
                        state
                );
                case ITEM -> result.add(item(root.itemId(), root.sound()));
                case TOOL -> result.add(tool(root.tool(), root.sound()));
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
                case MATERIAL, COMPONENT -> local.metalOverride() == null
                        ? inheritedBinding
                        : binding(local.metalOverride());
                case ITEM, TOOL, WAIT -> null;
            };
            SoundEvent sound = local.sound() != null ? local.sound() : inheritedSound;

            List<RelativeContext> localRelativeRequirements = inheritedRelativeRequirements;
            if (!local.relativeRequirements().isEmpty()) {
                if (local.metalOverride() == null || !local.metalOverride().isAny()) {
                    throw new IllegalStateException(
                            "Relative component stats require inputAny(...) / Metal.ANY at Component."
                                    + component.id().toUpperCase(java.util.Locale.ROOT)
                    );
                }
                if (inheritedBinding == null || inheritedBinding.fixedMaterial() == null) {
                    throw new IllegalStateException(
                            "inputAny(...).stat(...).atLeastParent()/atMostParent() requires a fixed inherited Metal.X binding at Component."
                                    + component.id().toUpperCase(java.util.Locale.ROOT)
                    );
                }
                List<RelativeContext> next = new ArrayList<>(inheritedRelativeRequirements);
                for (AssemblyRelativeRequirement requirement : local.relativeRequirements()) {
                    next.add(new RelativeContext(requirement, inheritedBinding.fixedMaterial()));
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
                            localRelativeRequirements,
                            sound,
                            state
                    );
                    case COMPONENT -> appendComponent(
                            result,
                            local.component(),
                            localBinding,
                            recipeRequirements,
                            localRelativeRequirements,
                            sound,
                            new HashSet<>(path),
                            state
                    );
                    case ITEM -> result.add(item(local.itemId(), sound));
                    case TOOL -> result.add(tool(local.tool(), sound));
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
            List<RelativeContext> relativeRequirements,
            SoundEvent sound,
            CompileState state
    ) {
        if (binding == null) {
            throw new IllegalStateException("Material." + material.name() + " has no inherited or explicit metal mode");
        }

        Binding materialBinding = binding.free()
                ? Binding.dynamicLeaf(state.newBindingId())
                : binding;

        List<AssemblyRequirement> requirements = new ArrayList<>(recipeRequirements);
        for (RelativeContext relative : relativeRequirements) {
            requirements.add(relative.requirement().resolveAgainst(relative.parentMaterial(), material));
        }

        result.add(new Step(
                Kind.MATERIAL,
                material,
                materialBinding.fixedMaterial(),
                materialBinding.id(),
                null,
                null,
                requirements,
                0,
                sound
        ));
    }

    private static Binding binding(AssemblyMetal metal) {
        if (metal == null) throw new IllegalStateException("Top-level Material/Component input requires a metal mode");
        return metal.isAny()
                ? Binding.unbound()
                : Binding.fixed(metal.resolve());
    }

    private static Step item(ResourceLocation itemId, SoundEvent sound) {
        return new Step(Kind.ITEM, null, null, -1, itemId, null, List.of(), 0, sound);
    }

    private static Step tool(AssemblyToolType tool, SoundEvent sound) {
        return new Step(Kind.TOOL, null, null, -1, null, tool, List.of(), 0, sound);
    }

    private static Step waitStep(int ticks, SoundEvent sound) {
        return new Step(Kind.WAIT, null, null, -1, null, null, List.of(), ticks, sound);
    }
}
