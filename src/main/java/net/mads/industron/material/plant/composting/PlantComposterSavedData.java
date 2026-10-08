package net.mads.industron.material.plant.composting;

import net.mads.industron.Industron;
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

/** Persistent material identity for vanilla Composters used by Industron plant composting. */
public final class PlantComposterSavedData extends SavedData {
    private static final String NAME = Industron.MOD_ID + "_plant_composters";

    private final Map<Long, Entry> entries = new HashMap<>();

    private PlantComposterSavedData() {
    }

    public static PlantComposterSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new Factory<>(PlantComposterSavedData::new, PlantComposterSavedData::load),
                NAME
        );
    }

    private static PlantComposterSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        PlantComposterSavedData data = new PlantComposterSavedData();
        ListTag list = tag.getList("Composters", Tag.TAG_COMPOUND);
        for (Tag raw : list) {
            if (!(raw instanceof CompoundTag entryTag)) continue;
            long pos = entryTag.getLong("Pos");
            String material = entryTag.getString("Material");
            int units = entryTag.getInt("Units");
            if (material.isBlank() || units <= 0) continue;
            data.entries.put(pos, new Entry(material, units));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<Long, Entry> stored : entries.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("Pos", stored.getKey());
            entry.putString("Material", stored.getValue().materialId());
            entry.putInt("Units", stored.getValue().units());
            list.add(entry);
        }
        tag.put("Composters", list);
        return tag;
    }

    public Optional<Entry> get(BlockPos pos) {
        return Optional.ofNullable(entries.get(pos.asLong()));
    }

    public void set(BlockPos pos, String materialId, int units) {
        if (materialId == null || materialId.isBlank() || units <= 0) {
            remove(pos);
            return;
        }
        entries.put(pos.asLong(), new Entry(materialId, units));
        setDirty();
    }

    public void remove(BlockPos pos) {
        if (entries.remove(pos.asLong()) != null) setDirty();
    }

    public record Entry(String materialId, int units) {
    }
}
