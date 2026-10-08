package net.mads.industron.runtime;

import net.mads.industron.machine.foundry.FoundryBlockEntity;
import net.mads.industron.machine.machines.electric.multiblock.MultiblockControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import java.lang.ref.WeakReference;
import java.util.*;

/** Server-thread position index; no per-tick area searches. Cosmetic state changes are ignored. */
public final class StructureWatch {
    private static final Map<Level, Map<Long, List<Watch>>> LEVELS = new WeakHashMap<>();
    private static final Map<BlockEntity, List<Long>> REGISTRATIONS = new WeakHashMap<>();
    private static final class Watch {
        final WeakReference<BlockEntity> owner;
        BlockState structuralState;
        Watch(WeakReference<BlockEntity> owner, BlockState state) { this.owner = owner; structuralState = state; }
    }
    private StructureWatch() {}
    public static void unload(Level level) {
        LEVELS.remove(level);
        REGISTRATIONS.keySet().removeIf(owner -> owner.getLevel() == level);
    }
    public static void clear() { LEVELS.clear(); REGISTRATIONS.clear(); }
    public static void register(BlockEntity owner, Iterable<BlockPos> positions) {
        Level level = owner.getLevel();
        if (level == null || level.isClientSide()) return;
        List<Long> keys = new ArrayList<>();
        for (BlockPos pos : positions) {
            if (keys.size() >= 16384) return; // Extremely tall structures retain the slow safety validation.
            keys.add(pos.asLong());
        }
        if (keys.equals(REGISTRATIONS.get(owner))) return;
        unregister(owner);
        REGISTRATIONS.put(owner, List.copyOf(keys));
        Map<Long, List<Watch>> watched = LEVELS.computeIfAbsent(level, ignored -> new HashMap<>());
        for (long key : keys) {
            BlockPos pos = BlockPos.of(key);
            if (!level.hasChunkAt(pos)) continue;
            watched.computeIfAbsent(pos.asLong(), ignored -> new ArrayList<>())
                    .add(new Watch(new WeakReference<>(owner), structural(level.getBlockState(pos))));
        }
    }
    public static void unregister(BlockEntity owner) {
        Map<Long, List<Watch>> watched = LEVELS.get(owner.getLevel());
        if (watched == null) return;
        List<Long> keys = REGISTRATIONS.remove(owner);
        if (keys == null) return;
        for (long key : keys) {
            List<Watch> list = watched.get(key);
            if (list == null) continue;
            list.removeIf(watch -> { BlockEntity target = watch.owner.get(); return target == null || target == owner; });
            if (list.isEmpty()) watched.remove(key);
        }
    }
    public static void changed(Level level, BlockPos pos, BlockState state) {
        Map<Long, List<Watch>> watched = LEVELS.get(level);
        if (watched == null) return;
        List<Watch> list = watched.get(pos.asLong());
        if (list == null) return;
        BlockState structural = structural(state);
        list.removeIf(watch -> { BlockEntity owner = watch.owner.get(); return owner == null || owner.isRemoved(); });
        for (Watch watch : list) {
            if (watch.structuralState == structural) continue;
            watch.structuralState = structural;
            BlockEntity owner = watch.owner.get();
            if (owner instanceof FoundryBlockEntity foundry) foundry.markStructureDirty();
            else if (owner instanceof MultiblockControllerBlockEntity controller) controller.markStructureDirty();
        }
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BlockState structural(BlockState state) {
        for (Property property : state.getProperties()) {
            String name = property.getName();
            if (name.equals("active") || name.equals("overlay_frame") || name.equals("formed"))
                state = state.setValue(property, (Comparable) property.getPossibleValues().iterator().next());
        }
        return state;
    }
}
