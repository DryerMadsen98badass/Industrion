package net.mads.industron.machine.control;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Set;

public interface MachineControlScheduleHost {
    String NBT_KEY = "MachineControlSchedules";

    EnumMap<Direction, MachineControlSchedule> machineControlSchedules();

    MachineControlVariableStore machineControlVariables();

    boolean acceptsMachineControlSchedules();

    void machineControlSchedulesChanged();

    @Nullable
    MachineControlTarget machineControlTarget();

    default boolean hasMachineControlSchedule(Direction side) {
        return side != null && machineControlSchedules().containsKey(side);
    }

    @Nullable
    default MachineControlSchedule machineControlSchedule(Direction side) {
        return side == null ? null : machineControlSchedules().get(side);
    }

    default Set<Direction> machineControlScheduleSides() {
        return Set.copyOf(machineControlSchedules().keySet());
    }

    default boolean installMachineControlSchedule(Direction side) {
        return installMachineControlSchedule(side, new MachineControlSchedule());
    }

    default boolean installMachineControlSchedule(Direction side, MachineControlSchedule schedule) {
        if (!acceptsMachineControlSchedules()
                || side == null
                || schedule == null
                || machineControlSchedules().containsKey(side)) {
            return false;
        }

        MachineControlSchedule installed = schedule.copy();
        machineControlVariables().adopt(installed);
        machineControlSchedules().put(side, installed);
        synchronizeMachineControlVariables();
        machineControlSchedulesChanged();
        return true;
    }

    @Nullable
    default MachineControlSchedule removeMachineControlScheduleAndGet(Direction side) {
        if (side == null) {
            return null;
        }

        MachineControlSchedule removed = machineControlSchedules().remove(side);
        if (removed != null) {
            machineControlVariables().synchronize(removed);
            if (machineControlSchedules().isEmpty()) machineControlVariables().clear();
            machineControlSchedulesChanged();
        }
        return removed;
    }

    default boolean removeMachineControlSchedule(Direction side) {
        return removeMachineControlScheduleAndGet(side) != null;
    }

    default int machineControlRedstoneInput(Direction side) {
        if (!(this instanceof BlockEntity blockEntity) || side == null) {
            return 0;
        }

        Level level = blockEntity.getLevel();
        if (level == null) {
            return 0;
        }

        return Math.max(0, Math.min(15, level.getSignal(blockEntity.getBlockPos().relative(side), side)));
    }

    default boolean evaluateMachineControlSchedule(Direction side) {
        MachineControlSchedule schedule = machineControlSchedule(side);
        MachineControlTarget target = machineControlTarget();
        if (schedule == null) return false;
        int redstone = machineControlRedstoneInput(side);
        return schedule.evaluate(target == null ? () -> redstone : target.machineControlContext(redstone),
                machineControlVariables()).machineEnabled();
    }

    /**
     * Evaluates all sides as one host-local program group. If a schedule writes a shared variable,
     * one additional pass makes that value visible to schedules evaluated earlier in the same tick.
     */
    default boolean evaluateAllMachineControlSchedules() {
        if (machineControlSchedules().isEmpty()) return true;
        long revision = machineControlVariables().revision();
        boolean enabled = true;
        for (Direction side : machineControlSchedules().keySet()) enabled &= evaluateMachineControlSchedule(side);
        if (machineControlVariables().revision() != revision) {
            enabled = true;
            for (Direction side : machineControlSchedules().keySet()) enabled &= evaluateMachineControlSchedule(side);
        }
        return enabled;
    }

    default int machineControlRedstoneOutput(Direction side) {
        MachineControlSchedule schedule = machineControlSchedule(side);
        MachineControlTarget target = machineControlTarget();
        if (schedule == null) return 0;
        int redstone = machineControlRedstoneInput(side);
        return schedule.evaluate(target == null ? () -> redstone : target.machineControlContext(redstone),
                machineControlVariables()).redstoneOutput();
    }

    default MachineControlVariableStore.Entry addMachineControlVariable(String name) {
        MachineControlVariableStore.Entry variable = machineControlVariables().add(name, 0);
        if (variable != null) {
            synchronizeMachineControlVariables();
            machineControlSchedulesChanged();
        }
        return variable;
    }

    default boolean renameMachineControlVariable(int id, String name) {
        if (!machineControlVariables().rename(id, name)) return false;
        synchronizeMachineControlVariables();
        machineControlSchedulesChanged();
        return true;
    }

    default boolean deleteMachineControlVariable(int id) {
        if (!machineControlVariables().delete(id)) return false;
        for (MachineControlSchedule schedule : machineControlSchedules().values()) schedule.deleteVariable(id);
        synchronizeMachineControlVariables();
        machineControlSchedulesChanged();
        return true;
    }

    default boolean setMachineControlVariableValue(int id, int value) {
        if (!machineControlVariables().setValue(id, value)) return false;
        synchronizeMachineControlVariables();
        machineControlSchedulesChanged();
        return true;
    }

    default void synchronizeMachineControlVariables() {
        for (MachineControlSchedule schedule : machineControlSchedules().values()) {
            machineControlVariables().synchronize(schedule);
        }
    }

    default void saveMachineControlSchedules(CompoundTag parent) {
        synchronizeMachineControlVariables();
        machineControlVariables().save(parent);
        if (machineControlSchedules().isEmpty()) {
            parent.remove(NBT_KEY);
            return;
        }

        CompoundTag schedules = new CompoundTag();
        machineControlSchedules().forEach((side, schedule) ->
                schedules.put(side.getSerializedName(), schedule.save())
        );
        parent.put(NBT_KEY, schedules);
    }

    default void loadMachineControlSchedules(CompoundTag parent) {
        machineControlSchedules().clear();
        machineControlVariables().load(parent);
        if (!acceptsMachineControlSchedules() || !parent.contains(NBT_KEY, Tag.TAG_COMPOUND)) {
            return;
        }

        CompoundTag schedules = parent.getCompound(NBT_KEY);
        for (Direction side : Direction.values()) {
            if (schedules.contains(side.getSerializedName(), Tag.TAG_COMPOUND)) {
                MachineControlSchedule schedule = MachineControlSchedule.load(schedules.getCompound(side.getSerializedName()));
                machineControlVariables().adopt(schedule);
                machineControlSchedules().put(side, schedule);
            }
        }
        synchronizeMachineControlVariables();
    }
}
