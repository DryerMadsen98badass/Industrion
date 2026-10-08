package net.mads.industron.progression;

import net.mads.industron.tool.EquipmentReplacement;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/** Rewrites prefilled structure containers, including nested container components, once at placement. */
public final class ProgressionStructureNbt {
    private ProgressionStructureNbt() {}

    public static CompoundTag replace(CompoundTag original, HolderLookup.Provider registries, boolean nether) {
        if (original == null) return null;
        CompoundTag copy = original.copy();
        rewrite(copy, registries, nether);
        return copy.equals(original) ? original : copy;
    }

    private static void rewrite(Tag tag, HolderLookup.Provider registries, boolean nether) {
        if (tag instanceof ListTag list) {
            for (Tag child : list) rewrite(child, registries, nether);
        } else if (tag instanceof CompoundTag compound) {
            for (String key : java.util.List.copyOf(compound.getAllKeys()))
                rewrite(compound.get(key), registries, nether);
            if (!compound.contains("id", Tag.TAG_STRING) || !compound.contains("count", Tag.TAG_ANY_NUMERIC)) return;
            ItemStack original = ItemStack.parseOptional(registries, compound);
            ItemStack changed = EquipmentReplacement.replace(ProgressionMaterials.replace(original, nether), nether);
            if (changed == original) return;
            compound.remove("id"); compound.remove("count"); compound.remove("components");
            if (changed.isEmpty()) return;
            CompoundTag saved = (CompoundTag) changed.save(registries);
            for (String key : saved.getAllKeys()) compound.put(key, saved.get(key).copy());
        }
    }
}
