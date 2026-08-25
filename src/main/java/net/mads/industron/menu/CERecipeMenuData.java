package net.mads.industron.menu;

import net.mads.industron.machine.runtime.CERecipeLogic;
import net.mads.industron.machine.runtime.CERecipeLogicMachine;
import net.mads.industron.machine.control.MachineControlTarget;
import net.minecraft.world.inventory.ContainerData;

import java.util.function.BooleanSupplier;

public final class CERecipeMenuData implements ContainerData {
    public static final int FORMED = 0;
    public static final int PROCESSING = 1;
    public static final int PROGRESS = 2;
    public static final int DURATION = 3;
    public static final int PARALLEL = 4;
    public static final int RESOURCE_PER_TICK = 5;
    public static final int ENABLED = 6;
    public static final int COUNT = 7;

    private final CERecipeLogicMachine machine;
    private final BooleanSupplier formed;

    public CERecipeMenuData(CERecipeLogicMachine machine, BooleanSupplier formed) {
        this.machine = machine;
        this.formed = formed;
    }

    @Override
    public int get(int index) {
        CERecipeLogic logic = machine.recipeLogic();
        return switch (index) {
            case FORMED -> formed.getAsBoolean() ? 1 : 0;
            case PROCESSING -> isMachineEnabled() && logic.isProcessing() ? 1 : 0;
            case PROGRESS -> logic.progress();
            case DURATION -> logic.duration();
            case PARALLEL -> logic.parallel();
            case RESOURCE_PER_TICK -> isMachineEnabled() ? logic.resourcePerTick() : 0;
            case ENABLED -> isMachineEnabled() ? 1 : 0;
            default -> 0;
        };
    }

    private boolean isMachineEnabled() {
        return !(machine instanceof MachineControlTarget target) || target.isMachineEnabled();
    }

    @Override
    public void set(int index, int value) {
    }

    @Override
    public int getCount() {
        return COUNT;
    }
}
