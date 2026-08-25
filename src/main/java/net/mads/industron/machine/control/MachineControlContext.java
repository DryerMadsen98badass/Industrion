package net.mads.industron.machine.control;

public interface MachineControlContext {
    int redstoneInput();

    /** Stable server tick used by schedule runtime caches. */
    default long evaluationTick() { return Long.MIN_VALUE; }

    /** Changes whenever item/fluid inputs relevant to schedules change. */
    default long inputRevision() { return evaluationTick(); }

    /** Changes whenever world-state reporters such as rain, sky access or day/night change. */
    default long worldRevision() { return evaluationTick(); }

    default boolean machineRunning() { return false; }
    default boolean hasActiveRecipe() { return false; }
    default int recipeProgress() { return 0; }
    default int recipeDuration() { return 0; }
    default int recipeMinimumChemicalBalance() { return 0; }
    default int recipeMaximumChemicalBalance() { return 0; }
    default int machineChemicalBalance() { return 0; }
    default int recipeMinimumRpm() { return 0; }
    default int recipeMaximumRpm() { return 0; }
    default int machineRpm() { return 0; }
    default long energyStored() { return 0L; }
    default long energyCapacity() { return 0L; }
    default int steamStored() { return 0; }
    default int steamCapacity() { return 0; }
    default boolean missingEnergy() { return false; }
    default boolean outputBlocked() { return false; }
    default boolean missingInput() { return false; }
    default boolean multiblockFormed() { return true; }
    default boolean isRaining() { return false; }
    default boolean isThundering() { return false; }
    default boolean hasDirectSky() { return false; }
    default boolean isDay() { return false; }
    default int temperature() { return 0; }
    default int itemInputCount() { return 0; }
    default int fluidInputAmount() { return 0; }
    default int itemInputCount(String filter) { return itemInputCount(); }
    default int fluidInputAmount(String filter) { return fluidInputAmount(); }
    default boolean itemInputMatches(String itemId) { return false; }
    default boolean itemInputMatchesTag(String tagId) { return false; }
    default boolean fluidInputMatches(String fluidId) { return false; }
    default boolean fluidInputMatchesTag(String tagId) { return false; }
}
