package net.mads.industron.tool;
import net.mads.industron.recipe.recipetypes.assembly.ToolDefinition;
import net.minecraft.world.item.ItemStack;
/** Shared material identity for plain tools and native Minecraft equipment. */
public interface MaterialEquipment {
    ToolDefinition definition();
    default ToolStackData data(ItemStack stack) { return stack.get(ToolComponents.PARTS.get()); }
    default ToolStats stats(ItemStack stack) { return ToolStatCalculator.calculate(definition(), data(stack)); }
    default boolean valid(ItemStack stack) { return stats(stack) != null; }
}
