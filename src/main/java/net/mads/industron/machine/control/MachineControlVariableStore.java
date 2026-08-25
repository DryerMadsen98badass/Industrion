package net.mads.industron.machine.control;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared variable namespace for every Machine Control Schedule installed on one block entity.
 * Schedules keep local definitions for item editing and serialization, while an installed host
 * uses this store as the authoritative names and runtime values.
 */
public final class MachineControlVariableStore {
    public static final String NBT_KEY = "MachineControlVariables";

    private final Entry[] entriesById = new Entry[MachineControlSchedule.MAX_VARIABLES];
    private final List<Entry> entries = new ArrayList<>();
    private final List<Entry> entriesView = Collections.unmodifiableList(entries);
    private int nextId;
    private long revision = 1L;

    public List<Entry> entries() {
        return List.copyOf(entries);
    }

    List<Entry> entriesView() {
        return entriesView;
    }

    public Entry entry(int id) {
        return id >= 0 && id < entriesById.length ? entriesById[id] : null;
    }

    public long revision() {
        return revision;
    }

    public Entry add(String requestedName, int value) {
        if (entries.size() >= entriesById.length) return null;
        int id = nextFreeId();
        if (id < 0) return null;
        Entry entry = new Entry(id, uniqueName(cleanName(requestedName), -1), clamp(value));
        entries.add(entry);
        entriesById[id] = entry;
        nextId = (id + 1) % entriesById.length;
        revision++;
        return entry;
    }

    public boolean rename(int id, String requestedName) {
        Entry entry = entry(id);
        if (entry == null) return false;
        String name = uniqueName(cleanName(requestedName), id);
        if (entry.name.equals(name)) return false;
        entry.name = name;
        revision++;
        return true;
    }

    public boolean delete(int id) {
        Entry entry = entry(id);
        if (entry == null) return false;
        entries.remove(entry);
        entriesById[id] = null;
        revision++;
        return true;
    }

    public boolean setValue(int id, int value) {
        Entry entry = entry(id);
        if (entry == null) return false;
        int clamped = clamp(value);
        if (entry.value == clamped) return false;
        entry.value = clamped;
        revision++;
        return true;
    }

    public int value(int id) {
        Entry entry = entry(id);
        return entry == null ? 0 : entry.value;
    }

    /** Imports an item/local schedule and remaps conflicting IDs by matching variable names. */
    public void adopt(MachineControlSchedule schedule) {
        if (schedule == null) return;
        Map<Integer, Integer> remap = new HashMap<>();
        for (MachineControlSchedule.Variable variable : schedule.variables()) {
            Entry shared = findByName(variable.name());
            if (shared == null) {
                int preferredId = variable.id();
                if (preferredId >= 0 && preferredId < entriesById.length && entriesById[preferredId] == null) {
                    shared = addAt(preferredId, variable.name(), variable.value());
                } else {
                    shared = add(variable.name(), variable.value());
                }
            }
            if (shared != null) remap.put(variable.id(), shared.id);
        }
        schedule.remapVariables(remap);
        synchronize(schedule);
    }

    public void synchronize(MachineControlSchedule schedule) {
        if (schedule != null) schedule.replaceVariables(entriesView);
    }

    public void clear() {
        entries.clear();
        java.util.Arrays.fill(entriesById, null);
        nextId = 0;
        revision++;
    }

    public void save(CompoundTag parent) {
        if (entries.isEmpty()) {
            parent.remove(NBT_KEY);
            return;
        }
        CompoundTag tag = new CompoundTag();
        tag.putInt("Version", 2);
        tag.putInt("NextId", nextId);
        ListTag list = new ListTag();
        for (Entry entry : entries) list.add(entry.save());
        tag.put("Entries", list);
        parent.put(NBT_KEY, tag);
    }

    public void load(CompoundTag parent) {
        clear();
        if (!parent.contains(NBT_KEY, Tag.TAG_COMPOUND)) return;
        CompoundTag tag = parent.getCompound(NBT_KEY);
        boolean migrateWholeNumbers = tag.getInt("Version") < 2;
        ListTag list = tag.getList("Entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size() && entries.size() < entriesById.length; i++) {
            Entry loaded = Entry.load(list.getCompound(i), migrateWholeNumbers);
            if (loaded == null || loaded.id < 0 || loaded.id >= entriesById.length || entriesById[loaded.id] != null) continue;
            entries.add(loaded);
            entriesById[loaded.id] = loaded;
        }
        nextId = Math.floorMod(tag.getInt("NextId"), entriesById.length);
        revision++;
    }

    private Entry addAt(int id, String requestedName, int value) {
        if (id < 0 || id >= entriesById.length || entriesById[id] != null || entries.size() >= entriesById.length) return null;
        Entry entry = new Entry(id, uniqueName(cleanName(requestedName), -1), clamp(value));
        entries.add(entry);
        entriesById[id] = entry;
        nextId = (id + 1) % entriesById.length;
        revision++;
        return entry;
    }

    private Entry findByName(String name) {
        for (Entry entry : entries) if (entry.name.equalsIgnoreCase(name)) return entry;
        return null;
    }

    private int nextFreeId() {
        for (int offset = 0; offset < entriesById.length; offset++) {
            int id = (nextId + offset) % entriesById.length;
            if (entriesById[id] == null) return id;
        }
        return -1;
    }

    private String uniqueName(String base, int excludedId) {
        String candidate = base;
        int suffix = 2;
        while (nameExists(candidate, excludedId)) candidate = base + " " + suffix++;
        return candidate;
    }

    private boolean nameExists(String name, int excludedId) {
        for (Entry entry : entries) if (entry.id != excludedId && entry.name.equalsIgnoreCase(name)) return true;
        return false;
    }

    private static String cleanName(String requestedName) {
        String name = requestedName == null ? "" : requestedName.trim().replaceAll("[^A-Za-z0-9 _-]", "");
        if (name.isEmpty()) name = "variable";
        return name.length() > 24 ? name.substring(0, 24) : name;
    }

    private static int clamp(int value) {
        return Math.max(0, value);
    }

    public static final class Entry {
        private final int id;
        private String name;
        private int value;

        private Entry(int id, String name, int value) {
            this.id = id;
            this.name = name;
            this.value = clamp(value);
        }

        public int id() { return id; }
        public String name() { return name; }
        public int value() { return value; }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("Id", id);
            tag.putString("Name", name);
            tag.putInt("Value", value);
            return tag;
        }

        private static Entry load(CompoundTag tag, boolean migrateWholeNumbers) {
            if (!tag.contains("Name", Tag.TAG_STRING)) return null;
            int value = tag.getInt("Value");
            return new Entry(tag.getInt("Id"), cleanName(tag.getString("Name")), migrateWholeNumbers ? MachineControlSchedule.toScaled(value) : value);
        }
    }
}
