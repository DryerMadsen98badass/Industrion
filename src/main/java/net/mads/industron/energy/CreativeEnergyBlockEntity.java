package net.mads.industron.energy;

import net.mads.industron.machine.MachineTier;
import net.mads.industron.machine.MachineTierStats;
import net.mads.industron.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class CreativeEnergyBlockEntity extends BlockEntity {
    private MachineTier tier = MachineTier.ULV;
    private int amps = 1;
    private long stored;
    private final CEEnergyContainer container = new CreativeContainer();

    public CreativeEnergyBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.CREATIVE_ENERGY.get(), pos, blockState);
    }

    @Nullable
    public CEEnergyContainer ceContainer() {
        return container;
    }

    public MachineTier tier() {
        return tier;
    }

    public int amps() {
        return amps;
    }

    public long ceStored() {
        return stored;
    }

    public long ceCapacity() {
        return MachineTierStats.ceCapacity(tier);
    }

    public void cycleTier() {
        int next = (MachineTierStats.tierIndex(tier) + 1) % MachineTier.ALL.size();
        tier = MachineTier.ALL.get(next);
        stored = provider() ? ceCapacity() : Math.min(stored, ceCapacity());
        contentChanged();
    }

    public void cycleAmps() {
        amps *= 2;
        if (amps > 16) {
            amps = 1;
        }
        contentChanged();
    }

    public String statusText() {
        return (provider() ? "Provider" : "Consumer") + ": " + tier.displayName() + " " + MachineTierStats.ceTier(tier) + " CE, " + amps + "A";
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CreativeEnergyBlockEntity blockEntity) {
        if (blockEntity.provider()) {
            blockEntity.fillProviderBuffer();
            CEEnergyNetwork.outputToAdjacentWires(level, pos, blockEntity.container);
        }
    }

    private void fillProviderBuffer() {
        if (stored != ceCapacity()) {
            stored = ceCapacity();
            contentChanged();
        }
    }

    private boolean provider() {
        return getBlockState().getBlock() instanceof CreativeEnergyBlock block && block.provider();
    }

    private void contentChanged() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("Tier", tier.id());
        tag.putInt("Amps", amps);
        tag.putLong("CE", stored);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Tier")) {
            tier = MachineTier.ALL.stream()
                    .filter(machineTier -> machineTier.id().equals(tag.getString("Tier")))
                    .findFirst()
                    .orElse(MachineTier.ULV);
        }
        amps = Math.max(1, tag.getInt("Amps"));
        stored = Math.max(0L, Math.min(tag.getLong("CE"), ceCapacity()));
    }

    private class CreativeContainer implements CEEnergyContainer {
        private long acceptedAmperage;
        private long lastInputTick = Long.MIN_VALUE;

        @Override
        public MachineTier tier() {
            return CreativeEnergyBlockEntity.this.tier;
        }

        @Override
        public long voltage() {
            return MachineTierStats.ceTier(tier);
        }

        @Override
        public long getEnergyStored() {
            return ceStored();
        }

        @Override
        public long getEnergyCapacity() {
            return ceCapacity();
        }

        @Override
        public long getInputAmperage() {
            return provider() ? 0 : amps;
        }

        @Override
        public long getOutputAmperage() {
            return provider() ? amps : 0;
        }

        @Override
        public long getInputVoltage() {
            return MachineTierStats.ceTier(tier);
        }

        @Override
        public long getOutputVoltage() {
            return MachineTierStats.ceTier(tier);
        }

        @Override
        public boolean inputsEnergy(Direction side) {
            return !provider();
        }

        @Override
        public boolean outputsEnergy(Direction side) {
            return provider();
        }

        @Override
        public long acceptEnergyFromNetwork(Direction side, long voltage, long amperage) {
            if (!inputsEnergy(side) || voltage <= 0 || amperage <= 0) {
                return 0;
            }
            if (voltage > getInputVoltage()) {
                explodeEnergyBlock();
                return Math.min(amperage, remainingInputAmperage());
            }
            long acceptedAmps = Math.min(amperage, remainingInputAmperage());
            acceptedAmps = Math.min(acceptedAmps, (ceCapacity() - stored) / voltage);
            if (acceptedAmps > 0) {
                changeEnergy(acceptedAmps * voltage);
                acceptedAmperage += acceptedAmps;
            }
            return acceptedAmps;
        }

        @Override
        public long changeEnergy(long differenceAmount) {
            long previous = stored;
            stored = Math.max(0L, Math.min(ceCapacity(), stored + differenceAmount));
            if (stored != previous) {
                contentChanged();
            }
            return stored - previous;
        }

        private long remainingInputAmperage() {
            long tick = level == null ? Long.MIN_VALUE : level.getGameTime();
            if (tick != lastInputTick) {
                acceptedAmperage = 0L;
                lastInputTick = tick;
            }
            return Math.max(0L, getInputAmperage() - acceptedAmperage);
        }
    }

    private void explodeEnergyBlock() {
        if (level == null || level.isClientSide()) {
            return;
        }
        level.explode(null, worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D, 4.0F, Level.ExplosionInteraction.BLOCK);
    }
}
