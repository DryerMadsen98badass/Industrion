package net.mads.industron.recipe.recipetypes.assembly;

import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.machine.MachineTier;
import net.mads.industron.recipe.recipes.assembly.ToolDefinitions;
import net.mads.industron.registry.ItemRegistry;
import net.mads.industron.tool.MaterialEquipment;
import net.mads.industron.tool.ToolMaterialLookup;
import net.mads.industron.tool.ToolMaterialRules;
import net.mads.industron.tool.ToolStatCalculator;
import net.mads.industron.tool.ToolStats;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Registry/lookup for permanent dynamic Industron tool families. */
public final class AssemblyTools {
    private static final List<ToolDefinition> DEFINITIONS = new ArrayList<>();
    private static boolean bootstrapped;

    private AssemblyTools() {
    }

    public static ToolDefinition register(ToolDefinition definition) {
        if (definition == null) throw new IllegalArgumentException("Tool definition cannot be null");
        if (DEFINITIONS.stream().anyMatch(existing -> existing.id().equals(definition.id()))) {
            throw new IllegalArgumentException("Duplicate assembly tool family: " + definition.id());
        }
        DEFINITIONS.add(definition);
        return definition;
    }

    public static List<ToolDefinition> definitions() {
        bootstrapDefinitions();
        return Collections.unmodifiableList(DEFINITIONS);
    }

    public static boolean hasType(AssemblyToolType type) {
        if (type == null) return false;
        bootstrapDefinitions();
        return DEFINITIONS.stream().anyMatch(definition -> definition.type().equals(type));
    }

    public static ToolDefinition definition(AssemblyToolType type) {
        if (type == null) return null;
        bootstrapDefinitions();
        return DEFINITIONS.stream()
                .filter(definition -> definition.type().equals(type))
                .findFirst()
                .orElse(null);
    }

    public static ToolVariantDefinition find(AssemblyToolType type, ItemStack stack) {
        if (type == null || stack == null || stack.isEmpty()) return null;
        bootstrapDefinitions();
        ToolDefinition definition = DEFINITIONS.stream()
                .filter(candidate -> candidate.type().equals(type))
                .findFirst()
                .orElse(null);
        return definition == null ? null : resolve(definition, stack);
    }

    public static ToolVariantDefinition find(String typeId, ItemStack stack) {
        if (typeId == null || stack == null || stack.isEmpty()) return null;
        bootstrapDefinitions();
        ToolDefinition definition = DEFINITIONS.stream()
                .filter(candidate -> candidate.type().id().equals(typeId))
                .findFirst()
                .orElse(null);
        return definition == null ? null : resolve(definition, stack);
    }

    /** Resolves any valid finished Industron tool stack, regardless of family. */
    public static ToolVariantDefinition findAny(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        bootstrapDefinitions();
        for (ToolDefinition definition : DEFINITIONS) {
            ToolVariantDefinition resolved = resolve(definition, stack);
            if (resolved != null) return resolved;
        }
        return null;
    }

    public static Optional<AssemblyToolType> findType(String typeId) {
        bootstrapDefinitions();
        return DEFINITIONS.stream()
                .filter(definition -> definition.id().equals(typeId))
                .map(ToolDefinition::type)
                .findFirst();
    }

    /** One valid stack used by JEI/validation when a recipe wants to display a tool family. */
    public static ItemStack exampleStack(AssemblyToolType type) {
        if (type == null) return ItemStack.EMPTY;
        bootstrapDefinitions();
        ToolDefinition definition = DEFINITIONS.stream()
                .filter(candidate -> candidate.type().equals(type))
                .findFirst()
                .orElse(null);
        if (definition == null) return ItemStack.EMPTY;

        if (definition.isDirectPartTool()) {
            ToolDefinition.PartSlot slot = definition.parts().getFirst();
            for (IndustrialSubstance material : ToolMaterialRules.candidates(slot.part())) {
                ItemStack stack = toolPartStack(material, slot.part());
                if (!stack.isEmpty()) return stack;
            }
            return ItemStack.EMPTY;
        }

        var holder = ItemRegistry.getComposedTool(definition.id());
        if (holder == null) return ItemStack.EMPTY;

        // MaterialEquipment now owns a valid canonical default ToolStackData. Reuse that exact
        // stack everywhere JEI or another generic system needs one instead of synthesizing a
        // second, potentially different example material combination here.
        ItemStack stack = new ItemStack(holder.get());
        return resolve(definition, stack) == null ? ItemStack.EMPTY : stack;
    }

    private static ToolVariantDefinition resolve(ToolDefinition definition, ItemStack stack) {
        ToolStats stats;
        if (definition.isDirectPartTool()) {
            ToolDefinition.PartSlot slot = definition.parts().getFirst();
            ToolMaterialLookup.Target target = ToolMaterialLookup.find(stack);
            if (target == null || target.part() != slot.part() || !ToolMaterialRules.allows(target.material(), slot.part())) {
                return null;
            }
            if (slot.part() == MaterialPart.PEBBLE) {
                return new ToolVariantDefinition(stack.getItem(), definition.type(), 1.0D, MachineTier.LV, SoundEvents.STONE_HIT);
            }
            stats = ToolStatCalculator.calculateSingle(target.material(), slot.part());
        } else {
            if (!(stack.getItem() instanceof MaterialEquipment item) || item.definition() != definition) return null;
            stats = item.stats(stack);
        }
        if (stats == null) return null;
        return new ToolVariantDefinition(stack.getItem(), definition.type(), stats.efficiencySeconds(), stats.tier(), null);
    }

    private static ItemStack toolPartStack(IndustrialSubstance material, net.mads.industron.material.MaterialPart part) {
        if (material instanceof net.mads.industron.material.IndustrialMaterial industrial) {
            var holder = ItemRegistry.getMaterialItem(industrial, part);
            return holder == null ? ItemStack.EMPTY : new ItemStack(holder.get());
        }
        if (material instanceof net.mads.industron.material.structure.StructureMaterial structure) {
            var holder = ItemRegistry.getStructureMaterialFormItem(structure, part);
            return holder == null ? ItemStack.EMPTY : new ItemStack(holder.get());
        }
        return ItemStack.EMPTY;
    }

    private static void bootstrapDefinitions() {
        if (bootstrapped) return;
        bootstrapped = true;
        ToolDefinitions.bootstrap();
    }
}
