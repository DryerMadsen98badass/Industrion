package net.mads.industron.material.melting;

import net.mads.industron.Industron;
import net.mads.industron.material.IndustrialMaterial;
import net.mads.industron.material.MaterialPart;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * One explicitly enabled meltable material form.
 *
 * <p>The exact millibucket amount lives on MaterialPart so remelting, casting and manual forging
 * all share one physical quantity source.</p>
 */
public record MeltablePart(MaterialPart part) {
    public MeltablePart {
        if (part == null) {
            throw new IllegalArgumentException("Meltable material part cannot be null");
        }
        if (part.isFluid()) {
            throw new IllegalArgumentException("Meltable material part must be an item or block: " + part);
        }
        if (part.materialAmountMb() <= 0) {
            throw new IllegalArgumentException("Meltable material part must define its material amount on MaterialPart: " + part);
        }
    }

    public int millibuckets() {
        return part.materialAmountMb();
    }

    /** Registry id for this form of the supplied material. */
    public ResourceLocation itemId(IndustrialMaterial material) {
        if (material == null) {
            throw new IllegalArgumentException("Material cannot be null");
        }
        if (material.hasExistingPart(part)) {
            return material.existingPart(part);
        }
        return ResourceLocation.fromNamespaceAndPath(Industron.MOD_ID, part.registryName(material));
    }

    /**
     * Resolves the actual registered item for this material/form combination.
     * Intended for runtime recipe systems that need the item together with its exact mB value.
     */
    public Item item(IndustrialMaterial material) {
        ResourceLocation id = itemId(material);
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == Items.AIR) {
            throw new IllegalStateException("No registered item exists for meltable form " + id);
        }
        return item;
    }

    public ItemStack stack(IndustrialMaterial material) {
        return new ItemStack(item(material));
    }
}
