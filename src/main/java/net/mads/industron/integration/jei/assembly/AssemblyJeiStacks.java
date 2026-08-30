package net.mads.industron.integration.jei.assembly;

import net.mads.industron.energy.WireThickness;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.defenitions.IndustrialMaterials;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.recipes.MaterialRecipeHelper;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyComponent;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyMetal;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyPlan;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyRelativeRequirement;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyRequirement;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyToolType;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyTools;
import net.mads.industron.recipe.recipetypes.assembly.ComponentDefinition;
import net.mads.industron.recipe.recipes.assembly.ComponentDefinitions;
import net.mads.industron.registry.ItemRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class AssemblyJeiStacks {
    private AssemblyJeiStacks() {
    }

    static List<ItemStack> material(AssemblyPlan.Step step, List<AssemblyPlan.Step> plan, int count) {
        if (step.kind() != AssemblyPlan.Kind.MATERIAL) return List.of();

        List<IndustrialMaterial> materials;
        if (step.fixedMaterial() != null) {
            materials = List.of(step.fixedMaterial());
        } else {
            materials = candidatesForBinding(step.bindingId(), plan);
        }

        List<ItemStack> result = new ArrayList<>();
        for (IndustrialMaterial material : materials) {
            Item item = resolveItem(material, step.material());
            if (item != null) result.add(new ItemStack(item, count));
        }
        return List.copyOf(result);
    }

    static List<ItemStack> exactItem(ResourceLocation itemId, int count) {
        Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
        return item == null ? List.of() : List.of(new ItemStack(item, count));
    }

    static List<ItemStack> tools(AssemblyToolType type) {
        return AssemblyTools.all().stream()
                .filter(tool -> tool.type().equals(type))
                .map(tool -> new ItemStack(tool.item()))
                .toList();
    }

    static List<ItemStack> directMaterial(
            MaterialPart part,
            AssemblyMetal override,
            IndustrialMaterial inheritedMaterial,
            int count
    ) {
        return directMaterial(part, override, inheritedMaterial, List.of(), count);
    }

    static List<ItemStack> directMaterial(
            MaterialPart part,
            AssemblyMetal override,
            IndustrialMaterial inheritedMaterial,
            List<AssemblyRelativeRequirement> relativeRequirements,
            int count
    ) {
        Map<Item, ItemStack> result = new LinkedHashMap<>();
        for (IndustrialMaterial material : bindingMaterials(override, inheritedMaterial)) {
            if (!material.has(part)) continue;
            if (!matchesRelative(material, part, inheritedMaterial, relativeRequirements)) continue;
            Item item = resolveItem(material, part);
            if (item != null) result.putIfAbsent(item, new ItemStack(item, count));
        }
        return List.copyOf(result.values());
    }

    static List<ItemStack> component(
            AssemblyComponent component,
            AssemblyMetal override,
            IndustrialMaterial inheritedMaterial,
            int count
    ) {
        return component(component, override, inheritedMaterial, List.of(), count);
    }

    static List<ItemStack> component(
            AssemblyComponent component,
            AssemblyMetal override,
            IndustrialMaterial inheritedMaterial,
            List<AssemblyRelativeRequirement> relativeRequirements,
            int count
    ) {
        ComponentDefinition definition = ComponentDefinitions.find(component);
        int representativeIndex = representativeIndex(definition);
        if (representativeIndex < 0) return List.of();

        if (override != null && override.isAny()) {
            if (inheritedMaterial == null && !relativeRequirements.isEmpty()) return List.of();
            final List<AssemblyPlan.Step> plan;
            try {
                plan = relativeRequirements.isEmpty()
                        ? AssemblyPlan.compileComponent(component)
                        : AssemblyPlan.compileFreeComponent(component, inheritedMaterial, relativeRequirements);
            } catch (IllegalStateException exception) {
                return List.of();
            }
            for (AssemblyPlan.Step step : plan) {
                if (step.kind() == AssemblyPlan.Kind.MATERIAL) {
                    return material(step, plan, count);
                }
                if (step.kind() == AssemblyPlan.Kind.ITEM) {
                    return exactItem(step.itemId(), count);
                }
            }
            return List.of();
        }

        ComponentDefinition.Step representative = definition.steps().get(representativeIndex);
        Map<Item, ItemStack> result = new LinkedHashMap<>();
        for (IndustrialMaterial componentMaterial : bindingMaterials(override, inheritedMaterial)) {
            if (!componentAvailable(component, componentMaterial)) continue;

            List<ItemStack> candidates = switch (representative.kind()) {
                case MATERIAL -> directMaterial(
                        representative.material(),
                        representative.metalOverride(),
                        componentMaterial,
                        representative.relativeRequirements(),
                        count
                );
                case ITEM -> exactItem(representative.itemId(), count);
                case COMPONENT, TOOL, WAIT -> List.of();
            };
            for (ItemStack candidate : candidates) {
                result.putIfAbsent(candidate.getItem(), candidate);
            }
        }
        return List.copyOf(result.values());
    }

    static int representativeIndex(ComponentDefinition definition) {
        if (definition == null) return -1;
        for (int index = 0; index < definition.steps().size(); index++) {
            ComponentDefinition.Step step = definition.steps().get(index);
            if (step.kind() == ComponentDefinition.StepKind.MATERIAL
                    || step.kind() == ComponentDefinition.StepKind.ITEM) {
                return index;
            }
        }
        return -1;
    }

    static boolean componentAvailable(AssemblyComponent component, IndustrialMaterial inheritedMaterial) {
        final List<AssemblyPlan.Step> plan;
        try {
            plan = AssemblyPlan.compileComponent(component, inheritedMaterial);
        } catch (IllegalStateException exception) {
            return false;
        }
        return planAvailable(plan);
    }

    static boolean freeComponentAvailable(
            AssemblyComponent component,
            IndustrialMaterial parentMaterial,
            List<AssemblyRelativeRequirement> relativeRequirements
    ) {
        final List<AssemblyPlan.Step> plan;
        try {
            plan = relativeRequirements.isEmpty()
                    ? AssemblyPlan.compileComponent(component)
                    : AssemblyPlan.compileFreeComponent(component, parentMaterial, relativeRequirements);
        } catch (IllegalStateException exception) {
            return false;
        }
        return planAvailable(plan);
    }

    private static boolean planAvailable(List<AssemblyPlan.Step> plan) {
        for (AssemblyPlan.Step step : plan) {
            switch (step.kind()) {
                case MATERIAL -> {
                    if (step.fixedMaterial() != null) {
                        if (!matches(step.fixedMaterial(), step) || resolveItem(step.fixedMaterial(), step.material()) == null) {
                            return false;
                        }
                    } else if (candidatesForBinding(step.bindingId(), plan).stream()
                            .noneMatch(material -> resolveItem(material, step.material()) != null)) {
                        return false;
                    }
                }
                case ITEM -> {
                    if (BuiltInRegistries.ITEM.getOptional(step.itemId()).isEmpty()) return false;
                }
                case TOOL -> {
                    if (tools(step.tool()).isEmpty()) return false;
                }
                case WAIT -> { }
            }
        }
        return true;
    }

    private static List<IndustrialMaterial> candidatesForBinding(int bindingId, List<AssemblyPlan.Step> plan) {
        List<AssemblyPlan.Step> group = plan.stream()
                .filter(step -> step.kind() == AssemblyPlan.Kind.MATERIAL)
                .filter(step -> step.usesDynamicBinding() && step.bindingId() == bindingId)
                .toList();

        return IndustrialMaterials.ALL.stream()
                .filter(material -> group.stream().allMatch(step -> matches(material, step)))
                .toList();
    }

    private static List<IndustrialMaterial> bindingMaterials(
            AssemblyMetal override,
            IndustrialMaterial inheritedMaterial
    ) {
        if (override == null) {
            return inheritedMaterial == null ? List.of() : List.of(inheritedMaterial);
        }
        if (override.isAny()) return IndustrialMaterials.ALL;
        return List.of(override.resolve());
    }

    private static boolean matchesRelative(
            IndustrialMaterial candidate,
            MaterialPart part,
            IndustrialMaterial parentMaterial,
            List<AssemblyRelativeRequirement> requirements
    ) {
        if (requirements.isEmpty()) return true;
        if (parentMaterial == null) return false;
        return requirements.stream()
                .map(requirement -> requirement.resolveAgainst(parentMaterial, part))
                .allMatch(requirement -> requirement.matches(candidate, part));
    }

    private static boolean matches(IndustrialMaterial material, AssemblyPlan.Step step) {
        if (!material.has(step.material())) return false;
        for (AssemblyRequirement requirement : step.requirements()) {
            if (!requirement.matches(material, step.material())) return false;
        }
        return true;
    }

    private static Item resolveItem(IndustrialMaterial material, MaterialPart part) {
        if (material.hasExistingPart(part)) {
            return BuiltInRegistries.ITEM.getOptional(material.existingPart(part)).orElse(null);
        }

        for (WireThickness thickness : WireThickness.ALL) {
            if (thickness.materialPart() != part) continue;
            var materialWires = ItemRegistry.ENERGY_WIRES.get(material.id());
            var holder = materialWires == null ? null : materialWires.get(thickness);
            return holder == null ? null : holder.get();
        }

        var materialItems = ItemRegistry.MATERIAL_ITEMS.get(material.id());
        var holder = materialItems == null ? null : materialItems.get(part);
        if (holder != null) return holder.get();

        return BuiltInRegistries.ITEM.getOptional(
                ResourceLocation.parse(MaterialRecipeHelper.itemId(material, part))
        ).orElse(null);
    }
}
