package net.mads.industron.recipe.recipetypes;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Extensible tool registry. Vanilla pickaxes are only test definitions.
 * IndustrialMaterials can later register generated tools through register(...).
 */
public final class AssemblyTools {
    public static final AssemblyToolType PICKAXE = new AssemblyToolType("pickaxe", "Pickaxe");

    private static final List<ToolDefinition> TOOLS = new ArrayList<>();

    static {
        register(new ToolDefinition(Items.WOODEN_PICKAXE, PICKAXE, 100, SoundEvents.WOOD_HIT));
        register(new ToolDefinition(Items.STONE_PICKAXE, PICKAXE, 40, SoundEvents.STONE_HIT));
        register(new ToolDefinition(Items.NETHERITE_PICKAXE, PICKAXE, 10, SoundEvents.NETHERITE_BLOCK_HIT));
    }

    private AssemblyTools() {
    }

    public static ToolDefinition register(ToolDefinition definition) {
        if (TOOLS.stream().anyMatch(existing -> existing.item() == definition.item() && existing.type().equals(definition.type()))) {
            throw new IllegalArgumentException("Duplicate assembly tool definition for " + definition.item() + " / " + definition.type().id());
        }
        TOOLS.add(definition);
        return definition;
    }

    public static List<ToolDefinition> all() {
        return Collections.unmodifiableList(TOOLS);
    }

    public static ToolDefinition find(AssemblyToolType type, net.minecraft.world.item.ItemStack stack) {
        return TOOLS.stream().filter(tool -> tool.type().equals(type) && tool.matches(stack)).findFirst().orElse(null);
    }
}
