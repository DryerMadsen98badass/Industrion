package net.mads.industron.energy;

import net.mads.industron.machine.MachineTier;
import net.minecraft.core.Direction;

public interface CEEnergyContainer {
    MachineTier tier();

    long acceptEnergyFromNetwork(Direction side, long voltage, long amperage);

    boolean inputsEnergy(Direction side);

    boolean outputsEnergy(Direction side);

    long changeEnergy(long differenceAmount);

    long getEnergyStored();

    long getEnergyCapacity();

    long getInputAmperage();

    long getInputVoltage();

    long getOutputAmperage();

    long getOutputVoltage();

    default long getEnergyCanBeInserted() {
        return getEnergyCapacity() - getEnergyStored();
    }

    default long addEnergy(long energyToAdd) {
        return changeEnergy(energyToAdd);
    }

    default long removeEnergy(long energyToRemove) {
        return -changeEnergy(-energyToRemove);
    }

    default long voltage() {
        return getInputVoltage();
    }

    default long stored() {
        return getEnergyStored();
    }

    default long capacity() {
        return getEnergyCapacity();
    }

    default long maxInputAmps() {
        return getInputAmperage();
    }

    default long maxOutputAmps() {
        return getOutputAmperage();
    }

    default boolean canInput(Direction side) {
        return inputsEnergy(side);
    }

    default boolean canOutput(Direction side) {
        return outputsEnergy(side);
    }

    default long insert(long amount, boolean simulate) {
        long inserted = Math.max(0L, Math.min(amount, getEnergyCanBeInserted()));
        if (!simulate && inserted > 0) {
            changeEnergy(inserted);
        }
        return inserted;
    }

    default long extract(long amount, boolean simulate) {
        long extracted = Math.max(0L, Math.min(amount, getEnergyStored()));
        if (!simulate && extracted > 0) {
            changeEnergy(-extracted);
        }
        return extracted;
    }
}
