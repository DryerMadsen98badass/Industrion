package net.mads.industron.tool;

import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.recipe.recipetypes.assembly.ToolDefinition;
import net.mads.industron.registry.ItemRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

/** Creates validated finished tool stacks from Assembly's captured permanent part materials. */
public final class ToolStackFactory {
    private ToolStackFactory() {
    }

    public static ItemStack create(ToolDefinition definition, Map<String, IndustrialSubstance> capturedMaterials) {
        if (definition == null || !definition.isAssembledTool()) {
            throw new IllegalArgumentException("Dynamic tool output requires an assembled tool definition");
        }
        if (capturedMaterials == null) {
            throw new IllegalArgumentException("Tool material captures cannot be null");
        }

        Map<String, String> materialKeys = new LinkedHashMap<>();
        for (ToolDefinition.PartSlot slot : definition.parts()) {
            IndustrialSubstance material = capturedMaterials.get(slot.role());
            if (material == null) {
                throw new IllegalStateException("Missing captured tool part material: " + definition.id() + "/" + slot.role());
            }
            if (!ToolMaterialRules.allows(material, slot.part())) {
                throw new IllegalStateException(
                        "Invalid material " + material.id() + " for tool part " + definition.id() + "/" + slot.role()
                );
            }
            materialKeys.put(slot.role(), ToolMaterialResolver.key(material));
        }

        ToolStackData data = new ToolStackData(materialKeys);
        ToolStats stats = ToolStatCalculator.calculate(definition, data);
        if (stats == null) {
            throw new IllegalStateException("Cannot calculate finished stats for tool " + definition.id());
        }

        var holder = ItemRegistry.getComposedTool(definition.id());
        if (holder == null) {
            throw new IllegalStateException("No composed tool item registered for " + definition.id());
        }

        IndustrialSubstance reference = capturedMaterials.get(definition.strengthReferenceRole());
        var existing = EquipmentExistingItems.get(reference.id(), definition.id());
        ItemStack stack = new ItemStack(existing == null ? holder.get() : existing);
        stack.set(ToolComponents.PARTS.get(), data);
        stack.set(DataComponents.MAX_DAMAGE, stats.durability());
        stack.set(DataComponents.DAMAGE, 0);
        if (existing != null) {
            stack.set(DataComponents.ATTRIBUTE_MODIFIERS, EquipmentItems.armourAttributes(definition, data));
        }
        return stack;
    }
}
