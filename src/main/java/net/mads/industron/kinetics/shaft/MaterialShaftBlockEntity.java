package net.mads.industron.kinetics.shaft;

import com.simibubi.create.content.kinetics.steamEngine.PoweredShaftBlockEntity;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import net.mads.industron.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * One block entity type shared by all generated material shafts.
 *
 * It extends Create's powered shaft entity so metal shafts can be driven directly
 * by a Steam Engine without being replaced by Create's normal shaft block. Wood
 * shafts reject that connection at canBePoweredBy/update.
 */
public final class MaterialShaftBlockEntity extends PoweredShaftBlockEntity {
    private boolean changingNetwork;
    private boolean removingShaft;

    public MaterialShaftBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.MATERIAL_SHAFT.get(), pos, state);
    }

    public ShaftLimits shaftLimits() {
        return getBlockState().getBlock() instanceof AbstractMaterialShaftBlock shaft
                ? shaft.shaftLimits()
                : ShaftLimits.METAL;
    }

    public boolean supportsSteamEngine() {
        return getBlockState().getBlock() instanceof AbstractMaterialShaftBlock shaft
                && shaft.supportsSteamEngine();
    }

    public float currentNetworkStress() {
        return stress;
    }


    @Override
    public void initialize() {
        super.initialize();
        syncOverloadTracking();
    }

    @Override
    public void setNetwork(Long networkIn) {
        changingNetwork = true;
        try {
            super.setNetwork(networkIn);
        } finally {
            changingNetwork = false;
        }
        syncOverloadTracking();
    }

    @Override
    public void updateFromNetwork(float maxStress, float currentStress, int networkSize) {
        super.updateFromNetwork(maxStress, currentStress, networkSize);
        if (!changingNetwork && !removingShaft && getLevel() instanceof ServerLevel) {
            ShaftOverloadManager.updateNetworkStress(this, currentStress);
        }
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        if (!changingNetwork && !removingShaft) {
            syncOverloadTracking();
        }
    }

    @Override
    public boolean canBePoweredBy(BlockPos globalPos) {
        return canAcceptSteamEngineAt(globalPos) && super.canBePoweredBy(globalPos);
    }

    @Override
    public void update(BlockPos sourcePos, int direction, float efficiency) {
        if (!canAcceptSteamEngineAt(sourcePos)) {
            return;
        }
        super.update(sourcePos, direction, efficiency);
    }

    @Override
    public int getRotationAngleOffset(Axis axis) {
        // Behave like a normal shaft until it is actually attached to a Steam Engine.
        return enginePos == null ? 0 : super.getRotationAngleOffset(axis);
    }

    @Override
    public void onChunkUnloaded() {
        // SmartBlockEntity deliberately does not call remove() for chunk unloads.
        // Drop the cached network entry here so unloaded chunks never accumulate in the manager.
        ShaftOverloadManager.unregister(this);
        super.onChunkUnloaded();
    }

    @Override
    public void remove() {
        removingShaft = true;
        ShaftOverloadManager.unregister(this);
        try {
            super.remove();
        } finally {
            ShaftOverloadManager.unregister(this);
        }
    }

    private boolean canAcceptSteamEngineAt(BlockPos enginePosition) {
        if (!supportsSteamEngine() || level == null) {
            return false;
        }
        BlockState engineState = level.getBlockState(enginePosition);
        if (!(engineState.getBlock() instanceof SteamEngineBlock)) {
            return false;
        }
        return getBlockState().getValue(BlockStateProperties.AXIS) != SteamEngineBlock.getFacing(engineState).getAxis();
    }

    private void syncOverloadTracking() {
        if (removingShaft || !(getLevel() instanceof ServerLevel)) {
            return;
        }
        ShaftOverloadManager.track(this);
    }
}
