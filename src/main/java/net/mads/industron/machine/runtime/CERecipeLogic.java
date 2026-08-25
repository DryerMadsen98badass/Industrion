package net.mads.industron.machine.runtime;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

public final class CERecipeLogic {
    private static final int IDLE_SEARCH_INTERVAL = 20;

    private final CERecipeLogicHost host;
    private CERecipeStatus status = CERecipeStatus.IDLE;
    private CERecipeExecution execution;
    private int progress;
    private int searchCooldown;

    public CERecipeLogic(CERecipeLogicHost host) {
        this.host = host;
    }

    public void serverTick() {
        if (!host.recipeMachineReady()) {
            boolean wasActive = isActive();
            clearExecution();
            status = CERecipeStatus.MACHINE_INVALID;
            host.onRecipeLogicChanged(wasActive);
            return;
        }

        if (execution == null) {
            if (!tickRecipeSearch()) {
                return;
            }
        }

        if (progress >= execution.duration()) {
            if (!host.canCompleteRecipe(execution)) {
                setStatus(CERecipeStatus.WAITING_FOR_OUTPUT);
                return;
            }
            finishRecipeAndSearchAgain();
            return;
        }

        CERecipeTickResult tickResult = host.consumeRecipeTick(execution);
        if (tickResult == CERecipeTickResult.CANCEL) {
            clearExecution();
            host.onRecipeLogicChanged(true);
            return;
        }
        if (tickResult == CERecipeTickResult.WAIT_FOR_CB) {
            setStatus(CERecipeStatus.WAITING_FOR_CB);
            return;
        }
        if (tickResult == CERecipeTickResult.WAIT_FOR_RPM) {
            setStatus(CERecipeStatus.WAITING_FOR_RPM);
            return;
        }
        if (tickResult == CERecipeTickResult.PAUSE) {
            setStatus(CERecipeStatus.WAITING_FOR_RESOURCE);
            return;
        }
        if (tickResult == CERecipeTickResult.WAIT_FOR_RESOURCE) {
            if (host.resetDurationWhenResourceMissing()) {
                progress = 0;
            }
            setStatus(CERecipeStatus.WAITING_FOR_RESOURCE);
            return;
        }

        progress++;
        if (progress >= execution.duration()) {
            if (!host.canCompleteRecipe(execution)) {
                setStatus(CERecipeStatus.WAITING_FOR_OUTPUT);
                return;
            }
            finishRecipeAndSearchAgain();
            return;
        }

        setStatus(CERecipeStatus.WORKING);
        host.onRecipeLogicChanged(false);
    }


    private void finishRecipeAndSearchAgain() {
        if (!host.completeRecipe(execution)) {
            setStatus(CERecipeStatus.WAITING_FOR_OUTPUT);
            host.onRecipeLogicChanged(false);
            return;
        }
        execution = null;
        progress = 0;
        searchCooldown = 0;

        var prepared = host.findAndConsumeRecipeInputs();
        if (prepared.isPresent()) {
            execution = prepared.get();
            status = CERecipeStatus.WORKING;
            host.onRecipeLogicChanged(false);
            return;
        }

        status = CERecipeStatus.IDLE;
        searchCooldown = IDLE_SEARCH_INTERVAL;
        host.onRecipeLogicChanged(true);
    }

    private boolean tickRecipeSearch() {
        if (searchCooldown > 0) {
            searchCooldown--;
            setStatus(CERecipeStatus.IDLE);
            return false;
        }

        var prepared = host.findAndConsumeRecipeInputs();
        if (prepared.isEmpty()) {
            searchCooldown = IDLE_SEARCH_INTERVAL;
            setStatus(CERecipeStatus.IDLE);
            return false;
        }

        execution = prepared.get();
        progress = 0;
        searchCooldown = 0;
        setStatus(CERecipeStatus.WORKING);
        host.onRecipeLogicChanged(true);
        return true;
    }

    private void setStatus(CERecipeStatus nextStatus) {
        if (status == nextStatus) {
            return;
        }
        boolean activeChanged = isActiveStatus(status) != isActiveStatus(nextStatus);
        status = nextStatus;
        host.onRecipeLogicChanged(activeChanged);
    }

    public void cancel() {
        if (execution == null && status == CERecipeStatus.IDLE) {
            return;
        }
        boolean wasActive = isActive();
        clearExecution();
        host.onRecipeLogicChanged(wasActive);
    }

    private void clearExecution() {
        execution = null;
        progress = 0;
        status = CERecipeStatus.IDLE;
    }

    public CERecipeStatus status() {
        return status;
    }

    public boolean isActive() {
        return isActiveStatus(status);
    }

    public boolean isProcessing() {
        return execution != null;
    }

    /** Current zero-based progress of the active recipe. */
    public int progress() {
        return progress;
    }

    private static boolean isActiveStatus(CERecipeStatus status) {
        return status == CERecipeStatus.WORKING;
    }


    public int duration() {
        return execution == null ? 0 : execution.duration();
    }

    public int remaining() {
        return Math.max(0, duration() - progress);
    }

    public int resourcePerTick() {
        return execution == null ? 0 : execution.resourcePerTick();
    }

    public int parallel() {
        return execution == null ? 1 : execution.parallel();
    }

    @Nullable
    public CERecipeExecution execution() {
        return execution;
    }

    public void save(CompoundTag parent, HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Status", status.name());
        tag.putInt("Progress", progress);
        tag.putInt("SearchCooldown", searchCooldown);
        if (execution != null) {
            tag.put("Execution", execution.save(registries));
        }
        parent.put("CERecipeLogic", tag);
    }

    public void load(CompoundTag parent, HolderLookup.Provider registries) {
        if (!parent.contains("CERecipeLogic")) {
            return;
        }
        CompoundTag tag = parent.getCompound("CERecipeLogic");
        try {
            status = CERecipeStatus.valueOf(tag.getString("Status"));
        } catch (IllegalArgumentException ignored) {
            status = CERecipeStatus.IDLE;
        }
        progress = Math.max(0, tag.getInt("Progress"));
        searchCooldown = Math.max(0, tag.getInt("SearchCooldown"));
        execution = tag.contains("Execution")
                ? CERecipeExecution.load(tag.getCompound("Execution"), registries)
                : null;
        if (execution == null) {
            progress = 0;
            status = CERecipeStatus.IDLE;
        } else {
            progress = Math.min(progress, execution.duration());
        }
    }
}
