package net.mads.industron.integration.jei.assembly;

import net.mads.industron.energy.WireThickness;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.material.MaterialCatalog;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.plant.PlantPart;
import net.mads.industron.material.plant.PlantPartItemCatalog;
import net.mads.industron.material.recipes.MaterialRecipeHelper;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyComponent;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyMaterialSelector;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyMaterialCatalog;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyPlan;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyRelativeRequirement;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyRequirement;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyRecipeDefinition;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyToolType;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyTools;
import net.mads.industron.recipe.recipetypes.assembly.ComponentDefinition;
import net.mads.industron.recipe.recipes.assembly.ComponentDefinitions;
import net.mads.industron.registry.ItemRegistry;
import net.mads.industron.tool.ToolMaterialLookup;
import net.mads.industron.tool.ToolMaterialRules;
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

        if (step.materialSelector().isToolSelector()) {
            List<ItemStack> result = new ArrayList<>();
            for (IndustrialSubstance material : ToolMaterialRules.candidates(step.material())) {
                if (!step.acceptsMaterial(material)) continue;
                ItemStack stack = ToolMaterialLookup.stackFor(material, step.material());
                if (stack.isEmpty()) continue;
                stack.setCount(count);
                result.add(stack);
            }
            return List.copyOf(result);
        }

        List<IndustrialSubstance> materials;
        if (step.fixedMaterial() != null) {
            materials = List.of(step.fixedMaterial());
        } else {
            materials = candidatesForBinding(step.bindingId(), plan);
        }

        Map<Item, ItemStack> result = new LinkedHashMap<>();
        for (IndustrialSubstance material : materials) {
            for (ItemStack stack : AssemblyMaterialCatalog.stacksFor(material, step.material())) {
                if (stack.isEmpty()) continue;
                stack.setCount(count);
                result.putIfAbsent(stack.getItem(), stack);
            }
        }
        return List.copyOf(result.values());
    }

    static List<ItemStack> exactItem(ResourceLocation itemId, int count) {
        Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
        return item == null ? List.of() : List.of(new ItemStack(item, count));
    }

    static List<ItemStack> plantPart(PlantPart part, int count) {
        List<ItemStack> result = new ArrayList<>();
        for (ResourceLocation id : PlantPartItemCatalog.allItemIds(part)) {
            BuiltInRegistries.ITEM.getOptional(id).ifPresent(item -> result.add(new ItemStack(item, count)));
        }
        return List.copyOf(result);
    }

    static List<ItemStack> tools(AssemblyToolType type) {
        ItemStack example = AssemblyTools.exampleStack(type);
        return example.isEmpty() ? List.of() : List.of(example);
    }

    static List<ItemStack> baseMaterial(AssemblyRecipeDefinition.BaseValue base) {
        if (base == null || !base.isMaterialSelection()) return List.of();

        if (base.materialSelector().isToolSelector()) {
            Map<Item, ItemStack> result = new LinkedHashMap<>();
            for (IndustrialSubstance material : ToolMaterialRules.candidates(base.material())) {
                if (!base.materialSelector().matchesTool(material, base.material())) continue;
                ItemStack stack = ToolMaterialLookup.stackFor(material, base.material());
                if (!stack.isEmpty()) result.putIfAbsent(stack.getItem(), stack);
            }
            return List.copyOf(result.values());
        }

        Map<Item, ItemStack> result = new LinkedHashMap<>();
        for (IndustrialSubstance material : AssemblyMaterialCatalog.candidates(base.materialSelector(), base.material())) {
            if (!base.requirements().isEmpty()) {
                if (!(material instanceof IndustrialMaterial industrial)) continue;
                if (base.requirements().stream().anyMatch(requirement -> !requirement.matches(industrial, base.material()))) continue;
            }
            for (ItemStack stack : AssemblyMaterialCatalog.stacksFor(material, base.material())) {
                if (!stack.isEmpty()) result.putIfAbsent(stack.getItem(), stack);
            }
        }
        return List.copyOf(result.values());
    }

    static List<ItemStack> directMaterial(
            MaterialPart part,
            AssemblyMaterialSelector override,
            IndustrialSubstance inheritedMaterial,
            int count
    ) {
        return directMaterial(part, override, inheritedMaterial, List.of(), count);
    }

    static List<ItemStack> directMaterial(
            MaterialPart part,
            AssemblyMaterialSelector override,
            IndustrialSubstance inheritedMaterial,
            List<AssemblyRelativeRequirement> relativeRequirements,
            int count
    ) {
        Map<Item, ItemStack> result = new LinkedHashMap<>();
        for (IndustrialSubstance material : bindingMaterials(override, inheritedMaterial, part)) {
            if (!matchesRelative(material, part, inheritedMaterial, relativeRequirements)) continue;
            for (ItemStack stack : AssemblyMaterialCatalog.stacksFor(material, part)) {
                if (stack.isEmpty()) continue;
                stack.setCount(count);
                result.putIfAbsent(stack.getItem(), stack);
            }
        }
        return List.copyOf(result.values());
    }

    static List<ItemStack> component(
            AssemblyComponent component,
            AssemblyMaterialSelector override,
            IndustrialSubstance inheritedMaterial,
            int count
    ) {
        return component(component, override, inheritedMaterial, List.of(), count);
    }

    static List<ItemStack> component(
            AssemblyComponent component,
            AssemblyMaterialSelector override,
            IndustrialSubstance inheritedMaterial,
            List<AssemblyRelativeRequirement> relativeRequirements,
            int count
    ) {
        ComponentDefinition definition = ComponentDefinitions.find(component);
        int representativeIndex = representativeIndex(definition);
        if (representativeIndex < 0) return List.of();

        if (override != null && override.isFree()) {
            if (inheritedMaterial == null && !relativeRequirements.isEmpty()) return List.of();
            final List<AssemblyPlan.Step> plan;
            try {
                if (relativeRequirements.isEmpty()) {
                    plan = AssemblyPlan.compileComponent(component, override, List.of());
                } else if (inheritedMaterial instanceof IndustrialMaterial parent) {
                    plan = AssemblyPlan.compileFreeComponent(component, parent, relativeRequirements);
                } else {
                    return List.of();
                }
            } catch (IllegalStateException exception) {
                return List.of();
            }
            for (AssemblyPlan.Step step : plan) {
                if (step.kind() == AssemblyPlan.Kind.MATERIAL) {
                    return material(step, plan, count);
                }
                if (step.kind() == AssemblyPlan.Kind.PLANT_PART) {
                    return plantPart(step.plantPart(), count);
                }
                if (step.kind() == AssemblyPlan.Kind.ITEM) {
                    return exactItem(step.itemId(), count);
                }
            }
            return List.of();
        }

        ComponentDefinition.Step representative = definition.steps().get(representativeIndex);
        Map<Item, ItemStack> result = new LinkedHashMap<>();
        for (IndustrialSubstance componentMaterial : bindingMaterials(override, inheritedMaterial, representative.material())) {
            if (!componentAvailable(component, componentMaterial)) continue;

            List<ItemStack> candidates = switch (representative.kind()) {
                case MATERIAL -> directMaterial(
                        representative.material(),
                        representative.materialOverride(),
                        componentMaterial,
                        representative.relativeRequirements(),
                        count
                );
                case PLANT_PART -> plantPart(representative.plantPart(), count);
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
                    || step.kind() == ComponentDefinition.StepKind.PLANT_PART
                    || step.kind() == ComponentDefinition.StepKind.ITEM) {
                return index;
            }
        }
        return -1;
    }

    static boolean componentAvailable(AssemblyComponent component, IndustrialSubstance inheritedMaterial) {
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
                        if (!matches(step.fixedMaterial(), step)
                                || AssemblyMaterialCatalog.stackFor(step.fixedMaterial(), step.material()).isEmpty()) {
                            return false;
                        }
                    } else if (candidatesForBinding(step.bindingId(), plan).stream()
                            .noneMatch(material -> !AssemblyMaterialCatalog.stackFor(material, step.material()).isEmpty())) {
                        return false;
                    }
                }
                case PLANT_PART -> {
                    if (plantPart(step.plantPart(), 1).isEmpty()) return false;
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

    private static List<IndustrialSubstance> candidatesForBinding(int bindingId, List<AssemblyPlan.Step> plan) {
        List<AssemblyPlan.Step> group = plan.stream()
                .filter(step -> step.kind() == AssemblyPlan.Kind.MATERIAL)
                .filter(step -> step.usesDynamicBinding() && step.bindingId() == bindingId)
                .toList();
        if (group.isEmpty()) return List.of();

        AssemblyPlan.Step first = group.getFirst();
        return AssemblyMaterialCatalog.candidates(first.materialSelector(), first.material()).stream()
                .filter(material -> group.stream().allMatch(step -> matches(material, step)))
                .toList();
    }

    private static List<IndustrialSubstance> bindingMaterials(
            AssemblyMaterialSelector override,
            IndustrialSubstance inheritedMaterial,
            MaterialPart part
    ) {
        if (override == null) {
            return inheritedMaterial == null ? List.of() : List.of(inheritedMaterial);
        }
        if (override.isFree()) return AssemblyMaterialCatalog.candidates(override, part);
        return List.of(override.resolveSubstance());
    }

    private static boolean matchesRelative(
            IndustrialSubstance candidate,
            MaterialPart part,
            IndustrialSubstance parentMaterial,
            List<AssemblyRelativeRequirement> requirements
    ) {
        if (requirements.isEmpty()) return true;
        if (!(parentMaterial instanceof IndustrialMaterial parent) || !(candidate instanceof IndustrialMaterial industrial)) return false;
        return requirements.stream()
                .map(requirement -> requirement.resolveAgainst(parent, part))
                .allMatch(requirement -> requirement.matches(industrial, part));
    }

    private static boolean matches(IndustrialSubstance material, AssemblyPlan.Step step) {
        if (!step.acceptsMaterial(material)) return false;
        if (!AssemblyMaterialCatalog.exposesPart(material, step.material())) return false;
        if (step.requirements().isEmpty()) return true;
        if (!(material instanceof IndustrialMaterial industrial)) return false;
        for (AssemblyRequirement requirement : step.requirements()) {
            if (!requirement.matches(industrial, step.material())) return false;
        }
        return true;
    }

}
