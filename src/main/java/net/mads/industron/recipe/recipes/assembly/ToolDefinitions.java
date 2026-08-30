package net.mads.industron.recipe.recipes.assembly;

import net.mads.industron.recipe.recipetypes.assembly.AssemblyToolType;
import net.mads.industron.recipe.recipetypes.assembly.AssemblyTools;
import net.mads.industron.recipe.recipetypes.assembly.ToolDefinition;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Items;

/** Concrete assembly tool definitions used by recipe/component vocabulary. */
public final class ToolDefinitions {
    public static final AssemblyToolType PICKAXE = new AssemblyToolType("pickaxe", "Pickaxe");

    private static boolean registered;

    private ToolDefinitions() {
    }

    public static void bootstrap() {
        if (registered) return;
        registered = true;
        AssemblyTools.register(new ToolDefinition(Items.WOODEN_PICKAXE, PICKAXE, 100, SoundEvents.WOOD_HIT));
        AssemblyTools.register(new ToolDefinition(Items.STONE_PICKAXE, PICKAXE, 40, SoundEvents.STONE_HIT));
        AssemblyTools.register(new ToolDefinition(Items.NETHERITE_PICKAXE, PICKAXE, 10, SoundEvents.NETHERITE_BLOCK_HIT));
    }
}
