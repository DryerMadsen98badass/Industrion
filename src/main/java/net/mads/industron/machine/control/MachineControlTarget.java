package net.mads.industron.machine.control;

public interface MachineControlTarget {
    boolean isMachineEnabled();

    void setMachineEnabled(boolean enabled);

    default void toggleMachineEnabled() {
        setMachineEnabled(!isMachineEnabled());
    }

    default MachineControlContext machineControlContext(int redstoneInput) {
        return () -> Math.max(0, Math.min(15, redstoneInput));
    }
}
