package net.mads.industron.recipe.recipetypes.assembly;

import net.mads.industron.recipe.recipes.assembly.ToolDefinitions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Extensible tool registry.
 * IndustrialMaterials can later register generated tools through register(...).
 */
public final class AssemblyTools {
    private static final List<ToolDefinition> TOOLS = new ArrayList<>();
    private static boolean bootstrapped;

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
        bootstrapDefinitions();
        return Collections.unmodifiableList(TOOLS);
    }

    public static ToolDefinition find(AssemblyToolType type, net.minecraft.world.item.ItemStack stack) {
        bootstrapDefinitions();
        return TOOLS.stream().filter(tool -> tool.type().equals(type) && tool.matches(stack)).findFirst().orElse(null);
    }

    private static void bootstrapDefinitions() {
        if (bootstrapped) return;
        bootstrapped = true;
        ToolDefinitions.bootstrap();
    }
}
