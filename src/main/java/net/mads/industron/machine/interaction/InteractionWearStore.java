package net.mads.industron.machine.interaction;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/** Persistent per-position wear counters owned by a machine/controller block entity. */
public final class InteractionWearStore {
    private final Map<BlockPos, Entry> entries = new HashMap<>();

    public int addWear(BlockPos pos, ResourceLocation blockId, int amount) {
        Entry entry = entries.get(pos);
        if (entry == null || !entry.blockId.equals(blockId)) {
            entry = new Entry(blockId, 0);
            entries.put(pos.immutable(), entry);
        }
        entry.wear += Math.max(0, amount);
        return entry.wear;
    }

    public void clear(BlockPos pos) {
        entries.remove(pos);
    }

    public void save(CompoundTag parent) {
        ListTag list = new ListTag();
        entries.forEach((pos, entry) -> {
            CompoundTag tag = new CompoundTag();
            tag.putLong("Pos", pos.asLong());
            tag.putString("Block", entry.blockId.toString());
            tag.putInt("Wear", entry.wear);
            list.add(tag);
        });
        parent.put("InteractionWear", list);
    }

    public void load(CompoundTag parent) {
        entries.clear();
        if (!parent.contains("InteractionWear")) {
            return;
        }
        ListTag list = parent.getList("InteractionWear", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            entries.put(BlockPos.of(tag.getLong("Pos")), new Entry(ResourceLocation.parse(tag.getString("Block")), tag.getInt("Wear")));
        }
    }

    private static final class Entry {
        private final ResourceLocation blockId;
        private int wear;

        private Entry(ResourceLocation blockId, int wear) {
            this.blockId = blockId;
            this.wear = wear;
        }
    }
}
