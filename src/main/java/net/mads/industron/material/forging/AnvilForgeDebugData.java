package net.mads.industron.material.forging;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Small synced debug payload carried only by the non-pickable visual item above an anvil. */
public final class AnvilForgeDebugData {
    private static final String MARKER = "IndustronAnvilForgeVisual";
    private static final String FORGE_VALUE = "ForgeValue";
    private static final String ANVIL_POS = "AnvilPos";

    private AnvilForgeDebugData() {
    }

    public static void write(ItemStack stack, BlockPos pos, int forgeValue) {
        CompoundTag tag = stack.get(DataComponents.CUSTOM_DATA) == null
                ? new CompoundTag()
                : stack.get(DataComponents.CUSTOM_DATA).copyTag();
        tag.putBoolean(MARKER, true);
        tag.putLong(ANVIL_POS, pos.asLong());
        tag.putInt(FORGE_VALUE, forgeValue);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static Integer readForgeValue(ItemStack stack, BlockPos pos) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        CompoundTag tag = data.copyTag();
        if (!tag.getBoolean(MARKER) || tag.getLong(ANVIL_POS) != pos.asLong()) return null;
        return tag.getInt(FORGE_VALUE);
    }
}
