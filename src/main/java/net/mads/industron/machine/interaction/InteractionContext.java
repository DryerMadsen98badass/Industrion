package net.mads.industron.machine.interaction;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;
import java.util.Map;

/** Runtime data needed to evaluate interactions for either a singleblock or a multiblock. */
public interface InteractionContext {
    Level level();
    BlockPos origin();
    Direction facing();
    List<ItemStack> itemInputs();
    List<FluidStack> fluidInputs();
    InteractionWearStore wearStore();

    /** Resolved named areas for the current concrete machine instance. */
    default Map<String, MachineArea.Resolved> areas() {
        return Map.of();
    }
}
