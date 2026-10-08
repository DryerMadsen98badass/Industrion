package net.mads.industron.tool;

import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.IndustrialSubstance;
import net.mads.industron.material.MaterialLookup;
import net.mads.industron.material.MaterialPart;
import net.mads.industron.material.structure.StructureMaterial;
import net.mads.industron.material.structure.StructureMaterialItem;
import net.mads.industron.registry.ItemRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/**
 * Tool-only material lookup. Normal Assembly/material processing intentionally keeps using
 * {@link MaterialLookup}, whose public target remains IndustrialMaterial-only.
 */
public final class ToolMaterialLookup {
    private ToolMaterialLookup() {
    }

    public static Target find(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;

        if (stack.getItem() instanceof StructureMaterialItem structureItem) {
            return new Target(structureItem.material(), structureItem.part());
        }

        MaterialLookup.MaterialTarget industrial = MaterialLookup.find(stack);
        if (industrial != null) {
            return new Target(industrial.material(), industrial.part());
        }
        return null;
    }

    /** Builds the normal/cold ItemStack for one permanent tool part. */
    public static ItemStack stackFor(IndustrialSubstance material, MaterialPart part) {
        if (material == null || part == null || ToolMaterialRules.isHotToolPart(part)) return ItemStack.EMPTY;

        if (material instanceof IndustrialMaterial industrial) {
            if (industrial.hasExistingPart(part)) {
                return BuiltInRegistries.ITEM.getOptional(industrial.existingPart(part))
                        .map(ItemStack::new)
                        .orElse(ItemStack.EMPTY);
            }
            var holder = ItemRegistry.getMaterialItem(industrial, part);
            return holder == null ? ItemStack.EMPTY : new ItemStack(holder.get());
        }

        if (material instanceof StructureMaterial structure) {
            if (structure.hasExistingPart(part)) {
                return BuiltInRegistries.ITEM.getOptional(structure.existingPart(part))
                        .map(ItemStack::new)
                        .orElse(ItemStack.EMPTY);
            }
            var holder = ItemRegistry.getStructureMaterialFormItem(structure, part);
            return holder == null ? ItemStack.EMPTY : new ItemStack(holder.get());
        }

        return ItemStack.EMPTY;
    }

    public record Target(IndustrialSubstance material, MaterialPart part) {
        public Target {
            if (material == null) throw new IllegalArgumentException("Tool material cannot be null");
            if (part == null) throw new IllegalArgumentException("Tool material part cannot be null");
        }
    }
}
