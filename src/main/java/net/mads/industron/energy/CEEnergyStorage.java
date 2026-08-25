package net.mads.industron.energy;

import net.mads.industron.machine.MachineTier;
import net.mads.industron.machine.MachineTierStats;
import net.minecraft.core.Direction;

import java.util.function.BooleanSupplier;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;

public class CEEnergyStorage implements CEEnergyContainer {
    private final MachineTier tier;
    private final long capacity;
    private final BooleanSupplier acceptsInput;
    private final BooleanSupplier allowsOutput;
    private final LongConsumer changeListener;
    private final LongConsumer overVoltageListener;
    private final LongSupplier gameTime;
    private long stored;
    private long acceptedAmperage;
    private long lastInputTick = Long.MIN_VALUE;

    public CEEnergyStorage(
            MachineTier tier,
            long capacity,
            BooleanSupplier acceptsInput,
            BooleanSupplier allowsOutput,
            LongConsumer changeListener,
            LongSupplier gameTime
    ) {
        this(tier, capacity, acceptsInput, allowsOutput, changeListener, ignored -> {
        }, gameTime);
    }

    public CEEnergyStorage(
            MachineTier tier,
            long capacity,
            BooleanSupplier acceptsInput,
            BooleanSupplier allowsOutput,
            LongConsumer changeListener,
            LongConsumer overVoltageListener,
            LongSupplier gameTime
    ) {
        this.tier = tier;
        this.capacity = capacity;
        this.acceptsInput = acceptsInput;
        this.allowsOutput = allowsOutput;
        this.changeListener = changeListener;
        this.overVoltageListener = overVoltageListener;
        this.gameTime = gameTime;
    }

    @Override
    public MachineTier tier() {
        return tier;
    }

    @Override
    public long voltage() {
        return MachineTierStats.ceTier(tier);
    }

    @Override
    public long getEnergyStored() {
        return stored;
    }

    @Override
    public long getEnergyCapacity() {
        return capacity;
    }

    @Override
    public long getInputAmperage() {
        return acceptsInput.getAsBoolean() ? 1L : 0L;
    }

    @Override
    public long getOutputAmperage() {
        return allowsOutput.getAsBoolean() ? 1L : 0L;
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
        return acceptsInput.getAsBoolean();
    }

    @Override
    public boolean outputsEnergy(Direction side) {
        return allowsOutput.getAsBoolean();
    }

    @Override
    public long acceptEnergyFromNetwork(Direction side, long voltage, long amperage) {
        if (!inputsEnergy(side) || voltage <= 0 || amperage <= 0) {
            return 0;
        }
        if (voltage > getInputVoltage()) {
            overVoltageListener.accept(voltage);
            return Math.min(amperage, remainingInputAmperage());
        }
        long acceptedAmps = Math.min(amperage, remainingInputAmperage());
        acceptedAmps = Math.min(acceptedAmps, getEnergyCanBeInserted() / voltage);
        if (acceptedAmps > 0) {
            changeEnergy(acceptedAmps * voltage);
            acceptedAmperage += acceptedAmps;
        }
        return acceptedAmps;
    }

    @Override
    public long changeEnergy(long differenceAmount) {
        long previous = stored;
        setStored(stored + differenceAmount);
        return stored - previous;
    }

    public void setStored(long stored) {
        long next = Math.max(0L, Math.min(capacity, stored));
        if (this.stored == next) {
            return;
        }
        this.stored = next;
        changeListener.accept(next);
    }

    private long remainingInputAmperage() {
        long tick = gameTime.getAsLong();
        if (tick != lastInputTick) {
            acceptedAmperage = 0L;
            lastInputTick = tick;
        }
        return Math.max(0L, getInputAmperage() - acceptedAmperage);
    }
}
