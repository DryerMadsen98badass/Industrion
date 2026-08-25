package net.mads.industron.machine.runtime;

public enum CERecipeTickResult {
    CONTINUE,
    PAUSE,
    WAIT_FOR_RESOURCE,
    WAIT_FOR_CB,
    WAIT_FOR_RPM,
    CANCEL
}
