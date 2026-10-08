package net.mads.industron.material.forging;

import net.mads.industron.Industron;
import net.mads.industron.material.MaterialPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Persistent wear and in-progress manual forging state for vanilla anvils. */
public final class AnvilForgeSavedData extends SavedData {
    private static final String NAME = Industron.MOD_ID + "_anvil_forging";

    private final Map<Long, Entry> entries = new HashMap<>();

    private AnvilForgeSavedData() {
    }

    public static AnvilForgeSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new Factory<>(AnvilForgeSavedData::new, AnvilForgeSavedData::load),
                NAME
        );
    }

    private static AnvilForgeSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        AnvilForgeSavedData data = new AnvilForgeSavedData();
        ListTag list = tag.getList("Anvils", Tag.TAG_COMPOUND);
        for (Tag raw : list) {
            if (!(raw instanceof CompoundTag entryTag)) continue;
            long pos = entryTag.getLong("Pos");
            int wear = Math.max(0, entryTag.getInt("Wear"));
            Session session = null;
            if (entryTag.contains("Session", Tag.TAG_COMPOUND)) {
                CompoundTag sessionTag = entryTag.getCompound("Session");
                String materialId = sessionTag.getString("Material");
                MaterialPart original = part(sessionTag.getString("OriginalPart"));
                MaterialPart visual = part(sessionTag.getString("VisualPart"));
                int count = sessionTag.getInt("OriginalCount");
                int forgeValue = sessionTag.getInt("ForgeValue");
                boolean worked = sessionTag.getBoolean("Worked");
                if (!materialId.isBlank() && original != null && visual != null && count > 0 && forgeValue >= 0) {
                    session = new Session(materialId, original, count, forgeValue, visual, worked);
                }
            }
            if (wear > 0 || session != null) data.entries.put(pos, new Entry(wear, session));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<Long, Entry> stored : entries.entrySet()) {
            Entry value = stored.getValue();
            if (value.wear <= 0 && value.session == null) continue;
            CompoundTag entry = new CompoundTag();
            entry.putLong("Pos", stored.getKey());
            entry.putInt("Wear", value.wear);
            if (value.session != null) {
                Session session = value.session;
                CompoundTag sessionTag = new CompoundTag();
                sessionTag.putString("Material", session.materialId);
                sessionTag.putString("OriginalPart", session.originalPart.name());
                sessionTag.putInt("OriginalCount", session.originalCount);
                sessionTag.putInt("ForgeValue", session.forgeValue);
                sessionTag.putString("VisualPart", session.visualPart.name());
                sessionTag.putBoolean("Worked", session.worked);
                entry.put("Session", sessionTag);
            }
            list.add(entry);
        }
        tag.put("Anvils", list);
        return tag;
    }

    public Optional<Session> session(BlockPos pos) {
        Entry entry = entries.get(pos.asLong());
        return entry == null ? Optional.empty() : Optional.ofNullable(entry.session);
    }

    public Session start(BlockPos pos, String materialId, MaterialPart originalPart) {
        Entry entry = entries.computeIfAbsent(pos.asLong(), ignored -> new Entry(0, null));
        Session session = new Session(
                materialId,
                originalPart,
                1,
                originalPart.forgeValue(),
                originalPart,
                false
        );
        entry.session = session;
        setDirty();
        return session;
    }

    public void addOriginalItem(BlockPos pos) {
        Entry entry = entries.get(pos.asLong());
        if (entry == null || entry.session == null) return;
        entry.session.originalCount++;
        setDirty();
    }

    public void setForgeValue(BlockPos pos, int forgeValue, MaterialPart stablePart) {
        Entry entry = entries.get(pos.asLong());
        if (entry == null || entry.session == null) return;

        Session session = entry.session;
        session.forgeValue = forgeValue;
        session.worked = true;

        if (stablePart != null) {
            // A completed MaterialPart becomes the new checkpoint. Later cancelled work
            // returns to this form instead of the very first form placed on the anvil.
            session.originalPart = stablePart;
            session.originalCount = 1;
            session.visualPart = stablePart;
        }

        setDirty();
    }

    /** Removes exactly one checkpoint item and discards all unfinished shaping after it. */
    public MaterialPart removeOneAndReset(BlockPos pos) {
        Entry entry = entries.get(pos.asLong());
        if (entry == null || entry.session == null) return null;

        Session session = entry.session;
        MaterialPart refund = session.originalPart;
        session.originalCount--;

        if (session.originalCount <= 0) {
            entry.session = null;
            cleanup(pos.asLong(), entry);
        } else {
            session.forgeValue = session.originalPart.forgeValue();
            session.visualPart = session.originalPart;
            session.worked = false;
        }

        setDirty();
        return refund;
    }

    public int wear(BlockPos pos) {
        Entry entry = entries.get(pos.asLong());
        return entry == null ? 0 : entry.wear;
    }

    public int addWear(BlockPos pos, int amount) {
        Entry entry = entries.computeIfAbsent(pos.asLong(), ignored -> new Entry(0, null));
        entry.wear = Math.max(0, entry.wear + Math.max(0, amount));
        setDirty();
        return entry.wear;
    }

    public void setWear(BlockPos pos, int wear) {
        Entry entry = entries.computeIfAbsent(pos.asLong(), ignored -> new Entry(0, null));
        entry.wear = Math.max(0, wear);
        cleanup(pos.asLong(), entry);
        setDirty();
    }

    public void clearSession(BlockPos pos) {
        Entry entry = entries.get(pos.asLong());
        if (entry == null) return;
        entry.session = null;
        cleanup(pos.asLong(), entry);
        setDirty();
    }

    public void clearAll(BlockPos pos) {
        if (entries.remove(pos.asLong()) != null) setDirty();
    }

    private void cleanup(long key, Entry entry) {
        if (entry.wear <= 0 && entry.session == null) entries.remove(key);
    }

    private static MaterialPart part(String name) {
        if (name == null || name.isBlank()) return null;
        try {
            return MaterialPart.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static final class Entry {
        private int wear;
        private Session session;

        private Entry(int wear, Session session) {
            this.wear = wear;
            this.session = session;
        }
    }

    public static final class Session {
        private final String materialId;
        private MaterialPart originalPart;
        private int originalCount;
        private int forgeValue;
        private MaterialPart visualPart;
        private boolean worked;

        private Session(
                String materialId,
                MaterialPart originalPart,
                int originalCount,
                int forgeValue,
                MaterialPart visualPart,
                boolean worked
        ) {
            this.materialId = materialId;
            this.originalPart = originalPart;
            this.originalCount = originalCount;
            this.forgeValue = forgeValue;
            this.visualPart = visualPart;
            this.worked = worked;
        }

        public String materialId() { return materialId; }
        public MaterialPart originalPart() { return originalPart; }
        public int originalCount() { return originalCount; }
        public int forgeValue() { return forgeValue; }
        public MaterialPart visualPart() { return visualPart; }
        public boolean worked() { return worked; }
    }
}
