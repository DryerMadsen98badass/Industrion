package net.mads.industron.machine.runtime;

import java.util.Optional;

public interface CERecipeLogicHost {
    boolean recipeMachineReady();

    Optional<CERecipeExecution> findAndConsumeRecipeInputs();

    CERecipeTickResult consumeRecipeTick(CERecipeExecution execution);

    boolean canCompleteRecipe(CERecipeExecution execution);

    boolean completeRecipe(CERecipeExecution execution);

    /**
     * Whether WAIT_FOR_RESOURCE should reset the active recipe progress.
     * The default keeps the existing behavior for all current hosts.
     */
    default boolean resetDurationWhenResourceMissing() {
        return true;
    }

    void onRecipeLogicChanged(boolean activeChanged);
}
