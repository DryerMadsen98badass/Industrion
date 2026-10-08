package net.mads.industron.machine.machines.kinetic.lathe;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.mads.industron.machine.machines.kinetic.KineticMachines;
import net.mads.industron.machine.runtime.CERecipeLogic;
import net.mads.industron.machine.runtime.CERecipeLogicMachine;
import net.mads.industron.recipe.CERecipeTypes;
import net.mads.industron.integration.create.kinetic.CEKineticProcessing;
import net.mads.industron.integration.create.kinetic.CEKineticRecipeHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;

public final class LatheBlockEntity extends KineticBlockEntity implements CEKineticRecipeHost, CERecipeLogicMachine {
    private final CEKineticProcessing processing = new CEKineticProcessing(this, CERecipeTypes.TURNING, 16);
    public LatheBlockEntity(BlockPos pos, BlockState state) { super(KineticMachines.LATHE_ENTITY.get(), pos, state); }
    @Override public CEKineticProcessing ceProcessing() { return processing; }
    @Override public CERecipeLogic recipeLogic() { return processing.logic(); }
    @Override public void tick() { super.tick(); processing.tick(); }
    @Override public float calculateStressApplied() { lastStressApplied = 4F; return lastStressApplied; }
    @Override public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        super.addToGoggleTooltip(tooltip, sneaking);
        return processing.goggles(tooltip);
    }
    @Override protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket); processing.write(tag, registries);
    }
    @Override protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket); processing.read(tag, registries);
    }
    @Override public void destroy() { super.destroy(); processing.destroy(); }
    @Override public void invalidate() { super.invalidate(); invalidateCapabilities(); }
}
