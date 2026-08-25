package net.mads.industron.machine.runtime;

public enum CERecipeStatus {
    IDLE,
    WORKING,
    WAITING_FOR_RESOURCE,
    WAITING_FOR_CB,
    WAITING_FOR_RPM,
    WAITING_FOR_OUTPUT,
    MACHINE_INVALID
}
